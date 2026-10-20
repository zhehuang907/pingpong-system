"""异常体系与全局处理器。

对应 Java 侧 ``BusinessException`` 与 ``GlobalExceptionHandler``。
业务异常统一返回 ``{code: 500, msg: <业务错误信息>}``，
保证前端错误处理逻辑无需改动。
"""

from __future__ import annotations

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.result import ResultJSONResponse, fail


class BusinessException(Exception):
    """业务异常。

    Attributes:
        msg: 面向用户的错误提示。
        code: 业务状态码，默认 500（与 Java 侧一致）。
    """

    def __init__(self, msg: str = "操作失败", code: int = 500):
        self.msg = msg
        self.code = code
        super().__init__(msg)


class AuthException(BusinessException):
    """认证失败。"""

    def __init__(self, msg: str = "未登录或登录已过期"):
        super().__init__(msg=msg, code=401)


class ForbiddenException(BusinessException):
    """权限不足。"""

    def __init__(self, msg: str = "无权访问该资源"):
        super().__init__(msg=msg, code=403)


class NotFoundException(BusinessException):
    """资源不存在。"""

    def __init__(self, msg: str = "资源不存在"):
        super().__init__(msg=msg, code=404)


class RateLimitException(BusinessException):
    """触发限流。"""

    def __init__(self, msg: str = "请求过于频繁，请稍后再试"):
        super().__init__(msg=msg, code=429)


def register_exception_handlers(app: FastAPI) -> None:
    """注册全局异常处理器。"""

    @app.exception_handler(BusinessException)
    async def _business_handler(request: Request, exc: BusinessException) -> JSONResponse:
        return ResultJSONResponse(
            status_code=200,  # 业务失败仍返回 200，由 code 字段区分（对齐 Java）
            content=fail(msg=exc.msg, code=exc.code),
        )

    @app.exception_handler(StarletteHTTPException)
    async def _http_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        return ResultJSONResponse(
            status_code=exc.status_code,
            content=fail(msg=str(exc.detail), code=exc.status_code),
        )

    @app.exception_handler(RequestValidationError)
    async def _validation_handler(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        # 提取首个可读错误，避免暴露完整内部结构
        errors = exc.errors()
        first = errors[0] if errors else {}
        loc = ".".join(str(x) for x in first.get("loc", []) if x != "body")
        msg = first.get("msg", "参数校验失败")
        return ResultJSONResponse(
            status_code=200,
            content=fail(msg=f"参数错误：{loc} {msg}".strip(), code=400),
        )

    @app.exception_handler(Exception)
    async def _unhandled_handler(request: Request, exc: Exception) -> JSONResponse:
        # 生产环境应记录日志；此处仅返回通用提示，避免泄漏堆栈
        return ResultJSONResponse(
            status_code=200,
            content=fail(msg="服务器内部错误", code=500),
        )
