# Docker Compose Preflight

## Purpose

Use this check after `docker compose up -d --build` and before browser or image-processing tests. It verifies that the
local stack is reachable and that the main routes are wired correctly.

This is not the authenticated image processing E2E test. For that workflow, use `scripts/local-e2e-smoke.ps1`.

## Command

Run from the repository root:

```powershell
.\scripts\docker-compose-preflight.ps1
```

The script automatically uses:

```text
infra/docker-compose/.env
```

If `.env` is missing, it falls back to:

```text
infra/docker-compose/.env.example
```

## Checked Items

| Check | Purpose |
| --- | --- |
| `docker compose config` | Validates Compose and environment interpolation |
| Docker container state | Verifies required containers are running or completed successfully |
| `GET /health` through NGINX | Confirms the single entry point is reachable |
| `GET /` through NGINX | Confirms frontend static routing works |
| `GET /v3/api-docs` through NGINX | Confirms backend Swagger/OpenAPI routing works |
| `GET /actuator/health` direct backend | Confirms Spring Boot health endpoint works |
| `GET /minio/health/live` direct MinIO | Confirms object storage API is reachable |
| RabbitMQ management queue lookup | Confirms queue topology exists |

## Options

Direct backend mode or non-default ports:

```powershell
.\scripts\docker-compose-preflight.ps1 `
  -NginxBaseUrl "http://localhost" `
  -BackendBaseUrl "http://localhost:8080" `
  -MinioBaseUrl "http://localhost:9000" `
  -RabbitManagementBaseUrl "http://localhost:15672"
```

Use a specific env file:

```powershell
.\scripts\docker-compose-preflight.ps1 -EnvFile ".env.example"
```

Skip Docker container checks and only check HTTP routes:

```powershell
.\scripts\docker-compose-preflight.ps1 -SkipDocker
```

Wait longer for slow cold starts:

```powershell
.\scripts\docker-compose-preflight.ps1 -TimeoutSeconds 60
```

## Expected Result

```text
Preflight passed. The stack is ready for browser login or scripts/local-e2e-smoke.ps1.
```

## Failure Triage

| Failure | Check |
| --- | --- |
| NGINX health fails | `image-preprocess-nginx` container, port 80 binding, `infra/nginx/conf.d/app.conf` |
| Frontend route fails | `image-preprocess-frontend` build and NGINX upstream routing |
| OpenAPI docs fail | `backend-api` container, `/v3/api-docs`, `infra/nginx/conf.d/api.conf` |
| Backend health fails | PostgreSQL/RabbitMQ health, Spring startup logs |
| MinIO health fails | `image-preprocess-minio`, port 9000 binding, bucket init container |
| RabbitMQ queue check fails | `image-preprocess-rabbitmq`, management port 15672, `definitions.json` import |

## Follow-Up Test

After preflight passes, run the authenticated E2E smoke flow:

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```
