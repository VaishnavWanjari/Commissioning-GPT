// Commissioning-GPT — built-in web UI (vanilla JS, no build step).
// Served by the FastAPI backend at "/"; talks to the same-origin API.

const API = ""; // same origin
const state = {
  documents: [],
  selected: new Set(),
  sopTypes: [],
  aiReady: false,
  chat: [],
  lastSop: null,
};

const $ = (id) => document.getElementById(id);
const esc = (s) =>
  String(s ?? "").replace(/[&<>"]/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c])
  );

async function jget(path) {
  const r = await fetch(API + path);
  if (!r.ok) throw new Error((await safeDetail(r)) || r.statusText);
  return r.json();
}
async function jpost(path, body) {
  const r = await fetch(API + path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error((await safeDetail(r)) || r.statusText);
  return r.json();
}
async function safeDetail(r) {
  try {
    return (await r.json()).detail;
  } catch {
    return null;
  }
}

function banner(kind, html) {
  $("banner-area").innerHTML = html
    ? `<div class="banner ${kind}">${html}</div>`
    : "";
}

// ---------------- Navigation ----------------
document.querySelectorAll(".nav-item").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    const view = btn.dataset.view;
    document.querySelectorAll(".view").forEach((v) => (v.hidden = true));
    $("view-" + view).hidden = false;
    if (view === "generator") renderGeneratorDocs();
    if (view === "chat") updateChatScope();
  });
});

// ---------------- Health / status ----------------
async function loadHealth() {
  try {
    const h = await jget("/api/health");
    state.aiReady = !!h.openai_configured;
    $("status-dot").classList.toggle("ok", state.aiReady);
    $("status-text").textContent = state.aiReady
      ? `AI ready · ${h.model}`
      : "AI key missing";
    if (!state.aiReady) {
      banner(
        "warn",
        "OPENAI_API_KEY is not configured — generation and chat are disabled. " +
          "Set it in <code>backend/.env</code> (see <code>.env.example</code>), then restart."
      );
    } else {
      banner("", "");
    }
  } catch (e) {
    $("status-text").textContent = "Backend offline";
    banner("error", "Cannot reach backend API: " + esc(e.message));
  }
  updateGenerateButton();
}

async function loadSopTypes() {
  try {
    state.sopTypes = await jget("/api/sop-types");
  } catch {
    state.sopTypes = ["Commissioning Procedure", "Standard Operating Procedure"];
  }
  $("sop-type").innerHTML = state.sopTypes
    .map((t) => `<option value="${esc(t)}">${esc(t)}</option>`)
    .join("");
  const idx = state.sopTypes.indexOf("Commissioning Procedure");
  if (idx >= 0) $("sop-type").selectedIndex = idx;
}

// ---------------- Documents ----------------
async function loadDocuments() {
  try {
    state.documents = await jget("/api/documents");
  } catch {
    state.documents = [];
  }
  renderLibrary();
  renderGeneratorDocs();
  updateSelBadge();
}

function updateSelBadge() {
  const n = state.selected.size;
  const b = $("sel-badge");
  b.hidden = n === 0;
  b.textContent = n;
}

function renderLibrary() {
  $("lib-count").textContent =
    `${state.documents.length} document${state.documents.length === 1 ? "" : "s"} · ${state.selected.size} selected`;
  const q = ($("lib-search").value || "").toLowerCase();
  const docs = q
    ? state.documents.filter((d) => d.filename.toLowerCase().includes(q))
    : state.documents;
  if (docs.length === 0) {
    $("lib-table").innerHTML = `<div class="empty">No documents. Upload some to get started.</div>`;
    return;
  }
  $("lib-table").innerHTML = `<table class="data-table"><thead><tr>
      <th></th><th>File</th><th>Pages</th><th>Size</th><th>OCR</th><th></th>
    </tr></thead><tbody>${docs
      .map(
        (d) => `<tr class="${state.selected.has(d.doc_id) ? "sel" : ""}">
        <td><input type="checkbox" data-doc="${d.doc_id}" ${state.selected.has(d.doc_id) ? "checked" : ""}></td>
        <td>${esc(d.filename)}</td>
        <td>${d.page_count || "—"}</td>
        <td>${d.char_count.toLocaleString()} ch</td>
        <td>${d.used_ocr ? "Yes" : "—"}</td>
        <td><button class="link danger" data-del="${d.doc_id}">Delete</button></td>
      </tr>`
      )
      .join("")}</tbody></table>`;

  $("lib-table").querySelectorAll("input[data-doc]").forEach((cb) =>
    cb.addEventListener("change", () => toggleSelect(cb.dataset.doc))
  );
  $("lib-table").querySelectorAll("button[data-del]").forEach((b) =>
    b.addEventListener("click", async () => {
      await fetch(API + "/api/documents/" + b.dataset.del, { method: "DELETE" });
      state.selected.delete(b.dataset.del);
      loadDocuments();
    })
  );
}

