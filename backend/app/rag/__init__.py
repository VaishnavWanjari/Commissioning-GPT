"""Retrieval-Augmented Generation: chunk, embed and retrieve context."""

from .store import VectorStore, build_context

__all__ = ["VectorStore", "build_context"]
