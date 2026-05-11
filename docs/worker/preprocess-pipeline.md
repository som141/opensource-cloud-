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

Issue 61 adds the debug artifact metadata hook:

- `DebugArtifactDescriptor`
- `PreprocessContext.recordDebugArtifact`
- `PreprocessResult.debugArtifacts`
- `ProcessingReport.debugArtifacts`

The hook is metadata-only. Actual debug image generation and upload are deferred to later OpenCV and artifact tasks.

Issue 63 adds the OpenCV loader and image codec boundary:

- `OpenCvLoader`
- `ImageCodecAdapter.decode`
- `ImageMatHolder`
- `MatResourceCleaner`

The pipeline steps still remain skeletons. The next implementation should replace `DecodeStep` with Object Storage
download plus codec decode.

Issue 65 connects `DecodeStep` to the codec boundary:

- `ImageDecodePort`
- `PreprocessContext.withSourceImageBytes`
- `PreprocessContext.storeDecodedImage`
- `PreprocessContext.releaseDecodedImage`
- `DecodeStep` real decode path when bytes are attached

The actual Object Storage download remains a later task. Until source bytes are attached, `DecodeStep` records a
deferred note and lets the skeleton pipeline continue.

Issue 67 connects Object Storage download to the context:

- `ObjectStoragePort.downloadBytes`
- `MinioObjectStorageClient.downloadBytes`
- `WorkerJobService` attaches downloaded bytes through `PreprocessContext.withSourceImageBytes`

The Worker can now decode real downloaded source bytes. Later steps after decode still remain skeleton implementations.

Issue 69 implements `ColorNormalizeStep`:

- `GRAY -> BGR`
- `BGRA -> BGR`
- `BGR` no-op

The step replaces the context-owned Mat only when conversion is needed. The remaining downstream steps still remain
skeleton implementations.

Issue 71 implements Geometry 1:

- `OrientationNormalizeStep`
- `DeskewStep`

Orientation normalization rotates landscape input to portrait orientation. Deskew estimates a correction angle from
foreground pixels and applies `warpAffine` when the angle is within the configured safety bound.

Issue 73 implements Geometry 2:

- `CropStep`
- `DpiNormalizeStep`

Crop detects foreground bounds from an inverse Otsu mask and replaces the context-owned Mat only when a valid bounded
crop is available. DPI normalization uses source DPI metadata and `targetDpi` to resize for OCR preprocessing; if source
DPI metadata is missing, the step records a fallback and leaves the image unchanged.

Issue 75 implements Quality 1:

- `DenoiseStep`
- `ContrastNormalizeStep`
- `BinarizationStep`

Denoise supports median and bilateral filtering. Contrast normalization applies CLAHE to grayscale input or to the
luminance channel of BGR input. Binarization converts the current image to a single-channel binary image with Otsu or
adaptive thresholding according to preset parameters.

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

- Actual OpenCV processing after decode.
- Processed image upload.
- Preview upload.
- Processing report upload.
- Optional debug artifact upload.
- Backend success callback.

If a skeleton or future OpenCV step throws an exception, the runner returns a failed `PreprocessResult`. The Worker maps
that to `PIPELINE_EXECUTION_FAILED` and reports it to the backend Internal Worker API.

## Next Implementation Steps

1. Implement Quality 2: morphology cleanup and optional sharpen.
2. Add report generation per step.
3. Add artifact save service.
4. Keep OCR text extraction out of the Worker runtime scope.
