# Normalization

Starting point: one DATA row holds student + contact + admission + payment + 18 term fees. FEE MAST mixes a 72-row plan table with a 568-row year-sem table.

## 1NF — atomic values, no repeating groups

Violations in Excel:

- 18 term-fee columns (JUL-DEC20 … JAN-JUN29)
- FEE MAST semester columns 1–8
- `Total Amount` / submitted amount as `51400+14000`

Fixes:

- `student_term_fee (student_id, term_id, amount)`
- `fee_structure_semester (fee_structure_id, semester_no, amount)`
- Store `*_raw` text plus a parsed numeric total (sum of parts)

## 2NF — no partial dependence on a composite key

Student facts depend on enrolment / student_id, not on (student, term). Term amount depends on the full composite `(student_id, term_id)`. Fee semester amount depends on `(fee_structure_id, semester_no)`. Satisfied after the unpivot.

## 3NF — no transitive dependence

In Excel, Branch name/code, category names, religion, fee plan year, etc. repeat on every student row.

Moved to lookup tables:

- branch, eligible_category, alloted_category, religion
- fee_structure (+ year, category) instead of repeating plan text
- admission_status, admission_round, remark_type, payment_mode
- academic_term instead of encoding year in column names

1:1 splits (`contact`, `admission`, `payment`) are not required for 3NF (those attributes still depend only on student_id). They match the team ER and keep optional/sparse groups from cluttering `student`. Payment is 1:N so future extra receipts do not duplicate the student.

## Anomalies prevented

| Risk in Excel | Prevention |
|---|---|
| Change “CSE” department name on 914 rows | Update `branch` once |
| Same Unique ID on two people | Not used as PK; enrolment_no is unique |
| Fee plan amount edited in one DATA row only | Canonical amounts live in `fee_structure_semester`; DATA term columns imported as scheduled copies |
| Delete a student and lose a branch | Branch is independent |
| Inconsistent `ADMITTED` vs `Admitted` | Normalized to `ADMITTED` |
| `HINDU ` vs `HINDU` | Trim / uppercase on import |

## Practical 3NF exceptions (documented)

- `payment.payment_mode_raw` keeps the original messy string while `payment_mode_id` points to a cleaned code.
- `college_unique_id` kept nullable and non-unique because that is the real data.
- `mponline_password` stored as provided; **application must encrypt** — not a normalization issue.
