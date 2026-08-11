# Human Approval Log

This log records explicit approvals for decisions that affect architecture, security, public API, dependencies, or database migrations.

## Phase 0–1
- No implementation changes made.
- Approval pending for Phase 1 foundation and initial design.

## Phase 2 Validation
- Approved adding `org.flywaydb:flyway-database-postgresql` as a production/runtime dependency managed by Spring Boot dependency management.
- Constraints: retained `flyway-core`; did not modify migrations; did not disable Flyway; did not replace PostgreSQL with H2; did not suppress the unsupported-database error; did not begin Phase 3 auth/JWT/Redis/rate limiting/observability work.
- Validation: `.\mvnw.cmd clean verify` and `.\mvnw.cmd dependency:tree` passed after the approved correction.
- Phase 2 validation continued with end-to-end tests. Defects found after Flyway startup were fixed as application/test defects, including PostgreSQL enum mapping, owner lookup semantics, malformed JSON RFC7807 handling, explicit controller binding names, pagination validation, and Springdoc compatibility for Spring Boot 3.5.
- No database migration was changed and no Phase 3 feature work was introduced.

## Phase 3 Authentication
- Approved adding only `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`.
- Approved using Spring Security resource server, `NimbusJwtDecoder`, RS256 JWT validation, and a custom JWT authentication converter instead of a custom bearer filter.
- Approved retaining `CurrentOwnerProvider` and replacing the placeholder provider with `SecurityCurrentOwnerProvider`; production URL APIs must remain free of owner-input request parameters.
- Approved adding only `V2__authentication_refresh_tokens.sql`; `V1__initial_schema.sql` was not modified.
- Approved opaque refresh tokens with SHA-256 digest storage, rotation, reuse detection, family revocation, CSRF-protected refresh/logout, and explicit CORS origins.
- Explicitly deferred Redis cache, rate limiting, observability expansion, and admin feature expansion.
- Validation: `.\mvnw.cmd clean verify` passed with 45 tests; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with only the existing obsolete-version warning.

## Phase 4 Redis Caching and Click Analytics
- Approved adding only `spring-boot-starter-data-redis`; direct `redis.clients:jedis` was not added.
- Approved Redis cache-aside using key format `url:v1:redirect:{shortCode}`, versioned redirect DTO values only, bounded TTLs, jitter, short ineligible TTL, no database-miss caching, Redis failure fallback to PostgreSQL, and after-commit invalidation.
- Approved bounded in-process single-flight miss protection with PostgreSQL fallback on capacity exhaustion or timeout.
- Approved best-effort async click analytics with sanitized IP/referrer/user-agent/correlation data, bounded queueing, drops on overload, retry handling, idempotent event inserts, and atomic SQL aggregate updates.
- Approved owner analytics endpoints and explicit `ROLE_ADMIN` analytics endpoints; normal URL APIs still enforce owner scope and do not allow admin bypass.
- Approved adding `V3__click_analytics_indexes.sql`; `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql` were not modified.
- Explicitly deferred rate limiting, broad observability expansion, retention jobs, and Phase 5 work.
- Validation: `.\mvnw.cmd clean verify` passed with 60 tests; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with only the existing obsolete-version warning.

## Phase 5 Operational Readiness
- Approved Phase 5 scope: observability, rate limiting, health/readiness, performance scripts, security hardening, runbooks, failure-mode validation, and final quality gates only.
- Approved Redis Lua rate limiting with the existing `StringRedisTemplate`; no new production dependency was authorized or added.
- Approved protected Actuator metrics exposure, public health/liveness/readiness probes, liveness excluding PostgreSQL/Redis, readiness requiring PostgreSQL and excluding Redis.
- Approved centralized correlation ID filter with strict safe input validation and MDC cleanup.
- Approved no Flyway migration for Phase 5 unless a genuine schema requirement was discovered; no migration was required.
- Validation: `.\mvnw.cmd clean verify` passed with 72 tests; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with only the existing obsolete-version warning. `scripts/verify.sh` was attempted but failed before Maven startup because Bash did not have `JAVA_HOME` configured.

## Phase 6 Final Release Readiness
- Approved final phase scope: release readiness, CI/CD, quality evidence, documentation, and submission preparation only.
- Approved adding JaCoCo Maven plugin as normal build-tooling evidence; no production dependency was added.
- Approved no new product features, no architecture redesign, no microservices, no Kafka, no new authentication mechanism, and no additional infrastructure.
- Validation: `.\mvnw.cmd clean verify` passed with 72 tests and generated JaCoCo coverage; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed without warnings; k6 was unavailable and not installed automatically.

