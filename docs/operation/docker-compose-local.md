# Docker Compose Local Run

## Purpose

This document explains how to run the local backend stack for development.

## Included Services

- PostgreSQL
- RabbitMQ with management UI
- MinIO
- NGINX
- backend-api
- preprocess-worker

NGINX serves a minimal frontend placeholder from `frontend/public`. The real React/Vite app is still a later task.

## First Run

From the repository root:

```powershell
Copy-Item infra/docker-compose/.env.example infra/docker-compose/.env
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env up -d --build
```

## Default URLs

| Component | URL |
| --- | --- |
| NGINX entry point | `http://localhost` |
| backend-api | `http://localhost:8080` |
| Swagger through NGINX | `http://localhost/swagger-ui/index.html` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| RabbitMQ Management | `http://localhost:15672` |
| MinIO Console | `http://localhost:9001` |

## Default Credentials

| Component | Username | Password |
| --- | --- | --- |
| PostgreSQL | `postgres` | `postgres` |
| RabbitMQ | `local` | `local` |
| MinIO | `minioadmin` | `minioadmin` |

These are local placeholders. Do not reuse them outside local development.

## Worker Listener

The Worker listener is disabled by default:

```text
WORKER_LISTENER_ENABLED=false
```

This prevents the skeleton Worker from consuming queue messages unintentionally while the OpenCV implementation is not
complete. Set it to `true` only when testing queue consumption explicitly.

## Google OAuth Redirect URI

When testing through NGINX, add this redirect URI in Google Console:

```text
http://localhost/login/oauth2/code/google
```

When bypassing NGINX and using backend-api directly, keep this URI too:

```text
http://localhost:8080/login/oauth2/code/google
```

## Stop

```powershell
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env down
```

To remove local data volumes:

```powershell
docker compose -f docker-compose.local.yml --env-file .env down -v
```

## Validate Config

```powershell
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env.example config
```
