# Issue 96 - Job Detail UI Flow

## Goal

Replace the `/jobs/{jobId}` placeholder with an API-backed Job detail screen for the local MVP.

The screen focuses on OCR preprocessing operations only:

- Job metadata
- progress counters
- image-level Worker item states
- processed image download
- processed-only ZIP download

OCR text extraction, benchmark screens, preview display, report display, and debug artifact browsing remain out of scope.

## User Flow

```text
User opens /jobs/{jobId}
  -> frontend loads Job detail
  -> frontend loads Job summary
  -> frontend loads JobItems
  -> while Job is not terminal, frontend refreshes every 2.5 seconds
  -> user downloads one processed image or the processed ZIP
```

## API Calls

```text
GET /api/v1/jobs/{jobId}
GET /api/v1/jobs/{jobId}/summary
GET /api/v1/jobs/{jobId}/items?size=500
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed
GET /api/v1/jobs/{jobId}/download.zip
```

All calls use the existing access token and refresh-cookie behavior through the shared API client.

## UI Behavior

- Shows Job status, preset, priority, debug flag, created/started/completed timestamps.
- Shows total, succeeded, failed, and processed-downloadable counts.
- Shows a progress bar based on `progressPercent`.
- Polls every `2.5s` until the Job reaches a terminal status.
- Lists JobItems with status, image ID, attempt, worker ID, timestamps, errors, and processed object key.
- Enables item download only when `processedObjectKey` exists.
- Enables processed ZIP download only when at least one processed item exists.
- Adds an "Open job detail" link from the upload result progress card.

## Terminal Statuses

```text
SUCCEEDED
FAILED
PARTIAL_SUCCEEDED
CANCELLED
```

## Completion Criteria

- `/jobs/{jobId}` is no longer a placeholder.
- Authenticated users can inspect Job progress and item results.
- Processed image download opens a signed download URL.
- Processed ZIP download opens a signed download URL.
- Frontend build passes.
