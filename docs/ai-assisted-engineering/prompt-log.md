# AI Prompt Log

This document records the major prompts used during AI-assisted planning and implementation.

| ID | Objective | Prompt Summary | Result | Decision |
| --- | --- | --- | --- | --- |
| P-001 | Requirements analysis | Analyze assignment and propose Phase 2 core implementation plan | Produced a phase-aligned implementation plan | Accepted |
| P-002 | Architecture validation | Confirm modular monolith package structure and owner abstraction design | Validated package ownership and service boundaries | Accepted |
| P-003 | Domain modeling | Generate JPA entities and relationships for users, URLs, and clicks | Created entity classes and repository interfaces | Accepted |
| P-004 | Migration design | Create Flyway schema migration for UUID PKs and owner FK | Added initial schema migration file | Accepted |
| P-005 | URL validation | Define original URL, alias, and expiration rules | Added validation service and safe URL checks | Accepted |
| P-006 | Owner provider | Introduce temporary current-owner abstraction for Phase 2 | Added `CurrentOwnerProvider` and placeholder implementation | Accepted |
| P-007 | Service layer | Implement `UrlService` with owner-aware repository access | Added create/get/list/update/delete/enable/disable logic | Accepted |
| P-008 | Controller layer | Expose REST endpoints without accepting owner input | Added URL creation and management controllers | Accepted |
| P-009 | Error handling | Define RFC7807-style exception handling | Added global `ProblemDetailExceptionHandler` | Accepted |
| P-010 | Unit testing | Update `UrlServiceTests` to align with owner abstraction | Fixed owner lookup mocking and validation assertions | Accepted |
| P-011 | Dependency resolution | Fix missing OpenAPI and Flyway dependencies that blocked compile | Updated `pom.xml` with required dependencies | Accepted |
| P-012 | Build validation | Run targeted Maven tests after refactor | Verified `UrlServiceTests` pass | Accepted |
| P-013 | Phase 2 validation | Resolve Maven/Testcontainers/Flyway validation blockers without Phase 3 work | Added test Docker API pin, added approved Flyway PostgreSQL module, switched test profile to Hibernate schema validation | Accepted |
| P-014 | Phase 2 end-to-end validation | Expand Phase 2 tests across repositories, services, controllers, redirects, OpenAPI, pagination, collisions, and RFC7807 errors | Added regression coverage and fixed defects found after Flyway/Testcontainers startup | Accepted |
| P-015 | Phase 3 authentication | Implement approved authentication, authorization, and secure ownership refinements without Redis/rate limiting/observability/admin expansion | Added Spring Security resource server, RS256 JWT, refresh-token rotation, CSRF refresh/logout, secure current-owner provider, V2 refresh-token migration, tests, and docs | Accepted |
| P-016 | Phase 4 Redis and analytics | Implement approved Redis cache-aside and click analytics refinements without Phase 5 work | Added Redis redirect cache, single-flight miss protection, async sanitized analytics, V3 migration, owner/admin analytics endpoints, tests, and docs | Accepted |
| P-017 | Phase 5 operational readiness | Implement approved observability, Redis Lua rate limiting, health/readiness, security hardening, k6 scripts, and operations documentation without new business features | Added bounded Micrometer metrics, correlation IDs, protected Actuator metrics, Redis Lua limiter policies, security headers, production analytics pepper validation, k6 scripts, tests, and docs | Accepted |
| P-018 | Phase 6 release readiness | Finalize CI/CD, quality evidence, documentation, dependency/secret audit, coverage, and submission summary without product changes | Added JaCoCo reporting, expanded GitHub Actions, removed obsolete Compose version, rewrote README, added final engineering summary and release checklist, updated traceability | Edited |
| P-019 | Final documentation synchronization | Perform final README and documentation synchronization pass without feature work | Reconciled README, docs index, stale phase language, coverage values, local startup instructions, and k6 request schema | Edited |
| P-020 | Hyperscale production evolution | Evolve the validated baseline toward 100M new URLs/day without rewriting or adding distributed infrastructure | Added hyperscale NFR/capacity/architecture docs, configurable 8-character Base62 generation, collision metrics, config-driven quotas, V4 blocked moderation, admin block/unblock, tests, and traceability | Edited |
| P-021 | Render live deployment configuration | Prepare deployment configuration for Render Web Service, Neon PostgreSQL, and Render Key Value without feature or architecture changes | Added `prod` profile, Render port support, Docker JAR runtime, deployment variable template, deployment docs, README links, and secret-ignore hardening | Edited |
| P-022 | Enterprise production evolution Stages 1-3 | Implement approved public short URL representation, idempotent URL creation, and destination editing with optimistic HTTP concurrency | Added derived `shortUrl`, V5 idempotency table, optional `Idempotency-Key`, destination PATCH with ETag/If-Match, tests, and docs | Edited |
| P-023 | Stage 4 workspace tenant foundation and workspace RBAC | Implement workspace tenant foundation and workspace RBAC only | Added V6 workspaces/memberships, default workspace provisioning, workspace role checks, workspace-scoped URL/analytics/idempotency behavior, tests, and docs | Edited |
| P-024 | Stage 5 immutable enterprise audit trail | Implement immutable enterprise audit trail only | Added V7 audit events, audit entities/repository/service/query APIs, transactional audit hooks for URL/workspace/membership/moderation mutations, bounded/redacted metadata, tests, and docs | Edited |

