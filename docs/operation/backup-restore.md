# Backup And Restore

## Purpose

The MVP stores durable data in PostgreSQL and MinIO volumes. Backups must cover both. RabbitMQ queue definitions are in
Git; queued messages are transient and should normally be drained before maintenance.

## What To Back Up

| Data | Source | Backup method |
| --- | --- | --- |
| PostgreSQL records | `image-preprocess-postgres` | `pg_dump` |
| Original and processed images | MinIO data volume | MinIO mirror or volume archive |
| Production secrets | `/opt/image-preprocess/shared/.env.prod` | Secure password manager or encrypted backup |
| Queue topology | `infra/rabbitmq/definitions.json` | Git |

## PostgreSQL Backup

Run on the deployment server:

```bash
mkdir -p /opt/image-preprocess/backups/postgres
docker exec image-preprocess-postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  > "/opt/image-preprocess/backups/postgres/image-preprocess-$(date +%Y%m%d-%H%M%S).sql"
```

If the shell does not have `POSTGRES_USER` and `POSTGRES_DB`, read them from:

```text
/opt/image-preprocess/shared/.env.prod
```

or pass explicit values:

```bash
docker exec image-preprocess-postgres \
  pg_dump -U image_preprocess -d image_preprocess \
  > /opt/image-preprocess/backups/postgres/image-preprocess.sql
```

## PostgreSQL Restore

Restore into an empty or intentionally reset database:

```bash
cat /opt/image-preprocess/backups/postgres/image-preprocess.sql | \
  docker exec -i image-preprocess-postgres \
  psql -U image_preprocess -d image_preprocess
```

Do not restore over active production traffic without stopping API and Worker first.

## MinIO Backup

Recommended MVP backup is a volume archive while the stack is stopped or quiesced:

```bash
mkdir -p /opt/image-preprocess/backups/minio
docker compose \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.local.yml \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.prod.yml \
  --env-file /opt/image-preprocess/current/infra/docker-compose/.env.prod \
  stop backend-api preprocess-worker

docker run --rm \
  -v image-preprocess-prod_minio-data:/source:ro \
  -v /opt/image-preprocess/backups/minio:/backup \
  alpine \
  tar -czf /backup/minio-data-$(date +%Y%m%d-%H%M%S).tgz -C /source .
```

Restart after the archive finishes:

```bash
docker compose \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.local.yml \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.prod.yml \
  --env-file /opt/image-preprocess/current/infra/docker-compose/.env.prod \
  up -d
```

## MinIO Restore

Restore only when the stack is stopped:

```bash
docker compose \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.local.yml \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.prod.yml \
  --env-file /opt/image-preprocess/current/infra/docker-compose/.env.prod \
  down

docker run --rm \
  -v image-preprocess-prod_minio-data:/target \
  -v /opt/image-preprocess/backups/minio:/backup \
  alpine \
  sh -c 'rm -rf /target/* && tar -xzf /backup/minio-data-YYYYMMDD-HHMMSS.tgz -C /target'
```

Then start the stack.

## Secret Backup

Back up:

```text
/opt/image-preprocess/shared/.env.prod
```

Store it in a password manager or encrypted secret storage. Never commit it to Git.

## Minimum Backup Schedule

For MVP demo:

- PostgreSQL: before every deployment and after successful demo data upload.
- MinIO: before every deployment that changes storage paths or worker artifact behavior.
- `.env.prod`: whenever secrets change.

For real usage:

- PostgreSQL: daily.
- MinIO: daily or object-storage-provider lifecycle backup.
- Retention: at least 7 daily backups and 4 weekly backups.
