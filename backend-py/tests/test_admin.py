"""管理端与「前端契约补全端点」测试。

覆盖：评论审核、购物车勾选、浏览历史单条删除、偏好画像别名路径、
       装备/分类/标签管理端增删、验证码图片。
"""

from __future__ import annotations

from sqlalchemy import select

from app.core.config import settings
from app.models.equipment import Equipment
from app.models.interaction import Comment
from app.models.user import User

PREFIX = settings.api_prefix


def _make_admin(client, db, username: str = "boss01"):
    """把注册好的用户提升为 ADMIN，返回其认证头。"""
    client.post(
        f"{PREFIX}/user/register", json={"username": username, "password": "test1234"}
    )
    login = client.post(
        f"{PREFIX}/user/login", json={"username": username, "password": "test1234"}
    )
    headers = {"Authorization": f"Bearer {login.json()['data']['token']}"}
    user = db.execute(select(User).where(User.username == username)).scalar_one()
    db.execute(User.__table__.update().where(User.id == user.id).values(role="ADMIN"))
    db.commit()
    return headers


def _seed_comment(db, user_id: int, equipment_id: int, content: str = "手感不错") -> int:
    """插入待审核评论，返回 ID。"""
    row = Comment(
        user_id=user_id,
        equipment_id=equipment_id,
        parent_id=0,
        content=content,
        rating=5,
        like_count=0,
        reply_count=0,
        status=1,
        review_status="PENDING",
        deleted=0,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row.id


def test_comment_moderation_flow(client, db, auth_headers, seed_equipment):
    """评论审核闭环：统计 → 列表 → 审核 → 批量删除。"""
    admin_headers = _make_admin(client, db)
    equipment_id = seed_equipment
    user_id = db.execute(select(User.id).limit(1)).scalar()
    comment_id = _seed_comment(db, user_id, equipment_id)

    # 统计：应含新插入的待审核评论
    stats = client.get(f"{PREFIX}/comment/admin/statistics", headers=admin_headers)
    assert stats.json()["code"] == 200, stats.text
    assert stats.json()["data"]["pending"] >= 1

    # 列表
    listing = client.get(
        f"{PREFIX}/comment/admin/list",
        params={"status": "PENDING"},
        headers=admin_headers,
    )
    assert listing.json()["code"] == 200, listing.text
    assert listing.json()["data"]["total"] >= 1
    assert any(r["id"] == comment_id for r in listing.json()["data"]["records"])

    # 详情
    detail = client.get(f"{PREFIX}/comment/admin/{comment_id}", headers=admin_headers)
    assert detail.json()["code"] == 200
    assert detail.json()["data"]["reviewStatus"] == "PENDING"

    # 驳回
    reject = client.put(
        f"{PREFIX}/comment/admin/review",
        json={"commentId": comment_id, "approved": False, "remark": "含联系方式"},
        headers=admin_headers,
    )
    assert reject.json()["code"] == 200, reject.text
    assert reject.json()["data"]["reviewStatus"] == "REJECTED"

    # 批量删除
    batch = client.post(
        f"{PREFIX}/comment/admin/batch-delete",
        json={"ids": [comment_id]},
        headers=admin_headers,
    )
    assert batch.json()["code"] == 200
    assert batch.json()["data"] == 1

    # 删除后详情应 404
    gone = client.get(f"{PREFIX}/comment/admin/{comment_id}", headers=admin_headers)
    assert gone.json()["code"] == 404


def test_comment_admin_requires_admin(client, db, auth_headers, seed_equipment):
    """非管理员访问评论审核接口应返回 403。"""
    equipment_id = seed_equipment
    user_id = db.execute(select(User.id).limit(1)).scalar()
    _seed_comment(db, user_id, equipment_id)
    resp = client.get(f"{PREFIX}/comment/admin/statistics", headers=auth_headers)
    assert resp.json()["code"] == 403, resp.text


def test_cart_selection(client, db, auth_headers, seed_equipment):
    """购物车：加入 → 取消勾选 → 勾选 → 全选。"""
    equipment_id = seed_equipment

    added = client.post(f"{PREFIX}/cart/add/{equipment_id}", headers=auth_headers)
    assert added.json()["code"] == 200, added.text
    item_id = added.json()["data"]["id"]

    off = client.put(
        f"{PREFIX}/cart/{item_id}/selected",
        json={"selected": 0},
        headers=auth_headers,
    )
    assert off.json()["code"] == 200
    assert off.json()["data"]["selected"] == 0

    summary = client.get(f"{PREFIX}/cart/summary", headers=auth_headers)
    assert summary.json()["data"]["selectedCount"] == 0

    all_on = client.put(
        f"{PREFIX}/cart/selected-all", json={"selected": 1}, headers=auth_headers
    )
    assert all_on.json()["code"] == 200
    assert all_on.json()["data"] >= 1
    assert client.get(f"{PREFIX}/cart/summary", headers=auth_headers).json()["data"][
        "selectedCount"
    ] >= 1


def test_history_delete_single(client, db, auth_headers, seed_equipment):
    """浏览历史：记录后单条删除。"""
    equipment_id = seed_equipment
    client.post(f"{PREFIX}/history/record/{equipment_id}", headers=auth_headers)

    before = client.get(f"{PREFIX}/history/list", headers=auth_headers)
    assert before.json()["code"] == 200
    assert len(before.json()["data"]) == 1
    history_id = before.json()["data"][0]["id"]

    removed = client.delete(f"{PREFIX}/history/{history_id}", headers=auth_headers)
    assert removed.json()["code"] == 200

    after = client.get(f"{PREFIX}/history/list", headers=auth_headers)
    assert len(after.json()["data"]) == 0


def test_user_preferences_alias(client, db, auth_headers):
    """偏好画像同时支持 /preferences（前端）与 /preference。"""
    payload = {
        "level": "INTERMEDIATE",
        "playStyle": "ATTACK",
        "budgetMin": 300,
        "budgetMax": 1200,
    }
    saved = client.put(f"{PREFIX}/user/preferences", json=payload, headers=auth_headers)
    assert saved.json()["code"] == 200, saved.text
    assert saved.json()["data"]["playStyle"] == "ATTACK"

    fetched = client.get(f"{PREFIX}/user/preferences", headers=auth_headers)
    assert fetched.json()["code"] == 200
    assert fetched.json()["data"]["level"] == "INTERMEDIATE"


def test_equipment_crud_requires_admin(client, auth_headers):
    """装备新增/删除必须管理员权限。"""
    resp = client.post(f"{PREFIX}/equipment", json={"name": "X"}, headers=auth_headers)
    assert resp.json()["code"] == 403, resp.text


def test_equipment_crud(client, db, auth_headers):
    """管理员可完成装备新增 → 更新 → 删除。"""
    admin_headers = _make_admin(client, db, username="boss02")
    created = client.post(
        f"{PREFIX}/equipment",
        json={"name": "测试胶皮", "categoryId": 1, "price": 199.00, "stock": 5},
        headers=admin_headers,
    )
    assert created.json()["code"] == 200, created.text
    equipment_id = created.json()["data"]["id"]
    assert created.json()["data"]["categoryId"] == 1, "响应字段应为驼峰"

    updated = client.put(
        f"{PREFIX}/equipment",
        json={"id": equipment_id, "name": "测试胶皮 Pro", "price": 259.00},
        headers=admin_headers,
    )
    assert updated.json()["code"] == 200
    assert updated.json()["data"]["name"] == "测试胶皮 Pro"

    deleted = client.delete(f"{PREFIX}/equipment/{equipment_id}", headers=admin_headers)
    assert deleted.json()["code"] == 200

    gone = client.get(f"{PREFIX}/equipment/detail/{equipment_id}")
    assert gone.json()["code"] == 404


def test_category_and_tag_crud(client, db):
    """分类与标签的管理端新增 / 删除。"""
    admin_headers = _make_admin(client, db, username="boss03")

    cat = client.post(
        f"{PREFIX}/category", json={"name": "测试分类", "level": 1}, headers=admin_headers
    )
    assert cat.json()["code"] == 200, cat.text
    category_id = cat.json()["data"]["id"]

    del_cat = client.delete(f"{PREFIX}/category/{category_id}", headers=admin_headers)
    assert del_cat.json()["code"] == 200

    tag = client.post(
        f"{PREFIX}/tag", json={"name": "测试标签", "category": "trait"}, headers=admin_headers
    )
    assert tag.json()["code"] == 200, tag.text
    tag_id = tag.json()["data"]["id"]

    del_tag = client.delete(f"{PREFIX}/tag/{tag_id}", headers=admin_headers)
    assert del_tag.json()["code"] == 200


def test_captcha_image_returns_svg(client):
    """验证码图片接口应返回可直接用于 <img> 的图片。"""
    resp = client.get(f"{PREFIX}/captcha", params={"k": "abc123", "t": "1"})
    assert resp.status_code == 200
    assert "svg" in resp.headers["content-type"]
    assert resp.text.startswith("<svg")


def test_kickoff_king_frontend_path(client):
    """前端 kickoffKing.js 使用的 /kaiqiuwang-ranking/overview 路径可用。"""
    resp = client.get(f"{PREFIX}/kaiqiuwang-ranking/overview")
    assert resp.json()["code"] == 200, resp.text
