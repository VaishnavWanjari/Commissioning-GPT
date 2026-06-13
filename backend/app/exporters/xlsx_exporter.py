"""Render an SOPDocument to an Excel workbook.

The workbook is organized as a usable field deliverable: a cover sheet plus a
checklist sheet, a procedure-steps sheet and a JSA sheet — the tabular sections
engineers most often want in Excel for sign-off.
"""

from __future__ import annotations

from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

from ..models.schemas import SOPDocument

_HEADER_FILL = PatternFill("solid", fgColor="1F3864")
_HEADER_FONT = Font(bold=True, color="FFFFFF")
_WRAP = Alignment(wrap_text=True, vertical="top")


def _write_sheet(ws, headers: list[str], rows: list[list], widths: list[int] | None = None):
    ws.append(headers)
    for col, _ in enumerate(headers, start=1):
        cell = ws.cell(row=1, column=col)
        cell.fill = _HEADER_FILL
        cell.font = _HEADER_FONT
        cell.alignment = Alignment(horizontal="center", vertical="center")
        if widths and col <= len(widths):
            ws.column_dimensions[get_column_letter(col)].width = widths[col - 1]
    for row in rows:
        ws.append([str(c) for c in row])
    for row in ws.iter_rows(min_row=2):
        for cell in row:
            cell.alignment = _WRAP
    ws.freeze_panes = "A2"


def export_xlsx(sop: SOPDocument, out_path: str | Path) -> str:
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    wb = Workbook()
    dc = sop.document_control

    # Cover sheet.
    cover = wb.active
    cover.title = "Document Control"
    _write_sheet(cover, ["Field", "Value"], [
        ["Title", dc.title], ["Document Number", dc.document_number],
        ["Procedure Type", sop.sop_type.value],
        ["Revision", dc.revision_number], ["Prepared By", dc.prepared_by],
        ["Checked By", dc.checked_by], ["Approved By", dc.approved_by],
        ["Date", dc.issue_date],
    ], widths=[24, 60])

    _write_sheet(wb.create_sheet("Prerequisites"),
                 ["Category", "Requirement", "Status"],
                 [[p.category, p.requirement, p.status] for p in sop.prerequisites],
                 widths=[24, 60, 16])

    _write_sheet(wb.create_sheet("Checklist"),
                 ["#", "Checklist Item", "Acceptance", "Done", "Remarks"],
                 [[i + 1, c.item, c.acceptance, "", ""] for i, c in enumerate(sop.checklists)],
                 widths=[6, 55, 35, 8, 25])

    _write_sheet(wb.create_sheet("Procedure Steps"),
                 ["Step", "Activity Description", "Action By", "Verification",
                  "Expected Result", "Hold Point", "Witness Point"],
                 [[s.step_no, s.activity_description, s.action_by, s.verification,
                   s.expected_result, "Y" if s.hold_point else "",
                   "Y" if s.witness_point else ""] for s in sop.procedure_steps],
                 widths=[8, 55, 16, 30, 30, 11, 13])

    _write_sheet(wb.create_sheet("JSA"),
                 ["Activity", "Hazard", "Consequence", "Mitigation", "Risk Rating"],
                 [[j.activity, j.hazard, j.consequence, j.mitigation, j.risk_rating]
                  for j in sop.jsa], widths=[30, 30, 30, 35, 12])

    _write_sheet(wb.create_sheet("Acceptance Criteria"),
                 ["#", "Acceptance Criterion"],
                 [[i + 1, ac] for i, ac in enumerate(sop.acceptance_criteria)],
                 widths=[6, 90])

    wb.save(str(out_path))
    return str(out_path)
