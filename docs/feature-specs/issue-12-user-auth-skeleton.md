# Issue 12. User/Auth domain skeleton

## 이슈

- Issue: `#12`
- Title: `✨ [Feat] user/auth 도메인 entity, repository skeleton 추가`
- Branch: `feat/som/12`
- Base: `feat/som/9`

## 목표

Google 소셜 로그인과 refresh token 관리를 위한 user/auth 도메인의 entity와 repository skeleton을 만든다.

## 작업 범위

1. `User`
2. `SocialAccount`
3. `RefreshToken`
4. `UserRole`
5. `UserStatus`
6. `SocialProvider`
7. `UserRepository`
8. `SocialAccountRepository`
9. `RefreshTokenRepository`
10. entity 단위 테스트

## 도메인 모델

`User`:

1. email
2. name
3. profileImageUrl
4. role
5. status

`SocialAccount`:

1. user
2. provider
3. providerUserId
4. email

`RefreshToken`:

1. userId
2. tokenHash
3. expiresAt
4. revokedAt

## repository 기준

1. `UserRepository`는 email 기반 조회를 지원한다.
2. `SocialAccountRepository`는 provider + providerUserId 기반 조회를 지원한다.
3. `RefreshTokenRepository`는 tokenHash와 userId 기반 조회/삭제를 지원한다.

## 범위 제외

1. OAuth2 login success handler
2. JWT 발급
3. Refresh token cookie 설정
4. SecurityConfig
5. Controller API
6. DB migration

## 주의 사항

1. Worker에 인증 로직을 넣지 않는다.
2. OAuth client secret을 커밋하지 않는다.
3. Access token을 장기 토큰으로 사용하지 않는다.
4. API 서버에 OpenCV 전처리 로직을 넣지 않는다.
