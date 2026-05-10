# Worker Retry Policy

## Purpose

This document defines the retry contract shared by the backend Job domain and future Worker implementation.

## Queue Layout

| Queue | Purpose |
| --- | --- |
| `image.preprocess.high` | High priority preprocessing work |
| `image.preprocess.normal` | Normal and low priority preprocessing work |
| `image.preprocess.retry` | Explicit retry and rerun work |
| `image.preprocess.dlq` | Final failed messages after retry exhaustion |

## Retryable Failures

The following failures should be treated as retryable by the Worker:

- Temporary Object Storage download failure.
- Temporary Object Storage upload failure.
- Backend internal API timeout.
- RabbitMQ delivery interruption before acknowledgement.
- Worker process crash before message acknowledgement.

## Non-Retryable Failures

The following failures should fail the item without repeated retry unless the user manually reruns it:

- Image decode failure.
- Unsupported file content despite accepted metadata.
- Invalid preset name.
- Invalid preset parameter payload.
- Corrupted image bytes.

## Backend Behavior Implemented In Issue 39

- `POST /api/v1/jobs/{jobId}/retry` requeues only `FAILED` and `DEAD_LETTERED` items.
- `POST /api/v1/jobs/{jobId}/rerun` requeues all non-`PROCESSING` items.
- Retried items move to `RETRYING`.
- Retried item `attempt` is incremented.
- Retry messages are published to `image.preprocess.retry`.

## Future Worker Behavior

1. Consume a message from the selected queue.
2. Report item started through the internal Worker API.
3. Download the original image from Object Storage.
4. Execute the OpenCV preprocessing pipeline.
5. Upload processed image, preview, processing report, and optional debug artifacts.
6. Report success or failure through the internal Worker API.
7. Acknowledge RabbitMQ only after the item result is safely reported.

## DLQ Rules

The exact automatic retry count is deferred to the Worker listener task. The target policy is:

- Retry transient failures up to 3 attempts.
- Send messages to `image.preprocess.dlq` after retry exhaustion.
- Preserve `jobId`, `itemId`, `imageId`, `attempt`, `traceId`, and failure reason.
- Allow backend manual retry to publish a new message to `image.preprocess.retry`.
