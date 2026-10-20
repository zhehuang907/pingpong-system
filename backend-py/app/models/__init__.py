"""ORM 模型汇总导出。

共 27 张表，与原 Java 实体一一对应。
导入本模块可确保所有模型注册到 ``Base.metadata``，
供建表（测试）与迁移工具使用。
"""

from app.db.base import Base
from app.models.ai import AiChatMessage, AiChatSession
from app.models.equipment import (
    Category,
    Equipment,
    EquipmentCombination,
    EquipmentTag,
    PlayerEquipmentRelation,
    StarPlayer,
    Tag,
)
from app.models.interaction import (
    BehaviorLog,
    BrowseHistory,
    CartItem,
    Comment,
    CommentLike,
    Favorite,
    RecommendRecord,
)
from app.models.kickoff import (
    KickoffKingSeason,
    KickoffKingStage,
    KickoffKingStanding,
)
from app.models.leaderboard import (
    LeaderboardShareStat,
    LeaderboardSnapshot,
    LeaderboardSubscription,
    ScheduleJobLock,
)
from app.models.system import SystemConfig
from app.models.user import LoginAudit, User, UserPreferenceProfile

__all__ = [
    "Base",
    "User",
    "LoginAudit",
    "UserPreferenceProfile",
    "Category",
    "Tag",
    "Equipment",
    "EquipmentTag",
    "EquipmentCombination",
    "StarPlayer",
    "PlayerEquipmentRelation",
    "Favorite",
    "Comment",
    "CommentLike",
    "BrowseHistory",
    "CartItem",
    "RecommendRecord",
    "BehaviorLog",
    "LeaderboardSnapshot",
    "LeaderboardSubscription",
    "LeaderboardShareStat",
    "ScheduleJobLock",
    "KickoffKingSeason",
    "KickoffKingStage",
    "KickoffKingStanding",
    "AiChatSession",
    "AiChatMessage",
    "SystemConfig",
]
