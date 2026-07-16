import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import * as profileService from "../../services/profileService";
import type { ContactInfo, Profile } from "../../services/profileService";
import type {
  EducationCandidate,
  ResumeImportResult,
  SkillCandidate,
  WorkExperienceCandidate,
} from "../../services/resumeImportService";
import { Button } from "../../components/Button";
import { FormField, TextAreaField } from "../../components/FormField";

type ContactField = keyof ContactInfo;

const CONTACT_FIELDS: { key: ContactField; label: string }[] = [
  { key: "fullName", label: "Full name" },
  { key: "email", label: "Email" },
  { key: "phone", label: "Phone" },
  { key: "location", label: "Location" },
  { key: "links", label: "Links" },
];

interface EditableWorkExperience extends WorkExperienceCandidate {
  include: boolean;
}
interface EditableEducation extends EducationCandidate {
  include: boolean;
}
interface EditableSkill extends SkillCandidate {
  include: boolean;
}

function currentContactValue(profile: Profile, key: ContactField): string {
  return profile[key] ?? "";
}

/** Fields already filled on the profile default to "keep existing" (FR-010); empty ones default to the extracted value (FR-011). */
function initContactApply(
  candidate: ResumeImportResult["contactInfo"],
  profile: Profile,
): Record<ContactField, boolean> {
  const apply = {} as Record<ContactField, boolean>;
  for (const { key } of CONTACT_FIELDS) {
    const hasExisting = currentContactValue(profile, key).trim().length > 0;
    const extractedValue = candidate[key];
    apply[key] = !hasExisting && !!extractedValue?.trim();
  }
  return apply;
}

function initContactValues(
  candidate: ResumeImportResult["contactInfo"],
  profile: Profile,
): Record<ContactField, string> {
  const values = {} as Record<ContactField, string>;
  for (const { key } of CONTACT_FIELDS) {
    values[key] = candidate[key] ?? currentContactValue(profile, key);
  }
  return values;
}

function LowConfidenceBadge() {
  return (
    <span className="text-xs px-2 py-0.5 rounded-full bg-amber-50 text-amber-800">
      needs manual input
    </span>
  );
}

