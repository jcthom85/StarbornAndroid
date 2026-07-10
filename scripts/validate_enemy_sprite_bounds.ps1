param(
    [double]$ExpectedBottomPadding = 0.02,
    [double]$Tolerance = 0.006,
    [int]$AlphaThreshold = 8
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$spriteDir = Join-Path $root "world_assets/src/main/assets/images/enemies"
$errors = [System.Collections.Generic.List[string]]::new()
$files = @(Get-ChildItem -LiteralPath $spriteDir -Filter "*_combat.png" -File)

foreach ($file in $files) {
    $bitmap = [System.Drawing.Bitmap]::new($file.FullName)
    try {
        $maxY = -1
        for ($y = $bitmap.Height - 1; $y -ge 0 -and $maxY -lt 0; $y--) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                if ($bitmap.GetPixel($x, $y).A -gt $AlphaThreshold) {
                    $maxY = $y
                    break
                }
            }
        }
        if ($maxY -lt 0) {
            $errors.Add("$($file.Name) contains no visible pixels.")
            continue
        }
        $padding = ($bitmap.Height - 1 - $maxY) / [double]$bitmap.Height
        if ([Math]::Abs($padding - $ExpectedBottomPadding) -gt $Tolerance) {
            $errors.Add("$($file.Name) bottom alpha padding is $([Math]::Round($padding * 100, 2))%; expected $([Math]::Round($ExpectedBottomPadding * 100, 2))%.")
        }
    } finally {
        $bitmap.Dispose()
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Enemy sprite bounds validation passed: $($files.Count) sprites share the ground baseline."
