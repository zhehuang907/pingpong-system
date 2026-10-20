"""榜单服务。

由 Java ``LeaderboardServiceImpl`` 移植。评分公式与权重逐项对齐：

- ``baseScore = 收藏数×0.3 + 评论数×0.2 + 均分×0.5 + 时间衰减``
- ``timeDecay = exp(-距最近互动小时数 / 240) × 1.8``
- ``hotScore  = baseScore + log1p(浏览量) × 0.55``
- ``valueScore = min(650 / max(price, 1), 3.5) + 均分 × 0.15``
- ``totalScore`` 按榜单类型二次加权（人气/性价比/新手/专业）

榜单结果写入快照表，读接口直查快照，避免每次请求全表聚合。
"""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.exceptions import BusinessException
from app.models.equipment import Equipment
from app.models.interaction import Comment, Favorite
from app.models.leaderboard import (
    LeaderboardShareStat,
    LeaderboardSnapshot,
    LeaderboardSubscription,
    ScheduleJobLock,
)

# 与 Java 常量一致
FAVORITE_WEIGHT = 0.3
COMMENT_WEIGHT = 0.2
RATING_WEIGHT = 0.5
MAX_BOARD_ITEMS = 50

BOARDS: Dict[str, Dict[str, str]] = {
    "weekly_popular": {"title": "本周人气王", "subtitle": "基于近 7 天真实互动热度生成"},
    "value_king": {"title": "性价比之王", "subtitle": "综合价格优势与真实口碑表现"},
    "beginner_pick": {"title": "新手入门首选", "subtitle": "面向入门阶段用户的稳定推荐"},
    "pro_favorite": {"title": "专业选手最爱", "subtitle": "偏向进阶对抗与专业场景的高分装备"},
}

DEFAULT_BOARD = "weekly_popular"


def resolve_board_type(board_type: Optional[str]) -> str:
    """归一化榜单类型，未知值回落默认榜。"""
    if not board_type:
        return DEFAULT_BOARD
    key = board_type.strip().lower()
    return key if key in BOARDS else DEFAULT_BOARD


def _round4(value: float) -> float:
    """保留 4 位小数，对齐 Java round(..., 4)。"""
    return round(value, 4)


def calculate_time_decay(latest: Optional[datetime], now: datetime) -> float:
    """时间衰减：``exp(-hours / 240) × 1.8``，hours 下限 1。"""
    if latest is None:
        hours = 240.0
    else:
        hours = max(1.0, (now - latest).total_seconds() / 3600.0)
    return _round4(math.exp(-hours / 240.0) * 1.8)


def calculate_value_score(price: Any, avg_rating: float) -> float:
    """性价比分：``min(650 / max(price,1), 3.5) + 均分 × 0.15``。"""
    try:
        p = max(float(price or 1), 1.0)
    except (TypeError, ValueError):
        p = 1.0
    price_score = min(650.0 / p, 3.5)
    return _round4(price_score + avg_rating * 0.15)


def beginner_boost(equipment: Equipment, avg_rating: float) -> float:
    """新手榜加成。"""
    boost = 0.0
    suitable = (equipment.suitable_for or "").lower()
    if any(k in suitable for k in ("入门", "新手", "训练")):
        boost += 1.35
    price = float(equipment.price or 0) if equipment.price is not None else 0.0
    if price <= 400:
        boost += 0.65
    if avg_rating >= 4.2:
        boost += 0.35
    return boost


def pro_boost(equipment: Equipment, avg_rating: float) -> float:
    """专业榜加成。"""
    boost = 0.0
    suitable = (equipment.suitable_for or "").lower()
    if any(k in suitable for k in ("专业", "竞技", "高阶")):
        boost += 1.45
    if avg_rating >= 4.5:
        boost += 0.8
    price = float(equipment.price or 0) if equipment.price is not None else 0.0
    if price >= 450:
        boost += 0.35
    return boost


