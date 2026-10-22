"""
excel_sync.py - Excel ↔ MySQL Database Synchronization Service

Design:
  - Source of truth: Database for app-edited records, Excel for bulk imports.
  - Primary sync direction: Excel → Database (import mode).
  - Conflict resolution: DB wins for records modified post-import; Excel wins for never-touched records.
  - Logging: All operations are logged to sync_log table and a local file.

Usage:
    python -m app.services.excel_sync --excel /path/to/workbook.xlsx --mode import
    python -m app.services.excel_sync --excel /path/to/workbook.xlsx --mode report
"""

import argparse
import logging
import os
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

# These are optional at import time – only required when the script is run.
try:
    import openpyxl
    _OPENPYXL_AVAILABLE = True
except ImportError:
    _OPENPYXL_AVAILABLE = False

try:
    from sqlalchemy import create_engine, text, select
    from sqlalchemy.orm import Session
    _SQLALCHEMY_AVAILABLE = True
except ImportError:
    _SQLALCHEMY_AVAILABLE = False


# ─── Logging setup ─────────────────────────────────────────────────────────
LOG_FORMAT = "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)
logger = logging.getLogger("excel_sync")


# ─── Result tracking ────────────────────────────────────────────────────────
@dataclass
class SyncResult:
    """Tracks counts and errors from a sync operation."""
    students_inserted: int = 0
    students_updated: int = 0
    students_skipped: int = 0
    students_failed: int = 0
    payments_inserted: int = 0
    payments_skipped: int = 0
    payments_failed: int = 0
    fee_structures_inserted: int = 0
    fee_structures_updated: int = 0
    fee_structures_failed: int = 0
    errors: list = field(default_factory=list)
    warnings: list = field(default_factory=list)

    def summary(self) -> str:
        lines = [
            "=== Excel → Database Sync Summary ===",
            f"Students:      inserted={self.students_inserted}, updated={self.students_updated}, "
            f"skipped={self.students_skipped}, failed={self.students_failed}",
            f"Payments:      inserted={self.payments_inserted}, skipped={self.payments_skipped}, "
            f"failed={self.payments_failed}",
            f"Fee Structures: inserted={self.fee_structures_inserted}, updated={self.fee_structures_updated}, "
            f"failed={self.fee_structures_failed}",
        ]
        if self.warnings:
            lines.append(f"Warnings ({len(self.warnings)}):")
            for w in self.warnings[:20]:
                lines.append(f"  ⚠ {w}")
        if self.errors:
            lines.append(f"Errors ({len(self.errors)}):")
            for e in self.errors[:20]:
                lines.append(f"  ✗ {e}")
        return "\n".join(lines)


# ─── Excel column mapping ────────────────────────────────────────────────────
# These map Excel DATA sheet column names to internal field names.
# Adjust if the actual workbook has different column headers.
STUDENT_COLUMN_MAP = {
    # Excel column header → internal field name
    "Enrollment No": "enrolment_no",
    "ENROLLMENT NO": "enrolment_no",
    "Enrolment No": "enrolment_no",
    "Name": "name",
    "NAME": "name",
    "Student Name": "name",
    "Branch": "branch",
    "BRANCH": "branch",
    "Gender": "gender",
    "GENDER": "gender",
    "Father Name": "father",
    "Father": "father",
    "Mother Name": "mother",
    "Mother": "mother",
    "Date of Birth": "date_of_birth",
    "DOB": "date_of_birth",
    "Mobile": "student_phone",
    "Student Mobile": "student_phone",
    "Email": "email",
    "Student Email": "email",
    "Category": "eligible_category",
    "Eligible Category": "eligible_category",
    "Allocated Category": "allocated_category",
    "Alloted Category": "allocated_category",
    "Admission Year": "admission_year",
    "ADMISSION YEAR": "admission_year",
    "Status": "status",
    "Admission Status": "status",
    "Round": "round",
    "Admission Round": "round",
    "Religion": "religion",
    "Domicile": "domicile",
    "EWS": "ews",
    "Unique ID": "college_unique_id",
    "College Unique ID": "college_unique_id",
    "Class Roll No": "class_roll_no",
    "Roll No": "roll_no",
    "Rank": "rank",
    "Marks": "marks",
    "Remarks": "remarks",
    "Aadhaar No": "aadhaar_no",
    "Aadhaar Linked Mobile": "aadhaar_linked_mobile",
    "Parent Mobile": "parents_phone",
    "Parents Mobile": "parents_phone",
    "Parent Email": "parents_email",
    "Parents Email": "parents_email",
    "Permanent Address": "permanent_address",
    "Present Address": "present_address",
}

