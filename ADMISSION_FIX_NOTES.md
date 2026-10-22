# Admission Module Fix Notes

Changes in this revision:
- Allocated Category options are loaded from the existing authenticated `GET /lookups/alloted-categories` endpoint, which reads the `alloted_category` reference table. The Android UI no longer uses the hardcoded eligible-category list for Allocated Category dropdowns/filters.
- Admission screens fetch all student pages from the existing API instead of relying only on the initial cached first page. This helps Summary and Records reflect the complete data set.
- Admission Year is treated as the calendar-year start; stored database values such as `2025-26` are normalized for display and filtering as Academic Year.
- Added separate Admission Year and Academic Year filters in Records and Summary.
- Added `Not Specified` for NULL Admission Round and send a JSON null when saving an unspecified round; backend request schema accepts nullable rounds.
- Status matching is case-insensitive and summary category grouping uses Allocated Category only.
- Admission updates no longer clear an existing offline admission category merely because the list API does not expose that field.

Verification performed in this environment:
- Python backend `compileall`: passed.
- `tests/test_excel_logic.py`: 42 passed.
- Full backend API tests could not be collected because the environment lacks the `pymysql` package.
- Android Gradle compilation could not start because the Gradle wrapper distribution could not be downloaded (`UnknownHostException: services.gradle.org`). Therefore Android compilation is NOT verified here.
- Live MySQL behavior was not testable in this environment. Please run the included project on the local machine and verify the real database lookup values.

## Follow-up review (2026-10-20)
- Fixed `ApiClient.getAllStudents()` pagination so a short but non-empty page (for example, 50 rows when 100 were requested) no longer ends the fetch prematurely. Fetching continues by the actual number of rows returned until an empty page is received.
- Added a no-progress guard to stop safely if the API repeats the same page, and deduplicated records using student ID/enrollment/unique ID.
- Polished the Profile screen with a generated initials avatar, clearer account copy, and a more consistent active-session label.
- Android build was attempted again, but Gradle could not download its distribution due to unavailable network access in this environment. Build and live database behavior remain unverified.

## Root-cause fix pass (2026-10-20)
- Admission loading now requests up to 10,000 students per page. The backend route and service previously capped requests at 100, forcing many sequential HTTP/database round trips; older deployments that capped responses at 50 made the issue particularly visible.
- Reports now fetch the bounded current dataset in one request (up to 10,000) instead of serially requesting 100 rows at a time. This removes dozens of repeated count/view queries during Generate.
- Report branch filtering now matches both `branch_code` and `branch_name`, including common display-name variants. Previously the endpoint only matched branch codes, so dropdown labels often produced zero matches.
- `v_student_search` now LEFT JOINs `fee_structure`; a student without an assigned fee structure no longer disappears from student/admission/report listings. Re-run `DATABASE/database/sql/mysql/03_views.sql` against the intended database to apply this view change.
- Verification in this package: backend Python compileall passes. Live database query timings and Android Gradle build still require verification in the user's local environment.
