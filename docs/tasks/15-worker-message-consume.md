# 15. Worker 메시지 소비

## 목표

RabbitMQ에 발행된 전처리 메시지를 Worker가 소비하고, 이미지 하나를 처리하는 작업 단위로 실행합니다.

## 먼저 읽을 문서

1. `docs/worker/listener-skeleton.md`
2. `docs/worker/retry-policy.md`
3. `docs/api/job-api.md`

## 작업 범위

1. `PreprocessJobListener` 구현
2. queue별 listener 연결
3. 메시지 필수 필드 검증
4. 처리 시작 callback
5. Object Storage 원본 다운로드
6. 전처리 pipeline 실행
7. 성공 또는 실패 callback
8. ack/nack/retry 정책 적용

## 메시지 계약

```json
{
  "messageId": "msg-uuid",
  "jobId": 1,
  "itemId": 10,
  "projectId": 1,
  "imageId": 100,
  "originalObjectKey": "originals/1/1/100/scan.png",
  "preset": "A4_SCAN_300DPI",
  "debug": false,
  "priority": "NORMAL",
  "attempt": 1
}
```

## 완료 기준

1. Worker가 API DB에 직접 접속하지 않습니다.
2. 메시지 실패가 무조건 성공 ack로 숨겨지지 않습니다.
3. retry 가능한 오류와 불가능한 오류를 구분합니다.
