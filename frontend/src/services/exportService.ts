import { apiClient } from "./apiClient";
import type { ResumeContent } from "./tailoringService";

export interface LayoutDescriptor {
  style: string;
  accentColor: string;
  fontFamily: string;
  sectionOrder: string[];
}

export interface Template {
  id: number;
  name: string;
  layoutDescriptor: LayoutDescriptor;
}

export interface RenderedLayout {
  content: ResumeContent;
  layoutDescriptor: LayoutDescriptor;
}

export type ExportFormat = "pdf" | "docx" | "txt";

export function listTemplates() {
  return apiClient.get<Template[]>("/templates").then((res) => res.data);
}

export function getPreview(resumeId: number, templateId: number) {
  return apiClient
    .get<RenderedLayout>(`/tailored-resumes/${resumeId}/preview`, { params: { templateId } })
    .then((res) => res.data);
}

export function createExport(resumeId: number, templateId: number, format: ExportFormat) {
  return apiClient
    .post<{ exportId: number; downloadUrl: string }>(`/tailored-resumes/${resumeId}/export`, {
      templateId,
      format,
    })
    .then((res) => res.data);
}

/**
 * The download endpoint requires a bearer token, so it can't be reached via a plain browser
 * navigation (window.location) — that sends no Authorization header. Fetch it as a blob through
 * apiClient (which attaches the token) instead, then trigger a client-side file save.
 */
export async function downloadAndSave(downloadUrl: string): Promise<void> {
  const response = await apiClient.get<Blob>(downloadUrl, { responseType: "blob" });
  const filename = parseFilename(response.headers["content-disposition"]) ?? "resume";

  const objectUrl = window.URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(objectUrl);
}

function parseFilename(contentDisposition?: string): string | null {
  if (!contentDisposition) return null;
  const match = /filename="?([^"]+)"?/.exec(contentDisposition);
  return match ? match[1] : null;
}
