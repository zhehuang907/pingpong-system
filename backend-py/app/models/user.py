"""用户领域 ORM 模型。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, LogicDeleteMixin, PK, TimestampMixin


class User(Base, TimestampMixin, LogicDeleteMixin):
    """用户表。"""

    __tablename__ = "user"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(50), nullable=False, unique=True)
    password: Mapped[str] = mapped_column(String(255), nullable=False)
    email: Mapped[str | None] = mapped_column(String(100), unique=True)
    phone: Mapped[str | None] = mapped_column(String(20))
    avatar: Mapped[str | None] = mapped_column(String(255))
    nickname: Mapped[str | None] = mapped_column(String(50))
    gender: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    level: Mapped[str] = mapped_column(String(20), default="初级", server_default="初级")
    play_style: Mapped[str | None] = mapped_column(String(50))
    role: Mapped[str] = mapped_column(String(20), default="USER", server_default="USER")
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
    last_login_time: Mapped[datetime | None] = mapped_column(DateTime)


class LoginAudit(Base, TimestampMixin, LogicDeleteMixin):
    """登录审计表。"""

    __tablename__ = "login_audit"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int | None] = mapped_column(BigInteger)
    username: Mapped[str | None] = mapped_column(String(50))
    success: Mapped[int | None] = mapped_column(Integer)
    ip_address: Mapped[str | None] = mapped_column(String(64))
    user_agent: Mapped[str | None] = mapped_column(String(255))
    device_fingerprint: Mapped[str | None] = mapped_column(String(128))
    device_name: Mapped[str | None] = mapped_column(String(128))
    risk_level: Mapped[str | None] = mapped_column(String(20))
    warning_message: Mapped[str | None] = mapped_column(String(255))


class UserPreferenceProfile(Base, TimestampMixin, LogicDeleteMixin):
    """用户偏好画像表（适配度问答结果）。"""

    __tablename__ = "user_preference_profile"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    level: Mapped[str | None] = mapped_column(String(20))
    play_style: Mapped[str | None] = mapped_column(String(50))
    grip_style: Mapped[str | None] = mapped_column(String(50))
    budget_min: Mapped[float | None] = mapped_column(Numeric(10, 2))
    budget_max: Mapped[float | None] = mapped_column(Numeric(10, 2))
    weekly_frequency: Mapped[str | None] = mapped_column(String(50))
    primary_goal: Mapped[str | None] = mapped_column(String(50))
    priority_focus: Mapped[str | None] = mapped_column(String(50))
    # 以下字段存 JSON 文本，与 Java 侧 String 保持一致
    preferred_categories: Mapped[str | None] = mapped_column(String(500))
    preferred_brands: Mapped[str | None] = mapped_column(String(500))
    answer_snapshot: Mapped[str | None] = mapped_column(String(2000))
    preference_tags: Mapped[str | None] = mapped_column(String(500))
    updated_source: Mapped[str | None] = mapped_column(String(50))
