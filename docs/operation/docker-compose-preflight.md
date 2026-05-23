# Docker Compose 사전 점검

## 목적

`docker compose up -d --build` 이후 브라우저 테스트나 이미지 처리 테스트를 하기 전에 로컬 스택이 정상 연결됐는지 확인합니다.

이 점검은 인증이 필요한 이미지 처리 E2E 테스트가 아닙니다.
인증 후 실제 업로드와 Worker 처리까지 확인하려면 `scripts/local-e2e-smoke.ps1`을 사용합니다.

## 실행 명령

레포지토리 루트에서 실행합니다.

```powershell
.\scripts\docker-compose-preflight.ps1
```

스크립트는 기본적으로 아래 파일을 사용합니다.

```text
infra/docker-compose/.env
```

파일이 없으면 아래 예시 파일로 대체합니다.

```text
infra/docker-compose/.env.example
```

## 점검 항목

| 점검 | 목적 |
| --- | --- |
| `docker compose config` | Compose 설정과 환경변수 치환이 유효한지 확인 |
| Docker container state | 필수 컨테이너가 실행 중이거나 정상 종료됐는지 확인 |
| NGINX 경유 `GET /health` | 단일 진입점이 응답하는지 확인 |
| NGINX 경유 `GET /` | 프론트엔드 정적 routing이 동작하는지 확인 |
| NGINX 경유 `GET /v3/api-docs` | Swagger/OpenAPI routing이 동작하는지 확인 |
| backend 직접 `GET /actuator/health` | Spring Boot health endpoint 확인 |
| MinIO 직접 `GET /minio/health/live` | Object Storage API 접근 확인 |
| RabbitMQ queue 조회 | Queue topology가 생성됐는지 확인 |

## 옵션

포트가 기본값이 아니거나 backend-api를 직접 확인하려면 아래 옵션을 사용합니다.

```powershell
.\scripts\docker-compose-preflight.ps1 `
  -NginxBaseUrl "http://localhost" `
  -BackendBaseUrl "http://localhost:8080" `
  -MinioBaseUrl "http://localhost:9000" `
  -RabbitManagementBaseUrl "http://localhost:15672"
```

특정 env 파일을 사용합니다.

```powershell
.\scripts\docker-compose-preflight.ps1 -EnvFile ".env.example"
```

Docker 컨테이너 상태 점검을 건너뛰고 HTTP route만 확인합니다.

```powershell
.\scripts\docker-compose-preflight.ps1 -SkipDocker
```

cold start가 느린 환경에서는 timeout을 늘립니다.

```powershell
.\scripts\docker-compose-preflight.ps1 -TimeoutSeconds 60
```

## 기대 결과

```text
Preflight passed. The stack is ready for browser login or scripts/local-e2e-smoke.ps1.
```

## 실패 대응

| 실패 지점 | 확인 위치 |
| --- | --- |
| NGINX health 실패 | `image-preprocess-nginx` 컨테이너, 80 포트 binding, `infra/nginx/conf.d/app.conf` |
| Frontend route 실패 | `image-preprocess-frontend` build 상태와 NGINX upstream routing |
| OpenAPI docs 실패 | `backend-api` 컨테이너, `/v3/api-docs`, `infra/nginx/conf.d/api.conf` |
| Backend health 실패 | PostgreSQL/RabbitMQ health, Spring startup log |
| MinIO health 실패 | `image-preprocess-minio`, 9000 포트 binding, bucket init 컨테이너 |
| RabbitMQ queue 점검 실패 | `image-preprocess-rabbitmq`, 15672 management port, `definitions.json` import |

## 후속 테스트

preflight가 통과하면 인증된 E2E 스모크 테스트를 실행합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```
