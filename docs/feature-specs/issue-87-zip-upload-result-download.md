# Issue 87 - ZIP Upload And Processed Result Download

## Scope

This task adds two local MVP workflow improvements:

1. Users can select a ZIP file in the frontend upload page.
2. Users can download all processed images for a Job as one ZIP file.

This does not change the core architecture. The Spring API still avoids receiving large file bodies directly, and the
Worker remains responsible for preprocessing images.

## ZIP Upload Flow

The frontend expands ZIP files in the browser with `jszip`.

```text
User selects images or a ZIP file
  -> Frontend extracts supported image entries from ZIP
  -> Frontend creates one upload session for extracted images
  -> Frontend requests presigned upload URLs
  -> Frontend uploads each extracted image to object storage
  -> Frontend completes the upload session
  -> Frontend creates a preprocessing Job
```

Supported image entries:

- `png`
- `jpg`
- `jpeg`
- `webp`
- `bmp`
- `tif`
- `tiff`

Limits:

- Maximum extracted images per ZIP: `500`
- Maximum extracted image bytes per ZIP: `512 MB`

Reasoning:

- Browser-side extraction keeps the Spring API out of large multipart ZIP uploads.
- The existing presigned upload and checksum validation path is reused.
- Each extracted image still becomes a separate JobItem and RabbitMQ message.

## Processed ZIP Download Flow

```text
User clicks "Download processed ZIP"
  -> Frontend calls GET /api/v1/jobs/{jobId}/download.zip
  -> API validates project read permission
  -> API finds SUCCEEDED JobItems with processedObjectKey
  -> API downloads processed images from object storage
  -> API creates processed-results.zip
  -> API uploads archive to object storage
  -> API returns a presigned download URL
  -> Frontend downloads the ZIP file
```

Archive path:

```text
archives/{projectId}/{jobId}/processed-results.zip
```

ZIP contents:

- Processed images only.
- Preview images are excluded.
- Processing reports are excluded.
- Debug artifacts are excluded.

Entry name format:

```text
image-{imageId}-item-{itemId}-processed.{ext}
```

## API Contract

```text
GET /api/v1/jobs/{jobId}/download.zip
Authorization: Bearer <access-token>
```

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "jobId": 1,
    "fileCount": 3,
    "objectKey": "archives/10/1/processed-results.zip",
    "downloadUrl": "http://localhost/image-preprocess-local/archives/10/1/processed-results.zip?...",
    "expiresAt": "2026-05-16T00:00:00Z"
  }
}
```

Failure behavior:

- `common404` when no processed images are ready.
- Permission errors follow the project permission policy.

## Completion Criteria

- Frontend accepts image files and ZIP files on the upload page.
- ZIP entries are expanded before presigned upload requests.
- Backend creates a processed-only ZIP archive for completed JobItems.
- Backend returns a presigned download URL for the generated archive.
- API docs explain the ZIP upload and result download behavior.
- Frontend build and backend tests pass.
