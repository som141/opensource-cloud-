# Issue 27. Swagger OpenAPI And JWT Bearer

## Issue

- Issue: `#27`
- Title: `Swagger/OpenAPI documentation and JWT Bearer configuration`

## Goal

Enable local Swagger UI so Auth and Project APIs can be manually tested before the frontend is complete.

## Work Order

1. Add `springdoc-openapi-starter-webmvc-ui`.
2. Add OpenAPI metadata and local server config.
3. Add JWT Bearer security scheme.
4. Add Swagger UI and API docs paths.
5. Tag Auth and Project controllers.
6. Add OpenAPI configuration test.
7. Add Swagger usage documentation.
8. Verify test, build, `/v3/api-docs`, and Swagger UI.

## Functional Scope

- Swagger UI is available at `/swagger-ui/index.html`.
- OpenAPI JSON is available at `/v3/api-docs`.
- Swagger UI exposes JWT Bearer authorization.
- Auth and Project APIs are grouped with tags.

## Out Of Scope

- Full operation-level success/failure examples for every endpoint.
- Generated client SDK.
- Swagger behind NGINX.
- Production auth policy for Swagger.

## Verification

- Backend tests pass.
- Backend build passes.
- Local Docker boot returns `UP` from `/actuator/health`.
- `/v3/api-docs` returns OpenAPI JSON with `bearerAuth`.
- Swagger UI HTML is reachable.
