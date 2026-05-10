# Issue 71. Worker Geometry 1 Orientation And Deskew

## Feature Summary

This task implements the first geometry stage of the Worker preprocessing pipeline.

`OrientationNormalizeStep` and `DeskewStep` are now backed by OpenCV operations. The goal is to make downstream crop,
quality, and binarization steps operate on a more predictable document geometry.

## Implemented Units

1. `OrientationNormalizeStep`
2. Landscape-to-portrait 90-degree rotation
3. Orientation skip/deferred step notes
4. `DeskewStep`
5. Foreground extraction with grayscale + Otsu inverse threshold
6. `minAreaRect` skew correction angle estimation
7. Max deskew angle guard
8. Low foreground fallback
9. Context holder replacement and previous Mat release on applied transforms
10. Unit tests for orientation rotate/no-op/deferred
11. Unit tests for deskew applied/blank fallback/deferred

## Orientation Behavior

If the decoded image is landscape, the step rotates it 90 degrees clockwise:

```text
width > height -> rotate 90 degrees clockwise
width <= height -> no-op
```

This is intentionally simple. EXIF orientation and content-aware orientation detection remain out of scope.

## Deskew Behavior

Deskew uses a conservative OpenCV flow:

```text
BGR/BGRA/GRAY -> grayscale
grayscale -> Otsu inverse threshold
foreground mask -> findNonZero
points -> minAreaRect
angle -> normalize to correction angle
correction angle within maxDeskewAngle -> warpAffine
```

If there are not enough foreground points, the step records a fallback note and skips deskew. If the detected correction
angle exceeds `maxDeskewAngle`, the step also records a fallback note and skips.

## Out Of Scope

1. Crop
2. DPI normalization
3. Denoise
4. Contrast normalization
5. Binarization
6. Morphology cleanup
7. Sharpen
8. Artifact upload
9. OCR text extraction
