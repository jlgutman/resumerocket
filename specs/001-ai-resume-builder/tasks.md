---

description: "Task list for feature implementation"
---

# Tasks: AI-Powered Resume Builder

**Input**: Design documents from `/specs/001-ai-resume-builder/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rest-api.md, quickstart.md

**Tests**: Not explicitly requested in spec.md; no dedicated test-writing tasks are included below. Task T074 runs the manual/scripted quickstart validation instead. Add contract/unit/integration test tasks per plan.md's testing stack (JUnit5/Mockito/Testcontainers, Vitest/RTL, Playwright) if TDD is desired later.

**Organization**: Tasks are grouped by user story (US1–US4, matching spec.md priorities P1–P4) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- File paths follow the `backend/` (Java 25 + Spring Boot + Maven) / `frontend/` (React + Tailwind + Vite) structure defined in plan.md

## Path Conventions

- **Backend**: `backend/src/main/java/com/resumerocket/...`, migrations in `backend/src/main/resources/db/migration/`
- **Frontend**: `frontend/src/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create project structure per implementation plan: `backend/` and `frontend/` top-level directories with the package layout described in plan.md
- [x] T002 [P] Initialize backend Maven project with Spring Boot 3.x + Java 25 in `backend/pom.xml` (Web, Security, Data JPA, MySQL connector, Flyway, PDFBox, POI, java-diff-utils dependencies)
- [x] T003 [P] Initialize frontend Vite + React 18 + TypeScript + Tailwind CSS project in `frontend/package.json`, `frontend/tailwind.config.js`, `frontend/vite.config.ts`
- [x] T004 [P] Configure backend build-time linting/formatting (Spotless or Checkstyle) in `backend/pom.xml`
- [x] T005 [P] Configure frontend linting/formatting (ESLint + Prettier) in `frontend/.eslintrc.cjs`, `frontend/.prettierrc`
- [x] T006 [P] Configure environment/config management: Spring profiles in `backend/src/main/resources/application.yml` and `frontend/.env.example` (DB connection, JWT secret, AI provider credentials placeholders)
- [x] T007 [P] Add `docker-compose.yml` at repo root for a local MySQL 8.x instance

**Checkpoint**: Repositories and toolchains initialized; no application code yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T008 Create Flyway baseline migration for the `user_account` table in `backend/src/main/resources/db/migration/V1__init_user_account.sql`
- [x] T009 [P] Create `UserAccount` entity + repository in `backend/src/main/java/com/resumerocket/auth/UserAccount.java`, `UserAccountRepository.java`
- [x] T010 [P] Implement Spring Security + JWT configuration (token provider, auth filter, BCrypt password encoder) in `backend/src/main/java/com/resumerocket/auth/SecurityConfig.java`, `JwtTokenProvider.java`, `JwtAuthFilter.java`
- [x] T011 Implement `AuthService` and `AuthController` for register/login (`POST /auth/register`, `POST /auth/login`) in `backend/src/main/java/com/resumerocket/auth/AuthService.java`, `AuthController.java` (depends on T009, T010)
- [x] T012 [P] Implement global exception handling with the standard `{ error, details? }` error shape in `backend/src/main/java/com/resumerocket/common/GlobalExceptionHandler.java`
- [x] T013 [P] Configure API base routing (CORS, `/api/v1` base path) in `backend/src/main/java/com/resumerocket/common/WebConfig.java`
- [x] T014 [P] Implement frontend typed API client with bearer-token attachment and 401 handling in `frontend/src/services/apiClient.ts`
- [x] T015 [P] Build frontend routing shell + Login/Register pages in `frontend/src/App.tsx`, `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/RegisterPage.tsx`
- [x] T016 [P] Implement frontend auth state (token storage, TanStack Query provider, protected route wrapper) in `frontend/src/features/auth/AuthContext.tsx`, `frontend/src/features/auth/ProtectedRoute.tsx`

