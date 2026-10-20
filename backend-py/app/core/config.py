"""全局配置中心。

对应 Java 侧的 ``application.yml``，所有可调参数集中在此，
行为权重、相似度权重、缓存时长等均可在 .env 中覆盖而无需改代码。
"""

from __future__ import annotations

from functools import lru_cache
from typing import List

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用配置。环境变量优先级高于 .env 文件。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # ---------------- 服务 ----------------
    app_name: str = "pingpong-recommendation"
    host: str = "0.0.0.0"
    port: int = 8081
    # 对齐 Java 的 server.servlet.context-path: /api
    api_prefix: str = "/api"
    debug: bool = True

    # ---------------- 数据库 ----------------
    db_host: str = "127.0.0.1"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = ""
    db_name: str = "pingpong_recommendation"
    db_pool_size: int = 20
    db_max_overflow: int = 10
    db_pool_recycle: int = 1800
    db_echo: bool = False

    @property
    def database_url(self) -> str:
        """SQLAlchemy 异步无关的连接串（使用 PyMySQL 同步驱动）。"""
        return (
            f"mysql+pymysql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}?charset=utf8mb4"
        )

    # ---------------- Redis ----------------
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""
    redis_db: int = 0
    redis_enabled: bool = True

    # ---------------- JWT ----------------
    # 与 Java 侧的 jwt.secret 使用同一环境变量，保证已签发 token 可跨语言互认
    jwt_secret: str = "dev-only-secret-change-me"
    jwt_expire_time: int = 86400
    jwt_header: str = "Authorization"
    jwt_prefix: str = "Bearer"
    jwt_algorithm: str = "HS512"

    # ---------------- 限流 ----------------
    rate_limit_enabled: bool = True
    rate_limit_window_seconds: int = 60
    rate_limit_max_requests: int = 120
    rate_limit_sensitive_window_seconds: int = 60
    rate_limit_sensitive_max_requests: int = 12

    # ---------------- 推荐：热门 ----------------
    recommend_hot_count: int = 10
    recommend_hot_view_threshold: int = 100

    # ---------------- 推荐：个性化 ----------------
    recommend_personal_count: int = 20
    weight_browse: float = 1.0
    weight_favorite: float = 3.0
    weight_comment: float = 5.0
    weight_purchase: float = 10.0
    # 单用户最多纳入多少条高权交互装备作为兴趣锚点
    recommend_anchor_limit: int = 20
    # 候选池上限，控制打分计算量
    recommend_candidate_limit: int = 300

    # ---------------- 推荐：相似 ----------------
    recommend_similar_count: int = 5
    weight_tag: float = 0.6
    weight_category: float = 0.3
    weight_price: float = 0.1
    # 相似度硬阈值，低于该值不进入结果
    similar_threshold: float = 0.3

    # ---------------- 缓存时长（秒） ----------------
    cache_personal_ttl: int = 1800  # 30 分钟
    cache_similar_ttl: int = 3600  # 1 小时
    cache_hot_ttl: int = 3600  # 1 小时

    # ---------------- 上传 ----------------
    upload_dir: str = "./uploads"
    upload_max_size: int = 5 * 1024 * 1024
    allowed_image_exts: List[str] = Field(
        default_factory=lambda: [".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"]
    )

    # ---------------- AI（DeepSeek） ----------------
    deepseek_enabled: bool = True
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_chat_path: str = "/chat/completions"
    deepseek_api_key: str = ""
    deepseek_model: str = "deepseek-chat"
    deepseek_temperature: float = 0.6
    deepseek_max_output_tokens: int = 900
    deepseek_timeout_millis: int = 90000
    deepseek_context_message_limit: int = 8
    deepseek_catalog_limit: int = 4

    # ---------------- 定时任务 ----------------
    scheduler_enabled: bool = False
    # 每日 02:00 全量重算榜单
    leaderboard_full_cron: dict = Field(
        default_factory=lambda: {"hour": 2, "minute": 0}
    )

    # ---------------- CORS ----------------
    cors_origins: List[str] = Field(
        default_factory=lambda: ["http://127.0.0.1:3000", "http://localhost:3000"]
    )


@lru_cache
def get_settings() -> Settings:
    """获取单例配置对象。"""
    return Settings()


settings = get_settings()
