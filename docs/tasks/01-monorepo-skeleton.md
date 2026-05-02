# 01. 모노레포 골격 생성

## 목표

API, Worker, Frontend, Infra, Docs, Scripts를 분리한 모노레포 구조를 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/00-repository-baseline.md`
4. `docs/tasks/README.md`

## 작업 범위

1. 최상위 디렉터리 생성
2. 루트 문서 갱신
3. CI 디렉터리 placeholder 생성
4. 상세 문서 디렉터리 생성
5. scripts 디렉터리 생성

## 작업 순서

1. `backend-api/`를 생성한다.
2. `preprocess-worker/`를 생성한다.
3. `frontend/`를 생성한다.
4. `infra/`를 생성한다.
5. `docs/architecture`, `docs/api`, `docs/database`, `docs/worker`, `docs/operation`을 생성한다.
6. `scripts/`를 생성한다.
7. `.github/workflows/`를 생성한다.
8. 루트 `README.md`에 프로젝트 목적과 구성 요소를 정리한다.
9. 필요하면 `CODEX_DIRECTORY_SPEC.md`를 추가해 디렉터리 규칙을 고정한다.

## 산출물

1. 모노레포 최상위 디렉터리
2. 상세 문서 디렉터리
3. 루트 README 초안
4. 디렉터리 구조 명세 문서

## 완료 기준

1. API와 Worker 디렉터리가 분리되어 있다.
2. `infra`와 `frontend`가 API 코드 안에 섞여 있지 않다.
3. 문서 경로가 이후 작업 단위를 수용할 수 있다.

## 금지 사항

1. API 서버와 Worker를 같은 Spring Boot 앱으로 만들지 않는다.
2. 임의로 UI 라이브러리를 추가하지 않는다.
3. `common` 같은 대형 잡동사니 디렉터리를 만들지 않는다.
