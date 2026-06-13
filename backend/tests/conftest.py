"""Shared pytest fixtures."""

from __future__ import annotations

import sys
from pathlib import Path

import pytest

# Make the backend package importable when running `pytest` from backend/.
BACKEND_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_ROOT))

from app.models.schemas import (  # noqa: E402
    DocumentControl,
    JSARow,
    ProcedureStep,
    Purpose,
    ReferenceRow,
    SOPDocument,
    SOPType,
)


@pytest.fixture
def sample_sop() -> SOPDocument:
    """A representative, partly-populated SOP for exporter tests."""
    return SOPDocument(
        sop_type=SOPType.COMMISSIONING_PROCEDURE,
        document_control=DocumentControl(
            title="Commissioning Procedure for Centrifugal Pump P-101",
            document_number="PRJ-COM-P101-001",
        ),
        purpose=Purpose(
            objective="Safely commission centrifugal pump P-101.",
            scope="Covers pre-commissioning checks through performance test.",
            intended_use="Field execution by commissioning team.",
        ),
        references=[ReferenceRow(category="Standard", reference="API 610", title="Centrifugal Pumps")],
        procedure_steps=[
            ProcedureStep(step_no="1", activity_description="Verify mechanical completion",
                          action_by="Commissioning Engineer", verification="MC certificate signed",
                          hold_point=True, expected_result="MC accepted"),
            ProcedureStep(step_no="2", activity_description="Check lube oil level",
                          action_by="Mechanical Technician", verification="Sight glass mid-level"),
        ],
        jsa=[JSARow(activity="Rotating equipment test", hazard="Entanglement",
                    consequence="Injury", mitigation="Guards in place, PTW", risk_rating="Medium")],
        acceptance_criteria=["Vibration within API 610 limits", "No abnormal noise"],
    )
