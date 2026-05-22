# Final E2E Verification

## Purpose

This is the final verification order before and after deployment. It separates unauthenticated readiness checks from
the authenticated Google OAuth image-processing flow.

## 1. Local Compose Readiness

```powershell
.\scripts\docker-compose-preflight.ps1
```

Expected:

```text
Preflight passed.
```

## 2. Local Authenticated Image Processing

1. Open `http://localhost/login`.
2. Sign in with Google.
3. Read the access token from DevTools:

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

4. Run:

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

Expected:

- Project is created.
- ZIP is expanded into image files.
- Images upload through presigned URLs.
- Upload completion creates image metadata.
- Job is created.
- Worker completes all JobItems.
- Processed image and processed-only ZIP are downloaded under `out/local-e2e-smoke/{runId}`.

## 3. Production Compose Config

On the server:

```bash
cd /opt/image-preprocess/current
docker compose \
  -f infra/docker-compose/docker-compose.local.yml \
  -f infra/docker-compose/docker-compose.prod.yml \
  --env-file infra/docker-compose/.env.prod \
  config
```

Expected:

- Only NGINX publishes a public port.
- backend-api, PostgreSQL, RabbitMQ, and MinIO direct ports are not published.
- `REFRESH_TOKEN_COOKIE_SECURE=true`.
- `MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN`.

## 4. GitHub Actions Deployment

Run `Deploy Production` from GitHub Actions or merge to `main` after the workflow is stable.

Expected:

- Compose template validation passes.
- Archive upload succeeds.
- Server deploy step succeeds.
- `GET /health` succeeds.
- `GET /v3/api-docs` contains `Image Preprocess Platform API`.

## 5. Production Browser Smoke

1. Open `https://YOUR_DOMAIN/login`.
2. Sign in with Google.
3. Confirm the browser returns to:

```text
https://YOUR_DOMAIN/oauth2/success
```

4. Confirm the URL does not contain an access token.
5. Open Upload.
6. Upload one PNG/JPEG document image.
7. Confirm the Job reaches `SUCCEEDED`.
8. Download the processed output.

## 6. Production Authenticated Script Smoke

After browser login:

```powershell
.\scripts\local-e2e-smoke.ps1 `
  -BaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<access-token>"
```

Expected:

- All JobItems succeed.
- Processed output ZIP downloads.

## Stop Conditions

Do not proceed to public use if any of these are true:

- OAuth returns `invalid_client` or redirect URI mismatch.
- `/health` fails.
- Worker cannot call internal API.
- Upload presigned URLs point to an unreachable host.
- Processed ZIP download is empty.
- Refresh token cookie is not Secure on HTTPS.
- `.env.prod` contains placeholder `CHANGE_ME` or `YOUR_DOMAIN` values.
