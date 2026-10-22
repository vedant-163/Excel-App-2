USE excel_app;

-- Application query catalog (Member 1 / API)

-- Q01 Search all students (paginated in API)
-- SELECT * FROM v_student_search ORDER BY sr_no LIMIT 50 OFFSET 0;

-- Q02 Slicer / SEARCH filters
SELECT *
FROM v_student_search
WHERE (:branch IS NULL OR branch_code = :branch)
  AND (:admission_year IS NULL OR admission_year = :admission_year)
  AND (:eligible_category IS NULL OR eligible_category = :eligible_category)
  AND (:alloted_category IS NULL OR alloted_category = :alloted_category)
  AND (:gender IS NULL OR gender = :gender)
  AND (:ews IS NULL OR ews = :ews)
  AND (:domicile IS NULL OR domicile = :domicile)
  AND (:religion IS NULL OR religion = :religion)
  AND (:remarks IS NULL OR remarks = :remarks)
  AND (
        :q IS NULL
        OR name LIKE CONCAT('%', :q, '%')
        OR enrolment_no LIKE CONCAT('%', :q, '%')
        OR class_roll_no LIKE CONCAT('%', :q, '%')
        OR college_unique_id LIKE CONCAT('%', :q, '%')
        OR student_phone LIKE CONCAT('%', :q, '%')
      )
ORDER BY sr_no;

-- Q03 Student detail
SELECT s.*, c.*, a.*, p.*, fs.fee_mast_code
FROM student s
JOIN contact c ON c.student_id = s.student_id
JOIN admission a ON a.student_id = s.student_id
LEFT JOIN payment p ON p.student_id = s.student_id
JOIN fee_structure fs ON fs.fee_structure_id = s.fee_structure_id
WHERE s.student_id = :id OR s.enrolment_no = :enrolment_no;

-- Q04 Term-wise fees for one student
SELECT t.term_code, t.calendar_year, t.term_half, f.amount
FROM student_term_fee f
JOIN academic_term t ON t.term_id = f.term_id
WHERE f.student_id = :id
ORDER BY t.sort_order;

-- Q05 Fee master
SELECT fs.fee_mast_code, fc.category_code, fs.structure_year,
       fss.semester_no, fss.amount
FROM fee_structure fs
JOIN fee_category fc ON fc.fee_category_id = fs.fee_category_id
JOIN fee_structure_semester fss ON fss.fee_structure_id = fs.fee_structure_id
ORDER BY fs.structure_year, fs.fee_mast_code, fss.semester_no;

-- Q06 Dashboard
SELECT * FROM v_dashboard_totals;
SELECT * FROM v_admission_year_summary;

-- Q07 REPORT pivot: branch x eligible category x gender for a year
SELECT
  branch_code,
  SUM(CASE WHEN eligible_category = 'OBC' AND gender = 'F' THEN student_count ELSE 0 END) AS obc_f,
  SUM(CASE WHEN eligible_category = 'OBC' AND gender = 'M' THEN student_count ELSE 0 END) AS obc_m,
  SUM(CASE WHEN eligible_category = 'OBC' THEN student_count ELSE 0 END) AS obc_total,
  SUM(CASE WHEN eligible_category = 'SC'  AND gender = 'F' THEN student_count ELSE 0 END) AS sc_f,
  SUM(CASE WHEN eligible_category = 'SC'  AND gender = 'M' THEN student_count ELSE 0 END) AS sc_m,
  SUM(CASE WHEN eligible_category = 'SC'  THEN student_count ELSE 0 END) AS sc_total,
  SUM(CASE WHEN eligible_category = 'ST'  AND gender = 'F' THEN student_count ELSE 0 END) AS st_f,
  SUM(CASE WHEN eligible_category = 'ST'  AND gender = 'M' THEN student_count ELSE 0 END) AS st_m,
  SUM(CASE WHEN eligible_category = 'ST'  THEN student_count ELSE 0 END) AS st_total,
  SUM(CASE WHEN eligible_category = 'UR'  AND gender = 'F' THEN student_count ELSE 0 END) AS ur_f,
  SUM(CASE WHEN eligible_category = 'UR'  AND gender = 'M' THEN student_count ELSE 0 END) AS ur_m,
  SUM(CASE WHEN eligible_category = 'UR'  THEN student_count ELSE 0 END) AS ur_total,
  SUM(student_count) AS grand_total
FROM v_report_branch_category
WHERE admission_year = :admission_year
GROUP BY branch_code
ORDER BY branch_code;

-- Q08 Branch x EWS
SELECT branch_code,
       SUM(CASE WHEN ews = 'N' THEN student_count ELSE 0 END) AS ews_n,
       SUM(CASE WHEN ews = 'Y' THEN student_count ELSE 0 END) AS ews_y,
       SUM(student_count) AS grand_total
FROM v_report_branch_ews
WHERE admission_year = :admission_year
GROUP BY branch_code;

-- Q09 Payments between dates
SELECT enrolment_no, name, payment_date, total_amount, student_submitted_amount, payment_mode
FROM v_student_search
WHERE payment_date BETWEEN :from_date AND :to_date
ORDER BY payment_date, name;

-- Q10 Monthly payment totals
SELECT DATE_FORMAT(payment_date, '%Y-%m') AS year_month,
       COUNT(*) AS payment_rows,
       SUM(total_amount) AS sum_total_amount,
       SUM(student_submitted_amount) AS sum_submitted
FROM payment
WHERE payment_date IS NOT NULL
GROUP BY DATE_FORMAT(payment_date, '%Y-%m')
ORDER BY year_month;

-- Q11 Lookup lists for dropdowns / slicers
SELECT branch_code FROM branch ORDER BY branch_code;
SELECT category_code FROM eligible_category ORDER BY category_code;
SELECT category_code FROM alloted_category ORDER BY category_code;
SELECT DISTINCT admission_year FROM admission ORDER BY admission_year;
SELECT religion_code FROM religion ORDER BY religion_code;
SELECT remark_code FROM remark_type ORDER BY remark_code;

-- Q12 Fee collection vs scheduled
SELECT
  s.enrolment_no,
  s.name,
  fs.fee_mast_code,
  fee.scheduled_fee_nonzero,
  p.student_submitted_amount,
  p.total_amount
FROM student s
JOIN fee_structure fs ON fs.fee_structure_id = s.fee_structure_id
LEFT JOIN v_student_fee_summary fee ON fee.student_id = s.student_id
LEFT JOIN payment p ON p.student_id = s.student_id
WHERE s.student_id = :id;
