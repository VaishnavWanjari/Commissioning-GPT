import { useEffect, useState } from "react";
import { api } from "./api/client";
import type { DocumentSummary, HealthResponse } from "./types";
import { UploadDropzone } from "./components/UploadDropzone";
import { DocumentLibrary } from "./components/DocumentLibrary";
import { SopGenerator } from "./components/SopGenerator";
import { ChatAssistant } from "./components/ChatAssistant";
import { StandardsLibrary } from "./components/StandardsLibrary";

type View = "upload" | "library" | "generator" | "chat" | "standards";

const NAV: { id: View; label: string; icon: string }[] = [
  { id: "upload", label: "Upload", icon: "⬆" },
  { id: "library", label: "Document Library", icon: "▤" },
  { id: "generator", label: "SOP Generator", icon: "⚙" },
  { id: "chat", label: "AI Assistant", icon: "✦" },
  { id: "standards", label: "Standards Library", icon: "§" },
];

export default function App() {
  const [view, setView] = useState<View>("upload");
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [backendError, setBackendError] = useState<string | null>(null);

  async function refreshDocuments() {
    try {
      const docs = await api.listDocuments();
      setDocuments(docs);
      setBackendError(null);
    } catch (err) {
      setBackendError((err as Error).message);
    }
  }

  useEffect(() => {
    api
      .health()
      .then((h) => {
        setHealth(h);
        setBackendError(null);
      })
      .catch((err) => setBackendError((err as Error).message));
    refreshDocuments();
  }, []);

  function toggleSelected(docId: string) {
    setSelected((prev) =>
      prev.includes(docId) ? prev.filter((d) => d !== docId) : [...prev, docId]
    );
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">CG</span>
          <div>
            <div className="brand-title">Commissioning-GPT</div>
            <div className="brand-sub">SOP Generator</div>
          </div>
        </div>

        <nav>
          {NAV.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${view === item.id ? "active" : ""}`}
              onClick={() => setView(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
              {item.id === "generator" && selected.length > 0 && (
                <span className="badge">{selected.length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <StatusDot ok={!!health?.openai_configured} />
          <span>
            {health
              ? health.openai_configured
                ? `AI ready · ${health.model}`
                : "AI key missing"
              : "Connecting…"}
          </span>
        </div>
      </aside>

      <main className="content">
        {backendError && (
          <div className="banner error">
            Backend offline or unreachable: {backendError}. Start it with{" "}
            <code>uvicorn app.main:app --reload</code> in <code>backend/</code>.
          </div>
        )}
        {health && !health.openai_configured && (
          <div className="banner warn">
            OPENAI_API_KEY is not configured — generation and chat are disabled.
            Set it in <code>backend/.env</code> (see <code>.env.example</code>).
          </div>
        )}

        {view === "upload" && (
          <UploadDropzone onUploaded={refreshDocuments} />
        )}
        {view === "library" && (
          <DocumentLibrary
            documents={documents}
            selected={selected}
            onToggle={toggleSelected}
            onRefresh={refreshDocuments}
          />
        )}
        {view === "generator" && (
          <SopGenerator
            documents={documents}
            selected={selected}
            onToggle={toggleSelected}
            aiReady={!!health?.openai_configured}
          />
        )}
        {view === "chat" && (
          <ChatAssistant documents={documents} selected={selected} />
        )}
        {view === "standards" && <StandardsLibrary />}
      </main>
    </div>
  );
}

function StatusDot({ ok }: { ok: boolean }) {
  return <span className={`status-dot ${ok ? "ok" : "off"}`} />;
}
