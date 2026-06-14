"""Pydantic models.

This module defines two families of models:

1.  API request/response models (upload, generate, chat, export).
2.  The :class:`SOPDocument` structure — a strict representation of an
    engineering-grade procedure following sections **A–P** of the project
    specification. This same structure is what the LLM is asked to produce
    (as JSON) and what the exporters render to ``.docx`` / PDF / Excel.
"""

from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


# --------------------------------------------------------------------------- #
#  Enumerations
# --------------------------------------------------------------------------- #
class SOPType(str, Enum):
    """The procedure types a user may request (spec section 4)."""

    STANDARD_OPERATING_PROCEDURE = "Standard Operating Procedure"
    METHOD_STATEMENT = "Method Statement"
    COMMISSIONING_PROCEDURE = "Commissioning Procedure"
    STARTUP_PROCEDURE = "Startup Procedure"
    SHUTDOWN_PROCEDURE = "Shutdown Procedure"
    MAINTENANCE_PROCEDURE = "Maintenance Procedure"
    PRESERVATION_PROCEDURE = "Preservation Procedure"
    OPERATIONAL_GUIDELINE = "Operational Guideline"
    WORK_INSTRUCTION = "Work Instruction"
    INSPECTION_PROCEDURE = "Inspection Procedure"
    TEST_PROCEDURE = "Test Procedure"
    EMERGENCY_PROCEDURE = "Emergency Procedure"
    TROUBLESHOOTING_GUIDE = "Troubleshooting Guide"


class ExportFormat(str, Enum):
    DOCX = "docx"
    PDF = "pdf"
    XLSX = "xlsx"


# --------------------------------------------------------------------------- #
#  Document ingestion models
# --------------------------------------------------------------------------- #
class ExtractedTable(BaseModel):
    """A table extracted from a source document."""

    caption: Optional[str] = None
    rows: list[list[str]] = Field(default_factory=list)


class ExtractedDocument(BaseModel):
    """Normalized representation of an ingested source document."""

    doc_id: str
    filename: str
    content_type: Optional[str] = None
    text: str = ""
    page_count: int = 0
    tables: list[ExtractedTable] = Field(default_factory=list)
    metadata: dict = Field(default_factory=dict)
    used_ocr: bool = False

    def summary(self) -> "DocumentSummary":
        return DocumentSummary(
            doc_id=self.doc_id,
            filename=self.filename,
            content_type=self.content_type,
            page_count=self.page_count,
            char_count=len(self.text),
            used_ocr=self.used_ocr,
        )


class DocumentSummary(BaseModel):
    """Lightweight document descriptor returned to the UI library."""

    doc_id: str
    filename: str
    content_type: Optional[str] = None
    page_count: int = 0
    char_count: int = 0
    used_ocr: bool = False


class ExtractedEntities(BaseModel):
    """Heuristically extracted engineering entities used to enrich prompts."""

    equipment_tags: list[str] = Field(default_factory=list)
    standards: list[str] = Field(default_factory=list)
    abbreviations: dict[str, str] = Field(default_factory=dict)
    operating_limits: list[str] = Field(default_factory=list)
    safety_keywords: list[str] = Field(default_factory=list)


# --------------------------------------------------------------------------- #
#  SOP document structure (sections A–P)
# --------------------------------------------------------------------------- #
class RevisionHistoryRow(BaseModel):
    revision: str = "0"
    date: str = Field(default_factory=lambda: date.today().isoformat())
    description: str = "Issued for Use"
    prepared_by: str = ""
    approved_by: str = ""


class DocumentControl(BaseModel):
    """Section A — Document Control."""

    title: str = ""
    document_number: str = ""
    revision_number: str = "0"
    prepared_by: str = "Commissioning Engineer"
    checked_by: str = "Lead Commissioning Engineer"
    approved_by: str = "Commissioning Manager"
    issue_date: str = Field(default_factory=lambda: date.today().isoformat())
    revision_history: list[RevisionHistoryRow] = Field(default_factory=list)



class Purpose(BaseModel):
    """Section B — Purpose."""

    objective: str = ""
    scope: str = ""
    intended_use: str = ""


class ReferenceRow(BaseModel):
    """Section C — References."""

    category: str = ""  # e.g. Vendor Manual, Aramco Standard, P&ID, Datasheet
    reference: str = ""
    title: str = ""


class DefinitionRow(BaseModel):
    """Section D — Definitions & Abbreviations."""

    term: str = ""
    definition: str = ""


class ResponsibilityRow(BaseModel):
    """Section E — Responsibilities."""

    role: str = ""
    responsibility: str = ""


