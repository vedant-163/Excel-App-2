from decimal import Decimal

from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session, selectinload

from app.exceptions import AppError
from app.models import (
    Admission,
    Contact,
    Payment,
    Student,
    StudentSearchView,
    StudentTermFee,
)
from app.schemas import StudentUpdate, StudentWrite, normalized_student_payload
from app.services.excel_logic import (
    admission_year_start,
    money,
    normalize_admission_year,
    parse_amount,
    parse_date,
    pending_amount,
    to_float,
)
from app.services.lookups import (
    default_fee_structure,
    resolve_alloted,
    resolve_branch,
    resolve_eligible,
    resolve_fee_structure,
    resolve_religion,
    resolve_remark,
    resolve_round,
    resolve_status,
)


LIST_SORTS = {
    "sr_no": StudentSearchView.sr_no,
    "name": StudentSearchView.name,
    "enrolment_no": StudentSearchView.enrolment_no,
    "admission_year": StudentSearchView.admission_year,
    "branch": StudentSearchView.branch_code,
}


def _blank_to_none(value: str | None) -> str | None:
    if value is None:
        return None
    text = value.strip()
    return text or None


def search_students(
    db: Session,
    *,
    q: str | None = None,
    unique_id: str | None = None,
    name: str | None = None,
    enrolment_no: str | None = None,
    class_roll_no: str | None = None,
    branch: str | None = None,
    admission_year: str | None = None,
    eligible_category: str | None = None,
    alloted_category: str | None = None,
    gender: str | None = None,
    ews: str | None = None,
    domicile: str | None = None,
    religion: str | None = None,
    remarks: str | None = None,
    status: str | None = None,
    sort: str = "sr_no",
    limit: int = 30,
    offset: int = 0,
) -> tuple[int, list[StudentSearchView]]:
    limit = min(max(limit, 1), 10000)
    offset = max(offset, 0)

    filters = []
    if unique_id:
        filters.append(StudentSearchView.college_unique_id.ilike(f"%{unique_id.strip()}%"))
    if name:
        filters.append(StudentSearchView.name.ilike(f"%{name.strip()}%"))
    if enrolment_no:
        filters.append(StudentSearchView.enrolment_no.ilike(f"%{enrolment_no.strip()}%"))
    if class_roll_no:
        roll_query = f"%{class_roll_no.strip()}%"
        filters.append(or_(
            StudentSearchView.class_roll_no.ilike(roll_query),
            StudentSearchView.exam_roll_no.ilike(roll_query),
        ))
    if branch:
        filters.append(StudentSearchView.branch_code == branch)
    if admission_year:
        # DB stores academic-year text (2020-21); accept 2020 or 2020-21.
        filters.append(
            StudentSearchView.admission_year == normalize_admission_year(admission_year)
        )
    if eligible_category:
        filters.append(StudentSearchView.eligible_category == eligible_category)
    if alloted_category:
        filters.append(StudentSearchView.alloted_category == alloted_category)
    if gender:
        filters.append(StudentSearchView.gender == gender.upper())
    if ews:
        filters.append(StudentSearchView.ews == ews.upper())
    if domicile:
        filters.append(StudentSearchView.domicile == domicile.upper())
    if religion:
        filters.append(StudentSearchView.religion == religion)
    if remarks:
        filters.append(StudentSearchView.remarks == remarks)
    if status:
        filters.append(func.upper(StudentSearchView.admission_status) == status.upper())
    if q:
        query_text = q.strip()
        like = f"%{query_text}%"
        query_matches = [
            StudentSearchView.name.ilike(like),
            StudentSearchView.enrolment_no.ilike(like),
            StudentSearchView.class_roll_no.ilike(like),
            StudentSearchView.exam_roll_no.ilike(like),
            StudentSearchView.college_unique_id.ilike(like),
            StudentSearchView.student_phone.ilike(like),
        ]
        if query_text.isdigit():
            query_matches.append(StudentSearchView.student_id == int(query_text))
        filters.append(or_(*query_matches))

    where = and_(*filters) if filters else True
    count = db.scalar(select(func.count()).select_from(StudentSearchView).where(where)) or 0
    sort_col = LIST_SORTS.get(sort, StudentSearchView.sr_no)
    rows = db.scalars(
        select(StudentSearchView)
        .where(where)
        .order_by(sort_col, StudentSearchView.student_id)
        .limit(limit)
        .offset(offset)
    ).all()
    return int(count), list(rows)


