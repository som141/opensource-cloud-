# Docker Compose 로컬 실행

## 목적

로컬 개발과 수동 검증을 위해 전체 MVP 스택을 Docker Compose로 실행하는 방법을 정리합니다.

## 포함 서비스

- PostgreSQL
- RabbitMQ Management UI
- MinIO
- NGINX 단일 진입점
- React/Vite 프론트엔드 정적 파일 서버
- `backend-api`
- `preprocess-worker`

## 최초 실행

레포지토리 루트에서 실행합니다.

```powershell
Copy-Item infra/docker-compose/.env.example infra/docker-compose/.env
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env up -d --build
```

## 기본 접속 주소

| 구성요소 | 주소 |
| --- | --- |
| NGINX 단일 진입점 | `http://localhost` |
| backend-api 직접 접속 | `http://localhost:8080` |
| NGINX 경유 Swagger | `http://localhost/swagger-ui/index.html` |
| backend-api 직접 Swagger | `http://localhost:8080/swagger-ui/index.html` |
| RabbitMQ Management | `http://localhost:15672` |
| MinIO Console | `http://localhost:9001` |

## 기본 계정

| 구성요소 | 아이디 | 비밀번호 |
| --- | --- | --- |
| PostgreSQL | `postgres` | `postgres` |
| RabbitMQ | `local` | `local` |
| MinIO | `minioadmin` | `minioadmin` |

위 값은 로컬 placeholder입니다. 운영 환경에서 재사용하지 않습니다.

RabbitMQ queue와 exchange 구조는 `infra/rabbitmq/definitions.json`으로 생성합니다.
사용자 계정은 `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS` 환경변수로 주입하므로 로컬과 운영 값을 분리할 수 있습니다.

## Worker listener

로컬 MVP 스모크 플로우에서는 Worker listener가 기본 활성화되어 있습니다.

```text
WORKER_LISTENER_ENABLED=true
```

큐에 메시지만 쌓이는 상태를 확인하고 싶을 때만 `false`로 변경합니다.

## 프론트엔드 수동 테스트

브라우저 테스트 전에 preflight를 먼저 실행합니다.

```powershell
.\scripts\docker-compose-preflight.ps1
```

preflight는 Docker Compose 설정, 컨테이너 상태, NGINX routing, backend health, Swagger/OpenAPI, MinIO health, RabbitMQ queue topology를 확인합니다.
실패 대응은 [Docker Compose 사전 점검](docker-compose-preflight.md)을 참고합니다.

수동 테스트 순서는 다음과 같습니다.

1. 스택을 실행합니다.
2. `http://localhost`에 접속합니다.
3. 왼쪽 하단 로그인 버튼으로 Google 로그인을 진행합니다.
4. 업로드 화면으로 이동합니다.
5. 스캔 문서 이미지 또는 ZIP 파일을 선택합니다.
6. 단계별 debug artifact가 필요하면 옵션을 켭니다.
7. `Upload batch and preprocess` 버튼을 누릅니다.
8. Job 진행률과 결과 다운로드 버튼을 확인합니다.

프론트엔드는 backend-api가 발급한 presigned URL을 사용해 원본 파일을 MinIO에 직접 업로드합니다.
로컬에서는 NGINX가 MinIO bucket 경로를 프록시하므로 브라우저 기준 같은 origin으로 동작합니다.

```text
MINIO_PUBLIC_ENDPOINT=http://localhost
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173
```

같은 프로젝트에 같은 파일을 다시 업로드하면 checksum 중복 검증으로 거절될 수 있습니다.
반복 테스트에는 새 프로젝트를 생성하는 방식을 권장합니다.

성공 시 대표 상태는 다음과 같습니다.

```text
Item status: SUCCEEDED
processedObjectKey: processed/{projectId}/{jobId}/{itemId}/processed.png
```

현재 사용자 화면에서는 preview/report가 아니라 처리된 이미지와 결과 ZIP 다운로드를 중심으로 노출합니다.

## API E2E 스모크 스크립트

Docker Compose MVP를 반복 검증하려면 아래 스크립트를 사용합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

스크립트는 synthetic 문서 이미지를 만들고 ZIP으로 묶은 뒤, 프론트엔드 업로드 흐름과 동일하게 ZIP을 이미지 단위로 풀어 presigned URL 업로드를 수행합니다.
이후 전처리 Job을 생성하고 Worker 완료를 기다린 다음 처리된 이미지와 processed-only ZIP을 다운로드합니다.

결과 파일은 아래 경로에 저장됩니다.

```text
out/local-e2e-smoke/{runId}
```

### Access Token 가져오기

스크립트는 정상 인증 API를 사용합니다. 개발용 인증 우회는 만들지 않습니다.

브라우저에서 로그인한 뒤 DevTools Console에서 확인합니다.

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

토큰을 직접 넘깁니다.

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

또는 현재 PowerShell 세션 환경변수로 지정합니다.

```powershell
$env:ACCESS_TOKEN = "<access-token>"
.\scripts\local-e2e-smoke.ps1
```

### backend-api 직접 호출 모드

NGINX를 우회해 backend-api로 직접 API를 호출하려면 아래처럼 실행합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "http://localhost:8080/api" `
  -AccessToken "<access-token>"
```

단, presigned URL은 브라우저 또는 호스트에서 접근 가능한 `MINIO_PUBLIC_ENDPOINT`를 사용해야 합니다.

## Google OAuth redirect URI

NGINX를 통해 테스트할 때 Google Console에 아래 redirect URI를 등록합니다.

```text
http://localhost/login/oauth2/code/google
```

backend-api를 직접 테스트할 때는 아래 URI도 등록합니다.

```text
http://localhost:8080/login/oauth2/code/google
```

## 종료

```powershell
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env down
```

로컬 데이터 볼륨까지 삭제하려면 아래 명령을 사용합니다.

```powershell
docker compose -f docker-compose.local.yml --env-file .env down -v
```

## 설정 검증

```powershell
cd infra/docker-compose
docker compose -f docker-compose.local.yml --env-file .env.example config
```
