# Issue 61. Worker Debug Artifact Hook Contract

## Feature Summary

This task adds a debug artifact hook contract to the Worker preprocessing pipeline. It does not generate or upload image
files yet. It only records metadata that later OpenCV steps and artifact upload services can use.

## Implemented Units

1. `DebugArtifactDescriptor`
2. `PreprocessContext.recordDebugArtifact`
3. `PreprocessResult.debugArtifacts`
4. `ProcessingReport.debugArtifacts`
5. `ProcessingReportFactory` propagation from result to report
6. Unit tests for debug enabled/disabled behavior and report propagation

## Debug Artifact Contract

When `debug=false`, calls to `recordDebugArtifact` are ignored.

When `debug=true`, the pipeline records metadata:

```json
{
  "stepName": "DESKEW",
  "fileName": "02_deskew.png",
  "objectKey": "processed/{projectId}/{jobId}/{itemId}/debug/02_deskew.png",
  "contentType": "image/png"
}
```

The object key follows the existing storage convention:

```text
processed/{projectId}/{jobId}/{itemId}/debug/{stepFileName}
```

## Runtime Boundary

This hook is metadata-only. The Worker still does not create actual debug images in this issue. Future OpenCV step
implementations will call `recordDebugArtifact` after generating debug images, and artifact services will upload the
actual files.

## Out Of Scope

1. OpenCV image mutation
2. Debug image file creation
3. Object Storage upload
4. Backend artifact row registration
5. OCR text extraction
