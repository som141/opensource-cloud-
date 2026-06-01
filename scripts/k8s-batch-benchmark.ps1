<#
.SYNOPSIS
Runs a repeatable image preprocessing batch benchmark through the public API.

.DESCRIPTION
The script creates or reuses a project, uploads image files through presigned URLs, creates a preprocessing job,
waits for completion, and writes a JSON result file. It can also sample Kubernetes Worker/HPA state while polling.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApiBaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$AccessToken,

    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [int]$MaxFiles = 500,

    [string]$ProjectName = "KEDA benchmark $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",

    [Nullable[int]]$ProjectId = $null,

    [ValidateSet("A4_SCAN_300DPI", "LOW_CONTRAST_SCAN", "RECEIPT", "NOISY_SCAN")]
    [string]$Preset = "A4_SCAN_300DPI",

    [ValidateSet("LOW", "NORMAL", "HIGH")]
    [string]$Priority = "NORMAL",

    [switch]$DebugArtifacts,

    [string]$Scenario = "manual",

    [string]$OutputDirectory = "benchmark-results",

    [int]$PollIntervalSeconds = 5,

    [int]$TimeoutMinutes = 90,

    [string]$Namespace = "docprep-cloud",

    [string]$KubeConfig = "",

    [string]$KubeContext = "",

    [switch]$NgrokSkipBrowserWarning,

    [switch]$SkipKubernetesSampling
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$supportedExtensions = @(".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tif", ".tiff")
$temporaryExtractPath = $null

function Get-ApiUrl {
    param([string]$Path)
    return "$($ApiBaseUrl.TrimEnd('/'))$Path"
}

function ConvertTo-HeaderMap {
    param($Headers)

    $result = @{}
    if ($null -eq $Headers) {
        return $result
    }

    foreach ($property in $Headers.PSObject.Properties) {
        $result[$property.Name] = [string]$property.Value
    }
    return $result
}

function Read-ErrorBody {
    param($Exception)

    if ($null -eq $Exception.Response) {
        return $Exception.Message
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        if ($null -eq $stream) {
            return $Exception.Message
        }
        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    } catch {
        return $Exception.Message
    }
}

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [string]$Path,
        $Body = $null
    )

    $headers = @{
        Authorization = "Bearer $AccessToken"
        Accept = "application/json"
    }
    if ($NgrokSkipBrowserWarning) {
        $headers["ngrok-skip-browser-warning"] = "true"
    }
    $uri = Get-ApiUrl $Path

    try {
        if ($null -ne $Body) {
            $json = $Body | ConvertTo-Json -Depth 20
            $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body $json
        } else {
            $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
        }
    } catch {
        $body = Read-ErrorBody $_.Exception
        throw "API request failed. method=$Method uri=$uri body=$body"
    }

    if ($null -ne $response.PSObject.Properties["isSuccess"] -and -not $response.isSuccess) {
        throw "API returned failure. code=$($response.code) message=$($response.message)"
    }

    if ($null -ne $response.PSObject.Properties["result"]) {
        return $response.result
    }
    return $response
}

function Invoke-UploadWithRetry {
    param(
        [string]$Uri,
        [hashtable]$Headers,
        [string]$Path,
        [string]$ContentType,
        [string]$FileName
    )

    $maxAttempts = 5
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        try {
            Invoke-WebRequest `
                -Method PUT `
                -Uri $Uri `
                -Headers $Headers `
                -InFile $Path `
                -ContentType $ContentType `
                -TimeoutSec 180 | Out-Null
            return
        } catch {
            if ($attempt -ge $maxAttempts) {
                throw "Upload failed after $maxAttempts attempts. file=$FileName error=$($_.Exception.Message)"
            }
            $sleepSeconds = [Math]::Min(30, 3 * $attempt)
            Write-Host "Upload retry $attempt/$maxAttempts for $FileName after error: $($_.Exception.Message)"
            Start-Sleep -Seconds $sleepSeconds
        }
    }
}

function Get-InputFiles {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "InputPath does not exist: $Path"
    }

    $item = Get-Item -LiteralPath $Path
    if (-not $item.PSIsContainer -and $item.Extension.ToLowerInvariant() -eq ".zip") {
        $temporaryExtractPath = Join-Path ([System.IO.Path]::GetTempPath()) "docprep-benchmark-$([Guid]::NewGuid())"
        New-Item -ItemType Directory -Path $temporaryExtractPath | Out-Null
        Expand-Archive -LiteralPath $item.FullName -DestinationPath $temporaryExtractPath -Force
        $script:temporaryExtractPath = $temporaryExtractPath
        $item = Get-Item -LiteralPath $temporaryExtractPath
    }

    if (-not $item.PSIsContainer) {
        if ($supportedExtensions -notcontains $item.Extension.ToLowerInvariant()) {
            throw "Unsupported input file extension: $($item.Extension)"
        }
        return @($item)
    }

    $files = Get-ChildItem -LiteralPath $item.FullName -Recurse -File |
        Where-Object { $supportedExtensions -contains $_.Extension.ToLowerInvariant() } |
        Sort-Object FullName |
        Select-Object -First $MaxFiles

    if ($files.Count -eq 0) {
        throw "No supported image files found under $($item.FullName)."
    }
    return @($files)
}

