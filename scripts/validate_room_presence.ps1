param(
    [switch]$StrictDuplicates
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "app/src/main/assets"
$roomsPath = Join-Path $assets "rooms.json"
$npcsPath = Join-Path $assets "npcs.json"
$charactersPath = Join-Path $assets "characters.json"
$milestonesPath = Join-Path $assets "milestones.json"

$rooms = Get-Content $roomsPath -Raw | ConvertFrom-Json
$npcs = Get-Content $npcsPath -Raw | ConvertFrom-Json
$characters = Get-Content $charactersPath -Raw | ConvertFrom-Json
$milestones = Get-Content $milestonesPath -Raw | ConvertFrom-Json

$npcIds = @{}
foreach ($npc in $npcs) {
    if ($npc.id) { $npcIds[$npc.id] = $true }
}

$milestoneIds = @{}
foreach ($milestone in $milestones) {
    if ($milestone.id) { $milestoneIds[$milestone.id] = $true }
}

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]
$phaseMap = [ordered]@{
    "baseline" = @()
}
$storyMilestoneOrder = @(
    "ms_w1_mq01_jed_talked",
    "ms_w1_mq01_complete",
    "ms_w1_mq02_complete",
    "ms_w1_mq03_complete",
    "ms_w1_mq04_complete",
    "ms_w1_zeke_directed_to_pod",
    "ms_w1_chime_spliced",
    "ms_w1_mq05_complete",
    "ms_w2_chime_recovered",
    "ms_w2_mq01_complete",
    "ms_w2_mq02_complete",
    "ms_w2_mq03_complete",
    "ms_w2_mq04_complete",
    "ms_w2_mq05_complete",
    "ms_w3_mq11_complete",
    "ms_w3_mq12_complete",
    "ms_w3_mq13_complete",
    "ms_w3_mq14_complete",
    "ms_w3_mq15_complete",
    "ms_w4_mq16_complete",
    "ms_w4_mq17_complete",
    "ms_w4_mq18_complete",
    "ms_w4_mq19_complete",
    "ms_w4_mq20_complete",
    "ms_w5_mq21_complete",
    "ms_w5_mq22_complete",
    "ms_w5_mq23_complete",
    "ms_w5_mq24_complete",
    "ms_w5_mq25_complete",
    "ms_w6_mq26_complete",
    "ms_w6_mq27_complete",
    "ms_w6_mq28_complete",
    "ms_w6_mq29_complete",
    "ms_w6_mq30_complete",
    "ms_game_complete"
)

function Test-DebugRoom($room) {
    if ($null -eq $room -or [string]::IsNullOrWhiteSpace($room.id)) { return $false }
    $roomId = [string]$room.id
    return $roomId.StartsWith("debug_", [System.StringComparison]::OrdinalIgnoreCase) -or
        $roomId.StartsWith("test_", [System.StringComparison]::OrdinalIgnoreCase)
}

function Expand-CompletedMilestones($requirements) {
    $completed = @()
    foreach ($milestone in @($requirements) | Where-Object { $_ }) {
        $index = [Array]::IndexOf($storyMilestoneOrder, [string]$milestone)
        if ($index -ge 0) {
            for ($i = 0; $i -le $index; $i++) {
                if ($completed -notcontains $storyMilestoneOrder[$i]) {
                    $completed += $storyMilestoneOrder[$i]
                }
            }
        } elseif ($completed -notcontains $milestone) {
            $completed += $milestone
        }
    }
    return $completed
}

