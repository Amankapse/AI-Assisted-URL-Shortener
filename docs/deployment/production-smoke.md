# Production Smoke Test

Use throwaway test users. Do not commit credentials.

## Frontend Flow

1. Open `https://ai-url-shortener-682u.onrender.com/` and confirm Angular loads.
2. Register a new user.
3. Log in.
4. Confirm the workspace selector loads.
5. Create a URL.
6. Copy the returned `shortUrl`.
7. Open the `shortUrl` in a new browser tab.
8. Confirm the backend returns a redirect, expected `302`.
9. Search for the created URL in `/app/urls`.
10. Open details and edit destination using the ETag-backed form.
11. Assign a campaign and tags.
12. Open URL analytics.
13. View URL audit history.
14. Create an API key.
15. Copy the raw key, acknowledge it, navigate away/back, and confirm the raw key is not recoverable.
16. Revoke the API key.
17. Logout.
18. Reload the browser and confirm unauthenticated state.

## Route Regression Checks

```powershell
$env:BASE_URL="https://ai-url-shortener-682u.onrender.com"
curl.exe -i "$env:BASE_URL/"
curl.exe -i "$env:BASE_URL/login"
curl.exe -i "$env:BASE_URL/app/urls"
curl.exe -i "$env:BASE_URL/app/admin/outbox"
curl.exe -i "$env:BASE_URL/api/v1/auth/me"
curl.exe -i "$env:BASE_URL/actuator/health/liveness"
curl.exe -i "$env:BASE_URL/swagger-ui/index.html"
curl.exe -i "$env:BASE_URL/v3/api-docs"
```

Expected:

- `/`, `/login`, and `/app/**` return Angular.
- `/api/v1/auth/me` remains a backend security response, normally `401` without a token.
- Actuator, Swagger, and OpenAPI remain backend routes.
- `/r/<shortCode>` remains the redirect route.

## Platform ADMIN Flow

Only run with an existing platform `ADMIN` account.

1. Open `/app/admin/overview`.
2. Confirm platform metrics and top links load.
3. Block a test URL.
4. Confirm public redirect becomes unavailable.
5. Unblock the URL.
6. Inspect `/app/admin/audit`.
7. Inspect `/app/admin/outbox` and confirm no unexplained growing backlog.

## Backend API Checks

```powershell
$env:BASE_URL="https://ai-url-shortener-682u.onrender.com"
curl.exe -i "$env:BASE_URL/actuator/health/liveness"
curl.exe -i "$env:BASE_URL/actuator/health/readiness"
curl.exe -i "$env:BASE_URL/v3/api-docs"
```

Register/login/refresh use CSRF and cookies, so browser or Postman is preferred for complete auth smoke testing.

## Expected Limitations

Render Free cold starts are deployment-platform behavior, not application steady-state latency. Record first request after idle separately from warm requests.
