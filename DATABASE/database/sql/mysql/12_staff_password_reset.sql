-- Additive migration for email-based staff password recovery.
-- Does not modify or delete existing student, fee, admission, or staff records.
USE excel_app;

CREATE TABLE IF NOT EXISTS staff_password_reset (
    reset_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    code_hash CHAR(64) NOT NULL,
    failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_staff_password_reset_staff (staff_id),
    INDEX idx_staff_password_reset_expiry (expires_at),
    CONSTRAINT fk_staff_password_reset_staff
        FOREIGN KEY (staff_id) REFERENCES staff_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
