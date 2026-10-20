"""定时任务。

对齐 Java ``LeaderboardScheduler``：
- 每日 02:00 全量重算榜单
- 每 6 小时增量重算

通过 ``schedule_job_lock`` 表加锁，防止多实例部署时重复执行。
"""

from __future__ import annotations

import atexit
import uuid

from apscheduler.schedulers.background import BackgroundScheduler

from app.core.config import settings
from app.db.session import session_scope
from app.services.leaderboard import (
    LeaderboardService,
    acquire_job_lock,
    release_job_lock,
)

_scheduler: BackgroundScheduler | None = None
# 本实例标识，用于任务锁属主判定
_OWNER = uuid.uuid4().hex[:16]


def _run_recalculate(mode: str) -> None:
    """执行榜单重算（带分布式锁）。"""
    job_key = f"leaderboard:{mode}"
    try:
        with session_scope() as db:
            if not acquire_job_lock(db, job_key, _OWNER, ttl_seconds=3600):
                print(f"[scheduler] 任务 {job_key} 已被其他实例占用，跳过")
                return
            result = LeaderboardService(db).recalculate(refresh_mode=mode)
            print(f"[scheduler] {job_key} 完成：{result}")
            release_job_lock(db, job_key, _OWNER)
    except Exception as exc:  # 调度任务异常不应影响主服务
        print(f"[scheduler] {job_key} 执行失败：{exc}")


def full_recalculate() -> None:
    """全量重算（每日 02:00）。"""
    _run_recalculate("FULL")


def incremental_recalculate() -> None:
    """增量重算（每 6 小时）。"""
    _run_recalculate("INCREMENTAL")


def start_scheduler() -> None:
    """启动调度器。"""
    global _scheduler
    if _scheduler is not None:
        return

    cron = settings.leaderboard_full_cron
    _scheduler = BackgroundScheduler(timezone="Asia/Shanghai")
    _scheduler.add_job(
        full_recalculate,
        trigger="cron",
        hour=cron.get("hour", 2),
        minute=cron.get("minute", 0),
        id="leaderboard_full",
        replace_existing=True,
    )
    _scheduler.add_job(
        incremental_recalculate,
        trigger="cron",
        hour="0,6,12,18",
        minute=0,
        id="leaderboard_incremental",
        replace_existing=True,
    )
    _scheduler.start()
    atexit.register(stop_scheduler)
    print("[scheduler] 榜单定时任务已启动（全量 02:00，增量每 6 小时）")


def stop_scheduler() -> None:
    """停止调度器。"""
    global _scheduler
    if _scheduler is not None:
        _scheduler.shutdown(wait=False)
        _scheduler = None
