"""
Lookup endpoints — dropdown values for the Android app.

The Kotlin app calls these once on startup and caches the lists locally
to populate spinners / dropdowns (replacing Excel validation lists).

Endpoints:
  GET /lookups/branches            → branch codes + names
  GET /lookups/eligible-categories → OBC / SC / ST / UR
  GET /lookups/alloted-categories  → detailed alloted codes
  GET /lookups/religions           → religion list
  GET /lookups/admission-statuses  → admission status codes
  GET /lookups/admission-rounds    → round codes
  GET /lookups/payment-modes       → payment mode options
  GET /lookups/remark-types        → remark codes
  GET /lookups/academic-terms      → JUL-DEC / JAN-JUN term codes
  GET /lookups/fee-categories      → fee category codes
  GET /lookups/admission-years     → distinct admission years in DB
"""

from fastapi import APIRouter, Depends
from sqlalchemy import distinct, select
from sqlalchemy.orm import Session

from app.deps import db_session
from app.models import (
    AcademicTerm,
    AdmissionRound,
    AdmissionStatus,
    AllotedCategory,
    Branch,
    EligibleCategory,
    FeeCategory,
    PaymentMode,
    Religion,
    RemarkType,
    StudentSearchView,
)

router = APIRouter(prefix="/lookups")

# Academic year is non-sensitive reference data. This public endpoint is used
# even by the temporary local-login build so the UI can still read years from
# the real database. All write/data routers remain protected.
public_router = APIRouter(prefix="/public/lookups")


@router.get("/branches", summary="All branch codes for dropdown")
def lookup_branches(db: Session = Depends(db_session)):
    rows = db.scalars(select(Branch).order_by(Branch.branch_code)).all()
    return {
        "success": True,
        "data": [
            {"branch_code": r.branch_code, "branch_name": r.branch_name}
            for r in rows
        ],
    }


@router.get("/eligible-categories", summary="Eligible category options (OBC/SC/ST/UR)")
def lookup_eligible(db: Session = Depends(db_session)):
    rows = db.scalars(select(EligibleCategory).order_by(EligibleCategory.category_code)).all()
    return {
        "success": True,
        "data": [
            {"code": r.category_code, "name": r.category_name}
            for r in rows
        ],
    }


@router.get("/alloted-categories", summary="Alloted category options")
def lookup_alloted(db: Session = Depends(db_session)):
    rows = db.scalars(select(AllotedCategory).order_by(AllotedCategory.category_code)).all()
    return {
        "success": True,
        "data": [
            {
                "code": r.category_code,
                "eligible_group": r.eligible_group,
                "description": r.description,
            }
            for r in rows
        ],
    }


@router.get("/religions", summary="Religion options")
def lookup_religions(db: Session = Depends(db_session)):
    rows = db.scalars(select(Religion).order_by(Religion.religion_code)).all()
    return {
        "success": True,
        "data": [{"code": r.religion_code, "name": r.religion_name} for r in rows],
    }


@router.get("/admission-statuses", summary="Admission status options")
def lookup_statuses(db: Session = Depends(db_session)):
    rows = db.scalars(select(AdmissionStatus).order_by(AdmissionStatus.status_code)).all()
    return {
        "success": True,
        "data": [{"code": r.status_code, "name": r.status_name} for r in rows],
    }


@router.get("/admission-rounds", summary="Admission round options")
def lookup_rounds(db: Session = Depends(db_session)):
    rows = db.scalars(select(AdmissionRound).order_by(AdmissionRound.round_code)).all()
    return {
        "success": True,
        "data": [{"code": r.round_code, "name": r.round_name} for r in rows],
    }


@router.get("/payment-modes", summary="Payment mode options")
def lookup_payment_modes(db: Session = Depends(db_session)):
    rows = db.scalars(select(PaymentMode).order_by(PaymentMode.mode_code)).all()
    return {
        "success": True,
        "data": [{"code": r.mode_code, "name": r.mode_name} for r in rows],
    }


@router.get("/remark-types", summary="Remark type options")
def lookup_remark_types(db: Session = Depends(db_session)):
    rows = db.scalars(select(RemarkType).order_by(RemarkType.remark_code)).all()
    return {
        "success": True,
        "data": [{"code": r.remark_code, "name": r.remark_name} for r in rows],
    }


@router.get("/academic-terms", summary="Academic term codes (JUL-DEC / JAN-JUN)")
def lookup_terms(db: Session = Depends(db_session)):
    rows = db.scalars(select(AcademicTerm).order_by(AcademicTerm.sort_order)).all()
    return {
        "success": True,
        "data": [
            {
                "term_id": r.term_id,
                "term_code": r.term_code,
                "calendar_year": r.calendar_year,
                "term_half": r.term_half,
                "sort_order": r.sort_order,
            }
            for r in rows
        ],
    }


@router.get("/fee-categories", summary="Fee category codes")
def lookup_fee_categories(db: Session = Depends(db_session)):
    rows = db.scalars(select(FeeCategory).order_by(FeeCategory.category_code)).all()
    return {
        "success": True,
        "data": [{"code": r.category_code, "name": r.category_name} for r in rows],
    }


@router.get("/admission-years", summary="Distinct admission years present in the database")
def lookup_admission_years(db: Session = Depends(db_session)):
    """
    Returns distinct admission_year values (e.g. 2020-21, 2021-22 …).
    The Android app uses this to populate the admission year slicer dropdown.
    """
    years = db.scalars(
        select(StudentSearchView.admission_year).distinct()
        .where(StudentSearchView.admission_year.is_not(None))
        .order_by(StudentSearchView.admission_year)
    ).all()
    return {"success": True, "data": list(years)}


@public_router.get("/academic-years", summary="Academic years currently present in the database")
def public_academic_years(db: Session = Depends(db_session)):
    years = db.scalars(
        select(StudentSearchView.admission_year).distinct()
        .where(StudentSearchView.admission_year.is_not(None))
        .order_by(StudentSearchView.admission_year)
    ).all()
    return {"success": True, "data": list(years)}