function Get-ContentType {
    param([string]$FileName)

    $extension = [System.IO.Path]::GetExtension($FileName).ToLowerInvariant()
    switch ($extension) {
        ".png" { return "image/png" }
        ".jpg" { return "image/jpeg" }
        ".jpeg" { return "image/jpeg" }
        ".webp" { return "image/webp" }
        ".bmp" { return "image/bmp" }
        ".tif" { return "image/tiff" }
        ".tiff" { return "image/tiff" }
        default { return "application/octet-stream" }
    }
}

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

function Invoke-KubectlJson {
    param([string[]]$Arguments)

    try {
        $baseArgs = Get-KubectlBaseArgs
        $output = & kubectl @baseArgs @Arguments 2>$null
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($output -join ""))) {
            return $null
        }
        return ($output -join [Environment]::NewLine) | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Get-OptionalPropertyValue {
    param(
        $Object,
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Get-KubernetesSample {
    param($Summary)

    if ($SkipKubernetesSampling) {
        return $null
    }
    if ([string]::IsNullOrWhiteSpace($KubeConfig) -and [string]::IsNullOrWhiteSpace($KubeContext)) {
        return $null
    }

    $deployment = Invoke-KubectlJson @("-n", $Namespace, "get", "deployment", "preprocess-worker", "-o", "json")
    $hpa = Invoke-KubectlJson @("-n", $Namespace, "get", "hpa", "keda-hpa-preprocess-worker", "-o", "json")
    $hpaName = "keda-hpa-preprocess-worker"
    if ($null -eq $hpa) {
        $hpa = Invoke-KubectlJson @("-n", $Namespace, "get", "hpa", "preprocess-worker-cpu", "-o", "json")
        $hpaName = "preprocess-worker-cpu"
    }
    $deploymentStatus = Get-OptionalPropertyValue $deployment "status"
    $hpaStatus = Get-OptionalPropertyValue $hpa "status"

    return [ordered]@{
        sampledAt = (Get-Date).ToUniversalTime().ToString("o")
        hpaName = $hpaName
        progressPercent = $Summary.progressPercent
        succeeded = $Summary.succeeded
        failed = $Summary.failed
        queued = $Summary.queued
        processing = $Summary.processing
        workerReplicas = Get-OptionalPropertyValue $deploymentStatus "replicas"
        workerReadyReplicas = Get-OptionalPropertyValue $deploymentStatus "readyReplicas"
        hpaCurrentReplicas = Get-OptionalPropertyValue $hpaStatus "currentReplicas"
        hpaDesiredReplicas = Get-OptionalPropertyValue $hpaStatus "desiredReplicas"
    }
}

function Save-Result {
    param($Result)

    if (-not (Test-Path -LiteralPath $OutputDirectory)) {
        New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
    }

    $safeScenario = ($Scenario -replace "[^A-Za-z0-9._-]", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($safeScenario)) {
        $safeScenario = "manual"
    }
    $fileCount = $Result["fileCount"]
    $fileName = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-$safeScenario-$fileCount-images.json"
    $path = Join-Path $OutputDirectory $fileName
    $Result | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $path -Encoding UTF8
    return $path
}

try {
    $startedAt = Get-Date
    $files = Get-InputFiles $InputPath
    $totalBytes = ($files | Measure-Object -Property Length -Sum).Sum

    Write-Host "Benchmark scenario: $Scenario"
    Write-Host "API base URL: $ApiBaseUrl"
    Write-Host "Input files: $($files.Count)"
    Write-Host "Input bytes: $totalBytes"

    if ($null -eq $ProjectId) {
        $project = Invoke-Api -Method POST -Path "/v1/projects" -Body @{
            name = $ProjectName
            description = "Created by scripts/k8s-batch-benchmark.ps1"
            defaultPreset = $Preset
        }
    } else {
        $project = Invoke-Api -Method GET -Path "/v1/projects/$ProjectId"
    }
    Write-Host "Project: #$($project.id) $($project.name)"

    $existingImages = Invoke-Api -Method GET -Path "/v1/projects/$($project.id)/images?size=1000"
    $existingImageIds = @{}
    foreach ($image in $existingImages.content) {
        $existingImageIds[[string]$image.id] = $true
    }

    $session = Invoke-Api -Method POST -Path "/v1/projects/$($project.id)/upload-sessions" -Body @{
        expectedFileCount = $files.Count
        expectedTotalSizeBytes = [int64]$totalBytes
    }
    Write-Host "Upload session: #$($session.id)"

    $filePayloads = @()
    foreach ($file in $files) {
        $hash = Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256
        $filePayloads += @{
            fileName = $file.Name
            contentType = Get-ContentType $file.Name
            sizeBytes = [int64]$file.Length
            checksumSha256 = $hash.Hash.ToLowerInvariant()
            path = $file.FullName
        }
    }

    $presigned = Invoke-Api -Method POST -Path "/v1/upload-sessions/$($session.id)/files/presigned-url" -Body @{
        files = $filePayloads | ForEach-Object {
            @{
                fileName = $_.fileName
                contentType = $_.contentType
                sizeBytes = $_.sizeBytes
                checksumSha256 = $_.checksumSha256
            }
        }
    }
    Write-Host "Presigned targets: $($presigned.uploadTargets.Count)"

    $uploadFileIds = @()
    for ($index = 0; $index -lt $presigned.uploadTargets.Count; $index++) {
        $target = $presigned.uploadTargets[$index]
        $payload = $filePayloads[$index]
        $headers = ConvertTo-HeaderMap $target.requiredHeaders
        foreach ($headerName in @($headers.Keys)) {
            if ($headerName -ieq "Content-Type") {
                $headers.Remove($headerName)
            }
        }
        if ($NgrokSkipBrowserWarning) {
            $headers["ngrok-skip-browser-warning"] = "true"
        }
        Write-Progress -Activity "Uploading originals" -Status $payload.fileName -PercentComplete (($index / $files.Count) * 100)
        Invoke-UploadWithRetry `
            -Uri $target.uploadUrl `
            -Headers $headers `
            -Path $payload.path `
            -ContentType $payload.contentType `
            -FileName $payload.fileName
        $uploadFileIds += $target.uploadFileId
    }
    Write-Progress -Activity "Uploading originals" -Completed

    Invoke-Api -Method POST -Path "/v1/upload-sessions/$($session.id)/complete" -Body @{
        uploadFileIds = $uploadFileIds
    } | Out-Null
    Write-Host "Upload session completed."

    $allImages = Invoke-Api -Method GET -Path "/v1/projects/$($project.id)/images?size=1000"
    $createdImages = @($allImages.content | Where-Object { -not $existingImageIds.ContainsKey([string]$_.id) } | Sort-Object id)
    if ($createdImages.Count -lt $files.Count) {
        throw "Only $($createdImages.Count)/$($files.Count) uploaded image metadata rows were found."
    }
    $createdImages = @($createdImages | Select-Object -Last $files.Count)

    $job = Invoke-Api -Method POST -Path "/v1/jobs" -Body @{
        projectId = $project.id
        imageIds = @($createdImages | ForEach-Object { $_.id })
        preset = $Preset
        presetParameters = @{ targetDpi = "300" }
        debug = [bool]$DebugArtifacts
        priority = $Priority
        outputOptions = @{
            saveProcessedImage = $true
            savePreview = $false
            saveReportJson = $false
            saveDebugArtifacts = [bool]$DebugArtifacts
        }
    }
    Write-Host "Job queued: #$($job.jobId), total=$($job.totalImages)"

    $samples = New-Object System.Collections.Generic.List[object]
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $summary = $null

    do {
        Start-Sleep -Seconds $PollIntervalSeconds
        $summary = Invoke-Api -Method GET -Path "/v1/jobs/$($job.jobId)/summary"
        $sample = Get-KubernetesSample $summary
        if ($null -ne $sample) {
            $samples.Add($sample)
        }
        $done = [int]$summary.succeeded + [int]$summary.failed
        Write-Host "Job #$($job.jobId): $($summary.progressPercent)% ($done/$($summary.total))"
        if ($done -ge [int]$summary.total) {
            break
        }
    } while ((Get-Date) -lt $deadline)

    if ($null -eq $summary) {
        throw "Job summary was not loaded."
    }
    if (([int]$summary.succeeded + [int]$summary.failed) -lt [int]$summary.total) {
        throw "Benchmark timed out after $TimeoutMinutes minutes."
    }

    $finishedAt = Get-Date
    $result = [ordered]@{
        scenario = $Scenario
        apiBaseUrl = $ApiBaseUrl
        projectId = $project.id
        jobId = $job.jobId
        preset = $Preset
        priority = $Priority
        debugArtifacts = [bool]$DebugArtifacts
        fileCount = $files.Count
        totalInputBytes = [int64]$totalBytes
        startedAt = $startedAt.ToUniversalTime().ToString("o")
        finishedAt = $finishedAt.ToUniversalTime().ToString("o")
        durationSeconds = [math]::Round(($finishedAt - $startedAt).TotalSeconds, 3)
        succeeded = $summary.succeeded
        failed = $summary.failed
        progressPercent = $summary.progressPercent
        kubernetesSamples = @($samples.ToArray())
    }

    $resultPath = Save-Result $result
    Write-Host "Benchmark result written: $resultPath"
} finally {
    if ($temporaryExtractPath -and (Test-Path -LiteralPath $temporaryExtractPath)) {
        Remove-Item -LiteralPath $temporaryExtractPath -Recurse -Force
    }
}
