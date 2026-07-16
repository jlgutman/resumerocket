# Phase 0 Research: PDF Resume Upload & Profile Prepopulation

No `[NEEDS CLARIFICATION]` markers remained in the Technical Context (all resolved directly from the existing codebase's established patterns), so this phase documents the technical decisions made rather than resolving open unknowns.

## 1. PDF text extraction library

**Decision**: Reuse Apache PDFBox 3.0.3 (`PDFTextStripper`) for extracting raw text from the uploaded PDF.

**Rationale**: PDFBox is already a backend dependency, currently used one-directionally for PDF *generation* in `export/PdfExportService.java`. Its built-in `PDFTextStripper` handles standard text-based PDF extraction well and requires no new dependency, new license review, or new failure surface.

**Alternatives considered**: Apache Tika — more general-purpose but adds a new dependency for capability PDFBox's stripper already covers for this use case (single-format, text-layer extraction). OCR-based extraction for scanned/image-only PDFs — explicitly out of scope per the spec's edge cases (a scanned PDF is a documented "can't auto-fill, ask user to enter manually" case, not something the feature needs to solve).

## 2. Turning extracted text into structured profile fields

**Decision**: Reuse the existing Spring AI `ChatClient` (OpenAI) plumbing from `tailoring/OpenAiTailoringServiceImpl`: a system prompt specifying the exact JSON shape to return, `chatClient.prompt().system(...).user(extractedText).call().content()`, and the same lenient JSON parsing (strip markdown code fences, parse via Jackson `readTree`) already implemented there.

**Rationale**: Resume layouts vary too widely for reliable regex/heuristic field extraction to be robust, and this repository already has working config (`OPENAI_API_KEY`, model, temperature in `application.yml`), error handling (`AiTailoringException`-style wrapping), and a lenient-parsing utility for exactly this "ask an LLM for structured JSON" pattern. A new `AiResumeExtractionService` interface with an `OpenAiResumeExtractionServiceImpl` mirrors that existing shape.

**Alternatives considered**: Regex/heuristic text parsing — fragile across the wide variety of real-world resume formats and would require constant tuning; a third-party resume-parsing API — adds a new external dependency, cost, and config surface for something the existing AI infrastructure already provides.

## 3. Persistence model for the extraction result

**Decision**: No new database table or entity. `POST /profile/resume-import` is synchronous and stateless: it returns the candidate structured data directly in the HTTP response. The frontend holds it in local component state for the review step. Nothing is written to the database until the user confirms, at which point the frontend calls the *existing* profile mutation endpoints (`PUT /profile`, `POST /profile/education`, `POST /profile/work-experience`, `POST /profile/skills`) for whichever fields/entries were approved.

**Rationale**: The spec's own Assumptions section states the staged draft "does not need to persist indefinitely" and "can be safely discarded." A persisted staging table (mirroring `AiSuggestion`/`ReviewState`) would require a new Flyway migration, a cleanup/expiry mechanism, and a second "apply" endpoint that would just re-implement logic the existing mutation endpoints already provide — since FR-010/011/012's additive-merge behavior is fully satisfied once the frontend only submits fields/entries the user explicitly approved through those endpoints. This is the simplest design that satisfies every functional requirement in the spec.

**Alternatives considered**: A `resume_import_draft` staging table with a status enum, analogous to `AiSuggestion`/`ReviewState` — rejected because it only pays for itself if drafts needed to survive across sessions/devices or be resumed later, which the spec does not require, and it duplicates the merge logic the existing endpoints already implement correctly.

## 4. Conflict handling (existing vs. extracted field values)

**Decision**: Implement FR-010's "preserve existing value by default, offer the conflicting value as a choice" entirely in the frontend review UI, not the backend. The extraction endpoint has no knowledge of current profile state; the frontend (which already loads the current profile via the existing `GET /profile` query on `ProfilePage`) computes, per field, whether it's currently empty or filled, and defaults the review selection accordingly — empty fields default to the extracted value, filled fields default to "keep existing" with the extracted value shown as an alternative the user can pick.

**Rationale**: Keeps the extraction endpoint a pure, stateless "PDF → candidates" transform, independent of any specific profile. The comparison data (current profile) is already loaded client-side, so no new backend endpoint, request parameter, or diffing logic is needed.

**Alternatives considered**: Have the backend accept the file plus a profile identifier and return a pre-diffed result marking each field `new`/`conflict`/`unchanged` — rejected as duplicating data the frontend already holds, and it would needlessly couple a stateless extraction call to a specific profile snapshot at extraction time.

## 5. Upload size limit

**Decision**: Cap uploads at 10MB via `spring.servlet.multipart.max-file-size` / `max-request-size` in `application.yml`, with a matching client-side pre-check for immediate feedback before the request is sent.

**Rationale**: Resumes are text-dense documents that rarely exceed a few MB even with embedded formatting or an inline photo; 10MB is a generous, standard web-app ceiling that bounds worst-case request size without constraining legitimate uploads.

**Alternatives considered**: Spring Boot's implicit default (1MB) — too tight for image-containing resume PDFs; no limit — an unbounded-upload resource-exhaustion risk.

## 6. Detecting unreadable / no-text PDFs

**Decision**: `PdfTextExtractor` checks the PDFBox-extracted text against a minimal non-whitespace character-count floor *before* any LLM call is made; a `PDDocument.load()` failure (corrupted or password-protected file) is caught directly. Both map to distinct error responses (`NO_EXTRACTABLE_TEXT`, `UNREADABLE_PDF`) so the frontend can show the specific edge-case messaging the spec calls for, instead of a generic failure.

**Rationale**: Directly satisfies FR-008/FR-009 and their corresponding edge cases, and avoids spending a paid, latency-costly LLM call on input that can be deterministically rejected first.

**Alternatives considered**: Let the LLM attempt extraction on empty/near-empty text and interpret whatever it returns — unreliable, and wastes an API call on a request that should be rejected before it ever reaches the model.
