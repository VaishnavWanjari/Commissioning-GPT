"""Document upload / library endpoints."""

from __future__ import annotations

import uuid
from pathlib import Path

from fastapi import APIRouter, File, HTTPException, UploadFile

from ..config import get_settings
from ..extraction import extract_entities
from ..ingestion import load_document
from ..models.schemas import DocumentSummary
from ..store import registry

router = APIRouter(prefix="/api/documents", tags=["documents"])


@router.post("", response_model=list[DocumentSummary])
async def upload_documents(files: list[UploadFile] = File(...)) -> list[DocumentSummary]:
    """Upload one or more documents; ingest and register each."""
    settings = get_settings()
    settings.ensure_dirs()
    summaries: list[DocumentSummary] = []

    for upload in files:
        doc_id = uuid.uuid4().hex
        suffix = Path(upload.filename or "file").suffix
        dest = settings.uploads_path / f"{doc_id}{suffix}"
        data = await upload.read()
        dest.write_bytes(data)

        try:
            doc = load_document(
                dest,
                original_filename=upload.filename,
                content_type=upload.content_type,
                doc_id=doc_id,
            )
        except Exception as exc:
            raise HTTPException(
                status_code=422,
                detail=f"Failed to ingest '{upload.filename}': {exc}",
            ) from exc

        # Attach extracted entities for quick UI display.
        ent = extract_entities(doc.text)
        doc.metadata["entities"] = ent.model_dump()
        registry.add(doc)
        summaries.append(doc.summary())

    return summaries


@router.get("", response_model=list[DocumentSummary])
async def list_documents() -> list[DocumentSummary]:
    return registry.list_summaries()


@router.get("/{doc_id}")
async def get_document(doc_id: str):
    doc = registry.get(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    # Return a trimmed view (full text can be large).
    preview = doc.text[:4000]
    return {
        "summary": doc.summary(),
        "preview": preview,
        "entities": doc.metadata.get("entities", {}),
    }


@router.delete("/{doc_id}")
async def delete_document(doc_id: str):
    if not registry.delete(doc_id):
        raise HTTPException(status_code=404, detail="Document not found")
    return {"deleted": doc_id}
