# 15. Worker Message Consume

## Goal

Connect `preprocess-worker` RabbitMQ message consumption to the backend Internal Worker API.

The Worker must consume image-level preprocessing messages, validate the message, report processing state to
`backend-api`, and decide whether RabbitMQ should requeue or reject the message. This task does not implement actual
OpenCV image preprocessing. The current pipeline still runs the skeleton and reports `PIPELINE_NOT_IMPLEMENTED`.

## Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/03-worker-skeleton.md`
4. `docs/tasks/14-internal-worker-api.md`
5. `docs/worker/retry-policy.md`
6. `docs/worker/preprocess-pipeline.md`

## Scope

1. RabbitMQ listener result handling
2. Message validation before external calls
3. Backend Internal Worker API HTTP client
4. Started, heartbeat, failed, succeeded, and artifact report client methods
5. Retryable vs non-retryable result model
6. RabbitMQ requeue/reject skeleton
7. Tests for listener, WorkerJobService, and Backend API client

## Order

1. Extend `BackendApiClient` with internal report methods.
2. Implement `WorkerJobReportClient` with `X-Worker-Token`.
3. Add `WorkerRuntimeProperties` for `worker.id`.
4. Extend `WorkerJobResult` with `retryable`.
5. Update `WorkerJobService` flow:
   - validate message
   - report started
   - prepare storage download
   - report heartbeat
   - run pipeline skeleton
   - report non-retryable `PIPELINE_NOT_IMPLEMENTED`
6. Update listener behavior:
   - success returns normally and auto-acks
   - retryable failure throws `ImmediateRequeueAmqpException`
   - non-retryable failure throws `AmqpRejectAndDontRequeueException`
7. Add unit tests.
8. Update docs.

## Runtime Flow

```text
RabbitMQ message
  -> PreprocessJobListener.handle
  -> WorkerJobService.process
  -> validate message
  -> POST /internal/v1/jobs/{jobId}/items/{itemId}/started
  -> ObjectStoragePort.prepareDownload
  -> POST /internal/v1/jobs/{jobId}/items/{itemId}/heartbeat
  -> PreprocessPipelineRunner.run
  -> POST /internal/v1/jobs/{jobId}/items/{itemId}/failed
  -> listener reject or requeue based on retryable flag
```

## Done Criteria

1. Worker does not connect directly to the backend database.
2. Worker calls backend internal APIs with `X-Worker-Token`.
3. Invalid messages are rejected without backend/storage calls.
4. Backend internal API failure is retryable.
5. Temporary storage download failure is retryable.
6. Current pipeline skeleton failure is non-retryable and reported as `PIPELINE_NOT_IMPLEMENTED`.
7. Tests pass.

## Forbidden

1. Do not implement OpenCV processing in this task.
2. Do not add OCR text extraction.
3. Do not call user-facing APIs from Worker.
4. Do not acknowledge failed retryable messages as if they succeeded.
