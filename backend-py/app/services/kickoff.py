"""开球王赛事服务。

由 Java ``KickoffKingServiceImpl`` 移植：赛季概览、分站列表、积分榜。
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.kickoff import KickoffKingSeason, KickoffKingStage, KickoffKingStanding


class KickoffKingService:
    """开球王赛事服务。"""

    def __init__(self, db: Session):
        self.db = db

    def get_overview(self) -> Dict[str, Any]:
        """赛事总览：赛季 + 分站 + 积分榜前三。"""
        season = self._current_season()
        if season is None:
            return {"season": None, "stages": [], "standings": []}

        stages = self.list_stages(season.id)
        standings = self.list_standings(season.id, limit=3)

        return {
            "season": self._season_dict(season),
            "stages": [self._stage_dict(s) for s in stages],
            "standings": [self._standing_dict(s) for s in standings],
        }

    def list_seasons(self) -> List[Dict[str, Any]]:
        """赛季列表。"""
        rows = list(
            self.db.execute(
                select(KickoffKingSeason).order_by(KickoffKingSeason.create_time.desc())
            )
            .scalars()
            .all()
        )
        return [self._season_dict(r) for r in rows]

    def list_stages(self, season_id: int) -> List[KickoffKingStage]:
        """分站列表。"""
        return list(
            self.db.execute(
                select(KickoffKingStage)
                .where(KickoffKingStage.season_id == season_id)
                .order_by(KickoffKingStage.stage_date.asc())
            )
            .scalars()
            .all()
        )

    def list_standings(self, season_id: int, limit: int = 50) -> List[KickoffKingStanding]:
        """积分榜。"""
        return list(
            self.db.execute(
                select(KickoffKingStanding)
                .where(KickoffKingStanding.season_id == season_id)
                .order_by(KickoffKingStanding.rank_position.asc())
                .limit(limit)
            )
            .scalars()
            .all()
        )

    # ---------------- 内部 ----------------

    def _current_season(self) -> Optional[KickoffKingSeason]:
        """当前赛季：优先进行中，其次最新创建。"""
        row = (
            self.db.execute(
                select(KickoffKingSeason)
                .where(KickoffKingSeason.season_status == "ONGOING")
                .order_by(KickoffKingSeason.create_time.desc())
                .limit(1)
            )
            .scalars()
            .first()
        )
        if row is not None:
            return row
        return (
            self.db.execute(
                select(KickoffKingSeason).order_by(KickoffKingSeason.create_time.desc())
            )
            .scalars()
            .first()
        )

    @staticmethod
    def _season_dict(s: KickoffKingSeason) -> Dict[str, Any]:
        """赛季序列化。"""
        return {
            "id": s.id,
            "seasonCode": s.season_code,
            "seasonName": s.season_name,
            "heroTitle": s.hero_title,
            "heroSubtitle": s.hero_subtitle,
            "seasonStatus": s.season_status,
            "totalStops": s.total_stops,
            "completedStops": s.completed_stops,
            "currentStopName": s.current_stop_name,
            "seasonStart": s.season_start.strftime("%Y-%m-%d") if s.season_start else None,
            "seasonEnd": s.season_end.strftime("%Y-%m-%d") if s.season_end else None,
            "announcement": s.announcement,
            "highlightTags": s.highlight_tags,
        }

    @staticmethod
    def _stage_dict(s: KickoffKingStage) -> Dict[str, Any]:
        """分站序列化。"""
        return {
            "id": s.id,
            "seasonId": s.season_id,
            "stageName": s.stage_name,
            "cityName": s.city_name,
            "stageType": s.stage_type,
            "stageStatus": s.stage_status,
            "stageDate": s.stage_date.strftime("%Y-%m-%d") if s.stage_date else None,
            "theme": s.theme,
            "focusSkill": s.focus_skill,
            "highlightDesc": s.highlight_desc,
        }

    @staticmethod
    def _standing_dict(s: KickoffKingStanding) -> Dict[str, Any]:
        """积分榜序列化。"""
        return {
            "id": s.id,
            "seasonId": s.season_id,
            "rankPosition": s.rank_position,
            "playerName": s.player_name,
            "associationName": s.association_name,
            "points": s.points,
            "stageWins": s.stage_wins,
            "podiumCount": s.podium_count,
            "serveStyle": s.serve_style,
            "hotForm": s.hot_form,
        }
