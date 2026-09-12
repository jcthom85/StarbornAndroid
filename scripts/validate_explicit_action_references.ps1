param([switch]$Strict)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'explicit_action_references.ps1')
$rooms = Get-Content (Join-Path $PSScriptRoot '../app/src/main/assets/rooms.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$errorsFound = @()
$count = 0
foreach ($room in $rooms) {
    foreach ($text in @($room.description, $room.description_dark) + @($room.description_variants | ForEach-Object { $_.description })) {
        if (-not $text) { continue }
        $errorsFound += @(Get-ExplicitReferenceErrors $text $room.actions | ForEach-Object { "$($room.id): $_" })
        foreach ($match in [regex]::Matches($text, '\[action:([^\]]+)]', 'IgnoreCase')) {
            $count++
        }
    }
}
$errorsFound | ForEach-Object { Write-Warning $_ }
Write-Output "Validated $count explicit action references; $($errorsFound.Count) errors."
if ($Strict -and $errorsFound.Count) { exit 1 }
