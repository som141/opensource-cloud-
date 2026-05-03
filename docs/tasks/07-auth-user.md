# 07. Auth/User

## 목표

Google 소셜 로그인, 사용자, 소셜 계정, 토큰 관리를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/06-global-error-response.md`
4. `docs/api/auth-api.md`

## 작업 범위

1. 사용자 entity
2. 소셜 계정 entity
3. Refresh token entity
4. OAuth2 provider adapter
5. JWT 발급
6. 내 정보, 토큰 갱신, 로그아웃 API

## 작업 순서

1. `User` entity를 만든다.
2. `SocialAccount` entity를 만든다.
3. `RefreshToken` entity를 만든다.
4. `UserRole`, `UserStatus` enum을 만든다.
5. repository를 만든다.
6. Google OAuth2 user info adapter를 만든다.
7. OAuth2 user info factory를 만든다.
8. `SecurityConfig`를 만든다.
9. `JwtTokenProvider`를 만든다.
10. `JwtAuthenticationFilter`를 만든다.
11. OAuth2 login success handler를 만든다.
12. 신규 사용자 자동 가입을 구현한다.
13. 기존 소셜 계정 로그인을 구현한다.
14. access token 발급을 구현한다.
15. refresh token 저장과 회전을 구현한다.
16. `/api/v1/auth/me`를 구현한다.
17. `/api/v1/auth/refresh`를 구현한다.
18. `/api/v1/auth/logout`을 구현한다.
19. 회원 탈퇴 soft delete는 별도 사용자 관리 작업에서 구현한다.

## 산출물

1. Auth/User domain 클래스
2. OAuth2 infra 클래스
3. Security infra 클래스
4. 인증 API
5. 인증 테스트

## 완료 기준

1. Google provider가 지원된다.
2. 최초 로그인 시 사용자 row가 생성된다.
3. provider와 provider user id로 기존 사용자를 찾는다.
4. Refresh token은 폐기 가능하다.

## 금지 사항

1. Worker에 인증 로직을 넣지 않는다.
2. OAuth client secret을 커밋하지 않는다.
3. Access token을 장기 토큰으로 사용하지 않는다.
