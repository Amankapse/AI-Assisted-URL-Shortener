# Deployment Documentation

This folder documents a live demonstration deployment on Render using:

- one Render Web Service Docker container for the packaged Angular SPA and Spring Boot API
- Neon PostgreSQL for the production database
- Render Key Value / Valkey for Redis-compatible cache and rate-limit storage

This is the deployable live-demo path for the current modular monolith. It is separate from the documented hyperscale production evolution target.

## Documents

| Document | Purpose |
| --- | --- |
| [Render deployment](render.md) | Single Render Web Service, Docker packaging, Neon, and Key Value setup |
| [Future Static Site frontend](frontend-render.md) | Optional future Angular CDN/static split, publish directory, SPA rewrite, public runtime config, and headers |
| [Neon PostgreSQL](neon.md) | Neon database setup and JDBC configuration |
| [Environment variables](environment-variables.md) | Required and optional production variables |
| [Production validation](production-validation.md) | Health, OpenAPI, and smoke-test checks |
| [Production smoke test](production-smoke.md) | Browser and API smoke flow for live deployment |
| [Free-tier limitations](free-tier-limitations.md) | Honest limits of Render/Neon free-tier deployment |
