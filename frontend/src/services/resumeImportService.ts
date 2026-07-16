import axios from "axios";
import { apiClient } from "./apiClient";

export interface ContactInfoCandidate {
  fullName: string | null;
  email: string | null;
  phone: string | null;
  location: string | null;
  links: string | null;
  fieldsNotExtracted: string[];
}

export interface WorkExperienceCandidate {
  company: string;
  title: string;
  startDate: string | null;
  endDate: string | null;
  description: string;
  lowConfidence: boolean;
}

export interface EducationCandidate {
  institution: string;
  credential: string | null;
  fieldOfStudy: string | null;
  startDate: string | null;
  endDate: string | null;
  description: string | null;
  lowConfidence: boolean;
}

export interface SkillCandidate {
  name: string;
  category: string | null;
  lowConfidence: boolean;
}

export interface ResumeImportResult {
  sourceFileName: string;
  contactInfo: ContactInfoCandidate;
  workExperience: WorkExperienceCandidate[];
  education: EducationCandidate[];
  skills: SkillCandidate[];
  warnings: string[];
}

export function uploadResume(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiClient
    .post<ResumeImportResult>("/profile/resume-import", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then((res) => res.data);
}

/** Backend errors carry a human-readable message in `{ error }` (GlobalExceptionHandler). */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.error === "string") {
    return error.response.data.error;
  }
  return fallback;
}
