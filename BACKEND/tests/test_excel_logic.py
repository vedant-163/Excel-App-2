"""Unit tests for Excel formula/dirty-cell helpers. No database required."""

from datetime import date
from decimal import Decimal

import pytest

from app.services.excel_logic import (
    admission_year_start,
    canonical_branch_code,
    normalize_admission_year,
    normalize_gender,
    normalize_yn,
    parse_amount,
    parse_date,
    pending_amount,
)


class TestParseAmount:
    @pytest.mark.parametrize(
        "raw, expected",
        [
            ("51400+14000", "65400.00"),
            ("1,21,600", "121600.00"),
            ("1000\n500", "1500.00"),
            (25000, "25000.00"),
            (1234.5, "1234.50"),
            ("", "0.00"),
            (None, "0.00"),
            ("n/a", "0.00"),
        ],
    )
    def test_parse(self, raw, expected):
        assert parse_amount(raw) == Decimal(expected)


class TestPending:
    def test_pending(self):
        assert pending_amount(Decimal("1000"), Decimal("400")) == Decimal("600.00")

    def test_never_negative(self):
        assert pending_amount(Decimal("100"), Decimal("500")) == Decimal("0.00")

    def test_none_values(self):
        assert pending_amount(None, None) == Decimal("0.00")


class TestGender:
    @pytest.mark.parametrize(
        "raw, expected",
        [("M", "M"), ("male", "M"), ("Boy", "M"), ("f", "F"), ("Girl", "F"),
         ("Other", "O"), (None, None), ("  ", None)],
    )
    def test_valid(self, raw, expected):
        assert normalize_gender(raw) == expected

    def test_invalid(self):
        with pytest.raises(ValueError):
            normalize_gender("x")


class TestYesNo:
    @pytest.mark.parametrize(
        "raw, expected",
        [("yes", "Y"), ("Y", "Y"), ("1", "Y"), ("no", "N"), ("0", "N"), (None, None)],
    )
    def test_valid(self, raw, expected):
        assert normalize_yn(raw, "ews") == expected

    def test_invalid(self):
        with pytest.raises(ValueError, match="ews"):
            normalize_yn("maybe", "ews")


class TestAdmissionYear:
    @pytest.mark.parametrize(
        "raw, expected",
        [("2020-21", "2020-21"), ("2020/2021", "2020-21"), ("2020", "2020-21"),
         ("2020 - 21", "2020-21"), (None, None), ("", None)],
    )
    def test_normalize(self, raw, expected):
        assert normalize_admission_year(raw) == expected

    def test_start(self):
        assert admission_year_start("2021-22") == 2021
        assert admission_year_start(None) is None


class TestParseDate:
    @pytest.mark.parametrize(
        "raw", ["2021-08-15", "15-08-2021", "15/08/2021", "15-Aug-2021"]
    )
    def test_formats(self, raw):
        assert parse_date(raw) == date(2021, 8, 15)

    def test_blank(self):
        assert parse_date("") is None

    def test_invalid(self):
        with pytest.raises(ValueError):
            parse_date("not a date")


class TestBranch:
    def test_alias(self):
        assert canonical_branch_code("Computer Science and Engineering") == "CSE"
        assert canonical_branch_code("mechanical") == "ME"

    def test_passthrough(self):
        assert canonical_branch_code("XYZ") == "XYZ"
        assert canonical_branch_code(None) is None
