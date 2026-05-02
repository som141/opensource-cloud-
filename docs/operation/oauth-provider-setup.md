# OAuth Provider Setup

## 목적

Google/Kakao 소셜 로그인을 실제로 검증하기 전에 필요한 외부 개발자 콘솔 설정과 환경변수를 정리한다.

## 현재 구현 단계

현재는 OAuth2 설정 skeleton 단계다.

포함:

1. Google/Kakao registration placeholder
2. Kakao provider endpoint 설정
3. provider별 user info adapter
4. OAuth2 login success redirect skeleton
5. SecurityConfig skeleton

제외:

1. 사용자 자동 가입
2. JWT access token 발급
3. refresh token cookie 저장
4. `/api/v1/auth/me` 같은 인증 API

## 사용자가 준비해야 할 값

실제 로그인 테스트 전 아래 값을 발급받아야 한다.

| Provider | 필요한 값 | 환경변수 |
|---|---|---|
| Google | OAuth Client ID | `GOOGLE_CLIENT_ID` |
| Google | OAuth Client Secret | `GOOGLE_CLIENT_SECRET` |
| Kakao | REST API Key | `KAKAO_CLIENT_ID` |
| Kakao | Client Secret | `KAKAO_CLIENT_SECRET` |

Kakao Client Secret은 Kakao 개발자 콘솔에서 활성화한 경우 필요하다. 현재 skeleton은 `client_secret_post` 방식을 전제로 한다.

## 로컬 Redirect URI

Google Console에 등록:

```text
http://localhost:8080/login/oauth2/code/google
```

Kakao Developers에 등록:

```text
http://localhost:8080/login/oauth2/code/kakao
```

프론트엔드 성공 redirect:

```text
http://localhost:5173/oauth2/success
```

## 환경변수 예시

```text
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:5173/oauth2/success
```

## 운영 Redirect URI

운영 환경에서는 NGINX 외부 도메인 기준으로 redirect URI를 등록한다.

예시:

```text
https://{서비스도메인}/login/oauth2/code/google
https://{서비스도메인}/login/oauth2/code/kakao
```

OAuth provider에 등록한 redirect URI와 Spring Boot가 인식하는 external base URL이 다르면 로그인이 실패한다.

## 보안 규칙

1. client secret을 Git에 커밋하지 않는다.
2. `.env`는 커밋하지 않는다.
3. provider console의 redirect URI는 필요한 환경만 등록한다.
4. 운영은 HTTPS를 전제로 한다.
5. Access token 발급은 후속 JWT 작업에서 짧은 만료 시간으로 구현한다.
