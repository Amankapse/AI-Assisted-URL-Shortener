# Runtime Configuration

The SPA is environment-independent. The same build can be promoted by replacing `app-config.json`.

Required keys:

| Key | Required | Secret | Purpose |
| --- | --- | --- | --- |
| `apiBaseUrl` | Yes | No | Spring Boot API origin |
| `publicShortUrlBase` | Yes | No | Public redirect origin used for link display |
| `environment` | Yes | No | Human-readable environment label |

Example:

```json
{
  "apiBaseUrl": "https://ai-url-shortener-682u.onrender.com",
  "publicShortUrlBase": "https://ai-url-shortener-682u.onrender.com",
  "environment": "production"
}
```

Only HTTP and HTTPS URLs are accepted. Startup fails with a clear error if required config is missing or malformed.
