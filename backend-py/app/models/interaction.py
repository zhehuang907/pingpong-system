"""用户互动领域 ORM 模型（收藏 / 评论 / 浏览 / 购物车 / 推荐记录）。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Float, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, LogicDeleteMixin, PK, TimestampMixin


class Favorite(Base, TimestampMixin, LogicDeleteMixin):
    """收藏表。"""

    __tablename__ = "favorite"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


class Comment(Base, TimestampMixin, LogicDeleteMixin):
    """评论表。

    ``user`` / ``replies`` / ``liked_by_current_user`` 等为 Java 侧
    非数据库列字段，由服务层装配，不在此建模。
    """

    __tablename__ = "comment"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    parent_id: Mapped[int] = mapped_column(BigInteger, default=0, server_default="0")
    content: Mapped[str] = mapped_column(Text, nullable=False)
    images: Mapped[str | None] = mapped_column(Text)
    rating: Mapped[int] = mapped_column(Integer, default=5, server_default="5")
    like_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    reply_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    # 后台审核
    review_status: Mapped[str | None] = mapped_column(String(20))
    reviewed_by: Mapped[int | None] = mapped_column(BigInteger)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime)
    # 注意：Java 侧 equipmentName / reviewerName 为 @TableField(exist = false)
    # 的瞬态字段，真实表中不存在，故不建模为列，由服务层按需装配。
    review_remark: Mapped[str | None] = mapped_column(String(255))


class CommentLike(Base, TimestampMixin, LogicDeleteMixin):
    """评论点赞表。"""

    __tablename__ = "comment_like"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    comment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


class BrowseHistory(Base, TimestampMixin, LogicDeleteMixin):
    """浏览历史表（含停留时长与交互深度信号）。"""

    __tablename__ = "browse_history"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    content_type: Mapped[str | None] = mapped_column(String(64))
    source_page: Mapped[str | None] = mapped_column(String(128))
    session_id: Mapped[str | None] = mapped_column(String(64))
    enter_time: Mapped[datetime | None] = mapped_column(DateTime)
    leave_time: Mapped[datetime | None] = mapped_column(DateTime)
    duration_seconds: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    interaction_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    interaction_actions: Mapped[str | None] = mapped_column(Text)
    content_snapshot: Mapped[str | None] = mapped_column(String(255))
    # Java 侧 equipmentName 为 @TableField(exist = false) 瞬态字段，不建模为列


class CartItem(Base, TimestampMixin, LogicDeleteMixin):
    """购物车表（表名 shopping_cart）。"""

    __tablename__ = "shopping_cart"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    quantity: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    selected: Mapped[int] = mapped_column(Integer, default=1, server_default="1")


class RecommendRecord(Base, TimestampMixin, LogicDeleteMixin):
    """推荐记录表（用于效果追踪与算法迭代）。"""

    __tablename__ = "recommend_record"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    equipment_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    recommend_type: Mapped[str | None] = mapped_column(String(50))
    score: Mapped[float | None] = mapped_column(Float)
    recommend_reason: Mapped[str | None] = mapped_column(String(255))
    is_clicked: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    is_purchased: Mapped[int] = mapped_column(Integer, default=0, server_default="0")


class BehaviorLog(Base, TimestampMixin, LogicDeleteMixin):
    """用户行为日志表。"""

    __tablename__ = "behavior_log"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int | None] = mapped_column(BigInteger)
    equipment_id: Mapped[int | None] = mapped_column(BigInteger)
    username: Mapped[str | None] = mapped_column(String(50))
    behavior_type: Mapped[str | None] = mapped_column(String(50))
    weight: Mapped[int | None] = mapped_column(Integer)
    ip: Mapped[str | None] = mapped_column(String(50))
    url: Mapped[str | None] = mapped_column(String(255))
    method: Mapped[str | None] = mapped_column(String(20))
    module: Mapped[str | None] = mapped_column(String(50))
    operation: Mapped[str | None] = mapped_column(String(100))
    params: Mapped[str | None] = mapped_column(Text)
    result: Mapped[str | None] = mapped_column(Text)
    time_taken: Mapped[int | None] = mapped_column(BigInteger)
    user_agent: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime | None] = mapped_column(DateTime)
