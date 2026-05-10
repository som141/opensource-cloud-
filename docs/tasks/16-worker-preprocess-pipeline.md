# 16. Worker Preprocess Pipeline

## Goal

Implement the Worker preprocessing pipeline toward the OpenCV document-image preprocessing mechanism from the
`image-test` repository.

This task is not a resize service and does not add OCR text extraction. The Worker prepares scanned document images
before OCR through decode, color normalization, orientation normalization, deskew, crop, denoise, contrast
normalization, binarization, morphology cleanup, DPI normalization, and optional sharpening.

## Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/15-worker-message-consume.md`
4. `docs/worker/preprocess-pipeline.md`
5. `docs/worker/image-test-integration.md`

## Scope

1. `PreprocessStep` interface
2. Pipeline runner
3. Context and result
4. Required document preprocessing steps
5. Timing, fallback, debug, and failure-report hooks
6. Step-level tests

## Incremental Order

1. Add pipeline timing and failure-report hook.
2. Add fallback note collection.
3. Add debug artifact hook contract.
4. Add image codec adapter and OpenCV loader.
5. Replace `DecodeStep` skeleton with actual image decode.
6. Add `ImageMatHolder` and resource cleanup rules.
7. Implement document steps one by one:
   - `DecodeStep`
   - `ColorNormalizeStep`
   - `OrientationNormalizeStep`
   - `DeskewStep`
   - `CropStep`
   - `DenoiseStep`
   - `ContrastNormalizeStep`
   - `BinarizationStep`
   - `MorphologyCleanupStep`
   - `DpiNormalizeStep`
   - `SharpenStep`
8. Add report generation per step.
9. Add artifact save integration.
10. Add Worker success callback after processed output exists.

## Current Issue 59 Scope

Issue 59 implements only the timing and failure-report hook:

1. `PreprocessStepExecution` records start time, end time, wall time, success flag, and error message.
2. `PreprocessContext` collects fallback notes.
3. `PreprocessPipelineRunner` records each step execution and stops on first failed step.
4. `PreprocessResult` exposes total wall time, success flag, error message, and fallback notes.
5. `ProcessingReportFactory` maps pipeline timing/fallback data into report DTOs.
6. `WorkerJobService` maps failed pipeline results to `PIPELINE_EXECUTION_FAILED`.

## Current Issue 61 Scope

Issue 61 implements only the debug artifact hook contract:

1. `DebugArtifactDescriptor` records step name, file name, object key, and content type.
2. `PreprocessContext` exposes `recordDebugArtifact`.
3. `debug=false` ignores debug artifact records.
4. `debug=true` records deterministic object keys under `processed/{projectId}/{jobId}/{itemId}/debug/`.
5. `PreprocessResult` exposes debug artifact descriptors.
6. `ProcessingReportFactory` carries debug artifact metadata into report DTOs.
7. Actual debug image generation and upload remain out of scope.

## Current Issue 63 Scope

Issue 63 implements only the OpenCV loader and codec boundary:

1. Worker-only OpenCV dependency.
2. Idempotent native OpenCV loading through `OpenCvLoader`.
3. Image byte decode through `ImageCodecAdapter`.
4. `ImageMatHolder` with OpenCV `Mat`, dimensions, color space, and release state.
5. `MatResourceCleaner` release hook.
6. Decode failure handling for empty or unsupported image bytes.
7. Actual `DecodeStep` replacement and downstream image processing remain out of scope.

## Current Issue 65 Scope

Issue 65 connects `DecodeStep` to the codec boundary:

1. `ImageDecodePort` is the domain port used by `DecodeStep`.
2. `ImageCodecAdapter` implements the port.
3. `PreprocessContext` can receive source image bytes.
4. `PreprocessContext` stores the decoded `ImageMatHolder`.
5. `PreprocessPipelineRunner` releases decoded Mat resources after pipeline completion.
6. `DecodeStep` records decoded width, height, and color space when bytes are attached.
7. Missing source bytes are deferred until the Object Storage download task.
8. Invalid attached bytes fail the decode step.

## Current Issue 67 Scope

Issue 67 connects Object Storage download bytes to the pipeline:

1. `ObjectStoragePort.downloadBytes` returns original object bytes.
2. `MinioObjectStorageClient` downloads bytes from the configured bucket.
3. `WorkerJobService` passes downloaded bytes to `PreprocessContext.withSourceImageBytes`.
4. Storage download failures still report `STORAGE_DOWNLOAD_FAILED`.
5. Decode or later pipeline failures still report `PIPELINE_EXECUTION_FAILED`.
6. Artifact upload and success callback remain out of scope.

## Done Criteria

1. A simple resize-only step does not exist.
2. `DPI_NORMALIZE` remains an OCR-quality normalization step, not a thumbnail resize shortcut.
3. Each step execution is recorded in context/result.
4. Failed steps are recorded in the result and report path.
5. API server does not execute the pipeline.
6. Tests pass.

## Forbidden

1. Do not execute the pipeline in `backend-api`.
2. Do not add OCR text extraction.
3. Do not collapse all presets into the same unparameterized behavior.
4. Do not bypass timing/failure reporting for future OpenCV steps.
