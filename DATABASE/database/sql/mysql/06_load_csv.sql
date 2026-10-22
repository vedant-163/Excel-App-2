-- Complete LOAD DATA LOCAL INFILE script for all tables.
-- Prerequisites:
--   1. Run 01_create_database.sql
--   2. Run 02_schema.sql
--   3. Start MySQL with: mysql --local-infile=1 -u root -p
--      OR set in MySQL Workbench: Edit > Preferences > SQL Editor > enable 'Allow LOAD DATA LOCAL'
--   4. Run this script.
-- CSV files are at: C:/database/data/csv/
-- Adjust path prefix if your CSV directory is different.

USE excel_app;
SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------
-- 1. Lookup / master tables (no FKs, load first)
-- -----------------------------------------------------------------------
LOAD DATA LOCAL INFILE 'C:/database/data/csv/branch.csv'
  INTO TABLE branch
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/fee_category.csv'
  INTO TABLE fee_category
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/eligible_category.csv'
  INTO TABLE eligible_category
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/alloted_category.csv'
  INTO TABLE alloted_category
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/religion.csv'
  INTO TABLE religion
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/admission_status.csv'
  INTO TABLE admission_status
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/admission_round.csv'
  INTO TABLE admission_round
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/payment_mode.csv'
  INTO TABLE payment_mode
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/remark_type.csv'
  INTO TABLE remark_type
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/academic_term.csv'
  INTO TABLE academic_term
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

-- -----------------------------------------------------------------------
-- 2. Fee structure (depends on fee_category)
-- -----------------------------------------------------------------------
LOAD DATA LOCAL INFILE 'C:/database/data/csv/fee_structure.csv'
  INTO TABLE fee_structure
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/fee_structure_semester.csv'
  INTO TABLE fee_structure_semester
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/fee_year_sem_code.csv'
  INTO TABLE fee_year_sem_code
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

-- -----------------------------------------------------------------------
-- 3. Core student data (depends on all lookups above)
-- -----------------------------------------------------------------------
LOAD DATA LOCAL INFILE 'C:/database/data/csv/student.csv'
  INTO TABLE student
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/contact.csv'
  INTO TABLE contact
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/admission.csv'
  INTO TABLE admission
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:/database/data/csv/payment.csv'
  INTO TABLE payment
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

-- -----------------------------------------------------------------------
-- 4. Large unpivoted table (loads last)
-- -----------------------------------------------------------------------
LOAD DATA LOCAL INFILE 'C:/database/data/csv/student_term_fee.csv'
  INTO TABLE student_term_fee
  FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

SET FOREIGN_KEY_CHECKS = 1;

-- Quick sanity check row counts (should match SQLite):
SELECT 'student'             , COUNT(*) FROM student
UNION ALL SELECT 'contact'           , COUNT(*) FROM contact
UNION ALL SELECT 'admission'         , COUNT(*) FROM admission
UNION ALL SELECT 'payment'           , COUNT(*) FROM payment
UNION ALL SELECT 'student_term_fee'  , COUNT(*) FROM student_term_fee
UNION ALL SELECT 'fee_structure'     , COUNT(*) FROM fee_structure
UNION ALL SELECT 'fee_structure_sem' , COUNT(*) FROM fee_structure_semester
UNION ALL SELECT 'academic_term'     , COUNT(*) FROM academic_term;
-- Expected: 6760 / 6760 / 6760 / 6760 / 121680 / 72 / 576 / 18
