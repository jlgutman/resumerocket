# Implementation Plan: PDF Resume Upload & Profile Prepopulation

**Branch**: `002-pdf-resume-upload` | **Date**: 2026-07-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-pdf-resume-upload/spec.md`

## Summary

Users upload a PDF resume from the Profile page; the backend extracts its text (Apache PDFBox, already a dependency) and passes it to the existing OpenAI `ChatClient` infrastructure to produce structured candidate profile data (contact info, work experience, education, skills). The result is returned directly in the HTTP response — nothing is persisted. The frontend shows a review screen (mirroring the existing AI-suggestion review pattern) where the user edits/removes/accepts candidates; on confirm, the frontend calls the *existing* profile mutation endpoints (`PUT /profile`, `POST /profile/education`, `POST /profile/work-experience`, `POST /profile/skills`) for whatever was approved, so new entries are always additive and existing filled fields are never silently overwritten.

## Technical Context

**Language/Version**: Java 25 (backend, Spring Boot 4.1); TypeScript + React 18 (frontend) — no new tier, extends the existing stack.

**Primary Dependencies**: Apache PDFBox 3.0.3 (already a backend dependency for PDF export; reused here for text extraction via `PDFTextStripper`), Spring AI `spring-ai-starter-model-openai` `ChatClient` (already used by `tailoring/OpenAiTailoringServiceImpl`; reused here to structure extracted text into JSON), Spring Web multipart file handling (`MultipartFile`, part of `spring-boot-starter-web`, not yet used elsewhere in this codebase but requires no new dependency); React Query + the existing `apiClient` axios instance on the frontend (`FormData` upload, no new dependency).

**Storage**: MySQL via Flyway — **no new tables or migration**. See research.md decision #3: extraction is stateless/synchronous; the only writes are through the existing profile mutation endpoints once the user confirms.

**Testing**: JUnit 5 + Mockito + Spring Boot Test + Testcontainers (MySQL) for the backend, with the new `AiResumeExtractionService` mocked in tests (never calling OpenAI, consistent with `tailoring`'s conventions); Vitest + React Testing Library for the frontend; Playwright for the end-to-end upload → review → confirm flow.

**Target Platform**: Server: Linux container (existing Spring Boot jar); Client: modern evergreen browsers — unchanged from feature 001.

**Project Type**: Web application (existing `backend/` + `frontend/` split); this feature adds one new backend feature-package and one new frontend feature-folder, no new services/tiers.

**Performance Goals**: Upload + text extraction + LLM structuring completes in <15s p95 (consistent with the existing AI tailoring p95 goal in specs/001, since both are a single synchronous LLM call); non-AI parts of the flow (file validation, review screen interactions) respond in <500ms p95.

**Constraints**: No profile data is ever written without explicit user confirmation (FR-005, FR-013); only `application/pdf` content is accepted (FR-002); uploads are capped at 10MB (FR-003, research.md decision #5); the feature must degrade to a clear error — never a silent failure or blank screen — when `OPENAI_API_KEY` is absent/invalid, the PDF is unreadable, or the PDF has no extractable text (FR-007, FR-008, FR-009).

**Scale/Scope**: One file per upload, one in-flight review per user at a time (client-side state only); no new concurrency concerns beyond the existing per-request handling already used for tailoring requests.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` remains the unfilled project template (no ratified principles or gates), same as when feature 001 was planned. There are no constitutional gates to evaluate against. This plan follows the same engineering defaults already established by feature 001 and the current codebase: package-by-feature backend organization, reuse of existing infrastructure over new dependencies, Flyway-owned schema, and layered test coverage (unit/integration/e2e).

**Gate result**: PASS (no gates defined).

**Post-Phase 1 re-check**: Design artifacts (research.md, data-model.md, contracts/rest-api.md, quickstart.md) introduce zero new dependencies and zero new database tables — strictly a subset of what feature 001 already established (PDFBox, Spring AI ChatClient, Flyway/MySQL, the profile package's mutation endpoints). No constitution exists to gate against. **Gate result**: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/002-pdf-resume-upload/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/resumerocket/
│   ├── resumeimport/               # NEW — this feature's package
│   │   ├── ResumeImportController.java     # POST /profile/resume-import (multipart)
│   │   ├── ResumeImportService.java        # orchestrates: validate file → PDFBox text extraction → AiResumeExtractionService
│   │   ├── PdfTextExtractor.java           # thin PDFBox wrapper (PDFTextStripper), detects empty/unreadable PDFs
│   │   ├── AiResumeExtractionService.java  # interface, mirrors tailoring.AiTailoringService
│   │   ├── OpenAiResumeExtractionServiceImpl.java  # ChatClient call + lenient JSON parsing, mirrors OpenAiTailoringServiceImpl
│   │   └── dto/
│   │       ├── ResumeImportResult.java
│   │       ├── ContactInfoCandidate.java
│   │       ├── WorkExperienceCandidate.java
│   │       ├── EducationCandidate.java
│   │       └── SkillCandidate.java
│   └── profile/                    # UNCHANGED — existing mutation endpoints are reused as-is by the frontend on confirm
├── src/main/resources/
│   └── application.yml             # add spring.servlet.multipart.max-file-size / max-request-size (10MB)
└── src/test/java/com/resumerocket/resumeimport/
    ├── PdfTextExtractorTest.java            # unit: fixture PDFs (normal, empty/scanned, corrupted)
    ├── ResumeImportServiceTest.java         # unit: orchestration, AiResumeExtractionService mocked
    └── ResumeImportControllerIT.java        # Testcontainers integration test, AiResumeExtractionService mocked

frontend/
├── src/
│   ├── features/
│   │   └── resumeImport/           # NEW — this feature's frontend folder
│   │       ├── ResumeUploadButton.tsx      # entry point on ProfilePage: file picker + upload trigger
│   │       └── ResumeImportReview.tsx      # review/edit/accept screen, mirrors features/tailoring/SuggestionReview.tsx
│   ├── services/
│   │   └── resumeImportService.ts  # NEW — POST multipart to /profile/resume-import
│   └── pages/
│       └── ProfilePage.tsx         # add <ResumeUploadButton /> alongside existing profile sections
└── tests/
    ├── unit/                        # Vitest: ResumeImportReview field edit/accept/reject behavior
    └── e2e/                         # Playwright: upload fixture PDF → review → confirm → profile reflects new data
```

**Structure Decision**: Extends the existing `backend/` + `frontend/` web application with one new backend feature package (`resumeimport`, following the same package-by-feature convention as `tailoring`/`jobdescription`) and one new frontend feature folder (`features/resumeImport/`). No new services, tiers, or databases. The `profile` package is intentionally left untouched — its existing CRUD endpoints are the write path this feature relies on (see research.md decision #3), which keeps the change additive and low-risk.

## Complexity Tracking

No violations to justify: no constitution gates exist (see Constitution Check above), and the design deliberately avoids adding structure — no new table, no new "apply" endpoint, no new service tier — beyond one extraction endpoint that mirrors an already-proven pattern (`tailoring`'s ChatClient usage) and one frontend review screen that mirrors an already-proven pattern (`SuggestionReview.tsx`).
