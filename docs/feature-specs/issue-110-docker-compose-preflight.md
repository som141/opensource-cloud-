# Issue 110 - Docker Compose Preflight Checks

## Purpose

Before cloud deployment, the local Docker Compose stack needs a repeatable readiness check. The check should catch
basic routing and service wiring failures before running OAuth login or full image preprocessing E2E tests.

## Scope

- Add a PowerShell preflight script for local Docker Compose.
- Check Compose config interpolation.
- Check expected container states.
- Check NGINX, frontend, backend OpenAPI, backend health, MinIO health, and RabbitMQ queue topology.
- Document how to run and how to triage failures.

## Out Of Scope

- OAuth login automation.
- Authenticated upload/job/worker E2E flow.
- Cloud Kubernetes checks.
- Prometheus/Grafana/Jaeger validation.

## Execution Order

1. Start local Docker Compose.
2. Run `scripts/docker-compose-preflight.ps1`.
3. Fix any route or container health failure.
4. Run `scripts/local-e2e-smoke.ps1` with a real access token.
5. Continue to cloud deployment rehearsal.

## Checks

| Step | Verification |
| --- | --- |
| Compose config | `docker compose -f docker-compose.local.yml --env-file .env config` |
| Containers | Required containers are running, bucket init exited successfully |
| NGINX | `/health` returns `ok` |
| Frontend | `/` returns HTML through NGINX |
| OpenAPI | `/v3/api-docs` returns the backend API document through NGINX |
| Backend | `/actuator/health` returns a health response |
| MinIO | `/minio/health/live` returns 200 |
| RabbitMQ | `image.preprocess.normal` queue exists through the management API |

## Completion Criteria

- `scripts/docker-compose-preflight.ps1` exists.
- `docs/operation/docker-compose-preflight.md` explains usage and failure triage.
- `scripts/README.md` and Docker Compose local docs reference the preflight script.
- PowerShell parser validation passes.
- Docker Compose config validation passes.
