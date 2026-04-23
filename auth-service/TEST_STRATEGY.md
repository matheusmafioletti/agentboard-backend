# TEST_STRATEGY — auth-service

## Scope

Integration and unit tests covering registration, login, JWT issuance, and tenant creation
end-to-end. The `TenantFilter` from `commons` is exercised through integration tests that hit
live HTTP endpoints.

## Test Layers

| Layer | Framework | Description |
|---|---|---|
| Unit | JUnit 5 + Mockito | `JwtTokenService` token generation and claim extraction; `AuthService` registration/login logic |
| Integration | `@SpringBootTest` + TestContainers (PostgreSQL 16) | Full application context; tests register/login flows against a real Postgres instance started by TestContainers; validates JWT + API key returned on registration |

## Mocked Components

Nothing — all dependencies are real within the integration test container.

## Pact Involvement

None — auth-service does not expose machine-readable contracts in this feature.

## Directory Layout

```
src/test/java/com/agentboard/auth/
├── unit/           ← Unit tests (pure logic, no Spring context)
└── integration/    ← @SpringBootTest + TestContainers PostgreSQL 16
    ├── RegisterIntegrationTest.java
    └── LoginIntegrationTest.java
```

## Key Test Scenarios

### RegisterIntegrationTest
- `POST /auth/register` happy path → 201, response includes JWT + API key
- Duplicate email → 409
- Missing required fields → 400

### LoginIntegrationTest
- `POST /auth/login` valid credentials → 200, JWT returned
- Wrong password → 401
- Unknown email → 401

## Running Tests

```bash
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk21.0.8_9"
.\gradlew.bat :auth-service:test
```
