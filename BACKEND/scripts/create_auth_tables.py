"""Create staff_user and audit_log tables and add admin user."""
from app.auth import hash_password
from app.database import SessionLocal
from sqlalchemy import text

db = SessionLocal()
try:
    db.execute(text("""
    CREATE TABLE IF NOT EXISTS staff_user (
      id INT AUTO_INCREMENT PRIMARY KEY,
      full_name VARCHAR(150) NOT NULL,
      username VARCHAR(64) NOT NULL,
      email VARCHAR(255) NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      role VARCHAR(32) NOT NULL DEFAULT 'staff',
      is_active TINYINT(1) NOT NULL DEFAULT 1,
      failed_login_attempts INT NOT NULL DEFAULT 0,
      locked_until DATETIME NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      UNIQUE KEY uq_staff_username (username),
      UNIQUE KEY uq_staff_email (email)
    ) ENGINE=InnoDB
    """))

    db.execute(text("""
    CREATE TABLE IF NOT EXISTS audit_log (
      audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
      staff_id INT NULL,
      username VARCHAR(64) NULL,
      action VARCHAR(16) NOT NULL,
      path VARCHAR(255) NOT NULL,
      status_code INT NOT NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_audit_staff_time (staff_id, created_at),
      INDEX idx_audit_time (created_at)
    ) ENGINE=InnoDB
    """))

    db.commit()
    print("Tables created OK")

    result = db.execute(text("SELECT COUNT(*) FROM staff_user WHERE username='admin'"))
    count = result.fetchone()[0]
    print(f"Admin users: {count}")

    if count == 0:
        pw_hash = hash_password("admin123")
        db.execute(text("""
            INSERT INTO staff_user (full_name, username, email, password_hash, role)
            VALUES ('Administrator', 'admin', 'admin@sgsits.ac.in', :ph, 'admin')
        """), {"ph": pw_hash})
        db.commit()
        print("Admin user created: username=admin password=admin123")
    else:
        print("Admin user already exists")

except Exception as e:
    print("ERROR:", e)
    db.rollback()
finally:
    db.close()
