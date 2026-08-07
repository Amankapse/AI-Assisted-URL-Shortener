# Observability

## Actuator exposure

Exposed endpoints:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`

Health endpoints are public for platform probes. Metrics are exposed for this assessment but are protected by `ROLE_ADMIN` in the application security rules. Sensitive Actuator endpoints such as `env`, `configprops`, `heapdump`, `threaddump`, `beans`, `mappings`, `loggers`, and `conditions` are not exposed.

## Health policy

Liveness indicates whether the JVM/application is alive and does not depend on PostgreSQL or Redis.

Readiness requires PostgreSQL because PostgreSQL is the source of truth. Redis is excluded from readiness because redirect cache and rate-limit failures have documented fallback behavior and Redis is not required for correctness.

Redis health is not part of Actuator health status. Redis degradation is observed through cache and rate-limit failure metrics.

## Metrics

Application meters use bounded tags only:

- `url_shortener.urls{operation, outcome, reason}`
- `url_shortener.redirects{outcome}`
- `url_shortener.redis.cache{operation, outcome}`
- `url_shortener.redis.singleflight{outcome}`
- `url_shortener.analytics.events{outcome}`
- `url_shortener.analytics.queue.depth`
- `url_shortener.analytics.batch.persistence`
- `url_shortener.analytics.batch.size`
- `url_shortener.auth{operation, outcome}`
- `url_shortener.rate_limit{limiter, outcome}`

Forbidden high-cardinality tags include user ID, URL ID, short code, email, IP address, token ID, and exception message.

## Correlation IDs

`X-Correlation-ID` is accepted only when it is a UUID or bounded safe identifier matching `[A-Za-z0-9._-]{1,64}`. Invalid or missing IDs are replaced with a generated UUID. The selected ID is stored in request context and MDC, returned in the response header, and included in Problem Details. MDC is cleared at request completion.
