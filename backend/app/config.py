"""Application configuration.

Settings are loaded from environment variables and an optional ``.env`` file
(see ``.env.example``). The OpenAI API key is intentionally optional at import
time so the backend can boot, run tests and serve non-AI endpoints without a
key; AI features fail with a clear, actionable error only when actually used.
"""

from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

# backend/  (two parents up from app/config.py)
BACKEND_ROOT = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    """Typed application settings."""

    model_config = SettingsConfigDict(
        env_file=str(BACKEND_ROOT / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # --- OpenAI ---
    openai_api_key: str | None = Field(default=None, alias="OPENAI_API_KEY")
    openai_model: str = Field(default="gpt-4o", alias="OPENAI_MODEL")
    openai_embedding_model: str = Field(
        default="text-embedding-3-small", alias="OPENAI_EMBEDDING_MODEL"
    )

    # --- Server ---
    host: str = Field(default="127.0.0.1", alias="HOST")
    port: int = Field(default=8000, alias="PORT")
    cors_origins: str = Field(
        default="http://localhost:5173,http://127.0.0.1:5173",
        alias="CORS_ORIGINS",
    )

    # --- Storage ---
    data_dir: str = Field(default="data", alias="DATA_DIR")

    # --- RAG ---
    rag_chunk_size: int = Field(default=1200, alias="RAG_CHUNK_SIZE")
    rag_chunk_overlap: int = Field(default=150, alias="RAG_CHUNK_OVERLAP")
    rag_top_k: int = Field(default=8, alias="RAG_TOP_K")

    # ----- Derived helpers -----
    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    @property
    def data_path(self) -> Path:
        p = BACKEND_ROOT / self.data_dir
        return p if Path(self.data_dir).is_absolute() is False else Path(self.data_dir)

    @property
    def uploads_path(self) -> Path:
        return self.data_path / "uploads"

    @property
    def exports_path(self) -> Path:
        return self.data_path / "exports"

    @property
    def index_path(self) -> Path:
        return self.data_path / "index"

    def ensure_dirs(self) -> None:
        """Create the runtime data directories if they do not exist."""
        for path in (self.uploads_path, self.exports_path, self.index_path):
            path.mkdir(parents=True, exist_ok=True)

    @property
    def has_openai(self) -> bool:
        return bool(self.openai_api_key and self.openai_api_key.strip())


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Return a cached Settings instance."""
    return Settings()
