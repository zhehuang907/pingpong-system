"""推荐引擎。

由 Java ``RecommendServiceImpl`` 完整移植，三路召回策略及全部权重、
阈值、打分公式与原实现逐项对齐：

1. **个性化推荐**：浏览/收藏/评论三类行为加权（1/3/5）构建用户-装备得分，
   结合用户画像与偏好问答二次打分，对候选池（默认 300）逐项计算
   「交互相似度 × 行为权重」的归一化得分后排序。
2. **相似推荐**：标签 / 分类 / 价格三维加权（0.6 / 0.3 / 0.1）计算装备相似度，
   设 0.3 硬阈值过滤噪声，品牌相同额外 +0.2。
3. **热门兜底**：按浏览量 / 收藏量 / 销量三级排序，服务于冷启动。

所有权重均来自配置（``app.core.config``），调参无需改代码。
"""

from __future__ import annotations

import json
import math
from datetime import datetime, timedelta
from decimal import Decimal
from typing import Any, Dict, Iterable, List, Optional, Sequence, Set, Tuple

from sqlalchemy import Select, desc, or_, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.redis_client import get_json, set_json
from app.models.equipment import Equipment
from app.models.interaction import BrowseHistory, Comment, Favorite, RecommendRecord
from app.models.user import User, UserPreferenceProfile

# ============================================================
# 常量与工具
# ============================================================

CACHE_KEY_PERSONAL = "recommend:personal:{user_id}"
CACHE_KEY_SIMILAR = "recommend:similar:{equipment_id}"
CACHE_KEY_HOT = "recommend:hot"


def _default_text(value: Optional[str]) -> str:
    """空值安全取文本（对应 Java defaultText）。"""
    return value or ""


def _default_int(value: Optional[int]) -> int:
    """空值安全取整数（对应 Java defaultInteger）。"""
    return value or 0


def _to_float(value: Any) -> Optional[float]:
    """Decimal / str / int → float，空值返回 None。"""
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _parse_json_str_set(raw: Optional[str]) -> Set[str]:
    """解析 JSON 字符串数组为集合（对应 Java parseJsonStringSet）。

    容错：非法 JSON 返回空集合，与 Java catch 行为一致。
    """
    if not raw:
        return set()
    try:
        data = json.loads(raw)
        if isinstance(data, list):
            return {str(x) for x in data if str(x).strip()}
        if isinstance(data, str):
            return {data} if data.strip() else set()
    except (json.JSONDecodeError, TypeError):
        return set()
    return set()


def _parse_tag_ids(raw: Optional[str]) -> Set[int]:
    """解析装备 tags 字段（JSON 数字数组）为 ID 集合。"""
    if not raw:
        return set()
    try:
        data = json.loads(raw)
        if isinstance(data, list):
            out = set()
            for x in data:
                try:
                    out.add(int(x))
                except (TypeError, ValueError):
                    continue
            return out
    except (json.JSONDecodeError, TypeError):
        return set()
    return set()


# ============================================================
# 相似度计算
# ============================================================


def calculate_tag_similarity(tags1: Optional[str], tags2: Optional[str]) -> float:
    """标签相似度：Jaccard 变体（交集 / 较大集合）。

    对应 Java ``calculateTagSimilarity``。
    """
    if not tags1 or not tags2:
        return 0.0
    set1 = _parse_tag_ids(tags1)
    set2 = _parse_tag_ids(tags2)
    if not set1 or not set2:
        return 0.0
    intersection = set1 & set2
    if not intersection:
        return 0.0
    return len(intersection) / max(len(set1), len(set2))


def calculate_price_similarity(price1: Any, price2: Any) -> float:
    """价格相似度：``1 - |差值| / 均值``，空值返回 0.5。

    对应 Java ``calculatePriceSimilarity``。
    """
    p1 = _to_float(price1)
    p2 = _to_float(price2)
    if p1 is None or p2 is None:
        return 0.5
    diff = abs(p1 - p2)
    avg = (p1 + p2) / 2
    if avg == 0:
        return 0.0
    return max(0.0, 1.0 - (diff / avg))


