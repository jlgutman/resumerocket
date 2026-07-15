import { apiClient } from "./apiClient";

export interface ContactInfoSnapshot {
  fullName: string | null;
  email: string | null;
  phone: string | null;
  location: string | null;
  links: string | null;
}

export interface ExperienceItem {
  company: string;
  title: string;
  startDate: string | null;
  endDate: string | null;
  currentRole: boolean;
  bullets: string[];
}

export interface EducationItem {
  institution: string;
  credential: string | null;
  fieldOfStudy: string | null;
  startDate: string | null;
  endDate: string | null;
  description: string | null;
}

export interface ResumeContent {
  contactInfo: ContactInfoSnapshot;
  summary: string;
  experience: ExperienceItem[];
  education: EducationItem[];
  skills: string[];
}

export type SuggestionType = "EMPHASIS" | "BULLET_REWRITE" | "SKILL_HIGHLIGHT";
export type ReviewState = "PENDING" | "ACCEPTED" | "REJECTED" | "EDITED";

export interface AiSuggestion {
  id: number;
  targetSection: string;
  suggestionType: SuggestionType;
  originalText: string | null;
  suggestedText: string;
  finalText: string | null;
  reviewState: ReviewState;
}

export type ResumeStatus = "DRAFT" | "FINALIZED";

export interface TailoredResume {
  id: number;
  jobDescriptionId: number | null;
  name: string;
  company: string | null;
  jobTitle: string | null;
  status: ResumeStatus;
  content: ResumeContent;
  suggestions: AiSuggestion[];
  unmatchedRequirements: string[];
  createdAt: string;
  clonedFromId: number | null;
  regeneratedFromId: number | null;
}

export function submitJobDescription(rawText: string) {
  return apiClient
    .post<{ id: number; extractedRequirements: string[] }>("/job-descriptions", { rawText })
    .then((res) => res.data);
}

export function tailorResume(jobDescriptionId: number) {
  return apiClient
    .post<TailoredResume>(`/job-descriptions/${jobDescriptionId}/tailor`)
    .then((res) => res.data);
}

export function getTailoredResume(id: number) {
  return apiClient.get<TailoredResume>(`/tailored-resumes/${id}`).then((res) => res.data);
}

export function updateTailoredResume(
  id: number,
  update: Partial<Pick<TailoredResume, "name" | "company" | "jobTitle" | "status">>,
) {
  return apiClient
    .patch<TailoredResume>(`/tailored-resumes/${id}`, update)
    .then((res) => res.data);
}

export function resolveSuggestion(
  resumeId: number,
  suggestionId: number,
  reviewState: Exclude<ReviewState, "PENDING">,
  finalText?: string,
) {
  return apiClient
    .patch<AiSuggestion>(`/tailored-resumes/${resumeId}/suggestions/${suggestionId}`, {
      reviewState,
      finalText,
    })
    .then((res) => res.data);
}
