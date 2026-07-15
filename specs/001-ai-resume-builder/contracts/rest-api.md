# REST API Contract: AI-Powered Resume Builder

Base path: `/api/v1`. All endpoints except `auth/register` and `auth/login` require a valid bearer JWT and are scoped to the authenticated user's own data (FR-001). Standard error shape: `{ "error": string, "details"?: object }` with appropriate 4xx/5xx status codes.

## Auth

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| POST | `/auth/register` | `{ email, password, fullName }` | `201 { userId, token }` | FR-001 |
| POST | `/auth/login` | `{ email, password }` | `200 { token }` | FR-001 |

## Master Profile (User Story 1)

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| GET | `/profile` | — | `200 MasterProfile` | Creates-on-first-access or 404 if never initialized |
| PUT | `/profile` | `{ contactInfo }` | `200 MasterProfile` | Updates contact info only; FR-004 (does not touch existing Tailored Resumes) |
| POST | `/profile/education` | `EducationEntry` (no id) | `201 EducationEntry` | FR-003 |
| PUT | `/profile/education/{id}` | `EducationEntry` | `200 EducationEntry` | |
| DELETE | `/profile/education/{id}` | — | `204` | |
| POST | `/profile/work-experience` | `WorkExperienceEntry` (no id) | `201 WorkExperienceEntry` | FR-003; `endDate` optional = current role |
| PUT | `/profile/work-experience/{id}` | `WorkExperienceEntry` | `200 WorkExperienceEntry` | |
| DELETE | `/profile/work-experience/{id}` | — | `204` | |
| POST | `/profile/skills` | `{ name, category? }` | `201 Skill` | |
| DELETE | `/profile/skills/{id}` | — | `204` | |

## Tailoring (User Story 2)

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| POST | `/job-descriptions` | `{ rawText }` | `201 { id, extractedRequirements }` | FR-005, FR-006 |
| POST | `/job-descriptions/{id}/tailor` | — | `202 TailoredResume` (status `draft`, with `suggestions[]`) | FR-007–FR-009; if the job description has near-zero overlap with the profile, response still `202` and `unmatchedRequirements[]` is populated (FR-020) |
| GET | `/tailored-resumes/{id}` | — | `200 TailoredResume` (includes `suggestions[]`) | |
| PATCH | `/tailored-resumes/{id}/suggestions/{suggestionId}` | `{ reviewState: "accepted"\|"rejected"\|"edited", finalText? }` | `200 AiSuggestion` | FR-010 |
| PATCH | `/tailored-resumes/{id}` | `{ name?, company?, jobTitle?, status? }` | `200 TailoredResume` | FR-015; sets `status: "finalized"` |

## Templates & Preview (User Story 3)

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| GET | `/templates` | — | `200 Template[]` | FR-011 |
| GET | `/tailored-resumes/{id}/preview?templateId=` | — | `200 { renderedLayout }` | Structured layout consumed by the frontend preview renderer (research.md §4); no file produced |
| POST | `/tailored-resumes/{id}/export` | `{ templateId, format: "pdf"\|"docx"\|"txt" }` | `201 { exportId, downloadUrl }` | FR-013, FR-014 |
| GET | `/exports/{exportId}/download` | — | `200` binary file stream | |

## Versions & History (User Story 4)

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| GET | `/tailored-resumes` | `?company=&sort=` | `200 TailoredResume[]` (summary fields) | FR-016 |
| GET | `/tailored-resumes/compare?leftId=&rightId=` | — | `200 { left, right, diff }` | FR-017; `diff` is a section-by-section structured diff (research.md §5) |
| POST | `/tailored-resumes/{id}/clone` | — | `201 TailoredResume` (new id, `clonedFromId` set) | FR-018 |
| POST | `/tailored-resumes/{id}/regenerate` | — | `201 TailoredResume` (new id, `regeneratedFromId` set, built from current Master Profile) | FR-019; original resume at `{id}` is unchanged |

## Shared Schemas (informative)

- `MasterProfile`: `{ contactInfo, educationEntries[], workExperienceEntries[], skills[] }`
- `TailoredResume`: `{ id, jobDescriptionId?, name, company?, jobTitle?, status, suggestions[], unmatchedRequirements[]?, createdAt, clonedFromId?, regeneratedFromId? }`
- `AiSuggestion`: `{ id, targetSection, suggestionType, originalText?, suggestedText, finalText?, reviewState }`

Full JSON Schemas / OpenAPI document to be generated from these definitions during implementation (`/speckit-tasks` scope), reflecting the entities in [data-model.md](../data-model.md).
