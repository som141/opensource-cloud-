# Issue 21. Upload Domain Skeleton

## Goal

Add a Spring domain-structured upload skeleton for large document image uploads. The API server prepares upload
metadata and presigned URLs, while clients upload file bodies directly to object storage.

## Overall Order

1. Create an upload session under a project.
2. Validate project edit permission.
3. Request presigned upload URLs for image files.
4. Validate file extension, content type, size, and checksum shape.
5. Store `UploadSessionFile` metadata with an original object key.
6. Return upload URL, expiration time, required headers, and `uploadFileId`.
7. Client uploads directly to object storage.
8. Client calls complete with issued `uploadFileIds`.
9. API verifies object existence through `ObjectStoragePort`.
10. API marks files uploaded and completes the session.
11. Later image-domain work creates final `Image` rows from completed upload files.

## Functional Units

### Upload Session

- `UploadSession` stores project, user, expected file count, expected total size, status, and completion timestamps.
- `UploadSessionStatus` has `CREATED`, `UPLOAD_URL_ISSUED`, `COMPLETED`, and `CANCELLED`.
- `UploadSessionService` creates, reads, cancels, and loads open sessions.

### Upload File

- `UploadSessionFile` stores file name, object key, content type, size, checksum, and file status.
- `UploadFileStatus` has `UPLOAD_URL_ISSUED` and `UPLOADED`.
- `UploadSessionFileRepository` supports duplicate checksum lookup and session file lookup.

### Presigned URL

- `PresignedUploadService` validates the request and issues upload targets.
- `PresignedUrlGenerator` is a storage infra port.
- `LocalPresignedUrlGenerator` is a compile-time/local skeleton only.
- Actual MinIO/S3 signing is deferred to the storage adapter task.

### Completion

- `UploadCompleteService` compares requested IDs with persisted upload files.
- Completion requires the number of uploaded files to match `expectedFileCount`.
- Completion verifies object existence through `ObjectStoragePort`.
- The current local adapter returns a placeholder existence result and will be replaced by MinIO/S3.

### Validation

- Supported extensions: `.png`, `.jpg`, `.jpeg`, `.tif`, `.tiff`, `.bmp`, `.webp`.
- Content type must match the extension.
- Per-file limit is 100 MiB in the skeleton.
- SHA-256 checksum must be a 64-character hex string at the DTO level.
- Duplicate checksum is rejected within the request and against existing files in the project.

## API Surface

- `POST /api/v1/projects/{projectId}/upload-sessions`
- `GET /api/v1/upload-sessions/{sessionId}`
- `POST /api/v1/upload-sessions/{sessionId}/files/presigned-url`
- `POST /api/v1/upload-sessions/{sessionId}/complete`
- `DELETE /api/v1/upload-sessions/{sessionId}`

## Out Of Scope

- Direct multipart upload through Spring API.
- Actual MinIO/S3 SDK signing.
- Image metadata row finalization.
- Image magic number inspection.
- ZIP extraction.
- Frontend upload UI.

## Verification

- Unit tests cover upload session state transitions.
- Unit tests cover upload file state transitions.
- Unit tests cover extension/content type/size validation.
- Docker Gradle test/build is required before PR creation.
