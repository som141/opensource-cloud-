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
id 'org.springframework.boot' version '3.4.5'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'
```

Spring Boot stays on `3.4.5`. `springdoc-openapi` uses the `2.8.x` line because it is the Spring Boot 3 compatible
line. The locally verified Swagger UI version is `2.8.5`.

## Local Docker Test

The local backend needs PostgreSQL. If the PostgreSQL container is already running, keep it running.

```powershell
docker ps
```

Start the backend from the repository root:

```powershell
$backendPath = Join-Path (Get-Location) 'backend-api'
docker run --rm `
  --name backend-swagger-test `
  --env-file backend-api\.env `
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/image_preprocess `
  -e RABBIT_HEALTH_ENABLED=false `
  -p 8080:8080 `
  -v "${backendPath}:/workspace" `
  -w /workspace `
  gradle:8.10-jdk21 gradle bootRun --no-daemon
```

In another PowerShell window, verify the backend and Swagger endpoints:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/v3/api-docs
Invoke-WebRequest http://localhost:8080/swagger-ui/index.html
```

Expected result:

- `/actuator/health` returns `status: UP`.
- `/v3/api-docs` returns OpenAPI JSON with `bearerAuth`.
- `/swagger-ui/index.html` returns HTTP `200`.

## JWT Bearer Test Flow

1. Start the backend.
2. Open `http://localhost:8080/oauth2/authorization/google`.
3. Complete Google login.
4. The backend redirects to the configured OAuth success URL without exposing the access token in the URL.
5. Use the frontend OAuth success page to refresh and store the short-lived access token, or call
   `POST /api/v1/auth/refresh` from a client that includes the `refresh_token` cookie.
6. Open `http://localhost:8080/swagger-ui/index.html`.
7. Click `Authorize`.
8. Paste only the access token value.
9. Run protected endpoints such as `GET /api/v1/auth/me` or `GET /api/v1/projects`.

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
