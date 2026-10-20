"""开球王赛事路由。

对齐 Java ``KickoffKingController`` 的 ``/overview``。
"""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.result import ok
from app.db.session import get_db
from app.services.kickoff import KickoffKingService

router = APIRouter(tags=["开球王"])


@router.get("/overview")
def overview(db: Session = Depends(get_db)):
    """开球王赛事总览：赛季 + 分站 + 积分榜前三。"""
    return ok(KickoffKingService(db).get_overview())


@router.get("/kaiqiuwang-ranking/overview")
def overview_frontend(db: Session = Depends(get_db)):
    """前端 ``kickoffKing.js`` 使用的路径，与 /overview 同源。"""
    return ok(KickoffKingService(db).get_overview())


@router.get("/kickoff-king/overview")
def overview_alias(db: Session = Depends(get_db)):
    """兼容带英文模块前缀的调用路径。"""
    return ok(KickoffKingService(db).get_overview())


@router.get("/kickoff-king/seasons")
def seasons(db: Session = Depends(get_db)):
    """赛季列表。"""
    return ok(KickoffKingService(db).list_seasons())
