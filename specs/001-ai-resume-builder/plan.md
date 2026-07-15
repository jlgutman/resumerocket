# Implementation Plan: AI-Powered Resume Builder

**Branch**: `001-ai-resume-builder` | **Date**: 2026-07-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-ai-resume-builder/spec.md`

## Summary

ResumeRocket lets a job seeker maintain one master professional profile and generate multiple resumes tailored to individual job descriptions using AI, then preview, export (PDF/DOCX/ATS-plaintext), and manage those tailored versions over time. Technical approach: a React + Tailwind CSS single-page application talking to a Java 25 / Spring Boot REST API backed by MySQL, with an AI tailoring service that analyzes job descriptions against the stored profile and returns reviewable suggestions (accept/reject/edit) before a resume version is saved and rendered to file.

## Technical Context

**Language/Version**: Java 25 (backend, Spring Boot 3.x); TypeScript + React 18 (frontend)

**Primary Dependencies**: Spring Boot Web, Spring Security, Spring Data JPA, Flyway (backend, built with Maven); React 18, Tailwind CSS, Vite, React Router, TanStack Query (frontend); Apache PDFBox (PDF export) and Apache POI (DOCX export); Spring AI (`spring-ai-starter-model-openai`) calling OpenAI for job-description analysis and resume tailoring, behind a provider-agnostic `AiTailoringService` interface (see research.md §1)

**Storage**: MySQL 8.x, schema-versioned via Flyway migrations

**Testing**: JUnit 5 + Mockito + Spring Boot Test + Testcontainers (MySQL) for backend; Vitest + React Testing Library for frontend unit/component tests; Playwright for end-to-end user-journey tests

**Target Platform**: Server: Linux container (Spring Boot executable jar); Client: modern evergreen browsers (Chrome, Firefox, Safari, Edge)

**Project Type**: Web application (separate frontend + backend)

**Performance Goals**: Non-AI API endpoints respond in <500ms p95; AI tailoring requests return in <15s p95; PDF/DOCX export generation completes in <5s p95; template/preview changes reflect in the UI in <200ms

**Constraints**: Exported files (PDF/DOCX/plaintext) must match the on-screen preview (FR-014); ATS plaintext export must contain no tables, columns, images, or non-standard characters that impede automated parsing (FR-013); all profile and resume data is strictly isolated per user account (FR-001); prior tailored resume versions must remain unchanged when the master profile is edited (FR-004, FR-019)

**Scale/Scope**: Initial target ~10,000 registered users, each with one master profile and up to ~50 saved tailored resume versions; moderate, non-real-time concurrent load on the AI tailoring path

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still the unfilled project template — no concrete principles, technology constraints, or governance rules have been ratified for this repository yet. There are therefore no constitutional gates to evaluate against for this feature. This plan proceeds using standard engineering defaults (layered backend architecture, componentized frontend, migration-managed schema, automated testing at unit/integration/e2e levels) documented above and in research.md. If a constitution is ratified later, this plan should be re-checked against it.

**Gate result**: PASS (no gates defined).

**Post-Phase 1 re-check**: Design artifacts (research.md, data-model.md, contracts/rest-api.md, quickstart.md) introduce no new dependencies beyond those already listed in Technical Context, and no constitution exists to gate against. **Gate result**: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-resume-builder/
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
│   ├── profile/            # Master profile, education, work experience, skills
│   ├── jobdescription/     # Job description intake and analysis
│   ├── tailoring/          # AI tailoring orchestration + suggestion review
│   ├── resume/             # Tailored resume versions, cloning, comparison
│   ├── export/             # PDF/DOCX/plaintext rendering
│   ├── template/           # Resume template definitions
│   ├── auth/                # Account creation, sign-in, session/JWT handling
│   └── common/              # Shared config, error handling, security config
├── src/main/resources/
│   └── db/migration/        # Flyway SQL migrations
└── src/test/java/com/resumerocket/
    ├── contract/             # API contract tests
    ├── integration/          # Testcontainers-backed integration tests
    └── unit/                 # Service/domain unit tests

frontend/
├── src/
│   ├── components/           # Shared UI components (Tailwind-styled)
│   ├── pages/                # Profile, Tailoring, Preview/Export, Version history
│   ├── features/             # Feature-scoped logic (profile, tailoring, resumes)
│   ├── services/             # API client layer (typed REST calls)
│   └── templates/            # Client-side resume template renderers (preview)
└── tests/
    ├── unit/                 # Vitest + React Testing Library
    └── e2e/                  # Playwright end-to-end flows
```

**Structure Decision**: Web application split into `backend/` (Java 25, Spring Boot, Maven, MySQL via Spring Data JPA/Flyway) and `frontend/` (React 18 + Tailwind CSS, built with Vite), communicating over a versioned JSON REST API. This follows the standard "Option 2: Web application" layout since the feature spans a browser-based UI and a server-side API with persistent storage and AI integration.

## Complexity Tracking

No violations to justify: the repository constitution has no ratified gates yet (see Constitution Check above), and the chosen structure (one backend service, one frontend app, domain-oriented packages) is the minimal structure that satisfies the feature's four user stories without speculative extra services or layers.
