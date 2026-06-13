// A reference catalogue of the standards bodies the generator recognises and
// maps into procedures (spec section 7). This is a static, offline reference;
// uploaded standards are detected automatically during ingestion.

const STANDARDS: { body: string; scope: string; examples: string }[] = [
  { body: "Saudi Aramco SAEP", scope: "Engineering Procedures", examples: "SAEP-302, SAEP-1160" },
  { body: "Saudi Aramco SAES", scope: "Engineering Standards", examples: "SAES-J-902, SAES-P-100" },
  { body: "Saudi Aramco SAIC", scope: "Inspection Checklists", examples: "SAIC-J-2001" },
  { body: "API", scope: "Petroleum & equipment", examples: "API 610, API 650, API 2218" },
  { body: "ASME", scope: "Pressure & mechanical", examples: "ASME B31.3, ASME BPVC VIII" },
  { body: "IEC", scope: "Electrical & instrumentation", examples: "IEC 61511, IEC 60534" },
  { body: "ISO", scope: "Quality & management", examples: "ISO 9001, ISO 14001, ISO 45001" },
  { body: "NFPA", scope: "Fire & life safety", examples: "NFPA 70, NFPA 72" },
];

export function StandardsLibrary() {
  return (
    <section className="panel">
      <header className="panel-header">
        <h1>Standards Library</h1>
        <p className="muted">
          The generator recognises and maps references from these bodies. Upload
          the actual standard documents to have specific clauses cited.
        </p>
      </header>

      <table className="data-table">
        <thead>
          <tr>
            <th>Standard Body</th>
            <th>Scope</th>
            <th>Examples</th>
          </tr>
        </thead>
        <tbody>
          {STANDARDS.map((s) => (
            <tr key={s.body}>
              <td><strong>{s.body}</strong></td>
              <td>{s.scope}</td>
              <td className="muted">{s.examples}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