## Final Documentation Synchronization
- Requested final documentation and README synchronization only.
- Constraints: no new features, no architecture/security/test redesign, no dependency changes, no migration changes, and no generated-code churn unless required to correct documentation-blocking drift.
- Approved scope was applied to README, documentation index, stale documentation language, AI traceability logs, `.gitignore` local key hygiene, and the k6 script request-body correction needed to match the current API DTO.
- Validation: Markdown links were checked; local Docker Compose PostgreSQL and Redis infrastructure was started and reported healthy; `docker compose config` passed without warnings; `.\mvnw.cmd clean verify` passed with 72 tests and generated JaCoCo coverage.

## Hyperscale Production Evolution
- Approved hyperscale documentation for NFRs, capacity, storage, analytics, global redirect, SLOs, lifecycle, and cost model.
- Approved config-driven short-code generation with `shortener.code.length=8` and `shortener.code.max-retries=5`; cryptographically secure Base62 generation retained.
- Approved collision retry/exhaustion metrics with no high-cardinality tags.
- Approved config-driven URL quotas without a persisted quota/billing table.
- Approved Flyway V4 using `blocked BOOLEAN NOT NULL DEFAULT FALSE`; V1-V3 were not modified.
- Approved explicit `ROLE_ADMIN` block/unblock operations under admin URL moderation endpoints.
- Explicitly not approved/implemented: Cassandra, DynamoDB, ScyllaDB, Bigtable, Kafka, Kinesis, Pulsar, Redis Cluster, CDN/edge, WAF, global load balancer, multi-region infrastructure, OLAP warehouse, or external malware provider.
- Validation: `.\mvnw.cmd clean verify` passed with 84 tests and JaCoCo line 85.52% / branch 66.49%; PostgreSQL and Redis Testcontainers started; Flyway V1-V4 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed without warnings.

## Render Live Deployment Configuration

- Requested deployment/configuration changes only for Render Web Service, Neon PostgreSQL, and Render Key Value / Valkey.
- Constraints: no architecture redesign, no business logic change, no authentication behavior change, no Flyway migration change, no PostgreSQL/Redis abstraction replacement, no secret commits, and no new production dependency unless a concrete blocker required approval.
- Approved implementation path: production profile configuration, Render `PORT` support, environment-driven Neon/Redis/JWT/CORS/privacy settings, Docker runtime JAR startup, `.env.example` deployment template, `.gitignore`/`.dockerignore` secret hygiene, README link, and deployment documentation.
- Render deploy blocker correction: deployment logs showed `no main manifest attribute` because the Maven package artifact was not repackaged as an executable Spring Boot JAR. Approved correction was binding `spring-boot:repackage` in the existing Spring Boot Maven plugin. No dependency, migration, auth, or business logic change was made.
- Validation: `.\mvnw.cmd clean verify` passed with 84 tests and JaCoCo line 84.44% / branch 65.41%; PostgreSQL and Redis Testcontainers started; Flyway V1-V4 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed; Markdown link check passed for 54 files; `docker compose up -d` plus bounded local `spring-boot:run` liveness smoke passed; Docker image build `url-shortener-render-smoke` passed.

## Enterprise Production Evolution Stages 1-3

- Approved implementation start for Stages 1-3 only: public URL representation, idempotent URL creation, and destination editing with optimistic concurrency.
- Approved migration: `V5__idempotency_keys.sql`. V1-V4 must not be modified.
- Approved public API changes: additive `shortUrl` response field, optional `Idempotency-Key` for URL creation, `ETag` on URL management responses, and `PATCH /api/v1/urls/{id}/destination` requiring `If-Match`.
- Explicitly deferred until after review: workspace migration, immutable audit trail, API keys, outbox, analytics publisher abstraction, cache invalidation durability, mapping-store abstraction, redirect coordination abstraction, tags/campaigns/search, QR codes, tracing, and Angular frontend.
- Validation: `.\mvnw.cmd clean verify` passed with 89 tests and JaCoCo line 85.83% / branch 65.58%; PostgreSQL and Redis Testcontainers started; Flyway V1-V5 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed without warnings.

## Stage 4 Workspace/Tenant Foundation and Workspace RBAC

