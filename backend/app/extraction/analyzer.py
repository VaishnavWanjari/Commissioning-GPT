"""Lightweight, deterministic entity extraction.

This is *not* a replacement for the LLM — it is a fast, offline pre-pass that
pulls out high-signal engineering entities (equipment tags, standard
references, abbreviations, operating limits, safety keywords). These feed two
purposes:

* enrich the LLM prompt with a structured "facts" block, and
* seed the References / Definitions sections so they are grounded in the
  source documents even before the LLM elaborates on them.
"""

from __future__ import annotations

import re
from collections import OrderedDict

from ..models.schemas import ExtractedEntities

# Equipment tags such as "20-PG-001", "P-101A", "10-K-2401".
_EQUIPMENT_TAG = re.compile(r"\b\d{0,3}-?[A-Z]{1,4}-?\d{2,4}[A-Z]?\b")

# Standards references for the bodies named in the spec (section 7).
_STANDARD_PATTERNS = [
    re.compile(r"\bSAEP[-\s]?\d{3,4}\b", re.IGNORECASE),
    re.compile(r"\bSAES[-\s]?[A-Z]{1,3}[-\s]?\d{2,4}\b", re.IGNORECASE),
    re.compile(r"\bSAIC[-\s]?[A-Z]{1,3}[-\s]?\d{2,4}\b", re.IGNORECASE),
    re.compile(r"\bAPI\s?(?:RP\s?|Std\s?|Spec\s?)?\d{2,4}[A-Z]?\b", re.IGNORECASE),
    re.compile(r"\bASME\s?[A-Z]{0,3}\.?\d{1,3}(?:\.\d{1,3})?\b", re.IGNORECASE),
    re.compile(r"\bIEC\s?\d{3,5}(?:-\d{1,3})?\b", re.IGNORECASE),
    re.compile(r"\bISO\s?\d{3,5}(?:[-:]\d{1,4})?\b", re.IGNORECASE),
    re.compile(r"\bNFPA\s?\d{1,3}[A-Z]?\b", re.IGNORECASE),
]

# Abbreviation followed by its expansion, e.g. "MOV (Motor Operated Valve)".
_ABBREV = re.compile(r"\b([A-Z]{2,6})\b\s*\(([^)]{3,60})\)")

# Operating limits / set-points, e.g. "150 psig", "350 °C", "1.5 bar".
_LIMIT = re.compile(
    r"\b\d+(?:\.\d+)?\s?(?:°?C|°?F|K|psi[ag]?|bar[ag]?|kPa|MPa|barg|"
    r"rpm|Hz|kW|MW|V|kV|A|mA|mm|cm|m3/h|m³/h|%|ppm|mbar)\b",
    re.IGNORECASE,
)

_SAFETY_TERMS = [
    "hazard", "ppe", "permit", "loto", "lock out", "tag out", "isolation",
    "h2s", "hydrogen sulfide", "toxic", "flammable", "explosive", "confined space",
    "hot work", "lifting", "pressure", "high voltage", "arc flash", "nitrogen",
    "asphyxiation", "spill", "emergency", "fire", "esd", "relief valve",
]


def _dedupe(items) -> list[str]:
    """Order-preserving de-duplication (case-insensitive)."""
    seen = OrderedDict()
    for item in items:
        key = item.strip().upper()
        if key and key not in seen:
            seen[key] = item.strip()
    return list(seen.values())


def extract_entities(text: str, *, max_each: int = 60) -> ExtractedEntities:
    """Extract engineering entities from a block of document text."""
    if not text:
        return ExtractedEntities()

    tags = [m.group(0) for m in _EQUIPMENT_TAG.finditer(text)]
    # Filter out obvious false positives (pure dates, plain years).
    tags = [t for t in tags if not re.fullmatch(r"\d{4}", t.replace("-", ""))]

    standards: list[str] = []
    for pat in _STANDARD_PATTERNS:
        standards.extend(m.group(0).strip() for m in pat.finditer(text))

    abbreviations: dict[str, str] = {}
    for m in _ABBREV.finditer(text):
        abbr, expansion = m.group(1), m.group(2).strip()
        abbreviations.setdefault(abbr, expansion)

    limits = [m.group(0) for m in _LIMIT.finditer(text)]

    lowered = text.lower()
    safety = [term for term in _SAFETY_TERMS if term in lowered]

    return ExtractedEntities(
        equipment_tags=_dedupe(tags)[:max_each],
        standards=_dedupe(standards)[:max_each],
        abbreviations=dict(list(abbreviations.items())[:max_each]),
        operating_limits=_dedupe(limits)[:max_each],
        safety_keywords=_dedupe(safety)[:max_each],
    )