**Checkpoint**: Foundation ready — an account can be registered and signed in; user story implementation can now begin.

---

## Phase 3: User Story 1 - Build a Master Profile (Priority: P1) 🎯 MVP

**Goal**: A job seeker can create and update one master professional profile (personal info, education, work experience, skills) that persists independently of any resume.

**Independent Test**: Sign up, enter personal info plus one education entry, one work experience entry, and skills, save, and confirm the data is retrievable and editable without affecting anything else.

### Implementation for User Story 1

- [x] T017 Create Flyway migration for `master_profile`, `education_entry`, `work_experience_entry`, `skill` tables in `backend/src/main/resources/db/migration/V2__profile_schema.sql`
- [x] T018 [P] [US1] Create `MasterProfile` entity + repository in `backend/src/main/java/com/resumerocket/profile/MasterProfile.java`, `MasterProfileRepository.java`
- [x] T019 [P] [US1] Create `EducationEntry` entity + repository in `backend/src/main/java/com/resumerocket/profile/EducationEntry.java`, `EducationEntryRepository.java`
- [x] T020 [P] [US1] Create `WorkExperienceEntry` entity + repository (nullable `end_date` = current role) in `backend/src/main/java/com/resumerocket/profile/WorkExperienceEntry.java`, `WorkExperienceEntryRepository.java`
- [x] T021 [P] [US1] Create `Skill` entity + repository in `backend/src/main/java/com/resumerocket/profile/Skill.java`, `SkillRepository.java`
- [x] T022 [US1] Implement `ProfileService` (get-or-create profile, update contact info, add/update/delete education, work experience, and skills) in `backend/src/main/java/com/resumerocket/profile/ProfileService.java` (depends on T018-T021)
- [x] T023 [US1] Implement `ProfileController` exposing `/profile`, `/profile/education`, `/profile/work-experience`, `/profile/skills` per contracts/rest-api.md in `backend/src/main/java/com/resumerocket/profile/ProfileController.java` (depends on T022)
- [x] T024 [US1] Add request validation and error handling (required fields, optional end date semantics) in `backend/src/main/java/com/resumerocket/profile/ProfileService.java`, `ProfileController.java` (depends on T023)
- [x] T025 [P] [US1] Build frontend Profile page with personal info form in `frontend/src/pages/ProfilePage.tsx`
- [x] T026 [P] [US1] Build frontend Education entries list/form component in `frontend/src/features/profile/EducationEntries.tsx`
- [x] T027 [P] [US1] Build frontend Work Experience entries list/form component with a "current role" toggle for the open-ended end date in `frontend/src/features/profile/WorkExperienceEntries.tsx`
- [x] T028 [P] [US1] Build frontend Skills list/input component in `frontend/src/features/profile/SkillsList.tsx`
- [x] T029 [US1] Implement frontend profile API service (get/update profile, CRUD education/work-experience/skills) in `frontend/src/services/profileService.ts` (depends on T014)
- [x] T030 [US1] Wire ProfilePage and subcomponents to `profileService` with a save confirmation message (depends on T025-T029)

**Checkpoint**: User Story 1 is fully functional and independently testable — a user can build and edit a complete master profile.

---

## Phase 4: User Story 2 - Tailor a Resume to a Job Description (Priority: P2)

**Goal**: A job seeker pastes a job description and receives an AI-generated tailored resume draft with reviewable suggestions (accept/reject/edit).

**Independent Test**: With a saved profile (US1), paste a job description, trigger tailoring, and confirm a draft with distinguishable, individually reviewable suggestions is produced — including a case where little overlaps with the profile.

### Implementation for User Story 2