function toggleSelect(id) {
  if (state.selected.has(id)) state.selected.delete(id);
  else state.selected.add(id);
  renderLibrary();
  renderGeneratorDocs();
  updateSelBadge();
}

function renderGeneratorDocs() {
  const host = $("g-doclist");
  if (!host) return;
  $("g-sel-count").textContent = state.selected.size;
  if (state.documents.length === 0) {
    host.innerHTML = `<div class="empty" style="padding:14px">Upload documents first.</div>`;
    return;
  }
  host.innerHTML = state.documents
    .map(
      (d) => `<label><input type="checkbox" data-gdoc="${d.doc_id}" ${
        state.selected.has(d.doc_id) ? "checked" : ""
      }> ${esc(d.filename)}</label>`
    )
    .join("");
  host.querySelectorAll("input[data-gdoc]").forEach((cb) =>
    cb.addEventListener("change", () => toggleSelect(cb.dataset.gdoc))
  );
}

// ---------------- Upload ----------------
function wireUpload() {
  const dz = $("dropzone");
  const input = $("file-input");
  dz.addEventListener("click", () => input.click());
  input.addEventListener("change", () => input.files && upload(input.files));
  ["dragover"].forEach((ev) =>
    dz.addEventListener(ev, (e) => {
      e.preventDefault();
      dz.classList.add("drag");
    })
  );
  dz.addEventListener("dragleave", () => dz.classList.remove("drag"));
  dz.addEventListener("drop", (e) => {
    e.preventDefault();
    dz.classList.remove("drag");
    upload(e.dataTransfer.files);
  });
}

async function upload(files) {
  if (!files || files.length === 0) return;
  $("drop-title").textContent = "Uploading & extracting…";
  const form = new FormData();
  Array.from(files).forEach((f) => form.append("files", f));
  try {
    const result = await fetch(API + "/api/documents", { method: "POST", body: form });
    if (!result.ok) throw new Error((await safeDetail(result)) || result.statusText);
    const docs = await result.json();
    $("upload-recent").innerHTML = `<div class="card"><h3>Just ingested</h3><ul class="list">${docs
      .map(
        (d) =>
          `<li><strong>${esc(d.filename)}</strong> <span class="muted">· ${
            d.page_count || "—"
          } pages · ${d.char_count.toLocaleString()} chars${d.used_ocr ? " · OCR" : ""}</span></li>`
      )
      .join("")}</ul></div>`;
    await loadDocuments();
  } catch (e) {
    $("upload-recent").innerHTML = `<div class="banner error">${esc(e.message)}</div>`;
  } finally {
    $("drop-title").textContent = "Drag & drop files here";
  }
}

// ---------------- Generate ----------------
function updateGenerateButton() {
  const btn = $("btn-generate");
  if (!btn) return;
  btn.disabled = !state.aiReady;
  $("g-hint").innerHTML = state.aiReady
    ? state.selected.size === 0
      ? "Tip: with no documents selected, a best-practice template is generated."
      : ""
    : "Generation requires <code>OPENAI_API_KEY</code> in the backend.";
}

async function generate() {
  const btn = $("btn-generate");
  btn.disabled = true;
  btn.textContent = "Generating…";
  $("g-result").innerHTML = "";
  try {
    const payload = {
      sop_type: $("sop-type").value,
      doc_ids: Array.from(state.selected),
      equipment_name: $("g-equipment").value || undefined,
      title: $("g-title").value || undefined,
      document_number: $("g-docnum").value || undefined,
      additional_instructions: $("g-instructions").value || undefined,
    };
    const res = await jpost("/api/generate", payload);
    state.lastSop = res.sop;
    renderResult(res);
  } catch (e) {
    $("g-result").innerHTML = `<div class="banner error">${esc(e.message)}</div>`;
  } finally {
    btn.textContent = "Generate SOP";
    btn.disabled = !state.aiReady;
  }
}

function renderResult(res) {
  const bar = `<div class="export-bar"><span class="muted">Export:</span>
    <button data-fmt="docx">Word (.docx)</button>
    <button data-fmt="pdf">PDF</button>
    <button data-fmt="xlsx">Excel Checklist</button>
    ${res.warnings.length ? `<span class="muted small">· ${res.warnings.length} note(s)</span>` : ""}
  </div>`;
  $("g-result").innerHTML = bar + renderPreview(res.sop);
  $("g-result")
    .querySelectorAll("button[data-fmt]")
    .forEach((b) => b.addEventListener("click", () => exportSop(b.dataset.fmt)));
}

