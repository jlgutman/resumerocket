import { useState, type KeyboardEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as profileService from "../../services/profileService";
import type { Skill } from "../../services/profileService";
import { Button } from "../../components/Button";
import { FormField } from "../../components/FormField";

export function SkillsList({ skills }: { skills: Skill[] }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["profile"] });

  const addMutation = useMutation({
    mutationFn: () => profileService.addSkill({ name, category: category || null }),
    onSuccess: () => {
      invalidate();
      setName("");
      setCategory("");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => profileService.deleteSkill(id),
    onSuccess: invalidate,
  });

  function handleAdd() {
    if (!name.trim()) return;
    addMutation.mutate();
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      handleAdd();
    }
  }

  return (
    <section className="bg-white rounded-lg shadow-sm p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Skills</h2>
      <div className="flex flex-wrap gap-2 mb-4">
        {skills.map((skill) => (
          <span
            key={skill.id}
            className="inline-flex items-center gap-1 bg-blue-50 text-blue-800 text-sm px-3 py-1 rounded-full"
          >
            {skill.name}
            <button
              type="button"
              aria-label={`Remove ${skill.name}`}
              className="text-blue-500 hover:text-blue-700"
              onClick={() => deleteMutation.mutate(skill.id)}
            >
              ×
            </button>
          </span>
        ))}
        {skills.length === 0 && <p className="text-sm text-gray-500">No skills added yet.</p>}
      </div>
      <div className="flex gap-3 items-end">
        <FormField
          id="skill-name"
          label="Skill"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <FormField
          id="skill-category"
          label="Category (optional)"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <Button onClick={handleAdd} disabled={addMutation.isPending}>
          Add
        </Button>
      </div>
    </section>
  );
}
