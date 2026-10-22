"""
Student endpoints — replaces the Excel DATA sheet CRUD + SEARCH slicer.

Screens served:
  • StudentScreen   – list + add + edit + delete
  • SearchScreen    – multi-field free-text and slicer search
  • AdmissionScreen – admission year / status / round filters
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.deps import db_session, require_user
from app.schemas import Ok, Page, StudentUpdate, StudentWrite
from app.services import student_service

router = APIRouter(prefix="/students")


# ── List / search ─────────────────────────────────────────────────────
@router.get("", summary="List students with optional slicer filters (Excel SEARCH sheet)")
def list_students(
    q: str | None = Query(None, description="Free-text across common student identifiers"),
    unique_id: str | None = Query(None, description="College unique ID"),
    name: str | None = Query(None, description="Student name"),
    enrolment_no: str | None = Query(None, description="Enrollment number"),
    class_roll_no: str | None = Query(None, description="Class roll number"),
    branch: str | None = Query(None, description="Branch code filter (Excel slicer)"),
    admission_year: str | None = Query(None, description="e.g. 2020-21 (Excel slicer)"),
    eligible_category: str | None = Query(None, description="OBC / SC / ST / UR (Excel slicer)"),
    alloted_category: str | None = Query(None, description="Exact alloted category code"),
    gender: str | None = Query(None, description="M or F"),
    ews: str | None = Query(None, description="Y or N"),
    domicile: str | None = Query(None, description="Y or N"),
    religion: str | None = Query(None, description="Religion code"),
    remarks: str | None = Query(None, description="Remarks code"),
    status: str | None = Query(None, description="Admission status code"),
    sort: str = Query("sr_no", description="Sort field: sr_no | name | enrolment_no | admission_year | branch"),
    limit: int = Query(30, ge=1, le=10000),
    offset: int = Query(0, ge=0),
    db: Session = Depends(db_session),
):
    """
    Implements all Excel SEARCH-sheet slicers as query parameters.
    Supports combined filters, free-text search, and pagination.
    Returns a consistent Page envelope so the Android app can show
    total count without loading all rows.
    """
    count, rows = student_service.search_students(
        db,
        q=q,
        unique_id=unique_id,
        name=name,
        enrolment_no=enrolment_no,
        class_roll_no=class_roll_no,
        branch=branch,
        admission_year=admission_year,
        eligible_category=eligible_category,
        alloted_category=alloted_category,
        gender=gender,
        ews=ews,
        domicile=domicile,
        religion=religion,
        remarks=remarks,
        status=status,
        sort=sort,
        limit=limit,
        offset=offset,
    )
    return {
        "success": True,
        "count": count,
        "limit": limit,
        "offset": offset,
        "results": [student_service.list_item(r) for r in rows],
    }


# ── Single student ────────────────────────────────────────────────────
@router.get(
    "/{student_key}",
    summary="Get full student detail by student_id or enrolment_no",
)
def get_student(
    student_key: str,
    db: Session = Depends(db_session),
):
    """
    Returns every field for one student including contact, admission,
    payment history and term-wise fees.
    `student_key` can be the numeric `student_id` or the `enrolment_no`.
    """
    data = student_service.get_student(db, student_key)
    return {"success": True, "message": "OK", "data": data}


# ── Term-wise fees for one student ───────────────────────────────────
@router.get(
    "/{student_key}/fees",
    summary="Get semester-wise scheduled fees (Excel JUL-DEC / JAN-JUN columns)",
)
def get_student_fees(
    student_key: str,
    db: Session = Depends(db_session),
):
    """
    Returns the unpivoted student_term_fee rows — one per academic term.
    Mirrors the 18 JUL-DEC / JAN-JUN columns from the Excel DATA sheet.
    """
    data = student_service.student_fees(db, student_key)
    return {"success": True, "message": "OK", "data": data}


# ── Create ────────────────────────────────────────────────────────────
@router.post("", status_code=201, summary="Add a new student (replaces Excel data-entry form)")
def create_student(
    body: StudentWrite,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    Validates all required fields, resolves lookup FKs, inserts the
    student + contact + admission rows in one transaction.

    Accepts both snake_case API fields and the camelCase names used
    in the current Kotlin Student data class (e.g. `enrollmentNo` or
    `enrolment_no` both work).
    """
    data = student_service.create_student(db, body)
    return {"success": True, "message": "Student created successfully", "data": data}


# ── Update ────────────────────────────────────────────────────────────
@router.put("/{student_key}", summary="Update an existing student's details")
def update_student(
    student_key: str,
    body: StudentUpdate,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    Partial-update: only the fields present in the request body are
    changed. Lookup fields (branch, category, etc.) are re-resolved so
    a code or name can be submitted freely.
    """
    data = student_service.update_student(db, student_key, body)
    return {"success": True, "message": "Student updated successfully", "data": data}


# ── Delete ────────────────────────────────────────────────────────────
@router.delete("/{student_key}", summary="Delete a student and all related records")
def delete_student(
    student_key: str,
    db: Session = Depends(db_session),
    _user: str = Depends(require_user),
):
    """
    Cascades to contact, admission, payment and term-fee rows.
    Requires authentication.
    """
    student_service.delete_student(db, student_key)
    return {"success": True, "message": "Student deleted successfully", "data": None}