async function exportSop(fmt) {
  if (!state.lastSop) return;
  try {
    const r = await fetch(API + "/api/export", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sop: state.lastSop, format: fmt }),
    });
    if (!r.ok) throw new Error(r.statusText);
    const blob = await r.blob();
    const dc = state.lastSop.document_control;
    const base = (dc.document_number || dc.title || "SOP").replace(/[^A-Za-z0-9._-]+/g, "_");
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `${base}.${fmt}`;
    a.click();
    URL.revokeObjectURL(a.href);
  } catch (e) {
    banner("error", "Export failed: " + esc(e.message));
  }
}

// ---------------- SOP preview rendering ----------------
function tbl(headers, rows) {
  if (!rows.length) return `<p class="muted small">— none —</p>`;
  return `<table class="preview-table"><thead><tr>${headers
    .map((h) => `<th>${esc(h)}</th>`)
    .join("")}</tr></thead><tbody>${rows
    .map((r) => `<tr>${r.map((c) => `<td>${esc(c)}</td>`).join("")}</tr>`)
    .join("")}</tbody></table>`;
}
function sec(letter, title, inner) {
  return `<section class="sop-section"><h3><span class="sec-letter">${letter}</span> ${esc(
    title
  )}</h3>${inner}</section>`;
}
function bullets(label, items) {
  if (!items || !items.length) return "";
  return `<div class="bullets">${label ? `<strong>${esc(label)}:</strong>` : ""}<ul>${items
    .map((i) => `<li>${esc(i)}</li>`)
    .join("")}</ul></div>`;
}

function renderPreview(sop) {
  const dc = sop.document_control;
  const flag = (s) =>
    [s.hold_point ? "HOLD" : "", s.witness_point ? "WITNESS" : ""].filter(Boolean).join(" / ");
  return `<article class="sop-preview">
    <h2>${esc(dc.title)}</h2><div class="sop-type-tag">${esc(sop.sop_type)}</div>
    ${sec("A", "Document Control", tbl(["Field", "Value"], [
      ["Document Number", dc.document_number], ["Revision", dc.revision_number],
      ["Prepared By", dc.prepared_by], ["Checked By", dc.checked_by],
      ["Approved By", dc.approved_by], ["Date", dc.issue_date],
    ]))}
    ${sec("B", "Purpose",
      `<p><strong>Objective:</strong> ${esc(sop.purpose.objective)}</p>
       <p><strong>Scope:</strong> ${esc(sop.purpose.scope)}</p>
       <p><strong>Intended Use:</strong> ${esc(sop.purpose.intended_use)}</p>`)}
    ${sec("C", "References", tbl(["Category", "Reference", "Title"],
      sop.references.map((r) => [r.category, r.reference, r.title])))}
    ${sec("D", "Definitions & Abbreviations", tbl(["Term", "Definition"],
      sop.definitions.map((d) => [d.term, d.definition])))}
    ${sec("E", "Responsibilities", tbl(["Role", "Responsibility"],
      sop.responsibilities.map((r) => [r.role, r.responsibility])))}
    ${sec("F", "Prerequisites", tbl(["Category", "Requirement", "Status"],
      sop.prerequisites.map((p) => [p.category, p.requirement, p.status])))}
    ${sec("G", "Tools & Equipment", tbl(["Sr", "Tool", "Qty", "Remarks"],
      sop.tools.map((t, i) => [t.sr_no || i + 1, t.tool, t.quantity, t.remarks])))}
    ${sec("H", "Materials & Consumables", tbl(["Sr", "Material", "Qty", "Remarks"],
      sop.materials.map((m, i) => [m.sr_no || i + 1, m.material, m.quantity, m.remarks])))}
    ${sec("I", "Safety Requirements",
      bullets("PPE", sop.safety.ppe) + bullets("Hazard Identification", sop.safety.hazard_identification) +
      bullets("Risk Assessment", sop.safety.risk_assessment) + bullets("Control Measures", sop.safety.control_measures) +
      bullets("Environmental", sop.safety.environmental_requirements) + bullets("Permits", sop.safety.permit_requirements) +
      bullets("Emergency Actions", sop.safety.emergency_actions))}
    ${sec("J", "Job Safety Analysis (JSA)", tbl(["Activity", "Hazard", "Consequence", "Mitigation", "Risk"],
      sop.jsa.map((j) => [j.activity, j.hazard, j.consequence, j.mitigation, j.risk_rating])))}
    ${sec("K", "Procedure Execution Steps", tbl(
      ["Step", "Activity Description", "Action By", "Verification", "Expected Result", "Hold/Witness"],
      sop.procedure_steps.map((s) => [s.step_no, s.activity_description, s.action_by, s.verification, s.expected_result, flag(s)])))}
    ${sec("L", "Checklists", tbl(["#", "Item", "Acceptance"],
      sop.checklists.map((c, i) => [i + 1, c.item, c.acceptance])))}
    ${sec("M", "Troubleshooting Guide", tbl(["Problem", "Possible Cause", "Corrective Action"],
      sop.troubleshooting.map((t) => [t.problem, t.possible_cause, t.corrective_action])))}
    ${sec("N", "Acceptance Criteria", bullets("", sop.acceptance_criteria))}
    ${sec("O", "Records", tbl(["Activity", "Date", "Signature", "Remarks"],
      sop.records.map((r) => [r.activity, r.date, r.signature, r.remarks])))}
    ${sec("P", "Attachments", tbl(["Category", "Reference", "Description"],
      sop.attachments.map((a) => [a.category, a.reference, a.description])))}
  </article>`;
}

