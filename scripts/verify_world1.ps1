param(
    [switch]$SkipGradle, [switch]$SkipTests, [switch]$SkipLint, [switch]$SkipMaestro,
    [Alias('InstallDebug')][switch]$InstallPlaytest, [string]$Device
)
$ErrorActionPreference = 'Stop'
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python3 -ErrorAction SilentlyContinue }
if (-not $python) { throw 'Install Python 3.10+ and add it to PATH.' }
$verifyArgs = @((Join-Path $PSScriptRoot 'maestro_runner.py'), 'verify')
if ($SkipGradle) { $verifyArgs += '--skip-gradle' }
if ($SkipTests) { $verifyArgs += '--skip-tests' }
if ($SkipLint) { $verifyArgs += '--skip-lint' }
if ($SkipMaestro) { $verifyArgs += '--skip-maestro' }
if ($InstallPlaytest) { $verifyArgs += '--install' }
if ($Device) { $verifyArgs += @('--device', $Device) }
& $python.Source @verifyArgs
exit $LASTEXITCODE
