"""后台管理服务：数据概览与评论审核。

由 Java ``AdminDashboardController`` 相关实现移植。
"""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any, Dict, List

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.equipment import Equipment
from app.models.interaction import BrowseHistory, Comment, Favorite
from app.models.user import User

REVIEW_STATUS_APPROVED = "APPROVED"
REVIEW_STATUS_REJECTED = "REJECTED"
REVIEW_STATUS_PENDING = "PENDING"


class AdminService:
    """后台管理服务。"""

    def __init__(self, db: Session):
        self.db = db

    def overview(self) -> Dict[str, Any]:
        """后台概览统计。"""
        user_count = self._count(User)
        equipment_count = self._count(Equipment)
        favorite_count = self._count(Favorite)
        comment_count = self._count(Comment)

        week_ago = datetime.now() - timedelta(days=7)
        new_users = (
            self.db.execute(
                select(func.count())
                .select_from(User)
                .where(User.deleted == 0, User.create_time >= week_ago)
            ).scalar()
            or 0
        )
        pending_comments = (
            self.db.execute(
                select(func.count())
                .select_from(Comment)
                .where(Comment.deleted == 0, Comment.review_status == REVIEW_STATUS_PENDING)
            ).scalar()
            or 0
        )

        return {
            "userCount": user_count,
            "equipmentCount": equipment_count,
            "favoriteCount": favorite_count,
            "commentCount": comment_count,
            "newUserCount": new_users,
            "pendingCommentCount": pending_comments,
        }

    def category_distribution(self) -> List[Dict[str, Any]]:
        """装备分类分布。"""
        rows = (
            self.db.execute(
                select(Equipment.category_id, func.count().label("cnt"))
                .where(Equipment.deleted == 0)
                .group_by(Equipment.category_id)
            )
            .all()
        )
        return [{"categoryId": r[0], "count": r[1]} for r in rows]

    def top_equipments(self, limit: int = 10) -> List[Dict[str, Any]]:
        """热门装备 Top N。"""
        rows = list(
            self.db.execute(
                select(Equipment)
                .where(Equipment.deleted == 0, Equipment.status == 1)
                .order_by(Equipment.view_count.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )
        return [
            {
                "id": e.id,
                "name": e.name,
                "brand": e.brand,
                "viewCount": e.view_count,
                "favoriteCount": e.favorite_count,
            }
            for e in rows
        ]

    def daily_trend(self, days: int = 7) -> List[Dict[str, Any]]:
        """近 N 天浏览趋势。"""
        start = datetime.now() - timedelta(days=days)
        rows = (
            self.db.execute(
                select(func.date(BrowseHistory.create_time).label("day"), func.count())
                .where(BrowseHistory.deleted == 0, BrowseHistory.create_time >= start)
                .group_by(func.date(BrowseHistory.create_time))
            )
            .all()
        )
        return [{"date": str(r[0]), "count": r[1]} for r in rows]

    # ---------------- 评论审核 ----------------

    def list_pending_comments(self, limit: int = 50) -> List[Dict[str, Any]]:
        """待审核评论。"""
        rows = list(
            self.db.execute(
                select(Comment)
                .where(Comment.deleted == 0, Comment.review_status == REVIEW_STATUS_PENDING)
                .order_by(Comment.create_time.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )
        return [self._comment_dict(c) for c in rows]

    def review_comment(
        self, comment_id: int, reviewer_id: int, approved: bool, remark: str = ""
    ) -> Dict[str, Any]:
        """审核评论：通过则展示，驳回则隐藏。"""
        row = self.db.get(Comment, comment_id)
        if row is None or row.deleted == 1:
            raise LookupError("评论不存在")

        row.review_status = REVIEW_STATUS_APPROVED if approved else REVIEW_STATUS_REJECTED
        row.reviewed_by = reviewer_id
        row.reviewed_at = datetime.now()
        row.review_remark = remark
        row.status = 1 if approved else 0
        self.db.commit()
        return self._comment_dict(row)

    def _count(self, model: Any) -> int:
        """通用计数（过滤逻辑删除）。"""
        stmt = select(func.count()).select_from(model)
        if hasattr(model, "deleted"):
            stmt = stmt.where(model.deleted == 0)
        return self.db.execute(stmt).scalar() or 0

    @staticmethod
    def _comment_dict(c: Comment) -> Dict[str, Any]:
        """评论序列化（含审核信息）。"""
        return {
            "id": c.id,
            "userId": c.user_id,
            "equipmentId": c.equipment_id,
            "content": c.content,
            "rating": c.rating,
            "status": c.status,
            "reviewStatus": c.review_status,
            "reviewRemark": c.review_remark,
            "createTime": c.create_time.strftime("%Y-%m-%d %H:%M:%S")
            if c.create_time
            else None,
        }
