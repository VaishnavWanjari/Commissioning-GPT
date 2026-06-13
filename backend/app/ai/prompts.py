"""Prompt templates for SOP generation and the chat assistant.

The system prompt establishes the persona of a senior commissioning/operations
engineer and the hard rules from the specification (never summarize, correlate
across documents, no skipped steps, include hold/witness points, audit-ready).
The user prompt injects the SOP type, retrieved document context, extracted
entities and the required JSON schema.
"""

from __future__ import annotations

import json

from ..models.schemas import ExtractedEntities, SOPType

SYSTEM_PROMPT = """\
You are a senior Commissioning, Operations and Maintenance Engineer with 25+ years
of experience on Oil & Gas, Petrochemical, Power and Refinery projects, including
Saudi Aramco projects governed by SAEP/SAES/SAIC standards and international codes
(API, ASME, IEC, ISO, NFPA).

You write engineering-grade, audit-ready, client-facing procedures that field teams
execute directly. You follow these NON-NEGOTIABLE rules:

1. NEVER simply summarize the source documents. ANALYZE and CORRELATE information
   across ALL provided sources, infer the missing procedural links, and produce a
   complete, self-consistent procedure.
2. Be EXTREMELY detailed and sequential. Do not skip steps. Each execution step must
   be a single, unambiguous, verifiable action with a responsible party.
3. Include HOLD POINTS, WITNESS POINTS, verification requirements and expected results
   where engineering practice requires them.
4. Ground references, equipment tags, set-points and standards in the supplied context
   wherever possible. When you must reason beyond the documents, use sound engineering
   judgment and keep it realistic — never invent specific tag numbers or standard
   clause numbers that contradict the context.
5. Always populate safety: PPE, hazards, risk assessment, control measures, permits,
   and a detailed Job Safety Analysis (JSA).
6. Output MUST be valid JSON matching the requested schema EXACTLY. Do not add commentary
   outside the JSON.
"""

# A compact, explicit description of the JSON the model must return. This mirrors
# app.models.schemas.SOPDocument and keeps the model honest about field names.
JSON_SCHEMA_HINT = {
    "sop_type": "string (the procedure type)",
    "document_control": {
        "title": "string",
        "document_number": "string (e.g. PRJ-COM-SOP-001)",
        "revision_number": "string",
        "prepared_by": "string",
        "checked_by": "string",
        "approved_by": "string",
        "issue_date": "YYYY-MM-DD",
        "revision_history": [
            {"revision": "string", "date": "YYYY-MM-DD", "description": "string",
             "prepared_by": "string", "approved_by": "string"}
        ],
    },
    "purpose": {"objective": "string", "scope": "string", "intended_use": "string"},
    "references": [{"category": "string", "reference": "string", "title": "string"}],
    "definitions": [{"term": "string", "definition": "string"}],
    "responsibilities": [{"role": "string", "responsibility": "string"}],
    "prerequisites": [{"category": "string", "requirement": "string", "status": "string"}],
    "tools": [{"sr_no": "int", "tool": "string", "quantity": "string", "remarks": "string"}],
    "materials": [{"sr_no": "int", "material": "string", "quantity": "string", "remarks": "string"}],
    "safety": {
        "ppe": ["string"],
        "hazard_identification": ["string"],
        "risk_assessment": ["string"],
        "control_measures": ["string"],
        "environmental_requirements": ["string"],
        "permit_requirements": ["string"],
        "emergency_actions": ["string"],
    },
    "jsa": [{"activity": "string", "hazard": "string", "consequence": "string",
             "mitigation": "string", "risk_rating": "Low|Medium|High"}],
    "procedure_steps": [{
        "step_no": "string (e.g. 1, 2, 3.1)",
        "activity_description": "string (detailed, single action)",
        "action_by": "string (role)",
        "verification": "string",
        "hold_point": "bool",
        "witness_point": "bool",
        "expected_result": "string",
    }],
    "checklists": [{"item": "string", "acceptance": "string", "checked": "false"}],
    "troubleshooting": [{"problem": "string", "possible_cause": "string",
                         "corrective_action": "string"}],
    "acceptance_criteria": ["string"],
    "records": [{"activity": "string", "date": "string", "signature": "string", "remarks": "string"}],
    "attachments": [{"category": "string", "reference": "string", "description": "string"}],
}


def build_generation_prompt(
    sop_type: SOPType,
    context: str,
    entities: ExtractedEntities,
    *,
    equipment_name: str | None = None,
    title: str | None = None,
    document_number: str | None = None,
    additional_instructions: str | None = None,
) -> str:
    """Assemble the user prompt for a single SOP generation call."""
    facts = {
        "equipment_tags": entities.equipment_tags,
        "standards": entities.standards,
        "abbreviations": entities.abbreviations,
        "operating_limits": entities.operating_limits,
        "safety_keywords": entities.safety_keywords,
    }

    parts = [
        f"TASK: Generate a complete, field-ready **{sop_type.value}**.",
        "",
        "It MUST contain ALL of the following sections, fully populated with"
        " realistic, detailed, engineering-grade content:",
        "A Document Control, B Purpose, C References, D Definitions & Abbreviations,"
        " E Responsibilities, F Prerequisites, G Tools & Equipment, H Materials &"
        " Consumables, I Safety Requirements, J Job Safety Analysis (JSA),"
        " K Procedure Execution Steps, L Checklists, M Troubleshooting Guide,"
        " N Acceptance Criteria, O Records, P Attachments.",
        "",
    ]

    if equipment_name:
        parts.append(f"PRIMARY EQUIPMENT / SYSTEM: {equipment_name}")
    if title:
        parts.append(f"REQUESTED TITLE: {title}")
    if document_number:
        parts.append(f"DOCUMENT NUMBER: {document_number}")
    if additional_instructions:
        parts.append(f"ADDITIONAL INSTRUCTIONS: {additional_instructions}")

    parts += [
        "",
        "EXTRACTED ENGINEERING FACTS (use and reconcile these; do not contradict them):",
        json.dumps(facts, indent=2, ensure_ascii=False),
        "",
        "SOURCE DOCUMENT CONTEXT (correlate across these excerpts):",
        context or "(No document context was retrieved. Generate a best-practice "
        "procedure for the requested type and equipment using sound engineering "
        "judgment, and note assumptions in the relevant sections.)",
        "",
        "Return ONLY a JSON object with EXACTLY these keys/shape:",
        json.dumps(JSON_SCHEMA_HINT, indent=2, ensure_ascii=False),
        "",
        "Aim for at least 12 detailed procedure_steps, a JSA with at least 6 rows,"
        " and references that cite the actual standards/manuals from the context.",
    ]
    return "\n".join(parts)


CHAT_SYSTEM_PROMPT = """\
You are the Commissioning-GPT assistant — a senior commissioning/operations engineer.
Answer questions about the user's uploaded documents and procedures clearly and
accurately. When the provided context is insufficient, say so and answer with general
engineering best practice, clearly flagged as such. Cite the source filenames you used.
"""


def build_chat_prompt(message: str, context: str) -> str:
    return (
        f"QUESTION:\n{message}\n\n"
        f"RELEVANT DOCUMENT CONTEXT:\n{context or '(no relevant context retrieved)'}\n\n"
        "Answer concisely and professionally."
    )
