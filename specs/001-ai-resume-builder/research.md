# Phase 0 Research: AI-Powered Resume Builder

## 1. AI/LLM integration for job description analysis and resume tailoring

**Decision**: Implement tailoring behind a provider-agnostic `AiTailoringService` interface in the backend, with a single concrete adapter — `OpenAiTailoringServiceImpl` — built on **Spring AI's `ChatClient`** talking to **OpenAI** (`spring-ai-starter-model-openai`, Spring AI 2.0.0 BOM), prompted for structured JSON output, to (a) extract key requirements/keywords from a job description and (b) generate emphasis/rewrite/skill suggestions from the master profile. Model and credentials are configuration-driven (`spring.ai.openai.api-key` / `OPENAI_API_KEY`, `spring.ai.openai.chat.options.model` / `OPENAI_MODEL`, default `gpt-4o-mini`). Requests are synchronous from the client's perspective but executed server-side with the adapter converting any failure (auth, network, malformed response) into `AiTailoringException`, which callers (`JobDescriptionService`, `TailoringService`) catch to degrade gracefully rather than fail the request.

**Rationale**: The spec requires natural-language analysis and rewriting (FR-006 through FR-010) that is impractical to hand-roll reliably. Spring AI's `ChatClient` abstraction gives a consistent, well-supported prompting API on top of the official OpenAI Java SDK, and keeping it behind the `AiTailoringService` interface means the vendor/starter can still be swapped (e.g. to Spring AI's Anthropic or Bedrock starters) without touching domain code in `tailoring`/`jobdescription`/`resume`. OpenAI + Spring AI was an explicit user choice (see project history), superseding the originally vendor-neutral placeholder decision.

**Alternatives considered**:
- Hand-rolled HTTP client against a specific vendor's REST API (the original v1 approach, calling Anthropic's Messages API directly via `RestClient`) — rejected in favor of Spring AI: less boilerplate (retry/observability/tool-calling come for free), and the `ChatClient` API is provider-agnostic at the Spring AI layer too, so a future vendor switch is a starter/config change rather than a rewrite.
- Self-hosted open-weight model — rejected for v1: higher infra/ops cost and slower time-to-value than a hosted API, no requirement for on-prem/offline AI.
- Rules-based keyword matching only (no LLM) — rejected: cannot satisfy FR-008 (bullet point rewriting) or produce natural language suggestions; would degrade the core differentiator.
- Synchronous-only with no timeout handling — rejected: job descriptions and profiles can be long (Edge Cases), so the service must handle slow/failed AI calls gracefully and surface partial results (FR-020).

## 2. PDF export

**Decision**: Apache PDFBox for server-side PDF generation, driven from the same structured resume/template model used for on-screen preview.

**Rationale**: Mature, actively maintained, Apache-2.0 licensed (no commercial licensing concerns), fine-grained layout control needed to keep exported PDF visually consistent with the previewed template (FR-014).

**Alternatives considered**:
- iText — rejected: AGPL/commercial dual license is a poor fit for an unspecified commercial model.
- Headless-browser HTML-to-PDF (e.g., rendering the React preview and printing to PDF) — considered viable and revisit-worthy if template fidelity via PDFBox proves too labor-intensive, but rejected for v1 to avoid a browser-automation runtime dependency in the backend.

## 3. DOCX export

**Decision**: Apache POI (XWPF) for DOCX generation from the same structured resume/template model.

**Rationale**: De facto standard Java library for OOXML documents, Apache-2.0 licensed, integrates naturally alongside PDFBox in a Maven-built Spring Boot service.

**Alternatives considered**: Hand-rolled OOXML XML generation — rejected as unnecessary complexity given POI's maturity.

## 4. Ensuring preview/export consistency (FR-014)

**Decision**: Define each template as a single structured layout/style descriptor consumed by both (a) a React preview renderer and (b) the backend PDFBox/POI exporters, rather than maintaining separate preview and export implementations. Resume *content* always comes from the same tailored-resume record; only the renderer differs per output target.

**Rationale**: Directly targets FR-014 and SC-004 (preview/export parity) by construction rather than by manual synchronization between two independently maintained renderers.

**Alternatives considered**: Fully independent frontend preview and backend export templates kept manually in sync — rejected: high risk of visual drift, hard to test for parity.

## 5. Version comparison / diffing (FR-017)

**Decision**: Use a text/structured diff library (`java-diff-utils`) on the backend to compute section-by-section differences between two tailored resume versions; the frontend renders the diff result side-by-side.

**Rationale**: Off-the-shelf, well-tested diffing avoids reinventing line/word diff algorithms; computing the diff server-side keeps the comparison logic co-located with the canonical resume data model.

**Alternatives considered**: Client-side diffing in the browser — rejected: would require duplicating resume structural knowledge in the frontend and shipping both full versions to the client regardless, so server-side computation is not meaningfully more expensive and is simpler to test.

## 6. Authentication

**Decision**: Spring Security with stateless JWT-based authentication (email/password credentials, hashed with BCrypt) issued by the backend and stored client-side; every API request scoped to the authenticated user's own data.

**Rationale**: Standard, well-supported pattern for a Spring Boot REST API paired with a React SPA; satisfies FR-001 (account isolation) without introducing an external identity provider the requirements never asked for.

**Alternatives considered**: Server-side session cookies — rejected: adds session-store infrastructure with no stated requirement for it, and JWT fits a decoupled SPA + REST API split more naturally. OAuth2/SSO — rejected per spec Assumptions (no enterprise SSO requirement).

## 7. Database schema management

**Decision**: Flyway versioned SQL migrations against MySQL 8.x, run automatically on backend startup in dev/test and via an explicit migration step in deployment.

**Rationale**: Keeps schema evolution auditable and reproducible across environments; integrates directly with Spring Boot.

**Alternatives considered**: Liquibase — comparable option; Flyway chosen for simpler plain-SQL migrations given no requirement for Liquibase's XML/YAML abstraction.

## 8. Testing strategy

**Decision**: Backend — JUnit 5 + Mockito for unit tests, Spring Boot Test + Testcontainers (MySQL) for integration tests, and contract tests against the REST API defined in `contracts/`. Frontend — Vitest + React Testing Library for component/unit tests, Playwright for end-to-end coverage of the four user-story flows.

**Rationale**: Matches each user story's "Independent Test" description in the spec with an automatable equivalent (contract test for API shape, integration test for persistence/AI-adapter wiring, e2e test for the full browser flow), and uses the standard toolchain for each stack (Spring/Maven, Vite/React).

**Alternatives considered**: Cypress instead of Playwright — comparable; Playwright chosen for built-in multi-browser support and first-class TypeScript integration matching the frontend stack.

## 9. Build tooling

**Decision**: Backend built with Maven (single module, as requested). Frontend built with Vite + npm.

**Rationale**: Matches explicit user direction (Java 25 + Spring Boot + Maven) and is the standard fast dev-server/build tool for a Tailwind-based React SPA.

**Alternatives considered**: Gradle for backend — rejected, user explicitly specified Maven. Create React App for frontend — rejected, unmaintained relative to Vite.

## Outstanding NEEDS CLARIFICATION

None. All Technical Context unknowns are resolved by the decisions above; the specific LLM vendor is intentionally deferred behind the `AiTailoringService` interface (decision #1) rather than hard-coded, since the feature spec does not mandate one and the interface boundary makes the choice a configuration/deployment concern rather than a design blocker.
