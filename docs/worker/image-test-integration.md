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

## Future Implementation Points

1. Object Storage download supplies source bytes to the context.
2. Each step updates the processing context with reportable facts.
3. `domain/artifact` saves processed, preview, report, and debug files to Object Storage.
4. `domain/report` produces `processing-report.json`.
5. Worker reports artifact metadata back through Backend internal API.
