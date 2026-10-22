-- Safe, additive migration for semester/year/category-aware fee payments.
-- Does not delete or reset existing payment data.
SET @db = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='payment' AND column_name='academic_year')=0,
  'ALTER TABLE payment ADD COLUMN academic_year VARCHAR(16) NULL AFTER student_id', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='payment' AND column_name='fee_category')=0,
  'ALTER TABLE payment ADD COLUMN fee_category VARCHAR(32) NULL AFTER academic_year', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='payment' AND column_name='semester_no')=0,
  'ALTER TABLE payment ADD COLUMN semester_no TINYINT UNSIGNED NULL AFTER fee_category', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE INDEX ix_pay_context ON payment(student_id, academic_year, fee_category, semester_no);


-- Optional branch-aware fee structures. Existing rows remain valid.
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@db AND table_name='fee_structure' AND column_name='branch_code')=0,
  'ALTER TABLE fee_structure ADD COLUMN branch_code VARCHAR(64) NULL AFTER structure_year', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
