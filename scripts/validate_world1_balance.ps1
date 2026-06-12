param(
    [switch]$Strict
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "app/src/main/assets"

function Read-AssetJson($name) {
    Get-Content (Join-Path $assets $name) -Raw | ConvertFrom-Json
}

function As-Array($value) {
    if ($null -eq $value) { return @() }
    if ($value -is [System.Array]) { return $value }
    return @($value)
}

function New-StringSet {
    return ,([System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal))
}

function Add-SetValue($set, $value) {
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        [void]$set.Add([string]$value)
    }
}

function Has-SetValue($set, $value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $false }
    return $set.Contains([string]$value)
}

function Has-JsonProperty($object, [string]$name) {
    if ($null -eq $object) { return $false }
    return $null -ne $object.PSObject.Properties[$name]
}

function Get-JsonProperty($object, [string]$name) {
    if (-not (Has-JsonProperty $object $name)) { return $null }
    return $object.PSObject.Properties[$name].Value
}

function Is-Blank($value) {
    return [string]::IsNullOrWhiteSpace([string]$value)
}

$hubs = Read-AssetJson "hubs.json"
$nodes = Read-AssetJson "hub_nodes.json"
$rooms = Read-AssetJson "rooms.json"
$enemies = Read-AssetJson "enemies.json"
$items = Read-AssetJson "items.json"
$skills = Read-AssetJson "skills.json"
$quests = Read-AssetJson "quests.json"
$milestones = Read-AssetJson "milestones.json"

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

$world1HubIds = New-StringSet
foreach ($hub in $hubs) {
    if ($hub.world_id -eq "world_1") { Add-SetValue $world1HubIds $hub.id }
}

$world1RoomIds = New-StringSet
foreach ($node in $nodes) {
    if ($node.world_id -eq "world_1" -or (Has-SetValue $world1HubIds $node.hub_id)) {
        foreach ($roomId in As-Array $node.rooms) { Add-SetValue $world1RoomIds $roomId }
    }
}

$itemsById = @{}
$itemIds = New-StringSet
foreach ($item in $items) {
    Add-SetValue $itemIds $item.id
    if ($item.id) { $itemsById[$item.id] = $item }
}

$skillIds = New-StringSet
$skillsById = @{}
foreach ($skill in $skills) {
    Add-SetValue $skillIds $skill.id
    if ($skill.id) { $skillsById[$skill.id] = $skill }
}

$enemiesById = @{}
foreach ($enemy in $enemies) {
    if ($enemy.id) { $enemiesById[$enemy.id] = $enemy }
}

$world1Rooms = @($rooms | Where-Object { Has-SetValue $world1RoomIds $_.id })
$world1EnemyIds = New-StringSet
foreach ($room in $world1Rooms) {
    foreach ($enemyId in As-Array $room.enemies) { Add-SetValue $world1EnemyIds $enemyId }
    foreach ($party in As-Array $room.enemy_parties) {
        foreach ($enemyId in As-Array $party) { Add-SetValue $world1EnemyIds $enemyId }
    }
}

$requiredEnemyTextFields = @("name", "combat_behavior", "combat_role", "element", "flavor", "description")
$requiredEnemyPositiveFields = @("hp", "speed")
$requiredEnemyNonNegativeFields = @("strength", "vitality", "agility", "focus", "luck", "xp_reward", "credit_reward")
$validResistanceElements = New-StringSet
foreach ($element in @("physical", "shock", "burn", "freeze", "acid", "source", "radiation")) {
    Add-SetValue $validResistanceElements $element
}
$validResistanceCodes = @(-100, -50, 0, 50, 100)

