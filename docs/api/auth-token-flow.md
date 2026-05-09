# 인증 흐름과 토큰 처리 상세 문서

## 목적

이 문서는 현재 `backend-api`에 구현된 Google OAuth 인증 흐름을 코드 기준으로 설명한다.

중점은 다음이다.

- 사용자가 Google 로그인을 누른 뒤 어떤 과정으로 서비스 사용자로 식별되는지
- access token이 언제 만들어지고 어디로 전달되는지
- refresh token이 언제 만들어지고 어디에 저장되는지
- refresh token rotation이 어떻게 동작하는지
- logout 시 access token과 refresh token이 각각 어떻게 처리되는지
- 보호 API에서 `@CurrentUser`가 어떻게 동작하는지

현재 인증 구현은 Google 로그인만 지원한다. Kakao 로그인은 제외되어 있다.

## 관련 클래스

| 영역 | 클래스 | 책임 |
| --- | --- | --- |
| 보안 설정 | `infra.security.SecurityConfig` | 공개 경로, 보호 경로, OAuth success handler, JWT filter 설정 |
| Google 사용자 정보 변환 | `infra.oauth.GoogleOAuth2UserInfo` | Google 응답의 `sub`, `email`, `name`, `picture` 추출 |
| OAuth 사용자 정보 factory | `infra.oauth.OAuth2UserInfoFactory` | `registrationId` 기준 provider adapter 선택 |
| 로그인 처리 | `domain.auth.service.AuthService` | 사용자 조회/생성, 소셜 계정 연결, 토큰 발급 요청 |
| OAuth 성공 처리 | `domain.auth.service.OAuth2LoginSuccessHandler` | 로그인 성공 후 refresh cookie 설정, access token redirect |
| access token | `infra.security.JwtTokenProvider` | JWT 생성과 검증 |
| refresh token | `domain.auth.service.TokenService` | refresh token 생성, hash 저장, rotation, revoke |
| refresh cookie | `domain.auth.service.RefreshTokenCookieService` | HttpOnly cookie 생성, 조회, 만료 |
| 요청 인증 | `infra.security.JwtAuthenticationFilter` | `Authorization: Bearer` 검증 후 SecurityContext 설정 |
| 현재 사용자 주입 | `infra.security.CurrentUserArgumentResolver` | `@CurrentUser Long currentUserId` 해석 |

## 공개 경로와 보호 경로

`SecurityConfig`에서 다음 경로는 access token 없이 접근 가능하다.

```text
/
/error
/actuator/health
/actuator/info
/oauth2/**
/login/oauth2/**
/api/v1/auth/refresh
/api/v1/auth/logout
/swagger-ui/**
/v3/api-docs/**
```

그 외 요청은 인증이 필요하다.

보호 API는 보통 다음 헤더를 사용한다.

```text
Authorization: Bearer <access-token>
```

## 전체 로그인 흐름

### 1. 사용자가 Google 로그인 시작

브라우저가 다음 주소로 이동한다.

```text
GET /oauth2/authorization/google
```

Spring Security OAuth2 Client가 `application.yml`의 Google client 설정을 사용해 Google 인증 화면으로 redirect한다.

로컬 Google Console에 등록해야 하는 redirect URI는 다음이다.

```text
http://localhost:8080/login/oauth2/code/google
```

### 2. Google 인증 완료 후 backend callback

사용자가 Google 인증을 완료하면 Google이 backend로 redirect한다.

```text
GET /login/oauth2/code/google
```

Spring Security가 OAuth 응답을 검증하고 `OAuth2AuthenticationToken`을 만든다.

### 3. OAuth 성공 handler 실행

로그인이 성공하면 `OAuth2LoginSuccessHandler.onAuthenticationSuccess()`가 실행된다.

핵심 코드는 다음 흐름이다.

```java
OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
OAuth2User oauthUser = oauthToken.getPrincipal();

IssuedTokenPair tokenPair = authService.loginWithOAuth2(
    oauthToken.getAuthorizedClientRegistrationId(),
    oauthUser.getAttributes()
);
```

