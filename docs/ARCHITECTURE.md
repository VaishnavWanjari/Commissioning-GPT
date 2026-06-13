# Architecture & Specification Mapping

This document maps the implementation to the 12 modules of the project
specification and explains the main data flow.

## End-to-end data flow

```
            ┌────────────────────── frontend/ (Electron + React) ──────────────────────┐
            │  UploadDropzone → DocumentLibrary → SopGenerator → SopPreview/Export      │
            │                              ChatAssistant · StandardsLibrary             │
            └───────────────────────────────────┬──────────────────────────────────────┘
                                                 │ HTTP (src/api/client.ts)
            ┌────────────────────────────────────▼──────────────────────────────────────┐
            │                         backend/ (FastAPI — app/)                          │
            │                                                                            │
            │  api/documents ──▶ ingestion/ ──▶ extraction/ ──▶ store (registry)         │
            │                     (PDF/Office/OCR)   (tags, standards, limits)           │
            │                                                                            │
            │  api/generate ──▶ generator/ ──▶ rag/ (FAISS retrieval) ──▶ ai/ (OpenAI)   │
            │                        │                                                   │
            │                        └────────────▶ models/schemas.SOPDocument           │
            │                                                                            │
            │  api/export ──▶ exporters/ (docx · pdf · xlsx)                             │
            │  api/chat   ──▶ rag/ + ai/                                                 │
            └────────────────────────────────────────────────────────────────────────────┘
```

## Specification module → code mapping

| # | Spec module | Where it lives |
|---|-------------|----------------|
| 1 | Document Upload | `frontend/.../UploadDropzone.tsx`, `backend/app/api/documents.py` |
| 2 | Document Processing Engine | `backend/app/ingestion/` (PDF/Office/OCR), `backend/app/extraction/analyzer.py` |
| 3 | AI Knowledge Engine | `backend/app/ai/` (provider + prompts), `backend/app/rag/` (FAISS RAG) |
| 4 | SOP Generation Types | `SOPType` enum in `backend/app/models/schemas.py` (13 types) |
| 5 | Generated Document Structure (A–P) | `SOPDocument` in `schemas.py`; rendered by `backend/app/exporters/` |
| 6 | Engineering Intelligence | `extraction/analyzer.py` + the senior-engineer system prompt in `ai/prompts.py` |
| 7 | Standards Mapping | `analyzer.py` regex patterns (SAEP/SAES/SAIC/API/ASME/IEC/ISO/NFPA); UI `StandardsLibrary.tsx` |
| 8 | Output Formats | `exporters/docx_exporter.py`, `pdf_exporter.py`, `xlsx_exporter.py` |
| 9 | AI Chat Assistant | `backend/app/api/chat.py`, `frontend/.../ChatAssistant.tsx` |
| 10 | User Interface | `frontend/src/` (React components + `styles/index.css`) |
| 11 | Technology Stack | Electron + React + TS (frontend), FastAPI + Python (backend), OpenAI, FAISS, PyMuPDF, python-docx, ReportLab |
| 12 | Quality Requirements | Full A–P structure, backfill safeguards in `generator/sop_generator.py`, strict Pydantic validation |

## Key design decisions

- **Provider abstraction** (`ai/provider.py`): a `LLMProvider` Protocol decouples
  the app from OpenAI so a local-LLM provider can be dropped in later. Tests
  inject a fake provider — no key/network needed.
- **Structured generation**: the LLM is asked for JSON matching `SOPDocument`
  (`response_format=json_object`). Output is validated, and thin sections are
  backfilled deterministically (`generator/_backfill`) so exports are never empty.
- **Graceful degradation**: OCR, embeddings and RAG all fall back safely when an
  optional dependency or the API key is unavailable, keeping the pipeline usable.
- **RAG**: documents are chunked and embedded into an in-memory FAISS index; a
  deterministic keyword ranker is the offline fallback used in tests.

## Extension points (next iterations)

- `ai/provider.py` — add `OllamaProvider` / local-LLM for offline use.
- `ingestion/` — semantic P&ID / logic-diagram and cause-&-effect parsing.
- `rag/store.py` — swap the in-memory FAISS index for a persistent vector DB.
- `exporters/` — company/Aramco title-block templates and branded styles.
