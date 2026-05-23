# 운영 환경변수와 Secret 주입

이 문서는 Docker Compose MVP를 운영 서버에 배포하기 전에 준비해야 하는 값과 주입 방식을 정리합니다.
실제 secret 값은 repository에 커밋하지 않습니다.

## 파일 기준

| 파일 또는 저장소 | Git 커밋 | 용도 |
| --- | --- | --- |
| `infra/docker-compose/.env.prod.example` | 예 | 필요한 key를 보여주는 템플릿 |
| `infra/docker-compose/.env.prod` | 아니오 | 로컬 또는 서버의 실제 운영 값 |
| `/opt/image-preprocess/shared/.env.prod` | 아니오 | GitHub Actions 배포 시 서버에서 사용하는 실제 값 |
| GitHub `production` Environment secrets | 아니오 | SSH 배포에 필요한 값 |

서버에서는 아래 파일을 실제 운영 값으로 둡니다.

```text
/opt/image-preprocess/shared/.env.prod
```

## 준비해야 할 값

| 영역 | 필요한 값 |
| --- | --- |
| 도메인 | `https://YOUR_DOMAIN` 형태의 운영 URL |
| Google OAuth | Client ID, Client Secret, JavaScript origin, redirect URI |
| JWT | 최소 32 byte 이상의 랜덤 `JWT_SECRET` |
| PostgreSQL | DB 이름, 사용자, password |
| RabbitMQ | 사용자, password |
| MinIO/S3 | access key, secret key, bucket, browser 접근 endpoint |
| Worker | backend-api와 Worker가 공유하는 internal token |

## Google OAuth 설정

NGINX 단일 진입점 구조에서는 Google Console에 아래 값을 등록합니다.

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
```

서버 `.env.prod`에는 아래 값을 설정합니다.

```text
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
CORS_ALLOWED_ORIGINS=https://YOUR_DOMAIN
```

Refresh Token cookie는 운영에서 HTTPS를 전제로 합니다.

```text
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

프론트와 API를 서로 다른 사이트로 분리하는 경우에만 `SameSite=None`을 검토합니다. `None`은 반드시 `Secure=true`가 필요합니다.

## Object Storage 설정

Compose 내부 MinIO를 사용하는 경우:

```text
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN
MINIO_BUCKET=image-preprocess-prod
```

NGINX는 아래 bucket prefix를 proxy합니다.

```text
/image-preprocess-local/
/image-preprocess-prod/
```

`MINIO_BUCKET`을 바꾸면 `infra/nginx/conf.d/app.conf`에도 matching prefix를 추가해야 합니다. 그렇지 않으면 presigned upload/download URL이 브라우저에서 접근되지 않습니다.

관리형 S3 호환 스토리지를 사용하는 경우:

- bucket은 private으로 유지합니다.
- `MINIO_ENDPOINT`는 Worker/API가 접근 가능한 내부 또는 API endpoint로 둡니다.
- `MINIO_PUBLIC_ENDPOINT`는 브라우저가 접근 가능한 presigned URL endpoint로 둡니다.

## RabbitMQ 설정

RabbitMQ topology는 `infra/rabbitmq/definitions.json`에 있습니다.
사용자와 password는 runtime 환경변수로 주입합니다.

```text
RABBITMQ_DEFAULT_USER=<rabbit-user>
RABBITMQ_DEFAULT_PASS=<rabbit-password>
SPRING_RABBITMQ_USERNAME=<rabbit-user>
SPRING_RABBITMQ_PASSWORD=<rabbit-password>
```

API와 Worker는 같은 RabbitMQ credential을 사용합니다.

## Secret 생성 예시

PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

OpenSSL:

```bash
openssl rand -base64 48
```

각 항목은 별도 값으로 생성합니다.

- `POSTGRES_PASSWORD`
- `RABBITMQ_DEFAULT_PASS`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`
- `WORKER_INTERNAL_TOKEN`

## GitHub Actions secrets와 서버 `.env.prod`의 분리

GitHub Actions secrets에는 SSH 배포에 필요한 값만 둡니다.

```text
DEPLOY_HOST
DEPLOY_USER
DEPLOY_SSH_PRIVATE_KEY
DEPLOY_SSH_PORT
DEPLOY_PATH
PROD_BASE_URL
```

서버 `.env.prod`에는 애플리케이션 실행에 필요한 값을 둡니다.

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
JWT_SECRET
POSTGRES_PASSWORD
RABBITMQ_DEFAULT_PASS
MINIO_ROOT_PASSWORD
WORKER_INTERNAL_TOKEN
```

이렇게 분리하면 GitHub Actions 로그나 설정 화면에 애플리케이션 secret이 노출되는 범위를 줄일 수 있습니다.

## 설정 검증

템플릿 검증:

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.prod.yml `
  --env-file infra/docker-compose/.env.prod.example `
  config
```

서버 실제 파일 검증:

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  config
```

## 현재 MVP 제한

- `docker-compose.prod.yml`은 production override이지만 TLS 인증서를 직접 발급하지 않습니다.
- HTTPS termination은 외부 proxy, load balancer, 또는 별도 TLS proxy에서 처리해야 합니다.
- `JPA_DDL_AUTO=update`는 MVP 리허설용입니다. 운영 데이터가 중요해지면 migration 도구와 `validate`로 전환해야 합니다.
