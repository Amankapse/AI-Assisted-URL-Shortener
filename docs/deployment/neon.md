# Neon PostgreSQL

## Setup

1. Create a Neon project.
2. Create or select a database for the URL shortener.
3. Copy the database host, database name, username, and password from Neon.
4. Convert the connection details into a JDBC URL:

```text
jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require
```

5. Configure these Render environment variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require
SPRING_DATASOURCE_USERNAME=<DB_USERNAME>
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
```

Do not manually create the application schema. Flyway applies `V1` through `V4` on first production startup.

## Pool Sizing

The production profile uses conservative Hikari defaults for a small Neon/Render deployment:

```text
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=300000
SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=900000
```

Increase pool size only after checking Neon limits, application concurrency, and measured database wait time.

