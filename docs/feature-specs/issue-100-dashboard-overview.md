# Issue 100 - Dashboard 운영 개요 화면

## 작업 목적

프론트 Dashboard의 placeholder를 제거하고, Docker Compose 기반 MVP에서 실제 사용자가 확인할 수 있는 운영 개요 화면을 제공한다.

이 화면은 관리자/감사/벤치마크 기능을 되살리지 않고, 현재 구현된 Project, Image, Job API만 사용한다.

## 전체 순서

1. `GET /api/v1/projects?size=100`으로 사용자가 접근 가능한 프로젝트를 조회한다.
2. 각 프로젝트에 대해 `GET /api/v1/projects/{projectId}/summary`를 호출해 이미지 수와 Job 수를 집계한다.
3. `GET /api/v1/jobs?size=100`으로 최근 전처리 Job 목록을 조회한다.
4. Job 카운터를 기준으로 대기, 처리 중, 성공, 실패, 성공률을 계산한다.
5. 최근 Job과 최근 Project를 카드 형태로 표시한다.
6. 사용자가 다음 행동으로 이동할 수 있도록 Upload, Projects, Job detail 링크를 제공한다.

## 기능 단위

### 1. Workspace Metrics

- 프로젝트 수는 project page의 `totalElements`가 있으면 우선 사용한다.
- 이미지 수는 프로젝트 요약의 `imageCount` 합계로 계산한다.
- Job 수는 job page의 `totalElements`가 있으면 우선 사용한다.
- 성공률은 `sum(succeededCount) / sum(totalCount)`로 계산한다.

### 2. Pipeline Health

- `queuedCount`, `processingCount`, `failedCount`를 전체 Job 기준으로 합산한다.
- active Job은 `CREATED`, `QUEUED`, `RUNNING`, `RETRYING`, `CANCEL_REQUESTED` 상태를 기준으로 계산한다.
- 별도 Admin API나 RabbitMQ 관리 API는 사용하지 않는다.

### 3. Recent Jobs

- `createdAt` 내림차순으로 최근 6개 Job을 표시한다.
- 각 항목은 Job ID, Project ID, preset, 상태, 성공/실패/대기/처리 중 카운터를 표시한다.
- Job detail 화면으로 이동하는 링크를 제공한다.

### 4. Recent Projects

- `updatedAt` 내림차순으로 최근 4개 프로젝트를 표시한다.
- 각 항목은 이름, 설명, 권한, 이미지 수, Job 수, 기본 프리셋을 표시한다.
- Project detail과 Upload 화면으로 이동하는 링크를 제공한다.

### 5. Auth Handling

- API 호출은 기존 `apiClient`를 사용한다.
- access token은 UI에 표시하지 않는다.
- 401 발생 시 기존 refresh-cookie 기반 재발급 흐름을 그대로 사용한다.
- 인증 실패 시 Google 로그인 CTA와 retry 버튼을 표시한다.

## 제외 범위

- Admin dashboard
- Audit log
- OCR/Benchmark 화면
- RabbitMQ queue health 직접 조회
- Prometheus/Grafana 지표 연동

위 항목은 현재 MVP 범위에서 제외된 상태다.

## 검증 기준

- `frontend/src/pages/DashboardPage.tsx`가 placeholder가 아닌 실제 API 기반 화면을 렌더링한다.
- `frontend/src/styles/global.css`는 기존 디자인 시스템을 확장하며 외부 UI 템플릿을 추가하지 않는다.
- `npm run build`가 통과한다.
- `git diff --check`가 통과한다.