def calculate_similarity(left: Equipment, right: Equipment) -> float:
    """装备综合相似度，上限 1.0。

    组成：分类权重 + 标签相似度 × 标签权重 + 价格相似度 × 价格权重 + 品牌加成 0.2
    """
    similarity = 0.0

    if left.category_id is not None and left.category_id == right.category_id:
        similarity += settings.weight_category

    similarity += calculate_tag_similarity(left.tags, right.tags) * settings.weight_tag
    similarity += calculate_price_similarity(left.price, right.price) * settings.weight_price

    if left.brand and left.brand == right.brand:
        similarity += 0.2

    return min(similarity, 1.0)


def get_price_range(price: Any) -> str:
    """价格区间分档，用于偏好统计。"""
    p = _to_float(price)
    if p is None:
        return "unknown"
    if p < 100:
        return "0-100"
    if p < 300:
        return "100-300"
    if p < 500:
        return "300-500"
    if p < 1000:
        return "500-1000"
    return "1000+"


# ============================================================
# 用户行为加权
# ============================================================


def calculate_browse_weight(history: BrowseHistory) -> float:
    """单条浏览记录的兴趣权重。

    对应 Java ``calculateBrowseHistoryWeight``。基础分为浏览权重，
    再叠加停留时长、交互次数、内容类型与时效四类信号——
    这是「深度浏览」与「划过即走」得以区分的关键。
    """
    score = settings.weight_browse
    duration = _default_int(history.duration_seconds)
    interaction = _default_int(history.interaction_count)

    # 停留时长：上限 600 秒，每 120 秒 +0.35
    score += min(duration, 600) / 120.0 * 0.35
    # 交互深度：上限 6 次，每次 +0.4
    score += min(interaction, 6) * 0.4

    content_type = _default_text(history.content_type).lower()
    if content_type:
        if "detail" in content_type:
            score += 0.8
        elif "compare" in content_type:
            score += 0.6
        elif "list" in content_type:
            score += 0.3

    reference = history.leave_time or history.update_time or history.create_time
    if reference is not None:
        now = datetime.now()
        if reference > now - timedelta(days=3):
            score += 0.5
        elif reference > now - timedelta(days=7):
            score += 0.25

    return score


def build_user_equipment_scores(
    browse_histories: Sequence[BrowseHistory],
    favorites: Sequence[Favorite],
    comments: Sequence[Comment],
) -> Dict[int, float]:
    """聚合三类行为生成 用户-装备 得分表。"""
    scores: Dict[int, float] = {}

    for h in browse_histories:
        if h.equipment_id is None:
            continue
        scores[h.equipment_id] = scores.get(h.equipment_id, 0.0) + calculate_browse_weight(h)

    for f in favorites:
        if f.equipment_id is None:
            continue
        scores[f.equipment_id] = scores.get(f.equipment_id, 0.0) + settings.weight_favorite

    for c in comments:
        if c.equipment_id is None:
            continue
        scores[c.equipment_id] = scores.get(c.equipment_id, 0.0) + settings.weight_comment

    return scores


# ============================================================
# 画像加成
# ============================================================


def is_profile_empty(profile: Optional[UserPreferenceProfile]) -> bool:
    """画像是否为空（对应 Java isPreferenceProfileEmpty）。"""
    if profile is None:
        return True
    return not any(
        [
            profile.level,
            profile.play_style,
            profile.grip_style,
            profile.budget_min is not None,
            profile.budget_max is not None,
            profile.weekly_frequency,
            profile.primary_goal,
            profile.priority_focus,
            profile.preferred_categories,
            profile.preferred_brands,
            profile.preference_tags,
        ]
    )


def is_price_in_budget(price: Any, low: Any, high: Any) -> bool:
    """价格是否落在预算区间内。"""
    p = _to_float(price)
    if p is None:
        return False
    lo = _to_float(low)
    hi = _to_float(high)
    if lo is not None and p < lo:
        return False
    if hi is not None and p > hi:
        return False
    return lo is not None or hi is not None


