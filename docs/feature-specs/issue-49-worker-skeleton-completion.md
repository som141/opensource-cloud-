# Issue 49 Worker Skeleton Completion

## Goal

Complete the missing preprocess-worker skeleton packages required by `docs/tasks/03-worker-skeleton.md`.
This PR is structural only. It does not implement actual OpenCV preprocessing and does not add OCR text extraction.

## Work Units

1. Add preprocessing model skeletons.
2. Add artifact type, path, upload request/result, and save service skeletons.
3. Add processing report DTO, model, factory, and writer skeletons.
4. Add OpenCV infra skeletons for loader, codec adapter, and Mat cleanup.
5. Add tracing and metrics integration seams.
6. Add focused tests for artifact paths, report creation, codec placeholder, and trace extraction.
7. Document that OCR runtime is out of scope.

## Added Boundaries

### `domain/preprocess/model`

- `ImageMatHolder`
- `CropBounds`
- `DeskewResult`
- `DpiInfo`
- `FallbackNote`
- `ProcessingParameter`

### `domain/artifact`

- `ArtifactType`
- `ArtifactPath`
- `ArtifactUploadRequest`
- `ArtifactUploadResult`
- `ArtifactSaveService`
- `ProcessedImageSaveService`
- `PreviewImageSaveService`
- `DebugArtifactSaveService`

### `domain/report`

- `ProcessingReport`
- `ProcessingStepReport`
- `ProcessingReportJson`
- `ProcessingTiming`
- `ProcessingMemoryUsage`
- `ProcessingFallbackSummary`
- `ProcessingReportFactory`
- `ProcessingReportWriter`

### `infra`

- `opencv/OpenCvLoader`
- `opencv/ImageCodecAdapter`
- `opencv/MatResourceCleaner`
- `tracing/WorkerTraceContext`
- `tracing/RabbitTraceContextExtractor`
- `metrics/WorkerMetricsRecorder`

## Explicit Non-Goals

- No Tesseract integration.
- No OCR API.
- No OCR result storage.
- No OCR benchmark implementation.
- No real OpenCV native library loading.
- No actual Object Storage upload.

## Follow-Up Tasks

1. Implement `13-sse-progress`.
2. Implement `14-internal-worker-api`.
3. Complete `15-worker-message-consume` with ack, retry, DLQ, and Backend reporting.
4. Implement `18-artifact-report` using the skeleton introduced here.
5. Implement real image-test/OpenCV preprocessing inside `16-worker-preprocess-pipeline`.
