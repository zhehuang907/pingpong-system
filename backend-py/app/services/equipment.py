"""装备 / 分类 / 标签服务。

由 Java ``EquipmentServiceImpl``、``CategoryServiceImpl``、``TagServiceImpl`` 移植。
浏览计数与收藏计数使用 SQL 层原子自增，避免「先查后改」导致的并发丢失。
"""

from __future__ import annotations

from decimal import Decimal
from typing import Any, Dict, List, Optional, Sequence

from sqlalchemy import and_, case, func, or_, select, text, update
from sqlalchemy.orm import Session

from app.core.exceptions import BusinessException, NotFoundException
from app.models.equipment import Category, Equipment, EquipmentTag, Tag
from app.models.interaction import BrowseHistory
from app.schemas.base import normalize_payload


class EquipmentService:
    """装备服务。"""

    def __init__(self, db: Session):
        self.db = db

    # ---------------- 查询 ----------------

    def get(self, equipment_id: int) -> Optional[Equipment]:
        """按 ID 查询（上架且未删）。"""
        row = self.db.get(Equipment, equipment_id)
        if row is None or row.deleted == 1:
            return None
        return row

    def list_equipments(
        self,
        category_id: Optional[int] = None,
        brand: Optional[str] = None,
        keyword: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        sort: str = "default",
        page: int = 1,
        size: int = 12,
    ):
        """装备分页筛选列表。

        Args:
            sort: default(综合) / price_asc / price_desc / hot / rating
        """
        stmt = select(Equipment).where(Equipment.status == 1, Equipment.deleted == 0)

        if category_id is not None:
            stmt = stmt.where(Equipment.category_id == category_id)
        if brand:
            stmt = stmt.where(Equipment.brand == brand)
        if keyword:
            like = f"%{keyword}%"
            stmt = stmt.where(
                or_(
                    Equipment.name.like(like),
                    Equipment.brand.like(like),
                    Equipment.model.like(like),
                    Equipment.description.like(like),
                )
            )
        if min_price is not None:
            stmt = stmt.where(Equipment.price >= min_price)
        if max_price is not None:
            stmt = stmt.where(Equipment.price <= max_price)

        # 排序
        if sort == "price_asc":
            stmt = stmt.order_by(Equipment.price.asc())
        elif sort == "price_desc":
            stmt = stmt.order_by(Equipment.price.desc())
        elif sort == "hot":
            stmt = stmt.order_by(Equipment.view_count.desc(), Equipment.sales_count.desc())
        elif sort == "rating":
            stmt = stmt.order_by(Equipment.rating.desc(), Equipment.rating_count.desc())
        else:
            stmt = stmt.order_by(Equipment.is_recommend.desc(), Equipment.create_time.desc())

        total = self.db.execute(
            select(func.count()).select_from(stmt.subquery())
        ).scalar() or 0

        rows = list(
            self.db.execute(stmt.offset((page - 1) * size).limit(size)).scalars().all()
        )
        return rows, total

    def search(self, keyword: str, limit: int = 20) -> List[Equipment]:
        """装备搜索（轻量，用于联想提示）。"""
        if not keyword:
            return []
        like = f"%{keyword}%"
        return list(
            self.db.execute(
                select(Equipment)
                .where(
                    Equipment.status == 1,
                    Equipment.deleted == 0,
                    or_(Equipment.name.like(like), Equipment.brand.like(like)),
                )
                .order_by(Equipment.view_count.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )

    def statistics(self) -> Dict[str, Any]:
        """装备统计概览（后台看板）。"""
        total = self.db.execute(
            select(func.count()).select_from(Equipment).where(Equipment.deleted == 0)
        ).scalar() or 0
        on_sale = self.db.execute(
            select(func.count())
            .select_from(Equipment)
            .where(Equipment.deleted == 0, Equipment.status == 1)
        ).scalar() or 0
        brand_count = self.db.execute(
            select(func.count(func.distinct(Equipment.brand))).where(Equipment.deleted == 0)
        ).scalar() or 0
        avg_price = self.db.execute(
            select(func.avg(Equipment.price)).where(Equipment.deleted == 0, Equipment.status == 1)
        ).scalar()
        return {
            "total": total,
            "onSale": on_sale,
            "brandCount": brand_count,
            "avgPrice": round(float(avg_price), 2) if avg_price is not None else 0.0,
        }

    # ---------------- 计数（原子操作） ----------------

    def incr_view_count(self, equipment_id: int) -> None:
        """原子递增浏览量。

        对应 Java ``UPDATE equipment SET view_count = view_count + 1``，
        避免并发下读改写丢失更新。
        """
        self.db.execute(
            update(Equipment)
            .where(Equipment.id == equipment_id, Equipment.deleted == 0)
            .values(view_count=func.coalesce(Equipment.view_count, 0) + 1)
        )
        self.db.commit()

    def incr_favorite_count(self, equipment_id: int, delta: int = 1) -> None:
        """原子递增/递减收藏数，保证不低于 0。

        对应 Java 的 ``GREATEST(0, favorite_count + delta)``。
        """
        current = func.coalesce(Equipment.favorite_count, 0) + delta
        self.db.execute(
            update(Equipment)
            .where(Equipment.id == equipment_id, Equipment.deleted == 0)
            # 用 CASE 而非 GREATEST：后者在 SQLite 不可用，会破坏测试环境
            .values(favorite_count=case((current < 0, 0), else_=current))
        )
        self.db.commit()

    # ---------------- 管理端写操作 ----------------

    def create(self, payload: Dict[str, Any]) -> Equipment:
        """新增装备。"""
        if not payload.get("name"):
            raise BusinessException("装备名称不能为空")
        row = Equipment(**normalize_payload(payload, Equipment))
        row.deleted = 0
        row.status = row.status if row.status is not None else 1
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row

    def update(self, equipment_id: int, payload: Dict[str, Any]) -> Equipment:
        """更新装备。"""
        row = self.db.get(Equipment, equipment_id)
        if row is None or row.deleted == 1:
            raise NotFoundException("装备不存在或已下架")
        for k, v in normalize_payload(payload, Equipment).items():
            if v is not None and k not in ("id", "deleted"):
                setattr(row, k, v)
        self.db.commit()
        self.db.refresh(row)
        return row

    def delete(self, equipment_id: int) -> None:
        """逻辑删除装备。"""
        row = self.db.get(Equipment, equipment_id)
        if row is None:
            raise NotFoundException("装备不存在或已下架")
        row.deleted = 1
        self.db.commit()

    # ---------------- 浏览历史写入 ----------------

    def record_browse(
        self,
        user_id: int,
        equipment_id: int,
        content_type: str = "detail",
        source_page: str = "",
        session_id: str = "",
        duration_seconds: int = 0,
        interaction_count: int = 0,
    ) -> BrowseHistory:
        """写入/更新浏览历史，返回记录实体。"""
        row = BrowseHistory(
            user_id=user_id,
            equipment_id=equipment_id,
            content_type=content_type,
            source_page=source_page,
            session_id=session_id,
            enter_time=None,
            leave_time=None,
            duration_seconds=duration_seconds,
            interaction_count=interaction_count,
            deleted=0,
        )
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row


class CategoryService:
    """分类服务。"""

    def __init__(self, db: Session):
        self.db = db

    def list_categories(self, parent_id: Optional[int] = None) -> List[Category]:
        """分类列表（默认返回启用分类）。"""
        stmt = select(Category).where(Category.deleted == 0, Category.status == 1)
        if parent_id is not None:
            stmt = stmt.where(Category.parent_id == parent_id)
        return list(self.db.execute(stmt.order_by(Category.sort.asc())).scalars().all())

    def get(self, category_id: int) -> Optional[Category]:
        """按 ID 查询。"""
        row = self.db.get(Category, category_id)
        return None if (row is None or row.deleted == 1) else row

    # ---------------- 管理端写操作 ----------------

    def create(self, payload: Dict[str, Any]) -> Category:
        """新增分类。"""
        if not payload.get("name"):
            raise BusinessException("分类名称不能为空")
        row = Category(**normalize_payload(payload, Category))
        row.deleted = 0
        row.status = row.status if row.status is not None else 1
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row

    def update(self, category_id: int, payload: Dict[str, Any]) -> Category:
        """更新分类。"""
        row = self.get(category_id)
        if row is None:
            raise NotFoundException("分类不存在")
        for k, v in normalize_payload(payload, Category).items():
            if v is not None and k not in ("id", "deleted"):
                setattr(row, k, v)
        self.db.commit()
        self.db.refresh(row)
        return row

    def delete(self, category_id: int) -> None:
        """逻辑删除分类。"""
        row = self.get(category_id)
        if row is None:
            raise NotFoundException("分类不存在")
        row.deleted = 1
        self.db.commit()


class TagService:
    """标签服务。"""

    def __init__(self, db: Session):
        self.db = db

    def list_tags(self, category: Optional[str] = None) -> List[Tag]:
        """标签列表。"""
        stmt = select(Tag).where(Tag.deleted == 0, Tag.status == 1)
        if category:
            stmt = stmt.where(Tag.category == category)
        return list(self.db.execute(stmt.order_by(Tag.sort.asc())).scalars().all())

    def get(self, tag_id: int) -> Optional[Tag]:
        """按 ID 查询。"""
        row = self.db.get(Tag, tag_id)
        return None if (row is None or row.deleted == 1) else row

    # ---------------- 管理端写操作 ----------------

    def create(self, payload: Dict[str, Any]) -> Tag:
        """新增标签。"""
        if not payload.get("name"):
            raise BusinessException("标签名称不能为空")
        row = Tag(**normalize_payload(payload, Tag))
        row.deleted = 0
        row.status = row.status if row.status is not None else 1
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row

    def update(self, tag_id: int, payload: Dict[str, Any]) -> Tag:
        """更新标签。"""
        row = self.get(tag_id)
        if row is None:
            raise NotFoundException("标签不存在")
        for k, v in normalize_payload(payload, Tag).items():
            if v is not None and k not in ("id", "deleted"):
                setattr(row, k, v)
        self.db.commit()
        self.db.refresh(row)
        return row

    def delete(self, tag_id: int) -> None:
        """逻辑删除标签。"""
        row = self.get(tag_id)
        if row is None:
            raise NotFoundException("标签不存在")
        row.deleted = 1
        self.db.commit()
