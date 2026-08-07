# Authentication And Ownership

## Current Phase

Phase 5 preserves the Phase 3 authentication and ownership model and adds operational hardening around it. Authentication remains RS256 JWT based, refresh-token rotation remains opaque-cookie based, and user URL ownership is still derived from `CurrentOwnerProvider` rather than client-supplied owner fields.

## Access Tokens

- Access tokens are RS256 JWTs validated by Spring Security resource server support.
- The JWT header includes `alg=RS256`, `typ=JWT`, and the active `kid`.
- Required claims are `sub`, `email`, `role`, `iss`, `aud`, `iat`, `exp`, and `jti`.
- `sub` is the internal user UUID and is the ownership key for URL operations.
- Tokens with invalid signatures, unsupported algorithms, wrong issuer, wrong audience, expired timestamps, or missing required claims are rejected.

## Refresh Tokens

- Refresh tokens are opaque random values with at least 256 bits of entropy.
- Only a SHA-256 digest is stored in PostgreSQL.
- Refresh tokens are delivered in the `refresh_token` cookie with `HttpOnly`, `SameSite=Strict`, the auth path scope, and `Secure` according to environment configuration.
- Refresh rotates the token and stores the replacement in the same family.
- Reuse of a revoked token marks reuse detection, revokes the active family, logs only safe family metadata, clears the cookie, and requires login again.

## CSRF And CORS

- Bearer-token API calls are stateless and are not CSRF-protected.
- Refresh and logout are CSRF-protected because the browser automatically sends the refresh cookie.
- The readable `XSRF-TOKEN` cookie must be echoed in `X-XSRF-TOKEN`.
- CORS uses `app.auth.allowed-origins` and does not allow wildcard origins with credentials.
- Login, registration, and refresh are Redis rate limited. Responses stay generic and do not reveal account existence.
- Security headers include `X-Content-Type-Options: nosniff`, frame denial, and `Referrer-Policy: no-referrer`. HSTS is enabled only when secure-cookie/production HTTPS behavior is configured.

## Ownership

`SecurityCurrentOwnerProvider` derives `OwnerIdentity(UUID userId, String email, UserRole role)` from the authenticated JWT principal. URL controllers do not accept owner IDs, emails, or user IDs, and URL services scope repository calls by the JWT subject UUID. `ADMIN` does not bypass ownership on user URL endpoints.
