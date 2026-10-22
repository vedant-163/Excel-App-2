"""Email-based staff password recovery. Tokens are hashed at rest and expire quickly."""
from datetime import datetime, timedelta
import hashlib
import hmac
import secrets
import smtplib
from email.message import EmailMessage

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.auth import hash_password
from app.config import Settings, get_settings
from app.exceptions import AppError
from app.models import StaffPasswordReset, StaffUser

GENERIC_MESSAGE = "If an active staff account exists for that email, a recovery code has been sent."


def _code_digest(code: str, settings: Settings) -> str:
    # Keyed digest prevents a database-only attacker from cheaply brute-forcing 6-digit codes.
    return hmac.new(settings.jwt_secret.encode("utf-8"), code.encode("utf-8"), hashlib.sha256).hexdigest()


def _smtp_configured(settings: Settings) -> bool:
    return all((settings.smtp_host, settings.smtp_username, settings.smtp_password, settings.smtp_from_email))


def _send_code(settings: Settings, recipient: str, code: str) -> None:
    message = EmailMessage()
    message["Subject"] = "SGSITS Student Management - Password Reset Code"
    message["From"] = settings.smtp_from_email
    message["To"] = recipient
    message.set_content(
        f"Your SGSITS Student Management password reset code is: {code}\n\n"
        f"It expires in {settings.password_reset_expiry_minutes} minutes.\n"
        "If you did not request this, ignore this email. Do not share this code."
    )
    try:
        with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10) as server:
            if settings.smtp_use_tls:
                server.starttls()
            server.login(settings.smtp_username, settings.smtp_password)
            server.send_message(message)
    except Exception as exc:
        raise AppError(503, "Password recovery email could not be sent. Contact the college administrator.") from exc


def request_password_reset(email: str, db: Session, settings: Settings | None = None) -> dict:
    settings = settings or get_settings()
    if not _smtp_configured(settings):
        raise AppError(503, "Password recovery email is not configured. Contact the college administrator.")

    normalized_email = email.strip().lower()
    staff = db.scalar(select(StaffUser).where(StaffUser.email == normalized_email, StaffUser.is_active.is_(True)))
    # Avoid account enumeration: unknown emails receive the same successful response.
    if not staff:
        return {"success": True, "message": GENERIC_MESSAGE}

    now = datetime.utcnow()
    # Cool down repeated requests for the same account to reduce email abuse.
    recent_request = db.scalar(
        select(StaffPasswordReset).where(
            StaffPasswordReset.staff_id == staff.staff_id,
            StaffPasswordReset.created_at > now - timedelta(seconds=60),
        ).order_by(StaffPasswordReset.created_at.desc())
    )
    if recent_request:
        return {"success": True, "message": GENERIC_MESSAGE}

    db.execute(
        update(StaffPasswordReset)
        .where(StaffPasswordReset.staff_id == staff.staff_id, StaffPasswordReset.used_at.is_(None))
        .values(used_at=now)
    )
    code = f"{secrets.randbelow(1_000_000):06d}"
    record = StaffPasswordReset(
        staff_id=staff.staff_id,
        code_hash=_code_digest(code, settings),
        expires_at=now + timedelta(minutes=settings.password_reset_expiry_minutes),
    )
    db.add(record)
    db.commit()
    try:
        _send_code(settings, staff.email, code)
    except Exception:
        record.used_at = datetime.utcnow()
        db.commit()
        raise
    return {"success": True, "message": GENERIC_MESSAGE}


def reset_password(email: str, code: str, new_password: str, db: Session, settings: Settings | None = None) -> dict:
    settings = settings or get_settings()
    normalized_email = email.strip().lower()
    staff = db.scalar(select(StaffUser).where(StaffUser.email == normalized_email, StaffUser.is_active.is_(True)))
    if not staff:
        raise AppError(400, "Invalid or expired reset code.")

    now = datetime.utcnow()
    reset_record = db.scalar(
        select(StaffPasswordReset)
        .where(
            StaffPasswordReset.staff_id == staff.staff_id,
            StaffPasswordReset.used_at.is_(None),
            StaffPasswordReset.expires_at > now,
        )
        .order_by(StaffPasswordReset.created_at.desc())
    )
    if not reset_record or reset_record.failed_attempts >= 5:
        raise AppError(400, "Invalid or expired reset code. Request a new code if needed.")

    submitted_hash = _code_digest(code, settings)
    if not hmac.compare_digest(reset_record.code_hash, submitted_hash):
        reset_record.failed_attempts += 1
        db.commit()
        raise AppError(400, "Invalid or expired reset code.")

    staff.password_hash = hash_password(new_password)
    staff.failed_login_attempts = 0
    staff.locked_until = None
    reset_record.used_at = now
    # Invalidate every other outstanding code for this account.
    db.execute(
        update(StaffPasswordReset)
        .where(StaffPasswordReset.staff_id == staff.staff_id, StaffPasswordReset.used_at.is_(None))
        .values(used_at=now)
    )
    db.commit()
    return {"success": True, "message": "Password reset successfully. Sign in with your new password."}
