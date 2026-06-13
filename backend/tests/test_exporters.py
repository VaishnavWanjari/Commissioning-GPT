"""Tests for the docx / pdf / xlsx exporters."""

from __future__ import annotations

import zipfile

from app.exporters import export_docx, export_pdf, export_xlsx


def test_docx_export_is_valid(sample_sop, tmp_path):
    out = tmp_path / "sop.docx"
    export_docx(sample_sop, out)
    assert out.exists() and out.stat().st_size > 0
    # A .docx is a zip; confirm the core document part is present.
    with zipfile.ZipFile(out) as z:
        assert "word/document.xml" in z.namelist()
        xml = z.read("word/document.xml").decode("utf-8", "ignore")
    assert "Procedure Execution Steps" in xml
    assert "Job Safety Analysis" in xml


def test_pdf_export_is_valid(sample_sop, tmp_path):
    out = tmp_path / "sop.pdf"
    export_pdf(sample_sop, out)
    assert out.exists() and out.stat().st_size > 0
    assert out.read_bytes()[:4] == b"%PDF"


def test_xlsx_export_is_valid(sample_sop, tmp_path):
    out = tmp_path / "sop.xlsx"
    export_xlsx(sample_sop, out)
    assert out.exists()
    with zipfile.ZipFile(out) as z:
        assert "xl/workbook.xml" in z.namelist()