foreach ($enemyId in @($world1EnemyIds)) {
    if (-not $enemiesById.ContainsKey($enemyId)) {
        $errors.Add("World 1 encounter references missing enemy '$enemyId'.")
        continue
    }
    $enemy = $enemiesById[$enemyId]
    $isBoss = ([string]$enemy.tier).Equals("boss", [System.StringComparison]::OrdinalIgnoreCase) -or
        (As-Array $enemy.tags | ForEach-Object { [string]$_ }) -contains "boss"
    $isElite = ([string]$enemy.tier).Equals("elite", [System.StringComparison]::OrdinalIgnoreCase) -or
        (As-Array $enemy.tags | ForEach-Object { [string]$_ }) -contains "elite"

    if (Has-JsonProperty $enemy "weaknesses") {
        $errors.Add("Enemy '$enemyId' uses ignored legacy 'weaknesses'; use 'resistances' with affinity codes -100, -50, 0, 50, or 100.")
    }

    foreach ($field in $requiredEnemyTextFields) {
        if (Is-Blank (Get-JsonProperty $enemy $field)) {
            $errors.Add("Enemy '$enemyId' is missing required text field '$field'.")
        }
    }

    foreach ($field in $requiredEnemyPositiveFields) {
        $value = Get-JsonProperty $enemy $field
        if ($null -eq $value -or [double]$value -le 0) {
            $errors.Add("Enemy '$enemyId' must have positive '$field'.")
        }
    }

    foreach ($field in $requiredEnemyNonNegativeFields) {
        $value = Get-JsonProperty $enemy $field
        if ($null -eq $value -or [double]$value -lt 0) {
            $errors.Add("Enemy '$enemyId' must have non-negative '$field'.")
        }
    }

    if (Has-JsonProperty $enemy "resistances") {
        foreach ($resistance in $enemy.resistances.PSObject.Properties) {
            if (-not (Has-SetValue $validResistanceElements $resistance.Name)) {
                $errors.Add("Enemy '$enemyId' resistance '$($resistance.Name)' is not a supported affinity element.")
            }
            if ($validResistanceCodes -notcontains [int]$resistance.Value) {
                $errors.Add("Enemy '$enemyId' resistance '$($resistance.Name)' uses '$($resistance.Value)'; expected one of -100, -50, 0, 50, 100.")
            }
        }
    }

    $abilityIds = @(As-Array $enemy.abilities | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($abilityIds.Count -eq 0) {
        $errors.Add("Enemy '$enemyId' must define at least one ability.")
    }

    foreach ($abilityId in $abilityIds) {
        if (-not (Has-SetValue $skillIds $abilityId)) {
            $errors.Add("Enemy '$enemyId' references unknown ability '$abilityId'.")
        } elseif (-not ([string]$skillsById[$abilityId].type).Equals("enemy", [System.StringComparison]::OrdinalIgnoreCase)) {
            $errors.Add("Enemy '$enemyId' ability '$abilityId' is not typed as an enemy skill.")
        }
    }
    foreach ($drop in As-Array $enemy.drops) {
        if (Is-Blank $drop.id) {
            $errors.Add("Enemy '$enemyId' has a drop without an item id.")
        } elseif (-not (Has-SetValue $itemIds $drop.id)) {
            $errors.Add("Enemy '$enemyId' drops unknown item '$($drop.id)'.")
        }
        if ($null -eq $drop.chance -or $drop.chance -lt 0 -or $drop.chance -gt 1) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' has invalid chance '$($drop.chance)'.")
        }

        $hasQuantity = Has-JsonProperty $drop "quantity"
        $hasQtyMin = Has-JsonProperty $drop "qty_min"
        $hasQtyMax = Has-JsonProperty $drop "qty_max"
        if ($hasQuantity -and [int]$drop.quantity -lt 1) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' has invalid quantity '$($drop.quantity)'.")
        }
        if ($hasQtyMin -and [int]$drop.qty_min -lt 1) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' has invalid qty_min '$($drop.qty_min)'.")
        }
        if ($hasQtyMax -and [int]$drop.qty_max -lt 1) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' has invalid qty_max '$($drop.qty_max)'.")
        }
        if ($hasQtyMin -xor $hasQtyMax) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' must define qty_min and qty_max together.")
        }
        if ($hasQtyMin -and $hasQtyMax -and [int]$drop.qty_max -lt [int]$drop.qty_min) {
            $errors.Add("Enemy '$enemyId' drop '$($drop.id)' has qty_max below qty_min.")
        }
    }

    if ($isBoss) {
        if ($enemy.hp -lt 350 -or $enemy.hp -gt 1000) {
            $warnings.Add("Boss '$enemyId' HP $($enemy.hp) is outside the World 1 target range 350-1000.")
        }
        if ($enemy.xp_reward -lt 250 -or $enemy.xp_reward -gt 450) {
            $warnings.Add("Boss '$enemyId' XP reward $($enemy.xp_reward) is outside the World 1 target range 250-450.")
        }
        if ($enemy.credit_reward -lt 100 -or $enemy.credit_reward -gt 300) {
            $warnings.Add("Boss '$enemyId' credit reward $($enemy.credit_reward) is outside the World 1 target range 100-300.")
        }
    } elseif ($isElite) {
        if ($enemy.hp -lt 80 -or $enemy.hp -gt 260) {
            $warnings.Add("Elite enemy '$enemyId' HP $($enemy.hp) is outside the World 1 target range 80-260.")
        }
        if ($enemy.xp_reward -lt 70 -or $enemy.xp_reward -gt 280) {
            $warnings.Add("Elite enemy '$enemyId' XP reward $($enemy.xp_reward) is outside the World 1 target range 70-280.")
        }
        if ($enemy.credit_reward -lt 20 -or $enemy.credit_reward -gt 125) {
            $warnings.Add("Elite enemy '$enemyId' credit reward $($enemy.credit_reward) is outside the World 1 target range 20-125.")
        }
    } else {
        if ($enemy.hp -lt 20 -or $enemy.hp -gt 180) {
            $warnings.Add("Enemy '$enemyId' HP $($enemy.hp) is outside the World 1 non-boss target range 20-180.")
        }
        if ($enemy.xp_reward -lt 20 -or $enemy.xp_reward -gt 140) {
            $warnings.Add("Enemy '$enemyId' XP reward $($enemy.xp_reward) is outside the World 1 non-boss target range 20-140.")
        }
        if ($enemy.credit_reward -lt 0 -or $enemy.credit_reward -gt 75) {
            $warnings.Add("Enemy '$enemyId' credit reward $($enemy.credit_reward) is outside the World 1 non-boss target range 0-75.")
        }
    }
}

