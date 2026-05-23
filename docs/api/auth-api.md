# 인증 API

## 목적

인증 API는 Google OAuth 로그인, 짧은 수명의 JWT Access Token, HttpOnly Refresh Token Cookie, 현재 사용자 조회를 제공합니다.
토큰 발급, 재발급, 로그아웃의 자세한 흐름은 [인증 토큰 흐름](auth-token-flow.md)을 참고합니다.

## 환경변수 주입

실제 값은 환경변수로 주입합니다. 저장소에는 `backend-api/.env.example`만 올리고, 실제 값은 `backend-api/.env`처럼 Git에서 제외된 파일에 둡니다.

필수 로컬 값:

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

## Google OAuth 로그인 흐름

1. 브라우저가 `GET /oauth2/authorization/google`로 이동합니다.
2. Google 인증 후 `GET /login/oauth2/code/google`로 callback이 들어옵니다.
3. Spring Security가 OAuth 응답을 검증합니다.
4. backend-api가 `User`를 찾거나 생성합니다.
5. backend-api가 Google `SocialAccount`를 연결합니다.
6. backend-api가 Access Token과 Refresh Token을 발급합니다.
7. Refresh Token은 hash로 DB에 저장합니다.
8. 원본 Refresh Token은 HttpOnly Cookie로 내려줍니다.
9. 브라우저는 `OAUTH2_SUCCESS_REDIRECT_URI`로 돌아갑니다.
10. 프론트는 refresh API를 호출해 Access Token을 받습니다.

로컬 Google Console redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

## Endpoint

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/oauth2/authorization/google` | 공개 | Google OAuth 로그인 시작 |
| `GET` | `/login/oauth2/code/google` | 공개 | Google OAuth callback |
| `GET` | `/api/v1/auth/me` | Bearer Access Token | 현재 사용자 조회 |
| `POST` | `/api/v1/auth/refresh` | Refresh Cookie | Refresh Token 회전과 새 Access Token 발급 |
| `POST` | `/api/v1/auth/logout` | Refresh Cookie | Refresh Token 폐기와 Cookie 만료 |

## 현재 사용자 조회

```text
GET /api/v1/auth/me
```

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
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

## Access Token 재발급

```text
POST /api/v1/auth/refresh
```

요청은 `refresh_token` HttpOnly Cookie를 사용합니다. 응답 body에는 새 Access Token만 포함하고, 새 Refresh Token은 회전된 HttpOnly Cookie로 내려줍니다.

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresAt": "2026-05-03T09:00:00Z",
    "refreshToken": null
  }
}
```

## 로그아웃

```text
POST /api/v1/auth/logout
```

요청은 `refresh_token` HttpOnly Cookie를 사용합니다. backend-api는 DB에 저장된 Refresh Token을 폐기하고 브라우저 Cookie를 만료시킵니다.

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "loggedOut": true
  }
}
```

## 보호 API 호출 방식

Access Token은 `Authorization` header에 넣습니다.

```text
Authorization: Bearer <access-token>
```

Controller에서는 `@CurrentUser`로 현재 사용자 ID를 받을 수 있습니다.

```java
public ApiResponse<?> handler(@CurrentUser Long currentUserId) {
    ...
}
```

## 보안 기준

- 실제 secret은 Git에 커밋하지 않습니다.
- Refresh Token은 SHA-256 hash로만 DB에 저장합니다.
- Access Token은 짧게 유지합니다.
- 로컬 HTTP 개발 환경에서만 `REFRESH_TOKEN_COOKIE_SECURE=false`를 사용합니다.
- 운영에서는 반드시 `REFRESH_TOKEN_COOKIE_SECURE=true`와 HTTPS를 사용합니다.
