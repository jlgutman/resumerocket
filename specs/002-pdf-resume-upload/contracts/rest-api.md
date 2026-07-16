# REST API Contract: PDF Resume Upload & Profile Prepopulation

Base path: `/api/v1`. Requires a valid bearer JWT, scoped to the authenticated user's own profile — same auth model as every other endpoint in this API (specs/001-ai-resume-builder/contracts/rest-api.md § Auth). Standard error shape: `{ "error": string, "details"?: object }`.

This feature adds exactly **one** new endpoint. It relies entirely on the *existing* Master Profile endpoints (specs/001-ai-resume-builder/contracts/rest-api.md § Master Profile) for actually writing data — no new write endpoint is introduced (research.md decision #3).

## Resume Import (User Story 1 & 2)

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| POST | `/profile/resume-import` | `multipart/form-data`, field `file` (PDF, ≤10MB) | `200 ResumeImportResult` | FR-001, FR-004; stateless — nothing is persisted by this call |

**Error responses** (see data-model.md § Error responses for the full table):

| Status | Error code | When |
|---|---|---|
| 400 | `UNSUPPORTED_FILE_TYPE` | file is not `application/pdf` (FR-002) |
| 413 | `FILE_TOO_LARGE` | file exceeds the configured max size (FR-003) |
| 422 | `UNREADABLE_PDF` | file cannot be opened/parsed as a PDF (FR-009) |
| 422 | `NO_EXTRACTABLE_TEXT` | PDF has no usable text layer, e.g. scanned image (FR-008) |
| 502 | `EXTRACTION_SERVICE_FAILED` | the AI structuring step fails or `OPENAI_API_KEY` is unavailable (FR-004) |

## Confirming an import — reuses existing Master Profile endpoints

There is no `/profile/resume-import/confirm` or "apply" endpoint. Once the user has reviewed the `ResumeImportResult` client-side and decided what to keep, edit, or discard (FR-005, FR-006, FR-010, FR-011), the frontend applies the result using the **same endpoints** a manual profile edit would use:

| Method | Path | Used for | Notes |
|---|---|---|---|
| PUT | `/profile` | approved `contactInfo` fields | `ContactInfoRequest` — unchanged; only fields the user approved are included in the request body, so unapproved existing values are left untouched (FR-010) |
| POST | `/profile/education` | each approved `EducationCandidate` | `EducationEntryRequest` — always creates a new entry (FR-012) |
| POST | `/profile/work-experience` | each approved `WorkExperienceCandidate` | `WorkExperienceEntryRequest` — always creates a new entry (FR-012) |
| POST | `/profile/skills` | each approved `SkillCandidate` | `SkillRequest` — always creates a new entry (FR-012) |

If the user cancels the review (FR-013), the frontend simply discards its local `ResumeImportResult` state and calls none of the above — the master profile is guaranteed unchanged because no persistence occurred at any point before confirmation.

## Shared Schemas (informative)

- `ResumeImportResult`: `{ sourceFileName, contactInfo, workExperience[], education[], skills[], warnings[] }` — full field definitions in [data-model.md](../data-model.md).
- `ContactInfoCandidate` / `WorkExperienceCandidate` / `EducationCandidate` / `SkillCandidate`: see [data-model.md](../data-model.md); each mirrors the shape of the corresponding existing `*Request` DTO plus extraction-confidence metadata (`fieldsNotExtracted` / `lowConfidence`).

Full JSON Schema / OpenAPI document to be generated from these definitions during implementation (`/speckit-tasks` scope).
