# Issue 88. Project UI Flow

## Goal

Replace project placeholder pages with API-backed project management screens for the local MVP.

The project domain groups uploaded document images and preprocessing jobs. This feature does not add OCR extraction,
benchmarking, admin dashboards, audit logs, or notification flows.

## Scope

1. Connect `/projects` to the real Project API.
2. Add project creation from the project list page.
3. Show project cards with role, owner, default preset, and update time.
4. Allow project owners to delete projects from the list.
5. Connect `/projects/{projectId}` to project detail, summary, images, and recent jobs.
6. Allow project metadata updates from the detail page.
7. Link project detail to upload and job/image detail routes.
8. Replace fixed project `jobCount = 0` with a real count from `JobRepository`.

## API Usage

```text
GET    /api/v1/projects?size=50
POST   /api/v1/projects
GET    /api/v1/projects/{projectId}
PATCH  /api/v1/projects/{projectId}
DELETE /api/v1/projects/{projectId}
GET    /api/v1/projects/{projectId}/summary
GET    /api/v1/projects/{projectId}/images?size=100
GET    /api/v1/jobs?size=100
```

The frontend filters the current user's job list by `projectId` until a project-scoped job list endpoint is needed.

## Completion Criteria

1. `/projects` is no longer a placeholder.
2. `/projects/{projectId}` is no longer a placeholder.
3. A logged-in user can create a project from the project list page.
4. Project detail shows summary metrics, images, and recent jobs.
5. Project summary returns the real job count.
6. Frontend build and backend tests pass.
