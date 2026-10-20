"""用户与认证服务。

由 Java ``UserServiceImpl`` 移植：注册、登录、资料维护、密码修改、
用户名/邮箱查重、偏好画像读写。

密码使用 BCrypt（与 Hutool BCrypt 同为标准 $2a$/$2b$ 格式），
**已有账号的密码哈希可直接复用，无需重置**。
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.core.exceptions import BusinessException
from app.core.redis_client import bind_user_token, revoke_user_token
from app.core.security import (
    generate_token,
    hash_password,
    validate_password,
    validate_username,
    verify_password,
)
from app.models.user import LoginAudit, User, UserPreferenceProfile
from app.schemas.user import (
    ChangePasswordIn,
    LoginIn,
    RegisterIn,
    UpdateProfileIn,
    UserPreferenceIn,
)


class UserService:
    """用户服务。"""

    def __init__(self, db: Session):
        self.db = db

    # ---------------- 查询 ----------------

    def get_by_id(self, user_id: int) -> Optional[User]:
        """按 ID 查用户（过滤逻辑删除）。"""
        return self.db.get(User, user_id)

    def get_by_username(self, username: str) -> Optional[User]:
        """按用户名查用户。"""
        return (
            self.db.execute(
                select(User).where(User.username == username, User.deleted == 0)
            )
            .scalars()
            .first()
        )

    def exists_username(self, username: str) -> bool:
        """用户名是否已占用。"""
        return self.get_by_username(username) is not None

    def exists_email(self, email: str) -> bool:
        """邮箱是否已占用。"""
        if not email:
            return False
        row = (
            self.db.execute(
                select(User).where(User.email == email, User.deleted == 0)
            )
            .scalars()
            .first()
        )
        return row is not None

    def count_users(self) -> int:
        """用户总数。"""
        return (
            self.db.execute(
                select(func.count()).select_from(User).where(User.deleted == 0)
            ).scalar()
            or 0
        )

    def list_users(self, keyword: str = "", page: int = 1, size: int = 20):
        """用户分页列表（后台管理）。"""
        stmt = select(User).where(User.deleted == 0)
        if keyword:
            like = f"%{keyword}%"
            stmt = stmt.where(
                or_(User.username.like(like), User.nickname.like(like), User.email.like(like))
            )
        total = self.db.execute(
            select(func.count()).select_from(stmt.subquery())
        ).scalar() or 0
        rows = (
            self.db.execute(
                stmt.order_by(User.create_time.desc())
                .offset((page - 1) * size)
                .limit(size)
            )
            .scalars()
            .all()
        )
        return rows, total

    # ---------------- 注册 / 登录 ----------------

    def register(self, payload: RegisterIn) -> User:
        """用户注册。

        校验用户名与密码策略、查重后写入，密码以 BCrypt 存储。
        """
        username = payload.username.strip()
        if not validate_username(username):
            raise BusinessException("用户名需为 3-20 位字母、数字或下划线")
        if not validate_password(payload.password):
            raise BusinessException("密码需为 6-20 位，且同时包含字母与数字")
        if self.exists_username(username):
            raise BusinessException("用户名已被注册")
        if payload.email and self.exists_email(payload.email):
            raise BusinessException("邮箱已被注册")

        user = User(
            username=username,
            password=hash_password(payload.password),
            email=payload.email,
            phone=payload.phone,
            nickname=payload.nickname or username,
            gender=0,
            level="初级",
            role="USER",
            status=1,
            deleted=0,
        )
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        return user

    def login(
        self,
        payload: LoginIn,
        ip: str = "",
        user_agent: str = "",
    ) -> tuple[str, int, User]:
        """用户登录。

        Returns:
            (token, 过期秒数, 用户实体)

        Raises:
            BusinessException: 账号不存在、密码错误或账号被禁用。
        """
        user = self.get_by_username(payload.username.strip())
        success = user is not None and verify_password(payload.password, user.password)

        self._audit(
            user_id=user.id if user else None,
            username=payload.username,
            success=1 if success else 0,
            ip=ip,
            user_agent=user_agent,
        )

        if user is None:
            raise BusinessException("用户不存在")
        if not success:
            raise BusinessException("密码错误")
        if user.status == 0:
            raise BusinessException("账号已被禁用")

        user.last_login_time = datetime.now()
        self.db.commit()

        token = generate_token(user.id, user.username)
        bind_user_token(user.id, token)
        return token, 86400, user

    def logout(self, user_id: int) -> None:
        """登出：清除服务端 token，使旧 token 立即失效。"""
        revoke_user_token(user_id)

    def change_password(
        self, user_id: int, payload: ChangePasswordIn, require_old: bool = True
    ) -> None:
        """修改密码。

        Args:
            require_old: 为 True 时校验原密码；找回密码场景传 False。
        """
        user = self.get_by_id(user_id)
        if user is None:
            raise BusinessException("用户不存在")

        if require_old:
            if not payload.old_password:
                raise BusinessException("原密码不能为空")
            if not verify_password(payload.old_password, user.password):
                raise BusinessException("原密码不正确")

        if not validate_password(payload.new_password):
            raise BusinessException("新密码需为 6-20 位，且同时包含字母与数字")

        user.password = hash_password(payload.new_password)
        self.db.commit()
        revoke_user_token(user_id)

    def update_profile(self, user_id: int, payload: UpdateProfileIn) -> User:
        """更新个人资料。"""
        user = self.get_by_id(user_id)
        if user is None:
            raise BusinessException("用户不存在")

        for field in ("nickname", "email", "phone", "avatar", "gender", "level", "play_style"):
            value = getattr(payload, field, None)
            if value is not None:
                setattr(user, field, value)

        self.db.commit()
        self.db.refresh(user)
        return user

    def set_status(self, user_id: int, status: int) -> User:
        """启用/禁用账号（管理员）。"""
        user = self.get_by_id(user_id)
        if user is None:
            raise BusinessException("用户不存在")
        user.status = 1 if status else 0
        self.db.commit()
        if user.status == 0:
            revoke_user_token(user_id)
        return user

    # ---------------- 偏好画像 ----------------

    def get_preference(self, user_id: int) -> Optional[UserPreferenceProfile]:
        """读取偏好画像。"""
        return (
            self.db.execute(
                select(UserPreferenceProfile).where(
                    UserPreferenceProfile.user_id == user_id,
                    UserPreferenceProfile.deleted == 0,
                )
            )
            .scalars()
            .first()
        )

    def save_preference(self, user_id: int, payload: UserPreferenceIn) -> UserPreferenceProfile:
        """保存偏好画像（适配度问答结果），存在则更新。"""
        profile = self.get_preference(user_id)
        if profile is None:
            profile = UserPreferenceProfile(user_id=user_id, deleted=0)
            self.db.add(profile)

        for field in (
            "level",
            "play_style",
            "grip_style",
            "budget_min",
            "budget_max",
            "weekly_frequency",
            "primary_goal",
            "priority_focus",
            "preferred_categories",
            "preferred_brands",
            "answer_snapshot",
            "preference_tags",
            "updated_source",
        ):
            value = getattr(payload, field, None)
            if value is not None:
                setattr(profile, field, value)

        self.db.commit()
        self.db.refresh(profile)
        return profile

    # ---------------- 内部 ----------------

    def _audit(
        self,
        user_id: Optional[int],
        username: str,
        success: int,
        ip: str,
        user_agent: str,
    ) -> None:
        """写入登录审计日志（对应 login_audit 表）。"""
        try:
            row = LoginAudit(
                user_id=user_id,
                username=username,
                success=success,
                ip_address=ip[:64] if ip else None,
                user_agent=user_agent[:255] if user_agent else None,
                risk_level="LOW" if success else "MEDIUM",
                deleted=0,
            )
            self.db.add(row)
            self.db.commit()
        except Exception:
            self.db.rollback()
