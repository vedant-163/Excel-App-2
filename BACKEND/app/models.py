from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    CHAR,
    Date,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    Numeric,
    SmallInteger,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.mysql import TINYINT
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base




class StaffUser(Base):
    __tablename__ = "staff_user"

    staff_id: Mapped[int] = mapped_column("id", Integer, primary_key=True)
    name: Mapped[str] = mapped_column("full_name", String(150), nullable=False)
    username: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    email: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(32), nullable=False, default="staff")
    is_active: Mapped[bool] = mapped_column(default=True)
    failed_login_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    locked_until: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow
    )


class StaffPasswordReset(Base):
    __tablename__ = "staff_password_reset"

    reset_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    staff_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("staff_user.id", ondelete="CASCADE"), nullable=False, index=True
    )
    code_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    failed_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    expires_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, index=True)
    used_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)



class AuditLog(Base):
    __tablename__ = "audit_log"

    audit_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    staff_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    username: Mapped[str | None] = mapped_column(String(64), nullable=True)
    action: Mapped[str] = mapped_column(String(16), nullable=False)
    path: Mapped[str] = mapped_column(String(255), nullable=False)
    status_code: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class Branch(Base):
    __tablename__ = "branch"

    branch_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    branch_code: Mapped[str] = mapped_column(String(32), unique=True)
    branch_name: Mapped[str | None] = mapped_column(String(100))
    parent_code: Mapped[str | None] = mapped_column(String(32))
    is_fw: Mapped[int] = mapped_column(TINYINT(1), default=0)
    department: Mapped[str | None] = mapped_column(String(100))
    remarks: Mapped[str | None] = mapped_column(String(255))


class FeeCategory(Base):
    __tablename__ = "fee_category"

    fee_category_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    category_code: Mapped[str] = mapped_column(String(64), unique=True)
    category_name: Mapped[str] = mapped_column(String(128))


class EligibleCategory(Base):
    __tablename__ = "eligible_category"

    eligible_category_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    category_code: Mapped[str] = mapped_column(String(16), unique=True)
    category_name: Mapped[str] = mapped_column(String(64))


class AllotedCategory(Base):
    __tablename__ = "alloted_category"

    alloted_category_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    category_code: Mapped[str] = mapped_column(String(64), unique=True)
    eligible_group: Mapped[str | None] = mapped_column(String(16))
    description: Mapped[str | None] = mapped_column(String(128))


class Religion(Base):
    __tablename__ = "religion"

    religion_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    religion_code: Mapped[str] = mapped_column(String(32), unique=True)
    religion_name: Mapped[str] = mapped_column(String(64))


class AdmissionStatus(Base):
    __tablename__ = "admission_status"

    status_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    status_code: Mapped[str] = mapped_column(String(32), unique=True)
    status_name: Mapped[str] = mapped_column(String(64))


class AdmissionRound(Base):
    __tablename__ = "admission_round"

    round_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    round_code: Mapped[str] = mapped_column(String(64), unique=True)
    round_name: Mapped[str] = mapped_column(String(128))


class PaymentMode(Base):
    __tablename__ = "payment_mode"

    payment_mode_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    mode_code: Mapped[str] = mapped_column(String(64), unique=True)
    mode_name: Mapped[str] = mapped_column(String(128))
    is_grouped: Mapped[int] = mapped_column(TINYINT(1), default=0)


class RemarkType(Base):
    __tablename__ = "remark_type"

    remark_type_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    remark_code: Mapped[str] = mapped_column(String(64), unique=True)
    remark_name: Mapped[str] = mapped_column(String(128))


class AcademicTerm(Base):
    __tablename__ = "academic_term"

    term_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    term_code: Mapped[str] = mapped_column(String(16), unique=True)
    excel_column: Mapped[str] = mapped_column(String(16))
    calendar_year: Mapped[int] = mapped_column(SmallInteger)
    term_half: Mapped[str] = mapped_column(Enum("JUL_DEC", "JAN_JUN"))
    sort_order: Mapped[int] = mapped_column(SmallInteger)


class FeeStructure(Base):
    __tablename__ = "fee_structure"

    fee_structure_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    fee_mast_code: Mapped[str] = mapped_column(String(64), unique=True)
    fee_category_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("fee_category.fee_category_id")
    )
    structure_year: Mapped[int] = mapped_column(SmallInteger)
    branch_code: Mapped[str | None] = mapped_column(String(64))

    category: Mapped[FeeCategory] = relationship()
    semesters: Mapped[list["FeeStructureSemester"]] = relationship(
        back_populates="fee_structure",
        cascade="all, delete-orphan",
    )


