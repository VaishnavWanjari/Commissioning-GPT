"""Dispatch an uploaded file to the right loader and normalize the result."""

from __future__ import annotations

import uuid
from pathlib import Path

from ..models.schemas import ExtractedDocument, ExtractedTable
from . import office_loader, pdf_loader, text_loader
from .image_loader import load_image

# Extensions we know how to ingest. Unknown extensions are attempted as text.
SUPPORTED_EXTENSIONS = {
    ".pdf",
    ".docx",
    ".xlsx",
    ".xls",
    ".pptx",
    ".txt",
    ".md",
    ".csv",
    ".log",
    ".png",
    ".jpg",
    ".jpeg",
    ".tif",
    ".tiff",
    ".bmp",
}

_IMAGE_EXT = {".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp"}
_TEXT_EXT = {".txt", ".md", ".csv", ".log"}


def load_document(
    path: Path,
    *,
    original_filename: str | None = None,
    content_type: str | None = None,
    doc_id: str | None = None,
) -> ExtractedDocument:
    """Ingest a file from ``path`` into a normalized :class:`ExtractedDocument`."""
    filename = original_filename or path.name
    ext = Path(filename).suffix.lower()
    doc_id = doc_id or uuid.uuid4().hex

    text = ""
    page_count = 0
    tables: list[ExtractedTable] = []
    metadata: dict = {}
    used_ocr = False

    if ext == ".pdf":
        text, page_count, tables, metadata, used_ocr = pdf_loader.load_pdf(path)
    elif ext == ".docx":
        text, tables, metadata = office_loader.load_docx(path)
    elif ext in {".xlsx", ".xls"}:
        text, tables, metadata = office_loader.load_xlsx(path)
    elif ext == ".pptx":
        text, metadata = office_loader.load_pptx(path)
    elif ext in _IMAGE_EXT:
        text, metadata = load_image(path)
        used_ocr = bool(text)
    elif ext in _TEXT_EXT:
        text, metadata = text_loader.load_text(path)
    else:
        # Best effort: try to read as text.
        text, metadata = text_loader.load_text(path)
        metadata["note"] = f"Unknown extension '{ext}', read as plain text."

    return ExtractedDocument(
        doc_id=doc_id,
        filename=filename,
        content_type=content_type,
        text=text,
        page_count=page_count,
        tables=tables,
        metadata=metadata,
        used_ocr=used_ocr,
    )
