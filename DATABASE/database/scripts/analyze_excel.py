"""Phase 1: inspect UG - Copy.xlsx (focused, no 702-col SEARCH scan)."""
from __future__ import annotations

import json
import os
import zipfile
from collections import Counter, defaultdict
from datetime import date, datetime, time

import pandas as pd
from openpyxl import load_workbook

PATH = r"c:\Users\vedan\Downloads\UG - Copy.xlsx"
OUT = r"C:\database\docs"
os.makedirs(OUT, exist_ok=True)


def cell_type(v):
    if v is None or v == "":
        return "NULL"
    if isinstance(v, bool):
        return "BOOL"
    if isinstance(v, datetime):
        return "DATETIME"
    if isinstance(v, date):
        return "DATE"
    if isinstance(v, time):
        return "TIME"
    if isinstance(v, int):
        return "INT"
    if isinstance(v, float):
        return "FLOAT"
    if isinstance(v, str) and v.startswith("="):
        return "FORMULA"
    if isinstance(v, str):
        return "TEXT"
    return type(v).__name__


def dump_grid(ws, max_row=None, max_col=None):
    max_row = max_row or ws.max_row
    max_col = max_col or ws.max_column
    rows = []
    for r in range(1, max_row + 1):
        row = []
        for c in range(1, max_col + 1):
            v = ws.cell(r, c).value
            row.append(v)
        if any(x is not None and x != "" for x in row):
            rows.append({"row": r, "values": row})
    return rows


def pandas_profile(df, sheet):
    cols = []
    for i, col in enumerate(df.columns, start=1):
        s = df[col]
        nonempty = s.notna() & (s.astype(str).str.strip() != "") & (s.astype(str) != "nan")
        n_nonempty = int(nonempty.sum())
        uniq = s[nonempty].astype(str).nunique()
        samples = s[nonempty].head(8).tolist()
        vc = s[nonempty].astype(str).value_counts().head(15)
        cols.append(
            {
                "col_index": i,
                "header": str(col),
                "pandas_dtype": str(s.dtype),
                "nonempty": n_nonempty,
                "nulls": int(len(s) - n_nonempty),
                "unique_count": int(uniq),
                "is_unique_when_filled": bool(n_nonempty > 0 and uniq == n_nonempty),
                "top_values": {k: int(v) for k, v in vc.items()},
                "samples": [str(x)[:120] for x in samples],
            }
        )
    return {"sheet": sheet, "rows": int(len(df)), "cols": int(len(df.columns)), "columns": cols}


def vba_members(path):
    names = []
    try:
        with zipfile.ZipFile(path) as z:
            vba = [n for n in z.namelist() if "vba" in n.lower() or n.lower().endswith(".bin")]
            names = vba
            if "xl/vbaProject.bin" in z.namelist():
                data = z.read("xl/vbaProject.bin")
                # crude extract of ASCII identifiers
                text = "".join(chr(b) if 32 <= b < 127 else "\n" for b in data)
                tokens = [t.strip() for t in text.split("\n") if t.strip()]
                interesting = [
                    t
                    for t in tokens
                    if any(
                        k in t.lower()
                        for k in ("module", "sheet", "thisworkbook", "macro", "sub ", "function")
                    )
                    or t.endswith(".bas")
                    or t.endswith(".frm")
                    or t.endswith(".cls")
                ]
                return {"zip_entries": names, "ascii_hints": interesting[:80], "size": len(data)}
    except Exception as e:
        return {"error": str(e)}
    return {"zip_entries": names}


