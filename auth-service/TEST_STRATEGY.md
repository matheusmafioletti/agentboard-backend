# TEST_STRATEGY — auth-service

## Scope

Black-box integration tests that verify authentication and JWT issuance end-to-end.

## Test Layers

| Layer | Framework | Description |
|---|---|---|
| Integration | `@SpringBootTest` + TestContainers (PostgreSQL) | Full application context; tests register/login flows against a real Postgres instance started by TestContainers |

## Mocked Components

Nothing — all dependencies are real within the integration test container.

## Pact Involvement

None — auth-service does not expose machine-readable contracts in this feature.

## Directory Layout

```
src/test/java/com/agentboard/auth/
├── unit/           ← Unit tests (pure logic, no Spring context)
└── integration/    ← @SpringBootTest + TestContainers PostgreSQL
```

## First Tests (next feature)

- `RegisterTest` — POST /api/auth/register → 201, JWT returned
- `LoginTest` — POST /api/auth/login → 200, JWT returned
