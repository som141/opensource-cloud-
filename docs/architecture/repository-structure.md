# 레포지토리 구조

이 문서는 공개 사용자/개발자용 모노레포 구조 설명입니다. Codex 작업 규칙 문서는 로컬 전용으로 관리하며 원격 저장소에 올리지 않습니다.

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
└── README.md
```

## 디렉터리 책임

| 경로 | 책임 |
| --- | --- |
| `backend-api/` | Spring Boot REST API. 인증, 프로젝트, 업로드, 이미지 메타데이터, Job 등록/조회, Worker internal callback 담당 |
| `preprocess-worker/` | RabbitMQ 메시지를 소비하고 OpenCV 기반 문서 이미지 전처리 파이프라인 실행 |
| `frontend/` | React/Vite 기반 화면과 API 호출 담당 |
| `infra/` | Docker Compose, NGINX, RabbitMQ, MinIO, PostgreSQL, production override, 배포 설정 |
| `docs/` | 공개 문서, API 문서, 운영 문서, 작업 단위 명세 |
| `scripts/` | 로컬 E2E, Docker Compose preflight 등 검증 스크립트 |
| `.github/` | 이슈/PR 템플릿과 GitHub Actions workflow |

## backend-api 구조 원칙

Spring 패키지는 도메인형 구조를 사용합니다.

```text
backend-api/src/main/java/com/moonju/preprocess/api/
├── domain/
│   ├── auth/
│   ├── user/
│   ├── project/
│   ├── upload/
│   ├── image/
│   ├── job/
│   └── preprocess/
├── infra/
│   ├── security/
│   ├── oauth/
│   ├── rabbitmq/
│   ├── storage/
│   └── persistence/
└── global/
    ├── config/
    ├── error/
    ├── response/
    └── support/
```

금지 사항:

- 최상위 `controller`, `service`, `repository`, `dto` 패키지에 모든 도메인을 섞지 않습니다.
- controller가 repository를 직접 호출하지 않습니다.
- 도메인 서비스가 MinIO SDK나 RabbitMQ Template에 직접 강하게 의존하지 않습니다.
- API 서버에 OpenCV 전처리 로직을 넣지 않습니다.

## preprocess-worker 구조 원칙

```text
preprocess-worker/src/main/java/com/moonju/preprocess/worker/
├── domain/
│   ├── workerjob/
│   ├── preprocess/
│   ├── artifact/
│   └── report/
├── infra/
│   ├── rabbitmq/
│   ├── storage/
│   ├── api/
│   └── opencv/
└── global/
```

Worker는 다음 책임만 가집니다.

- RabbitMQ 메시지 consume
- Object Storage에서 원본 파일 다운로드
- 문서 이미지 전처리 파이프라인 실행
- 처리된 이미지 저장
- backend-api internal API로 상태 보고

Worker가 하지 않는 일:

- 사용자 로그인
- OAuth 처리
- 프론트 화면용 API
- API 서버 DB 직접 접근

## frontend 구조 원칙

```text
frontend/src/
├── app/
├── pages/
├── features/
├── entities/
├── shared/
└── styles/
```

프론트엔드는 화면 표시와 API 호출에 집중합니다. Bootstrap, jQuery, AdminLTE 같은 임의 UI 템플릿은 사용하지 않습니다.

## 로컬 전용 문서

다음 파일은 로컬 작업 보조용이며 Git 추적 대상에서 제외합니다.

```text
AGENTS.md
CODEX_DIRECTORY_SPEC.md
LOCAL_CONFIG.md
*.local.md
.codex/
```

공개 문서는 `README.md`와 `docs/` 하위에 작성합니다.
