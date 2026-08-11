# API Notes

## Auth endpoints

- `POST /api/v1/auth/register`: create a `USER` account. Response omits password and password hash.
- `POST /api/v1/auth/login`: authenticate with email and password. Returns a bearer access token and sets an opaque `refresh_token` cookie.
- `POST /api/v1/auth/refresh`: rotate the refresh token and issue a new access token. Requires `refresh_token` cookie plus `X-XSRF-TOKEN`.
- `POST /api/v1/auth/logout`: revoke the presented refresh token and clear the cookie. Requires `X-XSRF-TOKEN` when a browser cookie is used.
- `GET /api/v1/auth/me`: return the authenticated user from the JWT subject.

Access tokens are RS256 JWTs with `sub` as the internal user UUID plus `email`, `role`, `authorities`, `iss`, `aud`, `iat`, `exp`, and `jti`. JWT headers include `alg=RS256`, `typ=JWT`, and the active `kid`.

## URL endpoints

- `POST /api/v1/urls`: create a short URL in the resolved workspace. Optional `campaignId` and `tags` can attach organization metadata at creation time. `Idempotency-Key` is optional; when supplied, the key is scoped to workspace, actor, operation, and key. Same-key/same-request replays the original successful response inside that scope. The idempotency fingerprint includes `campaignId` and a normalized sorted tag set.
- `GET /api/v1/urls`: list/search URLs in the resolved workspace with `q`, `state`, `campaignId`, `tag`, `createdFrom`, `createdTo`, `expiresBefore`, `expiresAfter`, `customAlias`, `sort`, `page`, and `size`.
- `GET /api/v1/urls/{id}`: fetch one URL in the resolved workspace and return an `ETag` for optimistic concurrency.
- `PATCH /api/v1/urls/{id}`: update expiration for a URL in the resolved workspace.
- `PATCH /api/v1/urls/{id}/destination`: update the destination URL without regenerating the short code. Requires `If-Match` with the current strong ETag.
- `PATCH /api/v1/urls/{id}/campaign`: assign or clear campaign metadata for a URL. Requires `If-Match` with the current strong ETag.
- `PUT /api/v1/urls/{id}/tags`: replace the URL tag set. Requires `If-Match` with the current strong ETag.
- `DELETE /api/v1/urls/{id}`: delete a URL in the resolved workspace.
- `POST /api/v1/urls/{id}/disable`: disable a URL in the resolved workspace.
- `POST /api/v1/urls/{id}/enable`: enable a URL in the resolved workspace.
- `GET /api/v1/urls/{id}/analytics`: return workspace-scoped totals and last-accessed timestamp for one URL.
- `GET /api/v1/urls/{id}/analytics/daily`: return workspace-scoped daily redirect counts for one URL.
- `GET /r/{shortCode}`: public redirect endpoint.
- `GET /v3/api-docs`: generated OpenAPI document.

Generated short codes are configurable and default to 8-character cryptographically random Base62 values. Existing 7-character short codes remain valid and are not rewritten.

URL management responses include `shortUrl`, derived at response time as `APP_PUBLIC_BASE_URL + "/r/" + shortCode`. The full short URL is not persisted.

URL search is intentionally bounded. `q` matches case-insensitive exact/prefix values across short code, custom alias, derived destination host, campaign name, and normalized tag name. It does not search raw destination URL query strings, user IDs, email addresses, API keys, audit metadata, outbox payloads, or analytics metadata. Sort is allowlisted to `createdAt`, `expiresAt`, `clickCount`, and `shortCode`, with `createdAt,desc` as the default. Page size is bounded by `APP_URL_SEARCH_MAX_PAGE_SIZE`.

Quota failures return RFC7807 `quota-exceeded` responses with HTTP 403. Quotas are resource allowances; rate limits are request-velocity controls and continue to use HTTP 429.

Workspace resolution uses optional `X-Workspace-ID`. If the header is absent, the authenticated user's default workspace is used. If the header is present but malformed, unknown, or unauthorized, the request fails and does not fall back to the default workspace.

## Workspace endpoints

