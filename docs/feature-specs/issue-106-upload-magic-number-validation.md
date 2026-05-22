# Issue 106 - Upload Magic Number Validation

## Purpose

Add server-side image signature validation to the upload completion flow. Frontend file filters and client-provided
content types are not security boundaries, so the API must inspect the bytes stored in Object Storage before finalizing
image metadata.

## End-To-End Flow

1. The frontend creates an upload session.
2. The frontend requests presigned upload URLs with file name, content type, size, and checksum metadata.
3. The frontend uploads each original file directly to Object Storage.
4. The frontend calls `POST /api/v1/upload-sessions/{sessionId}/complete`.
5. The API validates the requested upload file IDs and object existence.
6. The API downloads each uploaded object through `ObjectStoragePort`.
7. The API detects the actual image format from the magic number.
8. The detected format must match the stored file name extension and content type.
9. Only valid files are marked as uploaded and converted into finalized `Image` rows.

## Supported Signatures

| Format | Magic number rule | Extensions | Content types |
| --- | --- | --- | --- |
| PNG | `89 50 4E 47 0D 0A 1A 0A` | `.png` | `image/png` |
| JPEG | `FF D8 FF` prefix | `.jpg`, `.jpeg` | `image/jpeg` |
| WEBP | `RIFF` at offset 0 and `WEBP` at offset 8 | `.webp` | `image/webp` |
| BMP | `42 4D` prefix | `.bmp` | `image/bmp`, `image/x-ms-bmp` |
| TIFF | `49 49 2A 00` or `4D 4D 00 2A` | `.tif`, `.tiff` | `image/tiff` |

## Functional Units

### 1. Signature Validator

- `UploadedImageMagicNumberValidator` detects the actual image format from bytes.
- It compares detected format against `UploadSessionFile.originalFileName`.
- It compares detected format against `UploadSessionFile.contentType`.
- It throws `InvalidUploadFileException` when the signature is unsupported or mismatched.

### 2. Upload Complete Integration

- `UploadCompleteService` still verifies object existence first.
- After existence verification, it downloads object bytes.
- The validator runs before `UploadSessionFile.markUploaded()`.
- Invalid files prevent session completion and image row creation.

### 3. Tests

- Validator accepts PNG, JPEG, WEBP, BMP, and TIFF signatures.
- Validator rejects extension mismatches.
- Validator rejects content type mismatches.
- Validator rejects unsupported/corrupted signatures.
- Upload completion rejects invalid uploaded object bytes before image finalization.

## Out Of Scope

- Server-side ZIP extraction.
- Width, height, and DPI extraction.
- Full checksum recomputation.
- Antivirus scanning.

## Verification

- Backend test/build passes.
- `docs/api/upload-api.md` describes completion-time signature validation.
- `docs/api/image-api.md` describes magic validation before image row creation.
