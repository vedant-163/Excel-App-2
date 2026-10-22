from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.deps import db_session, require_user
from app.schemas import AdmissionUpdate
from app.services import student_service

router = APIRouter(prefix="/admissions")


@router.put("/{student_key}", summary="Create or update admission details for a student")
def update_admission(
    student_key: str,
    body: AdmissionUpdate,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    data = student_service.update_admission(
        db,
        student_key,
        body.model_dump(),
    )
    return {
        "success": True,
        "message": "Admission details saved successfully",
        "data": data,
    }