- Approved implementation scope: workspace tenant foundation and workspace RBAC only.
- Approved migration: `V6__workspaces_and_memberships.sql`; V1-V5 were not modified.
- Approved API surface: workspace endpoints under `/api/v1/workspaces` and optional `X-Workspace-ID` header for existing URL/analytics management APIs.
- Approved role model: workspace `OWNER`, `ADMIN`, `EDITOR`, `ANALYST`, and `VIEWER`; platform `ROLE_ADMIN` remains separate and is not workspace admin.
- Explicitly deferred: audit table/hooks implementation, API keys, outbox, analytics publisher abstraction, tags/campaigns/search, QR codes, tracing, Angular frontend, and Stage 5 work.
- Validation: `.\mvnw.cmd -q -Dtest=WorkspaceControllerIntegrationTests test` passed; `.\mvnw.cmd clean verify` passed with 92 tests and JaCoCo line 84.48% / branch 63.42%; PostgreSQL and Redis Testcontainers started; Flyway V1-V6 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed.

## Stage 5 Immutable Enterprise Audit Trail

- Approved implementation scope: immutable enterprise audit trail only.
- Approved migration: `V7__audit_events.sql`; V1-V6 were not modified.
- Approved API surface: read-only audit endpoints for workspace audit, URL audit, and explicit platform admin audit.
- Approved immutability boundary: application-level append-only behavior with no update/delete audit APIs; no cryptographic chaining or WORM storage claim.
- Approved metadata policy: allowlisted bounded JSONB metadata, `APP_AUDIT_METADATA_MAX_BYTES`, no sensitive values, no raw destination URLs, and same-transaction audit insert where practical.
- Explicitly deferred: API keys, transactional outbox, tags/campaigns/search, QR codes, tracing, Angular frontend, and distributed infrastructure.
- Validation: `.\mvnw.cmd clean verify` passed with 97 tests and JaCoCo line 85.62% / branch 62.80%; PostgreSQL and Redis Testcontainers started; Flyway V1-V7 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed without warnings.

## Stage 6 API Keys / Machine-To-Machine Authentication

- Approved implementation scope: API keys / machine-to-machine authentication only.
- Approved migration: `V8__api_keys.sql`; V1-V7 were not modified.
- Approved public API surface: workspace API-key create, list, and revoke endpoints under `/api/v1/workspaces/{workspaceId}/api-keys`.
- Approved credential model: header-only `X-API-Key`, one-time raw key return, HMAC-SHA-256 digest storage using `APP_API_KEY_HASH_PEPPER`, expiration, revocation, and workspace-bound scopes.
- Approved scope model: `links:read`, `links:write`, and `analytics:read`; no platform admin authority and no workspace/key management by API keys.
- Explicitly deferred: transactional outbox, tracing, QR codes, frontend, distributed API gateway, external secret manager implementation, and Stage 7 work.
- Validation: focused `.\mvnw.cmd -q -Dtest=ApiKeyIntegrationTests test` passed; `.\mvnw.cmd clean verify` passed with 105 tests and JaCoCo line 87.09% / branch 63.87%; PostgreSQL and Redis Testcontainers started; Flyway V1-V8 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed without warnings.

## Stage 7 Transactional Outbox And Durable Event Delivery

- Approved implementation scope: transactional outbox and durable event delivery only.
- Approved migration: `V9__outbox_events.sql`; V1-V8 were not modified.
- Approved no new production dependencies.
- Approved architecture: PostgreSQL transactional outbox, `DomainEventPublisher`, bounded dispatcher using `FOR UPDATE SKIP LOCKED`, at-least-once delivery, retry/dead-letter behavior, durable cache invalidation handler, optional outbox analytics publisher, and read-only platform-admin outbox inspection.
- Approved analytics default: `APP_ANALYTICS_PUBLISHER=local` in all profiles; `outbox` is opt-in and documented as not the hyperscale click-stream target.
- Explicitly deferred: tracing, QR codes, frontend, external broker infrastructure, distributed API gateway, and Stage 8 work.
- Focused validation: `.\mvnw.cmd -q "-Dtest=OutboxIntegrationTests,OutboxRollbackIntegrationTests" test` passed; PostgreSQL Testcontainers started; Flyway V1-V9 validated and applied; Hibernate schema validation succeeded.
- Full validation: `.\mvnw.cmd clean verify` passed with 113 tests and JaCoCo line 85.15% / branch 60.53%; PostgreSQL and Redis Testcontainers started; Flyway V1-V9 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed with no new production dependency; `docker compose config` passed without warnings.