def list_item(row: StudentSearchView) -> dict:
    return {
        "student_id": row.student_id,
        "sr_no": row.sr_no,
        "college_unique_id": row.college_unique_id,
        "enrolment_no": row.enrolment_no,
        "class_roll_no": row.class_roll_no,
        "exam_roll_no": row.exam_roll_no,
        "name": row.name,
        "branch_code": row.branch_code,
        "eligible_category": row.eligible_category,
        "alloted_category": row.alloted_category,
        "gender": row.gender,
        "ews": row.ews,
        "domicile": row.domicile,
        "religion": row.religion,
        "admission_year": row.admission_year,
        "admission_status": row.admission_status,
        "admission_round": row.admission_round,
        "remarks": row.remarks,
        "fee_structure": row.fee_structure,
        "student_phone": row.student_phone,
        "total_amount": to_float(row.total_amount),
        "paid_amount": to_float(row.paid_amount),
        "pending_amount": to_float(row.pending_amount),
    }


def _resolve_student_key(db: Session, student_key: str) -> Student:
    stmt = (
        select(Student)
        .options(
            selectinload(Student.branch),
            selectinload(Student.eligible_category),
            selectinload(Student.alloted_category),
            selectinload(Student.religion),
            selectinload(Student.fee_structure),
            selectinload(Student.contact),
            selectinload(Student.admission).selectinload(Admission.status),
            selectinload(Student.admission).selectinload(Admission.round),
            selectinload(Student.admission).selectinload(Admission.remark_type),
            selectinload(Student.payments).selectinload(Payment.payment_mode),
            selectinload(Student.term_fees).selectinload(StudentTermFee.term),
        )
    )
    # Prefer the unique business identifier first. Some institutions have numeric
    # enrollment numbers, so checking numeric student_id first can select the wrong record.
    student = db.scalar(stmt.where(Student.enrolment_no == student_key.strip()))
    if student:
        return student
    if student_key.strip().isdigit():
        student = db.scalar(stmt.where(Student.student_id == int(student_key.strip())))
        if student:
            return student
    raise AppError(404, "Student not found")


def get_student(db: Session, student_key: str) -> dict:
    student = _resolve_student_key(db, student_key)
    return serialize_student(student, include_password=False)


def serialize_student(student: Student, include_password: bool = False) -> dict:
    contact = student.contact
    admission = student.admission
    payments = sorted(student.payments, key=lambda p: p.payment_id)
    latest = payments[-1] if payments else None
    total = latest.total_amount if latest else None
    submitted = latest.student_submitted_amount if latest else None
    data = {
        "student_id": student.student_id,
        "sr_no": student.sr_no,
        "college_unique_id": student.college_unique_id,
        "class_roll_no": student.class_roll_no,
        "enrolment_no": student.enrolment_no,
        "name": student.name,
        "father_name": student.father_name,
        "mother_name": student.mother_name,
        "rank_text": student.rank_text,
        "marks": to_float(student.marks) if student.marks is not None else None,
        "exam_roll_no": student.exam_roll_no,
        "date_of_birth": student.date_of_birth.isoformat() if student.date_of_birth else None,
        "domicile": student.domicile,
        "gender": student.gender,
        "ews": student.ews,
        "branch_code": student.branch.branch_code if student.branch else None,
        "branch_name": student.branch.branch_name if student.branch else None,
        "eligible_category": student.eligible_category.category_code if student.eligible_category else None,
        "alloted_category": student.alloted_category.category_code if student.alloted_category else None,
        "religion": student.religion.religion_code if student.religion else None,
        "fee_structure": student.fee_structure.fee_mast_code if student.fee_structure else None,
        "admission_year": admission.admission_year if admission else None,
        "admission_year_start": admission.admission_year_start if admission else None,
        "admission_no": admission.admission_no if admission else None,
        "admission_status": admission.status.status_code if admission and admission.status else None,
        "admission_round": admission.round.round_code if admission and admission.round else None,
        "remarks": admission.remark_type.remark_code if admission and admission.remark_type else None,
        "remarks_raw": admission.remarks_raw if admission else None,
        "offline_category": admission.offline_category if admission else None,
        "student_phone": contact.student_phone if contact else None,
        "student_email": contact.student_email if contact else None,
        "parents_phone": contact.parents_phone if contact else None,
        "parents_email": contact.parents_email if contact else None,
        "aadhaar_no": contact.aadhaar_no if contact else None,
        "aadhaar_linked_mobile": contact.aadhaar_linked_mobile if contact else None,
        "permanent_address": contact.permanent_address if contact else None,
        "present_address": contact.present_address if contact else None,
        "total_amount": to_float(total),
        "total_amount_raw": latest.total_amount_raw if latest else None,
        "student_submitted_amount": to_float(submitted),
        "student_submitted_amount_raw": latest.student_submitted_amount_raw if latest else None,
        "paid_amount": to_float(submitted),
        "pending_amount": to_float(pending_amount(total, submitted)),
        "payments": [serialize_payment(p) for p in payments],
    }
    if include_password:
        data["mponline_password"] = student.mponline_password
    return data