여기서 `registrationId`는 `google`이고, `oauthUser.getAttributes()`에는 Google 사용자 정보가 들어 있다.

### 4. Google 사용자 정보 추출

`OAuth2UserInfoFactory`는 `registrationId`가 `google`이면 `GoogleOAuth2UserInfo`를 사용한다.

현재 Google 응답 필드는 다음처럼 내부 값으로 변환된다.

| Google field | 내부 의미 |
| --- | --- |
| `sub` | Google provider user id |
| `email` | 사용자 이메일 |
| `name` | 사용자 이름. 없으면 email 사용 |
| `picture` | 프로필 이미지 URL |

지원하지 않는 provider가 들어오면 `OAuth2LoginFailedException`이 발생한다.

### 5. 서비스 사용자 조회 또는 생성

`AuthService.findOrCreateUser()`는 다음 순서로 사용자를 찾는다.

1. `social_accounts`에서 `provider = GOOGLE`, `provider_user_id = Google sub`로 기존 소셜 계정을 찾는다.
2. 소셜 계정이 없으면 `users.email`로 기존 사용자를 찾는다.
3. 이메일 사용자도 없으면 새 `User`를 만든다.

새 사용자는 다음 상태로 생성된다.

```text
role = USER
status = ACTIVE
```

그 다음 `AuthService.linkSocialAccountIfNeeded()`가 Google 소셜 계정 연결이 없으면 `social_accounts` row를 만든다.

관련 테이블은 다음이다.

```text
users
social_accounts
```

## 토큰 발급 흐름

사용자 조회/생성이 끝나면 `AuthService`가 다음을 호출한다.

```java
tokenService.issue(user)
```

여기서 access token과 refresh token이 동시에 발급된다.

두 토큰은 목적과 저장 방식이 다르다.

| 토큰 | 목적 | 저장 위치 |
| --- | --- | --- |
| access token | 보호 API 호출 인증 | DB 저장 안 함. frontend로 전달 |
| refresh token | 새 access token 발급 | 원문은 cookie, hash는 DB |

## Access Token 처리

### access token의 성격

access token은 짧게 살아 있는 JWT다.

기본 만료 시간은 다음 설정을 따른다.

```text
ACCESS_TOKEN_EXPIRE_SECONDS=1800
```

기본값 기준 30분이다.

### access token 생성

`JwtTokenProvider.createAccessToken(user)`가 HMAC-SHA256 JWT를 만든다.

JWT header는 다음 형태다.

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

JWT payload에는 다음 claim이 들어간다.

```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1770000000,
  "exp": 1770001800
}
```

| claim | 의미 |
| --- | --- |
| `sub` | 내부 `users.id` |
| `email` | 사용자 이메일 |
| `role` | 사용자 권한 |
| `iat` | 발급 시각 epoch seconds |
| `exp` | 만료 시각 epoch seconds |

### access token 전달 방식

로그인 성공 후 backend는 access token을 frontend redirect URL의 query parameter로 전달한다.

```text
{OAUTH2_SUCCESS_REDIRECT_URI}?accessToken=<jwt>&accessTokenExpiresAt=<instant>
```

로컬 기본 redirect URI는 다음이다.

```text
http://localhost:5173/oauth2/success
```

즉, Google login 완료 후 최종적으로 브라우저는 다음과 유사한 주소로 이동한다.

```text
http://localhost:5173/oauth2/success?accessToken=<jwt>&accessTokenExpiresAt=2026-05-08T10:00:00Z
```

backend는 access token을 DB에 저장하지 않는다.

frontend는 이 access token을 읽어서 이후 보호 API 호출에 사용해야 한다. 현재 backend 코드는 frontend가 이 값을 memory,
session storage, local storage 중 어디에 보관할지 강제하지 않는다.

### 보호 API에서 access token 사용

