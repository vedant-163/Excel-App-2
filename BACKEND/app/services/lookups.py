from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.exceptions import AppError
from app.models import (
    AdmissionRound,
    AdmissionStatus,
    AllotedCategory,
    Branch,
    EligibleCategory,
    FeeCategory,
    FeeStructure,
    PaymentMode,
    Religion,
    RemarkType,
)
from app.services.excel_logic import canonical_branch_code


def _one(db: Session, model, column, value: str | None, label: str, required: bool = True):
    if value is None or str(value).strip() == "":
        if required:
            raise AppError(422, f"{label} is required")
        return None
    raw = str(value).strip()
    row = db.scalar(select(model).where(func.upper(column) == raw.upper()))
    if row is None:
        row = db.scalar(select(model).where(func.upper(column).like(raw.upper())))
    if row is None:
        raise AppError(422, f"Unknown {label}: {raw}")
    return row


def resolve_branch(db: Session, value: str | None, required: bool = True) -> Branch | None:
    code = canonical_branch_code(value)
    if not code:
        if required:
            raise AppError(422, "branch is required")
        return None
    row = db.scalar(select(Branch).where(func.upper(Branch.branch_code) == code.upper()))
    if row:
        return row
    row = db.scalar(
        select(Branch).where(func.upper(Branch.branch_name) == str(value).strip().upper())
    )
    if row:
        return row
    row = db.scalar(
        select(Branch).where(Branch.branch_name.ilike(f"%{value.strip()}%"))
    )
    if row:
        return row
    raise AppError(422, f"Unknown branch: {value}")


def resolve_eligible(db: Session, value: str | None, required: bool = True):
    return _one(db, EligibleCategory, EligibleCategory.category_code, value, "eligible category", required)


def resolve_alloted(db: Session, value: str | None, required: bool = True):
    return _one(db, AllotedCategory, AllotedCategory.category_code, value, "alloted category", required)


def resolve_religion(db: Session, value: str | None, required: bool = True):
    if value is None or str(value).strip() == "":
        if required:
            raise AppError(422, "religion is required")
        return None
    raw = value.strip()
    row = db.scalar(select(Religion).where(func.upper(Religion.religion_code) == raw.upper()))
    if row:
        return row
    row = db.scalar(select(Religion).where(func.upper(Religion.religion_name) == raw.upper()))
    if row:
        return row
    raise AppError(422, f"Unknown religion: {value}")


def resolve_status(db: Session, value: str | None) -> AdmissionStatus:
    if not value:
        row = db.scalar(select(AdmissionStatus).limit(1))
        if not row:
            raise AppError(422, "No admission status rows exist")
        return row
    raw = value.strip().upper()
    if raw == "CANCELLED":
        raw = "CANCELLED"
    row = db.scalar(select(AdmissionStatus).where(func.upper(AdmissionStatus.status_code) == raw))
    if row:
        return row
    row = db.scalar(
        select(AdmissionStatus).where(func.upper(AdmissionStatus.status_name) == raw)
    )
    if row:
        return row
    raise AppError(422, f"Unknown admission status: {value}")


def resolve_round(db: Session, value: str | None):
    if value is None or not str(value).strip():
        return None
    raw = str(value).strip()
    # Imported reference tables sometimes store codes as "(FIRST ROUND)"
    # while Android displays "FIRST ROUND". Match both code and display name.
    normalized = raw.strip().strip("()").strip().upper()
    rows = db.scalars(select(AdmissionRound)).all()
    for row in rows:
        code = str(row.round_code or "").strip()
        name = str(row.round_name or "").strip()
        if code.upper() == raw.upper() or name.upper() == raw.upper():
            return row
        if code.strip().strip("()").strip().upper() == normalized or name.strip().strip("()").strip().upper() == normalized:
            return row
    raise AppError(422, f"Unknown admission round: {raw}")


def resolve_remark(db: Session, value: str | None):
    return _one(db, RemarkType, RemarkType.remark_code, value, "remarks", required=False)


def resolve_payment_mode(db: Session, value: str | None):
    if not value:
        return None
    raw = value.strip()
    row = db.scalar(select(PaymentMode).where(func.upper(PaymentMode.mode_code) == raw.upper()))
    if row:
        return row
    row = db.scalar(select(PaymentMode).where(func.upper(PaymentMode.mode_name) == raw.upper()))
    return row


def resolve_fee_category(db: Session, value: str | None) -> FeeCategory:
    return _one(db, FeeCategory, FeeCategory.category_code, value, "fee category", required=True)


def resolve_fee_structure(db: Session, value: str | None, required: bool = True):
    return _one(db, FeeStructure, FeeStructure.fee_mast_code, value, "fee structure", required)


def default_fee_structure(
    db: Session,
    year_start: int | None,
    branch_code: str | None = None,
) -> FeeStructure:
    """Resolve a fee structure without silently borrowing another branch's fee."""
    stmt = select(FeeStructure).order_by(FeeStructure.structure_year.desc())
    if year_start is not None:
        stmt = stmt.where(FeeStructure.structure_year == year_start)
    if branch_code:
        stmt = stmt.where(func.upper(FeeStructure.branch_code) == branch_code.strip().upper())
    row = db.scalar(stmt.limit(1))
    # Fee setup must not block creating a student. Return None when no exact
    # year/branch structure exists; the student can be linked to a fee structure later.
    return row
