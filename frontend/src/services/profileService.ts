import { apiClient } from "./apiClient";

export interface ContactInfo {
  fullName: string;
  email: string;
  phone: string;
  location: string;
  links: string;
}

export interface EducationEntry {
  id: number;
  institution: string;
  credential: string | null;
  fieldOfStudy: string | null;
  startDate: string | null;
  endDate: string | null;
  description: string | null;
  displayOrder: number;
}

export type EducationEntryInput = Omit<EducationEntry, "id">;

export interface WorkExperienceEntry {
  id: number;
  company: string;
  title: string;
  startDate: string;
  endDate: string | null;
  currentRole: boolean;
  description: string;
  displayOrder: number;
}

export type WorkExperienceEntryInput = Omit<WorkExperienceEntry, "id" | "currentRole">;

export interface Skill {
  id: number;
  name: string;
  category: string | null;
}

export type SkillInput = Omit<Skill, "id">;

export interface Profile extends ContactInfo {
  id: number;
  educationEntries: EducationEntry[];
  workExperienceEntries: WorkExperienceEntry[];
  skills: Skill[];
}

export function getProfile() {
  return apiClient.get<Profile>("/profile").then((res) => res.data);
}

export function updateProfile(contactInfo: ContactInfo) {
  return apiClient.put<Profile>("/profile", contactInfo).then((res) => res.data);
}

export function addEducation(entry: EducationEntryInput) {
  return apiClient.post<EducationEntry>("/profile/education", entry).then((res) => res.data);
}

export function updateEducation(id: number, entry: EducationEntryInput) {
  return apiClient
    .put<EducationEntry>(`/profile/education/${id}`, entry)
    .then((res) => res.data);
}

export function deleteEducation(id: number) {
  return apiClient.delete(`/profile/education/${id}`);
}

export function addWorkExperience(entry: WorkExperienceEntryInput) {
  return apiClient
    .post<WorkExperienceEntry>("/profile/work-experience", entry)
    .then((res) => res.data);
}

export function updateWorkExperience(id: number, entry: WorkExperienceEntryInput) {
  return apiClient
    .put<WorkExperienceEntry>(`/profile/work-experience/${id}`, entry)
    .then((res) => res.data);
}

export function deleteWorkExperience(id: number) {
  return apiClient.delete(`/profile/work-experience/${id}`);
}

export function addSkill(skill: SkillInput) {
  return apiClient.post<Skill>("/profile/skills", skill).then((res) => res.data);
}

export function deleteSkill(id: number) {
  return apiClient.delete(`/profile/skills/${id}`);
}
