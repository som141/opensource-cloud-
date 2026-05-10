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

## Worker Behavior Implemented In Issue 57

1. Consume a message from the selected queue.
2. Report item started through the internal Worker API.
3. Prepare original image download through the Object Storage port.
4. Report heartbeat through the internal Worker API.
5. Execute the preprocessing pipeline skeleton.
6. Report non-retryable `PIPELINE_NOT_IMPLEMENTED` until the actual pipeline is implemented.
7. Let the listener requeue retryable failures and reject non-retryable failures.

## DLQ Rules

The exact automatic retry count is still deferred to queue-level RabbitMQ configuration. The current listener policy is:

- Throw `ImmediateRequeueAmqpException` for retryable failures.
- Throw `AmqpRejectAndDontRequeueException` for non-retryable failures.
- Send messages to `image.preprocess.dlq` when RabbitMQ DLQ bindings are configured.
- Preserve `jobId`, `itemId`, `imageId`, `attempt`, `traceId`, and failure reason.
- Allow backend manual retry to publish a new message to `image.preprocess.retry`.
