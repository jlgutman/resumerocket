import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as versionService from "../services/versionService";
import { FormField } from "../components/FormField";
import { Button } from "../components/Button";

export default function VersionHistoryPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [companyFilter, setCompanyFilter] = useState("");
  const [selected, setSelected] = useState<number[]>([]);

  const { data: resumes, isLoading } = useQuery({
    queryKey: ["tailored-resumes", companyFilter],
    queryFn: () => versionService.listTailoredResumes(companyFilter || undefined),
  });

  const cloneMutation = useMutation({
    mutationFn: (id: number) => versionService.cloneTailoredResume(id),
    onSuccess: (clone) => {
      queryClient.invalidateQueries({ queryKey: ["tailored-resumes"] });
      navigate(`/resumes/${clone.id}`);
    },
  });

  const regenerateMutation = useMutation({
    mutationFn: (id: number) => versionService.regenerateTailoredResume(id),
    onSuccess: (regenerated) => {
      queryClient.invalidateQueries({ queryKey: ["tailored-resumes"] });
      navigate(`/resumes/${regenerated.id}`);
    },
  });

  function toggleSelected(id: number) {
    setSelected((prev) => {
      if (prev.includes(id)) return prev.filter((v) => v !== id);
      if (prev.length >= 2) return [prev[1], id];
      return [...prev, id];
    });
  }

  function goToCompare() {
    if (selected.length !== 2) return;
    navigate(`/resumes/compare?leftId=${selected[0]}&rightId=${selected[1]}`);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">My Resumes</h1>
        <Link to="/tailor">
          <Button>Tailor a new resume</Button>
        </Link>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <FormField
          id="company-filter"
          label="Filter by company"
          value={companyFilter}
          onChange={(e) => setCompanyFilter(e.target.value)}
        />
        <Button variant="secondary" onClick={goToCompare} disabled={selected.length !== 2}>
          Compare selected ({selected.length}/2)
        </Button>
      </div>

      {isLoading && <p className="text-gray-500">Loading…</p>}

      <ul className="flex flex-col gap-3">
        {resumes?.map((resume) => (
          <li
            key={resume.id}
            className="bg-white rounded-lg shadow-sm p-4 flex flex-wrap items-center justify-between gap-3"
          >
            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                checked={selected.includes(resume.id)}
                onChange={() => toggleSelected(resume.id)}
                aria-label={`Select ${resume.name} for comparison`}
              />
              <div>
                <Link to={`/resumes/${resume.id}`} className="font-medium text-blue-700 hover:underline">
                  {resume.name}
                </Link>
                <p className="text-sm text-gray-500">
                  {resume.jobTitle ?? "—"} {resume.company ? `at ${resume.company}` : ""} ·{" "}
                  {resume.status.toLowerCase()} · {new Date(resume.createdAt).toLocaleDateString()}
                </p>
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={() => cloneMutation.mutate(resume.id)}
                disabled={cloneMutation.isPending}
              >
                Clone
              </Button>
              <Button
                variant="secondary"
                onClick={() => regenerateMutation.mutate(resume.id)}
                disabled={regenerateMutation.isPending}
              >
                Regenerate
              </Button>
            </div>
          </li>
        ))}
        {resumes && resumes.length === 0 && (
          <p className="text-sm text-gray-500">No tailored resumes yet.</p>
        )}
      </ul>
    </div>
  );
}
