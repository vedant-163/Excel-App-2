USE excel_app;

-- Non-destructive: preserve existing M/F rows and add support for the UI's Other option.
ALTER TABLE student MODIFY COLUMN gender ENUM('M','F','O') NOT NULL;
