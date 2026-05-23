# 이슈 23. Google OAuth와 JWT 흐름

## 목적

Google OAuth 로그인 후 Access Token과 Refresh Token을 발급하고, 프론트엔드가 인증 상태를 유지할 수 있게 합니다.

## 작업 범위

1. Google OAuth2 client 설정
2. OAuth success handler
3. 사용자 자동 가입 또는 기존 계정 연결
4. Access Token 발급
5. Refresh Token `HttpOnly` cookie 저장
6. `/api/v1/auth/me`와 `/api/v1/auth/refresh` 제공

## 보안 기준

1. Refresh Token은 JavaScript에서 읽을 수 없는 cookie로 저장합니다.
2. Access Token을 URL에 노출하는 방식은 장기적으로 제거합니다.
3. OAuth redirect URI는 NGINX 기준과 backend 직접 기준을 구분합니다.
