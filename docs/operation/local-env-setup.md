# Local Environment Setup

## Secret Handling Rule

Do not commit real secrets. The repository tracks only templates and documentation. Put real local values in ignored
files:

- `backend-api/.env`
- `LOCAL_CONFIG.md`
- `*.local.md`

The `.gitignore` file excludes these paths.

## Backend `.env` Template

Create `backend-api/.env` from `backend-api/.env.example`:

```powershell
Copy-Item backend-api\.env.example backend-api\.env
```

Then fill in real values:

```env
GOOGLE_CLIENT_ID=<google-oauth-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth-client-secret>
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:5173/oauth2/success

DB_URL=jdbc:postgresql://localhost:5432/image_preprocess
DB_USERNAME=postgres
DB_PASSWORD=postgres
JPA_DDL_AUTO=update

JWT_SECRET=<at-least-32-byte-secret>
ACCESS_TOKEN_EXPIRE_SECONDS=1800
REFRESH_TOKEN_EXPIRE_SECONDS=1209600
REFRESH_TOKEN_COOKIE_NAME=refresh_token
REFRESH_TOKEN_COOKIE_SECURE=false
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax

CORS_ALLOWED_ORIGINS=http://localhost:5173
RABBIT_HEALTH_ENABLED=false
```

## Google Console

Register this redirect URI for local backend execution:

```text
http://localhost:8080/login/oauth2/code/google
```

If NGINX later becomes the local entry point, add this URI too:

```text
http://localhost/login/oauth2/code/google
```

## Docker PostgreSQL

Start a local PostgreSQL container:

```powershell
docker run --name image-preprocess-postgres `
  -e POSTGRES_DB=image_preprocess `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16-alpine
```

If the container already exists:

```powershell
docker start image-preprocess-postgres
```

Stop it:

```powershell
docker stop image-preprocess-postgres
```

## Running Backend With Local Env

PowerShell example:

```powershell
Get-Content backend-api\.env | ForEach-Object {
  if ($_ -and -not $_.StartsWith("#")) {
    $name, $value = $_.Split("=", 2)
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
  }
}

Set-Location backend-api
gradle bootRun
```

With Docker Gradle image:

```powershell
$backendPath = Join-Path (Get-Location) "backend-api"
docker run --rm `
  -v "${backendPath}:/workspace" `
  -w /workspace `
  --env-file backend-api\.env `
  gradle:8.10-jdk21 `
  gradle test --no-daemon
```

When the Gradle container needs to connect to PostgreSQL running on the host, set:

```env
DB_URL=jdbc:postgresql://host.docker.internal:5432/image_preprocess
```

`RABBIT_HEALTH_ENABLED=false` keeps `/actuator/health` usable before the RabbitMQ task is implemented.
