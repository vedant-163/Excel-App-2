-- Normalized schema (3NF where practical) for UG Excel workbook
USE excel_app;

SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS v_student_search;
DROP VIEW IF EXISTS v_student_fee_summary;
DROP VIEW IF EXISTS v_report_branch_category;
DROP VIEW IF EXISTS v_report_branch_ews;
DROP VIEW IF EXISTS v_report_branch_domicile;
DROP VIEW IF EXISTS v_report_branch_religion;
DROP VIEW IF EXISTS v_dashboard_totals;
DROP VIEW IF EXISTS v_admission_year_summary;

DROP TABLE IF EXISTS student_term_fee;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS contact;
DROP TABLE IF EXISTS admission;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS fee_structure_semester;
DROP TABLE IF EXISTS fee_year_sem_code;
DROP TABLE IF EXISTS fee_structure;
DROP TABLE IF EXISTS academic_term;
DROP TABLE IF EXISTS alloted_category;
DROP TABLE IF EXISTS eligible_category;
DROP TABLE IF EXISTS admission_round;
DROP TABLE IF EXISTS admission_status;
DROP TABLE IF EXISTS payment_mode;
DROP TABLE IF EXISTS remark_type;
DROP TABLE IF EXISTS religion;
DROP TABLE IF EXISTS branch;
DROP TABLE IF EXISTS fee_category;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- Lookup / master tables (dropdowns, slicers)
-- ---------------------------------------------------------------------------

