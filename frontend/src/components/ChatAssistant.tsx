import { useState } from "react";
import { api } from "../api/client";
import type { ChatMessage, DocumentSummary } from "../types";

interface Props {
  documents: DocumentSummary[];
  selected: string[];
}

export function ChatAssistant({ documents, selected }: Props) {
  const [history, setHistory] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sources, setSources] = useState<string[]>([]);

  async function send() {
    const message = input.trim();
    if (!message || busy) return;
    setInput("");
    setError(null);
    const next = [...history, { role: "user" as const, content: message }];
    setHistory(next);
    setBusy(true);
    try {
      const res = await api.chat(message, selected, next);
      setHistory([...next, { role: "assistant", content: res.reply }]);
      setSources(res.sources);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="panel chat-panel">
      <header className="panel-header">
        <h1>AI Assistant</h1>
        <p className="muted">
          Ask about your documents, procedures and standards.{" "}
          {selected.length > 0
            ? `Scoped to ${selected.length} selected document(s).`
            : `Using all ${documents.length} document(s).`}
        </p>
      </header>

      <div className="chat-thread">
        {history.length === 0 && (
          <div className="empty">
            Try: “Summarise the commissioning prerequisites for P-101” or
            “Which API standards apply to this pump?”
          </div>
        )}
        {history.map((m, i) => (
          <div key={i} className={`chat-msg ${m.role}`}>
            <div className="chat-role">{m.role === "user" ? "You" : "Commissioning-GPT"}</div>
            <div className="chat-bubble">{m.content}</div>
          </div>
        ))}
        {busy && <div className="chat-msg assistant"><div className="chat-bubble">Thinking…</div></div>}
      </div>

      {sources.length > 0 && (
        <div className="muted small">Sources: {sources.join(", ")}</div>
      )}
      {error && <div className="banner error">{error}</div>}

      <div className="chat-input">
        <textarea
          value={input}
          rows={2}
          placeholder="Ask a question…"
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              send();
            }
          }}
        />
        <button className="primary" disabled={busy} onClick={send}>
          Send
        </button>
      </div>
    </section>
  );
}
