-- Performance indexes for Phase 8 (testing / production tuning)
-- Run AFTER 02_schema.sql and 03_views.sql
USE excel_app;

-- Full-text search on the student name + enrolment for GET /students?q=
ALTER TABLE student
  ADD FULLTEXT INDEX ft_student_name_enrolment (name, enrolment_no, class_roll_no);

-- Payment date range queries (Q09, Q10)
-- ix_pay_date is already created in 02_schema.sql; add composite for range+student
ALTER TABLE payment
  ADD INDEX ix_pay_date_student (payment_date, student_id);

-- Admission year is the most common slicer — composite with branch for pivot queries
ALTER TABLE admission
  ADD INDEX ix_adm_year_branch (admission_year, student_id);

-- Term fee range sums per term
ALTER TABLE student_term_fee
  ADD INDEX ix_stf_term_amount (term_id, amount);
