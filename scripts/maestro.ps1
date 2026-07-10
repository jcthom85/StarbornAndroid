param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MaestroArgs
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$maestroCli = Join-Path $repoRoot "tools\maestro\cli-2.6.0\maestro\bin\maestro.bat"

if (-not (Test-Path $maestroCli)) {
    throw "Maestro CLI is not installed at $maestroCli. Reinstall with docs/testing/maestro.md."
}

$realLocalAppData = [Environment]::GetFolderPath("LocalApplicationData")
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $realLocalAppData "Android\Sdk" }
$adbPath = Join-Path $androidHome "platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    throw "Android platform-tools not found at $adbPath. Install Android SDK platform-tools or set ANDROID_HOME."
}

if (-not $env:JAVA_HOME) {
    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path (Join-Path $androidStudioJbr "bin\java.exe")) {
        $env:JAVA_HOME = $androidStudioJbr
    }
}
if ($env:JAVA_HOME) {
    $env:PATH = "$(Join-Path $env:JAVA_HOME 'bin');$env:PATH"
}

$maestroHome = Join-Path $repoRoot ".maestro-home"
$maestroLocalAppData = Join-Path $maestroHome "AppData\Local"
$maestroRoaming = Join-Path $maestroHome "AppData\Roaming"
$maestroTmpRoot = Join-Path $maestroHome "tmp"
$runTemp = Join-Path $maestroTmpRoot ([System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path `
    (Join-Path $maestroHome ".maestro"), `
    (Join-Path $maestroLocalAppData "mobile_dev\maestro\Logs"), `
    $maestroRoaming, `
    $runTemp | Out-Null

$env:MAESTRO_CLI_NO_ANALYTICS = "true"
$env:MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED = "true"
$env:MAESTRO_DISABLE_UPDATE_CHECK = "true"
$env:ANDROID_HOME = $androidHome
$env:USERPROFILE = $maestroHome
$env:HOME = $maestroHome
$env:LOCALAPPDATA = $maestroLocalAppData
$env:APPDATA = $maestroRoaming
$env:TEMP = $runTemp
$env:TMP = $runTemp
$env:JAVA_OPTS = (($env:JAVA_OPTS, "-Duser.home=$maestroHome") | Where-Object { $_ } | Select-Object -Unique) -join " "
$env:PATH = "$androidHome\platform-tools;" + (Split-Path -Parent $maestroCli) + ";$env:PATH"

$tempBase = Join-Path $runTemp "maestro"
$stdoutPath = "$tempBase.out"
$stderrPath = "$tempBase.err"

try {
    $process = Start-Process `
        -FilePath $maestroCli `
        -ArgumentList $MaestroArgs `
        -WorkingDirectory $repoRoot `
        -NoNewWindow `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath

    $process.WaitForExit()

    $stdout = if (Test-Path $stdoutPath) { @(Get-Content -Path $stdoutPath) } else { @() }
    $stderr = if (Test-Path $stderrPath) { @(Get-Content -Path $stderrPath) } else { @() }
    $stdout
    $stderr

    $combinedOutput = ($stdout + $stderr) -join "`n"
    $fatalOutputPatterns = @(
        "There is not enough space on the disk",
        "Exception in thread `"main`"",
        "java.lang.UnsatisfiedLinkError",
        "... FAILED",
        "[Failed]",
        "Flow failed",
        "Element not found",
        "Failed to find",
        "Assertion is false",
        "No device found",
        "is not connected"
    )
    $hasFatalOutput = $fatalOutputPatterns | Where-Object { $combinedOutput.Contains($_) }
    $hasCompletedCommand = $combinedOutput.Contains("... COMPLETED")
    $hasPassedSuite = $combinedOutput -match "\d+/\d+ Flows Passed"
    $isInformationalCommand = $MaestroArgs -contains "--version" -or
        $MaestroArgs -contains "-v" -or
        $MaestroArgs -contains "--help" -or
        $MaestroArgs -contains "-h"

    if ($hasFatalOutput -or (-not $isInformationalCommand -and -not $hasCompletedCommand -and -not $hasPassedSuite)) {
        exit 1
    }
    exit 0
} finally {
    Remove-Item -LiteralPath $runTemp -Recurse -Force -ErrorAction SilentlyContinue
}