class FeeStructureSemester(Base):
    __tablename__ = "fee_structure_semester"
    __table_args__ = (UniqueConstraint("fee_structure_id", "semester_no"),)

    fee_structure_semester_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    fee_structure_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("fee_structure.fee_structure_id", ondelete="CASCADE")
    )
    semester_no: Mapped[int] = mapped_column(TINYINT)
    amount: Mapped[Decimal] = mapped_column(Numeric(12, 2))

    fee_structure: Mapped[FeeStructure] = relationship(back_populates="semesters")


class FeeYearSemCode(Base):
    __tablename__ = "fee_year_sem_code"

    year_sem_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    structure_year: Mapped[int] = mapped_column(SmallInteger)
    sem_code: Mapped[str] = mapped_column(String(32))
    year_sem: Mapped[str] = mapped_column(String(64), unique=True)
    semester_no: Mapped[int | None] = mapped_column(SmallInteger)


class Student(Base):
    __tablename__ = "student"

    student_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    sr_no: Mapped[int] = mapped_column(Integer, unique=True)
    college_unique_id: Mapped[str | None] = mapped_column(String(32))
    class_roll_no: Mapped[str] = mapped_column(String(32))
    enrolment_no: Mapped[str] = mapped_column(String(32), unique=True)
    name: Mapped[str] = mapped_column(String(150))
    father_name: Mapped[str | None] = mapped_column(String(150))
    mother_name: Mapped[str | None] = mapped_column(String(150))
    rank_text: Mapped[str | None] = mapped_column(String(32))
    marks: Mapped[Decimal | None] = mapped_column(Numeric(10, 6))
    exam_roll_no: Mapped[str | None] = mapped_column(String(32))
    date_of_birth: Mapped[date | None] = mapped_column(Date)
    branch_id: Mapped[int] = mapped_column(Integer, ForeignKey("branch.branch_id"))
    eligible_category_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("eligible_category.eligible_category_id")
    )
    alloted_category_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("alloted_category.alloted_category_id")
    )
    domicile: Mapped[str] = mapped_column(Enum("Y", "N"))
    gender: Mapped[str] = mapped_column(Enum("M", "F", "O"))
    ews: Mapped[str] = mapped_column(Enum("Y", "N"))
    religion_id: Mapped[int] = mapped_column(Integer, ForeignKey("religion.religion_id"))
    # A student record can be created before the fee schedule is configured.
    # Fee assignment is optional and can be completed later from Fee Structure.
    fee_structure_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("fee_structure.fee_structure_id"), nullable=True
    )
    mponline_password: Mapped[str | None] = mapped_column(String(128))

    branch: Mapped[Branch] = relationship()
    eligible_category: Mapped[EligibleCategory] = relationship()
    alloted_category: Mapped[AllotedCategory] = relationship()
    religion: Mapped[Religion] = relationship()
    fee_structure: Mapped[FeeStructure] = relationship()
    contact: Mapped["Contact | None"] = relationship(
        back_populates="student", cascade="all, delete-orphan", uselist=False
    )
    admission: Mapped["Admission | None"] = relationship(
        back_populates="student", cascade="all, delete-orphan", uselist=False
    )
    payments: Mapped[list["Payment"]] = relationship(
        back_populates="student", cascade="all, delete-orphan"
    )
    term_fees: Mapped[list["StudentTermFee"]] = relationship(
        back_populates="student", cascade="all, delete-orphan"
    )


class Contact(Base):
    __tablename__ = "contact"

    contact_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    student_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("student.student_id", ondelete="CASCADE"), unique=True
    )
    student_phone: Mapped[str | None] = mapped_column(String(50))
    student_email: Mapped[str | None] = mapped_column(String(150))
    parents_phone: Mapped[str | None] = mapped_column(String(50))
    parents_email: Mapped[str | None] = mapped_column(String(150))
    aadhaar_no: Mapped[str | None] = mapped_column(String(32))
    aadhaar_linked_mobile: Mapped[str | None] = mapped_column(String(32))
    permanent_address: Mapped[str | None] = mapped_column(String(500))
    present_address: Mapped[str | None] = mapped_column(String(500))

    student: Mapped[Student] = relationship(back_populates="contact")


class Admission(Base):
    __tablename__ = "admission"

    admission_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    student_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("student.student_id", ondelete="CASCADE"), unique=True
    )
    admission_no: Mapped[str | None] = mapped_column(String(32))
    status_id: Mapped[int] = mapped_column(Integer, ForeignKey("admission_status.status_id"))
    round_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("admission_round.round_id"))
    admission_year: Mapped[str] = mapped_column(String(16))
    admission_year_start: Mapped[int] = mapped_column(SmallInteger)
    offline_category: Mapped[str | None] = mapped_column(String(64))
    remark_type_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("remark_type.remark_type_id")
    )
    remarks_raw: Mapped[str | None] = mapped_column(String(128))

    student: Mapped[Student] = relationship(back_populates="admission")
    status: Mapped[AdmissionStatus] = relationship()
    round: Mapped[AdmissionRound | None] = relationship()
    remark_type: Mapped[RemarkType | None] = relationship()


