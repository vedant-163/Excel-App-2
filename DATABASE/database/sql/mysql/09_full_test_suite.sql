USE excel_app;
-- =============================================================
--  FULL TEST SUITE  —  Run to validate the complete database
--  Expected values come from the verified SQLite import.
-- =============================================================

-- -------------------------------------------------------------------
-- SECTION A: Row counts
-- -------------------------------------------------------------------
SELECT '=== SECTION A: ROW COUNTS ===' AS '';

SELECT 'student'            AS tbl, COUNT(*) AS row_count, 6760  AS expected, IF(COUNT(*)=6760,'✓ PASS','✗ FAIL') AS result FROM student
UNION ALL SELECT 'contact',          COUNT(*), 6760, IF(COUNT(*)=6760,'✓ PASS','✗ FAIL')   FROM contact
UNION ALL SELECT 'admission',        COUNT(*), 6760, IF(COUNT(*)=6760,'✓ PASS','✗ FAIL')   FROM admission
UNION ALL SELECT 'payment',          COUNT(*), 6760, IF(COUNT(*)=6760,'✓ PASS','✗ FAIL')   FROM payment
UNION ALL SELECT 'student_term_fee', COUNT(*), 121680, IF(COUNT(*)=121680,'✓ PASS','✗ FAIL') FROM student_term_fee
UNION ALL SELECT 'fee_structure',    COUNT(*), 72, IF(COUNT(*)=72,'✓ PASS','✗ FAIL')       FROM fee_structure
UNION ALL SELECT 'fee_str_semester', COUNT(*), 576, IF(COUNT(*)=576,'✓ PASS','✗ FAIL')     FROM fee_structure_semester
UNION ALL SELECT 'academic_term',    COUNT(*), 18, IF(COUNT(*)=18,'✓ PASS','✗ FAIL')       FROM academic_term
UNION ALL SELECT 'branch',           COUNT(*), 22, IF(COUNT(*)=22,'✓ PASS','✗ FAIL')       FROM branch
UNION ALL SELECT 'eligible_category',COUNT(*), 4,  IF(COUNT(*)=4,'✓ PASS','✗ FAIL')        FROM eligible_category
UNION ALL SELECT 'alloted_category', COUNT(*), 41, IF(COUNT(*)=41,'✓ PASS','✗ FAIL')       FROM alloted_category
UNION ALL SELECT 'religion',         COUNT(*), 6,  IF(COUNT(*)=6,'✓ PASS','✗ FAIL')        FROM religion
UNION ALL SELECT 'payment_mode',     COUNT(*), 41, IF(COUNT(*)=41,'✓ PASS','✗ FAIL')       FROM payment_mode
UNION ALL SELECT 'remark_type',      COUNT(*), 11, IF(COUNT(*)=11,'✓ PASS','✗ FAIL')       FROM remark_type
UNION ALL SELECT 'fee_year_sem_code',COUNT(*), 568, IF(COUNT(*)=568,'✓ PASS','✗ FAIL')     FROM fee_year_sem_code;

-- -------------------------------------------------------------------
-- SECTION B: Financial totals (must match Excel parsed values)
-- -------------------------------------------------------------------
SELECT '=== SECTION B: FINANCIAL TOTALS ===' AS '';

SELECT
    ROUND(SUM(total_amount),2)             AS sum_total_amount,
    515165638.00                           AS expected_total,
    IF(ROUND(SUM(total_amount),2)=515165638.00,'✓ PASS','✗ FAIL') AS total_result,
    ROUND(SUM(student_submitted_amount),2) AS sum_submitted,
    239262772.60                           AS expected_submitted,
    IF(ROUND(SUM(student_submitted_amount),2)=239262772.60,'✓ PASS','✗ FAIL') AS submitted_result,
    ROUND(SUM(dte_fee),2)                  AS sum_dte_fee,
    6209000.00                             AS expected_dte,
    IF(ROUND(SUM(dte_fee),2)=6209000.00,'✓ PASS','✗ FAIL') AS dte_result
FROM payment;

-- -------------------------------------------------------------------
-- SECTION C: Admission year breakdown (must match REPORT sheet)
-- -------------------------------------------------------------------
SELECT '=== SECTION C: ADMISSION YEAR COUNTS ===' AS '';

