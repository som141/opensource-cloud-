# Issue 25. Project Domain CRUD And Permission API

## Issue

- Issue: `#25`
- Title: `Project domain CRUD and permission validation`

## Goal

Implement the Project domain as the authorization boundary for future image, upload, and job APIs. A project has one
owner, multiple members, role-based permissions, soft deletion, and a summary endpoint.

## Work Order

1. Enable JPA auditing for `BaseEntity` timestamps.
2. Add `Project`, `ProjectMember`, `ProjectRole`, and `ProjectStatus`.
3. Add project repositories.
4. Add project DTOs.
5. Add project CRUD services.
6. Add project member services.
7. Add `ProjectPermissionService`.
8. Add project and member controllers.
9. Add Project API documentation.
10. Add unit tests.

## Functional Scope

- `OWNER` can read, update, manage members, and soft delete.
- `EDITOR` can read and update.
- `VIEWER` can read only.
- Project deletion uses `ProjectStatus.DELETED`.
- Member removal deletes the membership row.
- Project summary returns member count now and reserves image/job counts for later domains.

## Out Of Scope

- Project ownership transfer.
- Email invitation.
- Image and Job count integration.
- Team invitation by email before account creation.
- Audit logging.

## Verification

- Entity tests cover create/update/delete.
- Permission tests cover editor/viewer/member missing cases.
- Service tests cover create/update/invite/remove flows.
- Backend `test` and `build` must pass.
