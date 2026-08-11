# Frontend User Flows

## Authentication And Workspace Foundation

Register:

1. User opens `/register`.
2. Angular validates email and minimum password length.
3. Frontend calls `POST /api/v1/auth/register`.
4. On success, frontend logs in with the same credentials.
5. User lands on `/app`.

Login:

1. User opens `/login`.
2. Frontend calls `POST /api/v1/auth/login`.
3. Backend returns an access token and sets the refresh cookie.
4. Frontend stores the access token in memory and loads workspaces.

Reload:

1. Runtime config loads.
2. Frontend bootstraps CSRF through liveness.
3. Frontend attempts refresh once.
4. Authenticated users proceed to `/app`; unauthenticated users are sent to `/login`.

Workspace:

1. Frontend calls `GET /api/v1/workspaces`.
2. Default workspace is selected when present.
3. Future management requests receive `X-Workspace-ID` centrally.

## Stage 9B

Link dashboard:

1. User opens `/app/urls`.
2. Frontend loads campaigns, tags and paginated links for the selected workspace.
3. Filters and sort choices sync to query parameters.
4. Rapid filter changes debounce before reloading; stale list responses are ignored.
5. Enable, disable and soft-delete actions use existing backend endpoints. BLOCKED links show a text explanation and owner actions are disabled in the UI.

Create link:

1. User opens `/app/urls/new`.
2. Frontend validates HTTP/HTTPS destination, alias format, required expiration, campaign and normalized tags.
3. Frontend sends `POST /api/v1/urls` with one idempotency key for the logical attempt.
4. Retry after an error reuses the same key; Create another generates a new key.
5. Success displays copy/open/view/create-another actions.

Edit link details:

1. User opens `/app/urls/{id}`.
2. Frontend captures the latest `ETag`.
3. Destination, campaign and tag replacement send `If-Match`.
4. `412` or `428` responses show a reload-required conflict and do not silently replay the mutation.

Campaigns:

1. User opens `/app/campaigns`.
2. OWNER, ADMIN and EDITOR roles can create or edit campaigns.
3. OWNER and ADMIN roles can delete campaigns.
4. VIEWER can read campaigns only; backend authorization remains authoritative.

## Stage 9C

URL analytics:

1. User opens `/app/urls/{id}/analytics`.
2. Frontend loads analytics summary and daily redirects for the selected workspace context.
3. Zero-click URLs show an empty analytics state.
4. Daily counts are rendered as KPI/table data without fabricated geography, browser, device, or referrer fields.

Audit:

1. User opens `/app/audit`.
2. Frontend loads server-paged workspace audit events.
3. Filters use only backend-supported fields: `from`, `to`, `action`, `resourceType`, `actorType`, `page`, and `size`.
4. Metadata is rendered as text from an allowlist.
5. URL details also show URL-specific audit history.

API keys:

1. User opens `/app/api-keys`.
2. OWNER/ADMIN users can create and revoke keys; other roles see disabled controls.
3. Create sends name, supported scopes, and optional expiration.
4. Raw API key is shown once in a dedicated panel and is cleared after acknowledgment.
5. List and revoke flows show only safe key metadata.

Workspace management:

1. User opens `/app/workspace`.
2. Current workspace and role are shown.
3. Workspace creation uses the existing workspace create API.
4. OWNER/ADMIN users can list, add, change role, and remove members.
5. Removal and privileged demotion require confirmation.

Platform admin:

1. Platform `ADMIN` users see Platform navigation.
2. `/app/admin/overview` shows real admin analytics and top links.
3. `/app/admin/moderation` blocks/unblocks URLs by URL id with confirmation for blocking.
4. `/app/admin/audit` loads server-paged platform audit events.
5. `/app/admin/outbox` shows read-only outbox event summaries; no retry/replay/delete UI is exposed.