def is_price_near_budget(price: Any, low: Any, high: Any) -> bool:
    """价格是否贴近预算边界（上浮 15% / 下浮 15% 内）。"""
    p = _to_float(price)
    if p is None:
        return False
    hi = _to_float(high)
    lo = _to_float(low)
    if hi is not None and p > hi and p <= hi * 1.15:
        return True
    if lo is not None and p < lo and p >= lo * 0.85:
        return True
    return False


def _searchable_text(equipment: Equipment) -> str:
    """装备可检索文本（名称/品牌/描述/特点/适用人群/参数拼接）。"""
    return " ".join(
        [
            _default_text(equipment.name),
            _default_text(equipment.brand),
            _default_text(equipment.description),
            _default_text(equipment.features),
            _default_text(equipment.suitable_for),
            _default_text(equipment.parameters),
        ]
    )


def calculate_user_profile_bonus(user: Optional[User], equipment: Equipment) -> float:
    """用户资料加成：水平与打法匹配。"""
    if user is None:
        return 0.0

    score = 0.0
    suitable = _default_text(equipment.suitable_for)
    features = _default_text(equipment.features)

    if user.level and user.level in suitable:
        score += 1.5

    if user.play_style:
        if user.play_style in suitable or user.play_style in features:
            score += 2.0
        elif user.play_style == "全能":
            score += 0.5

    return score


def calculate_preference_score(
    profile: Optional[UserPreferenceProfile], equipment: Equipment
) -> float:
    """偏好画像加成：预算、打法、握拍、目标、关注点、类目、品牌、标签、频次。"""
    if is_profile_empty(profile):
        return 0.0
    assert profile is not None

    score = 0.0
    text = _searchable_text(equipment)

    # 预算
    if is_price_in_budget(equipment.price, profile.budget_min, profile.budget_max):
        score += 1.4
    elif is_price_near_budget(equipment.price, profile.budget_min, profile.budget_max):
        score += 0.4
    elif _to_float(equipment.price) is not None and (
        profile.budget_min is not None or profile.budget_max is not None
    ):
        score -= 0.8

    # 打法 / 握拍 / 目标 / 关注点
    if profile.play_style and profile.play_style in text:
        score += 1.6
    if profile.grip_style and profile.grip_style in text:
        score += 0.9
    if profile.primary_goal and profile.primary_goal in text:
        score += 1.0
    if profile.priority_focus and profile.priority_focus in text:
        score += 1.1

    # 偏好类目 / 品牌
    if equipment.category_id is not None:
        if str(equipment.category_id) in _parse_json_str_set(profile.preferred_categories):
            score += 1.2
    if equipment.brand and equipment.brand in _parse_json_str_set(profile.preferred_brands):
        score += 1.2

    # 偏好标签：最多计 3 个
    preference_tags = _parse_json_str_set(profile.preference_tags)
    if preference_tags:
        match = sum(1 for tag in preference_tags if tag and tag in text)
        score += min(match, 3) * 0.5

    # 使用频次
    if profile.weekly_frequency:
        rating = _to_float(equipment.rating) or 0.0
        price = _to_float(equipment.price)
        if profile.weekly_frequency == "高频" and rating >= 4.5:
            score += 0.6
        if profile.weekly_frequency == "休闲" and price is not None and price <= 800:
            score += 0.4

    # 问答快照
    score += calculate_snapshot_bonus(profile.answer_snapshot, text, equipment)
    return score


def calculate_snapshot_bonus(
    snapshot: Optional[str], text: str, equipment: Equipment
) -> float:
    """适配度问答快照加成。

    快照中的答案关键词若命中装备文本，每个 +0.3，上限 1.2。
    """
    if not snapshot:
        return 0.0
    try:
        data = json.loads(snapshot)
    except (json.JSONDecodeError, TypeError):
        return 0.0

    answers: List[str] = []
    if isinstance(data, dict):
        answers = [str(v) for v in data.values() if isinstance(v, (str, int, float))]
    elif isinstance(data, list):
        answers = [str(v) for v in data if isinstance(v, (str, int, float))]
    else:
        return 0.0

    hits = sum(1 for a in answers if a and a in text)
    return min(hits * 0.3, 1.2)


# ============================================================
# 推荐服务
# ============================================================


