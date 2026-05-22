# Issue 112 - Production Environment And Secret Injection Docs

## Purpose

The project needs a clear production environment checklist before cloud deployment. Developers must know which values
to issue, where to inject them, and which files must not be committed.

## Scope

- Add a production env template with placeholders only.
- Document Google OAuth redirect values.
- Document JWT, DB, RabbitMQ, MinIO/S3, and Worker token requirements.
- Document `.env.prod` local-host/server injection.
- Document deployment validation and smoke-test order.
- Keep production Compose implementation as a separate follow-up task.

## Files

| File | Purpose |
| --- | --- |
| `infra/docker-compose/.env.prod.example` | Production env template without real secrets |
| `docs/operation/production-env.md` | Required values and secret injection rules |
| `docs/operation/deployment-checklist.md` | Step-by-step deployment gate |
| `infra/docker-compose/docker-compose.local.yml` | Exposes backend auth/cookie env variables to the container |
| `infra/nginx/conf.d/app.conf` | Allows the production MinIO bucket prefix through NGINX |
| `infra/rabbitmq/definitions.json` | Keeps queue topology env-independent by removing pinned local credentials |

## Required User-Provided Values

| Area | Values |
| --- | --- |
| Domain | Public HTTPS domain |
| Google OAuth | Client ID, client secret, JavaScript origin, redirect URI |
| JWT | Random 32+ byte secret |
| Database | PostgreSQL password |
| Queue | RabbitMQ password |
| Storage | MinIO/S3 access key, secret key, bucket endpoint |
| Worker | Internal worker token |

## Non-Goals

- Do not add real secret values.
- Do not introduce Kubernetes manifests.
- Do not implement production HTTPS in this task.
- Do not change the OAuth token flow.
- Do not pin production RabbitMQ credentials inside `definitions.json`.

## Validation

1. `git diff --check`
2. PowerShell-free docs review.
3. Docker Compose config with `.env.prod.example`.
4. Docker Compose config with local `.env.example`.

## Follow-Up

- Add `docker-compose.prod.yml` or `docker-compose.prod.override.yml`.
- Restrict public port exposure.
- Add HTTPS/TLS termination.
- Add backup and migration policy.
