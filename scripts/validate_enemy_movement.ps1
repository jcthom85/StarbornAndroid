param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $repoRoot "app\src\main\assets"
$catalog = Get-Content (Join-Path $assets "enemy_movement.json") -Raw | ConvertFrom-Json
$rooms = Get-Content (Join-Path $assets "rooms.json") -Raw | ConvertFrom-Json
$enemies = Get-Content (Join-Path $assets "enemies.json") -Raw | ConvertFrom-Json

$errors = [System.Collections.Generic.List[string]]::new()
$roomById = @{}
$rooms | ForEach-Object { $roomById[$_.id] = $_ }
$enemyIds = [System.Collections.Generic.HashSet[string]]::new([string[]]($enemies.id))
$zoneById = @{}
$catalog.zones | ForEach-Object {
    if ($zoneById.ContainsKey($_.id)) { $errors.Add("Duplicate movement zone id '$($_.id)'.") }
    $zoneById[$_.id] = $_
}

$partyIds = [System.Collections.Generic.HashSet[string]]::new()
foreach ($party in $catalog.parties) {
    if (-not $partyIds.Add([string]$party.id)) {
        $errors.Add("Duplicate movement party id '$($party.id)'.")
    }
    if (-not $zoneById.ContainsKey($party.zone_id)) {
        $errors.Add("Party '$($party.id)' references unknown zone '$($party.zone_id)'.")
        continue
    }
    $zone = $zoneById[$party.zone_id]
    $zoneRooms = @($zone.rooms)
    $protected = @($zone.protected_rooms)
    foreach ($roomId in $zoneRooms + $protected + @($party.route) + @($party.start_room)) {
        if (-not $roomById.ContainsKey($roomId)) {
            $errors.Add("Party '$($party.id)' references unknown room '$roomId'.")
        }
    }
    foreach ($roomId in $zoneRooms) {
        if ($protected -contains $roomId) {
            $errors.Add("Zone '$($zone.id)' room '$roomId' is both allowed and protected.")
        }
    }
    foreach ($roomId in @($party.route)) {
        if ($zoneRooms -notcontains $roomId) {
            $errors.Add("Party '$($party.id)' route room '$roomId' is outside zone '$($zone.id)'.")
        }
        if ($protected -contains $roomId) {
            $errors.Add("Party '$($party.id)' route enters protected room '$roomId'.")
        }
        $room = $roomById[$roomId]
        if ($null -ne $room -and $null -ne $room.blocked_directions) {
            $enemyBlocks = @($room.blocked_directions.PSObject.Properties.Value | Where-Object { $_.type -eq "enemy" })
            if ($enemyBlocks.Count -gt 0) {
                $errors.Add("Mobile party '$($party.id)' route includes enemy-blocking room '$roomId'.")
            }
        }
    }
    for ($i = 0; $i -lt @($party.route).Count - 1; $i++) {
        $from = $roomById[$party.route[$i]]
        $to = $party.route[$i + 1]
        if ($null -ne $from -and @($from.connections.PSObject.Properties.Value) -notcontains $to) {
            $errors.Add("Party '$($party.id)' route step '$($party.route[$i])' -> '$to' is not connected.")
        }
    }
    foreach ($enemyId in @($party.enemies)) {
        if (-not $enemyIds.Contains([string]$enemyId)) {
            $errors.Add("Party '$($party.id)' references unknown enemy '$enemyId'.")
        }
    }
    if (@("stationary", "patrol") -notcontains $party.behavior) {
        $errors.Add("Party '$($party.id)' uses unsupported v1 behavior '$($party.behavior)'.")
    }
    if (@("passive", "aggressive", "very_aggressive") -notcontains $party.aggression) {
        $errors.Add("Party '$($party.id)' uses invalid aggression '$($party.aggression)'.")
    }
    if ([int]$party.move_interval_seconds -lt 1 -or [int]$party.engage_delay_seconds -lt 1) {
        $errors.Add("Party '$($party.id)' movement and engagement timings must be positive.")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Enemy movement validation passed: $($catalog.zones.Count) zone(s), $($catalog.parties.Count) party definition(s)."
