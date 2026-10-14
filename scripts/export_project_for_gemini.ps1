# Script to export the entire Starborn codebase and documentation into a clean ZIP for Gemini
$ErrorActionPreference = "Stop"

$includeDirs = @('app', 'desktopApp', 'gameRuntime', 'shared', 'scripts', 'tools', 'docs', 'starborn-observatory', 'data')
$allowedExts = @('.kt', '.kts', '.md', '.py', '.ps1', '.json', '.xml', '.html', '.toml', '.gradle')
$rootDir = Split-Path -Parent $PSScriptRoot
$zipTarget = Join-Path $rootDir "starborn_source_clean.zip"
$tempDir = Join-Path $rootDir "tmp_export"

Write-Host "Scanning Starborn project..." -ForegroundColor Cyan

$coreFiles = Get-ChildItem -Path (Join-Path $rootDir $includeDirs) -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
    $_.FullName -notmatch '\\(build|\.gradle|\.idea|play_assets|backups|__pycache__)\\' -and
    $allowedExts -contains $_.Extension.ToLower()
}

$rootFiles = Get-ChildItem -Path $rootDir -File | Where-Object { $allowedExts -contains $_.Extension.ToLower() }
$allFiles = $coreFiles + $rootFiles

Write-Host "Found $($allFiles.Count) files. Staging clean export..." -ForegroundColor Green

if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path $tempDir | Out-Null

foreach ($f in $allFiles) {
    $rel = $f.FullName.Substring($rootDir.Length + 1)
    $dest = Join-Path $tempDir $rel
    $destParent = Split-Path $dest
    if (-not (Test-Path $destParent)) { New-Item -ItemType Directory -Path $destParent -Force | Out-Null }
    Copy-Item $f.FullName $dest
}

if (Test-Path $zipTarget) { Remove-Item -Force $zipTarget }

Write-Host "Compressing to $zipTarget..." -ForegroundColor Cyan
Compress-Archive -Path "$tempDir\*" -DestinationPath $zipTarget -CompressionLevel Optimal
Remove-Item -Recurse -Force $tempDir

$sizeMB = [math]::Round(((Get-Item $zipTarget).Length / 1MB), 2)
Write-Host "Done! Export created at $zipTarget ($sizeMB MB)" -ForegroundColor Green
