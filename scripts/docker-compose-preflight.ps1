<#
.SYNOPSIS
Runs deployment preflight checks against the local Docker Compose stack.

.DESCRIPTION
This script verifies that the local Docker Compose stack is ready before a browser smoke test or deployment rehearsal.
It does not perform OAuth login or image preprocessing. Use local-e2e-smoke.ps1 for the authenticated image workflow.
#>
[CmdletBinding()]
param(
    [string]$ComposeDirectory = "infra/docker-compose",
    [string]$ComposeFile = "docker-compose.local.yml",
    [string]$EnvFile = "",
    [string]$NginxBaseUrl = "http://localhost",
    [string]$BackendBaseUrl = "http://localhost:8080",
    [string]$MinioBaseUrl = "http://localhost:9000",
    [string]$RabbitManagementBaseUrl = "http://localhost:15672",
    [int]$TimeoutSeconds = 20,
    [switch]$SkipDocker
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:Failures = New-Object System.Collections.Generic.List[string]

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message"
}

function Resolve-RepoRelativePath {
    param([string]$Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location).Path $Path)
}

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }

    foreach ($rawLine in Get-Content -LiteralPath $Path) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }

        $separator = $line.IndexOf("=")
        if ($separator -le 0) {
            continue
        }

        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim().Trim('"').Trim("'")
        $values[$key] = $value
    }
    return $values
}

function Get-EnvValue {
    param(
        [hashtable]$Values,
        [string]$Name,
        [string]$DefaultValue
    )

    if ($Values.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($Values[$Name])) {
        return $Values[$Name]
    }
    return $DefaultValue
}

function Invoke-PreflightCheck {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "[check] $Name"
    try {
        & $Action
        Write-Host "[ ok ] $Name"
    } catch {
        $message = "$Name failed: $($_.Exception.Message)"
        $script:Failures.Add($message)
        Write-Warning $message
    }
}

function Wait-HttpStatus {
    param(
        [string]$Name,
        [string]$Uri,
        [int]$ExpectedStatus = 200,
        [hashtable]$Headers = @{}
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = $null

    do {
        try {
            $response = Invoke-WebRequest -Uri $Uri -Headers $Headers -UseBasicParsing -TimeoutSec 8
            if ([int]$response.StatusCode -eq $ExpectedStatus) {
                return $response
            }
            $lastError = "HTTP $($response.StatusCode)"
        } catch {
            $lastError = $_.Exception.Message
        }

        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "$Name did not return HTTP $ExpectedStatus from $Uri within $TimeoutSeconds seconds. Last error: $lastError"
}

function New-BasicAuthHeader {
    param(
        [string]$Username,
        [string]$Password
    )

    $pair = "${Username}:${Password}"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
    return @{ Authorization = "Basic $encoded" }
}

function Get-ResponseText {
    param([object]$Response)

    if ($Response.Content -is [byte[]]) {
        return [Text.Encoding]::UTF8.GetString($Response.Content)
    }
    return [string]$Response.Content
}

function Assert-DockerAvailable {
    if ($SkipDocker) {
        return
    }
    if ($null -eq (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "docker command is not available."
    }
}

function Invoke-ComposeConfig {
    param(
        [string]$Directory,
        [string]$File,
        [string]$EnvFilePath
    )

    Push-Location $Directory
    try {
        $output = & docker compose -f $File --env-file $EnvFilePath config 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw ($output -join [Environment]::NewLine)
        }
    } finally {
        Pop-Location
    }
}

function Assert-ContainerState {
    param(
        [string]$Name,
        [switch]$AllowExitedSuccess
    )

    $raw = & docker inspect $Name 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Container not found: $Name"
    }

    $items = @(($raw -join [Environment]::NewLine) | ConvertFrom-Json)
    $state = $items[0].State
    if ($AllowExitedSuccess -and $state.Status -eq "exited" -and [int]$state.ExitCode -eq 0) {
        return
    }

    if ($state.Status -ne "running") {
        throw "Container $Name is $($state.Status)."
    }

    $healthProperty = $state.PSObject.Properties["Health"]
    if ($null -ne $healthProperty -and $null -ne $healthProperty.Value) {
        $health = $healthProperty.Value.Status
        if ($health -eq "unhealthy") {
            throw "Container $Name health is unhealthy."
        }
        if ($health -ne "healthy") {
            Write-Host "    health=$health"
        }
    }
}

$composeDirectoryPath = Resolve-RepoRelativePath $ComposeDirectory
if (-not (Test-Path -LiteralPath $composeDirectoryPath)) {
    throw "Compose directory does not exist: $composeDirectoryPath"
}

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $candidate = Join-Path $composeDirectoryPath ".env"
    if (Test-Path -LiteralPath $candidate) {
        $EnvFile = ".env"
    } else {
        $EnvFile = ".env.example"
    }
}