CREATE TABLE branch (
  branch_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  branch_code   VARCHAR(32) NOT NULL UNIQUE,
  branch_name   VARCHAR(100) NULL,
  parent_code   VARCHAR(32) NULL,
  is_fw         TINYINT(1) NOT NULL DEFAULT 0,
  department    VARCHAR(100) NULL,
  remarks       VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE fee_category (
  fee_category_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_code   VARCHAR(64) NOT NULL UNIQUE,
  category_name   VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE eligible_category (
  eligible_category_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_code        VARCHAR(16) NOT NULL UNIQUE,
  category_name        VARCHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE alloted_category (
  alloted_category_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_code       VARCHAR(64) NOT NULL UNIQUE,
  eligible_group      VARCHAR(16) NULL,
  description         VARCHAR(128) NULL
) ENGINE=InnoDB;

CREATE TABLE religion (
  religion_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  religion_code VARCHAR(32) NOT NULL UNIQUE,
  religion_name VARCHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE admission_status (
  status_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  status_code  VARCHAR(32) NOT NULL UNIQUE,
  status_name  VARCHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE admission_round (
  round_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  round_code  VARCHAR(64) NOT NULL UNIQUE,
  round_name  VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE payment_mode (
  payment_mode_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  mode_code       VARCHAR(64) NOT NULL UNIQUE,
  mode_name       VARCHAR(128) NOT NULL,
  is_grouped      TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE remark_type (
  remark_type_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  remark_code    VARCHAR(64) NOT NULL UNIQUE,
  remark_name    VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE academic_term (
  term_id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  term_code           VARCHAR(16) NOT NULL UNIQUE,  -- JUL-DEC20
  excel_column        VARCHAR(16) NOT NULL,         -- original DATA header
  calendar_year       SMALLINT NOT NULL,            -- 2020
  term_half           ENUM('JUL_DEC','JAN_JUN') NOT NULL,
  sort_order          SMALLINT NOT NULL,
  UNIQUE KEY uq_term_year_half (calendar_year, term_half)
) ENGINE=InnoDB;

CREATE TABLE fee_structure (
  fee_structure_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  fee_mast_code     VARCHAR(64) NOT NULL UNIQUE,    -- "2020 BTECH/BPH"
  fee_category_id   INT UNSIGNED NOT NULL,
  structure_year    SMALLINT NOT NULL,              -- 2020
  CONSTRAINT fk_fee_cat
    FOREIGN KEY (fee_category_id) REFERENCES fee_category(fee_category_id)
) ENGINE=InnoDB;

CREATE TABLE fee_structure_semester (
  fee_structure_semester_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  fee_structure_id          INT UNSIGNED NOT NULL,
  semester_no               TINYINT UNSIGNED NOT NULL,  -- 1..8
  amount                    DECIMAL(12,2) NOT NULL,
  UNIQUE KEY uq_fee_sem (fee_structure_id, semester_no),
  CONSTRAINT fk_fss_fs
    FOREIGN KEY (fee_structure_id) REFERENCES fee_structure(fee_structure_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- Helper codes from FEE MAST columns U-W (year & sem / DYNAMICPIVOT)
CREATE TABLE fee_year_sem_code (
  year_sem_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  structure_year  SMALLINT NOT NULL,
  sem_code        VARCHAR(32) NOT NULL,             -- BTBP1
  year_sem        VARCHAR(64) NOT NULL UNIQUE,      -- 2017 BTBP1
  semester_no     SMALLINT UNSIGNED NULL            -- up to 808 in actual data
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Core student + 1:1 satellite tables
-- ---------------------------------------------------------------------------

CREATE TABLE student (
  student_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  sr_no                INT UNSIGNED NOT NULL UNIQUE,          -- Excel Sr. No. (candidate key)
  college_unique_id    VARCHAR(32) NULL,                      -- Excel Unique ID (NOT unique, often null)
  class_roll_no        VARCHAR(32) NOT NULL,
  enrolment_no         VARCHAR(32) NOT NULL UNIQUE,           -- natural key
  name                 VARCHAR(150) NOT NULL,
  father_name          VARCHAR(150) NULL,
  mother_name          VARCHAR(150) NULL,
  rank_text            VARCHAR(32) NULL,                      -- numeric rank or CSAB etc.
  marks                DECIMAL(10,6) NULL,
  exam_roll_no         VARCHAR(32) NULL,
  date_of_birth        DATE NULL,
  branch_id            INT UNSIGNED NOT NULL,
  eligible_category_id INT UNSIGNED NOT NULL,
  alloted_category_id  INT UNSIGNED NOT NULL,
  domicile             ENUM('Y','N') NOT NULL,
  gender               ENUM('M','F') NOT NULL,
  ews                  ENUM('Y','N') NOT NULL,
  religion_id          INT UNSIGNED NOT NULL,
  fee_structure_id     INT UNSIGNED NOT NULL,
  mponline_password    VARCHAR(128) NULL,                     -- sensitive; app should encrypt
  CONSTRAINT fk_st_branch FOREIGN KEY (branch_id) REFERENCES branch(branch_id),
  CONSTRAINT fk_st_elig   FOREIGN KEY (eligible_category_id) REFERENCES eligible_category(eligible_category_id),
  CONSTRAINT fk_st_allot  FOREIGN KEY (alloted_category_id) REFERENCES alloted_category(alloted_category_id),
  CONSTRAINT fk_st_rel    FOREIGN KEY (religion_id) REFERENCES religion(religion_id),
  CONSTRAINT fk_st_fee    FOREIGN KEY (fee_structure_id) REFERENCES fee_structure(fee_structure_id),
  KEY ix_student_name (name),
  KEY ix_student_class_roll (class_roll_no),
  KEY ix_student_unique_id (college_unique_id),
  KEY ix_student_branch (branch_id),
  KEY ix_student_fee (fee_structure_id)
) ENGINE=InnoDB;

CREATE TABLE contact (
  contact_id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id              INT UNSIGNED NOT NULL UNIQUE,
  student_phone           VARCHAR(50) NULL,
  student_email           VARCHAR(150) NULL,
  parents_phone           VARCHAR(50) NULL,
  parents_email           VARCHAR(150) NULL,
  aadhaar_no              VARCHAR(32) NULL,
  aadhaar_linked_mobile   VARCHAR(32) NULL,
  permanent_address       VARCHAR(500) NULL,
  present_address         VARCHAR(500) NULL,
  CONSTRAINT fk_contact_student FOREIGN KEY (student_id) REFERENCES student(student_id)
    ON DELETE CASCADE,
  KEY ix_contact_phone (student_phone),
  KEY ix_contact_email (student_email),
  KEY ix_contact_aadhaar (aadhaar_no)
) ENGINE=InnoDB;

CREATE TABLE admission (
  admission_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id           INT UNSIGNED NOT NULL UNIQUE,
  admission_no         VARCHAR(32) NULL,
  status_id            INT UNSIGNED NOT NULL,
  round_id             INT UNSIGNED NULL,
  admission_year       VARCHAR(16) NOT NULL,          -- 2020-21
  admission_year_start SMALLINT NOT NULL,             -- 2020
  offline_category     VARCHAR(64) NULL,              -- unused in current Excel
  remark_type_id       INT UNSIGNED NULL,
  remarks_raw          VARCHAR(128) NULL,
  CONSTRAINT fk_adm_student FOREIGN KEY (student_id) REFERENCES student(student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_adm_status  FOREIGN KEY (status_id) REFERENCES admission_status(status_id),
  CONSTRAINT fk_adm_round   FOREIGN KEY (round_id) REFERENCES admission_round(round_id),
  CONSTRAINT fk_adm_remark  FOREIGN KEY (remark_type_id) REFERENCES remark_type(remark_type_id),
  KEY ix_adm_year (admission_year),
  KEY ix_adm_year_start (admission_year_start)
) ENGINE=InnoDB;

CREATE TABLE payment (
  payment_id                   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id                   INT UNSIGNED NOT NULL,
  total_amount                 DECIMAL(12,2) NULL,
  total_amount_raw             VARCHAR(128) NULL,
  student_submitted_amount     DECIMAL(12,2) NULL,
  student_submitted_amount_raw VARCHAR(128) NULL,
  online_application_txn_id    VARCHAR(255) NULL,
  payment_mode_id              INT UNSIGNED NULL,
  payment_mode_raw             VARCHAR(128) NULL,       -- preserve messy Excel text
  payment_date                 DATE NULL,
  payment_date_raw             VARCHAR(64) NULL,
  dte_fee                      DECIMAL(12,2) NULL,
  payment_status               VARCHAR(32) NULL,
  payment_reference_no         VARCHAR(64) NULL,
  CONSTRAINT fk_pay_student FOREIGN KEY (student_id) REFERENCES student(student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_pay_mode    FOREIGN KEY (payment_mode_id) REFERENCES payment_mode(payment_mode_id),
  KEY ix_pay_student (student_id),
  KEY ix_pay_date (payment_date)
) ENGINE=InnoDB;

-- Unpivoted JUL-DEC20 ... JAN-JUN29 amounts (1NF)
CREATE TABLE student_term_fee (
  student_term_fee_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id          INT UNSIGNED NOT NULL,
  term_id             INT UNSIGNED NOT NULL,
  amount              DECIMAL(12,2) NOT NULL DEFAULT 0,
  UNIQUE KEY uq_student_term (student_id, term_id),
  CONSTRAINT fk_stf_student FOREIGN KEY (student_id) REFERENCES student(student_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_stf_term    FOREIGN KEY (term_id) REFERENCES academic_term(term_id),
  KEY ix_stf_term (term_id)
) ENGINE=InnoDB;
