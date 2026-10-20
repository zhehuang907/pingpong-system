"""API 契约测试。

验证响应结构与 Java 侧一致：``{code, msg, data}`` 包裹 + 驼峰字段名。
"""

from __future__ import annotations

from sqlalchemy import select

from app.core.config import settings
from app.models.equipment import Equipment

PREFIX = settings.api_prefix


def test_health(client):
    """健康检查接口。"""
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["status"] == "UP"


def test_register_and_login(client):
    """注册 → 登录 → 携带 token 访问受保护接口。"""
    reg = client.post(
        f"{PREFIX}/user/register", json={"username": "alice01", "password": "test1234"}
    )
    assert reg.json()["code"] == 200, reg.text
    # 不泄露密码字段
    assert "password" not in reg.json()["data"]

    login = client.post(
        f"{PREFIX}/user/login", json={"username": "alice01", "password": "test1234"}
    )
    body = login.json()
    assert body["code"] == 200, body
    assert body["data"]["token"]
    assert body["data"]["tokenType"] == "Bearer"


def test_login_wrong_password(client):
    """密码错误应返回业务失败（code=500）而非 HTTP 5xx。"""
    client.post(
        f"{PREFIX}/user/register", json={"username": "bob01", "password": "test1234"}
    )
    resp = client.post(
        f"{PREFIX}/user/login", json={"username": "bob01", "password": "wrongpass1"}
    )
    body = resp.json()
    assert body["code"] == 500
    assert "密码" in body["msg"]


def test_user_info_requires_auth(client):
    """未登录访问受保护接口应返回 401。"""
    resp = client.get(f"{PREFIX}/user/info")
    body = resp.json()
    assert body["code"] == 401, body


def test_user_info_with_auth(client, auth_headers):
    """登录后可读取个人信息，且字段为驼峰。"""
    resp = client.get(f"{PREFIX}/user/info", headers=auth_headers)
    body = resp.json()
    assert body["code"] == 200, body
    data = body["data"]
    assert "username" in data
    assert "playStyle" in data, "字段应为驼峰命名"
    assert "password" not in data, "不应返回密码"


def test_equipment_list_public(client):
    """装备列表为公开接口，无需登录。"""
    resp = client.get(f"{PREFIX}/equipment/list")
    body = resp.json()
    assert body["code"] == 200, body
    assert "records" in body["data"]
    assert "total" in body["data"]


def test_favorite_toggle(client, auth_headers, db):
    """收藏切换：第一次收藏，第二次取消，并校验计数变化。"""
    db.add(
        Equipment(
            name="测试底板",
            category_id=1,
            brand="TestBrand",
            price=299.00,
            stock=10,
            status=1,
            deleted=0,
            favorite_count=0,
        )
    )
    db.commit()
    equipment_id = db.execute(select(Equipment.id)).scalar()

    first = client.post(f"{PREFIX}/favorite/toggle/{equipment_id}", headers=auth_headers)
    body = first.json()
    assert body["code"] == 200, body
    assert body["data"]["favorited"] is True
    assert body["data"]["favoriteCount"] == 1

    # 收藏列表应能看到刚收藏的装备
    fav_list = client.get(f"{PREFIX}/favorite/list", headers=auth_headers)
    assert fav_list.json()["code"] == 200
    assert len(fav_list.json()["data"]) == 1

    # 再次切换 → 取消收藏
    second = client.post(f"{PREFIX}/favorite/toggle/{equipment_id}", headers=auth_headers)
    body = second.json()
    assert body["code"] == 200, body
    assert body["data"]["favorited"] is False
    assert body["data"]["favoriteCount"] == 0


def test_favorite_toggle_missing_equipment(client, auth_headers):
    """收藏不存在的装备应返回 404 业务错误而非崩溃。"""
    resp = client.post(f"{PREFIX}/favorite/toggle/999999", headers=auth_headers)
    body = resp.json()
    assert body["code"] == 404, body


def test_recommend_hot_public(client):
    """热门推荐为公开接口。"""
    resp = client.get(f"{PREFIX}/recommend/hot")
    assert resp.json()["code"] == 200


def test_response_envelope_shape(client):
    """统一响应封装：所有接口均为 {code, msg, data}。"""
    resp = client.get(f"{PREFIX}/equipment/list")
    body = resp.json()
    assert set(body.keys()) >= {"code", "msg", "data"}
