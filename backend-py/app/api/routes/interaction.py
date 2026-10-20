"""收藏 / 评论 / 浏览历史 / 购物车路由。

对齐 Java ``FavoriteController``、``CommentController``、
``BrowseHistoryController``、``CartController``。
"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import (
    get_current_user,
    get_current_user_id,
    get_optional_user_id,
    require_admin,
)
from app.core.result import ok
from app.db.session import get_db
from app.models.equipment import Equipment
from app.models.user import User
from app.schemas.equipment import EquipmentOut
from app.services.equipment import EquipmentService
from app.services.interaction import (
    BrowseHistoryService,
    CartService,
    CommentService,
    FavoriteService,
)

# ---------------- 收藏 ----------------
favorite_router = APIRouter(prefix="/favorite", tags=["收藏"])


@favorite_router.post("/toggle/{equipment_id}")
def toggle_favorite(
    equipment_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """切换收藏状态，返回是否已收藏与最新收藏数。"""
    favorited, count = FavoriteService(db).toggle(
        current_user.id, equipment_id, EquipmentService(db)
    )
    return ok({"favorited": favorited, "favoriteCount": count})


@favorite_router.delete("/cancel/{equipment_id}")
def cancel_favorite(
    equipment_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """取消收藏。"""
    service = FavoriteService(db)
    if service.is_favorited(current_user.id, equipment_id):
        service.toggle(current_user.id, equipment_id, EquipmentService(db))
    return ok(True, "已取消收藏")


@favorite_router.get("/list")
def list_favorites(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
):
    """收藏列表（含装备详情）。"""
    service = FavoriteService(db)
    rows = service.list_favorites(current_user.id)
    items = []
    for f in rows:
        eq = db.get(Equipment, f.equipment_id)
        payload = (
            EquipmentOut.model_validate(eq).model_dump(by_alias=True) if eq else None
        )
        items.append(
            {
                "id": f.id,
                "equipmentId": f.equipment_id,
                "createTime": f.create_time.strftime("%Y-%m-%d %H:%M:%S")
                if f.create_time
                else None,
                "equipment": payload,
            }
        )
    return ok(items)


@favorite_router.get("/check/{equipment_id}")
def check_favorite(
    equipment_id: int,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """是否已收藏（未登录恒为 False）。"""
    if user_id is None:
        return ok(False)
    return ok(FavoriteService(db).is_favorited(user_id, equipment_id))


@favorite_router.get("/count")
def favorite_count(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
):
    """收藏数量。"""
    return ok(FavoriteService(db).count_favorites(current_user.id))


# ---------------- 评论 ----------------
comment_router = APIRouter(prefix="/comment", tags=["评论"])


class CommentIn(BaseModel):
    """发表评论入参。"""

    equipmentId: int
    content: str
    parentId: int = 0
    rating: int = 5
    images: Optional[str] = None


@comment_router.get("/equipment/{equipment_id}")
def list_comments(
    equipment_id: int,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """装备评论列表（含回复数与点赞状态）。"""
    return ok(CommentService(db).list_by_equipment(equipment_id, user_id))


@comment_router.get("/reply/{comment_id}")
def list_replies(comment_id: int, db: Session = Depends(get_db)):
    """评论回复列表。"""
    return ok(CommentService(db).list_replies(comment_id))


@comment_router.post("")
@comment_router.post("/add")
def add_comment(
    payload: CommentIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """发表评论或回复。

    前端 ``comment.js`` 调用 ``POST /comment``；``/comment/add`` 为兼容路径。
    """
    row = CommentService(db).create(
        user_id=current_user.id,
        equipment_id=payload.equipmentId,
        content=payload.content,
        parent_id=payload.parentId,
        rating=payload.rating,
        images=payload.images,
    )
    return ok({"id": row.id}, "评论已提交，等待审核")


@comment_router.post("/like/{comment_id}")
def like_comment(
    comment_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """切换评论点赞。"""
    liked, count = CommentService(db).like(current_user.id, comment_id)
    return ok({"liked": liked, "likeCount": count})


@comment_router.delete("/{comment_id}")
def delete_comment(
    comment_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """删除自己的评论。"""
    is_admin = (current_user.role or "").upper() == "ADMIN"
    CommentService(db).delete(comment_id, current_user.id, is_admin=is_admin)
    return ok(True, "已删除")


# ---------------- 评论：后台管理 ----------------
comment_admin_router = APIRouter(prefix="/comment/admin", tags=["评论管理"])


class ReviewIn(BaseModel):
    """评论审核入参。"""

    commentId: int
    approved: bool
    remark: str = ""


class BatchDeleteIn(BaseModel):
    """批量删除入参。"""

    ids: list[int]


@comment_admin_router.get("/statistics")
def comment_statistics(_: User = Depends(require_admin), db: Session = Depends(get_db)):
    """评论审核统计概览。"""
    return ok(CommentService(db).admin_statistics())


@comment_admin_router.get("/list")
def admin_comment_list(
    status: Optional[str] = Query(default=None),
    keyword: Optional[str] = Query(default=None),
    equipmentId: Optional[int] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=20, ge=1, le=100),
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    """后台评论分页列表。"""
    rows, total = CommentService(db).admin_list(
        status=status, keyword=keyword, equipment_id=equipmentId, page=page, size=size
    )
    return ok({"records": rows, "total": total, "current": page, "size": size})


@comment_admin_router.get("/{comment_id}")
def admin_comment_detail(
    comment_id: int, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """后台评论详情。"""
    return ok(CommentService(db).admin_detail(comment_id))


@comment_admin_router.put("/review")
def admin_review_comment(
    payload: ReviewIn,
    admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    """审核评论（通过 / 驳回）。"""
    return ok(
        CommentService(db).admin_review(
            payload.commentId, admin.id, payload.approved, payload.remark
        ),
        "审核完成",
    )


@comment_admin_router.delete("/{comment_id}")
def admin_delete_comment(
    comment_id: int, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """管理员删除评论。"""
    CommentService(db).admin_batch_delete([comment_id])
    return ok(True, "已删除")


@comment_admin_router.post("/batch-delete")
def admin_batch_delete_comments(
    payload: BatchDeleteIn, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """批量删除评论。"""
    count = CommentService(db).admin_batch_delete(payload.ids)
    return ok(count, f"已删除 {count} 条评论")


# ---------------- 浏览历史 ----------------
history_router = APIRouter(prefix="/history", tags=["浏览历史"])


class BrowseIn(BaseModel):
    """浏览记录入参。"""

    equipmentId: int
    contentType: str = "detail"
    sourcePage: str = ""
    sessionId: str = ""
    durationSeconds: int = 0
    interactionCount: int = 0


@history_router.post("/record")
def record_browse(
    payload: BrowseIn,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """上报浏览行为（未登录忽略，不影响主流程）。"""
    if user_id is None:
        return ok(False, "未登录，未记录")
    EquipmentService(db).record_browse(
        user_id=user_id,
        equipment_id=payload.equipmentId,
        content_type=payload.contentType,
        source_page=payload.sourcePage,
        session_id=payload.sessionId,
        duration_seconds=payload.durationSeconds,
        interaction_count=payload.interactionCount,
    )
    return ok(True)


@history_router.post("/record/{equipment_id}")
def record_browse_simple(
    equipment_id: int,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """简化上报：仅装备 ID。"""
    if user_id is None:
        return ok(False, "未登录，未记录")
    EquipmentService(db).record_browse(user_id=user_id, equipment_id=equipment_id)
    return ok(True)


@history_router.get("/list")
def list_history(
    limit: int = Query(default=50, le=200),
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """浏览历史列表。"""
    if user_id is None:
        return ok([])
    rows = BrowseHistoryService(db).list_history(user_id, limit)
    return ok(
        [
            {
                "id": r.id,
                "equipmentId": r.equipment_id,
                "contentType": r.content_type,
                "durationSeconds": r.duration_seconds,
                "createTime": r.create_time.strftime("%Y-%m-%d %H:%M:%S")
                if r.create_time
                else None,
            }
            for r in rows
        ]
    )


@history_router.delete("/{history_id}")
def remove_history(
    history_id: int,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """删除单条浏览历史。"""
    BrowseHistoryService(db).remove_one(user_id, history_id)
    return ok(True, "已删除")


@history_router.delete("/clear")
def clear_history(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """清空浏览历史。"""
    count = BrowseHistoryService(db).clear(user_id)
    return ok(count, f"已清除 {count} 条记录")


# ---------------- 购物车 ----------------
cart_router = APIRouter(prefix="/cart", tags=["购物车"])


class QuantityIn(BaseModel):
    """数量入参。"""

    quantity: int


class SelectedIn(BaseModel):
    """选中状态入参。"""

    selected: int


@cart_router.post("/add/{equipment_id}")
def add_to_cart(
    equipment_id: int,
    quantity: int = Query(default=1, ge=1),
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """加入购物车。"""
    row = CartService(db).add(user_id, equipment_id, quantity)
    return ok({"id": row.id, "quantity": row.quantity}, "已加入购物车")


@cart_router.get("/list")
def list_cart(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """购物车列表。"""
    rows = CartService(db).list_items(user_id)
    items = []
    for r in rows:
        eq = db.get(Equipment, r.equipment_id)
        items.append(
            {
                "id": r.id,
                "equipmentId": r.equipment_id,
                "quantity": r.quantity,
                "selected": r.selected,
                "equipment": EquipmentOut.model_validate(eq).model_dump(by_alias=True)
                if eq
                else None,
            }
        )
    return ok(items)


@cart_router.get("/summary")
def cart_summary(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """购物车汇总。"""
    return ok(CartService(db).summary(user_id))


@cart_router.get("/count")
def cart_count(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """购物车件数。"""
    return ok(CartService(db).summary(user_id)["totalQuantity"])


@cart_router.put("/{item_id}/quantity")
def update_quantity(
    item_id: int,
    payload: QuantityIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """修改数量（0 表示移除）。"""
    row = CartService(db).update_quantity(user_id, item_id, payload.quantity)
    return ok({"id": row.id, "quantity": row.quantity})


@cart_router.put("/selected-all")
def select_all(
    payload: SelectedIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """全选 / 全不选。"""
    count = CartService(db).select_all(user_id, payload.selected)
    return ok(count, f"已更新 {count} 个条目")


@cart_router.put("/{item_id}/selected")
def update_selected(
    item_id: int,
    payload: SelectedIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """修改单个条目选中状态。"""
    row = CartService(db).update_selected(user_id, item_id, payload.selected)
    return ok({"id": row.id, "selected": row.selected})


@cart_router.delete("/{item_id}")
def remove_item(
    item_id: int, user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """移除条目。"""
    CartService(db).remove(user_id, item_id)
    return ok(True, "已移除")


@cart_router.delete("/clear")
def clear_cart(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """清空购物车。"""
    service = CartService(db)
    for r in service.list_items(user_id):
        service.remove(user_id, r.id)
    return ok(True, "已清空")
