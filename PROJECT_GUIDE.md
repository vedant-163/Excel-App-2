# SGSITS Student Management System — Complete Project Guide

## 📌 Project Overview
This project converts an extensive Excel workbook (`UG.xlsm`) into a multi-tier client-server application:
- **Frontend (Member 1):** Jetpack Compose Android App (`UI/StudentManagementApp2`)
- **Backend (Member 2):** FastAPI Python REST API (`BACKEND/`)
- **Database (Member 3):** MySQL Relational Database (`DATABASE/`)

---

## 📦 Zip Archive & Folder Location

1. **Original Project Folder:**
   `C:\Users\sagar7987\Downloads\APP`

2. **Clean Complete ZIP File (Ready to Share/Submit):**
   `C:\Users\sagar7987\Downloads\StudentManagementApp_Complete_Project.zip` (Size: ~7.75 MB)

*(Temporary build caches like `.venv`, `.gradle`, and `build` folders have been excluded to keep the ZIP lightweight and clean.)*

---

## 🗂️ Project Directory Structure

```text
StudentManagementApp_Complete_Project/
│
├── BACKEND/                         # Python FastAPI Backend
│   ├── app/
│   │   ├── main.py                  # API Entry point & CORS middleware
│   │   ├── config.py                # Database configuration & URL encoding
│   │   ├── database.py              # SQLAlchemy engine & session pool
│   │   ├── models.py                # ORM models (student, admission, contact, payment, etc.)
│   │   ├── schemas.py               # Pydantic validation (supports both snake_case & camelCase)
│   │   ├── auth.py                  # JWT authentication module
│   │   ├── exceptions.py            # Global error handling & friendly messages
│   │   ├── routers/
│   │   │   ├── students.py          # Student CRUD & Excel slicer search API
│   │   │   ├── payments.py          # Payment tracking & monthly aggregation
│   │   │   ├── fee_structures.py    # Fee Structure management (FEE MAST sheet)
│   │   │   ├── branches.py          # Branch catalog endpoints
│   │   │   ├── reports.py           # Branch x Category pivot reports (REPORT 20-21 sheet)
│   │   │   ├── dashboard.py         # Global metrics (students, fees, admission year trends)
│   │   │   ├── lookups.py           # Dropdown data for Android spinners
│   │   │   └── auth.py              # Login endpoint
│   │   └── services/
│   │       ├── excel_logic.py       # Excel formulas & dirty cell parsing recreated
│   │       ├── student_service.py   # Student business logic & query builder
│   │       ├── payment_service.py   # Fee validation & pending amount calculations
│   │       └── lookups.py           # Foreign key resolver helpers
│   ├── tests/
│   │   └── test_api.py              # 38 Automated tests (All Passing)
│   ├── .env                         # Configured environment variables (Port 3307, DB credentials)
│   ├── .env.example
│   ├── requirements.txt
│   └── README.md
│
├── DATABASE/                        # MySQL Database & ETL Pipeline
│   ├── database/
│   │   ├── sql/mysql/
│   │   │   ├── 01_create_database.sql
│   │   │   ├── 02_schema.sql        # 18 Normalized tables
│   │   │   ├── 03_views.sql         # Search and summary views
│   │   │   ├── 07_indexes_and_fulltext.sql
│   │   │   └── 08_stored_procedures.sql
│   │   ├── data/
│   │   │   ├── excel_app.sqlite     # 6,760 students SQLite source
│   │   │   └── excel_app_dump.sql
│   │   ├── etl/
│   │   │   ├── migrate_to_mysql.py  # Fast PyMySQL data loader
│   │   │   └── load_mysql.py
│   │   └── setup_mysql.ps1          # Automated database setup script
│
└── UI/                              # Kotlin Android App
    └── StudentManagementApp2/
        ├── app/
        │   ├── src/main/
        │   │   ├── java/com/example/studentmanagementapp/
        │   │   │   ├── MainActivity.kt        # Jetpack Compose navigation & backend sync
        │   │   │   ├── ApiClient.kt           # HTTP client connecting to FastAPI backend
        │   │   │   ├── StudentStorage.kt      # Local cache + remote sync coordinator
        │   │   │   ├── Student.kt             # Student data model
        │   │   │   ├── StudentScreen.kt       # Student list & details UI
        │   │   │   ├── SearchScreen.kt        # Slicer filtering UI
        │   │   │   ├── AdmissionScreen.kt     # Admission status & round UI
        │   │   │   ├── FeePaymentScreen.kt    # Fee payment recording UI
        │   │   │   ├── FeeStructureScreen.kt  # Fee structure configuration UI
        │   │   │   └── ReportsScreen.kt       # Pivot report summary UI
        │   │   └── AndroidManifest.xml        # Includes INTERNET & Cleartext permissions
        ├── build.gradle.kts
        └── settings.gradle.kts
```

---

## 🚀 How to Run the Project

### Step 1: Start the Backend (if not already running)
```powershell
cd C:\Users\sagar7987\Downloads\APP\BACKEND
.venv\Scripts\activate
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- Interactive API Docs: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)

### Step 2: Run the Android App
1. Open **Android Studio**.
2. Click **Open** and select `C:\Users\sagar7987\Downloads\APP\UI\StudentManagementApp2`.
3. Choose an **Android Emulator** or a connected **Physical Device**.
4. Press **Run ▶️**.
5. The app will launch and load live students from the backend into the UI.
