# 문서 인덱스

DocPrep Cloud의 공개 문서는 사용자, 개발자, 운영자가 같은 흐름으로 읽을 수 있도록 `docs/` 하위에 둡니다.  
AI/Codex 작업 규칙 문서는 로컬 전용 파일로 관리하고 원격 저장소에는 올리지 않습니다.

## 처음 읽을 문서

| 문서 | 설명 |
| --- | --- |
| [README](../README.md) | 프로젝트 소개, 빠른 시작, 주요 링크 |
| [시스템 개요](architecture/system-overview.md) | 전체 아키텍처와 데이터 흐름 |
| [Kubernetes/KEDA 아키텍처](architecture/kubernetes-architecture.md) | Kubernetes 전환 구조와 KEDA autoscaling 기준 |
| [레포지토리 구조](architecture/repository-structure.md) | 모노레포 구성과 컴포넌트 책임 |
| [작업 계획](implementation-plan.md) | 구현 계획과 단계별 흐름 |
| [작업 단위 목록](tasks/README.md) | 기능별 작업 단위와 진행 흐름 |

## API 문서

| 문서 | 설명 |
| --- | --- |
| [API 인덱스](api/api-index.md) | API 문서 진입점 |
| [인증 API](api/auth-api.md) | Google OAuth, 토큰 재발급, 현재 사용자 |
| [프로젝트 API](api/project-api.md) | 프로젝트 생성/조회/수정 |
| [업로드 API](api/upload-api.md) | 업로드 세션, presigned URL, 완료 처리 |
| [이미지 API](api/image-api.md) | 이미지 메타데이터와 다운로드 |
| [Job API](api/job-api.md) | 전처리 작업 생성/조회/상태 |
| [Swagger/OpenAPI](api/swagger-openapi.md) | Swagger 사용 방식 |

## Worker 문서

| 문서 | 설명 |
| --- | --- |
| [전처리 파이프라인](worker/preprocess-pipeline.md) | OpenCV 기반 처리 단계 |
| [프리셋 명세](worker/preset-spec.md) | A4, 저대비, 영수증, 노이즈 문서 프리셋 |
| [리포트 JSON](worker/report-json-spec.md) | 처리 리포트 구조 |
| [재시도 정책](worker/retry-policy.md) | Worker 실패와 재시도 정책 |
| [image-test 통합](worker/image-test-integration.md) | 기존 전처리 메커니즘 연동 기준 |

## 운영 문서

| 문서 | 설명 |
| --- | --- |
| [운영 문서 인덱스](operation/README.md) | 운영/배포 문서 진입점 |
| [로컬 실행](operation/docker-compose-local.md) | Docker Compose local 실행 |
| [관측성 로컬 실행](operation/observability.md) | Prometheus, Grafana, Jaeger 실행과 확인 |
| [운영 배포 가이드](operation/production-deployment-guide.md) | 서버 준비부터 검증까지 전체 순서 |
| [Kubernetes/KEDA 배포](operation/kubernetes-deployment.md) | Kubernetes skeleton 적용 절차 |
| [운영 환경변수](operation/production-env.md) | `.env.prod`와 secret 주입 방식 |
| [GitHub Actions 배포](operation/github-actions-deployment.md) | production workflow 사용법 |
| [HTTPS/도메인 정책](operation/https-domain-policy.md) | 도메인, TLS, OAuth redirect 기준 |
| [백업/복구](operation/backup-restore.md) | PostgreSQL과 Object Storage 백업 |
| [최종 E2E 검증](operation/final-e2e-verification.md) | 운영 배포 후 검증 흐름 |
| [배포 체크리스트](operation/deployment-checklist.md) | 공개 전 필수 확인 항목 |

## 컨벤션

| 문서 | 설명 |
| --- | --- |
| [GitHub workflow](conventions/github-workflow.md) | 이슈, 브랜치, PR 규칙 |
| [코드 컨벤션](conventions/code-convention.md) | Spring/Frontend 코드 작성 기준 |
| [API 응답 컨벤션](conventions/api-response-convention.md) | 공통 응답 형식 |

## 문서 작성 기준

- 공개 문서는 기본적으로 한글로 작성합니다.
- 운영자가 따라 할 수 있도록 명령어, 환경변수, 검증 기준을 같이 적습니다.
- 기능 단위 변경은 `docs/feature-specs/issue-{번호}-{기능명}.md`에 남깁니다.
- 실제 secret 값은 문서에 적지 않고, 예시는 `CHANGE_ME`, `YOUR_DOMAIN` 형태로만 작성합니다.
