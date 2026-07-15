import { useQuery } from "@tanstack/react-query";
import * as exportService from "../../services/exportService";

export function TemplatePicker({
  selectedTemplateId,
  onSelect,
}: {
  selectedTemplateId: number | null;
  onSelect: (templateId: number) => void;
}) {
  const { data: templates, isLoading } = useQuery({
    queryKey: ["templates"],
    queryFn: exportService.listTemplates,
  });

  if (isLoading || !templates) {
    return <p className="text-sm text-gray-500">Loading templates…</p>;
  }

  return (
    <div className="flex flex-wrap gap-3">
      {templates.map((template) => (
        <button
          key={template.id}
          type="button"
          onClick={() => onSelect(template.id)}
          className={`flex-1 min-w-[140px] border rounded-md p-3 text-left transition-colors ${
            selectedTemplateId === template.id
              ? "border-blue-500 bg-blue-50"
              : "border-gray-200 hover:border-gray-300"
          }`}
        >
          <p className="font-medium text-gray-900">{template.name}</p>
          <p className="text-xs text-gray-500 capitalize">{template.layoutDescriptor.style} style</p>
        </button>
      ))}
    </div>
  );
}
