# Project API

## Scope

Project groups uploaded document images and preprocessing jobs. All image, upload, and job access checks should be based on project membership.

## Base Path

```text
/api/v1/projects
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/projects` | Create a project |
| `GET` | `/api/v1/projects` | List projects owned by the current user |
| `GET` | `/api/v1/projects/{projectId}` | Get project detail |
| `PATCH` | `/api/v1/projects/{projectId}` | Update project metadata |
| `DELETE` | `/api/v1/projects/{projectId}` | Soft delete project |
| `GET` | `/api/v1/projects/{projectId}/summary` | Get project summary skeleton |
| `POST` | `/api/v1/projects/{projectId}/members` | Invite project member |
| `GET` | `/api/v1/projects/{projectId}/members` | List project members |
| `DELETE` | `/api/v1/projects/{projectId}/members/{userId}` | Remove project member |

## Roles

| Role | Permission |
|---|---|
| `OWNER` | Read, edit, delete, and manage members |
| `EDITOR` | Read and edit project metadata |
| `VIEWER` | Read only |

## Notes

- Delete is soft delete through `ProjectStatus.DELETED`.
- Controller methods use `@CurrentUser`; the resolver will be implemented with the later JWT/auth work.
- Summary currently returns a skeleton response until image/job domains exist.
