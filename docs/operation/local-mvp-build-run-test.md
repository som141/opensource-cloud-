# Local MVP Build, Docker Run, and Test Guide

이 문서는 로컬에서 MVP 스택을 빌드하고 Docker Compose로 실행한 뒤, 실제 이미지를 업로드해서 Worker 전처리 결과를 다운로드하는 절차를 정리한다.

현재 로컬 MVP는 OCR 텍스트 추출이 아니라 OCR 전 단계 이미지 전처리 검증용이다.

## 1. 준비물

로컬 PC에 아래 항목이 필요하다.

| 항목 | 용도 |
| --- | --- |
| Docker Desktop | PostgreSQL, RabbitMQ, MinIO, NGINX, backend-api, preprocess-worker, frontend 실행 |
| Git | 브랜치 최신화 |
| Chrome 또는 Edge | Google OAuth 로그인과 프론트 테스트 |
| Google OAuth Client | 로컬 Google 로그인 테스트 |
| 테스트 이미지 | `png`, `jpg`, `jpeg` 등 실제 문서 이미지 |

아래 포트가 비어 있어야 한다.

```text
80
8080
9000
9001
5432
5672
15672
```

## 2. 코드 최신화

PR 브랜치를 테스트할 때는 해당 브랜치로 이동한다.

```powershell
git fetch origin
git checkout feat/som/83
git pull origin feat/som/83
```

main에 머지된 코드를 테스트할 때는 main을 최신화한다.

```powershell
git fetch origin
git checkout main
git pull origin main
```

## 3. 로컬 환경변수 파일 준비

처음 한 번만 `.env.example`을 복사한다.

```powershell
Copy-Item .\infra\docker-compose\.env.example .\infra\docker-compose\.env
```

`infra/docker-compose/.env`에 실제 로컬 값을 넣는다. 이 파일은 개인 설정 파일이므로 GitHub에 올리지 않는다.

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
JWT_SECRET=<32-bytes-or-longer-local-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost/oauth2/success
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:5173
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_REGION=us-east-1
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173
WORKER_LISTENER_ENABLED=true
WORKER_INTERNAL_TOKEN=local-worker-token
```

Google Console의 OAuth 클라이언트에는 아래 리디렉션 URI를 반드시 등록한다.

```text
http://localhost/login/oauth2/code/google
```

backend-api를 NGINX 없이 직접 테스트할 일이 있으면 아래 URI도 추가한다.

```text
http://localhost:8080/login/oauth2/code/google
```

## 4. 빌드만 먼저 확인하는 방법

Docker Compose 실행 전 전체 이미지 빌드만 확인할 수 있다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env build
```

로컬에 Gradle과 Node가 설치되어 있으면 개별 빌드도 가능하다.

```powershell
cd .\backend-api
gradle test
gradle build
cd ..
```

```powershell
cd .\preprocess-worker
gradle test
gradle build
cd ..
```

```powershell
cd .\frontend
npm install
npm run build
cd ..
```

이 저장소에는 현재 Gradle Wrapper가 없으므로 `gradle` 명령은 로컬 Gradle 설치가 필요하다. 로컬 Gradle이 없으면 Docker Compose 빌드로 검증한다.

## 5. Docker Compose 실행

설정 파일이 유효한지 먼저 확인한다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env config --quiet
```

전체 스택을 빌드하고 실행한다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env up -d --build
```

컨테이너 상태를 확인한다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env ps
```

`image-preprocess-minio-init` 컨테이너가 `Exited (0)`으로 보이면 정상이다. 이 컨테이너는 MinIO bucket 생성만 수행하고 종료된다.

## 6. 기본 접속 주소

| 대상 | 주소 |
| --- | --- |
| Frontend through NGINX | `http://localhost` |
| Login page | `http://localhost/login` |
| Upload smoke page | `http://localhost/upload` |
| backend-api direct | `http://localhost:8080` |
| Swagger through NGINX | `http://localhost/swagger-ui/index.html` |
| Swagger direct | `http://localhost:8080/swagger-ui/index.html` |
| RabbitMQ Management | `http://localhost:15672` |
| MinIO Console | `http://localhost:9001` |

로컬 기본 계정은 아래와 같다.

| 대상 | ID | Password |
| --- | --- | --- |
| RabbitMQ | `local` | `local` |
| MinIO | `minioadmin` | `minioadmin` |
| PostgreSQL | `postgres` | `postgres` |

## 7. 실행 상태 확인

프론트와 백엔드가 응답하는지 확인한다.

```powershell
curl.exe -I http://localhost
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:9000/minio/health/live
curl.exe -I http://localhost/upload
```

RabbitMQ 큐와 Worker consumer를 확인한다.

```powershell
docker exec image-preprocess-rabbitmq rabbitmqctl list_queues name messages consumers
```

정상이라면 `image.preprocess.normal`, `image.preprocess.high`, `image.preprocess.retry` 중 Worker가 구독하는 큐에 consumer가 보인다.

