# Production Environment Variables

Use placeholders only in documentation and committed files. Configure real values in Render environment settings or a secret manager.

| Variable | Required | Secret | Purpose | Example |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Yes | No | Activate production profile | `prod` |
| `PORT` | Render supplied | No | HTTP port used by Render | `10000` |
| `SPRING_DATASOURCE_URL` | Yes | No | Neon JDBC URL with SSL | `jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Yes | Yes | Neon database user | `<DB_USERNAME>` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Yes | Neon database password | `<DB_PASSWORD>` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | No | No | Hikari maximum pool size | `5` |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | No | No | Hikari minimum idle connections | `1` |
| `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | No | No | Hikari connection timeout in ms | `30000` |
| `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT` | No | No | Hikari idle timeout in ms | `300000` |
| `SPRING_DATASOURCE_HIKARI_MAX_LIFETIME` | No | No | Hikari max connection lifetime in ms | `900000` |
| `SPRING_DATA_REDIS_URL` | Yes | Yes | Render Key Value internal Redis/Valkey URL | `<RENDER_KEY_VALUE_INTERNAL_URL>` |
| `SPRING_REDIS_TIMEOUT` | No | No | Redis command timeout inherited by base config | `2s` |
| `APP_AUTH_ISSUER` | Yes | No | JWT issuer expected by resource server | `https://<SERVICE>.onrender.com` |
| `APP_AUTH_AUDIENCE` | Yes | No | JWT audience | `url-shortener-api` |
| `APP_AUTH_KEY_ID` | Yes | No | JWT JOSE `kid` value | `prod-key-1` |
| `APP_AUTH_PRIVATE_KEY_PEM` | Yes | Yes | PKCS8 RSA private key PEM content | `<MULTILINE_PKCS8_PRIVATE_KEY_PEM_WITH_ESCAPED_NEWLINES>` |
| `APP_AUTH_PUBLIC_KEY_PEM` | Yes | No | X.509 RSA public key PEM content | `<MULTILINE_X509_PUBLIC_KEY_PEM_WITH_ESCAPED_NEWLINES>` |
| `APP_AUTH_ACCESS_TOKEN_TTL` | No | No | Access-token lifetime | `15m` |
| `APP_AUTH_REFRESH_TOKEN_TTL` | No | No | Refresh-token lifetime | `14d` |
| `APP_AUTH_SECURE_COOKIES` | Yes | No | Secure refresh-cookie flag for HTTPS production | `true` |
| `APP_AUTH_ALLOWED_ORIGINS` | Yes | No | Explicit CORS allowlist for external clients/future split frontend; same-origin Angular does not depend on CORS | `https://ai-url-shortener-682u.onrender.com` |
| `APP_ANALYTICS_IP_HASH_PEPPER` | Yes | Yes | HMAC pepper for IP anonymization | `<STRONG_RANDOM_SECRET>` |
| `APP_ANALYTICS_PUBLISHER` | No | No | Analytics publisher mode: `local` or `outbox`; defaults to `local` | `local` |
| `APP_ANALYTICS_QUEUE_CAPACITY` | No | No | Analytics queue capacity | `1000` |
| `APP_ANALYTICS_BATCH_SIZE` | No | No | Analytics batch size | `100` |
| `APP_ANALYTICS_FLUSH_INTERVAL` | No | No | Analytics flush interval | `1s` |
| `APP_ANALYTICS_OFFER_TIMEOUT` | No | No | Analytics enqueue timeout | `10ms` |
| `APP_ANALYTICS_SHUTDOWN_FLUSH_TIMEOUT` | No | No | Shutdown flush timeout | `5s` |
| `APP_ANALYTICS_RETRY_COUNT` | No | No | Analytics persistence retries | `2` |
| `APP_ANALYTICS_TOP_LINKS_MAX` | No | No | Admin top-links cap | `25` |
| `APP_REDIRECT_CACHE_ENABLED` | No | No | Redirect cache toggle | `true` |
| `APP_REDIRECT_CACHE_TTL` | No | No | Eligible redirect cache TTL | `10m` |
| `APP_REDIRECT_CACHE_INELIGIBLE_TTL` | No | No | Disabled/expired/deleted/blocked cache TTL | `30s` |
| `APP_REDIRECT_CACHE_JITTER` | No | No | Redirect cache TTL jitter | `30s` |
| `APP_REDIRECT_CACHE_SINGLE_FLIGHT_TIMEOUT` | No | No | Single-flight wait timeout | `2s` |
| `APP_REDIRECT_CACHE_SINGLE_FLIGHT_CAPACITY` | No | No | In-flight key capacity | `1024` |
| `APP_RATE_LIMIT_ENABLED` | No | No | Rate-limit toggle | `true` |
| `APP_RATE_LIMIT_KEY_SALT` | Yes | Yes | Salt for hashed limiter keys | `<STRONG_RANDOM_SECRET>` |
| `APP_RATE_LIMIT_REDIS_TIMEOUT` | No | No | Redis timeout for limiter operations | `250ms` |
| `APP_RATE_LIMIT_REGISTRATION_LIMIT` | No | No | Registration limit | `5` |
| `APP_RATE_LIMIT_REGISTRATION_WINDOW` | No | No | Registration window | `1m` |
| `APP_RATE_LIMIT_LOGIN_LIMIT` | No | No | Login limit | `5` |
| `APP_RATE_LIMIT_LOGIN_WINDOW` | No | No | Login window | `1m` |
| `APP_RATE_LIMIT_REFRESH_LIMIT` | No | No | Refresh limit | `30` |
| `APP_RATE_LIMIT_REFRESH_WINDOW` | No | No | Refresh window | `1m` |
| `APP_RATE_LIMIT_URL_CREATE_LIMIT` | No | No | Authenticated URL creation limit | `60` |
| `APP_RATE_LIMIT_URL_CREATE_WINDOW` | No | No | URL creation window | `1m` |
| `APP_RATE_LIMIT_REDIRECT_LIMIT` | No | No | Public redirect limit | `600` |
| `APP_RATE_LIMIT_REDIRECT_WINDOW` | No | No | Redirect window | `1m` |
| `APP_RATE_LIMIT_ADMIN_ANALYTICS_LIMIT` | No | No | Admin analytics limit | `120` |
| `APP_RATE_LIMIT_ADMIN_ANALYTICS_WINDOW` | No | No | Admin analytics window | `1m` |
| `SHORTENER_CODE_LENGTH` | No | No | Generated Base62 code length | `8` |
| `SHORTENER_CODE_MAX_RETRIES` | No | No | Bounded generation retries | `5` |
| `SHORTENER_QUOTA_ENABLED` | No | No | URL quota toggle | `true` |
| `SHORTENER_QUOTA_DAILY_CREATIONS_PER_USER` | No | No | Daily URL creations per user | `10000` |
| `SHORTENER_QUOTA_MAX_ACTIVE_LINKS_PER_USER` | No | No | Max active links per user | `100000` |
| `SHORTENER_QUOTA_DAILY_CUSTOM_ALIASES_PER_USER` | No | No | Daily custom aliases per user | `1000` |
| `APP_AUDIT_METADATA_MAX_BYTES` | No | No | Maximum serialized safe audit metadata size | `4096` |
| `APP_AUDIT_RETENTION` | No | No | Documented audit retention horizon; no destructive purge job is implemented | `3650d` |
| `APP_API_KEY_HASH_PEPPER` | Yes | Yes | HMAC pepper for one-way API-key digests | `<STRONG_RANDOM_SECRET>` |
| `APP_API_KEY_DEFAULT_EXPIRY` | No | No | Default machine API-key expiry when omitted | `90d` |
| `APP_API_KEY_MAX_EXPIRY` | No | No | Maximum allowed machine API-key lifetime | `365d` |
| `APP_API_KEY_LAST_USED_UPDATE_INTERVAL` | No | No | Minimum interval between last-used database updates | `15m` |
| `APP_API_KEY_MAX_HEADER_LENGTH` | No | No | Maximum accepted `X-API-Key` header length | `256` |
| `APP_RATE_LIMIT_API_KEY_REQUESTS` | No | No | Per-key machine API request limit | `600` |
| `APP_RATE_LIMIT_API_KEY_WINDOW` | No | No | Per-key machine API request window | `1m` |
| `APP_OUTBOX_ENABLED` | No | No | Enable transactional outbox dispatcher | `true` |
| `APP_OUTBOX_BATCH_SIZE` | No | No | Dispatcher claim batch size | `25` |
| `APP_OUTBOX_POLL_INTERVAL` | No | No | Dispatcher poll interval | `1s` |
| `APP_OUTBOX_WORKERS` | No | No | Dispatcher worker count | `1` |
| `APP_OUTBOX_MAX_ATTEMPTS` | No | No | Retry attempts before dead-lettering | `5` |
| `APP_OUTBOX_BASE_BACKOFF` | No | No | Initial retry backoff | `1s` |
| `APP_OUTBOX_MAX_BACKOFF` | No | No | Maximum retry backoff | `1m` |
| `APP_OUTBOX_CLAIM_TIMEOUT` | No | No | Stale claim recovery timeout | `5m` |
| `APP_OUTBOX_RETENTION` | No | No | Processed outbox retention before bounded cleanup | `7d` |
| `APP_OUTBOX_CLEANUP_INTERVAL` | No | No | Processed-row cleanup interval | `1h` |
| `APP_OUTBOX_CLEANUP_BATCH_SIZE` | No | No | Processed-row cleanup batch size | `500` |
| `APP_OUTBOX_MAX_PAYLOAD_BYTES` | No | No | Maximum serialized outbox payload size | `8192` |
| `APP_OUTBOX_SHUTDOWN_GRACE_PERIOD` | No | No | Dispatcher shutdown wait period | `10s` |
| `APP_OUTBOX_METRICS_REFRESH_INTERVAL` | No | No | Outbox gauge refresh interval | `30s` |
| `SERVER_MAX_HTTP_FORM_POST_SIZE` | No | No | Tomcat form body limit | `2MB` |
| `SERVER_MAX_SWALLOW_SIZE` | No | No | Tomcat swallow size limit | `2MB` |
| `JAVA_TOOL_OPTIONS` | No | No | JVM heap/GC options for small container | `-Xms64m -Xmx320m -XX:+UseG1GC` |

## Frontend Public Build Variables

These variables are used by `npm run build:render` to write `dist/frontend/browser/app-config.json` before the Angular files are packaged into the Spring Boot JAR. They are browser-visible and must not contain secrets.

| Variable | Required | Secret | Purpose | Example |
| --- | --- | --- | --- | --- |
| `FRONTEND_API_BASE_URL` | Yes | No | Public Spring Boot API origin used by Angular `HttpClient`; empty means same-origin `/api/v1/...` | empty string |
| `FRONTEND_PUBLIC_SHORT_URL_BASE` | Yes | No | Public redirect base shown in frontend runtime config | `https://ai-url-shortener-682u.onrender.com` |
| `FRONTEND_ENVIRONMENT` | No | No | Runtime environment label | `production` |

`APP_PUBLIC_BASE_URL` belongs to the backend and controls generated `shortUrl` values. Keep it pointed at the backend redirect origin while redirects are served by `/r/{shortCode}` on the Spring Boot service.
