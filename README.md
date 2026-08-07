# AI-Assisted URL Shortener Assessment

This repository captures a phased, AI-assisted URL shortener assessment built with Java 21, Spring Boot 3.5, PostgreSQL, Flyway, JPA, Testcontainers, and OpenAPI.

## Purpose

This assessment is designed to demonstrate disciplined AI-assisted engineering execution rather than fully automated code generation. The repository will show:

- Requirement understanding and ambiguity resolution
- Task decomposition into phased, reviewable work
- AI usage in implementation, debugging, testing, documentation, and review preparation
- Traceability of generated, edited, and rejected AI outputs
- Human approval for high-impact decisions
- Architecture, security, and validation artifacts

## Current status

Phase 5 operational readiness validation is complete:

- Requirement normalization
- Non-functional requirements
- Architecture overview and ADRs
- Three documented scenarios: greenfield, brownfield, ambiguous
- AI-assisted execution plan, prompt log, traceability matrix, and approval log
- Test strategy
- Core URL, redirect, repository, DTO validation, Flyway, PostgreSQL Testcontainers, pagination, collision, RFC7807, and OpenAPI tests
- RS256 JWT access tokens, BCrypt password hashing, opaque refresh-token rotation, CSRF-protected refresh/logout, explicit CORS allowlist, deny-by-default security, and owner-aware URL operations through `CurrentOwnerProvider`
- Production URL APIs remain free of owner-input request parameters
- Redis cache-aside redirect resolution with bounded TTLs, jitter, safe fallback to PostgreSQL, after-commit invalidation, and in-process single-flight protection
- Best-effort async click analytics with bounded queueing, sanitized IP/referrer/user-agent data, PostgreSQL event persistence, atomic aggregate updates, and owner/admin analytics APIs
- Spring Boot Actuator health/liveness/readiness and ADMIN-protected metrics exposure
- Micrometer application metrics for URL creation, redirects, Redis cache/single-flight, analytics, authentication, and rate limiting
- Redis Lua fixed-window rate limiting with configuration-driven fail-open/fail-closed policies
- Centralized `X-Correlation-ID` validation, MDC propagation, response headers, and Problem Details correlation
- Security headers, production analytics pepper validation, bounded request/resource configuration, k6 performance scripts, and operations/security documentation

## Next step

Phase 5 is ready for review. Phase 6/final release work remains future scope until Phase 5 is reviewed and approved.

## Project structure

Key documentation added under `docs/`:

- `docs/requirements`
- `docs/architecture`
- `docs/scenarios`
- `docs/ai-assisted-engineering`
- `docs/testing`

## How to review

Read `AGENTS.md` first. Then review the requirements documents, ADRs, and execution plan before authorizing implementation.

## Validation

Phase 5 validation commands:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

`clean verify` passed with 72 tests. PostgreSQL and Redis Testcontainers started successfully. Flyway validated and applied `V1__initial_schema.sql`, `V2__authentication_refresh_tokens.sql`, and `V3__click_analytics_indexes.sql`; Hibernate schema validation succeeded. `dependency:tree` confirmed compatible Spring Boot-managed Flyway, Actuator, Micrometer, Redis, and Testcontainers versions with no added rate-limiting library. `docker compose config` passed with only the existing obsolete `version` warning.

Production/local JWT keys must be supplied through `APP_AUTH_PRIVATE_KEY_PEM` and `APP_AUTH_PUBLIC_KEY_PEM`. The `test` profile generates ephemeral RSA keys only for reproducible tests.
Production/local analytics IP hashing should set `APP_ANALYTICS_IP_HASH_PEPPER`; raw IP addresses, full user agents, full referrers, cookies, headers, tokens, and owner data are not stored in click events or Redis cache values.
