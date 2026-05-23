# 운영 배포 가이드

이 문서는 Docker Compose 기반 MVP를 클라우드 VM 또는 단일 서버에 배포하는 전체 순서입니다.  
Kubernetes/KEDA 전환 전까지는 이 문서를 운영 배포 기준으로 사용합니다.

## 1. 준비물

| 항목 | 필요 값 |
| --- | --- |
| 서버 | Ubuntu 계열 VM 또는 Docker 실행 가능한 Linux 서버 |
| 도메인 | HTTPS를 붙일 운영 도메인 |
| Google OAuth | 운영 Client ID, Client Secret |
| GitHub Secrets | SSH 배포용 secret |
| 서버 `.env.prod` | DB, RabbitMQ, MinIO, JWT, Worker token 실제 값 |

## 2. 서버 기본 준비

서버에 Docker Engine과 Docker Compose plugin을 설치합니다.

```bash
docker --version
docker compose version
```

배포 유저와 디렉터리를 만듭니다.

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo mkdir -p /opt/image-preprocess/shared /opt/image-preprocess/releases
sudo chown -R deploy:deploy /opt/image-preprocess
```

배포 유저가 Docker를 실행할 수 있는지 확인합니다.

```bash
sudo -iu deploy docker ps
```

## 3. 운영 환경변수 작성

서버에 실제 운영 env 파일을 만듭니다.

```bash
cp infra/docker-compose/.env.prod.example /opt/image-preprocess/shared/.env.prod
```

실제 운영에서는 repo 파일을 직접 복사하기보다 템플릿 내용을 참고해 서버 파일을 작성해도 됩니다.  
필수 값은 [운영 환경변수 문서](production-env.md)를 기준으로 채웁니다.

중요 원칙:

- `CHANGE_ME` 값은 모두 교체합니다.
- `YOUR_DOMAIN` 값은 실제 도메인으로 교체합니다.
- `.env.prod`는 Git에 커밋하지 않습니다.
- DB, RabbitMQ, MinIO, JWT, Worker token은 서로 다른 secret을 사용합니다.

## 4. GitHub Environment와 Secrets 등록

GitHub repository에서 `production` Environment를 만들고 아래 secrets를 등록합니다.

| Secret | 예시 | 설명 |
| --- | --- | --- |
| `DEPLOY_HOST` | `203.0.113.10` | 운영 서버 IP 또는 host |
| `DEPLOY_USER` | `deploy` | SSH 배포 유저 |
| `DEPLOY_SSH_PRIVATE_KEY` | private key text | 배포 유저 접속용 private key |
| `DEPLOY_SSH_PORT` | `22` | SSH 포트. 비워두면 22 |
| `DEPLOY_PATH` | `/opt/image-preprocess` | 서버 배포 경로 |
| `PROD_BASE_URL` | `https://YOUR_DOMAIN` | 운영 base URL |

애플리케이션 secret인 `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, DB password는 GitHub Actions secrets에 넣지 않고 서버 `.env.prod`에 둡니다.

## 5. 도메인과 HTTPS

DNS에서 운영 도메인을 서버 또는 로드밸런서로 연결합니다.  
HTTPS는 아래 중 하나로 처리합니다.

- Cloudflare, AWS ALB, NCP Load Balancer 같은 외부 TLS 종료
- 서버 앞단 Caddy/Traefik/Nginx TLS proxy
- 추후 production NGINX TLS 설정 추가

운영 OAuth와 refresh cookie는 HTTPS를 전제로 합니다.

## 6. Google OAuth 운영 설정

Google Console에 운영 URI를 등록합니다.

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
```

서버 `.env.prod`에는 아래 값을 맞춥니다.

```text
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
CORS_ALLOWED_ORIGINS=https://YOUR_DOMAIN
REFRESH_TOKEN_COOKIE_SECURE=true
```

## 7. 첫 배포 실행

GitHub Actions에서 아래 workflow를 수동 실행합니다.

```text
Actions -> Deploy Production -> Run workflow
```

workflow는 repository를 압축해 서버에 업로드하고, 서버의 shared `.env.prod`를 사용해 production compose stack을 실행합니다.

사용되는 compose 명령:

```bash
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  up -d --build
```

## 8. 배포 직후 확인

서버에서 컨테이너 상태를 확인합니다.

```bash
cd /opt/image-preprocess/current
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  ps
```

브라우저 또는 curl로 확인합니다.

```bash
curl -f https://YOUR_DOMAIN/health
curl -f https://YOUR_DOMAIN/v3/api-docs
```

## 9. 운영 E2E 검증

1. `https://YOUR_DOMAIN` 접속
2. Google 로그인
3. 프로젝트 생성
4. 이미지 또는 ZIP 업로드
5. 전처리 Job 생성
6. Job 상태가 `SUCCEEDED`가 되는지 확인
7. 처리된 이미지 또는 결과 ZIP 다운로드

스크립트 검증은 로그인 후 Access Token을 가져와 실행합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

## 10. 공개 전 결정 사항

- Swagger를 공개할지, 관리자/VPN 뒤로 숨길지 결정합니다.
- `JPA_DDL_AUTO=update`를 migration 기반 `validate`로 전환할 시점을 정합니다.
- PostgreSQL과 Object Storage 백업 주기를 정합니다.
- 장애 시 rollback 방식을 정합니다.
- Prometheus/Grafana/로그 수집을 운영 범위에 포함할지 결정합니다.

## 11. 관련 문서

- [운영 환경변수](production-env.md)
- [GitHub Actions 배포](github-actions-deployment.md)
- [HTTPS/도메인 정책](https-domain-policy.md)
- [배포 체크리스트](deployment-checklist.md)
- [최종 E2E 검증](final-e2e-verification.md)
- [백업/복구](backup-restore.md)
