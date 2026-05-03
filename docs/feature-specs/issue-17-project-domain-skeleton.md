# Issue 17. Project domain skeleton

## Issue

- Issue: `#17`
- Title: `✨ [Feat] project 도메인 skeleton 추가`
- Branch: `feat/som/17`
- Base: `feat/som/15`

## Goal

Add the Project domain skeleton that groups uploaded document images and preprocessing jobs.

## Scope

1. `Project` entity
2. `ProjectMember` entity
3. `ProjectRole`
4. `ProjectStatus`
5. Project repositories
6. Project request/response DTOs
7. Project service skeleton
8. Project member service skeleton
9. Project permission service skeleton
10. Project controllers
11. Project API documentation
12. Entity unit tests

## Out of Scope

1. Real JWT current-user resolver
2. Full project CRUD integration test
3. Image/job count aggregation
4. Member invitation notification
5. Swagger/OpenAPI annotation work

## Completion Criteria

1. Domain-type Spring package structure is preserved.
2. Controllers do not call repositories directly.
3. Project delete is represented as soft delete.
4. `OWNER`, `EDITOR`, and `VIEWER` roles are separated.
5. Docker-based Gradle test and build pass.
