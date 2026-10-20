"""pytest 公共夹具。

测试使用 SQLite 内存库，无需外部 MySQL/Redis 即可运行，
重点验证业务逻辑与算法正确性，而非数据库方言差异。
"""

from __future__ import annotations

import os
import sys
from typing import Generator

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 测试环境禁用 Redis（避免依赖外部服务，走降级分支）
os.environ.setdefault("REDIS_ENABLED", "false")
os.environ.setdefault("RATE_LIMIT_ENABLED", "false")
os.environ.setdefault("SCHEDULER_ENABLED", "false")

from fastapi.testclient import TestClient  # noqa: E402
from sqlalchemy import create_engine  # noqa: E402
from sqlalchemy.orm import Session, sessionmaker  # noqa: E402
from sqlalchemy.pool import StaticPool  # noqa: E402

from app.core.config import settings  # noqa: E402
from app.db.session import dispose_engine, init_engine  # noqa: E402
from app.main import create_app  # noqa: E402
from app.models import Base  # noqa: E402


@pytest.fixture(scope="function")
def engine():
    """每个用例独立的 SQLite 内存引擎。"""
    eng = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(eng)
    yield eng
    eng.dispose()


@pytest.fixture(scope="function")
def db(engine) -> Generator[Session, None, None]:
    """数据库会话。"""
    factory = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
    session = factory()
    yield session
    session.close()


@pytest.fixture(scope="function")
def client(engine):
    """API 测试客户端，绑定到内存数据库。"""
    from app.db import session as db_session

    # 先建表，再把应用会话工厂指向该内存库。
    # 应用启动时会调用 ensure_engine()，因引擎已存在而不会覆盖此处设置。
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
    db_session.init_engine(url="sqlite:///:memory:", echo=False)
    db_session._session_factory = factory

    app = create_app()
    with TestClient(app) as c:
        yield c

    dispose_engine()


@pytest.fixture(scope="function")
def seed_equipment(engine):
    """预置一条可用装备，供依赖装备 ID 的用例使用。

    返回装备 ID。种子数据与用例运行在同一个 SQLite 内存库上（StaticPool），
    因此 API 会话可见。
    """
    from app.models.equipment import Category, Equipment

    with sessionmaker(bind=engine, expire_on_commit=False)() as s:
        category = Category(name="底板", level=1, parent_id=0, sort=1, status=1, deleted=0)
        s.add(category)
        s.flush()
        equipment = Equipment(
            name="测试底板",
            category_id=category.id,
            brand="TestBrand",
            price=299.00,
            stock=10,
            description="用于测试的装备",
            suitable_for="全面型",
            tags="进攻,全面",
            rating=4.5,
            status=1,
            deleted=0,
        )
        s.add(equipment)
        s.commit()
        return equipment.id


@pytest.fixture
def auth_headers(client):
    """注册并登录，返回可用的认证请求头。"""
    client.post(
        f"{settings.api_prefix}/user/register",
        json={"username": "tester01", "password": "test1234"},
    )
    resp = client.post(
        f"{settings.api_prefix}/user/login",
        json={"username": "tester01", "password": "test1234"},
    )
    token = resp.json()["data"]["token"]
    return {"Authorization": f"Bearer {token}"}
