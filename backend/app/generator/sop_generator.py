"""Orchestrate SOP generation: retrieve context -> prompt LLM -> validate.

Flow:
    1. Concatenate/extract entities from the selected documents.
    2. Build a RAG context relevant to the requested procedure type.
    3. Ask the LLM for a structured JSON SOP.
    4. Validate/repair into a :class:`SOPDocument`, then backfill any thin
       sections (document control, references) from deterministic extraction so
       the output is never empty even if the model under-delivers.
"""

from __future__ import annotations

from datetime import date

from ..ai.prompts import (
    SYSTEM_PROMPT,
    build_generation_prompt,
)
from ..ai.provider import LLMProvider, get_provider
from ..extraction import extract_entities
from ..models.schemas import (
    DocumentControl,
    ExtractedDocument,
    GenerateResponse,
    ReferenceRow,
    SOPDocument,
    SOPType,
)
from ..rag import build_context


def _slug(text: str) -> str:
    return "".join(c for c in text.upper() if c.isalnum())[:6] or "GEN"


def generate_sop(
    sop_type: SOPType,
    docs: list[ExtractedDocument],
    *,
    equipment_name: str | None = None,
    title: str | None = None,
    document_number: str | None = None,
    additional_instructions: str | None = None,
    provider: LLMProvider | None = None,
) -> GenerateResponse:
    """Generate a structured SOP from the given documents."""
    warnings: list[str] = []
    provider = provider or get_provider()

    combined_text = "\n\n".join(d.text for d in docs)
    entities = extract_entities(combined_text)

    # Retrieval query biased toward the requested procedure + equipment.
    query = " ".join(
        filter(None, [sop_type.value, equipment_name or "", "procedure steps safety prerequisites"])
    )
    context, sources = ("", [])
    if docs:
        try:
            context, sources = build_context(docs, query)
        except Exception as exc:  # retrieval is best-effort
            warnings.append(f"Context retrieval degraded: {exc}")
            context = combined_text[:6000]

    user_prompt = build_generation_prompt(
        sop_type,
        context,
        entities,
        equipment_name=equipment_name,
        title=title,
        document_number=document_number,
        additional_instructions=additional_instructions,
    )

    raw = provider.complete_json(SYSTEM_PROMPT, user_prompt)

    # Ensure the declared type matches the request regardless of model output.
    raw["sop_type"] = sop_type.value
    try:
        sop = SOPDocument.model_validate(raw)
    except Exception as exc:
        warnings.append(f"Model output required repair: {exc}")
        sop = _coerce(raw, sop_type)

    _backfill(sop, sop_type, entities, equipment_name, title, document_number, sources)

    used = [d.filename for d in docs]
    return GenerateResponse(sop=sop, used_documents=used, warnings=warnings)


def _coerce(raw: dict, sop_type: SOPType) -> SOPDocument:
    """Best-effort construction when strict validation fails."""
    sop = SOPDocument(sop_type=sop_type)
    for field_name in SOPDocument.model_fields:
        if field_name in raw:
            try:
                setattr(
                    sop,
                    field_name,
                    SOPDocument.model_validate(
                        {**sop.model_dump(), field_name: raw[field_name]}
                    ).__getattribute__(field_name),
                )
            except Exception:
                continue
    return sop


def _backfill(
    sop: SOPDocument,
    sop_type: SOPType,
    entities,
    equipment_name: str | None,
    title: str | None,
    document_number: str | None,
    sources: list[str],
) -> None:
    """Fill thin sections so the export is always complete and professional."""
    dc = sop.document_control or DocumentControl()
    sop.document_control = dc

    if not dc.title:
        base = equipment_name or "Equipment / System"
        dc.title = title or f"{sop_type.value} for {base}"
    if not dc.document_number:
        dc.document_number = document_number or (
            f"PRJ-{_slug(sop_type.value)}-{_slug(equipment_name or 'GEN')}-001"
        )
    if not dc.issue_date:
        dc.issue_date = date.today().isoformat()

    # Seed References from extracted standards if the model returned none.
    if not sop.references and entities.standards:
        sop.references = [
            ReferenceRow(category="Standard", reference=s, title="")
            for s in entities.standards[:15]
        ]
    # Add the actual source documents as references/attachments.
    existing_refs = {r.reference for r in sop.references}
    for src in sources:
        if src not in existing_refs:
            sop.references.append(
                ReferenceRow(category="Source Document", reference=src, title="")
            )