def serialize_payment(payment: Payment) -> dict:
    return {
        "payment_id": payment.payment_id,
        "student_id": payment.student_id,
        "academic_year": payment.academic_year,
        "fee_category": payment.fee_category,
        "semester_no": payment.semester_no,
        "total_amount": to_float(payment.total_amount),
        "total_amount_raw": payment.total_amount_raw,
        "student_submitted_amount": to_float(payment.student_submitted_amount),
        "student_submitted_amount_raw": payment.student_submitted_amount_raw,
        "paid_amount": to_float(payment.student_submitted_amount),
        "pending_amount": to_float(
            pending_amount(payment.total_amount, payment.student_submitted_amount)
        ),
        "online_application_txn_id": payment.online_application_txn_id,
        "payment_mode": payment.payment_mode.mode_code if payment.payment_mode else None,
        "payment_mode_raw": payment.payment_mode_raw,
        "payment_date": payment.payment_date.isoformat() if payment.payment_date else None,
        "payment_date_raw": payment.payment_date_raw,
        "dte_fee": to_float(payment.dte_fee),
        "payment_status": payment.payment_status,
        "payment_reference_no": payment.payment_reference_no,
    }


def student_fees(db: Session, student_key: str) -> list[dict]:
    student = _resolve_student_key(db, student_key)
    rows = sorted(student.term_fees, key=lambda f: f.term.sort_order if f.term else 0)
    return [
        {
            "term_code": fee.term.term_code,
            "calendar_year": fee.term.calendar_year,
            "term_half": fee.term.term_half,
            "sort_order": fee.term.sort_order,
            "scheduled_fee": to_float(fee.amount) or 0,
        }
        for fee in rows
        if fee.term
    ]


def _next_sr_no(db: Session) -> int:
    current = db.scalar(select(func.max(Student.sr_no)))
    return int(current or 0) + 1


def create_student(db: Session, body: StudentWrite) -> dict:
    payload = normalized_student_payload(body)
    enrolment = payload["enrolment_no"]
    if not enrolment:
        raise AppError(422, "enrolment_no is required")
    if not payload["class_roll_no"]:
        raise AppError(422, "class_roll_no is required")
    if not payload["gender"]:
        raise AppError(422, "gender is required")
    if db.scalar(select(Student).where(Student.enrolment_no == enrolment)):
        raise AppError(409, "enrolment_no already exists")

    try:
        student = _apply_payload(db, Student(sr_no=_next_sr_no(db), enrolment_no=enrolment), payload)
        db.add(student)
        db.commit()
        db.refresh(student)
    except AppError:
        db.rollback()
        raise
    return get_student(db, str(student.student_id))


def update_student(db: Session, student_key: str, body: StudentUpdate) -> dict:
    student = _resolve_student_key(db, student_key)
    payload = normalized_student_payload(body)
    enrolment = payload["enrolment_no"]
    if enrolment and enrolment != student.enrolment_no:
        exists = db.scalar(select(Student).where(Student.enrolment_no == enrolment))
        if exists:
            raise AppError(409, "enrolment_no already exists")
    try:
        _apply_payload(db, student, payload, is_update=True)
        db.commit()
    except AppError:
        db.rollback()
        raise
    return get_student(db, str(student.student_id))


def update_admission(
    db: Session,
    student_key: str,
    payload: dict,
) -> dict:
    """Update only admission-related data without overwriting unrelated student fields."""
    student = _resolve_student_key(db, student_key)

    branch = resolve_branch(db, payload.get("branch"), required=True)
    eligible = resolve_eligible(db, payload.get("eligible_category"), required=True)
    alloted = resolve_alloted(db, payload.get("allocated_category"), required=True)
    status = resolve_status(db, payload.get("status"))
    round_obj = resolve_round(db, payload.get("round"))
    year = payload.get("admission_year")
    year_start = admission_year_start(year)
    if not year_start:
        raise AppError(422, "Invalid admission year")

    if student.admission is None:
        student.admission = Admission(
            admission_year=year,
            admission_year_start=year_start,
            status=status,
        )

    admission = student.admission
    student.branch = branch
    student.eligible_category = eligible
    student.alloted_category = alloted

    admission.admission_year = year
    admission.admission_year_start = year_start
    admission.status = status
    admission.round = round_obj
    # The list endpoint does not expose offline_category. Preserve the existing
    # database value when the client sends a blank value rather than clearing it
    # accidentally while editing another admission field.
    offline_category = payload.get("offline_category")
    if offline_category is not None and str(offline_category).strip():
        admission.offline_category = str(offline_category).strip()
    admission.remarks_raw = payload.get("remarks") or None
    admission.remark_type = (
        resolve_remark(db, payload.get("remarks"))
        if payload.get("remarks")
        else None
    )

    db.commit()
    db.refresh(student)
    return serialize_student(student, include_password=False)


