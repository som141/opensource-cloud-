# image-test Integration Boundary

## Purpose

The Worker will integrate the document image preprocessing mechanism from `som141/image-test`.
The integration target is OCR preprocessing only, not OCR text extraction.

## In Scope

- Decode source image bytes into an OpenCV image representation.
- Normalize color input into a predictable processing format.
- Normalize orientation and deskew scanned document images.
- Crop document regions from scanner borders or background.
- Denoise scan artifacts.
- Normalize contrast for low-contrast scans.
- Binarize document images for OCR readiness.
- Run morphology cleanup to reduce small noise and repair strokes.
- Normalize DPI for OCR quality consistency.
- Optionally sharpen final document images.
- Produce processed image, preview image, debug artifacts, and processing report metadata.

## Out of Scope

- Running Tesseract or any OCR engine as a product feature.
- Returning recognized text to users.
- Persisting OCR text results.
- Searching or correcting OCR text.
- Providing OCR billing, language packs, or text-review workflows.

## Current Skeleton

Issue 49 completes the missing Worker skeleton boundaries:

- `domain/preprocess/model`
- `domain/artifact`
- `domain/report`
- `infra/opencv`
- `infra/tracing`
- `infra/metrics`

These classes are intentionally lightweight. They define integration seams for later OpenCV work without adding native
OpenCV dependencies or OCR runtime behavior in the skeleton PR.

Issue 63 adds the first real OpenCV runtime boundary:

- `OpenCvLoader` loads the native OpenCV library through the Worker runtime.
- `ImageCodecAdapter` decodes image bytes into an OpenCV `Mat`.
- `ImageMatHolder` exposes source key, dimensions, color space, loaded/released state, and the owned `Mat`.
- `MatResourceCleaner` releases holder-owned Mat resources.

This still does not replace the pipeline `DecodeStep`. The next task should connect Object Storage download bytes to
`DecodeStep` and store the decoded holder in the preprocessing context.

Issue 65 connects the `DecodeStep` to the codec boundary when source bytes are available. The pipeline context now owns
the decoded holder during execution and releases it when the runner finishes.

Issue 67 connects Object Storage download bytes to the context before pipeline execution. A valid Worker message can now
fetch the original object and feed those bytes into `DecodeStep`.

Issue 69 normalizes decoded image color layout to BGR. This keeps downstream OpenCV document preprocessing steps from
handling multiple input channel layouts.

Issue 71 adds the first geometry corrections. Orientation normalization handles obvious landscape scans, and deskew
uses foreground geometry to correct moderate scan tilt.

Issue 73 adds the second geometry increment. Crop uses foreground bounds to remove surrounding border/background where
safe, and DPI normalization scales the current Mat only when source DPI metadata is available. Missing source DPI is a
fallback no-op because OCR-oriented DPI normalization should be based on real metadata, not guessed values.

Issue 75 adds the first quality-processing increment. Denoise removes scan noise with median or bilateral filtering,
contrast normalization applies CLAHE, and binarization produces a single-channel binary image using Otsu or adaptive
thresholding.

Issue 77 completes the currently planned quality-processing steps. Morphology cleanup applies open/close operations with
black document strokes treated as foreground, and optional sharpen applies unsharp masking only for presets that enable
`sharpen`.

Issue 79 persists the pipeline output. The Worker encodes the final Mat as `processed.png`, creates a bounded
`preview.png`, writes `processing-report.json`, uploads all three artifacts to Object Storage, and then calls the Backend
Internal Worker API success endpoint with the object keys.

## Future Implementation Points

1. Generate and upload real per-step debug images.
2. Expand report fields with detected image-processing facts.
3. Keep OCR text extraction outside the product runtime.
