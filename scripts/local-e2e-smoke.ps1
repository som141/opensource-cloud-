<#
.SYNOPSIS
Runs the local Docker Compose MVP smoke flow through HTTP APIs.

.DESCRIPTION
This script creates synthetic document images, packages them into a ZIP, expands the ZIP locally to mirror the
frontend ZIP-upload behavior, uploads the extracted images through presigned URLs, creates a preprocessing Job, waits
for Worker completion, and downloads one processed image plus the processed-results ZIP.

The script needs a real user access token because the public APIs are protected by Google OAuth login.
#>
[CmdletBinding()]
param(
    [string]$AccessToken = $env:ACCESS_TOKEN,
    [string]$BaseUrl = "http://localhost/api",
    [string]$ProjectName = "Local E2E Smoke $(Get-Date -Format 'yyyyMMdd-HHmmss')",
    [string]$OutputDirectory = "out/local-e2e-smoke",
    [int]$TimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AccessToken)) {
    throw "Access token is required. Sign in at http://localhost/login, then run localStorage.getItem('doc-pipeline.access-token') in DevTools, or set ACCESS_TOKEN."
}

$script:AccessToken = $AccessToken
$script:BaseUrl = $BaseUrl.TrimEnd("/")

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message"
}

function Join-ApiUrl {
    param([string]$Path)
    if ($Path.StartsWith("/")) {
        return "$script:BaseUrl$Path"
    }
    return "$script:BaseUrl/$Path"
}

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $headers = @{
        Authorization = "Bearer $script:AccessToken"
    }
    $parameters = @{
        Method  = $Method
        Uri     = (Join-ApiUrl $Path)
        Headers = $headers
    }
    if ($Method -ne "GET") {
        $parameters.ContentType = "application/json"
        $parameters.Body = ($Body | ConvertTo-Json -Depth 20)
    }

    try {
        $response = Invoke-RestMethod @parameters
    } catch {
        throw "API $Method $Path failed: $($_.Exception.Message)"
    }

    if (-not $response.isSuccess) {
        throw "API $Method $Path returned failure: $($response.code) $($response.message)"
    }
    return $response.result
}

