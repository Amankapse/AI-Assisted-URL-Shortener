# Acceptance Criteria

## Core service criteria

- A short URL can be created for a valid destination URL.
- Public redirects work for valid short codes.
- Optional custom alias creation is supported and validated.
- Links can be enabled, disabled, and expired.
- Users can view their paginated link history.
- Analytics are recorded for redirect events.
- Admin role can access platform-level moderation and analytics.

## Security criteria

- Registration and login use strong password hashing.
- JWT authentication and refresh tokens protect user sessions.
- `USER` can only manage their own links.
- `ADMIN` can access administrative endpoints only.
- Invalid or expired tokens are rejected.
- Malformed URLs and unsafe schemes are rejected.
- CSRF decision is documented and consistent with bearer-token API.

## Operational criteria

- Health and readiness endpoints exist.
- The system remains functional if Redis is unavailable.
- Redirect cache-aside behavior is documented.
- Database schema uses Flyway migrations.
- APIs follow base path `/api/v1`.

## Quality criteria

- Phase 0–1 documentation is complete and structured.
- An execution plan is defined with phases and validation gates.
- AI traceability is documented in `docs/ai-assisted-engineering/traceability-matrix.md`.
- Human approval is recorded for significant design decisions.
- No application code is implemented before Phase 1.
