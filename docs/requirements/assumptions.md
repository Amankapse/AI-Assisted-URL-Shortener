# Assumptions

This project is built on a set of defensible assumptions where the assignment does not fully specify scale, ownership, or enterprise behavior.

## Product assumptions

- The service is a secure enterprise-facing API with no browser session-based authentication.
- All link creation and management requires authenticated users.
- Public redirects are available without authentication.
- Users may optionally request a custom alias within validation rules.
- Links can expire by explicit timestamp or be disabled by the owner or admin.
- Analytics are click-based and eventually consistent for admin summaries.

## Operational assumptions

- The design assumes 100,000 registered users and 20,000 monthly active users.
- Normal load is approximately 20 URL creations per second, with a 100 per second peak.
- Normal redirect volume is approximately 1,000 per second, with a 5,000 per second peak.
- The read-to-write ratio is assumed to be about 50:1 for redirects to creations.
- URL creation P95 target is below 300 ms.
- Redis-hit redirect P95 target is below 50 ms.
- PostgreSQL-fallback redirect P95 target is below 200 ms.
- Availability objective is 99.9%.
- Analytics retention is assumed to be 12 months.

## Architecture assumptions

- PostgreSQL is the authoritative store and supports all primary business queries.
- Redis is used only for redirect caching and short-code resolution.
- The system is a single deployable monolith in the assignment timeframe.
- Future separation into microservices is possible but not required now.

## Security assumptions

- Bearer token authentication is used for stateless API requests, so CSRF protection is not required for access-token-based requests.
- Refresh tokens are stored in Secure HttpOnly cookies and protected with CSRF defenses.
- Refresh-token digests use SHA-256. The raw token is shown only once in an HttpOnly cookie and is never stored.
- Production JWT signing keys are provided through environment variables; test-only ephemeral RSA keys are acceptable only for reproducible test runs.
- Rate limits are future work and were not introduced during Phase 3 authentication validation.