foreach ($room in $world1Rooms) {
    $parties = @()
    if ((As-Array $room.enemy_parties).Count -gt 0) {
        $parties = As-Array $room.enemy_parties
    } elseif ((As-Array $room.enemies).Count -gt 0) {
        $parties = @(@(As-Array $room.enemies))
    }

    foreach ($party in $parties) {
        $ids = @(As-Array $party | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($ids.Count -eq 0) { continue }
        $partyEnemies = @($ids | ForEach-Object { $enemiesById[[string]$_] } | Where-Object { $_ })
        $hasBoss = @($partyEnemies | Where-Object {
            ([string]$_.tier).Equals("boss", [System.StringComparison]::OrdinalIgnoreCase) -or
                ((As-Array $_.tags | ForEach-Object { [string]$_ }) -contains "boss")
        }).Count -gt 0
        $hpTotal = ($partyEnemies | Measure-Object -Property hp -Sum).Sum
        $xpTotal = ($partyEnemies | Measure-Object -Property xp_reward -Sum).Sum

        if ($ids.Count -gt 3) {
            $warnings.Add("Room '$($room.id)' encounter has $($ids.Count) enemies; World 1 target is 1-3.")
        }
        if (-not $hasBoss) {
            if ($hpTotal -gt 300) {
                $warnings.Add("Room '$($room.id)' non-boss encounter HP total $hpTotal exceeds the World 1 target cap 300.")
            }
            if ($xpTotal -gt 250) {
                $warnings.Add("Room '$($room.id)' non-boss encounter XP total $xpTotal exceeds the World 1 target cap 250.")
            }
        }
    }
}

$hydraulicMilestone = $milestones | Where-Object { $_.id -eq "ms_w1_sq03_hydraulic_kick_ready" } | Select-Object -First 1
$hydraulicUnlocks = @(As-Array $hydraulicMilestone.effects.unlock_abilities)
if ($hydraulicUnlocks -notcontains "nova_hydraulic_kick") {
    $errors.Add("Heavy Lifting milestone 'ms_w1_sq03_hydraulic_kick_ready' must unlock 'nova_hydraulic_kick'.")
}

foreach ($quest in $quests | Where-Object { $_.id -like "w1_*" }) {
    foreach ($reward in As-Array $quest.rewards) {
        if (($reward.type -eq "item" -or $reward.type -eq "items") -and -not (Has-SetValue $itemIds $reward.item_id)) {
            $errors.Add("Quest '$($quest.id)' rewards unknown item '$($reward.item_id)'.")
        }
    }
}

foreach ($warning in $warnings) { Write-Warning $warning }
foreach ($errorMessage in $errors) { Write-Error $errorMessage }

if ($errors.Count -gt 0 -or ($Strict -and $warnings.Count -gt 0)) {
    exit 1
}

Write-Host "World 1 balance validation passed: $($world1EnemyIds.Count) enemy type(s), $($world1Rooms.Count) room(s). Warnings: $($warnings.Count)."
