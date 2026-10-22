"""Read-only smoke test of every GET endpoint against the live MySQL database.

Usage (from BACKEND):  python scripts/smoke_test_live.py
Mints a token for the first active staff user locally; no password needed.
Only GET requests are made, so no data is modified.
"""

import sys
import warnings
from pathlib import Path

warnings.filterwarnings("ignore")
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from fastapi.testclient import TestClient  # noqa: E402
from sqlalchemy import func, select  # noqa: E402

from app.auth import create_token  # noqa: E402
from app.config import get_settings  # noqa: E402
from app.database import SessionLocal  # noqa: E402
from app.main import app  # noqa: E402
from app.models import StaffUser, Student  # noqa: E402

LOOKUPS = (
    "branches eligible-categories alloted-categories religions admission-statuses "
    "admission-rounds payment-modes remark-types academic-terms fee-categories "
    "admission-years"
).split()
REPORTS = (
    "branch-category branch-ews branch-domicile branch-religion "
    "admission-year-summary fee-status"
).split()


def main() -> int:
    db = SessionLocal()
    staff = db.scalar(select(StaffUser).where(StaffUser.is_active.is_(True)))
    count = db.scalar(select(func.count()).select_from(Student))
    print(f"active staff: {staff.username if staff else None} | students: {count}")
    if not staff:
        print("No active staff user. Run scripts/create_admin.py first.")
        return 2

    headers = {"Authorization": "Bearer " + create_token(staff, get_settings()).access_token}
    client = TestClient(app, raise_server_exceptions=False)
    sid = db.scalar(select(Student.student_id).limit(1))

    paths = ["/", "/dashboard", "/students?limit=3", "/payments", "/payments/monthly",
             "/fee-structures", "/branches"]
    if sid:
        paths += [f"/students/{sid}", f"/students/{sid}/fees"]
    paths += [f"/lookups/{x}" for x in LOOKUPS]
    paths += [f"/reports/{x}" for x in REPORTS] + ["/reports/branch-category?year=2020-21"]

    failures = 0
    for path in paths:
        r = client.get(path, headers=headers)
        ok = r.status_code == 200
        failures += not ok
        print(r.status_code, path, "" if ok else r.text[:300])
    print(f"\n{len(paths) - failures}/{len(paths)} OK")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
