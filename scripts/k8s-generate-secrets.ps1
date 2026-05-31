<#
.SYNOPSIS
Generates local Kubernetes Secret manifests from environment variables.

.DESCRIPTION
This script reads production-like values from environment variables and writes only gitignored Kubernetes Secret
manifests under infra/k8s. It never prints secret values. Use -Apply only after checking the target kubectl context.
#>
[CmdletBinding()]
param(
    [string]$Namespace = "docprep-cloud",
    [string]$BackendSecretPath = "infra/k8s/backend-api/secret.yml",
    [string]$WorkerSecretPath = "infra/k8s/preprocess-worker/secret.yml",
    [switch]$Force,
    [switch]$Apply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-RepoPath {
    param([string]$Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    $repoRoot = Split-Path -Parent $PSScriptRoot
    return Join-Path $repoRoot $Path
}

function Get-RequiredEnv {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable is missing: $Name"
    }
    if ($value -match "CHANGE_ME") {
        throw "Environment variable still contains placeholder text: $Name"
    }
    if ($value -match "[`r`n]") {
        throw "Multiline values are not supported for Kubernetes stringData in this script: $Name"
    }
    return $value
}

function ConvertTo-YamlSingleQuoted {
    param([string]$Value)

    return "'" + $Value.Replace("'", "''") + "'"
}

function New-SecretManifest {
    param(
        [string]$Name,
        [string]$Namespace,
        [string]$AppName,
        [hashtable]$Values
    )

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("apiVersion: v1")
    $lines.Add("kind: Secret")
    $lines.Add("metadata:")
    $lines.Add("  name: $Name")
    $lines.Add("  namespace: $Namespace")
    $lines.Add("  labels:")
    $lines.Add("    app.kubernetes.io/name: $AppName")
    $lines.Add("    app.kubernetes.io/part-of: docprep-cloud")
    $lines.Add("type: Opaque")
    $lines.Add("stringData:")

    foreach ($key in ($Values.Keys | Sort-Object)) {
        $lines.Add("  ${key}: $(ConvertTo-YamlSingleQuoted $Values[$key])")
    }

    return ($lines -join [Environment]::NewLine) + [Environment]::NewLine
}

function Write-SecretFile {
    param(
        [string]$Path,
        [string]$Content
    )

    $resolvedPath = Resolve-RepoPath $Path
    $directory = Split-Path -Parent $resolvedPath
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    if ((Test-Path -LiteralPath $resolvedPath) -and -not $Force) {
        throw "Secret file already exists: $resolvedPath. Re-run with -Force to overwrite."
    }

    Set-Content -LiteralPath $resolvedPath -Value $Content -Encoding UTF8
    Write-Host "Wrote secret manifest: $Path"
}

function Invoke-KubectlApply {
    param([string]$Path)

    if ($null -eq (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        throw "kubectl command is not available."
    }

    $resolvedPath = Resolve-RepoPath $Path
    & kubectl apply -f $resolvedPath
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl apply failed: $Path"
    }
}

$backendValues = @{
    DB_USERNAME = Get-RequiredEnv "DB_USERNAME"
    DB_PASSWORD = Get-RequiredEnv "DB_PASSWORD"
    SPRING_RABBITMQ_USERNAME = Get-RequiredEnv "SPRING_RABBITMQ_USERNAME"
    SPRING_RABBITMQ_PASSWORD = Get-RequiredEnv "SPRING_RABBITMQ_PASSWORD"
    GOOGLE_CLIENT_ID = Get-RequiredEnv "GOOGLE_CLIENT_ID"
    GOOGLE_CLIENT_SECRET = Get-RequiredEnv "GOOGLE_CLIENT_SECRET"
    JWT_SECRET = Get-RequiredEnv "JWT_SECRET"
    MINIO_ACCESS_KEY = Get-RequiredEnv "MINIO_ACCESS_KEY"
    MINIO_SECRET_KEY = Get-RequiredEnv "MINIO_SECRET_KEY"
    WORKER_INTERNAL_TOKEN = Get-RequiredEnv "WORKER_INTERNAL_TOKEN"
}

$workerValues = @{
    SPRING_RABBITMQ_USERNAME = Get-RequiredEnv "SPRING_RABBITMQ_USERNAME"
    SPRING_RABBITMQ_PASSWORD = Get-RequiredEnv "SPRING_RABBITMQ_PASSWORD"
    RABBITMQ_AMQP_URI = Get-RequiredEnv "RABBITMQ_AMQP_URI"
    MINIO_ACCESS_KEY = Get-RequiredEnv "MINIO_ACCESS_KEY"
    MINIO_SECRET_KEY = Get-RequiredEnv "MINIO_SECRET_KEY"
    WORKER_INTERNAL_TOKEN = Get-RequiredEnv "WORKER_INTERNAL_TOKEN"
}

if ($backendValues.JWT_SECRET.Length -lt 32) {
    throw "JWT_SECRET must be at least 32 characters."
}

if ($backendValues.WORKER_INTERNAL_TOKEN.Length -lt 16) {
    throw "WORKER_INTERNAL_TOKEN must be at least 16 characters."
}

$backendManifest = New-SecretManifest `
    -Name "backend-api-secret" `
    -Namespace $Namespace `
    -AppName "backend-api" `
    -Values $backendValues

$workerManifest = New-SecretManifest `
    -Name "preprocess-worker-secret" `
    -Namespace $Namespace `
    -AppName "preprocess-worker" `
    -Values $workerValues

Write-SecretFile -Path $BackendSecretPath -Content $backendManifest
Write-SecretFile -Path $WorkerSecretPath -Content $workerManifest

if ($Apply) {
    Write-Host "Applying generated secrets to the current kubectl context."
    Invoke-KubectlApply -Path $BackendSecretPath
    Invoke-KubectlApply -Path $WorkerSecretPath
}

Write-Host "Secret generation completed. Generated files are gitignored and must not be committed."
