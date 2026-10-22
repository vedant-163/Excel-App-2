# Excel → App Database (Member: Database)

Undergraduate admission / fee workbook converted to a fully normalized 3NF MySQL design for an **Android phone app**.

The phone talks to a REST API backed by MySQL. See [docs/05_api_requirements.md](docs/05_api_requirements.md).

**Source workbook:** `c:\Users\vedan\Downloads\UG - Copy.xlsx`  
**Sheets:** `DATA` (6,760 × 57), `FEE MAST` (72 plans + 568 year-sem codes), `SEARCH` (UI), `REPORT 20-21 onwords` (pivot).

---

## ✅ Project Status — COMPLETE

All 8 phases finished and verified. Every test passes against the live MySQL database.

| Phase | Deliverable | Status |
|---|---|---|
| 1 — Analyze | Excel Workbook Analysis | ✅ Done |
| 1 — Analyze | Data Dictionary (all 57 cols) | ✅ Done |
| 2 — ER Diagram | Final ER Diagram (Mermaid + text) | ✅ Done |
| 3 — Normalize | 3NF Normalization | ✅ Done |
| 4 — MySQL | Create Database | ✅ Done |
| 5 — SQL | Schema (13 tables, all PKs/FKs) | ✅ Done |
| 5 — SQL | 8 Views (SEARCH/REPORT/Dashboard) | ✅ Done |
| 5 — SQL | Query Catalog Q01–Q12 | ✅ Done |
| 5 — SQL | Performance Indexes + FULLTEXT | ✅ Done |
| 5 — SQL | 6 Stored Procedures | ✅ Done |
| 5 — ETL | SQLite verified import | ✅ Done |
| 5 — ETL | SQLite → MySQL one-step loader | ✅ Done |
| 5 — ETL | CSV exports (18 tables) | ✅ Done |
| 5 — Dump | MySQL full dump (12 MB) | ✅ Done |
| 6 — API | API contract for Member 1 (Android) | ✅ Done |
| 7 — Excel | Excel Feature → DB/App mapping | ✅ Done |
| 8 — Test | Full test suite (all PASS) | ✅ Done |

---

## What You Can Use Today

| Deliverable | Location |
|---|---|
| Workbook analysis | [docs/01_excel_workbook_analysis.md](docs/01_excel_workbook_analysis.md) |
| Data dictionary | [docs/02_data_dictionary.md](docs/02_data_dictionary.md) |
| ER diagram | [docs/03_er_diagram.md](docs/03_er_diagram.md) |
| Normalization notes | [docs/04_normalization.md](docs/04_normalization.md) |
| API contract for app | [docs/05_api_requirements.md](docs/05_api_requirements.md) |
| Excel feature mapping | [docs/06_excel_feature_mapping.md](docs/06_excel_feature_mapping.md) |
| Create database | [sql/mysql/01_create_database.sql](sql/mysql/01_create_database.sql) |
| Schema (13 tables) | [sql/mysql/02_schema.sql](sql/mysql/02_schema.sql) |
| 8 Views | [sql/mysql/03_views.sql](sql/mysql/03_views.sql) |
| App query catalog (Q01–Q12) | [sql/mysql/04_queries.sql](sql/mysql/04_queries.sql) |
| Basic test queries | [sql/mysql/05_tests.sql](sql/mysql/05_tests.sql) |
| LOAD DATA CSV script | [sql/mysql/06_load_csv.sql](sql/mysql/06_load_csv.sql) |
| Performance indexes | [sql/mysql/07_indexes_and_fulltext.sql](sql/mysql/07_indexes_and_fulltext.sql) |
| Stored procedures (6 SPs) | [sql/mysql/08_stored_procedures.sql](sql/mysql/08_stored_procedures.sql) |
| Full automated test suite | [sql/mysql/09_full_test_suite.sql](sql/mysql/09_full_test_suite.sql) |
| MySQL full dump (12 MB) | [data/excel_app_dump.sql](data/excel_app_dump.sql) |
| Verified SQLite database | `data/excel_app.sqlite` |
| CSV extracts (18 tables) | `data/csv/` |
| One-click setup script | [setup_mysql.ps1](setup_mysql.ps1) |

---

## Verified Database Numbers

| Check | Expected | Actual | Status |
|---|---|---|---|
| Students | 6,760 | 6,760 | ✅ PASS |
| Unique enrolment numbers | 6,760 | 6,760 | ✅ PASS |
| Total fee amount | 515,165,638.00 | 515,165,638.00 | ✅ PASS |
| Submitted amount | 239,262,772.60 | 239,262,772.60 | ✅ PASS |
| DTE fee sum | 6,209,000.00 | 6,209,000.00 | ✅ PASS |
| JUL-DEC20 term fee | 53,237,100 | 53,237,100 | ✅ PASS |
| 2025-26 admissions (REPORT total) | 1,189 | 1,189 | ✅ PASS |
| Orphan records | 0 | 0 | ✅ PASS |

