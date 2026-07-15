# ResumeRocket E2E Tests

End-to-end tests for ResumeRocket, powered by [Playwright](https://playwright.dev). These tests
drive the real frontend against a real backend and database — there is no mocking layer, so the
full stack needs to be running.

## Setup

```bash
cd e2e-testing
npm install
npm run install:browsers   # downloads the Chromium/Firefox/WebKit binaries, first time only
```

## Before running

Start the rest of the stack from the repo root, following the main [README](../README.md):

1. `docker compose up -d` (MySQL)
2. Backend running on `http://localhost:8080/api/v1` (`cd backend && mvn spring-boot:run`)

The frontend dev server (`http://localhost:5173`) does **not** need to be started manually —
Playwright will start it for you if it isn't already running (see `webServer` in
`playwright.config.ts`). If you already have `npm run dev` running in `frontend/`, Playwright
reuses it.

## Running the tests

```bash
npm test              # headless, all browsers
npm run test:headed   # headed, watch the browser
npm run test:ui       # interactive UI mode, great for debugging
npm run report        # open the HTML report from the last run
```

Run a single file or grep a test name:

```bash
npx playwright test tests/auth.spec.ts
npx playwright test -g "invalid login"
```

## Test data

Tests register a brand-new user with a unique, timestamped email for every run (see
`utils/test-data.ts`), so they don't collide with each other or with data from previous runs and
need no database cleanup step.

## Layout

```
e2e-testing/
├── playwright.config.ts   # base URL, browsers, auto-started dev server
├── tests/
│   ├── auth.spec.ts       # register, login, logout, protected-route redirects
│   ├── profile.spec.ts    # editing contact info, education, experience, and skills
│   └── resumes.spec.ts    # tailoring, editing, cloning, comparing resumes
└── utils/
    ├── test-data.ts       # unique test-user generator
    ├── register.ts        # shared "register a fresh user" helper
    └── tailor.ts          # shared "tailor a resume from a job description" helper
```
