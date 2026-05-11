# Issue 75. Worker Quality 1 Denoise Contrast Binarization

## Purpose

Implement the first Worker quality-processing increment by replacing `DenoiseStep`, `ContrastNormalizeStep`, and
`BinarizationStep` skeleton behavior with OpenCV-backed document preprocessing.

This remains OCR preprocessing only. It does not add OCR text extraction and does not move image processing into
`backend-api`.

## Scope

1. `DenoiseStep`
   - Reads the context-owned decoded `ImageMatHolder`.
   - Supports `median` denoise by default.
   - Supports `bilateral` denoise when explicitly configured.
   - Supports `none`, `off`, and `false` as no-op modes.
   - Falls back to `median` for unsupported modes.

2. `ContrastNormalizeStep`
   - Applies CLAHE contrast normalization.
   - Keeps grayscale input grayscale.
   - Converts BGR input to Lab, applies CLAHE to luminance, then converts back to BGR.
   - Uses preset-configurable `contrastClipLimit` and `contrastTileGridSize`.

3. `BinarizationStep`
   - Converts current image to grayscale.
   - Supports `otsu` thresholding.
   - Supports `adaptive` thresholding.
   - Falls back to `otsu` for unsupported modes.
   - Produces a single-channel binary image for downstream morphology cleanup.

4. Tests
   - Denoise apply/no-op/fallback/deferred paths.
   - Contrast BGR/GRAY/deferred paths.
   - Binarization Otsu/adaptive/fallback/deferred paths.

## Parameter Contract

`DenoiseStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `denoiseMode` | `median` | `median`, `bilateral`, `none`, `off`, or `false` |
| `denoiseKernelSize` | `3` | Odd kernel size for median blur |
| `denoiseDiameter` | `5` | Odd diameter for bilateral filtering |

`ContrastNormalizeStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `contrastClipLimit` | `1.2` | CLAHE clip limit |
| `contrastTileGridSize` | `8` | CLAHE tile grid width/height |

`BinarizationStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `binarizationMode` | `otsu` | `otsu` or `adaptive` |
| `adaptiveBlockSize` | `31` | Odd block size for adaptive threshold |
| `adaptiveC` | `7.0` | Constant subtracted in adaptive threshold |

## Out Of Scope

1. Morphology cleanup.
2. Optional sharpen.
3. Processed image upload.
4. Preview generation.
5. Processing report JSON upload.
6. Worker success callback after artifact persistence.
7. OCR text extraction.

## Verification

The PR must run:

```bash
cd preprocess-worker
../gradlew test
```

If the repository still has no Gradle wrapper at the root, use the existing Docker-based Gradle verification path.
