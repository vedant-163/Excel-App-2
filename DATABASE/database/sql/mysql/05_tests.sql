USE excel_app;

-- Run after import. Replace expected values if the Excel file is updated.

-- Row counts
SELECT 'student' AS tbl, COUNT(*) AS n FROM student
UNION ALL SELECT 'contact', COUNT(*) FROM contact
UNION ALL SELECT 'admission', COUNT(*) FROM admission
UNION ALL SELECT 'payment', COUNT(*) FROM payment
UNION ALL SELECT 'fee_structure', COUNT(*) FROM fee_structure
UNION ALL SELECT 'fee_structure_semester', COUNT(*) FROM fee_structure_semester
UNION ALL SELECT 'student_term_fee', COUNT(*) FROM student_term_fee
UNION ALL SELECT 'academic_term', COUNT(*) FROM academic_term;

-- Excel DATA has 6760 rows; enrolment_no is unique
SELECT COUNT(*) AS students, COUNT(DISTINCT enrolment_no) AS unique_enrolments FROM student;
-- Expect: 6760 / 6760

-- Excel Unique ID is NOT unique — do not treat as PK
SELECT COUNT(*) AS filled_unique_id,
       COUNT(DISTINCT college_unique_id) AS distinct_unique_id
FROM student
WHERE college_unique_id IS NOT NULL AND college_unique_id <> '';

-- Amount totals must match Excel (pandas to_numeric coerce)
SELECT
  SUM(total_amount) AS sum_total_amount,                 -- expect 302546063
  SUM(student_submitted_amount) AS sum_submitted,        -- expect 93060900
  SUM(dte_fee) AS sum_dte_fee                            -- expect 6209000
FROM payment;

-- Term fee JUL-DEC20
SELECT SUM(f.amount) AS jul_dec20_sum                    -- expect 53237100
FROM student_term_fee f
JOIN academic_term t ON t.term_id = f.term_id
WHERE t.term_code = 'JUL-DEC20';

-- Admission year counts
SELECT admission_year, COUNT(*) n
FROM admission
GROUP BY admission_year
ORDER BY admission_year;
-- 2020-21 959, 2021-22 938, 2022-23 1148, 2023-24 1260, 2024-25 1266, 2025-26 1189

-- Orphan checks
SELECT COUNT(*) AS students_missing_contact
FROM student s LEFT JOIN contact c ON c.student_id = s.student_id
WHERE c.contact_id IS NULL;

SELECT COUNT(*) AS students_missing_admission
FROM student s LEFT JOIN admission a ON a.student_id = s.student_id
WHERE a.admission_id IS NULL;

SELECT COUNT(*) AS fee_codes_unmatched
FROM student s
LEFT JOIN fee_structure fs ON fs.fee_structure_id = s.fee_structure_id
WHERE fs.fee_structure_id IS NULL;

-- REPORT 2025-26 grand total should be 1189
SELECT COUNT(*) AS y2025
FROM admission
WHERE admission_year = '2025-26';
