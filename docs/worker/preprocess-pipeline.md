# Worker Preprocess Pipeline

## Purpose

The Worker preprocessing pipeline models the OCR-oriented document image preprocessing mechanism that will be backed by
OpenCV and the `image-test` repository logic. It is not a thumbnail or resize-only pipeline.

This platform does not provide an OCR text extraction service. The Worker prepares document images before OCR by
normalizing scan quality, geometry, contrast, binarization, morphology, and DPI.

## Current Implementation

Issue 43 added the initial pipeline skeleton:

- `PreprocessContext`
- `PreprocessPipeline`
- `PreprocessPipelineRunner`
- `PreprocessResult`
- `PreprocessStep`
- `PreprocessStepCatalog`
- built-in preset registry

Each step currently records a skeleton execution note. Actual image mutation is deferred.

Issue 59 adds runtime metadata hooks:

- per-step `startedAt`
- per-step `endedAt`
- per-step wall time
- per-step success/failure flag
- failed-step error message
- total pipeline wall time
- fallback note collection

These hooks are required before implementing the OpenCV-backed steps because reports and debug artifacts need stable
metadata regardless of which step succeeds or fails.

## Required Execution Order

Every built-in document preset executes the following order:

1. `DECODE`
2. `COLOR_NORMALIZE`
3. `ORIENTATION_NORMALIZE`
4. `DESKEW`
5. `CROP`
6. `DENOISE`
7. `CONTRAST_NORMALIZE`
8. `BINARIZATION`
9. `MORPHOLOGY_CLEANUP`
10. `DPI_NORMALIZE`
11. `OPTIONAL_SHARPEN`

`DPI_NORMALIZE` is reserved for OCR quality normalization. It must not be reduced to a thumbnail resize shortcut.

## Preset Registry

Supported preset names:

- `A4_SCAN_300DPI`
- `LOW_CONTRAST_SCAN`
- `RECEIPT`
- `NOISY_SCAN`
- `AUTO`

Preset names must stay aligned with the backend `/api/v1/preprocess/presets` contract.

## Worker Runtime Boundary

The current Worker flow executes the skeleton and then reports:

```text
PIPELINE_NOT_IMPLEMENTED
```

This is intentional. A successful Worker result requires all of the following future integrations:

- Actual OpenCV processing.
- Object Storage download.
- Processed image upload.
- Preview upload.
- Processing report upload.
- Optional debug artifact upload.
- Backend success callback.

If a skeleton or future OpenCV step throws an exception, the runner returns a failed `PreprocessResult`. The Worker maps
that to `PIPELINE_EXECUTION_FAILED` and reports it to the backend Internal Worker API.

## Next Implementation Steps

1. Add debug artifact hook contract.
2. Add image codec adapter and OpenCV loader.
3. Replace `DecodeStep` skeleton with actual image decode.
4. Add `ImageMatHolder` and resource cleanup.
5. Implement steps one by one with unit tests.
6. Add report generation per step.
7. Add artifact save service.
8. Keep OCR text extraction out of the Worker runtime scope.
