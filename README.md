# agentboard-backend

AgentBoard backend — Gradle multi-project containing four Spring Boot 3.2 microservices.

![CI](https://github.com/agentboard/agentboard-backend/actions/workflows/ci.yml/badge.svg)

## Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 21 |
| Docker + Docker Compose | 24+ / Compose v2 |
| Git | any |

> The Gradle wrapper (`gradlew`) is bundled — no standalone Gradle install required.

## Subprojects

| Module | Port | Description |
|---|---|---|
| `commons` | — | Shared security, multitenancy, and exception types |
| `auth-service` | 8080 | Authentication and JWT issuance |
| `board-service` | 8081 | Kanban board state machine, work items, and MCP HTTP API |
| `api-docs-service` | 8082 | Unified Swagger UI aggregating auth + board OpenAPI specs |

## API documentation

Unified Swagger UI (always available when the service is running):

**http://localhost:8082/swagger-ui/index.html** (production: `https://agentboard.matheusmafioletti.com/api/swagger`)

Start order: PostgreSQL → `auth-service` → `board-service` → `api-docs-service`.

Each upstream service exposes its OpenAPI JSON at `/v3/api-docs` (no local Swagger UI).
Pact contracts and MCP JSON schemas remain the CI integration source of truth; OpenAPI is for human discovery only.

## Setup

```bash
# Clone
git clone https://github.com/agentboard/agentboard-backend
cd agentboard-backend

# Compile all subprojects
./gradlew compileJava

# Run Checkstyle (lint)
./gradlew checkstyleMain

# Run all tests (empty suites pass vacuously)
./gradlew test

# Full CI build
./gradlew build
```

## On Windows

```powershell
.\gradlew.bat build
```

## Docker

Build context is the repository root. Each service has its own Dockerfile:

```bash
# From repository root
docker build -f auth-service/Dockerfile -t agentboard-auth:local .
docker build -f board-service/Dockerfile -t agentboard-board:local .
docker build -f api-docs-service/Dockerfile -t agentboard-api-docs:local .
```

Images published to GHCR on every push to `main`:

- `ghcr.io/agentboard/agentboard-auth`
- `ghcr.io/agentboard/agentboard-board`
- `ghcr.io/agentboard/agentboard-api-docs`

## Deploy (demo VPS)

On push to `develop`, the **CD** workflow builds changed service images, pushes to GHCR, and dispatches a deploy to [agentboard-infra](https://github.com/matheusmafioletti/agentboard-infra). After a successful deploy, infra dispatches `post-deploy-verify` to run staging smoke tests.

Orchestration lives in [agentboard-infra](https://github.com/matheusmafioletti/agentboard-infra) (`docker-compose.prod.yml`).

## CI/CD pipeline

Three workflows run on pull requests and after deploy:

| Workflow | Trigger | Purpose |
|---|---|---|
| **CI** | `pull_request` → `develop`/`main` | Lint, test, build, publish preview images, scoped API tests |
| **Pre-merge** | `workflow_call` from **CI** after it succeeds | Full API + Playwright + Cypress + Selenium `@local` suite |
| **CD** | `push` → `develop`/`main` | Build, publish GHCR images (`develop`), deploy (`develop`), production simulation (`main`) |
| **Post-deploy** | `repository_dispatch` `post-deploy-verify` or manual | Staging smoke across API + all E2E frameworks |

### Branch protection — required checks

Configure these status checks on `develop` (and `main` if applicable):

**CI workflow (runs on every PR push):**

- `build`

**Pre-merge workflow (invoked by CI only after build and preview images succeed):**

- `gate`
- `api-local-full`
- `e2e-playwright`
- `e2e-cypress`
- `e2e-selenium`

Checks appear as `Pre-merge / <job>`. If CI fails, the `pre-merge` caller job is skipped and the E2E suite does not run.

`api-local-scoped` is informational only (skipped when no deployable service changed) — not a required check.

### Scoped API tests (`api-local-scoped`)

On PRs, after preview images are published, RestAssured tests run against the E2E Docker stack using PR SHA tags. Cucumber tag scope follows `paths-filter` outputs from the `build` job:

| Change detected | Cucumber filter |
|---|---|
| `auth-service` (or commons/gradle affecting auth) | `@auth` |
| `board-service` (or commons/gradle affecting board) | `@board or @projects` |
| `api-docs-service` only | `@smoke` |
| Both auth and board (commons/gradle) | `@auth or @board or @projects or @invites` |

### GitHub Secrets

| Secret | Description |
|--------|-------------|
| `INFRA_DEPLOY_PAT` | PAT with `repo` scope — dispatches deploy/rollback to `agentboard-infra` |
| `QA_REPORTS_PAT` | PAT with `contents: write` on `agentboard-qa-reports` — publishes test reports to GitHub Pages |
| `E2E_STAGING_USER_EMAIL` | Staging smoke user email (post-deploy workflow) |
| `E2E_STAGING_USER_PASSWORD` | Staging smoke user password (post-deploy workflow) |

### QA reports portal

Test reports are published to [agentboard-qa-reports](https://github.com/matheusmafioletti/agentboard-qa-reports) GitHub Pages (`publish-reports-pr` / `publish-reports-staging`). Requires `QA_REPORTS_PAT` secret.

`publish-reports-pr` does not block merge (`continue-on-error: true`).

### E2E stack composite action

`.github/actions/e2e-stack` checks out `agentboard-infra`, logs into GHCR, and runs `e2e-up.sh` / `seed-e2e-data.sh` or `e2e-down.sh`. Image tags are passed as inputs (`auth_tag`, `board_tag`, `api_docs_tag`, `web_tag`).

## Structure

```
agentboard-backend/
├── commons/            # Shared library (no Spring Boot plugin)
├── auth-service/       # Spring Boot app, port 8080
├── board-service/      # Spring Boot app, port 8081
└── api-docs-service/   # Spring Boot app, port 8082 — unified Swagger UI
```
