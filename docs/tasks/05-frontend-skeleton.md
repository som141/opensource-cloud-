# 05. Frontend Skeleton

## Goal

Create the frontend skeleton that is built as static files and served through NGINX.

The frontend only renders screens and calls APIs. It must not contain image preprocessing business logic, Object Storage
secrets, or backend-only security decisions.

## Documents To Read First

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/04-infra-directory-skeleton.md`
4. `docs/api/auth-api.md`
5. `docs/api/job-api.md`
6. `docs/architecture/nginx-routing.md`

## Scope

1. React + Vite + TypeScript project structure
2. App routes
3. Page placeholders
4. Feature and entity folders
5. API client skeleton
6. SSE client skeleton
7. Static deployment Dockerfile and frontend NGINX config

## Work Order

1. Add `frontend/package.json`.
2. Add Vite and TypeScript config.
3. Add `frontend/src/app`.
4. Add `frontend/src/pages`.
5. Add `frontend/src/features`.
6. Add `frontend/src/entities`.
7. Add `frontend/src/shared`.
8. Add `frontend/src/styles`.
9. Add Google login placeholder.
10. Add dashboard placeholder.
11. Add project list/detail placeholders.
12. Add upload placeholder.
13. Add job detail placeholder.
14. Add image detail placeholder.
15. Add preprocessing quality placeholder.
16. Add admin placeholder.
17. Add frontend Dockerfile and NGINX config.
18. Connect the root reverse proxy to the frontend container.

## Deliverables

1. Frontend directory structure
2. Route placeholders
3. API/SSE client placeholders
4. Static file deployment skeleton

## Completion Criteria

1. Frontend is not mixed into the API server.
2. `/api`, `/oauth2`, and `/login/oauth2` calls go through NGINX.
3. No arbitrary UI template library is added.
4. `npm run build` passes.
5. Docker Compose config includes a frontend service.

## Forbidden

1. Do not add Bootstrap, jQuery, or AdminLTE.
2. Do not execute image preprocessing business logic in the frontend.
3. Do not expose Object Storage secrets in the frontend.
4. Do not implement OCR text extraction screens as a product feature.
