from getpass import getpass

from sqlalchemy import select

from app.auth import hash_password
from app.database import SessionLocal
from app.models import StaffUser


def main() -> None:
    username = input("Admin username: ").strip()
    email = input("Admin email: ").strip().lower()
    name = input("Admin name: ").strip()
    password = getpass("Admin password: ")
    confirm = getpass("Confirm password: ")

    if not username or not email or not name:
        raise SystemExit("Username, email and name are required.")
    if len(password) < 12:
        raise SystemExit("Use a password of at least 12 characters.")
    if password != confirm:
        raise SystemExit("Passwords do not match.")

    with SessionLocal() as db:
        existing = db.scalar(
            select(StaffUser).where(
                (StaffUser.username == username) | (StaffUser.email == email)
            )
        )
        if existing:
            raise SystemExit("A staff account with that username or email already exists.")

        db.add(
            StaffUser(
                name=name,
                username=username,
                email=email,
                password_hash=hash_password(password),
                role="admin",
                is_active=True,
            )
        )
        db.commit()

    print("Administrator account created successfully.")


if __name__ == "__main__":
    main()
