"""AI chat assistant endpoint (RAG over uploaded documents)."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException

from ..ai.prompts import CHAT_SYSTEM_PROMPT, build_chat_prompt
from ..ai.provider import get_provider
from ..config import get_settings
from ..models.schemas import ChatRequest, ChatResponse
from ..rag import build_context
from ..store import registry

router = APIRouter(prefix="/api/chat", tags=["chat"])


@router.post("", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    if not get_settings().has_openai:
        raise HTTPException(
            status_code=503,
            detail="OPENAI_API_KEY is not configured. Set it in backend/.env to use chat.",
        )

    docs = registry.get_many(req.doc_ids) if req.doc_ids else registry.get_many(
        [s.doc_id for s in registry.list_summaries()]
    )

    context, sources = ("", [])
    if docs:
        try:
            context, sources = build_context(docs, req.message)
        except Exception:
            context = "\n\n".join(d.text[:2000] for d in docs[:3])

    try:
        provider = get_provider()
        reply = provider.complete_text(
            CHAT_SYSTEM_PROMPT, build_chat_prompt(req.message, context)
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Chat failed: {exc}") from exc

    return ChatResponse(reply=reply, sources=sources)
