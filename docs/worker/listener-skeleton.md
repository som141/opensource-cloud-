# Worker Listener Skeleton

## Purpose

The Worker listener consumes preprocessing messages produced by the backend Job domain. It is the boundary between
RabbitMQ and the future OpenCV document image preprocessing pipeline.

## Current Scope

Implemented in issue 41:

- `PreprocessWorkerApplication`
- `PreprocessJobListener`
- `PreprocessJobMessage`
- `WorkerJobService`
- `BackendApiClient` port
- `ObjectStoragePort` port
- safe local configuration defaults

## Listener Activation

The listener is disabled by default:

```text
WORKER_LISTENER_ENABLED=false
```

Enable it only when RabbitMQ is running and the team intentionally wants the Worker to consume messages:

```text
WORKER_LISTENER_ENABLED=true
```

## Queues

| Environment Variable | Default |
| --- | --- |
| `RABBITMQ_PREPROCESS_HIGH_QUEUE` | `image.preprocess.high` |
| `RABBITMQ_PREPROCESS_NORMAL_QUEUE` | `image.preprocess.normal` |
| `RABBITMQ_PREPROCESS_RETRY_QUEUE` | `image.preprocess.retry` |

## Message Contract

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

## Current Runtime Behavior

Until the OpenCV preprocessing pipeline is fully implemented, a valid message follows this skeleton flow:

1. Validate message identity fields.
2. Report started through the backend API client boundary.
3. Prepare object storage download through the storage port boundary.
4. Execute the preprocessing pipeline skeleton.
5. Report `PIPELINE_NOT_IMPLEMENTED`.

This prevents the skeleton Worker from pretending to process images successfully before the real pipeline exists.

## Next Work

1. Add Worker internal API HTTP client implementation.
2. Add MinIO/S3 SDK adapter implementation.
3. Replace pipeline skeleton steps with image-test/OpenCV-backed implementations.
4. Connect pipeline result to processed image, preview, report, and debug artifact upload.