// ---------------- Chat ----------------
function updateChatScope() {
  $("chat-scope").textContent =
    state.selected.size > 0
      ? `Scoped to ${state.selected.size} selected document(s).`
      : `Using all ${state.documents.length} document(s).`;
}

function renderChat() {
  const host = $("chat-thread");
  if (state.chat.length === 0) {
    host.innerHTML = `<div class="empty">Try: “Summarise the commissioning prerequisites for P-101”.</div>`;
    return;
  }
  host.innerHTML = state.chat
    .map(
      (m) => `<div class="chat-msg ${m.role}"><div class="chat-role">${
        m.role === "user" ? "You" : "Commissioning-GPT"
      }</div><div class="chat-bubble">${esc(m.content)}</div></div>`
    )
    .join("");
  host.scrollTop = host.scrollHeight;
}

async function sendChat() {
  const input = $("chat-input");
  const msg = input.value.trim();
  if (!msg) return;
  input.value = "";
  state.chat.push({ role: "user", content: msg });
  renderChat();
  try {
    const res = await jpost("/api/chat", {
      message: msg,
      doc_ids: Array.from(state.selected),
      history: state.chat,
    });
    state.chat.push({ role: "assistant", content: res.reply });
    $("chat-sources").textContent = res.sources.length ? "Sources: " + res.sources.join(", ") : "";
    renderChat();
  } catch (e) {
    state.chat.push({ role: "assistant", content: "Error: " + e.message });
    renderChat();
  }
}

// ---------------- Standards ----------------
const STANDARDS = [
  ["Saudi Aramco SAEP", "Engineering Procedures", "SAEP-302, SAEP-1160"],
  ["Saudi Aramco SAES", "Engineering Standards", "SAES-J-902, SAES-P-100"],
  ["Saudi Aramco SAIC", "Inspection Checklists", "SAIC-J-2001"],
  ["API", "Petroleum & equipment", "API 610, API 650, API 2218"],
  ["ASME", "Pressure & mechanical", "ASME B31.3, ASME BPVC VIII"],
  ["IEC", "Electrical & instrumentation", "IEC 61511, IEC 60534"],
  ["ISO", "Quality & management", "ISO 9001, ISO 14001, ISO 45001"],
  ["NFPA", "Fire & life safety", "NFPA 70, NFPA 72"],
];
function renderStandards() {
  $("standards-table").innerHTML = `<table class="data-table"><thead><tr>
    <th>Standard Body</th><th>Scope</th><th>Examples</th></tr></thead><tbody>${STANDARDS.map(
    (s) => `<tr><td><strong>${esc(s[0])}</strong></td><td>${esc(s[1])}</td><td class="muted">${esc(
      s[2]
    )}</td></tr>`
  ).join("")}</tbody></table>`;
}

// ---------------- Init ----------------
function wire() {
  wireUpload();
  $("lib-search").addEventListener("input", renderLibrary);
  $("btn-generate").addEventListener("click", generate);
  $("sop-type").addEventListener("change", () => {});
  $("btn-chat-send").addEventListener("click", sendChat);
  $("chat-input").addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendChat();
    }
  });
}

(async function init() {
  wire();
  renderStandards();
  await loadHealth();
  await loadSopTypes();
  await loadDocuments();
})();
