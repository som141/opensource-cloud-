# 최종 E2E 검증

이 문서는 배포 전후 최종 검증 순서입니다. 인증 없는 readiness check와 Google OAuth가 필요한 이미지 전처리 흐름을 분리합니다.

## 1. 로컬 Compose readiness

```powershell
.\scripts\docker-compose-preflight.ps1
```

기대 결과:

```text
Preflight passed.
```

## 2. 로컬 인증 이미지 처리

1. `http://localhost/login` 접속
2. Google 로그인
3. DevTools에서 Access Token 확인

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

4. 스모크 스크립트 실행

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

기대 결과:

- 프로젝트가 생성된다.
- ZIP 파일이 이미지 파일로 풀린다.
- presigned URL로 이미지가 업로드된다.
- 업로드 완료 후 이미지 메타데이터가 생성된다.
- Job이 생성된다.
- Worker가 모든 JobItem을 완료한다.
- 처리된 이미지와 processed-only ZIP이 `out/local-e2e-smoke/{runId}` 아래에 다운로드된다.

## 3. 운영 Compose 설정 검증

서버에서 실행합니다.

```bash
cd /opt/image-preprocess/current
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  config
```

기대 결과:

- 외부 공개 포트는 NGINX만 가진다.
- backend-api, PostgreSQL, RabbitMQ, MinIO 직접 포트가 공개되지 않는다.
- `REFRESH_TOKEN_COOKIE_SECURE=true`이다.
- `MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN`이다.

## 4. GitHub Actions 배포 확인

GitHub Actions에서 `Deploy Production`을 실행합니다. workflow 안정화 후에는 `main` merge로 자동 실행될 수 있습니다.

기대 결과:

- Compose template validation 통과
- release archive upload 성공
- server deploy step 성공
- `GET /health` 성공
- `GET /v3/api-docs` 응답 확인

## 5. 운영 브라우저 스모크

1. `https://YOUR_DOMAIN/login` 접속
2. Google 로그인
3. 아래 주소로 돌아오는지 확인

```text
https://YOUR_DOMAIN/oauth2/success
```

4. URL에 Access Token이 포함되지 않는지 확인
5. Upload 화면으로 이동
6. PNG/JPEG 문서 이미지 또는 ZIP 업로드
7. Job이 `SUCCEEDED`가 되는지 확인
8. 처리된 결과 다운로드

## 6. 운영 인증 스크립트 스모크

브라우저 로그인 후 Access Token을 가져와 실행합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

기대 결과:

- 모든 JobItem이 성공한다.
- 처리된 output ZIP이 다운로드된다.

## 중단 조건

아래 항목 중 하나라도 발생하면 공개 사용으로 진행하지 않습니다.

- OAuth가 `invalid_client` 또는 redirect URI mismatch를 반환한다.
- `/health`가 실패한다.
- Worker가 internal API를 호출하지 못한다.
- presigned upload/download URL이 브라우저에서 접근 불가능한 host를 가리킨다.
- processed ZIP 다운로드 결과가 비어 있다.
- HTTPS 환경에서 Refresh Token cookie가 Secure가 아니다.
- `.env.prod`에 `CHANGE_ME` 또는 `YOUR_DOMAIN` placeholder가 남아 있다.
