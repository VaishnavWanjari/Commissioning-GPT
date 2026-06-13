import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { DocumentSummary, ExportFormat, GenerateResponse } from "../types";
import { SopPreview } from "./SopPreview";

interface Props {
  documents: DocumentSummary[];
  selected: string[];
  onToggle: (docId: string) => void;
  aiReady: boolean;
}

const FALLBACK_TYPES = [
  "Standard Operating Procedure",
  "Method Statement",
  "Commissioning Procedure",
  "Startup Procedure",
  "Shutdown Procedure",
  "Maintenance Procedure",
  "Preservation Procedure",
  "Operational Guideline",
  "Work Instruction",
  "Inspection Procedure",
  "Test Procedure",
  "Emergency Procedure",
  "Troubleshooting Guide",
];

export function SopGenerator({ documents, selected, onToggle, aiReady }: Props) {
  const [types, setTypes] = useState<string[]>(FALLBACK_TYPES);
  const [sopType, setSopType] = useState(FALLBACK_TYPES[2]);
  const [equipment, setEquipment] = useState("");
  const [title, setTitle] = useState("");
  const [docNumber, setDocNumber] = useState("");
  const [instructions, setInstructions] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<GenerateResponse | null>(null);

  useEffect(() => {
    api.sopTypes().then(setTypes).catch(() => setTypes(FALLBACK_TYPES));
  }, []);

  async function generate() {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const res = await api.generate({
        sop_type: sopType,
        doc_ids: selected,
        equipment_name: equipment || undefined,
        title: title || undefined,
        document_number: docNumber || undefined,
        additional_instructions: instructions || undefined,
      });
      setResult(res);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function doExport(format: ExportFormat) {
    if (!result) return;
    const blob = await api.exportSop(result.sop, format);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    const base =
      result.sop.document_control.document_number ||
      result.sop.document_control.title ||
      "SOP";
    a.download = `${base.replace(/[^A-Za-z0-9._-]+/g, "_")}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <h1>SOP Generator</h1>
        <p className="muted">
          Select source documents and a procedure type to generate a complete,
          audit-ready document (sections A–P).
        </p>
      </header>

      <div className="generator-grid">
        <div className="card">
          <h3>1 · Procedure type</h3>
          <select value={sopType} onChange={(e) => setSopType(e.target.value)}>
            {types.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>

          <h3 style={{ marginTop: 18 }}>2 · Details (optional)</h3>
          <label>Primary equipment / system</label>
          <input
            value={equipment}
            onChange={(e) => setEquipment(e.target.value)}
            placeholder="e.g. Centrifugal Pump P-101"
          />
          <label>Title</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Auto-generated if blank" />
          <label>Document number</label>
          <input
            value={docNumber}
            onChange={(e) => setDocNumber(e.target.value)}
            placeholder="Auto-generated if blank"
          />
          <label>Additional instructions</label>
          <textarea
            value={instructions}
            rows={3}
            onChange={(e) => setInstructions(e.target.value)}
            placeholder="e.g. Emphasise nitrogen purging and PTW requirements"
          />
        </div>

        <div className="card">
          <h3>3 · Source documents ({selected.length} selected)</h3>
          {documents.length === 0 ? (
            <div className="empty small">Upload documents first.</div>
          ) : (
            <ul className="check-list">
              {documents.map((d) => (
                <li key={d.doc_id}>
                  <label>
                    <input
                      type="checkbox"
                      checked={selected.includes(d.doc_id)}
                      onChange={() => onToggle(d.doc_id)}
                    />
                    {d.filename}
                  </label>
                </li>
              ))}
            </ul>
          )}

          <button
            className="primary big"
            disabled={busy || !aiReady}
            onClick={generate}
            title={aiReady ? "" : "Configure OPENAI_API_KEY to enable generation"}
          >
            {busy ? "Generating…" : "Generate SOP"}
          </button>
          {!aiReady && (
            <p className="muted small">
              Generation requires <code>OPENAI_API_KEY</code> in the backend.
            </p>
          )}
          {selected.length === 0 && aiReady && (
            <p className="muted small">
              Tip: with no documents selected, a best-practice template is generated.
            </p>
          )}
        </div>
      </div>

      {error && <div className="banner error">{error}</div>}

      {result && (
        <>
          <div className="export-bar">
            <span className="muted">Export:</span>
            <button onClick={() => doExport("docx")}>Word (.docx)</button>
            <button onClick={() => doExport("pdf")}>PDF</button>
            <button onClick={() => doExport("xlsx")}>Excel Checklist</button>
            {result.warnings.length > 0 && (
              <span className="muted small">· {result.warnings.length} note(s)</span>
            )}
          </div>
          <SopPreview sop={result.sop} />
        </>
      )}
    </section>
  );
}
