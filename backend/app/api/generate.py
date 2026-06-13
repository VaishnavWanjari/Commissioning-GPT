"""SOP generation endpoint."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException

from ..config import get_settings
from ..generator import generate_sop
from ..models.schemas import GenerateRequest, GenerateResponse
from ..store import registry

router = APIRouter(prefix="/api/generate", tags=["generate"])


@router.post("", response_model=GenerateResponse)
async def generate(req: GenerateRequest) -> GenerateResponse:
    if not get_settings().has_openai:
        raise HTTPException(
            status_code=503,
            detail="OPENAI_API_KEY is not configured. Set it in backend/.env to "
            "generate SOPs (see .env.example).",
        )

    docs = registry.get_many(req.doc_ids)
    if req.doc_ids and not docs:
        raise HTTPException(status_code=404, detail="None of the requested documents were found.")

    try:
        return generate_sop(
            req.sop_type,
            docs,
            equipment_name=req.equipment_name,
            title=req.title,
            document_number=req.document_number,
            additional_instructions=req.additional_instructions,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Generation failed: {exc}") from exc