## 8. 실제 이미지 전처리 테스트

브라우저에서 아래 순서로 테스트한다.

1. `http://localhost/login`으로 접속한다.
2. `Sign in with Google`을 눌러 로그인한다.
3. 로그인 성공 후 `/oauth2/success`를 거쳐 토큰이 브라우저에 저장된다.
4. `http://localhost/upload`로 이동한다.
5. 프로젝트 이름을 입력하거나 기본값을 그대로 둔다.
6. 전처리할 실제 문서 이미지를 선택한다.
7. debug artifact가 필요하면 `Save per-step debug artifacts`를 켠다.
8. `Upload and preprocess` 버튼을 누른다.
9. 화면에서 Job progress와 item status를 확인한다.
10. `Item #...: SUCCEEDED`가 나오면 `Download processed`, `Download preview`, `Download report` 버튼을 눌러 결과를 확인한다.

성공 시 화면에 아래 object key가 표시된다.

```text
processedObjectKey: processed/{projectId}/{jobId}/{itemId}/processed.png
previewObjectKey: processed/{projectId}/{jobId}/{itemId}/preview.png
reportObjectKey: processed/{projectId}/{jobId}/{itemId}/processing-report.json
```

결과 다운로드 버튼은 아래 API를 호출해서 presigned URL을 받은 뒤 새 탭으로 연다.

```text
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=preview
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=report
```

## 9. MinIO에서 직접 결과 확인

브라우저에서 `http://localhost:9001`에 접속한다.

```text
ID: minioadmin
Password: minioadmin
```

`image-preprocess-local` bucket에서 아래 경로를 확인한다.

```text
originals/{projectId}/{uploadSessionId}/{imageId}/{originalFileName}
processed/{projectId}/{jobId}/{itemId}/processed.png
processed/{projectId}/{jobId}/{itemId}/preview.png
processed/{projectId}/{jobId}/{itemId}/processing-report.json
processed/{projectId}/{jobId}/{itemId}/debug/*.png
```

debug artifact는 `/upload` 화면에서 debug 옵션을 켠 경우에만 저장된다.

## 10. 로그 확인

문제가 생기면 아래 로그를 먼저 본다.

```powershell
docker logs image-preprocess-nginx --tail 100
docker logs image-preprocess-backend-api --tail 200
docker logs image-preprocess-worker --tail 200
docker logs image-preprocess-rabbitmq --tail 100
docker logs image-preprocess-minio --tail 100
```

실시간 로그를 보고 싶으면 `-f`를 붙인다.

```powershell
docker logs -f image-preprocess-worker
```

## 11. 자주 나는 문제

| 증상 | 확인할 것 |
| --- | --- |
| Google `401 invalid_client` | `.env`의 `GOOGLE_CLIENT_ID`가 Google Console의 클라이언트 ID와 같은지 확인 |
| Google redirect URI 오류 | Google Console에 `http://localhost/login/oauth2/code/google` 등록 여부 확인 |
| `/oauth2/success`에서 JSON 에러 표시 | NGINX와 frontend 이미지가 최신인지 확인 후 `up -d --build` 재실행 |
| 업로드 완료 시 중복 파일 오류 | 같은 프로젝트에 같은 파일을 다시 넣은 경우다. 새 프로젝트 이름으로 테스트 |
| Job이 계속 대기 상태 | `WORKER_LISTENER_ENABLED=true`와 RabbitMQ consumer 수 확인 |
| 다운로드 버튼이 새 탭을 열지 않음 | 브라우저 팝업 차단 여부 확인 |
| MinIO presigned URL 접근 실패 | `.env`의 `MINIO_PUBLIC_ENDPOINT=http://localhost:9000` 확인 |
| 포트 바인딩 실패 | 같은 포트를 쓰는 다른 컨테이너나 로컬 프로세스 종료 |

## 12. 중지와 초기화

컨테이너만 중지한다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env down
```

DB, RabbitMQ, MinIO 데이터를 모두 지우고 처음 상태로 돌린다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env down -v
```

초기화 후 다시 실행한다.

```powershell
docker compose -f .\infra\docker-compose\docker-compose.local.yml --env-file .\infra\docker-compose\.env up -d --build
```

## 13. 완료 기준

로컬 테스트는 아래 조건을 만족하면 통과로 본다.

| 항목 | 기준 |
| --- | --- |
| Docker stack | frontend, backend-api, preprocess-worker, postgres, rabbitmq, minio, nginx 실행 |
| Login | Google 로그인 후 `/oauth2/success` 처리 |
| Upload | `/upload`에서 원본 이미지가 MinIO에 업로드 |
| Job | backend-api가 Job과 JobItem 생성 |
| Queue | RabbitMQ 메시지를 Worker가 소비 |
| Worker | JobItem 상태가 `SUCCEEDED` |
| Artifacts | processed, preview, report가 MinIO에 저장 |
| Download | `/upload` 화면의 다운로드 버튼으로 결과 확인 |

