# Frontend Security

Frontend security controls:

- JWT access token is memory-only.
- Refresh token stays in backend `HttpOnly` cookie.
- Refresh/logout use `withCredentials` and `X-XSRF-TOKEN` when the CSRF cookie is present.
- API requests are centralized through typed services.
- Workspace context is propagated centrally through `X-Workspace-ID`.
- RFC7807 errors are rendered as text, not HTML.
- No `DomSanitizer.bypassSecurityTrust...` usage.
- Runtime config contains origins only, never secrets.
- Link creation uses an idempotency key for retry safety and does not expose owner IDs.
- Destination, campaign and tag metadata edits use backend ETag/`If-Match` concurrency protection.
- Dashboard sort values are allowlisted client-side before being sent.
- RFC7807 `429`, `409`, `412` and `428` responses are surfaced to users without exposing internal state.
- BLOCKED links disable owner mutation affordances in the UI; backend authorization and moderation remain authoritative.
- API-key raw secrets are displayed only in the create component's transient state and are cleared after acknowledgment.
- API-key raw secrets are not written to localStorage, sessionStorage, URLs, route state, or notifications.
- Audit metadata is treated as untrusted data and rendered only as text from an allowlist.
- Workspace switching clears enterprise page state through component reloads keyed to the selected workspace signal.
- Platform ADMIN navigation is based on `ROLE_ADMIN`; workspace OWNER/ADMIN roles do not unlock platform routes.
- Outbox visibility is read-only; no retry, replay, delete or mutation controls are exposed.

Recommended CSP for static hosting:

```text
default-src 'self';
script-src 'self';
style-src 'self';
img-src 'self' data:;
connect-src 'self' https://<api-origin>;
frame-ancestors 'none';
base-uri 'self';
form-action 'self';
```

Do not add inline scripts or libraries requiring `unsafe-eval` without security review.
