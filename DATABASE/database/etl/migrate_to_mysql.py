"""Load data from excel_app.sqlite to MySQL using PyMySQL."""
import os
import sqlite3
import pymysql

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DB_PATH = os.path.join(ROOT, "data", "excel_app.sqlite")

TABLES = [
    "branch",
    "fee_category",
    "eligible_category",
    "alloted_category",
    "religion",
    "admission_status",
    "admission_round",
    "payment_mode",
    "remark_type",
    "academic_term",
    "fee_structure",
    "fee_structure_semester",
    "fee_year_sem_code",
    "student",
    "contact",
    "admission",
    "payment",
    "student_term_fee",
]

def main():
    if not os.path.exists(DB_PATH):
        print(f"Error: {DB_PATH} not found")
        return

    print(f"Connecting to SQLite: {DB_PATH}")
    sqlite = sqlite3.connect(DB_PATH)
    sqlite.row_factory = sqlite3.Row

    print("Connecting to MySQL on port 3307...")
    mysql_conn = pymysql.connect(
        host="127.0.0.1",
        port=3307,
        user="root",
        password="Sagar@7987",
        database="excel_app",
        autocommit=False,
    )
    
    with mysql_conn.cursor() as cur:
        cur.execute("SET FOREIGN_KEY_CHECKS=0;")
        for t in reversed(TABLES):
            cur.execute(f"DELETE FROM `{t}`;")
        
        for t in TABLES:
            rows = sqlite.execute(f"SELECT * FROM `{t}`;").fetchall()
            if not rows:
                print(f"Table {t}: 0 rows")
                continue
            cols = list(rows[0].keys())
            placeholders = ",".join(["%s"] * len(cols))
            col_sql = ",".join(f"`{c}`" for c in cols)
            sql = f"INSERT INTO `{t}` ({col_sql}) VALUES ({placeholders});"
            data = [tuple(r[c] for c in cols) for r in rows]
            cur.executemany(sql, data)
            print(f"Loaded {t}: {len(data)} rows")

        cur.execute("SET FOREIGN_KEY_CHECKS=1;")
        mysql_conn.commit()
    
    mysql_conn.close()
    sqlite.close()
    print("ALL DATA LOADED INTO MYSQL SUCCESSFULLY!")

if __name__ == "__main__":
    main()
