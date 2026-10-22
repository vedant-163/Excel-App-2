# SGSITS Student Management – Python Backend

FastAPI backend that connects the **Kotlin Android app** (Member 1) to the **MySQL database** (Member 2/3).

---

## Quick Start

```bash
cd C:\APP\BACKEND

# 1. Create virtual environment
py -m venv .venv
.venv\Scripts\activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure database credentials
copy .env.example .env
# Edit .env and set DB_PASSWORD and a random JWT_SECRET

# 4. Run the server
uvicorn app.main:app --reload
```

Open **http://127.0.0.1:8000/docs** for the interactive Swagger UI.

---

## Project Structure

```
BACKEND/
├── app/
│   ├── main.py          ← FastAPI app, CORS, router wiring
│   ├── config.py        ← Settings from .env
│   ├── database.py      ← SQLAlchemy engine + session
│   ├── models.py        ← ORM models (mirrors Member 2's schema)
│   ├── schemas.py       ← Pydantic request/response models
│   ├── deps.py          ← FastAPI dependency injectors
│   ├── auth.py          ← JWT login + token verification
│   ├── exceptions.py    ← Unified error handlers
│   ├── routers/
│   │   ├── auth.py           POST /auth/login
│   │   ├── students.py       GET/POST/PUT/DELETE /students
│   │   ├── payments.py       GET/POST/PUT /payments
│   │   ├── fee_structures.py GET/POST/PUT/DELETE /fee-structures
│   │   ├── branches.py       GET/POST/PUT/DELETE /branches
│   │   ├── lookups.py        GET /lookups/*  (dropdown data)
│   │   ├── reports.py        GET /reports/*  (Excel REPORT sheet)
│   │   └── dashboard.py      GET /dashboard
│   └── services/
│       ├── student_service.py  ← CRUD + search logic
│       ├── payment_service.py  ← Payment logic + validation
│       ├── lookups.py          ← FK resolution helpers
│       └── excel_logic.py      ← Excel formula reimplementations
├── tests/
│   └── test_api.py      ← pytest test suite
├── requirements.txt
├── .env.example
└── README.md
```

---

## API Endpoints

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/login` | Returns JWT Bearer token |

### Students (Excel DATA sheet)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/students` | List with slicer filters + free-text search |
| GET | `/students/{id}` | Full student detail |
| GET | `/students/{id}/fees` | Term-wise fees (18 Excel columns) |
| POST | `/students` | Create student |
| PUT | `/students/{id}` | Update student |
| DELETE | `/students/{id}` | Delete student |

### Payments (Excel payment columns)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/payments` | List payments (optional date filter) |
| GET | `/payments/monthly` | Monthly totals for charts |
| GET | `/payments/{id}` | Single payment |
| POST | `/payments` | Create/update payment |
| PUT | `/payments/{id}` | Update payment by ID |

### Fee Structures (Excel FEE MAST sheet)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/fee-structures` | All structures with semester amounts |
| GET | `/fee-structures/{id}` | Single structure |
| POST | `/fee-structures` | Create new |
| PUT | `/fee-structures/{id}` | Update |
| DELETE | `/fee-structures/{id}` | Delete |

### Branches
| Method | Path | Description |
|--------|------|-------------|
| GET | `/branches` | All branches |
| POST | `/branches` | Add branch |
| PUT | `/branches/{id}` | Update |
| DELETE | `/branches/{id}` | Delete (blocked if students assigned) |

### Lookups (dropdown data for Android spinners)
All `GET`:
- `/lookups/branches`
- `/lookups/eligible-categories`
- `/lookups/alloted-categories`
- `/lookups/religions`
- `/lookups/admission-statuses`
- `/lookups/admission-rounds`
- `/lookups/payment-modes`
- `/lookups/remark-types`
- `/lookups/academic-terms`
- `/lookups/fee-categories`
- `/lookups/admission-years`

