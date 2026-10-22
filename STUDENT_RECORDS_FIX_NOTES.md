# Student Records fixes in this package

1. Add Student no longer fails solely because the selected admission year/branch has no matching fee structure. The fee structure link is optional until assigned later.
2. Edit Student Date of Birth uses a date picker rather than free-text typing.
3. Edit Student Admission Round includes `Not specified`, which saves as a blank/null round.

## Required manual MySQL step
Run `BACKEND/migrations/20261019_allow_student_without_fee_structure.sql` in MySQL Workbench against `excel_app` before testing Add Student. This is needed because the existing database column may currently be NOT NULL. Back up the database first.

Android compilation and live MySQL/API testing have not been performed in this environment.
