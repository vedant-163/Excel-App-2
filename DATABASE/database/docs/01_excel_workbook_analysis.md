# Excel Workbook Analysis — UG (Excel to App)

Source file: `c:\Users\vedan\Downloads\UG - Copy.xlsx` (21.0 MB)  
Analyzed: 24 Sep 2026

## Sheets

| Sheet | Role | Size | Store in MySQL? |
|---|---|---|---|
| **DATA** | Master student + admission + payment + 18 term-fee columns | 6,760 data rows × 57 columns (header + 6760) | Yes — split into normalized tables |
| **FEE MAST** | Fee structure master + year/sem helper codes | 568 rows × 23 columns (72 fee plans + 568 year-sem codes) | Yes — `fee_structure` + `fee_year_sem_code` |
| **SEARCH** | Formatted search / filter UI (slicers, named ranges) | Excel reports 10,000 × 702 used range; no stored row data in the first 80×200 cells | No — app UI + `v_student_search` |
| **REPORT 20-21 onwords** | Pivot-style dashboard (branch × category × gender, EWS, domicile, religion) | 30 × 31, merged headers, 13 merged ranges | No — SQL aggregation views |

AutoFilter on DATA: `A1:AL6761` (student core columns). Freeze pane: `A31`.  
One data-validation dropdown on **Branch** (partial cell list) with values `CE,CSE,EC,EE,EI,IPE,BME,IT,MECH,BEIL`. Actual DATA has additional codes (`CSE_FW`, `B.PHARMACY`, `CE-RR`, etc.).

## Excel features found

| Feature | Evidence | Database handling |
|---|---|---|
| Raw tables | DATA, FEE MAST | Normalized tables |
| AutoFilter | DATA `A1:AL6761` | `WHERE` / API query params |
| Slicers | Defined names `Slicer_Admission_Year`, `Slicer_Alloted_Category`, `Slicer_Branch`, `Slicer_Domicile`, `Slicer_Eligible_Category`, `Slicer_EWS`, `Slicer_Gender`, `Slicer_Religion`, `Slicer_Remarks` | Same filters on `v_student_search` |
| Pivot | Named range `DYNAMICPIVOT`; REPORT sheet layout | `GROUP BY` views |
| VBA / macros | Workbook opens with VBA flag; slicer list extension | Application workflows, not SQL |
| Formulas | No formula cells in DATA/FEE MAST sample rows (values are stored) | Parsed amounts for `51400+14000` style text |
| Dropdown | Branch list validation | `branch` lookup table |

## What is unique vs repeated

**Unique in DATA (true keys):**

- `Sr. No.` — 6,760 / 6,760 unique (Excel row serial). Candidate key, not a business key.
- `Enrolment No.` — 6,760 / 6,760 unique. **Natural primary key.**

**Not unique (do not use as PK):**

- `Unique ID` — only 3,673 filled, 3,669 distinct, **4 duplicate IDs** reused by different students (e.g. 221336 on CSE and EI). 3,087 blanks.
- `Class Roll No.` — 5,941 distinct (reused across years / batches).
- `Name` — 6,429 distinct (homonyms).
- `Adharcard No.` — 1,174 filled, 1,173 distinct.

**Repeated / lookup data:** Branch (22), Eligible Category (4), Alloted Category (42 raw / 41 after trim), Domicile (Y/N), Gender (M/F), EWS (Y/N), Status (`ADMITTED`/`Admitted` — same meaning), Religion (HINDU plus a trailing-space variant), Admission Year (6 values), `fee stru` (15 values, all present in FEE MAST), 18 term-fee columns repeated per student.

## Relationships between sheets

```
FEE MAST.FEE MAST  1 ─── N  DATA.fee stru     (all 6,760 DATA rows match a FEE MAST code)
DATA               N ─── 1  SEARCH filters     (UI only)
DATA               N ─── 1  REPORT pivots      (counts for a selected year; 2025-26 total = 1,189)
```

FEE MAST actually holds **two side-by-side tables**:

1. Columns A–K: 72 fee plans (`FEE MAST`, `FEE CATEGORY`, semester amounts 1–8, `YR`).
2. Columns U–W: 568 unique `year & sem` codes (`2017 BTBP1` …) used by pivots/slicers. Columns L–T are empty.

## Calculated / dirty fields

DATA has **no Excel formulas** in sampled rows, but several columns are not clean atomic values:

- `Total Amount` / `Student submitted Amount` often store **split payments** as text: `51400+\n14000`, `1,21,600`, `10000 online+41900`.
- Parser used on import: strip Indian commas, extract numbers, **sum** parts when `+` is present.
- Parsed workbook totals: Total Amount **515,165,638**; submitted **239,262,772.6**; DTE Fee **6,209,000**.
- Naive `pandas.to_numeric` under-counts because it drops comma/plus cells — do not use that as the Excel total.
- `Rank` mixes integers and tokens like `CSAB`.
- `Payment Mode` has 46 messy free-text variants (`CASH/CARD/UPI/DD`, `DIMAND DRAFT`, …).
- `Payment Date` mixes real dates and odd strings.
- `Ofline Admission Category` and `Adharcard Linked Mob. NO.` are **100% empty**.

## VBA / macro-related data to store

Nothing from VBA needs its own MySQL table. Persist:

- Slicer dimensions as lookup tables (already required by DATA).
- `DYNAMICPIVOT` / `year & sem` codes as `fee_year_sem_code` so Member 3 can rebuild pivot-like reports.

SEARCH is an interface sheet, not a data table (matches the provided ER note).
