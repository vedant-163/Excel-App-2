USE excel_app;

-- SEARCH-sheet equivalent: one row per student, filter-friendly
CREATE OR REPLACE VIEW v_student_search AS
SELECT
  s.student_id, s.sr_no, s.college_unique_id, s.class_roll_no, s.enrolment_no, s.name,
  s.father_name, s.mother_name, s.rank_text, s.marks, s.exam_roll_no, s.date_of_birth,
  b.branch_code, b.branch_name, ec.category_code AS eligible_category, ac.category_code AS alloted_category,
  s.domicile, s.gender, s.ews, r.religion_code AS religion, fs.fee_mast_code AS fee_structure,
  fs.structure_year AS fee_structure_year, a.admission_year, a.admission_year_start,
  st.status_code AS admission_status, rd.round_code AS admission_round, rt.remark_code AS remarks,
  c.student_phone, c.student_email, c.parents_phone, c.parents_email, c.aadhaar_no,
  c.permanent_address, c.present_address, p.total_amount, p.student_submitted_amount,
  p.online_application_txn_id, p.payment_mode, p.payment_date, p.dte_fee, p.paid_amount, p.pending_amount
FROM student s
JOIN branch b ON b.branch_id = s.branch_id
JOIN eligible_category ec ON ec.eligible_category_id = s.eligible_category_id
JOIN alloted_category ac ON ac.alloted_category_id = s.alloted_category_id
JOIN religion r ON r.religion_id = s.religion_id
LEFT JOIN fee_structure fs ON fs.fee_structure_id = s.fee_structure_id
JOIN admission a ON a.student_id = s.student_id
JOIN admission_status st ON st.status_id = a.status_id
LEFT JOIN admission_round rd ON rd.round_id = a.round_id
LEFT JOIN remark_type rt ON rt.remark_type_id = a.remark_type_id
LEFT JOIN contact c ON c.student_id = s.student_id
LEFT JOIN (
  SELECT p1.student_id, SUM(COALESCE(p1.total_amount,0)) AS total_amount,
         SUM(COALESCE(p1.student_submitted_amount,0)) AS student_submitted_amount,
         MAX(p1.online_application_txn_id) AS online_application_txn_id,
         MAX(COALESCE(pm1.mode_code,p1.payment_mode_raw)) AS payment_mode,
         MAX(p1.payment_date) AS payment_date, SUM(COALESCE(p1.dte_fee,0)) AS dte_fee,
         SUM(COALESCE(p1.student_submitted_amount,0)) AS paid_amount,
         SUM(GREATEST(COALESCE(p1.total_amount,0)-COALESCE(p1.student_submitted_amount,0),0)) AS pending_amount
  FROM payment p1 LEFT JOIN payment_mode pm1 ON pm1.payment_mode_id=p1.payment_mode_id
  GROUP BY p1.student_id
) p ON p.student_id=s.student_id;

CREATE OR REPLACE VIEW v_student_fee_summary AS
SELECT
  student_id,
  SUM(amount) AS scheduled_fee_total,
  SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) AS scheduled_fee_nonzero
FROM student_term_fee
GROUP BY student_id;

-- REPORT 20-21 onwards replacements (parameterize year in app queries)
CREATE OR REPLACE VIEW v_report_branch_category AS
SELECT
  admission_year,
  branch_code,
  eligible_category,
  gender,
  COUNT(*) AS student_count
FROM v_student_search
GROUP BY admission_year, branch_code, eligible_category, gender;

CREATE OR REPLACE VIEW v_report_branch_ews AS
SELECT
  admission_year,
  branch_code,
  ews,
  COUNT(*) AS student_count
FROM v_student_search
GROUP BY admission_year, branch_code, ews;

CREATE OR REPLACE VIEW v_report_branch_domicile AS
SELECT
  admission_year,
  branch_code,
  domicile,
  COUNT(*) AS student_count
FROM v_student_search
GROUP BY admission_year, branch_code, domicile;

CREATE OR REPLACE VIEW v_report_branch_religion AS
SELECT
  admission_year,
  branch_code,
  religion,
  COUNT(*) AS student_count
FROM v_student_search
GROUP BY admission_year, branch_code, religion;

CREATE OR REPLACE VIEW v_dashboard_totals AS
SELECT
  COUNT(*) AS student_count,
  COUNT(DISTINCT branch_code) AS branch_count,
  SUM(total_amount) AS sum_total_amount,
  SUM(student_submitted_amount) AS sum_submitted_amount,
  SUM(dte_fee) AS sum_dte_fee,
  SUM(pending_amount) AS sum_pending_amount
FROM v_student_search;

CREATE OR REPLACE VIEW v_admission_year_summary AS
SELECT
  admission_year,
  COUNT(*) AS student_count,
  SUM(total_amount) AS sum_total_amount,
  SUM(student_submitted_amount) AS sum_submitted_amount
FROM v_student_search
GROUP BY admission_year
ORDER BY admission_year;
