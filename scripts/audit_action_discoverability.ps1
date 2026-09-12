param([switch]$Strict, [switch]$SummaryOnly)

# Static candidate audit, not a route/state reachability proof. No asset writes.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'explicit_action_references.ps1')
$assetRoot = Join-Path (Split-Path -Parent $PSScriptRoot) 'app/src/main/assets'
$rooms = Get-Content (Join-Path $assetRoot 'rooms.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$nodes = Get-Content (Join-Path $assetRoot 'hub_nodes.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$hubs = Get-Content (Join-Path $assetRoot 'hubs.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$hubWorlds = @{}
foreach ($hub in $hubs) { $hubWorlds[[string]$hub.id] = [string]$hub.world_id }
$roomWorlds = @{}
foreach ($node in $nodes) {
    $world = [string]$node.world_id
    if (-not $world) { $world = $hubWorlds[[string]$node.hub_id] }
    foreach ($roomId in @($node.rooms)) { $roomWorlds[[string]$roomId] = $world }
}

function Has-ActionName([string]$label, [string]$description) {
    if (-not $label -or -not $description) { return $false }
    if (Test-ExplicitActionReference $description $label) { return $true }
    # Marker display spans are reserved, even for unknown/hidden targets.
    $description = [regex]::Replace($description, '\[(npc|action):([^\]]+)]', '', 'IgnoreCase')
    # Mirrors the runtime's literal variants; NPC markers may claim overlapping
    # text first, so even a match here does not establish a usable hit target.
    foreach ($candidate in @($label, $label.Replace('_', ' '), $label.Replace('-', ' '), $label.Replace([string][char]0x2019, "'"))) {
        if ($description.IndexOf($candidate, [StringComparison]::OrdinalIgnoreCase) -ge 0) { return $true }
    }
    return $false
}

function Variant-DisablesAction($variant, $action) {
    if ($action.requires_milestone_not_set -and @($variant.requires_milestones) -contains $action.requires_milestone_not_set) { return $true }
    foreach ($required in @($action.requires_milestones) + @($action.requires_milestone)) {
        if ($required -and @($variant.forbidden_milestones) -contains $required) { return $true }
    }
    return $false
}

$findings = [Collections.Generic.List[object]]::new()
$actionCount = 0
foreach ($room in $rooms) {
    $world = $roomWorlds[[string]$room.id]
    if (-not $world) { $world = 'unmapped' }
    foreach ($action in @($room.actions)) {
        if ($null -eq $action) { continue }
        $type = ([string]$action.type).ToLowerInvariant()
        # Service actions have separate controls in the exploration UI.
        if ($type -in @('shop','tinkering','firstaid','first_aid','rest_stop','reststop','rest','fish','fishing') -or $type.StartsWith('arcade')) { continue }
        $actionCount++
        $descriptions = @([pscustomobject]@{ Name='base'; Text=$room.description; Gate=$null })
        if ($room.description_dark) { $descriptions += [pscustomobject]@{ Name='dark'; Text=$room.description_dark; Gate=$null } }
        $variantIndex = 0
        foreach ($variant in @($room.description_variants)) {
            if ($null -eq $variant) { continue }
            $descriptions += [pscustomobject]@{ Name="variant[$variantIndex]"; Text=$variant.description; Gate=$variant }
            $variantIndex++
        }
        foreach ($description in $descriptions) {
            if (-not $description.Text) { continue }
            if (Has-ActionName ([string]$action.name) ([string]$description.Text)) { continue }
            if ($description.Gate -and (Variant-DisablesAction $description.Gate $action)) { continue }
            $findings.Add([pscustomobject]@{ World=$world; Room=$room.id; Action=$action.name; Description=$description.Name })
        }
    }
}
Write-Output "Audited $($rooms.Count) rooms, $actionCount inline actions; $($findings.Count) missing-name candidates."
$findings | Group-Object World | Sort-Object Name | ForEach-Object { Write-Output "$($_.Name): $($_.Count) candidates" }
if (-not $SummaryOnly) { $findings | Sort-Object World,Room,Action,Description | Format-Table -AutoSize }
if ($Strict -and $findings.Count -gt 0) { exit 1 }