## Stage 8 Campaigns, Tags, Search, Filtering, And Large-Workspace UX

- Approved implementation scope: Stage 8 campaigns, tags, search, filtering, and large-workspace UX only.
- Approved migration: `V10__campaigns_tags_and_search.sql`; V1-V9 were not modified.
- Approved public API surface: campaign endpoints under `/api/v1/workspaces/{workspaceId}/campaigns`, tag list endpoint under `/api/v1/workspaces/{workspaceId}/tags`, additive URL create `campaignId`/`tags`, URL metadata endpoints `PATCH /api/v1/urls/{id}/campaign` and `PUT /api/v1/urls/{id}/tags`, and optional URL list/search query parameters.
- Approved authorization: campaign/tag reads for workspace readers and scoped API keys; campaign lifecycle management human-only; URL campaign/tag assignment through existing link-write authorization.
- Approved search constraints: PostgreSQL-backed bounded exact/prefix search, allowlisted sort fields, offset paging, no raw destination-query/email/user/API-key/audit/outbox/analytics search, no pg_trgm/OpenSearch/new production dependency.
- Explicitly deferred: Angular/frontend, QR codes, tracing, search-specific caches, distributed search infrastructure, separate search limiter, and Stage 9 work.
- Validation: `docker info` passed; focused `.\mvnw.cmd -q -Dtest=OrganizationSearchIntegrationTests test` passed; `.\mvnw.cmd clean verify` passed with 118 backend tests and JaCoCo line 85.11% / branch 60.78%; PostgreSQL and Redis Testcontainers started; Flyway V1-V10 validated and applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with a non-fatal local Docker config access warning in this shell.
- Corrections after Flyway startup: stale `idempotency_records` cleanup corrected to `idempotency_keys`; explicit Stage 8 controller `@PathVariable` names added; missing `PUT /api/v1/urls/**` security authorization added; idempotency normalization test made deterministic; campaignless search response mapping fixed.

## Stage 9A Angular Production Frontend Foundation

