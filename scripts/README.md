# scripts

This directory contains local development and operation helper scripts.

## Current Scripts

- `local-e2e-smoke.ps1`: Runs the Docker Compose local MVP flow through HTTP APIs. It creates synthetic document
  images, simulates ZIP expansion, uploads through presigned URLs, creates a preprocessing Job, waits for Worker
  completion, and downloads processed outputs.

Run from the repository root:

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
