"""LLM provider abstraction.

A thin interface (:class:`LLMProvider`) decouples the rest of the app from any
specific vendor. The MVP ships an :class:`OpenAIProvider`; a local-LLM provider
(Ollama / llama.cpp) can be added later without touching callers. Tests inject
a fake provider, so no network or API key is needed in CI.
"""

from __future__ import annotations

import json
from typing import Protocol

from ..config import get_settings


class LLMProvider(Protocol):
    """Minimal contract every provider must satisfy."""

    def complete_json(self, system: str, user: str) -> dict:
        """Return a JSON object parsed from the model's response."""
        ...

    def complete_text(self, system: str, user: str) -> str:
        """Return a free-form text completion."""
        ...


class OpenAIProvider:
    """OpenAI Chat Completions implementation."""

    def __init__(self, api_key: str | None = None, model: str | None = None) -> None:
        settings = get_settings()
        self._api_key = api_key or settings.openai_api_key
        self._model = model or settings.openai_model
        if not self._api_key:
            raise RuntimeError(
                "OPENAI_API_KEY is not set. AI generation/chat require an OpenAI "
                "key. Set it in backend/.env (see .env.example)."
            )
        from openai import OpenAI

        self._client = OpenAI(api_key=self._api_key)

    def complete_json(self, system: str, user: str) -> dict:
        resp = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            response_format={"type": "json_object"},
            temperature=0.2,
        )
        content = resp.choices[0].message.content or "{}"
        return json.loads(content)

    def complete_text(self, system: str, user: str) -> str:
        resp = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            temperature=0.3,
        )
        return resp.choices[0].message.content or ""


def get_provider() -> LLMProvider:
    """Return the configured provider (OpenAI for the MVP)."""
    return OpenAIProvider()
