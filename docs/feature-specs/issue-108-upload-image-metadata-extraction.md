# Issue 108 - Upload Image Metadata Extraction

## Purpose

Upload completion should finalize `Image` rows with original image metadata, not only file name and object key fields.
The metadata is used by project views, job preparation, and later worker output comparison.

## Scope

- Extract metadata from the same object bytes already downloaded during upload completion validation.
- Persist width, height, `dpiX`, and `dpiY` into the `Image` entity.
- Keep metadata extraction non-fatal after magic number validation succeeds.
- Do not add OCR or preprocessing work to the API server.

## Supported Formats

| Format | Metadata |
| --- | --- |
| PNG | Width, height, DPI from `pHYs` when present |
| JPEG | Width, height, DPI from JFIF APP0 when present |
| WEBP | Width, height from VP8X, VP8L, or VP8 headers |
| BMP | Width and height |
| TIFF | Width, height, DPI from baseline IFD tags when present |

## Upload Completion Flow

1. Find the open upload session.
2. Validate project edit permission.
3. Verify requested upload file IDs match the session expectation.
4. Verify each object exists in object storage.
5. Download each object once.
6. Validate the image magic number against file name and content type.
7. Extract image metadata from the downloaded bytes.
8. Mark upload files as `UPLOADED`.
9. Mark the upload session as `COMPLETED`.
10. Create one `Image` row per file with extracted metadata.
11. Create one `ORIGINAL` artifact row per image.

## Failure Policy

- Missing object: fail upload completion.
- Invalid or mismatched image signature: fail upload completion.
- Metadata parsing failure: continue with `null` metadata fields.
- Duplicate upload finalization: skip existing `Image` rows by `uploadSessionFileId`.

## Tests

- `ImageMetadataExtractorTests` covers PNG, JPEG, WEBP VP8X, BMP, and unsupported bytes.
- `UploadCompleteServiceTests` verifies metadata is extracted and passed into image finalization.
- `ImageCreateServiceTests` verifies metadata is applied to created images.
- `ImageTests` verifies `Image.fromUpload` stores metadata fields.
