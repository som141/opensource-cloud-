# Auth API

## Purpose

Auth API supports Google OAuth login, short-lived JWT access tokens, HttpOnly refresh-token cookies, and current-user
lookup for protected APIs.

## Environment Injection

Runtime values are injected through environment variables. Use `backend-api/.env.example` as the committed template and
create an ignored local file such as `backend-api/.env` for real values.

Required local values:

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:5173/oauth2/success

DB_URL=jdbc:postgresql://localhost:5432/image_preprocess
DB_USERNAME=postgres
DB_PASSWORD=postgres
JPA_DDL_AUTO=update

JWT_SECRET=<at-least-32-byte-secret>
ACCESS_TOKEN_EXPIRE_SECONDS=1800
REFRESH_TOKEN_EXPIRE_SECONDS=1209600
REFRESH_TOKEN_COOKIE_NAME=refresh_token
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax

CORS_ALLOWED_ORIGINS=http://localhost:5173
RABBIT_HEALTH_ENABLED=false
```

## Google OAuth Flow

1. Browser opens `GET /oauth2/authorization/google`.
2. Google redirects to `GET /login/oauth2/code/google`.
3. Spring Security validates the OAuth response.
4. Backend creates or finds `User`.
5. Backend links `SocialAccount` for Google.
6. Backend creates an access token and refresh token.
7. Backend stores a hashed refresh token in DB.
8. Backend sets the raw refresh token as an HttpOnly cookie.
9. Backend redirects to `OAUTH2_SUCCESS_REDIRECT_URI` with the access token as a query parameter.

Local Google Console redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/oauth2/authorization/google` | Public | Start Google OAuth login |
| `GET` | `/login/oauth2/code/google` | Public | Google OAuth callback |
| `GET` | `/api/v1/auth/me` | Bearer access token | Read current user |
| `POST` | `/api/v1/auth/refresh` | Refresh cookie | Rotate refresh token and issue a new access token |
| `POST` | `/api/v1/auth/logout` | Refresh cookie | Revoke refresh token and expire cookie |

## `GET /api/v1/auth/me`

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "id": 1,
    "email": "moonju@example.com",
    "name": "MoonJu",
    "profileImageUrl": "https://example.com/profile.png",
    "role": "USER",
    "providers": ["GOOGLE"]
  }
}
```

## `POST /api/v1/auth/refresh`

The request uses the `refresh_token` HttpOnly cookie. The response body includes the new access token only. The new
refresh token is returned as a rotated HttpOnly cookie.

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresAt": "2026-05-03T09:00:00Z",
    "refreshToken": null
  }
}
```

## `POST /api/v1/auth/logout`

The request uses the `refresh_token` HttpOnly cookie. Backend revokes the persisted refresh token and expires the
browser cookie.

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "loggedOut": true
  }
}
```

## Protected API Usage

Use the access token with the `Authorization` header:

```text
Authorization: Bearer <access-token>
```

Controllers can resolve the current user ID with:

```java
public ApiResponse<?> handler(@CurrentUser Long currentUserId) {
    ...
}
```

## Security Notes

- Real secrets must never be committed.
- Refresh tokens are stored only as SHA-256 hashes.
- Access tokens are short-lived.
- Local `REFRESH_TOKEN_COOKIE_SECURE=false` is for HTTP development only.
- Production must set `REFRESH_TOKEN_COOKIE_SECURE=true` and use HTTPS.
