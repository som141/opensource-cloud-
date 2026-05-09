# Upload API

## Purpose

The upload API prepares large document image uploads without sending file bodies through the Spring API server.
Clients create an upload session, request presigned object storage URLs, upload files directly to object storage, and
then notify the API that the upload is complete.

## Rules

- The Spring API must not receive large image file bodies.
- Presigned upload URLs must include an expiration time.
- Upload completion must verify object existence through `ObjectStoragePort`.
- Image rows are not finalized before upload completion.
- Upload session access is controlled by project membership, not only by the user who created the session.
- Supported image extensions are `png`, `jpg`, `jpeg`, `tif`, `tiff`, `bmp`, and `webp`.
- Checksums are collected as SHA-256 hex strings and are used for duplicate detection.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/projects/{projectId}/upload-sessions` | Create an upload session |
| `GET` | `/api/v1/upload-sessions/{sessionId}` | Read an upload session |
| `POST` | `/api/v1/upload-sessions/{sessionId}/files/presigned-url` | Issue presigned upload URLs |
| `POST` | `/api/v1/upload-sessions/{sessionId}/complete` | Complete an upload session |
| `DELETE` | `/api/v1/upload-sessions/{sessionId}` | Cancel an upload session |

## Create Upload Session

Request:

```json
{
  "expectedFileCount": 3,
  "expectedTotalSizeBytes": 12000000
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common201",
  "message": "Created successfully.",
  "result": {
    "id": 1,
    "projectId": 10,
    "userId": 20,
    "status": "CREATED",
    "expectedFileCount": 3,
    "expectedTotalSizeBytes": 12000000,
    "completedAt": null,
    "cancelledAt": null
  }
}
```

## Issue Presigned Upload URLs

Request:

```json
{
  "files": [
    {
      "fileName": "scan_001.png",
      "contentType": "image/png",
      "sizeBytes": 4200000,
      "checksumSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
  ]
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "sessionId": 1,
    "uploadTargets": [
      {
        "uploadFileId": 100,
        "objectKey": "originals/10/1/uuid/scan_001.png",
        "uploadUrl": "http://localhost:9000/local-presigned/originals/10/1/uuid/scan_001.png",
        "expiresAt": "2026-05-03T08:00:00Z",
        "requiredHeaders": {
          "Content-Type": "image/png"
        }
      }
    ]
  }
}
```

The response uses `uploadFileId`, not `imageId`, because the image metadata row should be finalized only after
object storage verification succeeds.

Repeated presigned URL requests for the same session cannot exceed the session's expected file count or expected total
size. This prevents clients from issuing more object keys than the upload session originally declared.

## Complete Upload Session

Request:

```json
{
  "uploadFileIds": [100, 101, 102]
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "sessionId": 1,
    "status": "COMPLETED",
    "uploadedFileCount": 3
  }
}
```

## Status

| Status | Meaning |
| --- | --- |
| `CREATED` | Session exists but no upload URL has been issued yet |
| `UPLOAD_URL_ISSUED` | One or more presigned upload URLs have been issued |
| `COMPLETED` | All expected files have been verified in object storage |
| `CANCELLED` | User cancelled the upload session |

## Next Work

- Replace the local presigned URL generator with a MinIO/S3 adapter.
- Add image magic number validation after object download or metadata extraction.
- Add ZIP upload as a separate expansion task.
