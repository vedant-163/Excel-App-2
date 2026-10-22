from getpass import getpass

from app.auth import hash_password
from app.database import SessionLocal
from app.models import StaffUser


username = input("New admin username: ").strip()
email = input("New admin email: ").strip()
full_name = input("Admin full name: ").strip()
password = getpass("New admin password: ")
confirm = getpass("Confirm password: ")

if not username or not email or not full_name:
    raise SystemExit("Username, email and full name are required.")

if password != confirm:
    raise SystemExit("Passwords do not match.")

if len(password) < 12:
    raise SystemExit("Use a password of at least 12 characters.")

db = SessionLocal()

try:
    existing = db.query(StaffUser).filter(
        (StaffUser.username == username) |
        (StaffUser.email == email)
    ).first()

    if existing:
        raise SystemExit("That username or email already exists.")

    admin = StaffUser(
        username=username,
        email=email,
        name=full_name,
        password_hash=hash_password(password),
        role="admin",
        is_active=True,
        failed_login_attempts=0,
        locked_until=None,
    )

    db.add(admin)
    db.commit()
    print("Admin account created successfully.")

except Exception:
    db.rollback()
    raise
finally:
    db.close()