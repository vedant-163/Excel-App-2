USE excel_app;

SELECT id, username, email, role, is_active,
       failed_login_attempts, locked_until
FROM staff_user
WHERE username = 'admin2';