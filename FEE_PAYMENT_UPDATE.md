# Fee & Payment DB-connected update

This update keeps existing data and adds only nullable context columns.

## MySQL migration (run once)
Run:
`DATABASE/database/sql/mysql/10_fee_payment_context.sql`

It adds:
- payment.academic_year
- payment.fee_category
- payment.semester_no
- fee_structure.branch_code

No rows are deleted, truncated, or reset.

## App flow
Android Fee & Payment -> FastAPI -> MySQL.
Payment records are reloaded from the server after save.
Fee structures are loaded from the server for applicable-fee calculation.

## Important
The Android project could not be Gradle-compiled in this environment because the Gradle 9.5 distribution host was unreachable. Backend Python compile checks pass.
