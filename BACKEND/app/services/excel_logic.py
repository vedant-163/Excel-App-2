"""Excel workbook formulas / dirty-cell parsing recreated in Python."""

from __future__ import annotations

import re
from datetime import date, datetime
from decimal import Decimal, ROUND_HALF_UP, InvalidOperation

ZERO = Decimal("0.00")


def money(value: Decimal | int | float | str | None) -> Decimal:
    if value is None or value == "":
        return ZERO
    if isinstance(value, Decimal):
        return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    try:
        return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except (InvalidOperation, ValueError):
        return parse_amount(str(value))


def parse_amount(raw: str | int | float | Decimal | None) -> Decimal:
    """Parse Excel fee cells including `51400+14000`, `1,21,600`, and newlines."""
    if raw is None or raw == "":
        return ZERO
    if isinstance(raw, (int, float, Decimal)):
        return money(raw)

    text = str(raw).replace("\n", " ").replace("\r", " ").strip()
    if not text:
        return ZERO

    text = text.replace(",", "")
    numbers = re.findall(r"-?\d+(?:\.\d+)?", text)
    if not numbers:
        return ZERO
    total = sum(Decimal(n) for n in numbers)
    return money(total)


def pending_amount(
    total: Decimal | None,
    submitted: Decimal | None,
) -> Decimal:
    pending = money(total) - money(submitted)
    return pending if pending > ZERO else ZERO


def normalize_gender(value: str | None) -> str | None:
    if value is None or str(value).strip() == "":
        return None
    token = str(value).strip().upper()
    mapping = {
        "M": "M",
        "MALE": "M",
        "BOY": "M",
        "F": "F",
        "FEMALE": "F",
        "GIRL": "F",
        "O": "O",
        "OTHER": "O",
    }
    if token not in mapping:
        raise ValueError("gender must be M/F/O (Male/Female/Other)")
    return mapping[token]


def normalize_yn(value: str | None, field: str) -> str | None:
    if value is None or str(value).strip() == "":
        return None
    token = str(value).strip().upper()
    mapping = {
        "Y": "Y",
        "YES": "Y",
        "TRUE": "Y",
        "1": "Y",
        "N": "N",
        "NO": "N",
        "FALSE": "N",
        "0": "N",
    }
    # The Android form also uses descriptive domicile values.
    # Treat Madhya Pradesh as the home-state domicile flag and Other State as non-domicile.
    if field.lower() == "domicile":
        if token in {"MADHYA PRADESH", "MP", "M.P."}:
            return "Y"
        if token in {"OTHER STATE", "OUTSIDE MADHYA PRADESH"}:
            return "N"
    if token not in mapping:
        raise ValueError(f"{field} must be Y/N (or Yes/No)")
    return mapping[token]


def normalize_admission_year(value: str | None) -> str | None:
    """Store academic years like the Excel sheet: 2020-21."""
    if value is None or str(value).strip() == "":
        return None
    text = str(value).strip()
    match = re.fullmatch(r"(\d{4})\s*[-/]\s*(\d{2,4})", text)
    if match:
        start = int(match.group(1))
        end_raw = match.group(2)
        end = int(end_raw) if len(end_raw) == 4 else 2000 + int(end_raw)
        return f"{start}-{str(end)[-2:]}"
    if re.fullmatch(r"\d{4}", text):
        start = int(text)
        return f"{start}-{str(start + 1)[-2:]}"
    return text


def admission_year_start(value: str | None) -> int | None:
    normalized = normalize_admission_year(value)
    if not normalized:
        return None
    match = re.match(r"(\d{4})", normalized)
    return int(match.group(1)) if match else None


def parse_date(value: str | date | None) -> date | None:
    if value is None or value == "":
        return None
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    text = str(value).strip()
    for fmt in ("%Y-%m-%d", "%d-%m-%Y", "%d/%m/%Y", "%Y/%m/%d", "%d-%b-%Y"):
        try:
            return datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    raise ValueError("dates must use YYYY-MM-DD")


def to_float(value: Decimal | None) -> float | None:
    if value is None:
        return None
    return float(money(value))


BRANCH_ALIASES = {
    "computer science and engineering": "CSE",
    "computer science": "CSE",
    "computer science & engineering": "CSE",
    "information technology": "IT",
    "electronics and telecommunication engineering": "EC",
    "electronics & telecommunication engineering": "EC",
    "electronics and communication": "EC",
    "electrical engineering": "EE",
    "electrical": "EE",
    "electronics and instrumentation engineering": "EI",
    "electronics & instrumentation engineering": "EI",
    "mechanical engineering": "ME",
    "mechanical": "ME",
    "mech": "ME",
    "civil engineering": "CE",
    "civil": "CE",
    "industrial and production engineering": "IPE",
    "industrial & production engineering": "IPE",
    "biomedical engineering": "BME",
    "biomedical": "BME",
    "bachelor of pharmacy": "B.PHARMACY",
    "b.pharmacy": "B.PHARMACY",
    "b pharmacy": "B.PHARMACY",
    "pharmacy": "B.PHARMACY",
    "artificial intelligence and robotics": "AIR",
    "mech": "ME",
}


def canonical_branch_code(value: str | None) -> str | None:
    if value is None or str(value).strip() == "":
        return None
    raw = str(value).strip()
    alias = BRANCH_ALIASES.get(raw.lower())
    return (alias or raw).upper() if alias else raw
