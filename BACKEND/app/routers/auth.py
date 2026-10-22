from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth import login
from app.services.password_recovery import request_password_reset, reset_password
from app.deps import db_session
from app.schemas import ForgotPasswordRequest, LoginRequest, ResetPasswordRequest, TokenResponse

router = APIRouter(prefix="/auth")


@router.post("/login", response_model=TokenResponse, summary="Staff login — returns JWT")
def do_login(body: LoginRequest, db: Session = Depends(db_session)):
    return login(body, db)


@router.post("/forgot-password", summary="Send a password reset code to registered staff email")
def forgot_password(body: ForgotPasswordRequest, db: Session = Depends(db_session)):
    return request_password_reset(body.email, db)


@router.post("/reset-password", summary="Reset staff password using the emailed one-time code")
def do_reset_password(body: ResetPasswordRequest, db: Session = Depends(db_session)):
    return reset_password(body.email, body.code, body.new_password, db)
