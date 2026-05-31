<#
.SYNOPSIS
Switches the preprocess-worker scaling mode for KEDA comparison tests.

.DESCRIPTION
Use keda-on for queue-based autoscaling and keda-off-fixed for fixed worker replica tests.
The script intentionally changes only the ScaledObject/HPA and preprocess-worker replica count.
#>
[CmdletBinding()]
param(
    [ValidateSet("keda-on", "keda-off-fixed")]
    [string]$Mode,

    [int]$FixedReplicas = 1,

    [string]$Namespace = "docprep-cloud",

    [string]$KubeConfig = "",

    [string]$KubeContext = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Mode -eq "keda-off-fixed" -and $FixedReplicas -lt 1) {
    throw "FixedReplicas must be 1 or greater when Mode is keda-off-fixed."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$workerDir = Join-Path $repoRoot "infra/k8s/preprocess-worker"
$scaledObjectPath = Join-Path $workerDir "scaledobject.yml"
$triggerAuthPath = Join-Path $workerDir "triggerauthentication.yml"

function Get-KubectlBaseArgs {
    $baseArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($KubeConfig)) {
        $baseArgs += @("--kubeconfig", $KubeConfig)
    }
    if (-not [string]::IsNullOrWhiteSpace($KubeContext)) {
        $baseArgs += @("--context", $KubeContext)
    }
    return $baseArgs
}

function Invoke-Kubectl {
    param([string[]]$Arguments)

    $baseArgs = Get-KubectlBaseArgs
    $output = & kubectl @baseArgs @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($output -join [Environment]::NewLine)
    }
    return $output
}

function Show-WorkerStatus {
    Write-Host ""
    Write-Host "Current worker autoscaling status"
    Invoke-Kubectl @("-n", $Namespace, "get", "deploy", "preprocess-worker") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "hpa", "keda-hpa-preprocess-worker", "--ignore-not-found") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "scaledobject", "preprocess-worker", "--ignore-not-found") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "pods", "-l", "app.kubernetes.io/name=preprocess-worker") | Out-Host
}

if ($Mode -eq "keda-on") {
    Write-Host "Switching to KEDA ON mode."
    Write-Host "- ScaledObject enabled"
    Write-Host "- Deployment desired replicas controlled by KEDA"
    Write-Host "- minReplicaCount=0, maxReplicaCount=20"

    Invoke-Kubectl @("apply", "-f", $triggerAuthPath) | Out-Host
    Invoke-Kubectl @("apply", "-f", $scaledObjectPath) | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "scale", "deployment/preprocess-worker", "--replicas=0") | Out-Host
    Show-WorkerStatus
    exit 0
}

Write-Host "Switching to KEDA OFF fixed-worker mode."
Write-Host "- ScaledObject disabled"
Write-Host "- HPA disabled"
Write-Host "- Deployment replicas fixed to $FixedReplicas"

Invoke-Kubectl @("-n", $Namespace, "delete", "scaledobject", "preprocess-worker", "--ignore-not-found=true") | Out-Host
Invoke-Kubectl @("-n", $Namespace, "delete", "hpa", "keda-hpa-preprocess-worker", "--ignore-not-found=true") | Out-Host
Invoke-Kubectl @("-n", $Namespace, "scale", "deployment/preprocess-worker", "--replicas=$FixedReplicas") | Out-Host
Show-WorkerStatus
