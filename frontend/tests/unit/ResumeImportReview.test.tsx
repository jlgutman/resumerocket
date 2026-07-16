import { render, screen, within, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ResumeImportReview } from "../../src/features/resumeImport/ResumeImportReview";
import type { Profile } from "../../src/services/profileService";
import type { ResumeImportResult } from "../../src/services/resumeImportService";

vi.mock("../../src/services/profileService", () => ({
  updateProfile: vi.fn().mockResolvedValue({}),
  addWorkExperience: vi.fn().mockResolvedValue({}),
  addEducation: vi.fn().mockResolvedValue({}),
  addSkill: vi.fn().mockResolvedValue({}),
}));

import * as profileService from "../../src/services/profileService";

const CURRENT_PROFILE: Profile = {
  id: 1,
  fullName: "",
  email: "",
  phone: "555-0100", // already filled -> conflict case
  location: "", // empty -> fill case
  links: "",
  educationEntries: [],
  workExperienceEntries: [],
  skills: [],
};

const IMPORT_RESULT: ResumeImportResult = {
  sourceFileName: "resume.pdf",
  contactInfo: {
    fullName: "Jane Doe",
    email: "jane@example.com",
    phone: "555-9999",
    location: "Remote",
    links: null,
    fieldsNotExtracted: ["links"],
  },
  workExperience: [
    {
      company: "Acme Corp",
      title: "Engineer",
      startDate: "2020-01-01",
      endDate: null,
      description: "Built things",
      lowConfidence: false,
    },
  ],
  education: [],
  skills: [{ name: "TypeScript", category: null, lowConfidence: true }],
  warnings: ["Unusual formatting detected"],
};

function renderReview(onClose = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ResumeImportReview
        result={IMPORT_RESULT}
        currentProfile={CURRENT_PROFILE}
        onClose={onClose}
      />
    </QueryClientProvider>,
  );
  return { onClose };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ResumeImportReview", () => {
  it("defaults to keeping an already-filled field and applying an empty one (FR-010, FR-011)", () => {
    renderReview();
    expect(screen.getByLabelText("Apply Phone")).not.toBeChecked();
    expect(screen.getByLabelText("Apply Location")).toBeChecked();
  });

  it("flags fields/entries the extractor could not confidently fill (FR-007)", () => {
    renderReview();
    // One badge for the unextracted "links" contact field, one for the low-confidence skill.
    expect(screen.getAllByText("needs manual input")).toHaveLength(2);
  });

  it("shows the extraction warnings", () => {
    renderReview();
    expect(screen.getByText("Unusual formatting detected")).toBeVisible();
  });

  it("submits an edited value instead of the originally-extracted one (FR-006)", async () => {
    renderReview();
    const titleInput = screen.getByLabelText("Title") as HTMLInputElement;
    fireEvent.change(titleInput, { target: { value: "Senior Engineer" } });

    fireEvent.click(screen.getByRole("button", { name: "Confirm import" }));

    await waitFor(() => expect(profileService.addWorkExperience).toHaveBeenCalled());
    expect(profileService.addWorkExperience).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Senior Engineer" }),
    );
  });

  it("excludes an entry from the confirm submission when unchecked (FR-006)", async () => {
    renderReview();
    const workExperienceCard = screen.getByDisplayValue("Acme Corp").closest("li")!;
    fireEvent.click(within(workExperienceCard).getByLabelText("Include"));

    fireEvent.click(screen.getByRole("button", { name: "Confirm import" }));

    await waitFor(() => expect(profileService.updateProfile).toHaveBeenCalled());
    expect(profileService.addWorkExperience).not.toHaveBeenCalled();
  });

  it("discards the review without calling any mutation on cancel (FR-013)", () => {
    const { onClose } = renderReview();
    fireEvent.click(screen.getByRole("button", { name: "Discard" }));

    expect(onClose).toHaveBeenCalled();
    expect(profileService.updateProfile).not.toHaveBeenCalled();
    expect(profileService.addWorkExperience).not.toHaveBeenCalled();
  });
});