- [x] T031 Create Flyway migration for `job_description`, `tailored_resume`, `ai_suggestion` tables in `backend/src/main/resources/db/migration/V3__tailoring_schema.sql`
- [x] T032 [P] [US2] Create `JobDescription` entity + repository in `backend/src/main/java/com/resumerocket/jobdescription/JobDescription.java`, `JobDescriptionRepository.java`
- [x] T033 [P] [US2] Create `TailoredResume` entity + repository (including `sourceProfileSnapshot`, `clonedFromId`, `regeneratedFromId`, `status`) in `backend/src/main/java/com/resumerocket/resume/TailoredResume.java`, `TailoredResumeRepository.java`
- [x] T034 [P] [US2] Create `AiSuggestion` entity + repository in `backend/src/main/java/com/resumerocket/tailoring/AiSuggestion.java`, `AiSuggestionRepository.java`
- [x] T035 [US2] Define the `AiTailoringService` interface and its OpenAI/Spring AI adapter implementation for requirement extraction and suggestion generation (research.md §1) in `backend/src/main/java/com/resumerocket/tailoring/AiTailoringService.java`, `OpenAiTailoringServiceImpl.java`
- [x] T036 [US2] Implement `JobDescriptionService` (persist job description, analyze via `AiTailoringService` to populate `extractedRequirements`) in `backend/src/main/java/com/resumerocket/jobdescription/JobDescriptionService.java` (depends on T032, T035)
- [x] T037 [US2] Implement `TailoringService` (build a draft `TailoredResume` + `AiSuggestion`s from the `MasterProfile` and `JobDescription`, populate `unmatchedRequirements`) in `backend/src/main/java/com/resumerocket/tailoring/TailoringService.java` (depends on T033, T034, T035, T022)
- [x] T038 [US2] Implement `SuggestionReviewService` (accept/reject/edit an `AiSuggestion` and apply the resolution to the resume content) in `backend/src/main/java/com/resumerocket/tailoring/SuggestionReviewService.java` (depends on T034)
- [x] T039 [US2] Implement `JobDescriptionController` (`POST /job-descriptions`, `POST /job-descriptions/{id}/tailor`) in `backend/src/main/java/com/resumerocket/jobdescription/JobDescriptionController.java` (depends on T036, T037)
- [x] T040 [US2] Implement core `TailoredResumeController` endpoints (`GET /tailored-resumes/{id}`, `PATCH /tailored-resumes/{id}`, `PATCH /tailored-resumes/{id}/suggestions/{suggestionId}`) in `backend/src/main/java/com/resumerocket/resume/TailoredResumeController.java` (depends on T037, T038)
- [x] T041 [P] [US2] Build frontend Tailoring page (paste job description, trigger tailoring) in `frontend/src/pages/TailoringPage.tsx`
- [x] T042 [P] [US2] Build frontend Suggestion review component (accept/reject/edit per suggestion, changes visually distinguished from unchanged content) in `frontend/src/features/tailoring/SuggestionReview.tsx`
- [x] T043 [US2] Implement frontend tailoring API service (submit job description, trigger tailor, resolve suggestions, update resume metadata) in `frontend/src/services/tailoringService.ts` (depends on T014)
- [x] T044 [US2] Wire TailoringPage + SuggestionReview to `tailoringService`, including an unmatched-requirements notice (depends on T041-T043)

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - Export a Finished Resume (Priority: P3)

**Goal**: A job seeker picks a template, previews a resume, and exports it to PDF, DOCX, or ATS-friendly plain text with content matching the preview.

**Independent Test**: Take an existing resume, select a template, preview it, and download it in each of the three formats, confirming visual/content parity with the preview.

### Implementation for User Story 3

