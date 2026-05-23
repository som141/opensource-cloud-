# 이슈 45. Docker Compose local

## 목적

로컬에서 API, Worker, 프론트엔드, DB, queue, storage, NGINX를 한 번에 실행할 수 있게 합니다.

## 작업 범위

1. `docker-compose.local.yml`
2. `.env.example`
3. PostgreSQL
4. RabbitMQ
5. MinIO
6. backend-api
7. preprocess-worker
8. frontend
9. NGINX

## 완료 기준

1. `docker compose up -d --build`로 로컬 스택이 실행됩니다.
2. 기본 secret은 로컬 전용 값으로만 사용합니다.
3. 실제 secret은 `.env`에 주입하고 커밋하지 않습니다.
