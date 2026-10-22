"""
SGSITS Student Management System – FastAPI Backend
Entry point: uvicorn app.main:app --reload
"""

from fastapi import Depends, FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import jwt
from sqlalchemy import select

from app.database import SessionLocal
from app.models import AuditLog

from app.config import get_settings
from app.deps import require_user
from app.exceptions import register_exception_handlers
from app.routers import admissions, auth, branches, dashboard, fee_structures, lookups, payments, reports, students

settings = get_settings()

app = FastAPI(
    title="SGSITS Student Management API",
    description=(
        "Python backend for the SGSITS Student Management Android app. "
        "Connects the Kotlin UI with the MySQL database, "
        "replicating Excel slicers, formulas and data-entry workflows."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# ── CORS ─────────────────────────────────────────────────────────────
# Allow the Android emulator (10.0.2.2) and any LAN address to call the API.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Exception handlers ────────────────────────────────────────────────
register_exception_handlers(app)


@app.middleware("http")
async def audit_mutating_requests(request: Request, call_next):
    """Record staff mutations without storing request bodies or secrets."""
    response = await call_next(request)
    if request.method not in {"POST", "PUT", "PATCH", "DELETE"}:
        return response

    username = None
    staff_id = None
    authorization = request.headers.get("authorization", "")
    if authorization.lower().startswith("bearer "):
        token = authorization.split(" ", 1)[1].strip()
        try:
            payload = jwt.decode(
                token,
                settings.jwt_secret,
                algorithms=["HS256"],
            )
            staff_id = int(payload.get("sub"))
            username = payload.get("username")
        except Exception:
            pass

    # Never let audit logging make a successful business request fail.
    db = SessionLocal()
    try:
        db.add(
            AuditLog(
                staff_id=staff_id,
                username=username,
                action=request.method,
                path=request.url.path[:255],
                status_code=response.status_code,
            )
        )
        db.commit()
    except Exception:
        db.rollback()
    finally:
        db.close()

    return response

# ── Routers ───────────────────────────────────────────────────────────
app.include_router(auth.router,           tags=["Auth"])
app.include_router(lookups.public_router, tags=["Public Lookups"])
protected = {"dependencies": [Depends(require_user)]}

app.include_router(students.router,       tags=["Students"], **protected)
app.include_router(admissions.router,     tags=["Admissions"], **protected)
app.include_router(payments.router,       tags=["Payments"], **protected)
app.include_router(fee_structures.router, tags=["Fee Structures"], **protected)
app.include_router(branches.router,       tags=["Branches"], **protected)
app.include_router(lookups.router,        tags=["Lookups"], **protected)
app.include_router(reports.router,        tags=["Reports"], **protected)
app.include_router(dashboard.router,      tags=["Dashboard"], **protected)


# ── Health check ──────────────────────────────────────────────────────
@app.get("/", summary="Health check")
def root():
    return {"success": True, "message": "SGSITS backend is running", "version": "1.0.0"}
