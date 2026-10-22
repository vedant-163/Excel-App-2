"""
Reports endpoints — replaces Excel REPORT 20-21 onwards pivot/summary sheet.

Screens served:
  • ReportsScreen – branch × category, EWS, domicile, religion breakdowns
    and admission-year summaries
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import and_, case, func, or_, select
from sqlalchemy.orm import Session

from app.deps import db_session
from app.exceptions import AppError
from app.models import StudentSearchView
from app.services.excel_logic import normalize_admission_year

router = APIRouter(prefix="/reports")


# ── Paginated student rows for Android report drill-down ──────────────
@router.get("/students", summary="Filtered and paginated student rows for Android reports")
def report_students(
    year: str | None = Query(None),
    branch: str | None = Query(None),
    category: str | None = Query(None),
    limit: int = Query(10000, ge=1, le=10000),
    offset: int = Query(0, ge=0),
    db: Session = Depends(db_session),
):
    """Filter in SQL so the Android app does not download the entire student table.
    Branch labels from the UI are matched against common database branch codes/names.
    Category matches either eligible or allocated category, as the report UI displays
    the allocated category when present and otherwise falls back to eligible category.
    """
    from sqlalchemy import or_

    filters = []
    if year:
        filters.append(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )
    if branch:
        # The Android dropdown can send either a branch code or a display name.
        # Match both columns; older code matched branch_code only, so selecting
        # labels such as "Computer Science & Engineering" returned zero rows.
        label = branch.strip().lower()
        aliases = {
            "computer science and engineering": ["CSE", "Computer Science", "Computer Science & Engineering", "Computer Science and Engineering"],
            "computer science & engineering": ["CSE", "Computer Science", "Computer Science & Engineering", "Computer Science and Engineering"],
            "information technology": ["IT", "Information Technology"],
            "electronics and telecommunication engineering": ["ETC", "E&TC", "Electronics & Telecommunication Engineering", "Electronics and Telecommunication Engineering"],
            "electronics & telecommunication engineering": ["ETC", "E&TC", "Electronics & Telecommunication Engineering", "Electronics and Telecommunication Engineering"],
            "electrical engineering": ["EE", "Electrical", "Electrical Engineering"],
            "electronics and instrumentation engineering": ["E&I", "EI", "Electronics & Instrumentation Engineering", "Electronics and Instrumentation Engineering"],
            "electronics & instrumentation engineering": ["E&I", "EI", "Electronics & Instrumentation Engineering", "Electronics and Instrumentation Engineering"],
            "mechanical engineering": ["ME", "Mechanical", "Mechanical Engineering"],
            "civil engineering": ["CE", "Civil", "Civil Engineering"],
            "industrial and production engineering": ["I&P", "Industrial & Production Engineering", "Industrial and Production Engineering"],
            "industrial & production engineering": ["I&P", "Industrial & Production Engineering", "Industrial and Production Engineering"],
            "biomedical engineering": ["Biomedical", "Biomedical Engineering"],
            "bachelor of pharmacy": ["B.Pharmacy", "B Pharmacy", "BPharm", "B.Pharm", "Pharmacy", "Bachelor of Pharmacy"],
            "artificial intelligence and robotics": ["AI & Robotics", "AI and Robotics", "Artificial Intelligence & Robotics", "Artificial Intelligence and Robotics"],
            "artificial intelligence & robotics": ["AI & Robotics", "AI and Robotics", "Artificial Intelligence & Robotics", "Artificial Intelligence and Robotics"],
        }
        values = list(dict.fromkeys([branch.strip(), *aliases.get(label, [])]))
        filters.append(or_(*[
            func.lower(func.trim(StudentSearchView.branch_code)) == value.lower()
            for value in values
        ] + [
            func.lower(func.trim(StudentSearchView.branch_name)) == value.lower()
            for value in values
        ]))
    if category:
        # Match the report UI's category rule: show allocated category when it
        # exists; otherwise fall back to eligible category. OR-ing both columns
        # caused records to be fetched that the UI then filtered back out.
        normalized_allotted = func.upper(func.trim(func.coalesce(StudentSearchView.alloted_category, "")))
        normalized_eligible = func.upper(func.trim(func.coalesce(StudentSearchView.eligible_category, "")))
        # The UI uses the allocated category only when it is one of the report
        # categories; detailed allocation labels (e.g. sub-category codes) fall
        # back to the student's eligible category. Keep SQL filtering identical.
        standard_categories = ("UR", "OBC", "SC", "ST", "OTHER")
        effective_category = case(
            (normalized_allotted.in_(standard_categories), normalized_allotted),
            (normalized_eligible.in_(standard_categories), normalized_eligible),
            else_="OTHER",
        )
        filters.append(effective_category == category.strip().upper())

    where = and_(*filters) if filters else True
    total = db.scalar(select(func.count()).select_from(StudentSearchView).where(where)) or 0
    rows = db.scalars(
        select(StudentSearchView).where(where)
        .order_by(StudentSearchView.sr_no, StudentSearchView.student_id)
        .limit(limit).offset(offset)
    ).all()
    from app.services.student_service import list_item
    return {"success": True, "count": int(total), "limit": limit, "offset": offset,
            "results": [list_item(row) for row in rows]}



# ── Branch × category pivot (SP sp_report_branch_category) ───────────
@router.get(
    "/branch-category",
    summary="Branch × eligible-category × gender pivot (Excel REPORT table)",
)
def report_branch_category(
    year: str | None = Query(None, description="Admission year e.g. 2020-21"),
    db: Session = Depends(db_session),
):
    """
    Replicates the Excel REPORT 20-21 pivot:
    Branch × OBC/SC/ST/UR × Male/Female counts.
    Equivalent to stored procedure sp_report_branch_category.
    """
    stmt = (
        select(
            StudentSearchView.branch_code,
            StudentSearchView.eligible_category,
            StudentSearchView.gender,
            func.count().label("student_count"),
        )
        .group_by(
            StudentSearchView.branch_code,
            StudentSearchView.eligible_category,
            StudentSearchView.gender,
        )
        .order_by(StudentSearchView.branch_code)
    )
    if year:
        stmt = stmt.where(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )

    rows = db.execute(stmt).mappings().all()

    # Pivot: branch → {obc_m, obc_f, sc_m, …, grand_total}
    pivot: dict[str, dict] = {}
    categories = ["OBC", "SC", "ST", "UR"]
    for row in rows:
        branch = row["branch_code"] or "UNKNOWN"
        cat = (row["eligible_category"] or "").upper()
        gender = (row["gender"] or "").upper()
        count = int(row["student_count"])
        if branch not in pivot:
            pivot[branch] = {
                "branch_code": branch,
                **{f"{c.lower()}_{g}": 0 for c in categories for g in ("m", "f", "total")},
                "grand_total": 0,
            }
        if cat in categories:
            g_key = "m" if gender == "M" else "f" if gender == "F" else None
            if g_key:
                pivot[branch][f"{cat.lower()}_{g_key}"] += count
            pivot[branch][f"{cat.lower()}_total"] += count
        pivot[branch]["grand_total"] += count

    return {"success": True, "message": "OK", "data": list(pivot.values())}


# ── EWS breakdown ─────────────────────────────────────────────────────
@router.get("/branch-ews", summary="Branch × EWS student count")
def report_branch_ews(
    year: str | None = Query(None),
    db: Session = Depends(db_session),
):
    stmt = (
        select(
            StudentSearchView.branch_code,
            StudentSearchView.ews,
            func.count().label("student_count"),
        )
        .group_by(StudentSearchView.branch_code, StudentSearchView.ews)
        .order_by(StudentSearchView.branch_code)
    )
    if year:
        stmt = stmt.where(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )
    rows = db.execute(stmt).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


# ── Domicile breakdown ────────────────────────────────────────────────
@router.get("/branch-domicile", summary="Branch × domicile student count")
def report_branch_domicile(
    year: str | None = Query(None),
    db: Session = Depends(db_session),
):
    stmt = (
        select(
            StudentSearchView.branch_code,
            StudentSearchView.domicile,
            func.count().label("student_count"),
        )
        .group_by(StudentSearchView.branch_code, StudentSearchView.domicile)
        .order_by(StudentSearchView.branch_code)
    )
    if year:
        stmt = stmt.where(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )
    rows = db.execute(stmt).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


# ── Religion breakdown ────────────────────────────────────────────────
@router.get("/branch-religion", summary="Branch × religion student count")
def report_branch_religion(
    year: str | None = Query(None),
    db: Session = Depends(db_session),
):
    stmt = (
        select(
            StudentSearchView.branch_code,
            StudentSearchView.religion,
            func.count().label("student_count"),
        )
        .group_by(StudentSearchView.branch_code, StudentSearchView.religion)
        .order_by(StudentSearchView.branch_code)
    )
    if year:
        stmt = stmt.where(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )
    rows = db.execute(stmt).mappings().all()
    return {
        "success": True,
        "data": [dict(r) for r in rows],
    }


# ── Admission-year summary ────────────────────────────────────────────
@router.get(
    "/admission-year-summary",
    summary="Per admission-year student and fee totals",
)
def admission_year_summary(db: Session = Depends(db_session)):
    """
    Mirrors v_admission_year_summary view.
    Used by the Reports screen year-on-year trend card.
    """
    stmt = (
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
    rows = db.execute(stmt).mappings().all()
    return {
        "success": True,
        "data": [
            {
                "admission_year": row["admission_year"],
                "student_count": int(row["student_count"]),
                "sum_total_amount": float(row["sum_total_amount"] or 0),
                "sum_submitted_amount": float(row["sum_submitted_amount"] or 0),
            }
            for row in rows
        ],
    }


# ── Fee payment status breakdown ──────────────────────────────────────
@router.get("/fee-status", summary="Count of students with pending vs fully-paid fees")
def report_fee_status(
    year: str | None = Query(None),
    db: Session = Depends(db_session),
):
    """
    Calculates per-student pending_amount and groups into
    'Fully Paid' / 'Partially Paid' / 'Unpaid' buckets.
    Useful for the Reports screen payment-status chart.
    """
    stmt = select(
        StudentSearchView.branch_code,
        StudentSearchView.pending_amount,
        StudentSearchView.total_amount,
        StudentSearchView.paid_amount,
    )
    if year:
        stmt = stmt.where(
            StudentSearchView.admission_year == normalize_admission_year(year)
        )

    rows = db.execute(stmt).mappings().all()
    fully_paid = partially_paid = unpaid = 0
    for row in rows:
        total = float(row["total_amount"] or 0)
        paid = float(row["paid_amount"] or 0)
        pending = float(row["pending_amount"] or 0)
        if total <= 0:
            continue
        if pending <= 0:
            fully_paid += 1
        elif paid > 0:
            partially_paid += 1
        else:
            unpaid += 1

    return {
        "success": True,
        "data": {
            "fully_paid": fully_paid,
            "partially_paid": partially_paid,
            "unpaid": unpaid,
            "total": fully_paid + partially_paid + unpaid,
        },
    }


# ── Generic group counts (used by the Android Reports screen) ─────────
_GROUP_COLUMNS = {
    "branch": StudentSearchView.branch_code,
    "category": StudentSearchView.eligible_category,
    "status": StudentSearchView.admission_status,
    "ews": StudentSearchView.ews,
    "domicile": StudentSearchView.domicile,
    "religion": StudentSearchView.religion,
}


@router.get(
    "/group-counts",
    summary="Student counts grouped by one attribute, computed in SQL",
)
def report_group_counts(
    group_by: str = Query(..., description="branch | category | status | ews | domicile | religion"),
    year: str | None = Query(None, description="Admission year, 2020 or 2020-21"),
    branch: str | None = Query(None),
    eligible_category: str | None = Query(None),
    db: Session = Depends(db_session),
):
    """
    Returns [{"group": <value or null>, "student_count": n}] plus the grand total,
    so the app never has to download every student to build a report.
    """
    column = _GROUP_COLUMNS.get(group_by.lower())
    if column is None:
        raise AppError(
            400,
            f"group_by must be one of: {', '.join(sorted(_GROUP_COLUMNS))}",
        )
    stmt = select(column.label("grp"), func.count().label("student_count")).group_by(column).order_by(column)
    if year:
        stmt = stmt.where(StudentSearchView.admission_year == normalize_admission_year(year))
    if branch:
        stmt = stmt.where(StudentSearchView.branch_code == branch)
    if eligible_category:
        stmt = stmt.where(StudentSearchView.eligible_category == eligible_category)

    rows = db.execute(stmt).mappings().all()
    data = [{"group": r["grp"], "student_count": int(r["student_count"])} for r in rows]
    return {
        "success": True,
        "group_by": group_by.lower(),
        "total": sum(d["student_count"] for d in data),
        "data": data,
    }
