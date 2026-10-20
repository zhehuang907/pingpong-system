"""AI 导购助手路由。

对齐 Java ``AiChatController``：会话 CRUD + 消息发送。
"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id
from app.core.result import ok
from app.db.session import get_db
from app.services.ai_chat import AiChatService

router = APIRouter(prefix="/ai-chat", tags=["AI 助手"])


class CreateSessionIn(BaseModel):
    """创建会话入参。"""

    title: str = "新的咨询"
    scene: str = "equipment"


class SendMessageIn(BaseModel):
    """发送消息入参。"""

    sessionId: int
    content: str


class RenameIn(BaseModel):
    """重命名入参。"""

    title: str


@router.get("/sessions")
def list_sessions(user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)):
    """会话列表。"""
    return ok(AiChatService(db).list_sessions(user_id))


@router.post("/sessions")
def create_session(
    payload: Optional[CreateSessionIn] = None,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """创建会话。"""
    service = AiChatService(db)
    row = service.create_session(
        user_id,
        title=payload.title if payload else "新的咨询",
        scene=payload.scene if payload else "equipment",
    )
    return ok(service._session_dict(row), "会话已创建")


@router.get("/sessions/{session_id}")
def get_session(
    session_id: int, user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """会话详情（含消息）。"""
    return ok(AiChatService(db).get_session_detail(session_id, user_id))


@router.put("/sessions/{session_id}")
def rename_session(
    session_id: int,
    payload: RenameIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """重命名会话。"""
    row = AiChatService(db).rename_session(session_id, user_id, payload.title)
    return ok(AiChatService._session_dict(row), "已重命名")


@router.delete("/sessions/{session_id}")
def delete_session(
    session_id: int, user_id: int = Depends(get_current_user_id), db: Session = Depends(get_db)
):
    """删除会话。"""
    AiChatService(db).delete_session(session_id, user_id)
    return ok(True, "已删除")


@router.post("/messages")
def send_message(
    payload: SendMessageIn,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """发送消息并获取 AI 回复。"""
    return ok(AiChatService(db).send_message(user_id, payload.sessionId, payload.content))
