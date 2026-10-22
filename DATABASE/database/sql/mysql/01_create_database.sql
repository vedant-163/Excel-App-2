-- Excel-to-App UG admission/fee database
-- Target: MySQL 8.0+ / MySQL Workbench
-- Source workbook: UG - Copy.xlsx (DATA, FEE MAST, SEARCH, REPORT 20-21 onwords)

CREATE DATABASE IF NOT EXISTS excel_app
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE excel_app;
