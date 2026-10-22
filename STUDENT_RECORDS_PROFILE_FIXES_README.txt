SGSITS Student Management App - targeted fixes

Included changes:
- Edit Student searches the backend API first by Unique ID, Enrollment Number, then Name; uses the in-memory list only as fallback.
- View Student Records passes search, branch, academic year and status filters to the API.
- Domicile choices now use Yes/No values expected by the backend Y/N normalizer; existing Y/N values display as Yes/No.
- Profile content scrolls independently and Sign out stays pinned and visible at the bottom.

IMPORTANT - MANUAL BACKEND / MYSQL STEPS
1. The real .env file is intentionally NOT included because it contains local credentials. Copy BACKEND/.env.example to BACKEND/.env on your PC.
2. Fill in your actual MySQL password and a strong JWT_SECRET locally. Do not share the .env file publicly.
3. Ensure MySQL is running and database excel_app exists. This archive cannot test your local database/server.
4. From the BACKEND folder, run: python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
5. Check http://127.0.0.1:8000/docs and test GET /students while signed in.
6. Open the Android project in Android Studio, allow Gradle sync, and run Build > Make Project. The environment used to prepare this archive could not download Gradle, so a Kotlin compile could not be completed here.
7. Verify Edit Student using an exact enrollment number, a unique ID, and a partial name; verify View Records filters and sign out on an emulator/device.

Network note: for a physical Android phone, API base URL must point to the PC's LAN IP, not 127.0.0.1. For Android Emulator, use the project's configured emulator host address.
