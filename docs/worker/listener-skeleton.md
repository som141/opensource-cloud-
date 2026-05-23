# Worker Listener 골격

## 목적

Worker listener는 backend Job 도메인이 발행한 전처리 메시지를 소비합니다.
RabbitMQ와 OpenCV 문서 이미지 전처리 pipeline 사이의 경계입니다.

## 현재 범위

이 문서는 초기 Worker listener skeleton과 이후 구현 방향을 설명합니다.

포함 구성:

- `PreprocessWorkerApplication`
- `PreprocessJobListener`
- `PreprocessJobMessage`
- `WorkerJobService`
- `BackendApiClient` port
- `ObjectStoragePort` port
- 안전한 로컬 기본 설정

## Listener 활성화

기본값은 비활성화입니다.

```text
WORKER_LISTENER_ENABLED=false
```

RabbitMQ가 실행 중이고 Worker가 실제로 메시지를 소비해야 할 때만 활성화합니다.

```text
WORKER_LISTENER_ENABLED=true
```

## Queue

| 환경변수 | 기본값 |
| --- | --- |
| `RABBITMQ_PREPROCESS_HIGH_QUEUE` | `image.preprocess.high` |
| `RABBITMQ_PREPROCESS_NORMAL_QUEUE` | `image.preprocess.normal` |
| `RABBITMQ_PREPROCESS_RETRY_QUEUE` | `image.preprocess.retry` |

## 메시지 계약

```json
{
  "messageId": "msg-uuid",
  "jobId": 1,
  "itemId": 10,
  "projectId": 1,
  "imageId": 100,
  "userId": 20,
  "originalObjectKey": "originals/1/1/100/scan.png",
  "preset": "A4_SCAN_300DPI",
  "presetParameters": {
    "targetDpi": "300"
  },
  "debug": false,
  "priority": "NORMAL",
  "attempt": 1,
  "traceId": "trace-uuid",
  "createdAt": "2026-05-10T00:00:00Z"
}
```

## 실행 흐름

유효한 메시지는 다음 순서로 처리합니다.

1. 메시지 필수 식별자를 검증합니다.
2. backend internal API로 처리 시작을 보고합니다.
3. Object Storage port로 원본 다운로드를 준비합니다.
4. 전처리 pipeline을 실행합니다.
5. 결과 artifact를 저장합니다.
6. 성공 또는 실패 상태를 backend internal API로 보고합니다.

## 다음 작업

1. Worker internal API HTTP client 구현
2. MinIO/S3 SDK adapter 구현
3. OpenCV 기반 실제 pipeline step 연결
4. processed image, preview, report, debug artifact 업로드 연결