- Approved implementation scope: Stage 9A foundation only.
- Approved dependencies/tooling: Angular 22 baseline generated dependencies, TypeScript strict mode, Angular Router, HttpClient, Reactive Forms, Signals, functional interceptors/guards, and Angular CLI-generated build/test tooling.
- Explicitly not approved: Angular Material, PrimeNG, Bootstrap, Tailwind, NgRx, chart libraries, OpenAPI generator, Playwright, Cypress, frontend monitoring SDK, backend feature changes, backend auth redesign, or Stage 9B/9C/9D work.
- Approved frontend/backend independence: Angular builds as a separate static artifact and is not copied into Spring Boot resources.
- Approved auth model: memory-only access token, backend HttpOnly refresh cookie, startup refresh attempt, single-flight refresh on concurrent JWT 401s, no refresh for login/register/refresh/logout/API-key errors.
- Approved CSRF model: preserve backend CSRF; frontend uses `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header for refresh/logout. Stage 9A documents current liveness-based CSRF bootstrap as an existing backend behavior, not a new API contract.
- Validation: `npm ci` passed; `npm test -- --watch=false` passed with 7 Angular unit tests; `npm run build` passed with initial bundle 264.00 kB raw / 71.95 kB estimated transfer; backend regression `.\mvnw.cmd clean verify` passed with 118 tests and JaCoCo line 85.11% / branch 60.78%; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with a non-fatal local Docker config access warning.
- Remaining warning: `npm audit --audit-level=moderate` reports 3 moderate vulnerabilities in the Angular CLI dev-dependency chain through `@modelcontextprotocol/sdk` and `@hono/node-server`. The suggested `npm audit fix --force` would install `@angular/cli@21.0.4`, which conflicts with the approved Angular 22 requirement, so it was rejected pending upstream Angular CLI remediation.

## Stage 9B Production Link Management Experience

- Approved implementation scope: Stage 9B production link-management experience only.
- Approved frontend scope: `/app/urls`, `/app/urls/new`, `/app/urls/:id`, `/app/campaigns`, URL dashboard, create flow, details editing, campaign management, filters, sorting, pagination, workspace switching, and existing RFC7807 handling.
- Approved API use: existing backend contracts only, including `Idempotency-Key`, `ETag`, `If-Match`, URL campaign/tag metadata endpoints, campaign endpoints, tag list endpoint, and existing enable/disable/delete behavior.
- Explicitly not approved: analytics UI, audit UI, API-key UI, workspace-member management UI, platform-admin UI, QR codes, custom domains, tracing, backend architecture changes, backend migrations, backend auth changes, backend API contract changes, Stage 9C, new UI libraries, OpenAPI generator, Playwright, Cypress, NgRx, chart libraries, and frontend monitoring SDKs.
- Validation: `npm ci` passed; `npm test -- --watch=false` passed with 13 Angular tests; `npm run build` passed with initial bundle 100.65 kB raw / 26.07 kB estimated transfer; backend regression `.\mvnw.cmd clean verify` passed with 118 tests and JaCoCo line 85.11% / branch 60.78%; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed.
- Remaining warning: `npm ci` continues to report 3 moderate Angular CLI dev-dependency-chain vulnerabilities and pending install-script review notices. `npm audit fix --force` remains rejected because it would downgrade the approved Angular 22 CLI baseline.

## Stage 9C Enterprise Operations Frontend

- Approved implementation scope: Stage 9C enterprise operations frontend only on top of Stage 9A/9B.
- Approved API usage: existing backend contracts for URL analytics, workspace audit, URL audit, API keys, workspace/member management, platform analytics, moderation, platform audit, and read-only outbox.
- Explicitly not approved: backend migrations, backend dependencies, backend API contracts, Stage 9D deployment/hardening, QR codes, custom domains, tracing UI, chart libraries, NgRx, Material/PrimeNG/Bootstrap/Tailwind, OpenAPI generator, Playwright, Cypress, telemetry SDKs, API-key update/rotate APIs, outbox retry/replay/delete UI, fabricated analytics fields, and treating workspace OWNER as platform ADMIN.
- Security decisions: raw API keys are shown once in transient component state and cleared on acknowledgment; audit metadata is allowlisted and rendered as text; platform navigation requires authenticated user role `ADMIN`.
- Validation: `npm ci` passed with known Angular CLI dev-chain warnings; `npm test -- --watch=false` passed with 21 Angular tests; `npm run build` passed with initial bundle 103.64 kB raw / 26.71 kB estimated transfer and Stage 9C feature chunks lazy-loaded; `npm audit --audit-level=moderate` reported the known 3 moderate Angular CLI dev-chain findings; `.\mvnw.cmd clean verify` passed with 118 backend tests and JaCoCo line 85.11% / branch 60.78%; PostgreSQL and Redis Testcontainers started; Flyway V1-V10 validated/applied; Hibernate schema validation succeeded; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed.

## Stage 9D Production Deployment Hardening And Final Evidence

- Approved implementation scope: Stage 9D deployment hardening, release readiness, and final evidence only.
- Approved frontend deployment direction: keep Angular independently deployable as a Render Static Site/CDN artifact; do not merge it into the Spring Boot JAR.
- Approved public runtime configuration: generate browser-visible `app-config.json` from Render Static Site environment variables containing only public API origin, public short URL base, and environment label.
- Approved CI policy: continue running frontend install/test/build and add a high-severity audit gate; keep known moderate Angular CLI development-chain findings documented rather than forcing a destabilizing downgrade.
- Explicitly not approved: backend migrations, backend business-logic changes, backend auth redesign, SameSite/CSRF weakening, QR codes, custom domains, tracing UI, Kafka/Kinesis/Pulsar, distributed KV implementation, Redis Cluster code, Kubernetes manifests, billing, malware integration, and new analytics dimensions.
- Validation: `.\mvnw.cmd clean verify` passed with 118 backend tests and JaCoCo line 85.11% / branch 60.78%; PostgreSQL and Redis Testcontainers started; Flyway V1-V10 validated/applied; Hibernate schema validation succeeded. `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed; `npm ci`, `npm test -- --watch=false` with 21 tests, `npm run build`, and `npm run build:render` passed. `npm audit --audit-level=moderate` reported the known 3 moderate Angular CLI dev-chain findings, while `npm audit --audit-level=high` passed. `git diff --check` passed; repository and compiled frontend secret scans found no committed secret values. k6 was unavailable locally, and live frontend cookie/CSRF/reload smoke validation remains pending until the frontend Render Static Site URL is available.
- Follow-up CI correction: GitHub Actions exposed a brittle total-audit-row assertion in `OutboxRollbackIntegrationTests`. Production code was unchanged; the test now cleans before execution and asserts the failed URL mutation rolls back `short_urls` and `URL_CREATED` audit output while preserving setup audit rows. Focused rollback test and full `.\mvnw.cmd clean verify` passed afterward.
