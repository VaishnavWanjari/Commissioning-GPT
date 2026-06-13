// Types mirroring the backend Pydantic models (app/models/schemas.py).

export interface DocumentSummary {
  doc_id: string;
  filename: string;
  content_type?: string | null;
  page_count: number;
  char_count: number;
  used_ocr: boolean;
}

export interface RevisionHistoryRow {
  revision: string;
  date: string;
  description: string;
  prepared_by: string;
  approved_by: string;
}

export interface DocumentControl {
  title: string;
  document_number: string;
  revision_number: string;
  prepared_by: string;
  checked_by: string;
  approved_by: string;
  issue_date: string;
  revision_history: RevisionHistoryRow[];
}

export interface Purpose {
  objective: string;
  scope: string;
  intended_use: string;
}

export interface ReferenceRow { category: string; reference: string; title: string; }
export interface DefinitionRow { term: string; definition: string; }
export interface ResponsibilityRow { role: string; responsibility: string; }
export interface PrerequisiteRow { category: string; requirement: string; status: string; }
export interface ToolRow { sr_no: number; tool: string; quantity: string; remarks: string; }
export interface MaterialRow { sr_no: number; material: string; quantity: string; remarks: string; }

export interface SafetyRequirements {
  ppe: string[];
  hazard_identification: string[];
  risk_assessment: string[];
  control_measures: string[];
  environmental_requirements: string[];
  permit_requirements: string[];
  emergency_actions: string[];
}

export interface JSARow {
  activity: string; hazard: string; consequence: string;
  mitigation: string; risk_rating: string;
}

export interface ProcedureStep {
  step_no: string;
  activity_description: string;
  action_by: string;
  verification: string;
  hold_point: boolean;
  witness_point: boolean;
  expected_result: string;
}

export interface ChecklistItem { item: string; acceptance: string; checked: boolean; }
export interface TroubleshootingRow { problem: string; possible_cause: string; corrective_action: string; }
export interface RecordRow { activity: string; date: string; signature: string; remarks: string; }
export interface AttachmentRow { category: string; reference: string; description: string; }

export interface SOPDocument {
  sop_type: string;
  document_control: DocumentControl;
  purpose: Purpose;
  references: ReferenceRow[];
  definitions: DefinitionRow[];
  responsibilities: ResponsibilityRow[];
  prerequisites: PrerequisiteRow[];
  tools: ToolRow[];
  materials: MaterialRow[];
  safety: SafetyRequirements;
  jsa: JSARow[];
  procedure_steps: ProcedureStep[];
  checklists: ChecklistItem[];
  troubleshooting: TroubleshootingRow[];
  acceptance_criteria: string[];
  records: RecordRow[];
  attachments: AttachmentRow[];
}

export interface GenerateResponse {
  sop: SOPDocument;
  used_documents: string[];
  warnings: string[];
}

export interface ChatMessage { role: "user" | "assistant"; content: string; }
export interface ChatResponse { reply: string; sources: string[]; }

export interface HealthResponse {
  status: string;
  version: string;
  openai_configured: boolean;
  model: string;
}

export type ExportFormat = "docx" | "pdf" | "xlsx";
