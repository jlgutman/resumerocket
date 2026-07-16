# Phase 1 Data Model: PDF Resume Upload & Profile Prepopulation

Per research.md decision #3, this feature introduces **no new persisted database entities or Flyway migration**. The "Resume Upload" and "Extracted Profile Draft" concepts from the spec's Key Entities section are realized as transient request/response objects that exist only for the lifetime of one HTTP request (extraction) plus the frontend's in-memory review state (until confirm or discard) — never written to the database.

The tables below document those transfer objects, plus how they map onto the **pre-existing** persisted entities (`MasterProfile`, `WorkExperienceEntry`, `EducationEntry`, `Skill` — see specs/001-ai-resume-builder/data-model.md) that are the actual write target once a user confirms.

## ResumeImportResult (response body of `POST /profile/resume-import`)

Not persisted. Returned directly to the caller; held in frontend component state during review.

| Field | Type | Notes |
|---|---|---|
| sourceFileName | string | original uploaded filename, for display in the review UI |
| contactInfo | ContactInfoCandidate | see below |
| workExperience | WorkExperienceCandidate[] | zero or more; empty array if the resume has no detectable work history |
| education | EducationCandidate[] | zero or more |
| skills | SkillCandidate[] | zero or more |
| warnings | string[] | human-readable notes, e.g. "Unusual formatting detected — some sections may be incomplete" (FR-007 low-confidence signal at the result level) |

## ContactInfoCandidate

Maps 1:1 onto `MasterProfile`'s contact fields (`fullName`, `email`, `phone`, `location`, `links`) — see specs/001-ai-resume-builder/data-model.md § Master Profile.

| Field | Type | Notes |
|---|---|---|
| fullName | string, nullable | null if not confidently extracted |
| email | string, nullable | |
| phone | string, nullable | |
| location | string, nullable | |
| links | string, nullable | comma-separated, matching `MasterProfile.links` |
| fieldsNotExtracted | string[] | names of the above fields the extractor could not confidently populate (FR-007) |

**Merge semantics on confirm** (research.md decision #4, FR-010/FR-011): the frontend submits only the fields the user approved via `PUT /profile` (`ContactInfoRequest`, unchanged shape) — no backend-side conflict logic needed, since the caller already resolved conflicts before submitting.

## WorkExperienceCandidate

Maps 1:1 onto `WorkExperienceEntry`'s creatable fields — see specs/001-ai-resume-builder/data-model.md § Work Experience Entry.

| Field | Type | Notes |
|---|---|---|
| company | string | required in the entity; may be empty string if unextracted, flagged via `lowConfidence` |
| title | string | |
| startDate | date, nullable | entity requires non-null; a null candidate value must be filled by the user before it can be submitted to `POST /profile/work-experience` |
| endDate | date, nullable | null = current role, same convention as the entity |
| description | string | |
| lowConfidence | boolean | true if any required field on this candidate could not be confidently extracted (FR-007) |

**Merge semantics on confirm** (FR-012): each approved candidate becomes one `POST /profile/work-experience` call — always a new entry, never matched against or merged into an existing one.

## EducationCandidate

Maps 1:1 onto `EducationEntry`'s creatable fields — see specs/001-ai-resume-builder/data-model.md § Education Entry.

| Field | Type | Notes |
|---|---|---|
| institution | string | |
| credential | string, nullable | |
| fieldOfStudy | string, nullable | |
| startDate | date, nullable | |
| endDate | date, nullable | null = in progress, same convention as the entity |
| description | string, nullable | |
| lowConfidence | boolean | |

**Merge semantics on confirm**: each approved candidate becomes one `POST /profile/education` call, always additive (FR-012).

## SkillCandidate

Maps 1:1 onto `Skill`'s creatable fields — see specs/001-ai-resume-builder/data-model.md § Skill.

| Field | Type | Notes |
|---|---|---|
| name | string | |
| category | string, nullable | |
| lowConfidence | boolean | |

**Merge semantics on confirm**: each approved candidate becomes one `POST /profile/skills` call, always additive (FR-012). Duplicate detection against existing skills (e.g. same name already present) is a frontend display concern (dim/pre-uncheck an exact-name match), not a backend rule — FR-012 only requires that entries are never silently merged/overwritten, not that duplicates be prevented.

## Error responses

Not a data entity, but part of this feature's contract — uses the existing `ErrorResponse` shape from `common/GlobalExceptionHandler`. New error codes introduced by this feature:

| Code | HTTP Status | Trigger | Corresponds to |
|---|---|---|---|
| `UNSUPPORTED_FILE_TYPE` | 400 | uploaded file is not `application/pdf` | FR-002, edge case |
| `FILE_TOO_LARGE` | 413 | upload exceeds the configured max size | FR-003, edge case |
| `UNREADABLE_PDF` | 422 | `PDDocument.load()` fails (corrupted/password-protected) | FR-009, edge case |
| `NO_EXTRACTABLE_TEXT` | 422 | PDF loads but yields no usable text (scanned/image-only) | FR-008, edge case |
| `EXTRACTION_SERVICE_FAILED` | 502 | the AI structuring call fails (timeout, provider error, missing `OPENAI_API_KEY`) | FR-004 (extraction can't complete); user can still fall back to manual entry |
