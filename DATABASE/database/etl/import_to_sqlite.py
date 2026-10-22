"""
Import UG - Copy.xlsx into a local SQLite database and verify Excel totals.

Usage:
  python etl/import_to_sqlite.py
  python etl/import_to_sqlite.py --excel "c:\\Users\\vedan\\Downloads\\UG - Copy.xlsx"
"""
from __future__ import annotations

import argparse
import csv
import os
import re
import sqlite3
from datetime import datetime, date

import pandas as pd

DEFAULT_EXCEL = r"c:\Users\vedan\Downloads\UG - Copy.xlsx"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DB_PATH = os.path.join(ROOT, "data", "excel_app.sqlite")
CSV_DIR = os.path.join(ROOT, "data", "csv")

TERM_COLUMNS = [
    "JUL-DEC20",
    "JAN-JUN21",
    "JUL-DEC21",
    "JAN-JUN22",
    "JUL-DEC22",
    "JAN-JUN23",
    "JUL-DEC23",
    "JAN-JUN24",
    "JUL-DEC24",
    "JAN-JUN25",
    "JUL-DEC25",
    "JAN-JUN26",
    "JUL-DEC26",
    "JAN-JUN27",
    "JUL-DEC27",
    "JAN-JUN28",
    "JUL-DEC28",
    "JAN-JUN29",
]