class RecommendService:
    """推荐引擎服务。

    用法::

        service = RecommendService(db)
        items = service.get_personal_recommend(user_id=1, count=20)
    """

    def __init__(self, db: Session):
        self.db = db

    # ---------------- 个性化推荐 ----------------

    def get_personal_recommend(
        self, user_id: int, count: Optional[int] = None
    ) -> List[Dict[str, Any]]:
        """个性化推荐。

        流程：缓存命中 → 行为聚合 → 冷启动降级 → 候选池打分 → 排序 →
        装饰推荐理由 → 写缓存 → 记录推荐日志。
        """
        count = count or settings.recommend_personal_count
        cache_key = CACHE_KEY_PERSONAL.format(user_id=user_id)

        cached = get_json(cache_key)
        if cached:
            return cached

        scores = self._load_user_scores(user_id)
        user = self.db.get(User, user_id)
        profile = self._load_profile(user_id)

        # 冷启动：无行为且无画像 → 降级热门
        if not scores and is_profile_empty(profile):
            hot = self.get_hot_recommend(count)
            set_json(cache_key, hot, settings.cache_personal_ttl)
            return hot

        anchors = self._load_anchor_equipments(scores)
        candidates = self._load_candidates(exclude_ids=list(scores.keys()))

        scored: List[Tuple[int, float]] = []
        for eq in candidates:
            score = self._personal_score(eq, anchors, scores, user, profile)
            scored.append((eq.id, score))

        scored.sort(key=lambda x: x[1], reverse=True)
        index = {eq.id: eq for eq in candidates}
        result = [index[eid] for eid, _ in scored[:count] if eid in index]

        if not result:
            result_eqs = self.get_hot_recommend(count)
            set_json(cache_key, result_eqs, settings.cache_personal_ttl)
            return result_eqs

        decorated = self._decorate_personal(result, user, profile, scores)

        for item in decorated:
            self._record_recommend(
                user_id=user_id,
                equipment_id=item["id"],
                recommend_type="PERSONAL",
                score=item.get("_score", 0.0),
                reason=item.get("recommendReason") or "根据浏览行为与偏好问答进行个性化推荐",
            )

        set_json(cache_key, decorated, settings.cache_personal_ttl)
        return decorated

    # ---------------- 相似推荐 ----------------

    def get_similar_recommend(
        self, equipment_id: int, count: Optional[int] = None
    ) -> List[Dict[str, Any]]:
        """相似装备推荐（详情页「看了又看」）。"""
        count = count or settings.recommend_similar_count
        cache_key = CACHE_KEY_SIMILAR.format(equipment_id=equipment_id)

        cached = get_json(cache_key)
        if cached:
            return cached

        target = self.db.get(Equipment, equipment_id)
        if target is None or target.deleted == 1:
            return []

        others = (
            self.db.execute(
                select(Equipment).where(
                    Equipment.status == 1,
                    Equipment.deleted == 0,
                    Equipment.id != equipment_id,
                )
            )
            .scalars()
            .all()
        )

        scored: List[Tuple[int, float]] = []
        for eq in others:
            sim = calculate_similarity(target, eq)
            if sim > settings.similar_threshold:
                scored.append((eq.id, sim * 10))

        scored.sort(key=lambda x: x[1], reverse=True)
        index = {eq.id: eq for eq in others}
        result = [index[eid] for eid, _ in scored[:count] if eid in index]

        decorated = self._decorate_similar(result, target)
        set_json(cache_key, decorated, settings.cache_similar_ttl)
        return decorated

    # ---------------- 热门推荐 ----------------

    def get_hot_recommend(self, count: Optional[int] = None) -> List[Dict[str, Any]]:
        """热门推荐：浏览量 / 收藏量 / 销量三级排序。"""
        count = count or settings.recommend_hot_count
        cache_key = CACHE_KEY_HOT

        cached = get_json(cache_key)
        if cached:
            return cached

        rows = (
            self.db.execute(
                select(Equipment)
                .where(Equipment.status == 1, Equipment.deleted == 0)
                .order_by(
                    desc(Equipment.view_count),
                    desc(Equipment.favorite_count),
                    desc(Equipment.sales_count),
                )
                .limit(count)
            )
            .scalars()
            .all()
        )

        decorated = self._decorate_hot(list(rows))
        set_json(cache_key, decorated, settings.cache_hot_ttl)
        return decorated

    # ---------------- 行为分析 ----------------

    def analyze_user_behavior(self, user_id: int) -> Dict[str, Any]:
        """用户行为分析：类目 / 品牌 / 价格区间偏好分布。"""
        scores = self._load_user_scores(user_id)
        equipments = self._load_anchor_equipments(scores, limit=len(scores) or 1)

        category: Dict[str, float] = {}
        brand: Dict[str, float] = {}
        price_range: Dict[str, float] = {}

        for eq in equipments:
            weight = scores.get(eq.id, 0.0)
            if eq.category_id is not None:
                category[str(eq.category_id)] = category.get(str(eq.category_id), 0.0) + weight
            if eq.brand:
                brand[eq.brand] = brand.get(eq.brand, 0.0) + weight
            pr = get_price_range(eq.price)
            price_range[pr] = price_range.get(pr, 0.0) + weight

        return {
            "categoryPreference": _sorted_map(category),
            "brandPreference": _sorted_map(brand),
            "pricePreference": _sorted_map(price_range),
            "interactionCount": len(scores),
        }

    def get_user_profile_insight(self, user_id: int) -> Dict[str, Any]:
        """用户画像洞察：偏好分布 + 信号明细。"""
        scores = self._load_user_scores(user_id)
        equipments = self._load_anchor_equipments(scores, limit=len(scores) or 1)

        category: Dict[str, float] = {}
        brand: Dict[str, float] = {}
        price: Dict[str, float] = {}

        for eq in equipments:
            weight = scores.get(eq.id, 0.0)
            if eq.category_id is not None:
                category[str(eq.category_id)] = category.get(str(eq.category_id), 0.0) + weight
            if eq.brand:
                brand[eq.brand] = brand.get(eq.brand, 0.0) + weight
            pr = get_price_range(eq.price)
            price[pr] = price.get(pr, 0.0) + weight

        signals = [
            {
                "type": "category",
                "name": k,
                "weight": round(v, 4),
            }
            for k, v in _sorted_map(category)[:5]
        ]
        return {
            "categoryPreference": _sorted_map(category),
            "brandPreference": _sorted_map(brand),
            "pricePreference": _sorted_map(price),
            "signals": signals,
        }

    # ---------------- 推荐记录 ----------------

    def record_click(self, user_id: int, equipment_id: int) -> None:
        """记录推荐结果被点击（用于 CTR 统计）。"""
        row = (
            self.db.execute(
                select(RecommendRecord)
                .where(
                    RecommendRecord.user_id == user_id,
                    RecommendRecord.equipment_id == equipment_id,
                    RecommendRecord.deleted == 0,
                )
                .order_by(desc(RecommendRecord.create_time))
                .limit(1)
            )
            .scalars()
            .first()
        )
        if row is not None:
            row.is_clicked = 1
            self.db.commit()

    # ============================================================
    # 内部方法
    # ============================================================

    def _load_user_scores(self, user_id: int) -> Dict[int, float]:
        """加载用户三类行为并聚合得分。"""
        histories = (
            self.db.execute(
                select(BrowseHistory)
                .where(BrowseHistory.user_id == user_id, BrowseHistory.deleted == 0)
                .order_by(desc(BrowseHistory.update_time), desc(BrowseHistory.create_time))
                .limit(50)
            )
            .scalars()
            .all()
        )
        favorites = (
            self.db.execute(
                select(Favorite).where(Favorite.user_id == user_id, Favorite.deleted == 0)
            )
            .scalars()
            .all()
        )
        comments = (
            self.db.execute(
                select(Comment)
                .where(Comment.user_id == user_id, Comment.deleted == 0)
                .order_by(desc(Comment.create_time))
                .limit(20)
            )
            .scalars()
            .all()
        )
        return build_user_equipment_scores(histories, favorites, comments)

    def _load_anchor_equipments(
        self, scores: Dict[int, float], limit: Optional[int] = None
    ) -> List[Equipment]:
        """取权重最高的若干条已交互装备作为兴趣锚点。"""
        if not scores:
            return []
        limit = limit or settings.recommend_anchor_limit
        top_ids = [
            eid
            for eid, _ in sorted(scores.items(), key=lambda x: x[1], reverse=True)[:limit]
        ]
        if not top_ids:
            return []
        return list(
            self.db.execute(
                select(Equipment).where(Equipment.id.in_(top_ids))
            )
            .scalars()
            .all()
        )

    def _load_candidates(self, exclude_ids: Sequence[int]) -> List[Equipment]:
        """推荐候选池：上架未删、排除已交互，按热度取前 N 条。"""
        stmt = select(Equipment).where(Equipment.status == 1, Equipment.deleted == 0)
        if exclude_ids:
            stmt = stmt.where(~Equipment.id.in_(list(exclude_ids)))
        stmt = stmt.order_by(
            desc(Equipment.view_count), desc(Equipment.favorite_count)
        ).limit(settings.recommend_candidate_limit)
        return list(self.db.execute(stmt).scalars().all())

    def _load_profile(self, user_id: int) -> Optional[UserPreferenceProfile]:
        """加载用户偏好画像。"""
        return (
            self.db.execute(
                select(UserPreferenceProfile)
                .where(
                    UserPreferenceProfile.user_id == user_id,
                    UserPreferenceProfile.deleted == 0,
                )
                .limit(1)
            )
            .scalars()
            .first()
        )

    def _personal_score(
        self,
        equipment: Equipment,
        anchors: Sequence[Equipment],
        scores: Dict[int, float],
        user: Optional[User],
        profile: Optional[UserPreferenceProfile],
    ) -> float:
        """候选装备个性化得分（对应 Java calculatePersonalScore）。"""
        base = 1.0

        if equipment.is_recommend == 1:
            base += 2.0
        rating = _to_float(equipment.rating)
        if rating is not None:
            base += rating * 0.5

        base += math.log1p(_default_int(equipment.view_count)) * 0.3
        base += math.log1p(_default_int(equipment.favorite_count)) * 0.5

        personalized = 0.0
        total_weight = 0.0
        for anchor in anchors:
            weight = scores.get(anchor.id)
            if weight is None or weight <= 0:
                continue
            personalized += calculate_similarity(anchor, equipment) * weight
            total_weight += weight

        if total_weight > 0:
            base += (personalized / total_weight) * 5.0

        base += calculate_user_profile_bonus(user, equipment)
        base += calculate_preference_score(profile, equipment)
        return base

    # ---------------- 装饰：推荐理由 ----------------

    def _decorate_personal(
        self,
        items: Sequence[Equipment],
        user: Optional[User],
        profile: Optional[UserPreferenceProfile],
        scores: Dict[int, float],
    ) -> List[Dict[str, Any]]:
        channel = "行为推荐" if is_profile_empty(profile) else "画像推荐"
        out = []
        for eq in items:
            payload = _equipment_to_dict(eq)
            payload["recommendChannel"] = channel
            payload["recommendReason"] = self._personal_reason(eq, user, profile)
            payload["recommendTags"] = self._personal_tags(eq, user, profile)
            payload["_score"] = round(self._personal_score(eq, [], scores, user, profile), 4)
            out.append(payload)
        return out

    def _decorate_hot(self, items: Sequence[Equipment]) -> List[Dict[str, Any]]:
        out = []
        for eq in items:
            payload = _equipment_to_dict(eq)
            payload["recommendChannel"] = "热门精选"
            payload["recommendReason"] = (
                f"已有 {_default_int(eq.favorite_count)} 人收藏，"
                f"{_default_int(eq.view_count)} 次浏览"
            )
            payload["recommendTags"] = ["热门"]
            out.append(payload)
        return out

    def _decorate_similar(
        self, items: Sequence[Equipment], target: Equipment
    ) -> List[Dict[str, Any]]:
        out = []
        for eq in items:
            payload = _equipment_to_dict(eq)
            sim = calculate_similarity(target, eq)
            payload["recommendChannel"] = "相似推荐"
            payload["recommendReason"] = f"与「{target.name}」相似度 {sim:.0%}"
            payload["similarity"] = round(sim, 4)
            payload["recommendTags"] = ["相似装备"]
            out.append(payload)
        return out

    def _personal_reason(
        self,
        equipment: Equipment,
        user: Optional[User],
        profile: Optional[UserPreferenceProfile],
    ) -> str:
        """生成个性化推荐理由（可解释性）。"""
        reasons: List[str] = []
        text = _searchable_text(equipment)

        if not is_profile_empty(profile) and profile is not None:
            if is_price_in_budget(equipment.price, profile.budget_min, profile.budget_max):
                reasons.append("价格落在你的预算范围内")
            if profile.play_style and profile.play_style in text:
                reasons.append(f"更贴近你的{profile.play_style}打法")
            if profile.priority_focus and profile.priority_focus in text:
                reasons.append(f"命中了你最看重的{profile.priority_focus}")

        if user is not None and user.level and user.level in _default_text(
            equipment.suitable_for
        ):
            reasons.append(f"适合{user.level}阶段")
        if user is not None and user.play_style and user.play_style in text:
            reasons.append(f"匹配你的{user.play_style}打法")

        if not reasons:
            reasons.append("根据你的浏览与收藏行为推荐")
        return "，".join(reasons)

    def _personal_tags(
        self,
        equipment: Equipment,
        user: Optional[User],
        profile: Optional[UserPreferenceProfile],
    ) -> List[str]:
        """生成个性化推荐标签。"""
        tags: List[str] = []
        if is_price_in_budget(
            equipment.price,
            profile.budget_min if profile else None,
            profile.budget_max if profile else None,
        ):
            tags.append("预算匹配")
        if user is not None and user.play_style and user.play_style in _searchable_text(
            equipment
        ):
            tags.append(user.play_style)
        if (_to_float(equipment.rating) or 0) >= 4.5:
            tags.append("高分好评")
        if equipment.is_recommend == 1:
            tags.append("编辑推荐")
        return tags or ["个性化推荐"]

    def _record_recommend(
        self,
        user_id: int,
        equipment_id: int,
        recommend_type: str,
        score: float,
        reason: str,
    ) -> None:
        """写入推荐记录，供后续效果分析。"""
        try:
            row = RecommendRecord(
                user_id=user_id,
                equipment_id=equipment_id,
                recommend_type=recommend_type,
                score=score,
                recommend_reason=reason[:250] if reason else None,
                is_clicked=0,
                is_purchased=0,
                deleted=0,
            )
            self.db.add(row)
            self.db.commit()
        except Exception:
            self.db.rollback()


