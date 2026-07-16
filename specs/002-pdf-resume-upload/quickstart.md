# Quickstart: PDF Resume Upload & Profile Prepopulation

Validates the three user stories end-to-end once implemented. References [data-model.md](./data-model.md) and [contracts/rest-api.md](./contracts/rest-api.md) instead of duplicating field/endpoint details.

## Prerequisites

- Full stack running per the repo root `CLAUDE.md` ("Running the stack"): MySQL via `docker compose up -d`, backend via `mvn spring-boot:run` (`backend/`), frontend via `npm run dev` (`frontend/`).
- `OPENAI_API_KEY` set for the backend — required for this feature's extraction step (unlike base tailoring, there is no no-key fallback here; see data-model.md § Error responses, `EXTRACTION_SERVICE_FAILED`).
- A small set of fixture PDF resumes to upload during manual testing:
  - A well-formatted, single-column, text-based resume (for the happy path).
  - A non-PDF file, e.g. a `.docx` or `.jpg` (for FR-002).
  - A PDF over the 10MB limit (for FR-003) — a PDF with a large embedded image works.
  - A scanned/image-only PDF with no text layer (for FR-008) — e.g. a PDF made from a photo of a printed resume.
  - A password-protected or intentionally corrupted PDF (for FR-009).

## Scenario 1 — Bootstrap an empty profile from a resume (User Story 1, P1)

1. Register a new account and sign in; confirm the master profile is empty (`GET /profile`).
2. On the Profile page, use the new upload entry point to submit the well-formatted fixture PDF (`POST /profile/resume-import`).
3. **Expected**: A review screen appears showing extracted contact info, work experience entries, education entries, and skills (FR-004, FR-005) — nothing has been saved yet (`GET /profile` still returns the pre-upload empty state).
4. Confirm the import without changes.
5. **Expected**: `GET /profile` now reflects the extracted contact info and all extracted work/education/skill entries (Acceptance Scenario 2 of User Story 1).
6. Repeat steps 2–3, but this time close/cancel the review screen instead of confirming.
7. **Expected**: `GET /profile` is unchanged from before the second upload (Acceptance Scenario 3, FR-013).

## Scenario 2 — Correct extracted data before saving (User Story 2, P1)

1. Upload the well-formatted fixture PDF again to a fresh account.
2. In the review screen, edit one extracted field (e.g. correct a misparsed phone number) and remove one extracted entry (e.g. delete one skill).
3. Confirm the import.
4. **Expected**: `GET /profile` reflects the *edited* value, not the originally-extracted one, and the removed entry is absent (FR-006).
5. Upload a resume fixture with an unusual/multi-column layout likely to produce low-confidence extraction.
6. **Expected**: The review screen visibly flags at least one field or entry as low-confidence/needs-manual-input rather than silently leaving it blank or guessing (FR-007).

## Scenario 3 — Update an existing profile without losing data (User Story 3, P2)

1. Using the account from Scenario 1 (profile already populated from the first upload), manually edit one field via the normal profile UI (e.g. set a location) and note the current work experience entries.
2. Upload a second fixture resume containing one additional job not already in the profile, and a different phone number than what's currently saved.
3. **Expected** in the review screen: the additional job is presented as a new addition (not replacing existing entries); the conflicting phone number is *not* pre-applied — the existing value is preserved by default with the extracted value offered as a selectable alternative (FR-010).
4. Confirm without changing the phone number selection.
5. **Expected**: `GET /profile` shows the original phone number unchanged, the new job added alongside the existing ones, and any previously-empty field the new resume filled in now populated (FR-011, FR-012).

## Edge cases to exercise manually

- Upload the non-PDF fixture → expect a clear rejection before any review screen appears (`UNSUPPORTED_FILE_TYPE`, FR-002).
- Upload the oversized PDF fixture → expect rejection stating the size limit (`FILE_TOO_LARGE`, FR-003).
- Upload the scanned/image-only PDF fixture → expect a message explaining the file couldn't be auto-filled, not a blank or broken review screen (`NO_EXTRACTABLE_TEXT`, FR-008).
- Upload the corrupted/password-protected PDF fixture → expect an explanatory error (`UNREADABLE_PDF`, FR-009).
- Temporarily unset `OPENAI_API_KEY` and upload the well-formatted fixture → expect `EXTRACTION_SERVICE_FAILED` surfaced as a clear error, not a hang or silent failure.

## Success Criteria Checkpoints

Map each scenario back to [spec.md](./spec.md) Success Criteria: Scenario 1 → SC-001, SC-003; Scenario 2 → SC-002; Scenario 3 → SC-004; Edge cases → SC-005.