SCHEMA = r"""
PRAGMA foreign_keys = ON;

CREATE TABLE branch (
  branch_id INTEGER PRIMARY KEY AUTOINCREMENT,
  branch_code TEXT NOT NULL UNIQUE,
  branch_name TEXT,
  parent_code TEXT,
  is_fw INTEGER NOT NULL DEFAULT 0,
  department TEXT,
  remarks TEXT
);

CREATE TABLE fee_category (
  fee_category_id INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL UNIQUE,
  category_name TEXT NOT NULL
);

CREATE TABLE eligible_category (
  eligible_category_id INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL UNIQUE,
  category_name TEXT NOT NULL
);

CREATE TABLE alloted_category (
  alloted_category_id INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL UNIQUE,
  eligible_group TEXT,
  description TEXT
);

CREATE TABLE religion (
  religion_id INTEGER PRIMARY KEY AUTOINCREMENT,
  religion_code TEXT NOT NULL UNIQUE,
  religion_name TEXT NOT NULL
);

CREATE TABLE admission_status (
  status_id INTEGER PRIMARY KEY AUTOINCREMENT,
  status_code TEXT NOT NULL UNIQUE,
  status_name TEXT NOT NULL
);

CREATE TABLE admission_round (
  round_id INTEGER PRIMARY KEY AUTOINCREMENT,
  round_code TEXT NOT NULL UNIQUE,
  round_name TEXT NOT NULL
);

CREATE TABLE payment_mode (
  payment_mode_id INTEGER PRIMARY KEY AUTOINCREMENT,
  mode_code TEXT NOT NULL UNIQUE,
  mode_name TEXT NOT NULL,
  is_grouped INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE remark_type (
  remark_type_id INTEGER PRIMARY KEY AUTOINCREMENT,
  remark_code TEXT NOT NULL UNIQUE,
  remark_name TEXT NOT NULL
);

CREATE TABLE academic_term (
  term_id INTEGER PRIMARY KEY AUTOINCREMENT,
  term_code TEXT NOT NULL UNIQUE,
  excel_column TEXT NOT NULL,
  calendar_year INTEGER NOT NULL,
  term_half TEXT NOT NULL,
  sort_order INTEGER NOT NULL
);

CREATE TABLE fee_structure (
  fee_structure_id INTEGER PRIMARY KEY AUTOINCREMENT,
  fee_mast_code TEXT NOT NULL UNIQUE,
  fee_category_id INTEGER NOT NULL,
  structure_year INTEGER NOT NULL,
  FOREIGN KEY (fee_category_id) REFERENCES fee_category(fee_category_id)
);

CREATE TABLE fee_structure_semester (
  fee_structure_semester_id INTEGER PRIMARY KEY AUTOINCREMENT,
  fee_structure_id INTEGER NOT NULL,
  semester_no INTEGER NOT NULL,
  amount REAL NOT NULL,
  UNIQUE (fee_structure_id, semester_no),
  FOREIGN KEY (fee_structure_id) REFERENCES fee_structure(fee_structure_id)
);

CREATE TABLE fee_year_sem_code (
  year_sem_id INTEGER PRIMARY KEY AUTOINCREMENT,
  structure_year INTEGER NOT NULL,
  sem_code TEXT NOT NULL,
  year_sem TEXT NOT NULL UNIQUE,
  semester_no INTEGER
);

CREATE TABLE student (
  student_id INTEGER PRIMARY KEY AUTOINCREMENT,
  sr_no INTEGER NOT NULL UNIQUE,
  college_unique_id TEXT,
  class_roll_no TEXT NOT NULL,
  enrolment_no TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  father_name TEXT,
  mother_name TEXT,
  rank_text TEXT,
  marks REAL,
  exam_roll_no TEXT,
  date_of_birth TEXT,
  branch_id INTEGER NOT NULL,
  eligible_category_id INTEGER NOT NULL,
  alloted_category_id INTEGER NOT NULL,
  domicile TEXT NOT NULL,
  gender TEXT NOT NULL,
  ews TEXT NOT NULL,
  religion_id INTEGER NOT NULL,
  fee_structure_id INTEGER NOT NULL,
  mponline_password TEXT,
  FOREIGN KEY (branch_id) REFERENCES branch(branch_id),
  FOREIGN KEY (eligible_category_id) REFERENCES eligible_category(eligible_category_id),
  FOREIGN KEY (alloted_category_id) REFERENCES alloted_category(alloted_category_id),
  FOREIGN KEY (religion_id) REFERENCES religion(religion_id),
  FOREIGN KEY (fee_structure_id) REFERENCES fee_structure(fee_structure_id)
);

CREATE TABLE contact (
  contact_id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL UNIQUE,
  student_phone TEXT,
  student_email TEXT,
  parents_phone TEXT,
  parents_email TEXT,
  aadhaar_no TEXT,
  aadhaar_linked_mobile TEXT,
  permanent_address TEXT,
  present_address TEXT,
  FOREIGN KEY (student_id) REFERENCES student(student_id)
);

CREATE TABLE admission (
  admission_id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL UNIQUE,
  admission_no TEXT,
  status_id INTEGER NOT NULL,
  round_id INTEGER,
  admission_year TEXT NOT NULL,
  admission_year_start INTEGER NOT NULL,
  offline_category TEXT,
  remark_type_id INTEGER,
  remarks_raw TEXT,
  FOREIGN KEY (student_id) REFERENCES student(student_id),
  FOREIGN KEY (status_id) REFERENCES admission_status(status_id),
  FOREIGN KEY (round_id) REFERENCES admission_round(round_id),
  FOREIGN KEY (remark_type_id) REFERENCES remark_type(remark_type_id)
);

CREATE TABLE payment (
  payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL,
  total_amount REAL,
  total_amount_raw TEXT,
  student_submitted_amount REAL,
  student_submitted_amount_raw TEXT,
  online_application_txn_id TEXT,
  payment_mode_id INTEGER,
  payment_mode_raw TEXT,
  payment_date TEXT,
  payment_date_raw TEXT,
  dte_fee REAL,
  payment_status TEXT,
  payment_reference_no TEXT,
  FOREIGN KEY (student_id) REFERENCES student(student_id),
  FOREIGN KEY (payment_mode_id) REFERENCES payment_mode(payment_mode_id)
);

CREATE TABLE student_term_fee (
  student_term_fee_id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id INTEGER NOT NULL,
  term_id INTEGER NOT NULL,
  amount REAL NOT NULL DEFAULT 0,
  UNIQUE (student_id, term_id),
  FOREIGN KEY (student_id) REFERENCES student(student_id),
  FOREIGN KEY (term_id) REFERENCES academic_term(term_id)
);

CREATE INDEX ix_student_name ON student(name);
CREATE INDEX ix_student_class_roll ON student(class_roll_no);
CREATE INDEX ix_student_branch ON student(branch_id);
CREATE INDEX ix_adm_year ON admission(admission_year);
CREATE INDEX ix_pay_date ON payment(payment_date);
CREATE INDEX ix_stf_term ON student_term_fee(term_id);

CREATE VIEW v_student_search AS
SELECT
  s.student_id, s.sr_no, s.college_unique_id, s.class_roll_no, s.enrolment_no,
  s.name, s.father_name, s.mother_name, s.rank_text, s.marks, s.exam_roll_no,
  s.date_of_birth, b.branch_code, b.branch_name,
  ec.category_code AS eligible_category,
  ac.category_code AS alloted_category,
  s.domicile, s.gender, s.ews, r.religion_code AS religion,
  fs.fee_mast_code AS fee_structure, fs.structure_year AS fee_structure_year,
  a.admission_year, a.admission_year_start,
  st.status_code AS admission_status, rd.round_code AS admission_round,
  rt.remark_code AS remarks,
  c.student_phone, c.student_email, c.parents_phone, c.parents_email,
  c.aadhaar_no, c.permanent_address, c.present_address,
  p.total_amount, p.student_submitted_amount, p.online_application_txn_id,
  COALESCE(pm.mode_code, p.payment_mode_raw) AS payment_mode,
  p.payment_date, p.dte_fee,
  COALESCE(p.student_submitted_amount, 0) AS paid_amount,
  CASE
    WHEN COALESCE(p.total_amount, 0) - COALESCE(p.student_submitted_amount, 0) > 0
    THEN COALESCE(p.total_amount, 0) - COALESCE(p.student_submitted_amount, 0)
    ELSE 0
  END AS pending_amount
FROM student s
JOIN branch b ON b.branch_id = s.branch_id
JOIN eligible_category ec ON ec.eligible_category_id = s.eligible_category_id
JOIN alloted_category ac ON ac.alloted_category_id = s.alloted_category_id
JOIN religion r ON r.religion_id = s.religion_id
JOIN fee_structure fs ON fs.fee_structure_id = s.fee_structure_id
JOIN admission a ON a.student_id = s.student_id
JOIN admission_status st ON st.status_id = a.status_id
LEFT JOIN admission_round rd ON rd.round_id = a.round_id
LEFT JOIN remark_type rt ON rt.remark_type_id = a.remark_type_id
LEFT JOIN contact c ON c.student_id = s.student_id
LEFT JOIN payment p ON p.student_id = s.student_id
LEFT JOIN payment_mode pm ON pm.payment_mode_id = p.payment_mode_id;
"""


