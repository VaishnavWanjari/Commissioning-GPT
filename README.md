# Commissioning-GPT — AI-Powered SOP Generator

A professional desktop application that ingests engineering documents (vendor &
OEM manuals, datasheets, P&IDs, cause-&-effect, Saudi Aramco SAEP/SAES/SAIC and
API/ASME/IEC/ISO/NFPA standards, scanned procedures, …) and **generates**
complete, audit-ready procedures:

- Standard Operating Procedures (SOP) · Method Statements
- Commissioning / Startup / Shutdown / Maintenance / Preservation procedures
- Job Safety Analysis (JSA) · Work / Inspection / Test / Emergency procedures
- Operational Guidelines · Troubleshooting Guides

Every generated document follows a full engineering structure (sections **A–P**):
Document Control, Purpose, References, Definitions, Responsibilities,
Prerequisites, Tools, Materials, Safety, JSA, step-by-step Procedure (with hold &
witness points), Checklists, Troubleshooting, Acceptance Criteria, Records and
Attachments — exportable to **Word (.docx)**, **PDF** and **Excel checklist**.

> Built for commissioning, operations, maintenance, construction, project and
> quality engineers on Oil & Gas, Petrochemical, Power and Refinery projects.

---

## Architecture

```
Electron + React (TypeScript)  ──HTTP──▶  Python FastAPI backend
        frontend/                              backend/
  drag-drop upload, library,            ingestion (PDF/Office/OCR) →
  SOP generator, AI chat,               extraction → RAG (FAISS) →
  standards library, export             OpenAI generation → docx/pdf/xlsx
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for how the code maps to the
12 specification modules.

---

## ⚡ Quick start on Windows — **no administrator needed**

The backend ships a **built-in web UI**, so you can run the whole app with only a
per-user Python install (no Node, no admin prompt):

1. Install Python 3.11+ from python.org — **uncheck "Install for all users"**
   (per-user, no admin) and **check "Add python.exe to PATH"**.
2. Double-click **`setup-windows.bat`** (one-time), then set `OPENAI_API_KEY` in
   `backend\.env`.
3. Double-click **`run-windows.bat`** — it opens <http://127.0.0.1:8000> in your
   browser.

Full details, plus the no-admin Electron desktop build, are in
[`docs/RUN_LOCAL_NO_ADMIN.md`](docs/RUN_LOCAL_NO_ADMIN.md).

---

## Prerequisites

- **Python 3.11+**
- **Node.js 18+** (for the Electron/React desktop UI)
- An **OpenAI API key** (for generation & chat)
- *Optional:* the `tesseract` binary for OCR of scanned PDFs/images
  (`apt-get install tesseract-ocr` / `choco install tesseract`)

---

## Backend — setup & run

```bash
cd backend
python -m venv .venv
# Windows: .venv\Scripts\activate    macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env        # then edit .env and set OPENAI_API_KEY
uvicorn app.main:app --reload
```

The API serves on `http://127.0.0.1:8000`. Interactive docs at `/docs`.

Key endpoints:

| Method | Path | Purpose |
|-------|------|---------|
| `GET`  | `/api/health` | Liveness + whether the OpenAI key is configured |
| `GET`  | `/api/sop-types` | List of selectable procedure types |
| `POST` | `/api/documents` | Upload & ingest one or more files |
| `GET`  | `/api/documents` | List the document library |
| `POST` | `/api/generate` | Generate a structured SOP from selected docs |
| `POST` | `/api/chat` | Ask the AI assistant about your documents |
| `POST` | `/api/export` | Download the SOP as `.docx` / `.pdf` / `.xlsx` |

### Run the tests

```bash
cd backend && pytest
```

Tests mock the LLM, so **no API key or network is required** for CI.

---

## Frontend — setup & run

```bash
cd frontend
npm install

# Fast UI iteration in a browser (expects the backend running on :8000):
npm run dev            # http://localhost:5173

# Run inside the Electron shell:
npm run dev:electron

# Build a Windows installer (.exe via electron-builder):
npm run build          # output in frontend/release/
```

The renderer talks to the backend over HTTP; override the base URL with
`VITE_API_BASE` if needed.

---

## Packaging for Windows

`npm run build` produces an NSIS installer under `frontend/release/`. The Python
backend is shipped/started separately for the MVP — point the app at it via the
`COMMISSIONING_GPT_BACKEND` environment variable (a command the Electron main
process will spawn), or run the backend as a service. Bundling the backend into
the installer (e.g. with PyInstaller) is a planned enhancement.

---

## Configuration

All backend settings live in `backend/.env` (see `backend/.env.example`):

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | — | **Required** for generation & chat |
| `OPENAI_MODEL` | `gpt-4o` | Chat/generation model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | RAG embeddings |
| `CORS_ORIGINS` | `http://localhost:5173,…` | Allowed UI origins |
| `RAG_CHUNK_SIZE` / `RAG_TOP_K` | `1200` / `8` | Retrieval tuning |

---

## Status & roadmap

This is a **working MVP**: upload → extract → retrieve → generate → export runs
end-to-end and produces a real, engineering-grade document. Scaffolded for the
next iterations: deeper P&ID/logic-diagram semantic parsing, a local-LLM
provider for fully offline use, a persistent vector database, standards content
packs, and backend bundling into the Windows installer.

## License

MIT
