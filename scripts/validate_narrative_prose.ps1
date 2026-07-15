param([switch]$Strict)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$errors = [Collections.Generic.List[string]]::new()
$warnings = [Collections.Generic.List[string]]::new()

function Read-Json([string]$relativePath) {
    Get-Content (Join-Path $root $relativePath) -Raw | ConvertFrom-Json
}

$dialogue = Read-Json "app/src/main/assets/dialogue.json"
$rooms = Read-Json "app/src/main/assets/rooms.json"
$events = Read-Json "app/src/main/assets/events.json"
$quests = Read-Json "app/src/main/assets/quests.json"
$cinematics = Read-Json "app/src/main/assets/cinematics.json"

$trackedFiles = @(
    "app/src/main/assets/dialogue.json",
    "app/src/main/assets/rooms.json",
    "app/src/main/assets/events.json",
    "app/src/main/assets/quests.json",
    "app/src/main/assets/cinematics.json",
    "app/src/main/assets/items.json",
    "app/src/main/assets/skills.json",
    "app/src/main/assets/enemies.json",
    "app/src/main/assets/milestones.json",
    "app/src/main/assets/tutorial_scripts.json",
    "docs/story/Starborn_Writer_Handbook.md",
    "docs/story/Characters.md",
    "docs/story/Character_Arcs.md",
    "docs/story/Content_Standards.md"
)
foreach ($relativePath in $trackedFiles) {
    $text = [IO.File]::ReadAllText((Join-Path $root $relativePath))
    if ($text.Contains([char]0x00C3)) {
        $errors.Add("Encoding corruption in $relativePath")
    }
}

$world1Dialogue = $dialogue | Where-Object {
    $_.id -match '(^|_)w1_' -or $_.id -match '^(jed|hank|bogs|scrapper|doc|warden)_'
}
foreach ($line in $world1Dialogue) {
    $wordCount = @([string]$line.text -split '\s+' | Where-Object { $_ }).Count
    if ($wordCount -gt 32) {
        $errors.Add("World 1 dialogue '$($line.id)' is $wordCount words (maximum 32).")
    } elseif ($wordCount -gt 18) {
        $warnings.Add("World 1 dialogue '$($line.id)' is $wordCount words; review for splitting or cuts.")
    }
}

$world1Rooms = $rooms | Where-Object {
    $_.background_image -like "*world_1*" -and $_.id -notmatch '^debug_'
}
$world1RoomCopy = [Collections.Generic.List[object]]::new()
foreach ($room in $world1Rooms) {
    foreach ($entry in @(
        @{ Field = "description"; Text = [string]$room.description },
        @{ Field = "description_dark"; Text = [string]$room.description_dark }
    )) {
        if ([string]::IsNullOrWhiteSpace($entry.Text)) { continue }
        $wordCount = @($entry.Text -split '\s+' | Where-Object { $_ }).Count
        if ($wordCount -gt 45) {
            $errors.Add("World 1 room '$($room.id)' $($entry.Field) is $wordCount words (maximum 45).")
        }
        $world1RoomCopy.Add([pscustomobject]@{ Room = $room.id; Field = $entry.Field; Text = $entry.Text })
    }
    $variantIndex = 0
    foreach ($variant in @($room.description_variants)) {
        $text = [string]$variant.description
        if (-not [string]::IsNullOrWhiteSpace($text)) {
            $wordCount = @($text -split '\s+' | Where-Object { $_ }).Count
            if ($wordCount -gt 45) {
                $errors.Add("World 1 room '$($room.id)' variant[$variantIndex] is $wordCount words (maximum 45).")
            }
            $world1RoomCopy.Add([pscustomobject]@{ Room = $room.id; Field = "variant[$variantIndex]"; Text = $text })
        }
        $variantIndex++
    }
    foreach ($action in @($room.actions)) {
        $text = [string]$action.condition_unmet_message
        if (-not [string]::IsNullOrWhiteSpace($text)) {
            $world1RoomCopy.Add([pscustomobject]@{ Room = $room.id; Field = "action:$($action.name)"; Text = $text })
        }
    }
}

