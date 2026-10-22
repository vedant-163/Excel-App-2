from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session, selectinload

from app.exceptions import AppError
from app.models import FeeCategory, FeeStructure, FeeStructureSemester, Payment, PaymentMode, Student
from app.schemas import PaymentWrite
from app.services.excel_logic import parse_amount, parse_date, pending_amount, to_float
from app.services.lookups import resolve_payment_mode
from app.services.student_service import serialize_payment


def _student(db: Session, student_id: int | None, enrolment_no: str | None) -> Student:
    if student_id is not None:
        student = db.get(Student, student_id)
        if student:
            return student
    if enrolment_no:
        student = db.scalar(select(Student).where(Student.enrolment_no == enrolment_no))
        if student:
            return student
    raise AppError(404, "Student not found")


def _academic_year_start(value: str | None) -> int | None:
    raw = (value or "").strip()
    return int(raw[:4]) if len(raw) >= 4 and raw[:4].isdigit() else None


def _configured_fee(db: Session, student: Student, academic_year: str | None, fee_category: str | None, semester_no: int | None):
    year_start = _academic_year_start(academic_year)
    if year_start is None or not fee_category or semester_no is None:
        return None
    branch_code = student.branch.branch_code if student.branch else None
    if branch_code:
        amt = db.scalar(
            select(FeeStructureSemester.amount)
            .join(FeeStructure, FeeStructure.fee_structure_id == FeeStructureSemester.fee_structure_id)
            .join(FeeCategory, FeeCategory.fee_category_id == FeeStructure.fee_category_id)
            .where(
                FeeStructure.structure_year == year_start,
                func.upper(FeeStructure.branch_code) == branch_code.upper(),
                func.upper(FeeCategory.category_code) == fee_category.strip().upper(),
                FeeStructureSemester.semester_no == semester_no,
            )
            .limit(1)
        )
        if amt is not None:
            return amt
    # Fallback to general structure with null/empty branch code
    return db.scalar(
        select(FeeStructureSemester.amount)
        .join(FeeStructure, FeeStructure.fee_structure_id == FeeStructureSemester.fee_structure_id)
        .join(FeeCategory, FeeCategory.fee_category_id == FeeStructure.fee_category_id)
        .where(
            FeeStructure.structure_year == year_start,
            or_(FeeStructure.branch_code.is_(None), FeeStructure.branch_code == ""),
            func.upper(FeeCategory.category_code) == fee_category.strip().upper(),
            FeeStructureSemester.semester_no == semester_no,
        )
        .limit(1)
    )


def _previous_paid(db: Session, student_id: int, academic_year: str | None, fee_category: str | None, semester_no: int | None):
    return db.scalar(
        select(func.coalesce(func.sum(Payment.student_submitted_amount), 0)).where(
            Payment.student_id == student_id,
            Payment.academic_year == (academic_year.strip() if academic_year else None),
            Payment.fee_category == (fee_category.strip() if fee_category else None),
            Payment.semester_no == semester_no,
        )
    ) or 0


