"""Redis 客户端封装。

对应 Spring 的 ``RedisTemplate``，用于三处：
1. 推荐结果缓存
2. JWT token 服务端校验（单端登录互斥 + 自动续期）
3. 接口限流计数

Redis 不可用时**降级为空操作**而非抛错——与 Java 侧
"Redis 不可用时退化为纯 JWT 校验" 的容错策略保持一致。
"""

from __future__ import annotations

import json
from typing import Any, Optional

from app.core.config import settings

_client = None
_available: Optional[bool] = None


def get_redis():
    """获取 Redis 客户端，不可用时返回 None。"""
    global _client, _available

    if not settings.redis_enabled:
        return None
    if _available is False:
        return None
    if _client is not None:
        return _client

    try:
        import redis

        _client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            db=settings.redis_db,
            decode_responses=True,
            socket_connect_timeout=2,
            socket_timeout=2,
        )
        _client.ping()
        _available = True
        return _client
    except Exception:
        _available = False
        _client = None
        return None


def is_available() -> bool:
    """Redis 是否可用。"""
    return get_redis() is not None


# ============================================================
# 通用读写
# ============================================================


def set_value(key: str, value: str, ttl: int | None = None) -> bool:
    """写入字符串并设置过期时间。"""
    r = get_redis()
    if r is None:
        return False
    try:
        r.set(key, value, ex=ttl)
        return True
    except Exception:
        return False


def get_value(key: str) -> Optional[str]:
    """读取字符串。"""
    r = get_redis()
    if r is None:
        return None
    try:
        return r.get(key)
    except Exception:
        return None


def delete(key: str) -> bool:
    """删除键。"""
    r = get_redis()
    if r is None:
        return False
    try:
        r.delete(key)
        return True
    except Exception:
        return False


def expire(key: str, ttl: int) -> bool:
    """续期。"""
    r = get_redis()
    if r is None:
        return False
    try:
        r.expire(key, ttl)
        return True
    except Exception:
        return False


def incr(key: str, ttl: int | None = None) -> int:
    """自增计数，返回自增后的值；Redis 不可用时返回 1（放行）。"""
    r = get_redis()
    if r is None:
        return 1
    try:
        value = r.incr(key)
        if value == 1 and ttl:
            r.expire(key, ttl)
        return int(value)
    except Exception:
        return 1


# ============================================================
# JSON 缓存（推荐结果）
# ============================================================


def set_json(key: str, value: Any, ttl: int) -> bool:
    """序列化写入 JSON。"""
    return set_value(key, json.dumps(value, ensure_ascii=False, default=str), ttl)


def get_json(key: str) -> Any:
    """读取并反序列化 JSON，缺失时返回 None。"""
    raw = get_value(key)
    if raw is None:
        return None
    try:
        return json.loads(raw)
    except Exception:
        return None


# ============================================================
# Token 校验：对齐 JwtInterceptor.validateTokenWithRedis
# ============================================================

TOKEN_KEY_PREFIX = "user:token:"


def get_user_token(user_id: int) -> Optional[str]:
    """读取服务端保存的 token。"""
    return get_value(f"{TOKEN_KEY_PREFIX}{user_id}")


def bind_user_token(user_id: int, token: str, ttl: int | None = None) -> bool:
    """登录成功后登记 token，实现单端登录互斥。"""
    return set_value(
        f"{TOKEN_KEY_PREFIX}{user_id}", token, ttl or settings.jwt_expire_time
    )


def refresh_user_token(user_id: int, token: str, ttl: int | None = None) -> bool:
    """校验通过后续期，实现滑动过期。"""
    stored = get_user_token(user_id)
    if stored is not None and stored != token:
        return False
    if stored is None:
        return True
    return expire(f"{TOKEN_KEY_PREFIX}{user_id}", ttl or settings.jwt_expire_time)


def revoke_user_token(user_id: int) -> bool:
    """登出时清除服务端 token。"""
    return delete(f"{TOKEN_KEY_PREFIX}{user_id}")
