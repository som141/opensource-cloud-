# 14. Worker 내부 API

## 목표

Worker가 처리 시작, heartbeat, 성공, 실패, artifact 정보를 API 서버에 보고할 수 있는 내부 API를 제공합니다.

## 먼저 읽을 문서

1. `docs/api/job-api.md`
2. `docs/worker/retry-policy.md`

## 작업 범위

1. Worker token 인증 필터 추가
2. 처리 시작 보고 API
3. heartbeat 보고 API
4. 성공 보고 API
5. 실패 보고 API
6. artifact 등록 API
7. Job/JobItem 상태 전이 검증

## 대표 endpoint

```text
POST /internal/v1/jobs/{jobId}/items/{itemId}/started
POST /internal/v1/jobs/{jobId}/items/{itemId}/heartbeat
POST /internal/v1/jobs/{jobId}/items/{itemId}/succeeded
POST /internal/v1/jobs/{jobId}/items/{itemId}/failed
POST /internal/v1/jobs/{jobId}/items/{itemId}/artifacts
```

## 완료 기준

1. 외부 사용자 인증과 Worker 인증을 섞지 않습니다.
2. Worker API는 일반 브라우저용 API로 노출하지 않습니다.
3. 실패 상태와 retry 가능 여부가 저장됩니다.
