param(
    [double]$GroundLine = 0.98,
    [int]$AlphaThreshold = 8
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$spriteDir = Join-Path $root "world_assets/src/main/assets/images/enemies"

function Get-AlphaBounds([System.Drawing.Bitmap]$bitmap, [int]$threshold) {
    $minX = $bitmap.Width
    $minY = $bitmap.Height
    $maxX = -1
    $maxY = -1
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            if ($bitmap.GetPixel($x, $y).A -gt $threshold) {
                $minX = [Math]::Min($minX, $x)
                $minY = [Math]::Min($minY, $y)
                $maxX = [Math]::Max($maxX, $x)
                $maxY = [Math]::Max($maxY, $y)
            }
        }
    }
    if ($maxX -lt 0) { return $null }
    return [System.Drawing.Rectangle]::FromLTRB($minX, $minY, $maxX + 1, $maxY + 1)
}

foreach ($file in Get-ChildItem -LiteralPath $spriteDir -Filter "*_combat.png" -File) {
    $stream = [System.IO.MemoryStream]::new([System.IO.File]::ReadAllBytes($file.FullName))
    $source = [System.Drawing.Bitmap]::new($stream)
    try {
        $bounds = Get-AlphaBounds $source $AlphaThreshold
        if ($null -eq $bounds) { throw "$($file.Name) contains no visible pixels." }

        $targetBottom = [Math]::Min($source.Height - 1, [int][Math]::Round(($source.Height - 1) * $GroundLine))
        $targetLeft = [int][Math]::Round(($source.Width - $bounds.Width) / 2.0)
        $targetTop = $targetBottom - $bounds.Height + 1
        if ($targetTop -lt 0) { throw "$($file.Name) cannot fit on the normalized canvas." }

        $output = [System.Drawing.Bitmap]::new(
            $source.Width,
            $source.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($output)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImage(
                    $source,
                    [System.Drawing.Rectangle]::new($targetLeft, $targetTop, $bounds.Width, $bounds.Height),
                    $bounds,
                    [System.Drawing.GraphicsUnit]::Pixel
                )
            } finally {
                $graphics.Dispose()
            }
            $tempPath = "$($file.FullName).normalized.png"
            $output.Save($tempPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Copy-Item -LiteralPath $tempPath -Destination $file.FullName -Force
            Remove-Item -LiteralPath $tempPath -Force
        } finally {
            $output.Dispose()
        }
    } finally {
        $source.Dispose()
        $stream.Dispose()
    }
}

Write-Host "Normalized $(@(Get-ChildItem -LiteralPath $spriteDir -Filter '*_combat.png' -File).Count) enemy sprites to ground line $GroundLine."