$duplicateRoomCopyAllowlist = @()
$world1RoomCopy |
    Group-Object Text |
    Where-Object { $_.Count -gt 1 -and $_.Name -notin $duplicateRoomCopyAllowlist } |
    ForEach-Object {
        $locations = ($_.Group | ForEach-Object { "$($_.Room)/$($_.Field)" }) -join ", "
        $errors.Add("Duplicate World 1 room copy appears at ${locations}: '$($_.Name)'")
    }

$openingQuest = $quests | Where-Object id -eq "w1_mq01"
$openingRoomIds = @("pit_nova_bunk", "workshop_yard")
$openingRooms = $rooms | Where-Object { $_.id -in $openingRoomIds }
$novaBunk = $rooms | Where-Object id -eq "pit_nova_bunk"
if ([string]$novaBunk.description_dark -notmatch '(?i)\bbunk light\b') {
    $errors.Add("Nova's dark-state bunk description must expose the 'bunk light' inline action.")
}
$openingEventIds = @(
    "w1_mq01_turn_on_bunk_light",
    "w1_mq01_inspect_safety_fault",
    "w1_mq01_inspect_loader_relay",
    "w1_mq01_faulted_loader_victory"
)
$openingEvents = $events | Where-Object { $_.id -in $openingEventIds }
$openingText = @(
    ($openingQuest | ConvertTo-Json -Depth 20)
    ($openingRooms | ConvertTo-Json -Depth 20)
    ($openingEvents | ConvertTo-Json -Depth 20)
) -join [Environment]::NewLine
foreach ($phrase in @("impossible reflection", "three-note", "third note", "chosen one", "destiny")) {
    if ($openingText -match [regex]::Escape($phrase)) {
        $errors.Add("Unsupported opening language remains: '$phrase'.")
    }
}

$world1ActiveText = @(
    ($world1Dialogue | ConvertTo-Json -Depth 20)
    ($world1Rooms | ConvertTo-Json -Depth 30)
    (($events | Where-Object { $_.id -match '^w1_' }) | ConvertTo-Json -Depth 40)
    (($quests | Where-Object { $_.id -match '^w1_' }) | ConvertTo-Json -Depth 30)
    (($cinematics | Where-Object { $_.id -in @('intro_prologue', 'scene_cutter_surge', 'scene_relic_sync', 'scene_launch_crash') }) | ConvertTo-Json -Depth 20)
) -join [Environment]::NewLine
foreach ($pattern in @(
    '(?i)chosen\s+(one|by)',
    '(?i)Aethel\s+(blood|ancestry|gene)',
    '(?i)prior\s+(Chime\s+)?exposure',
    '(?i)(Echo|Fork|relic)\s+(recognizes|remembers|wants|chooses|judges)\s+Nova',
    '(?i)Nova.{0,30}memor(y|ies).{0,20}(erased|lost|gone)',
    '(?i)Chime.{0,30}(supplies|provides|generates).{0,15}(power|fuel|thrust)'
)) {
    if ($world1ActiveText -match $pattern) {
        $errors.Add("Forbidden World 1 canon pattern matched: $pattern")
    }
}

$cinematicText = $cinematics | ConvertTo-Json -Depth 20
foreach ($requiredPhrase in @(
    "It is off. That hum is not mine.",
    "87 kHz",
    "Cold loop: 68%",
    "Ground phase: 180 degrees",
    "CUTTER FAULT 4C-117",
    "operator NOVA VANCE"
)) {
    if ($cinematicText -notmatch [regex]::Escape($requiredPhrase)) {
        $errors.Add("Required opening fact or benchmark line is missing: '$requiredPhrase'")
    }
}

$canonical = [IO.File]::ReadAllText((Join-Path $root "docs/story/Starborn_Writer_Handbook.md"))
foreach ($archive in @(
    "docs/story/archive/Style_Guide.md",
    "docs/story/archive/Narrative_Voice_and_Tone_Guide.md"
)) {
    $text = [IO.File]::ReadAllText((Join-Path $root $archive))
    if ($text -notmatch "SUPERSEDED") {
        $errors.Add("Archived guide is not marked SUPERSEDED: $archive")
    }
}
if ($canonical -notmatch "People speak plainly") {
    $errors.Add("Canonical handbook is missing the grounded prose rule.")
}

$warnings | ForEach-Object { Write-Warning $_ }
if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}
Write-Host "Narrative prose validation passed: $($world1Dialogue.Count) World 1 dialogue entries checked, $($warnings.Count) warning(s)."
