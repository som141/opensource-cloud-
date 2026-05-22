# Issue 98 - Image Detail UI Flow

## Goal

Replace the `/images/{imageId}` placeholder with an API-backed image detail screen for the local MVP.

The screen focuses on source image metadata and OCR preprocessing outputs related to that image.

## Scope

Included:

- Original image metadata.
- Original image download URL.
- Project and upload session references.
- Related JobItems that processed this image.
- Processed image download through JobItem artifact APIs.
- Links back to the owning project and related jobs.

Excluded:

- OCR text extraction.
- Benchmark screens.
- Full debug artifact browsing.
- Inline image preview rendering.

## Data Flow

```text
User opens /images/{imageId}
  -> GET /api/v1/images/{imageId}
  -> GET /api/v1/jobs?size=100
  -> filter jobs by image.projectId
  -> GET /api/v1/jobs/{jobId}/items?size=500 for each project job
  -> filter JobItems by imageId
```

The current backend stores processed outputs on JobItems, not directly on Image rows. For the MVP UI, the frontend
therefore resolves related processed results through JobItem APIs.

## Download APIs

Original image:

```text
GET /api/v1/images/{imageId}/download?type=original
```

Processed image:

```text
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed
```

Batch ZIP downloads stay on the Job detail screen:

```text
GET /api/v1/jobs/{jobId}/download.zip
```

## UI Behavior

- Shows file name, status, project ID, upload session ID, object key, checksum, content type, size, dimensions, DPI, and
  created time.
- Shows `not extracted` when width, height, or DPI metadata is not available yet.
- Enables original download when the image exists.
- Lists related JobItems with status, preset, worker ID, error information, and processed object key.
- Enables processed download only when `processedObjectKey` exists.
- Provides navigation to `/projects/{projectId}`, `/jobs/{jobId}`, and `/upload`.

## Completion Criteria

- `/images/{imageId}` is no longer a placeholder.
- Authenticated users can inspect original image metadata.
- Original image download opens a signed URL.
- Related processed outputs can be downloaded when available.
- Frontend build passes.
