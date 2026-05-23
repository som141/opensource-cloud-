# Swagger와 OpenAPI

## 목적

Swagger UI는 로컬 API 테스트와 OpenAPI schema 확인을 위해 사용합니다. 자동화 테스트를 대체하지는 않지만, 프론트엔드가 완성되기 전 인증/프로젝트 API를 빠르게 수동 확인하는 데 유용합니다.

## 로컬 주소

```text
Swagger UI: http://localhost:8080/swagger-ui/index.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
```

## 의존성

backend-api는 아래 의존성을 사용합니다.

```gradle
id 'org.springframework.boot' version '3.4.5'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'
```

Spring Boot는 `3.4.5`를 유지합니다. `springdoc-openapi`는 Spring Boot 3 호환 라인인 `2.8.x`를 사용하며, 로컬에서 확인한 Swagger UI 버전은 `2.8.5`입니다.

## 로컬 Docker 테스트

backend-api는 PostgreSQL이 필요합니다. PostgreSQL 컨테이너가 이미 실행 중이면 그대로 둡니다.

```powershell
docker ps
```

repository root에서 backend를 실행합니다.

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

다른 PowerShell 창에서 확인합니다.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/v3/api-docs
Invoke-WebRequest http://localhost:8080/swagger-ui/index.html
```

기대 결과:

- `/actuator/health`가 `status: UP`을 반환합니다.
- `/v3/api-docs`가 `bearerAuth`가 포함된 OpenAPI JSON을 반환합니다.
- `/swagger-ui/index.html`이 HTTP `200`을 반환합니다.

## JWT Bearer 테스트 흐름

1. backend를 실행합니다.
2. `http://localhost:8080/oauth2/authorization/google`을 엽니다.
3. Google 로그인을 완료합니다.
4. backend가 Access Token을 URL에 노출하지 않고 OAuth success URL로 redirect하는지 확인합니다.
5. 프론트 OAuth success page 또는 `POST /api/v1/auth/refresh`를 통해 Access Token을 받습니다.
6. `http://localhost:8080/swagger-ui/index.html`을 엽니다.
7. `Authorize`를 클릭합니다.
8. Access Token 값만 붙여 넣습니다.
9. `GET /api/v1/auth/me`, `GET /api/v1/projects` 같은 보호 API를 실행합니다.

`Bearer ` prefix는 붙이지 않습니다. Swagger가 Bearer scheme을 자동으로 적용합니다.

## 공개 endpoint와 보호 endpoint

공개 endpoint:

- `GET /swagger-ui/index.html`
- `GET /v3/api-docs`
- `GET /oauth2/authorization/google`
- `GET /login/oauth2/code/google`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

보호 endpoint:

- `GET /api/v1/auth/me`
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}`
- `GET /api/v1/projects/{projectId}/summary`
- 프로젝트 멤버 API

## 참고

- refresh와 logout은 HttpOnly refresh cookie를 사용하므로 raw Swagger 요청보다 브라우저 흐름에서 확인하기 쉽습니다.
- Spring Boot 3.4에서 Springdoc이 메서드 파라미터 이름을 안정적으로 추론하도록 Gradle에 `-parameters`가 활성화되어 있습니다.
- 안정적인 Swagger UI 진입점으로 `springdoc.swagger-ui.path=/swagger-ui.html`을 설정합니다. 실제 UI는 `/swagger-ui/index.html`에서도 제공됩니다.
