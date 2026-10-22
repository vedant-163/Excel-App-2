"""
Fee Structure endpoints — replaces the Excel FEE MAST sheet.

Screens served:
  • FeeStructureScreen – view / add / edit fee structures and per-semester amounts
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session, selectinload

from app.deps import db_session, require_user
from app.exceptions import AppError
from app.models import Branch, FeeCategory, FeeStructure, FeeStructureSemester
from app.schemas import FeeStructureSemesterUpsert, FeeStructureWrite, Ok
from app.services.excel_logic import parse_amount
from app.services.lookups import resolve_branch, resolve_fee_category

router = APIRouter(prefix="/fee-structures")


# ─── helpers ──────────────────────────────────────────────────────────

def _serialize_fs(fs: FeeStructure) -> dict:
    return {
        "fee_structure_id": fs.fee_structure_id,
        "fee_mast_code": fs.fee_mast_code,
        "fee_category": fs.category.category_code if fs.category else None,
        "fee_category_name": fs.category.category_name if fs.category else None,
        "structure_year": fs.structure_year,
        "branch": fs.branch_code,
        "semesters": [
            {"semester_no": s.semester_no, "amount": float(s.amount)}
            for s in sorted(fs.semesters, key=lambda x: x.semester_no)
        ],
    }


def _load(db: Session, fee_structure_id: int) -> FeeStructure:
    fs = db.scalar(
        select(FeeStructure)
        .options(
            selectinload(FeeStructure.category),
            selectinload(FeeStructure.semesters),
        )
        .where(FeeStructure.fee_structure_id == fee_structure_id)
    )
    if not fs:
        raise AppError(404, "Fee structure not found")
    return fs


# ─── Semester-level upsert/delete helpers ─────────────────────────────
@router.post("/semester", status_code=200, summary="Create or update one semester fee")
def upsert_fee_semester(
    body: FeeStructureSemesterUpsert,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    UI-friendly endpoint: one screen row represents one semester amount.
    It updates an existing year + branch + category structure when present,
    otherwise creates the structure and its first semester. Existing data is
    preserved; only the selected semester is changed.
    """
    year_text = str(body.academic_year).strip()
    try:
        structure_year = int(year_text[:4])
    except ValueError:
        raise AppError(422, "Academic year must start with a calendar year")

    branch = resolve_branch(db, body.branch)
    category = resolve_fee_category(db, body.fee_category)

    fs = db.scalar(
        select(FeeStructure)
        .options(selectinload(FeeStructure.semesters), selectinload(FeeStructure.category))
        .where(
            FeeStructure.structure_year == structure_year,
            FeeStructure.branch_code == branch.branch_code,
            FeeStructure.fee_category_id == category.fee_category_id,
        )
    )

    if fs is None:
        code = f"{structure_year} {branch.branch_code}/{category.category_code}"[:64]
        conflict = db.scalar(select(FeeStructure).where(FeeStructure.fee_mast_code == code))
        if conflict:
            code = f"{structure_year}-{branch.branch_code}-{category.category_code}"[:64]
        fs = FeeStructure(
            fee_mast_code=code,
            fee_category_id=category.fee_category_id,
            structure_year=structure_year,
            branch_code=branch.branch_code,
        )
        db.add(fs)
        db.flush()

    existing = next((x for x in fs.semesters if x.semester_no == body.semester_no), None)
    amount = parse_amount(body.amount)
    if existing is None:
        fs.semesters.append(FeeStructureSemester(semester_no=body.semester_no, amount=amount))
    else:
        existing.amount = amount

    db.commit()
    return {
        "success": True,
        "message": "Fee semester saved successfully",
        "data": _serialize_fs(_load(db, fs.fee_structure_id)),
    }


