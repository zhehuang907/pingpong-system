"""系统配置 ORM 模型。"""

from __future__ import annotations

from sqlalchemy import BigInteger, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, LogicDeleteMixin, PK, TimestampMixin


class SystemConfig(Base, TimestampMixin, LogicDeleteMixin):
    """系统配置表。"""

    __tablename__ = "system_config"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    config_key: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    config_value: Mapped[str | None] = mapped_column(Text)
    config_type: Mapped[str | None] = mapped_column(String(50))
    config_name: Mapped[str | None] = mapped_column(String(100))
    description: Mapped[str | None] = mapped_column(String(255))
    status: Mapped[int] = mapped_column(Integer, default=1, server_default="1")
