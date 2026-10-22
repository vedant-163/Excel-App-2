"""Run the 10_fee_payment_context.sql migration safely."""
from app.database import SessionLocal
from sqlalchemy import text

db = SessionLocal()
try:
    # Add academic_year column if missing
    result = db.execute(text("""
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='payment' AND column_name='academic_year'
    """))
    if result.fetchone()[0] == 0:
        db.execute(text("ALTER TABLE payment ADD COLUMN academic_year VARCHAR(16) NULL AFTER student_id"))
        print("Added payment.academic_year")

    result = db.execute(text("""
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='payment' AND column_name='fee_category'
    """))
    if result.fetchone()[0] == 0:
        db.execute(text("ALTER TABLE payment ADD COLUMN fee_category VARCHAR(32) NULL AFTER academic_year"))
        print("Added payment.fee_category")

    result = db.execute(text("""
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='payment' AND column_name='semester_no'
    """))
    if result.fetchone()[0] == 0:
        db.execute(text("ALTER TABLE payment ADD COLUMN semester_no TINYINT UNSIGNED NULL AFTER fee_category"))
        print("Added payment.semester_no")

    # Add branch_code to fee_structure if missing
    result = db.execute(text("""
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='fee_structure' AND column_name='branch_code'
    """))
    if result.fetchone()[0] == 0:
        db.execute(text("ALTER TABLE fee_structure ADD COLUMN branch_code VARCHAR(64) NULL AFTER structure_year"))
        print("Added fee_structure.branch_code")

    db.commit()
    print("Fee payment migration complete OK")

    # Show payment columns
    result = db.execute(text("DESCRIBE payment"))
    print("\nPayment table columns:")
    for row in result:
        print(f"  {row[0]} {row[1]}")

except Exception as e:
    print("ERROR:", e)
    db.rollback()
finally:
    db.close()
