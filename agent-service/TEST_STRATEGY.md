# TEST_STRATEGY — agent-service

## Scope

Tests covering schema validation, timeout logic, and board-service HTTP integration.

## Test Layers

| Layer | Framework | Description |
|---|---|---|
| Unit | JUnit 5 + Mockito | Schema validation and timeout logic; no Spring context |
| Integration | `@SpringBootTest` + TestContainers (PostgreSQL) + WireMock | Full context; board-service HTTP calls stubbed via WireMock |
| Contract (Consumer) | Pact JVM 4.6 (`consumer/junit5`) | Defines interactions agent-service expects from board-service; publishes pact files |

## Mocked Components

- **board-service HTTP calls** — mocked via WireMock 3.x in integration tests

## Pact Involvement

agent-service is a **Pact Consumer** — it defines and publishes the contracts that board-service must verify.
Consumer test tasks: `./gradlew test` (pact files written to `build/pacts/`)

## Directory Layout

```
src/test/java/com/agentboard/agent/
├── unit/           ← Schema validation, timeout logic
├── integration/    ← @SpringBootTest + TestContainers + WireMock
└── contract/       ← Pact consumer interaction definitions
```
