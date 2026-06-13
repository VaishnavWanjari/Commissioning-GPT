// Typed client for the Commissioning-GPT FastAPI backend.

import type {
  ChatMessage,
  ChatResponse,
  DocumentSummary,
  ExportFormat,
  GenerateResponse,
  HealthResponse,
  SOPDocument,
} from "../types";

const API_BASE =
  (import.meta as any).env?.VITE_API_BASE ?? "http://127.0.0.1:8000";

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.detail ?? detail;
    } catch {
      /* ignore */
    }
    throw new Error(detail);
  }
  return res.json() as Promise<T>;
}

export const api = {
  async health(): Promise<HealthResponse> {
    return handle(await fetch(`${API_BASE}/api/health`));
  },

  async sopTypes(): Promise<string[]> {
    return handle(await fetch(`${API_BASE}/api/sop-types`));
  },

  async listDocuments(): Promise<DocumentSummary[]> {
    return handle(await fetch(`${API_BASE}/api/documents`));
  },

  async uploadDocuments(files: FileList | File[]): Promise<DocumentSummary[]> {
    const form = new FormData();
    Array.from(files).forEach((f) => form.append("files", f));
    return handle(
      await fetch(`${API_BASE}/api/documents`, { method: "POST", body: form })
    );
  },

  async deleteDocument(docId: string): Promise<void> {
    await handle(
      await fetch(`${API_BASE}/api/documents/${docId}`, { method: "DELETE" })
    );
  },

  async generate(payload: {
    sop_type: string;
    doc_ids: string[];
    equipment_name?: string;
    title?: string;
    document_number?: string;
    additional_instructions?: string;
  }): Promise<GenerateResponse> {
    return handle(
      await fetch(`${API_BASE}/api/generate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })
    );
  },

  async chat(
    message: string,
    docIds: string[],
    history: ChatMessage[]
  ): Promise<ChatResponse> {
    return handle(
      await fetch(`${API_BASE}/api/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message, doc_ids: docIds, history }),
      })
    );
  },

  async exportSop(sop: SOPDocument, format: ExportFormat): Promise<Blob> {
    const res = await fetch(`${API_BASE}/api/export`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sop, format }),
    });
    if (!res.ok) throw new Error(`Export failed: ${res.statusText}`);
    return res.blob();
  },
};

export { API_BASE };
