import type { CompareResult, DiffLine } from "../../services/versionService";

const LINE_STYLES: Record<DiffLine["type"], string> = {
  UNCHANGED: "text-gray-700",
  ADDED: "bg-green-50 text-green-800",
  REMOVED: "bg-red-50 text-red-700 line-through",
};

const SECTION_LABEL: Record<string, string> = {
  summary: "Summary",
  experience: "Experience",
  education: "Education",
  skills: "Skills",
};

export function CompareView({ result }: { result: CompareResult }) {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-xs uppercase text-gray-500">Left</p>
          <p className="font-medium text-gray-900">{result.left.name}</p>
          <p className="text-sm text-gray-500">{result.left.company ?? "—"}</p>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-xs uppercase text-gray-500">Right</p>
          <p className="font-medium text-gray-900">{result.right.name}</p>
          <p className="text-sm text-gray-500">{result.right.company ?? "—"}</p>
        </div>
      </div>

      {result.diff.map((section) => (
        <div key={section.section} className="bg-white rounded-lg shadow-sm p-4">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-2">
            {SECTION_LABEL[section.section] ?? section.section}
          </h2>
          <div className="flex flex-col gap-1 font-mono text-sm">
            {section.lines.length === 0 && <p className="text-gray-400">No content</p>}
            {section.lines.map((line, idx) => (
              <div key={idx} className={`px-2 py-0.5 rounded ${LINE_STYLES[line.type]}`}>
                {line.type === "ADDED" && "+ "}
                {line.type === "REMOVED" && "- "}
                {line.text}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
