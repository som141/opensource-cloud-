# Issue 51 Frontend Skeleton

## Goal

Add a React + Vite frontend skeleton that can be built into static files and served by a frontend NGINX container behind
the root NGINX reverse proxy.

## Work Units

1. Add Vite, TypeScript, React, and package scripts.
2. Add app route skeleton without adding a routing library.
3. Add placeholder pages for login, dashboard, project, upload, job, image, preprocessing quality, and admin.
4. Add feature folders for auth and job progress.
5. Add entity type placeholders.
6. Add shared API client and Job SSE client placeholders.
7. Add frontend Dockerfile and frontend NGINX config.
8. Update local Docker Compose and root NGINX to route `/` and `/assets` to the frontend container.

## Route Placeholders

| Path | Purpose |
| --- | --- |
| `/` | Dashboard placeholder |
| `/login` | Google login entry |
| `/oauth2/success` | OAuth success callback placeholder |
| `/projects` | Project list placeholder |
| `/projects/:projectId` | Project detail placeholder |
| `/upload` | Upload session placeholder |
| `/jobs/:jobId` | Job detail and SSE path placeholder |
| `/images/:imageId` | Image detail placeholder |
| `/benchmarks` | Preprocessing quality placeholder, not OCR product workflow |
| `/admin` | Admin placeholder |

## Non-Goals

- No completed product UI.
- No backend API implementation in the frontend.
- No image preprocessing logic in the frontend.
- No Object Storage direct secret access.
- No Bootstrap, jQuery, or AdminLTE.

## Verification

Run from `frontend/`:

```bash
npm install
npm run build
```

Run from `infra/docker-compose/`:

```bash
docker compose -f docker-compose.local.yml --env-file .env.example config
```
