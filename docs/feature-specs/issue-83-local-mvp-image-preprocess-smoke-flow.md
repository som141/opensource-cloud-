# Issue 83. Local MVP Image Preprocess Smoke Flow

## Goal

Make the local Docker stack usable for a minimal frontend-driven image preprocessing smoke test.

The smoke flow is still OCR preprocessing only. It does not run OCR text extraction and does not store recognized text.

## Scope

1. Replace backend storage placeholders with a real MinIO-backed adapter.
2. Generate browser-reachable presigned upload/download URLs with a public endpoint.
3. Verify uploaded object existence against MinIO before completing an upload session.
4. Enable the local Worker listener by default for Docker Compose smoke tests.
5. Add a frontend `/upload` smoke page that can:
   - use the Google OAuth access token stored by the callback page,
   - create or select a project,
   - upload one image through the presigned URL,
   - complete the upload session,
   - create a preprocessing job,
   - poll job summary and item status,
   - open presigned download URLs for processed image, preview image, and processing report.

## Local Flow

```text
Browser
  -> /oauth2/authorization/google
  -> /oauth2/success?accessToken=...
  -> /upload
  -> POST /api/v1/projects
  -> POST /api/v1/projects/{projectId}/upload-sessions
  -> POST /api/v1/upload-sessions/{sessionId}/files/presigned-url
  -> PUT http://localhost:9000/{bucket}/{objectKey}
  -> POST /api/v1/upload-sessions/{sessionId}/complete
  -> GET /api/v1/projects/{projectId}/images
  -> POST /api/v1/jobs
  -> GET /api/v1/jobs/{jobId}/summary
  -> GET /api/v1/jobs/{jobId}/items
  -> GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed|preview|report
```

## Required Local Environment

`infra/docker-compose/.env` must contain real Google OAuth values:

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost/oauth2/success
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_REGION=us-east-1
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173
WORKER_LISTENER_ENABLED=true
```

Register this Google OAuth redirect URI:

```text
http://localhost/login/oauth2/code/google
```

For the step-by-step build, Docker run, smoke test, download, and troubleshooting guide, see:

```text
docs/operation/local-mvp-build-run-test.md
```

## Expected Result

After submitting one image from `/upload`:

1. MinIO contains the original object under `originals/...`.
2. The Worker consumes the RabbitMQ message.
3. The job item reaches `SUCCEEDED` or shows a concrete failure code.
4. On success, the job item exposes:
   - `processedObjectKey`
   - `previewObjectKey`
   - `reportObjectKey`
5. The frontend exposes download buttons for processed image, preview image, and report JSON.
6. If debug is enabled, MinIO also contains `processed/{projectId}/{jobId}/{itemId}/debug/*.png`.

## Verified Local Result

The Docker smoke flow was verified with a generated document-like PNG:

```text
frontend /upload page: available through http://localhost/upload
backend health: UP
RabbitMQ queues: image.preprocess.high, normal, retry each has one Worker consumer
MinIO CORS preflight for presigned PUT: 204
Worker item result: SUCCEEDED
processedObjectKey: processed/{projectId}/{jobId}/{itemId}/processed.png
previewObjectKey: processed/{projectId}/{jobId}/{itemId}/preview.png
reportObjectKey: processed/{projectId}/{jobId}/{itemId}/processing-report.json
download buttons: processed, preview, report
debug artifacts: uploaded when debug=true
```

The summary API currently returns counts and progress percentage, not a textual job status. The local smoke page uses
`succeeded + failed >= total` and item status for completion detection.

## Known Limits

1. The smoke page is intentionally minimal and not a full production upload UI.
2. The smoke page opens presigned URLs in a new tab. It is not a full artifact browser yet.
3. Debug artifact download buttons are not included yet; debug object keys remain available in MinIO.
4. SSE still needs an auth-compatible frontend integration. This page uses polling for local smoke verification.
