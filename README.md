# 대규모 문서 이미지 전처리 플랫폼

대량의 스캔 문서 이미지를 OCR에 적합한 상태로 전처리하는 비동기 분산 처리 플랫폼이다.

## 핵심 구조

1. NGINX는 프론트엔드와 Spring REST API의 단일 진입점 역할을 한다.
2. Spring Boot API는 인증, 프로젝트, 업로드, 이미지 메타데이터, Job 등록/조회, SSE 진행률을 담당한다.
3. Preprocess Worker는 RabbitMQ 메시지를 소비하고 `image-test` 기반 OpenCV 문서 이미지 전처리 pipeline을 실행한다.
4. PostgreSQL은 사용자, 프로젝트, 이미지, 작업 상태, 리포트 메타데이터를 저장한다.
5. MinIO/S3는 원본 이미지, 처리 결과, preview, debug artifact, processing report를 저장한다.
6. Prometheus, Grafana, OpenTelemetry, Jaeger는 관측성을 담당한다.

## 작업 규칙

작업 전 반드시 아래 문서를 읽는다.

1. `.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/README.md`
4. 현재 작업에 해당하는 `docs/tasks/*.md`

GitHub 작업은 이슈 기반 브랜치와 PR 단위로 진행한다. 자세한 규칙은 `docs/conventions/github-workflow.md`를 따른다.

## 모노레포 구성

```text
backend-api/        Spring Boot REST API 서버
preprocess-worker/  RabbitMQ 기반 이미지 전처리 Worker
frontend/           NGINX로 정적 배포되는 웹 프론트엔드
infra/              Docker Compose, NGINX, RabbitMQ, MinIO, PostgreSQL, monitoring, k8s 설정
docs/               아키텍처, API, DB, Worker, 운영 문서
scripts/            로컬 실행과 운영 보조 스크립트
```

## 금지 사항

1. Worker를 단순 이미지 리사이징 서비스로 축소하지 않는다.
2. API 서버에 OpenCV 전처리 로직을 넣지 않는다.
3. Worker에 로그인, 사용자 인증, 화면용 API 로직을 넣지 않는다.
4. Spring 패키지를 최상위 `controller`, `service`, `repository`, `dto` 계층형으로 만들지 않는다.
5. Bootstrap, jQuery, AdminLTE 같은 임의 UI 템플릿을 추가하지 않는다.
