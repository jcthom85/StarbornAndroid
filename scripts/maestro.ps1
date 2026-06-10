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

$maestroHome = Join-Path $repoRoot ".maestro-home"
$maestroLocalAppData = Join-Path $maestroHome "AppData\Local"
$maestroRoaming = Join-Path $maestroHome "AppData\Roaming"
New-Item -ItemType Directory -Force -Path `
    (Join-Path $maestroHome ".maestro"), `
    (Join-Path $maestroLocalAppData "mobile_dev\maestro\Logs"), `
    $maestroRoaming | Out-Null

$env:MAESTRO_CLI_NO_ANALYTICS = "true"
$env:MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED = "true"
$env:MAESTRO_DISABLE_UPDATE_CHECK = "true"
$env:ANDROID_HOME = $androidHome
$env:USERPROFILE = $maestroHome
$env:HOME = $maestroHome
$env:LOCALAPPDATA = $maestroLocalAppData
$env:APPDATA = $maestroRoaming
$env:JAVA_OPTS = (($env:JAVA_OPTS, "-Duser.home=$maestroHome") | Where-Object { $_ } | Select-Object -Unique) -join " "
$env:PATH = "$androidHome\platform-tools;" + (Split-Path -Parent $maestroCli) + ";$env:PATH"

& $maestroCli @MaestroArgs
exit $LASTEXITCODE
