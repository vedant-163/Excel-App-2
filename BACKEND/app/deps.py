from fastapi import Depends
from sqlalchemy.orm import Session

from app.auth import get_current_user
from app.database import get_db


def db_session(db: Session = Depends(get_db)) -> Session:
    return db


def require_user(user: str = Depends(get_current_user)) -> str:
    return user