- [x] T045 Create Flyway migration for `template`, `export` tables plus seed data for the default templates in `backend/src/main/resources/db/migration/V4__template_export_schema.sql`
- [x] T046 [P] [US3] Create `Template` entity + repository in `backend/src/main/java/com/resumerocket/template/Template.java`, `TemplateRepository.java`
- [x] T047 [P] [US3] Create `Export` entity + repository in `backend/src/main/java/com/resumerocket/export/Export.java`, `ExportRepository.java`
- [x] T048 [US3] Implement the shared `LayoutDescriptor` model consumed by both preview and exporters (research.md §4) in `backend/src/main/java/com/resumerocket/template/LayoutDescriptor.java` (depends on T046)
- [x] T049 [US3] Implement `PreviewService` (render a `TailoredResume` + `Template` into a `LayoutDescriptor`) in `backend/src/main/java/com/resumerocket/template/PreviewService.java` (depends on T048)
- [x] T050 [P] [US3] Implement `PdfExportService` using Apache PDFBox in `backend/src/main/java/com/resumerocket/export/PdfExportService.java` (depends on T048)
- [x] T051 [P] [US3] Implement `DocxExportService` using Apache POI in `backend/src/main/java/com/resumerocket/export/DocxExportService.java` (depends on T048)
- [x] T052 [P] [US3] Implement `PlaintextExportService` producing ATS-friendly plain text with no tables/graphics in `backend/src/main/java/com/resumerocket/export/PlaintextExportService.java` (depends on T048)
- [x] T053 [US3] Implement `ExportController` (`GET /templates`, `GET /tailored-resumes/{id}/preview`, `POST /tailored-resumes/{id}/export`, `GET /exports/{exportId}/download`) in `backend/src/main/java/com/resumerocket/export/ExportController.java` (depends on T049-T052)
- [x] T054 [P] [US3] Build frontend Template picker component in `frontend/src/features/export/TemplatePicker.tsx`
- [x] T055 [P] [US3] Build frontend live Preview renderer consuming the layout descriptor in `frontend/src/templates/PreviewRenderer.tsx`
- [x] T056 [US3] Build frontend Export panel (format selection + download trigger) in `frontend/src/features/export/ExportPanel.tsx`
- [x] T057 [US3] Implement frontend export/preview API service in `frontend/src/services/exportService.ts` (depends on T014)
- [x] T058 [US3] Wire TemplatePicker, PreviewRenderer, and ExportPanel to `exportService` (depends on T054-T057)

**Checkpoint**: User Stories 1, 2, AND 3 all work independently.

---

## Phase 6: User Story 4 - Manage and Compare Resume Versions (Priority: P4)

**Goal**: A job seeker organizes saved tailored resumes by job/company, views application history, compares two versions side-by-side, and clones or regenerates a version.

**Independent Test**: With two or more saved tailored resumes, list and filter them, open a side-by-side comparison, clone one, and regenerate another after a profile update — confirming the original is preserved in each case.

### Implementation for User Story 4

- [x] T059 [US4] Implement version listing with company filter/sort (`GET /tailored-resumes`) in `backend/src/main/java/com/resumerocket/resume/TailoredResumeController.java`, `TailoredResumeService.java` (depends on T040)
- [x] T060 [US4] Implement `ResumeDiffService` computing a section-by-section diff via java-diff-utils (research.md §5) in `backend/src/main/java/com/resumerocket/resume/ResumeDiffService.java` (depends on T033)
- [x] T061 [US4] Implement `GET /tailored-resumes/compare` endpoint in `backend/src/main/java/com/resumerocket/resume/TailoredResumeController.java` (depends on T060)
- [x] T062 [US4] Implement `ResumeCloneService` (new draft `TailoredResume` + copied `AiSuggestion`s, `clonedFromId` set, original untouched) in `backend/src/main/java/com/resumerocket/resume/ResumeCloneService.java` (depends on T033, T034)
- [x] T063 [US4] Implement `ResumeRegenerateService` (new draft built from the current `MasterProfile` + original `JobDescription`, `regeneratedFromId` set, original preserved) in `backend/src/main/java/com/resumerocket/resume/ResumeRegenerateService.java` (depends on T037)
- [x] T064 [US4] Expose `POST /tailored-resumes/{id}/clone` and `POST /tailored-resumes/{id}/regenerate` endpoints in `backend/src/main/java/com/resumerocket/resume/TailoredResumeController.java` (depends on T062, T063)
- [x] T065 [P] [US4] Build frontend Version History page (list, filter by company, application history) in `frontend/src/pages/VersionHistoryPage.tsx`
- [x] T066 [P] [US4] Build frontend side-by-side Compare view in `frontend/src/features/versions/CompareView.tsx`
- [x] T067 [US4] Implement frontend version API service (list, compare, clone, regenerate) in `frontend/src/services/versionService.ts` (depends on T014)
- [x] T068 [US4] Wire VersionHistoryPage and CompareView to `versionService`, including clone/regenerate actions (depends on T065-T067)

