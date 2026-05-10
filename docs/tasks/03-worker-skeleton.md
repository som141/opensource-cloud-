# 03. preprocess-worker Spring skeleton

## Goal

Create the Spring Boot Worker skeleton that consumes RabbitMQ messages and executes an OpenCV-based document image
preprocessing pipeline.

The Worker is for OCR preprocessing only. It does not run OCR engines and does not return or store recognized text.

## Documents To Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/02-backend-api-skeleton.md`
4. `docs/worker/preprocess-pipeline.md`
5. `docs/worker/image-test-integration.md`

## Scope

1. Worker Gradle project
2. Worker application entrypoint
3. Message consume domain
4. Preprocess pipeline domain
5. Preprocess model domain
6. Artifact/report domain
7. Storage/API/OpenCV/tracing/metrics infra boundaries

## Work Order

1. Create `preprocess-worker/build.gradle`.
2. Create `PreprocessWorkerApplication.java`.
3. Create `domain/workerjob`.
4. Create `domain/preprocess/pipeline`.
5. Create `domain/preprocess/preset`.
6. Create `domain/preprocess/step`.
7. Create `domain/preprocess/model`.
8. Create `domain/artifact`.
9. Create `domain/report`.
10. Create `infra/rabbitmq`.
11. Create `infra/storage`.
12. Create `infra/api`.
13. Create `infra/opencv`.
14. Create `infra/tracing`.
15. Create `infra/metrics`.

## Deliverables

1. Worker Spring Boot skeleton
2. `PreprocessStep` based package structure
3. Worker Dockerfile placeholder
4. Worker application config file
5. Artifact/report skeleton boundaries
6. OpenCV integration seam without native runtime dependency

## Completion Criteria

1. Worker has no OAuth login logic.
2. Worker does not connect directly to the API database.
3. Worker has the preprocessing structure from `DecodeStep` to `SharpenStep`.
4. Worker has model/report/artifact skeletons for future processing results.
5. Worker has an explicit image-test integration seam.
6. Worker has no OCR text extraction runtime.

## Forbidden

1. Do not reduce the Worker to a simple resize job.
2. Do not add public external controllers to the Worker.
3. Do not put user authorization decisions in the Worker.
4. Do not add Tesseract or OCR text extraction logic in this skeleton task.
