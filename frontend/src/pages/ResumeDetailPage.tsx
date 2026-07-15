import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as tailoringService from "../services/tailoringService";
import * as exportService from "../services/exportService";
import { SuggestionReview } from "../features/tailoring/SuggestionReview";
import { TemplatePicker } from "../features/export/TemplatePicker";
import { ExportPanel } from "../features/export/ExportPanel";
import { PreviewRenderer } from "../templates/PreviewRenderer";
import { FormField } from "../components/FormField";
import { Button } from "../components/Button";

export default function ResumeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const resumeId = Number(id);
  const queryClient = useQueryClient();

  const { data: resume, isLoading } = useQuery({
    queryKey: ["tailored-resume", resumeId],
    queryFn: () => tailoringService.getTailoredResume(resumeId),
    enabled: Number.isFinite(resumeId),
  });

  const [name, setName] = useState("");
  const [company, setCompany] = useState("");
  const [jobTitle, setJobTitle] = useState("");

  useEffect(() => {
    if (resume) {
      setName(resume.name);
      setCompany(resume.company ?? "");
      setJobTitle(resume.jobTitle ?? "");
    }
  }, [resume]);

  const metadataMutation = useMutation({
    mutationFn: () => tailoringService.updateTailoredResume(resumeId, { name, company, jobTitle }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["tailored-resume", resumeId] }),
  });

  const [templateId, setTemplateId] = useState<number | null>(null);

  const { data: previewLayout } = useQuery({
    queryKey: ["resume-preview", resumeId, templateId],
    queryFn: () => exportService.getPreview(resumeId, templateId as number),
    enabled: Number.isFinite(resumeId) && templateId !== null,
  });

  if (isLoading || !resume) {
    return <p className="text-gray-500">Loading resume…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="bg-white rounded-lg shadow-sm p-6">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <FormField id="resume-name" label="Resume name" value={name} onChange={(e) => setName(e.target.value)} />
          <FormField
            id="resume-company"
            label="Company"
            value={company}
            onChange={(e) => setCompany(e.target.value)}
          />
          <FormField
            id="resume-job-title"
            label="Job title"
            value={jobTitle}
            onChange={(e) => setJobTitle(e.target.value)}
          />
        </div>
        <div className="mt-3">
          <Button onClick={() => metadataMutation.mutate()} disabled={metadataMutation.isPending}>
            Save details
          </Button>
        </div>
      </section>

      {resume.unmatchedRequirements.length > 0 && (
        <section className="bg-amber-50 border border-amber-200 rounded-lg p-4">
          <h2 className="text-sm font-semibold text-amber-900 mb-1">
            Requirements we couldn't match to your profile
          </h2>
          <ul className="list-disc list-inside text-sm text-amber-800">
            {resume.unmatchedRequirements.map((req) => (
              <li key={req}>{req}</li>
            ))}
          </ul>
          <p className="text-xs text-amber-700 mt-2">
            Consider adding related experience or skills to your master profile and regenerating
            this resume.
          </p>
        </section>
      )}

      <section className="bg-white rounded-lg shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">AI Suggestions</h2>
        <SuggestionReview resumeId={resume.id} suggestions={resume.suggestions} />
      </section>

      <section className="bg-white rounded-lg shadow-sm p-6 flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-gray-900">Template & Export</h2>
        <TemplatePicker selectedTemplateId={templateId} onSelect={setTemplateId} />
        <ExportPanel resumeId={resume.id} templateId={templateId} />
      </section>

      {previewLayout && <PreviewRenderer layout={previewLayout} />}
    </div>
  );
}
