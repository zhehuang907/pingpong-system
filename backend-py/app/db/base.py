"""SQLAlchemy 声明式基类。

提供公共字段（主键 id、create_time、update_time、deleted）
与逻辑删除查询的通用能力，对应 MyBatis-Plus 的
``id-type: auto`` 与 ``logic-delete-field: deleted`` 全局配置。
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy import BigInteger, Boolean, DateTime, Integer, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

# 主键类型：MySQL 用 BIGINT，SQLite 降级为 INTEGER。
# 原因：SQLite 只有 "INTEGER PRIMARY KEY" 才具备自增（rowid 别名）语义，
# 直接使用 BIGINT 主键会导致插入时无法自动生成 ID，测试环境将失败。
PK = BigInteger().with_variant(Integer, "sqlite")


class Base(DeclarativeBase):
    """所有 ORM 模型的基类。"""

    # 统一将 datetime 映射为 MySQL DATETIME（不带时区）
    type_annotation_map = {
        datetime: DateTime,
    }


class TimestampMixin:
    """创建/更新时间自动填充。

    对应 MyBatis-Plus 的 ``FieldFill.INSERT`` / ``FieldFill.INSERT_UPDATE``。
    """

    create_time: Mapped[datetime] = mapped_column(
        DateTime,
        nullable=False,
        server_default=func.now(),
        comment="创建时间",
    )
    update_time: Mapped[datetime] = mapped_column(
        DateTime,
        nullable=False,
        server_default=func.now(),
        server_onupdate=func.now(),
        comment="更新时间",
    )


class LogicDeleteMixin:
    """逻辑删除标记。

    对应 MyBatis-Plus ``logic-delete-value: 1 / logic-not-delete-value: 0``。
    所有查询默认追加 ``deleted = 0`` 条件。
    """

    deleted: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
        server_default="0",
        comment="逻辑删除：0=未删，1=已删",
    )


def model_to_dict(obj: Any, exclude: set[str] | None = None) -> dict:
    """将 ORM 对象转为字典，供序列化使用。

    Args:
        obj: SQLAlchemy 模型实例。
        exclude: 需要排除的字段名集合。

    Returns:
        字段名 → 值的映射（遵循 ``map-underscore-to-camel-case`` 的反向约定，
        此处保留 snake_case，由 Schema 层负责转驼峰）。
    """
    exclude = exclude or set()
    return {
        c.name: getattr(obj, c.name)
        for c in obj.__table__.columns
        if c.name not in exclude
    }
