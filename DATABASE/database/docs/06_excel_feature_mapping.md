# Excel feature → database / app split

For Member 3 (Excel/VBA) and Member 1 (app). MySQL stores data only.

| Excel feature | What it does in the workbook | Database | Application |
|---|---|---|---|
| DATA table | One row per student | Normalized tables + `v_student_search` | Student list / form |
| FEE MAST | Fee plans by year/category/semester | `fee_structure`, `fee_structure_semester` | Fee master screen |
| FEE MAST year & sem | Pivot helper codes | `fee_year_sem_code` | Optional report labels |
| SEARCH sheet | Find / filter students | `WHERE` on view | Android search bar + RecyclerView |
| AutoFilter | Column filters | Query params | Filter chips / bottom sheet |
| Slicers (Year, Branch, Category, EWS, Gender, Domicile, Religion, Remarks) | Interactive filter of DATA/REPORT | Same dimensions as indexed columns | Spinners / chips on phone |
| REPORT pivot | Counts by branch / category / gender / EWS / domicile / religion | `GROUP BY` views + Q07–Q08 | Report screen; year spinner |
| DYNAMICPIVOT | Named source for pivot | Views | Rebuild charts from API |
| VBA / macros | UI automation, likely refresh/search | None | Button handlers calling API |
| Dropdown (Branch) | Restricts some DATA.Branch cells | `branch` table | Android Spinner / dropdown |
| Split fee text `51400+14000` | Informal multiple receipts | `total_amount` parsed sum + `total_amount_raw` | Show raw + total; optional installment UI later |
| MPOnline Password | Stored in sheet | `student.mponline_password` | Encrypt; never list in grids |
| Frozen header / print | Presentation | — | App layout |

```
MySQL (server)
  → Backend REST/JSON
    → Android phone (search, filters, reports, forms)
```

Do not put the MySQL server or 6,760-row dump inside the APK. Cache lookups and the last search page locally if offline viewing is needed.
