import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import * as tailoringService from "../services/tailoringService";
import { TextAreaField } from "../components/FormField";
import { Button } from "../components/Button";

export default function TailoringPage() {
  const navigate = useNavigate();
  const [jobDescriptionText, setJobDescriptionText] = useState("");
  const [error, setError] = useState<string | null>(null);

  const tailorMutation = useMutation({
    mutationFn: async (rawText: string) => {
      const jobDescription = await tailoringService.submitJobDescription(rawText);
      return tailoringService.tailorResume(jobDescription.id);
    },
    onSuccess: (resume) => {
      navigate(`/resumes/${resume.id}`);
    },
    onError: () => {
      setError("We couldn't tailor a resume from that job description. Please try again.");
    },
  });

  function handleSubmit() {
    setError(null);
    if (!jobDescriptionText.trim()) {
      setError("Paste a job description first.");
      return;
    }
    tailorMutation.mutate(jobDescriptionText);
  }

  return (
    <div className="flex flex-col gap-4 max-w-2xl">
      <h1 className="text-xl font-semibold text-gray-900">Tailor a Resume</h1>
      <p className="text-sm text-gray-600">
        Paste the job description you're applying for. We'll analyze it against your master
        profile and suggest resume changes you can review before saving.
      </p>
      <TextAreaField
        id="job-description"
        label="Job description"
        rows={14}
        value={jobDescriptionText}
        onChange={(e) => setJobDescriptionText(e.target.value)}
      />
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div>
        <Button onClick={handleSubmit} disabled={tailorMutation.isPending}>
          {tailorMutation.isPending ? "Tailoring..." : "Tailor my resume"}
        </Button>
      </div>
    </div>
  );
}
