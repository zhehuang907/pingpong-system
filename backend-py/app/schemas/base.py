"""Pydantic Schema 基类。

**关键契约**：Java 侧由 Jackson 序列化 Java 字段，默认产出驼峰 JSON
（``categoryId`` / ``viewCount`` / ``isRecommend``）。
Python 侧字段为 snake_case，若直接输出会导致前端全线报错。
故此处统一配置驼峰别名，确保所有响应字段命名与 Java 侧完全一致。
"""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict


def to_camel(snake: str) -> str:
    """snake_case → camelCase。

    Examples:
        >>> to_camel("category_id")
        'categoryId'
        >>> to_camel("is_recommend")
        'isRecommend'
        >>> to_camel("id")
        'id'
    """
    parts = snake.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


def _snake_regex_sub(name: str) -> str:
    """camelCase → snake_case（处理连续大写缩写如 ``imageURL``）。"""
    import re

    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def normalize_payload(payload: dict, model: type) -> dict:
    """把前端传入的驼峰（或混合）键名归一化为模型字段名。

    前端 JSON 一律驼峰（``categoryId`` / ``originalPrice``），而 ORM 模型为
    snake_case。若不做转换，``Equipment(**payload)`` 会把所有多词字段静默
    丢弃（过滤器以 ``hasattr`` 判定），造成「新增成功但字段为空」的隐蔽缺陷。

    同时过滤掉模型中不存在的键，避免非法字段直接打进构造函数。

    ``{"categoryId": 1, "foo": 2}`` → ``{"category_id": 1}``
    """
    if not payload:
        return {}
    columns = {c.name for c in getattr(model, "__table__").columns}
    result: dict = {}
    for key, value in payload.items():
        name = key if key in columns else _snake_regex_sub(key)
        if name in columns:
            result[name] = value
    return result


class CamelModel(BaseModel):
    """输出驼峰、同时允许用 snake_case 赋值的模型基类。"""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
        # 允许 ORM 附加字段（recommendReason 等非列字段）
        arbitrary_types_allowed=True,
    )


class ResultModel(CamelModel):
    """统一响应体模型，仅用于文档展示。"""

    code: int = 200
    msg: str = "操作成功"
    data: object | None = None
