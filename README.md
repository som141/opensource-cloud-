# DocPrep Cloud

대규모 스캔 문서 이미지를 OCR 전에 안정적으로 전처리하는 비동기 SaaS 플랫폼입니다.
이 프로젝트는 단순 이미지 리사이징 서비스가 아니라, Spring Boot API와 RabbitMQ Worker를 분리해 대량 문서 이미지를 큐 기반으로 처리하는 시스템입니다.

## 핵심 가치

- **대량 처리**: 여러 이미지 또는 ZIP 파일을 업로드하면 이미지 단위 `JobItem`으로 분리하고 큐 기반으로 처리합니다.
- **OCR 전처리 특화**: Worker는 `decode`, 색상 정규화, 방향 보정, `deskew`, `crop`, `denoise`, 대비 정규화, 이진화, 형태학 정리, DPI 정규화, 선명화 단계를 수행합니다.
- **API와 Worker 분리**: Spring REST API는 인증, 프로젝트, 업로드, 작업 등록/조회만 담당하고, 이미지 전처리는 Worker가 담당합니다.
- **운영 가능한 구조**: NGINX 단일 진입점, PostgreSQL, MinIO/S3, RabbitMQ, Docker Compose, Prometheus/Grafana/Jaeger, GitHub Actions 배포 흐름을 포함합니다.
- **보안 기본값**: Google OAuth 로그인, `HttpOnly` refresh token cookie, private object storage, 내부 Worker token 구조를 사용합니다.

## 현재 MVP 기능

| 영역 | 기능 |
| --- | --- |
| 인증 | Google OAuth 로그인, Access Token 재발급, Refresh Token `HttpOnly` Cookie 저장 |
| 프로젝트 | 프로젝트 생성, 목록 조회, 상세 조회, 사용자별 프로젝트 관리 |
| 업로드 | 다중 이미지 업로드, ZIP 업로드, presigned URL 기반 Object Storage 업로드 |
| 작업 | 전처리 Job 생성, JobItem 상태 관리, Worker callback, 실패 상태 저장 |
| 결과 | 처리된 이미지 다운로드, Job 결과 ZIP 다운로드 |
| 프론트엔드 | 프로젝트, 업로드, Job 상세, 이미지 상세, 대시보드 MVP 화면 |
| 운영 | Docker Compose local/prod, NGINX routing, Prometheus/Grafana/Jaeger 관측성, GitHub Actions production deploy workflow |

## 아키텍처

```text
Browser
  |
  v
NGINX
  |-- /                 -> Frontend
  |-- /api/*            -> Spring Boot backend-api
  |-- /oauth2/*         -> Spring Security OAuth2
  |-- /login/oauth2/*   -> OAuth callback
  |
  v
backend-api
  |-- PostgreSQL        -> user, project, image, job metadata
  |-- MinIO/S3          -> original, processed output
  |-- RabbitMQ          -> preprocess queue
                            |
                            v
                    preprocess-worker
                      image-test 기반 OpenCV 전처리 파이프라인
```

## 모노레포 구성

```text
backend-api/          Spring Boot REST API
preprocess-worker/   RabbitMQ 기반 문서 이미지 전처리 Worker
frontend/            React/Vite 프론트엔드
infra/               Docker Compose, NGINX, RabbitMQ, MinIO, PostgreSQL, 배포 설정
docs/                사용자 문서, API 문서, 운영 문서, 작업 명세
scripts/             로컬 검증과 운영 보조 스크립트
```

## 빠른 시작

### 1. 로컬 환경변수 준비

```powershell
Copy-Item infra/docker-compose/.env.example infra/docker-compose/.env
```

Google OAuth를 실제로 테스트하려면 `infra/docker-compose/.env`에 Google Client ID/Secret을 넣고 Google Console에 아래 redirect URI를 등록합니다.

```text
http://localhost/login/oauth2/code/google
```

### 2. Docker Compose 실행

```powershell
docker compose -f infra/docker-compose/docker-compose.local.yml --env-file infra/docker-compose/.env up -d --build
```

### 3. 접속 주소

