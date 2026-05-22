# GitHub Actions Production Deployment

## Purpose

Production deployment is driven by GitHub Actions. The workflow packages the repository, uploads it to a deployment
server over SSH, copies the server-side `.env.prod`, starts the Docker Compose production stack, and runs unauthenticated
HTTP checks.

Workflow file:

```text
.github/workflows/deploy-production.yml
```

## Trigger

The workflow runs on:

- manual `workflow_dispatch`
- push to `main`

Use the manual trigger for the first deployment. Keep automatic `main` deployment only after the server is stable.

## Required GitHub Environment

Create a GitHub environment named:

```text
production
```

Then add these secrets to that environment.

| Secret | Example | Purpose |
| --- | --- | --- |
| `DEPLOY_HOST` | `203.0.113.10` | SSH host or IP |
| `DEPLOY_USER` | `deploy` | SSH user on the server |
| `DEPLOY_SSH_PRIVATE_KEY` | private key text | Private key for the deploy user |
| `DEPLOY_SSH_PORT` | `22` | Optional SSH port. Defaults to `22` when empty |
| `DEPLOY_PATH` | `/opt/image-preprocess` | Server deployment directory |
| `PROD_BASE_URL` | `https://YOUR_DOMAIN` | Public base URL used for post-deploy checks |

Do not store application secrets such as `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, or DB passwords in this workflow. Those
belong in the server-side `.env.prod`.

Keep `DEPLOY_PATH` simple, for example `/opt/image-preprocess`. Do not use paths containing spaces or shell quotes.

## Server Prerequisites

On the deployment server:

1. Install Docker Engine and Docker Compose plugin.
2. Create a deploy user.
3. Allow the deploy user to run Docker.
4. Create deployment directories.
5. Create the real production env file.

Example:

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/image-preprocess/shared /opt/image-preprocess/releases
sudo chown -R deploy:deploy /opt/image-preprocess
```

Create:

```text
/opt/image-preprocess/shared/.env.prod
```

Use this repo file as the template:

```text
infra/docker-compose/.env.prod.example
```

The workflow refuses to deploy if `/opt/image-preprocess/shared/.env.prod` is missing.

## Workflow Deployment Steps

1. Checkout repository.
2. Validate production Compose using `.env.prod.example`.
3. Create a release archive, excluding Git metadata, node modules, output files, and local env files.
4. Configure SSH from GitHub Actions secrets.
5. Upload the archive to `${DEPLOY_PATH}/releases/image-preprocess-release.tgz`.
6. Extract into `${DEPLOY_PATH}/current`.
7. Copy `${DEPLOY_PATH}/shared/.env.prod` into `current/infra/docker-compose/.env.prod`.
8. Run Docker Compose config validation on the server.
9. Run Docker Compose `up -d --build`.
10. Prune unused Docker images.
11. Run post-deploy checks against `${PROD_BASE_URL}/health` and `${PROD_BASE_URL}/v3/api-docs`.

## Compose Command Used By The Workflow

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  up -d --build
```

`docker-compose.prod.yml` is an override. It keeps only NGINX exposed and resets direct ports for backend-api,
PostgreSQL, RabbitMQ, and MinIO.

## Post-Deploy Checks

The workflow checks only unauthenticated endpoints:

- `GET /health`
- `GET /v3/api-docs`

Google OAuth and authenticated image preprocessing cannot be fully automated without a test identity and browser login.
Run the authenticated E2E smoke manually after deployment.

## Manual Authenticated E2E After Deploy

1. Open `${PROD_BASE_URL}/login`.
2. Sign in with Google.
3. In browser DevTools, read:

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

4. Run:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

## Rollback

This workflow keeps a single `current` directory and does not implement release history rollback yet. A safe manual
rollback is:

1. Re-run a previous successful workflow commit.
2. Or SSH into the server, checkout/restore a known-good archive, and run the same Compose command.

Add timestamped release directories if rollback becomes a frequent operational need.