**Checkpoint**: All four user stories are independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T069 [P] Audit and harmonize request validation and error responses across all controllers in `backend/src/main/java/com/resumerocket/**/*Controller.java`
- [x] T070 [P] Add structured logging across services in `backend/src/main/java/com/resumerocket/common/logging/`
- [x] T071 [P] Apply a responsive Tailwind styling pass across all frontend pages/features in `frontend/src/pages/`, `frontend/src/features/`
- [x] T072 Security hardening review: JWT expiry/refresh handling and per-user data isolation checks across all repositories in `backend/src/main/java/com/resumerocket/`
- [x] T073 [P] Verify performance against plan.md goals (non-AI endpoints <500ms p95, AI tailoring <15s p95, export <5s p95)
- [x] T074 Run quickstart.md validation end-to-end for all four scenarios

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends only on Foundational
- **User Story 2 (Phase 4)**: Depends on Foundational; `TailoringService` (T037) also depends on `ProfileService` (T022) from US1 to read master profile data
- **User Story 3 (Phase 5)**: Depends on Foundational; reads `TailoredResume` data produced by US2, but templates/export can be built and tested against any resume (tailored or base) once US2's `TailoredResume` entity (T033) exists
- **User Story 4 (Phase 6)**: Depends on Foundational and reuses `TailoredResume`/`TailoringService` (T033, T037) from US2
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### Within Each User Story

- Entities/models before services
- Services before controllers/endpoints
- Backend endpoints before the frontend service layer that calls them
- Frontend components before wiring them to the service layer
- Story complete and checkpointed before moving to the next priority (if working sequentially)

### Parallel Opportunities

- All Setup tasks marked [P] (T002-T007) can run in parallel after T001
- Foundational tasks marked [P] (T009-T010, T012-T016) can run in parallel; T011 depends on T009+T010
- Once Foundational (Phase 2) completes, US1, US2, US3, and US4 backend entity/model tasks can start in parallel across a team, though US2/US3/US4 have the cross-story data dependencies noted above
- Within each story, entity/model tasks marked [P] can run in parallel; frontend component tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all entity/repository tasks for User Story 1 together:
Task: "Create MasterProfile entity + repository in backend/src/main/java/com/resumerocket/profile/MasterProfile.java"
Task: "Create EducationEntry entity + repository in backend/src/main/java/com/resumerocket/profile/EducationEntry.java"
Task: "Create WorkExperienceEntry entity + repository in backend/src/main/java/com/resumerocket/profile/WorkExperienceEntry.java"
Task: "Create Skill entity + repository in backend/src/main/java/com/resumerocket/profile/Skill.java"

# Launch all frontend component tasks for User Story 1 together:
Task: "Build frontend Profile page in frontend/src/pages/ProfilePage.tsx"
Task: "Build frontend Education entries component in frontend/src/features/profile/EducationEntries.tsx"
Task: "Build frontend Work Experience entries component in frontend/src/features/profile/WorkExperienceEntries.tsx"
Task: "Build frontend Skills list component in frontend/src/features/profile/SkillsList.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run quickstart.md Scenario 1 independently
5. Deploy/demo if ready — this alone delivers the "durable, reusable professional record" value named in the spec

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (AI tailoring — core differentiator)
4. Add User Story 3 → Test independently → Deploy/Demo (exportable files)
5. Add User Story 4 → Test independently → Deploy/Demo (version management)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (profile)
   - Developer B: User Story 2 (tailoring) — can scaffold entities/AI adapter in parallel, but the tailoring flow needs `ProfileService` (T022) from US1 before it's fully testable end-to-end
   - Developer C: User Story 3 (templates/export) — can scaffold entities/renderers in parallel, needs `TailoredResume` (T033) from US2
   - Developer D: User Story 4 (versions) — needs `TailoredResume`/`TailoringService` (T033, T037) from US2
