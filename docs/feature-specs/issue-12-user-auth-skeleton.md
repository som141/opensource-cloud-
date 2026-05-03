# Issue 12. User/Auth domain skeleton

## Issue

- Issue: `#12`
- Title: `✨ [Feat] user/auth 도메인 entity, repository skeleton 추가`
- Branch: `feat/som/12`
- Base: `feat/som/9`

## Goal

Create the entity and repository skeleton for Google social login and refresh token management in the user/auth domain.

## Scope

1. `User`
2. `SocialAccount`
3. `RefreshToken`
4. `UserRole`
5. `UserStatus`
6. `SocialProvider`
7. `UserRepository`
8. `SocialAccountRepository`
9. `RefreshTokenRepository`
10. Entity unit tests

## Domain Model

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

## Repository Rules

1. `UserRepository` supports email-based lookup.
2. `SocialAccountRepository` supports provider + providerUserId lookup.
3. `RefreshTokenRepository` supports tokenHash and userId based lookup and revoke flows.

## Out of Scope

1. OAuth2 login success handler
2. JWT issuance
3. Refresh token cookie setup
4. `SecurityConfig`
5. Controller APIs
6. DB migration

## Constraints

1. Worker must not include auth logic.
2. OAuth client secrets must never be committed.
3. Access tokens must not be reused as refresh tokens.
4. API server must not include OpenCV preprocessing logic.
