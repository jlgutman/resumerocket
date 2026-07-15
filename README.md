# resumerocket
ResumeRocket - AI-Powered Resume Builder

## Prerequisites

- Java 25 (JDK)
- Maven 3.9+
- Node.js 18+ and npm
- Docker (for the MySQL database)
- An OpenAI API key (for AI-powered resume tailoring)

## 1. Start the database

The backend expects a MySQL instance. Start it with Docker Compose from the repo root:

```bash
docker compose up -d
```

This starts MySQL 8.4 on `localhost:3306` with database `resumerocket`, user `resumerocket` / password `resumerocket` (root password `resumerocket_root`).

## 2. Configure and start the backend

```bash
cd backend
cp .env.example .env
```

Edit `.env` and set at minimum:
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — match the Docker Compose credentials above (`resumerocket` / `resumerocket`), or your own MySQL setup
- `OPENAI_API_KEY` — required for AI resume tailoring features

Load the env vars and run the app with Maven:

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

The API starts on `http://localhost:8080/api/v1`.

## 3. Configure and start the frontend

In a separate terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The app starts on `http://localhost:5173` and talks to the backend at the URL configured in `frontend/.env` (`VITE_API_BASE_URL`, defaults to `http://localhost:8080/api/v1`).

## Other useful commands

Backend (from `backend/`):
```bash
mvn test                # run tests
mvn spotless:apply      # format code
```

Frontend (from `frontend/`):
```bash
npm run build     # production build
npm run lint       # lint
npm run test       # unit tests
```

## 4. Run the end-to-end tests

E2E tests live in [`e2e-testing/`](e2e-testing/README.md) and use Playwright to drive the real
frontend against the real backend/database (steps 1-3 above must be running):

```bash
cd e2e-testing
npm install
npm run install:browsers   # first time only
npm test
```

## Stopping the database

```bash
docker compose down
```
