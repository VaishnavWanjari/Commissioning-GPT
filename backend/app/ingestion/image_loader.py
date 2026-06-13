"""OCR helpers for scanned documents and images.

OCR is optional: it requires ``pytesseract`` plus the system ``tesseract``
binary. All helpers degrade gracefully (returning empty text / ``False``)
when OCR is unavailable, so the rest of the pipeline keeps working.
"""

from __future__ import annotations

import io
from functools import lru_cache
from pathlib import Path


@lru_cache(maxsize=1)
def ocr_available() -> bool:
    """True when pytesseract and the tesseract binary are both usable."""
    try:
        import pytesseract  # noqa: F401
        from PIL import Image  # noqa: F401
    except ImportError:
        return False
    try:
        import pytesseract

        pytesseract.get_tesseract_version()
        return True
    except Exception:
        return False


def ocr_image_bytes(data: bytes) -> str:
    """Run OCR on raw image bytes; return extracted text (or '' on failure)."""
    if not ocr_available():
        return ""
    try:
        import pytesseract
        from PIL import Image

        with Image.open(io.BytesIO(data)) as img:
            return pytesseract.image_to_string(img)
    except Exception:
        return ""


def load_image(path: Path) -> tuple[str, dict]:
    """OCR an image file on disk."""
    data = path.read_bytes()
    text = ocr_image_bytes(data)
    return text, {"ocr_available": ocr_available()}
