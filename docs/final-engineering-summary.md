# Final Engineering Summary

## Requirement Interpretation

The original URL-shortener requirement was normalized into phased, testable requirements: core URL management, secure ownership, authentication, PostgreSQL persistence, Redis redirect optimization, analytics, observability, rate limiting, operational readiness, and final release evidence. Ambiguous terms such as "production-ready" and "enterprise-ready" were translated into explicit concerns: security boundaries, failure modes, reproducible tests, migrations, metrics, runbooks, rollback guidance, and honest limitations.

## Architecture

The application uses a modular monolith because the assessment benefits from clear package boundaries without distributed-system overhead. The modules are organized around auth, user, URL, redirect, analytics, security, common infrastructure, and configuration.

PostgreSQL is the source of truth for users, workspaces, memberships, URLs, refresh-token digests, API-key digests, analytics, idempotency records, and audit events. Flyway owns schema evolution and Hibernate validates mappings against the migrated schema.

Redis is used only as an optimization for redirect cache-aside and distributed rate limiting. Redirect correctness remains PostgreSQL-backed, and Redis health does not make the service unready.

Spring Security was chosen for JWT validation, CSRF, CORS, authorization, and security headers. Access tokens are RS256 JWTs; refresh tokens are opaque, rotated, stored as SHA-256 digests, and protected by CSRF when transported by cookie. Workspace-bound API keys extend authentication for machine clients while keeping human session behavior unchanged.

Analytics are asynchronous and bounded so public redirect latency is protected. This deliberately trades perfect analytics completeness for service availability under pressure.

Hyperscale evolution was handled as incremental production hardening, not a rewrite. The implemented baseline now includes configurable 8-character Base62 short-code generation, bounded collision retries with low-cardinality metrics, config-driven URL quotas, V4 administrative blocking, and blocked-cache invalidation. Distributed URL storage, Redis Cluster, CDN/edge routing, WAF, durable event streams, analytical warehouses, and multi-region deployment are documented as future production architecture only.

## Greenfield Scenario

The greenfield work decomposed the shortener into domain entities, repositories, services, DTOs, validation, controllers, migrations, tests, and documentation. AI assistance was used to draft initial structures and tests, but output was reviewed against the modular-monolith rules, security constraints, and PostgreSQL/Flyway requirements.

Validation included repository integration tests, service tests, MockMvc controller tests, DTO validation tests, redirect tests, OpenAPI exposure checks, pagination checks, collision behavior, and RFC7807 errors.

## Brownfield Scenario

Several later phases enhanced the working foundation without redesigning it.

Redis caching added cache-aside redirect lookup, bounded TTLs, schema-versioned cache values, PostgreSQL fallback, and after-commit invalidation. Impacted components included redirect service, cache service, URL mutation operations, configuration, tests, and docs. Risks were cache poisoning, stale redirects, Redis failure, and stampede behavior; tests covered hit/miss/failure/invalidation paths.

Analytics added sanitized click events, queue bounds, retry behavior, owner analytics, admin analytics, and soft delete to preserve history. Risks included privacy leakage, queue saturation, write amplification, and foreign-key conflicts; tests covered sanitization, persistence, overload, authorization, and soft-delete behavior.

Phase 5 added observability, rate limiting, correlation IDs, health behavior, and security hardening without changing business behavior. Risks included high-cardinality metrics, excessive Actuator exposure, unsafe rate-limit failure modes, and readiness tied to Redis. Tests and documentation closed those risks.

The hyperscale evolution added short-code namespace headroom, collision metrics, quota checks, and moderation blocking while keeping the existing public user URL APIs backward compatible. Existing 7-character short codes still resolve; new generated codes default to 8 characters.

The enterprise audit evolution added append-only application-level audit events for URL, workspace, membership, and moderation mutations. Audit events are written synchronously in the same PostgreSQL transaction as the mutation where practical. Metadata is allowlisted and bounded; destination changes store host and SHA-256 hashes instead of raw URLs.