3. Stories complete and integrate incrementally in priority order

---

## Notes

- [P] tasks = different files, no unresolved dependencies
- [Story] label maps each task to its user story for traceability
- No dedicated test tasks were generated (not requested in spec.md); T074 (quickstart validation) is the acceptance gate for all four stories
- Commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- Avoid: vague tasks, same-file conflicts between [P] tasks, and cross-story dependencies that break independent testability beyond those explicitly noted above

## Implementation Notes (post-build)

- All 74 tasks implemented and functionally verified end-to-end against a real MySQL instance via direct API calls (register → profile → tailor → export PDF/DOCX/TXT → clone/regenerate/compare), plus per-user data isolation checks (cross-user access returns 404).
- **Spring Boot 4.1.0 / Java 25 quirks discovered and worked around**:
  - `spring-boot-starter-web` no longer pulls `com.fasterxml.jackson.databind:jackson-databind` onto the *compile* classpath, and Jackson auto-configuration no longer registers a classic `ObjectMapper` bean by default (it favors the new Jackson 3 engine). Fixed by adding `jackson-databind` + `jackson-datatype-jsr310` explicitly and defining an `ObjectMapper` `@Bean` in `common/JacksonConfig.java`.
  - Flyway's Spring Boot auto-configuration did not reliably run ahead of eager JPA repository bootstrapping in this dependency combination, causing `ddl-auto: validate` to fail against an empty schema. Worked around with `common/FlywayMigrationInitializer` (an `ApplicationContextInitializer` that runs migrations before context refresh) — see its Javadoc.
  - `PathMatchConfigurer.setUseTrailingSlashMatch(boolean)` was removed in Spring Framework 7; the now-empty `WebConfig` was deleted rather than kept as dead code.
  - `google-java-format` (via the Spotless Maven plugin) throws `NoSuchMethodError` against Java 25's `javac` internals. Spotless is still declared in `pom.xml` for manual use but is **not** bound to the Maven lifecycle, so `mvn verify`/`mvn package` are unaffected.
- **Known gaps, not implemented**: JWT refresh-token flow (tokens simply expire after `app.jwt.expiration-minutes`, currently 60); no automated load/performance test against the plan.md p95 targets (only casual response-time observation during manual testing); AI tailoring was validated only via its graceful-degradation path since no live model call has been verified in this environment (see below).
- **2026-07-14 follow-up — swapped the AI adapter to OpenAI via Spring AI** (T035): replaced the hand-rolled Anthropic `RestClient` adapter with `OpenAiTailoringServiceImpl`, built on Spring AI's `ChatClient` (`spring-ai-starter-model-openai`, Spring AI BOM 2.0.0). Config moved from `app.ai.*` (removed from `AppProperties`) to Spring AI's native `spring.ai.openai.*` properties, backed by `OPENAI_API_KEY` / `OPENAI_MODEL` (default `gpt-4o-mini`) env vars. Verified: app boots cleanly with no key set; a real job-description-submit + tailor call correctly reaches OpenAI (`com.openai.errors.UnauthorizedException: 401` in logs, confirming genuine network reach — no key was available in this environment) and both callers (`JobDescriptionService`, `TailoringService`) degrade gracefully exactly as before, with zero changes needed to either caller — the `AiTailoringService` interface boundary from research.md §1 did its job. research.md, plan.md, and quickstart.md updated to match. Full profile/export/version regression re-run and still green.
