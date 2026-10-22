"""
Branch endpoints — replaces the BRANCH lookup table management.

Screens served:
  • BranchCatalog screen (Kotlin) – view / add branches
"""

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.deps import db_session, require_user
from app.exceptions import AppError
from app.models import Branch
from app.schemas import BranchWrite

router = APIRouter(prefix="/branches")


def _serialize(b: Branch) -> dict:
    return {
        "branch_id": b.branch_id,
        "branch_code": b.branch_code,
        "branch_name": b.branch_name,
        "department": b.department,
        "is_fw": bool(b.is_fw),
        "parent_code": b.parent_code,
        "remarks": b.remarks,
    }


@router.get("", summary="List all branches / courses")
def list_branches(db: Session = Depends(db_session)):
    rows = db.scalars(select(Branch).order_by(Branch.branch_code)).all()
    return {"success": True, "message": "OK", "data": [_serialize(r) for r in rows]}


@router.get("/{branch_id}", summary="Get one branch by numeric ID")
def get_branch(branch_id: int, db: Session = Depends(db_session)):
    b = db.get(Branch, branch_id)
    if not b:
        raise AppError(404, "Branch not found")
    return {"success": True, "message": "OK", "data": _serialize(b)}


@router.post("", status_code=201, summary="Add a new branch")
def create_branch(
    body: BranchWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    if db.scalar(select(Branch).where(Branch.branch_code == body.branch_code.upper())):
        raise AppError(409, f"branch_code '{body.branch_code}' already exists")
    b = Branch(
        branch_code=body.branch_code.upper(),
        branch_name=body.branch_name,
        department=body.department,
        remarks=body.remarks,
        is_fw=int(body.is_fw),
        parent_code=body.parent_code,
    )
    db.add(b)
    db.commit()
    db.refresh(b)
    return {"success": True, "message": "Branch created", "data": _serialize(b)}


@router.put("/{branch_id}", summary="Update a branch")
def update_branch(
    branch_id: int,
    body: BranchWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    b = db.get(Branch, branch_id)
    if not b:
        raise AppError(404, "Branch not found")
    conflict = db.scalar(
        select(Branch).where(
            Branch.branch_code == body.branch_code.upper(),
            Branch.branch_id != branch_id,
        )
    )
    if conflict:
        raise AppError(409, f"branch_code '{body.branch_code}' already exists")
    b.branch_code = body.branch_code.upper()
    b.branch_name = body.branch_name
    b.department = body.department
    b.remarks = body.remarks
    b.is_fw = int(body.is_fw)
    b.parent_code = body.parent_code
    db.commit()
    return {"success": True, "message": "Branch updated", "data": _serialize(b)}


@router.delete("/{branch_id}", summary="Delete a branch (only if no students assigned)")
def delete_branch(
    branch_id: int,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    b = db.get(Branch, branch_id)
    if not b:
        raise AppError(404, "Branch not found")
    db.delete(b)
    try:
        db.commit()
    except Exception:
        db.rollback()
        raise AppError(409, "Cannot delete branch: students are assigned to it")
    return {"success": True, "message": "Branch deleted", "data": None}
