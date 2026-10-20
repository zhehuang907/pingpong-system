"""认证与密码安全。

对齐 Java 侧实现：
- 密码：Hutool ``BCrypt.hashpw`` / ``BCrypt.checkpw`` → Python ``bcrypt``
  （同为 $2a$/$2b$ 标准格式，已有账号密码哈希可直接复用，无需重置）
- Token：jjwt HS512，claims 含 userId / username，subject=username
"""

from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional

import bcrypt
import jwt

from app.core.config import settings

# ============================================================
# 密码：BCrypt
# ============================================================


def hash_password(plain: str) -> str:
    """生成 BCrypt 密码哈希，与 Hutool BCrypt.hashpw 兼容。"""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(plain.encode("utf-8"), salt).decode("utf-8")


def verify_password(plain: str, hashed: str) -> bool:
    """校验密码，与 Hutool BCrypt.checkpw 兼容。

    哈希格式异常（如历史脏数据）时返回 False 而非抛异常。
    """
    if not plain or not hashed:
        return False
    try:
        return bcrypt.checkpw(plain.encode("utf-8"), hashed.encode("utf-8"))
    except (ValueError, TypeError):
        return False


# ============================================================
# 凭证策略：对应 CredentialPolicyUtil
# ============================================================

USERNAME_PATTERN = re.compile(r"^[a-zA-Z0-9_]{3,20}$")
# 至少 6 位，且同时包含字母与数字
PASSWORD_PATTERN = re.compile(r"^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$")


def validate_username(username: str) -> bool:
    """用户名：3-20 位字母、数字、下划线。"""
    return bool(username and USERNAME_PATTERN.match(username))


def validate_password(password: str) -> bool:
    """密码：6-20 位，须同时含字母与数字。"""
    return bool(password and PASSWORD_PATTERN.match(password))


# ============================================================
# JWT：HS512
# ============================================================


def generate_token(user_id: int, username: str, expire_seconds: Optional[int] = None) -> str:
    """签发 JWT。

    claims 结构与 Java 保持一致：
    ``{"userId": ..., "username": ..., "sub": username, "iat": ..., "exp": ...}``
    """
    expire = expire_seconds if expire_seconds is not None else settings.jwt_expire_time
    now = datetime.now(tz=timezone.utc)
    payload: Dict[str, Any] = {
        "userId": user_id,
        "username": username,
        "sub": username,
        "iat": now,
        "exp": now + timedelta(seconds=expire),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def parse_token(token: str) -> Dict[str, Any]:
    """解析并校验签名与过期时间，失败抛出 jwt.PyJWTError。"""
    return jwt.decode(
        token,
        settings.jwt_secret,
        algorithms=[settings.jwt_algorithm],
    )


def validate_token(token: str) -> bool:
    """仅判断 token 是否有效（对应 JwtUtil.validateToken）。"""
    try:
        parse_token(token)
        return True
    except jwt.PyJWTError:
        return False


def get_user_id_from_token(token: str) -> Optional[int]:
    """从 token 提取 userId，兼容数字与字符串两种载荷。"""
    try:
        claims = parse_token(token)
    except jwt.PyJWTError:
        return None
    raw = claims.get("userId")
    if raw is None:
        return None
    try:
        return int(raw)
    except (TypeError, ValueError):
        return None


def get_username_from_token(token: str) -> Optional[str]:
    """从 token 提取 username（subject）。"""
    try:
        return parse_token(token).get("sub")
    except jwt.PyJWTError:
        return None


def resolve_token(authorization: Optional[str]) -> Optional[str]:
    """从 Authorization 头提取裸 token，兼容带/不带 Bearer 前缀两种写法。

    对应 JwtInterceptor.resolveToken。
    """
    if not authorization:
        return None
    value = authorization.strip()
    prefix = settings.jwt_prefix
    if value.lower().startswith(prefix.lower() + " "):
        return value[len(prefix) + 1 :].strip()
    if value.lower().startswith(prefix.lower()):
        return value[len(prefix) :].strip()
    return value
