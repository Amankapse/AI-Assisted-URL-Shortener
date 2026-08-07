# Deployment Documentation

This folder documents a live demonstration deployment on Render using:

- Render Web Service for the Spring Boot container
- Neon PostgreSQL for the production database
- Render Key Value / Valkey for Redis-compatible cache and rate-limit storage

This is the deployable live-demo path for the current modular monolith. It is separate from the documented hyperscale production evolution target.

## Documents

| Document | Purpose |
| --- | --- |
| [Render deployment](render.md) | Render Web Service and Key Value setup |
| [Neon PostgreSQL](neon.md) | Neon database setup and JDBC configuration |
| [Environment variables](environment-variables.md) | Required and optional production variables |
| [Production validation](production-validation.md) | Health, OpenAPI, and smoke-test checks |
| [Free-tier limitations](free-tier-limitations.md) | Honest limits of Render/Neon free-tier deployment |

