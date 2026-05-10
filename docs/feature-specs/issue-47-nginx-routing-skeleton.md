# Issue 47. NGINX Routing Skeleton

## Goal

Add the local NGINX single entry point skeleton. NGINX serves the frontend static placeholder and routes backend-related
paths to `backend-api`.

## Overall Order

1. Add `infra/nginx/nginx.conf`.
2. Add `infra/nginx/conf.d/app.conf`.
3. Add API and OAuth proxy rules.
4. Add SSE proxy rule with buffering disabled.
5. Add admin placeholder routes.
6. Add shared proxy and security header snippets.
7. Add a minimal frontend static placeholder.
8. Add NGINX service to Docker Compose.
9. Update local Compose environment values.
10. Add architecture and operation documentation.
11. Validate NGINX syntax.
12. Validate Docker Compose config.

## Routing Units

| Route | Handling |
| --- | --- |
| `/` | Static frontend placeholder |
| `/assets/*` | Static assets |
| `/api/*` | Proxy to backend-api |
| `/oauth2/*` | Proxy to backend-api |
| `/login/oauth2/*` | Proxy to backend-api |
| `/v3/api-docs` | Proxy to backend-api |
| `/swagger-ui/*` | Proxy to backend-api |
| `/api/v1/jobs/*/events` | Proxy to backend-api with SSE settings |
| `/grafana/*` | Placeholder |
| `/jaeger/*` | Placeholder |

## Out Of Scope

- React/Vite frontend implementation.
- Production TLS certificates.
- HSTS/CSP production hardening.
- Grafana and Jaeger upstream services.
- Kubernetes ingress.

## Verification

- `nginx -t` validates config syntax.
- `docker compose config` validates local Compose integration.
- Secret scan verifies no real OAuth or private key values are committed.
