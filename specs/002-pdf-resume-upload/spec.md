# Feature Specification: PDF Resume Upload & Profile Prepopulation

**Feature Branch**: `002-pdf-resume-upload`

**Created**: 2026-07-15

**Status**: Draft

**Input**: User description: "add new feature to upload pdf resume to prepopulate the profile values"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bootstrap a new profile from an existing resume (Priority: P1)

A new user who already has a resume in PDF form uploads it instead of typing their entire work history, education, and contact details from scratch. The system reads the PDF, extracts the relevant profile information, and shows it to the user for confirmation before anything is saved.

**Why this priority**: This is the core value of the feature — eliminating the tedious manual data entry that is the biggest barrier to a new user completing their master profile. Without this, the feature delivers no value.

**Independent Test**: Can be fully tested by uploading a well-formatted single-column PDF resume to a brand-new (empty) profile and confirming the extracted data, delivering a populated profile without any manual field entry.

**Acceptance Scenarios**:

1. **Given** a user with an empty master profile, **When** they upload a valid PDF resume, **Then** the system extracts contact information, work experience entries, education entries, and skills, and presents them in a review screen.
2. **Given** the review screen showing extracted data, **When** the user confirms the import, **Then** the master profile is populated with the confirmed data.
3. **Given** the review screen showing extracted data, **When** the user closes or cancels the review without confirming, **Then** no changes are made to the master profile.

---

### User Story 2 - Review and correct extracted data before saving (Priority: P1)

Resume parsing is not always perfect. Before any data reaches the master profile, the user can see exactly what was extracted, edit any field that is wrong or incomplete, remove entries that shouldn't be imported, and add anything the system missed.

**Why this priority**: Automatically-extracted data can be wrong or misaligned (e.g., dates parsed incorrectly, work entries merged). Without a review step, incorrect data could silently corrupt the user's profile, undermining trust in the whole feature. This is P1 because Story 1 is not safely usable without it.

**Independent Test**: Can be fully tested by uploading a resume that produces at least one incorrect or missing field, editing that field in the review screen, and confirming that the corrected value (not the originally-extracted value) is the one saved to the profile.

**Acceptance Scenarios**:

1. **Given** extracted data in the review screen, **When** the user edits a field, **Then** the edited value — not the originally extracted value — is what gets saved on confirmation.
2. **Given** extracted data in the review screen, **When** the user removes an extracted work experience, education, or skill entry, **Then** that entry is excluded from the saved profile.
3. **Given** extracted data in the review screen, **When** some fields could not be confidently extracted, **Then** those fields are clearly marked as needing manual input rather than silently left blank or guessed.

---

### User Story 3 - Update an existing profile from a newer resume (Priority: P2)

A returning user with an already-populated master profile uploads an updated version of their resume (e.g., after a new job or new skills) to quickly add the new information without re-entering everything or losing what's already there.

**Why this priority**: This extends the feature's value to returning users, but the primary onboarding use case (Story 1) delivers value on its own first; this is a valuable but secondary enhancement.

**Independent Test**: Can be fully tested by uploading a resume to a profile that already has data, and confirming that existing filled-in fields are left untouched while new work/education/skill entries from the resume are added, and previously-empty fields get filled in.

**Acceptance Scenarios**:

1. **Given** a master profile with existing contact information and work history, **When** the user uploads a new resume containing an additional, more recent job, **Then** the review screen shows the new job as an addition rather than replacing existing entries.
2. **Given** a master profile field that already has a value (e.g., phone number), **When** the uploaded resume contains a different value for that same field, **Then** the existing value is preserved by default and the conflicting extracted value is shown to the user as an available option rather than applied automatically.
3. **Given** a master profile field that is currently empty (e.g., links), **When** the uploaded resume contains a value for that field, **Then** the review screen offers to fill it in.

---

### Edge Cases

