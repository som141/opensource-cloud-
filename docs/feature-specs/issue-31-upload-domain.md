# Issue 31. Upload Domain

## Goal

Implement the backend upload domain for large document image uploads. The API server creates upload sessions, issues
presigned object storage targets, validates upload metadata, and marks a session complete after object existence checks.
The Spring API does not receive large file bodies.

## Overall Order

1. User selects a project and starts an upload session.
2. API validates project edit permission with `ProjectPermissionService`.
3. API stores `UploadSession` with expected file count and expected total size.
4. User requests presigned upload URLs with file metadata.
5. API validates extension, content type, per-file size, checksum shape, duplicate checksum, expected count, and
   expected size.
6. API stores `UploadSessionFile` metadata and returns `uploadFileId`, object key, URL, expiration, and required
   headers.
7. Client uploads image files directly to Object Storage.
8. Client calls complete with issued `uploadFileIds`.
9. API verifies each object through `ObjectStoragePort`.
10. API marks each file as `UPLOADED` and the session as `COMPLETED`.
11. Later image-domain work creates final `Image` rows from completed upload files.

## Functional Units

### Upload Session

- `UploadSession` stores project, creator, status, expected file count, expected total size, and completion timestamps.
- `UploadSessionStatus` has `CREATED`, `UPLOAD_URL_ISSUED`, `COMPLETED`, and `CANCELLED`.
- Session read/cancel checks project membership, so a project editor can operate on a teammate's session if permitted.
- Completed or cancelled sessions cannot receive new upload URLs.
- Completed sessions cannot be cancelled.

### Upload File

- `UploadSessionFile` stores original file name, object key, content type, size, checksum, and status.
- `UploadFileStatus` has `UPLOAD_URL_ISSUED` and `UPLOADED`.
- Duplicate checksum is rejected within a project.

### Presigned URL

- `PresignedUploadService` issues upload targets through `PresignedUrlGenerator`.
- The current local generator is a skeleton for local development.
- Actual MinIO/S3 signing is a storage adapter task.
- Repeated presigned URL requests cannot exceed the upload session's expected file count or expected total size.

### Completion

- `UploadCompleteService` requires requested IDs to belong to the session.
- Completion requires the uploaded file count to match `expectedFileCount`.
- Completion verifies object existence through `ObjectStoragePort`.
- The local object storage adapter is a placeholder and will be replaced by MinIO/S3 behavior.

### Validation

- Supported extensions: `.png`, `.jpg`, `.jpeg`, `.tif`, `.tiff`, `.bmp`, `.webp`.
- Content type must match the file extension.
- Per-file limit is 100 MiB in this implementation.
- SHA-256 checksum must be a 64-character hex string at the DTO validation layer.

## API Surface

- `POST /api/v1/projects/{projectId}/upload-sessions`
- `GET /api/v1/upload-sessions/{sessionId}`
- `POST /api/v1/upload-sessions/{sessionId}/files/presigned-url`
- `POST /api/v1/upload-sessions/{sessionId}/complete`
- `DELETE /api/v1/upload-sessions/{sessionId}`

## Out Of Scope

- Direct multipart upload through Spring API.
- Actual MinIO/S3 SDK signing.
- Final `Image` metadata row creation.
- Image magic number inspection.
- ZIP extraction.
- Frontend upload UI.

## Verification

- Unit tests cover upload session creation, read permission, and cancel rejection.
- Unit tests cover upload session and file state transitions.
- Unit tests cover presigned URL generation and duplicate checksum rejection.
- Unit tests cover repeated presigned URL count and size limit rejection.
- Unit tests cover upload completion and missing-object failure.
- Controller tests cover common response codes for create and cancel.
