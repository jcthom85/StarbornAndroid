param(
    [switch]$StrictArt,
    [switch]$StrictAudio,
    [switch]$StrictInlineActions
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "app/src/main/assets"
$assetPackAssets = Join-Path $root "world_assets/src/main/assets"
$rawDir = Join-Path $root "app/src/main/res/raw"
$minImageBytes = 4096
$minAudioBytes = 4096
$pngSignature = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

function Read-AssetJson($name) {
    Get-Content (Join-Path $assets $name) -Raw | ConvertFrom-Json
}

function As-Array($value) {
    if ($null -eq $value) { return @() }
    if ($value -is [System.Array]) { return $value }
    return @($value)
}

function Read-FileHeader($path, [int]$count) {
    if (-not (Test-Path -LiteralPath $path)) { return $null }
    $stream = [System.IO.File]::OpenRead($path)
    try {
        $buffer = [byte[]]::new($count)
        $read = $stream.Read($buffer, 0, $count)
        if ($read -lt $count) { return $null }
        return ,$buffer
    } finally {
        $stream.Dispose()
    }
}

function Read-BigEndianUInt32($bytes, [int]$offset) {
    return ([uint32]$bytes[$offset] -shl 24) -bor ([uint32]$bytes[$offset + 1] -shl 16) -bor ([uint32]$bytes[$offset + 2] -shl 8) -bor [uint32]$bytes[$offset + 3]
}

function Get-PngInfo($path) {
    $header = Read-FileHeader $path 24
    if ($null -eq $header) {
        return [pscustomobject]@{ Valid = $false; Width = 0; Height = 0; Reason = "file is shorter than a PNG header" }
    }
    for ($i = 0; $i -lt $pngSignature.Length; $i++) {
        if ($header[$i] -ne $pngSignature[$i]) {
            return [pscustomobject]@{ Valid = $false; Width = 0; Height = 0; Reason = "missing PNG signature" }
        }
    }
    $chunkName = [System.Text.Encoding]::ASCII.GetString($header, 12, 4)
    if ($chunkName -ne "IHDR") {
        return [pscustomobject]@{ Valid = $false; Width = 0; Height = 0; Reason = "missing IHDR chunk" }
    }
    return [pscustomobject]@{
        Valid = $true
        Width = [int](Read-BigEndianUInt32 $header 16)
        Height = [int](Read-BigEndianUInt32 $header 20)
        Reason = ""
    }
}

function Get-AssetFile($relativePath) {
    if ([string]::IsNullOrWhiteSpace($relativePath)) { return $null }
    foreach ($assetRoot in @($assets, $assetPackAssets)) {
        $path = Join-Path $assetRoot $relativePath
        if (Test-Path -LiteralPath $path) {
            return Get-Item -LiteralPath $path
        }
    }
    return $null
}

function Has-Asset($relativePath) {
    return $null -ne (Get-AssetFile $relativePath)
}

function Validate-ImageAsset($label, $relativePath, [int]$minimumWidth, [int]$minimumHeight) {
    if ([string]::IsNullOrWhiteSpace($relativePath)) {
        $warnings.Add("Missing World 1 image: $label has no path.")
        return
    }
    $file = Get-AssetFile $relativePath
    if ($null -eq $file) {
        $warnings.Add("Missing World 1 image: $label -> $relativePath")
        return
    }
    if ($file.Length -lt $minImageBytes) {
        $warnings.Add("Invalid World 1 image: $label -> $relativePath is only $($file.Length) bytes.")
    }
    if ($file.Extension -ieq ".png") {
        $info = Get-PngInfo $file.FullName
        if (-not $info.Valid) {
            $warnings.Add("Invalid World 1 image: $label -> $relativePath ($($info.Reason)).")
        } elseif ($info.Width -lt $minimumWidth -or $info.Height -lt $minimumHeight) {
            $warnings.Add("Invalid World 1 image: $label -> $relativePath is $($info.Width)x$($info.Height), expected at least ${minimumWidth}x${minimumHeight}.")
        }
    } elseif ($file.Extension -notin @(".jpg", ".jpeg", ".webp")) {
        $warnings.Add("Invalid World 1 image: $label -> $relativePath uses unsupported extension '$($file.Extension)'.")
    }
}

function Normalize-CueId($cueId) {
    if ([string]::IsNullOrWhiteSpace($cueId)) { return $null }
    return $cueId.Trim().ToLowerInvariant().Replace('-', '_')
}

function Get-RawAudioFile($cueId) {
    $name = Normalize-CueId $cueId
    if (-not $name) { return $null }
    return Get-ChildItem -LiteralPath $rawDir -File -ErrorAction SilentlyContinue | Where-Object { $_.BaseName -eq $name } | Select-Object -First 1
}

function Has-RawAudio($cueId) {
    return $null -ne (Get-RawAudioFile $cueId)
}

function Validate-RawAudio($cueId) {
    if ([string]::IsNullOrWhiteSpace($cueId)) { return }
    $file = Get-RawAudioFile $cueId
    if ($null -eq $file) {
        $warnings.Add("Missing raw audio resource for World 1 cue: $cueId")
        return
    }
    if ($file.Length -lt $minAudioBytes) {
        $warnings.Add("Invalid raw audio resource for World 1 cue '$cueId': $($file.Name) is only $($file.Length) bytes.")
        return
    }
    $header = Read-FileHeader $file.FullName 12
    if ($null -eq $header) {
        $warnings.Add("Invalid raw audio resource for World 1 cue '$cueId': $($file.Name) is too short to inspect.")
        return
    }
    if ($file.Extension -ieq ".mp3") {
        $hasId3 = [System.Text.Encoding]::ASCII.GetString($header, 0, 3) -eq "ID3"
        $hasFrameSync = $header[0] -eq 0xFF -and (($header[1] -band 0xE0) -eq 0xE0)
        if (-not ($hasId3 -or $hasFrameSync)) {
            $warnings.Add("Invalid raw audio resource for World 1 cue '$cueId': $($file.Name) is not a recognizable MP3.")
        }
    } elseif ($file.Extension -ieq ".wav") {
        $riff = [System.Text.Encoding]::ASCII.GetString($header, 0, 4)
        $wave = [System.Text.Encoding]::ASCII.GetString($header, 8, 4)
        if ($riff -ne "RIFF" -or $wave -ne "WAVE") {
            $warnings.Add("Invalid raw audio resource for World 1 cue '$cueId': $($file.Name) is not a recognizable WAV.")
        }
    } else {
        $warnings.Add("Invalid raw audio resource for World 1 cue '$cueId': unsupported extension '$($file.Extension)'.")
    }
}

function Add-SetValue($set, $value) {
    if (-not [string]::IsNullOrWhiteSpace($value)) { [void]$set.Add($value) }
}

function Split-CueLayers($cueId) {
    if ([string]::IsNullOrWhiteSpace($cueId)) { return @() }
    return @(([string]$cueId) -split '[+,;]' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Get-JsonPropertyValue($object, [string]$name) {
    if ($null -eq $object -or [string]::IsNullOrWhiteSpace($name)) { return $null }
    $property = $object.PSObject.Properties[$name]
    if ($null -eq $property) { return $null }
    return [string]$property.Value
}

function Get-OppositeDirection([string]$direction) {
    switch ($direction) {
        "north" { return "south" }
        "south" { return "north" }
        "east" { return "west" }
        "west" { return "east" }
        default { return $null }
    }
}

function Get-RoomConnection($room, [string]$direction) {
    if ($null -eq $room -or $null -eq $room.connections) { return $null }
    $property = $room.connections.PSObject.Properties[$direction]
    if ($null -eq $property) { return $null }
    return [string]$property.Value
}

function Has-PlaceholderCopy($text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $false }
    return [regex]::IsMatch(
        [string]$text,
        "(?i)\b(placeholder|todo|tbd|fixme|lorem ipsum|demo)\b|\(default\)|\bdefault\):|'\`$scene_id'"
    )
}

function Get-InlineActionNameVariants([string]$label) {
    if ([string]::IsNullOrWhiteSpace($label)) { return @() }
    $variants = New-Object System.Collections.Generic.List[string]
    [void]$variants.Add($label)

    $dashNormalized = $label.Replace("-", " ")
    if ($dashNormalized -ne $label) { [void]$variants.Add($dashNormalized) }

    $apostropheNormalized = $label.Replace([string][char]0x2019, "'")
    if ($apostropheNormalized -ne $label) { [void]$variants.Add($apostropheNormalized) }

    return @($variants | Select-Object -Unique)
}

function Test-InlineActionNameAppearsInDescription([string]$label, [string]$description) {
    if ([string]::IsNullOrWhiteSpace($label) -or [string]::IsNullOrWhiteSpace($description)) { return $false }
    $haystack = $description.ToLowerInvariant()
    foreach ($variant in Get-InlineActionNameVariants $label) {
        if ([string]::IsNullOrWhiteSpace($variant)) { continue }
        if ($haystack.Contains($variant.ToLowerInvariant())) { return $true }
    }
    return $false
}

function Validate-PolishedCopy($context, $text) {
    if (Has-PlaceholderCopy $text) {
        $errors.Add("$context contains placeholder copy: '$text'.")
    }
}

function Validate-RequiredCopy($context, $text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        $errors.Add("$context is missing display copy.")
        return
    }
    Validate-PolishedCopy $context $text
}

function Validate-OptionalCopy($context, $text) {
    if (-not [string]::IsNullOrWhiteSpace($text)) {
        Validate-PolishedCopy $context $text
    }
}

$hubs = Read-AssetJson "hubs.json"
$nodes = Read-AssetJson "hub_nodes.json"
$rooms = Read-AssetJson "rooms.json"
$items = Read-AssetJson "items.json"
$enemies = Read-AssetJson "enemies.json"
$npcs = Read-AssetJson "npcs.json"
$characters = Read-AssetJson "characters.json"
$milestones = Read-AssetJson "milestones.json"
$audioCatalog = Read-AssetJson "audio_catalog.json"
$audioBindings = Read-AssetJson "audio_bindings.json"

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

$world1HubIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($hub in $hubs) {
    if ($hub.world_id -eq "world_1") { Add-SetValue $world1HubIds $hub.id }
}

$world1Nodes = @($nodes | Where-Object { $_.world_id -eq "world_1" -or $world1HubIds.Contains([string]$_.hub_id) })
$world1RoomIds = New-Object System.Collections.Generic.HashSet[string]
$world1NodeByRoomId = @{}
foreach ($node in $world1Nodes) {
    foreach ($roomId in As-Array $node.rooms) {
        Add-SetValue $world1RoomIds $roomId
        $roomKey = [string]$roomId
        if ([string]::IsNullOrWhiteSpace($roomKey)) { continue }
        if ($world1NodeByRoomId.ContainsKey($roomKey)) {
            $errors.Add("World 1 room '$roomKey' is listed in multiple nodes ('$($world1NodeByRoomId[$roomKey].id)' and '$($node.id)').")
        } else {
            $world1NodeByRoomId[$roomKey] = $node
        }
    }
}

$roomsById = @{}
foreach ($room in $rooms) { if ($room.id) { $roomsById[$room.id] = $room } }
$itemIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($item in $items) { Add-SetValue $itemIds $item.id }
$enemyIds = New-Object System.Collections.Generic.HashSet[string]
$enemiesById = @{}
foreach ($enemy in $enemies) {
    Add-SetValue $enemyIds $enemy.id
    if ($enemy.id) { $enemiesById[$enemy.id] = $enemy }
}
$actorIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($npc in $npcs) { Add-SetValue $actorIds $npc.id }
foreach ($character in $characters) { Add-SetValue $actorIds $character.id }
$milestoneIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($milestone in $milestones) { Add-SetValue $milestoneIds $milestone.id }

$world1Rooms = New-Object System.Collections.Generic.List[object]
foreach ($roomId in $world1RoomIds) {
    if (-not $roomsById.ContainsKey($roomId)) {
        $errors.Add("World 1 node references missing room '$roomId'.")
    } else {
        $world1Rooms.Add($roomsById[$roomId])
    }
}

foreach ($hub in $hubs | Where-Object { $world1HubIds.Contains([string]$_.id) }) {
    Validate-RequiredCopy "World 1 hub '$($hub.id)' title" $hub.title
    Validate-RequiredCopy "World 1 hub '$($hub.id)' description" $hub.description
}

foreach ($node in $world1Nodes) {
    Validate-RequiredCopy "World 1 node '$($node.id)' title" $node.title
}

foreach ($room in $world1Rooms) {
    Validate-RequiredCopy "Room '$($room.id)' title" $room.title
    Validate-RequiredCopy "Room '$($room.id)' description" $room.description

    foreach ($entry in $room.item_flavor.PSObject.Properties) {
        Validate-OptionalCopy "Room '$($room.id)' item_flavor '$($entry.Name)'" $entry.Value
    }
    foreach ($entry in $room.enemy_flavor.PSObject.Properties) {
        Validate-OptionalCopy "Room '$($room.id)' enemy_flavor '$($entry.Name)'" $entry.Value
    }
    foreach ($entry in $room.blocked_directions.PSObject.Properties) {
        $block = $entry.Value
        Validate-OptionalCopy "Room '$($room.id)' blocked '$($entry.Name)' locked message" $block.message_locked
        Validate-OptionalCopy "Room '$($room.id)' blocked '$($entry.Name)' unlock message" $block.message_unlock
    }
    foreach ($action in As-Array $room.actions) {
        $actionContext = "Room '$($room.id)' action '$($action.name)'"
        Validate-RequiredCopy "$actionContext name" $action.name
        Validate-OptionalCopy "$actionContext condition_unmet_message" $action.condition_unmet_message
        Validate-OptionalCopy "$actionContext already_open_message" $action.already_open_message
        Validate-OptionalCopy "$actionContext popup_title" $action.popup_title
        if (-not (Test-InlineActionNameAppearsInDescription ([string]$action.name) ([string]$room.description))) {
            $warnings.Add("$actionContext is not mentioned in the room description, so it cannot be highlighted or tapped inline.")
        }
    }
}

$cardinalDirections = @("north", "south", "east", "west")
foreach ($node in $world1Nodes) {
    $nodeRoomIds = New-Object System.Collections.Generic.HashSet[string]
    foreach ($roomId in As-Array $node.rooms) { Add-SetValue $nodeRoomIds $roomId }

    $entryRoomId = [string]$node.entry_room
    if ([string]::IsNullOrWhiteSpace($entryRoomId)) {
        $errors.Add("World 1 node '$($node.id)' has no entry_room.")
        continue
    }
    if (-not $nodeRoomIds.Contains($entryRoomId)) {
        $errors.Add("World 1 node '$($node.id)' entry_room '$entryRoomId' is not listed in its rooms.")
        continue
    }
    if (-not $roomsById.ContainsKey($entryRoomId)) { continue }

    $visited = New-Object System.Collections.Generic.HashSet[string]
    $queue = [System.Collections.Generic.Queue[string]]::new()
    [void]$visited.Add($entryRoomId)
    $queue.Enqueue($entryRoomId)

    while ($queue.Count -gt 0) {
        $currentRoomId = $queue.Dequeue()
        if (-not $roomsById.ContainsKey($currentRoomId)) { continue }
        $room = $roomsById[$currentRoomId]
        if ($null -eq $room.connections) { continue }
        foreach ($entry in $room.connections.PSObject.Properties) {
            $connectedRoomId = [string]$entry.Value
            if ($nodeRoomIds.Contains($connectedRoomId) -and -not $visited.Contains($connectedRoomId)) {
                [void]$visited.Add($connectedRoomId)
                $queue.Enqueue($connectedRoomId)
            }
        }
    }

    foreach ($roomId in @($nodeRoomIds)) {
        if ($roomsById.ContainsKey($roomId) -and -not $visited.Contains($roomId)) {
            $errors.Add("World 1 node '$($node.id)' room '$roomId' is not reachable from entry_room '$entryRoomId' through node-local connections.")
        }
    }
}

foreach ($room in $world1Rooms) {
    if ($null -eq $room.connections) { continue }
    foreach ($entry in $room.connections.PSObject.Properties) {
        $direction = [string]$entry.Name
        if ($cardinalDirections -notcontains $direction) { continue }

        $connectedRoomId = [string]$entry.Value
        if ([string]::IsNullOrWhiteSpace($connectedRoomId) -or -not $roomsById.ContainsKey($connectedRoomId)) { continue }
        if (-not $world1RoomIds.Contains($connectedRoomId)) { continue }

        $oppositeDirection = Get-OppositeDirection $direction
        $backConnection = Get-RoomConnection $roomsById[$connectedRoomId] $oppositeDirection
        if ($backConnection -ne [string]$room.id) {
            $errors.Add("Room '$($room.id)' connection '$direction' points to '$connectedRoomId', but '$connectedRoomId' does not point '$oppositeDirection' back to '$($room.id)'.")
        }
    }
}

foreach ($room in $world1Rooms) {
    foreach ($entry in $room.connections.PSObject.Properties) {
        if (-not $roomsById.ContainsKey([string]$entry.Value)) {
            $errors.Add("Room '$($room.id)' connection '$($entry.Name)' points to missing room '$($entry.Value)'.")
        }
    }
    foreach ($itemId in As-Array $room.items) {
        if (-not $itemIds.Contains([string]$itemId)) { $errors.Add("Room '$($room.id)' references unknown item '$itemId'.") }
    }
    foreach ($entry in $room.item_flavor.PSObject.Properties) {
        if (-not $itemIds.Contains([string]$entry.Name)) { $errors.Add("Room '$($room.id)' item_flavor references unknown item '$($entry.Name)'.") }
    }
    foreach ($enemyId in As-Array $room.enemies) {
        if (-not $enemyIds.Contains([string]$enemyId)) { $errors.Add("Room '$($room.id)' references unknown enemy '$enemyId'.") }
    }
    foreach ($party in As-Array $room.enemy_parties) {
        foreach ($enemyId in As-Array $party) {
            if (-not $enemyIds.Contains([string]$enemyId)) { $errors.Add("Room '$($room.id)' enemy party references unknown enemy '$enemyId'.") }
        }
    }
    foreach ($entry in $room.enemy_flavor.PSObject.Properties) {
        if (-not $enemyIds.Contains([string]$entry.Name)) { $errors.Add("Room '$($room.id)' enemy_flavor references unknown enemy '$($entry.Name)'.") }
    }
    foreach ($actorId in As-Array $room.npcs) {
        if (-not $actorIds.Contains([string]$actorId)) { $errors.Add("Room '$($room.id)' references unknown actor '$actorId'.") }
    }
    foreach ($rule in As-Array $room.npc_presence) {
        if (-not $actorIds.Contains([string]$rule.npc)) { $errors.Add("Room '$($room.id)' npc_presence references unknown actor '$($rule.npc)'.") }
        foreach ($milestoneId in As-Array $rule.requires_milestones) {
            if (-not $milestoneIds.Contains([string]$milestoneId)) { $errors.Add("Room '$($room.id)' npc_presence requires unknown milestone '$milestoneId'.") }
        }
        foreach ($milestoneId in As-Array $rule.forbidden_milestones) {
            if (-not $milestoneIds.Contains([string]$milestoneId)) { $errors.Add("Room '$($room.id)' npc_presence forbids unknown milestone '$milestoneId'.") }
        }
    }
}

foreach ($hub in $hubs | Where-Object { $world1HubIds.Contains([string]$_.id) }) {
    Validate-ImageAsset "hub '$($hub.id)' background" $hub.background_image 768 1024
}
foreach ($node in $world1Nodes) {
    Validate-ImageAsset "node '$($node.id)' icon" $node.icon_image 512 512
}
foreach ($room in $world1Rooms) {
    Validate-ImageAsset "room '$($room.id)' background" $room.background_image 768 1024
}

$world1EnemyIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($room in $world1Rooms) {
    foreach ($enemyId in As-Array $room.enemies) { Add-SetValue $world1EnemyIds $enemyId }
    foreach ($party in As-Array $room.enemy_parties) {
        foreach ($enemyId in As-Array $party) { Add-SetValue $world1EnemyIds $enemyId }
    }
}
foreach ($enemyId in @($world1EnemyIds)) {
    if ($enemiesById.ContainsKey($enemyId)) {
        Validate-ImageAsset "enemy '$enemyId' combat portrait" $enemiesById[$enemyId].portrait 768 768
    }
}

$world1ActorIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($room in $world1Rooms) {
    foreach ($actorId in As-Array $room.npcs) { Add-SetValue $world1ActorIds $actorId }
    foreach ($rule in As-Array $room.npc_presence) { Add-SetValue $world1ActorIds $rule.npc }
}

foreach ($character in $characters) {
    Validate-ImageAsset "character '$($character.id)' portrait" $character.mini_icon_path 768 768
    Validate-ImageAsset "character '$($character.id)' combat portrait" $character.combat_icon_path 768 768
}

foreach ($npc in $npcs) {
    if ($world1ActorIds.Contains([string]$npc.id)) {
        Validate-RequiredCopy "World 1 NPC '$($npc.id)' name" $npc.name
        Validate-RequiredCopy "World 1 NPC '$($npc.id)' description" $npc.description
        Validate-OptionalCopy "World 1 NPC '$($npc.id)' role" $npc.role
        foreach ($interaction in As-Array $npc.interactions) {
            Validate-RequiredCopy "World 1 NPC '$($npc.id)' interaction label" $interaction.label
        }
        Validate-ImageAsset "NPC '$($npc.id)' portrait" $npc.portrait 768 768
    }
}

$catalogIds = New-Object System.Collections.Generic.HashSet[string]
$requiredAudioIds = New-Object System.Collections.Generic.HashSet[string]
$requiredRuntimeAudioIds = @(
    "music_title_theme",
    "music_victory_theme"
)
foreach ($cueId in $requiredRuntimeAudioIds) { Add-SetValue $requiredAudioIds $cueId }
foreach ($track in As-Array $audioCatalog.tracks) {
    Add-SetValue $catalogIds $track.id
    $tags = @(As-Array $track.tags | ForEach-Object { [string]$_ })
    if ($track.id -like "music_w1_*" -or $track.id -eq "music_crash_flight" -or $tags -contains "world_1") {
        Add-SetValue $requiredAudioIds $track.id
    }
}
foreach ($cue in As-Array $audioCatalog.cues) { Add-SetValue $catalogIds $cue.id }

function Add-AudioRequirement($label, $cueId) {
    $layers = @(Split-CueLayers $cueId)
    if ($layers.Count -eq 0) {
        $errors.Add("$label does not resolve an audio cue.")
        return
    }
    foreach ($layer in $layers) { Add-SetValue $requiredAudioIds $layer }
}

foreach ($hubId in $world1HubIds) {
    Add-AudioRequirement "World 1 hub '$hubId' music binding" (Get-JsonPropertyValue $audioBindings.music $hubId)
    Add-AudioRequirement "World 1 hub '$hubId' ambience binding" (Get-JsonPropertyValue $audioBindings.ambience $hubId)
}
foreach ($room in $world1Rooms) {
    $roomId = [string]$room.id
    $node = $world1NodeByRoomId[$roomId]
    $hubId = if ($null -ne $node) { [string]$node.hub_id } else { $null }

    $musicBinding = Get-JsonPropertyValue $audioBindings.music $roomId
    if ([string]::IsNullOrWhiteSpace($musicBinding)) { $musicBinding = Get-JsonPropertyValue $audioBindings.music $hubId }
    Add-AudioRequirement "World 1 room '$roomId' music binding" $musicBinding

    $ambienceBinding = Get-JsonPropertyValue $audioBindings.ambience $roomId
    if ([string]::IsNullOrWhiteSpace($ambienceBinding)) { $ambienceBinding = Get-JsonPropertyValue $audioBindings.ambience $hubId }
    Add-AudioRequirement "World 1 room '$roomId' ambience binding" $ambienceBinding

    if (-not [string]::IsNullOrWhiteSpace($room.weather)) {
        Add-AudioRequirement "World 1 room '$roomId' weather '$($room.weather)' binding" (Get-JsonPropertyValue $audioBindings.weather ([string]$room.weather))
    }
}
foreach ($sectionName in @("ui", "battle")) {
    $section = $audioBindings.$sectionName
    foreach ($entry in $section.PSObject.Properties) {
        foreach ($layer in Split-CueLayers $entry.Value) { Add-SetValue $requiredAudioIds $layer }
    }
}

foreach ($cueId in $requiredAudioIds) {
    if (-not $catalogIds.Contains([string]$cueId)) { $warnings.Add("Audio binding uses cue not listed in catalog: $cueId") }
    Validate-RawAudio $cueId
}

foreach ($warning in $warnings) { Write-Warning $warning }
foreach ($errorMessage in $errors) { Write-Error $errorMessage }

$inlineActionWarnings = @($warnings | Where-Object { $_ -like "*cannot be highlighted or tapped inline*" })

if (
    $errors.Count -gt 0 -or
    ($StrictArt -and ($warnings | Where-Object { $_ -like "Missing World 1 *image*" }).Count -gt 0) -or
    ($StrictAudio -and ($warnings | Where-Object { $_ -like "*audio*" -or $_ -like "Audio binding*" }).Count -gt 0) -or
    ($StrictInlineActions -and $inlineActionWarnings.Count -gt 0)
) {
    exit 1
}

Write-Host "World 1 validation passed: $($world1Rooms.Count) rooms across $($world1Nodes.Count) nodes. Warnings: $($warnings.Count)."
