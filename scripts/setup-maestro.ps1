param([switch]$InstallPlaytest, [string]$Device, [string]$ArchivePath)
$ErrorActionPreference = 'Stop'
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python3 -ErrorAction SilentlyContinue }
if (-not $python) { throw 'Install Python 3.10+ and add it to PATH.' }
$setupArgs = @((Join-Path $PSScriptRoot 'maestro_runner.py'), 'setup')
if ($InstallPlaytest) { $setupArgs += '--install' }
if ($Device) { $setupArgs += @('--device', $Device) }
if ($ArchivePath) { $setupArgs += @('--archive', $ArchivePath) }
& $python.Source @setupArgs
exit $LASTEXITCODE