$envFilePath = Resolve-RepoRelativePath (Join-Path $ComposeDirectory $EnvFile)
if (-not (Test-Path -LiteralPath $envFilePath)) {
    throw "Env file does not exist: $envFilePath"
}

$envValues = Read-DotEnv $envFilePath
$rabbitUser = Get-EnvValue $envValues "RABBITMQ_DEFAULT_USER" "local"
$rabbitPassword = Get-EnvValue $envValues "RABBITMQ_DEFAULT_PASS" "local"
$normalQueue = "image.preprocess.normal"

Write-Step "Docker Compose preflight"
Write-Host "Compose directory: $composeDirectoryPath"
Write-Host "Env file: $envFilePath"
Write-Host "NGINX: $NginxBaseUrl"
Write-Host "Backend: $BackendBaseUrl"
Write-Host "MinIO: $MinioBaseUrl"
Write-Host "RabbitMQ management: $RabbitManagementBaseUrl"

Invoke-PreflightCheck "docker command" {
    Assert-DockerAvailable
}

if (-not $SkipDocker) {
    Invoke-PreflightCheck "docker compose config" {
        Invoke-ComposeConfig -Directory $composeDirectoryPath -File $ComposeFile -EnvFilePath $envFilePath
    }

    Invoke-PreflightCheck "containers are running" {
        Assert-ContainerState "image-preprocess-nginx"
        Assert-ContainerState "image-preprocess-frontend"
        Assert-ContainerState "image-preprocess-backend-api"
        Assert-ContainerState "image-preprocess-worker"
        Assert-ContainerState "image-preprocess-postgres"
        Assert-ContainerState "image-preprocess-rabbitmq"
        Assert-ContainerState "image-preprocess-minio"
        Assert-ContainerState "image-preprocess-minio-init" -AllowExitedSuccess
    }
}

Invoke-PreflightCheck "NGINX health route" {
    $response = Wait-HttpStatus -Name "NGINX health" -Uri "$($NginxBaseUrl.TrimEnd('/'))/health"
    $content = Get-ResponseText $response
    if ($content.Trim() -ne "ok") {
        throw "Unexpected NGINX health body: $content"
    }
}

Invoke-PreflightCheck "frontend route through NGINX" {
    $response = Wait-HttpStatus -Name "Frontend" -Uri "$($NginxBaseUrl.TrimEnd('/'))/"
    $content = Get-ResponseText $response
    if ($content -notmatch "<html") {
        throw "Frontend route did not return HTML."
    }
}

Invoke-PreflightCheck "Swagger docs through NGINX" {
    $response = Wait-HttpStatus -Name "OpenAPI docs" -Uri "$($NginxBaseUrl.TrimEnd('/'))/v3/api-docs"
    $content = Get-ResponseText $response
    if ($content -notmatch "Image Preprocess Platform API") {
        throw "OpenAPI docs response does not contain the expected API title."
    }
}

Invoke-PreflightCheck "backend actuator health direct" {
    $response = Wait-HttpStatus -Name "Backend health" -Uri "$($BackendBaseUrl.TrimEnd('/'))/actuator/health"
    $content = Get-ResponseText $response
    if ($content -notmatch '"status"') {
        throw "Backend health response does not contain status."
    }
}

Invoke-PreflightCheck "MinIO health direct" {
    Wait-HttpStatus -Name "MinIO health" -Uri "$($MinioBaseUrl.TrimEnd('/'))/minio/health/live" | Out-Null
}

Invoke-PreflightCheck "RabbitMQ management queue topology" {
    $headers = New-BasicAuthHeader -Username $rabbitUser -Password $rabbitPassword
    $queueUri = "$($RabbitManagementBaseUrl.TrimEnd('/'))/api/queues/%2F/$normalQueue"
    $response = Wait-HttpStatus -Name "RabbitMQ queue" -Uri $queueUri -Headers $headers
    $content = Get-ResponseText $response
    if ($content -notmatch $normalQueue) {
        throw "RabbitMQ queue response does not contain $normalQueue."
    }
}

if ($script:Failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Preflight failed:"
    $script:Failures | ForEach-Object { Write-Host "- $_" }
    exit 1
}

Write-Host ""
Write-Host "Preflight passed. The stack is ready for browser login or scripts/local-e2e-smoke.ps1."
