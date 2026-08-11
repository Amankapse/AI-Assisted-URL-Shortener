# Testing Strategy

## Test tiers

- Unit tests: validate service, validation, and Phase 2 ownership logic in isolation.
- Integration tests: verify API flows against PostgreSQL using Testcontainers.
- Security tests: validate authentication, authorization, CSRF decisions, token handling, secure ownership, and deny-by-default behavior.
- Regression tests: preserve existing behavior during brownfield enhancements.

## Key coverage areas

- Phase 2 URL foundation: create, read, list, update expiration, delete, enable/disable, custom alias validation, generated short-code collision retry, and collision exhaustion.
- Phase 2 ownership: service and repository access are scoped through `CurrentOwnerProvider` and `OwnerIdentity`; production request DTOs do not accept owner identifiers.
- Phase 2 redirect: active redirects return 302 and disabled, expired, deleted, or missing links return safe RFC7807 responses.
- Phase 2 persistence: PostgreSQL Testcontainers runs Flyway V1, Hibernate schema validation, unique constraints, foreign keys, optimistic versioning, owner filtering, pagination, and short-code resolution checks.
- Phase 2 API: MockMvc verifies DTO validation, malformed JSON, pagination bounds, OpenAPI `/v3/api-docs`, and response DTOs that do not expose JPA internals.
- Phase 3 auth/security: registration, login, BCrypt hashing, generic authentication failures, RS256 JWT validation, issuer/audience/expiration/claim enforcement, non-RS256 rejection, refresh-token rotation and reuse detection, CSRF-protected refresh/logout, explicit cookie clearing, URL ownership enforcement from JWT `sub`, owner-input injection rejection, and deny-by-default admin path behavior.
- Phase 4 Redis cache: cache hit/miss behavior, malformed and unsupported cache entries, TTL bounding, Redis read/write/eviction failure fallback, after-commit invalidation through Redis integration, and bounded single-flight concurrency/timeout/capacity behavior.
- Phase 4 analytics: sanitized click metadata, bounded queue overload drops, retry behavior, idempotent batch persistence, atomic aggregate updates, owner analytics, admin analytics authorization, top-links limits, soft-delete analytics preservation, and Redis Testcontainers integration.
- Phase 5 operations: Redis Lua rate limiting, 429 RFC7807 responses, correlation ID validation, security headers, Actuator exposure, liveness/readiness policy, application metric counters, sensitive metric tag prevention, and documented Redis/PostgreSQL failure behavior.
- Hyperscale evolution: configurable 8-character short-code generation, invalid configuration bounds, collision metrics, quota enforcement, V4 blocked moderation state, admin block/unblock authorization, blocked redirect behavior, blocked cache DTO handling, and legacy 7-character short-code resolution.
- Enterprise evolution Stage 1-3: derived full short URL responses, trailing slash normalization, optional idempotent URL creation, same-key replay, different-fingerprint conflicts, concurrent idempotent create safety, destination editing, and ETag/If-Match lost-update prevention.
- Enterprise evolution Stage 4: default workspace provisioning on registration, workspace APIs, centralized workspace authorization, `X-Workspace-ID` resolution, tenant-isolated URL management, role matrix behavior, viewer analytics access, platform-admin separation from workspace admin, invalid explicit workspace headers, V6 migration tables/indexes, and workspace-scoped idempotency keys.
- Enterprise evolution Stage 5: V7 audit migration, URL mutation audit events, idempotency replay without duplicate audit rows, stale ETag failure without audit rows, redacted destination metadata, workspace membership audit events, audit endpoint authorization, platform moderation audit, and audit metadata size rejection.
- Enterprise evolution Stage 6: V8 API-key migration, human OWNER/ADMIN API-key management, raw-key one-time return, digest-only storage, scoped machine URL and analytics access, workspace isolation, malformed/dual/revoked/expired key rejection, API-key/human idempotency separation, API-key audit actor attribution, and public redirect behavior when malformed API-key headers are present.
- Enterprise evolution Stage 7: V9 outbox migration, same-transaction URL/audit/outbox behavior, rollback of failed outbox publishing, PostgreSQL `SKIP LOCKED` claim isolation, stale claim recovery, retry/dead-letter behavior, cache invalidation handler success, admin outbox authorization and safe DTO behavior, and UTC clock alignment for dispatcher scheduling.
- Enterprise evolution Stage 8: V10 campaigns/tags/search migration, campaign lifecycle RBAC, campaign delete URL detach behavior, normalized tag validation and workspace isolation, URL create with campaign/tags, URL metadata ETag updates, bounded URL search/filtering, search exclusion of raw destination query strings, API-key read/write metadata boundaries, idempotency fingerprint normalization for unordered tag sets, and audit events for campaign/tag mutations.
- Stage 9A Angular frontend foundation: runtime config validation, Problem Details mapping, memory-only access-token state, refresh single-flight, auth header propagation, workspace header propagation, and app bootstrap shell.
- Future areas: retention jobs, mutation testing, and formal SAST/SCA tooling.

