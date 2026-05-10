# Issue 69. Worker ColorNormalizeStep

## Feature Summary

This task replaces the Worker `ColorNormalizeStep` skeleton with OpenCV-backed color normalization.

The step normalizes decoded images into BGR so downstream document preprocessing steps can work with a predictable color
layout.

## Implemented Units

1. `GRAY -> BGR` conversion
2. `BGRA -> BGR` conversion
3. `BGR` no-op path
4. Context holder replacement after conversion
5. Previous Mat release when holder is replaced
6. Step notes for before/after color space
7. Unit tests for GRAY, BGRA, BGR, missing holder, and unsupported channel layout

## Runtime Behavior

After `DecodeStep` stores an `ImageMatHolder`, `ColorNormalizeStep` checks the holder color space:

```text
GRAY -> BGR
BGRA -> BGR
BGR  -> BGR no-op
```

When conversion creates a new Mat, the context replaces the previous holder and releases the old Mat. If decoded image
data is not available yet, the step records a deferred note and keeps the current skeleton-compatible pipeline behavior.

Unsupported channel layouts fail the step and are reported by the Worker as `PIPELINE_EXECUTION_FAILED`.

## Out Of Scope

1. Orientation normalization
2. Deskew
3. Crop
4. Denoise
5. Contrast normalization
6. Binarization
7. Morphology cleanup
8. DPI normalization
9. Artifact upload
10. OCR text extraction
