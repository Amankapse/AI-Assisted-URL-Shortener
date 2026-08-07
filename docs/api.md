# API Notes

## Auth endpoints

- `POST /api/v1/auth/register`: create a `USER` account. Response omits password and password hash.
- `POST /api/v1/auth/login`: authenticate with email and password. Returns a bearer access token and sets an opaque `refresh_token` cookie.
- `POST /api/v1/auth/refresh`: rotate the refresh token and issue a new access token. Requires `refresh_token` cookie plus `X-XSRF-TOKEN`.
- `POST /api/v1/auth/logout`: revoke the presented refresh token and clear the cookie. Requires `X-XSRF-TOKEN` when a browser cookie is used.
- `GET /api/v1/auth/me`: return the authenticated user from the JWT subject.

Access tokens are RS256 JWTs with `sub` as the internal user UUID plus `email`, `role`, `authorities`, `iss`, `aud`, `iat`, `exp`, and `jti`. JWT headers include `alg=RS256`, `typ=JWT`, and the active `kid`.

## URL endpoints

- `POST /api/v1/urls`: create a short URL for the current owner.
- `GET /api/v1/urls`: list the current owner's URLs with `page` and `size` pagination.
- `GET /api/v1/urls/{id}`: fetch one URL owned by the current owner.
- `PATCH /api/v1/urls/{id}`: update expiration for an owned URL.
- `DELETE /api/v1/urls/{id}`: delete an owned URL.
- `POST /api/v1/urls/{id}/disable`: disable an owned URL.
- `POST /api/v1/urls/{id}/enable`: enable an owned URL.
- `GET /api/v1/urls/{id}/analytics`: return owner-scoped totals and last-accessed timestamp for one URL.
- `GET /api/v1/urls/{id}/analytics/daily`: return owner-scoped daily redirect counts for one URL.
- `GET /r/{shortCode}`: public redirect endpoint.
- `GET /v3/api-docs`: generated OpenAPI document.

## Admin analytics endpoints

- `GET /api/v1/admin/analytics/overview`: return aggregate user, link, state, and redirect counts. Requires `ROLE_ADMIN`.
- `GET /api/v1/admin/analytics/top-links?limit={n}`: return top redirected short codes. Requires `ROLE_ADMIN`; `limit` is bounded by configuration.

Admin analytics endpoints do not grant `ADMIN` bypass access to normal user URL APIs.

## Ownership

Authenticated URL management uses `SecurityCurrentOwnerProvider` to build `OwnerIdentity(UUID userId, String email, UserRole role)` from the JWT principal. URL services scope repository queries by `sub` UUID. Request DTOs and controller parameters do not accept owner IDs, emails, or user IDs from clients, and `ADMIN` does not bypass ownership on user URL endpoints.

## Redirect cache and analytics privacy

Redirect cache keys use `url:v1:redirect:{shortCode}`. Cache values contain only a schema version, URL ID, destination URL, enabled flag, expiration timestamp, and deleted state. Redis does not store JPA entities, auth state, owner data, tokens, headers, cookies, or personal data.

Valid redirects enqueue a best-effort analytics event. Stored event metadata is sanitized: IP addresses are HMAC-SHA-256 hashed with the configured pepper, referrers are reduced to host, user agents are normalized to category, and correlation IDs are bounded safe values.

## CSRF and CORS

Bearer-token URL APIs are stateless and do not require CSRF tokens. Refresh and logout use the HttpOnly `refresh_token` cookie and require the readable `XSRF-TOKEN` cookie value to be echoed in `X-XSRF-TOKEN`. CORS uses the configured `app.auth.allowed-origins` list and does not use a wildcard when credentials are allowed.

## Error responses

Validation, malformed JSON, bad requests, not-found, unauthorized, and forbidden responses use RFC7807-style `application/problem+json` responses. Global application errors include a `correlationId` property; security entry-point responses return safe problem details without stack traces.
