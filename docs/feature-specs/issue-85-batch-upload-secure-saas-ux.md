# Issue 85. Batch Upload And Secure SaaS Upload UX

## Goal

Extend the local MVP upload screen from a single-image smoke flow to a batch upload console and remove access token
exposure from the OAuth success URL and upload UI.

The feature is still OCR preprocessing only. It does not run OCR text extraction.

## Design References

The UI direction follows common SaaS dashboard patterns without copying a specific product:

1. Vercel dashboard documentation: scope-aware dashboard, activity/status surfaces, and recent item panels.
2. Vercel dashboard redesign notes: fast access to important project status and deployment-like status cards.
3. Linear dashboards documentation: metric blocks, tables, filters, and high-level plus drill-down views.
4. Dropbox upload documentation: web upload supports selecting multiple files in a single upload action.

Reference URLs:

- `https://vercel.com/docs/concepts/dashboard-features`
- `https://vercel.com/blog/dashboard-redesign`
- `https://linear.app/docs/dashboards`
- `https://help.dropbox.com/create-upload/add-files`

## Scope

1. Remove the visible access token input from `/upload`.
2. Remove access token query parameters from OAuth success redirects.
3. Let `OAuthSuccessPage` obtain an access token through `POST /api/v1/auth/refresh` using the HttpOnly refresh cookie.
4. Support selecting multiple document image files in `/upload`.
5. Create one upload session with `expectedFileCount = selectedFiles.length`.
6. Request presigned upload URLs for all selected files in one API call.
7. Upload all selected original files directly to MinIO.
8. Complete the upload session with all issued upload file IDs.
9. Resolve newly created image metadata rows and create one preprocessing job with multiple `imageIds`.
10. Poll job summary and item list, then display per-item artifact download actions.
11. Refresh the frontend visual system into a SaaS-style batch operations console.

## Updated OAuth Flow

```text
Browser
  -> /oauth2/authorization/google
  -> /login/oauth2/code/google
  -> backend sets refresh_token HttpOnly cookie
  -> backend redirects to /oauth2/success?login=success
  -> frontend calls POST /api/v1/auth/refresh with credentials
  -> backend rotates refresh token cookie and returns a short-lived access token
  -> frontend stores the access token for API calls
```

If an older backend still redirects with `accessToken` in the URL, the frontend stores it and immediately removes the
token query parameters with `history.replaceState`.

## Batch Upload Flow

```text
Browser
  -> select N images
  -> POST /api/v1/projects or reuse selected project
  -> GET /api/v1/projects/{projectId}/images
  -> POST /api/v1/projects/{projectId}/upload-sessions
  -> POST /api/v1/upload-sessions/{sessionId}/files/presigned-url with N files
  -> PUT each file to MinIO presigned URL
  -> POST /api/v1/upload-sessions/{sessionId}/complete with N uploadFileIds
  -> GET /api/v1/projects/{projectId}/images
  -> POST /api/v1/jobs with N imageIds
  -> GET /api/v1/jobs/{jobId}/summary
  -> GET /api/v1/jobs/{jobId}/items
  -> GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed|preview|report
```

## Completion Criteria

1. The upload page accepts multiple images.
2. Access token raw value is not rendered in the UI.
3. OAuth success URL no longer contains `accessToken`.
4. NGINX routes exact `/oauth2/success` to the frontend while keeping `/oauth2/authorization/google` on backend-api.
5. The frontend can run a multi-image batch through the existing backend API and Worker.
6. The UI shows selected file count, total size, job progress, and item-level artifact downloads.

## Local Static Cache Policy

The local frontend container disables long-lived caching for the SPA shell so OAuth callback and upload UI changes are
visible immediately after a Docker rebuild.

- `index.html` and fallback SPA routes use `Cache-Control: no-store, no-cache, must-revalidate`.
- `/assets/*` uses `Cache-Control: no-cache, must-revalidate`.
- This is a local verification policy. A production deployment can later use immutable hashed assets after the release
  pipeline guarantees content hashes change for every changed bundle.