class LeaderboardService:
    """榜单服务。"""

    def __init__(self, db: Session):
        self.db = db

    # ---------------- 读 ----------------

    def list_boards(self) -> List[Dict[str, Any]]:
        """榜单目录。"""
        return [
            {"code": code, "title": meta["title"], "subtitle": meta["subtitle"]}
            for code, meta in BOARDS.items()
        ]

    def get_board(
        self, board_type: Optional[str], limit: int = 20
    ) -> Dict[str, Any]:
        """单个榜单详情（读快照）。"""
        code = resolve_board_type(board_type)
        rows = list(
            self.db.execute(
                select(LeaderboardSnapshot)
                .where(LeaderboardSnapshot.board_type == code)
                .order_by(LeaderboardSnapshot.rank_position.asc())
                .limit(limit)
            )
            .scalars()
            .all()
        )

        items = []
        for r in rows:
            eq = self.db.get(Equipment, r.equipment_id) if r.equipment_id else None
            items.append(
                {
                    "rankPosition": r.rank_position,
                    "equipmentId": r.equipment_id,
                    "equipmentName": eq.name if eq else None,
                    "brand": eq.brand if eq else None,
                    "price": float(eq.price) if (eq and eq.price is not None) else None,
                    "mainImage": eq.main_image if eq else None,
                    "imageUrl": eq.image_url if eq else None,
                    "totalScore": float(r.total_score) if r.total_score is not None else 0.0,
                    "hotScore": float(r.hot_score) if r.hot_score is not None else 0.0,
                    "valueScore": float(r.value_score) if r.value_score is not None else 0.0,
                    "favoriteCount": r.favorite_count,
                    "commentCount": r.comment_count,
                    "avgRating": float(r.avg_rating) if r.avg_rating is not None else 0.0,
                    "satisfactionRate": float(r.satisfaction_rate)
                    if r.satisfaction_rate is not None
                    else 0.0,
                    "boardReason": r.board_reason,
                }
            )

        meta = BOARDS[code]
        return {
            "boardType": code,
            "boardName": meta["title"],
            "subtitle": meta["subtitle"],
            "items": items,
            "refreshedAt": rows[0].refreshed_at.strftime("%Y-%m-%d %H:%M:%S")
            if rows and rows[0].refreshed_at
            else None,
        }

    def get_overview(self, limit: int = 10) -> Dict[str, Any]:
        """榜单总览：四类榜单各取前 N。"""
        return {
            "boards": [self.get_board(code, limit) for code in BOARDS],
        }

    # ---------------- 订阅 ----------------

    def subscribe(self, user_id: int, board_type: Optional[str]) -> bool:
        """订阅榜单。"""
        code = resolve_board_type(board_type)
        row = self._find_subscription(user_id, code)
        if row is None:
            self.db.add(
                LeaderboardSubscription(user_id=user_id, board_type=code, active=1)
            )
        else:
            row.active = 1
        self.db.commit()
        return True

    def unsubscribe(self, user_id: int, board_type: Optional[str]) -> bool:
        """取消订阅。"""
        code = resolve_board_type(board_type)
        row = self._find_subscription(user_id, code)
        if row is not None:
            row.active = 0
            self.db.commit()
        return True

    def _find_subscription(
        self, user_id: int, board_type: str
    ) -> Optional[LeaderboardSubscription]:
        """查找订阅记录。"""
        return (
            self.db.execute(
                select(LeaderboardSubscription).where(
                    LeaderboardSubscription.user_id == user_id,
                    LeaderboardSubscription.board_type == board_type,
                )
            )
            .scalars()
            .first()
        )

    # ---------------- 分享统计 ----------------

    def record_share(
        self, user_id: int, board_type: Optional[str], equipment_id: int, channel: str
    ) -> Dict[str, Any]:
        """记录榜单分享。"""
        code = resolve_board_type(board_type)
        row = (
            self.db.execute(
                select(LeaderboardShareStat).where(
                    LeaderboardShareStat.board_type == code,
                    LeaderboardShareStat.equipment_id == equipment_id,
                    LeaderboardShareStat.channel == channel,
                )
            )
            .scalars()
            .first()
        )
        if row is None:
            row = LeaderboardShareStat(
                board_type=code,
                equipment_id=equipment_id,
                channel=channel,
                share_count=0,
            )
            self.db.add(row)

        row.share_count = (row.share_count or 0) + 1
        row.last_share_user_id = user_id
        row.last_shared_at = datetime.now()
        self.db.commit()

        return {
            "boardType": code,
            "equipmentId": equipment_id,
            "channel": channel,
            "shareCount": row.share_count,
        }

    # ---------------- 重算 ----------------

    def recalculate(self, refresh_mode: str = "FULL") -> Dict[str, Any]:
        """重算全部榜单并写入快照。

        Args:
            refresh_mode: FULL（全量）/ INCREMENTAL（增量），仅作标记写入。
        """
        now = datetime.now()
        equipments = list(
            self.db.execute(
                select(Equipment).where(Equipment.status == 1, Equipment.deleted == 0)
            )
            .scalars()
            .all()
        )

        metrics_by_board: Dict[str, List[Dict[str, Any]]] = {code: [] for code in BOARDS}

        for eq in equipments:
            favorites = self._count_favorites(eq.id)
            comments = self._comments(eq.id)
            comment_count = len(comments)
            avg_rating = (
                round(sum(c.rating or 0 for c in comments) / comment_count, 2)
                if comment_count
                else 0.0
            )
            satisfaction = (
                round(
                    sum(1 for c in comments if (c.rating or 0) >= 4) / comment_count * 100,
                    2,
                )
                if comment_count
                else round(avg_rating * 20, 2)
            )
            latest = self._latest_interaction(eq, comments)
            decay = calculate_time_decay(latest, now)

            base = _round4(
                favorites * FAVORITE_WEIGHT
                + comment_count * COMMENT_WEIGHT
                + avg_rating * RATING_WEIGHT
                + decay
            )
            hot = _round4(base + math.log1p(eq.view_count or 0) * 0.55)
            value = calculate_value_score(eq.price, avg_rating)

            totals = {
                "weekly_popular": _round4(base + math.log1p(eq.view_count or 0) * 0.25),
                "value_king": _round4(base + value * 0.8),
                "beginner_pick": _round4(base + beginner_boost(eq, avg_rating)),
                "pro_favorite": _round4(base + pro_boost(eq, avg_rating)),
            }

            for code, total in totals.items():
                metrics_by_board[code].append(
                    {
                        "equipment": eq,
                        "total": total,
                        "hot": hot,
                        "value": value,
                        "decay": decay,
                        "favorites": favorites,
                        "comments": comment_count,
                        "avg_rating": avg_rating,
                        "satisfaction": satisfaction,
                        "latest": latest,
                    }
                )

        written = 0
        for code, metrics in metrics_by_board.items():
            metrics.sort(key=lambda m: m["total"], reverse=True)
            self._replace_snapshots(code, metrics[:MAX_BOARD_ITEMS], now, refresh_mode)
            written += len(metrics[:MAX_BOARD_ITEMS])

        return {"refreshedAt": now.strftime("%Y-%m-%d %H:%M:%S"), "items": written}

    def _replace_snapshots(
        self,
        board_type: str,
        metrics: List[Dict[str, Any]],
        now: datetime,
        refresh_mode: str,
    ) -> None:
        """清空旧快照并写入新榜单（同事务内替换）。"""
        old = (
            self.db.execute(
                select(LeaderboardSnapshot).where(
                    LeaderboardSnapshot.board_type == board_type
                )
            )
            .scalars()
            .all()
        )
        for o in old:
            self.db.delete(o)
        self.db.flush()

        for position, m in enumerate(metrics, start=1):
            eq: Equipment = m["equipment"]
            self.db.add(
                LeaderboardSnapshot(
                    board_type=board_type,
                    board_name=BOARDS[board_type]["title"],
                    equipment_id=eq.id,
                    rank_position=position,
                    total_score=m["total"],
                    hot_score=m["hot"],
                    value_score=m["value"],
                    time_decay=m["decay"],
                    favorite_count=m["favorites"],
                    comment_count=m["comments"],
                    avg_rating=m["avg_rating"],
                    satisfaction_rate=m["satisfaction"],
                    latest_interaction_time=m["latest"],
                    board_reason=_build_reason(board_type, m),
                    refresh_mode=refresh_mode,
                    refreshed_at=now,
                )
            )
        self.db.commit()

    # ---------------- 内部 ----------------

    def _count_favorites(self, equipment_id: int) -> int:
        """装备收藏数。"""
        return (
            self.db.execute(
                select(func.count())
                .select_from(Favorite)
                .where(Favorite.equipment_id == equipment_id, Favorite.deleted == 0)
            ).scalar()
            or 0
        )

    def _comments(self, equipment_id: int) -> List[Comment]:
        """装备评论。"""
        return list(
            self.db.execute(
                select(Comment).where(
                    Comment.equipment_id == equipment_id,
                    Comment.deleted == 0,
                    Comment.status == 1,
                )
            )
            .scalars()
            .all()
        )

    def _latest_interaction(
        self, equipment: Equipment, comments: List[Comment]
    ) -> Optional[datetime]:
        """最近互动时间（评论时间优先，回落到更新时间）。"""
        times = [c.create_time for c in comments if c.create_time]
        if times:
            return max(times)
        return equipment.update_time or equipment.create_time


