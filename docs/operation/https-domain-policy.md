# HTTPS And Domain Policy

## Purpose

Google OAuth production login requires HTTPS. The application should be exposed through one public domain and keep
backend, database, queue, and object storage ports private.

## Recommended Layout

```text
User Browser
  -> HTTPS domain
  -> TLS terminator or cloud load balancer
  -> NGINX container on HTTP :80
  -> frontend / backend-api / MinIO bucket proxy
```

The current Docker Compose production override exposes only NGINX. Direct ports for backend-api, PostgreSQL, RabbitMQ,
and MinIO are reset by `docker-compose.prod.yml`.

## TLS Options

### Option A: Cloudflare Or Load Balancer TLS Termination

Use this for the first MVP deployment.

- Public browser traffic uses `https://YOUR_DOMAIN`.
- Cloudflare/load balancer terminates TLS.
- It forwards HTTP to the server's NGINX container.
- Keep `NGINX_HTTP_PORT=80` or bind NGINX to an internal port depending on your load balancer setup.

### Option B: Host NGINX/Caddy In Front Of Compose

Use a host-level reverse proxy with Let's Encrypt:

```text
host nginx/caddy :443
  -> localhost:8088
  -> compose nginx :80
```

Set:

```text
NGINX_HTTP_PORT=127.0.0.1:8088
```

### Option C: TLS Inside The Compose NGINX Container

This is not implemented yet. It needs certificate volume mounts, port `443`, renewal automation, and a production NGINX
server block. Treat it as a separate hardening task.

## Required Google OAuth Values

Configure Google Console:

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
```

Configure `.env.prod`:

```text
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
CORS_ALLOWED_ORIGINS=https://YOUR_DOMAIN
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

## Object Storage URL Policy

For the in-compose MinIO MVP:

```text
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN
MINIO_BUCKET=image-preprocess-prod
```

The frontend receives presigned URLs using the public endpoint. NGINX proxies this bucket prefix:

```text
/image-preprocess-prod/
```

If the bucket name changes, update:

```text
infra/nginx/conf.d/app.conf
```

## Public Port Policy

Only one public entry point should be open:

| Port | Public? | Reason |
| --- | --- | --- |
| `80` or `443` | Yes | NGINX or TLS terminator |
| `8080` backend-api | No | Routed through NGINX |
| `5432` PostgreSQL | No | Internal only |
| `5672` RabbitMQ AMQP | No | Internal only |
| `15672` RabbitMQ Management | No | SSH tunnel only |
| `9000` MinIO API | No | Bucket path routed through NGINX |
| `9001` MinIO Console | No | SSH tunnel only |

## Swagger Policy

The current MVP leaves:

```text
/swagger-ui/
/v3/api-docs
```

reachable through NGINX. Before public production, choose one:

1. Leave it open for MVP demo only.
2. Restrict by IP at the load balancer/firewall.
3. Add Basic Auth or disable the NGINX routes.

For a student/demo MVP, option 1 is acceptable only if no real user data is uploaded.
