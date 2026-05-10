# Issue 45. Docker Compose Local Environment

## Goal

Provide a local Docker Compose environment for the current backend and Worker development stage. The environment runs
PostgreSQL, RabbitMQ, MinIO, backend-api, and preprocess-worker with safe placeholder values only.

## Overall Order

1. Add Compose `.env.example`.
2. Add PostgreSQL service.
3. Add RabbitMQ service and queue definitions.
4. Add MinIO service and bucket initialization.
5. Add backend-api service connected to PostgreSQL, RabbitMQ, and MinIO.
6. Add preprocess-worker service connected to backend-api, RabbitMQ, and MinIO.
7. Keep Worker listener disabled by default.
8. Add operation documentation.
9. Validate Compose syntax with `docker compose config`.

## Services

| Service | Purpose | Local Port |
| --- | --- | --- |
| `postgres` | Relational metadata DB | `5432` |
| `rabbitmq` | Job queue and management UI | `5672`, `15672` |
| `minio` | S3-compatible object storage | `9000`, `9001` |
| `minio-bucket-init` | Creates local bucket | none |
| `backend-api` | Spring REST API | `8080` |
| `preprocess-worker` | RabbitMQ Worker skeleton | none |

## Queue Defaults

- `image.preprocess.high`
- `image.preprocess.normal`
- `image.preprocess.retry`
- `image.preprocess.dlq`
- `image.benchmark.normal`

## Safe Placeholder Values

The committed values are local placeholders:

- PostgreSQL: `postgres` / `postgres`
- RabbitMQ: `local` / `local`
- MinIO: `minioadmin` / `minioadmin`
- Worker token: `local-worker-token`
- Google OAuth: `local-google-client-id` / `local-google-client-secret`

Real secrets must be placed in an ignored `.env` file or an external secret manager, never committed.

## Out Of Scope

- NGINX reverse proxy routing.
- Frontend service wiring.
- Observability stack.
- Kubernetes/KEDA manifests.
- Production-grade secret handling.

## Verification

`docker compose -f docker-compose.local.yml --env-file .env.example config` validates the composed configuration.
