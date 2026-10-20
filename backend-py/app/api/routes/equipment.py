"""装备 / 分类 / 标签路由。

对齐 Java ``EquipmentController``、``CategoryController``、``TagController``。
"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_optional_user_id, require_admin
from app.core.exceptions import BusinessException
from app.core.result import ok
from app.db.session import get_db
from app.models.user import User
from app.schemas.equipment import CategoryOut, EquipmentOut, TagOut
from app.services.equipment import CategoryService, EquipmentService, TagService

router = APIRouter(prefix="/equipment", tags=["装备"])
category_router = APIRouter(prefix="/category", tags=["分类"])
tag_router = APIRouter(prefix="/tag", tags=["标签"])


def _dump_equipment(eq) -> dict:
    """装备序列化为驼峰字典。"""
    return EquipmentOut.model_validate(eq).model_dump(by_alias=True)


@router.get("/list")
def list_equipments(
    categoryId: Optional[int] = Query(default=None),
    brand: Optional[str] = Query(default=None),
    keyword: Optional[str] = Query(default=None),
    minPrice: Optional[float] = Query(default=None),
    maxPrice: Optional[float] = Query(default=None),
    sort: str = Query(default="default"),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=12, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """装备分页筛选列表。"""
    rows, total = EquipmentService(db).list_equipments(
        category_id=categoryId,
        brand=brand,
        keyword=keyword,
        min_price=minPrice,
        max_price=maxPrice,
        sort=sort,
        page=page,
        size=size,
    )
    return ok(
        {
            "records": [_dump_equipment(r) for r in rows],
            "total": total,
            "size": size,
            "current": page,
            "pages": (total + size - 1) // size if size else 0,
        }
    )


@router.get("/search")
def search(keyword: str = Query(default=""), limit: int = Query(default=20), db: Session = Depends(get_db)):
    """装备搜索。"""
    rows = EquipmentService(db).search(keyword, limit)
    return ok([_dump_equipment(r) for r in rows])


@router.get("/statistics")
def statistics(db: Session = Depends(get_db)):
    """装备统计概览。"""
    return ok(EquipmentService(db).statistics())


@router.get("/detail/{equipment_id}")
def detail(
    equipment_id: int,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """装备详情，并原子递增浏览量。"""
    service = EquipmentService(db)
    eq = service.get(equipment_id)
    if eq is None:
        from app.core.exceptions import NotFoundException

        raise NotFoundException("装备不存在或已下架")

    service.incr_view_count(equipment_id)
    db.refresh(eq)
    return ok(_dump_equipment(eq))


# ---------------- 管理端写操作 ----------------


@router.post("")
def create_equipment(
    payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """新增装备。"""
    row = EquipmentService(db).create(payload)
    return ok(_dump_equipment(row), "已新增")


@router.put("")
def update_equipment(
    payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """更新装备（请求体需含 id）。"""
    equipment_id = payload.get("id")
    if not equipment_id:
        raise BusinessException("缺少装备 id")
    row = EquipmentService(db).update(int(equipment_id), payload)
    return ok(_dump_equipment(row), "已更新")


@router.delete("/{equipment_id}")
def delete_equipment(
    equipment_id: int, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """逻辑删除装备。"""
    EquipmentService(db).delete(equipment_id)
    return ok(True, "已删除")


# ============================================================
# 分类
# ============================================================


@category_router.get("/list")
def list_categories(parentId: Optional[int] = Query(default=None), db: Session = Depends(get_db)):
    """分类列表。"""
    rows = CategoryService(db).list_categories(parentId)
    return ok([CategoryOut.model_validate(r).model_dump(by_alias=True) for r in rows])


@category_router.get("/tree")
def category_tree(db: Session = Depends(get_db)):
    """分类树。"""
    rows = CategoryService(db).list_categories()
    by_id = {
        r.id: {**CategoryOut.model_validate(r).model_dump(by_alias=True), "children": []}
        for r in rows
    }
    roots = []
    for r in rows:
        node = by_id[r.id]
        parent = by_id.get(r.parent_id or 0)
        if parent is not None and (r.parent_id or 0) != 0:
            parent["children"].append(node)
        else:
            roots.append(node)
    return ok(roots)


@category_router.post("")
def create_category(
    payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """新增分类。"""
    row = CategoryService(db).create(payload)
    return ok(CategoryOut.model_validate(row).model_dump(by_alias=True), "已新增")


@category_router.put("")
def update_category(
    payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """更新分类（请求体需含 id）。"""
    category_id = payload.get("id")
    if not category_id:
        raise BusinessException("缺少分类 id")
    row = CategoryService(db).update(int(category_id), payload)
    return ok(CategoryOut.model_validate(row).model_dump(by_alias=True), "已更新")


@category_router.delete("/{category_id}")
def delete_category(
    category_id: int, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    """逻辑删除分类。"""
    CategoryService(db).delete(category_id)
    return ok(True, "已删除")


# ============================================================
# 标签
# ============================================================


@tag_router.get("/list")
def list_tags(category: Optional[str] = Query(default=None), db: Session = Depends(get_db)):
    """标签列表。"""
    rows = TagService(db).list_tags(category)
    return ok([TagOut.model_validate(r).model_dump(by_alias=True) for r in rows])


@tag_router.post("")
def create_tag(payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    """新增标签。"""
    row = TagService(db).create(payload)
    return ok(TagOut.model_validate(row).model_dump(by_alias=True), "已新增")


@tag_router.put("")
def update_tag(payload: dict, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    """更新标签（请求体需含 id）。"""
    tag_id = payload.get("id")
    if not tag_id:
        raise BusinessException("缺少标签 id")
    row = TagService(db).update(int(tag_id), payload)
    return ok(TagOut.model_validate(row).model_dump(by_alias=True), "已更新")


@tag_router.delete("/{tag_id}")
def delete_tag(tag_id: int, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    """逻辑删除标签。"""
    TagService(db).delete(tag_id)
    return ok(True, "已删除")