보호 API 호출 시 frontend는 다음 헤더를 붙인다.

```text
Authorization: Bearer <access-token>
```

`JwtAuthenticationFilter`는 모든 요청에서 다음을 수행한다.

1. `Authorization` header를 읽는다.
2. 값이 `Bearer `로 시작하는지 확인한다.
3. `Bearer ` 뒤의 JWT 문자열만 꺼낸다.
4. `JwtTokenProvider.parseAccessToken()`에 넘긴다.
5. JWT 구조, 서명, 만료 시간을 검증한다.
6. 검증 성공 시 `CustomUserPrincipal`을 만든다.
7. `SecurityContextHolder`에 인증 객체를 저장한다.

이후 controller에서는 다음처럼 현재 사용자 ID를 받을 수 있다.

```java
public ApiResponse<?> handler(@CurrentUser Long currentUserId) {
    ...
}
```

### access token이 잘못된 경우

다음 경우 `JwtTokenProvider.parseAccessToken()`은 인증 실패로 처리한다.

- JWT 구조가 3-part가 아닌 경우
- 서명이 맞지 않는 경우
- `exp`가 지난 경우
- claim 파싱 중 오류가 난 경우

이 경우 filter는 SecurityContext를 채우지 않는다.

요청 경로가 보호 API라면 Spring Security가 인증되지 않은 요청으로 거부한다.

### logout 시 access token 처리

현재 구현은 access token을 서버에 저장하지 않는다.

따라서 logout 시 이미 발급된 access token을 DB에서 삭제하거나 blacklist에 넣지 않는다.

대신 access token은 짧은 만료 시간에 의해 자연스럽게 만료된다.

## Refresh Token 처리

### refresh token의 성격

refresh token은 access token을 새로 발급받기 위한 긴 수명의 랜덤 토큰이다.

기본 만료 시간은 다음 설정을 따른다.

```text
REFRESH_TOKEN_EXPIRE_SECONDS=1209600
```

기본값 기준 14일이다.

### refresh token 생성

`TokenService.createRefreshToken(userId)`가 refresh token을 만든다.

동작은 다음 순서다.

1. `SecureRandom`으로 48 bytes를 생성한다.
2. bytes를 hex 문자열로 바꾼다.
3. 이 문자열이 raw refresh token이다.
4. raw refresh token을 SHA-256으로 hash한다.
5. DB에는 hash만 저장한다.
6. raw refresh token은 caller에게 반환한다.

중요한 점은 DB에 raw refresh token 원문을 저장하지 않는다는 것이다.

### refresh token DB 저장

DB에는 다음 테이블에 hash만 저장된다.

```text
refresh_tokens
```

주요 컬럼은 다음이다.

| 컬럼 | 의미 |
| --- | --- |
| `user_id` | 토큰 소유자 |
| `token_hash` | raw refresh token의 SHA-256 hash |
| `expires_at` | refresh token 만료 시각 |
| `revoked_at` | revoke된 시각. null이면 아직 revoke 안 됨 |

`token_hash`에는 unique constraint가 있다.

### refresh token cookie 저장

브라우저에는 raw refresh token이 HttpOnly cookie로 내려간다.

```text
Set-Cookie: refresh_token=<raw-refresh-token>; HttpOnly; Path=/; SameSite=Lax; Max-Age=1209600
```

cookie 설정은 다음 환경 변수를 따른다.

