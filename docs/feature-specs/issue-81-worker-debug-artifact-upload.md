# Issue 81. Worker Debug Artifact Upload

## Goal

Store real per-step Worker debug artifact images when a preprocess job is created with `debug=true`.

This remains OCR preprocessing only. The Worker does not run OCR text extraction and does not persist recognized text.

## Scope

1. Capture a cloned OpenCV `Mat` snapshot after each successful pipeline step when debug mode is enabled.
2. Keep debug mode disabled by default so normal jobs do not pay the storage and memory cost.
3. Upload captured snapshots as PNG files to Object Storage.
4. Keep debug artifact descriptors in `processing-report.json`.
5. Release cloned debug snapshot Mats after the pipeline completes.

## Runtime Flow

1. `WorkerJobService` downloads the source image from Object Storage.
2. `PreprocessPipelineRunner` executes the configured preset steps in OCR preprocessing order.
3. After a step succeeds, the runner asks `PreprocessContext` to capture a debug snapshot.
4. `PreprocessContext` clones the current context-owned Mat and records a matching `DebugArtifactDescriptor`.
5. Before runner cleanup, `WorkerJobService` saves processed, preview, and debug artifacts from the output callback.
6. The runner releases the decoded output Mat and all cloned debug snapshot Mats in `finally`.
7. `ProcessingReportFactory` writes the debug artifact metadata into `processing-report.json`.

## Debug Output Paths

```text
processed/{projectId}/{jobId}/{itemId}/debug/00_decoded.png
processed/{projectId}/{jobId}/{itemId}/debug/01_normalized.png
processed/{projectId}/{jobId}/{itemId}/debug/02_orientation.png
processed/{projectId}/{jobId}/{itemId}/debug/03_deskew.png
processed/{projectId}/{jobId}/{itemId}/debug/04_crop.png
processed/{projectId}/{jobId}/{itemId}/debug/05_denoise.png
processed/{projectId}/{jobId}/{itemId}/debug/06_contrast.png
processed/{projectId}/{jobId}/{itemId}/debug/07_binarized.png
processed/{projectId}/{jobId}/{itemId}/debug/08_morphology.png
processed/{projectId}/{jobId}/{itemId}/debug/09_dpi.png
processed/{projectId}/{jobId}/{itemId}/debug/10_sharpen.png
```

## Failure Policy

If debug artifact encoding or upload fails, the Worker reports `ARTIFACT_UPLOAD_FAILED`.

This is intentional because `debug=true` means the caller explicitly requested these artifacts as part of the job
result. The job can be retried through the existing Worker retry path.

## Done Criteria

1. `debug=false` jobs upload only processed, preview, and report artifacts.
2. `debug=true` jobs upload per-step debug PNG snapshots.
3. Snapshot Mats are released after runner completion.
4. Report JSON keeps debug artifact metadata.
5. Worker tests verify upload and release behavior.