## P-013 Validation Notes

- Blocker: Flyway failed during PostgreSQL Testcontainers startup with `Unsupported Database: PostgreSQL 15.18`.
- Root cause: Flyway 11 separates PostgreSQL database support from `flyway-core`.
- Approved correction: retained `flyway-core` and added `org.flywaydb:flyway-database-postgresql` without an explicit version.
- Test environment correction: added `src/test/resources/docker-java.properties` with `api.version=1.44` so Testcontainers 1.x can talk to Docker Engine 29.
- Validation: `.\mvnw.cmd clean verify` passed with PostgreSQL Testcontainers, Flyway V1 migration, and Hibernate `ddl-auto=validate`.
- Dependency validation: `.\mvnw.cmd dependency:tree` confirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Follow-up: after Flyway started successfully, later failures were treated as application/test defects. The Phase 2 suite was expanded in P-014.

## P-014 Validation Notes

- Scope: Phase 2 validation only. No Phase 3 auth/JWT/Redis cache/rate limiting/observability work was started.
- Added tests for short-code generation, URL validation, owner-aware URL service behavior, short-code collision retries and exhaustion, redirect resolution, repository constraints and pagination on PostgreSQL Testcontainers, controller DTO validation, redirect behavior, RFC7807 responses, and OpenAPI exposure.
- Defects found after Flyway startup:
  - PostgreSQL named enum writes failed for `UserRole` and `UserStatus`; fixed Hibernate enum mapping with `SqlTypes.NAMED_ENUM`.
  - Read/manage operations could create a placeholder owner; fixed them to require an existing owner while keeping create as the bootstrap path.
  - Collision exhaustion surfaced as `IllegalStateException`; fixed to return a safe `BadRequestException`.
  - Malformed JSON fell through to the generic 500 handler; added an RFC7807 malformed-request handler.
  - Controller request/path variables relied on missing Java parameter metadata; made binding names explicit.
  - Pagination bounds were not enforced at the controller boundary; added `@Min`/`@Max` validation.
  - `springdoc-openapi-starter-webmvc-ui:2.2.0` was incompatible with Spring Boot 3.5 / Spring Framework 6.2 and failed `/v3/api-docs`; updated to the Spring Boot 3.5-compatible `2.8.6` line.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 41 tests, PostgreSQL Testcontainers, Flyway V1 migration, and Hibernate schema validation.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, and `springdoc-openapi-starter-webmvc-ui:2.8.6`.
  - `docker compose config` passed; Docker Compose reported only the existing obsolete `version` attribute warning.
  - `.\scripts\verify.ps1` was not run because that file does not exist.
- Coverage: no JaCoCo plugin is configured yet, so line/branch coverage was not generated during this validation.

## P-015 Validation Notes

