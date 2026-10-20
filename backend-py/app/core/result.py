"""统一响应封装。

对应 Java 侧的 ``com.pingpong.common.Result<T>``，
保证前端无需任何改动即可对接 Python 后端。
"""

from __future__ import annotations

import datetime
import decimal
import json
from typing import Any, Generic, Optional, TypeVar

from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

T = TypeVar("T")


class Result(BaseModel, Generic[T]):
    """统一响应体：{code, msg, data}。

    与 Java 侧字段命名与取值完全一致：
    - code 200 表示成功，500 表示业务失败
    - msg 默认「操作成功」/「操作失败」
    """

    code: int = Field(default=200, description="状态码，200 成功 / 500 失败")
    msg: str = Field(default="操作成功", description="提示信息")
    data: Optional[T] = Field(default=None, description="业务数据")

    @classmethod
    def success(cls, data: Any = None, msg: str = "操作成功") -> "Result":
        return cls(code=200, msg=msg, data=data)

    @classmethod
    def error(cls, msg: str = "操作失败", code: int = 500) -> "Result":
        return cls(code=code, msg=msg, data=None)


def ok(data: Any = None, msg: str = "操作成功") -> dict:
    """构造成功响应字典。"""
    return {"code": 200, "msg": msg, "data": data}


def fail(msg: str = "操作失败", code: int = 500) -> dict:
    """构造失败响应字典。"""
    return {"code": code, "msg": msg, "data": None}


class ResultJSONResponse(JSONResponse):
    """自定义 JSON 响应。

    将 datetime 序列化为 ``yyyy-MM-dd HH:mm:ss``，
    对齐 Java Jackson 的 ``spring.jackson.date-format`` 配置，
    避免前端拿到 ISO8601（含 T）格式产生解析差异。
    """

    def render(self, content: Any) -> bytes:
        def _default(o: Any) -> Any:
            if isinstance(o, (datetime.datetime, datetime.date)):
                return o.strftime("%Y-%m-%d %H:%M:%S")
            if isinstance(o, decimal.Decimal):
                return float(o)
            if isinstance(o, bytes):
                return o.decode("utf-8", errors="ignore")
            return str(o)

        return json.dumps(
            content,
            ensure_ascii=False,
            allow_nan=False,
            indent=None,
            separators=(",", ":"),
            default=_default,
        ).encode("utf-8")
