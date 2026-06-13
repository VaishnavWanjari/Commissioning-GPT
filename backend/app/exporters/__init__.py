"""Render a :class:`SOPDocument` to .docx / PDF / Excel."""

from .docx_exporter import export_docx
from .pdf_exporter import export_pdf
from .xlsx_exporter import export_xlsx

__all__ = ["export_docx", "export_pdf", "export_xlsx", "export_sop"]


def export_sop(sop, fmt: str, out_path) -> str:
    """Dispatch to the exporter for ``fmt`` ('docx' | 'pdf' | 'xlsx')."""
    fmt = fmt.lower()
    if fmt == "docx":
        return export_docx(sop, out_path)
    if fmt == "pdf":
        return export_pdf(sop, out_path)
    if fmt == "xlsx":
        return export_xlsx(sop, out_path)
    raise ValueError(f"Unsupported export format: {fmt}")
