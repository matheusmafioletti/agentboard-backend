# TEST_STRATEGY.md — board-service

## Automated

| Layer | Scope |
|-------|--------|
| Unit | `WorkItemServiceTest` hierarchy validation + `WorkItemDisplayKeys` format |
| Integration | `WorkItemControllerIT` REST round-trips incl. `displayKey`, `parentPreview` with `includeParent=true` |
| Consumer-driven | MCP Pact specs under `repos/agentboard-mcp-server/src/test/contract/board-service.pact.test.ts` mirror `/api/v1/work-items` DTO deltas |

## Feature 008 notes

New JSON fields on list/detail payloads: `displayKey` (always set after Flyway **V18**), optional `parentPreview` when `includeParent=true`.

Provider verification runs via Gradle Pact targets when wired in CI; regenerate consumer pacts after changing interaction bodies.

## Feature 009 notes

`displayKey` format changed from `TYPE-<hex8>` to `F`/`U`/`T` + positive integer (e.g. `F1`, `U102`, `T1023`).

- **Flyway V19**: deterministic backfill per `(project_id, type)` ordered by `created_at ASC, id ASC`.
- **Allocation**: `WorkItemService.createWorkItem` calls `WorkItemRepository.findMaxDisplayKeySeq` (native query) to determine `next = max + 1` before constructing the entity.
- **Unit**: `WorkItemServiceTest#displayKeyFormat_usesTypePrefixAndSequentialInteger` and `displayKeyPrefix_returnsCorrectCharPerType` cover `WorkItemDisplayKeys`.
- **IT**: `WorkItemControllerIT` assertions changed from `startsWith("FEATURE-")` to `matchesPattern("F[1-9][0-9]*")`.
- **Pact**: MCP consumer pact `displayKey` examples updated to `F1` / `U1`; provider verification must pass before merge.
