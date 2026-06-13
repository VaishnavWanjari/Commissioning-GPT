"""OpenAI embedding wrapper used by the vector store."""

from __future__ import annotations

from ..config import get_settings


def embed_texts(texts: list[str]) -> list[list[float]]:
    """Embed a batch of texts with the configured OpenAI embedding model.

    Raises a clear RuntimeError when no API key is configured.
    """
    settings = get_settings()
    if not settings.has_openai:
        raise RuntimeError(
            "OPENAI_API_KEY is not set. Embeddings (used for retrieval) require "
            "an OpenAI key. Set it in backend/.env (see .env.example)."
        )
    if not texts:
        return []

    from openai import OpenAI

    client = OpenAI(api_key=settings.openai_api_key)
    resp = client.embeddings.create(
        model=settings.openai_embedding_model,
        input=texts,
    )
    return [item.embedding for item in resp.data]
