"""In-memory document registry with lightweight disk persistence.

For the MVP the registry lives in process memory (fast, simple) and mirrors a
JSON sidecar per document so the library survives a backend restart. Uploaded
originals are kept on disk under ``data/uploads`` and re-ingested lazily if the
in-memory cache is cold.
"""

from __future__ import annotations

import json
import threading

from .config import get_settings
from .models.schemas import DocumentSummary, ExtractedDocument


class DocumentRegistry:
    """Thread-safe registry of ingested documents."""

    def __init__(self) -> None:
        self._docs: dict[str, ExtractedDocument] = {}
        self._lock = threading.Lock()
        self._loaded = False

    # --- persistence -----------------------------------------------------
    def _meta_path(self, doc_id: str):
        return get_settings().index_path / f"{doc_id}.json"

    def _hydrate(self) -> None:
        """Load any persisted documents from disk (once)."""
        if self._loaded:
            return
        settings = get_settings()
        settings.ensure_dirs()
        for meta_file in settings.index_path.glob("*.json"):
            try:
                data = json.loads(meta_file.read_text(encoding="utf-8"))
                doc = ExtractedDocument.model_validate(data)
                self._docs[doc.doc_id] = doc
            except Exception:  # pragma: no cover - tolerate corrupt sidecars
                continue
        self._loaded = True

    def _persist(self, doc: ExtractedDocument) -> None:
        get_settings().ensure_dirs()
        self._meta_path(doc.doc_id).write_text(
            doc.model_dump_json(), encoding="utf-8"
        )

    # --- API -------------------------------------------------------------
    def add(self, doc: ExtractedDocument) -> None:
        with self._lock:
            self._hydrate()
            self._docs[doc.doc_id] = doc
            self._persist(doc)

    def get(self, doc_id: str) -> ExtractedDocument | None:
        with self._lock:
            self._hydrate()
            return self._docs.get(doc_id)

    def get_many(self, doc_ids: list[str]) -> list[ExtractedDocument]:
        with self._lock:
            self._hydrate()
            return [self._docs[d] for d in doc_ids if d in self._docs]

    def list_summaries(self) -> list[DocumentSummary]:
        with self._lock:
            self._hydrate()
            return [d.summary() for d in self._docs.values()]

    def delete(self, doc_id: str) -> bool:
        with self._lock:
            self._hydrate()
            existed = self._docs.pop(doc_id, None) is not None
            meta = self._meta_path(doc_id)
            if meta.exists():
                meta.unlink()
            return existed

    def clear(self) -> None:
        """Mostly for tests."""
        with self._lock:
            self._docs.clear()
            self._loaded = True


# Module-level singleton used across the API routers.
registry = DocumentRegistry()
