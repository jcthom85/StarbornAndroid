param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $AdbArgs
)

$ErrorActionPreference = "Stop"
$localAppData = [Environment]::GetFolderPath("LocalApplicationData")
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $localAppData "Android\Sdk" }
$adb = Join-Path $androidHome "platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    throw "adb.exe not found at $adb. Install Android SDK Platform-Tools or set ANDROID_HOME."
}

$timeoutSeconds = 45
if ($env:STARBORN_ADB_TIMEOUT_SECONDS) {
    $timeoutSeconds = [int]$env:STARBORN_ADB_TIMEOUT_SECONDS
}

$tempBase = Join-Path ([System.IO.Path]::GetTempPath()) ("starborn-adb-{0}" -f ([System.Guid]::NewGuid().ToString("N")))
$stdoutPath = "$tempBase.out"
$stderrPath = "$tempBase.err"

try {
    $process = Start-Process `
        -FilePath $adb `
        -ArgumentList $AdbArgs `
        -NoNewWindow `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath

    if (-not $process.WaitForExit($timeoutSeconds * 1000)) {
        try {
            $process.Kill()
        } catch {
            Write-Warning "Timed out waiting for adb and failed to kill process $($process.Id): $($_.Exception.Message)"
        }
        throw "adb timed out after ${timeoutSeconds}s: adb $($AdbArgs -join ' ')"
    }

    if (Test-Path $stdoutPath) {
        Get-Content -Path $stdoutPath
    }
    if (Test-Path $stderrPath) {
        Get-Content -Path $stderrPath | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    }

    exit $process.ExitCode
} finally {
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -ErrorAction SilentlyContinue
}
