import { useRef, useState } from "react";
import { api } from "../api/client";
import type { DocumentSummary } from "../types";

const ACCEPTED =
  ".pdf,.docx,.xlsx,.xls,.pptx,.txt,.md,.csv,.log,.png,.jpg,.jpeg,.tif,.tiff,.bmp";

export function UploadDropzone({ onUploaded }: { onUploaded: () => void }) {
  const [dragging, setDragging] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [recent, setRecent] = useState<DocumentSummary[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  async function upload(files: FileList | File[]) {
    if (!files || (files as FileList).length === 0) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api.uploadDocuments(files);
      setRecent(result);
      onUploaded();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <h1>Upload Documents</h1>
        <p className="muted">
          Vendor &amp; OEM manuals, datasheets, P&amp;IDs, cause &amp; effect,
          Aramco / API / ASME / IEC / ISO standards, scanned procedures and more.
        </p>
      </header>

      <div
        className={`dropzone ${dragging ? "drag" : ""}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          upload(e.dataTransfer.files);
        }}
        onClick={() => inputRef.current?.click()}
      >
        <div className="dropzone-icon">⬆</div>
        <div className="dropzone-title">
          {busy ? "Uploading & extracting…" : "Drag & drop files here"}
        </div>
        <div className="muted">or click to browse</div>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept={ACCEPTED}
          style={{ display: "none" }}
          onChange={(e) => e.target.files && upload(e.target.files)}
        />
      </div>

      {error && <div className="banner error">{error}</div>}

      {recent.length > 0 && (
        <div className="card">
          <h3>Just ingested</h3>
          <ul className="list">
            {recent.map((d) => (
              <li key={d.doc_id}>
                <strong>{d.filename}</strong>
                <span className="muted">
                  {" "}
                  · {d.page_count || "—"} pages · {d.char_count.toLocaleString()}{" "}
                  chars{d.used_ocr ? " · OCR" : ""}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
