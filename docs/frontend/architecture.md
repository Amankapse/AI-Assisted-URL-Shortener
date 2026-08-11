# Frontend Architecture

The frontend is a source-separated Angular 22 single-page application under `frontend/` and communicates with the backend through typed API clients under `frontend/src/app/core/api`. The current Render demo packages the production Angular build into the Spring Boot JAR during the Docker build so one Web Service can serve both UI and API. The source architecture remains independent and can later split Angular back to CDN/static hosting through deployment/configuration changes.

Stage 9 uses standalone components, route-level lazy loading, functional guards, functional interceptors, Reactive Forms, and Signals. Access tokens remain in memory only. Refresh tokens remain backend-owned `Secure`, `HttpOnly`, `SameSite=Strict` cookies.

## Boundaries

- `core/config`: runtime JSON config loaded before bootstrap completes.
- `core/api`: handwritten API clients and DTO contracts.
- `core/auth`: auth state and refresh orchestration.
- `core/workspace`: selected workspace state.
- `core/interceptors`: bearer token, CSRF, workspace header, and centralized error behavior.
- `features/auth`: login and registration.
- `features/urls`: production link dashboard, create form, and ETag-aware details editor.
- `features/campaigns`: workspace campaign list/create/edit/delete UX with role-aware affordances.
- `features/analytics`: URL analytics using existing owner/workspace analytics APIs.
- `features/audit`: workspace audit trail.
- `features/api-keys`: API-key lifecycle management with one-time raw-key display.
- `features/workspace`: workspace and membership management.
- `features/admin`: platform ADMIN overview, moderation, audit, and read-only outbox visibility.
- `features/dashboard`: earlier workspace landing surface, no longer the primary authenticated route.
- `layout`: authenticated app shell.

Stage 9D adds single-service Render deployment hardening and evidence. QR codes, custom domains, distributed tracing UI, and advanced charting remain out of scope.

## Link Management Contract

The frontend uses handwritten clients for the existing backend contracts:

- `POST /api/v1/urls` with `Idempotency-Key`.
- `GET /api/v1/urls` with bounded filters, pagination and an allowlisted `sort` value.
- `GET /api/v1/urls/{id}` with captured `ETag`.
- `PATCH /api/v1/urls/{id}/destination`, `PATCH /api/v1/urls/{id}/campaign`, and `PUT /api/v1/urls/{id}/tags` with `If-Match`.
- `POST /api/v1/urls/{id}/enable`, `POST /api/v1/urls/{id}/disable`, and `DELETE /api/v1/urls/{id}` for state changes.
- `/api/v1/workspaces/{workspaceId}/campaigns` and `/api/v1/workspaces/{workspaceId}/tags` for workspace metadata.

The dashboard ignores stale list responses during rapid filter changes or workspace switching. It does not add backend APIs or change authorization behavior.

## Enterprise Operations Contracts

Stage 9C consumes existing backend contracts only:

- URL analytics: `GET /api/v1/urls/{id}/analytics` and `/analytics/daily`.
- Workspace audit: `GET /api/v1/workspaces/{workspaceId}/audit`.
- URL audit history: `GET /api/v1/urls/{urlId}/audit`.
- API keys: `GET/POST /api/v1/workspaces/{workspaceId}/api-keys` and `POST /{id}/revoke`.
- Workspace members: `GET/POST /api/v1/workspaces/{id}/members`, `PATCH/DELETE /members/{userId}`.
- Admin analytics: `GET /api/v1/admin/analytics/overview` and `/top-links`.
- Admin moderation: `POST /api/v1/admin/urls/{id}/block` and `/unblock`.
- Admin audit: `GET /api/v1/admin/audit`.
- Admin outbox: `GET /api/v1/admin/outbox`.

Platform administration is guarded by authenticated user role `ADMIN` in the frontend and by backend authorization. Workspace `OWNER` is not treated as platform admin.
