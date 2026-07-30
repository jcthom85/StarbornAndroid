# Checks Maestro flow selectors that quote authored game content against the
# assets those strings came from.
#
# The Maestro suite rotted badly through mid-2026: a canon migration rewrote
# dialogue and room prose, and several flows kept asserting the old wording.
# Nothing linked the two, so the breakage was invisible until a device run.
# This validator closes that gap the same way validate_world1_content.ps1's
# -StrictInlineActions links room actions to room descriptions.
#
# It deliberately only inspects selectors that look like authored prose --
# long, sentence-like strings -- because UI chrome ("CONTINUE", "MENU") lives in
# Kotlin, not in the asset JSON, and would produce nothing but false positives.
#
# Exit code 1 on findings when -Strict is passed; otherwise warnings only.

param(
    [switch]$Strict
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "app/src/main/assets"
$flowDir = Join-Path $root "playtests/maestro"

function Read-AssetText($name) {
    $path = Join-Path $assets $name
    if (-not (Test-Path $path)) { return "" }
    return (Get-Content $path -Raw)
}

# One haystack of every string the flows could legitimately quote: authored
# content from the assets, plus UI chrome (labels, contentDescriptions) which
# lives in Kotlin. A selector missing from BOTH is the real signal -- copy that
# was rewritten or removed and left a flow behind.
$sourceText = Get-ChildItem -Path (Join-Path $root "app/src/main/java") -Filter *.kt -Recurse |
    ForEach-Object { Get-Content $_.FullName -Raw }

$haystack = @(
    Read-AssetText "dialogue.json"
    Read-AssetText "rooms.json"
    Read-AssetText "events.json"
    Read-AssetText "quests.json"
    Read-AssetText "cinematics.json"
    Read-AssetText "items.json"
    Read-AssetText "tutorial_scripts.json"
    Read-AssetText "npcs.json"
    Read-AssetText "milestones.json"
    Read-AssetText "shops.json"
    Read-AssetText "hub_node_descriptions.json"
    Read-AssetText "hubs.json"
    Read-AssetText "hub_nodes.json"
    Read-AssetText "skills.json"
    Read-AssetText "enemies.json"
    Read-AssetText "statuses.json"
    ($sourceText -join "`n")
) -join "`n"

# JSON escapes apostrophes/quotes; normalise so "Loader's idle" matches.
$normalizedHaystack = $haystack -replace '\\"', '"' -replace '\\u0027', "'"

$findings = New-Object System.Collections.Generic.List[string]
$checked = 0

foreach ($flow in Get-ChildItem -Path $flowDir -Filter *.yaml) {
    $lines = Get-Content $flow.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match '^\s*#') { continue }

        # Only selector-bearing directives. notVisible/assertNotVisible are
        # excluded on purpose: asserting the absence of a string that no longer
        # exists anywhere is legitimate (and often the point of the assertion).
        if ($line -match '(notVisible:|assertNotVisible:)') { continue }
        if ($line -notmatch '(visible:|tapOn:|assertVisible:)\s*"(.+)"\s*$') { continue }
        $selector = $Matches[2]

        # Strip regex affixes the flows use for partial matching.
        $probe = $selector -replace '^\.\*', '' -replace '\.\*$', ''

        # Heuristic: authored prose only. Skip short labels and UI chrome.
        if ($probe.Length -lt 25) { continue }
        if ($probe -notmatch '\s') { continue }
        if ($probe -cmatch '^[A-Z0-9 ,&:''-]+$') { continue }   # ALL-CAPS headers
        # Interior regex wildcards mean the literal never appears contiguously.
        if ($probe -match '\.\*') { continue }

        # Labels the UI composes at runtime ("Item acquired: $item") never exist
        # as a whole literal in either source or assets; only the prefix does.
        $composed = $false
        foreach ($prefix in @(
            "Item acquired:", "Acquired ", "Craft ", "Dismiss ", "Engage ",
            "Save Slot", "Load Slot", "Details for ", "Travel ", "Enter "
        )) {
            if ($probe.StartsWith($prefix)) { $composed = $true; break }
        }
        if ($composed) { continue }

        $checked++
        if ($normalizedHaystack.Contains($probe)) { continue }

        $rel = $flow.Name
        $findings.Add("$rel line $($i + 1): selector text not found in any authored asset -- `"$probe`"")
    }
}

foreach ($finding in $findings) { Write-Warning $finding }

if ($findings.Count -gt 0) {
    Write-Host ""
    Write-Host "Maestro selector validation: $($findings.Count) selector(s) quote prose that no longer exists in the assets."
    Write-Host "Either the copy was rewritten (update the flow) or the selector was always wrong."
    if ($Strict) { exit 1 }
    exit 0
}

Write-Host "Maestro selector validation passed: $checked prose selector(s) matched authored content."
