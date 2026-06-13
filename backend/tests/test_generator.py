"""Tests for the SOP generator orchestration using a fake LLM provider."""

from __future__ import annotations

from app.generator import generate_sop
from app.models.schemas import ExtractedDocument, SOPType


class FakeProvider:
    """A deterministic stand-in for the OpenAI provider (no network/key)."""

    def __init__(self, payload: dict):
        self._payload = payload

    def complete_json(self, system: str, user: str) -> dict:
        return self._payload

    def complete_text(self, system: str, user: str) -> str:
        return "fake answer"


def _full_payload() -> dict:
    return {
        "purpose": {"objective": "Commission pump", "scope": "P-101", "intended_use": "field"},
        "references": [{"category": "Standard", "reference": "API 610", "title": "Pumps"}],
        "procedure_steps": [
            {"step_no": "1", "activity_description": "Verify MC", "action_by": "CE",
             "verification": "cert", "hold_point": True, "witness_point": False,
             "expected_result": "ok"},
        ],
        "jsa": [{"activity": "test", "hazard": "rotating", "consequence": "injury",
                 "mitigation": "guard", "risk_rating": "Medium"}],
        "acceptance_criteria": ["Vibration within limits"],
    }


def _docs() -> list[ExtractedDocument]:
    return [ExtractedDocument(
        doc_id="d1", filename="pump_manual.pdf",
        text="Pump 20-P-101A per API 610 and SAES-J-902. Discharge 150 psig.",
    )]


def test_generate_populates_structure():
    resp = generate_sop(
        SOPType.COMMISSIONING_PROCEDURE,
        _docs(),
        equipment_name="P-101",
        provider=FakeProvider(_full_payload()),
    )
    sop = resp.sop
    assert sop.sop_type == SOPType.COMMISSIONING_PROCEDURE
    assert sop.procedure_steps[0].hold_point is True
    assert sop.purpose.objective == "Commission pump"
    assert resp.used_documents == ["pump_manual.pdf"]


def test_backfill_fills_document_control():
    # Model returns almost nothing; backfill must complete document control.
    resp = generate_sop(
        SOPType.MAINTENANCE_PROCEDURE,
        _docs(),
        equipment_name="P-101",
        provider=FakeProvider({}),
    )
    dc = resp.sop.document_control
    assert dc.title  # auto-generated
    assert dc.document_number  # auto-generated
    # Source document should be added to references.
    assert any(r.reference == "pump_manual.pdf" for r in resp.sop.references)


def test_type_overridden_from_request():
    payload = _full_payload()
    payload["sop_type"] = "Method Statement"  # wrong on purpose
    resp = generate_sop(
        SOPType.SHUTDOWN_PROCEDURE, _docs(), provider=FakeProvider(payload)
    )
    assert resp.sop.sop_type == SOPType.SHUTDOWN_PROCEDURE
