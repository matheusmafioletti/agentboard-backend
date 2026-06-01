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

On push to `main`, the CI workflow builds images, pushes to GHCR, and SSH-deploys to the VPS configured via GitHub Secrets:

| Secret | Description |
|--------|-------------|
| `VPS_HOST` | VPS IP or hostname |
| `VPS_USER` | SSH user |
| `VPS_SSH_KEY` | Private SSH key |
| `VPS_DEPLOY_PATH` | Path to `agentboard-infra` clone (e.g. `/opt/agentboard`) |

Orchestration lives in [agentboard-infra](https://github.com/agentboard/agentboard-infra) (`docker-compose.prod.yml`).

## Structure

```
agentboard-backend/
├── commons/            # Shared library (no Spring Boot plugin)
├── auth-service/       # Spring Boot app, port 8080
├── board-service/      # Spring Boot app, port 8081
└── api-docs-service/   # Spring Boot app, port 8082 — unified Swagger UI
```
