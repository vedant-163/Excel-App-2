-- Security hardening for an existing SGSITS Student Management database.
-- Does NOT modify, delete, or reset any student/admission/payment records.
USE excel_app;

ALTER TABLE staff_user
  ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS locked_until DATETIME NULL;

CREATE TABLE IF NOT EXISTS audit_log (
  audit_id    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  staff_id    INT UNSIGNED NULL,
  username    VARCHAR(64) NULL,
  action      VARCHAR(16) NOT NULL,
  path        VARCHAR(255) NOT NULL,
  status_code INT NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_staff_time (staff_id, created_at),
  INDEX idx_audit_time (created_at)
) ENGINE=InnoDB;
