# 13. SSE 진행률

## 목표

프론트엔드가 반복 polling 없이 Job 진행률을 받을 수 있도록 SSE endpoint를 제공합니다.

## 먼저 읽을 문서

1. `docs/api/job-api.md`
2. `docs/architecture/nginx-routing.md`

## 작업 범위

1. `GET /api/v1/jobs/{jobId}/events` endpoint 추가
2. Job 진행률 이벤트 DTO 정의
3. Job 상태 변경 시 이벤트 발행
4. NGINX SSE buffering 비활성화
5. 프론트엔드 Job 상세 화면에서 SSE 연결

## 이벤트 예시

```json
{
  "eventType": "JOB_PROGRESS",
  "jobId": 1,
  "total": 10,
  "succeeded": 7,
  "failed": 1,
  "progressPercent": 80.0
}
```

## 완료 기준

1. SSE endpoint가 Swagger 또는 문서에 반영됩니다.
2. NGINX 경유로도 이벤트가 지연 없이 전달됩니다.
3. 연결 종료와 재연결 상황에서 화면이 깨지지 않습니다.