class PrerequisiteRow(BaseModel):
    """Section F — Prerequisites."""

    category: str = ""  # Mechanical Completion, Permit, Isolation, Utility, etc.
    requirement: str = ""
    status: str = ""


class ToolRow(BaseModel):
    """Section G — Tools & Equipment."""

    sr_no: int = 0
    tool: str = ""
    quantity: str = ""
    remarks: str = ""


class MaterialRow(BaseModel):
    """Section H — Materials & Consumables."""

    sr_no: int = 0
    material: str = ""
    quantity: str = ""
    remarks: str = ""


class SafetyRequirements(BaseModel):
    """Section I — Safety Requirements."""

    ppe: list[str] = Field(default_factory=list)
    hazard_identification: list[str] = Field(default_factory=list)
    risk_assessment: list[str] = Field(default_factory=list)
    control_measures: list[str] = Field(default_factory=list)
    environmental_requirements: list[str] = Field(default_factory=list)
    permit_requirements: list[str] = Field(default_factory=list)
    emergency_actions: list[str] = Field(default_factory=list)


class JSARow(BaseModel):
    """Section J — Job Safety Analysis."""

    activity: str = ""
    hazard: str = ""
    consequence: str = ""
    mitigation: str = ""
    risk_rating: str = ""  # e.g. Low / Medium / High


class ProcedureStep(BaseModel):
    """Section K — Procedure Execution Steps."""

    step_no: str = ""
    activity_description: str = ""
    action_by: str = ""
    verification: str = ""
    hold_point: bool = False
    witness_point: bool = False
    expected_result: str = ""


class ChecklistItem(BaseModel):
    """Section L — Checklists."""

    item: str = ""
    acceptance: str = ""
    checked: bool = False


class TroubleshootingRow(BaseModel):
    """Section M — Troubleshooting Guide."""

    problem: str = ""
    possible_cause: str = ""
    corrective_action: str = ""


class RecordRow(BaseModel):
    """Section O — Records."""

    activity: str = ""
    date: str = ""
    signature: str = ""
    remarks: str = ""


class AttachmentRow(BaseModel):
    """Section P — Attachments."""

    category: str = ""  # Drawing, Datasheet, Vendor Reference, Standard
    reference: str = ""
    description: str = ""


class SOPDocument(BaseModel):
    """The full generated procedure (sections A–P)."""

    sop_type: SOPType = SOPType.STANDARD_OPERATING_PROCEDURE

    # A
    document_control: DocumentControl = Field(default_factory=DocumentControl)
    # B
    purpose: Purpose = Field(default_factory=Purpose)
    # C
    references: list[ReferenceRow] = Field(default_factory=list)
    # D
    definitions: list[DefinitionRow] = Field(default_factory=list)
    # E
    responsibilities: list[ResponsibilityRow] = Field(default_factory=list)
    # F
    prerequisites: list[PrerequisiteRow] = Field(default_factory=list)
    # G
    tools: list[ToolRow] = Field(default_factory=list)
    # H
    materials: list[MaterialRow] = Field(default_factory=list)
    # I
    safety: SafetyRequirements = Field(default_factory=SafetyRequirements)
    # J
    jsa: list[JSARow] = Field(default_factory=list)
    # K
    procedure_steps: list[ProcedureStep] = Field(default_factory=list)
    # L
    checklists: list[ChecklistItem] = Field(default_factory=list)
    # M
    troubleshooting: list[TroubleshootingRow] = Field(default_factory=list)
    # N
    acceptance_criteria: list[str] = Field(default_factory=list)
    # O
    records: list[RecordRow] = Field(default_factory=list)
    # P
    attachments: list[AttachmentRow] = Field(default_factory=list)


# --------------------------------------------------------------------------- #
#  API request / response models
# --------------------------------------------------------------------------- #
class GenerateRequest(BaseModel):
    sop_type: SOPType
    doc_ids: list[str] = Field(default_factory=list)
    equipment_name: Optional[str] = None
    title: Optional[str] = None
    document_number: Optional[str] = None
    additional_instructions: Optional[str] = None


class GenerateResponse(BaseModel):
    sop: SOPDocument
    used_documents: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)


class ChatMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    message: str
    doc_ids: list[str] = Field(default_factory=list)
    history: list[ChatMessage] = Field(default_factory=list)


class ChatResponse(BaseModel):
    reply: str
    sources: list[str] = Field(default_factory=list)


class ExportRequest(BaseModel):
    sop: SOPDocument
    format: ExportFormat = ExportFormat.DOCX
