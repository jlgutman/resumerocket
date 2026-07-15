# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

ResumeRocket is an AI-powered resume builder: users maintain one master professional profile, then generate multiple job-tailored resume versions using an LLM, review the AI's suggested edits, and export to PDF/DOCX/TXT. It is a two-tier app — a Spring Boot REST API (`backend/`) and a React SPA (`frontend/`) — with Playwright end-to-end tests (`e2e-testing/`) that drive the real stack.

## Running the stack

All three tiers must run together for anything beyond unit tests. Start them in order:

```bash
# 1. Database (from repo root) — MySQL 8.4 on localhost:3306, db/user/pass all "resumerocket"
docker compose up -d

# 2. Backend (from backend/) — API on http://localhost:8080/api/v1
cp .env.example .env            # then set SPRING_DATASOURCE_* and OPENAI_API_KEY
export $(grep -v '^#' .env | xargs)   # config is env-var driven; app won't read .env on its own
mvn spring-boot:run

# 3. Frontend (from frontend/) — SPA on http://localhost:5173
cp .env.example .env
npm install
npm run dev
```

`OPENAI_API_KEY` is required for the AI tailoring flow; the rest of the app works without it.

## Common commands

Backend (`backend/`, Maven):
```bash
mvn test                              # all tests (Testcontainers spins up MySQL — needs Docker)
mvn test -Dtest=ClassName#methodName  # single test
mvn spotless:apply                    # format (google-java-format); spotless:check to verify
mvn spring-boot:run                   # run API
```
Spotless is **not** bound to the build lifecycle (google-java-format is incompatible with Java 25's javac internals), so run it manually.

Frontend (`frontend/`, npm):
```bash
npm run dev      # vite dev server
npm run build    # tsc -b && vite build
npm run lint     # eslint (ts,tsx)
npm run test     # vitest run
npm run format   # prettier
```

E2E (`e2e-testing/`, Playwright — requires the full stack running per above):
```bash
npm install
npm run install:browsers   # first time only
npm test                   # headless; test:headed / test:ui for debugging; report to view results
```

## Backend architecture

Spring Boot 4.1 on Java 25. Code is organized **package-by-feature** under `com.resumerocket`, each package holding its own controller/service/entity/DTO/repository:

- `auth` — registration/login, stateless JWT. `SecurityConfig` permits `/auth/**` and authenticates everything else; `JwtAuthFilter` validates the bearer token per request. There is intentionally **no** `UserDetailsService`/`AuthenticationManager` — `AuthService` checks credentials directly against the repository.
- `profile` — the master profile: `MasterProfile` plus child `EducationEntry`, `WorkExperienceEntry`, `Skill`. This is the single source of truth users edit; tailored resumes are generated *from* it.
- `jobdescription` — stored job postings the user tailors against.
- `tailoring` — the AI layer. `OpenAiTailoringServiceImpl` (Spring AI `ChatClient`) turns a profile + job description into `AiSuggestion`s; `SuggestionReviewService` tracks each suggestion's `ReviewState` (accept/reject/modify).
- `resume` — `TailoredResume` versions plus clone/regenerate/diff/compare services (`java-diff-utils` powers side-by-side version comparison).
- `template` / `export` — resume templates and rendering to PDF (`pdfbox`), DOCX (`poi-ooxml`), and plain text. Exports are written to disk (`EXPORT_STORAGE_DIR`, default `./data/exports`).
- `common` — cross-cutting: `BaseEntity`, `GlobalExceptionHandler` → `ErrorResponse`, `ApiException`, config beans.

Key conventions:
- **The database schema is owned by Flyway**, not JPA. Migrations live in `src/main/resources/db/migration` (`V1..V4`); `spring.jpa.hibernate.ddl-auto` is `validate`, so entities must match the migrated schema exactly. Schema changes require a new `V*__*.sql` migration — never rely on Hibernate to alter tables.
- Config is entirely env-var driven via `application.yml` with profiles `dev`/`test`/`prod` (see `AppProperties` for `app.*` settings: JWT, CORS, export dir). The API context path is `/api/v1`.
- Spring Boot 4 defaults to Jackson 3; Jackson 2 `jackson-databind` is pulled in explicitly (see the comment in `pom.xml`) — be deliberate about which Jackson you import.
- Integration tests use Testcontainers MySQL, so `mvn test` needs Docker running.

## Frontend architecture

React 18 + TypeScript + Vite, styled with Tailwind. Routing (`react-router-dom`) and server state (`@tanstack/react-query`) are the backbone; there is no other global store.

- `services/apiClient.ts` — the single axios instance. A request interceptor attaches the JWT from `localStorage` (`resumerocket.token`); a response interceptor clears the token and redirects to `/login` on any 401. All other `services/*.ts` call through this client.
- `features/auth/` — `AuthContext` holds auth state, `ProtectedRoute` gates the authenticated routes defined in `App.tsx`. Everything except `/login` and `/register` sits behind `ProtectedRoute` + `Layout`.
- Structure split: `pages/` are route-level screens, `features/<domain>/` hold domain components (profile editors, suggestion review, version compare, export panel), `services/` are the API wrappers, `components/` are shared primitives.
- `VITE_API_BASE_URL` (`.env`) points the client at the backend; defaults to `http://localhost:8080/api/v1`.

## Spec-driven workflow

This repo uses **spec-kit** (GitHub's spec-driven development toolkit). Feature specs, plans, and task lists live in `specs/<feature>/` (see `specs/001-ai-resume-builder/`); templates and the project constitution are in `.specify/`. The workflow is driven by the `speckit-*` skills (`speckit-specify` → `speckit-plan` → `speckit-tasks` → `speckit-implement`, plus `clarify`/`analyze`/`checklist`/`converge`). When implementing a feature, check `specs/<feature>/` for the authoritative spec, plan, and `tasks.md`.
