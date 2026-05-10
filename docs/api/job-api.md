# Job API

## Purpose

Job API registers large-scale document image preprocessing work. It validates project access, selected images, and
preprocess preset parameters, then publishes image-level RabbitMQ messages for Worker execution.

The API server does not execute OpenCV preprocessing. Worker execution and artifact upload are handled by later Worker
tasks.

## State Model

### Job Status

| Status | Meaning |
| --- | --- |
| `CREATED` | Job entity was created before queueing |
| `QUEUED` | JobItems were queued and messages were published |
| `RUNNING` | At least one Worker is processing an item |
| `PARTIAL_SUCCEEDED` | Some items succeeded and some failed |
| `SUCCEEDED` | All items succeeded |
| `FAILED` | Job failed without successful completion |
| `CANCEL_REQUESTED` | User requested cancellation |
| `CANCELLED` | Job was cancelled |
| `RETRYING` | Failed or selected items were requeued |

### JobItem Status

| Status | Meaning |
| --- | --- |
| `PENDING` | Item exists but is not queued yet |
| `QUEUED` | Item is ready for Worker consumption |
| `PROCESSING` | Worker is processing the image |
| `SUCCEEDED` | Worker completed successfully |
| `FAILED` | Worker reported a retryable or final failure |
| `SKIPPED` | Item was skipped intentionally |
| `CANCELLED` | Item was cancelled before processing |
| `RETRYING` | Item was requeued |
| `DEAD_LETTERED` | Item exceeded retry handling and moved to DLQ |

## Endpoints

All endpoints require `Authorization: Bearer <access-token>`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/jobs` | Create preprocessing job |
| `GET` | `/api/v1/jobs` | List my jobs |
| `GET` | `/api/v1/jobs/{jobId}` | Read job detail |
| `GET` | `/api/v1/jobs/{jobId}/items` | List image-level job items |
| `GET` | `/api/v1/jobs/{jobId}/summary` | Read progress counters |
| `GET` | `/api/v1/jobs/{jobId}/events` | Subscribe to progress SSE stream |
| `POST` | `/api/v1/jobs/{jobId}/cancel` | Request cancellation |
| `POST` | `/api/v1/jobs/{jobId}/retry` | Retry failed and dead-lettered items |
| `POST` | `/api/v1/jobs/{jobId}/rerun` | Requeue all non-processing items |
| `GET` | `/api/v1/jobs/{jobId}/artifacts` | Read artifact listing placeholder |
| `GET` | `/api/v1/jobs/{jobId}/download.zip` | Read ZIP download placeholder |

## Create Job

Request:

```json
{
  "projectId": 1,
  "imageIds": [100, 101, 102],
  "preset": "LOW_CONTRAST_SCAN",
  "presetParameters": {
    "targetDpi": "300",
    "binarizationMode": "adaptive"
  },
  "debug": false,
  "priority": "NORMAL",
  "outputOptions": {
    "saveProcessedImage": true,
    "savePreview": true,
    "saveReportJson": true,
    "saveDebugArtifacts": false
  }
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common201",
  "message": "Created.",
  "result": {
    "jobId": 1,
    "status": "QUEUED",
    "totalImages": 3,
    "queuedImages": 3,
    "createdAt": "2026-05-10T12:00:00"
  }
}
```

Rules:

- `imageIds` must be non-empty and unique.
- All images must belong to the requested project.
- Deleted images are rejected.
- Preset parameters are validated by `POST /api/v1/preprocess/presets/validate` logic before messages are published.
- The API publishes one RabbitMQ message per image.

## RabbitMQ Routing

| Priority | Queue |
| --- | --- |
| `HIGH` | `image.preprocess.high` |
| `LOW` | `image.preprocess.normal` |
| `NORMAL` | `image.preprocess.normal` |

Retry requests publish to `image.preprocess.retry`.

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
  "preset": "LOW_CONTRAST_SCAN",
  "presetParameters": {
    "targetDpi": "300"
  },
  "debug": false,
  "priority": "NORMAL",
  "attempt": 1,
  "traceId": "trace-uuid",
  "createdAt": "2026-05-10T03:00:00Z"
}
```

## List And Detail

```text
GET /api/v1/jobs?page=0&size=20
GET /api/v1/jobs/{jobId}
GET /api/v1/jobs/{jobId}/items?page=0&size=50
```

`GET /api/v1/jobs` lists jobs created by the current user. Detail and item APIs validate project read permission.

## Summary

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "jobId": 1,
    "total": 1000,
    "queued": 120,
    "processing": 20,
    "succeeded": 850,
    "failed": 10,
    "progressPercent": 86.0
  }
}
```

`progressPercent` is calculated from `(succeeded + failed) / total * 100`.

## SSE Progress Stream

```text
GET /api/v1/jobs/{jobId}/events
Accept: text/event-stream
```

The stream validates normal Job read permission before opening the connection. On subscribe, the API sends a heartbeat
event and a current `JOB_PROGRESS` snapshot based on `GET /api/v1/jobs/{jobId}/summary`.

Event types:

| Event | Purpose |
| --- | --- |
| `HEARTBEAT` | Keeps the stream alive and verifies the connection |
| `JOB_PROGRESS` | Sends current progress counters |
| `JOB_COMPLETED` | Reserved for completed Job transition |
| `JOB_FAILED` | Reserved for failed Job transition |

Example payload:

```json
{
  "eventType": "JOB_PROGRESS",
  "jobId": 1,
  "total": 1000,
  "queued": 120,
  "processing": 20,
  "succeeded": 850,
  "failed": 10,
  "progressPercent": 86.0,
  "emittedAt": "2026-05-10T00:00:00Z"
}
```

Current limitation: Worker callbacks are not connected yet, so automatic progress publishing on every Worker state
change is deferred to the Internal Worker API task.

## Cancel

```text
POST /api/v1/jobs/{jobId}/cancel
```

Cancellation is cooperative. The API marks the Job as `CANCEL_REQUESTED` and cancels items that have not started.
Processing items are left unchanged so the Worker can finish or stop at a safe checkpoint.

## Retry

```text
POST /api/v1/jobs/{jobId}/retry
POST /api/v1/jobs/{jobId}/rerun
```

- `retry` requeues only `FAILED` and `DEAD_LETTERED` items.
- `rerun` requeues all non-`PROCESSING` items.
- Each retry increments `attempt`.
- Retried messages are published to `image.preprocess.retry`.

## Current Limitations

- SSE progress streaming currently provides the endpoint and initial summary snapshot.
- Worker success/failure callback API is not implemented in this issue.
- Artifact listing and ZIP download currently return placeholders.
