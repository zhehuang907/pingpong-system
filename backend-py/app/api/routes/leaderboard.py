"""榜单路由。

对齐 Java ``LeaderboardController``：
``/leaderboard/home``、``/leaderboard/detail/{boardType}``、
``/leaderboard/equipment/{id}/positions``、
``/leaderboard/subscriptions``（GET/POST/DELETE）、
``/leaderboard/share``、``/leaderboard/share/stats``
"""

from __future__ import annotations

from typing import Any, Dict, Optional

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id, get_optional_user_id
from app.core.result import ok
from app.db.session import get_db
from app.services.leaderboard import LeaderboardService

router = APIRouter(prefix="/leaderboard", tags=["榜单"])


class ShareIn(BaseModel):
    """分享上报入参。"""

    boardType: Optional[str] = None
    equipmentId: int
    channel: str = "link"


@router.get("/home")
def home(limit: int = Query(default=10, le=50), db: Session = Depends(get_db)):
    """榜单首页（四类榜单各取前 N）。"""
    return ok(LeaderboardService(db).get_overview(limit))


@router.get("/detail/{board_type}")
def detail(
    board_type: str, limit: int = Query(default=20, le=50), db: Session = Depends(get_db)
):
    """单个榜单详情。"""
    return ok(LeaderboardService(db).get_board(board_type, limit))


@router.get("/equipment/{equipment_id}/positions")
def equipment_positions(equipment_id: int, db: Session = Depends(get_db)):
    """装备在各个榜单中的排名位置。"""
    from app.models.leaderboard import LeaderboardSnapshot
    from sqlalchemy import select

    rows = (
        db.execute(
            select(LeaderboardSnapshot).where(
                LeaderboardSnapshot.equipment_id == equipment_id
            )
        )
        .scalars()
        .all()
    )
    return ok(
        [
            {
                "boardType": r.board_type,
                "boardName": r.board_name,
                "rankPosition": r.rank_position,
                "totalScore": float(r.total_score) if r.total_score is not None else 0.0,
            }
            for r in rows
        ]
    )


@router.get("/subscriptions")
def list_subscriptions(
    user_id: Optional[int] = Depends(get_optional_user_id), db: Session = Depends(get_db)
):
    """订阅列表。"""
    if user_id is None:
        return ok([])
    from app.models.leaderboard import LeaderboardSubscription
    from sqlalchemy import select

    rows = (
        db.execute(
            select(LeaderboardSubscription).where(
                LeaderboardSubscription.user_id == user_id,
                LeaderboardSubscription.active == 1,
            )
        )
        .scalars()
        .all()
    )
    return ok([{"boardType": r.board_type, "active": r.active} for r in rows])


@router.post("/subscriptions/{board_type}")
def subscribe(
    board_type: str, user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """订阅榜单。"""
    LeaderboardService(db).subscribe(user_id, board_type)
    return ok(True, "已订阅")


@router.delete("/subscriptions/{board_type}")
def unsubscribe(
    board_type: str, user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """取消订阅。"""
    LeaderboardService(db).unsubscribe(user_id, board_type)
    return ok(True, "已取消订阅")


@router.post("/share")
def share(
    payload: ShareIn,
    user_id: Optional[int] = Depends(get_optional_user_id),
    db: Session = Depends(get_db),
):
    """上报榜单分享。"""
    result = LeaderboardService(db).record_share(
        user_id or 0, payload.boardType, payload.equipmentId, payload.channel
    )
    return ok(result)


@router.get("/share/stats")
def share_stats(boardType: Optional[str] = Query(default=None), db: Session = Depends(get_db)):
    """分享统计。"""
    from app.models.leaderboard import LeaderboardShareStat
    from sqlalchemy import select

    stmt = select(LeaderboardShareStat)
    if boardType:
        stmt = stmt.where(LeaderboardShareStat.board_type == boardType)
    rows = db.execute(stmt).scalars().all()
    total = sum(r.share_count or 0 for r in rows)
    return ok(
        {
            "boardType": boardType or "ALL",
            "total": total,
            "items": [
                {
                    "boardType": r.board_type,
                    "equipmentId": r.equipment_id,
                    "channel": r.channel,
                    "shareCount": r.share_count,
                }
                for r in rows
            ],
        }
    )
