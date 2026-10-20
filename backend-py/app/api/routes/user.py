"""用户资料与偏好路由。

对齐 Java ``UserController`` 的 ``/user/info``（GET/PUT）等受保护端点。
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, File, UploadFile
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_current_user_id
from app.core.result import ok
from app.db.session import get_db
from app.models.user import User
from app.schemas.user import (
    ChangePasswordIn,
    UpdateProfileIn,
    UserOut,
    UserPreferenceIn,
    UserPreferenceOut,
)
from app.services.storage import ImageStorageService
from app.services.user import UserService

router = APIRouter(prefix="/user", tags=["用户"])


@router.get("/info")
def get_info(current_user: User = Depends(get_current_user)):
    """当前登录用户信息。"""
    return ok(UserOut.model_validate(current_user).model_dump(by_alias=True))


@router.put("/info")
def update_info(
    payload: UpdateProfileIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """更新个人资料。"""
    user = UserService(db).update_profile(current_user.id, payload)
    return ok(UserOut.model_validate(user).model_dump(by_alias=True), "资料已更新")


@router.put("/password")
@router.post("/password")
def change_password(
    payload: ChangePasswordIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """修改密码（需校验原密码）。

    Java 侧为 ``PUT /user/password``；此处同时接受 POST，兼容两种调用。
    """
    UserService(db).change_password(current_user.id, payload, require_old=True)
    return ok(True, "密码已修改，请重新登录")


@router.post("/avatar")
def upload_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """上传头像并返回最新用户信息（对齐 Java ``POST /user/avatar``）。"""
    url = ImageStorageService().save(file, subdir="avatar")
    user = UserService(db).update_profile(
        current_user.id, UpdateProfileIn(avatar=url)
    )
    return ok(UserOut.model_validate(user).model_dump(by_alias=True), "头像已更新")


@router.get("/preferences")
@router.get("/preference")
def get_preference(
    user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """读取偏好画像。

    Java 侧为 ``GET /user/preferences``；保留 ``/preference`` 单数形式向后兼容。
    """
    profile = UserService(db).get_preference(user_id)
    if profile is None:
        return ok(None)
    return ok(UserPreferenceOut.model_validate(profile).model_dump(by_alias=True))


@router.put("/preferences")
@router.post("/preference")
def save_preference(
    payload: UserPreferenceIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """保存偏好画像（适配度问答结果），保存后立即影响推荐排序。"""
    profile = UserService(db).save_preference(user_id, payload)
    return ok(
        UserPreferenceOut.model_validate(profile).model_dump(by_alias=True), "偏好已保存"
    )
