# Free-Tier Limitations

The Render/Neon deployment is a live demonstration deployment. It is not the documented 100M-new-URLs/day hyperscale architecture target.

Expected limits:

- Render Free services may cold-start after inactivity.
- Render Free service memory and CPU are limited; use conservative JVM heap settings.
- Render Key Value free-tier data can be non-persistent or reset depending on the selected plan.
- Cache and rate-limit state may reset when Redis/Valkey data is lost.
- Neon free tier has connection, storage, compute, and suspend/resume limits.
- PostgreSQL remains authoritative, but free-tier capacity is not sized for high sustained write or redirect volume.
- There is no CDN/edge redirect layer in this live demo.
- There is no multi-region deployment, WAF, durable event broker, OLAP warehouse, or external secret manager integration in the demo environment.

The hyperscale target remains documented in the architecture documents as a production evolution path, not as a claim about this free-tier deployment.

