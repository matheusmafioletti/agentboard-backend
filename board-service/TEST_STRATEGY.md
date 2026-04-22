# TEST_STRATEGY — board-service

## Scope

Tests covering the board state machine, card ordering, and agent-service HTTP integration.

## Test Layers

| Layer | Framework | Description |
|---|---|---|
| Unit | JUnit 5 + Mockito | State machine transitions and card ordering logic; no Spring context |
| Integration | `@SpringBootTest` + TestContainers (PostgreSQL) + WireMock | Full context; agent-service HTTP calls stubbed via WireMock |
| Contract (Provider) | Pact JVM 4.6 (`junit5spring`) | Verifies board-service fulfills contracts published by agent-service (Pact consumer) |

## Mocked Components

- **agent-service HTTP calls** — mocked via WireMock 3.x in integration tests

## Pact Involvement

board-service is a **Pact Provider** — it verifies interactions defined by the agent-service consumer.
Contract verification tasks: `./gradlew pactVerify`

## Directory Layout

```
src/test/java/com/agentboard/board/
├── unit/           ← State machine, ordering logic
├── integration/    ← @SpringBootTest + TestContainers + WireMock
└── contract/       ← Pact provider verification tests
```
