"""Render an SOPDocument to a polished Microsoft Word (.docx) file.

The layout mirrors a real commissioning deliverable: a title block / document
control table, numbered section headings (A–P), styled tables for every tabular
section, and a footer carrying the document number, revision and page number.
"""

from __future__ import annotations

from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor

from ..models.schemas import SOPDocument

_HEADER_FILL = "1F3864"   # dark blue
_ACCENT = RGBColor(0x1F, 0x38, 0x64)


# --------------------------------------------------------------------------- #
#  Low-level helpers
# --------------------------------------------------------------------------- #
def _shade_cell(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.makeelement(qn("w:shd"), {qn("w:fill"): fill, qn("w:val"): "clear"})
    tc_pr.append(shd)


def _style_header_row(row) -> None:
    for cell in row.cells:
        _shade_cell(cell, _HEADER_FILL)
        for para in cell.paragraphs:
            for run in para.runs:
                run.font.bold = True
                run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
                run.font.size = Pt(9)


def _add_table(doc: Document, headers: list[str], rows: list[list[str]]):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    try:
        table.style = "Light Grid Accent 1"
    except KeyError:  # style not present in default template
        table.style = "Table Grid"
    hdr = table.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = h
    _style_header_row(table.rows[0])
    for row in rows:
        cells = table.add_row().cells
        for i, _ in enumerate(headers):
            cells[i].text = str(row[i]) if i < len(row) else ""
            for para in cells[i].paragraphs:
                for run in para.runs:
                    run.font.size = Pt(9)
    doc.add_paragraph()
    return table


def _heading(doc: Document, letter: str, title: str) -> None:
    h = doc.add_heading(level=1)
    run = h.add_run(f"{letter}.  {title}")
    run.font.color.rgb = _ACCENT
    run.font.size = Pt(13)


def _bullets(doc: Document, items: list[str], label: str | None = None) -> None:
    if label:
        p = doc.add_paragraph()
        p.add_run(label).bold = True
    for item in items or []:
        doc.add_paragraph(str(item), style="List Bullet")


def _flag(step) -> str:
    flags = []
    if step.hold_point:
        flags.append("HOLD POINT")
    if step.witness_point:
        flags.append("WITNESS POINT")
    return " / ".join(flags)


# --------------------------------------------------------------------------- #
#  Title block & footer
# --------------------------------------------------------------------------- #
def _add_title_block(doc: Document, sop: SOPDocument) -> None:
    dc = sop.document_control
    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = title.add_run(dc.title or sop.sop_type.value)
    run.bold = True
    run.font.size = Pt(20)
    run.font.color.rgb = _ACCENT

    sub = doc.add_paragraph()
    sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    s = sub.add_run(sop.sop_type.value.upper())
    s.font.size = Pt(11)
    s.font.color.rgb = RGBColor(0x55, 0x55, 0x55)
    doc.add_paragraph()


def _add_footer(doc: Document, sop: SOPDocument) -> None:
    section = doc.sections[0]
    footer = section.footer
    para = footer.paragraphs[0]
    dc = sop.document_control
    para.text = f"Doc No: {dc.document_number}   |   Rev: {dc.revision_number}   |   {dc.title}"
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for run in para.runs:
        run.font.size = Pt(8)
        run.font.color.rgb = RGBColor(0x77, 0x77, 0x77)


# --------------------------------------------------------------------------- #
#  Main entry point
# --------------------------------------------------------------------------- #
def export_docx(sop: SOPDocument, out_path: str | Path) -> str:
    out_path = Path(out_path)
    doc = Document()

    # Base font.
    style = doc.styles["Normal"]
    style.font.name = "Calibri"
    style.font.size = Pt(10.5)

    _add_title_block(doc, sop)

    # --- A. Document Control ---
    dc = sop.document_control
    _heading(doc, "A", "Document Control")
    _add_table(
        doc,
        ["Field", "Value"],
        [
            ["Title", dc.title],
            ["Document Number", dc.document_number],
            ["Revision Number", dc.revision_number],
            ["Prepared By", dc.prepared_by],
            ["Checked By", dc.checked_by],
            ["Approved By", dc.approved_by],
            ["Date", dc.issue_date],
        ],
    )
    if dc.revision_history:
        p = doc.add_paragraph()
        p.add_run("Revision History").bold = True
        _add_table(
            doc,
            ["Rev", "Date", "Description", "Prepared By", "Approved By"],
            [[r.revision, r.date, r.description, r.prepared_by, r.approved_by]
             for r in dc.revision_history],
        )

    # --- B. Purpose ---
    _heading(doc, "B", "Purpose")
    for label, val in (
        ("Objective", sop.purpose.objective),
        ("Scope", sop.purpose.scope),
        ("Intended Use", sop.purpose.intended_use),
    ):
        if val:
            p = doc.add_paragraph()
            p.add_run(f"{label}: ").bold = True
            p.add_run(val)

    # --- C. References ---
    _heading(doc, "C", "References")
    _add_table(
        doc,
        ["Category", "Reference", "Title"],
        [[r.category, r.reference, r.title] for r in sop.references] or [["", "", ""]],
    )

    # --- D. Definitions & Abbreviations ---
    _heading(doc, "D", "Definitions and Abbreviations")
    _add_table(
        doc,
        ["Term", "Definition"],
        [[d.term, d.definition] for d in sop.definitions] or [["", ""]],
    )

    # --- E. Responsibilities ---
    _heading(doc, "E", "Responsibilities")
    _add_table(
        doc,
        ["Role", "Responsibility"],
        [[r.role, r.responsibility] for r in sop.responsibilities] or [["", ""]],
    )

    # --- F. Prerequisites ---
    _heading(doc, "F", "Prerequisites")
    _add_table(
        doc,
        ["Category", "Requirement", "Status"],
        [[p.category, p.requirement, p.status] for p in sop.prerequisites] or [["", "", ""]],
    )

    # --- G. Tools & Equipment ---
    _heading(doc, "G", "Tools and Equipment Required")
    _add_table(
        doc,
        ["Sr No", "Tool", "Quantity", "Remarks"],
        [[str(t.sr_no or i + 1), t.tool, t.quantity, t.remarks]
         for i, t in enumerate(sop.tools)] or [["", "", "", ""]],
    )

    # --- H. Materials & Consumables ---
    _heading(doc, "H", "Materials and Consumables")
    _add_table(
        doc,
        ["Sr No", "Material", "Quantity", "Remarks"],
        [[str(m.sr_no or i + 1), m.material, m.quantity, m.remarks]
         for i, m in enumerate(sop.materials)] or [["", "", "", ""]],
    )

    # --- I. Safety Requirements ---
    _heading(doc, "I", "Safety Requirements")
    sft = sop.safety
    _bullets(doc, sft.ppe, "PPE Requirements:")
    _bullets(doc, sft.hazard_identification, "Hazard Identification:")
    _bullets(doc, sft.risk_assessment, "Risk Assessment:")
    _bullets(doc, sft.control_measures, "Control Measures:")
    _bullets(doc, sft.environmental_requirements, "Environmental Requirements:")
    _bullets(doc, sft.permit_requirements, "Permit Requirements:")
    _bullets(doc, sft.emergency_actions, "Emergency Actions:")

    # --- J. Job Safety Analysis ---
    _heading(doc, "J", "Job Safety Analysis (JSA)")
    _add_table(
        doc,
        ["Activity", "Hazard", "Consequence", "Mitigation", "Risk Rating"],
        [[j.activity, j.hazard, j.consequence, j.mitigation, j.risk_rating]
         for j in sop.jsa] or [["", "", "", "", ""]],
    )

    # --- K. Procedure Execution Steps ---
    _heading(doc, "K", "Procedure Execution Steps")
    _add_table(
        doc,
        ["Step", "Activity Description", "Action By", "Verification", "Expected Result", "Hold/Witness"],
        [[s.step_no, s.activity_description, s.action_by, s.verification,
          s.expected_result, _flag(s)] for s in sop.procedure_steps]
        or [["", "", "", "", "", ""]],
    )

    # --- L. Checklists ---
    _heading(doc, "L", "Checklists")
    _add_table(
        doc,
        ["#", "Checklist Item", "Acceptance", "Done"],
        [[str(i + 1), c.item, c.acceptance, "☐"] for i, c in enumerate(sop.checklists)]
        or [["", "", "", ""]],
    )

    # --- M. Troubleshooting Guide ---
    _heading(doc, "M", "Troubleshooting Guide")
    _add_table(
        doc,
        ["Problem", "Possible Cause", "Corrective Action"],
        [[t.problem, t.possible_cause, t.corrective_action] for t in sop.troubleshooting]
        or [["", "", ""]],
    )

    # --- N. Acceptance Criteria ---
    _heading(doc, "N", "Acceptance Criteria")
    _bullets(doc, sop.acceptance_criteria)

    # --- O. Records ---
    _heading(doc, "O", "Records")
    _add_table(
        doc,
        ["Activity", "Date", "Signature", "Remarks"],
        [[r.activity, r.date, r.signature, r.remarks] for r in sop.records]
        or [["", "", "", ""]],
    )

    # --- P. Attachments ---
    _heading(doc, "P", "Attachments")
    _add_table(
        doc,
        ["Category", "Reference", "Description"],
        [[a.category, a.reference, a.description] for a in sop.attachments]
        or [["", "", ""]],
    )

    _add_footer(doc, sop)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(out_path))
    return str(out_path)