| 서비스 | 주소 |
| --- | --- |
| Frontend | `http://localhost` |
| Backend API | `http://localhost/api` |
| Swagger UI | `http://localhost/swagger-ui/index.html` |
| MinIO Console | `http://localhost:9001` |
| RabbitMQ Management | `http://localhost:15672` |

### 4. 로컬 E2E 스모크 테스트

브라우저에서 로그인한 뒤 Access Token을 가져와 실행합니다.

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

```powershell
.\scripts\local-e2e-smoke.ps1 -BaseUrl "http://localhost/api" -AccessToken "<access-token>"
```

## 운영 배포 요약

운영 배포는 GitHub Actions와 Docker Compose production override를 기준으로 합니다.

1. 운영 서버에 Docker Engine과 Docker Compose plugin을 설치합니다.
2. 서버에 `/opt/image-preprocess/shared/.env.prod`를 생성합니다.
3. GitHub `production` Environment에 SSH 배포 secrets를 등록합니다.
4. 도메인과 HTTPS를 연결합니다.
5. Google OAuth Console에 운영 redirect URI를 추가합니다.
6. GitHub Actions의 `Deploy Production` workflow를 실행합니다.
7. 로그인, 업로드, Worker 처리, 결과 다운로드까지 E2E로 확인합니다.

자세한 절차는 [운영 문서](docs/operation/README.md)를 기준으로 진행합니다.

## 문서

| 문서 | 설명 |
| --- | --- |
| [문서 인덱스](docs/README.md) | 프로젝트 사용자 문서 진입점 |
| [시스템 개요](docs/architecture/system-overview.md) | 컴포넌트와 데이터 흐름 |
| [Kubernetes/KEDA 아키텍처](docs/architecture/kubernetes-architecture.md) | Kubernetes 전환 구조와 Worker autoscaling |
| [레포지토리 구조](docs/architecture/repository-structure.md) | 모노레포 디렉터리와 책임 |
| [API 문서](docs/api/api-index.md) | 도메인별 API 문서 진입점 |
| [Worker 파이프라인](docs/worker/preprocess-pipeline.md) | OpenCV 전처리 단계 |
| [로컬 실행](docs/operation/docker-compose-local.md) | Docker Compose 로컬 실행 |
| [관측성 로컬 실행](docs/operation/observability.md) | Prometheus, Grafana, Jaeger 실행과 확인 |
| [Kubernetes/KEDA 배포](docs/operation/kubernetes-deployment.md) | Kubernetes skeleton 적용 절차 |
| [운영 배포](docs/operation/production-deployment-guide.md) | 운영 배포 전체 순서 |
| [GitHub Actions 배포](docs/operation/github-actions-deployment.md) | CI/CD 배포 workflow |
| [GHCR 이미지 빌드/푸시](docs/operation/ghcr-image-workflow.md) | Kubernetes 배포용 이미지 생성 |
| [Kubernetes manifest 렌더링](docs/operation/kubernetes-manifest-render-workflow.md) | 이미지 태그를 주입한 K8s YAML artifact 생성 |
| [환경변수와 Secrets](docs/operation/production-env.md) | 운영 secret 주입 방식 |
| [배포 체크리스트](docs/operation/deployment-checklist.md) | 공개 전 점검 항목 |

## 공개 문서와 로컬 AI 규칙 분리

GitHub에 올라가는 문서는 `README.md`와 `docs/` 하위 사용자 문서입니다.
Codex/AI 작업 규칙 문서는 `AGENTS.md`, `CODEX_DIRECTORY_SPEC.md`, `LOCAL_CONFIG.md` 같은 로컬 전용 문서로 관리하며 Git 추적 대상에서 제외합니다.

## 개발 원칙

- Spring은 도메인형 패키지 구조를 유지합니다.
- API 서버는 OpenCV 전처리를 직접 수행하지 않습니다.
- Worker는 OAuth, 사용자 인증, 화면용 API를 갖지 않습니다.
- Object Storage 접근은 adapter/port 구조로 분리합니다.
- Bootstrap, jQuery, AdminLTE 같은 임의 템플릿을 추가하지 않습니다.
- 기능 변경 PR은 관련 문서와 검증 결과를 함께 포함합니다.
