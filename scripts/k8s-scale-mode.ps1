<#
.SYNOPSIS
Switches the preprocess-worker scaling mode for KEDA comparison tests.

.DESCRIPTION
Use keda-on for queue-based autoscaling and keda-off-fixed for fixed worker replica tests.
The script intentionally changes only the ScaledObject/HPA and preprocess-worker replica count.
#>
[CmdletBinding()]
param(
    [ValidateSet("keda-on", "keda-on-min1", "hpa-cpu", "keda-off-fixed")]
    [string]$Mode,

    [int]$FixedReplicas = 1,

    [int]$HpaMinReplicas = 1,

    [int]$HpaMaxReplicas = 20,

    [int]$TargetCpuUtilization = 60,

    [string]$Namespace = "docprep-cloud",

    [string]$KubeConfig = "",

    [string]$KubeContext = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Mode -eq "keda-off-fixed" -and $FixedReplicas -lt 1) {
    throw "FixedReplicas must be 1 or greater when Mode is keda-off-fixed."
}
if ($Mode -eq "hpa-cpu" -and $HpaMinReplicas -lt 1) {
    throw "HpaMinReplicas must be 1 or greater when Mode is hpa-cpu."
}
if ($Mode -eq "hpa-cpu" -and $HpaMaxReplicas -lt $HpaMinReplicas) {
    throw "HpaMaxReplicas must be greater than or equal to HpaMinReplicas."
}
if ($Mode -eq "hpa-cpu" -and ($TargetCpuUtilization -lt 1 -or $TargetCpuUtilization -gt 100)) {
    throw "TargetCpuUtilization must be between 1 and 100."
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

function Assert-MetricsApiAvailable {
    try {
        Invoke-Kubectl @("get", "--raw", "/apis/metrics.k8s.io/v1beta1/nodes") | Out-Null
    } catch {
        throw "metrics.k8s.io API is not available. Install metrics-server before using hpa-cpu mode."
    }
}

function Wait-WorkerRollout {
    param([int]$ExpectedReplicas)

    if ($ExpectedReplicas -lt 1) {
        return
    }
    Invoke-Kubectl @("-n", $Namespace, "rollout", "status", "deployment/preprocess-worker", "--timeout=180s") | Out-Host
}

function Show-WorkerStatus {
    Write-Host ""
    Write-Host "Current worker autoscaling status"
    Invoke-Kubectl @("-n", $Namespace, "get", "deploy", "preprocess-worker") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "hpa", "keda-hpa-preprocess-worker", "--ignore-not-found") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "hpa", "preprocess-worker-cpu", "--ignore-not-found") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "get", "scaledobject", "preprocess-worker", "--ignore-not-found") | Out-Host
    $baseArgs = Get-KubectlBaseArgs
    $pods = & kubectl @baseArgs -n $Namespace get pods -l "app.kubernetes.io/name=preprocess-worker" --ignore-not-found=true 2>$null
    if ($LASTEXITCODE -eq 0) {
        $pods | Out-Host
    } else {
        Write-Host "No preprocess-worker pods are running. This is normal for KEDA minReplicaCount=0."
    }
}

if ($Mode -eq "keda-on" -or $Mode -eq "keda-on-min1") {
    $minReplicas = 0
    if ($Mode -eq "keda-on-min1") {
        $minReplicas = 1
    }
    Write-Host "Switching to KEDA ON mode."
    Write-Host "- ScaledObject enabled"
    Write-Host "- Deployment desired replicas controlled by KEDA"
    Write-Host "- minReplicaCount=$minReplicas, maxReplicaCount=20"

    Invoke-Kubectl @("-n", $Namespace, "delete", "hpa", "preprocess-worker-cpu", "--ignore-not-found=true") | Out-Host
    Invoke-Kubectl @("apply", "-f", $triggerAuthPath) | Out-Host
    Invoke-Kubectl @("apply", "-f", $scaledObjectPath) | Out-Host
    if ($minReplicas -ne 0) {
        $patchFile = New-TemporaryFile
        try {
            Set-Content `
                -LiteralPath $patchFile `
                -Value "{""spec"":{""minReplicaCount"":$minReplicas,""maxReplicaCount"":20}}" `
                -Encoding ASCII
            Invoke-Kubectl @(
                "-n", $Namespace,
                "patch", "scaledobject", "preprocess-worker",
                "--type", "merge",
                "--patch-file", $patchFile
            ) | Out-Host
        } finally {
            Remove-Item -LiteralPath $patchFile -Force -ErrorAction SilentlyContinue
        }
    }
    Invoke-Kubectl @("-n", $Namespace, "scale", "deployment/preprocess-worker", "--replicas=$minReplicas") | Out-Host
    Wait-WorkerRollout $minReplicas
    Show-WorkerStatus
    exit 0
}

if ($Mode -eq "hpa-cpu") {
    Write-Host "Switching to Kubernetes HPA CPU mode."
    Write-Host "- ScaledObject disabled"
    Write-Host "- HPA uses CPU average utilization"
    Write-Host "- minReplicas=$HpaMinReplicas, maxReplicas=$HpaMaxReplicas, targetCpu=$TargetCpuUtilization%"

    Assert-MetricsApiAvailable
    Invoke-Kubectl @("-n", $Namespace, "delete", "scaledobject", "preprocess-worker", "--ignore-not-found=true") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "delete", "hpa", "keda-hpa-preprocess-worker", "--ignore-not-found=true") | Out-Host
    Invoke-Kubectl @("-n", $Namespace, "scale", "deployment/preprocess-worker", "--replicas=$HpaMinReplicas") | Out-Host
    Wait-WorkerRollout $HpaMinReplicas

    $hpaManifest = @"
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: preprocess-worker-cpu
  namespace: $Namespace
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: preprocess-worker
  minReplicas: $HpaMinReplicas
  maxReplicas: $HpaMaxReplicas
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: $TargetCpuUtilization
"@
    $tempFile = New-TemporaryFile
    try {
        Set-Content -LiteralPath $tempFile -Value $hpaManifest -Encoding UTF8
        Invoke-Kubectl @("apply", "-f", $tempFile) | Out-Host
    } finally {
        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    }
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
