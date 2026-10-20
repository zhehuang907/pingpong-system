"""后台管理、文件上传与验证码路由。

对齐 Java ``AdminDashboardController``、``FileUploadController``、``CaptchaController``。
"""

from __future__ import annotations

import io
import random
import string

from fastapi import APIRouter, Depends, File, Query, UploadFile
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id, require_admin
from app.core.config import settings
from app.core.result import ok
from app.db.session import get_db
from app.models.user import User
from app.schemas.user import UpdateProfileIn
from app.services.admin import AdminService
from app.services.equipment import EquipmentService
from app.services.storage import ImageStorageService
from app.services.user import UserService

admin_router = APIRouter(prefix="/admin/dashboard", tags=["后台管理"])
upload_router = APIRouter(prefix="/upload", tags=["文件上传"])
captcha_router = APIRouter(prefix="/captcha", tags=["验证码"])


# ============================================================
# 后台概览
# ============================================================


@admin_router.get("/overview")
def overview(_: User = Depends(require_admin), db: Session = Depends(get_db)):
    """后台数据概览。"""
    return ok(AdminService(db).overview())


@admin_router.get("/category-distribution")
def category_distribution(_: User = Depends(require_admin), db: Session = Depends(get_db)):
    """装备分类分布。"""
    return ok(AdminService(db).category_distribution())


@admin_router.get("/top-equipments")
def top_equipments(
    limit: int = Query(default=10, le=50),
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    """热门装备 Top N。"""
    return ok(AdminService(db).top_equipments(limit))


@admin_router.get("/daily-trend")
def daily_trend(
    days: int = Query(default=7, le=30), _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """近 N 天浏览趋势。"""
    return ok(AdminService(db).daily_trend(days))


@admin_router.get("/export")
def export(_: User = Depends(require_admin), db: Session = Depends(get_db)):
    """导出装备数据（CSV）。"""
    rows, _ = EquipmentService(db).list_equipments(page=1, size=1000)
    lines = ["id,name,brand,price,stock,viewCount,favoriteCount"]
    for r in rows:
        lines.append(
            f"{r.id},{r.name},{r.brand or ''},{r.price or 0},{r.stock or 0},"
            f"{r.view_count or 0},{r.favorite_count or 0}"
        )
    content = "\n".join(lines).encode("utf-8-sig")
    return StreamingResponse(
        io.BytesIO(content),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": "attachment; filename=equipments.csv"},
    )


# ============================================================
# 评论审核
# ============================================================


class ReviewIn(BaseModel):
    """审核入参。"""

    commentId: int
    approved: bool
    remark: str = ""


@admin_router.get("/comments/pending")
def pending_comments(
    limit: int = Query(default=50, le=200), _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """待审核评论。"""
    return ok(AdminService(db).list_pending_comments(limit))


@admin_router.post("/comments/review")
def review_comment(
    payload: ReviewIn, admin: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """审核评论。"""
    result = AdminService(db).review_comment(
        payload.commentId, admin.id, payload.approved, payload.remark
    )
    return ok(result, "审核完成")


# ============================================================
# 文件上传
# ============================================================


@upload_router.post("")
def upload_file(
    file: UploadFile = File(...),
    subdir: str = Query(default="common"),
    user_id: int = Depends(get_current_user_id),
):
    """通用文件上传，对齐 Java ``POST /upload``。"""
    url = ImageStorageService().save(file, subdir=subdir)
    return ok({"url": url, "fileName": file.filename}, "上传成功")


@upload_router.post("/equipment-image")
def upload_equipment_image(
    file: UploadFile = File(...), user_id: int = Depends(get_current_user_id)
):
    """上传装备图片。"""
    url = ImageStorageService().save(file, subdir="equipment")
    return ok({"url": url, "fileName": file.filename}, "上传成功")


@upload_router.post("/avatar")
def upload_avatar(
    file: UploadFile = File(...), user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """上传用户头像并更新资料。"""
    url = ImageStorageService().save(file, subdir="avatar")
    user = UserService(db).update_profile(user_id, UpdateProfileIn(avatar=url))
    return ok({"url": url, "avatar": user.avatar}, "头像已更新")


@upload_router.post("/image")
def upload_image(
    file: UploadFile = File(...),
    subdir: str = Query(default="equipment"),
    user_id: int = Depends(get_current_user_id),
):
    """通用图片上传。"""
    url = ImageStorageService().save(file, subdir=subdir)
    return ok({"url": url}, "上传成功")


# ============================================================
# 验证码（演示实现）
# ============================================================

_CAPTCHA_STORE: dict[str, str] = {}


def _render_captcha_svg(code: str) -> bytes:
    """将验证码渲染为 SVG 图片（无第三方图像库依赖，浏览器可直接 ``<img>`` 引用）。"""
    rng = random.Random(code)
    # 干扰线
    lines = "".join(
        f'<line x1="{rng.randint(0, 20)}" y1="{rng.randint(0, 40)}" '
        f'x2="{rng.randint(80, 100)}" y2="{rng.randint(0, 40)}" '
        f'stroke="hsl({rng.randint(0, 360)},60%,70%)" stroke-width="1"/>'
        for _ in range(4)
    )
    chars = ""
    for i, ch in enumerate(code):
        chars += (
            f'<text x="{16 + i * 20}" y="30" font-size="24" font-family="monospace" '
            f'fill="hsl({rng.randint(0, 360)},70%,40%)" '
            f'transform="rotate({rng.randint(-20, 20)} {16 + i * 20} 30)">{ch}</text>'
        )
    svg = (
        '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="40">'
        '<rect width="100" height="40" fill="#f5f7fa"/>'
        f"{lines}{chars}</svg>"
    )
    return svg.encode("utf-8")


@captcha_router.get("")
def get_captcha_image(key: str = Query(default=""), t: str = Query(default="")):
    """返回验证码图片。

    前端 ``Login.vue`` 以 ``<img src="/api/captcha?k=...&t=...">`` 引用，
    因此这里必须返回图片二进制而非 JSON。用 SVG 渲染可避免引入 Pillow 依赖。
    """
    code = "".join(random.choices(string.digits, k=4))
    _CAPTCHA_STORE[key or ""] = code
    return StreamingResponse(
        io.BytesIO(_render_captcha_svg(code)),
        media_type="image/svg+xml",
        headers={"Cache-Control": "no-store"},
    )


@captcha_router.get("/verify")
def verify_captcha(key: str = Query(default=""), code: str = Query(default="")):
    """校验验证码：比对 key 对应的值。

    前端调用 ``GET /captcha/verify?key=xxx&code=xxx``，返回业务 code=200 表示通过。
    """
    matched = bool(key) and _CAPTCHA_STORE.get(key) == code
    if matched:
        _CAPTCHA_STORE.pop(key, None)
    return ok(matched, "验证通过" if matched else "验证码错误或已过期")


@captcha_router.post("/verify")
def verify_captcha_post(payload: dict):
    """校验验证码（POST 形式，body 为 ``{token|key, code}``）。"""
    token = (payload or {}).get("token") or (payload or {}).get("key", "")
    code = (payload or {}).get("code", "")
    matched = bool(token) and _CAPTCHA_STORE.get(token) == code
    if matched:
        _CAPTCHA_STORE.pop(token, None)
    return ok(matched, "验证通过" if matched else "验证码错误或已过期")
