# Phase 1 Data Model: AI-Powered Resume Builder

Derived from the Key Entities section of [spec.md](./spec.md). Field lists are the attributes needed to satisfy the functional requirements; implementation may add standard bookkeeping columns (`id`, `created_at`, `updated_at`) on every table.

## User Account

Represents a registered job seeker. Owns exactly one Master Profile and any number of Tailored Resumes (FR-001, Assumptions).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| email | string | unique, required, used for sign-in |
| password_hash | string | BCrypt hash; never exposed via API |
| full_name | string | required |
| created_at | timestamp | |

**Relationships**: 1 User Account → 1 Master Profile; 1 User Account → many Tailored Resumes.

## Master Profile

The user's canonical professional record (FR-002). One per User Account.

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| user_account_id | FK → User Account | unique (1:1) |
| contact_info | structured (name, email, phone, location, links) | required subset: name |
| updated_at | timestamp | changes here must not mutate existing Tailored Resumes (FR-004) |

**Relationships**: 1 Master Profile → many Education Entries, many Work Experience Entries, many Skills.

## Education Entry

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| master_profile_id | FK → Master Profile | required |
| institution | string | required |
| credential | string | e.g., degree/certificate name |
| field_of_study | string | optional |
| start_date | date | optional |
| end_date | date | nullable = in progress |
| description | text | optional rich text |
| display_order | int | user-controlled ordering |

**Validation**: unlimited entries per profile (FR-003); `end_date` may be null to represent ongoing study.

## Work Experience Entry

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| master_profile_id | FK → Master Profile | required |
| company | string | required |
| title | string | required |
| start_date | date | required |
| end_date | date | nullable = current role (Acceptance Scenario 4, Story 1) |
| description | text | rich/multi-line, required |
| display_order | int | user-controlled ordering |

**Validation**: unlimited entries per profile (FR-003).

## Skill

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| master_profile_id | FK → Master Profile | required |
| name | string | required, e.g., "Kubernetes" |
| category | string | optional grouping (e.g., "Technical", "Language") |

## Job Description

The pasted text submitted for a tailoring request (FR-005).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| user_account_id | FK → User Account | |
| raw_text | text | required, the pasted job posting |
| extracted_requirements | JSON | keywords/requirements identified by AI analysis (FR-006) |
| created_at | timestamp | |

**Relationships**: 1 Job Description → many Tailored Resumes (a job description may be re-tailored/regenerated).

## Tailored Resume (Version)

A named, saved snapshot of resume content (FR-015). Frozen at save time relative to the Master Profile (FR-004).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| user_account_id | FK → User Account | |
| job_description_id | FK → Job Description | nullable (a resume may exist without tailoring, i.e. exported base profile) |
| source_profile_snapshot | JSON | frozen copy of profile content used to build this version |
| name | string | user-assigned, required (e.g., "Acme Corp - Backend Engineer") |
| company | string | optional, for organization/history (FR-016) |
| job_title | string | optional |
| status | enum | `draft`, `finalized` |
| cloned_from_id | FK → Tailored Resume | nullable, set when created via clone (FR-018) |
| regenerated_from_id | FK → Tailored Resume | nullable, set when created via profile-update regeneration (FR-019); original is preserved, not overwritten unless the user explicitly requests it |
| created_at | timestamp | used for application history ordering (FR-016) |

**Relationships**: 1 Tailored Resume → many AI Suggestions; 1 Tailored Resume → many Exports; optional self-referential links for clone/regenerate lineage.

**State transitions**: `draft` (created from tailoring, suggestions pending) → `finalized` (user has resolved all suggestions and optionally exported). Cloning or regenerating always creates a new `draft` row; it never mutates the source row (FR-004, FR-019, Acceptance Scenario 3 in Story 4).

## AI Suggestion

A proposed change tied to a Tailored Resume (FR-008–FR-010).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| tailored_resume_id | FK → Tailored Resume | required |
| target_section | enum | `work_experience`, `skill`, `summary`, etc. |
| suggestion_type | enum | `emphasis`, `bullet_rewrite`, `skill_highlight` |
| original_text | text | nullable (e.g., a newly suggested skill has no "original") |
| suggested_text | text | required |
| final_text | text | nullable until resolved; set to user's edit if they modify it |
| review_state | enum | `pending`, `accepted`, `rejected`, `edited` |

**Validation**: every suggestion must be reviewable individually (Acceptance Scenario 3, Story 2); a Tailored Resume is not `finalized` while suggestions remain `pending` for export purposes, though export of a draft is still allowed per Story 3 test independence.

## Template

A named visual layout, independent of resume content (FR-011).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| name | string | e.g., "Modern", "Classic", "Technical" |
| layout_descriptor | JSON | structured layout/style definition consumed by both the preview renderer and the exporters (see research.md §4) |

## Export

A generated file for a specific Tailored Resume + Template (FR-013).

| Field | Type | Notes |
|---|---|---|
| id | UUID/long PK | |
| tailored_resume_id | FK → Tailored Resume | required |
| template_id | FK → Template | required |
| format | enum | `pdf`, `docx`, `txt` |
| generated_at | timestamp | |
| file_reference | string | storage location/key of the generated file |

## Entity Relationship Summary

```text
User Account (1) ── (1) Master Profile ── (many) Education Entry
                                        └─ (many) Work Experience Entry
                                        └─ (many) Skill

User Account (1) ── (many) Job Description
User Account (1) ── (many) Tailored Resume ── (many) AI Suggestion
                                            └─ (many) Export ── (1) Template
Job Description (1) ── (many) Tailored Resume
Tailored Resume (self-referencing) ── cloned_from_id / regenerated_from_id
```
