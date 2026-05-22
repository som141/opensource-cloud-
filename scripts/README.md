# scripts

This directory contains local development and operation helper scripts.

## Current Scripts

- `local-e2e-smoke.ps1`: Runs the Docker Compose local MVP flow through HTTP APIs. It creates synthetic document
  images, simulates ZIP expansion, uploads through presigned URLs, creates a preprocessing Job, waits for Worker
  completion, and downloads processed outputs.
- `docker-compose-preflight.ps1`: Checks Docker Compose config, container states, NGINX routing, backend health,
  Swagger/OpenAPI, MinIO health, and RabbitMQ queue topology before running browser or E2E tests.

Run from the repository root:

```powershell
.\scripts\docker-compose-preflight.ps1
```

Then run the authenticated E2E smoke flow:

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

To get the token, sign in through `http://localhost/login` and read this value in browser DevTools:

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

## Planned Scripts

- `local-up.sh`
- `local-down.sh`
- `seed-dev-data.sh`
- `clean-object-storage.sh`
