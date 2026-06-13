"""Commissioning-GPT FastAPI application entrypoint.

Run in development with:

    cd backend
    uvicorn app.main:app --reload
"""

from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import __version__
from .api import chat, documents, export, generate
from .config import get_settings
from .models.schemas import SOPType

settings = get_settings()
settings.ensure_dirs()

app = FastAPI(
    title="Commissioning-GPT API",
    description="AI-powered generator for engineering SOPs, method statements, "
    "commissioning/maintenance procedures, JSA and work instructions.",
    version=__version__,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(documents.router)
app.include_router(generate.router)
app.include_router(chat.router)
app.include_router(export.router)


@app.get("/api/health", tags=["meta"])
async def health() -> dict:
    """Liveness probe + capability flags for the UI."""
    return {
        "status": "ok",
        "version": __version__,
        "openai_configured": settings.has_openai,
        "model": settings.openai_model,
    }


@app.get("/api/sop-types", tags=["meta"])
async def sop_types() -> list[str]:
    """The list of procedure types the UI can offer."""
    return [t.value for t in SOPType]


# --------------------------------------------------------------------------- #
#  Built-in web UI (no Node / no build required)
#
#  Serving the static UI from the backend lets users run the whole app with
#  only a per-user Python install (no admin rights). The Electron/React desktop
#  app remains available separately under ../frontend. This mount is LAST so it
#  never shadows the /api/* routes above.
# --------------------------------------------------------------------------- #
_WEB_DIR = Path(__file__).resolve().parent / "web"
if _WEB_DIR.is_dir():
    app.mount("/", StaticFiles(directory=str(_WEB_DIR), html=True), name="web")
