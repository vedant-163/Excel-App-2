# API / data requirements for Member 1 (Android app)

Client: **Android phone** (Kotlin/Java). MySQL stays on a server; the phone never connects to MySQL directly.

```
Android app  →  HTTPS REST (JSON)  →  backend  →  MySQL excel_app
```

Use Retrofit/OkHttp (or equivalent). Base path: `/api/v1`  
Primary list model: view `v_student_search`  
Auth (login token), pagination, and screens are Member 1’s job. Database guarantees the payloads below.

**Phone rules**

- Never download all 6,760 students in one call. Default `limit=30` (max 100).
- Cache lookup lists locally (branches, years, categories) — they rarely change.
- List endpoint must stay light (no addresses, Aadhaar, or password).
- Dates as `YYYY-MM-DD` strings. Money as numbers (not Excel `51400+14000` text) plus optional `*_raw` on detail.
- Search as the user types: debounce 300ms, send `q`.

## Read

| API | SQL / view | Notes |
|---|---|---|
| `GET /students` | `v_student_search` + filters | SEARCH sheet + slicers |
| `GET /students/{id}` | student + contact + admission + payment + term fees | `{id}` = student_id or enrolment_no |
| `GET /students/{id}/fees` | `student_term_fee` ⨝ `academic_term` | 18 terms |
| `GET /lookups/branches` | `branch` | dropdown / slicer |
| `GET /lookups/eligible-categories` | `eligible_category` | |
| `GET /lookups/alloted-categories` | `alloted_category` | |
| `GET /lookups/religions` | `religion` | |
| `GET /lookups/admission-years` | distinct `admission.admission_year` | |
| `GET /lookups/remarks` | `remark_type` | |
| `GET /lookups/payment-modes` | `payment_mode` | |
| `GET /fee-structures` | fee_structure + semesters | FEE MAST |
| `GET /dashboard` | `v_dashboard_totals` + `v_admission_year_summary` | |
| `GET /reports/branch-category?year=2025-26` | query Q07 | REPORT left block |
| `GET /reports/branch-ews?year=` | Q08 | |
| `GET /reports/branch-domicile?year=` | `v_report_branch_domicile` | |
| `GET /reports/branch-religion?year=` | `v_report_branch_religion` | |
| `GET /payments?from=&to=` | Q09 | date filter |
| `GET /payments/monthly` | Q10 | |

### `GET /students` query params (Excel slicers)

`q`, `branch`, `admission_year`, `eligible_category`, `alloted_category`, `gender`, `ews`, `domicile`, `religion`, `remarks`, `status`, `sort`, `limit`, `offset`

Search `q` matches name, enrolment_no, class_roll_no, college_unique_id, phone.

## Write (coordinate before enabling deletes)

| API | Table | Rules |
|---|---|---|
| `POST /students` | student + contact + admission (+ optional payment) | enrolment_no unique; branch/categories must exist |
| `PUT /students/{id}` | same | do not change student_id |
| `DELETE /students/{id}` | CASCADE contact, admission, term fees, payments | confirm with college staff |
| `POST /payments` | payment | student_id required |
| `PUT /payments/{id}` | payment | keep `*_raw` if user types `50000+14000` |
| `POST /fee-structures` | fee_structure + 8 semester rows | fee_mast_code unique |
| `PUT /fee-structures/{id}` | | |
| `POST /lookups/branches` | branch | |

Do not send `mponline_password` to the phone list or search. Detail/admin only, encrypted at rest.

## Suggested Android screens ↔ APIs

| Android screen | Excel equivalent | Calls |
|---|---|---|
| Login | — | `POST /auth/login` (backend) |
| Home / dashboard cards | REPORT totals | `GET /dashboard` |
| Search students | SEARCH sheet | `GET /students?q=` |
| Filter chips / bottom sheet | Slicers | lookups + same GET with params |
| Student profile | DATA row | `GET /students/{id}` |
| Semester fee list | JUL-DEC / JAN-JUN columns | `GET /students/{id}/fees` |
| Reports (year spinner) | REPORT 20-21 onwards | `GET /reports/...` |
| Add / edit student | DATA entry | `POST` / `PUT /students` |
| Record payment | payment columns | `POST /payments` |

## List response envelope (Android paging)

```json
{
  "count": 6760,
  "limit": 30,
  "offset": 0,
  "results": [ { "student_id": 1, "name": "..." } ]
}
```

Use `count` + `offset` for RecyclerView infinite scroll.

## JSON sketch — student list item

```json
{
  "student_id": 1,
  "sr_no": 1,
  "college_unique_id": null,
  "enrolment_no": "0801ME201040",
  "class_roll_no": "AB-10001",
  "name": "JAYESH KETHWAS",
  "branch_code": "ME",
  "eligible_category": "SC",
  "alloted_category": "SCXOP",
  "gender": "M",
  "ews": "N",
  "domicile": "Y",
  "religion": "HINDU",
  "admission_year": "2020-21",
  "admission_status": "ADMITTED",
  "fee_structure": "2020 BTECH/BPH",
  "total_amount": 51400,
  "paid_amount": 0,
  "pending_amount": 51400
}
```

## Empty Excel columns

`Ofline Admission Category` and `Adharcard Linked Mob. NO.` can be omitted from v1 UI; columns exist for later use.
