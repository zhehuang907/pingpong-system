"""FastAPI 依赖注入。

对应 Spring 的 ``@Autowired`` 与拦截器注入的当前用户上下文
（Java 侧用 ``BaseContext`` + ``request.setAttribute``）。
这里统一收敛为类型安全的依赖函数。
"""

from __future__ import annotations

from typing import Optional

from fastapi import Depends, Request
from sqlalchemy.orm import Session

from app.core.exceptions import AuthException, ForbiddenException
from app.core.security import get_user_id_from_token, resolve_token
from app.db.session import get_db
from app.models.user import User

# 当前登录用户在 request.state 中的属性名
CURRENT_USER_ID_ATTR = "current_user_id"
CURRENT_USERNAME_ATTR = "current_username"
CURRENT_USER_ATTR = "current_user"


def get_db_session() -> Session:
    """数据库会话依赖（重导出，便于路由统一引用）。"""
    return Depends(get_db)


def _extract_user_id(request: Request) -> Optional[int]:
    """从请求头解析当前用户 ID，失败返回 None。"""
    auth = request.headers.get("authorization") or request.headers.get("Authorization")
    token = resolve_token(auth)
    if not token:
        return None
    return get_user_id_from_token(token)


def get_optional_user_id(request: Request) -> Optional[int]:
    """可选登录：未登录返回 None，用于公开接口也能个性化的场景。"""
    uid = getattr(request.state, CURRENT_USER_ID_ATTR, None)
    if uid is not None:
        return uid
    return _extract_user_id(request)


def get_current_user_id(request: Request) -> int:
    """必须登录：未登录或 token 无效时抛 401。"""
    uid = getattr(request.state, CURRENT_USER_ID_ATTR, None)
    if uid is None:
        uid = _extract_user_id(request)
    if uid is None:
        raise AuthException("未登录或登录已过期")
    return uid


def get_current_user(
    request: Request, db: Session = Depends(get_db)
) -> User:
    """获取当前登录用户实体。"""
    uid = get_current_user_id(request)
    user = db.get(User, uid)
    if user is None or user.deleted == 1:
        raise AuthException("用户不存在或已注销")
    if user.status == 0:
        raise AuthException("账号已被禁用")
    return user


def require_admin(
    request: Request, db: Session = Depends(get_db)
) -> User:
    """管理员权限校验，对应 Java 侧 AdminAccessHelper。"""
    user = get_current_user(request, db)
    if (user.role or "").upper() != "ADMIN":
        raise ForbiddenException("需要管理员权限")
    return user
