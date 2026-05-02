# 디렉터리 구조 명세

## 목적

이 문서는 대규모 문서 이미지 전처리 플랫폼의 모노레포 경계를 고정한다. 이후 구현 작업은 이 구조를 기준으로 진행한다.

## 최상위 구조

```text
opensource-cloud-/
├── backend-api/
├── preprocess-worker/
├── frontend/
├── infra/
├── docs/
├── scripts/
├── .github/
├── README.md
└── CODEX_DIRECTORY_SPEC.md
```

## 컴포넌트 책임

| 경로 | 책임 |
|---|---|
| `backend-api/` | Spring Boot REST API. 인증, 프로젝트, 업로드, 이미지 메타데이터, Job 등록/조회, SSE 진행률, Worker internal API를 담당한다. |
| `preprocess-worker/` | RabbitMQ 메시지를 소비하고 image-test 기반 OpenCV 문서 이미지 전처리 pipeline을 실행한다. |
| `frontend/` | 웹 화면과 API 호출을 담당한다. 정적 파일은 NGINX를 통해 제공한다. |
| `infra/` | NGINX, Docker Compose, RabbitMQ, MinIO, PostgreSQL, Prometheus, Grafana, OpenTelemetry, Jaeger, Kubernetes/KEDA 설정을 둔다. |
| `docs/` | 기능 명세, API 명세, DB 설계, Worker 설계, 운영 문서를 둔다. |
| `scripts/` | 로컬 실행, 정리, 운영 보조 스크립트를 둔다. |

## backend-api 예정 구조

```text
backend-api/src/main/java/com/moonju/preprocess/api/
├── domain/
├── infra/
└── global/
```

규칙:

1. 최상위 `controller`, `service`, `repository`, `dto` 패키지를 만들지 않는다.
2. 도메인 내부에 controller, service, entity, repository, dto, exception을 둔다.
3. API 서버에 OpenCV 전처리 로직을 넣지 않는다.
4. 외부 시스템 접근은 `infra` 하위 포트/어댑터로 분리한다.

## preprocess-worker 예정 구조

```text
preprocess-worker/src/main/java/com/moonju/preprocess/worker/
├── domain/
├── infra/
└── global/
```

규칙:

1. Worker는 OAuth 로그인 로직을 가지지 않는다.
2. Worker는 API DB에 직접 접속하지 않는다.
3. Worker는 단순 resize가 아니라 문서 이미지 전처리 pipeline을 수행한다.
4. 처리 상태는 backend-api internal API로 보고한다.

## frontend 예정 구조

```text
frontend/src/
├── app/
├── pages/
├── features/
├── entities/
├── shared/
└── styles/
```

규칙:

1. Bootstrap, jQuery, AdminLTE를 임의로 추가하지 않는다.
2. 프론트엔드는 화면 표시와 API 호출만 담당한다.
3. Object Storage secret을 프론트에 노출하지 않는다.

## infra 구조

```text
infra/
├── docker-compose/
├── nginx/
├── rabbitmq/
├── minio/
├── postgres/
├── monitoring/
└── k8s/
```

규칙:

1. NGINX는 `/`, `/api`, `/oauth2`, `/login/oauth2`, SSE 경로를 분리한다.
2. RabbitMQ queue 이름은 문서와 동일하게 유지한다.
3. 운영 secret 값을 커밋하지 않는다.
4. Kubernetes Worker 확장은 RabbitMQ queue length 기반 KEDA를 전제로 한다.

## 현재 단계

현재 단계는 디렉터리 skeleton만 만든다. 실제 Spring Boot, Frontend, Docker Compose, NGINX 설정 구현은 후속 이슈에서 진행한다.
