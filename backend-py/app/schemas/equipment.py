"""装备 / 分类 / 标签 Schema。"""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional

from pydantic import Field

from app.schemas.base import CamelModel


class CategoryOut(CamelModel):
    """分类输出。"""

    id: int
    name: str
    parent_id: Optional[int] = 0
    level: Optional[int] = 1
    icon: Optional[str] = None
    sort: Optional[int] = 0
    status: Optional[int] = 1
    create_time: Optional[datetime] = None


class TagOut(CamelModel):
    """标签输出。"""

    id: int
    name: str
    category: Optional[str] = None
    color: Optional[str] = None
    sort: Optional[int] = 0
    status: Optional[int] = 1
    create_time: Optional[datetime] = None


class EquipmentOut(CamelModel):
    """装备输出。

    字段命名与 Java ``Equipment`` 实体一致；
    ``recommend_channel`` / ``recommend_reason`` / ``recommend_tags``
    为推荐服务运行时填充的非数据库字段。
    """

    id: int
    name: str
    category_id: Optional[int] = None
    brand: Optional[str] = None
    model: Optional[str] = None
    price: Optional[float] = None
    original_price: Optional[float] = None
    stock: Optional[int] = 0
    description: Optional[str] = None
    parameters: Optional[str] = None
    images: Optional[str] = None
    main_image: Optional[str] = None
    image_url: Optional[str] = None
    video_url: Optional[str] = None
    suitable_for: Optional[str] = None
    features: Optional[str] = None
    rating: Optional[float] = 0
    rating_count: Optional[int] = 0
    view_count: Optional[int] = 0
    favorite_count: Optional[int] = 0
    sales_count: Optional[int] = 0
    tags: Optional[str] = None
    status: Optional[int] = 1
    is_recommend: Optional[int] = 0
    create_time: Optional[datetime] = None
    update_time: Optional[datetime] = None

    # 推荐附加字段（非数据库列）
    recommend_channel: Optional[str] = None
    recommend_reason: Optional[str] = None
    recommend_tags: Optional[List[str]] = None
    similarity: Optional[float] = None


class EquipmentListItem(CamelModel):
    """装备列表项（轻量，减少传输量）。"""

    id: int
    name: str
    category_id: Optional[int] = None
    brand: Optional[str] = None
    price: Optional[float] = None
    main_image: Optional[str] = None
    image_url: Optional[str] = None
    rating: Optional[float] = 0
    view_count: Optional[int] = 0
    favorite_count: Optional[int] = 0
    sales_count: Optional[int] = 0
    status: Optional[int] = 1


class PageOut(CamelModel):
    """分页包装，对齐 MyBatis-Plus Page 的字段命名。"""

    records: List[object] = Field(default_factory=list)
    total: int = 0
    size: int = 10
    current: int = 1
    pages: int = 0
