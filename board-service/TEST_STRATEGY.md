# TEST_STRATEGY — board-service

## Scope

Tests covering the board state machine, feature card CRUD, tenant isolation, WebSocket
real-time events, MCP tool API endpoints, and Pact provider contract verification.

## Test Layers

| Layer | Framework | Description |
|---|---|---|
| Unit | JUnit 5 + Mockito | `FeatureCardService`, `TaskService`, `BoardEventPublisher` logic; tenant isolation invariants; no Spring context |
| Integration | `@SpringBootTest` + TestContainers (PostgreSQL 16) + WireMock | Full context; auth-service HTTP calls stubbed via WireMock 3.x; Flyway migrations applied on startup |
| Contract (Provider) | Pact JVM 4.6 (`junit5spring`) | Verifies board-service fulfils contracts published by `agentboard-mcp-server` (Pact consumer); pact files read from `../agentboard-mcp-server/pacts/` |

## Dual Authentication

- **JWT auth** (`TenantFilter` from commons): applies to `/api/**` paths for browser clients
- **API key auth** (`ApiKeyFilter`): applies to `/api/features/**` when `X-API-Key` header present;
  used exclusively by the MCP server
- **Internal endpoints** (`/internal/**`): no auth; called only by auth-service on the same host
- **WebSocket** (`/ws`): exempt from HTTP security; STOMP CONNECT frame carries JWT via
  `TenantWebSocketInterceptor`

## WebSocket Integration Tests

- Connect with valid JWT → subscribe to tenant topic → trigger mutation → assert event received
  within 1s (`WebSocketIntegrationTest`)
- Connect with invalid JWT → assert STOMP error frame
- Tenant A connection receives only Tenant A events (`TenantIsolationApiTest`)

## Fake-Agent Pattern (E2E)

Integration tests use direct HTTP calls (RestAssured) to simulate MCP tool calls, bypassing the
real MCP server. This decouples board-service E2E validation from the Node.js MCP server process.

## Directory Layout

```
src/test/java/com/agentboard/board/
├── unit/
│   ├── FeatureCardServiceTest.java
│   ├── TaskServiceTest.java
│   └── BoardEventPublisherTest.java
├── integration/
│   ├── FeatureCardApiTest.java
│   ├── CardMoveApiTest.java
│   ├── McpApiTest.java
│   ├── McpMutationApiTest.java
│   ├── WebSocketIntegrationTest.java
│   └── TenantIsolationApiTest.java
└── contract/
    └── BoardServicePactProviderTest.java
```

## Running Tests

```bash
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk21.0.8_9"
.\gradlew.bat :board-service:test
.\gradlew.bat :board-service:pactVerify
```