## Tooling

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers for PostgreSQL and Redis integration tests
- MockMvc for controller/security tests
- JaCoCo for coverage reporting
- Angular CLI unit-test builder with Vitest/jsdom for frontend tests
- k6 scripts for load/performance validation

## Historical phase validation results

The following phase counts are retained as historical milestones. Before Stage 8, the suite contained 113 backend tests. Stage 8 added 5 integration tests, bringing the verified backend suite to 118 tests. Stage 9A adds 7 Angular frontend unit tests.

## Historical Phase 2 validation result

The Phase 2 validation suite now contains 41 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers with `jdbc:tc:postgresql:15-alpine:///shortener`, applied `V1__initial_schema.sql`, and validated Hibernate mappings against the migrated schema.

## Historical Phase 3 validation result

The Phase 3 validation suite contains 45 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers, applied `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql`, validated Hibernate mappings against the migrated schema, and verified the complete current test suite. The dependency tree confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Security `6.5.0`, and transitive `spring-security-oauth2-jose:6.5.0`.

## Historical Phase 4 validation result

The Phase 4 validation suite contains 60 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers and Redis Testcontainers, applied `V1__initial_schema.sql`, `V2__authentication_refresh_tokens.sql`, and `V3__click_analytics_indexes.sql`, validated Hibernate mappings against the migrated PostgreSQL schema, and verified the complete current test suite. The dependency tree confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no direct Jedis dependency. `docker compose config` passed with only the existing obsolete `version` warning.

## Stage 8 validation result

Stage 8 implementation is fully validated:

```powershell
docker info
.\mvnw.cmd -q -Dtest=OrganizationSearchIntegrationTests test
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

Validation confirmed Docker Desktop server 29.6.1, PostgreSQL and Redis Testcontainers, Flyway V1-V10 validation/application, Hibernate schema validation, `OrganizationSearchIntegrationTests`, and the full 118-test backend suite. JaCoCo backend coverage was 85.11% line / 60.78% branch. `dependency:tree` passed. `docker compose config` passed, with Docker emitting a non-fatal local config access warning in this shell.

Defects found after Flyway startup were treated as application/test defects and corrected: stale `idempotency_records` cleanup was changed to `idempotency_keys`; Stage 8 controllers were given explicit `@PathVariable` names; `PUT /api/v1/urls/**` authorization was added; the idempotency normalization test now uses one expiration timestamp; search response mapping now handles URLs with no campaign.

## Stage 9A frontend validation result

Stage 9A frontend foundation passes:

```powershell
cd frontend
npm test -- --watch=false
npm run build
```

Validation confirmed 7 Angular unit tests and a production build with initial bundle size 264.00 kB raw / 71.95 kB estimated transfer. Stage 9A does not include URL management, analytics, audit, API-key, workspace-management, or admin UI views.

## Final Stage 9D local validation result

The final Stage 9D local validation suite contains 118 backend tests and 21 frontend tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
cd frontend
npm ci
npm test -- --watch=false
npm run build
npm run build:render
```

The final suite includes tests for Redis-backed limiter isolation and outage behavior, generic 429 Problem Details, correlation ID sanitization and propagation, Actuator exposure, security headers, liveness/readiness policy, application metric counters, bounded Micrometer tags, short-code configuration and collision metrics, quota enforcement, V4 moderation persistence, admin block/unblock authorization, blocked redirects, blocked cache DTO behavior, legacy 7-character short-code resolution, derived full short URL responses, optional idempotent create, ETag/If-Match destination updates, workspace tenant isolation, workspace RBAC, workspace-scoped idempotency, workspace-bound API-key authentication, transactional outbox delivery behavior, and Stage 8 campaign/tag/search behavior.

The final green run used PostgreSQL Testcontainers and Redis Testcontainers, validated Flyway V1 through V10, applied all ten migrations to clean PostgreSQL containers, and completed Hibernate schema validation. JaCoCo results are recorded in [coverage-summary.md](coverage-summary.md). `dependency:tree` confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Micrometer `1.15.0`, Spring Boot Actuator `3.5.0`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, Testcontainers `1.21.0`, and no added outbox or rate-limiting library. `docker compose config` passed without warnings when run with normal Docker config access.

Frontend validation confirmed `npm ci`, 21 Angular tests, the standard production build, and the Render production build. The initial Angular bundle remains 103.64 kB raw / 26.71 kB estimated transfer. `npm audit --audit-level=moderate` reports the known 3 moderate Angular CLI development-chain findings through `@modelcontextprotocol/sdk` and `@hono/node-server`; `npm audit --audit-level=high` passes. Forced remediation remains rejected because it would downgrade Angular CLI.

`scripts/verify.sh` was attempted, but the Bash environment failed before Maven startup because `JAVA_HOME` is not defined there. The equivalent required Windows commands above passed.