def upsert_payment(db: Session, body: PaymentWrite) -> dict:
    student = _student(db, body.student_id, body.enrolment_no)
    total_raw = body.total_amount_raw or (
        str(body.total_amount) if body.total_amount is not None else None
    ) or (str(body.totalFee) if body.totalFee is not None else None)
    submitted_raw = body.student_submitted_amount_raw or (
        str(body.student_submitted_amount)
        if body.student_submitted_amount is not None
        else None
    ) or (str(body.paidAmount) if body.paidAmount is not None else None)

    total = parse_amount(total_raw) if total_raw is not None else None
    submitted = parse_amount(submitted_raw) if submitted_raw is not None else None

    academic_year = body.academic_year or body.academicYear
    fee_category = body.fee_category or body.feeCategory
    semester_no = body.semester_no
    if semester_no is None and body.semester:
        try:
            semester_no = int(body.semester.lower().replace("semester", "").strip())
        except ValueError:
            raise AppError(422, "Invalid semester")

    configured_fee = _configured_fee(db, student, academic_year, fee_category, semester_no)
    if configured_fee is not None:
        total = parse_amount(configured_fee)
        total_raw = str(total)
    if total is None:
        raise AppError(422, "Total fee could not be determined for this student, branch, category and semester")
    if submitted is None or submitted <= 0:
        raise AppError(422, "Paid amount must be greater than zero")

    previous_paid = _previous_paid(db, student.student_id, academic_year, fee_category, semester_no)
    if previous_paid + submitted > total:
        remaining = max(float(total - previous_paid), 0.0)
        raise AppError(422, f"Payment exceeds the remaining fee balance of {remaining:.2f}")
    # Derive status from persisted-context totals on the server, not a potentially
    # stale client-side balance calculation.
    calculated_status = "PAID" if previous_paid + submitted >= total else "PARTIAL"

    mode_text = body.payment_mode or body.paymentMode
    mode = resolve_payment_mode(db, mode_text)
    if mode_text and mode is None:
        raise AppError(422, f"Unknown payment mode: {mode_text}")
    pay_date = body.payment_date or body.paymentDate

    transaction_id = body.online_application_txn_id or body.transactionId
    if transaction_id and db.scalar(select(Payment).where(Payment.online_application_txn_id == transaction_id.strip())):
        raise AppError(409, "Transaction ID already exists")

    existing = db.scalar(
        select(Payment)
        .where(Payment.student_id == student.student_id)
        .order_by(Payment.payment_id.desc())
    )
    payment = Payment(student_id=student.student_id) if (body.create_new or not existing) else existing

    if academic_year:
        payment.academic_year = academic_year.strip()
    if fee_category:
        payment.fee_category = fee_category.strip()
    if semester_no is not None:
        payment.semester_no = semester_no

    if total_raw is not None:
        payment.total_amount_raw = total_raw
        payment.total_amount = total
    if submitted_raw is not None:
        payment.student_submitted_amount_raw = submitted_raw
        payment.student_submitted_amount = submitted
    payment.online_application_txn_id = (
        body.online_application_txn_id or body.transactionId or payment.online_application_txn_id
    )
    if mode_text:
        payment.payment_mode = mode
        payment.payment_mode_raw = mode_text
    if pay_date:
        payment.payment_date_raw = pay_date
        payment.payment_date = parse_date(pay_date)
    if body.dte_fee is not None:
        payment.dte_fee = parse_amount(body.dte_fee)
    payment.payment_status = calculated_status
    if body.payment_reference_no:
        payment.payment_reference_no = body.payment_reference_no

    db.add(payment)
    db.commit()
    db.refresh(payment)
    return serialize_payment(payment)


def get_payment(db: Session, payment_id: int) -> dict:
    payment = db.scalar(
        select(Payment)
        .options(selectinload(Payment.payment_mode))
        .where(Payment.payment_id == payment_id)
    )
    if not payment:
        raise AppError(404, "Payment not found")
    return serialize_payment(payment)


