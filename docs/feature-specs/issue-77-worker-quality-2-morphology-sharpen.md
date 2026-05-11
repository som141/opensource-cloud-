# Issue 77. Worker Quality 2 Morphology Sharpen

## Purpose

Implement the second Worker quality-processing increment by replacing `MorphologyCleanupStep` and `SharpenStep`
skeleton behavior with OpenCV-backed document preprocessing.

This remains OCR preprocessing only. It does not add OCR text extraction and does not move image processing into
`backend-api`.

## Scope

1. `MorphologyCleanupStep`
   - Reads the context-owned decoded `ImageMatHolder`.
   - Treats black document strokes as foreground by inverting the binary image internally.
   - Supports `open`, `close`, and `open_close` cleanup.
   - Supports `none`, `off`, and `false` as no-op modes.
   - Falls back to `open_close` for unsupported modes.
   - Re-inverts the cleaned image to the pipeline's normal black-text-on-white representation.

2. `SharpenStep`
   - Runs only when `sharpen=true`, `sharpen=on`, or `sharpen=yes`.
   - Uses unsharp mask with configurable amount and sigma.
   - Keeps the current channel layout when sharpening.
   - Skips by default when the preset does not explicitly enable sharpening.

3. Tests
   - Morphology apply/no-op/fallback/deferred paths.
   - Sharpen enabled/disabled/missing/deferred paths.

## Parameter Contract

`MorphologyCleanupStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `morphologyMode` | `open_close` | `open`, `close`, `open_close`, `none`, `off`, or `false` |
| `morphologyKernelSize` | `2` | Rectangular morphology kernel width/height |

`SharpenStep` supports:

| Parameter | Default | Meaning |
|---|---:|---|
| `sharpen` | `false` | Enables optional sharpening when `true`, `on`, or `yes` |
| `sharpenAmount` | `0.6` | Unsharp mask weight |
| `sharpenSigma` | `1.0` | Gaussian blur sigma for unsharp mask |

## Out Of Scope

1. Processed image upload.
2. Preview generation.
3. Processing report JSON upload.
4. Debug artifact image upload.
5. Worker success callback after artifact persistence.
6. OCR text extraction.

## Verification

The PR must run:

```bash
docker run --rm -v "${PWD}\preprocess-worker:/workspace" -w /workspace gradle:8.10-jdk21 gradle test --no-daemon
docker compose -f infra/docker-compose/docker-compose.local.yml build preprocess-worker
```
