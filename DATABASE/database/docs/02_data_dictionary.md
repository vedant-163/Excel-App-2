# Excel Data Dictionary

Legend: **PK** primary key in Excel sense / **CK** candidate key / **FK** maps to another sheet or lookup / **—** descriptive. Nullable is “can be empty in Excel”.

## DATA (6,760 rows)

| Excel Sheet | Column | Suggested SQL type | Meaning | Key? | Nullable | Unique when filled? |
|---|---|---|---|---|---|---|
| DATA | Sr. No. | INT | Excel serial | CK | No | Yes |
| DATA | Unique ID | VARCHAR(32) | College unique id | — | Yes (3087 empty) | No (4 duplicates) |
| DATA | Class Roll No. | VARCHAR(32) | Class roll (e.g. AB-10001) | — | No | No |
| DATA | Enrolment No. | VARCHAR(32) | University enrolment | **natural PK** | No | Yes |
| DATA | Branch | VARCHAR(32) | Programme / branch code | FK → branch | No | No (22 values) |
| DATA | Rank | VARCHAR(32) | Entrance rank or token (CSAB) | — | Yes | No |
| DATA | Marks | DECIMAL(10,6) | Entrance / qualifying marks | — | Yes | No |
| DATA | Roll No | VARCHAR(32) | Exam roll number | — | Yes | Almost (8 dups) |
| DATA | Name | VARCHAR(150) | Student name | — | No | No |
| DATA | Father | VARCHAR(150) | Father name | — | Rare | No |
| DATA | Mother | VARCHAR(150) | Mother name | — | Rare | No |
| DATA | Eligible Category | VARCHAR(16) | UR / SC / ST / OBC | FK | No | No (4) |
| DATA | Alloted Category | VARCHAR(64) | Seat allotment code (URXOP, …) | FK | No | No (42) |
| DATA | Domicile | CHAR(1) | MP domicile Y/N | — | No | No |
| DATA | Gender | CHAR(1) | M/F | — | No | No |
| DATA | Student PhoneNo | VARCHAR(20) | Student mobile | — | Rare | No |
| DATA | EWS | CHAR(1) | Economically weaker section Y/N | slicer | No | No |
| DATA | Status | VARCHAR(32) | Admission status (normalize to ADMITTED) | FK | No | No |
| DATA | Round | VARCHAR(64) | Counselling round | FK | Yes (5579 empty) | No (7) |
| DATA | Total Amount | DECIMAL + RAW text | Admission fee quoted; may be `a+b` | — | Partial | No |
| DATA | Student submitted Amount | DECIMAL + RAW text | Amount paid; may be split text | — | Often empty | No |
| DATA | Online Application Transaciton ID | VARCHAR(64) | Online txn / application id | — | Partial | No |
| DATA | Payment Mode | VARCHAR(64) | Free-text mode | FK (normalized) | Yes | No (46 messy) |
| DATA | Payment Date | DATE + RAW | Payment date | — | Partial | No |
| DATA | Date of Birth | DATE | DOB | — | Rare | No |
| DATA | Student Email ID | VARCHAR(150) | Student email | — | Often empty | No |
| DATA | Parents PhoneNo | VARCHAR(20) | Parent mobile | — | Often empty | No |
| DATA | Parents Email ID | VARCHAR(150) | Parent email | — | Often empty | No |
| DATA | Adharcard No. | VARCHAR(12) | Aadhaar | — | Mostly empty | Almost |
| DATA | Adharcard Linked Mob. NO. | VARCHAR(20) | Unused in this file | — | Always empty | — |
| DATA | Permanent Address | VARCHAR(500) | Permanent address | — | Rare | No |
| DATA | Present Address | VARCHAR(500) | Present address | — | Often empty | No |
| DATA | Religion | VARCHAR(32) | Religion (trim `HINDU `) | FK / slicer | No | No (6 after trim) |
| DATA | DTE Fee | DECIMAL(12,2) | DTE counselling fee | — | Partial | No (4 values) |
| DATA | MPOnline Password | VARCHAR(128) | Portal password (**sensitive**) | — | Often empty | No |
| DATA | Ofline Admission Category | VARCHAR(64) | Unused | — | Always empty | — |
| DATA | Admission Year | VARCHAR(16) | Batch year `2020-21` … `2025-26` | slicer | No | No (6) |
| DATA | Remarks | VARCHAR(128) | LATERAL ENTRY, NRI, GULF, … | FK / slicer | Mostly empty | No (11) |
| DATA | fee stru | VARCHAR(64) | Fee plan code | **FK → FEE MAST** | No | No (15) |
| DATA | JUL-DEC20 … JAN-JUN29 (18 cols) | DECIMAL(12,2) | Term-wise scheduled fee (0 = N/A) | unpivot → student_term_fee | No (filled, often 0) | No |

## FEE MAST

| Excel Sheet | Column | SQL type | Meaning | Key? |
|---|---|---|---|---|
| FEE MAST | FEE MAST | VARCHAR(64) | Plan code e.g. `2020 BTECH/BPH` | PK of fee plan (72 rows) |
| FEE MAST | FEE CATEGORY | VARCHAR(64) | Category family (8 codes) | FK |
| FEE MAST | 1 … 8 | DECIMAL(12,2) | Amount for semester 1–8 | unpivot |
| FEE MAST | YR | SMALLINT | Plan year 2017–2025 | — |
| FEE MAST | (cols L–T) | — | Empty | ignore |
| FEE MAST | Unnamed year col (U) | SMALLINT | Helper year | part of year_sem |
| FEE MAST | Unnamed sem code (V) | VARCHAR(32) | e.g. BTBP1 | — |
| FEE MAST | year & sem | VARCHAR(64) | `2017 BTBP1` | unique, 568 rows |

## SEARCH

Not a data table. Interface for filtering DATA. No columns to import. Implement as `GET /students` + query params.

## REPORT 20-21 onwords

| Block | Meaning |
|---|---|
| Cols A–N | Branch × Eligible category (OBC/SC/ST/UR) × Gender (F/M) + totals |
| Cols O–R | Branch × EWS (N/Y) |
| Cols S–V | Branch × Domicile (N/Y) |
| Cols W–AE | Branch × Religion |
| Grand Total row | For the slicer year (file snapshot total **1,189** = admission year 2025-26) |

These numbers are **aggregates of DATA**, not independent facts.
