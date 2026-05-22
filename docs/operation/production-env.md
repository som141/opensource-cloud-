# Production Environment And Secret Injection

## Purpose

This document lists the values required before deploying the Docker Compose MVP to a cloud VM or similar host. It also
defines how secrets should be injected without committing real values to Git.

## Files

| File | Commit to Git | Purpose |
| --- | --- | --- |
| `infra/docker-compose/.env.prod.example` | Yes | Template showing required keys |
| `infra/docker-compose/.env.prod` | No | Real production values on the deployment host |
| Deployment platform secret store | No | Preferred secret source for managed platforms |

Create the real production env file on the server:

```powershell
Copy-Item infra/docker-compose/.env.prod.example infra/docker-compose/.env.prod
```

Then replace every `CHANGE_ME...` and `YOUR_DOMAIN` value.

## Values You Must Prepare

| Area | Required values |
| --- | --- |
| Domain | Public HTTPS domain, for example `https://example.com` |
| Google OAuth | Client ID, client secret, authorized JavaScript origin, authorized redirect URI |
| JWT | At least 32-byte random `JWT_SECRET` |
| PostgreSQL | Database name, user, password |
| RabbitMQ | User and password |
| MinIO/S3 | Access key, secret key, bucket, public endpoint |
| Worker | Internal worker token shared by API and Worker |

## Google OAuth Configuration

For the current NGINX single-entry setup, configure Google Console with:

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
```

Set application redirect after successful login:

```text
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
```

The frontend and API are served from the same domain through NGINX, so the default production cookie settings are:

```text
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

Use `SameSite=None` only if the frontend and API are intentionally split across different sites. `None` requires
`Secure=true`.

## Object Storage Public Endpoint

For in-compose MinIO behind NGINX:

```text
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN
MINIO_BUCKET=image-preprocess-prod
```

NGINX currently proxies the local and production bucket prefixes:

```text
/image-preprocess-local/
/image-preprocess-prod/
```

If you change `MINIO_BUCKET`, add the matching bucket prefix to `infra/nginx/conf.d/app.conf` or presigned upload and
download URLs will not be reachable from the browser.

For managed S3-compatible storage, keep the bucket private and set `MINIO_PUBLIC_ENDPOINT` to the browser-reachable
endpoint used by generated presigned URLs.

## RabbitMQ Credentials

`infra/rabbitmq/definitions.json` stores vhost, exchange, queue, and binding topology only. It does not pin a user or
password. Runtime credentials come from:

```text
RABBITMQ_DEFAULT_USER
RABBITMQ_DEFAULT_PASS
```

The API and Worker receive the same values through `SPRING_RABBITMQ_USERNAME` and `SPRING_RABBITMQ_PASSWORD` in the
Compose file.

## Secret Generation

Examples:

```powershell
openssl rand -base64 48
```

or:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

Generate separate values for:

- `POSTGRES_PASSWORD`
- `RABBITMQ_DEFAULT_PASS`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`
- `WORKER_INTERNAL_TOKEN`

Do not reuse the same secret across services.

## Local Validation With Production Template

Validate interpolation without exposing real secrets:

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  --env-file infra/docker-compose/.env.prod.example `
  config
```

Validate the actual server file on the deployment host:

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  --env-file infra/docker-compose/.env.prod `
  config
```

## Current MVP Limitations

- `docker-compose.prod.yml` is an MVP production override. It limits published service ports and adds restart policies,
  but it does not implement TLS certificates inside the NGINX container.
- HTTPS termination is not implemented in this Compose file. Put a TLS proxy or cloud load balancer in front of NGINX,
  or add a production NGINX/TLS task.
- `JPA_DDL_AUTO=update` is acceptable for MVP rehearsal. Move to migrations and `validate` for durable production data.