- What happens when the uploaded file is not a PDF (e.g., `.docx`, `.jpg`)? The system rejects it with a clear message before attempting extraction.
- What happens when the PDF is password-protected or corrupted and cannot be opened? The system shows an error explaining the file couldn't be read and invites the user to try a different file or enter details manually.
- What happens when the PDF is a scanned image with no extractable text? The system detects that no usable text was found and informs the user that this file can't be auto-filled, rather than presenting an empty or garbage review screen.
- What happens when the uploaded file exceeds the maximum allowed size? The system rejects it before upload completes and states the size limit.
- What happens when the resume layout is unusual (e.g., multi-column, tables, infographic-style) and extraction quality is low? The system still shows whatever it could extract, clearly flags low-confidence or missing sections, and lets the user fill in the rest manually — it never blocks the user from proceeding with manual entry.
- What happens when the resume contains no work experience or no education section at all? The corresponding section is simply left empty in the review screen rather than treated as an error.
- What happens if the user uploads a second resume while a previous review is still unconfirmed? The system treats the newer upload as replacing the pending (unconfirmed) review; nothing already saved to the profile is affected either way, since nothing is saved until confirmation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to upload a single PDF file of their resume from the profile section of the application.
- **FR-002**: System MUST reject files that are not PDFs, with a clear, specific error message.
- **FR-003**: System MUST reject files exceeding the maximum allowed upload size, with a clear error message stating the limit.
- **FR-004**: System MUST extract, where present in the PDF, contact information (full name, email, phone, location, links), work experience entries (company, title, start/end dates, description), education entries (institution, credential/degree, field of study, start/end dates, description), and skills.
- **FR-005**: System MUST present all extracted data in a review step and MUST NOT write any of it to the user's master profile until the user explicitly confirms the import.
- **FR-006**: Users MUST be able to edit, remove, or add to any extracted field or entry before confirming the import.
- **FR-007**: System MUST clearly indicate any fields or sections it could not confidently extract, rather than leaving them silently blank or fabricating a value.
- **FR-008**: System MUST detect PDFs that yield no extractable text (e.g., scanned images) and inform the user the file could not be auto-filled, rather than presenting a blank or nonsensical review.
- **FR-009**: System MUST detect and reject unreadable PDFs (corrupted or password-protected) with an explanatory error rather than failing silently.
- **FR-010**: When the master profile already has a value in a given field, System MUST preserve the existing value by default on import and MUST present the conflicting extracted value to the user as a choice rather than overwriting silently.
- **FR-011**: When the master profile field is currently empty, System MUST offer the corresponding extracted value to fill it, subject to user confirmation per FR-005.
- **FR-012**: System MUST add new work experience, education, and skill entries found in the resume as new entries, never merging or overwriting existing entries of those types.
- **FR-013**: Users MUST be able to cancel the review at any point, leaving the master profile completely unchanged.
- **FR-014**: System MUST NOT require an active AI job-tailoring configuration (e.g., no dependency on a job description) to perform resume upload and extraction — this is a standalone profile-population capability.

### Key Entities

- **Resume Upload**: Represents one user-submitted PDF file and its processing outcome (received, extracted, failed, or expired-pending-review). Tied to the user's account; not retained as a permanent artifact once its review is confirmed or discarded.
- **Extracted Profile Draft**: The staged, not-yet-saved result of parsing a Resume Upload — a proposed set of contact information, work experience entries, education entries, and skills, plus flags for any fields that couldn't be confidently extracted. Exists only until the user confirms or discards it.
- **Master Profile** *(existing entity)*: The user's single source-of-truth profile that Extracted Profile Draft values are merged into upon confirmation.
- **Work Experience Entry / Education Entry / Skill** *(existing entities)*: The profile sub-records that new entries from an Extracted Profile Draft are added to.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user with a well-formatted, single-column PDF resume can go from upload to a fully reviewed and confirmed profile (contact info plus at least one work experience and one education entry) in under 3 minutes.
- **SC-002**: For well-formatted, text-based PDF resumes, at least 80% of standard fields (name, email, phone, most recent job title, most recent company, most recent institution) are correctly extracted without manual correction.
- **SC-003**: 100% of extracted data is visible to the user for review, and zero profile changes occur without explicit user confirmation.
- **SC-004**: Users uploading a resume to update an existing profile never lose previously-entered data they did not explicitly choose to replace.
- **SC-005**: Users attempting to upload an unsupported file type or an unreadable PDF receive an explanatory message within a few seconds, with no unhandled failure or blank/frozen screen.

## Assumptions

- Only PDF is supported in this iteration; other formats (DOCX, images, plain text) are out of scope and rejected with a clear message.
- A user uploads and reviews one resume at a time; batch/multi-file upload is out of scope.
- The maximum upload file size follows common web-application norms (e.g., in the low tens of megabytes); the exact limit is a technical/configuration detail, not a product decision, and can be tuned during implementation.
- Extraction is best-effort text/layout parsing of the PDF; it does not require any AI/LLM involvement beyond what's needed to structure extracted text into profile fields, and is a distinct capability from the existing AI-driven resume *tailoring* feature.
- The Extracted Profile Draft (staged review data) is temporary and does not need to persist indefinitely — if abandoned, it can be safely discarded (e.g., after the session ends or after a reasonable inactivity period).
- Resumes are assumed to be in a language the extraction process can handle at a basic level (e.g., English); non-English resumes may yield lower extraction quality, which is acceptable per the edge-case handling (partial extraction, manual fallback).
