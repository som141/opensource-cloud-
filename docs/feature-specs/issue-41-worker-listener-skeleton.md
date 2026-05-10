# Issue 41. Preprocess Worker Listener Skeleton

## Goal

Add the first executable `preprocess-worker` Spring Boot application skeleton. This issue introduces RabbitMQ listener
boundaries, message DTOs, backend callback ports, and object storage ports without implementing OpenCV preprocessing.

## Overall Order

1. Add Worker Gradle and Spring Boot build files.
2. Add safe local placeholder values in `application.yml`.
3. Add Worker application entrypoint.
4. Add RabbitMQ queue properties.
5. Add `PreprocessJobMessage` DTO matching the backend Job publisher contract.
6. Add listener skeleton for high, normal, and retry preprocessing queues.
7. Add `WorkerJobService` orchestration skeleton.
8. Add backend internal API client port and no-op report client.
9. Add object storage port and no-op MinIO boundary implementation.
10. Add unit tests for message validation, listener delegation, service orchestration, and queue defaults.
11. Document Worker listener behavior and next work.

## Functional Units

### Listener

- Listens to `image.preprocess.high`, `image.preprocess.normal`, and `image.preprocess.retry`.
- Delegates message handling to `WorkerJobService`.
- Is disabled by default through `WORKER_LISTENER_ENABLED=false` until the preprocessing pipeline exists.

### Message Validation

- Requires `messageId`, `jobId`, `itemId`, `projectId`, and `imageId`.
- Requires `originalObjectKey` and `preset`.
- Requires `attempt >= 1`.

### Worker Orchestration

- Reports started through the backend client boundary.
- Prepares object storage download through the storage port boundary.
- Reports `PIPELINE_NOT_IMPLEMENTED` until the OpenCV pipeline step task is implemented.

## Safe Local Defaults

These values are placeholders only and can be overridden by environment variables:

- `SPRING_RABBITMQ_HOST=localhost`
- `SPRING_RABBITMQ_PORT=5672`
- `SPRING_RABBITMQ_USERNAME=guest`
- `SPRING_RABBITMQ_PASSWORD=guest`
- `BACKEND_API_INTERNAL_URL=http://localhost:8080`
- `WORKER_INTERNAL_TOKEN=local-worker-token`
- `MINIO_ENDPOINT=http://localhost:9000`
- `MINIO_ACCESS_KEY=minioadmin`
- `MINIO_SECRET_KEY=minioadmin`
- `MINIO_BUCKET=image-preprocess-local`

## Out Of Scope

- OpenCV preprocessing pipeline implementation.
- image-test repository integration.
- Real HTTP calls to backend internal APIs.
- Real MinIO SDK download/upload.
- Worker heartbeat.
- Benchmark listener.

## Verification

- Worker unit tests cover message validation, listener delegation, worker orchestration, and queue property defaults.
- Worker build verifies the Spring Boot application is packageable.
