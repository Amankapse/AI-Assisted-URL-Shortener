# Authentication And Ownership

## Current Phase

The implementation preserves RS256 authentication and adds workspace tenancy on top of the authenticated actor model. Authentication is JWT based, refresh-token rotation is opaque-cookie based, and URL tenant access is derived from workspace membership rather than client-supplied owner fields.

## Access Tokens

- Access tokens are RS256 JWTs validated by Spring Security resource server support.
- The JWT header includes `alg=RS256`, `typ=JWT`, and the active `kid`.
- Required claims are `sub`, `email`, `role`, `iss`, `aud`, `iat`, `exp`, and `jti`.
- `sub` is the internal user UUID and identifies the actor used for workspace membership checks.
- Tokens with invalid signatures, unsupported algorithms, wrong issuer, wrong audience, expired timestamps, or missing required claims are rejected.

## Refresh Tokens

- Refresh tokens are opaque random values with at least 256 bits of entropy.
- Only a SHA-256 digest is stored in PostgreSQL.
- Refresh tokens are delivered in the `refresh_token` cookie with `HttpOnly`, `SameSite=Strict`, the auth path scope, and `Secure` according to environment configuration.
- Refresh rotates the token and stores the replacement in the same family.
- Reuse of a revoked token marks reuse detection, revokes the active family, logs only safe family metadata, clears the cookie, and requires login again.

## API Keys

- API keys are workspace-bound machine credentials accepted only through the `X-API-Key` header.
- A human workspace `OWNER` or `ADMIN` must create or revoke keys; API keys cannot manage keys, members, workspaces, auth endpoints, or platform admin endpoints.
- Raw key material is returned exactly once on creation. PostgreSQL stores only the key prefix and an HMAC-SHA-256 digest using `APP_API_KEY_HASH_PEPPER`.
- Authentication performs prefix lookup followed by constant-time digest comparison.
- Supported scopes are `links:read`, `links:write`, and `analytics:read`. Scopes are additive but still limited to the key's workspace.
- Supplying both a bearer token and an API key is rejected with a generic unauthorized response.
- Revoked, expired, malformed, and unknown keys return the same generic unauthorized behavior.
- API-key request velocity is controlled by the `api-key` Redis Lua rate-limit policy and fails closed by default.

## CSRF And CORS

- Bearer-token API calls are stateless and are not CSRF-protected.
- Refresh and logout are CSRF-protected because the browser automatically sends the refresh cookie.
- The readable `XSRF-TOKEN` cookie must be echoed in `X-XSRF-TOKEN`.
- CORS uses `app.auth.allowed-origins` and does not allow wildcard origins with credentials.
- Login, registration, and refresh are Redis rate limited. Responses stay generic and do not reveal account existence.
- Security headers include `X-Content-Type-Options: nosniff`, frame denial, and `Referrer-Policy: no-referrer`. HSTS is enabled only when secure-cookie/production HTTPS behavior is configured.

## Ownership

`SecurityCurrentOwnerProvider` derives `OwnerIdentity(UUID userId, String email, UserRole role)` from the authenticated JWT principal. URL controllers do not accept owner IDs, emails, or user IDs for URL ownership. Workspace-aware services resolve `X-Workspace-ID` or the actor's default workspace, require membership through the centralized workspace authorization service, and scope repository calls by `workspace_id`.

`short_urls.owner_id` remains creator/legacy actor metadata. It is not the tenant boundary. Platform `ADMIN` does not imply workspace admin and does not bypass workspace URL endpoints.

Machine actors use `ApiKeyPrincipal` instead of `OwnerIdentity`; service-layer authorization resolves their workspace and scope through the centralized workspace authorization service. API keys never accept client-supplied owner IDs and cannot cross to another workspace through `X-Workspace-ID`.
