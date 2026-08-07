# ADR-004: Security Model

## Status
Accepted

## Context
The service will expose user management and administrative endpoints. It must enforce strong authentication and authorization.

## Decision
Use Spring Security resource server support with RS256 JWT bearer tokens for access-token authentication and role-based access control with `USER` and `ADMIN` roles. Use opaque refresh tokens stored only as SHA-256 digests and rotated through a CSRF-protected cookie flow.

Key security rules:
- Deny access by default.
- Public redirect endpoint is unauthenticated.
- All management APIs require a valid JWT bearer token.
- `USER` can manage only their own links and analytics.
- `ADMIN` does not bypass ownership on user endpoints; future privileged behavior must use explicit admin endpoints.
- Passwords are hashed with BCrypt.
- JWTs require RS256 signature validation plus issuer, audience, expiration, and required claims.
- CSRF is not required for bearer-token stateless APIs. Refresh and logout are CSRF-protected because the refresh token is transported in an HttpOnly cookie.
- Refresh-token reuse revokes the token family and clears the refresh cookie.

## Consequences
- Stateless authentication simplifies horizontal scaling.
- The API must validate and reject invalid or expired tokens.
- Ownership checks are required in services and repositories.
- Security tests must prove role separation and token rejection.
- Production deployments must provide RSA keys through environment variables; test-only ephemeral keys are allowed only under the `test` profile.
