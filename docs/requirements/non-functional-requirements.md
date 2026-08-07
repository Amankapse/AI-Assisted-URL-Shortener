# Non-functional Requirements

## Security

- Enforce authentication and authorization for all non-public endpoints.
- Validate JWT signature, issuer, audience, and expiration.
- Protect sensitive data with BCrypt or Argon2 password hashing.
- Reject malformed URLs, only allow `http` and `https` schemes.
- Prevent reserved alias conflicts with application routes.
- Keep tokens and secrets out of logs.
- Use secure HTTP headers and a restrictive CORS policy.

## Reliability

- Application instances are stateless.
- Redis may fail without preventing redirects; PostgreSQL must remain authoritative.
- Short-code resolution uses cache-aside with fallback.
- Health and readiness probes are available.
- Pagination prevents unbounded list results.

## Performance

- Use Redis for hot-path redirect lookups.
- Add database indexes for short code, user history, status, and analytics queries.
- Maintain bounded request payloads and reasonable default limits.
- Use connection pooling and timeouts.
- Avoid long-running transactions around external I/O.

## Maintainability

- Use feature package boundaries and clear module responsibilities.
- Keep controllers thin and services authoritative.
- Do not expose JPA entities through APIs.
- Use request/response DTOs.
- Maintain an ADR log for architecture decisions.
- Keep AI traceability documentation up to date.

## Testability

- Cover core business logic with unit tests.
- Use Testcontainers for PostgreSQL and Redis integration tests.
- Add security tests for authentication, authorization, and CSRF decisions.
- Include regression tests for changed behavior.

## Operability

- Provide setup instructions and a runbook.
- Document expected metrics and alerting targets.
- Include a plan for rollback and recovery.
