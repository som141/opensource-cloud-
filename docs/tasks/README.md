# 작업 단위 문서

이 디렉터리는 프로젝트를 기능 단위로 쪼개 실행하기 위한 작업 목록입니다.
각 문서는 한 이슈 또는 한 PR에서 다루기 좋은 범위로 작성합니다.

## 작성 기준

- 설명은 한글로 작성합니다.
- 클래스명, API 경로, 환경변수명, queue 이름은 원문 그대로 둡니다.
- API 서버, Worker, 프론트엔드, 인프라 책임을 섞지 않습니다.
- Worker는 단순 resize가 아니라 OCR 전처리 파이프라인을 수행한다는 전제를 유지합니다.

## 작업 순서

| 번호 | 문서 | 목적 |
| --- | --- | --- |
| 00 | [레포지토리 기준](00-repository-baseline.md) | 작업 시작 전 기준 확인 |
| 01 | [모노레포 골격](01-monorepo-skeleton.md) | 최상위 구조 생성 |
| 02 | [backend-api 골격](02-backend-api-skeleton.md) | Spring API 기본 구조 |
| 03 | [Worker 골격](03-worker-skeleton.md) | Worker 애플리케이션 구조 |
| 04 | [인프라 골격](04-infra-directory-skeleton.md) | Docker/NGINX/DB 설정 위치 |
| 05 | [프론트엔드 골격](05-frontend-skeleton.md) | React/Vite 화면 구조 |
| 06 | [공통 응답과 예외](06-global-error-response.md) | API 응답 표준화 |
| 07 | [인증/사용자](07-auth-user.md) | Google OAuth와 사용자 |
| 08 | [프로젝트](08-project.md) | 프로젝트 관리 |
| 09 | [업로드](09-upload.md) | 파일/ZIP 업로드 |
| 10 | [이미지](10-image.md) | 이미지 메타데이터와 결과 |
| 11 | [전처리 프리셋](11-preprocess-preset.md) | 프리셋 관리 |
| 12 | [Job](12-job.md) | 작업 생성과 상태 |
| 13 | [SSE 진행률](13-sse-progress.md) | 실시간 진행률 |
| 14 | [Worker 내부 API](14-internal-worker-api.md) | Worker callback |
| 15 | [Worker 메시지 소비](15-worker-message-consume.md) | RabbitMQ 소비 |
| 16 | [Worker 전처리 파이프라인](16-worker-preprocess-pipeline.md) | OpenCV 단계 구현 |
| 17 | [Worker 프리셋](17-worker-preset.md) | 프리셋별 파라미터 |
| 18 | [Artifact와 Report](18-artifact-report.md) | 결과 저장과 리포트 |
| 19 | [NGINX/Docker Compose](19-nginx-docker-compose.md) | 로컬 통합 실행 |
| 20 | [관측성](20-observability.md) | Prometheus/Grafana/Trace |
| 21 | [OCR 벤치마크](21-ocr-benchmark.md) | 보류 또는 제외 대상 |
| 22 | [알림](22-notification.md) | 작업 완료/실패 알림 |
| 23 | [Admin/Audit](23-admin-audit.md) | 보류 또는 제외 대상 |
| 24 | [Kubernetes/KEDA](24-kubernetes-keda.md) | 운영 확장 |
| 25 | [최종 문서/테스트](25-final-docs-tests.md) | 배포 전 검증 |
