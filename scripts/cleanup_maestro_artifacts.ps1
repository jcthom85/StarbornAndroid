param(
    [switch] $Apply,
    [int] $KeepDays = 14
)

$ErrorActionPreference = "Stop"

function Get-DirectoryStats {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return [PSCustomObject]@{
            Path = $Path
            Files = 0
            Bytes = 0L
            MB = 0
        }
    }

    $files = @(Get-ChildItem -LiteralPath $Path -Recurse -Force -File -ErrorAction SilentlyContinue)
    $bytes = ($files | Measure-Object Length -Sum).Sum
    if ($null -eq $bytes) { $bytes = 0L }

    [PSCustomObject]@{
        Path = $Path
        Files = $files.Count
        Bytes = [int64] $bytes
        MB = [math]::Round($bytes / 1MB, 2)
    }
}

function Assert-PathUnderRoot {
    param(
        [string] $Path,
        [string] $Root
    )

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    $resolvedRoot = (Resolve-Path -LiteralPath $Root).Path.TrimEnd("\") + "\"
    if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to delete outside expected root: $resolvedPath"
    }
}

function Remove-SafeDirectory {
    param(
        [string] $Path,
        [string] $Root
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }
    Assert-PathUnderRoot -Path $Path -Root $Root
    Remove-Item -LiteralPath $Path -Recurse -Force
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$maestroHome = Join-Path $repoRoot ".maestro-home"
$maestroTemp = Join-Path $maestroHome "tmp"
$maestroTests = Join-Path $maestroHome ".maestro\tests"

if (-not (Test-Path -LiteralPath $maestroHome)) {
    Write-Host "No .maestro-home directory found. Nothing to clean."
    exit 0
}

$cutoff = (Get-Date).AddDays(-1 * [math]::Max($KeepDays, 0))
$tempStats = Get-DirectoryStats -Path $maestroTemp
$oldTestDirs = @()

if (Test-Path -LiteralPath $maestroTests) {
    $oldTestDirs = @(Get-ChildItem -LiteralPath $maestroTests -Directory -Force |
        Where-Object { $_.LastWriteTime -lt $cutoff })
}

$oldTestStats = @($oldTestDirs | ForEach-Object { Get-DirectoryStats -Path $_.FullName })
$oldTestBytes = ($oldTestStats | Measure-Object Bytes -Sum).Sum
if ($null -eq $oldTestBytes) { $oldTestBytes = 0L }
$oldTestFiles = ($oldTestStats | Measure-Object Files -Sum).Sum
if ($null -eq $oldTestFiles) { $oldTestFiles = 0 }

Write-Host "Maestro cleanup summary"
Write-Host "Repo: $repoRoot"
Write-Host "Mode: $(if ($Apply) { 'APPLY' } else { 'DRY RUN' })"
Write-Host ""
Write-Host ("Temp cache: {0} file(s), {1} MB" -f $tempStats.Files, $tempStats.MB)
Write-Host ("Old test runs: {0} folder(s), {1} file(s), {2} MB; cutoff {3}" -f `
    $oldTestDirs.Count,
    $oldTestFiles,
    [math]::Round($oldTestBytes / 1MB, 2),
    $cutoff.ToString("yyyy-MM-dd HH:mm:ss"))
Write-Host ""

if (-not $Apply) {
    Write-Host "Dry run only. Re-run with -Apply to delete:"
    Write-Host "  - .maestro-home\tmp"
    Write-Host "  - .maestro-home\.maestro\tests folders older than KeepDays"
    exit 0
}

Remove-SafeDirectory -Path $maestroTemp -Root $maestroHome
New-Item -ItemType Directory -Force -Path $maestroTemp | Out-Null

foreach ($dir in $oldTestDirs) {
    Remove-SafeDirectory -Path $dir.FullName -Root $maestroTests
}

$remainingTempStats = Get-DirectoryStats -Path $maestroTemp
$remainingTestsStats = Get-DirectoryStats -Path $maestroTests

Write-Host "Cleanup complete."
Write-Host ("Remaining temp cache: {0} file(s), {1} MB" -f $remainingTempStats.Files, $remainingTempStats.MB)
Write-Host ("Remaining test history: {0} file(s), {1} MB" -f $remainingTestsStats.Files, $remainingTestsStats.MB)
