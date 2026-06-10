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

& $adb @AdbArgs
exit $LASTEXITCODE
