"""JWT 认证中间件。

对应 Java 侧 ``JwtInterceptor``：
- OPTIONS 预检直接放行
- 无 token 时放行（游客可访问公开接口，受保护接口由路由依赖拦截）
- 有 token 时校验签名 + 有效期，并与 Redis 中的 token 比对（单端登录互斥）
- 校验通过后自动续期（滑动过期）
"""

from __future__ import annotations

from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response

from app.core.config import settings
from app.core.redis_client import (
    get_user_token,
    is_available,
    refresh_user_token,
)
from app.core.security import get_user_id_from_token, resolve_token
from app.api.deps import CURRENT_USER_ID_ATTR, CURRENT_USERNAME_ATTR

# 与 WebConfig.addInterceptors 的 excludePathPatterns 完全一致
PUBLIC_PATH_PATTERNS: tuple[str, ...] = (
    "/user/login",
    "/user/register",
    "/user/forgot-password",
    "/user/check-username",
    "/user/check-email",
    "/equipment/list",
    "/equipment/detail",
    "/equipment/search",
    "/equipment/statistics",
    "/category/",
    "/tag/",
    "/comment/equipment/",
    "/comment/reply/",
    "/ws",
    "/captcha/",
    "/upload/",
    "/health",
    "/docs",
    "/openapi.json",
    "/redoc",
)


def _strip_prefix(path: str) -> str:
    """去除 /api 前缀。"""
    prefix = settings.api_prefix
    if prefix and path.startswith(prefix):
        return path[len(prefix) :] or "/"
    return path


def is_public_path(path: str) -> bool:
    """判定是否为免鉴权路径。"""
    rel = _strip_prefix(path)
    return any(rel.startswith(p) for p in PUBLIC_PATH_PATTERNS)


class JwtAuthMiddleware(BaseHTTPMiddleware):
    """解析并校验 JWT，将用户身份写入 request.state。"""

    async def dispatch(
        self, request: Request, call_next: RequestResponseEndpoint
    ) -> Response:
        if request.method == "OPTIONS":
            return await call_next(request)

        request.state.current_user_id = None
        request.state.current_username = None

        auth = request.headers.get("authorization") or request.headers.get("Authorization")
        token = resolve_token(auth)
        if not token:
            return await call_next(request)

        user_id = get_user_id_from_token(token)
        if user_id is None:
            # token 无效：不在此处强拒，交由路由依赖决定是否必须登录，
            # 以兼容「游客可看公开接口」的设计
            return await call_next(request)

        # Redis 双校验：若服务端存有 token 则必须一致（单端登录互斥）
        if is_available():
            stored = get_user_token(user_id)
            if stored is not None and stored != token:
                return await call_next(request)
            refresh_user_token(user_id, token, settings.jwt_expire_time)

        request.state.current_user_id = user_id
        return await call_next(request)
