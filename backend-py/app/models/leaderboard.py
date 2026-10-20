"""榜单与调度领域 ORM 模型。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, LogicDeleteMixin, PK, TimestampMixin


class LeaderboardSnapshot(Base, TimestampMixin):
    """榜单快照表。

    由定时任务重算写入，读接口直接查快照，避免每次全表聚合。
    """

    __tablename__ = "leaderboard_snapshot"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    board_type: Mapped[str | None] = mapped_column(String(50))
    board_name: Mapped[str | None] = mapped_column(String(100))
    equipment_id: Mapped[int | None] = mapped_column(BigInteger)
    rank_position: Mapped[int | None] = mapped_column(Integer)
    total_score: Mapped[float | None] = mapped_column(Numeric(12, 4))
    hot_score: Mapped[float | None] = mapped_column(Numeric(12, 4))
    value_score: Mapped[float | None] = mapped_column(Numeric(12, 4))
    time_decay: Mapped[float | None] = mapped_column(Numeric(8, 4))
    favorite_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    comment_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    avg_rating: Mapped[float | None] = mapped_column(Numeric(3, 2))
    satisfaction_rate: Mapped[float | None] = mapped_column(Numeric(5, 4))
    latest_interaction_time: Mapped[datetime | None] = mapped_column(DateTime)
    board_reason: Mapped[str | None] = mapped_column(String(255))
    refresh_mode: Mapped[str | None] = mapped_column(String(20))
    refreshed_at: Mapped[datetime | None] = mapped_column(DateTime)


class LeaderboardSubscription(Base, TimestampMixin):
    """榜单订阅表。"""

    __tablename__ = "leaderboard_subscription"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int | None] = mapped_column(BigInteger)
    board_type: Mapped[str | None] = mapped_column(String(50))
    active: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    last_viewed_at: Mapped[datetime | None] = mapped_column(DateTime)


class LeaderboardShareStat(Base, TimestampMixin):
    """榜单分享统计表。"""

    __tablename__ = "leaderboard_share_stat"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    board_type: Mapped[str | None] = mapped_column(String(50))
    equipment_id: Mapped[int | None] = mapped_column(BigInteger)
    channel: Mapped[str | None] = mapped_column(String(50))
    share_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    last_share_user_id: Mapped[int | None] = mapped_column(BigInteger)
    last_shared_at: Mapped[datetime | None] = mapped_column(DateTime)


class ScheduleJobLock(Base, TimestampMixin, LogicDeleteMixin):
    """定时任务锁表。

    防止多实例部署时重复执行榜单重算（对应 schedule_job_lock）。
    """

    __tablename__ = "schedule_job_lock"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    job_key: Mapped[str | None] = mapped_column(String(100))
    owner: Mapped[str | None] = mapped_column(String(100))
    locked_until: Mapped[datetime | None] = mapped_column(DateTime)
