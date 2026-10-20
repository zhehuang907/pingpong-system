"""AI 导购助手服务（DeepSeek）。

由 Java ``AiChatServiceImpl`` 移植：会话管理、多轮上下文裁剪、
装备目录注入、消息与用量落库。

上下文策略与 Java 一致：
- 历史消息最多取最近 8 条（``context-message-limit``）
- 装备目录最多注入 4 条（``catalog-limit``）
"""

from __future__ import annotations

import json
from datetime import datetime
from typing import Any, Dict, List, Optional

import httpx
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.exceptions import BusinessException
from app.models.ai import AiChatMessage, AiChatSession
from app.models.equipment import Equipment

SYSTEM_PROMPT = (
    "你是乒乓球装备导购助手。请基于提供的装备目录回答问题，"
    "给出具体型号建议并说明适用打法与价位。"
    "若目录中没有匹配装备，请如实说明，不要编造型号。"
)


class AiChatService:
    """AI 会话服务。"""

    def __init__(self, db: Session):
        self.db = db

    # ---------------- 会话 ----------------

    def list_sessions(self, user_id: int, limit: int = 50) -> List[Dict[str, Any]]:
        """会话列表。"""
        rows = list(
            self.db.execute(
                select(AiChatSession)
                .where(AiChatSession.user_id == user_id, AiChatSession.deleted == 0)
                .order_by(AiChatSession.last_message_at.desc())
                .limit(limit)
            )
            .scalars()
            .all()
        )
        return [self._session_dict(r) for r in rows]

    def create_session(self, user_id: int, title: str = "新的咨询", scene: str = "equipment") -> AiChatSession:
        """创建会话。"""
        row = AiChatSession(
            user_id=user_id,
            title=title or "新的咨询",
            scene=scene,
            message_count=0,
            deleted=0,
        )
        self.db.add(row)
        self.db.commit()
        self.db.refresh(row)
        return row

    def get_session(self, session_id: int, user_id: int) -> AiChatSession:
        """获取会话（校验归属）。"""
        row = self.db.get(AiChatSession, session_id)
        if row is None or row.deleted == 1:
            raise BusinessException("会话不存在")
        if row.user_id != user_id:
            raise BusinessException("无权访问该会话")
        return row

    def get_session_detail(self, session_id: int, user_id: int) -> Dict[str, Any]:
        """会话详情（含消息列表）。"""
        session = self.get_session(session_id, user_id)
        messages = list(
            self.db.execute(
                select(AiChatMessage)
                .where(AiChatMessage.session_id == session_id, AiChatMessage.deleted == 0)
                .order_by(AiChatMessage.create_time.asc())
            )
            .scalars()
            .all()
        )
        return {
            **self._session_dict(session),
            "messages": [self._message_dict(m) for m in messages],
        }

    def rename_session(self, session_id: int, user_id: int, title: str) -> AiChatSession:
        """重命名会话。"""
        session = self.get_session(session_id, user_id)
        session.title = title
        self.db.commit()
        return session

    def delete_session(self, session_id: int, user_id: int) -> None:
        """删除会话（逻辑删除会话与消息）。"""
        session = self.get_session(session_id, user_id)
        session.deleted = 1
        messages = (
            self.db.execute(
                select(AiChatMessage).where(
                    AiChatMessage.session_id == session_id, AiChatMessage.deleted == 0
                )
            )
            .scalars()
            .all()
        )
        for m in messages:
            m.deleted = 1
        self.db.commit()

    # ---------------- 对话 ----------------

    def send_message(self, user_id: int, session_id: int, content: str) -> Dict[str, Any]:
        """发送消息并获取 AI 回复。

        流程：落库用户消息 → 构建上下文（历史 + 装备目录）→
        调用 DeepSeek → 落库助手消息 → 更新会话摘要。
        """
        if not content or not content.strip():
            raise BusinessException("消息内容不能为空")

        session = self.get_session(session_id, user_id)

        user_msg = AiChatMessage(
            session_id=session_id,
            user_id=user_id,
            role="user",
            content=content.strip(),
            status="SUCCESS",
            deleted=0,
        )
        self.db.add(user_msg)
        self.db.flush()

        history = self._recent_messages(session_id)
        catalog = self._build_catalog(content)

        try:
            answer, usage = self._call_deepseek(history + [{"role": "user", "content": content}], catalog)
            status = "SUCCESS"
            error = None
        except Exception as exc:  # 降级：不让 AI 故障阻断会话
            answer = "抱歉，AI 服务暂时不可用，请稍后再试。"
            usage = {}
            status = "FAILED"
            error = str(exc)[:500]

        assistant_msg = AiChatMessage(
            session_id=session_id,
            user_id=user_id,
            role="assistant",
            content=answer,
            status=status,
            model=settings.deepseek_model if status == "SUCCESS" else None,
            finish_reason=usage.get("finish_reason"),
            prompt_tokens=usage.get("prompt_tokens", 0),
            completion_tokens=usage.get("completion_tokens", 0),
            total_tokens=usage.get("total_tokens", 0),
            error_message=error,
            deleted=0,
        )
        self.db.add(assistant_msg)

        session.message_count = (session.message_count or 0) + 2
        session.last_message_preview = content.strip()[:100]
        session.last_message_at = datetime.now()
        self.db.commit()
        self.db.refresh(assistant_msg)

        return self._message_dict(assistant_msg)

    # ---------------- 内部 ----------------

    def _recent_messages(self, session_id: int) -> List[Dict[str, str]]:
        """取最近 N 条历史消息（限制上下文长度）。"""
        rows = list(
            self.db.execute(
                select(AiChatMessage)
                .where(
                    AiChatMessage.session_id == session_id,
                    AiChatMessage.deleted == 0,
                    AiChatMessage.status == "SUCCESS",
                )
                .order_by(AiChatMessage.create_time.desc())
                .limit(settings.deepseek_context_message_limit)
            )
            .scalars()
            .all()
        )
        rows.reverse()
        return [{"role": m.role or "user", "content": m.content or ""} for m in rows]

    def _build_catalog(self, keyword: str) -> List[Dict[str, Any]]:
        """按关键词召回装备目录，注入 Prompt 作为事实依据，抑制幻觉。"""
        stmt = select(Equipment).where(Equipment.status == 1, Equipment.deleted == 0)
        if keyword:
            like = f"%{keyword[:20]}%"
            from sqlalchemy import or_

            stmt = stmt.where(
                or_(Equipment.name.like(like), Equipment.brand.like(like))
            )
        rows = list(
            self.db.execute(
                stmt.order_by(Equipment.view_count.desc()).limit(
                    settings.deepseek_catalog_limit
                )
            )
            .scalars()
            .all()
        )
        return [
            {
                "id": e.id,
                "name": e.name,
                "brand": e.brand,
                "price": float(e.price) if e.price is not None else None,
                "suitableFor": e.suitable_for,
                "features": (e.features or "")[:200],
            }
            for e in rows
        ]

    def _call_deepseek(
        self, messages: List[Dict[str, str]], catalog: List[Dict[str, Any]]
    ) -> tuple[str, Dict[str, Any]]:
        """调用 DeepSeek Chat Completions。"""
        if not settings.deepseek_enabled or not settings.deepseek_api_key:
            raise BusinessException("AI 服务未配置")

        system = SYSTEM_PROMPT
        if catalog:
            system += "\n\n可参考的装备目录：\n" + json.dumps(catalog, ensure_ascii=False)

        payload = {
            "model": settings.deepseek_model,
            "messages": [{"role": "system", "content": system}, *messages],
            "temperature": settings.deepseek_temperature,
            "max_tokens": settings.deepseek_max_output_tokens,
            "stream": False,
        }
        headers = {
            "Authorization": f"Bearer {settings.deepseek_api_key}",
            "Content-Type": "application/json",
        }

        with httpx.Client(timeout=settings.deepseek_timeout_millis / 1000) as client:
            resp = client.post(
                f"{settings.deepseek_base_url}{settings.deepseek_chat_path}",
                json=payload,
                headers=headers,
            )
            resp.raise_for_status()
            data = resp.json()

        choices = data.get("choices") or []
        if not choices:
            raise BusinessException("AI 返回内容为空")

        answer = choices[0].get("message", {}).get("content", "")
        usage = data.get("usage", {})
        return answer, {
            "finish_reason": choices[0].get("finish_reason"),
            "prompt_tokens": usage.get("prompt_tokens", 0),
            "completion_tokens": usage.get("completion_tokens", 0),
            "total_tokens": usage.get("total_tokens", 0),
        }

    @staticmethod
    def _session_dict(s: AiChatSession) -> Dict[str, Any]:
        """会话序列化。"""
        return {
            "id": s.id,
            "userId": s.user_id,
            "title": s.title,
            "scene": s.scene,
            "lastMessagePreview": s.last_message_preview,
            "messageCount": s.message_count,
            "lastMessageAt": s.last_message_at.strftime("%Y-%m-%d %H:%M:%S")
            if s.last_message_at
            else None,
            "createTime": s.create_time.strftime("%Y-%m-%d %H:%M:%S")
            if s.create_time
            else None,
        }

    @staticmethod
    def _message_dict(m: AiChatMessage) -> Dict[str, Any]:
        """消息序列化。"""
        return {
            "id": m.id,
            "sessionId": m.session_id,
            "role": m.role,
            "content": m.content,
            "status": m.status,
            "model": m.model,
            "totalTokens": m.total_tokens,
            "createTime": m.create_time.strftime("%Y-%m-%d %H:%M:%S")
            if m.create_time
            else None,
        }
