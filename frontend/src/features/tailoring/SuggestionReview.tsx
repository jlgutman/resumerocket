import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as tailoringService from "../../services/tailoringService";
import type { AiSuggestion } from "../../services/tailoringService";
import { Button } from "../../components/Button";
import { TextAreaField } from "../../components/FormField";

const TYPE_LABEL: Record<AiSuggestion["suggestionType"], string> = {
  EMPHASIS: "Emphasis",
  BULLET_REWRITE: "Bullet rewrite",
  SKILL_HIGHLIGHT: "Suggested skill",
};

const STATE_BADGE: Record<AiSuggestion["reviewState"], string> = {
  PENDING: "bg-yellow-50 text-yellow-800",
  ACCEPTED: "bg-green-50 text-green-800",
  REJECTED: "bg-gray-100 text-gray-500",
  EDITED: "bg-blue-50 text-blue-800",
};

function SuggestionCard({ resumeId, suggestion }: { resumeId: number; suggestion: AiSuggestion }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [editedText, setEditedText] = useState(suggestion.suggestedText);

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["tailored-resume", resumeId] });

  const resolveMutation = useMutation({
    mutationFn: ({
      reviewState,
      finalText,
    }: {
      reviewState: "ACCEPTED" | "REJECTED" | "EDITED";
      finalText?: string;
    }) => tailoringService.resolveSuggestion(resumeId, suggestion.id, reviewState, finalText),
    onSuccess: () => {
      invalidate();
      setEditing(false);
    },
  });

  return (
    <li className="border border-gray-200 rounded-md p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium uppercase tracking-wide text-gray-500">
          {TYPE_LABEL[suggestion.suggestionType]} · {suggestion.targetSection}
        </span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${STATE_BADGE[suggestion.reviewState]}`}>
          {suggestion.reviewState.toLowerCase()}
        </span>
      </div>

      {suggestion.originalText && (
        <p className="text-sm text-gray-500 line-through mb-1">{suggestion.originalText}</p>
      )}
      <p className="text-sm text-gray-900 bg-blue-50/60 rounded px-2 py-1">
        {suggestion.finalText ?? suggestion.suggestedText}
      </p>

      {editing && (
        <div className="mt-3">
          <TextAreaField
            id={`edit-${suggestion.id}`}
            label="Your edit"
            rows={2}
            value={editedText}
            onChange={(e) => setEditedText(e.target.value)}
          />
        </div>
      )}

      {suggestion.reviewState === "PENDING" && (
        <div className="mt-3 flex gap-2">
          <Button
            onClick={() => resolveMutation.mutate({ reviewState: "ACCEPTED" })}
            disabled={resolveMutation.isPending}
          >
            Accept
          </Button>
          <Button
            variant="secondary"
            onClick={() => resolveMutation.mutate({ reviewState: "REJECTED" })}
            disabled={resolveMutation.isPending}
          >
            Reject
          </Button>
          {editing ? (
            <Button
              variant="secondary"
              onClick={() =>
                resolveMutation.mutate({ reviewState: "EDITED", finalText: editedText })
              }
              disabled={resolveMutation.isPending}
            >
              Save edit
            </Button>
          ) : (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )}
        </div>
      )}
    </li>
  );
}

export function SuggestionReview({
  resumeId,
  suggestions,
}: {
  resumeId: number;
  suggestions: AiSuggestion[];
}) {
  if (suggestions.length === 0) {
    return <p className="text-sm text-gray-500">No AI suggestions for this resume.</p>;
  }
  return (
    <ul className="flex flex-col gap-3">
      {suggestions.map((suggestion) => (
        <SuggestionCard key={suggestion.id} resumeId={resumeId} suggestion={suggestion} />
      ))}
    </ul>
  );
}
