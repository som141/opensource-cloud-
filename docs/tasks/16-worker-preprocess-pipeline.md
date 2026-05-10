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
