import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as profileService from "../../services/profileService";
import type { EducationEntry, EducationEntryInput } from "../../services/profileService";
import { Button } from "../../components/Button";
import { FormField, TextAreaField } from "../../components/FormField";

const EMPTY_FORM: EducationEntryInput = {
  institution: "",
  credential: "",
  fieldOfStudy: "",
  startDate: "",
  endDate: "",
  description: "",
  displayOrder: 0,
};

export function EducationEntries({ entries }: { entries: EducationEntry[] }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<EducationEntryInput>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["profile"] });

  const addMutation = useMutation({
    mutationFn: (entry: EducationEntryInput) => profileService.addEducation(entry),
    onSuccess: () => {
      invalidate();
      setForm(EMPTY_FORM);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, entry }: { id: number; entry: EducationEntryInput }) =>
      profileService.updateEducation(id, entry),
    onSuccess: () => {
      invalidate();
      setEditingId(null);
      setForm(EMPTY_FORM);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => profileService.deleteEducation(id),
    onSuccess: invalidate,
  });

  function startEdit(entry: EducationEntry) {
    setEditingId(entry.id);
    setForm({
      institution: entry.institution,
      credential: entry.credential ?? "",
      fieldOfStudy: entry.fieldOfStudy ?? "",
      startDate: entry.startDate ?? "",
      endDate: entry.endDate ?? "",
      description: entry.description ?? "",
      displayOrder: entry.displayOrder,
    });
  }

  function handleSubmit() {
    if (!form.institution.trim()) return;
    if (editingId !== null) {
      updateMutation.mutate({ id: editingId, entry: form });
    } else {
      addMutation.mutate(form);
    }
  }

  return (
    <section className="bg-white rounded-lg shadow-sm p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Education</h2>
      <ul className="flex flex-col gap-3 mb-4">
        {entries.map((entry) => (
          <li
            key={entry.id}
            className="border border-gray-200 rounded-md p-3 flex justify-between items-start"
          >
            <div>
              <p className="font-medium text-gray-900">
                {entry.credential ? `${entry.credential}, ` : ""}
                {entry.institution}
              </p>
              <p className="text-sm text-gray-500">
                {entry.fieldOfStudy} {entry.startDate ?? ""} – {entry.endDate ?? "Present"}
              </p>
              {entry.description && <p className="text-sm text-gray-700 mt-1">{entry.description}</p>}
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => startEdit(entry)}>
                Edit
              </Button>
              <Button variant="danger" onClick={() => deleteMutation.mutate(entry.id)}>
                Delete
              </Button>
            </div>
          </li>
        ))}
        {entries.length === 0 && <p className="text-sm text-gray-500">No education added yet.</p>}
      </ul>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField
          id="edu-institution"
          label="Institution"
          value={form.institution}
          onChange={(e) => setForm({ ...form, institution: e.target.value })}
        />
        <FormField
          id="edu-credential"
          label="Credential"
          value={form.credential ?? ""}
          onChange={(e) => setForm({ ...form, credential: e.target.value })}
        />
        <FormField
          id="edu-field"
          label="Field of study"
          value={form.fieldOfStudy ?? ""}
          onChange={(e) => setForm({ ...form, fieldOfStudy: e.target.value })}
        />
        <div className="grid grid-cols-2 gap-3">
          <FormField
            id="edu-start"
            label="Start date"
            type="date"
            value={form.startDate ?? ""}
            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
          />
          <FormField
            id="edu-end"
            label="End date"
            type="date"
            value={form.endDate ?? ""}
            onChange={(e) => setForm({ ...form, endDate: e.target.value })}
          />
        </div>
        <div className="sm:col-span-2">
          <TextAreaField
            id="edu-description"
            label="Description"
            rows={2}
            value={form.description ?? ""}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
      </div>
      <div className="mt-3 flex gap-2">
        <Button onClick={handleSubmit} disabled={addMutation.isPending || updateMutation.isPending}>
          {editingId !== null ? "Save changes" : "Add education"}
        </Button>
        {editingId !== null && (
          <Button
            variant="secondary"
            onClick={() => {
              setEditingId(null);
              setForm(EMPTY_FORM);
            }}
          >
            Cancel
          </Button>
        )}
      </div>
    </section>
  );
}
