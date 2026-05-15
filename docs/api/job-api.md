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
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=processed` | Create processed image download URL |
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=preview` | Create preview image download URL |
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=report` | Create processing report download URL |
| `GET` | `/api/v1/jobs/{jobId}/summary` | Read progress counters |
| `GET` | `/api/v1/jobs/{jobId}/events` | Subscribe to progress SSE stream |
| `POST` | `/api/v1/jobs/{jobId}/cancel` | Request cancellation |
| `POST` | `/api/v1/jobs/{jobId}/retry` | Retry failed and dead-lettered items |
| `POST` | `/api/v1/jobs/{jobId}/rerun` | Requeue all non-processing items |
| `GET` | `/api/v1/jobs/{jobId}/artifacts` | Read artifact listing placeholder |
| `GET` | `/api/v1/jobs/{jobId}/download.zip` | Create processed image ZIP download URL |

Internal Worker endpoints are not user-facing APIs. They require `X-Worker-Token` and are mounted under
`/internal/v1/**`.

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

## JobItem Artifact Download

```text
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=preview
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=report
```

The API validates that the current user can read the Job's project, finds the requested JobItem, maps the requested
artifact type to the Worker-registered object key, and returns a temporary Object Storage download URL.

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "jobId": 1,
    "itemId": 10,
    "type": "PROCESSED",
    "objectKey": "processed/1/1/10/processed.png",
    "downloadUrl": "http://localhost:9000/image-preprocess-local/processed/1/1/10/processed.png?...",
    "expiresAt": "2026-05-15T09:00:00Z"
  }
}
```

Rules:

- `type` must be one of `processed`, `preview`, or `report`.
- If the Worker has not registered the requested object key yet, the API returns `common404`.
- Debug artifact expansion remains separate from this JobItem-level result download flow.

## Processed ZIP Download

```text
GET /api/v1/jobs/{jobId}/download.zip
```

The API creates a ZIP archive from succeeded JobItems that have a `processedObjectKey`, uploads the archive to object
storage, and returns a temporary download URL. The archive contains processed images only. Preview images, processing
reports, and debug artifacts are intentionally excluded from this MVP download flow.

Archive object key:

```text
archives/{projectId}/{jobId}/processed-results.zip
```

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "jobId": 1,
    "fileCount": 3,
    "objectKey": "archives/10/1/processed-results.zip",
    "downloadUrl": "http://localhost/image-preprocess-local/archives/10/1/processed-results.zip?...",
    "expiresAt": "2026-05-16T00:00:00Z"
  }
}
```

Rules:

- The current user must have read permission for the Job's project.
- Only `SUCCEEDED` items with a non-empty `processedObjectKey` are included.
- If no processed images are ready, the API returns `common404`.
- The ZIP file is regenerated on request so the archive reflects the latest processed item set.

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

Worker state reports now refresh Job counters and publish SSE progress events through the Internal Worker API.

## Internal Worker API

Authentication:

```http
X-Worker-Token: <WORKER_INTERNAL_TOKEN>
```

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/started` | Mark item as `PROCESSING` |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/heartbeat` | Update processing heartbeat |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/succeeded` | Mark item as `SUCCEEDED` and store result keys |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/failed` | Mark item as `FAILED` and store error metadata |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/artifacts` | Register artifact object keys |
| `GET` | `/internal/v1/preprocess/presets` | Return built-in presets for Worker |

Started request:

```json
{
  "workerId": "local-worker-1",
  "attempt": 1
}
```

Succeeded request:

```json
{
  "workerId": "local-worker-1",
  "processedObjectKey": "processed/1/1/10/processed.png",
  "previewObjectKey": "processed/1/1/10/preview.png",
  "reportObjectKey": "processed/1/1/10/processing-report.json"
}
```

Failure request:

```json
{
  "workerId": "local-worker-1",
  "errorCode": "DECODE_FAILED",
  "errorMessage": "Cannot decode image.",
  "retryable": false
}
```

Rules:

- User access tokens do not authorize `/internal/**`.
- Missing or invalid `X-Worker-Token` returns `WORKER401`.
- Invalid item state transitions return `WORKER409`.
- The API updates DB state only; actual OpenCV preprocessing stays in `preprocess-worker`.

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

- Artifact listing currently returns a placeholder.
- Detailed debug artifact row expansion is deferred to a later artifact domain task.
