# ============================================================
#  Excel App — MySQL Full Setup Script (non-interactive)
# ============================================================

$MYSQL      = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$MYSQLADMIN = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"
$DB_DIR     = $PSScriptRoot
$PYTHON     = "python"
$DBUSER     = "root"
$DBHOST     = "localhost"

# Use environment variable DB_PASS if set, otherwise prompt securely
if ($env:DB_PASS) {
    $PASS = $env:DB_PASS
} else {
    $secPass = Read-Host "Enter MySQL root password" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass)
    $PASS = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Excel App -- MySQL Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# --- Test connection ---
Write-Host ""
Write-Host "[1/6] Testing MySQL connection..." -ForegroundColor Yellow
$test = (& $MYSQLADMIN -h $DBHOST -u $DBUSER -p"$PASS" ping 2>&1) -join " "
if ($test -notmatch "alive") {
    Write-Host "ERROR: Cannot connect to MySQL. Wrong password?" -ForegroundColor Red
    Write-Host $test
    exit 1
}
Write-Host "      Connected OK." -ForegroundColor Green

# Helper: run a SQL file
function Run-SQL($step, $file) {
    Write-Host ""
    Write-Host "[$step] $file" -ForegroundColor Yellow
    & $MYSQL -h $DBHOST -u $DBUSER -p"$PASS" --local-infile=1 -e "source $file" 2>&1 | Write-Host
    if ($LASTEXITCODE -ne 0) { Write-Host "ERROR" -ForegroundColor Red; exit 1 }
    Write-Host "      Done." -ForegroundColor Green
}

# --- SQL files ---
Run-SQL "2/6 CREATE DB"  "$DB_DIR\sql\mysql\01_create_database.sql"
Run-SQL "3/6 SCHEMA"     "$DB_DIR\sql\mysql\02_schema.sql"
Run-SQL "4/6 VIEWS"      "$DB_DIR\sql\mysql\03_views.sql"
Run-SQL "5/6 INDEXES"    "$DB_DIR\sql\mysql\07_indexes_and_fulltext.sql"

# --- Python ETL: SQLite -> MySQL ---
Write-Host ""
Write-Host "[6/6] Importing data (6,760 students + 121,680 term-fee rows)..." -ForegroundColor Yellow
Write-Host "      Please wait ~1-2 minutes..." -ForegroundColor Gray
& $PYTHON "$DB_DIR\etl\load_mysql.py" --host $DBHOST --user $DBUSER --password "$PASS"
if ($LASTEXITCODE -ne 0) { Write-Host "Import ERROR" -ForegroundColor Red; exit 1 }
Write-Host "      Import complete." -ForegroundColor Green

# --- Verification tests ---
Write-Host ""
Write-Host "========================================"
Write-Host "  VERIFICATION RESULTS" -ForegroundColor Cyan
Write-Host "========================================"
& $MYSQL -h $DBHOST -u $DBUSER -p"$PASS" --table excel_app -e "
SELECT 'student'            AS tbl, COUNT(*) AS row_count FROM student
UNION ALL SELECT 'contact',          COUNT(*) FROM contact
UNION ALL SELECT 'admission',        COUNT(*) FROM admission
UNION ALL SELECT 'payment',          COUNT(*) FROM payment
UNION ALL SELECT 'student_term_fee', COUNT(*) FROM student_term_fee
UNION ALL SELECT 'fee_structure',    COUNT(*) FROM fee_structure
UNION ALL SELECT 'fee_str_semester', COUNT(*) FROM fee_structure_semester
UNION ALL SELECT 'academic_term',    COUNT(*) FROM academic_term;

SELECT 'total_amount'     AS check_name, SUM(total_amount)           AS value FROM payment
UNION ALL SELECT 'submitted_amount', SUM(student_submitted_amount)             FROM payment
UNION ALL SELECT 'dte_fee',          SUM(dte_fee)                              FROM payment;

SELECT admission_year, COUNT(*) AS students
FROM admission GROUP BY admission_year ORDER BY admission_year;

SELECT COUNT(*) AS orphan_contacts   FROM student s LEFT JOIN contact   c ON c.student_id=s.student_id WHERE c.contact_id   IS NULL;
SELECT COUNT(*) AS orphan_admissions FROM student s LEFT JOIN admission a ON a.student_id=s.student_id WHERE a.admission_id IS NULL;
" 2>&1

Write-Host ""
Write-Host "========================================" -ForegroundColor Gray
Write-Host "  EXPECTED" -ForegroundColor Gray
Write-Host "    student/contact/admission/payment : 6760 each"
Write-Host "    student_term_fee                  : 121680"
Write-Host "    fee_structure                     : 72"
Write-Host "    fee_str_semester                  : 576"
Write-Host "    academic_term                     : 18"
Write-Host "    total_amount                      : 515165638.00"
Write-Host "    submitted_amount                  : 239262772.60"
Write-Host "    dte_fee                           : 6209000.00"
Write-Host "    2020-21:959  2021-22:938  2022-23:1148"
Write-Host "    2023-24:1260 2024-25:1266 2025-26:1189"
Write-Host "    orphan_contacts / orphan_admissions : 0"
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ALL DONE! Open MySQL Workbench" -ForegroundColor Green
Write-Host "  Database : excel_app" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