def nz(v):
    if v is None or (isinstance(v, float) and pd.isna(v)):
        return None
    if isinstance(v, str) and v.strip() == "":
        return None
    return v


def as_str(v, max_len=None):
    v = nz(v)
    if v is None:
        return None
    if isinstance(v, datetime):
        s = v.strftime("%Y-%m-%d")
    elif isinstance(v, date):
        s = v.isoformat()
    elif isinstance(v, float) and v.is_integer():
        s = str(int(v))
    else:
        s = str(v).strip()
        if s.lower() == "nan":
            return None
    if max_len:
        s = s[:max_len]
    return s or None


def parse_money(v):
    """Parse Excel money cells: 51400, 1,21,600, '51400+\\n14000', '10000 online+41900'."""
    v = nz(v)
    if v is None:
        return None
    if isinstance(v, bool):
        return None
    if isinstance(v, (int, float)) and not isinstance(v, bool):
        if pd.isna(v):
            return None
        return float(v)
    s = str(v).strip()
    if s == "" or s.lower() == "nan":
        return None
    s = s.replace(",", "").replace("\n", " ")
    parts = re.findall(r"\d+(?:\.\d+)?", s)
    if not parts:
        return None
    if "+" in s or len(parts) > 1:
        return float(sum(float(p) for p in parts))
    try:
        return float(parts[0])
    except ValueError:
        return None


def as_num(v):
    return parse_money(v)


def as_date(v):
    v = nz(v)
    if v is None:
        return None, None
    if isinstance(v, datetime):
        return v.date().isoformat(), None
    if isinstance(v, date):
        return v.isoformat(), None
    s = str(v).strip()
    if s.lower() == "nan" or s == "":
        return None, None
    for fmt in ("%Y-%m-%d", "%d-%m-%Y", "%d/%m/%Y", "%Y/%m/%d", "%d-%b-%Y", "%d.%m.%Y"):
        try:
            return datetime.strptime(s, fmt).date().isoformat(), s
        except ValueError:
            continue
    parsed = pd.to_datetime(s, errors="coerce", dayfirst=True)
    if pd.isna(parsed):
        return None, s
    return parsed.date().isoformat(), s


