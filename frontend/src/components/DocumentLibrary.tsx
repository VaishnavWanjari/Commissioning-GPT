import { useMemo, useState } from "react";
import { api } from "../api/client";
import type { DocumentSummary } from "../types";

interface Props {
  documents: DocumentSummary[];
  selected: string[];
  onToggle: (docId: string) => void;
  onRefresh: () => void;
}

export function DocumentLibrary({ documents, selected, onToggle, onRefresh }: Props) {
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q
      ? documents.filter((d) => d.filename.toLowerCase().includes(q))
      : documents;
  }, [documents, query]);

  async function remove(docId: string) {
    await api.deleteDocument(docId);
    onRefresh();
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <h1>Document Library</h1>
        <p className="muted">
          {documents.length} document{documents.length === 1 ? "" : "s"} ·{" "}
          {selected.length} selected for generation
        </p>
      </header>

      <input
        className="search"
        placeholder="Search documents…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {filtered.length === 0 ? (
        <div className="empty">No documents. Upload some to get started.</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th></th>
              <th>File</th>
              <th>Pages</th>
              <th>Size</th>
              <th>OCR</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((d) => (
              <tr key={d.doc_id} className={selected.includes(d.doc_id) ? "sel" : ""}>
                <td>
                  <input
                    type="checkbox"
                    checked={selected.includes(d.doc_id)}
                    onChange={() => onToggle(d.doc_id)}
                  />
                </td>
                <td>{d.filename}</td>
                <td>{d.page_count || "—"}</td>
                <td>{d.char_count.toLocaleString()} ch</td>
                <td>{d.used_ocr ? "Yes" : "—"}</td>
                <td>
                  <button className="link danger" onClick={() => remove(d.doc_id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