---

## Database Architecture — 13 Tables + 8 Views + 6 SPs

```
excel_app
│
├── LOOKUP TABLES
│   ├── branch              (22)   — CE, CSE, ME, EE, EC, IT, EI, IPE, BME, B.PHARMACY, *_FW
│   ├── fee_category        ( 8)   — plan family codes
│   ├── eligible_category   ( 4)   — UR / OBC / SC / ST
│   ├── alloted_category    (41)   — URXOP, SCXOP, ...
│   ├── religion            ( 6)   — HINDU, MUSLIM, ...
│   ├── admission_status    ( 1)   — ADMITTED
│   ├── admission_round     ( 7)   — counselling rounds
│   ├── payment_mode        (41)   — CASH, ONLINE, DD, ...
│   ├── remark_type         (11)   — LATERAL ENTRY, NRI, GULF, ...
│   └── academic_term       (18)   — JUL-DEC20 → JAN-JUN29
│
├── FEE STRUCTURE
│   ├── fee_structure         (72)   — one per FEE MAST plan
│   ├── fee_structure_semester(576)  — unpivoted sem 1-8 amounts
│   └── fee_year_sem_code     (568)  — DYNAMICPIVOT helper (cols U-W)
│
└── STUDENT DATA (6,760 students)
    ├── student           (6,760)  — identity + academics + FK refs
    ├── contact           (6,760)  — phone, email, Aadhaar, address  [1:1]
    ├── admission         (6,760)  — year, status, round, remark      [1:1]
    ├── payment           (6,760)  — fee amounts, mode, date          [1:N ready]
    └── student_term_fee  (121,680)— unpivoted 18 term fee columns    [1:N]

VIEWS (replace SEARCH sheet / REPORT sheet)
    ├── v_student_search          — all student data joined, filter-ready
    ├── v_student_fee_summary     — per-student scheduled fee totals
    ├── v_report_branch_category  — branch × category × gender counts
    ├── v_report_branch_ews       — branch × EWS counts
    ├── v_report_branch_domicile  — branch × domicile counts
    ├── v_report_branch_religion  — branch × religion counts
    ├── v_dashboard_totals        — overall summary for home screen
    └── v_admission_year_summary  — per-year totals

STORED PROCEDURES (called by backend API)
    ├── sp_search_students        — GET /students with all slicer filters
    ├── sp_get_student            — GET /students/{id} full detail
    ├── sp_get_student_fees       — GET /students/{id}/fees (18 terms)
    ├── sp_report_branch_category — GET /reports/branch-category?year=
    ├── sp_dashboard              — GET /dashboard
    └── sp_upsert_payment         — POST/PUT /payments
```

---

## MySQL Workbench Setup (Fresh Machine)

```
1. Run: sql/mysql/01_create_database.sql
2. Run: sql/mysql/02_schema.sql
3. Run: sql/mysql/03_views.sql
4. Run: sql/mysql/07_indexes_and_fulltext.sql
5. Run: sql/mysql/08_stored_procedures.sql

Import data (choose ONE):
  Option A — Python (recommended):
    pip install mysql-connector-python
    python etl/load_mysql.py --host localhost --user root --password YOURPASS

  Option B — Restore from dump (fastest):
    mysql -u root -p excel_app < data/excel_app_dump.sql

  Option C — SQL bulk load:
    Run: sql/mysql/06_load_csv.sql  (after enabling local_infile)

6. Verify: sql/mysql/09_full_test_suite.sql  (all results should show PASS)
```

---

## Team Responsibilities

| Member | Role | Key Files |
|---|---|---|
| **Member 1 (Android App)** | UI, Kotlin/Retrofit, connect to API | [docs/05_api_requirements.md](docs/05_api_requirements.md) |
| **Member 2 (Database)** | ✅ All done | This folder |
| **Member 3 (Excel/VBA)** | Confirm slicer behavior, VBA → app button mapping | [docs/06_excel_feature_mapping.md](docs/06_excel_feature_mapping.md) |

**Phone rules for Member 1:**
- Never download all 6,760 students at once. Default `limit=30` (max 100).
- Use stored procedures via backend API — phone never connects to MySQL directly.
- Cache lookup tables locally (branches, years, categories).
- Dates as `YYYY-MM-DD`. Amounts as numbers (not raw text).
