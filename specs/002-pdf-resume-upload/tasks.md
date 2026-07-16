---

description: "Task list for feature implementation"
---

# Tasks: PDF Resume Upload & Profile Prepopulation

**Input**: Design documents from `/specs/002-pdf-resume-upload/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rest-api.md, quickstart.md

**Tests**: Not explicitly requested in spec.md; no dedicated test-writing tasks are embedded in the user-story phases. Test tasks (T023–T027) are included in the Polish phase instead, matching plan.md's testing stack (JUnit5/Mockito/Testcontainers, Vitest, Playwright) and this repo's existing convention (see specs/001-ai-resume-builder/tasks.md). Move them earlier and write them first if a TDD approach is desired.

**Organization**: Tasks are grouped by user story (US1–US3, matching spec.md priorities P1/P1/P2) to enable independent implementation and testing of each story. This feature extends the existing `backend/`/`frontend/` codebase from specs/001-ai-resume-builder — no new project scaffolding is needed, only the new `resumeimport` backend package and `resumeImport` frontend feature folder defined in plan.md.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths follow the `backend/` (Java 25 + Spring Boot + Maven) / `frontend/` (React + Tailwind + Vite) structure defined in plan.md

## Path Conventions

- **Backend**: `backend/src/main/java/com/resumerocket/resumeimport/...` (new package), reusing `backend/src/main/java/com/resumerocket/profile/...` (unchanged) as the write path
- **Frontend**: `frontend/src/features/resumeImport/...` (new folder), `frontend/src/services/resumeImportService.ts`, wired into the existing `frontend/src/pages/ProfilePage.tsx`

---

## Phase 1: Setup

**Purpose**: The one piece of shared configuration every story depends on

- [X] T001 Add `spring.servlet.multipart.max-file-size: 10MB` and `spring.servlet.multipart.max-request-size: 10MB` to `backend/src/main/resources/application.yml` (research.md #5, FR-003)

**Checkpoint**: Backend accepts multipart uploads up to the configured limit; no feature code yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The stateless extraction endpoint that every user story's review screen depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Create response DTOs `ResumeImportResult`, `ContactInfoCandidate`, `WorkExperienceCandidate`, `EducationCandidate`, `SkillCandidate` per data-model.md in `backend/src/main/java/com/resumerocket/resumeimport/dto/ResumeImportResult.java`, `ContactInfoCandidate.java`, `WorkExperienceCandidate.java`, `EducationCandidate.java`, `SkillCandidate.java`
- [X] T003 [P] Implement `PdfTextExtractor` (Apache PDFBox `PDFTextStripper` wrapper; throws a distinct exception for `PDDocument.load()` failure vs. empty/near-empty extracted text) in `backend/src/main/java/com/resumerocket/resumeimport/PdfTextExtractor.java` (research.md #1, #6; FR-008, FR-009)
- [X] T004 [P] Define the `AiResumeExtractionService` interface (`ResumeImportResult extract(String resumeText)`) in `backend/src/main/java/com/resumerocket/resumeimport/AiResumeExtractionService.java` (research.md #2)
- [X] T005 Implement `OpenAiResumeExtractionServiceImpl`: system prompt specifying the `ResumeImportResult` JSON shape, `chatClient.prompt().system(...).user(resumeText).call().content()`, lenient JSON parsing (strip markdown fences, Jackson `readTree`) mirroring `tailoring/OpenAiTailoringServiceImpl`, populating `fieldsNotExtracted`/`lowConfidence` per data-model.md in `backend/src/main/java/com/resumerocket/resumeimport/OpenAiResumeExtractionServiceImpl.java` (depends on T002, T004)
- [X] T006 Implement `ResumeImportService`: validate content-type is `application/pdf` and size ≤ configured limit, run `PdfTextExtractor`, call `AiResumeExtractionService`, translate failures into the error codes from data-model.md (`UNSUPPORTED_FILE_TYPE`, `FILE_TOO_LARGE`, `UNREADABLE_PDF`, `NO_EXTRACTABLE_TEXT`, `EXTRACTION_SERVICE_FAILED`) in `backend/src/main/java/com/resumerocket/resumeimport/ResumeImportService.java` (depends on T003, T005)
- [X] T007 Add the new error-code exception types and register their HTTP status mappings (400/413/422/422/502 per data-model.md § Error responses) in `backend/src/main/java/com/resumerocket/resumeimport/exception/` and `backend/src/main/java/com/resumerocket/common/GlobalExceptionHandler.java` (depends on T006)
- [X] T008 Implement `ResumeImportController` exposing `POST /profile/resume-import` (multipart field `file`) per contracts/rest-api.md in `backend/src/main/java/com/resumerocket/resumeimport/ResumeImportController.java` (depends on T006, T007)
- [X] T009 [P] Implement frontend `resumeImportService.uploadResume(file)` (multipart `FormData` POST to `/profile/resume-import`, typed `ResumeImportResult` response, surfaces the backend error codes) in `frontend/src/services/resumeImportService.ts`

**Checkpoint**: `POST /profile/resume-import` is fully functional and independently callable (e.g. via curl/Postman) — returns structured candidate data for a valid PDF and the correct error code for each failure case. No UI exists yet.

---

## Phase 3: User Story 1 - Bootstrap a new profile from an existing resume (Priority: P1) 🎯 MVP

**Goal**: A user with an empty profile uploads a PDF resume, sees the extracted data in a review screen, and confirming it populates their master profile — with cancel leaving the profile untouched.

**Independent Test**: Upload a well-formatted single-column PDF resume to a brand-new (empty) profile, confirm the extracted data, and verify the profile is populated without any manual field entry; verify cancel leaves it empty.

### Implementation for User Story 1

- [X] T010 [P] [US1] Build `ResumeUploadButton` (file input restricted to `.pdf`, upload trigger, loading state, maps each backend error code from T006/T008 to a specific user-facing message) in `frontend/src/features/resumeImport/ResumeUploadButton.tsx` (depends on T009)
- [X] T011 [US1] Build `ResumeImportReview` displaying the `ResumeImportResult`: contact info fields, work experience/education/skill candidates, each with a default-checked include checkbox, in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T009)
- [X] T012 [US1] Implement the confirm handler: for each approved contact field call `profileService.updateProfile`, for each approved candidate call the corresponding existing `profileService.addEducation` / `addWorkExperience` / `addSkill`, then invalidate the `"profile"` query key in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T011; contracts/rest-api.md § Confirming an import; FR-005)
- [X] T013 [US1] Implement cancel/close handling that discards the local `ResumeImportResult` state and calls no mutation endpoint in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T011; FR-013)
- [X] T014 [US1] Wire `<ResumeUploadButton />` and `<ResumeImportReview />` into `frontend/src/pages/ProfilePage.tsx` (depends on T010, T012, T013)

**Checkpoint**: User Story 1 is fully functional and independently testable — upload → review → confirm populates an empty profile; cancel leaves it untouched.

---

## Phase 4: User Story 2 - Review and correct extracted data before saving (Priority: P1)

**Goal**: Before anything is saved, the user can edit any extracted field, remove any extracted entry, and see which fields the extractor couldn't confidently fill in.

**Independent Test**: Upload a resume that produces at least one incorrect or missing field, edit that field in the review screen, confirm, and verify the corrected value (not the originally-extracted one) is what was saved; verify a low-confidence field is visibly flagged.

### Implementation for User Story 2

- [X] T015 [US2] Replace the read-only field display from T011 with inline-editable inputs for every contact field and every work/education/skill candidate field in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T011; FR-006)
- [X] T016 [US2] Wire each candidate's include checkbox to exclude it from the T012 confirm submission when unchecked (remove-before-save) in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T012, T015; FR-006)
- [X] T017 [US2] Render a visible "needs manual input" indicator for any field/entry where `fieldsNotExtracted`/`lowConfidence` is set, instead of leaving it blank or silently guessed in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T015; FR-007)
- [X] T018 [US2] Tighten the `OpenAiResumeExtractionServiceImpl` system prompt (T005) to explicitly instruct the model to mark a field as not-extracted rather than fabricate a value when it's not confident, and verify `fieldsNotExtracted`/`lowConfidence` populate accordingly in `backend/src/main/java/com/resumerocket/resumeimport/OpenAiResumeExtractionServiceImpl.java` (depends on T005; FR-007)

**Checkpoint**: User Stories 1 and 2 together are independently testable — the full extract → correct → confirm flow works end-to-end on an empty profile.

---

## Phase 5: User Story 3 - Update an existing profile from a newer resume (Priority: P2)

**Goal**: For a profile that already has data, uploading a new resume never silently overwrites an existing filled field, and new work/education/skill entries are added rather than merged into existing ones.

**Independent Test**: Upload a resume to a profile that already has data; verify existing filled-in fields are left untouched by default (conflicting extracted values offered, not applied), new entries from the resume are added, and previously-empty fields get filled in.

### Implementation for User Story 3

- [X] T019 [US3] Pass the profile already loaded by `ProfilePage`'s existing `GET /profile` query into `ResumeImportReview` as a prop in `frontend/src/pages/ProfilePage.tsx` (depends on T014)
- [X] T020 [US3] For each contact field, compute against the passed-in current profile whether it's already filled; default-select "keep existing" (extracted value shown as a selectable alternative) when filled, default-select the extracted value when currently empty, per research.md #4 in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T015, T019; FR-010, FR-011)
- [X] T021 [US3] Verify/adjust the T012 confirm handler so every approved work experience/education/skill candidate is always submitted via `POST` as a new entry, never matched or merged against an existing one, in `frontend/src/features/resumeImport/ResumeImportReview.tsx` (depends on T012, T020; FR-012)

**Checkpoint**: All three user stories are independently functional — new and returning users can both safely use the feature without data loss.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Automated test coverage and final formatting/validation pass

- [X] T022 [P] Backend unit tests for `PdfTextExtractor` against fixture PDFs (normal text, empty/scanned, corrupted/password-protected) in `backend/src/test/java/com/resumerocket/resumeimport/PdfTextExtractorTest.java`
- [X] T023 [P] Backend unit test for `ResumeImportService` orchestration and error-code mapping, with `AiResumeExtractionService` mocked (never calls OpenAI, no `OPENAI_API_KEY` required) in `backend/src/test/java/com/resumerocket/resumeimport/ResumeImportServiceTest.java`
- [X] T024 [P] Backend Testcontainers integration test for `POST /profile/resume-import` covering the happy path and each error code, with `AiResumeExtractionService` mocked, in `backend/src/test/java/com/resumerocket/resumeimport/ResumeImportControllerIT.java`
- [X] T025 [P] Frontend Vitest tests for `ResumeImportReview` edit/include-exclude/low-confidence-flag/conflict-default behavior, mocking `resumeImportService` and `profileService`, in `frontend/tests/unit/ResumeImportReview.test.tsx`
- [X] T026 [P] Playwright e2e test driving the real stack: upload a fixture PDF → review screen → confirm → `GET /profile` reflects the new data, in `e2e-testing/tests/resume-import.spec.ts`
- [X] T027 Run `mvn spotless:apply` (`backend/`) and `npm run format` (`frontend/`) per CLAUDE.md formatting conventions
- [X] T028 Execute the quickstart.md scenarios (Scenarios 1–3 plus all five edge cases) end-to-end against the running stack

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories (no story has a working endpoint to call before this completes)
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - US1 and US2 are both P1 and share the same file (`ResumeImportReview.tsx`) — implement sequentially in the order listed (US1 first, US2 builds directly on it)
  - US3 depends on US1's `ProfilePage` wiring (T014) and US2's editable fields (T015) being in place, since it refines the same review screen with conflict-aware defaults
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependency on other stories
- **User Story 2 (P1)**: Builds directly on US1's `ResumeImportReview.tsx` (same file, additive changes) — implement after US1
- **User Story 3 (P2)**: Builds on both US1 (`ProfilePage` wiring) and US2 (editable fields) — implement last

### Within Each User Story

- Services/DTOs before controllers (Foundational phase)
- Review screen skeleton (US1) before edit/remove controls (US2) before conflict-aware defaults (US3)
- Story complete and checkpoint-verified before moving to the next priority

### Parallel Opportunities

- T002, T003, T004 (Foundational DTOs/extractor/interface) can run in parallel — different files, no interdependencies
- T009 (frontend service) can run in parallel with any backend Foundational task once the contract (contracts/rest-api.md) is fixed
- T010 (upload button) can run in parallel with T011 (review screen skeleton) within US1 — different files
- All Polish-phase test tasks (T022-T026) can run in parallel — different files, independent of each other

---

## Parallel Example: Foundational Phase

```bash
# Launch independent Foundational tasks together:
Task: "Create response DTOs in backend/src/main/java/com/resumerocket/resumeimport/dto/*.java"
Task: "Implement PdfTextExtractor in backend/src/main/java/com/resumerocket/resumeimport/PdfTextExtractor.java"
Task: "Define AiResumeExtractionService interface in backend/src/main/java/com/resumerocket/resumeimport/AiResumeExtractionService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run quickstart.md Scenario 1 — upload a well-formatted resume to an empty profile, confirm, verify `GET /profile` is populated; confirm cancel leaves it empty
5. Deploy/demo if ready — note that without US2's edit/remove controls, the MVP only supports accept-all-or-cancel, and without US3's conflict defaults it should only be demoed against empty profiles

### Incremental Delivery

1. Complete Setup + Foundational → extraction endpoint ready and independently callable
2. Add User Story 1 → test independently (quickstart Scenario 1) → deploy/demo (MVP!)
3. Add User Story 2 → test independently (quickstart Scenario 2) → deploy/demo
4. Add User Story 3 → test independently (quickstart Scenario 3) → deploy/demo
5. Polish phase → automated test coverage, formatting, full quickstart re-validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 and US2 intentionally share `ResumeImportReview.tsx` rather than being fully file-isolated, because they are two facets of one review screen (per spec.md, both are P1 and US1's acceptance scenarios explicitly require the review-before-save behavior US2 elaborates on) — implement them in sequence, not in parallel
- No new Flyway migration or persisted entity is introduced by this feature (research.md #3) — do not add one
- Commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- Avoid: vague tasks, same-file conflicts within a single phase, cross-story dependencies that break independence beyond the intentional US1→US2→US3 sequencing noted above
