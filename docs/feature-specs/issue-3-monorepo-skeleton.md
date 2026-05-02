# Issue 3. Monorepo skeleton

## 이슈

- Issue: `#3`
- Title: `🧹 [Chore] backend-api, preprocess-worker, frontend, infra 기본 디렉터리 생성`
- Branch: `chore/som/3`
- Base: `docs/som/1`

## 목표

대규모 문서 이미지 전처리 플랫폼의 모노레포 기본 경로를 만든다.

## 작업 범위

1. `backend-api/` 기본 경로 생성
2. `preprocess-worker/` 기본 경로 생성
3. `frontend/` 기본 경로 생성
4. `infra/` 하위 시스템별 경로 생성
5. `docs/architecture`, `docs/api`, `docs/database`, `docs/worker`, `docs/operation` 경로 생성
6. `scripts/` 경로 생성
7. `.github/workflows/` placeholder 생성
8. `CODEX_DIRECTORY_SPEC.md` 추가

## 범위 제외

1. Spring Boot build.gradle 생성
2. Java package skeleton 생성
3. Frontend package.json 생성
4. Docker Compose 실제 서비스 정의
5. NGINX 실제 routing 설정
6. 비즈니스 로직 구현

## 완료 기준

1. API와 Worker 디렉터리가 분리되어 있다.
2. `infra`와 `frontend`가 API 코드 안에 섞여 있지 않다.
3. 빈 디렉터리가 Git에서 추적되도록 placeholder가 있다.
4. 실제 비즈니스 로직이 추가되지 않았다.

## 후속 작업

1. `backend-api` Spring skeleton 생성
2. `preprocess-worker` Spring skeleton 생성
3. `frontend` skeleton 생성
4. Docker Compose와 NGINX skeleton 생성
