# 07. Auth/User

## Goal

Implement Google social login, user, social account, and token management.

## Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/06-global-error-response.md`
4. `docs/api/auth-api.md`

## Scope

1. User entity
2. Social account entity
3. Refresh token entity
4. OAuth2 provider adapter
5. JWT issuance
6. Auth APIs for current user, token refresh, and logout

## Steps

1. Create the `User` entity.
2. Create the `SocialAccount` entity.
3. Create the `RefreshToken` entity.
4. Create the `UserRole` and `UserStatus` enums.
5. Create repositories.
6. Create the Google OAuth2 user info adapter.
7. Create the OAuth2 user info factory.
8. Create `SecurityConfig`.
9. Create `JwtTokenProvider`.
10. Create `JwtAuthenticationFilter`.
11. Create the OAuth2 login success handler.
12. Implement automatic sign-up for the first Google login.
13. Implement login for an existing social account.
14. Implement access token issuance.
15. Implement refresh token storage and rotation.
16. Implement `/api/v1/auth/me`.
17. Implement `/api/v1/auth/refresh`.
18. Implement `/api/v1/auth/logout`.
19. Implement social account link and unlink APIs if multi-provider support is reintroduced later.
20. Implement user soft delete.

## Deliverables

1. Auth/User domain classes
2. OAuth2 infra classes
3. Security infra classes
4. Auth APIs
5. Auth tests

## Completion Criteria

1. Google provider integration works through a dedicated adapter.
2. A user row is created on the first login.
3. Existing users are found by provider and provider user id.
4. Refresh tokens can be revoked and rotated.

## Constraints

1. Worker must not include auth logic.
2. OAuth client secrets must never be committed.
3. Access tokens must not be reused as refresh tokens.
