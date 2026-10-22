# Staff Forgot Password setup

This is an additive feature. It keeps the existing login/JWT flow and student/fee tables unchanged.

## What was added

- Android login screen: **Forgot password?** dialog, request-code step, and reset-password step.
- FastAPI public endpoints:
  - `POST /auth/forgot-password` — sends a six-digit code to the registered staff email.
  - `POST /auth/reset-password` — verifies the code and stores the new Argon2 password hash.
- MySQL table `staff_password_reset` stores only the SHA-256 hash of the code, expiry, usage status, and failed attempts.
- Codes expire after 15 minutes by default, are single-use, and allow at most five attempts. Requests have a 60-second per-account cooldown.

## Required one-time setup

1. Back up the database before running any migration.
2. In MySQL Workbench, run `DATABASE/database/sql/mysql/12_staff_password_reset.sql` against the existing `excel_app` database. It only creates a new table; it does not change student, admission, fee, payment, or staff records.
3. In `BACKEND/.env`, configure SMTP using values supplied by the college/IT administrator. Do not put credentials in source code, prompts, or a shared ZIP:

   ```env
   SMTP_HOST=smtp.example.edu
   SMTP_PORT=587
   SMTP_USERNAME=your-approved-sender-account
   SMTP_PASSWORD=your-smtp-app-password
   SMTP_FROM_EMAIL=your-approved-sender@example.edu
   SMTP_USE_TLS=true
   PASSWORD_RESET_EXPIRY_MINUTES=15
   ```

   The values above are placeholders, not working credentials. For Gmail, use an approved account and an App Password if the account policy supports it; never use or share your normal account password. College IT may require its own SMTP relay.

4. Restart FastAPI after updating `.env`.
5. Build and install the Android app. On the Login screen tap **Forgot password?**, request a code for a registered staff email, then enter the code and a new password of at least 10 characters.

## Important behavior

- If SMTP is not configured, the endpoint returns a clear configuration error and staff should contact the authorized administrator. It does not pretend an email was sent.
- The app's existing API base URL still needs to point to the computer/server running FastAPI. Keep that URL unchanged if the current connection is working; if the backend host changes, update the base URL to the new reachable address.
- Test with a non-critical staff test account first. Verify code expiry, wrong-code limit, successful login with the new password, and rejection of the old password.
- The reset table migration is additive, but take a database backup before all production changes.