# Columns that contain semester fee amounts (one per semester)
SEMESTER_FEE_PATTERN = ["S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8",
                         "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8",
                         "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                         "Semester 5", "Semester 6", "Semester 7", "Semester 8"]

FEE_MAST_COLUMN_MAP = {
    "Fee Mast Code": "fee_mast_code",
    "Code": "fee_mast_code",
    "Fee Category": "fee_category",
    "Category": "fee_category",
    "Academic Year": "academic_year",
    "Year": "academic_year",
    "Branch": "branch",
}


# ─── Normalisation helpers ────────────────────────────────────────────────────
def _str(val) -> str:
    """Convert any value to a stripped string; return '' for None/NaN."""
    if val is None:
        return ""
    import math
    try:
        if isinstance(val, float) and math.isnan(val):
            return ""
    except Exception:
        pass
    return str(val).strip()


def _normalize_branch(branch: str) -> str:
    """Normalize branch name/code to uppercase canonical form."""
    b = branch.strip().upper()
    # Common aliases
    ALIASES = {
        "COMPUTER SCIENCE": "CSE",
        "COMP SCI": "CSE",
        "INFORMATION TECHNOLOGY": "IT",
        "INFO TECH": "IT",
        "ELECTRONICS": "EC",
        "ELECTRONICS & COMM": "EC",
        "ELECTRONICS AND COMMUNICATION": "EC",
        "ELECTRICAL": "EE",
        "ELECTRICAL ENGINEERING": "EE",
        "MECHANICAL": "ME",
        "CIVIL": "CE",
        "CHEMICAL": "CH",
        "METALLURGY": "MT",
        "BIOTECHNOLOGY": "BT",
        "APPLIED MATHS": "AM",
        "APPLIED MATHEMATICS": "AM",
        "PHYSICS": "AP",
        "AI & ROBOTICS": "AIR",
        "ARTIFICIAL INTELLIGENCE": "AIR",
    }
    return ALIASES.get(b, b)


def _normalize_year(year_val) -> Optional[str]:
    """Parse academic year like '2021-22' or '2021-2022' → '2021-22'."""
    s = _str(year_val)
    if not s:
        return None
    # If it's a plain integer year like 2021 or 2021.0
    try:
        y = int(float(s))
        return f"{y}-{str(y + 1)[-2:]}"
    except (ValueError, OverflowError):
        pass
    # Already formatted like 2021-22 or 2021-2022
    parts = s.replace("–", "-").split("-")
    if len(parts) >= 2:
        start = parts[0].strip()
        return f"{start}-{parts[-1].strip()[-2:]}" if start.isdigit() else s
    return s


def _parse_money(val) -> Optional[float]:
    """Parse money values including '51400+14000' compound strings."""
    s = _str(val)
    if not s:
        return None
    # Handle compound amounts like "51400+14000"
    total = 0.0
    for part in s.replace(" ", "").split("+"):
        part = part.replace(",", "").strip()
        try:
            total += float(part)
        except ValueError:
            return None
    return total if total > 0 else None


def _parse_bool(val) -> Optional[bool]:
    s = _str(val).upper()
    if s in ("Y", "YES", "TRUE", "1"):
        return True
    if s in ("N", "NO", "FALSE", "0", ""):
        return False
    return None


# ─── Excel reading ─────────────────────────────────────────────────────────
def read_excel_sheet(wb, sheet_name: str) -> tuple[list[str], list[list]]:
    """Read an Excel sheet, returning (headers, rows)."""
    if sheet_name not in wb.sheetnames:
        logger.warning(f"Sheet '{sheet_name}' not found in workbook. Available: {wb.sheetnames}")
        return [], []
    ws = wb[sheet_name]
    rows = list(ws.iter_rows(values_only=True))
    if not rows:
        return [], []
    headers = [_str(h) for h in rows[0]]
    data_rows = [list(row) for row in rows[1:] if any(v is not None for v in row)]
    return headers, data_rows


def map_row(headers: list[str], row: list, col_map: dict) -> dict:
    """Map a row using header names to internal field names."""
    result = {}
    for i, header in enumerate(headers):
        if i < len(row):
            internal = col_map.get(header)
            if internal:
                result[internal] = row[i]
    return result


def detect_semester_columns(headers: list[str]) -> dict[int, int]:
    """Return mapping of column_index → semester_number for semester fee columns."""
    result = {}
    for i, h in enumerate(headers):
        for j, pattern in enumerate(SEMESTER_FEE_PATTERN):
            if h.strip().upper() == pattern.upper():
                sem_no = (j % 8) + 1  # 0-7 → 1-8
                result[i] = sem_no
                break
    return result


# ─── Database helpers ─────────────────────────────────────────────────────
def get_db_engine(db_url: str = None):
    """Create SQLAlchemy engine from URL or environment variables."""
    if db_url:
        return create_engine(db_url)
    host = os.getenv("DB_HOST", "127.0.0.1")
    port = os.getenv("DB_PORT", "3306")
    name = os.getenv("DB_NAME", "excel_app")
    user = os.getenv("DB_USER", "root")
    password = os.getenv("DB_PASSWORD", "")
    url = f"mysql+pymysql://{user}:{password}@{host}:{port}/{name}?charset=utf8mb4"
    return create_engine(url, pool_pre_ping=True)


def get_existing_enrolments(conn) -> set[str]:
    """Fetch all existing enrolment_no values from the student table."""
    result = conn.execute(text("SELECT enrolment_no FROM student WHERE enrolment_no IS NOT NULL"))
    return {row[0].strip().lower() for row in result if row[0]}


def get_existing_fee_structures(conn) -> dict[tuple, int]:
    """Return mapping of (structure_year, branch_code, fee_category_id) → fee_structure_id."""
    result = conn.execute(text(
        "SELECT fee_structure_id, structure_year, branch_code, fee_category_id FROM fee_structure"
    ))
    return {(r[1], r[2].upper() if r[2] else "", r[3]): r[0] for r in result}


def resolve_branch_id(conn, branch_code: str) -> Optional[int]:
    """Resolve branch code to branch_id."""
    result = conn.execute(
        text("SELECT branch_id FROM branch WHERE UPPER(branch_code) = :code LIMIT 1"),
        {"code": branch_code.upper()}
    ).fetchone()
    return result[0] if result else None


def resolve_fee_category_id(conn, category_code: str) -> Optional[int]:
    """Resolve fee category code to fee_category_id."""
    result = conn.execute(
        text("SELECT fee_category_id FROM fee_category WHERE UPPER(category_code) = :code LIMIT 1"),
        {"code": category_code.upper()}
    ).fetchone()
    return result[0] if result else None


# ─── Student import ─────────────────────────────────────────────────────────
def import_students_from_sheet(
    conn,
    headers: list[str],
    rows: list[list],
    result: SyncResult,
    dry_run: bool = False,
) -> None:
    """Import student records from Excel DATA sheet into database."""
    existing = get_existing_enrolments(conn)
    logger.info(f"Found {len(existing)} existing students in DB. Processing {len(rows)} Excel rows...")

    for row_idx, row in enumerate(rows, start=2):  # start=2 because row 1 is header
        mapped = map_row(headers, row, STUDENT_COLUMN_MAP)
        enrolment = _str(mapped.get("enrolment_no", "")).lower()

        if not enrolment:
            result.warnings.append(f"Row {row_idx}: Skipped – no enrollment number")
            result.students_skipped += 1
            continue

        name = _str(mapped.get("name", ""))
        if not name:
            result.warnings.append(f"Row {row_idx} ({enrolment}): Skipped – no student name")
            result.students_skipped += 1
            continue

        branch_raw = _str(mapped.get("branch", ""))
        branch_code = _normalize_branch(branch_raw) if branch_raw else None

        # Resolve branch_id
        branch_id = None
        if branch_code:
            branch_id = resolve_branch_id(conn, branch_code)
            if branch_id is None:
                result.warnings.append(
                    f"Row {row_idx} ({enrolment}): Branch '{branch_raw}' not found in DB; student will be saved without branch link"
                )

        admission_year_raw = _str(mapped.get("admission_year", ""))
        admission_year = _normalize_year(admission_year_raw) if admission_year_raw else None

        gender_raw = _str(mapped.get("gender", "")).upper()
        gender = "M" if gender_raw in ("M", "MALE", "BOY") else ("F" if gender_raw in ("F", "FEMALE", "GIRL") else (gender_raw if gender_raw else None))

        ews_raw = mapped.get("ews")
        ews = _parse_bool(ews_raw)

        try:
            if enrolment in existing:
                # Student exists – skip (DB is source of truth for app-edited records)
                result.students_skipped += 1
            else:
                if not dry_run:
                    conn.execute(text("""
                        INSERT INTO student (
                            enrolment_no, name, branch_id, gender, father_name, mother_name,
                            date_of_birth, college_unique_id, class_roll_no, roll_no,
                            rank, marks, ews, religion, domicile, remarks
                        ) VALUES (
                            :enrolment_no, :name, :branch_id, :gender, :father, :mother,
                            :dob, :uid, :class_roll, :roll,
                            :rank, :marks, :ews, :religion, :domicile, :remarks
                        )
                    """), {
                        "enrolment_no": enrolment,
                        "name": name,
                        "branch_id": branch_id,
                        "gender": gender,
                        "father": _str(mapped.get("father")) or None,
                        "mother": _str(mapped.get("mother")) or None,
                        "dob": _str(mapped.get("date_of_birth")) or None,
                        "uid": _str(mapped.get("college_unique_id")) or None,
                        "class_roll": _str(mapped.get("class_roll_no")) or None,
                        "roll": _str(mapped.get("roll_no")) or None,
                        "rank": _str(mapped.get("rank")) or None,
                        "marks": _str(mapped.get("marks")) or None,
                        "ews": 1 if ews else 0 if ews is not None else None,
                        "religion": _str(mapped.get("religion")) or None,
                        "domicile": _str(mapped.get("domicile")) or None,
                        "remarks": _str(mapped.get("remarks")) or None,
                    })

                    # Insert contact record
                    conn.execute(text("""
                        INSERT INTO contact (student_id, phone, email, parent_phone, parent_email,
                            permanent_address, present_address, aadhaar_no, aadhaar_linked_mobile)
                        SELECT student_id, :phone, :email, :parent_phone, :parent_email,
                            :perm_addr, :pres_addr, :aadhaar, :aadhaar_mob
                        FROM student WHERE enrolment_no = :enrolment_no
                        LIMIT 1
                    """), {
                        "enrolment_no": enrolment,
                        "phone": _str(mapped.get("student_phone")) or None,
                        "email": _str(mapped.get("email")) or None,
                        "parent_phone": _str(mapped.get("parents_phone")) or None,
                        "parent_email": _str(mapped.get("parents_email")) or None,
                        "perm_addr": _str(mapped.get("permanent_address")) or None,
                        "pres_addr": _str(mapped.get("present_address")) or None,
                        "aadhaar": _str(mapped.get("aadhaar_no")) or None,
                        "aadhaar_mob": _str(mapped.get("aadhaar_linked_mobile")) or None,
                    })

                    # Insert admission record if admission_year exists
                    if admission_year:
                        status_raw = _str(mapped.get("status", "")).upper()
                        round_raw = _str(mapped.get("round", ""))
                        conn.execute(text("""
                            INSERT INTO admission (student_id, admission_year, status, round, remarks)
                            SELECT student_id, :yr, :status, :rnd, :rem
                            FROM student WHERE enrolment_no = :enrolment_no
                            LIMIT 1
                        """), {
                            "enrolment_no": enrolment,
                            "yr": admission_year,
                            "status": status_raw or None,
                            "rnd": round_raw or None,
                            "rem": _str(mapped.get("remarks")) or None,
                        })

                    existing.add(enrolment)
                result.students_inserted += 1

        except Exception as e:
            result.students_failed += 1
            result.errors.append(f"Row {row_idx} ({enrolment}): {e}")
            logger.error(f"Failed to import student row {row_idx}: {e}")


# ─── Fee structure import ────────────────────────────────────────────────────
def import_fee_structures_from_sheet(
    conn,
    headers: list[str],
    rows: list[list],
    result: SyncResult,
    dry_run: bool = False,
) -> None:
    """Import fee structure records from the FEE MAST sheet."""
    sem_cols = detect_semester_columns(headers)
    logger.info(f"Detected semester columns: {sem_cols}")

    for row_idx, row in enumerate(rows, start=2):
        mapped = map_row(headers, row, FEE_MAST_COLUMN_MAP)
        category_raw = _str(mapped.get("fee_category", ""))
        year_raw = _str(mapped.get("academic_year", ""))
        branch_raw = _str(mapped.get("branch", ""))

        if not (category_raw and year_raw):
            result.warnings.append(f"FEE MAST row {row_idx}: Skipped – missing category or year")
            continue

        branch_code = _normalize_branch(branch_raw) if branch_raw else None
        year_text = _normalize_year(year_raw)
        structure_year = int(year_text[:4]) if year_text and year_text[:4].isdigit() else None
        if not structure_year:
            result.warnings.append(f"FEE MAST row {row_idx}: Cannot parse year '{year_raw}'")
            continue

        fee_category_id = resolve_fee_category_id(conn, category_raw)
        if fee_category_id is None:
            result.warnings.append(f"FEE MAST row {row_idx}: Fee category '{category_raw}' not found in DB")
            continue

        try:
            # Check if fee structure exists
            existing_row = conn.execute(text("""
                SELECT fee_structure_id FROM fee_structure
                WHERE structure_year = :yr
                AND UPPER(branch_code) = :br
                AND fee_category_id = :cat
                LIMIT 1
            """), {
                "yr": structure_year,
                "br": branch_code.upper() if branch_code else "",
                "cat": fee_category_id
            }).fetchone()

            if existing_row:
                fs_id = existing_row[0]
                action = "exists"
            else:
                if not dry_run:
                    fee_mast_code = _str(mapped.get("fee_mast_code")) or f"{structure_year} {branch_code}/{category_raw}"[:64]
                    conn.execute(text("""
                        INSERT INTO fee_structure (fee_mast_code, fee_category_id, structure_year, branch_code)
                        VALUES (:code, :cat, :yr, :br)
                    """), {
                        "code": fee_mast_code,
                        "cat": fee_category_id,
                        "yr": structure_year,
                        "br": branch_code,
                    })
                    fs_id = conn.execute(text("SELECT LAST_INSERT_ID()")).scalar()
                else:
                    fs_id = None
                result.fee_structures_inserted += 1
                action = "inserted"

            # Insert semester amounts
            for col_idx, sem_no in sem_cols.items():
                if col_idx < len(row):
                    amount = _parse_money(row[col_idx])
                    if amount is not None and amount > 0 and fs_id and not dry_run:
                        conn.execute(text("""
                            INSERT INTO fee_structure_semester (fee_structure_id, semester_no, amount)
                            VALUES (:fs_id, :sem, :amt)
                            ON DUPLICATE KEY UPDATE amount = :amt
                        """), {"fs_id": fs_id, "sem": sem_no, "amt": amount})

            logger.debug(f"FEE MAST row {row_idx}: {action} – {structure_year} / {branch_code} / {category_raw}")

        except Exception as e:
            result.fee_structures_failed += 1
            result.errors.append(f"FEE MAST row {row_idx}: {e}")
            logger.error(f"Fee structure row {row_idx} failed: {e}")


# ─── Main sync orchestrator ──────────────────────────────────────────────────
def sync_excel_to_db(
    excel_path: str,
    mode: str = "import",
    dry_run: bool = False,
    db_url: str = None,
) -> SyncResult:
    """
    Main sync function.

    Args:
        excel_path: Path to the Excel workbook (.xlsx)
        mode: 'import' (Excel → DB) or 'report' (check without writing)
        dry_run: If True, parse and validate but do not write to DB.
        db_url: Optional SQLAlchemy DB URL (defaults to env vars).

    Returns:
        SyncResult with counts and error/warning lists.
    """
    result = SyncResult()

    if not _OPENPYXL_AVAILABLE:
        result.errors.append("openpyxl is not installed. Run: pip install openpyxl")
        return result

    if not _SQLALCHEMY_AVAILABLE:
        result.errors.append("sqlalchemy/pymysql not installed. Run: pip install sqlalchemy pymysql")
        return result

    if not os.path.exists(excel_path):
        result.errors.append(f"Excel file not found: {excel_path}")
        return result

    logger.info(f"Opening workbook: {excel_path}")
    try:
        wb = openpyxl.load_workbook(excel_path, read_only=True, data_only=True)
    except Exception as e:
        result.errors.append(f"Cannot open workbook: {e}")
        return result

    logger.info(f"Workbook sheets: {wb.sheetnames}")

    # Find the DATA sheet (may be named differently)
    data_sheet_names = ["DATA", "Data", "data", "STUDENT DATA", "Student Data"]
    data_sheet = next((n for n in data_sheet_names if n in wb.sheetnames), None)

    # Find the FEE MAST sheet
    fee_sheet_names = ["FEE MAST", "Fee Mast", "FEE_MAST", "FEE MASTER", "Fee Master"]
    fee_sheet = next((n for n in fee_sheet_names if n in wb.sheetnames), None)

    if not data_sheet:
        result.warnings.append(f"DATA sheet not found. Available: {wb.sheetnames}. Skipping student import.")
    if not fee_sheet:
        result.warnings.append(f"FEE MAST sheet not found. Available: {wb.sheetnames}. Skipping fee structure import.")

    if mode == "report":
        logger.info("Report mode: will not write to database.")
        dry_run = True

    engine = get_db_engine(db_url)

    with engine.connect() as conn:
        with conn.begin():
            if data_sheet:
                logger.info(f"Reading student data from sheet: '{data_sheet}'")
                headers, rows = read_excel_sheet(wb, data_sheet)
                logger.info(f"Found {len(rows)} data rows, {len(headers)} columns")
                import_students_from_sheet(conn, headers, rows, result, dry_run=dry_run)

            if fee_sheet:
                logger.info(f"Reading fee structure data from sheet: '{fee_sheet}'")
                headers, rows = read_excel_sheet(wb, fee_sheet)
                logger.info(f"Found {len(rows)} fee rows, {len(headers)} columns")
                import_fee_structures_from_sheet(conn, headers, rows, result, dry_run=dry_run)

    wb.close()
    logger.info(result.summary())
    return result


# ─── CLI entry point ─────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Sync Excel workbook with MySQL database")
    parser.add_argument("--excel", required=True, help="Path to Excel workbook (.xlsx)")
    parser.add_argument(
        "--mode",
        choices=["import", "report"],
        default="import",
        help="'import' writes to DB; 'report' only reports differences (read-only)"
    )
    parser.add_argument("--dry-run", action="store_true", help="Parse and validate without writing")
    parser.add_argument("--db-url", help="SQLAlchemy DB URL (overrides .env variables)")
    parser.add_argument("--log-file", help="Optional log file path")
    args = parser.parse_args()

    if args.log_file:
        fh = logging.FileHandler(args.log_file, encoding="utf-8")
        fh.setFormatter(logging.Formatter(LOG_FORMAT))
        logging.getLogger().addHandler(fh)

    if args.dry_run and args.mode == "import":
        logger.info("DRY RUN: No data will be written to the database.")

    result = sync_excel_to_db(
        excel_path=args.excel,
        mode=args.mode,
        dry_run=args.dry_run,
        db_url=args.db_url,
    )

    print(result.summary())
    sys.exit(1 if result.errors else 0)


if __name__ == "__main__":
    main()
