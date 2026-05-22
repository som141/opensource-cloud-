# Issue 114 - Production Compose And GitHub Actions Deployment

## Purpose

Prepare the MVP for cloud deployment by separating production Compose behavior, documenting HTTPS/domain policy,
adding GitHub Actions deployment automation, and defining final E2E verification.

## Scope

- Add `infra/docker-compose/docker-compose.prod.yml`.
- Add `.github/workflows/deploy-production.yml`.
- Document GitHub Actions secrets and server prerequisites.
- Document HTTPS/domain policy.
- Document backup and restore operations.
- Document final local and production E2E verification order.

## Production Compose Behavior

The production override is used with the existing local Compose file:

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  up -d --build
```

It applies:

- `restart: unless-stopped` to long-running services.
- Direct port reset for backend-api, PostgreSQL, RabbitMQ, and MinIO.
- NGINX remains the single public entry point.

## GitHub Actions Deployment Flow

1. Validate production Compose template.
2. Create a repository archive.
3. Upload archive to the deployment server over SSH.
4. Extract into `${DEPLOY_PATH}/current`.
5. Copy `${DEPLOY_PATH}/shared/.env.prod` into the release.
6. Run Compose config validation on the server.
7. Run Compose `up -d --build`.
8. Check `${PROD_BASE_URL}/health`.
9. Check `${PROD_BASE_URL}/v3/api-docs`.

## Required GitHub Secrets

| Secret | Purpose |
| --- | --- |
| `DEPLOY_HOST` | SSH host/IP |
| `DEPLOY_USER` | SSH user |
| `DEPLOY_SSH_PRIVATE_KEY` | SSH private key |
| `DEPLOY_SSH_PORT` | Optional SSH port |
| `DEPLOY_PATH` | Deployment directory |
| `PROD_BASE_URL` | Public HTTPS base URL |

## Out Of Scope

- Creating real production secrets.
- Running Google OAuth automatically in CI.
- Implementing TLS certificates inside Compose NGINX.
- Implementing timestamped rollback releases.

## Validation

- `docker compose` config with local and production files.
- JSON/YAML/document syntax sanity checks.
- Deployment workflow dry validation by reviewing generated Compose config.
- Actual deployment requires GitHub Actions secrets and a prepared server.
