# Rate Limiting

## Design

Rate limiting uses Redis with an atomic Lua fixed-window script through the existing `StringRedisTemplate`. No new production dependency is used.

Keys use:

```text
rl:v1:{limiter}:{sha256(salt + limiter-subject)}
```

Keys do not contain raw IP addresses, email addresses, tokens, URL IDs, short codes, or owner data. Redis keys have explicit TTLs equal to the configured limiter window.

## Policies

| Limiter | Subject | Failure mode |
| --- | --- | --- |
| `registration` | trusted servlet remote address | fail-closed |
| `login` | trusted servlet remote address + normalized email digest | fail-closed |
| `refresh` | trusted servlet remote address + refresh-token digest | fail-closed |
| `url-create` | authenticated user UUID | fail-open |
| `redirect` | trusted servlet remote address | fail-open |
| `admin-analytics` | authenticated admin UUID | fail-open |

The servlet/container remote address is the default trusted client IP. The application does not parse arbitrary `X-Forwarded-For` headers. Proxy support must be configured at the trusted proxy/container boundary.

## Response

Exceeded limits return HTTP 429 with RFC7807 `application/problem+json`, error code `rate_limit_exceeded`, correlation ID, and `Retry-After` when available. Responses do not disclose Redis keys, internal counters, thresholds, or account existence.
