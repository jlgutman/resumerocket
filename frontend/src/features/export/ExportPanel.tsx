import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import * as exportService from "../../services/exportService";
import type { ExportFormat } from "../../services/exportService";
import { Button } from "../../components/Button";

const FORMATS: { value: ExportFormat; label: string }[] = [
  { value: "pdf", label: "PDF" },
  { value: "docx", label: "DOCX" },
  { value: "txt", label: "ATS Plain Text" },
];

export function ExportPanel({ resumeId, templateId }: { resumeId: number; templateId: number | null }) {
  const [format, setFormat] = useState<ExportFormat>("pdf");
  const [error, setError] = useState<string | null>(null);

  const exportMutation = useMutation({
    mutationFn: async () => {
      if (!templateId) {
        throw new Error("Select a template first");
      }
      const result = await exportService.createExport(resumeId, templateId, format);
      await exportService.downloadAndSave(result.downloadUrl);
    },
    onError: () => setError("Export failed. Please try again."),
  });

  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2">
        {FORMATS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFormat(f.value)}
            className={`px-3 py-1.5 rounded-md text-sm border ${
              format === f.value
                ? "border-blue-500 bg-blue-50 text-blue-700"
                : "border-gray-200 text-gray-600 hover:border-gray-300"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div>
        <Button
          onClick={() => exportMutation.mutate()}
          disabled={!templateId || exportMutation.isPending}
        >
          {exportMutation.isPending ? "Exporting..." : "Download"}
        </Button>
      </div>
    </div>
  );
}
