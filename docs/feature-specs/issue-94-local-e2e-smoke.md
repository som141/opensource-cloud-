# Issue 94 - Docker Compose Local E2E Smoke

## Goal

Add a reproducible local MVP smoke flow for Docker Compose. The smoke flow verifies that the core preprocessing path
works end to end before cloud deployment:

```text
Google-authenticated user token
  -> create project
  -> generate ZIP input and expand images
  -> create upload session
  -> upload originals through presigned URLs
  -> complete upload session
  -> create preprocessing Job
  -> wait for Worker completion
  -> download one processed image
  -> download processed-results ZIP
```

OCR text extraction is intentionally out of scope. The smoke target is OCR preprocessing output generation.

## Implementation

The smoke runner is:

```text
scripts/local-e2e-smoke.ps1
```

The script:

- Generates two synthetic document-like PNG images.
- Compresses them into a ZIP file.
- Expands the ZIP locally to mirror the frontend browser-side ZIP upload behavior.
- Uses the normal API upload session and presigned object storage upload flow.
- Creates one Job with both uploaded image IDs.
- Polls Job summary and JobItem status until terminal state.
- Downloads one processed image through the JobItem artifact download API.
- Downloads the processed-only ZIP through `GET /api/v1/jobs/{jobId}/download.zip`.
- Writes a `summary.json` file under `out/local-e2e-smoke/{runId}`.

## Authentication

The public APIs require a user access token. The script does not add a development auth bypass.

To obtain a local token:

1. Start Docker Compose.
2. Open `http://localhost/login`.
3. Sign in with Google.
4. Open browser DevTools.
5. Read the token:

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

Then pass it as `-AccessToken` or set `ACCESS_TOKEN`.

## Command

From the repository root:

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

Optional direct-backend mode:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "http://localhost:8080/api" `
  -AccessToken "<access-token>"
```

## Success Criteria

- Project creation returns `common200` or `common201`.
- Upload session completes.
- Job reaches terminal state before timeout.
- `failed` count is `0`.
- At least one JobItem has `processedObjectKey`.
- Processed image download file exists and is non-empty.
- Processed ZIP download file exists and is non-empty.

## Output

```text
out/local-e2e-smoke/{runId}/
  source/
    smoke-document-001.png
    smoke-document-002.png
  extracted/
    smoke-document-001.png
    smoke-document-002.png
  downloads/
    processed-item-{itemId}.png
    job-{jobId}-processed-results.zip
  smoke-input.zip
  summary.json
```

## Notes

- The script creates a fresh project per run, so duplicate checksum validation should not block repeat runs.
- The ZIP upload part is simulated at the client boundary by local expansion, matching the frontend design.
- If the Worker is disabled, the script times out while polling the Job.