- Scope: Phase 3 authentication, authorization, and secure ownership only. Redis cache, rate limiting, observability expansion, and admin feature expansion were not started.
- Approved dependencies: added only `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`. No explicit `spring-security-oauth2-jose` dependency was added; it is present transitively from resource server.
- Approved migration: added `V2__authentication_refresh_tokens.sql`; `V1__initial_schema.sql` was not modified.
- Carried-forward blocker record: Flyway PostgreSQL support was missing from `flyway-core`; root cause was Flyway 11 separating database-specific support; approved correction was the production/runtime `org.flywaydb:flyway-database-postgresql` dependency managed by Spring Boot. Validation reconfirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Defects found after Flyway started were treated as code/test defects:
  - V2 digest column initially mismatched Hibernate validation and was corrected to `VARCHAR(64)`.
  - Refresh-token family revocation was rolled back by the outer transaction during reuse detection; fixed with no-rollback handling and explicit family revocation.
  - Invalid refresh responses needed to clear the cookie; fixed at the controller boundary.
  - Repository integration cleanup needed to delete refresh tokens before users because of the V2 foreign key.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 45 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Flyway applied `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql`.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed Spring Boot-managed compatible versions: `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Security `6.5.0`, and transitive `spring-security-oauth2-jose:6.5.0`.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.

## P-016 Validation Notes

- Scope: Phase 4 Redis caching and click analytics only. No rate limiting, broad observability expansion, retention jobs, or Phase 5 work was started.
- Approved dependency: added only `spring-boot-starter-data-redis`; no direct `redis.clients:jedis` dependency was added.
- Approved migration: added `V3__click_analytics_indexes.sql`; `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql` were not modified.
- Carried-forward blocker record: Flyway PostgreSQL support is separated from `flyway-core`; approved correction remains the production/runtime `org.flywaydb:flyway-database-postgresql` dependency managed by Spring Boot. Validation reconfirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Defects found after Flyway started were treated as migration/schema/mapping/test defects:
  - Physical URL deletion violated the `click_events` foreign key once analytics existed; fixed by introducing soft delete in V3 and filtering normal owner/redirect queries while preserving analytics history.
  - Existing auth test cleanup did not delete short URLs before users after cross-suite data was present; fixed cleanup order.
  - Redirect analytics assertions raced the async queue; fixed tests to use bounded eventual assertions.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 60 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Redis Testcontainers started successfully for cache integration validation.
  - Flyway validated 3 migrations and applied V1, V2, and V3.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no direct Jedis dependency.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.

## P-017 Validation Notes

- Scope: Phase 5 operational readiness only. No Phase 6 work, new business features, authentication redesign, Redis cache redesign, analytics redesign, migration change, or new production dependency was introduced.
- Approved correction/design:
  - Used the existing `StringRedisTemplate` with an atomic Redis Lua fixed-window script.
  - Added configuration-driven limiter policies for registration, login, refresh, URL creation, public redirect, and admin analytics.
  - Kept login/register/refresh fail-closed, public redirect fail-open, and URL/admin limiters fail-open as documented.
  - Protected `/actuator/metrics` behind `ROLE_ADMIN`; exposed only health, liveness, readiness, info, and metrics at management exposure level.
  - Centralized `X-Correlation-ID` validation in a servlet filter and reused the same ID in Problem Details.
- Defects and refinements found during Phase 5:
  - Root health/readiness initially became 503 when Redis was unavailable because Redis health was still part of aggregate health status; fixed by excluding Redis health and documenting Redis degradation through metrics/logs.
  - URL creation alias conflicts initially double-counted failure metrics as both alias conflict and validation; fixed to record a single bounded reason.
  - A Redis Testcontainers integration test left the Lettuce client active until container shutdown; added explicit client cleanup.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 72 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Redis Testcontainers started successfully for cache and rate-limit integration validation.
  - Flyway validated 3 migrations and applied V1, V2, and V3.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Boot Actuator `3.5.0`, Micrometer `1.15.0`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no added rate-limiting library.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.
  - k6 was not executed because it is not installed in this environment.
  - `scripts/verify.sh` was attempted but failed before Maven startup because Bash did not have `JAVA_HOME` configured; the equivalent Windows validation commands passed.

## P-017 AI Output Examples

### Accepted

AI-generated Redis Lua fixed-window limiting through `StringRedisTemplate` was accepted because it met the no-new-dependency requirement and keeps Redis operations atomic.

### Edited

AI-generated URL creation metrics initially counted a custom-alias conflict twice. The implementation was edited to emit only `reason=alias_conflict`.

### Rejected

Treating Redis health as part of readiness was rejected after tests showed Redis outage made health probes fail. Redis is not required for correctness because PostgreSQL fallback exists, so Redis was removed from Actuator health readiness and is tracked through degradation metrics instead.

## P-018 Validation Notes

- Scope: final release readiness only. No product feature, migration, authentication redesign, microservice split, Kafka, or new infrastructure was introduced.
- Release gaps addressed:
  - Added JaCoCo Maven plugin and generated coverage evidence.
  - Expanded GitHub Actions workflow for Java 21, Docker availability, Maven verify, Compose validation, and artifact upload.
  - Removed obsolete top-level Compose `version`.
  - Rewrote README for final reviewer evaluation.
  - Added final engineering summary, release readiness checklist, and test quality review.
  - Sanitized `.env.example` secret-bearing values into placeholders.
  - Removed redundant Testcontainers BOM/property so Spring Boot dependency management is authoritative.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 72 tests.
  - JaCoCo generated line coverage 83.91% and branch coverage 63.25%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1, V2, and V3.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed and confirmed no direct Jedis dependency and no rate-limiting library.
  - `docker compose config` passed without warnings.
  - k6 was not executed because it is not installed.

## P-018 AI Output Examples

### Accepted

AI-generated final README structure was accepted because it presents the project summary, architecture, quick start, validation, security, AI traceability, and limitations in reviewer-friendly form.

### Edited

AI initially left `.env.example` with local concrete secret-bearing values. The file was edited to use placeholders for database password, RSA key material, analytics pepper, and rate-limit key salt.

### Rejected

Adding heavy late-stage SAST/SCA tooling was rejected because it would introduce new build risk at the final phase without prior approval. The final submission records manual secret/dependency checks and leaves formal SAST/SCA as production evolution.

## P-019 Validation Notes

- Scope: final documentation and README synchronization only. No application feature, authentication, authorization, migration, dependency, or architecture change was introduced.
- Documentation updates:
  - Rewrote `README.md` into a reviewer-facing entry point with prerequisites, environment variables, local RSA key generation, startup sequence, API workflow, health/OpenAPI URLs, testing, coverage, security, limitations, and documentation index.
  - Added `docs/README.md` as a documentation index.
  - Corrected stale final-state language in testing, performance, rollback, requirements, and authentication documentation.
  - Updated coverage references after the latest JaCoCo output: line coverage 83.91% and branch coverage 63.25%.
- Defect found during documentation verification:
  - `performance/k6/url-create.js` did not include the required `expiresAt` request field documented by the current API DTO. The script was corrected so the performance artifact matches the actual API contract.
- Secret hygiene correction:
  - Added local JWT private-key filename patterns to `.gitignore`; no key material was committed.
- Validation:
  - Markdown link check covered 40 Markdown files outside `target` and found no broken relative links.
  - `docker compose up -d` started local PostgreSQL and Redis infrastructure, and `docker compose ps` reported both services healthy.
  - `docker compose config` passed without warnings.
  - `.\mvnw.cmd clean verify` passed with 72 tests and generated JaCoCo coverage.

## P-019 AI Output Examples

### Accepted

AI-generated final README structure was accepted because it matched the requested evaluator-facing sections and pointed readers to the supporting architecture, security, testing, operations, and AI traceability documents.

### Edited

AI-generated coverage references were edited after rerunning JaCoCo so the repository reports the measured final values: 83.91% line coverage and 63.25% branch coverage.

### Rejected

Claiming Docker Compose starts the full application was rejected. The README now states that `compose.yaml` starts PostgreSQL and Redis infrastructure only, while the Spring Boot application is started separately through the Maven wrapper.

## P-020 Validation Notes

- Scope: hyperscale production evolution as an incremental change to the validated baseline.
- Implemented now:
  - Config-driven short-code generation using `shortener.code.length=8` and `shortener.code.max-retries=5`.
  - Cryptographically secure Base62 generation retained.
  - Existing 7-character short-code resolution preserved; no historical short codes are rewritten.
  - Bounded collision retries now emit low-cardinality metrics for success, retry, and exhaustion.
  - Config-driven `UrlQuotaService` added for daily creations, active links, and daily custom aliases.
  - Flyway V4 added `short_urls.blocked BOOLEAN NOT NULL DEFAULT FALSE` plus an index.
  - Admin block/unblock endpoints added under `/api/v1/admin/urls/{id}`.
  - Redirect cache DTO includes blocked state; blocked redirects return safe not-found behavior.
- Intentionally deferred as architecture-only:
  - Distributed URL mapping store, Redis Cluster, CDN/edge routing, WAF/global load balancer, durable event stream, OLAP warehouse, multi-AZ/multi-region deployment, and external malware/phishing provider.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 84 tests.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1, V2, V3, and V4.
  - Hibernate schema validation succeeded.
  - JaCoCo generated line coverage 85.52% and branch coverage 66.49%.
  - `.\mvnw.cmd dependency:tree` passed and confirmed no new production dependency was added.
  - `docker compose config` passed without warnings.

## P-020 AI Output Examples

### Accepted

The AI-generated separation between implemented baseline and architecture-only hyperscale components was accepted because it prevents false claims about CDN, Kafka, Redis Cluster, distributed KV storage, WAF, and multi-region infrastructure.

### Edited

The moderation model was narrowed to a `blocked` boolean instead of a lifecycle enum replacement. This preserved existing enabled, deleted, and expiration behavior while adding the approved abuse-control state.

### Rejected

Switching short-code generation to MD5 truncation, sequential public IDs, or Hashids-as-security was rejected. The implementation retained cryptographically secure random Base62 codes and the PostgreSQL unique constraint as the final concurrency-safe authority.

## P-021 Validation Notes

- Scope: Render live deployment configuration only. No business logic, authentication behavior, Flyway migration, public API contract, or production dependency was changed.
- Implemented:
  - Added `application-prod.yml` with environment-driven Neon PostgreSQL, Render Key Value, JWT, CORS, analytics privacy, rate-limit salt, conservative Hikari, Flyway enabled, and Hibernate `ddl-auto=validate`.
  - Added shared `server.port=${PORT:8080}` for Render while preserving local default port 8080.
  - Changed Docker runtime startup to run the built application JAR from a Java 21 JRE image instead of Maven `spring-boot:run`.
  - Added `.dockerignore` and expanded `.gitignore` to avoid sending or committing local env files, PEM/key files, and `secrets/`.
  - Reorganized `.env.example` into deployment sections with placeholders only.
  - Added Render, Neon, environment-variable, production-validation, and free-tier limitation docs.
  - Fixed `application-local.yml` profile activation from invalid `spring.profiles` to `spring.config.activate.on-profile` after the local startup smoke exposed the Spring Boot 3.5 compatibility defect.
  - Bound `spring-boot:repackage` in `pom.xml` after Render deployment logs showed `no main manifest attribute` for `/app/app.jar`.
- Selected mechanisms:
  - Redis/Valkey: `SPRING_DATA_REDIS_URL` for Render Key Value internal URL.
  - JWT: existing `APP_AUTH_PRIVATE_KEY_PEM` and `APP_AUTH_PUBLIC_KEY_PEM` environment PEM content; no production test-key fallback.
  - Health check: `/actuator/health/liveness`; readiness still requires PostgreSQL and excludes Redis.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 84 tests and JaCoCo line coverage 84.44% / branch coverage 65.41%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1, V2, V3, and V4.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed and confirmed no new production dependency.
  - `docker compose config` passed.
  - Markdown link check passed for 54 Markdown files.
  - `docker compose up -d` started local infrastructure and a bounded local `spring-boot:run` liveness smoke passed using temporary in-memory RSA key material.
  - `docker build -t url-shortener-render-smoke .` passed.
  - After the packaging fix, `.\mvnw.cmd clean package -DskipTests` showed `spring-boot:repackage` replacing the main artifact; manifest inspection confirmed `Main-Class: org.springframework.boot.loader.launch.JarLauncher` and `Start-Class: com.example.urlshortener.UrlShortenerApplication`; bounded `java -jar target\url-shortener-0.1.0.jar` liveness smoke passed; Docker image rebuild passed; `.\mvnw.cmd clean verify` passed with 84 tests.

## P-021 AI Output Examples

### Accepted

Using Spring Boot `spring.data.redis.url` in the production profile was accepted because it supports Render Key Value internal URLs with the smallest profile-only change.

### Edited

The initial deployment template used generic `JWT_*` names from the request. It was edited to the repository's existing `APP_AUTH_*` property names to avoid duplicate configuration paths.

The local compatibility smoke exposed invalid Spring Boot 3.5 profile activation in `application-local.yml`; the config was edited to `spring.config.activate.on-profile`.

Render deployment logs exposed a non-executable normal JAR. The build configuration was edited to bind `spring-boot:repackage` because this project imports Spring Boot dependency management without using `spring-boot-starter-parent`.

### Rejected

Hard-coding Render or Neon hostnames, adding a new Redis client dependency, and changing Flyway migrations were rejected as outside the deployment-only scope.

Changing the Docker entrypoint back to Maven `spring-boot:run` was rejected because Render runtime should execute the packaged application artifact, not run Maven in production.

## P-022 Validation Notes

- Scope: enterprise production evolution Stages 1-3 only. Workspace migration, audit, API keys, outbox, analytics abstraction, mapping-store abstraction, QR codes, tracing, and Angular frontend were not started.
- Approved migration: added `V5__idempotency_keys.sql`; V1-V4 were not modified.
- Implemented:
  - `APP_PUBLIC_BASE_URL` and derived `shortUrl` response field. Full short URLs are not persisted.
  - Optional `Idempotency-Key` support for `POST /api/v1/urls`, scoped to the authenticated user.
  - SHA-256 request fingerprints and PostgreSQL unique constraint `(scope, idempotency_key)` as the authoritative concurrency boundary.
  - Same key plus same request replays the original successful response; same key plus different request returns RFC7807 HTTP 409.
  - `PATCH /api/v1/urls/{id}/destination` updates `short_urls.original_url`, preserves the short code and analytics history, validates the destination, enforces ownership, and invalidates redirect cache after commit.
  - `ETag` on URL management responses and required `If-Match` for destination changes to prevent silent lost updates.
- Focused validation:
  - `.\mvnw.cmd -q "-Dtest=UrlServiceTests,UrlControllerIntegrationTests" test` passed.
  - PostgreSQL Testcontainers started successfully.
  - Flyway validated and applied V1-V5.
  - Hibernate schema validation succeeded.
- Full validation:
  - `.\mvnw.cmd clean verify` passed with 89 tests and JaCoCo line coverage 85.83% / branch coverage 65.58%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1-V5.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed and confirmed no new production dependency.
  - `docker compose config` passed without warnings.
- Defect found during full validation:
  - Existing JWT tampering test used a brittle last-two-character mutation that did not always invalidate the token. The test was edited to append an extra character, preserving the security assertion without changing production auth behavior.

## P-023 Validation Notes

- Scope: Stage 4 workspace/tenant foundation and workspace RBAC only.
- Explicitly not started: audit/event table, API keys, outbox, tags/campaigns/search, QR codes, tracing, Angular frontend, and Stage 5 work.
- Approved migration: added `V6__workspaces_and_memberships.sql`; V1-V5 were not modified.
- Implemented:
  - `workspaces`, `workspace_memberships`, and `WorkspaceRole` with `OWNER`, `ADMIN`, `EDITOR`, `ANALYST`, and `VIEWER`.
  - `short_urls.workspace_id` with backfill for existing users/URLs, not-null constraint, foreign key, and workspace indexes.
  - Default workspace plus `OWNER` membership creation during registration.
  - Central workspace authorization service and resolver using optional `X-Workspace-ID`; absent header falls back to default workspace, invalid/unauthorized explicit header does not.
  - URL management and owner analytics repository queries now include `workspace_id`; public redirects remain unchanged.
  - `short_urls.owner_id` retained as creator/legacy actor metadata, not the tenant boundary.
  - Idempotent URL creation scope changed to workspace + actor + operation + key while preserving V5 storage.
  - Workspace APIs for listing/creating workspaces and managing existing-user memberships.
  - URL quotas now count workspace resources while request rate limiting remains user/request scoped.
  - Stage 5 audit-hook candidates identified but not implemented: `WORKSPACE_CREATED`, `MEMBER_ADDED`, `MEMBER_ROLE_CHANGED`, and `MEMBER_REMOVED`.
- Defects found and corrected:
  - Workspace controller path variables initially depended on compiler parameter metadata; fixed with explicit `@PathVariable` names.
  - Integration fixture created duplicate default workspaces for registered users; fixed to reuse existing default membership.
- Validation:
  - `.\mvnw.cmd -q -Dtest=WorkspaceControllerIntegrationTests test` passed.
  - `.\mvnw.cmd clean verify` passed with 92 tests and JaCoCo line coverage 84.48% / branch coverage 63.42%.
  - Flyway validated and applied V1-V6.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed with no new production dependency.
  - `docker compose config` passed.

## P-024 Validation Notes

- Scope: Stage 5 immutable enterprise audit trail only.
- Explicitly not started: API keys, transactional outbox, tags/campaigns/search, QR codes, tracing, Angular frontend, and distributed infrastructure.
- Approved migration: added `V7__audit_events.sql`; V1-V6 were not modified.
- Implemented:
  - `AuditEventEntity`, `AuditAction`, `AuditActorType`, `AuditResourceType`, `AuditRepository`, `AuditService`, and `AuditQueryService`.
  - Audit rows for URL create, destination change, expiration change, enable, disable, delete, admin block/unblock, workspace creation, member add, member role change, and member removal.
  - Read-only audit APIs for workspace audit, URL audit, and explicit platform admin audit.
  - Same-transaction audit insert behavior for business mutations where practical; audit persistence failure fails the mutation.
  - Bounded safe metadata using `APP_AUDIT_METADATA_MAX_BYTES`; destination changes store hosts and SHA-256 hashes rather than raw URLs.
  - No audit events for reads or public redirects.
- Defects found and corrected:
  - Audit controller parameters initially relied on compiler parameter metadata; fixed with explicit `@PathVariable` and `@RequestParam` names.
  - Nullable JPQL filters caused PostgreSQL `could not determine data type of parameter` errors; replaced fixed nullable queries with dynamic JPA specifications.
- Focused validation:
  - `.\mvnw.cmd -q "-Dtest=com.example.urlshortener.audit.AuditTrailIntegrationTests,com.example.urlshortener.audit.AuditServiceTests" test` passed.
  - PostgreSQL Testcontainers started, Flyway validated and applied V1-V7, and Hibernate schema validation succeeded.
- Full validation:
  - `.\mvnw.cmd clean verify` passed with 97 tests and JaCoCo line coverage 85.62% / branch coverage 62.80%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1-V7.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed with no new production dependency.
  - `docker compose config` passed without warnings.

## P-024 AI Output Examples

### Accepted

The AI-generated append-only audit table with UUID identifiers, workspace/actor/resource logical IDs, action/resource enums, correlation ID, schema version, and JSONB metadata was accepted because it supports queryability while preserving audit retention without cascading foreign keys.

### Edited

The initial audit query repository used nullable JPQL filters. It was edited to dynamic JPA specifications after PostgreSQL rejected ambiguous null timestamp parameters in Testcontainers.

### Rejected

Persisting raw destination URLs in audit metadata was rejected. Destination-change audit metadata stores only hostnames and SHA-256 hashes so sensitive query parameters are not exposed through audit APIs.

## AI was wrong example

### Prompt

> Generate Testcontainers dependencies for Redis.

### AI Output

Suggested:

```text
org.testcontainers:redis
```

### Human Review

This dependency does not exist in Maven Central as a standalone artifact.

### Action

Rejected.

Replaced with:

```text
org.testcontainers:testcontainers
GenericContainer<>("redis:7-alpine")
```

### Validation

```bash
./mvnw clean verify
```

Passed.

## P-025 Stage 6 API Keys / Machine-To-Machine Authentication

- Scope: Stage 6 API keys only.
- Explicitly not started: transactional outbox, tracing, QR codes, frontend, distributed gateway/infrastructure, and Stage 7 work.
- Approved migration: added `V8__api_keys.sql`; V1-V7 were not modified.
- Implemented:
  - Workspace-bound API-key table with non-cascading workspace/user references, unique prefix/digest constraints, expiration, revocation, last-used tracking, and optimistic versioning.
  - API-key management endpoints for human workspace `OWNER`/`ADMIN` actors.
  - Header-only API-key authentication through `X-API-Key`.
  - One-time raw key return and HMAC-SHA-256 digest storage using `APP_API_KEY_HASH_PEPPER`.
  - Machine scopes `links:read`, `links:write`, and `analytics:read` with workspace isolation.
  - API-key rate limiter policy and generic RFC7807 401/429 behavior.
  - API-key create/revoke audit events and API-key actor attribution for machine URL mutations.
- Defects found and corrected:
  - Raw key parsing was adjusted to avoid ambiguity when Base64URL secrets contain underscores.
  - The API-key filter was disabled for servlet auto-registration and kept only in the Spring Security chain.
  - Rate-limit exceptions thrown from the filter path were handled directly as RFC7807 429 responses because MVC exception advice does not handle pre-controller filter failures.
- Focused validation:
  - `.\mvnw.cmd -q -Dtest=ApiKeyIntegrationTests test` passed.
  - PostgreSQL Testcontainers started, Flyway validated and applied V1-V8, and Hibernate schema validation succeeded.
- Full validation:
  - `.\mvnw.cmd clean verify` passed with 105 tests and JaCoCo line coverage 87.09% / branch coverage 63.87%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1-V8.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed with no new production dependency.
  - `docker compose config` passed without warnings.

## P-025 AI Output Examples

### Accepted

The HMAC-backed API-key storage model was accepted because it avoids storing raw bearer credentials while still allowing deterministic verification by prefix lookup and constant-time digest comparison.

### Edited

The generated key format and parser were edited so underscores inside the Base64URL secret do not corrupt parsing. The filter integration was also edited to avoid duplicate servlet registration.

### Rejected

Granting API keys broad `ROLE_ADMIN` or workspace-management permissions was rejected. Machine credentials are restricted to explicit URL and analytics scopes in one workspace.

## P-026 Stage 7 Transactional Outbox And Durable Event Delivery

- Scope: Stage 7 transactional outbox only.
- Explicitly not started: tracing, QR codes, frontend, external broker infrastructure, distributed gateway, and Stage 8 work.
- Approved migration: `V9__outbox_events.sql`; V1-V8 were not modified.
- Implemented:
  - PostgreSQL `outbox_events` table with `PENDING`, `PROCESSING`, `PROCESSED`, and `DEAD` states.
  - Same-transaction `DomainEventPublisher` implementation backed by PostgreSQL.
  - URL mutation events for create, destination change, expiration change, enable, disable, delete, block, and unblock.
  - Durable cache invalidation events while retaining immediate after-commit Redis invalidation.
  - Bounded dispatcher using PostgreSQL `FOR UPDATE SKIP LOCKED`, claim timeout recovery, exponential backoff with jitter, max attempts, and dead-letter state.
  - Handler abstraction for cache invalidation, analytics outbox events, and URL mutation event delivery placeholders.
  - `APP_ANALYTICS_PUBLISHER=local|outbox`, defaulting to `local` in all profiles.
  - Read-only platform-admin outbox inspection endpoint with safe DTOs and no raw payload exposure.
  - Low-cardinality outbox metrics and processed-row cleanup.
- Defects found and corrected:
  - Outbox integration tests initially inserted timestamps using the machine-local clock while the application clock is UTC, making test rows appear scheduled in the future. The test helper was corrected to use the application `Clock`.
  - Admin outbox query parameters were made explicit because this Maven build does not compile Java parameter names for Spring MVC inference.
- Focused validation:
  - `.\mvnw.cmd -q "-Dtest=OutboxIntegrationTests,OutboxRollbackIntegrationTests" test` passed.
  - PostgreSQL Testcontainers started, Flyway validated and applied V1-V9, and Hibernate schema validation succeeded.
- Full validation:
  - `.\mvnw.cmd clean verify` passed with 113 tests and JaCoCo line coverage 85.15% / branch coverage 60.53%.
  - PostgreSQL and Redis Testcontainers started successfully.
  - Flyway validated and applied V1-V9.
  - Hibernate schema validation succeeded.
  - `.\mvnw.cmd dependency:tree` passed with no new production dependency.
  - `docker compose config` passed without warnings.

## P-026 AI Output Examples

### Accepted

The PostgreSQL transactional outbox design using `FOR UPDATE SKIP LOCKED`, bounded workers, at-least-once delivery, and handler idempotency was accepted because it meets the durability requirement without adding unapproved infrastructure.

### Edited

The analytics publisher design was edited to keep `APP_ANALYTICS_PUBLISHER=local` as the default in every profile. Outbox-backed analytics remains opt-in because it adds a PostgreSQL write to the redirect path and is not the hyperscale click-stream target.

### Rejected

Adding a public retry endpoint for dead-lettered outbox rows was rejected. Stage 7 exposes only read-only platform-admin inspection; operational repair remains a documented manual procedure.
