import type { ReactNode } from "react";
import type { SOPDocument } from "../types";

/** Renders the generated SOP as a scrollable, document-style preview. */
export function SopPreview({ sop }: { sop: SOPDocument }) {
  const dc = sop.document_control;
  return (
    <article className="sop-preview">
      <h2>{dc.title}</h2>
      <div className="sop-type-tag">{sop.sop_type}</div>

      <Section letter="A" title="Document Control">
        <KeyVals
          rows={[
            ["Document Number", dc.document_number],
            ["Revision", dc.revision_number],
            ["Prepared By", dc.prepared_by],
            ["Checked By", dc.checked_by],
            ["Approved By", dc.approved_by],
            ["Date", dc.issue_date],
          ]}
        />
      </Section>

      <Section letter="B" title="Purpose">
        {dc && (
          <>
            <P label="Objective" value={sop.purpose.objective} />
            <P label="Scope" value={sop.purpose.scope} />
            <P label="Intended Use" value={sop.purpose.intended_use} />
          </>
        )}
      </Section>

      <Section letter="C" title="References">
        <Table
          headers={["Category", "Reference", "Title"]}
          rows={sop.references.map((r) => [r.category, r.reference, r.title])}
        />
      </Section>

      <Section letter="D" title="Definitions & Abbreviations">
        <Table
          headers={["Term", "Definition"]}
          rows={sop.definitions.map((d) => [d.term, d.definition])}
        />
      </Section>

      <Section letter="E" title="Responsibilities">
        <Table
          headers={["Role", "Responsibility"]}
          rows={sop.responsibilities.map((r) => [r.role, r.responsibility])}
        />
      </Section>

      <Section letter="F" title="Prerequisites">
        <Table
          headers={["Category", "Requirement", "Status"]}
          rows={sop.prerequisites.map((p) => [p.category, p.requirement, p.status])}
        />
      </Section>

      <Section letter="G" title="Tools & Equipment">
        <Table
          headers={["Sr", "Tool", "Qty", "Remarks"]}
          rows={sop.tools.map((t, i) => [String(t.sr_no || i + 1), t.tool, t.quantity, t.remarks])}
        />
      </Section>

      <Section letter="H" title="Materials & Consumables">
        <Table
          headers={["Sr", "Material", "Qty", "Remarks"]}
          rows={sop.materials.map((m, i) => [String(m.sr_no || i + 1), m.material, m.quantity, m.remarks])}
        />
      </Section>

      <Section letter="I" title="Safety Requirements">
        <Bullets label="PPE" items={sop.safety.ppe} />
        <Bullets label="Hazard Identification" items={sop.safety.hazard_identification} />
        <Bullets label="Risk Assessment" items={sop.safety.risk_assessment} />
        <Bullets label="Control Measures" items={sop.safety.control_measures} />
        <Bullets label="Environmental" items={sop.safety.environmental_requirements} />
        <Bullets label="Permits" items={sop.safety.permit_requirements} />
        <Bullets label="Emergency Actions" items={sop.safety.emergency_actions} />
      </Section>

      <Section letter="J" title="Job Safety Analysis (JSA)">
        <Table
          headers={["Activity", "Hazard", "Consequence", "Mitigation", "Risk"]}
          rows={sop.jsa.map((j) => [j.activity, j.hazard, j.consequence, j.mitigation, j.risk_rating])}
        />
      </Section>

      <Section letter="K" title="Procedure Execution Steps">
        <Table
          headers={["Step", "Activity Description", "Action By", "Verification", "Expected Result", "Hold/Witness"]}
          rows={sop.procedure_steps.map((s) => [
            s.step_no,
            s.activity_description,
            s.action_by,
            s.verification,
            s.expected_result,
            [s.hold_point ? "HOLD" : "", s.witness_point ? "WITNESS" : ""].filter(Boolean).join(" / "),
          ])}
        />
      </Section>

      <Section letter="L" title="Checklists">
        <Table
          headers={["#", "Item", "Acceptance"]}
          rows={sop.checklists.map((c, i) => [String(i + 1), c.item, c.acceptance])}
        />
      </Section>

      <Section letter="M" title="Troubleshooting Guide">
        <Table
          headers={["Problem", "Possible Cause", "Corrective Action"]}
          rows={sop.troubleshooting.map((t) => [t.problem, t.possible_cause, t.corrective_action])}
        />
      </Section>

      <Section letter="N" title="Acceptance Criteria">
        <Bullets items={sop.acceptance_criteria} />
      </Section>

      <Section letter="O" title="Records">
        <Table
          headers={["Activity", "Date", "Signature", "Remarks"]}
          rows={sop.records.map((r) => [r.activity, r.date, r.signature, r.remarks])}
        />
      </Section>

      <Section letter="P" title="Attachments">
        <Table
          headers={["Category", "Reference", "Description"]}
          rows={sop.attachments.map((a) => [a.category, a.reference, a.description])}
        />
      </Section>
    </article>
  );
}

function Section({ letter, title, children }: { letter: string; title: string; children: ReactNode }) {
  return (
    <section className="sop-section">
      <h3>
        <span className="sec-letter">{letter}</span> {title}
      </h3>
      {children}
    </section>
  );
}

function Table({ headers, rows }: { headers: string[]; rows: string[][] }) {
  if (rows.length === 0) return <p className="muted small">— none —</p>;
  return (
    <table className="preview-table">
      <thead>
        <tr>{headers.map((h) => <th key={h}>{h}</th>)}</tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i}>{row.map((c, j) => <td key={j}>{c}</td>)}</tr>
        ))}
      </tbody>
    </table>
  );
}

function Bullets({ items, label }: { items: string[]; label?: string }) {
  if (!items || items.length === 0) return null;
  return (
    <div className="bullets">
      {label && <strong>{label}:</strong>}
      <ul>{items.map((it, i) => <li key={i}>{it}</li>)}</ul>
    </div>
  );
}

function P({ label, value }: { label: string; value: string }) {
  if (!value) return null;
  return (
    <p>
      <strong>{label}:</strong> {value}
    </p>
  );
}

function KeyVals({ rows }: { rows: [string, string][] }) {
  return (
    <table className="preview-table">
      <tbody>
        {rows.map(([k, v]) => (
          <tr key={k}>
            <td style={{ width: "30%", fontWeight: 600 }}>{k}</td>
            <td>{v}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
