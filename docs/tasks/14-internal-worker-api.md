# 14. Internal Worker API

## Goal

Implement internal-only APIs that allow the preprocessing Worker to report image-level execution state and artifact
metadata back to the Spring API server.

The API server still does not execute OpenCV preprocessing. The Worker consumes RabbitMQ messages, downloads original
objects, runs the document preprocessing pipeline, uploads artifacts, and calls these internal endpoints only to update
state.

## Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/12-job.md`
4. `docs/tasks/13-sse-progress.md`
5. `docs/api/job-api.md`

## Scope

1. Add Worker service-token validation with `X-Worker-Token`.
2. Add item started report API.
3. Add item heartbeat report API.
4. Add item succeeded report API.
5. Add item failed report API.
6. Add artifact metadata registration API.
7. Add Worker preset lookup API.
8. Refresh Job progress counters after Worker state reports.
9. Publish SSE progress/completed/failed events after Worker state reports.

## Order

1. Add Worker token configuration to `application.yml` and local env examples.
2. Add `WorkerAuthenticationFilter` for `/internal/**`.
3. Restrict `/internal/**` to `ROLE_WORKER` in Spring Security.
4. Extend `JobItem` with processing, heartbeat, success, failure, and artifact registration methods.
5. Extend `Job` with counter refresh logic from item statuses.
6. Add request/response DTOs under `domain.job.dto`.
7. Add `InternalWorkerJobService`.
8. Add `InternalWorkerJobController`.
9. Add `InternalWorkerPresetController`.
10. Add unit tests for entity state transitions, service logic, controllers, and Worker token filter.
11. Update `docs/api/job-api.md`.
12. Add `docs/feature-specs/issue-55-internal-worker-api.md`.

## Endpoints

All endpoints require:

```http
X-Worker-Token: <WORKER_INTERNAL_TOKEN>
```

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/started` | Mark item as `PROCESSING` |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/heartbeat` | Update processing heartbeat |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/succeeded` | Mark item as `SUCCEEDED` and store result keys |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/failed` | Mark item as `FAILED` and store failure code |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/artifacts` | Register artifact keys separately |
| `GET` | `/internal/v1/preprocess/presets` | Return built-in preprocessing presets for Worker |

## Done Criteria

1. Normal user access tokens cannot call `/internal/**`.
2. Missing or invalid `X-Worker-Token` returns `WORKER401`.
3. Worker state reports validate current `JobItem` state.
4. Job counters update after started, heartbeat, succeeded, and failed reports.
5. SSE progress is published after Worker state changes.
6. Worker still does not connect directly to the API database.

## Forbidden

1. Do not expose internal Worker APIs as user-facing APIs.
2. Do not authorize Worker APIs with user access tokens.
3. Do not mark Worker failure as success.
4. Do not add image preprocessing logic to the API server.
