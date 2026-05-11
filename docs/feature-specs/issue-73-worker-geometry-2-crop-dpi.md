# Issue 73. Worker Geometry 2 Crop And DPI Normalize

## Purpose

Implement the second Worker geometry increment by replacing `CropStep` and `DpiNormalizeStep` skeleton behavior with
real OpenCV-backed processing.

This task is still OCR preprocessing only. It does not add OCR text extraction and does not move image processing into
`backend-api`.

## Scope

1. `CropStep`
   - Read the context-owned decoded `ImageMatHolder`.
   - Convert the current image to grayscale for detection.
   - Build an inverse Otsu foreground mask.
   - Find foreground bounds with `Core.findNonZero` and `Imgproc.boundingRect`.
   - Expand bounds by `cropMarginPixels` and clamp to image size.
   - Replace the context-owned Mat only when a valid crop is applied.
   - Record fallback notes when foreground points or bounds are not usable.

2. `DpiNormalizeStep`
   - Read source DPI from Worker context parameters.
   - Use `targetDpi` with a default of `300`.
   - Resize by `targetDpi / sourceDpi` with safe min/max scale bounds.
   - Replace the context-owned Mat only when scaling is needed.
   - Record fallback notes when source DPI metadata is missing.

3. Tests
   - Crop applied path.
   - Crop blank-image fallback path.
   - DPI scale path.
   - DPI no-op path.
   - DPI missing-metadata fallback path.
   - Missing decoded image deferred path.

## Parameter Contract

`CropStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `cropMarginPixels` | `4` | Extra pixels around detected foreground bounds |

`DpiNormalizeStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `targetDpi` | `300` | Desired OCR preprocessing DPI |
| `sourceDpi` | none | Same source DPI for X/Y axes |
| `sourceDpiX` / `sourceDpiY` | none | Axis-specific source DPI |
| `dpi`, `dpiX`, `dpiY`, `xDpi`, `yDpi` | none | Compatibility aliases |

If source DPI is unavailable, `DpiNormalizeStep` records a fallback and keeps the image unchanged. This avoids inventing
metadata and keeps upload/image metadata extraction as the correct long-term source of truth.

## Out Of Scope

1. Denoise, contrast normalization, binarization, morphology cleanup, and sharpen implementation.
2. Processed image upload.
3. Preview generation.
4. Processing report JSON upload.
5. Worker success callback after artifact persistence.
6. OCR text extraction.

## Verification

The PR must run:

```bash
cd preprocess-worker
../gradlew test
```

The PR should also build the Worker Docker image when Docker is available.
