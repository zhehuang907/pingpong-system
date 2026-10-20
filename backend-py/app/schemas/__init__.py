"""Pydantic Schema 包。

所有对外响应统一使用驼峰字段命名，与原 Java 后端契约一致。
"""

from app.schemas.base import CamelModel, to_camel
from app.schemas.equipment import CategoryOut, EquipmentOut, TagOut
from app.schemas.user import LoginOut, UserOut

__all__ = [
    "CamelModel",
    "to_camel",
    "EquipmentOut",
    "CategoryOut",
    "TagOut",
    "UserOut",
    "LoginOut",
]
