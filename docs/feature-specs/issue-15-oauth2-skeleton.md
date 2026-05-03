# Issue 15. Google OAuth2 skeleton

## Issue

- Issue: `#15`
- Title: `✨ [Feat] Google OAuth2 설정 skeleton 추가`
- Branch: `feat/som/15`
- Base: `feat/som/12`

## Goal

Add the provider configuration and user info adapter skeleton required for Google OAuth2 login.

## Scope

1. Google OAuth2 registration placeholder
2. `OAuth2UserInfo`
3. `GoogleOAuth2UserInfo`
4. `OAuth2UserInfoFactory`
5. `OAuth2LoginFailedException`
6. `OAuth2LoginSuccessHandler`
7. `SecurityConfig`
8. OAuth provider setup document

## Out of Scope

1. Automatic user sign-up
2. Existing account login flow
3. JWT access token issuance
4. Refresh token issuance and storage
5. Auth controller APIs
6. Frontend OAuth callback page

## Required User Preparation

For real login testing you need:

1. Google OAuth Client ID
2. Google OAuth Client Secret

Local redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

## Verification

1. Provider parsing unit test
2. Unsupported provider exception test
3. Docker-based `gradle test`
4. Secret leak check
