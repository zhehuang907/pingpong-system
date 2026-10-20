"""认证与安全测试。

对齐 Java ``CredentialPolicyUtilTest``、``JwtInterceptorTest``。
"""

from __future__ import annotations

from app.core.security import (
    generate_token,
    get_user_id_from_token,
    get_username_from_token,
    hash_password,
    resolve_token,
    validate_password,
    validate_token,
    validate_username,
    verify_password,
)


def test_password_hash_and_verify():
    """BCrypt 哈希与校验往返一致，且不同盐值产生不同哈希。"""
    hashed = hash_password("test1234")
    assert hashed != "test1234"
    assert verify_password("test1234", hashed)
    assert not verify_password("wrong", hashed)
    assert hash_password("test1234") != hashed, "BCrypt 每次应生成不同盐值"


def test_password_policy():
    """密码策略：6-20 位且同时含字母与数字。"""
    assert validate_password("test1234")
    assert validate_password("Abc123")
    assert not validate_password("123456"), "纯数字不合规"
    assert not validate_password("abcdef"), "纯字母不合规"
    assert not validate_password("a1"), "长度不足"
    assert not validate_password("a" * 21 + "1"), "长度超限"


def test_username_policy():
    """用户名策略：3-20 位字母、数字、下划线。"""
    assert validate_username("tester01")
    assert validate_username("test_user")
    assert not validate_username("ab"), "长度不足"
    assert not validate_username("test-user"), "含非法字符"
    assert not validate_username("用户名"), "非 ASCII"


def test_jwt_roundtrip():
    """JWT 签发后可解析出 userId 与 username。"""
    token = generate_token(42, "alice")
    assert validate_token(token)
    assert get_user_id_from_token(token) == 42
    assert get_username_from_token(token) == "alice"


def test_jwt_invalid():
    """被篡改或过期的 token 应校验失败。"""
    assert not validate_token("not-a-jwt")
    assert get_user_id_from_token("bad.token.value") is None

    expired = generate_token(1, "bob", expire_seconds=-10)
    assert not validate_token(expired)


def test_resolve_token():
    """Authorization 头解析：兼容带/不带 Bearer 前缀。"""
    assert resolve_token("Bearer abc.def") == "abc.def"
    assert resolve_token("bearer abc.def") == "abc.def"
    assert resolve_token("abc.def") == "abc.def"
    assert resolve_token(None) is None
    assert resolve_token("") is None
