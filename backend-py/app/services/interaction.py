"""收藏 / 评论 / 购物车 / 浏览历史服务。

由 Java ``FavoriteServiceImpl``、``CommentServiceImpl``、``CartServiceImpl`` 移植。

收藏采用「切换」语义：已收藏则取消并回退计数，未收藏则新增并递增计数；
计数变更走 SQL 原子更新，避免并发下计数漂移。
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional, Sequence, Tuple

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.core.exceptions import BusinessException, ForbiddenException, NotFoundException
from app.models.equipment import Equipment
from app.models.interaction import BrowseHistory, CartItem, Comment, CommentLike, Favorite
from app.models.user import User


class FavoriteService:
    """收藏服务。"""

    def __init__(self, db: Session):
        self.db = db

    def is_favorited(self, user_id: int, equipment_id: int) -> bool:
        """是否已收藏。"""
        row = self._find(user_id, equipment_id)
        return row is not None

    def list_favorites(self, user_id: int) -> List[Favorite]:
        """用户的收藏列表。"""
        return list(
            self.db.execute(
                select(Favorite)
                .where(Favorite.user_id == user_id, Favorite.deleted == 0)
                .order_by(Favorite.create_time.desc())
            )
            .scalars()
            .all()
        )

    def count_favorites(self, user_id: int) -> int:
        """收藏数量。"""
        return (
            self.db.execute(
                select(func.count())
                .select_from(Favorite)
                .where(Favorite.user_id == user_id, Favorite.deleted == 0)
            ).scalar()
            or 0
        )

    def toggle(
        self, user_id: int, equipment_id: int, equipment_service: Any
    ) -> Tuple[bool, int]:
        """切换收藏状态。

        Returns:
            (是否已收藏, 当前收藏总数)
        """
        equipment = self.db.get(Equipment, equipment_id)
        if equipment is None or equipment.deleted == 1:
            raise NotFoundException("装备不存在或已下架")

        # 与 Java 一致：返回值基于进入方法时的计数快照 ± 1，而非重新查库，
        # 保证「切换」接口在一次请求内自洽（并发下计数以数据库原子更新为准）。
        snapshot = equipment.favorite_count or 0

        existing = self._find(user_id, equipment_id)
        if existing is not None:
            existing.deleted = 1
            self.db.commit()
            equipment_service.incr_favorite_count(equipment_id, -1)
            return False, max(0, snapshot - 1)

        row = Favorite(user_id=user_id, equipment_id=equipment_id, deleted=0)
        self.db.add(row)
        self.db.commit()
        equipment_service.incr_favorite_count(equipment_id, 1)
        return True, snapshot + 1

    def _find(self, user_id: int, equipment_id: int) -> Optional[Favorite]:
        """查找有效收藏记录。"""
        return (
            self.db.execute(
                select(Favorite).where(
                    Favorite.user_id == user_id,
                    Favorite.equipment_id == equipment_id,
                    Favorite.deleted == 0,
                )
            )
            .scalars()
            .first()
        )


class CommentService:
    """评论服务（含回复、点赞、后台审核）。"""

    def __init__(self, db: Session):
        self.db = db

    def list_by_equipment(
        self, equipment_id: int, current_user_id: Optional[int] = None, limit: int = 50
    ) -> List[Dict[str, Any]]:
        """装备下的一级评论（含当前用户点赞状态）。"""
        rows = list(
            self.db.execute(
                select(Comment)
                .where(
                    Comment.equipment_id == equipment_id,
                    Comment.deleted == 0,
                    Comment.status == 1,
                    Comment.parent_id == 0,
                )
                .order_by(Comment.create_time.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )

        liked_ids = self._liked_comment_ids(current_user_id) if current_user_id else set()
        user_ids = {c.user_id for c in rows}
        users = {
            u.id: u
            for u in self.db.execute(select(User).where(User.id.in_(user_ids)))
            .scalars()
            .all()
        } if user_ids else {}

        result = []
        for c in rows:
            author = users.get(c.user_id)
            result.append(
                {
                    "id": c.id,
                    "userId": c.user_id,
                    "equipmentId": c.equipment_id,
                    "parentId": c.parent_id,
                    "content": c.content,
                    "images": c.images,
                    "rating": c.rating,
                    "likeCount": c.like_count,
                    "replyCount": c.reply_count,
                    "status": c.status,
                    "reviewStatus": c.review_status,
                    "createTime": c.create_time.strftime("%Y-%m-%d %H:%M:%S")
                    if c.create_time
                    else None,
                    "username": author.username if author else None,
                    "nickname": author.nickname if author else None,
                    "avatar": author.avatar if author else None,
                    "likedByCurrentUser": c.id in liked_ids,
                }
            )
        return result

    def list_replies(self, parent_id: int) -> List[Dict[str, Any]]:
        """评论的回复列表。"""
        rows = list(
            self.db.execute(
                select(Comment)
                .where(
                    Comment.parent_id == parent_id,
                    Comment.deleted == 0,
                    Comment.status == 1,
                )
                .order_by(Comment.create_time.asc())
            )
            .scalars()
            .all()
        )
        user_ids = {c.user_id for c in rows}
        users = {
            u.id: u
            for u in self.db.execute(select(User).where(User.id.in_(user_ids)))
            .scalars()
            .all()
        } if user_ids else {}
        return [
            {
                "id": c.id,
                "userId": c.user_id,
                "parentId": c.parent_id,
                "content": c.content,
                "createTime": c.create_time.strftime("%Y-%m-%d %H:%M:%S")
                if c.create_time
                else None,
                "username": (users.get(c.user_id).username if users.get(c.user_id) else None),
                "nickname": (users.get(c.user_id).nickname if users.get(c.user_id) else None),
                "avatar": (users.get(c.user_id).avatar if users.get(c.user_id) else None),
            }
            for c in rows
        ]

    def create(
        self,
        user_id: int,
        equipment_id: int,
        content: str,
        parent_id: int = 0,
        rating: int = 5,
        images: Optional[str] = None,
    ) -> Comment:
        """发表评论或回复。"""
        if not content or not content.strip():
            raise BusinessException("评论内容不能为空")

        equipment = self.db.get(Equipment, equipment_id)
        if equipment is None or equipment.deleted == 1:
            raise NotFoundException("装备不存在或已下架")

        row = Comment(
            user_id=user_id,
            equipment_id=equipment_id,
            parent_id=parent_id,
            content=content.strip(),
            images=images,
            rating=rating,
            like_count=0,
            reply_count=0,
            status=1,
            review_status="PENDING",
            deleted=0,
        )
        self.db.add(row)
        self.db.flush()

        if parent_id:
            parent = self.db.get(Comment, parent_id)
            if parent is not None:
                parent.reply_count = (parent.reply_count or 0) + 1

        self.db.commit()
        self.db.refresh(row)
        return row

    def delete(self, comment_id: int, user_id: int, is_admin: bool = False) -> None:
        """删除评论（作者本人或管理员）。"""
        row = self.db.get(Comment, comment_id)
        if row is None or row.deleted == 1:
            raise NotFoundException("评论不存在")
        if not is_admin and row.user_id != user_id:
            raise ForbiddenException("无权删除他人评论")
        row.deleted = 1
        self.db.commit()

    def like(self, user_id: int, comment_id: int) -> Tuple[bool, int]:
        """切换评论点赞。

        Returns:
            (是否已点赞, 最新点赞数)
        """
        row = self.db.get(Comment, comment_id)
        if row is None or row.deleted == 1:
            raise NotFoundException("评论不存在")

        existing = (
            self.db.execute(
                select(CommentLike).where(
                    CommentLike.user_id == user_id,
                    CommentLike.comment_id == comment_id,
                    CommentLike.deleted == 0,
                )
            )
            .scalars()
            .first()
        )

        if existing is not None:
            existing.deleted = 1
            row.like_count = max(0, (row.like_count or 0) - 1)
            self.db.commit()
            return False, row.like_count

        self.db.add(CommentLike(user_id=user_id, comment_id=comment_id, deleted=0))
        row.like_count = (row.like_count or 0) + 1
        self.db.commit()
        return True, row.like_count

    # ---------------- 后台管理端 ----------------

    def admin_list(
        self,
        status: Optional[str] = None,
        keyword: Optional[str] = None,
        equipment_id: Optional[int] = None,
        page: int = 1,
        size: int = 20,
    ) -> Tuple[List[Dict[str, Any]], int]:
        """后台评论分页列表。

        Args:
            status: ``PENDING`` / ``APPROVED`` / ``REJECTED``，为空表示全部。
        """
        conds = [Comment.deleted == 0]
        if status:
            conds.append(Comment.review_status == status.upper())
        if equipment_id:
            conds.append(Comment.equipment_id == equipment_id)
        if keyword:
            pattern = f"%{keyword.strip()}%"
            conds.append(or_(Comment.content.like(pattern)))

        base = select(Comment).where(*conds)
        total = (
            self.db.execute(
                select(func.count()).select_from(base.subquery())
            ).scalar()
            or 0
        )
        rows = list(
            self.db.execute(
                base.order_by(Comment.create_time.desc())
                .offset((page - 1) * size)
                .limit(size)
            )
            .scalars()
            .all()
        )
        users = self._users_by_ids({c.user_id for c in rows})
        equipments = self._equipments_by_ids({c.equipment_id for c in rows})
        return [self._admin_dump(c, users, equipments) for c in rows], total

    def admin_detail(self, comment_id: int) -> Dict[str, Any]:
        """后台评论详情。"""
        row = self.db.get(Comment, comment_id)
        if row is None or row.deleted == 1:
            raise NotFoundException("评论不存在")
        users = self._users_by_ids({row.user_id})
        equipments = self._equipments_by_ids({row.equipment_id})
        return self._admin_dump(row, users, equipments)

    def admin_review(
        self,
        comment_id: int,
        admin_id: int,
        approved: bool,
        remark: str = "",
    ) -> Dict[str, Any]:
        """评论审核。

        通过则 ``status=1`` 对外可见；驳回则 ``status=0`` 隐藏。
        """
        row = self.db.get(Comment, comment_id)
        if row is None or row.deleted == 1:
            raise NotFoundException("评论不存在")
        row.review_status = "APPROVED" if approved else "REJECTED"
        row.status = 1 if approved else 0
        row.reviewed_by = admin_id
        row.reviewed_at = datetime.now()
        row.review_remark = remark or ("" if approved else "内容不符合规范")
        self.db.commit()
        return {"id": row.id, "reviewStatus": row.review_status, "status": row.status}

    def admin_batch_delete(self, ids: Sequence[int]) -> int:
        """批量删除评论，返回实际删除条数。"""
        if not ids:
            return 0
        rows = (
            self.db.execute(
                select(Comment).where(Comment.id.in_(ids), Comment.deleted == 0)
            )
            .scalars()
            .all()
        )
        for r in rows:
            r.deleted = 1
        self.db.commit()
        return len(rows)

    def admin_statistics(self) -> Dict[str, Any]:
        """评论审核统计概览。"""
        total = (
            self.db.execute(
                select(func.count()).select_from(Comment).where(Comment.deleted == 0)
            ).scalar()
            or 0
        )
        pending = (
            self.db.execute(
                select(func.count())
                .select_from(Comment)
                .where(Comment.deleted == 0, Comment.review_status == "PENDING")
            ).scalar()
            or 0
        )
        approved = (
            self.db.execute(
                select(func.count())
                .select_from(Comment)
                .where(Comment.deleted == 0, Comment.review_status == "APPROVED")
            ).scalar()
            or 0
        )
        rejected = (
            self.db.execute(
                select(func.count())
                .select_from(Comment)
                .where(Comment.deleted == 0, Comment.review_status == "REJECTED")
            ).scalar()
            or 0
        )
        today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        today_count = (
            self.db.execute(
                select(func.count())
                .select_from(Comment)
                .where(Comment.deleted == 0, Comment.create_time >= today)
            ).scalar()
            or 0
        )
        avg_rating = self.db.execute(
            select(func.avg(Comment.rating)).where(Comment.deleted == 0)
        ).scalar()
        return {
            "total": total,
            "pending": pending,
            "approved": approved,
            "rejected": rejected,
            "todayCount": today_count,
            "avgRating": round(float(avg_rating), 2) if avg_rating is not None else 0.0,
        }

    def _admin_dump(
        self,
        c: Comment,
        users: Dict[int, User],
        equipments: Dict[int, Equipment],
    ) -> Dict[str, Any]:
        """后台列表用的序列化。"""
        author = users.get(c.user_id)
        eq = equipments.get(c.equipment_id)
        reviewer = users.get(c.reviewed_by) if c.reviewed_by else None
        return {
            "id": c.id,
            "userId": c.user_id,
            "equipmentId": c.equipment_id,
            "parentId": c.parent_id,
            "content": c.content,
            "images": c.images,
            "rating": c.rating,
            "likeCount": c.like_count,
            "replyCount": c.reply_count,
            "status": c.status,
            "reviewStatus": c.review_status,
            "reviewRemark": c.review_remark,
            "reviewedBy": c.reviewed_by,
            "reviewerName": reviewer.username if reviewer else None,
            "username": author.username if author else None,
            "nickname": author.nickname if author else None,
            "avatar": author.avatar if author else None,
            # equipmentName 在 Java 侧为瞬态字段（非表列），此处由关联表装配
            "equipmentName": eq.name if eq else None,
            "createTime": c.create_time.strftime("%Y-%m-%d %H:%M:%S")
            if c.create_time
            else None,
            "reviewedAt": c.reviewed_at.strftime("%Y-%m-%d %H:%M:%S")
            if c.reviewed_at
            else None,
        }

    def _users_by_ids(self, ids: set) -> Dict[int, User]:
        """批量取用户，避免 N+1。"""
        if not ids:
            return {}
        rows = self.db.execute(select(User).where(User.id.in_(ids))).scalars().all()
        return {u.id: u for u in rows}

    def _equipments_by_ids(self, ids: set) -> Dict[int, Equipment]:
        """批量取装备，避免 N+1。"""
        if not ids:
            return {}
        rows = (
            self.db.execute(select(Equipment).where(Equipment.id.in_(ids)))
            .scalars()
            .all()
        )
        return {e.id: e for e in rows}

    def _liked_comment_ids(self, user_id: int) -> set:
        """当前用户已点赞的评论 ID 集合。"""
        rows = self.db.execute(
            select(CommentLike.comment_id).where(
                CommentLike.user_id == user_id, CommentLike.deleted == 0
            )
        ).all()
        return {r[0] for r in rows}


class CartService:
    """购物车服务。"""

    def __init__(self, db: Session):
        self.db = db

    def list_items(self, user_id: int) -> List[CartItem]:
        """购物车条目。"""
        return list(
            self.db.execute(
                select(CartItem)
                .where(CartItem.user_id == user_id, CartItem.deleted == 0)
                .order_by(CartItem.create_time.desc())
            )
            .scalars()
            .all()
        )

    def add(self, user_id: int, equipment_id: int, quantity: int = 1) -> CartItem:
        """加入购物车，已存在则累加数量。"""
        equipment = self.db.get(Equipment, equipment_id)
        if equipment is None or equipment.deleted == 1:
            raise NotFoundException("装备不存在或已下架")

        existing = (
            self.db.execute(
                select(CartItem).where(
                    CartItem.user_id == user_id,
                    CartItem.equipment_id == equipment_id,
                    CartItem.deleted == 0,
                )
            )
            .scalars()
            .first()
        )
        if existing is not None:
            existing.quantity = (existing.quantity or 0) + max(1, quantity)
            self.db.commit()
            self.db.refresh(existing)
            return existing

        row = CartItem(
            user_id=user_id,
            equipment_id=equipment_id,
            quantity=max(1, quantity),
            selected=1,
            deleted=0,
        )
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row

    def update_quantity(self, user_id: int, item_id: int, quantity: int) -> CartItem:
        """修改数量，数量为 0 时移除。"""
        row = self.db.get(CartItem, item_id)
        if row is None or row.deleted == 1 or row.user_id != user_id:
            raise NotFoundException("购物车条目不存在")
        if quantity <= 0:
            row.deleted = 1
        else:
            row.quantity = quantity
        self.db.commit()
        return row

    def remove(self, user_id: int, item_id: int) -> None:
        """移除条目。"""
        row = self.db.get(CartItem, item_id)
        if row is None or row.user_id != user_id:
            raise NotFoundException("购物车条目不存在")
        row.deleted = 1
        self.db.commit()

    def update_selected(self, user_id: int, item_id: int, selected: int) -> CartItem:
        """修改条目选中状态。"""
        row = self.db.get(CartItem, item_id)
        if row is None or row.deleted == 1 or row.user_id != user_id:
            raise NotFoundException("购物车条目不存在")
        row.selected = 1 if selected else 0
        self.db.commit()
        self.db.refresh(row)
        return row

    def select_all(self, user_id: int, selected: int) -> int:
        """全选 / 全不选，返回受影响条数。"""
        rows = self.list_items(user_id)
        target = 1 if selected else 0
        for r in rows:
            r.selected = target
        self.db.commit()
        return len(rows)

    def summary(self, user_id: int) -> Dict[str, Any]:
        """购物车汇总（件数、选中件数、总价）。"""
        items = self.list_items(user_id)
        total_quantity = sum(i.quantity or 0 for i in items)
        selected = [i for i in items if i.selected == 1]
        total_amount = 0.0
        for i in selected:
            eq = self.db.get(Equipment, i.equipment_id)
            if eq is not None and eq.price is not None:
                total_amount += float(eq.price) * (i.quantity or 0)
        return {
            "totalQuantity": total_quantity,
            "selectedCount": len(selected),
            "totalAmount": round(total_amount, 2),
        }


class BrowseHistoryService:
    """浏览历史服务。"""

    def __init__(self, db: Session):
        self.db = db

    def list_history(self, user_id: int, limit: int = 50) -> List[BrowseHistory]:
        """浏览历史列表。"""
        return list(
            self.db.execute(
                select(BrowseHistory)
                .where(BrowseHistory.user_id == user_id, BrowseHistory.deleted == 0)
                .order_by(BrowseHistory.create_time.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )

    def remove_one(self, user_id: int, history_id: int) -> None:
        """删除单条浏览历史。"""
        row = self.db.get(BrowseHistory, history_id)
        if row is None or row.deleted == 1 or row.user_id != user_id:
            raise NotFoundException("浏览记录不存在")
        row.deleted = 1
        self.db.commit()

    def clear(self, user_id: int) -> int:
        """清空浏览历史，返回清除条数。"""
        rows = self.list_history(user_id, limit=1000)
        for r in rows:
            r.deleted = 1
        self.db.commit()
        return len(rows)