function New-SmokeDocumentImage {
    param(
        [string]$Path,
        [string]$Title,
        [string]$BodyText
    )

    Add-Type -AssemblyName System.Drawing
    $bitmap = New-Object System.Drawing.Bitmap 900, 1200
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $fontTitle = New-Object System.Drawing.Font([System.Drawing.FontFamily]::GenericSansSerif, 42, [System.Drawing.FontStyle]::Bold)
    $fontBody = New-Object System.Drawing.Font([System.Drawing.FontFamily]::GenericSansSerif, 26, [System.Drawing.FontStyle]::Regular)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::Black, 4)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.DrawRectangle($pen, 60, 60, 780, 1080)
        $graphics.DrawString($Title, $fontTitle, [System.Drawing.Brushes]::Black, 120, 140)
        $graphics.DrawString($BodyText, $fontBody, [System.Drawing.Brushes]::Black, 120, 240)
        $graphics.DrawLine($pen, 120, 340, 780, 340)
        $graphics.DrawString("Queue-backed preprocessing smoke input", $fontBody, [System.Drawing.Brushes]::Black, 120, 410)
        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $pen.Dispose()
        $fontBody.Dispose()
        $fontTitle.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Expand-SmokeZip {
    param(
        [string]$ZipPath,
        [string]$Destination
    )

    if (Test-Path $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Destination | Out-Null
    Expand-Archive -Path $ZipPath -DestinationPath $Destination -Force
    return @(Get-ChildItem -Path $Destination -Recurse -File | Where-Object {
        $_.Extension.ToLowerInvariant() -in @(".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tif", ".tiff")
    } | Sort-Object FullName)
}

function Get-ContentType {
    param([string]$Path)

    $extension = [System.IO.Path]::GetExtension($Path).ToLowerInvariant()
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

function Send-PresignedUpload {
    param(
        [object]$Target,
        [System.IO.FileInfo]$File
    )

    Add-Type -AssemblyName System.Net.Http
    $contentType = Get-ContentType $File.FullName
    if ($null -ne $Target.requiredHeaders -and $null -ne $Target.requiredHeaders."Content-Type") {
        $contentType = $Target.requiredHeaders."Content-Type"
    }

    $client = [System.Net.Http.HttpClient]::new()
    $content = $null
    try {
        $bytes = [System.IO.File]::ReadAllBytes($File.FullName)
        $content = [System.Net.Http.ByteArrayContent]::new($bytes)
        $content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($contentType)
        $response = $client.PutAsync([Uri]$Target.uploadUrl, $content).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            throw "Presigned upload failed for $($File.Name): HTTP $([int]$response.StatusCode) $responseBody"
        }
    } finally {
        if ($null -ne $content) {
            $content.Dispose()
        }
        $client.Dispose()
    }
}

function Save-Download {
    param(
        [string]$Url,
        [string]$Path
    )

    Invoke-WebRequest -Uri $Url -OutFile $Path
    $item = Get-Item -LiteralPath $Path
    if ($item.Length -le 0) {
        throw "Downloaded file is empty: $Path"
    }
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$runDirectory = Join-Path $OutputDirectory $runId
$sourceDirectory = Join-Path $runDirectory "source"
$extractDirectory = Join-Path $runDirectory "extracted"
$downloadDirectory = Join-Path $runDirectory "downloads"
New-Item -ItemType Directory -Path $sourceDirectory, $downloadDirectory -Force | Out-Null

Write-Step "Generating synthetic document images"
$firstImage = Join-Path $sourceDirectory "smoke-document-001.png"
$secondImage = Join-Path $sourceDirectory "smoke-document-002.png"
New-SmokeDocumentImage -Path $firstImage -Title "Smoke Document 001" -BodyText "Generated at $runId"
New-SmokeDocumentImage -Path $secondImage -Title "Smoke Document 002" -BodyText "Generated at $runId"

Write-Step "Creating and expanding ZIP input"
$zipPath = Join-Path $runDirectory "smoke-input.zip"
Compress-Archive -Path (Join-Path $sourceDirectory "*.png") -DestinationPath $zipPath -Force
$files = Expand-SmokeZip -ZipPath $zipPath -Destination $extractDirectory
if ($files.Count -eq 0) {
    throw "No image files were extracted from $zipPath"
}

Write-Step "Creating project"
$project = Invoke-Api -Method POST -Path "/v1/projects" -Body @{
    name = $ProjectName
    description = "Created by scripts/local-e2e-smoke.ps1"
    defaultPreset = "A4_SCAN_300DPI"
}

Write-Step "Creating upload session"
$expectedBytes = ($files | Measure-Object -Property Length -Sum).Sum
$session = Invoke-Api -Method POST -Path "/v1/projects/$($project.id)/upload-sessions" -Body @{
    expectedFileCount = $files.Count
    expectedTotalSizeBytes = [long]$expectedBytes
}

Write-Step "Requesting presigned upload URLs"
$fileRequests = @($files | ForEach-Object {
    @{
        fileName = $_.Name
        contentType = Get-ContentType $_.FullName
        sizeBytes = [long]$_.Length
        checksumSha256 = (Get-FileHash -Algorithm SHA256 -Path $_.FullName).Hash.ToLowerInvariant()
    }
})
$presigned = Invoke-Api -Method POST -Path "/v1/upload-sessions/$($session.id)/files/presigned-url" -Body @{
    files = $fileRequests
}
$uploadTargets = @($presigned.uploadTargets)
if ($uploadTargets.Count -ne $files.Count) {
    throw "Upload target count mismatch. expected=$($files.Count), actual=$($uploadTargets.Count)"
}

Write-Step "Uploading originals to object storage"
for ($index = 0; $index -lt $uploadTargets.Count; $index++) {
    Send-PresignedUpload -Target $uploadTargets[$index] -File $files[$index]
}

Write-Step "Completing upload session"
$uploadFileIds = @($uploadTargets | ForEach-Object { [long]$_.uploadFileId })
Invoke-Api -Method POST -Path "/v1/upload-sessions/$($session.id)/complete" -Body @{
    uploadFileIds = $uploadFileIds
} | Out-Null

Write-Step "Resolving uploaded image metadata"
$imagesPage = Invoke-Api -Method GET -Path "/v1/projects/$($project.id)/images?size=200"
$images = @($imagesPage.content | Sort-Object id)
if ($images.Count -lt $files.Count) {
    throw "Image metadata count is lower than uploaded file count. images=$($images.Count), files=$($files.Count)"
}
$imageIds = @($images | Select-Object -Last $files.Count | ForEach-Object { [long]$_.id })

Write-Step "Creating preprocessing job"
$job = Invoke-Api -Method POST -Path "/v1/jobs" -Body @{
    projectId = [long]$project.id
    imageIds = $imageIds
    preset = "A4_SCAN_300DPI"
    presetParameters = @{
        targetDpi = "300"
    }
    debug = $false
    priority = "NORMAL"
    outputOptions = @{
        saveProcessedImage = $true
        savePreview = $false
        saveReportJson = $false
        saveDebugArtifacts = $false
    }
}

Write-Step "Polling job until terminal state"
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$summary = $null
$items = @()
do {
    Start-Sleep -Seconds 2
    $summary = Invoke-Api -Method GET -Path "/v1/jobs/$($job.jobId)/summary"
    $itemsPage = Invoke-Api -Method GET -Path "/v1/jobs/$($job.jobId)/items?size=200"
    $items = @($itemsPage.content)
    Write-Host ("    progress={0}% succeeded={1} failed={2} total={3}" -f `
        [math]::Round([double]$summary.progressPercent, 1),
        $summary.succeeded,
        $summary.failed,
        $summary.total)
    $finished = ([int]$summary.succeeded + [int]$summary.failed) -ge [int]$summary.total
} while (-not $finished -and (Get-Date) -lt $deadline)

if (-not $finished) {
    throw "Timed out waiting for job #$($job.jobId)."
}
if ([int]$summary.failed -gt 0) {
    throw "Job #$($job.jobId) finished with failed items."
}

Write-Step "Downloading one processed image"
$processedItem = @($items | Where-Object { -not [string]::IsNullOrWhiteSpace($_.processedObjectKey) } | Select-Object -First 1)
if ($processedItem.Count -eq 0) {
    throw "No processed item object key was registered."
}
$processedDownload = Invoke-Api -Method GET -Path "/v1/jobs/$($job.jobId)/items/$($processedItem[0].id)/download?type=processed"
$processedOutputPath = Join-Path $downloadDirectory "processed-item-$($processedItem[0].id).png"
Save-Download -Url $processedDownload.downloadUrl -Path $processedOutputPath

Write-Step "Downloading processed ZIP"
$zipDownload = Invoke-Api -Method GET -Path "/v1/jobs/$($job.jobId)/download.zip"
$zipOutputPath = Join-Path $downloadDirectory "job-$($job.jobId)-processed-results.zip"
Save-Download -Url $zipDownload.downloadUrl -Path $zipOutputPath

$summaryPath = Join-Path $runDirectory "summary.json"
[ordered]@{
    projectId = $project.id
    jobId = $job.jobId
    uploadedImages = $files.Count
    succeeded = $summary.succeeded
    failed = $summary.failed
    processedImage = $processedOutputPath
    processedZip = $zipOutputPath
    zipObjectKey = $zipDownload.objectKey
} | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryPath -Encoding UTF8

Write-Step "Smoke flow succeeded"
Write-Host "Summary: $summaryPath"
Write-Host "Processed image: $processedOutputPath"
Write-Host "Processed ZIP: $zipOutputPath"
