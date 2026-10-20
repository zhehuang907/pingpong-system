"""认证路由。

对齐 Java ``UserController`` 的公开端点：
``/user/register``、``/user/login``、``/user/logout``、
``/user/check-username``、``/user/check-email``、``/user/forgot-password``
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id
from app.core.result import ok
from app.core.security import generate_token
from app.db.session import get_db
from app.schemas.user import LoginIn, LoginOut, RegisterIn, UserOut
from app.services.user import UserService

router = APIRouter(prefix="/user", tags=["认证"])


@router.post("/register")
def register(payload: RegisterIn, db: Session = Depends(get_db)):
    """用户注册。"""
    service = UserService(db)
    user = service.register(payload)
    return ok(UserOut.model_validate(user).model_dump(by_alias=True), "注册成功")


@router.post("/login")
def login(payload: LoginIn, request: Request, db: Session = Depends(get_db)):
    """用户登录，返回 JWT。"""
    service = UserService(db)
    ip = request.client.host if request.client else ""
    ua = request.headers.get("user-agent", "")
    token, expire, user = service.login(payload, ip=ip, user_agent=ua)

    data = LoginOut(
        token=token,
        token_type="Bearer",
        expire_time=expire,
        user=UserOut.model_validate(user),
    )
    return ok(data.model_dump(by_alias=True), "登录成功")


@router.post("/logout")
def logout(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """登出：服务端 token 立即失效。"""
    UserService(db).logout(user_id)
    return ok(True, "已退出登录")


@router.get("/check-username")
def check_username(username: str, db: Session = Depends(get_db)):
    """用户名是否已占用。"""
    return ok(UserService(db).exists_username(username))


@router.get("/check-email")
def check_email(email: str, db: Session = Depends(get_db)):
    """邮箱是否已占用。"""
    return ok(UserService(db).exists_email(email))


@router.post("/forgot-password")
def forgot_password(payload: dict, db: Session = Depends(get_db)):
    """找回密码：校验用户名与邮箱后重置。

    注意：此处为教学/演示实现，生产环境应发送邮件验证码后再允许重置。
    """
    service = UserService(db)
    username = (payload or {}).get("username", "")
    email = (payload or {}).get("email", "")
    new_password = (payload or {}).get("newPassword", "")

    user = service.get_by_username(username)
    if user is None or (email and user.email != email):
        # 不泄漏账号是否存在，统一返回成功
        return ok(True, "若账号匹配，重置链接已发送")

    from app.schemas.user import ChangePasswordIn

    service.change_password(
        user.id, ChangePasswordIn(new_password=new_password), require_old=False
    )
    return ok(True, "密码已重置")


@router.post("/refresh")
def refresh_token(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """刷新 token。"""
    user = UserService(db).get_by_id(user_id)
    if user is None:
        return ok(None, "用户不存在")
    token = generate_token(user.id, user.username)
    from app.core.redis_client import bind_user_token

    bind_user_token(user.id, token)
    return ok({"token": token, "tokenType": "Bearer", "expireTime": 86400})
