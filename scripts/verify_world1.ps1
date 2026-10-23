param(
    [switch]$SkipGradle,
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$SkipMaestro,
    [Alias('InstallDebug')][switch]$InstallPlaytest,
    [string]$Device
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'maestro-common.ps1')
$repoRoot = Split-Path -Parent $PSScriptRoot
$previousSerial = $env:ANDROID_SERIAL
$previousJava = $env:JAVA_HOME
function Invoke-Checked([scriptblock]$Command) {
    & $Command
    if ($LASTEXITCODE -ne 0) { throw "Verification failed (exit $LASTEXITCODE)." }
}
Push-Location $repoRoot
try {
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent (Get-StarbornJava))
    if ($Device) { $env:ANDROID_SERIAL = $Device }
    Invoke-Checked { node scripts/validate-maestro.cjs }
    Invoke-Checked { powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validate_maestro_selectors.ps1 -Strict }
    if (-not $SkipGradle) {
        Invoke-Checked { .\gradlew.bat :app:validateWorld1Assets :app:runAssetIntegrity --console=plain }
    }
    if (-not $SkipTests) { Invoke-Checked { .\gradlew.bat :app:testDebugUnitTest --console=plain } }
    if (-not $SkipLint) { Invoke-Checked { .\gradlew.bat :app:lintDebug --console=plain } }
    if ($InstallPlaytest) {
        $setupArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', 'scripts/setup-maestro.ps1', '-InstallPlaytest')
        if ($Device) { $setupArgs += @('-Device', $Device) }
        Invoke-Checked { powershell @setupArgs }
    }
    if (-not $SkipMaestro) {
        Invoke-Checked { powershell -NoProfile -ExecutionPolicy Bypass -File scripts/maestro.ps1 test playtests/maestro/suite_smoke.yaml }
        Invoke-Checked { powershell -NoProfile -ExecutionPolicy Bypass -File scripts/maestro.ps1 test playtests/maestro/opening_equipment_later.yaml }
    }
} finally {
    $env:ANDROID_SERIAL = $previousSerial
    $env:JAVA_HOME = $previousJava
    Pop-Location
}
