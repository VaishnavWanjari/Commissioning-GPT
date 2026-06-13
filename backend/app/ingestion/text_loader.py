"""Plain-text / markdown / csv ingestion."""

from __future__ import annotations

from pathlib import Path


def load_text(path: Path) -> tuple[str, dict]:
    """Read a text file, tolerating unknown encodings."""
    for encoding in ("utf-8", "utf-16", "latin-1"):
        try:
            return path.read_text(encoding=encoding), {"encoding": encoding}
        except (UnicodeDecodeError, UnicodeError):
            continue
    # Last resort: replace undecodable bytes.
    return path.read_text(encoding="utf-8", errors="replace"), {"encoding": "utf-8/replace"}
