param(
    [double]$ExpectedBottomPadding = 0.02,
    [double]$Tolerance = 0.006,
    [int]$AlphaThreshold = 8
)
$ErrorActionPreference = 'Stop'
$python = Get-Command python3 -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
if (-not $python) { throw 'Install Python 3.10+ to validate PNG alpha bounds.' }
& $python.Source (Join-Path $PSScriptRoot 'validate_enemy_sprite_bounds.py') --expected-bottom-padding $ExpectedBottomPadding --tolerance $Tolerance --alpha-threshold $AlphaThreshold
exit $LASTEXITCODE
