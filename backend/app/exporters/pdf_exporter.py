"""Render an SOPDocument to PDF using ReportLab."""

from __future__ import annotations

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

from ..models.schemas import SOPDocument

_ACCENT = colors.HexColor("#1F3864")


def _styles():
    base = getSampleStyleSheet()
    base.add(ParagraphStyle("SectionHeading", parent=base["Heading2"],
                            textColor=_ACCENT, spaceBefore=10, spaceAfter=6))
    base.add(ParagraphStyle("Cell", parent=base["BodyText"], fontSize=8, leading=10))
    base.add(ParagraphStyle("DocTitle", parent=base["Title"], textColor=_ACCENT))
    return base


def _table(headers, rows, styles, col_widths=None):
    cell = styles["Cell"]
    data = [[Paragraph(f"<b>{h}</b>", cell) for h in headers]]
    for row in rows or [[""] * len(headers)]:
        data.append([Paragraph(str(c).replace("\n", "<br/>"), cell) for c in row])
    table = Table(data, colWidths=col_widths, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), _ACCENT),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.grey),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F2F5FA")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 2),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
    ]))
    return table


def export_pdf(sop: SOPDocument, out_path: str | Path) -> str:
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    styles = _styles()
    story: list = []
    H = lambda letter, title: story.append(Paragraph(f"{letter}. {title}", styles["SectionHeading"]))  # noqa: E731
    body = styles["BodyText"]

    dc = sop.document_control
    story.append(Paragraph(dc.title or sop.sop_type.value, styles["DocTitle"]))
    story.append(Paragraph(sop.sop_type.value.upper(), styles["Normal"]))
    story.append(Spacer(1, 8))

    H("A", "Document Control")
    story.append(_table(
        ["Field", "Value"],
        [["Title", dc.title], ["Document Number", dc.document_number],
         ["Revision", dc.revision_number], ["Prepared By", dc.prepared_by],
         ["Checked By", dc.checked_by], ["Approved By", dc.approved_by],
         ["Date", dc.issue_date]],
        styles, col_widths=[45 * mm, 120 * mm]))
    story.append(Spacer(1, 6))

    H("B", "Purpose")
    for label, val in (("Objective", sop.purpose.objective), ("Scope", sop.purpose.scope),
                       ("Intended Use", sop.purpose.intended_use)):
        if val:
            story.append(Paragraph(f"<b>{label}:</b> {val}", body))
    story.append(Spacer(1, 6))

    H("C", "References")
    story.append(_table(["Category", "Reference", "Title"],
                        [[r.category, r.reference, r.title] for r in sop.references], styles))
    H("D", "Definitions and Abbreviations")
    story.append(_table(["Term", "Definition"],
                        [[d.term, d.definition] for d in sop.definitions], styles))
    H("E", "Responsibilities")
    story.append(_table(["Role", "Responsibility"],
                        [[r.role, r.responsibility] for r in sop.responsibilities], styles))
    H("F", "Prerequisites")
    story.append(_table(["Category", "Requirement", "Status"],
                        [[p.category, p.requirement, p.status] for p in sop.prerequisites], styles))
    H("G", "Tools and Equipment Required")
    story.append(_table(["Sr", "Tool", "Qty", "Remarks"],
                        [[t.sr_no or i + 1, t.tool, t.quantity, t.remarks]
                         for i, t in enumerate(sop.tools)], styles))
    H("H", "Materials and Consumables")
    story.append(_table(["Sr", "Material", "Qty", "Remarks"],
                        [[m.sr_no or i + 1, m.material, m.quantity, m.remarks]
                         for i, m in enumerate(sop.materials)], styles))

    H("I", "Safety Requirements")
    sft = sop.safety
    for label, items in (("PPE", sft.ppe), ("Hazard Identification", sft.hazard_identification),
                         ("Risk Assessment", sft.risk_assessment),
                         ("Control Measures", sft.control_measures),
                         ("Environmental", sft.environmental_requirements),
                         ("Permits", sft.permit_requirements),
                         ("Emergency Actions", sft.emergency_actions)):
        if items:
            story.append(Paragraph(f"<b>{label}:</b>", body))
            for it in items:
                story.append(Paragraph(f"• {it}", styles["Cell"]))

    H("J", "Job Safety Analysis (JSA)")
    story.append(_table(["Activity", "Hazard", "Consequence", "Mitigation", "Risk"],
                        [[j.activity, j.hazard, j.consequence, j.mitigation, j.risk_rating]
                         for j in sop.jsa], styles))
    H("K", "Procedure Execution Steps")
    story.append(_table(["Step", "Activity Description", "Action By", "Verification", "Hold/Witness"],
                        [[s.step_no, s.activity_description, s.action_by, s.verification,
                          ("HP " if s.hold_point else "") + ("WP" if s.witness_point else "")]
                         for s in sop.procedure_steps], styles,
                        col_widths=[15 * mm, 75 * mm, 25 * mm, 35 * mm, 20 * mm]))
    H("L", "Checklists")
    story.append(_table(["#", "Checklist Item", "Acceptance"],
                        [[i + 1, c.item, c.acceptance] for i, c in enumerate(sop.checklists)], styles))
    H("M", "Troubleshooting Guide")
    story.append(_table(["Problem", "Possible Cause", "Corrective Action"],
                        [[t.problem, t.possible_cause, t.corrective_action]
                         for t in sop.troubleshooting], styles))
    H("N", "Acceptance Criteria")
    for ac in sop.acceptance_criteria:
        story.append(Paragraph(f"• {ac}", body))
    H("O", "Records")
    story.append(_table(["Activity", "Date", "Signature", "Remarks"],
                        [[r.activity, r.date, r.signature, r.remarks] for r in sop.records], styles))
    H("P", "Attachments")
    story.append(_table(["Category", "Reference", "Description"],
                        [[a.category, a.reference, a.description] for a in sop.attachments], styles))

    def _footer(canvas, doc_):
        canvas.saveState()
        canvas.setFont("Helvetica", 7)
        canvas.setFillColor(colors.grey)
        canvas.drawCentredString(
            A4[0] / 2, 10 * mm,
            f"Doc No: {dc.document_number}  |  Rev: {dc.revision_number}  |  Page {doc_.page}",
        )
        canvas.restoreState()

    pdf = SimpleDocTemplate(str(out_path), pagesize=A4,
                            leftMargin=18 * mm, rightMargin=18 * mm,
                            topMargin=18 * mm, bottomMargin=18 * mm)
    pdf.build(story, onFirstPage=_footer, onLaterPages=_footer)
    return str(out_path)
