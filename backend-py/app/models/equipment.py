"""装备领域 ORM 模型。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, LogicDeleteMixin, PK, TimestampMixin


class Category(Base, TimestampMixin, LogicDeleteMixin):
    """装备分类表。"""

    __tablename__ = "category"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    parent_id: Mapped[int] = mapped_column(BigInteger, default=0, server_default="0")
    level: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    icon: Mapped[str | None] = mapped_column(String(255))
    sort: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")


class Tag(Base, TimestampMixin, LogicDeleteMixin):
    """标签表。"""

    __tablename__ = "tag"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    category: Mapped[str | None] = mapped_column(String(50))
    color: Mapped[str] = mapped_column(String(20), default="#409EFF", server_default="#409EFF")
    sort: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")


class Equipment(Base, TimestampMixin, LogicDeleteMixin):
    """装备表。

    注意：``recommend_channel`` / ``recommend_reason`` / ``recommend_tags``
    在 Java 侧为 ``@TableField(exist = false)``（非数据库列），
    由推荐服务在运行时填充，故此处不建模为列。
    """

    __tablename__ = "equipment"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    category_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    brand: Mapped[str | None] = mapped_column(String(50))
    model: Mapped[str | None] = mapped_column(String(100))
    price: Mapped[float | None] = mapped_column(Numeric(10, 2))
    original_price: Mapped[float | None] = mapped_column(Numeric(10, 2))
    stock: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    description: Mapped[str | None] = mapped_column(Text)
    # JSON 文本：与 Java 侧 String 保持一致，避免前端解析歧义
    parameters: Mapped[str | None] = mapped_column(Text)
    images: Mapped[str | None] = mapped_column(Text)
    main_image: Mapped[str | None] = mapped_column(String(255))
    image_url: Mapped[str | None] = mapped_column(String(255))
    video_url: Mapped[str | None] = mapped_column(String(255))
    suitable_for: Mapped[str | None] = mapped_column(String(255))
    features: Mapped[str | None] = mapped_column(Text)
    rating: Mapped[float] = mapped_column(Numeric(3, 2), default=0, server_default="0")
    rating_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    view_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    favorite_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    sales_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    tags: Mapped[str | None] = mapped_column(Text)
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    is_recommend: Mapped[int] = mapped_column(Integer, default=0, server_default="0")


class EquipmentTag(Base, TimestampMixin, LogicDeleteMixin):
    """装备-标签关联表。"""

    __tablename__ = "equipment_tag"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    tag_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


class EquipmentCombination(Base):
    """装备组合表（底板 + 正反手胶皮）。"""

    __tablename__ = "equipment_combination"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int | None] = mapped_column(BigInteger)
    name: Mapped[str | None] = mapped_column(String(100))
    blade_id: Mapped[int | None] = mapped_column(BigInteger)
    forehand_rubber_id: Mapped[int | None] = mapped_column(BigInteger)
    backhand_rubber_id: Mapped[int | None] = mapped_column(BigInteger)
    compatibility_score: Mapped[float | None] = mapped_column(Numeric(5, 2))
    description: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime | None] = mapped_column(DateTime)
    updated_at: Mapped[datetime | None] = mapped_column(DateTime)


class StarPlayer(Base, TimestampMixin, LogicDeleteMixin):
    """球星表。"""

    __tablename__ = "star_player"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    name: Mapped[str | None] = mapped_column(String(50))
    country: Mapped[str | None] = mapped_column(String(50))
    gender: Mapped[int | None] = mapped_column(Integer)
    level: Mapped[str | None] = mapped_column(String(50))
    play_style: Mapped[str | None] = mapped_column(String(50))
    ranking: Mapped[int | None] = mapped_column(Integer)
    achievements: Mapped[str | None] = mapped_column(Text)
    avatar: Mapped[str | None] = mapped_column(String(255))
    banner_image: Mapped[str | None] = mapped_column(String(255))
    intro: Mapped[str | None] = mapped_column(Text)
    highlights: Mapped[str | None] = mapped_column(Text)
    featured: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")


class PlayerEquipmentRelation(Base):
    """球星-装备关联表。"""

    __tablename__ = "player_equipment_relation"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    player_id: Mapped[int | None] = mapped_column(BigInteger)
    equipment_id: Mapped[int | None] = mapped_column(BigInteger)
    type: Mapped[str | None] = mapped_column(String(50))
    is_current: Mapped[int | None] = mapped_column(Integer)
