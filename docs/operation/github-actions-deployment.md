# GitHub Actions 운영 배포

운영 배포는 GitHub Actions가 repository를 서버로 전송하고, 서버의 `.env.prod`를 사용해 Docker Compose production stack을 실행하는 방식입니다.

Workflow 파일:

```text
.github/workflows/deploy-production.yml
```

## 실행 조건

Workflow는 아래 경우 실행됩니다.

- `workflow_dispatch`: GitHub UI에서 수동 실행
- `push` to `main`: main 반영 후 자동 실행

첫 운영 배포는 반드시 수동 실행으로 검증하는 것을 권장합니다. 서버가 안정화된 뒤 main 자동 배포를 유지할지 결정합니다.

## GitHub Environment

GitHub repository에 아래 Environment를 만듭니다.

```text
production
```

`production` Environment에 아래 secrets를 등록합니다.

| Secret | 예시 | 용도 |
| --- | --- | --- |
| `DEPLOY_HOST` | `203.0.113.10` | SSH 접속 대상 서버 |
| `DEPLOY_USER` | `deploy` | 서버 배포 유저 |
| `DEPLOY_SSH_PRIVATE_KEY` | private key text | 배포 유저 private key |
| `DEPLOY_SSH_PORT` | `22` | SSH 포트. 비워두면 22 |
| `DEPLOY_PATH` | `/opt/image-preprocess` | 서버 배포 경로 |
| `PROD_BASE_URL` | `https://YOUR_DOMAIN` | 배포 후 health check 기준 URL |

주의:

- `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, DB password 같은 애플리케이션 secret은 GitHub Actions secrets가 아니라 서버 `.env.prod`에 둡니다.
- `DEPLOY_PATH`에는 공백이나 shell quote를 넣지 않습니다.
- private key는 passphrase 없는 배포 전용 key를 권장합니다.

## 서버 사전 준비

서버에서 한 번만 수행합니다.

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/image-preprocess/shared /opt/image-preprocess/releases
sudo chown -R deploy:deploy /opt/image-preprocess
```

실제 운영 환경변수 파일을 생성합니다.

```text
/opt/image-preprocess/shared/.env.prod
```

템플릿:

```text
infra/docker-compose/.env.prod.example
```

workflow는 이 파일이 없으면 배포를 중단합니다.

## 배포 과정

Workflow는 아래 순서로 동작합니다.

1. repository checkout
2. `.env.prod.example`로 production compose 설정 검증
3. Git metadata, node_modules, output, local env 파일을 제외하고 release archive 생성
4. GitHub Actions secrets로 SSH 설정
5. archive를 `${DEPLOY_PATH}/releases/image-preprocess-release.tgz`로 업로드
6. `${DEPLOY_PATH}/current`에 압축 해제
7. `${DEPLOY_PATH}/shared/.env.prod`를 `current/infra/docker-compose/.env.prod`로 복사
8. 서버에서 Docker Compose config 검증
9. Docker Compose `up -d --build` 실행
10. 사용하지 않는 Docker image prune
11. `${PROD_BASE_URL}/health`, `${PROD_BASE_URL}/v3/api-docs` 확인

## Workflow가 사용하는 Compose 명령

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  up -d --build
```

`docker-compose.prod.yml`은 production override입니다. NGINX만 외부에 노출하고 backend-api, PostgreSQL, RabbitMQ, MinIO의 직접 공개 포트를 제거합니다.

## 배포 후 자동 확인 범위

Workflow는 인증 없는 endpoint만 확인합니다.

- `GET /health`
- `GET /v3/api-docs`

Google OAuth와 이미지 전처리 E2E는 브라우저 로그인이 필요하므로 수동 검증합니다.

## 수동 E2E 검증

1. `${PROD_BASE_URL}` 접속
2. Google 로그인
3. 프로젝트 생성
4. 이미지 또는 ZIP 업로드
5. 전처리 Job 생성
6. Worker 처리 완료 확인
7. 처리된 이미지 또는 ZIP 결과 다운로드

스크립트 검증:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

## Rollback

현재 workflow는 단일 `current` 디렉터리 방식입니다. 자동 release history rollback은 아직 없습니다.

임시 rollback 방법:

1. 이전에 성공한 commit으로 workflow를 다시 실행합니다.
2. 또는 서버에서 known-good archive를 복원하고 같은 Compose 명령을 실행합니다.

운영 안정화 이후에는 timestamped release directory와 `current` symlink 방식으로 확장하는 것이 좋습니다.
