"""开球王赛事领域 ORM 模型。"""

from __future__ import annotations

from datetime import date, datetime

from sqlalchemy import BigInteger, Date, DateTime, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, PK, TimestampMixin


class KickoffKingSeason(Base, TimestampMixin):
    """开球王赛季表。"""

    __tablename__ = "kickoff_king_season"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    season_code: Mapped[str | None] = mapped_column(String(50))
    season_name: Mapped[str | None] = mapped_column(String(100))
    hero_title: Mapped[str | None] = mapped_column(String(100))
    hero_subtitle: Mapped[str | None] = mapped_column(String(255))
    season_status: Mapped[str | None] = mapped_column(String(20))
    total_stops: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    completed_stops: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    current_stop_name: Mapped[str | None] = mapped_column(String(100))
    season_start: Mapped[date | None] = mapped_column(Date)
    season_end: Mapped[date | None] = mapped_column(Date)
    announcement: Mapped[str | None] = mapped_column(Text)
    highlight_tags: Mapped[str | None] = mapped_column(String(255))


class KickoffKingStage(Base, TimestampMixin):
    """开球王分站表。"""

    __tablename__ = "kickoff_king_stage"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    season_id: Mapped[int | None] = mapped_column(BigInteger)
    stage_name: Mapped[str | None] = mapped_column(String(100))
    city_name: Mapped[str | None] = mapped_column(String(50))
    stage_type: Mapped[str | None] = mapped_column(String(50))
    stage_status: Mapped[str | None] = mapped_column(String(20))
    stage_date: Mapped[date | None] = mapped_column(Date)
    theme: Mapped[str | None] = mapped_column(String(100))
    focus_skill: Mapped[str | None] = mapped_column(String(100))
    highlight_desc: Mapped[str | None] = mapped_column(Text)


class KickoffKingStanding(Base, TimestampMixin):
    """开球王积分榜表。"""

    __tablename__ = "kickoff_king_standing"

    id: Mapped[int] = mapped_column(PK, primary_key=True, autoincrement=True)
    season_id: Mapped[int | None] = mapped_column(BigInteger)
    rank_position: Mapped[int | None] = mapped_column(Integer)
    player_name: Mapped[str | None] = mapped_column(String(50))
    association_name: Mapped[str | None] = mapped_column(String(100))
    points: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    stage_wins: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    podium_count: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    serve_style: Mapped[str | None] = mapped_column(String(50))
    hot_form: Mapped[str | None] = mapped_column(String(50))
