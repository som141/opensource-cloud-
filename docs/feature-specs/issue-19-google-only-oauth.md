# Issue 19. Google-only OAuth cleanup

## Issue

- Issue: `#19`
- Title: `♻️ [Refactor] Kakao OAuth 제거 및 Google 전용 정리`
- Branch: `feat/som/19`
- Base: `feat/som/17`

## Goal

Remove Kakao OAuth support from the current skeleton and align the backend and docs to Google-only login.

## Scope

1. Remove Kakao OAuth registration from `application.yml`
2. Remove Kakao environment variables from `.env.example`
3. Remove `KakaoOAuth2UserInfo`
4. Remove Kakao branch from `OAuth2UserInfoFactory`
5. Remove Kakao from `SocialProvider`
6. Remove Kakao-specific unit tests
7. Update current docs and planning files

## Completion Criteria

1. Backend OAuth config contains Google only.
2. Kakao-related code is removed from the backend source set.
3. Current operational docs describe Google only.
4. Docker-based Gradle test and build pass.