export function ResumeImportReview({
  result,
  currentProfile,
  onClose,
}: {
  result: ResumeImportResult;
  currentProfile: Profile;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();

  const [contactValues, setContactValues] = useState(() =>
    initContactValues(result.contactInfo, currentProfile),
  );
  const [contactApply, setContactApply] = useState(() =>
    initContactApply(result.contactInfo, currentProfile),
  );
  const [workExperience, setWorkExperience] = useState<EditableWorkExperience[]>(() =>
    result.workExperience.map((candidate) => ({ ...candidate, include: true })),
  );
  const [education, setEducation] = useState<EditableEducation[]>(() =>
    result.education.map((candidate) => ({ ...candidate, include: true })),
  );
  const [skills, setSkills] = useState<EditableSkill[]>(() =>
    result.skills.map((candidate) => ({ ...candidate, include: true })),
  );
  const [confirmError, setConfirmError] = useState<string | null>(null);

  function updateWorkExperience(index: number, patch: Partial<EditableWorkExperience>) {
    setWorkExperience((prev) =>
      prev.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)),
    );
  }

  function updateEducation(index: number, patch: Partial<EditableEducation>) {
    setEducation((prev) => prev.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
  }

  function updateSkill(index: number, patch: Partial<EditableSkill>) {
    setSkills((prev) => prev.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
  }

  const confirmMutation = useMutation({
    mutationFn: async () => {
      const mergedContact: ContactInfo = {
        fullName: currentProfile.fullName ?? "",
        email: currentProfile.email ?? "",
        phone: currentProfile.phone ?? "",
        location: currentProfile.location ?? "",
        links: currentProfile.links ?? "",
      };
      for (const { key } of CONTACT_FIELDS) {
        if (contactApply[key]) {
          mergedContact[key] = contactValues[key];
        }
      }
      await profileService.updateProfile(mergedContact);

      // Every approved candidate is always created as a new entry — never matched or merged
      // against an existing one (FR-012).
      for (const entry of workExperience) {
        if (!entry.include) continue;
        if (
          !entry.company.trim() ||
          !entry.title.trim() ||
          !entry.startDate ||
          !entry.description.trim()
        ) {
          continue;
        }
        await profileService.addWorkExperience({
          company: entry.company,
          title: entry.title,
          startDate: entry.startDate,
          endDate: entry.endDate,
          description: entry.description,
          displayOrder: 0,
        });
      }

      for (const entry of education) {
        if (!entry.include || !entry.institution.trim()) continue;
        await profileService.addEducation({
          institution: entry.institution,
          credential: entry.credential,
          fieldOfStudy: entry.fieldOfStudy,
          startDate: entry.startDate,
          endDate: entry.endDate,
          description: entry.description,
          displayOrder: 0,
        });
      }

      for (const entry of skills) {
        if (!entry.include || !entry.name.trim()) continue;
        await profileService.addSkill({ name: entry.name, category: entry.category });
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile"] });
      onClose();
    },
    onError: () => {
      setConfirmError(
        "Some of your reviewed changes may not have saved. Please check your profile before retrying.",
      );
    },
  });

  return (
    <section className="bg-white rounded-lg shadow-sm p-6 border-2 border-blue-200">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-900">
          Review data extracted from &ldquo;{result.sourceFileName}&rdquo;
        </h2>
        <Button variant="secondary" onClick={onClose} disabled={confirmMutation.isPending}>
          Cancel
        </Button>
      </div>

      {result.warnings.length > 0 && (
        <ul className="mb-4 text-sm text-amber-700 bg-amber-50 rounded-md p-3 flex flex-col gap-1">
          {result.warnings.map((warning) => (
            <li key={warning}>{warning}</li>
          ))}
        </ul>
      )}

      <div className="mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Contact info</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {CONTACT_FIELDS.map(({ key, label }) => {
            const hasExisting = currentContactValue(currentProfile, key).trim().length > 0;
            const extractedValue = result.contactInfo[key];
            const notExtracted = result.contactInfo.fieldsNotExtracted.includes(key);
            return (
              <div key={key} className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                  <label className="flex items-center gap-2 text-sm text-gray-700">
                    <input
                      type="checkbox"
                      checked={contactApply[key]}
                      onChange={(e) =>
                        setContactApply((prev) => ({ ...prev, [key]: e.target.checked }))
                      }
                    />
                    {`Apply ${label}`}
                  </label>
                  {notExtracted && <LowConfidenceBadge />}
                </div>
                <FormField
                  id={`import-contact-${key}`}
                  label={
                    hasExisting && extractedValue
                      ? `${label} (existing kept unless "Apply ${label}" is checked)`
                      : label
                  }
                  value={contactValues[key]}
                  onChange={(e) => setContactValues((prev) => ({ ...prev, [key]: e.target.value }))}
                />
              </div>
            );
          })}
        </div>
      </div>

      <div className="mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          Work experience ({workExperience.length})
        </h3>
        <ul className="flex flex-col gap-3">
          {workExperience.map((entry, index) => (
            <li key={index} className="border border-gray-200 rounded-md p-3">
              <div className="flex items-center gap-2 mb-2">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    checked={entry.include}
                    onChange={(e) => updateWorkExperience(index, { include: e.target.checked })}
                  />
                  Include
                </label>
                {entry.lowConfidence && <LowConfidenceBadge />}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <FormField
                  id={`we-company-${index}`}
                  label="Company"
                  value={entry.company}
                  onChange={(e) => updateWorkExperience(index, { company: e.target.value })}
                />
                <FormField
                  id={`we-title-${index}`}
                  label="Title"
                  value={entry.title}
                  onChange={(e) => updateWorkExperience(index, { title: e.target.value })}
                />
                <FormField
                  id={`we-start-${index}`}
                  label="Start date"
                  type="date"
                  value={entry.startDate ?? ""}
                  onChange={(e) => updateWorkExperience(index, { startDate: e.target.value })}
                />
                <FormField
                  id={`we-end-${index}`}
                  label="End date (blank = current role)"
                  type="date"
                  value={entry.endDate ?? ""}
                  onChange={(e) => updateWorkExperience(index, { endDate: e.target.value || null })}
                />
                <div className="sm:col-span-2">
                  <TextAreaField
                    id={`we-description-${index}`}
                    label="Description"
                    rows={2}
                    value={entry.description}
                    onChange={(e) => updateWorkExperience(index, { description: e.target.value })}
                  />
                </div>
              </div>
            </li>
          ))}
          {workExperience.length === 0 && (
            <p className="text-sm text-gray-500">No work experience detected in this resume.</p>
          )}
        </ul>
      </div>

      <div className="mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Education ({education.length})</h3>
        <ul className="flex flex-col gap-3">
          {education.map((entry, index) => (
            <li key={index} className="border border-gray-200 rounded-md p-3">
              <div className="flex items-center gap-2 mb-2">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    checked={entry.include}
                    onChange={(e) => updateEducation(index, { include: e.target.checked })}
                  />
                  Include
                </label>
                {entry.lowConfidence && <LowConfidenceBadge />}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <FormField
                  id={`edu-institution-${index}`}
                  label="Institution"
                  value={entry.institution}
                  onChange={(e) => updateEducation(index, { institution: e.target.value })}
                />
                <FormField
                  id={`edu-credential-${index}`}
                  label="Credential"
                  value={entry.credential ?? ""}
                  onChange={(e) => updateEducation(index, { credential: e.target.value })}
                />
                <FormField
                  id={`edu-field-${index}`}
                  label="Field of study"
                  value={entry.fieldOfStudy ?? ""}
                  onChange={(e) => updateEducation(index, { fieldOfStudy: e.target.value })}
                />
                <div className="grid grid-cols-2 gap-3">
                  <FormField
                    id={`edu-start-${index}`}
                    label="Start date"
                    type="date"
                    value={entry.startDate ?? ""}
                    onChange={(e) => updateEducation(index, { startDate: e.target.value })}
                  />
                  <FormField
                    id={`edu-end-${index}`}
                    label="End date (blank = in progress)"
                    type="date"
                    value={entry.endDate ?? ""}
                    onChange={(e) => updateEducation(index, { endDate: e.target.value || null })}
                  />
                </div>
                <div className="sm:col-span-2">
                  <TextAreaField
                    id={`edu-description-${index}`}
                    label="Description"
                    rows={2}
                    value={entry.description ?? ""}
                    onChange={(e) => updateEducation(index, { description: e.target.value })}
                  />
                </div>
              </div>
            </li>
          ))}
          {education.length === 0 && (
            <p className="text-sm text-gray-500">No education detected in this resume.</p>
          )}
        </ul>
      </div>

      <div className="mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Skills ({skills.length})</h3>
        <ul className="flex flex-col gap-2">
          {skills.map((entry, index) => (
            <li
              key={index}
              className="border border-gray-200 rounded-md p-2 flex items-center gap-3"
            >
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={entry.include}
                  onChange={(e) => updateSkill(index, { include: e.target.checked })}
                />
                Include
              </label>
              <FormField
                id={`skill-name-${index}`}
                label="Skill"
                value={entry.name}
                onChange={(e) => updateSkill(index, { name: e.target.value })}
              />
              <FormField
                id={`skill-category-${index}`}
                label="Category"
                value={entry.category ?? ""}
                onChange={(e) => updateSkill(index, { category: e.target.value })}
              />
              {entry.lowConfidence && <LowConfidenceBadge />}
            </li>
          ))}
          {skills.length === 0 && (
            <p className="text-sm text-gray-500">No skills detected in this resume.</p>
          )}
        </ul>
      </div>

      <div className="flex items-center gap-3">
        <Button onClick={() => confirmMutation.mutate()} disabled={confirmMutation.isPending}>
          {confirmMutation.isPending ? "Saving…" : "Confirm import"}
        </Button>
        <Button variant="secondary" onClick={onClose} disabled={confirmMutation.isPending}>
          Discard
        </Button>
        {confirmError && <span className="text-sm text-red-600">{confirmError}</span>}
      </div>
    </section>
  );
}
