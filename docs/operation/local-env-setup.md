# 로컬 환경변수 설정

## Secret 관리 규칙

실제 secret은 Git에 커밋하지 않습니다.
레포지토리에는 템플릿과 설명 문서만 올리고, 실제 로컬 값은 Git에서 제외되는 파일에 둡니다.

- `backend-api/.env`
- `infra/docker-compose/.env`
- `LOCAL_CONFIG.md`
- `*.local.md`

위 경로는 `.gitignore`에서 제외 대상으로 관리합니다.

## backend-api `.env` 작성

`backend-api/.env.example`을 복사해 로컬 전용 `.env`를 만듭니다.

```powershell
Copy-Item backend-api\.env.example backend-api\.env
```

필요한 값을 채웁니다.

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:5173/oauth2/success

DB_URL=jdbc:postgresql://localhost:5432/image_preprocess
DB_USERNAME=postgres
DB_PASSWORD=postgres
JPA_DDL_AUTO=update

JWT_SECRET=<at-least-32-byte-secret>
ACCESS_TOKEN_EXPIRE_SECONDS=1800
REFRESH_TOKEN_EXPIRE_SECONDS=1209600
REFRESH_TOKEN_COOKIE_NAME=refresh_token
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax

CORS_ALLOWED_ORIGINS=http://localhost:5173
RABBIT_HEALTH_ENABLED=false
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=image-preprocess-local
MINIO_REGION=us-east-1
```

## Google Console 설정

backend-api를 직접 실행할 때는 아래 redirect URI를 등록합니다.

```text
http://localhost:8080/login/oauth2/code/google
```

NGINX를 로컬 단일 진입점으로 사용할 때는 아래 URI도 등록합니다.

```text
http://localhost/login/oauth2/code/google
```

## Docker Compose `.env` 작성

전체 로컬 MVP 스택은 Compose env 파일을 사용합니다.

```powershell
Copy-Item infra/docker-compose/.env.example infra/docker-compose/.env
```

필수 값은 다음과 같습니다.

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
JWT_SECRET=<at-least-32-byte-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost/oauth2/success

MINIO_PUBLIC_ENDPOINT=http://localhost
MINIO_REGION=us-east-1
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173
WORKER_LISTENER_ENABLED=true
```

`infra/docker-compose/.env`는 커밋하지 않습니다.

## Docker PostgreSQL 단독 실행

backend-api만 로컬에서 실행하고 DB만 Docker로 띄우려면 아래 명령을 사용합니다.

```powershell
docker run --name image-preprocess-postgres `
  -e POSTGRES_DB=image_preprocess `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16-alpine
```

컨테이너가 이미 존재하면 시작만 합니다.

```powershell
docker start image-preprocess-postgres
```

중지합니다.

```powershell
docker stop image-preprocess-postgres
```

## 로컬 env로 backend-api 실행

PowerShell에서 `.env`를 현재 프로세스 환경변수로 주입합니다.

```powershell
Get-Content backend-api\.env | ForEach-Object {
  if ($_ -and -not $_.StartsWith("#")) {
    $name, $value = $_.Split("=", 2)
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
  }
}

Set-Location backend-api
gradle bootRun
```

Docker Gradle image로 테스트할 때는 아래처럼 실행합니다.

```powershell
$backendPath = Join-Path (Get-Location) "backend-api"
docker run --rm `
  -v "${backendPath}:/workspace" `
  -w /workspace `
  --env-file backend-api\.env `
  gradle:8.10-jdk21 `
  gradle test --no-daemon
```

Gradle 컨테이너에서 호스트 PostgreSQL에 접근해야 하면 `DB_URL`을 아래처럼 바꿉니다.

```env
DB_URL=jdbc:postgresql://host.docker.internal:5432/image_preprocess
```

`RABBIT_HEALTH_ENABLED=false`는 RabbitMQ 없이 backend-api health를 먼저 확인해야 하는 로컬 상황에서 사용합니다.
