# Issue 33. Image Domain Upload Completion Integration

## Goal

Finalize uploaded files as `Image` metadata after upload completion and expose read/download APIs for original and
future preprocessing artifacts.

## Overall Order

1. Upload domain verifies uploaded object existence.
2. Upload domain marks `UploadSessionFile` rows as `UPLOADED`.
3. Upload domain marks the session as `COMPLETED`.
4. Image domain creates one `Image` row for each uploaded file.
5. Image domain creates one `ORIGINAL` `ImageArtifact` row for each image.
6. User lists project images.
7. User opens image detail.
8. User requests temporary download URLs for original, processed, or preview artifacts.
9. User requests processing report or debug artifact URLs after worker integration creates them.

## Functional Units

### Image Metadata

- `Image` stores project, upload session, upload file, uploader, original object key, content type, size, checksum,
  format, status, and future metadata fields.
- `ImageStatus` has `UPLOADED`, `PROCESSING`, `PROCESSED`, `FAILED`, and `DELETED`.
- `ImageFormat` supports `PNG`, `JPG`, `JPEG`, `TIF`, `TIFF`, `BMP`, and `WEBP`.
- `uploadSessionFileId` is unique to prevent duplicate finalization.

### Artifact Metadata

- `ImageArtifact` stores image, optional job/job item IDs, artifact type, object key, content type, size, and debug
  step.
- `ImageArtifactType` has `ORIGINAL`, `PROCESSED`, `PREVIEW`, `PROCESSING_REPORT`, and `DEBUG`.
- Original artifact rows are created immediately after upload completion.
- Processed, preview, report, and debug artifact rows are reserved for worker/internal API integration.

### APIs

- `GET /api/v1/projects/{projectId}/images`
- `GET /api/v1/images/{imageId}`
- `DELETE /api/v1/images/{imageId}`
- `GET /api/v1/images/{imageId}/download?type=original`
- `GET /api/v1/images/{imageId}/download?type=processed`
- `GET /api/v1/images/{imageId}/download?type=preview`
- `GET /api/v1/images/{imageId}/report`
- `GET /api/v1/images/{imageId}/debug-artifacts`

### Access Control

- Project read permission is required for list, detail, download, report, and debug artifact APIs.
- Project edit permission is required for image soft delete.
- Object Storage URLs remain private and are exposed only through temporary download URL responses.

## Out Of Scope

- Real MinIO/S3 presigned download implementation.
- Metadata extraction for width, height, and DPI.
- Worker artifact registration.
- Object deletion cleanup.
- ZIP download.

## Verification

- Entity tests cover `Image` creation from upload and original artifact creation.
- Service tests cover upload finalization, list/detail/delete, download URL creation, report, and debug artifact lookup.
- Upload completion tests verify image finalization is invoked after object existence verification.
- Controller test covers common no-content delete response.