def update_payment(db: Session, payment_id: int, body: PaymentWrite) -> dict:
    payment = db.get(Payment, payment_id)
    if not payment:
        raise AppError(404, "Payment not found")

    student = _student(db, payment.student_id, None)
    academic_year = body.academic_year or body.academicYear or payment.academic_year
    fee_category = body.fee_category or body.feeCategory or payment.fee_category
    semester_no = body.semester_no
    if semester_no is None and body.semester:
        try:
            semester_no = int(body.semester.lower().replace("semester", "").strip())
        except ValueError as exc:
            raise AppError(422, "Invalid semester") from exc
    if semester_no is None:
        semester_no = payment.semester_no

    configured_fee = _configured_fee(db, student, academic_year, fee_category, semester_no)
    total_raw = body.total_amount_raw or (
        str(body.total_amount) if body.total_amount is not None else None
    ) or (str(body.totalFee) if body.totalFee is not None else None)
    submitted_raw = body.student_submitted_amount_raw or (
        str(body.student_submitted_amount)
        if body.student_submitted_amount is not None
        else None
    ) or (str(body.paidAmount) if body.paidAmount is not None else None)

    total = parse_amount(configured_fee) if configured_fee is not None else (parse_amount(total_raw) if total_raw is not None else parse_amount(payment.total_amount))
    submitted = parse_amount(submitted_raw) if submitted_raw is not None else parse_amount(payment.student_submitted_amount)
    if total is None:
        raise AppError(422, "Total fee could not be determined for this student, branch, category and semester")
    if submitted is None or submitted <= 0:
        raise AppError(422, "Paid amount must be greater than zero")

    previous_paid = db.scalar(
        select(func.coalesce(func.sum(Payment.student_submitted_amount), 0)).where(
            Payment.student_id == student.student_id,
            Payment.payment_id != payment_id,
            Payment.academic_year == (academic_year.strip() if academic_year else None),
            Payment.fee_category == (fee_category.strip() if fee_category else None),
            Payment.semester_no == semester_no,
        )
    ) or 0
    if previous_paid + submitted > total:
        remaining = max(float(total - previous_paid), 0.0)
        raise AppError(422, f"Payment exceeds the remaining fee balance of {remaining:.2f}")

    transaction_id = body.online_application_txn_id or body.transactionId
    if transaction_id:
        transaction_id = transaction_id.strip()
        duplicate = db.scalar(
            select(Payment).where(
                Payment.online_application_txn_id == transaction_id,
                Payment.payment_id != payment_id,
            )
        )
        if duplicate:
            raise AppError(409, "Transaction ID already exists")

    payment.academic_year = academic_year.strip() if academic_year else None
    payment.fee_category = fee_category.strip() if fee_category else None
    payment.semester_no = semester_no
    payment.total_amount = total
    payment.total_amount_raw = str(total)
    payment.student_submitted_amount = submitted
    payment.student_submitted_amount_raw = submitted_raw or str(submitted)

    mode_text = body.payment_mode or body.paymentMode
    if mode_text:
        resolved_mode = resolve_payment_mode(db, mode_text)
        if resolved_mode is None:
            raise AppError(422, f"Unknown payment mode: {mode_text}")
        payment.payment_mode = resolved_mode
        payment.payment_mode_raw = mode_text.strip()

    pay_date = body.payment_date or body.paymentDate
    if pay_date:
        payment.payment_date_raw = pay_date
        payment.payment_date = parse_date(pay_date)
    if transaction_id:
        payment.online_application_txn_id = transaction_id
    if body.dte_fee is not None:
        payment.dte_fee = parse_amount(body.dte_fee)
    if body.payment_status:
        payment.payment_status = body.payment_status
    if body.payment_reference_no:
        payment.payment_reference_no = body.payment_reference_no

    db.add(payment)
    db.commit()
    db.refresh(payment)
    return serialize_payment(payment)

