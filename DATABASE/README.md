# Excel-to-App Project

Undergraduate Admission & Fee Management System migrated from Excel to a full-stack mobile application.

---

## 👥 Repository & Team Structure

This repository is organized to allow all team members to collaborate cleanly:

```text
Excel-App/
│
├── database/     # Database Member: 3NF MySQL Schema, Data, Views, Stored Procs, Docs
├── backend/      # Member 2: REST API Server (Python / Node.js / Java) connecting to MySQL
└── frontend/     # Member 1: Mobile / Android Application UI (Kotlin / Java)
```

---

### 🗄️ 1. Database (`/database`) — Complete ✅
* **Database Name:** `excel_app` (MySQL 8.0)
* **Tables:** 13 normalized 3NF tables (unpivoted repeating semester fee columns).
* **Data Migrated:** All 6,760 students and 121,680 semester fee records verified (100% financial match with Excel).
* **Stored Procedures:** 6 procedures created for all app endpoints (`CALL sp_search_students`, `CALL sp_dashboard`, etc.).
* **Detailed Guide:** See [database/README.md](database/README.md) for architecture, table breakdown, and setup.

---

### ⚙️ 2. Backend (`/backend`) — Member 2
* **Role:** Builds the REST API service bridging Frontend and MySQL.
* **Specification:** Implements endpoints defined in [database/docs/05_api_requirements.md](database/docs/05_api_requirements.md).
* **Database Interface:** Calls stored procedures in [database/sql/mysql/08_stored_procedures.sql](database/sql/mysql/08_stored_procedures.sql).

---

### 📱 3. Frontend (`/frontend`) — Member 1
* **Role:** Android Application UI (Kotlin/Java) using Retrofit/OkHttp.
* **Screens:** Login, Dashboard cards, Student Search with slicer filters, Student Profile, and Semester Fee Breakdown.
* **Communication:** Connects to Backend REST API (never connects directly to MySQL).