class Payment(Base):
    __tablename__ = "payment"

    payment_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    student_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("student.student_id", ondelete="CASCADE")
    )
    academic_year: Mapped[str | None] = mapped_column(String(16))
    fee_category: Mapped[str | None] = mapped_column(String(32))
    semester_no: Mapped[int | None] = mapped_column(TINYINT)
    total_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    total_amount_raw: Mapped[str | None] = mapped_column(String(128))
    student_submitted_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    student_submitted_amount_raw: Mapped[str | None] = mapped_column(String(128))
    online_application_txn_id: Mapped[str | None] = mapped_column(String(255))
    payment_mode_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("payment_mode.payment_mode_id")
    )
    payment_mode_raw: Mapped[str | None] = mapped_column(String(128))
    payment_date: Mapped[date | None] = mapped_column(Date)
    payment_date_raw: Mapped[str | None] = mapped_column(String(64))
    dte_fee: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    payment_status: Mapped[str | None] = mapped_column(String(32))
    payment_reference_no: Mapped[str | None] = mapped_column(String(64))

    student: Mapped[Student] = relationship(back_populates="payments")
    payment_mode: Mapped[PaymentMode | None] = relationship()


class StudentTermFee(Base):
    __tablename__ = "student_term_fee"
    __table_args__ = (UniqueConstraint("student_id", "term_id"),)

    student_term_fee_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    student_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("student.student_id", ondelete="CASCADE")
    )
    term_id: Mapped[int] = mapped_column(Integer, ForeignKey("academic_term.term_id"))
    amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=0)

    student: Mapped[Student] = relationship(back_populates="term_fees")
    term: Mapped[AcademicTerm] = relationship()


class StudentSearchView(Base):
    """Read-only mapping of v_student_search (Excel SEARCH sheet)."""

    __tablename__ = "v_student_search"

    student_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    sr_no: Mapped[int | None] = mapped_column(Integer)
    college_unique_id: Mapped[str | None] = mapped_column(String(32))
    class_roll_no: Mapped[str | None] = mapped_column(String(32))
    enrolment_no: Mapped[str | None] = mapped_column(String(32))
    name: Mapped[str | None] = mapped_column(String(150))
    father_name: Mapped[str | None] = mapped_column(String(150))
    mother_name: Mapped[str | None] = mapped_column(String(150))
    rank_text: Mapped[str | None] = mapped_column(String(32))
    marks: Mapped[Decimal | None] = mapped_column(Numeric(10, 6))
    exam_roll_no: Mapped[str | None] = mapped_column(String(32))
    date_of_birth: Mapped[date | None] = mapped_column(Date)
    branch_code: Mapped[str | None] = mapped_column(String(32))
    branch_name: Mapped[str | None] = mapped_column(String(100))
    eligible_category: Mapped[str | None] = mapped_column(String(16))
    alloted_category: Mapped[str | None] = mapped_column(String(64))
    domicile: Mapped[str | None] = mapped_column(CHAR(1))
    gender: Mapped[str | None] = mapped_column(CHAR(1))
    ews: Mapped[str | None] = mapped_column(CHAR(1))
    religion: Mapped[str | None] = mapped_column(String(32))
    fee_structure: Mapped[str | None] = mapped_column(String(64))
    fee_structure_year: Mapped[int | None] = mapped_column(SmallInteger)
    admission_year: Mapped[str | None] = mapped_column(String(16))
    admission_year_start: Mapped[int | None] = mapped_column(SmallInteger)
    admission_status: Mapped[str | None] = mapped_column(String(32))
    admission_round: Mapped[str | None] = mapped_column(String(64))
    remarks: Mapped[str | None] = mapped_column(String(64))
    student_phone: Mapped[str | None] = mapped_column(String(50))
    student_email: Mapped[str | None] = mapped_column(String(150))
    parents_phone: Mapped[str | None] = mapped_column(String(50))
    parents_email: Mapped[str | None] = mapped_column(String(150))
    aadhaar_no: Mapped[str | None] = mapped_column(String(32))
    permanent_address: Mapped[str | None] = mapped_column(String(500))
    present_address: Mapped[str | None] = mapped_column(String(500))
    total_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    student_submitted_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    online_application_txn_id: Mapped[str | None] = mapped_column(String(255))
    payment_mode: Mapped[str | None] = mapped_column(String(128))
    payment_date: Mapped[date | None] = mapped_column(Date)
    dte_fee: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    paid_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
    pending_amount: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))
