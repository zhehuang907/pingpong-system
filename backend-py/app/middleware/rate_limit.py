"""接口限流中间件。

对应 Java 侧 ``RateLimitInterceptor`` 与配置：
- 普通接口：60 秒 120 次
- 敏感接口（登录/注册/改密/发送验证码）：60 秒 12 次

采用 Redis 固定窗口计数；Redis 不可用时自动放行（与 Java 侧容错一致）。
"""

from __future__ import annotations

import time
from typing import Set

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response

from app.core.config import settings
from app.core.exceptions import RateLimitException
from app.core.redis_client import get_value, incr

# 敏感端点（去除 /api 前缀后的路径片段）
SENSITIVE_PATHS: Set[str] = {
    "/user/login",
    "/user/register",
    "/user/forgot-password",
    "/user/change-password",
    "/user/update-password",
    "/captcha/send",
    "/captcha/verify",
    "/upload",
    "/ai/chat/send",
}


def _client_ip(request: Request) -> str:
    """获取客户端 IP，兼容反向代理。"""
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",")[0].strip()
    real = request.headers.get("x-real-ip")
    if real:
        return real.strip()
    return request.client.host if request.client else "unknown"


def _strip_prefix(path: str) -> str:
    """去除 API 前缀，还原为 Java 侧的相对路径。"""
    prefix = settings.api_prefix
    if prefix and path.startswith(prefix):
        return path[len(prefix) :] or "/"
    return path


def _is_sensitive(path: str) -> bool:
    """判定是否为敏感端点。"""
    rel = _strip_prefix(path)
    if rel in SENSITIVE_PATHS:
        return True
    return any(rel.startswith(p) for p in SENSITIVE_PATHS)


class RateLimitMiddleware(BaseHTTPMiddleware):
    """基于 Redis 固定窗口的限流中间件。"""

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        if not settings.rate_limit_enabled:
            return await call_next(request)

        # 静态资源与预检请求不限流
        if request.method == "OPTIONS":
            return await call_next(request)

        path = request.url.path
        if path.startswith("/api/upload/") or path.startswith("/uploads/"):
            return await call_next(request)

        sensitive = _is_sensitive(path)
        if sensitive:
            window = settings.rate_limit_sensitive_window_seconds
            limit = settings.rate_limit_sensitive_max_requests
            scope = "sens"
        else:
            window = settings.rate_limit_window_seconds
            limit = settings.rate_limit_max_requests
            scope = "norm"

        ip = _client_ip(request)
        bucket = int(time.time()) // window
        key = f"ratelimit:{scope}:{ip}:{bucket}"

        count = incr(key, ttl=window)
        if count > limit:
            raise RateLimitException()

        response = await call_next(request)
        # 回填限流剩余量，便于前端提示
        remaining = max(0, limit - count)
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Window"] = str(window)
        return response


def get_rate_limit_state(ip: str, sensitive: bool = False) -> dict:
    """查询当前限流状态（调试/测试用）。"""
    if sensitive:
        window = settings.rate_limit_sensitive_window_seconds
        limit = settings.rate_limit_sensitive_max_requests
        scope = "sens"
    else:
        window = settings.rate_limit_window_seconds
        limit = settings.rate_limit_max_requests
        scope = "norm"
    bucket = int(time.time()) // window
    key = f"ratelimit:{scope}:{ip}:{bucket}"
    raw = get_value(key)
    used = int(raw) if raw else 0
    return {"limit": limit, "used": used, "remaining": max(0, limit - used), "window": window}