def upsert_lookup(cur, table, code_col, code, extra_sql="", extra_args=()):
    code = as_str(code)
    if not code:
        return None
    cur.execute(f"SELECT {table}_id FROM {table} WHERE {code_col}=?", (code,))
    # tables don't all have {table}_id - handle per table
    return code


def get_id(cur, table, pk, col, value, insert_cols=None, insert_vals=None):
    value = as_str(value)
    if value is None:
        return None
    cur.execute(f"SELECT {pk} FROM {table} WHERE {col}=?", (value,))
    row = cur.fetchone()
    if row:
        return row[0]
    if insert_cols is None:
        insert_cols = [col]
        insert_vals = [value]
    placeholders = ",".join("?" * len(insert_cols))
    cur.execute(
        f"INSERT INTO {table} ({','.join(insert_cols)}) VALUES ({placeholders})",
        insert_vals,
    )
    return cur.lastrowid


def parse_term(col: str):
    # JUL-DEC20 or JAN-JUN21
    m = re.match(r"(JUL-DEC|JAN-JUN)(\d{2})$", col)
    if not m:
        raise ValueError(col)
    half = "JUL_DEC" if m.group(1) == "JUL-DEC" else "JAN_JUN"
    year = 2000 + int(m.group(2))
    return col, year, half


def seed_terms(cur):
    term_ids = {}
    for i, col in enumerate(TERM_COLUMNS, start=1):
        code, year, half = parse_term(col)
        cur.execute(
            """INSERT INTO academic_term(term_code, excel_column, calendar_year, term_half, sort_order)
               VALUES (?,?,?,?,?)""",
            (code, col, year, half, i),
        )
        term_ids[col] = cur.lastrowid
    return term_ids


def load_excel(path):
    data = pd.read_excel(path, sheet_name="DATA")
    data.columns = [str(c).strip() for c in data.columns]
    fee = pd.read_excel(path, sheet_name="FEE MAST")
    return data, fee


def import_fee(cur, fee: pd.DataFrame):
    fee_rows = fee[fee["FEE MAST"].notna()].copy()
    for _, row in fee_rows.iterrows():
        cat = as_str(row["FEE CATEGORY"])
        cat_id = get_id(
            cur,
            "fee_category",
            "fee_category_id",
            "category_code",
            cat,
            ["category_code", "category_name"],
            [cat, cat],
        )
        code = as_str(row["FEE MAST"])
        year = int(as_num(row["YR"]))
        cur.execute(
            """INSERT INTO fee_structure(fee_mast_code, fee_category_id, structure_year)
               VALUES (?,?,?)""",
            (code, cat_id, year),
        )
        fs_id = cur.lastrowid
        for sem in range(1, 9):
            amt = as_num(row[sem])
            if amt is None:
                continue
            cur.execute(
                """INSERT INTO fee_structure_semester(fee_structure_id, semester_no, amount)
                   VALUES (?,?,?)""",
                (fs_id, sem, amt),
            )

    ys = fee.iloc[:, [20, 21, 22]].copy()
    ys.columns = ["year", "sem_code", "year_sem"]
    ys = ys.dropna(subset=["year_sem"])
    for _, row in ys.iterrows():
        sem_code = as_str(row["sem_code"])
        m = re.search(r"(\d+)$", sem_code or "")
        sem_no = int(m.group(1)) if m else None
        cur.execute(
            """INSERT OR IGNORE INTO fee_year_sem_code(structure_year, sem_code, year_sem, semester_no)
               VALUES (?,?,?,?)""",
            (int(as_num(row["year"])), sem_code, as_str(row["year_sem"]), sem_no),
        )


def parent_branch(code: str):
    if code.endswith("_FW"):
        return code[:-3], 1
    if code.endswith("-RR"):
        return code[:-3], 0
    return code, 0