def delete_student(db: Session, student_key: str) -> None:
    student = _resolve_student_key(db, student_key)
    db.delete(student)
    db.commit()


def _apply_payload(db: Session, student: Student, payload: dict, is_update: bool = False) -> Student:
    year_start = admission_year_start(payload.get("admission_year"))
    branch = resolve_branch(db, payload.get("branch"), required=not is_update)
    eligible = resolve_eligible(db, payload.get("eligible_category"), required=not is_update)
    alloted = resolve_alloted(db, payload.get("alloted_category"), required=not is_update)
    religion = resolve_religion(db, payload.get("religion"), required=not is_update)
    fee = None
    if payload.get("fee_structure"):
        fee = resolve_fee_structure(db, payload.get("fee_structure"), required=True)
    elif not is_update:
        fee = default_fee_structure(db, year_start, branch.branch_code if branch else None)

    if payload.get("name"):
        student.name = payload["name"]
    if payload.get("enrolment_no"):
        student.enrolment_no = payload["enrolment_no"]
    if payload.get("class_roll_no"):
        student.class_roll_no = payload["class_roll_no"]
    for field in (
        "college_unique_id",
        "exam_roll_no",
        "father_name",
        "mother_name",
        "rank_text",
        "marks",
        "date_of_birth",
        "domicile",
        "gender",
        "ews",
        "mponline_password",
    ):
        if payload.get(field) is not None:
            setattr(student, field, payload[field])
    if branch:
        student.branch = branch
    if eligible:
        student.eligible_category = eligible
    if alloted:
        student.alloted_category = alloted
    if religion:
        student.religion = religion
    if fee:
        student.fee_structure = fee

    if student.contact is None:
        student.contact = Contact()
    contact = student.contact
    for src, dest in (
        ("student_phone", "student_phone"),
        ("student_email", "student_email"),
        ("parents_phone", "parents_phone"),
        ("parents_email", "parents_email"),
        ("aadhaar_no", "aadhaar_no"),
        ("aadhaar_linked_mobile", "aadhaar_linked_mobile"),
        ("permanent_address", "permanent_address"),
        ("present_address", "present_address"),
    ):
        if payload.get(src) is not None:
            setattr(contact, dest, payload[src])

    if student.admission is None:
        student.admission = Admission(
            admission_year=payload.get("admission_year") or "2025-26",
            admission_year_start=year_start or 2025,
            status=resolve_status(db, payload.get("status")),
        )
    admission = student.admission
    if payload.get("admission_year"):
        admission.admission_year = payload["admission_year"]
        admission.admission_year_start = year_start or admission.admission_year_start
    if payload.get("admission_no") is not None:
        admission.admission_no = payload["admission_no"]
    if payload.get("offline_category") is not None:
        admission.offline_category = payload["offline_category"]
    if payload.get("remarks") is not None:
        admission.remarks_raw = payload["remarks"]
        admission.remark_type = resolve_remark(db, payload["remarks"])
    if payload.get("status"):
        admission.status = resolve_status(db, payload["status"])
    if payload.get("round"):
        admission.round = resolve_round(db, payload["round"])

    total_raw = payload.get("total_amount_raw")
    submitted_raw = payload.get("submitted_raw")
    if total_raw or submitted_raw or payload.get("payment_mode"):
        payment = student.payments[0] if student.payments else Payment()
        if payment not in student.payments:
            student.payments.append(payment)
        if total_raw is not None:
            payment.total_amount_raw = total_raw
            payment.total_amount = parse_amount(total_raw)
        if submitted_raw is not None:
            payment.student_submitted_amount_raw = submitted_raw
            payment.student_submitted_amount = parse_amount(submitted_raw)
        if payload.get("payment_mode"):
            payment.payment_mode_raw = payload["payment_mode"]
        if payload.get("payment_date"):
            payment.payment_date_raw = payload["payment_date"]
            payment.payment_date = parse_date(payload["payment_date"])
    return student
