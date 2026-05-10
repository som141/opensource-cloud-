# Worker Preprocess Pipeline

## Purpose

The Worker preprocessing pipeline models the OCR-oriented document image preprocessing mechanism that will be backed by
OpenCV and the `image-test` repository logic. It is not a thumbnail or resize-only pipeline.

## Current Implementation

Issue 43 adds the pipeline skeleton:

- `PreprocessContext`
- `PreprocessPipeline`
- `PreprocessPipelineRunner`
- `PreprocessResult`
- `PreprocessStep`
- `PreprocessStepCatalog`
- built-in preset registry

Each step currently records a skeleton execution note. Actual image mutation is deferred.

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

## Next Implementation Steps

1. Add image codec adapter and OpenCV loader.
2. Replace `DecodeStep` skeleton with actual image decode.
3. Add `ImageMatHolder` and resource cleanup.
4. Implement steps one by one with unit tests.
5. Add report generation per step.
6. Add artifact save service.
