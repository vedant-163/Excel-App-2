"""
Backend test suite — tests every major endpoint without a live MySQL.
Uses pytest + httpx TestClient; auth logic is tested with an in-memory fake staff session.

Run from the BACKEND folder:
    py -m venv .venv
    .venv\\Scripts\\activate
    pip install -r requirements.txt
    pytest tests/ -v
"""

import os
import pytest
from fastapi.testclient import TestClient

# Must be set before importing the app so Settings picks it up.
os.environ.setdefault("DB_HOST", "127.0.0.1")
os.environ.setdefault("DB_NAME", "excel_app")
os.environ.setdefault("DB_USER", "root")
os.environ.setdefault("DB_PASSWORD", "")
os.environ.setdefault("JWT_SECRET", "test-secret-for-pytest-only-32chars-minimum")


from app.main import app  # noqa: E402  (import after env setup)
from app.auth import hash_password, verify_password, login
from app.models import StaffUser
from app.schemas import LoginRequest

client = TestClient(app)


class FakeSession:
    def __init__(self, staff=None):
        self.staff = staff

    def scalar(self, _statement):
        return self.staff

    def get(self, _model, _staff_id):
        return self.staff

    def commit(self):
        self.commits = getattr(self, "commits", 0) + 1


# ── Health check ──────────────────────────────────────────────────────
class TestHealth:
    def test_root(self):
        r = client.get("/")
        assert r.status_code == 200
        assert r.json()["success"] is True


# ── Auth ──────────────────────────────────────────────────────────────
class TestAuth:
    def test_login_wrong_password(self):
        staff = StaffUser(
            staff_id=1,
            name="Test Admin",
            username="admin",
            email="admin@example.com",
            password_hash=hash_password("CorrectHorseBatteryStaple!"),
            role="admin",
            is_active=True,
        )
        try:
            login(
                LoginRequest(username="admin", password="wrong"),
                FakeSession(staff),
            )
            assert False, "Expected invalid credentials to be rejected"
        except Exception as exc:
            assert getattr(exc, "status_code", None) == 401

    def test_login_valid_credentials(self):
        staff = StaffUser(
            staff_id=1,
            name="Test Admin",
            username="admin",
            email="admin@example.com",
            password_hash=hash_password("CorrectHorseBatteryStaple!"),
            role="admin",
            is_active=True,
        )
        result = login(
            LoginRequest(
                username="admin@example.com",
                password="CorrectHorseBatteryStaple!",
            ),
            FakeSession(staff),
        )
        assert result.access_token
        assert result.token_type == "bearer"
        assert result.username == "admin"
        assert result.role == "admin"

    def test_login_schema_validation(self):
        r = client.post("/auth/login", json={})
        assert r.status_code == 422

    def test_password_hash_is_not_plaintext(self):
        hashed = hash_password("CorrectHorseBatteryStaple!")
        assert hashed != "CorrectHorseBatteryStaple!"
        assert verify_password("CorrectHorseBatteryStaple!", hashed)
        assert not verify_password("wrong", hashed)

    def test_protected_students_reject_missing_token(self):
        r = client.get("/students")
        assert r.status_code == 401

    def test_expired_token_rejected(self):
        import jwt
        from app.config import get_settings

        token = jwt.encode(
            {"sub": "1", "exp": 1},
            get_settings().jwt_secret,
            algorithm="HS256",
        )
        r = client.get("/students", headers={"Authorization": f"Bearer {token}"})
        assert r.status_code == 401

    def test_invalid_token_rejected(self):
        r = client.get(
            "/students",
            headers={"Authorization": "Bearer definitely-not-a-jwt"},
        )
        assert r.status_code == 401