### Reports (Excel REPORT 20-21 sheet)
All `GET`, all accept optional `?year=2020-21`:
- `/reports/branch-category` — Branch × OBC/SC/ST/UR × M/F pivot
- `/reports/branch-ews` — Branch × EWS
- `/reports/branch-domicile` — Branch × domicile
- `/reports/branch-religion` — Branch × religion
- `/reports/admission-year-summary`
- `/reports/fee-status` — Fully paid / partially paid / unpaid counts

### Dashboard
| Method | Path | Description |
|--------|------|-------------|
| GET | `/dashboard` | Student count + fee totals + year breakdown |

---

## Excel Feature → API Mapping

| Excel Feature | Backend Implementation |
|--------------|----------------------|
| DATA sheet (57 cols) | `GET/POST/PUT /students` + related tables |
| SEARCH sheet slicers | `GET /students?branch=CSE&gender=M&...` |
| FEE MAST sheet | `GET/POST/PUT /fee-structures` |
| REPORT 20-21 pivots | `GET /reports/*` |
| SP sp_search_students | `student_service.search_students()` |
| SP sp_upsert_payment | `payment_service.upsert_payment()` |
| SP sp_dashboard | `GET /dashboard` |
| Amount formula `A+B` | `parse_amount()` in `excel_logic.py` |
| Pending = Total − Paid | `pending_amount()` in `excel_logic.py` |
| Gender M/Male/Boy | `normalize_gender()` in `excel_logic.py` |
| Admission year 2020-21 | `normalize_admission_year()` |

---

## Android Integration

### Emulator address
- Android Emulator → use `10.0.2.2:8000` (not 127.0.0.1)
- Physical device → use your PC's LAN IP, e.g. `192.168.1.5:8000`

### Authentication flow
```
POST /auth/login  { "username": "admin", "password": "..." }
→ { "access_token": "eyJ...", "token_type": "bearer" }

All write requests:
Authorization: Bearer eyJ...
```

### JSON response format (consistent across all endpoints)
```json
{
  "success": true,
  "message": "Student created successfully",
  "data": { ... }
}
```

Error responses:
```json
{
  "success": false,
  "message": "enrolment_no is required",
  "data": null
}
```

---

## Running Tests

```bash


pytest tests/ -v

# With live MySQL (full integration)
pytest tests/ -v
```

---

## Environment Variables (.env)

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | 127.0.0.1 | MySQL host |
| `DB_PORT` | 3306 | MySQL port |
| `DB_NAME` | excel_app | Database name |
| `DB_USER` | root | MySQL user |
| `DB_PASSWORD` | (empty) | MySQL password |
| `JWT_SECRET` | required | Random secret, at least 32 characters |
| `JWT_EXPIRE_MINUTES` | 480 | Token lifetime |
| `CORS_ORIGINS` | * | Allowed origins (set to app URL in prod) |


## Staff authentication setup

1. Apply `../DATABASE/database/sql/mysql/07_auth.sql` to the existing `excel_app` database.
2. Set a strong random `JWT_SECRET` in `.env` (32+ characters).
3. Install the backend requirements.
4. From `BACKEND`, run `python scripts/create_admin.py`.
5. Enter the administrator username, email, name, and password interactively. The password is Argon2-hashed and is never written to source code.
6. Start FastAPI with `uvicorn app.main:app --reload`.

`/auth/login` accepts a username or registered email and returns a short-lived Bearer JWT. All student, payment, fee-structure, branch, lookup, report, and dashboard endpoints require that token. There is no public registration endpoint.

## Security hardening migration

After applying `DATABASE/database/sql/mysql/07_auth.sql`, also apply
`DATABASE/database/sql/mysql/08_security_hardening.sql`. It adds login-lockout
fields and the audit log table. It does not modify or delete student data.


## Staff password recovery (optional)

Email-based Forgot Password requires SMTP settings in the local `BACKEND/.env`: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM_EMAIL`, and `SMTP_USE_TLS`. The recovery table migration is `DATABASE/database/sql/mysql/12_staff_password_reset.sql`. See `PASSWORD_RECOVERY_SETUP.md` for setup and testing. Keep `.env` private and do not include it in shared ZIP files.
