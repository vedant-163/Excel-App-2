"""
Dashboard endpoint — high-level totals for the home screen.

Screens served:
  • Dashboard card in MainActivity (studentsCount, fee totals)
"""

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.deps import db_session
from app.models import StudentSearchView
from app.services.excel_logic import to_float

router = APIRouter(prefix="/dashboard")


@router.get("", summary="Dashboard totals — student count, fee summary, admission year breakdown")
def dashboard(db: Session = Depends(db_session)):
    """
    Returns two result sets matching stored procedure sp_dashboard:

    1. Global totals (student count, branch count, fee totals, pending)
    2. Per-admission-year breakdown

    The Android MainActivity uses `student_count` to show the welcome card number.
    """
    # ── Global totals (v_dashboard_totals) ──────────────────────────
    totals_stmt = select(
        func.count().label("student_count"),
        func.count(func.distinct(StudentSearchView.branch_code)).label("branch_count"),
        func.sum(StudentSearchView.total_amount).label("sum_total_amount"),
        func.sum(StudentSearchView.student_submitted_amount).label("sum_submitted_amount"),
        func.sum(StudentSearchView.dte_fee).label("sum_dte_fee"),
        func.sum(StudentSearchView.pending_amount).label("sum_pending_amount"),
    )
    totals_row = db.execute(totals_stmt).mappings().one()

    totals = {
        "student_count": int(totals_row["student_count"] or 0),
        "branch_count": int(totals_row["branch_count"] or 0),
        "sum_total_amount": to_float(totals_row["sum_total_amount"]) or 0.0,
        "sum_submitted_amount": to_float(totals_row["sum_submitted_amount"]) or 0.0,
        "sum_dte_fee": to_float(totals_row["sum_dte_fee"]) or 0.0,
        "sum_pending_amount": to_float(totals_row["sum_pending_amount"]) or 0.0,
    }

    # ── Per admission-year breakdown (v_admission_year_summary) ──────
    year_stmt = (
        select(
            StudentSearchView.admission_year,
            func.count().label("student_count"),
            func.sum(StudentSearchView.total_amount).label("sum_total_amount"),
            func.sum(StudentSearchView.student_submitted_amount).label("sum_submitted_amount"),
        )
        .where(StudentSearchView.admission_year.is_not(None))
        .group_by(StudentSearchView.admission_year)
        .order_by(StudentSearchView.admission_year)
    )
    year_rows = db.execute(year_stmt).mappings().all()
    by_year = [
        {
            "admission_year": row["admission_year"],
            "student_count": int(row["student_count"]),
            "sum_total_amount": to_float(row["sum_total_amount"]) or 0.0,
            "sum_submitted_amount": to_float(row["sum_submitted_amount"]) or 0.0,
        }
        for row in year_rows
    ]

    return {
        "success": True,
        "message": "OK",
        "data": {
            "totals": totals,
            "by_year": by_year,
        },
    }
