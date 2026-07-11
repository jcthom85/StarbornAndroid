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

$trackedFiles = @(
    "app/src/main/assets/dialogue.json",
    "app/src/main/assets/rooms.json",
    "app/src/main/assets/events.json",
    "app/src/main/assets/quests.json",
    "app/src/main/assets/cinematics.json",
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
    } elseif ($wordCount -gt 20) {
        $warnings.Add("World 1 dialogue '$($line.id)' is $wordCount words; review for splitting or cuts.")
    }
}

$openingQuest = $quests | Where-Object id -eq "w1_mq01"
$openingRoomIds = @("pit_nova_bunk", "workshop_yard")
$openingRooms = $rooms | Where-Object { $_.id -in $openingRoomIds }
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
