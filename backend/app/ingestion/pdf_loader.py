"""PDF ingestion using PyMuPDF, with OCR fallback for scanned pages.

Pages that contain a text layer are read directly. Pages with little or no
extractable text (typical of scanned manuals or P&IDs) are rasterized and
passed through OCR when the optional dependencies (pytesseract + the system
``tesseract`` binary) are available.
"""

from __future__ import annotations

from pathlib import Path

from .image_loader import ocr_image_bytes, ocr_available

# Below this many characters a page is considered "scanned" and OCR is tried.
_MIN_TEXT_CHARS = 20


def load_pdf(path: Path) -> tuple[str, int, list, dict, bool]:
    """Return (text, page_count, tables, metadata, used_ocr).

    Tables are not extracted from PDFs in the MVP (PyMuPDF table extraction is
    noisy for engineering drawings); the raw text already carries tabular data
    well enough for the LLM. The hook is kept for a future enhancement.
    """
    try:
        import fitz  # PyMuPDF
    except ImportError as exc:  # pragma: no cover - dependency guard
        raise RuntimeError(
            "PyMuPDF (fitz) is required to read PDF files. Install with "
            "`pip install PyMuPDF`."
        ) from exc

    used_ocr = False
    parts: list[str] = []

    with fitz.open(path) as doc:
        page_count = doc.page_count
        metadata = dict(doc.metadata or {})
        for page in doc:
            text = page.get_text("text").strip()
            if len(text) < _MIN_TEXT_CHARS and ocr_available():
                # Rasterize at 2x for better OCR accuracy on dense drawings.
                pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
                ocr_text = ocr_image_bytes(pix.tobytes("png"))
                if ocr_text.strip():
                    text = ocr_text.strip()
                    used_ocr = True
            if text:
                parts.append(text)

    return "\n\n".join(parts), page_count, [], metadata, used_ocr