# ============================================================
# 序列化与排序工具
# ============================================================


def _equipment_to_dict(eq: Equipment) -> Dict[str, Any]:
    """ORM 装备 → 驼峰字典（与 Java Jackson 输出一致）。"""
    return {
        "id": eq.id,
        "name": eq.name,
        "categoryId": eq.category_id,
        "brand": eq.brand,
        "model": eq.model,
        "price": _to_float(eq.price),
        "originalPrice": _to_float(eq.original_price),
        "stock": eq.stock,
        "description": eq.description,
        "parameters": eq.parameters,
        "images": eq.images,
        "mainImage": eq.main_image,
        "imageUrl": eq.image_url,
        "videoUrl": eq.video_url,
        "suitableFor": eq.suitable_for,
        "features": eq.features,
        "rating": _to_float(eq.rating),
        "ratingCount": eq.rating_count,
        "viewCount": eq.view_count,
        "favoriteCount": eq.favorite_count,
        "salesCount": eq.sales_count,
        "tags": eq.tags,
        "status": eq.status,
        "isRecommend": eq.is_recommend,
        "createTime": eq.create_time.strftime("%Y-%m-%d %H:%M:%S")
        if eq.create_time
        else None,
        "updateTime": eq.update_time.strftime("%Y-%m-%d %H:%M:%S")
        if eq.update_time
        else None,
    }


def _sorted_map(mapping: Dict[str, float]) -> List[Tuple[str, float]]:
    """按权重降序返回列表。"""
    return sorted(mapping.items(), key=lambda x: x[1], reverse=True)
