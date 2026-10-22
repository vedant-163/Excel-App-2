from datetime import date
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.services.excel_logic import (
    normalize_admission_year,
    normalize_gender,
    normalize_yn,
    parse_amount,
    parse_date,
)


class Ok(BaseModel):
    success: bool = True
    message: str
    data: Any = None


class Page(BaseModel):
    count: int
    limit: int
    offset: int
    results: list[Any]


class LoginRequest(BaseModel):
    username: str
    password: str


class ForgotPasswordRequest(BaseModel):
    email: str = Field(min_length=5, max_length=255)


class ResetPasswordRequest(BaseModel):
    email: str = Field(min_length=5, max_length=255)
    code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")
    new_password: str = Field(min_length=10, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    username: str
    role: str


class StudentListItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    student_id: int
    sr_no: int | None = None
    college_unique_id: str | None = None
    enrolment_no: str | None = None
    class_roll_no: str | None = None
    name: str | None = None
    branch_code: str | None = None
    eligible_category: str | None = None
    alloted_category: str | None = None
    gender: str | None = None
    ews: str | None = None
    domicile: str | None = None
    religion: str | None = None
    admission_year: str | None = None
    admission_status: str | None = None
    admission_round: str | None = None
    remarks: str | None = None
    fee_structure: str | None = None
    student_phone: str | None = None
    total_amount: float | None = None
    paid_amount: float | None = None
    pending_amount: float | None = None


class TermFeeOut(BaseModel):
    term_code: str
    calendar_year: int
    term_half: str
    sort_order: int
    scheduled_fee: float


class PaymentOut(BaseModel):
    payment_id: int
    student_id: int
    total_amount: float | None = None
    total_amount_raw: str | None = None
    student_submitted_amount: float | None = None
    student_submitted_amount_raw: str | None = None
    paid_amount: float | None = None
    pending_amount: float | None = None
    online_application_txn_id: str | None = None
    payment_mode: str | None = None
    payment_mode_raw: str | None = None
    payment_date: date | None = None
    payment_date_raw: str | None = None
    dte_fee: float | None = None
    payment_status: str | None = None
    payment_reference_no: str | None = None


class StudentWrite(BaseModel):
    """Accepts API snake_case and the current Kotlin camelCase field names."""

    name: str
    enrolment_no: str | None = None
    enrollmentNo: str | None = None
    class_roll_no: str | None = None
    classRollNo: str | None = None
    college_unique_id: str | None = None
    uniqueId: str | None = None
    exam_roll_no: str | None = None
    rollNo: str | None = None
    father_name: str | None = None
    father: str | None = None
    mother_name: str | None = None
    mother: str | None = None
    rank_text: str | None = None
    rank: str | None = None
    marks: str | float | int | None = None
    date_of_birth: str | None = None
    dateOfBirth: str | None = None
    branch: str | None = None
    branch_code: str | None = None
    eligible_category: str | None = None
    eligibleCategory: str | None = None
    alloted_category: str | None = None
    allocatedCategory: str | None = None
    domicile: str | None = "N"
    gender: str | None = None
    ews: str | None = "N"
    religion: str | None = None
    fee_structure: str | None = None
    fee_mast_code: str | None = None
    student_phone: str | None = None
    studentPhone: str | None = None
    student_email: str | None = None
    email: str | None = None
    parents_phone: str | None = None
    parentsPhone: str | None = None
    parents_email: str | None = None
    parentsEmail: str | None = None
    aadhaar_no: str | None = None
    aadhaarNo: str | None = None
    aadhaar_linked_mobile: str | None = None
    aadhaarLinkedMobile: str | None = None
    permanent_address: str | None = None
    permanentAddress: str | None = None
    present_address: str | None = None
    presentAddress: str | None = None
    admission_year: str | None = None
    admissionYear: str | None = None
    admission_no: str | None = None
    status: str | None = None
    round: str | None = None
    remarks: str | None = None
    offline_category: str | None = None
    offlineAdmissionCategory: str | None = None
    mponline_password: str | None = None
    total_amount: str | float | int | None = None
    student_submitted_amount: str | float | int | None = None
    payment_mode: str | None = None
    payment_date: str | None = None

    @field_validator("gender")
    @classmethod
    def validate_gender(cls, value: str | None) -> str | None:
        if value is None or not value.strip():
            return value
        normalize_gender(value)
        return value

    def pick(self, *names: str) -> str | None:
        data = self.model_dump()
        for name in names:
            value = data.get(name)
            if value is not None and str(value).strip() != "":
                return str(value).strip()
        return None


class StudentUpdate(StudentWrite):
    name: str | None = None


class AdmissionUpdate(BaseModel):
    admission_year: str
    branch: str
    eligible_category: str
    allocated_category: str
    offline_category: str | None = None
    round: str | None = None
    status: str
    remarks: str | None = None


class PaymentWrite(BaseModel):
    student_id: int | None = None
    enrolment_no: str | None = None
    academic_year: str | None = None
    fee_category: str | None = None
    semester_no: int | None = Field(default=None, ge=1, le=8)
    academicYear: str | None = None
    feeCategory: str | None = None
    semester: str | None = None
    total_amount: str | float | int | None = None
    total_amount_raw: str | None = None
    student_submitted_amount: str | float | int | None = None
    student_submitted_amount_raw: str | None = None
    paidAmount: str | float | int | None = None
    totalFee: str | float | int | None = None
    online_application_txn_id: str | None = None
    transactionId: str | None = None
    payment_mode: str | None = None
    paymentMode: str | None = None
    payment_date: str | None = None
    paymentDate: str | None = None
    dte_fee: str | float | int | None = None
    payment_status: str | None = None
    payment_reference_no: str | None = None
    create_new: bool = False


class FeeSemesterIn(BaseModel):
    semester_no: int = Field(ge=1, le=8)
    amount: str | float | int

    @field_validator("amount")
    @classmethod
    def parse_sem_amount(cls, value: str | float | int) -> float:
        return float(parse_amount(value))


class FeeStructureWrite(BaseModel):
    fee_mast_code: str
    fee_category: str
    structure_year: int
    branch_code: str | None = None
    branch: str | None = None
    semesters: list[FeeSemesterIn] = Field(min_length=1, max_length=8)


class FeeStructureSemesterUpsert(BaseModel):
    academic_year: str
    branch: str
    fee_category: str
    semester_no: int = Field(ge=1, le=8)
    amount: str | float | int

    @field_validator("amount")
    @classmethod
    def parse_amount_value(cls, value: str | float | int) -> float:
        return float(parse_amount(value))


class BranchWrite(BaseModel):
    branch_code: str
    branch_name: str | None = None
    department: str | None = None
    remarks: str | None = None
    is_fw: bool = False
    parent_code: str | None = None


def normalized_student_payload(body: StudentWrite) -> dict:
    gender = body.pick("gender")
    domicile = body.pick("domicile")
    ews = body.pick("ews")
    dob = body.pick("date_of_birth", "dateOfBirth")
    year = body.pick("admission_year", "admissionYear")
    marks = body.pick("marks")
    return {
        "name": body.name.strip() if body.name else None,
        "enrolment_no": body.pick("enrolment_no", "enrollmentNo"),
        "class_roll_no": body.pick("class_roll_no", "classRollNo"),
        "college_unique_id": body.pick("college_unique_id", "uniqueId"),
        "exam_roll_no": body.pick("exam_roll_no", "rollNo"),
        "father_name": body.pick("father_name", "father"),
        "mother_name": body.pick("mother_name", "mother"),
        "rank_text": body.pick("rank_text", "rank"),
        "marks": parse_amount(marks) if marks else None,
        "date_of_birth": parse_date(dob) if dob else None,
        "branch": body.pick("branch_code", "branch"),
        "eligible_category": body.pick("eligible_category", "eligibleCategory"),
        "alloted_category": body.pick("alloted_category", "allocatedCategory"),
        "domicile": normalize_yn(domicile, "domicile") or "N",
        "gender": normalize_gender(gender),
        "ews": normalize_yn(ews, "ews") or "N",
        "religion": body.pick("religion"),
        "fee_structure": body.pick("fee_structure", "fee_mast_code"),
        "student_phone": body.pick("student_phone", "studentPhone"),
        "student_email": body.pick("student_email", "email"),
        "parents_phone": body.pick("parents_phone", "parentsPhone"),
        "parents_email": body.pick("parents_email", "parentsEmail"),
        "aadhaar_no": body.pick("aadhaar_no", "aadhaarNo"),
        "aadhaar_linked_mobile": body.pick(
            "aadhaar_linked_mobile", "aadhaarLinkedMobile"
        ),
        "permanent_address": body.pick("permanent_address", "permanentAddress"),
        "present_address": body.pick("present_address", "presentAddress"),
        "admission_year": normalize_admission_year(year),
        "admission_no": body.pick("admission_no"),
        "status": body.pick("status"),
        "round": body.pick("round"),
        "remarks": body.pick("remarks"),
        "offline_category": body.pick("offline_category", "offlineAdmissionCategory"),
        "mponline_password": body.pick("mponline_password"),
        "total_amount_raw": body.pick("total_amount"),
        "submitted_raw": body.pick("student_submitted_amount"),
        "payment_mode": body.pick("payment_mode"),
        "payment_date": body.pick("payment_date"),
    }