@router.delete("/{fee_structure_id}/semester/{semester_no}", summary="Delete one semester fee")
def delete_fee_semester(
    fee_structure_id: int,
    semester_no: int,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    if semester_no < 1 or semester_no > 8:
        raise AppError(422, "semester_no must be between 1 and 8")
    fs = _load(db, fee_structure_id)
    row = next((x for x in fs.semesters if x.semester_no == semester_no), None)
    if row is None:
        raise AppError(404, "Fee semester not found")
    db.delete(row)
    db.flush()
    if not fs.semesters:
        db.delete(fs)
    db.commit()
    return {"success": True, "message": "Fee semester deleted", "data": None}


# ─── List ──────────────────────────────────────────────────────────────
@router.get("", summary="List fee structures with optional combined filters")
def list_fee_structures(
    academic_year: str | None = Query(None, description="Academic year, e.g. 2025-26"),
    fee_category: str | None = Query(None, description="Fee category code"),
    semester_no: int | None = Query(None, ge=1, le=8),
    branch: str | None = Query(None, description="Branch code"),
    db: Session = Depends(db_session),
):
    """Return configured fee structures; every supplied filter is combined with AND."""
    stmt = (
        select(FeeStructure)
        .options(
            selectinload(FeeStructure.category),
            selectinload(FeeStructure.semesters),
        )
    )
    if academic_year and academic_year.strip():
        year_text = academic_year.strip()
        year_start = int(year_text[:4]) if year_text[:4].isdigit() else None
        if year_start is not None:
            stmt = stmt.where(FeeStructure.structure_year == year_start)
    if fee_category and fee_category.strip():
        stmt = stmt.join(FeeCategory, FeeCategory.fee_category_id == FeeStructure.fee_category_id)
        stmt = stmt.where(func.upper(FeeCategory.category_code) == fee_category.strip().upper())
    if semester_no is not None:
        stmt = stmt.join(
            FeeStructureSemester,
            FeeStructureSemester.fee_structure_id == FeeStructure.fee_structure_id,
        ).where(FeeStructureSemester.semester_no == semester_no)
    if branch and branch.strip():
        branch_value = branch.strip().upper()
        # Accept both persisted branch codes (e.g. CSE) and the canonical
        # display names sent by the Android fee screens.
        stmt = stmt.join(
            Branch,
            func.upper(Branch.branch_code) == func.upper(FeeStructure.branch_code),
        ).where(
            or_(
                func.upper(FeeStructure.branch_code) == branch_value,
                func.upper(Branch.branch_name) == branch_value,
            )
        )
    rows = db.scalars(
        stmt.order_by(FeeStructure.structure_year.desc(), FeeStructure.fee_mast_code)
    ).unique().all()
    return {"success": True, "message": "OK", "data": [_serialize_fs(r) for r in rows]}


# ─── Single ────────────────────────────────────────────────────────────
@router.get("/{fee_structure_id}", summary="Get one fee structure by ID")
def get_fee_structure(fee_structure_id: int, db: Session = Depends(db_session)):
    fs = _load(db, fee_structure_id)
    return {"success": True, "message": "OK", "data": _serialize_fs(fs)}


# ─── Create ────────────────────────────────────────────────────────────
@router.post("", status_code=201, summary="Add a new fee structure (Excel FEE MAST row)")
def create_fee_structure(
    body: FeeStructureWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    Creates a fee structure with up to 8 semester amounts.

    Business rule: semester_no must be 1-8 and amounts must be >= 0.
    The fee_mast_code must be unique (matches Excel unique code column).
    """
    # Duplicate check
    existing = db.scalar(
        select(FeeStructure).where(FeeStructure.fee_mast_code == body.fee_mast_code)
    )
    if existing:
        raise AppError(409, f"fee_mast_code '{body.fee_mast_code}' already exists")

    category = resolve_fee_category(db, body.fee_category)
    fs = FeeStructure(
        fee_mast_code=body.fee_mast_code,
        fee_category_id=category.fee_category_id,
        structure_year=body.structure_year,
        branch_code=body.branch_code or body.branch,
    )
    for sem in body.semesters:
        fs.semesters.append(
            FeeStructureSemester(
                semester_no=sem.semester_no,
                amount=parse_amount(sem.amount),
            )
        )
    db.add(fs)
    db.commit()
    db.refresh(fs)
    return {
        "success": True,
        "message": "Fee structure created successfully",
        "data": _serialize_fs(_load(db, fs.fee_structure_id)),
    }


# ─── Update ────────────────────────────────────────────────────────────
@router.put("/{fee_structure_id}", summary="Update a fee structure and its semester amounts")
def update_fee_structure(
    fee_structure_id: int,
    body: FeeStructureWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    Replaces the semester list entirely (delete-and-recreate strategy).
    """
    fs = _load(db, fee_structure_id)

    # Unique code check (exclude self)
    conflict = db.scalar(
        select(FeeStructure).where(
            FeeStructure.fee_mast_code == body.fee_mast_code,
            FeeStructure.fee_structure_id != fee_structure_id,
        )
    )
    if conflict:
        raise AppError(409, f"fee_mast_code '{body.fee_mast_code}' already exists")

    category = resolve_fee_category(db, body.fee_category)
    fs.fee_mast_code = body.fee_mast_code
    fs.fee_category_id = category.fee_category_id
    fs.structure_year = body.structure_year
    fs.branch_code = body.branch_code or body.branch

    # Replace semesters
    for old in list(fs.semesters):
        db.delete(old)
    db.flush()
    for sem in body.semesters:
        db.add(
            FeeStructureSemester(
                fee_structure_id=fee_structure_id,
                semester_no=sem.semester_no,
                amount=parse_amount(sem.amount),
            )
        )
    db.commit()
    return {
        "success": True,
        "message": "Fee structure updated successfully",
        "data": _serialize_fs(_load(db, fee_structure_id)),
    }


# ─── Delete ────────────────────────────────────────────────────────────
@router.delete("/{fee_structure_id}", summary="Delete a fee structure (and its semester rows)")
def delete_fee_structure(
    fee_structure_id: int,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    fs = db.get(FeeStructure, fee_structure_id)
    if not fs:
        raise AppError(404, "Fee structure not found")
    db.delete(fs)
    db.commit()
    return {"success": True, "message": "Fee structure deleted", "data": None}
