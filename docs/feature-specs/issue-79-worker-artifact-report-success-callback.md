# Issue 79. Worker Artifact Report Success Callback

## Purpose

Persist Worker preprocessing outputs after the OpenCV pipeline finishes, then report success to the Backend Internal
Worker API with artifact object keys.

This task is still OCR preprocessing only. It does not add OCR text extraction and does not move image processing into
`backend-api`.

## Scope

1. Pipeline output handoff
   - `PreprocessPipelineRunner` keeps the final `ImageMatHolder` alive while an output callback runs.
   - The runner still releases the context-owned Mat in `finally`.
   - `PreprocessResult.skeletonOnly` is now based on whether a real output image exists.

2. Artifact storage
   - `ObjectStoragePort.uploadBytes` is added.
   - `MinioObjectStorageClient` uploads generated artifact bytes.
   - `ProcessedImageSaveService` writes `processed.png`.
   - `PreviewImageSaveService` writes `preview.png`.
   - `ProcessingReportSaveService` writes `processing-report.json`.

3. Report JSON
   - `ProcessingReportFactory.createReport` maps pipeline result metadata.
   - `ProcessingReportWriter.writeJsonBytes` serializes the report envelope as UTF-8 JSON.

4. Worker success callback
   - `WorkerJobService` uploads processed, preview, and report artifacts.
   - On successful upload, Worker calls `/internal/v1/jobs/{jobId}/items/{itemId}/succeeded`.
   - The success payload includes `processedObjectKey`, `previewObjectKey`, and `reportObjectKey`.

5. Failure behavior
   - Storage download failure still reports `STORAGE_DOWNLOAD_FAILED`.
   - Pipeline failure still reports `PIPELINE_EXECUTION_FAILED`.
   - Artifact upload failure reports `ARTIFACT_UPLOAD_FAILED`.
   - Backend reporting failure remains `BACKEND_REPORT_FAILED`.

## Object Storage Paths

```text
processed/{projectId}/{jobId}/{itemId}/processed.png
processed/{projectId}/{jobId}/{itemId}/preview.png
processed/{projectId}/{jobId}/{itemId}/processing-report.json
```

## Out Of Scope

1. Real debug artifact image upload.
2. Per-step debug image generation.
3. OCR text extraction.
4. API server OpenCV processing.

## Verification

The PR must run:

```bash
docker run --rm -v "${PWD}\preprocess-worker:/workspace" -w /workspace gradle:8.10-jdk21 gradle test --no-daemon
docker compose -f infra/docker-compose/docker-compose.local.yml build preprocess-worker
```
