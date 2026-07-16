import { useRef, useState, type ChangeEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import * as resumeImportService from "../../services/resumeImportService";
import type { ResumeImportResult } from "../../services/resumeImportService";
import { Button } from "../../components/Button";

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

export function ResumeUploadButton({
  onImported,
}: {
  onImported: (result: ResumeImportResult) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const uploadMutation = useMutation({
    mutationFn: (file: File) => resumeImportService.uploadResume(file),
    onSuccess: (result) => {
      setError(null);
      onImported(result);
    },
    onError: (err) => {
      setError(
        resumeImportService.extractErrorMessage(
          err,
          "We couldn't process this resume. Please try again or enter your details manually.",
        ),
      );
    },
  });

  function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";
    if (!file) {
      return;
    }
    if (file.type !== "application/pdf") {
      setError("Only PDF files are supported.");
      return;
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setError("File exceeds the maximum upload size of 10MB.");
      return;
    }
    setError(null);
    uploadMutation.mutate(file);
  }

  return (
    <div className="flex flex-col gap-2">
      <input
        ref={inputRef}
        type="file"
        accept="application/pdf"
        className="hidden"
        onChange={handleFileSelected}
      />
      <Button
        variant="secondary"
        onClick={() => inputRef.current?.click()}
        disabled={uploadMutation.isPending}
      >
        {uploadMutation.isPending ? "Reading resume…" : "Upload resume to prefill profile"}
      </Button>
      {error && <span className="text-sm text-red-600">{error}</span>}
    </div>
  );
}
