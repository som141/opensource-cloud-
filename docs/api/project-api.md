# Project API

## Purpose

Project API manages the unit that groups uploaded document images and preprocessing jobs. Future image, upload, and job
APIs should authorize access through project membership.

## Roles

| Role | Read | Update Project | Manage Members | Delete Project |
| --- | --- | --- | --- | --- |
| `OWNER` | Yes | Yes | Yes | Yes |
| `EDITOR` | Yes | Yes | No | No |
| `VIEWER` | Yes | No | No | No |

## Endpoints

All endpoints require `Authorization: Bearer <access-token>`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/projects` | Create a project |
| `GET` | `/api/v1/projects` | List projects for the current user |
| `GET` | `/api/v1/projects/{projectId}` | Read project detail |
| `PATCH` | `/api/v1/projects/{projectId}` | Update project metadata |
| `DELETE` | `/api/v1/projects/{projectId}` | Soft delete project |
| `GET` | `/api/v1/projects/{projectId}/summary` | Read project summary |
| `POST` | `/api/v1/projects/{projectId}/members` | Invite project member |
| `GET` | `/api/v1/projects/{projectId}/members` | List project members |
| `DELETE` | `/api/v1/projects/{projectId}/members/{userId}` | Remove project member |

## Create Project

Request:

```json
{
  "name": "Library Scan Batch",
  "description": "2026 first library document preprocessing batch",
  "defaultPreset": "LOW_CONTRAST_SCAN"
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common201",
  "message": "Created.",
  "result": {
    "id": 1,
    "name": "Library Scan Batch",
    "description": "2026 first library document preprocessing batch",
    "defaultPreset": "LOW_CONTRAST_SCAN",
    "status": "ACTIVE",
    "ownerId": 1,
    "ownerEmail": "owner@example.com",
    "myRole": "OWNER",
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  }
}
```

## List Projects

Query parameters follow Spring pageable conventions:

```text
GET /api/v1/projects?page=0&size=20
```

The response result is `PageResponse<ProjectResponse>`.

## Update Project

`OWNER` and `EDITOR` can update project metadata.

Request:

```json
{
  "name": "Updated Batch",
  "description": "Updated description",
  "defaultPreset": "A4_SCAN_300DPI"
}
```

## Delete Project

Only `OWNER` can delete a project. Deletion is soft delete through `ProjectStatus.DELETED`.

## Invite Member

Only `OWNER` can invite members. `OWNER` cannot be invited through this API.

Request:

```json
{
  "userId": 2,
  "role": "EDITOR"
}
```

## Project Summary

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "projectId": 1,
    "name": "Library Scan Batch",
    "memberCount": 2,
    "imageCount": 0,
    "jobCount": 0
  }
}
```

`imageCount` and `jobCount` are placeholders until Image and Job domains are implemented.
