# Issue 39. Job Domain And RabbitMQ Publishing

## Goal

Implement the backend Job domain that registers document image preprocessing work and publishes one RabbitMQ message
per image. The API server only manages metadata, permissions, validation, and queue publishing. It does not run OpenCV
preprocessing.

## Overall Order

1. Define Job and JobItem state enums.
2. Add Job and JobItem entities.
3. Add repository interfaces for Job and JobItem lookup.
4. Add Job creation request/response DTOs.
5. Validate project edit permission before creating a Job.
6. Validate image ownership and exclude deleted images.
7. Validate preprocess preset parameters through the Preprocess Preset domain.
8. Persist Job and one JobItem per selected image.
9. Publish one RabbitMQ message per JobItem.
10. Add Job list/detail/item/summary APIs.
11. Add cancel, failed-item retry, and full rerun APIs.
12. Document API and Worker retry contract.
13. Add entity, service, controller, and publisher tests.

## Functional Units

### Job Creation

- Requires project edit permission.
- Requires non-empty, unique image IDs.
- Rejects images outside the project or deleted images.
- Validates preset name and parameters before persisting queue work.
- Creates a `Job` row and one `JobItem` row per image.
- Publishes messages to `image.preprocess.high` for `HIGH` priority, otherwise `image.preprocess.normal`.

### RabbitMQ Message

Each message contains:

- `messageId`
- `jobId`
- `itemId`
- `projectId`
- `imageId`
- `userId`
- `originalObjectKey`
- `preset`
- `presetParameters`
- `debug`
- `priority`
- `attempt`
- `traceId`
- `createdAt`

### Query APIs

- Users can list their own jobs.
- Job detail, items, summary, and artifact placeholders validate project read permission.
- Artifact and ZIP download APIs are placeholders until Worker artifact integration is implemented.

### Cancel And Retry

- Cancel requests mark the Job as `CANCEL_REQUESTED`.
- Queued, pending, and retrying items are marked `CANCELLED`.
- Processing items are not force-killed by the API server.
- Retry publishes only failed and dead-lettered items.
- Rerun republishes every non-processing item.

## API Surface

- `POST /api/v1/jobs`
- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{jobId}`
- `GET /api/v1/jobs/{jobId}/items`
- `GET /api/v1/jobs/{jobId}/summary`
- `POST /api/v1/jobs/{jobId}/cancel`
- `POST /api/v1/jobs/{jobId}/retry`
- `POST /api/v1/jobs/{jobId}/rerun`
- `GET /api/v1/jobs/{jobId}/artifacts`
- `GET /api/v1/jobs/{jobId}/download.zip`

## Out Of Scope

- SSE progress streaming.
- Worker listener implementation.
- Worker internal callback API.
- Worker artifact registration.
- Result ZIP generation.
- API-side OpenCV preprocessing.

## Verification

- Entity tests cover Job and JobItem state transitions.
- Service tests cover create, read permission validation, cancel, retry, and message factory behavior.
- Controller tests cover common response wrapping.
- Publisher tests cover normal/high priority queue routing and retry queue routing.
