import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as profileService from "../../services/profileService";
import type {
  WorkExperienceEntry,
  WorkExperienceEntryInput,
} from "../../services/profileService";
import { Button } from "../../components/Button";
import { FormField, TextAreaField } from "../../components/FormField";

const EMPTY_FORM: WorkExperienceEntryInput = {
  company: "",
  title: "",
  startDate: "",
  endDate: null,
  description: "",
  displayOrder: 0,
};

export function WorkExperienceEntries({ entries }: { entries: WorkExperienceEntry[] }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<WorkExperienceEntryInput>(EMPTY_FORM);
  const [isCurrentRole, setIsCurrentRole] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["profile"] });

  const addMutation = useMutation({
    mutationFn: (entry: WorkExperienceEntryInput) => profileService.addWorkExperience(entry),
    onSuccess: () => {
      invalidate();
      resetForm();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, entry }: { id: number; entry: WorkExperienceEntryInput }) =>
      profileService.updateWorkExperience(id, entry),
    onSuccess: () => {
      invalidate();
      resetForm();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => profileService.deleteWorkExperience(id),
    onSuccess: invalidate,
  });

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setIsCurrentRole(false);
  }

  function startEdit(entry: WorkExperienceEntry) {
    setEditingId(entry.id);
    setIsCurrentRole(entry.currentRole);
    setForm({
      company: entry.company,
      title: entry.title,
      startDate: entry.startDate,
      endDate: entry.endDate,
      description: entry.description,
      displayOrder: entry.displayOrder,
    });
  }

  function handleSubmit() {
    if (!form.company.trim() || !form.title.trim() || !form.startDate) return;
    const payload: WorkExperienceEntryInput = {
      ...form,
      endDate: isCurrentRole ? null : form.endDate,
    };
    if (editingId !== null) {
      updateMutation.mutate({ id: editingId, entry: payload });
    } else {
      addMutation.mutate(payload);
    }
  }

  return (
    <section className="bg-white rounded-lg shadow-sm p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Work Experience</h2>
      <ul className="flex flex-col gap-3 mb-4">
        {entries.map((entry) => (
          <li
            key={entry.id}
            className="border border-gray-200 rounded-md p-3 flex justify-between items-start"
          >
            <div>
              <p className="font-medium text-gray-900">
                {entry.title} · {entry.company}
              </p>
              <p className="text-sm text-gray-500">
                {entry.startDate} – {entry.currentRole ? "Present" : entry.endDate}
              </p>
              <p className="text-sm text-gray-700 mt-1 whitespace-pre-line">{entry.description}</p>
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
        {entries.length === 0 && (
          <p className="text-sm text-gray-500">No work experience added yet.</p>
        )}
      </ul>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <FormField
          id="we-company"
          label="Company"
          value={form.company}
          onChange={(e) => setForm({ ...form, company: e.target.value })}
        />
        <FormField
          id="we-title"
          label="Title"
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
        />
        <FormField
          id="we-start"
          label="Start date"
          type="date"
          value={form.startDate}
          onChange={(e) => setForm({ ...form, startDate: e.target.value })}
        />
        <div className="flex flex-col gap-1">
          <FormField
            id="we-end"
            label="End date"
            type="date"
            value={form.endDate ?? ""}
            disabled={isCurrentRole}
            onChange={(e) => setForm({ ...form, endDate: e.target.value })}
          />
          <label className="flex items-center gap-2 text-sm text-gray-700 mt-1">
            <input
              type="checkbox"
              checked={isCurrentRole}
              onChange={(e) => setIsCurrentRole(e.target.checked)}
            />
            This is my current role
          </label>
        </div>
        <div className="sm:col-span-2">
          <TextAreaField
            id="we-description"
            label="Description"
            rows={3}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
      </div>
      <div className="mt-3 flex gap-2">
        <Button onClick={handleSubmit} disabled={addMutation.isPending || updateMutation.isPending}>
          {editingId !== null ? "Save changes" : "Add work experience"}
        </Button>
        {editingId !== null && (
          <Button variant="secondary" onClick={resetForm}>
            Cancel
          </Button>
        )}
      </div>
    </section>
  );
}
