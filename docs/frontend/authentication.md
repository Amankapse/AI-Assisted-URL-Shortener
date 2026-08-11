# Frontend Authentication

The browser keeps the JWT access token in Angular memory only. It is never written to `localStorage`, `sessionStorage`, IndexedDB, route parameters, or query parameters.

On startup the app:

1. Loads `/app-config.json`.
2. Calls `/actuator/health/liveness` with credentials to allow the backend CSRF cookie filter to expose `XSRF-TOKEN`.
3. Attempts exactly one `/api/v1/auth/refresh`.
4. Loads `/api/v1/auth/me` after refresh succeeds.
5. Clears auth state if refresh fails.

Multiple 401 responses that came from JWT-authenticated requests share one refresh request. Refresh is not attempted for login, registration, refresh, logout, API-key requests, or unauthenticated requests.

Refresh and logout include credentials and, when present, the `X-XSRF-TOKEN` header derived from the non-HttpOnly `XSRF-TOKEN` cookie. The frontend never reads the HttpOnly refresh token.

SameSite remains `Strict`; if frontend/backend are deployed on cross-site Render domains and refresh cookies do not flow, that requires a separate backend security/deployment decision.
