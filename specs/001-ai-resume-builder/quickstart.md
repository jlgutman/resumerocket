# Quickstart: AI-Powered Resume Builder

Validates the four user stories end-to-end once implemented. References [data-model.md](./data-model.md) and [contracts/rest-api.md](./contracts/rest-api.md) instead of duplicating field/endpoint details.

## Prerequisites

- Java 25 + Maven installed; Node.js (LTS) + npm installed
- MySQL 8.x running locally (or via Docker), with a database created for the app
- Environment variables configured for the backend: DB connection (`SPRING_DATASOURCE_*`), JWT signing secret, and `OPENAI_API_KEY` (+ optionally `OPENAI_MODEL`, default `gpt-4o-mini`) for the Spring AI-backed `AiTailoringService` adapter (see research.md §1). Without a key, tailoring still succeeds via the built-in graceful-degradation path (no suggestions, all requirements reported as unmatched).

## Setup

```bash
# Backend
cd backend
mvn spring-boot:run
# API available at http://localhost:8080/api/v1, Flyway migrations run automatically

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
```

## Scenario 1 — Build a Master Profile (User Story 1, P1)

1. Register a new account (`POST /auth/register`) and sign in via the UI or `POST /auth/login`.
2. In the UI, fill in personal info, add one education entry, one work experience entry (leave end date blank to mark it current), and add two skills. Save.
3. **Expected**: A confirmation is shown; `GET /profile` returns the saved data including the open-ended work experience entry.
4. Edit the work experience description and save again.
5. **Expected**: The change persists; no Tailored Resume exists yet, so FR-004 (non-mutation of prior resumes) is not yet exercised — covered in Scenario 4.

## Scenario 2 — Tailor a Resume to a Job Description (User Story 2, P2)

1. Paste a real job description into the tailoring screen (`POST /job-descriptions`).
2. Request tailoring (`POST /job-descriptions/{id}/tailor`).
3. **Expected**: A draft Tailored Resume is returned with `suggestions[]` covering emphasis, rewritten bullets, and skill highlights; changed content is visually marked in the UI.
4. Accept one suggestion, reject one, and edit one directly (`PATCH .../suggestions/{suggestionId}`).
5. **Expected**: The resume content reflects each resolution immediately.
6. Repeat with a job description that has little overlap with the profile.
7. **Expected**: `unmatchedRequirements[]` is non-empty and shown to the user instead of a silent failure (FR-020).

## Scenario 3 — Export a Finished Resume (User Story 3, P3)

1. From the tailored (or base) resume, select a template (`GET /templates`, then preview via `GET /tailored-resumes/{id}/preview?templateId=`).
2. **Expected**: Preview updates without altering underlying resume content.
3. Export to PDF, then DOCX, then plain text (`POST /tailored-resumes/{id}/export`, then `GET /exports/{exportId}/download`).
4. **Expected**: All three downloads succeed; PDF/DOCX visually match the preview (FR-014); the plaintext export has no tables/graphics and preserves section structure (FR-013).

## Scenario 4 — Manage and Compare Resume Versions (User Story 4, P4)

1. Name and save the tailored resume from Scenario 2 with a company/job title (`PATCH /tailored-resumes/{id}`).
2. Repeat Scenario 2 with a second job description to produce a second named version.
3. List versions (`GET /tailored-resumes`) and confirm both appear with correct company/job title metadata (FR-016).
4. Compare the two (`GET /tailored-resumes/compare?leftId=&rightId=`).
5. **Expected**: A structured diff highlighting differences is returned.
6. Clone one version (`POST /tailored-resumes/{id}/clone`).
7. **Expected**: A new independent draft is created; editing it does not change the original.
8. Update the master profile (edit a work experience entry), then regenerate the earlier tailored resume (`POST /tailored-resumes/{id}/regenerate`).
9. **Expected**: A new version is created from the updated profile; the original tailored resume at `{id}` is still retrievable unchanged (FR-004, FR-019).

## Success Criteria Checkpoints

Map each scenario back to [spec.md](./spec.md) Success Criteria: Scenario 1 → SC-001; Scenario 2 → SC-002, SC-003; Scenario 3 → SC-004; Scenario 4 → SC-005, SC-007. SC-006 (90% first-time completion) is validated via usability testing/analytics, not a single scripted run.
