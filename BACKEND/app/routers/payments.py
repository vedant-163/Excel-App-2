"""
Payment endpoints — replaces the Excel Fee & Payment columns.

Screens served:
  • FeePaymentScreen – view, add, edit payments per student
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.deps import db_session, require_user
from app.schemas import PaymentWrite
from app.services import payment_service

router = APIRouter(prefix="/payments")


# ── List payments (with optional date range) ──────────────────────────
@router.get("", summary="List all payments; optionally filter by date range")
def list_payments(
    from_date: str | None = Query(None, description="YYYY-MM-DD"),
    to_date: str | None = Query(None, description="YYYY-MM-DD"),
    academic_year: str | None = Query(None),
    fee_category: str | None = Query(None),
    semester_no: int | None = Query(None, ge=1, le=8),
    payment_mode: str | None = Query(None),
    branch: str | None = Query(None, description="Student branch code"),
    db: Session = Depends(db_session),
):
    """List payments; supplied academic-year/category/semester/mode filters combine with AND."""
    data = payment_service.list_payments(
        db, from_date, to_date, academic_year, fee_category, semester_no, payment_mode, branch
    )
    return {"success": True, "message": "OK", "data": data}


# ── Monthly payment aggregation ───────────────────────────────────────
@router.get("/monthly", summary="Monthly payment totals — replaces Excel REPORT pivot")
def monthly_payments(db: Session = Depends(db_session)):
    """
    Aggregates total_amount and submitted_amount by calendar month.
    Used by the Reports screen to build payment-trend charts.
    """
    data = payment_service.monthly_payments(db)
    return {"success": True, "message": "OK", "data": data}


# ── Single payment ────────────────────────────────────────────────────
@router.get("/{payment_id}", summary="Get one payment record by payment_id")
def get_payment(payment_id: int, db: Session = Depends(db_session)):
    data = payment_service.get_payment(db, payment_id)
    return {"success": True, "message": "OK", "data": data}


# ── Create / upsert payment for a student ────────────────────────────
@router.post(
    "",
    status_code=201,
    summary="Create or update a student's payment (Excel SP sp_upsert_payment)",
)
def upsert_payment(
    body: PaymentWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    If the student already has a payment row and `create_new` is false,
    updates that row (one-payment-per-student Excel model).
    Set `create_new: true` to add an additional payment row.

    Business rules enforced:
      - paid_amount cannot exceed total_amount
      - pending_amount = max(total - paid, 0)

    Accepts both snake_case and Kotlin camelCase fields:
      paidAmount / student_submitted_amount
      totalFee   / total_amount
      paymentMode / payment_mode
      paymentDate / payment_date
    """
    data = payment_service.upsert_payment(db, body)
    return {"success": True, "message": "Payment saved successfully", "data": data}


# ── Update existing payment ───────────────────────────────────────────
@router.put("/{payment_id}", summary="Update a specific payment record by payment_id")
def update_payment(
    payment_id: int,
    body: PaymentWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    data = payment_service.update_payment(db, payment_id, body)
    return {"success": True, "message": "Payment updated successfully", "data": data}
