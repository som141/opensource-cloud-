# Swagger And OpenAPI

## Purpose

Swagger UI is enabled for local API testing and OpenAPI schema inspection. It is not a replacement for automated tests,
but it is the fastest way to manually verify Auth and Project endpoints while the frontend is still incomplete.

## Local URLs

```text
Swagger UI: http://localhost:8080/swagger-ui/index.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

## Dependency

The backend uses:

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'
```

Spring Boot is `3.4.5`, so the springdoc `2.8.x` line is used.

## JWT Bearer Test Flow

1. Start the backend.
2. Open `http://localhost:8080/oauth2/authorization/google`.
3. Complete Google login.
4. Copy the `accessToken` query parameter from the OAuth success redirect URL.
5. Open `http://localhost:8080/swagger-ui/index.html`.
6. Click `Authorize`.
7. Paste only the access token value.
8. Run protected endpoints such as `GET /api/v1/auth/me` or `GET /api/v1/projects`.

Do not paste the `Bearer ` prefix. Swagger applies the Bearer scheme automatically.

## Public And Protected Endpoints

Public endpoints:

- `GET /swagger-ui/index.html`
- `GET /v3/api-docs`
- `GET /oauth2/authorization/google`
- `GET /login/oauth2/code/google`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

Protected endpoints:

- `GET /api/v1/auth/me`
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}`
- `GET /api/v1/projects/{projectId}/summary`
- Project member APIs

## Notes

- Refresh and logout use the HttpOnly refresh cookie, so they are easier to verify in a browser flow than with a raw
  Swagger request.
- `-parameters` is enabled in Gradle so Springdoc can infer method parameter names reliably on Spring Boot 3.4.
- `springdoc.swagger-ui.path=/swagger-ui.html` is configured as the stable Swagger UI entry point. It also serves the
  actual UI at `/swagger-ui/index.html`.
