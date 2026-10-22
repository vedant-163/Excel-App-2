# Student Management App — Fixes Applied

This build was updated from `projectUpdated (2).zip` without resetting or deleting database data.

## Main fixes

- Removed the Android hardcoded `admin/admin123` login path and the local-login bypass.
- Android login now requires a successful FastAPI JWT login before entering the app.
- Removed the second local authentication path from Search and dashboard startup.
- Added stable database `student_id` to the Android `Student` model/cache/API mapping.
- Student edit/delete now use `student_id` when available instead of the college Unique ID.
- Backend student resolution now checks exact enrollment number before numeric student ID.
- Corrected admission year ↔ academic year mapping (`2024` ↔ `2024-25`).
- Search always uses the authenticated server-side search API.
- Payment history now carries branch information and matches students by enrollment number, not numeric DB ID.
- Payment creation now validates cumulative payments against the configured fee for year + branch + category + semester when a fee structure exists.
- Duplicate transaction IDs are rejected.
- Unknown payment modes are rejected with a 422 response.
- Unknown admission statuses no longer silently fall back to the first database row.
- Added `Other` gender support (`O`) in backend normalization/model.
- Added a non-destructive MySQL migration: `DATABASE/database/sql/mysql/11_gender_other_migration.sql`.
- Replaced the search view's one-payment-per-student join with a per-student payment aggregation so multiple payments do not duplicate student rows.
- Disabled Android app backup for student data.

## Database safety

Do **not** run `02_schema.sql` on an existing production database. It is destructive.

The new gender migration is additive, but should be run once on the existing MySQL database after checking the current `student.gender` column.

## Verification performed

- Python backend source compilation: PASS.
- Android Gradle build could not be executed in this environment because Gradle 9.5 is not cached and external network access is unavailable.
- Backend pytest collection could not run because the environment does not have the `pymysql` package installed.

The live MySQL database and real Android emulator were not modified by this work.