def list_payments(
    db: Session,
    from_date: str | None = None,
    to_date: str | None = None,
    academic_year: str | None = None,
    fee_category: str | None = None,
    semester_no: int | None = None,
    payment_mode: str | None = None,
    branch: str | None = None,
) -> list[dict]:
    stmt = (
        select(Payment, Student)
        .options(selectinload(Payment.payment_mode), selectinload(Student.branch))
        .join(Student, Student.student_id == Payment.student_id)
    )
    if from_date:
        stmt = stmt.where(Payment.payment_date >= parse_date(from_date))
    if to_date:
        stmt = stmt.where(Payment.payment_date <= parse_date(to_date))
    if academic_year and academic_year.strip():
        year_text = academic_year.strip()
        year_start = _academic_year_start(year_text)
        if year_start is not None:
            # Accept both 2025-26 and 2025-2026 without changing stored values.
            stmt = stmt.where(func.left(func.trim(Payment.academic_year), 4) == str(year_start))
        else:
            stmt = stmt.where(Payment.academic_year == year_text)
    if fee_category and fee_category.strip():
        stmt = stmt.where(func.upper(Payment.fee_category) == fee_category.strip().upper())
    if semester_no is not None:
        stmt = stmt.where(Payment.semester_no == semester_no)
    if branch and branch.strip():
        from app.models import Branch
        from app.services.excel_logic import canonical_branch_code
        branch_value = branch.strip().upper()
        code = canonical_branch_code(branch)
        code_value = code.upper() if code else branch_value
        stmt = stmt.join(Branch, Student.branch_id == Branch.branch_id).where(
            or_(
                func.upper(Branch.branch_code) == branch_value,
                func.upper(Branch.branch_code) == code_value,
                func.upper(Branch.branch_name) == branch_value,
                func.upper(Branch.branch_name) == code_value,
            )
        )
    if payment_mode and payment_mode.strip():
        mode_value = payment_mode.strip().upper()
        stmt = stmt.outerjoin(PaymentMode, Payment.payment_mode_id == PaymentMode.payment_mode_id).where(
            or_(
                func.upper(Payment.payment_mode_raw) == mode_value,
                func.upper(PaymentMode.mode_code) == mode_value,
                func.upper(PaymentMode.mode_name) == mode_value,
            )
        )
    stmt = stmt.order_by(Payment.payment_date.desc(), Student.name)
    rows = db.execute(stmt).all()
    result = []
    for payment, student in rows:
        paid_total = payment.student_submitted_amount
        pending_value = pending_amount(payment.total_amount, payment.student_submitted_amount)
        # For context-aware records, calculate the outstanding balance from all
        # payments for that student/year/category/semester, not this transaction alone.
        if payment.academic_year and payment.fee_category and payment.semester_no is not None:
            paid_total = db.scalar(
                select(func.coalesce(func.sum(Payment.student_submitted_amount), 0)).where(
                    Payment.student_id == payment.student_id,
                    Payment.academic_year == payment.academic_year,
                    Payment.fee_category == payment.fee_category,
                    Payment.semester_no == payment.semester_no,
                )
            ) or 0
            configured = _configured_fee(
                db, student, payment.academic_year, payment.fee_category, payment.semester_no
            )
            total_for_context = configured if configured is not None else payment.total_amount
            pending_value = pending_amount(total_for_context, paid_total)
        result.append({
            "payment_id": payment.payment_id,
            "student_id": payment.student_id,
            "enrolment_no": student.enrolment_no,
            "name": student.name,
            "branch": student.branch.branch_code if student.branch else "",
            "payment_date": payment.payment_date.isoformat() if payment.payment_date else None,
            "total_amount": to_float(payment.total_amount),
            "student_submitted_amount": to_float(payment.student_submitted_amount),
            "paid_amount": to_float(payment.student_submitted_amount),
            "pending_amount": to_float(pending_value),
            "payment_mode": payment.payment_mode_raw or (payment.payment_mode.mode_name if payment.payment_mode else None),
            "transaction_id": payment.online_application_txn_id or payment.payment_reference_no,
            "academic_year": payment.academic_year,
            "fee_category": payment.fee_category,
            "semester_no": payment.semester_no,
            "payment_status": payment.payment_status,
        })
    return result


def monthly_payments(db: Session) -> list[dict]:
    from sqlalchemy import func

    stmt = (
        select(
            func.date_format(Payment.payment_date, "%Y-%m").label("year_month"),
            func.count().label("payment_rows"),
            func.sum(Payment.total_amount).label("sum_total_amount"),
            func.sum(Payment.student_submitted_amount).label("sum_submitted"),
        )
        .where(Payment.payment_date.is_not(None))
        .group_by(func.date_format(Payment.payment_date, "%Y-%m"))
        .order_by("year_month")
    )
    rows = db.execute(stmt).mappings().all()
    return [
        {
            "year_month": row["year_month"],
            "payment_rows": int(row["payment_rows"]),
            "sum_total_amount": to_float(row["sum_total_amount"]),
            "sum_submitted": to_float(row["sum_submitted"]),
        }
        for row in rows
    ]
