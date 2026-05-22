# Issue 102 - Job 취소와 실패 재시도 UI

## 작업 목적

Job detail 화면에서 사용자가 전처리 작업을 직접 운영할 수 있도록 기존 Job command API를 연결한다.

이번 범위는 UI 연결만 다룬다. Backend의 cancel/retry API는 이미 구현되어 있으며, 전체 rerun은 실수 위험이 커서 MVP 화면에서는 노출하지 않는다.

## 전체 순서

1. Job detail 진입 시 기존처럼 Job, summary, JobItem 목록을 조회한다.
2. Job 상태가 취소 가능한 상태인지 계산한다.
3. JobItem 중 `FAILED`, `DEAD_LETTERED` 상태인 항목 수를 계산한다.
4. 사용자가 취소 버튼을 누르면 확인창 후 `POST /api/v1/jobs/{jobId}/cancel`을 호출한다.
5. 사용자가 실패 재시도 버튼을 누르면 확인창 후 `POST /api/v1/jobs/{jobId}/retry`를 호출한다.
6. command API 성공 후 Job, summary, JobItem 목록을 다시 조회한다.
7. 처리 결과 메시지를 화면 상단에 표시한다.

## 기능 단위

### 1. Cancel Job

- 연결 API: `POST /api/v1/jobs/{jobId}/cancel`
- 취소 버튼 노출 위치: Job detail의 `Controls and downloads` 섹션
- 버튼 활성 조건:
  - Job 상태가 `CREATED`, `QUEUED`, `RUNNING`, `RETRYING` 중 하나
  - 다른 Job action이 진행 중이 아님
- 버튼 비활성 조건:
  - `SUCCEEDED`, `FAILED`, `PARTIAL_SUCCEEDED`, `CANCEL_REQUESTED`, `CANCELLED`
  - 이미 command API 요청이 진행 중인 경우

### 2. Retry Failed Items

- 연결 API: `POST /api/v1/jobs/{jobId}/retry`
- 재시도 대상:
  - `FAILED`
  - `DEAD_LETTERED`
- 버튼 라벨은 재시도 가능한 항목 수를 포함한다.
- command API 응답의 `queuedItems`를 성공 메시지에 표시한다.
- 전체 rerun API는 이번 UI에서 제공하지 않는다.

### 3. Data Refresh

- command API 성공 후 `loadJob(false)`를 호출한다.
- 재조회 대상:
  - `GET /api/v1/jobs/{jobId}`
  - `GET /api/v1/jobs/{jobId}/summary`
  - `GET /api/v1/jobs/{jobId}/items?size=500`
- 기존 자동 polling 흐름은 유지한다.

### 4. Error Handling

- command API 실패 시 기존 `status-card error` 영역에 에러 메시지를 표시한다.
- 성공 시 `status-card success` 영역에 command 결과를 표시한다.
- access token/refresh token 처리는 기존 `apiClient` 흐름을 그대로 사용한다.

## 제외 범위

- `POST /api/v1/jobs/{jobId}/rerun` UI 노출
- JobItem 개별 retry API
- Backend cancel/retry 로직 변경
- Worker retry policy 변경
- Preview/report/debug artifact 다운로드

## 검증 기준

- `frontend/src/pages/JobDetailPage.tsx`에서 cancel/retry failed items 버튼이 표시된다.
- 버튼 활성/비활성 조건이 Job/JobItem 상태와 맞는다.
- command API 호출 후 Job detail 데이터가 다시 로드된다.
- `frontend/src/styles/global.css`는 기존 디자인 시스템을 확장하며 외부 UI 템플릿을 추가하지 않는다.
- `npm run build`가 통과한다.
- `git diff --check`가 통과한다.
