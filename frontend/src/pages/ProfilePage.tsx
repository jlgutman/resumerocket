import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as profileService from "../services/profileService";
import type { ContactInfo } from "../services/profileService";
import { FormField } from "../components/FormField";
import { Button } from "../components/Button";
import { EducationEntries } from "../features/profile/EducationEntries";
import { WorkExperienceEntries } from "../features/profile/WorkExperienceEntries";
import { SkillsList } from "../features/profile/SkillsList";
import { ResumeUploadButton } from "../features/resumeImport/ResumeUploadButton";
import { ResumeImportReview } from "../features/resumeImport/ResumeImportReview";
import type { ResumeImportResult } from "../services/resumeImportService";

const EMPTY_CONTACT: ContactInfo = { fullName: "", email: "", phone: "", location: "", links: "" };

export default function ProfilePage() {
  const queryClient = useQueryClient();
  const { data: profile, isLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: profileService.getProfile,
  });

  const [contactInfo, setContactInfo] = useState<ContactInfo>(EMPTY_CONTACT);
  const [savedMessageVisible, setSavedMessageVisible] = useState(false);
  const [importResult, setImportResult] = useState<ResumeImportResult | null>(null);

  useEffect(() => {
    if (profile) {
      setContactInfo({
        fullName: profile.fullName ?? "",
        email: profile.email ?? "",
        phone: profile.phone ?? "",
        location: profile.location ?? "",
        links: profile.links ?? "",
      });
    }
  }, [profile]);

  const updateMutation = useMutation({
    mutationFn: (info: ContactInfo) => profileService.updateProfile(info),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile"] });
      setSavedMessageVisible(true);
      setTimeout(() => setSavedMessageVisible(false), 3000);
    },
  });

  if (isLoading || !profile) {
    return <p className="text-gray-500">Loading your profile…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="bg-white rounded-lg shadow-sm p-6">
        <h1 className="text-xl font-semibold text-gray-900 mb-4">Your Master Profile</h1>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField
            id="fullName"
            label="Full name"
            value={contactInfo.fullName}
            onChange={(e) => setContactInfo({ ...contactInfo, fullName: e.target.value })}
          />
          <FormField
            id="contactEmail"
            label="Email"
            type="email"
            value={contactInfo.email}
            onChange={(e) => setContactInfo({ ...contactInfo, email: e.target.value })}
          />
          <FormField
            id="phone"
            label="Phone"
            value={contactInfo.phone}
            onChange={(e) => setContactInfo({ ...contactInfo, phone: e.target.value })}
          />
          <FormField
            id="location"
            label="Location"
            value={contactInfo.location}
            onChange={(e) => setContactInfo({ ...contactInfo, location: e.target.value })}
          />
          <div className="sm:col-span-2">
            <FormField
              id="links"
              label="Links (portfolio, LinkedIn, GitHub — comma-separated)"
              value={contactInfo.links}
              onChange={(e) => setContactInfo({ ...contactInfo, links: e.target.value })}
            />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <Button
            onClick={() => updateMutation.mutate(contactInfo)}
            disabled={updateMutation.isPending}
          >
            Save profile
          </Button>
          {savedMessageVisible && (
            <span className="text-sm text-green-600">Your information has been saved.</span>
          )}
        </div>
      </section>

      <section className="bg-white rounded-lg shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Prefill from a resume</h2>
        <ResumeUploadButton onImported={setImportResult} />
      </section>

      {importResult && (
        <ResumeImportReview
          result={importResult}
          currentProfile={profile}
          onClose={() => setImportResult(null)}
        />
      )}

      <EducationEntries entries={profile.educationEntries} />
      <WorkExperienceEntries entries={profile.workExperienceEntries} />
      <SkillsList skills={profile.skills} />
    </div>
  );
}
