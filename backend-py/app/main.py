"""FastAPI 应用入口。

对应 Java 侧 ``PingpongRecommendationApplication`` + ``WebConfig``：
统一 ``/api`` 前缀、UTF-8 编码、CORS、静态资源挂载、全局异常处理。
"""

from __future__ import annotations

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api import build_api_router
from app.core.config import settings
from app.core.exceptions import register_exception_handlers
from app.core.result import ResultJSONResponse, ok
from app.core.redis_client import is_available
from app.db.session import check_connection, dispose_engine, ensure_engine
from app.middleware import JwtAuthMiddleware, RateLimitMiddleware

DEFAULT_RESPONSE_CLASS = ResultJSONResponse


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动自检，退出释放连接。"""
    # 使用 ensure_engine：若测试已注入内存库则不覆盖
    ensure_engine()
    db_ok = check_connection()
    redis_ok = is_available()

    app.state.db_available = db_ok
    app.state.redis_available = redis_ok

    print("=" * 60)
    print(f"{settings.app_name} 启动完成")
    print(f"  监听地址 : http://{settings.host}:{settings.port}{settings.api_prefix}")
    print(f"  数据库   : {'已连接' if db_ok else '未连接（接口将返回空数据）'}")
    print(f"  Redis    : {'已连接' if redis_ok else '未连接（已降级为无缓存/无限流）'}")
    print("=" * 60)

    if settings.scheduler_enabled:
        from app.scheduler.tasks import start_scheduler

        start_scheduler()

    yield

    if settings.scheduler_enabled:
        from app.scheduler.tasks import stop_scheduler

        stop_scheduler()
    dispose_engine()


def create_app() -> FastAPI:
    """构建 FastAPI 应用。"""
    app = FastAPI(
        title="乒乓球装备推荐系统 API",
        description=(
            "由 Spring Boot 后端迁移至 FastAPI 的 Python 实现。"
            "接口路径、请求/响应结构与原 Java 后端保持一致。"
        ),
        version="1.0.0",
        lifespan=lifespan,
        default_response_class=ResultJSONResponse,
        docs_url="/api/docs",
        redoc_url="/api/redoc",
        openapi_url="/api/openapi.json",
    )

    # CORS：对齐前端 dev server 地址
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 中间件顺序：限流 → 鉴权（对应 Spring 拦截器注册顺序）
    app.add_middleware(RateLimitMiddleware)
    app.add_middleware(JwtAuthMiddleware)

    register_exception_handlers(app)

    # 业务路由（统一 /api 前缀，对齐 Java context-path）
    app.include_router(build_api_router(), prefix=settings.api_prefix)

    # 健康检查（不受 /api 前缀约束，便于容器探针）
    @app.get("/health", tags=["系统"])
    def health():
        """健康检查：返回数据库与 Redis 连通状态。"""
        return ok(
            {
                "status": "UP",
                "database": "UP" if getattr(app.state, "db_available", False) else "DOWN",
                "redis": "UP" if getattr(app.state, "redis_available", False) else "DOWN",
            }
        )

    # 静态资源：上传目录（对应 Java 的 /api/upload/** 与 /uploads/**）
    upload_dir = settings.upload_dir
    os.makedirs(upload_dir, exist_ok=True)
    try:
        app.mount("/uploads", StaticFiles(directory=upload_dir), name="uploads")
        app.mount(
            f"{settings.api_prefix}/upload",
            StaticFiles(directory=upload_dir),
            name="api-uploads",
        )
    except RuntimeError:
        # 目录不存在时跳过挂载，不影响主流程
        pass

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
    )
