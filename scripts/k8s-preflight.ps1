<#
.SYNOPSIS
Checks Kubernetes prerequisites before running the Deploy Kubernetes workflow.

.DESCRIPTION
This script performs read-only checks against the current kubectl context. It does not create or update resources.
#>
[CmdletBinding()]
param(
    [string]$Namespace = "docprep-cloud",
    [string]$TlsSecretName = "docprep-cloud-tls",
    [string]$IngressClassName = "nginx",
    [string]$KubeContext = "",
    [switch]$SkipKeda,
    [switch]$SkipTls,
    [switch]$SkipApplicationSecrets,
    [switch]$RequireGhcrPullSecret
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:Failures = New-Object System.Collections.Generic.List[string]

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

function Invoke-Kubectl {
    param([string[]]$Arguments)

    $output = & kubectl @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($output -join [Environment]::NewLine)
    }
    return $output
}

Write-Host "Kubernetes preflight"
Write-Host "Namespace: $Namespace"
Write-Host "TLS secret: $TlsSecretName"
Write-Host "IngressClass: $IngressClassName"

Invoke-PreflightCheck "kubectl command" {
    if ($null -eq (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        throw "kubectl command is not available."
    }
}

if (-not [string]::IsNullOrWhiteSpace($KubeContext)) {
    Invoke-PreflightCheck "select kubectl context" {
        Invoke-Kubectl @("config", "use-context", $KubeContext) | Out-Null
    }
}

Invoke-PreflightCheck "current kubectl context" {
    Invoke-Kubectl @("config", "current-context") | Out-Host
}

Invoke-PreflightCheck "cluster reachable" {
    Invoke-Kubectl @("cluster-info") | Out-Null
}

Invoke-PreflightCheck "namespace exists" {
    Invoke-Kubectl @("get", "namespace", $Namespace) | Out-Null
}

if (-not $SkipApplicationSecrets) {
    Invoke-PreflightCheck "backend-api-secret exists" {
        Invoke-Kubectl @("-n", $Namespace, "get", "secret", "backend-api-secret") | Out-Null
    }

    Invoke-PreflightCheck "preprocess-worker-secret exists" {
        Invoke-Kubectl @("-n", $Namespace, "get", "secret", "preprocess-worker-secret") | Out-Null
    }
}

if (-not $SkipTls) {
    Invoke-PreflightCheck "TLS secret exists" {
        Invoke-Kubectl @("-n", $Namespace, "get", "secret", $TlsSecretName) | Out-Null
    }
}

if (-not $SkipKeda) {
    Invoke-PreflightCheck "KEDA ScaledObject CRD exists" {
        Invoke-Kubectl @("get", "crd", "scaledobjects.keda.sh") | Out-Null
    }

    Invoke-PreflightCheck "KEDA TriggerAuthentication CRD exists" {
        Invoke-Kubectl @("get", "crd", "triggerauthentications.keda.sh") | Out-Null
    }
}

Invoke-PreflightCheck "IngressClass exists" {
    Invoke-Kubectl @("get", "ingressclass", $IngressClassName) | Out-Null
}

if ($RequireGhcrPullSecret) {
    Invoke-PreflightCheck "GHCR image pull secret exists" {
        Invoke-Kubectl @("-n", $Namespace, "get", "secret", "ghcr-pull-secret") | Out-Null
    }
}

if ($script:Failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Kubernetes preflight failed:"
    $script:Failures | ForEach-Object { Write-Host "- $_" }
    exit 1
}

Write-Host ""
Write-Host "Kubernetes preflight passed. The cluster is ready for manifest dry-run or apply."
