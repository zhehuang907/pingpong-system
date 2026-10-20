"""API 路由聚合。"""

from fastapi import APIRouter

from app.api.routes.admin import admin_router, captcha_router, upload_router
from app.api.routes.ai import router as ai_router
from app.api.routes.auth import router as auth_router
from app.api.routes.equipment import category_router, router as equipment_router, tag_router
from app.api.routes.interaction import (
    cart_router,
    comment_admin_router,
    comment_router,
    favorite_router,
    history_router,
)
from app.api.routes.kickoff import router as kickoff_router
from app.api.routes.leaderboard import router as leaderboard_router
from app.api.routes.recommend import router as recommend_router
from app.api.routes.user import router as user_router


def build_api_router() -> APIRouter:
    """组装全部业务路由。

    对应 Java 侧 15 个 Controller，路径前缀与之一一对应。
    """
    api = APIRouter()

    api.include_router(auth_router)
    api.include_router(user_router)
    api.include_router(equipment_router)
    api.include_router(category_router)
    api.include_router(tag_router)
    api.include_router(favorite_router)
    api.include_router(comment_router)
    api.include_router(comment_admin_router)
    api.include_router(history_router)
    api.include_router(cart_router)
    api.include_router(recommend_router)
    api.include_router(leaderboard_router)
    api.include_router(kickoff_router)
    api.include_router(ai_router)
    api.include_router(admin_router)
    api.include_router(upload_router)
    api.include_router(captcha_router)

    return api
