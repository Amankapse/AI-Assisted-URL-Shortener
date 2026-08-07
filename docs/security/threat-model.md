# Threat Model

| Threat | Impact | Mitigation | Residual risk |
| --- | --- | --- | --- |
| Credential stuffing | Account compromise | BCrypt, generic login failures, Redis login limiter | Distributed low-rate attacks |
| Brute force registration/login | Abuse and resource use | IP/account rate limits, validation | Proxy/IP reputation not implemented |
| Resource exhaustion through URL creation | Storage growth and cost | Rate limits plus config-driven URL quotas | Current quota implementation uses aggregate PostgreSQL counts, not distributed counters |
| IDOR | Cross-user data access | JWT `sub` ownership in services/repositories, no owner IDs in DTOs | Bugs in future admin APIs |
| JWT tampering | Unauthorized access | RS256 only, issuer/audience/expiry/claim validation | Key compromise |
| Refresh-token theft/reuse | Session hijack | HttpOnly Secure SameSite cookie, rotation, reuse family revocation | Client compromise |
| CSRF | Cookie refresh/logout abuse | CSRF required for refresh/logout, bearer APIs stateless | Misconfigured clients |
| CORS abuse | Browser credential leakage | Explicit allowlist, no wildcard credentials | Bad allowed-origin config |
| SSRF via destination URL | Internal network access | URL validation restricts schemes and rejects private/link-local hosts | DNS rebinding not fully solved |
| Open redirect | Intended product behavior abused | API clearly models redirects; destination validation | Users can still create misleading links |
| Phishing or malware links | User harm and platform abuse | Administrative block/unblock moderation and safe blocked redirect response | External reputation provider not implemented |
| Malicious aliases | Route collision/confusion | Alias pattern and reserved-route validation | Future route additions need reserved-list review |
| SQL injection | Data compromise | JPA/JdbcTemplate parameters, no dynamic user SQL | Future native queries |
| Redis abuse/key leakage | Operational impact | Namespaced hashed limiter keys, no auth data cached, bounded TTLs | Redis credentials/network hardening outside app |
| Cache poisoning | Wrong redirects | Versioned DTO validation, PostgreSQL fallback, after-commit invalidation | Compromised Redis |
| Hot-link abuse | Origin overload from viral shortcode | Redis cache-aside, TTL jitter, single-flight within one JVM, rate limits | CDN/edge caching and distributed hot-key mitigation are architecture-only |
| Analytics leakage | Privacy exposure | HMAC IP hash, referrer host only, UA category only, no raw headers/tokens | Weak/missing production pepper blocked in prod |
| Log injection | Log integrity issues | Correlation ID strict format, no raw tokens/passwords | Unsafe future log statements |
| Denial of service | Availability loss | Rate limits, request size limits, bounded queues, pagination max | Volumetric DDoS needs infrastructure controls |
| Internet-scale perimeter bypass | Direct origin abuse | Production architecture requires WAF, gateway, trusted proxy controls, CDN/edge | Not implemented in local prototype |
| Oversized requests | Memory pressure | Tomcat form/swallow limits and DTO limits | Container/proxy limits also required |
| Enumeration | User/account discovery | Generic login failures and generic rate-limit response | Registration duplicate still reveals registered email |
| Admin privilege abuse | Data exposure | Explicit admin endpoints, no admin bypass on user APIs | Admin account compromise |
| Actuator exposure | Secret/config disclosure | Only health/info/metrics exposed; metrics admin-protected | Misconfigured security profiles |
