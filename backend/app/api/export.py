"""Export a generated SOP to .docx / PDF / Excel and stream it back."""

from __future__ import annotations

import re
import uuid

from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse

from ..config import get_settings
from ..exporters import export_sop
from ..models.schemas import ExportFormat, ExportRequest

router = APIRouter(prefix="/api/export", tags=["export"])

_MEDIA_TYPES = {
    ExportFormat.DOCX: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ExportFormat.PDF: "application/pdf",
    ExportFormat.XLSX: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
}


def _safe_name(text: str) -> str:
    name = re.sub(r"[^A-Za-z0-9._-]+", "_", text).strip("_")
    return name or "SOP"


@router.post("")
async def export(req: ExportRequest):
    settings = get_settings()
    settings.ensure_dirs()

    dc = req.sop.document_control
    base = _safe_name(dc.document_number or dc.title or req.sop.sop_type.value)
    filename = f"{base}_{uuid.uuid4().hex[:6]}.{req.format.value}"
    out_path = settings.exports_path / filename

    try:
        export_sop(req.sop, req.format.value, out_path)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Export failed: {exc}") from exc

    download_name = f"{base}.{req.format.value}"
    return FileResponse(
        path=str(out_path),
        media_type=_MEDIA_TYPES.get(req.format, "application/octet-stream"),
        filename=download_name,
    )