foreach ($room in $rooms) {
    $includeInStoryPresenceRules = -not (Test-DebugRoom $room)

    foreach ($npcId in @($room.npcs)) {
        if (-not $npcIds.ContainsKey($npcId)) {
            $warnings.Add("Room '$($room.id)' references uncataloged static actor '$npcId'.")
        }
    }

    foreach ($rule in @($room.npc_presence | Where-Object { $_ })) {
        if (-not $rule.npc) {
            $errors.Add("Room '$($room.id)' has npc_presence entry without npc.")
            continue
        }
        if (-not $npcIds.ContainsKey($rule.npc)) {
            $errors.Add("Room '$($room.id)' npc_presence references unknown NPC '$($rule.npc)'.")
        }
        foreach ($milestone in @($rule.requires_milestones) | Where-Object { $_ }) {
            if (-not $milestoneIds.ContainsKey($milestone)) {
                $warnings.Add("Room '$($room.id)' npc_presence for '$($rule.npc)' requires uncataloged milestone '$milestone'.")
            }
        }
        foreach ($milestone in @($rule.forbidden_milestones) | Where-Object { $_ }) {
            if (-not $milestoneIds.ContainsKey($milestone)) {
                $warnings.Add("Room '$($room.id)' npc_presence for '$($rule.npc)' forbids uncataloged milestone '$milestone'.")
            }
        }

        if ($includeInStoryPresenceRules) {
            $requirements = @($rule.requires_milestones) | Where-Object { $_ } | Sort-Object -Unique
            if ($requirements.Count -gt 0) {
                $key = "after:" + ($requirements -join "+")
                if (-not $phaseMap.Contains($key)) {
                    $phaseMap[$key] = Expand-CompletedMilestones $requirements
                }
            }
        }
    }
}

$allRequired = @(
    foreach ($room in $rooms) {
        if (Test-DebugRoom $room) { continue }
        foreach ($rule in @($room.npc_presence | Where-Object { $_ })) {
            foreach ($milestone in @($rule.requires_milestones) | Where-Object { $_ }) { $milestone }
        }
    }
) | Sort-Object -Unique
if ($allRequired.Count -gt 0) {
    $phaseMap["all_presence_requirements"] = Expand-CompletedMilestones $allRequired
}

foreach ($phaseName in $phaseMap.Keys) {
    $completed = @($phaseMap[$phaseName])
    $byNpc = @{}
    foreach ($room in $rooms) {
        if (Test-DebugRoom $room) { continue }

        $visible = New-Object System.Collections.Generic.List[string]
        foreach ($npcId in @($room.npcs)) {
            if ($npcId) { $visible.Add($npcId) }
        }
        foreach ($rule in @($room.npc_presence | Where-Object { $_ })) {
            if (-not $rule.npc) { continue }
            $requires = @($rule.requires_milestones) | Where-Object { $_ }
            $forbidden = @($rule.forbidden_milestones) | Where-Object { $_ }
            $hasRequirements = $true
            foreach ($milestone in $requires) {
                if ($completed -notcontains $milestone) { $hasRequirements = $false; break }
            }
            $blocked = $false
            foreach ($milestone in $forbidden) {
                if ($completed -contains $milestone) { $blocked = $true; break }
            }
            if ($hasRequirements -and -not $blocked) {
                $visible.Add($rule.npc)
            }
        }
        foreach ($npcId in @($visible | Sort-Object -Unique)) {
            if (-not $byNpc.ContainsKey($npcId)) { $byNpc[$npcId] = New-Object System.Collections.Generic.List[string] }
            $byNpc[$npcId].Add($room.id)
        }
    }
    foreach ($npcId in $byNpc.Keys) {
        $locations = @($byNpc[$npcId]) | Sort-Object -Unique
        if ($locations.Count -gt 1) {
            $warnings.Add("Phase '$phaseName': NPC '$npcId' is visible in multiple rooms: $($locations -join ', ').")
        }
    }
}

foreach ($warning in $warnings) { Write-Warning $warning }
foreach ($errorMessage in $errors) { Write-Error $errorMessage }

if ($errors.Count -gt 0 -or ($StrictDuplicates -and $warnings.Count -gt 0)) {
    exit 1
}

Write-Host "Room presence validation passed: $($rooms.Count) rooms, $($npcIds.Count) NPCs, $($warnings.Count) duplicate availability warning(s)."
