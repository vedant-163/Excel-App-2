StudentManagementAPP – fixes in this package

Changes made in this pass:
- Fee Structure record filters now include academic years already present in the returned fee records, as well as the central academic-year catalog.
- Fee Structure branch filters include the centralized branch catalog and branches returned by existing fee records.
- Fee Structure category filter uses fee-plan categories rather than admission reservation categories.
- Add/Edit Fee Structure branch choices include known branch catalog options plus branches already present in the selected academic year's API data.
- New Payment fee-category choices now use categories returned by configured fee structures for the selected year/branch, with fee-plan category fallback instead of UR/OBC/SC/ST.
- Fee category fallback uses fee-plan category values.

Verification:
- Backend Python compileall syntax check passed.
- Android Kotlin/Gradle build could not run because Gradle distribution download failed (network unavailable). No claim of Android build success.

Important remaining verification / environment-specific steps:
- Add Student with no matching fee structure requires the included migration BACKEND/migrations/20261019_allow_student_without_fee_structure.sql to be reviewed and run manually in the correct MySQL schema. It changes student.fee_structure_id to nullable. Do not run until you confirm the table/column matches the live database and have a backup. Backend model already declares this field nullable.
- Fee & Payment crash and Fee Structure freeze cannot be confirmed fixed without the actual Android Logcat stack trace and live API/database. This package adds defensive/logic fixes but is not represented as fully crash-tested.
- Please capture Android Studio Logcat around the crash (first FATAL EXCEPTION line through Caused by) if the crash persists.
