# NGINX Routing

## Purpose

NGINX is the single local entry point for the browser. It proxies frontend traffic to the frontend NGINX container and
reverse proxies API, OAuth, Swagger, and SSE traffic to `backend-api`.

## Local Entry Points

| Path | Target |
| --- | --- |
| `/` | `frontend:80` |
| `/assets/*` | `frontend:80` |
| `/api/*` | `backend-api:8080` |
| `/oauth2/*` | `backend-api:8080` |
| `/login/oauth2/*` | `backend-api:8080` |
| `/v3/api-docs` | `backend-api:8080` |
| `/swagger-ui/*` | `backend-api:8080` |
| `/api/v1/jobs/*/events` | `backend-api:8080` with SSE buffering disabled |
| `/grafana/*` | Placeholder until observability stack is added |
| `/jaeger/*` | Placeholder until observability stack is added |

## SSE Rules

The Job event path disables response buffering:

```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 1h;
add_header X-Accel-Buffering no always;
```

## OAuth Rules

Google OAuth callback must be registered for the NGINX entry point when testing through NGINX:

```text
http://localhost/login/oauth2/code/google
```

The direct backend callback is still useful when bypassing NGINX:

```text
http://localhost:8080/login/oauth2/code/google
```

## Security Headers

The local skeleton applies basic browser safety headers:

- `X-Frame-Options`
- `X-Content-Type-Options`
- `Referrer-Policy`
- `Permissions-Policy`

Production HTTPS, HSTS, CSP tuning, and admin authentication are deferred to deployment-specific work.
