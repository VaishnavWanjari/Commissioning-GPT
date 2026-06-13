"""A small, in-memory FAISS vector store over document chunks.

The store is built on demand from a set of documents (cheap for the MVP's
document counts). When OpenAI embeddings are unavailable, ``build_context``
falls back to a deterministic keyword-overlap ranker so the rest of the
pipeline still functions (e.g. in tests).
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from ..config import get_settings
from ..models.schemas import ExtractedDocument
from .embeddings import embed_texts


@dataclass
class Chunk:
    doc_id: str
    filename: str
    text: str


@dataclass
class VectorStore:
    chunks: list[Chunk] = field(default_factory=list)
    _index: object | None = None
    _embeddings: list[list[float]] | None = None

    # --- building --------------------------------------------------------
    @classmethod
    def from_documents(cls, docs: list[ExtractedDocument]) -> "VectorStore":
        settings = get_settings()
        chunks = _chunk_documents(
            docs, settings.rag_chunk_size, settings.rag_chunk_overlap
        )
        store = cls(chunks=chunks)
        if settings.has_openai and chunks:
            store._build_faiss()
        return store

    def _build_faiss(self) -> None:
        import faiss
        import numpy as np

        vectors = embed_texts([c.text for c in self.chunks])
        arr = np.array(vectors, dtype="float32")
        faiss.normalize_L2(arr)
        index = faiss.IndexFlatIP(arr.shape[1])
        index.add(arr)
        self._index = index
        self._embeddings = vectors

    # --- search ----------------------------------------------------------
    def search(self, query: str, top_k: int | None = None) -> list[Chunk]:
        top_k = top_k or get_settings().rag_top_k
        if not self.chunks:
            return []
        if self._index is not None:
            return self._faiss_search(query, top_k)
        return _keyword_search(query, self.chunks, top_k)

    def _faiss_search(self, query: str, top_k: int) -> list[Chunk]:
        import faiss
        import numpy as np

        qvec = np.array(embed_texts([query]), dtype="float32")
        faiss.normalize_L2(qvec)
        _, idx = self._index.search(qvec, min(top_k, len(self.chunks)))
        return [self.chunks[i] for i in idx[0] if 0 <= i < len(self.chunks)]


def _chunk_documents(
    docs: list[ExtractedDocument], chunk_size: int, overlap: int
) -> list[Chunk]:
    """Split documents into overlapping chunks using LangChain's splitter."""
    try:
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size, chunk_overlap=overlap
        )
        split = splitter.split_text
    except ImportError:  # pragma: no cover - fallback splitter
        def split(text: str) -> list[str]:
            step = max(chunk_size - overlap, 1)
            return [text[i : i + chunk_size] for i in range(0, len(text), step)]

    chunks: list[Chunk] = []
    for doc in docs:
        for piece in split(doc.text or ""):
            piece = piece.strip()
            if piece:
                chunks.append(Chunk(doc_id=doc.doc_id, filename=doc.filename, text=piece))
    return chunks


_WORD = re.compile(r"[a-zA-Z0-9]+")


def _keyword_search(query: str, chunks: list[Chunk], top_k: int) -> list[Chunk]:
    """Deterministic fallback ranking by query/keyword overlap."""
    q_terms = {w.lower() for w in _WORD.findall(query)}
    if not q_terms:
        return chunks[:top_k]

    scored: list[tuple[int, int, Chunk]] = []
    for i, chunk in enumerate(chunks):
        terms = {w.lower() for w in _WORD.findall(chunk.text)}
        scored.append((len(q_terms & terms), -i, chunk))
    scored.sort(reverse=True)
    return [c for score, _, c in scored[:top_k] if score > 0] or chunks[:top_k]


def build_context(
    docs: list[ExtractedDocument], query: str, top_k: int | None = None
) -> tuple[str, list[str]]:
    """Return (context_text, source_filenames) for a query over ``docs``."""
    store = VectorStore.from_documents(docs)
    hits = store.search(query, top_k=top_k)
    sources: list[str] = []
    blocks: list[str] = []
    for hit in hits:
        if hit.filename not in sources:
            sources.append(hit.filename)
        blocks.append(f"[Source: {hit.filename}]\n{hit.text}")
    return "\n\n---\n\n".join(blocks), sources