def _build_reason(board_type: str, metric: Dict[str, Any]) -> str:
    """生成上榜理由。"""
    if board_type == "weekly_popular":
        return f"近 7 天 {metric['favorites']} 次收藏、{metric['comments']} 条讨论"
    if board_type == "value_king":
        return f"性价比得分 {metric['value']}，均价表现突出"
    if board_type == "beginner_pick":
        return "适合入门阶段，口碑稳定"
    return "专业场景表现优异，评分领先"


# ============================================================
# 分布式任务锁（对应 Java schedule_job_lock）
# ============================================================


def acquire_job_lock(db: Session, job_key: str, owner: str, ttl_seconds: int = 3600) -> bool:
    """尝试获取任务锁，避免多实例重复执行。

    锁未过期且属主不同则获取失败；锁过期后允许抢占。
    """
    now = datetime.now()
    row = (
        db.execute(select(ScheduleJobLock).where(ScheduleJobLock.job_key == job_key))
        .scalars()
        .first()
    )

    if row is None:
        db.add(
            ScheduleJobLock(
                job_key=job_key, owner=owner, locked_until=now + timedelta(seconds=ttl_seconds)
            )
        )
        db.commit()
        return True

    if row.locked_until is not None and row.locked_until > now and row.owner != owner:
        return False

    row.owner = owner
    row.locked_until = now + timedelta(seconds=ttl_seconds)
    db.commit()
    return True


def release_job_lock(db: Session, job_key: str, owner: str) -> None:
    """释放任务锁。"""
    row = (
        db.execute(select(ScheduleJobLock).where(ScheduleJobLock.job_key == job_key))
        .scalars()
        .first()
    )
    if row is not None and row.owner == owner:
        row.locked_until = datetime.now()
        db.commit()
