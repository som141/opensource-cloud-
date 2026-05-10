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

## Future Implementation Points

1. `infra/opencv/OpenCvLoader` loads the OpenCV native library.
2. `infra/opencv/ImageCodecAdapter` decodes source bytes into `ImageMatHolder`.
3. `domain/preprocess/step/DecodeStep` uses the codec adapter.
4. Each step updates the processing context with reportable facts.
5. `domain/artifact` saves processed, preview, report, and debug files to Object Storage.
6. `domain/report` produces `processing-report.json`.
7. Worker reports artifact metadata back through Backend internal API.
