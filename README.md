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
| `board-service` | 8081 | Kanban board state machine and card management |
| `agent-service` | 8082 | Agent task polling and execution coordination |

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

## Structure

```
agentboard-backend/
├── commons/            # Shared library (no Spring Boot plugin)
├── auth-service/       # Spring Boot app, port 8080
├── board-service/      # Spring Boot app, port 8081
└── agent-service/      # Spring Boot app, port 8082
```
