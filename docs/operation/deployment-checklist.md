# Deployment Checklist

## Purpose

Use this checklist before exposing the MVP outside the local machine.

## 1. Prepare Domain And OAuth

- Point the target domain to the deployment host or load balancer.
- Ensure HTTPS is available before using production OAuth credentials.
- Add Google OAuth JavaScript origin: `https://YOUR_DOMAIN`.
- Add Google OAuth redirect URI: `https://YOUR_DOMAIN/login/oauth2/code/google`.
- Set `OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success`.

## 2. Prepare Secrets

- Copy `infra/docker-compose/.env.prod.example` to `infra/docker-compose/.env.prod` on the server.
- Replace every `CHANGE_ME...` value.
- Replace every `YOUR_DOMAIN` value.
- Keep `.env.prod` out of Git.
- Generate distinct secrets for DB, RabbitMQ, MinIO, JWT, and Worker internal auth.

## 3. Validate Configuration

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.prod.yml `
  --env-file infra/docker-compose/.env.prod `
  config
```

If using the template only:

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.prod.yml `
  --env-file infra/docker-compose/.env.prod.example `
  config
```

## 4. Build And Start Manually

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.prod.yml `
  --env-file infra/docker-compose/.env.prod `
  up -d --build
```

For normal production deployment, prefer GitHub Actions:

```text
Actions -> Deploy Production -> Run workflow
```

See `docs/operation/github-actions-deployment.md`.

## 5. Run Preflight Or Post-Deploy Checks

For local host validation:

```powershell
.\scripts\docker-compose-preflight.ps1
```

For a deployed domain:

```powershell
.\scripts\docker-compose-preflight.ps1 `
  -NginxBaseUrl "https://YOUR_DOMAIN" `
  -BackendBaseUrl "http://YOUR_SERVER_IP:8080" `
  -MinioBaseUrl "http://YOUR_SERVER_IP:9000" `
  -RabbitManagementBaseUrl "http://YOUR_SERVER_IP:15672"
```

If direct service ports are firewalled, validate those services from the server itself or through SSH port forwarding.

## 6. Browser Smoke Test

- Open `https://YOUR_DOMAIN/login`.
- Continue with Google.
- Confirm the redirect returns to `/oauth2/success` without an access token in the URL.
- Open Upload.
- Upload a small PNG/JPEG document image.
- Confirm Job reaches `SUCCEEDED`.
- Download the processed output.

## 7. Authenticated E2E Smoke

After login, read the access token from browser DevTools:

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

Then run:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

## 8. Security Gate Before Public Access

- `REFRESH_TOKEN_COOKIE_SECURE=true`.
- `JWT_SECRET` is not a default value.
- `WORKER_INTERNAL_TOKEN` is not a default value.
- MinIO bucket is private.
- Swagger exposure is intentionally accepted or blocked by a later NGINX rule.
- Only required ports are reachable from the public internet.
- Real `.env.prod` is not committed.

## 9. Operational Follow-Up

- Add HTTPS termination configuration.
- Add database migration tooling and change `JPA_DDL_AUTO` to `validate`.
- Follow `docs/operation/backup-restore.md` for PostgreSQL and object storage backup.
- Add observability stack only after the MVP flow is stable.
