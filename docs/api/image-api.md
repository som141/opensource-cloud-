# Image API

## Purpose

Image API manages finalized image metadata after upload completion. The API exposes project image lists, image detail,
soft delete, original/processed/preview download URLs, processing report lookup, and debug artifact lookup.

## Rules

- `Image` rows are created only after upload completion verifies object existence.
- The Spring API does not expose public object storage URLs.
- Download endpoints return temporary signed URLs through `PresignedDownloadUrlGenerator`.
- Original files and preprocessing artifacts are stored separately as `ImageArtifact` rows.
- Access is controlled by project membership through `ProjectPermissionService`.
- Deleting an image is a soft delete and does not immediately remove Object Storage objects.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/projects/{projectId}/images` | List project images |
| `GET` | `/api/v1/images/{imageId}` | Read image detail |
| `DELETE` | `/api/v1/images/{imageId}` | Soft delete image |
| `GET` | `/api/v1/images/{imageId}/download?type=original` | Get original download URL |
| `GET` | `/api/v1/images/{imageId}/download?type=processed` | Get processed download URL |
| `GET` | `/api/v1/images/{imageId}/download?type=preview` | Get preview download URL |
| `GET` | `/api/v1/images/{imageId}/report` | Get processing report download URL |
| `GET` | `/api/v1/images/{imageId}/debug-artifacts` | List debug artifact download URLs |

## List Images

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "content": [
      {
        "id": 200,
        "projectId": 10,
        "originalFileName": "scan_001.png",
        "contentType": "image/png",
        "sizeBytes": 1024,
        "format": "PNG",
        "status": "UPLOADED",
        "createdAt": "2026-05-09T21:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## Image Detail

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "id": 200,
    "projectId": 10,
    "uploadSessionId": 1,
    "uploadSessionFileId": 100,
    "uploaderId": 20,
    "originalFileName": "scan_001.png",
    "originalObjectKey": "originals/10/1/file/scan_001.png",
    "contentType": "image/png",
    "sizeBytes": 1024,
    "checksumSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "format": "PNG",
    "status": "UPLOADED",
    "width": null,
    "height": null,
    "dpiX": null,
    "dpiY": null,
    "createdAt": "2026-05-09T21:00:00"
  }
}
```

## Download URL

Request:

```text
GET /api/v1/images/200/download?type=original
```

Supported `type` values:

- `original`
- `processed`
- `preview`

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "imageId": 200,
    "type": "ORIGINAL",
    "objectKey": "originals/10/1/file/scan_001.png",
    "downloadUrl": "http://localhost:9000/local-download/originals/10/1/file/scan_001.png",
    "expiresAt": "2026-05-09T12:10:00Z",
    "requiredHeaders": {}
  }
}
```

## Report And Debug Artifacts

`/report` returns the latest `PROCESSING_REPORT` artifact download URL.

`/debug-artifacts` returns all `DEBUG` artifacts for the image. Debug artifacts are expected to be created later by the
worker/report task and are empty until worker integration exists.

## Upload Completion Integration

When `POST /api/v1/upload-sessions/{sessionId}/complete` succeeds:

1. API verifies the original objects exist.
2. API downloads each original object and validates the image magic number.
3. API rejects files whose byte signature does not match the declared extension or content type.
4. API marks upload files as `UPLOADED`.
5. API marks the upload session as `COMPLETED`.
6. API creates one `Image` row per uploaded file.
7. API creates one `ORIGINAL` `ImageArtifact` row per image.

The `Image` row is idempotent by `uploadSessionFileId`, so repeated internal finalization does not create duplicate
images.

## Next Work

- Replace local download URL generator with a MinIO/S3 adapter.
- Add metadata extraction for width, height, and DPI.
- Add worker internal artifact registration for processed, preview, report, and debug files.
