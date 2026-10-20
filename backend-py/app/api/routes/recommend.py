"""推荐路由。

对齐 Java ``RecommendController``：
``/recommend/personal``、``/recommend/similar/{id}``、``/recommend/hot``、
``/recommend/analyze``、``/recommend/profile-insight``、
``/recommend/workbench``、``/recommend/click/{id}``
"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id, get_optional_user_id
from app.core.result import ok
from app.db.session import get_db
from app.services.recommend import RecommendService

router = APIRouter(prefix="/recommend", tags=["推荐"])


@router.get("/personal")
def personal(
    count: Optional[int] = Query(default=None, ge=1, le=50),
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """个性化推荐。

    未登录时回落热门推荐，保证接口始终可用。
    """
    service = RecommendService(db)
    if user_id is None:
        return ok(service.get_hot_recommend(count))
    return ok(service.get_personal_recommend(user_id, count))


@router.get("/similar/{equipment_id}")
def similar(
    equipment_id: int,
    count: Optional[int] = Query(default=None, ge=1, le=50),
    db: Session = Depends(get_db),
):
    """相似装备推荐。"""
    return ok(RecommendService(db).get_similar_recommend(equipment_id, count))


@router.get("/hot")
def hot(count: Optional[int] = Query(default=None, ge=1, le=50), db: Session = Depends(get_db)):
    """热门推荐。"""
    return ok(RecommendService(db).get_hot_recommend(count))


@router.get("/analyze")
def analyze(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """用户行为分析（类目/品牌/价格偏好分布）。"""
    return ok(RecommendService(db).analyze_user_behavior(user_id))


@router.get("/profile-insight")
def profile_insight(
    user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """用户画像洞察。"""
    return ok(RecommendService(db).get_user_profile_insight(user_id))


@router.get("/workbench")
def workbench(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """推荐工作台：行为分析 + 画像洞察 + 三路推荐结果。"""
    service = RecommendService(db)
    return ok(
        {
            "behavior": service.analyze_user_behavior(user_id),
            "insight": service.get_user_profile_insight(user_id),
            "personal": service.get_personal_recommend(user_id, 10),
            "hot": service.get_hot_recommend(10),
        }
    )


@router.post("/click/{equipment_id}")
def click(
    equipment_id: int,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """上报推荐点击（用于 CTR 统计）。"""
    if user_id is None:
        return ok(False, "未登录，未记录")
    RecommendService(db).record_click(user_id, equipment_id)
    return ok(True)