def main():
    print("exists", os.path.exists(PATH), "size", os.path.getsize(PATH))
    print("vba", vba_members(PATH))

    print("Loading workbook (formulas, not values)...")
    wb = load_workbook(PATH, data_only=False, keep_vba=True, read_only=False)
    print("sheets", wb.sheetnames)
    print("defined_names", list(wb.defined_names.keys()))

    meta = {
        "file": PATH,
        "sheets": wb.sheetnames,
        "has_vba": getattr(wb, "vba_archive", None) is not None,
        "defined_names": list(wb.defined_names.keys()),
        "vba": vba_members(PATH),
        "sheet_meta": [],
    }

    for ws in wb.worksheets:
        dvs = list(ws.data_validations.dataValidation) if ws.data_validations else []
        item = {
            "title": ws.title,
            "max_row": ws.max_row,
            "max_col": ws.max_column,
            "tables": list(ws.tables.keys()) if hasattr(ws, "tables") else [],
            "auto_filter": ws.auto_filter.ref if ws.auto_filter else None,
            "merged": [str(x) for x in ws.merged_cells.ranges],
            "freeze": ws.freeze_panes,
            "validations": [
                {
                    "sqref": str(dv.sqref),
                    "type": dv.type,
                    "formula1": dv.formula1,
                    "formula2": dv.formula2,
                }
                for dv in dvs
            ],
        }
        print("META", ws.title, item["max_row"], item["max_col"], "tables", item["tables"], "filter", item["auto_filter"], "merged", len(item["merged"]), "dv", len(dvs))
        meta["sheet_meta"].append(item)

    # REPORT: dump whole used grid
    ws_r = wb["REPORT 20-21 onwords"]
    report_grid = dump_grid(ws_r, max_row=min(ws_r.max_row, 40), max_col=min(ws_r.max_column, 40))
    with open(os.path.join(OUT, "report_grid.json"), "w", encoding="utf-8") as f:
        json.dump(report_grid, f, indent=2, default=str)
    print("Wrote report_grid.json", len(report_grid), "rows")

    # SEARCH: find last used col/row in first 80 columns / 80 rows, plus formulas
    ws_s = wb["SEARCH"]
    print("Scanning SEARCH used cells (first 80 cols, 120 rows + formula hunt)...")
    search_used = []
    formulas = []
    last_c, last_r = 0, 0
    for r in range(1, 121):
        for c in range(1, 81):
            v = ws_s.cell(r, c).value
            if v is None or v == "":
                continue
            last_c = max(last_c, c)
            last_r = max(last_r, r)
            search_used.append({"r": r, "c": c, "v": v if not (isinstance(v, str) and len(v) > 200) else v[:200]})
            if isinstance(v, str) and v.startswith("="):
                formulas.append({"r": r, "c": c, "f": v[:500]})
    # also check a few known far columns? skip
    with open(os.path.join(OUT, "search_layout.json"), "w", encoding="utf-8") as f:
        json.dump({"last_r": last_r, "last_c": last_c, "cells": search_used, "formulas": formulas}, f, indent=2, default=str)
    print("SEARCH used in window", last_r, last_c, "cells", len(search_used), "formulas", len(formulas))

    # DATA formulas in first 3 rows of data
    ws_d = wb["DATA"]
    data_headers = [ws_d.cell(1, c).value for c in range(1, ws_d.max_column + 1)]
    data_formula_map = {}
    for r in range(2, 6):
        for c in range(1, ws_d.max_column + 1):
            v = ws_d.cell(r, c).value
            if isinstance(v, str) and v.startswith("="):
                data_formula_map.setdefault(data_headers[c - 1], v)
    print("DATA headers", data_headers)
    print("DATA formula samples", data_formula_map)

    ws_f = wb["FEE MAST"]
    fee_headers = [ws_f.cell(1, c).value for c in range(1, ws_f.max_column + 1)]
    fee_formula_map = {}
    for r in range(2, 6):
        for c in range(1, ws_f.max_column + 1):
            v = ws_f.cell(r, c).value
            if isinstance(v, str) and v.startswith("="):
                fee_formula_map.setdefault(fee_headers[c - 1], v)
    print("FEE headers", fee_headers)
    print("FEE formula samples", fee_formula_map)

    # DATA validations already in meta
    with open(os.path.join(OUT, "workbook_meta.json"), "w", encoding="utf-8") as f:
        json.dump(
            {
                **meta,
                "data_headers": data_headers,
                "data_formula_samples": data_formula_map,
                "fee_headers": fee_headers,
                "fee_formula_samples": fee_formula_map,
            },
            f,
            indent=2,
            default=str,
        )

    wb.close()

    print("Pandas DATA...")
    data = pd.read_excel(PATH, sheet_name="DATA")
    print("DATA shape", data.shape)
    print("DATA columns", list(data.columns))
    data_prof = pandas_profile(data, "DATA")
    with open(os.path.join(OUT, "data_profile.json"), "w", encoding="utf-8") as f:
        json.dump(data_prof, f, indent=2, default=str)

    print("Pandas FEE MAST...")
    fee = pd.read_excel(PATH, sheet_name="FEE MAST")
    print("FEE shape", fee.shape)
    print("FEE columns", list(fee.columns))
    fee_prof = pandas_profile(fee, "FEE MAST")
    with open(os.path.join(OUT, "fee_profile.json"), "w", encoding="utf-8") as f:
        json.dump(fee_prof, f, indent=2, default=str)

    # relationship probes
    probes = {}
    if "Unique ID" in data.columns:
        probes["unique_id_unique"] = int(data["Unique ID"].nunique(dropna=True))
        probes["unique_id_nonempty"] = int(data["Unique ID"].notna().sum())
    # try common names
    for col in data.columns:
        probes.setdefault("all_unique_flags", {})[str(col)] = bool(
            data[col].notna().sum() > 0 and data[col].nunique(dropna=True) == data[col].notna().sum()
        )

    # FEE vs DATA fee structure
    fee_name_col = None
    for c in fee.columns:
        if str(c).strip().lower() in ("fee mast", "fee_mast", "fee structure", "fee_structure"):
            fee_name_col = c
    probes["fee_name_col"] = str(fee_name_col)
    if fee_name_col is not None and "Fee Structure" in data.columns:
        data_fs = set(data["Fee Structure"].dropna().astype(str).str.strip())
        fee_fs = set(fee[fee_name_col].dropna().astype(str).str.strip())
        probes["data_fee_structures"] = sorted(data_fs)[:50]
        probes["fee_mast_keys"] = sorted(fee_fs)[:50]
        probes["fee_in_data_not_in_mast"] = sorted(data_fs - fee_fs)[:50]
        probes["fee_in_mast_not_in_data"] = sorted(fee_fs - data_fs)[:50]

    with open(os.path.join(OUT, "relationship_probes.json"), "w", encoding="utf-8") as f:
        json.dump(probes, f, indent=2, default=str)
    print("Done probes")


if __name__ == "__main__":
    main()
