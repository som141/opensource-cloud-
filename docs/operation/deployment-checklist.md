# 배포 체크리스트

운영 또는 외부 시연 환경에 MVP를 노출하기 전에 아래 항목을 확인합니다.

## 1. 도메인과 OAuth

- [ ] 운영 도메인이 서버 또는 로드밸런서를 가리킨다.
- [ ] HTTPS가 적용되어 있다.
- [ ] Google OAuth JavaScript origin이 등록되어 있다.
- [ ] Google OAuth redirect URI가 등록되어 있다.
- [ ] `OAUTH2_SUCCESS_REDIRECT_URI`가 운영 도메인 기준이다.

예시:

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
```

## 2. Secret과 환경변수

- [ ] 서버에 `/opt/image-preprocess/shared/.env.prod`가 있다.
- [ ] `CHANGE_ME` 값이 남아 있지 않다.
- [ ] `YOUR_DOMAIN` 값이 남아 있지 않다.
- [ ] `.env.prod`가 Git에 커밋되지 않았다.
- [ ] `JWT_SECRET`은 충분히 긴 랜덤 값이다.
- [ ] `WORKER_INTERNAL_TOKEN`은 기본값이 아니다.
- [ ] DB, RabbitMQ, MinIO password를 서로 다르게 설정했다.

## 3. 서버와 Docker

- [ ] Docker Engine이 설치되어 있다.
- [ ] Docker Compose plugin이 설치되어 있다.
- [ ] 배포 유저가 Docker를 실행할 수 있다.
- [ ] `/opt/image-preprocess/shared`와 `/opt/image-preprocess/releases` 경로가 있다.
- [ ] 서버 디스크 용량이 원본/결과 이미지 저장량을 감당한다.

## 4. GitHub Actions

- [ ] GitHub `production` Environment가 있다.
- [ ] Actions workflow permission이 `Read and write permissions`로 설정되어 있다.
- [ ] `DEPLOY_HOST`가 등록되어 있다.
- [ ] `DEPLOY_USER`가 등록되어 있다.
- [ ] `DEPLOY_SSH_PRIVATE_KEY`가 등록되어 있다.
- [ ] `DEPLOY_PATH`가 등록되어 있다.
- [ ] `PROD_BASE_URL`이 등록되어 있다.
- [ ] 첫 배포는 수동 `workflow_dispatch`로 실행한다.

## 4.1 GHCR 이미지

- [ ] `Build GHCR Images` workflow가 성공했다.
- [ ] backend-api 이미지가 GHCR에 올라갔다.
- [ ] preprocess-worker 이미지가 GHCR에 올라갔다.
- [ ] frontend 이미지가 GHCR에 올라갔다.
- [ ] `Render Kubernetes Manifests` workflow가 성공했다.
- [ ] 렌더링 artifact에 `YOUR_REGISTRY` 또는 `CHANGE_ME`가 남아 있지 않다.
- [ ] Kubernetes manifest 또는 overlay에 실제 image tag가 반영되어 있다.
- [ ] GHCR package가 private이면 cluster에 `imagePullSecret`이 준비되어 있다.

## 5. Compose 설정 검증

템플릿으로 먼저 검증합니다.

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.prod.yml `
  --env-file infra/docker-compose/.env.prod.example `
  config
```

서버에서는 실제 `.env.prod`로 검증합니다.

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  config
```

## 6. 배포 후 기본 확인

- [ ] NGINX 컨테이너가 실행 중이다.
- [ ] backend-api 컨테이너가 실행 중이다.
- [ ] preprocess-worker 컨테이너가 실행 중이다.
- [ ] PostgreSQL 컨테이너가 실행 중이다.
- [ ] RabbitMQ 컨테이너가 실행 중이다.
- [ ] MinIO 컨테이너가 실행 중이다.

```bash
curl -f https://YOUR_DOMAIN/health
curl -f https://YOUR_DOMAIN/v3/api-docs
```

## 7. 브라우저 스모크 테스트

- [ ] `https://YOUR_DOMAIN`에 접속된다.
- [ ] Google 로그인이 성공한다.
- [ ] 로그인 성공 후 URL에 Access Token이 노출되지 않는다.
- [ ] 프로젝트를 생성할 수 있다.
- [ ] 이미지 또는 ZIP 파일을 업로드할 수 있다.
- [ ] 전처리 Job이 생성된다.
- [ ] Job이 `SUCCEEDED`로 끝난다.
- [ ] 처리된 이미지를 다운로드할 수 있다.
- [ ] Job 결과 ZIP을 다운로드할 수 있다.

## 8. 인증 E2E 스크립트

브라우저 로그인 후 DevTools에서 Access Token을 가져옵니다.

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

실행:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

## 9. 공개 전 보안 확인

- [ ] `REFRESH_TOKEN_COOKIE_SECURE=true`
- [ ] MinIO bucket은 private이다.
- [ ] 직접 노출할 필요 없는 포트는 외부에서 막혀 있다.
- [ ] Swagger 공개 여부를 의도적으로 결정했다.
- [ ] `.env.prod`와 private key가 repository에 없다.
- [ ] Google OAuth 테스트 사용자 또는 게시 상태를 운영 목적에 맞게 설정했다.

## 10. 운영 후속 작업

- [ ] PostgreSQL 백업 절차를 확인했다.
- [ ] Object Storage 백업 절차를 확인했다.
- [ ] 장애 시 rollback 절차를 정했다.
- [ ] 로그와 모니터링 수집 방식을 정했다.
- [ ] migration 도구 도입 후 `JPA_DDL_AUTO=validate` 전환 계획을 세웠다.
