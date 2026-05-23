# 백업과 복구

MVP에서 영속 데이터는 PostgreSQL과 MinIO volume에 저장됩니다.
운영 백업은 두 저장소를 모두 포함해야 합니다. RabbitMQ queue topology는 Git에 있으며, 대기 중인 메시지는 유지 대상이 아니라 배포/점검 전에 가능한 비우는 것을 기준으로 합니다.

## 백업 대상

| 데이터 | 위치 | 백업 방식 |
| --- | --- | --- |
| 사용자, 프로젝트, 이미지, Job 메타데이터 | PostgreSQL | `pg_dump` |
| 원본 이미지와 처리 결과 | MinIO data volume | volume archive 또는 MinIO mirror |
| 운영 secret | `/opt/image-preprocess/shared/.env.prod` | password manager 또는 암호화 백업 |
| Queue topology | `infra/rabbitmq/definitions.json` | Git |

## PostgreSQL 백업

운영 서버에서 실행합니다.

```bash
mkdir -p /opt/image-preprocess/backups/postgres
docker exec image-preprocess-postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  > "/opt/image-preprocess/backups/postgres/image-preprocess-$(date +%Y%m%d-%H%M%S).sql"
```

shell에 `POSTGRES_USER`, `POSTGRES_DB`가 없다면 아래 파일에서 값을 확인합니다.

```text
/opt/image-preprocess/shared/.env.prod
```

또는 명시적으로 실행합니다.

```bash
docker exec image-preprocess-postgres \
  pg_dump -U image_preprocess -d image_preprocess \
  > /opt/image-preprocess/backups/postgres/image-preprocess.sql
```

## PostgreSQL 복구

비어 있는 DB 또는 의도적으로 초기화한 DB에만 복구합니다.

```bash
cat /opt/image-preprocess/backups/postgres/image-preprocess.sql | \
  docker exec -i image-preprocess-postgres \
  psql -U image_preprocess -d image_preprocess
```

운영 트래픽이 살아 있는 상태에서 덮어쓰지 않습니다. 복구 전 backend-api와 preprocess-worker를 먼저 중지합니다.

## MinIO 백업

MVP에서는 stack을 멈추거나 API/Worker를 일시 중지한 뒤 volume archive를 만드는 방식을 권장합니다.

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

백업 후 stack을 다시 시작합니다.

```bash
docker compose \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.local.yml \
  -f /opt/image-preprocess/current/infra/docker-compose/docker-compose.prod.yml \
  --env-file /opt/image-preprocess/current/infra/docker-compose/.env.prod \
  up -d
```

## MinIO 복구

stack을 중지한 뒤 복구합니다.

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

복구가 끝나면 stack을 다시 시작합니다.

## Secret 백업

아래 파일은 반드시 별도로 보관합니다.

```text
/opt/image-preprocess/shared/.env.prod
```

password manager 또는 암호화 저장소에 저장합니다. Git에는 절대 커밋하지 않습니다.

## 최소 백업 주기

MVP 시연 기준:

- PostgreSQL: 배포 전, 시연 데이터 업로드 후
- MinIO: 저장 경로 또는 Worker artifact 동작이 바뀌는 배포 전
- `.env.prod`: secret 변경 시

실사용 기준:

- PostgreSQL: 매일
- MinIO: 매일 또는 object-storage-provider lifecycle backup
- 보존 기간: 최소 일별 7개, 주별 4개
