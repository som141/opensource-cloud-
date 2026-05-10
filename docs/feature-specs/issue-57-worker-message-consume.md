# Issue 57. Worker Message Consume And Internal Report

## Feature Summary

This task connects the Worker RabbitMQ listener to the backend Internal Worker API. It gives the Worker a concrete
state-reporting path without adding actual OpenCV image processing yet.

## Implemented Units

1. `BackendApiClient` internal report contract
2. `WorkerJobReportClient` HTTP implementation
3. `WorkerRuntimeProperties` for `worker.id`
4. `WorkerJobResult.retryable`
5. Listener-level requeue/reject decision
6. Worker service started/heartbeat/failure report flow
7. Tests for Worker service, listener decisions, and HTTP client request behavior

## Internal API Calls

All calls include:

```http
X-Worker-Token: <WORKER_INTERNAL_TOKEN>
Content-Type: application/json
```

| Worker method | Backend endpoint |
| --- | --- |
| `reportStarted` | `POST /internal/v1/jobs/{jobId}/items/{itemId}/started` |
| `reportHeartbeat` | `POST /internal/v1/jobs/{jobId}/items/{itemId}/heartbeat` |
| `reportSucceeded` | `POST /internal/v1/jobs/{jobId}/items/{itemId}/succeeded` |
| `reportFailed` | `POST /internal/v1/jobs/{jobId}/items/{itemId}/failed` |
| `registerArtifacts` | `POST /internal/v1/jobs/{jobId}/items/{itemId}/artifacts` |

## Current Processing Result

The Worker still runs the pipeline skeleton only. After the skeleton executes, the Worker reports:

```text
PIPELINE_NOT_IMPLEMENTED
```

This result is non-retryable because retrying cannot make an unimplemented pipeline succeed.

## Retry Policy

| Failure | Retryable | RabbitMQ behavior |
| --- | --- | --- |
| Invalid message | No | Reject without requeue |
| Backend internal API unavailable | Yes | Immediate requeue |
| Temporary storage download failure | Yes | Immediate requeue |
| Pipeline execution exception | Yes | Immediate requeue after failure report attempt |
| Pipeline skeleton not implemented | No | Reject without requeue |

## Environment

```env
BACKEND_API_INTERNAL_URL=http://backend-api:8080
WORKER_INTERNAL_TOKEN=local-worker-token
WORKER_ID=local-worker-1
WORKER_LISTENER_ENABLED=false
```

`WORKER_INTERNAL_TOKEN` must match backend-api's `WORKER_INTERNAL_TOKEN`.

## Out Of Scope

1. Actual OpenCV processing
2. Real object download/upload
3. Artifact report generation
4. Success callback from a real processed output
5. OCR text extraction
