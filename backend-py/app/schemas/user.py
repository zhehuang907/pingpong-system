"""用户 / 认证 Schema。"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import Field

from app.schemas.base import CamelModel


class LoginIn(CamelModel):
    """登录入参。"""

    username: str
    password: str


class RegisterIn(CamelModel):
    """注册入参。"""

    username: str
    password: str
    email: Optional[str] = None
    phone: Optional[str] = None
    nickname: Optional[str] = None


class ChangePasswordIn(CamelModel):
    """修改密码入参。"""

    old_password: Optional[str] = None
    new_password: str


class UpdateProfileIn(CamelModel):
    """更新个人资料入参。"""

    nickname: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    avatar: Optional[str] = None
    gender: Optional[int] = None
    level: Optional[str] = None
    play_style: Optional[str] = None


class UserOut(CamelModel):
    """用户输出（绝不返回 password 字段）。"""

    id: int
    username: str
    email: Optional[str] = None
    phone: Optional[str] = None
    avatar: Optional[str] = None
    nickname: Optional[str] = None
    gender: Optional[int] = 0
    level: Optional[str] = None
    play_style: Optional[str] = None
    role: Optional[str] = "USER"
    status: Optional[int] = 1
    last_login_time: Optional[datetime] = None
    create_time: Optional[datetime] = None


class LoginOut(CamelModel):
    """登录响应。

    ``token`` 与 ``tokenType`` 供前端 ``Authorization: Bearer <token>`` 使用。
    """

    token: str
    token_type: str = "Bearer"
    expire_time: int
    user: UserOut


class UserPreferenceIn(CamelModel):
    """偏好画像入参（适配度问答结果）。"""

    level: Optional[str] = None
    play_style: Optional[str] = None
    grip_style: Optional[str] = None
    budget_min: Optional[float] = None
    budget_max: Optional[float] = None
    weekly_frequency: Optional[str] = None
    primary_goal: Optional[str] = None
    priority_focus: Optional[str] = None
    preferred_categories: Optional[str] = None
    preferred_brands: Optional[str] = None
    answer_snapshot: Optional[str] = None
    preference_tags: Optional[str] = None
    updated_source: Optional[str] = None


class UserPreferenceOut(UserPreferenceIn):
    """偏好画像输出。"""

    id: int
    user_id: int
    create_time: Optional[datetime] = None
    update_time: Optional[datetime] = None
