"""Optional loader: SQLite (already imported) -> MySQL.

pip install mysql-connector-python
python etl/load_mysql.py --host localhost --user root --password YOURPASS
"""
from __future__ import annotations

import argparse
import os
import sqlite3

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
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="localhost")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--database", default="excel_app")
    args = ap.parse_args()

    try:
        import mysql.connector
    except ImportError as e:
        raise SystemExit("Install mysql-connector-python first") from e

    if not os.path.exists(DB_PATH):
        raise SystemExit("Run python etl/import_to_sqlite.py first")

    sqlite = sqlite3.connect(DB_PATH)
    sqlite.row_factory = sqlite3.Row
    mysql_conn = mysql.connector.connect(
        host=args.host, user=args.user, password=args.password, database=args.database, allow_local_infile=True
    )
    cur = mysql_conn.cursor()
    cur.execute("SET FOREIGN_KEY_CHECKS=0")
    for t in reversed(TABLES):
        cur.execute(f"DELETE FROM {t}")
    for t in TABLES:
        rows = sqlite.execute(f"SELECT * FROM {t}").fetchall()
        if not rows:
            print(t, 0)
            continue
        cols = rows[0].keys()
        placeholders = ",".join(["%s"] * len(cols))
        col_sql = ",".join(f"`{c}`" for c in cols)
        sql = f"INSERT INTO {t} ({col_sql}) VALUES ({placeholders})"
        data = [tuple(r[c] for c in cols) for r in rows]
        cur.executemany(sql, data)
        print(t, len(data))
    cur.execute("SET FOREIGN_KEY_CHECKS=1")
    mysql_conn.commit()
    mysql_conn.close()
    sqlite.close()
    print("MySQL load complete")


if __name__ == "__main__":
    main()
