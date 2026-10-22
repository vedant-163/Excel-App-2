SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'excel_app'
  AND TABLE_NAME IN (
      'student',
      'admission',
      'admission_round',
      'admission_status',
      'payment',
      'student_term_fee',
      'fee_structure'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;