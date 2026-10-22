-- NOTE: This file has been superseded by 06_load_csv.sql
-- which contains the complete LOAD DATA LOCAL INFILE for all 18 tables.
-- Use 06_load_csv.sql instead.

USE excel_app;
SET FOREIGN_KEY_CHECKS = 0;

-- Example (Windows). Change the directory:
-- LOAD DATA LOCAL INFILE 'C:/database/data/csv/branch.csv'
-- INTO TABLE branch
-- FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
-- IGNORE 1 LINES;

-- Recommended: use the Python one-step importer (SQLite verified),
-- then in MySQL Workbench: Server > Data Import, or:
--
--   pip install mysql-connector-python
--   python etl/load_mysql.py --host localhost --user root --password ...
--
-- Until MySQL is installed, the verified database is:
--   C:\database\data\excel_app.sqlite

SET FOREIGN_KEY_CHECKS = 1;