SELECT admission_year, COUNT(*) AS students,
    CASE admission_year
        WHEN '2020-21' THEN IF(COUNT(*)=959,'✓ PASS','✗ FAIL')
        WHEN '2021-22' THEN IF(COUNT(*)=938,'✓ PASS','✗ FAIL')
        WHEN '2022-23' THEN IF(COUNT(*)=1148,'✓ PASS','✗ FAIL')
        WHEN '2023-24' THEN IF(COUNT(*)=1260,'✓ PASS','✗ FAIL')
        WHEN '2024-25' THEN IF(COUNT(*)=1266,'✓ PASS','✗ FAIL')
        WHEN '2025-26' THEN IF(COUNT(*)=1189,'✓ PASS','✗ FAIL')
        ELSE '? UNKNOWN'
    END AS result
FROM admission
GROUP BY admission_year
ORDER BY admission_year;

-- -------------------------------------------------------------------
-- SECTION D: JUL-DEC20 term fee total
-- -------------------------------------------------------------------
SELECT '=== SECTION D: TERM FEE TOTAL ===' AS '';

SELECT
    SUM(f.amount) AS jul_dec20_sum,
    53237100      AS expected,
    IF(SUM(f.amount)=53237100,'✓ PASS','✗ FAIL') AS result
FROM student_term_fee f
JOIN academic_term t ON t.term_id = f.term_id
WHERE t.term_code = 'JUL-DEC20';

-- -------------------------------------------------------------------
-- SECTION E: Unique key integrity
-- -------------------------------------------------------------------
SELECT '=== SECTION E: UNIQUE KEY INTEGRITY ===' AS '';

SELECT COUNT(*) AS total_students,
       COUNT(DISTINCT enrolment_no) AS unique_enrolments,
       IF(COUNT(*)=COUNT(DISTINCT enrolment_no),'✓ PASS','✗ FAIL') AS result
FROM student;

-- -------------------------------------------------------------------
-- SECTION F: Orphan record checks
-- -------------------------------------------------------------------
SELECT '=== SECTION F: ORPHAN CHECKS ===' AS '';

SELECT COUNT(*) AS orphan_contacts,
       IF(COUNT(*)=0,'✓ PASS','✗ FAIL') AS result
FROM student s LEFT JOIN contact c ON c.student_id=s.student_id
WHERE c.contact_id IS NULL;

SELECT COUNT(*) AS orphan_admissions,
       IF(COUNT(*)=0,'✓ PASS','✗ FAIL') AS result
FROM student s LEFT JOIN admission a ON a.student_id=s.student_id
WHERE a.admission_id IS NULL;

SELECT COUNT(*) AS unmatched_fee_structure,
       IF(COUNT(*)=0,'✓ PASS','✗ FAIL') AS result
FROM student s LEFT JOIN fee_structure fs ON fs.fee_structure_id=s.fee_structure_id
WHERE fs.fee_structure_id IS NULL;

-- -------------------------------------------------------------------
-- SECTION G: View functional tests
-- -------------------------------------------------------------------
SELECT '=== SECTION G: VIEWS ===' AS '';

SELECT IF(COUNT(*)=6760,'✓ PASS','✗ FAIL') AS v_student_search_rows FROM v_student_search;
SELECT IF(student_count=6760,'✓ PASS','✗ FAIL') AS v_dashboard_student_count FROM v_dashboard_totals;
SELECT COUNT(*) AS v_report_branch_cat_rows FROM v_report_branch_category;
SELECT COUNT(*) AS v_admission_year_rows FROM v_admission_year_summary;

-- -------------------------------------------------------------------
-- SECTION H: Stored procedures smoke tests
-- -------------------------------------------------------------------
SELECT '=== SECTION H: STORED PROCEDURES ===' AS '';

-- Search: find a known student
CALL sp_search_students(NULL,'CSE','2025-26',NULL,NULL,NULL,NULL,NULL,NULL,NULL,5,0);

-- Student detail: first student
CALL sp_get_student(1, NULL);

-- Student fees: first student (expect 18 terms)
CALL sp_get_student_fees(1);

-- Report pivot for 2025-26
CALL sp_report_branch_category('2025-26');

-- Dashboard
CALL sp_dashboard();

SELECT '=== ALL TESTS COMPLETE ===' AS '';