- `GET /api/v1/workspaces`: list workspaces where the authenticated user is a member.
- `GET /api/v1/workspaces/{id}`: fetch a workspace visible to the authenticated member.
- `POST /api/v1/workspaces`: create a workspace and assign the creator as `OWNER`.
- `GET /api/v1/workspaces/{id}/members`: list members. Requires workspace `OWNER` or `ADMIN`.
- `POST /api/v1/workspaces/{id}/members`: add an existing user by email. Requires workspace `OWNER` or `ADMIN`; adding non-existent users is rejected.
- `PATCH /api/v1/workspaces/{id}/members/{userId}`: change a member role. `OWNER` can manage all roles; `ADMIN` cannot grant, demote, or remove `OWNER`.
- `DELETE /api/v1/workspaces/{id}/members/{userId}`: remove a member. The final `OWNER` cannot be removed.

## Campaign and tag endpoints

- `POST /api/v1/workspaces/{workspaceId}/campaigns`: create a workspace campaign. Requires human workspace `OWNER`, `ADMIN`, or `EDITOR`.
- `GET /api/v1/workspaces/{workspaceId}/campaigns`: list active campaigns. Human workspace members and API keys with `links:read` or `links:write` may read.
- `GET /api/v1/workspaces/{workspaceId}/campaigns/{campaignId}`: fetch a campaign.
- `PATCH /api/v1/workspaces/{workspaceId}/campaigns/{campaignId}`: update campaign name/description. Requires human workspace `OWNER`, `ADMIN`, or `EDITOR`.
- `DELETE /api/v1/workspaces/{workspaceId}/campaigns/{campaignId}`: soft-delete a campaign. Requires human workspace `OWNER` or `ADMIN`; URLs are detached and remain active.
- `GET /api/v1/workspaces/{workspaceId}/tags`: list normalized workspace tags. Human workspace members and API keys with `links:read` or `links:write` may read.

Tags are stored relationally and normalized with `Locale.ROOT` lowercase. They must match `^[a-z0-9][a-z0-9_-]{0,49}$`, are deduplicated as an unordered set, and are bounded by `APP_TAGS_PER_URL`.

Workspace roles are `OWNER`, `ADMIN`, `EDITOR`, `ANALYST`, and `VIEWER`. Link write operations require `OWNER`, `ADMIN`, or `EDITOR`. Analytics reads are allowed for all workspace members, including `ANALYST` and `VIEWER`.

## Audit endpoints

- `GET /api/v1/workspaces/{workspaceId}/audit`: list workspace audit events. Requires workspace `OWNER` or `ADMIN`.
- `GET /api/v1/urls/{id}/audit`: list audit events for a URL. Requires workspace `OWNER`, `ADMIN`, or `EDITOR`.
- `GET /api/v1/admin/audit`: list platform audit events. Requires platform `ROLE_ADMIN`.

Supported filters are `from`, `to`, `action`, `resourceType`, `actorType`, `page`, and `size`. The platform endpoint also supports `workspaceId`. Results are ordered by `occurredAt DESC`; page size is bounded to 100. The API exposes DTOs only: event ID, timestamp, schema version, workspace ID, actor type/ID, action, resource type/ID, correlation ID, and `safeMetadata`.

Audited actions are URL creation, destination change, expiration change, enable, disable, delete, block, unblock, URL campaign/tag changes, campaign create/update/delete, workspace creation, member add, member role change, and member removal. Reads and public redirects are not audit events. Destination-change metadata stores host and SHA-256 URL hashes rather than raw URLs.

## API key endpoints

- `POST /api/v1/workspaces/{workspaceId}/api-keys`: create a workspace-bound machine API key. Requires a human JWT actor with workspace `OWNER` or `ADMIN`.
- `GET /api/v1/workspaces/{workspaceId}/api-keys`: list API-key metadata for a workspace. Requires workspace `OWNER` or `ADMIN`.
- `POST /api/v1/workspaces/{workspaceId}/api-keys/{id}/revoke`: logically revoke an API key. Requires workspace `OWNER` or `ADMIN`; repeat revocation is idempotent.

API keys are supplied only in the `X-API-Key` header. Raw key material is returned once on creation and is never stored. PostgreSQL stores a globally unique prefix plus an HMAC-SHA-256 digest using `APP_API_KEY_HASH_PEPPER`. Machine scopes are `links:read`, `links:write`, and `analytics:read`.

API-key authentication is workspace-bound. It can access scoped URL and URL analytics APIs for its own workspace, including idempotent URL creation with actor-specific idempotency scope. API keys with `links:read` can list/search URLs and read campaign/tag metadata; API keys with `links:write` can assign URL campaign/tags through URL metadata endpoints. API keys cannot create, update, or delete campaigns, manage workspaces, create or revoke API keys, call human auth endpoints, or call platform admin endpoints. Supplying both `Authorization: Bearer ...` and `X-API-Key` is rejected with generic HTTP 401 behavior.

