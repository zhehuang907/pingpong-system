"""数据库会话管理。

对应 Spring Boot 的 HikariCP 连接池配置：
``maximum-pool-size: 20 / minimum-idle: 5 / max-lifetime: 1800000``。
"""

from __future__ import annotations

from collections.abc import Generator
from contextlib import contextmanager
from typing import Any, Optional

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.config import settings

_engine: Optional[Engine] = None
_session_factory: Optional[sessionmaker] = None


def init_engine(echo: Optional[bool] = None, url: Optional[str] = None) -> Engine:
    """创建（或重建）数据库引擎。

    Args:
        echo: 是否输出 SQL，默认取配置 ``db_echo``。
        url: 自定义连接串，测试时可指向 SQLite。

    Returns:
        SQLAlchemy Engine 实例。
    """
    global _engine, _session_factory

    target_url = url or settings.database_url
    use_echo = settings.db_echo if echo is None else echo

    # SQLite（测试用）不支持池参数
    if target_url.startswith("sqlite"):
        engine = create_engine(
            target_url,
            echo=use_echo,
            future=True,
            connect_args={"check_same_thread": False},
        )
    else:
        engine = create_engine(
            target_url,
            echo=use_echo,
            future=True,
            pool_size=settings.db_pool_size,
            max_overflow=settings.db_max_overflow,
            pool_recycle=settings.db_pool_recycle,
            pool_pre_ping=True,
        )

    _engine = engine
    _session_factory = sessionmaker(
        bind=engine, autoflush=False, autocommit=False, expire_on_commit=False
    )
    return engine


def get_engine() -> Engine:
    """获取引擎，未初始化时惰性创建。"""
    global _engine
    if _engine is None:
        init_engine()
    assert _engine is not None
    return _engine


def get_session_factory() -> sessionmaker:
    """获取会话工厂。"""
    global _session_factory
    if _session_factory is None:
        init_engine()
    assert _session_factory is not None
    return _session_factory


def get_db() -> Generator[Session, None, None]:
    """FastAPI 依赖：请求级数据库会话。

    用法：``db: Session = Depends(get_db)``
    请求结束自动关闭，异常时回滚。
    """
    db = get_session_factory()()
    try:
        yield db
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


@contextmanager
def session_scope() -> Generator[Session, None, None]:
    """独立事务上下文，供定时任务等非请求场景使用。"""
    db = get_session_factory()()
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def check_connection() -> bool:
    """连通性探测，用于启动自检。"""
    try:
        with get_engine().connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except Exception:
        return False


def dispose_engine() -> None:
    """释放连接池，测试 tearDown 与进程退出时调用。"""
    global _engine, _session_factory
    if _engine is not None:
        _engine.dispose()
    _engine = None
    _session_factory = None


def ensure_engine() -> Engine:
    """确保引擎已初始化；已存在时直接复用。

    与 `init_engine` 的区别：本函数**不会覆盖**既有引擎，
    供应用启动（lifespan）使用，避免测试注入的内存库被默认配置覆盖。
    """
    if _engine is None:
        init_engine()
    return get_engine()


def _set_sqlite_pragmas(dbapi_conn: Any, conn_record: Any) -> None:
    """仅在 SQLite 下启用外键约束。

    必须判断方言：该监听对所有 Engine 生效，
    无条件执行 PRAGMA 会导致 MySQL 报语法错误。
    """
    dialect = conn_record.info.get("dialect") if conn_record is not None else None
    if dialect is not None and getattr(dialect, "name", "") != "sqlite":
        return
    try:
        cursor = dbapi_conn.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()
    except Exception:
        # 非 SQLite 驱动无此语法，忽略
        pass


event.listens_for(Engine, "connect")(_set_sqlite_pragmas)
