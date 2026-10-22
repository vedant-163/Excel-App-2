-- Staff authentication migration. Safe for existing student data.
USE excel_app;

CREATE TABLE IF NOT EXISTS staff_user (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  full_name      VARCHAR(150) NOT NULL,
  username       VARCHAR(64) NOT NULL,
  email          VARCHAR(255) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  role           VARCHAR(32) NOT NULL DEFAULT 'staff',
  is_active      TINYINT(1) NOT NULL DEFAULT 1,
  failed_login_attempts INT NOT NULL DEFAULT 0,
  locked_until   DATETIME NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_staff_username (username),
  UNIQUE KEY uq_staff_email (email)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS audit_log (
  audit_id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  staff_id   INT UNSIGNED NULL,
  username   VARCHAR(64) NULL,
  action     VARCHAR(16) NOT NULL,
  path       VARCHAR(255) NOT NULL,
  status_code INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_staff_time (staff_id, created_at),
  INDEX idx_audit_time (created_at)
) ENGINE=InnoDB;

-- If staff_user was created by an older version of this migration, run: 
-- ALTER TABLE staff_user ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
--     ADD COLUMN locked_until DATETIME NULL;
