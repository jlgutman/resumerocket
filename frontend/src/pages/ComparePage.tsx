import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import * as versionService from "../services/versionService";
import { CompareView } from "../features/versions/CompareView";

export default function ComparePage() {
  const [searchParams] = useSearchParams();
  const leftId = Number(searchParams.get("leftId"));
  const rightId = Number(searchParams.get("rightId"));
  const enabled = Number.isFinite(leftId) && Number.isFinite(rightId) && leftId > 0 && rightId > 0;

  const { data, isLoading } = useQuery({
    queryKey: ["compare", leftId, rightId],
    queryFn: () => versionService.compareTailoredResumes(leftId, rightId),
    enabled,
  });

  if (!enabled) {
    return <p className="text-gray-500">Select two resumes from "My Resumes" to compare.</p>;
  }
  if (isLoading || !data) {
    return <p className="text-gray-500">Loading comparison…</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-gray-900">Compare Resumes</h1>
      <CompareView result={data} />
    </div>
  );
}
