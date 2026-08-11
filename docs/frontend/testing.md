# Frontend Testing

The frontend uses Angular CLI-generated unit-test tooling with Vitest and jsdom.

Current coverage of frontend behavior:

- runtime config loads and rejects malformed origins
- Problem Details mapper preserves safe fields and retry guidance
- access token remains memory-only
- refresh requests are single-flight
- auth interceptor adds bearer tokens
- workspace interceptor adds `X-Workspace-ID` only for management API requests
- app host component creates successfully
- URL create API sends `Idempotency-Key`
- URL destination update API sends `If-Match`
- tag normalization, deduplication and validation
- URL state label/description mapping for moderation states
- campaign action affordances for workspace roles
- URL analytics API success and zero-click shape
- API-key create, list, revoke, and raw-key non-persistence regression
- workspace audit filters and pagination
- workspace member list/add/change/remove clients
- platform moderation and outbox admin endpoints
- audit metadata allowlisting
- workspace permission separation from platform admin state

Security/dependency note: `npm audit --audit-level=moderate` currently reports 3 moderate findings in the Angular CLI dev-dependency chain through `@modelcontextprotocol/sdk` and `@hono/node-server`. The suggested forced fix would downgrade Angular CLI to 21.0.4, so it was not applied.

Run:

```powershell
cd frontend
npm test -- --watch=false
npm run build
```

Stage 9C validation: `npm test -- --watch=false` passed with 21 tests, and `npm run build` passed with initial bundle 103.64 kB raw / 26.71 kB estimated transfer. The local sandbox blocks Angular compiler reads for source/style files and `node_modules`, so frontend validation commands are run with scoped filesystem access outside the sandbox.

End-to-end browser automation is deferred.
