from datetime import datetime, timedelta, timezone

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import InvalidHashError, VerifyMismatchError
from fastapi import Depends, Header
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.config import Settings, get_settings
from app.database import get_db
from app.exceptions import AppError
from app.models import StaffUser
from app.schemas import LoginRequest, TokenResponse

password_hasher = PasswordHasher()
LOGIN_MAX_ATTEMPTS = 5
LOGIN_LOCK_MINUTES = 10


def hash_password(password: str) -> str:
    return password_hasher.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    try:
        return password_hasher.verify(password_hash, password)
    except (VerifyMismatchError, InvalidHashError):
        return False


def create_token(staff: StaffUser, settings: Settings) -> TokenResponse:
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.jwt_expire_minutes)
    payload = {
        "sub": str(staff.staff_id),
        "username": staff.username,
        "role": staff.role,
        "exp": expire,
    }
    token = jwt.encode(payload, settings.jwt_secret, algorithm="HS256")
    return TokenResponse(
        access_token=token,
        expires_in=settings.jwt_expire_minutes * 60,
        username=staff.username,
        role=staff.role,
    )


def login(body: LoginRequest, db: Session, settings: Settings | None = None) -> TokenResponse:
    settings = settings or get_settings()
    identifier = body.username.strip()
    staff = db.scalar(
        select(StaffUser).where(
            or_(StaffUser.username == identifier, StaffUser.email == identifier)
        )
    )

    now = datetime.now(timezone.utc).replace(tzinfo=None)
    if staff and staff.locked_until and staff.locked_until > now:
        raise AppError(429, "Too many failed login attempts. Try again later.")

    valid = bool(
        staff
        and staff.is_active
        and verify_password(body.password, staff.password_hash)
    )
    if not valid:
        if staff:
            staff.failed_login_attempts = (staff.failed_login_attempts or 0) + 1
            if staff.failed_login_attempts >= LOGIN_MAX_ATTEMPTS:
                staff.failed_login_attempts = 0
                staff.locked_until = now + timedelta(minutes=LOGIN_LOCK_MINUTES)
            db.commit()
        raise AppError(401, "Invalid username or password")

    staff.failed_login_attempts = 0
    staff.locked_until = None
    db.commit()
    return create_token(staff, settings)


def get_current_user(
    authorization: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
    db: Session = Depends(get_db),
) -> str:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise AppError(401, "Sign in required")
    token = authorization.split(" ", 1)[1].strip()
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    except jwt.ExpiredSignatureError as exc:
        raise AppError(401, "Token expired") from exc
    except jwt.InvalidTokenError as exc:
        raise AppError(401, "Invalid token") from exc

    try:
        staff_id = int(payload.get("sub", ""))
    except (TypeError, ValueError) as exc:
        raise AppError(401, "Invalid token") from exc

    staff = db.get(StaffUser, staff_id)
    if not staff or not staff.is_active:
        raise AppError(401, "Account is inactive or no longer exists")
    return staff.username
