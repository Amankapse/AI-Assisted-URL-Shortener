# API Key Security

Stage 6 adds machine-to-machine authentication for workspace automation without changing the human JWT and refresh-token model.

## Credential Format

API keys are generated as opaque values with:

- a fixed service prefix: `usk_live`
- a random lookup prefix
- a random 256-bit secret

Clients send API keys only through:

```text
X-API-Key: <raw-api-key>
```

The API does not accept keys in query parameters, path variables, cookies, or request bodies.

## Storage

Raw keys are returned exactly once from the create response. They are not stored.

PostgreSQL stores:

- globally unique key prefix
- HMAC-SHA-256 digest of the raw key using `APP_API_KEY_HASH_PEPPER`
- workspace ID
- creator user ID
- scopes
- created, expiration, revoked, and last-used timestamps

Digest comparison uses constant-time byte comparison. The pepper is a production secret and must be provided through environment or secret management.

## Authorization

API keys are bound to one workspace. Supported scopes are:

- `links:read`
- `links:write`
- `analytics:read`

API keys can call scoped URL and URL analytics APIs only within their workspace. They cannot:

- register or login users
- refresh or logout browser sessions
- create, list, or revoke API keys
- manage workspaces or members
- access platform admin APIs
- receive `ROLE_ADMIN`

Supplying both `Authorization: Bearer ...` and `X-API-Key` is rejected.

## Rotation And Revocation

Revocation is logical through `revoked_at` and is idempotent. Rotation is performed by creating a replacement key, updating clients, then revoking the old key.

Pepper rotation is not automatic. Rotating `APP_API_KEY_HASH_PEPPER` invalidates existing digests unless a future dual-pepper verification window or coordinated key reissue process is added.

## Logging And Audit

Raw API keys, digests, and pepper values must never be logged. API-key create and revoke operations are audited as `API_KEY_CREATED` and `API_KEY_REVOKED`. URL mutations performed by a machine actor record audit actor type `API_KEY`.

## Residual Risk

API keys are bearer credentials. A stolen raw key remains usable until expiration or revocation. Operators should use short lifetimes, least-privilege scopes, secure secret storage in callers, and monitor key usage and rate-limit rejections.
