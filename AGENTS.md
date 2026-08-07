## Mission

Build a production-ready, AI-assisted URL shortener assessment using Java 21 and Spring Boot 3.5.

This is an engineer-led AI-assisted development project. AI may plan, propose, implement, test, review, and document work, but the human engineer owns architecture, correctness, security, maintainability, and release approval.

## Working Method

For every non-trivial change:

1. Read the relevant requirements and existing code.
2. Identify assumptions and ambiguities.
3. Propose a concise implementation plan.
4. List files expected to change.
5. Identify security, data, compatibility, and performance risks.
6. Wait for human approval before:
   - adding production dependencies;
   - changing authentication or authorization;
   - creating or modifying database migrations;
   - changing public API contracts;
   - running destructive commands.
7. Implement the smallest coherent change.
8. Run required validation commands.
9. Review the resulting diff.
10. Update relevant documentation and the AI traceability log.

Do not implement unrelated improvements.

## Technology

- Java 21
- Spring Boot 3.5.x
- Maven
- Spring MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- JUnit 5
- Mockito
- Testcontainers
- Docker Compose
- GitHub Actions
- Spring Boot Actuator
- Micrometer
- OpenAPI

Do not add a production dependency without explicit approval.

## Architecture

Use a modular monolith with clear feature packages:

- auth
- user
- url
- redirect
- analytics
- admin
- security
- common
- config

Rules:

- Controllers handle HTTP concerns only.
- Business logic belongs in application services.
- Persistence belongs in repositories.
- Do not expose JPA entities through APIs.
- Use request and response DTOs.
- Keep module dependencies explicit.
- Avoid circular dependencies.
- Prefer constructor injection.
- Keep transactions short.
- Do not perform remote or slow operations inside a database transaction.
- PostgreSQL is the source of truth.
- Redis is an optimization and must not be required for correctness.

## Security

- Deny access by default.
- Use roles USER and ADMIN.
- Enforce ownership in the service and repository layers.
- Never trust a client-supplied user ID for ownership.
- Validate JWT signature, issuer, audience, and expiration.
- Hash passwords using BCrypt or Argon2.
- Never log passwords, tokens, cookies, secrets or personal information.
- Accept only HTTP and HTTPS destination URLs.
- Validate custom aliases against reserved routes and safe-character rules.
- Use parameterized persistence APIs.
- Return safe error responses without stack traces.
- Document the CSRF decision based on the credential transport model.
- Apply rate limits to authentication and URL-creation endpoints.
- Use secure headers and explicit CORS allowlists.
- Do not use `permitAll()` except for explicitly documented public endpoints.

## Database

- Use Flyway for every schema change.
- Use UUID identifiers internally unless a strong reason is documented.
- Add a unique constraint to short codes and user email.
- Use optimistic locking where concurrent updates may occur.
- Avoid N+1 queries.
- Add pagination to list APIs.
- Add indexes only when tied to a documented query.
- Never delete or rename a production column in the same release that introduces its replacement.
- Review migration locking, rollback and backward compatibility.

## Caching

- Use cache-aside for short-code resolution.
- Include bounded TTLs.
- Ensure Redis failure degrades to PostgreSQL.
- Avoid caching authorization decisions.
- Prevent cache stampede for hot keys.
- Invalidate cache only after a successful database commit.

## Testing

Every feature must include meaningful tests.

Required where applicable:

- Unit tests.
- Controller/security tests.
- PostgreSQL integration tests using Testcontainers.
- Redis integration tests using Testcontainers.
- Authorization and ownership tests.
- Validation and boundary tests.
- Concurrency tests.
- Migration tests.
- Cache hit, miss, invalidation and failure tests.
- Regression tests for brownfield changes.

Do not write tests that only verify mocks were called. Assert observable business behavior.

## API Standards

- Base path: `/api/v1`
- Use appropriate HTTP status codes.
- Return consistent RFC 7807-style errors.
- Include a correlation ID in errors and logs.
- Preserve backward compatibility.
- Use idempotency keys for operations where retries may create duplicate outcomes.
- Document APIs through OpenAPI.

## Observability

Include:

- Structured logs.
- Correlation IDs.
- Request latency.
- Error rate.
- Redirect cache hit rate.
- URL creation rate.
- Redirect rate.
- Authentication failures.
- Rate-limit rejections.
- Database and Redis health.
- Readiness and liveness endpoints.

Do not log destination URLs at an unsafe level if they may contain sensitive query parameters.

## AI Traceability

After every AI-assisted task, update:

- `docs/ai-assisted-engineering/traceability-matrix.md`
- `docs/ai-assisted-engineering/prompt-log.md`
- `docs/ai-assisted-engineering/human-approval-log.md`

Record:

- requirement ID
- prompt ID
- generated files
- accepted, edited or rejected decision
- human changes
- reason
- validation performed
- remaining risks

Preserve examples of rejected AI suggestions where they demonstrate engineering judgment.

## Build and Validation

Before marking a task complete, run:

```
./mvnw clean verify
docker compose config
```

Where available, also run:

```
./scripts/verify.sh
```

## Definition of Done

A task is complete only when:

- Acceptance criteria are met.
- Code compiles.
- Tests pass.
- Security and ownership rules are validated.
- Public API behavior is documented.
- Database changes are backward-compatible.
- Logs and metrics are sufficient.
- No secrets are committed.
- AI traceability is updated.
- The final diff is manually reviewable.
