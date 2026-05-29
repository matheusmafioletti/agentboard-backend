# TEST_STRATEGY.md — api-docs-service

## Automated

| Layer | Scope |
|-------|--------|
| Integration | `OpenApiProxyIntegrationTest` — WireMock upstream stubs; proxy endpoints return expected JSON; Swagger UI redirect is reachable |

## Notes

- This module has no domain logic or database; tests focus on HTTP proxying and public Swagger UI routes.
- Upstream availability in production is validated operationally (auth + board must be running before specs load in the UI).
