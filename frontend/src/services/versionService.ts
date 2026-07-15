import { apiClient } from "./apiClient";
import type { ResumeStatus, TailoredResume } from "./tailoringService";

export interface TailoredResumeSummary {
  id: number;
  name: string;
  company: string | null;
  jobTitle: string | null;
  status: ResumeStatus;
  createdAt: string;
}

export type DiffLineType = "UNCHANGED" | "ADDED" | "REMOVED";

export interface DiffLine {
  type: DiffLineType;
  text: string;
}

export interface SectionDiff {
  section: string;
  lines: DiffLine[];
}

export interface CompareResult {
  left: TailoredResume;
  right: TailoredResume;
  diff: SectionDiff[];
}

export function listTailoredResumes(company?: string) {
  return apiClient
    .get<TailoredResumeSummary[]>("/tailored-resumes", { params: company ? { company } : {} })
    .then((res) => res.data);
}

export function compareTailoredResumes(leftId: number, rightId: number) {
  return apiClient
    .get<CompareResult>("/tailored-resumes/compare", { params: { leftId, rightId } })
    .then((res) => res.data);
}

export function cloneTailoredResume(id: number) {
  return apiClient.post<TailoredResume>(`/tailored-resumes/${id}/clone`).then((res) => res.data);
}

export function regenerateTailoredResume(id: number) {
  return apiClient
    .post<TailoredResume>(`/tailored-resumes/${id}/regenerate`)
    .then((res) => res.data);
}
