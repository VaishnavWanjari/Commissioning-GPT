"""Tests for the heuristic entity extractor."""

from __future__ import annotations

from app.extraction import extract_entities

SAMPLE = """
Commissioning of centrifugal pump 20-P-101A shall comply with SAES-J-902 and
API 610. The discharge pressure shall not exceed 150 psig at 350 °C. The MOV
(Motor Operated Valve) shall be stroked per IEC 60534. Refer to ISO 9001 for
quality. Hot work and confined space permits are required. H2S hazard present.
"""


def test_extracts_equipment_tags():
    ent = extract_entities(SAMPLE)
    assert any("P-101" in t for t in ent.equipment_tags)


def test_extracts_standards():
    ent = extract_entities(SAMPLE)
    joined = " ".join(ent.standards).upper()
    assert "API 610" in joined.replace("  ", " ")
    assert any("SAES" in s.upper() for s in ent.standards)
    assert any("IEC" in s.upper() for s in ent.standards)


def test_extracts_abbreviations():
    ent = extract_entities(SAMPLE)
    assert ent.abbreviations.get("MOV", "").lower().startswith("motor operated")


def test_extracts_limits_and_safety():
    ent = extract_entities(SAMPLE)
    assert any("psig" in lim.lower() for lim in ent.operating_limits)
    assert "h2s" in [s.lower() for s in ent.safety_keywords] or \
        "hot work" in [s.lower() for s in ent.safety_keywords]


def test_empty_text():
    ent = extract_entities("")
    assert ent.equipment_tags == []
