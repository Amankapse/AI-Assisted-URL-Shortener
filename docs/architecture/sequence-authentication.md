# Authentication Sequence

## Register

1. Client posts email and password to `POST /api/v1/auth/register`.
2. `AuthService` normalizes email, validates password bounds, rejects duplicates, hashes the password with BCrypt, and creates an active `USER`.
3. API returns a safe user DTO.

## Login

1. Client posts credentials to `POST /api/v1/auth/login`.
2. `AuthService` validates the active user and BCrypt password match.
3. `JwtTokenService` issues an RS256 access token.
4. `RefreshTokenService` creates an opaque refresh token, stores only its SHA-256 digest, and returns the raw value once.
5. API returns the access token and sets the HttpOnly refresh cookie.

## Refresh

1. Client posts to `POST /api/v1/auth/refresh` with the `refresh_token` cookie and `X-XSRF-TOKEN`.
2. `RefreshTokenService` finds the token by digest, rejects expired tokens, rotates valid tokens, and revokes the old token.
3. If a revoked token is reused, the family is marked compromised, active family tokens are revoked, the response clears the cookie, and the client must log in again.
4. Successful refresh returns a new access token and replacement refresh cookie.

## Ownership

1. Protected URL endpoints require `Authorization: Bearer <token>`.
2. Spring Security validates RS256 signature, issuer, audience, expiration, and required claims.
3. The JWT principal is converted into `OwnerIdentity` through `SecurityCurrentOwnerProvider`.
4. URL services use the JWT `sub` UUID for repository ownership scope.
