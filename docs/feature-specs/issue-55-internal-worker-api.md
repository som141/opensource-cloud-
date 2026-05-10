# Issue 55. Internal Worker API Skeleton

## Feature Summary

This task adds internal-only Worker report APIs to the backend API. The Worker can report item lifecycle state and
artifact object keys while the API server updates PostgreSQL state and publishes SSE progress events.

## Implemented Units

1. Worker internal token configuration
2. `X-Worker-Token` authentication filter for `/internal/**`
3. Internal item state report endpoints
4. Internal Worker preset lookup endpoint
5. `JobItem` state transition methods for processing, heartbeat, success, failure, and artifacts
6. `Job` progress counter refresh from item status
7. Unit tests for filter, entity transitions, service logic, and controllers

## API Contract

### Authentication

```http
X-Worker-Token: <WORKER_INTERNAL_TOKEN>
```

`WORKER_INTERNAL_TOKEN` is supplied to both `backend-api` and `preprocess-worker` through local env or deployment
secrets.

### Started

```http
POST /internal/v1/jobs/{jobId}/items/{itemId}/started
```

```json
{
  "workerId": "local-worker-1",
  "attempt": 1
}
```

### Heartbeat

```http
POST /internal/v1/jobs/{jobId}/items/{itemId}/heartbeat
```

```json
{
  "workerId": "local-worker-1"
}
```

### Succeeded

```http
POST /internal/v1/jobs/{jobId}/items/{itemId}/succeeded
```

```json
{
  "workerId": "local-worker-1",
  "processedObjectKey": "processed/1/1/10/processed.png",
  "previewObjectKey": "processed/1/1/10/preview.png",
  "reportObjectKey": "processed/1/1/10/processing-report.json"
}
```

### Failed

```http
POST /internal/v1/jobs/{jobId}/items/{itemId}/failed
```

```json
{
  "workerId": "local-worker-1",
  "errorCode": "DECODE_FAILED",
  "errorMessage": "Cannot decode image.",
  "retryable": false
}
```

### Artifacts

```http
POST /internal/v1/jobs/{jobId}/items/{itemId}/artifacts
```

```json
{
  "processedObjectKey": "processed/1/1/10/processed.png",
  "previewObjectKey": "processed/1/1/10/preview.png",
  "reportObjectKey": "processed/1/1/10/processing-report.json"
}
```

## State Rules

1. `started` accepts `PENDING`, `QUEUED`, `RETRYING`, or already `PROCESSING` items.
2. `heartbeat`, `succeeded`, and `failed` accept only `PROCESSING` items.
3. `artifacts` accepts `PROCESSING` or `SUCCEEDED` items.
4. Invalid state transitions return `WORKER409`.
5. After report handling, the API refreshes `Job` counters from all related `JobItem` rows.

## Environment

Backend API:

```env
WORKER_INTERNAL_TOKEN=local-worker-token
```

Docker Compose passes the same token to both services:

```yaml
backend-api:
  environment:
    WORKER_INTERNAL_TOKEN: ${WORKER_INTERNAL_TOKEN:-local-worker-token}

preprocess-worker:
  environment:
    WORKER_INTERNAL_TOKEN: ${WORKER_INTERNAL_TOKEN:-local-worker-token}
```

## Out Of Scope

1. Actual image preprocessing execution remains in `preprocess-worker`.
2. RabbitMQ consumer-to-internal-API wiring is handled by the Worker implementation task.
3. Debug artifact detail rows are not expanded in this skeleton.