Stage 6 added machine-to-machine API keys as a brownfield security extension. The change added V8 `api_keys`, one-time raw key return, HMAC-SHA-256 digest storage, workspace-bound scopes, API-key rate limiting, API-key create/revoke audit events, and service-layer actor attribution without changing human JWT/refresh-token behavior.

Stage 7 added transactional outbox delivery as a brownfield durability extension. The change added V9 `outbox_events`, same-transaction URL mutation events, durable cache invalidation work, opt-in outbox analytics publishing, bounded PostgreSQL `SKIP LOCKED` dispatch, retry/dead-letter handling, low-cardinality metrics, and read-only platform-admin inspection without adding a broker or changing the redirect cache-aside architecture.

## Ambiguous Scenario

"Make it enterprise ready" was treated as an ambiguous request and converted into concrete acceptance criteria:

- deny-by-default security
- JWT and refresh-token validation
- CSRF rationale
- owner enforcement without client owner IDs
- machine credentials constrained to workspace scopes
- Redis as optional optimization
- bounded queues and timeouts
- rate limiting with explicit failure modes
- liveness/readiness policy
- metrics with bounded tags
- documented runbooks and rollback behavior
- reproducible Docker/Testcontainers validation
- performance scripts with no fabricated results

The implementation intentionally stopped short of claiming enterprise deployment features that are not present, such as multi-region operation, Kubernetes manifests, external secret-manager integration, or durable event streaming.

The 100M-new-URLs/day requirement was treated as an architecture target. The local prototype is not represented as meeting that volume; the documentation separates the current baseline from future distributed stores, edge routing, event streaming, and global infrastructure.

## AI-Assisted Engineering

AI was used to transform requirements into structured specs, propose implementation slices, draft code and tests, and prepare documentation. Human review accepted, edited, or rejected outputs based on correctness, security, maintainability, and scope control.

Accepted examples:

- Modular monolith package structure with clear service/repository/controller boundaries.
- Redis cache-aside redirect resolution with PostgreSQL fallback.
- Redis Lua fixed-window rate limiting using the existing `StringRedisTemplate`.

Edited examples:

- Ownership evolved from a Phase 2 placeholder provider to `SecurityCurrentOwnerProvider` backed by JWT subject while preserving the `CurrentOwnerProvider` abstraction.
- Flyway validation was corrected by adding `flyway-database-postgresql` after identifying that PostgreSQL support is separated from Flyway core.
- URL creation metrics were adjusted to avoid double-counting custom alias conflicts.
- Hyperscale output was edited to keep only safe local changes in code while documenting CDN, WAF, Kafka/Kinesis/Pulsar, Redis Cluster, and distributed KV stores as future architecture.
- Audit query implementation was edited from nullable JPQL parameters to dynamic JPA specifications after PostgreSQL rejected ambiguous null timestamp parameters during Testcontainers validation.
- API-key output was edited to avoid delimiter ambiguity in generated key parsing, to disable servlet auto-registration of the security filter, and to return RFC7807 429 responses from the filter path when the API-key limiter rejects.
- Outbox test output was edited to use the application UTC `Clock` rather than machine-local time so dispatcher scheduling semantics are tested accurately.

Rejected examples:

- Invalid `org.testcontainers:redis` dependency; replaced with `GenericContainer<>("redis:7-alpine")`.
- A custom JWT filter approach; replaced with Spring Security resource server support and `NimbusJwtDecoder`.
- Redis readiness dependency; rejected because Redis is not required for correctness and would cause bad orchestration behavior.
- Switching to MD5 truncation, sequential public IDs, or Hashids as a security mechanism was rejected for short-code generation.

Traceability is maintained in `docs/ai-assisted-engineering/`.

## Engineering Judgment

The implementation chose simpler, safer approaches where appropriate:

- Modular monolith instead of microservices to avoid unnecessary distributed complexity.
- No Kafka or durable broker in the prototype; analytics are best-effort and bounded.
- Transactional outbox gives durable database-backed delivery for control-plane events, but it is not represented as an external event-streaming platform.
- PostgreSQL is authoritative; Redis failures degrade performance, not correctness.
- Single-flight is in-process and documented rather than pretending to solve cross-node stampedes.
- Security is deny-by-default, with explicit public endpoints and explicit admin endpoints.
- Metrics avoid user IDs, URL IDs, short codes, emails, IP addresses, token IDs, and exception messages as tags.
- Audit metadata avoids passwords, tokens, cookies, raw destination URLs, raw IP addresses, and unbounded JSON; audit rows are append-only through application behavior.
- API-key storage uses one-way HMAC digests and never stores raw keys; API keys do not receive `ROLE_ADMIN` and cannot manage workspaces or other API keys.
- Outbox payloads are bounded DTOs, not JPA entities or secrets; admin inspection omits raw payload JSON.
- The shortcode default moved to 8-character random Base62 rather than sequential public IDs, MD5 truncation, or Hashids-as-security.
- Moderation is represented as `blocked` alongside existing enabled/deleted/expiration state rather than replacing lifecycle with a broad enum migration.

## Validation

Final release validation includes:

- `.\mvnw.cmd clean verify`
- `.\mvnw.cmd dependency:tree`
- `docker compose config`
- PostgreSQL Testcontainers
- Redis Testcontainers
- Flyway V1 through V9 validation and application
- Hibernate schema validation
- JaCoCo coverage report generation
- GitHub Actions workflow definition
- k6 scripts for reproducible performance smoke testing

Final local results:

- Tests: 113 passing
- Line coverage: 85.15%
- Branch coverage: 60.53%
- Docker: PostgreSQL and Redis Testcontainers started successfully
- Migrations: Flyway V1 through V9 validated and applied
- Schema: Hibernate validation succeeded
- Compose: `docker compose config` passed without warnings

The local environment did not have k6 installed, so no measured performance results are claimed. The GitHub Actions workflow is defined but must be verified remotely after the branch is pushed.

## Risks And Trade-Offs

Known limitations:

- Analytics are best-effort and can drop events under overload.
- No durable event broker.
- Transactional outbox is at-least-once and database-backed; it is not an exactly-once broker.
- No distributed single-flight.
- No external secret manager integration.
- No Kubernetes, autoscaling, or multi-AZ deployment manifests.
- Registration duplicate behavior can reveal that an email already exists.
- CI workflow must be verified remotely after the branch is pushed.
- Local load testing was not executed because k6 was unavailable.
- Audit immutability is application-level only; cryptographic chaining and WORM storage are not implemented.
- API keys are bearer credentials; stolen raw keys remain usable until expiration or revocation, and pepper rotation requires coordinated key reissue.
- The local implementation does not include CDN/edge routing, WAF, Redis Cluster, distributed URL storage, Kafka/Kinesis/Pulsar, OLAP analytics warehouse, or multi-region infrastructure.

## Production Evolution

At enterprise scale, the next evolution would include:

- managed PostgreSQL with backups, PITR, replicas, and migration runbooks
- managed Redis with authentication, TLS, monitoring, and eviction policies
- distributed URL mapping store for hyperscale short-code lookups
- CDN/edge routing, WAF, and global load balancing for the redirect path
- external secret manager for RSA keys, database credentials, Redis credentials, analytics pepper, and rate-limit salt
- centralized logs, metrics, traces, dashboards, and alerts
- durable event broker for analytics ingestion
- distributed stampede protection or lease/lock strategy
- Kubernetes or equivalent deployment manifests
- autoscaling, multi-AZ deployment, and infrastructure DDoS protection
- formal SAST/SCA/secret-scanning gates
- measured load tests in representative infrastructure
