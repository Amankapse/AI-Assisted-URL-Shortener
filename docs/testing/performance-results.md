# Performance Results

## Measured local results

k6 was not executed because `k6 version` failed with `The term 'k6' is not recognized`. No throughput, latency, or error-rate values are claimed for final validation.

Available environment details:

- Java: `21.0.10`
- Docker server: `29.6.1`
- Application instances: not started for load testing
- PostgreSQL/Redis: Testcontainers used during Maven validation; Compose config uses `postgres:15-alpine` and `redis:7-alpine`
- CPU/RAM: unavailable; Windows WMI queries were denied in this execution environment
- Virtual users/duration/requests/throughput/P50/P95/P99/error rate: not measured because k6 is not installed

Do not treat local laptop smoke results as enterprise capacity claims.
