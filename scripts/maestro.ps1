param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MaestroArgs)
$ErrorActionPreference = 'Stop'
$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python3 -ErrorAction SilentlyContinue }
if (-not $python) { throw 'Install Python 3.10+ and add it to PATH.' }
& $python.Source (Join-Path $PSScriptRoot 'maestro_runner.py') run @MaestroArgs
exit $LASTEXITCODE
