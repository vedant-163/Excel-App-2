# SGSITS Student Management System

A multi-tier client-server application converting the comprehensive SGSITS Excel admission and fee workbook (`UG.xlsm`) into a production-grade software suite:
- **Backend:** FastAPI Python REST API with SQLAlchemy 2.0 ORM, JWT authentication, and Argon2 password hashing.
- **Database:** MySQL 8.0 relational database (18 normalized 3NF tables, 8 analytical views, 6 stored procedures, and 6,760 verified student records).
- **Frontend:** Native Android mobile application built with Kotlin, Jetpack Compose, Material3, and Android KeyStore AES-GCM encryption.

---

## 🗂️ Project Structure

```text
Excel-App-2/
│
├── BACKEND/                         # Python FastAPI Backend
│   ├── app/
│   │   ├── main.py                  # API Entry point, CORS & audit logging middleware
│   │   ├── config.py                # Environment configuration & DB URL resolver
│   │   ├── database.py              # SQLAlchemy engine & session pool
│   │   ├── models.py                # 3NF ORM models & database views
│   │   ├── schemas.py               # Pydantic validation (supports snake_case and camelCase)
│   │   ├── auth.py                  # JWT authentication & Argon2 password hashing
│   │   ├── deps.py                  # Authentication & DB session dependencies
│   │   ├── exceptions.py            # Global exception handlers
│   │   ├── routers/                 # Modular endpoints (students, payments, admissions, etc.)
│   │   └── services/                # Business logic, fee calculation & Excel formula mappings
│   ├── scripts/                     # Admin account creation & sync scripts
│   ├── migrations/                  # Incremental database migrations
│   ├── tests/                       # Unit & integration test suite
│   ├── requirements.txt             # Python dependencies
│   └── .env.example                 # Template for environment configuration
│
├── DATABASE/database/               # MySQL Database & ETL Pipeline
│   ├── sql/mysql/                   # Schemas, views, indexes, and stored procedures
│   ├── data/                        # Verified SQLite seed data & SQL dumps (6,760 students)
│   ├── etl/                         # PyMySQL ETL data loader scripts
│   ├── docs/                        # Data dictionaries, ER diagrams & normalization specs
│   └── setup_mysql.ps1              # Automated database setup script
│
├── UI/StudentManagementApp2/        # Kotlin Jetpack Compose Android App
│   ├── app/src/main/
│   │   ├── java/com/example/studentmanagementapp/
│   │   │   ├── MainActivity.kt      # Main activity, splash screen & dashboard routing
│   │   │   ├── ApiClient.kt         # Custom HttpURLConnection client with coroutines
│   │   │   ├── AuthStorage.kt       # Android KeyStore AES-GCM encrypted session storage
│   │   │   ├── StudentStorage.kt    # Encrypted offline student cache
│   │   │   ├── StudentScreen.kt     # Student directory, creation, edit, details UI
│   │   │   ├── SearchScreen.kt      # Excel slicer multi-filter search UI
│   │   │   ├── AdmissionScreen.kt   # Admission rounds, categories, and records UI
│   │   │   ├── FeePaymentScreen.kt  # Fee payments, transactions & balance calculation
│   │   │   ├── FeeStructureScreen.kt# Master fee schedule configuration (semesters 1-8)
│   │   │   ├── ReportsScreen.kt     # Branch x Category pivot & demographic summaries
│   │   │   ├── LoginScreen.kt       # Staff authentication & OTP forgot password UI
│   │   │   └── ProfileScreen.kt     # Active session details & logout
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
└── Documentation / Fix Notes/
    ├── PROJECT_GUIDE.md             # Complete step-by-step project guide
    ├── ADMISSION_FIX_NOTES.md       # Admission module documentation
    ├── FEE_PAYMENT_FIXES.md         # Fee payment logic & validation rules
    ├── PASSWORD_RECOVERY_SETUP.md   # Staff password recovery configuration
    └── FIXES_APPLIED.md             # Change history and stability notes
```

---

## 🚀 Quick Start Guide

### 1. Database Setup (MySQL 8.0)
The database must be running MySQL 8.0 with the `excel_app` schema:
```powershell
# In MySQL Workbench or CLI:
source DATABASE/database/sql/mysql/01_create_database.sql;
source DATABASE/database/sql/mysql/02_schema.sql;
source DATABASE/database/sql/mysql/03_views.sql;
source DATABASE/database/sql/mysql/07_indexes_and_fulltext.sql;
source DATABASE/database/sql/mysql/08_stored_procedures.sql;
```
*To populate the 6,760 verified student records, import `DATABASE/database/data/excel_app_dump.sql` or run `DATABASE/database/etl/load_mysql.py`.*

> [!WARNING]
> Do **not** re-run `02_schema.sql` on an existing database with data, as it contains destructive `DROP TABLE` statements. Use the incremental migrations in `DATABASE/database/sql/mysql/` instead.

### 2. Backend Setup (FastAPI)
```powershell
cd BACKEND

# 1. Create and activate virtual environment
python -m venv .venv
.venv\Scripts\activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure environment
copy .env.example .env
# Edit .env with your MySQL port, password, and JWT secret.

# 4. Create an Admin user
python scripts/create_admin.py

# 5. Start the API server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
Interactive Swagger API documentation will be available at [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs).

### 3. Android App Setup (Android Studio)
1. Open **Android Studio**.
2. Select **Open** and choose the `UI/StudentManagementApp2` directory.
3. In `app/src/main/java/com/example/studentmanagementapp/ApiClient.kt`, set `BASE_URL`:
   - Android Emulator: `http://10.0.2.2:8000`
   - Physical Device: `http://<YOUR_PC_LAN_IP>:8000`
4. Sync Gradle and press **Run ▶️**.
5. Sign in using the staff credentials created in step 2.

---

## 🔒 Security Features
- **Argon2 Password Hashing:** Staff passwords are encrypted using state-of-the-art Argon2id.
- **Account Lockout Policy:** 5 failed login attempts lock the account for 10 minutes.
- **Android Keystore AES-GCM:** Auth tokens and offline caches are encrypted at rest using hardware-backed keys.
- **Auditing:** All mutating actions (`POST`, `PUT`, `DELETE`) are captured in the `audit_log` table.
- **Data Protection:** Backup extraction is explicitly disabled (`android:allowBackup="false"`).

---

## 📄 License
Academic project for SGSITS Indore. All rights reserved.