## Admin analytics endpoints

- `GET /api/v1/admin/analytics/overview`: return aggregate user, link, state, and redirect counts. Requires `ROLE_ADMIN`.
- `GET /api/v1/admin/analytics/top-links?limit={n}`: return top redirected short codes. Requires `ROLE_ADMIN`; `limit` is bounded by configuration.

Admin analytics endpoints do not grant `ADMIN` bypass access to normal workspace URL APIs.

## Admin moderation endpoints

- `POST /api/v1/admin/urls/{id}/block`: administratively block a URL. Requires `ROLE_ADMIN`; idempotent; returns 204.
- `POST /api/v1/admin/urls/{id}/unblock`: remove administrative block. Requires `ROLE_ADMIN`; idempotent; returns 204.

Blocked links return safe not-found behavior on redirect. Owners cannot remove administrative blocks through normal user enable endpoints. Analytics history is retained.

## Admin outbox endpoint

- `GET /api/v1/admin/outbox`: list safe transactional outbox summaries. Requires platform `ROLE_ADMIN`.

Supported filters are `status`, `eventType`, `from`, `to`, `page`, and `size`. Page size is bounded to 100. Responses include event ID, event type/version, aggregate type, status, attempt count, timestamps, and stable last error code. Raw payload JSON is intentionally not exposed.

API keys and workspace admins cannot access this platform endpoint.

## Operational endpoints

- `GET /actuator/health`: public health endpoint for platform checks.
- `GET /actuator/health/liveness`: public JVM/application liveness probe; does not depend on PostgreSQL or Redis.
- `GET /actuator/health/readiness`: public readiness probe; requires PostgreSQL and intentionally excludes Redis.
- `GET /actuator/metrics`: exposed but protected by `ROLE_ADMIN`.

Sensitive Actuator endpoints such as `env`, `configprops`, `heapdump`, `threaddump`, `beans`, `mappings`, `loggers`, and `conditions` are not exposed.

## Ownership

Authenticated URL management uses `SecurityCurrentOwnerProvider` to build `OwnerIdentity(UUID userId, String email, UserRole role)` from the JWT principal. Workspace-owned repository queries include `workspace_id`, and the authenticated actor must be a member with the required workspace role. `short_urls.owner_id` remains as creator/legacy actor metadata; it is not accepted from clients and is not the tenant boundary. Request DTOs and controller parameters do not accept owner IDs, emails, or user IDs for URL ownership, and platform `ADMIN` does not imply workspace administration.

## Redirect cache and analytics privacy

Redirect cache keys use `url:v1:redirect:{shortCode}`. Cache values contain only a schema version, URL ID, destination URL, enabled flag, expiration timestamp, deleted state, and blocked state. Redis does not store JPA entities, auth state, owner data, tokens, headers, cookies, or personal data.

Valid redirects enqueue a best-effort analytics event. Stored event metadata is sanitized: IP addresses are HMAC-SHA-256 hashed with the configured pepper, referrers are reduced to host, user agents are normalized to category, and correlation IDs are bounded safe values.

`APP_ANALYTICS_PUBLISHER=local` keeps the current bounded in-memory analytics queue. `APP_ANALYTICS_PUBLISHER=outbox` writes sanitized click events through the transactional outbox and preserves idempotent analytics persistence, but it adds a PostgreSQL write to the redirect path and is not the hyperscale click-stream target.

## CSRF and CORS

Bearer-token URL APIs are stateless and do not require CSRF tokens. Refresh and logout use the HttpOnly `refresh_token` cookie and require the readable `XSRF-TOKEN` cookie value to be echoed in `X-XSRF-TOKEN`. CORS uses the configured `app.auth.allowed-origins` list and does not use a wildcard when credentials are allowed.

## Error responses

Validation, malformed JSON, bad requests, idempotency conflicts, precondition failures, not-found, unauthorized, forbidden, and rate-limit responses use RFC7807-style `application/problem+json` responses. Global application errors include a `correlationId` property; security entry-point responses return safe problem details without stack traces. Rate-limit responses use HTTP 429 with error code `rate_limit_exceeded` and include `Retry-After` when available.
