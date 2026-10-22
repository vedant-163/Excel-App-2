#!/usr/bin/env python3
"""
sync_excel.py – Convenience runner for Excel ↔ Database synchronization.

Run from the BACKEND directory:
    python scripts/sync_excel.py --excel /path/to/SGSITS_Data.xlsx
    python scripts/sync_excel.py --excel /path/to/SGSITS_Data.xlsx --mode report
    python scripts/sync_excel.py --excel /path/to/SGSITS_Data.xlsx --dry-run
"""

import sys
import os

# Add the BACKEND directory to path so app.services.excel_sync can be imported
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Load environment variables from .env if python-dotenv is available
try:
    from dotenv import load_dotenv
    env_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
    if os.path.exists(env_path):
        load_dotenv(env_path)
        print(f"Loaded environment from {env_path}")
except ImportError:
    pass

from app.services.excel_sync import main

if __name__ == "__main__":
    main()
