"""推荐引擎测试。

对齐 Java ``RecommendServiceImplTest`` 的核心断言：
个性化推荐必须优先返回与用户历史交互相似的装备。
"""

from __future__ import annotations

import json
from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.models.equipment import Category, Equipment
from app.models.interaction import BrowseHistory, Comment, Favorite
from app.models.user import User
from app.services.recommend import (
    calculate_price_similarity,
    calculate_similarity,
    calculate_tag_similarity,
    get_price_range,
    RecommendService,
)


def _equipment(
    db: Session,
    name: str,
    category_id: int,
    price: float,
    brand: str = "蝴蝶",
    tags: str | None = None,
    suitable_for: str = "",
) -> Equipment:
    """构造装备。"""
    eq = Equipment(
        name=name,
        category_id=category_id,
        brand=brand,
        price=price,
        status=1,
        deleted=0,
        view_count=0,
        favorite_count=0,
        sales_count=0,
        rating=4.0,
        rating_count=0,
        stock=10,
        is_recommend=0,
        tags=tags,
        suitable_for=suitable_for,
        create_time=datetime.now(),
        update_time=datetime.now(),
    )
    db.add(eq)
    db.flush()
    return eq


def test_personal_recommend_prefers_similar_to_interactions(db: Session):
    """核心断言：推荐结果应优先命中与已交互装备相似的候选。

    场景：用户深度浏览并收藏了一款「底板」类装备，
    候选池中同类的 B 应与用户兴趣更匹配，排在异类的 C 之前。
    """
    cat_blade = Category(name="底板", parent_id=0, level=1, status=1, deleted=0)
    cat_rubber = Category(name="胶皮", parent_id=0, level=1, status=1, deleted=0)
    db.add_all([cat_blade, cat_rubber])
    db.flush()

    user = User(username="u1", password="x", level="初级", role="USER", status=1, deleted=0)
    db.add(user)
    db.flush()

    target = _equipment(db, "A-底板", cat_blade.id, 500, tags=json.dumps([1, 2]))
    similar = _equipment(db, "B-底板", cat_blade.id, 520, tags=json.dumps([1, 2]))
    unlike = _equipment(db, "C-胶皮", cat_rubber.id, 120, tags=json.dumps([9]))

    # 用户对 target 产生：深度浏览 + 收藏 + 评论
    db.add(
        BrowseHistory(
            user_id=user.id,
            equipment_id=target.id,
            content_type="detail",
            duration_seconds=600,
            interaction_count=6,
            leave_time=datetime.now(),
            deleted=0,
        )
    )
    db.add(Favorite(user_id=user.id, equipment_id=target.id, deleted=0))
    db.add(
        Comment(
            user_id=user.id,
            equipment_id=target.id,
            content="很好用",
            parent_id=0,
            rating=5,
            status=1,
            deleted=0,
        )
    )
    db.commit()

    result = RecommendService(db).get_personal_recommend(user.id, count=10)
    names = [item["name"] for item in result]

    assert names, "推荐结果不应为空"
    assert "A-底板" not in names, "已交互装备不应出现在推荐结果中"
    assert names.index("B-底板") < names.index("C-胶皮"), (
        f"同类装备应排在异类之前，实际顺序：{names}"
    )


def test_similarity_weights(db: Session):
    """相似度：同类 + 同标签 + 同品牌应显著高于完全不同。"""
    left = _equipment(db, "L", 1, 500, brand="蝴蝶", tags=json.dumps([1, 2, 3]))
    right = _equipment(db, "R", 1, 510, brand="蝴蝶", tags=json.dumps([1, 2, 3]))
    other = _equipment(db, "O", 2, 100, brand="红双喜", tags=json.dumps([7]))

    high = calculate_similarity(left, right)
    low = calculate_similarity(left, other)

    assert high > low
    assert 0.0 <= high <= 1.0


def test_tag_similarity_jaccard():
    """标签相似度：交集 / 较大集合。"""
    assert calculate_tag_similarity(json.dumps([1, 2, 3]), json.dumps([1, 2, 3])) == 1.0
    assert calculate_tag_similarity(json.dumps([1, 2]), json.dumps([2, 3])) == 0.5
    assert calculate_tag_similarity(json.dumps([1]), json.dumps([2])) == 0.0
    assert calculate_tag_similarity(None, json.dumps([1])) == 0.0
    assert calculate_tag_similarity("not-json", json.dumps([1])) == 0.0


def test_price_similarity():
    """价格相似度：相同为 1，差距越大越低，空值为 0.5。"""
    assert calculate_price_similarity(500, 500) == 1.0
    assert calculate_price_similarity(400, 600) < 1.0
    assert calculate_price_similarity(None, 100) == 0.5
    assert calculate_price_similarity(0, 0) == 0.0


def test_price_range_buckets():
    """价格区间分档。"""
    assert get_price_range(50) == "0-100"
    assert get_price_range(200) == "100-300"
    assert get_price_range(400) == "300-500"
    assert get_price_range(800) == "500-1000"
    assert get_price_range(2000) == "1000+"
    assert get_price_range(None) == "unknown"


def test_hot_recommend_fallback(db: Session):
    """冷启动：无行为数据时回落热门推荐。"""
    user = User(username="u2", password="x", role="USER", status=1, deleted=0)
    db.add(user)
    db.flush()

    hot = _equipment(db, "热门装备", 1, 300)
    hot.view_count = 999
    cold = _equipment(db, "冷门装备", 1, 300)
    cold.view_count = 1
    db.commit()

    result = RecommendService(db).get_hot_recommend(10)
    assert result[0]["name"] == "热门装备", "热门榜应按浏览量降序"

    personal = RecommendService(db).get_personal_recommend(user.id, 10)
    assert personal, "冷启动用户仍应拿到推荐结果"


def test_similar_recommend_threshold(db: Session):
    """相似推荐：低于阈值（0.3）的装备不应进入结果。"""
    target = _equipment(db, "目标", 1, 500, brand="蝴蝶", tags=json.dumps([1, 2]))
    _equipment(db, "相近", 1, 520, brand="蝴蝶", tags=json.dumps([1, 2]))
    _equipment(db, "无关", 9, 50, brand="其他", tags=json.dumps([99]))
    db.commit()

    result = RecommendService(db).get_similar_recommend(target.id, 10)
    names = [r["name"] for r in result]

    assert "相近" in names
    assert "无关" not in names, "低相似度装备应被阈值过滤"
    assert "目标" not in names, "不应推荐自身"