def import_students(cur, data: pd.DataFrame, term_ids: dict):
    branch_cache = {}
    elig_cache = {}
    allot_cache = {}
    rel_cache = {}
    fee_cache = {}
    status_cache = {}
    round_cache = {}
    remark_cache = {}
    mode_cache = {}

    def cached(cache, table, pk, col, value, cols=None, vals=None):
        value = as_str(value)
        if value is None:
            return None
        if value in cache:
            return cache[value]
        i = get_id(cur, table, pk, col, value, cols, vals)
        cache[value] = i
        return i

    n = 0
    for _, row in data.iterrows():
        branch_code = as_str(row["Branch"])
        parent, is_fw = parent_branch(branch_code)
        if branch_code not in branch_cache:
            branch_cache[branch_code] = get_id(
                cur,
                "branch",
                "branch_id",
                "branch_code",
                branch_code,
                ["branch_code", "branch_name", "parent_code", "is_fw"],
                [branch_code, branch_code, parent if parent != branch_code else None, is_fw],
            )
        elig = as_str(row["Eligible Category"])
        if elig not in elig_cache:
            elig_cache[elig] = get_id(
                cur,
                "eligible_category",
                "eligible_category_id",
                "category_code",
                elig,
                ["category_code", "category_name"],
                [elig, elig],
            )
        allot = as_str(row["Alloted Category"])
        if allot not in allot_cache:
            allot_cache[allot] = get_id(
                cur,
                "alloted_category",
                "alloted_category_id",
                "category_code",
                allot,
                ["category_code", "eligible_group"],
                [allot, elig],
            )
        rel = (as_str(row["Religion"]) or "UNKNOWN").upper()
        if rel not in rel_cache:
            rel_cache[rel] = get_id(
                cur,
                "religion",
                "religion_id",
                "religion_code",
                rel,
                ["religion_code", "religion_name"],
                [rel, rel.title()],
            )
        fee_code = as_str(row["fee stru"])
        if fee_code not in fee_cache:
            cur.execute("SELECT fee_structure_id FROM fee_structure WHERE fee_mast_code=?", (fee_code,))
            found = cur.fetchone()
            if not found:
                raise RuntimeError(f"fee stru not in FEE MAST: {fee_code}")
            fee_cache[fee_code] = found[0]

        status_raw = (as_str(row["Status"]) or "ADMITTED").upper()
        if status_raw not in status_cache:
            status_cache[status_raw] = get_id(
                cur,
                "admission_status",
                "status_id",
                "status_code",
                status_raw,
                ["status_code", "status_name"],
                [status_raw, status_raw.title()],
            )

        round_raw = as_str(row["Round"])
        round_id = None
        if round_raw:
            if round_raw not in round_cache:
                round_cache[round_raw] = get_id(
                    cur,
                    "admission_round",
                    "round_id",
                    "round_code",
                    round_raw,
                    ["round_code", "round_name"],
                    [round_raw, round_raw.strip("() ")],
                )
            round_id = round_cache[round_raw]

        remark_raw = as_str(row["Remarks"])
        remark_id = None
        if remark_raw:
            if remark_raw not in remark_cache:
                remark_cache[remark_raw] = get_id(
                    cur,
                    "remark_type",
                    "remark_type_id",
                    "remark_code",
                    remark_raw,
                    ["remark_code", "remark_name"],
                    [remark_raw, remark_raw],
                )
            remark_id = remark_cache[remark_raw]

        mode_raw = as_str(row["Payment Mode"])
        mode_id = None
        if mode_raw:
            mode_norm = re.sub(r"\s+", " ", mode_raw).upper()
            if mode_norm not in mode_cache:
                mode_cache[mode_norm] = get_id(
                    cur,
                    "payment_mode",
                    "payment_mode_id",
                    "mode_code",
                    mode_norm,
                    ["mode_code", "mode_name", "is_grouped"],
                    [mode_norm, mode_norm, 1 if "/" in mode_norm or "+" in mode_norm or "," in mode_norm else 0],
                )
            mode_id = mode_cache[mode_norm]

        uid = as_str(row["Unique ID"])
        dob, _ = as_date(row["Date of Birth"])
        pay_date, pay_date_raw = as_date(row["Payment Date"])
        year = as_str(row["Admission Year"])
        year_start = int(year.split("-")[0]) if year else None

        cur.execute(
            """INSERT INTO student(
                sr_no, college_unique_id, class_roll_no, enrolment_no, name, father_name, mother_name,
                rank_text, marks, exam_roll_no, date_of_birth, branch_id, eligible_category_id,
                alloted_category_id, domicile, gender, ews, religion_id, fee_structure_id, mponline_password
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (
                int(row["Sr. No."]),
                uid,
                as_str(row["Class Roll No."]),
                as_str(row["Enrolment No."]),
                as_str(row["Name"]),
                as_str(row["Father"]),
                as_str(row["Mother"]),
                as_str(row["Rank"]),
                as_num(row["Marks"]),
                as_str(row["Roll No"]),
                dob,
                branch_cache[branch_code],
                elig_cache[elig],
                allot_cache[allot],
                as_str(row["Domicile"]),
                as_str(row["Gender"]),
                as_str(row["EWS"]),
                rel_cache[rel],
                fee_cache[fee_code],
                as_str(row["MPOnline Password"]),
            ),
        )
        sid = cur.lastrowid

        aadhaar = as_str(row["Adharcard No."])
        if aadhaar and aadhaar.endswith(".0"):
            aadhaar = aadhaar[:-2]
        cur.execute(
            """INSERT INTO contact(
                student_id, student_phone, student_email, parents_phone, parents_email,
                aadhaar_no, aadhaar_linked_mobile, permanent_address, present_address
            ) VALUES (?,?,?,?,?,?,?,?,?)""",
            (
                sid,
                as_str(row["Student PhoneNo"]),
                as_str(row["Student Email ID"]),
                as_str(row["Parents PhoneNo"]),
                as_str(row["Parents Email ID"]),
                aadhaar,
                as_str(row["Adharcard Linked Mob. NO."]),
                as_str(row["Permanent Address"], 500),
                as_str(row["Present Address"], 500),
            ),
        )

        cur.execute(
            """INSERT INTO admission(
                student_id, status_id, round_id, admission_year, admission_year_start,
                offline_category, remark_type_id, remarks_raw
            ) VALUES (?,?,?,?,?,?,?,?)""",
            (
                sid,
                status_cache[status_raw],
                round_id,
                year,
                year_start,
                as_str(row["Ofline Admission Category"]),
                remark_id,
                remark_raw,
            ),
        )

        cur.execute(
            """INSERT INTO payment(
                student_id, total_amount, total_amount_raw, student_submitted_amount,
                student_submitted_amount_raw, online_application_txn_id,
                payment_mode_id, payment_mode_raw, payment_date, payment_date_raw, dte_fee
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
            (
                sid,
                as_num(row["Total Amount"]),
                as_str(row["Total Amount"], 128) if not isinstance(nz(row["Total Amount"]), (int, float)) else None,
                as_num(row["Student submitted Amount"]),
                as_str(row["Student submitted Amount"], 128)
                if not isinstance(nz(row["Student submitted Amount"]), (int, float))
                else None,
                as_str(row["Online Application Transaciton ID"]),
                mode_id,
                mode_raw,
                pay_date,
                pay_date_raw if pay_date is None else None,
                as_num(row["DTE Fee"]),
            ),
        )

        term_rows = []
        for col in TERM_COLUMNS:
            amt = as_num(row[col])
            if amt is None:
                amt = 0
            term_rows.append((sid, term_ids[col], amt))
        cur.executemany(
            "INSERT INTO student_term_fee(student_id, term_id, amount) VALUES (?,?,?)",
            term_rows,
        )
        n += 1
        if n % 1000 == 0:
            print(f"  imported {n} students")
    print(f"  imported {n} students")


def export_csv(conn):
    os.makedirs(CSV_DIR, exist_ok=True)
    tables = [
        "branch",
        "fee_category",
        "eligible_category",
        "alloted_category",
        "religion",
        "admission_status",
        "admission_round",
        "payment_mode",
        "remark_type",
        "academic_term",
        "fee_structure",
        "fee_structure_semester",
        "fee_year_sem_code",
        "student",
        "contact",
        "admission",
        "payment",
        "student_term_fee",
    ]
    for t in tables:
        df = pd.read_sql_query(f"SELECT * FROM {t}", conn)
        path = os.path.join(CSV_DIR, f"{t}.csv")
        df.to_csv(path, index=False, quoting=csv.QUOTE_MINIMAL)
        print("  csv", t, len(df))


def run_tests(conn, data: pd.DataFrame):
    cur = conn.cursor()
    failures = []

    def check(name, got, expected, tol=0.01):
        ok = abs(float(got) - float(expected)) <= tol if expected is not None else got == expected
        status = "PASS" if ok else "FAIL"
        print(f"  [{status}] {name}: got={got} expected={expected}")
        if not ok:
            failures.append(name)

    n_students = cur.execute("SELECT COUNT(*) FROM student").fetchone()[0]
    check("student count", n_students, len(data))
    n_enr = cur.execute("SELECT COUNT(DISTINCT enrolment_no) FROM student").fetchone()[0]
    check("enrolment unique", n_enr, len(data))

    excel_total = data["Total Amount"].map(parse_money).fillna(0).sum()
    db_total = cur.execute("SELECT COALESCE(SUM(total_amount),0) FROM payment").fetchone()[0]
    check("sum Total Amount (parsed expressions + Indian commas)", db_total, excel_total)

    excel_sub = data["Student submitted Amount"].map(parse_money).fillna(0).sum()
    db_sub = cur.execute("SELECT COALESCE(SUM(student_submitted_amount),0) FROM payment").fetchone()[0]
    check("sum submitted (parsed)", db_sub, excel_sub)

    excel_dte = pd.to_numeric(data["DTE Fee"], errors="coerce").fillna(0).sum()
    db_dte = cur.execute("SELECT COALESCE(SUM(dte_fee),0) FROM payment").fetchone()[0]
    check("sum DTE Fee", db_dte, excel_dte)

    db_j20 = cur.execute(
        """SELECT COALESCE(SUM(f.amount),0)
           FROM student_term_fee f JOIN academic_term t ON t.term_id=f.term_id
           WHERE t.term_code='JUL-DEC20'"""
    ).fetchone()[0]
    excel_j20 = pd.to_numeric(data["JUL-DEC20"], errors="coerce").fillna(0).sum()
    check("sum JUL-DEC20", db_j20, excel_j20)

    orphans = cur.execute(
        """SELECT COUNT(*) FROM student s LEFT JOIN contact c ON c.student_id=s.student_id
           WHERE c.contact_id IS NULL"""
    ).fetchone()[0]
    check("contact orphans", orphans, 0)

    y = data["Admission Year"].astype(str).str.strip().value_counts().to_dict()
    db_years = dict(cur.execute("SELECT admission_year, COUNT(*) FROM admission GROUP BY admission_year"))
    for k, v in y.items():
        check(f"year {k}", db_years.get(k, 0), int(v))

    report_year = "2025-26"
    excel_2025 = int((data["Admission Year"].astype(str).str.strip() == report_year).sum())
    db_2025 = cur.execute(
        "SELECT COUNT(*) FROM admission WHERE admission_year=?", (report_year,)
    ).fetchone()[0]
    check("REPORT year 2025-26 count", db_2025, excel_2025)

    if failures:
        raise SystemExit(f"{len(failures)} tests failed: {failures}")
    print("All tests passed.")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--excel", default=DEFAULT_EXCEL)
    args = ap.parse_args()
    if not os.path.exists(args.excel):
        raise SystemExit(f"Excel not found: {args.excel}")

    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    print("Reading", args.excel)
    data, fee = load_excel(args.excel)
    print("DATA", data.shape, "FEE", fee.shape)

    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.executescript(SCHEMA)
    cur = conn.cursor()
    print("Seeding terms...")
    term_ids = seed_terms(cur)
    print("Importing FEE MAST...")
    import_fee(cur, fee)
    print("Importing DATA...")
    import_students(cur, data, term_ids)
    conn.commit()
    print("Testing...")
    run_tests(conn, data)
    print("Exporting CSV for MySQL...")
    export_csv(conn)
    conn.close()
    print("SQLite ready:", DB_PATH)


if __name__ == "__main__":
    main()