```text
REFRESH_TOKEN_COOKIE_NAME=refresh_token
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

로컬 개발에서는 HTTP를 쓰기 때문에 `secure=false`다.

운영에서는 HTTPS를 전제로 `REFRESH_TOKEN_COOKIE_SECURE=true`가 필요하다.

### 왜 refresh token은 cookie로 두는가

현재 구조에서 refresh token은 JSON body로 frontend JavaScript에 직접 넘기지 않는다.

대신 HttpOnly cookie에 넣는다.

목적은 다음이다.

- JavaScript가 refresh token 값을 직접 읽지 못하게 한다.
- XSS가 발생했을 때 refresh token 원문 탈취 가능성을 낮춘다.
- frontend는 refresh token 값을 몰라도 `/api/v1/auth/refresh`를 호출할 수 있다.

## OAuth 로그인 성공 응답

Google 로그인 성공 후 backend 응답은 두 가지를 동시에 한다.

1. `Set-Cookie`로 raw refresh token을 HttpOnly cookie에 저장한다.
2. frontend success URL로 redirect하면서 access token을 query parameter로 넘긴다.

결과적으로 브라우저는 다음 상태가 된다.

```text
브라우저 cookie:
refresh_token=<raw-refresh-token>; HttpOnly

브라우저 주소:
http://localhost:5173/oauth2/success?accessToken=<jwt>&accessTokenExpiresAt=<instant>
```

## Access Token 재발급 흐름

frontend는 access token이 만료되었거나 만료에 가까워졌을 때 다음 API를 호출한다.

```text
POST /api/v1/auth/refresh
```

요청 body에 refresh token을 넣지 않는다.

브라우저가 `refresh_token` cookie를 자동으로 보낸다.

### backend 처리 순서

`AuthController.refresh()`는 다음을 수행한다.

1. `RefreshTokenCookieService.resolveRefreshToken()`으로 cookie에서 raw refresh token을 꺼낸다.
2. `authService.refresh(refreshToken)`을 호출한다.

`TokenService.refresh()`는 다음을 수행한다.

1. raw refresh token을 SHA-256 hash한다.
2. `refresh_tokens.token_hash`로 DB row를 찾는다.
3. row가 없으면 실패 처리한다.
4. `revoked_at`이 있으면 실패 처리한다.
5. `expires_at`이 지났으면 실패 처리한다.
6. 기존 refresh token을 revoke한다.
7. 사용자 정보를 다시 조회한다.
8. 새 access token과 새 refresh token을 발급한다.

`AuthController.refresh()`는 마지막으로 다음을 수행한다.

1. 새 raw refresh token을 새 HttpOnly cookie로 내려준다.
2. 새 access token을 response body에 담는다.
3. response body에서는 refresh token 값을 제거한다.

응답 body 예시는 다음이다.

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresAt": "2026-05-08T10:00:00Z",
    "refreshToken": null
  }
}
```

응답 header에는 새 cookie가 포함된다.

```text
Set-Cookie: refresh_token=<new-raw-refresh-token>; HttpOnly; Path=/; SameSite=Lax; Max-Age=1209600
```

### refresh token rotation

현재 구현은 refresh token rotation 방식을 사용한다.

즉, refresh API가 성공하면 기존 refresh token은 즉시 revoke되고, 새 refresh token이 발급된다.

따라서 한 번 성공적으로 사용된 refresh token은 다시 사용할 수 없다.

## Logout 흐름

frontend는 로그아웃 시 다음 API를 호출한다.

```text
POST /api/v1/auth/logout
```

이 요청도 body에 refresh token을 넣지 않는다.

브라우저가 `refresh_token` cookie를 자동으로 보낸다.

### backend 처리 순서

`AuthController.logout()`은 다음을 수행한다.

1. cookie에서 raw refresh token을 꺼낸다.
2. `authService.logout(refreshToken)`을 호출한다.
3. refresh token cookie를 만료시키는 `Set-Cookie`를 내려준다.
4. `loggedOut=true`를 응답한다.

`TokenService.revoke()`는 다음을 수행한다.

1. raw refresh token을 SHA-256 hash한다.
2. DB에서 hash가 같은 refresh token을 찾는다.
3. 찾으면 `revoked_at`을 설정한다.
4. 못 찾으면 별도 오류 없이 종료한다.

응답 body 예시는 다음이다.

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

응답 header는 cookie를 삭제하도록 내려간다.

```text
Set-Cookie: refresh_token=; HttpOnly; Path=/; SameSite=Lax; Max-Age=0
```