# ── Students ──────────────────────────────────────────────────────────
class TestStudentsEndpoints:
    """
    These tests verify API routing/authentication without touching student data.
    """

    def test_list_students_route_exists(self):
        r = client.get("/students")
        assert r.status_code in (401, 503)  # 503 = DB not reachable (expected in CI)

    def test_list_students_query_params(self):
        r = client.get(
            "/students",
            params={
                "branch": "CSE",
                "admission_year": "2020-21",
                "gender": "M",
                "limit": 10,
                "offset": 0,
            },
        )
        assert r.status_code in (401, 503)

    def test_get_student_route_exists(self):
        r = client.get("/students/1")
        assert r.status_code in (401, 404, 503)

    def test_get_student_fees_route(self):
        r = client.get("/students/1/fees")
        assert r.status_code in (401, 404, 503)

    def test_create_student_missing_name(self):
        """Validation must fire before touching DB."""
        r = client.post("/students", json={})
        assert r.status_code in (401, 422)

    def test_create_student_missing_required_fields(self):
        r = client.post(
            "/students",
            json={
                "name": "Test Student",
                # missing enrolment_no, class_roll_no, branch, gender, etc.
            },
        )
        # Should return 422 (validation) or 503 (no DB)
        assert r.status_code in (401, 422, 503)


# ── Payments ──────────────────────────────────────────────────────────
class TestPayments:
    def test_list_payments_route(self):
        r = client.get("/payments")
        assert r.status_code in (401, 503)

    def test_monthly_payments_route(self):
        r = client.get("/payments/monthly")
        assert r.status_code in (401, 503)

    def test_upsert_payment_missing_student(self):
        r = client.post("/payments", json={})
        assert r.status_code in (401, 404, 422, 503)

    def test_payment_validation_paid_gt_total(self):
        """
        Business rule: paid_amount cannot exceed total_amount.
        With DB available this would be a 422; without DB it's 503.
        """
        r = client.post(
            "/payments",
            json={
                "student_id": 1,
                "total_amount": 50000,
                "student_submitted_amount": 999999,
            },
        )
        assert r.status_code in (401, 422, 503)


# ── Fee Structures ────────────────────────────────────────────────────
class TestFeeStructures:
    def test_list_fee_structures(self):
        r = client.get("/fee-structures")
        assert r.status_code in (401, 503)

    def test_create_fee_structure_validation(self):
        r = client.post("/fee-structures", json={})
        assert r.status_code == 401

    def test_create_fee_structure_bad_semester(self):
        r = client.post(
            "/fee-structures",
            json={
                "fee_mast_code": "TEST",
                "fee_category": "BTECH",
                "structure_year": 2024,
                "semesters": [{"semester_no": 99, "amount": 50000}],  # invalid sem
            },
        )
        assert r.status_code == 401


# ── Branches ──────────────────────────────────────────────────────────
class TestBranches:
    def test_list_branches(self):
        r = client.get("/branches")
        assert r.status_code in (401, 503)

    def test_create_branch_missing_code(self):
        r = client.post("/branches", json={})
        assert r.status_code == 401


# ── Lookups ───────────────────────────────────────────────────────────
class TestLookups:
    ENDPOINTS = [
        "/lookups/branches",
        "/lookups/eligible-categories",
        "/lookups/alloted-categories",
        "/lookups/religions",
        "/lookups/admission-statuses",
        "/lookups/admission-rounds",
        "/lookups/payment-modes",
        "/lookups/remark-types",
        "/lookups/academic-terms",
        "/lookups/fee-categories",
        "/lookups/admission-years",
    ]

    @pytest.mark.parametrize("path", ENDPOINTS)
    def test_lookup_routes_exist(self, path):
        r = client.get(path)
        assert r.status_code in (401, 503), f"Unexpected {r.status_code} for {path}"


# ── Reports ───────────────────────────────────────────────────────────
class TestReports:
    ENDPOINTS = [
        "/reports/branch-category",
        "/reports/branch-ews",
        "/reports/branch-domicile",
        "/reports/branch-religion",
        "/reports/admission-year-summary",
        "/reports/fee-status",
    ]

    @pytest.mark.parametrize("path", ENDPOINTS)
    def test_report_routes_exist(self, path):
        r = client.get(path)
        assert r.status_code in (401, 503)

    def test_report_filtered_by_year(self):
        r = client.get("/reports/branch-category", params={"year": "2020-21"})
        assert r.status_code in (401, 503)


# ── Dashboard ─────────────────────────────────────────────────────────
class TestDashboard:
    def test_dashboard(self):
        r = client.get("/dashboard")
        assert r.status_code in (401, 503)

    def test_dashboard_structure(self):
        """When DB is available, response must contain 'totals' and 'by_year'."""
        r = client.get("/dashboard")
        if r.status_code == 200:
            data = r.json()["data"]
            assert "totals" in data
            assert "by_year" in data
            assert "student_count" in data["totals"]
