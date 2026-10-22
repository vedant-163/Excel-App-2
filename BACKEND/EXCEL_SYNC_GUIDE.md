# Excel ↔ Database Synchronization Guide

## Overview

The `excel_sync.py` module provides a one-way synchronization from the original Excel workbook into the MySQL database. The **database is the source of truth** for all records edited through the Android app.

## Source of Truth Policy

| Scenario | Winner |
|----------|--------|
| Record only in Excel (not in DB) | Excel → Import to DB |
| Record in DB only (created via app) | DB – no Excel change needed |
| Record in both, identical | No action |
| Record in both, different values | **DB wins** – Excel value is not imported |

## How to Run

### Prerequisites

```bash
cd BACKEND
pip install openpyxl pymysql sqlalchemy python-dotenv
```

### Import Mode (Excel → DB)

```bash
python scripts/sync_excel.py --excel "C:/path/to/SGSITS_Workbook.xlsx"
```

### Report Mode (Check Only, No Write)

```bash
python scripts/sync_excel.py --excel "C:/path/to/SGSITS_Workbook.xlsx" --mode report
```

### Dry Run (Parse Without Writing)

```bash
python scripts/sync_excel.py --excel "C:/path/to/SGSITS_Workbook.xlsx" --dry-run
```

### With Log File

```bash
python scripts/sync_excel.py --excel "C:/path/to/SGSITS_Workbook.xlsx" --log-file sync_log.txt
```

## Excel Sheet Structure Expected

### DATA Sheet
The script looks for a sheet named `DATA`, `Data`, or `Student Data`. Expected columns (column names are flexible, see STUDENT_COLUMN_MAP):

| Column | Description |
|--------|-------------|
| Enrollment No | Student's unique enrollment number (primary key) |
| Name | Student's full name |
| Branch | Branch code or name (CSE, IT, EC, etc.) |
| Gender | M/F/Male/Female |
| Father Name | Father's name |
| Mother Name | Mother's name |
| Date of Birth | DOB |
| Mobile | Student's phone number |
| Email | Student's email |
| Category | Eligible category (GEN, OBC, SC, ST, etc.) |
| Allocated Category | Actually allocated category |
| Admission Year | Academic year of admission (2021-22) |
| Status | Admission status |
| Round | Admission round |
| Religion | Religion |
| Domicile | Domicile state |
| EWS | Yes/No |
| Unique ID | College-assigned unique ID |
| Class Roll No | Class roll number |
| Roll No | University roll number |
| Rank | Entrance rank |
| Marks | Entrance marks |
| Remarks | Additional remarks |

### FEE MAST Sheet
The script looks for a sheet named `FEE MAST`, `Fee Mast`, or `FEE MASTER`. Expected columns:

| Column | Description |
|--------|-------------|
| Fee Mast Code | Unique code for the fee plan |
| Fee Category | Fee category (BTECH/BPH, BTECH SF&GF, etc.) |
| Academic Year | Year of fee structure (2021-22) |
| Branch | Branch this fee applies to |
| Semester 1..8 | Fee amounts for each semester |

## Conflict Resolution Details

1. **Student records**: Existing enrollment numbers in the database are **never overwritten** by Excel import. New enrollment numbers from Excel are inserted.

2. **Fee structures**: Existing fee structures (matched by year + branch + category) are preserved. New semester amounts from Excel use `INSERT ... ON DUPLICATE KEY UPDATE` which safely updates amounts.

3. **Duplicate enrollment numbers**: The first occurrence is used; subsequent duplicates are warned and skipped.

4. **Invalid data**: Rows with missing enrollment number or name are skipped with a warning.

5. **Unknown branches/categories**: Rows referencing unknown branch codes or fee categories are warned and skipped.

## Error Log Format

Errors and warnings are printed to the console and optionally to a log file:

```
2026-10-22 18:00:00 [WARNING] excel_sync: Row 45: Branch 'COMP SCI' not found in DB...
2026-10-22 18:00:01 [ERROR] excel_sync: Row 89 (0123456789): Duplicate key error...
```

## Adding New Columns

To add support for new Excel columns, add entries to `STUDENT_COLUMN_MAP` or `FEE_MAST_COLUMN_MAP` in `app/services/excel_sync.py` and update the corresponding INSERT statement.