로그아웃 후 refresh token은 더 이상 사용할 수 없다.

이미 발급된 access token은 서버에 저장되어 있지 않으므로 즉시 폐기되지는 않는다. 대신 짧은 만료 시간까지 기다려 자연 만료된다.

## 내 정보 조회 흐름

frontend는 현재 로그인 사용자를 확인하기 위해 다음 API를 호출한다.

```text
GET /api/v1/auth/me
Authorization: Bearer <access-token>
```

backend 처리 순서는 다음이다.

1. `JwtAuthenticationFilter`가 access token을 검증한다.
2. 검증 성공 시 `SecurityContextHolder`에 `CustomUserPrincipal`을 저장한다.
3. `CurrentUserArgumentResolver`가 `@CurrentUser Long currentUserId`에 사용자 ID를 주입한다.
4. `AuthService.me()`가 DB에서 사용자를 조회한다.
5. 연결된 social provider 목록을 조회한다.
6. 사용자 정보를 응답한다.

응답 예시는 다음이다.

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

## 전체 시퀀스

```text
Browser
  -> GET /oauth2/authorization/google
  -> Google login page
  -> GET /login/oauth2/code/google

Spring Security
  -> Google OAuth response 검증
  -> OAuth2LoginSuccessHandler 실행

AuthService
  -> Google user info 변환
  -> SocialAccount 조회
  -> User email 조회
  -> 없으면 User 생성
  -> Google SocialAccount 연결

TokenService
  -> JWT access token 생성
  -> raw refresh token 생성
  -> refresh token SHA-256 hash DB 저장

Backend response
  -> Set-Cookie: refresh_token=<raw>; HttpOnly
  -> frontend success URL로 redirect
  -> redirect query에 accessToken 포함

Frontend
  -> access token을 읽어서 보호 API 호출에 사용
  -> Authorization: Bearer <access-token>
  -> access token 만료 시 /api/v1/auth/refresh 호출
  -> logout 시 /api/v1/auth/logout 호출
```

## 토큰 처리 요약

| 항목 | Access token | Refresh token |
| --- | --- | --- |
| 형식 | JWT HS256 | 48 bytes random hex string |
| 기본 수명 | 30분 | 14일 |
| frontend 전달 | redirect query, refresh response body | HttpOnly cookie |
| DB 저장 | 저장하지 않음 | SHA-256 hash만 저장 |
| API 사용 방식 | `Authorization: Bearer` header | browser cookie 자동 전송 |
| refresh 시 | 새 access token 발급 | 기존 token revoke 후 새 token 발급 |
| logout 시 | 즉시 폐기하지 않음, 만료 대기 | DB revoke 및 cookie 만료 |

## 보안상 중요한 결정

- Google provider만 지원한다.
- access token은 짧게 유지하고 DB에 저장하지 않는다.
- refresh token 원문은 DB에 저장하지 않고 SHA-256 hash만 저장한다.
- refresh token은 HttpOnly cookie로만 전달한다.
- `/api/v1/auth/refresh` 성공 시 refresh token rotation을 수행한다.
- `/api/v1/auth/logout`은 refresh token을 revoke하고 cookie를 만료시킨다.
- 운영 환경에서는 HTTPS와 `REFRESH_TOKEN_COOKIE_SECURE=true`가 필요하다.
- OAuth client secret, JWT secret 같은 실제 secret은 Git에 올리지 않는다.

## 현재 한계와 추후 보완점

- access token blacklist가 없어서 logout 직후 access token 즉시 무효화는 하지 않는다.
- 만료된 refresh token row를 정리하는 scheduled cleanup은 아직 없다.
- 다중 provider 계정 연결은 아직 없다.
- Kakao login은 의도적으로 제외되어 있다.
- frontend의 access token 저장 위치는 backend에서 강제하지 않는다.
- cross-origin 환경에서는 refresh/logout 요청 시 cookie 전달 설정을 frontend와 CORS 정책에서 맞춰야 한다.

