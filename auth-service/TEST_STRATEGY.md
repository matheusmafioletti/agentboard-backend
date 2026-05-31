# auth-service — Test Strategy (Feature 010)

## Layers

| Layer | Scope | Tooling |
|-------|--------|---------|
| Unit | `MembershipService`, `InviteService`, token hashing | JUnit 5, Mockito |
| Integration | Register, login selection, create tenant, invites, admin CRUD | Spring Boot Test, Testcontainers PostgreSQL 16, RestAssured/MockMvc |
| Contract | OpenAPI document lists new endpoints | `OpenApiDocsIntegrationTest` |

## Key test classes

- `unit/service/MembershipServiceTest.java`
- `unit/service/InviteServiceTest.java`
- `integration/LegacyMembershipMigrationIT.java`
- `integration/RegisterIntegrationTest.java`
- `integration/LoginTenantSelectionIntegrationTest.java`
- `integration/CreateTenantIntegrationTest.java`
- `integration/TenantInviteIntegrationTest.java` (identify, verify-credentials, accept new/existing user)
- `integration/LoginTenantSelectionIntegrationTest.java` (includes `switchTenant`)

## Run commands

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk21.0.8_9"
cd repos/agentboard-backend
.\gradlew.bat :auth-service:test
.\gradlew.bat :auth-service:checkstyleMain
```

Flyway runs automatically on application startup (tests and `bootRun`).
