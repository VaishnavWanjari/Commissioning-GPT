"""Document ingestion — turn uploaded files into normalized text + tables."""

from .loader import load_document, SUPPORTED_EXTENSIONS

__all__ = ["load_document", "SUPPORTED_EXTENSIONS"]
