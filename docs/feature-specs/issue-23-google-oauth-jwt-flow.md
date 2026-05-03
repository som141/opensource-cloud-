# Issue 23. Google OAuth Login Flow And JWT Auth

## Issue

- Issue: `#23`
- Title: `Google OAuth 로그인 flow와 JWT 인증 구현`

## Goal

Replace the previous OAuth skeleton with a functional Google-only login flow. The backend should identify or create a
service user, link the Google social account, issue JWT access tokens, persist refresh-token hashes, and resolve
`@CurrentUser` for protected APIs.

## Work Order

1. Add environment variable contract for Google OAuth, DB, JWT, refresh cookie, and CORS.
2. Keep real secret files ignored by Git.
3. Add Google OAuth user-info adapter.
4. Add OAuth success handler that provisions the user.
5. Add access-token generation and validation.
6. Add refresh-token generation, hashing, persistence, rotation, and revocation.
7. Add refresh-token HttpOnly cookie helper.
8. Add `GET /api/v1/auth/me`.
9. Add `POST /api/v1/auth/refresh`.
10. Add `POST /api/v1/auth/logout`.
11. Add JWT filter.
12. Add `@CurrentUser` argument resolver.
13. Add DB datasource config for Docker PostgreSQL.
14. Add docs and tests.

## Functional Scope

### OAuth Login

- Provider is Google only.
- `SocialProvider` keeps only `GOOGLE`.
- Existing user is found by linked social account first.
- If no social account exists, existing email user is reused or a new user is created.
- Google social account is linked if missing.

### Tokens

- Access token is HMAC-SHA256 JWT.
- Refresh token is random raw token returned only by HttpOnly cookie.
- Refresh token hash is stored in DB.
- Refresh endpoint rotates the refresh token.
- Logout revokes the current refresh token.

### Current User

- JWT filter reads `Authorization: Bearer <access-token>`.
- Security context stores `CustomUserPrincipal`.
- `@CurrentUser Long currentUserId` resolves the authenticated user ID.

## Out Of Scope

- Frontend OAuth success page.
- Swagger UI manual OAuth test.
- Production secret manager.
- Multi-provider account linking.
- Other OAuth providers.

## Verification

- Unit tests cover Google user-info parsing.
- Unit tests cover JWT create/parse.
- Unit tests cover refresh cookie behavior.
- Unit tests cover token issuance/refresh.
- Docker PostgreSQL is used for local boot verification where possible.
