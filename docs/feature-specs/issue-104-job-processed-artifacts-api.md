# Issue 104 - Job Processed Artifact API

## Purpose

Replace the placeholder response from `GET /api/v1/jobs/{jobId}/artifacts` with a real processed-image artifact
listing for the MVP flow.

The endpoint is a user-facing read API. It exposes processed output images only. Preview images, processing reports, and
debug artifacts stay out of the MVP response.

## End-To-End Flow

1. The user calls `GET /api/v1/jobs/{jobId}/artifacts`.
2. The API loads the Job and validates project read permission.
3. The API loads every JobItem for the Job.
4. The API selects only JobItems with status `SUCCEEDED` and a non-empty `processedObjectKey`.
5. The API creates a presigned download URL for each processed object key.
6. The API returns the Job-level item count and processed artifact list in the common response format.

## Response Shape

```json
{
  "jobId": 1,
  "totalItems": 3,
  "processedReadyCount": 2,
  "processedArtifacts": [
    {
      "itemId": 10,
      "imageId": 100,
      "status": "SUCCEEDED",
      "objectKey": "processed/1/1/10/processed.png",
      "downloadUrl": "http://localhost:9000/image-preprocess-local/processed/1/1/10/processed.png?...",
      "expiresAt": "2026-05-15T09:00:00Z"
    }
  ]
}
```

## Functional Units

### 1. Placeholder Removal

- Remove the previous `JobArtifactResponse.skeleton()` path.
- Replace the old `message` field with a real artifact list response.

### 2. Processed-Only Policy

Included:

- JobItem status is `SUCCEEDED`.
- `processedObjectKey` is not null or blank.

Excluded:

- Preview artifacts.
- Processing reports.
- Debug artifacts.
- Processing, failed, cancelled, queued, or retrying items.

### 3. Download URL Generation

- Use the existing `PresignedDownloadUrlGenerator`.
- Use the same 10-minute expiration window as item-level downloads.
- Do not call the generator when no processed artifacts are ready.

### 4. Tests

- Verify that processed artifacts are listed with download URLs.
- Verify that an empty list is returned when no processed image is ready.
- Keep existing item-level artifact download tests unchanged.

## Out Of Scope

- Preview/report/debug artifact listing.
- `ImageArtifact` row-based artifact expansion.
- Frontend changes.
- ZIP download behavior changes.

## Verification

- `JobArtifactResponse` returns real processed artifact data instead of placeholder text.
- `JobQueryService.artifacts()` keeps project read permission validation.
- Backend tests pass.
- `docs/api/job-api.md` matches the actual API behavior.
