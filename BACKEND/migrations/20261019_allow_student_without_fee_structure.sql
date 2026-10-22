-- Run this manually in MySQL Workbench against excel_app before testing Add Student.
-- Allows student records to be created even when a matching fee structure has not yet been configured.
USE excel_app;
ALTER TABLE student MODIFY COLUMN fee_structure_id INT NULL;
