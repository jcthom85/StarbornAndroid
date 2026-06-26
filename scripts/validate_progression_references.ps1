param(
    [switch]$StrictMilestones
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

function Get-Prop($object, $name) {
    if ($null -eq $object) { return $null }
    $property = $object.PSObject.Properties[$name]
    if ($null -eq $property) { return $null }
    return $property.Value
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

function Normalize-ItemLookupKey($raw) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return $null }
    $lower = ([string]$raw).Trim().ToLowerInvariant()
    if ([string]::IsNullOrWhiteSpace($lower)) { return $null }
    return $lower
}

function Add-ItemLookupValue($set, $raw) {
    $lower = Normalize-ItemLookupKey $raw
    if (-not $lower) { return }
    foreach ($variant in @(
        $lower,
        ($lower -replace '\s+', '_'),
        ($lower -replace '-', '_'),
        ($lower -replace '[^a-z0-9_]', '')
    )) {
        if (-not [string]::IsNullOrWhiteSpace($variant)) {
            [void]$set.Add($variant)
        }
    }
}

function Has-ItemLookupValue($set, $raw) {
    $lower = Normalize-ItemLookupKey $raw
    if (-not $lower) { return $false }
    $candidates = @(
        $lower,
        ($lower -replace '\s+', '_'),
        ($lower -replace '-', '_'),
        ($lower -replace '[^a-z0-9_]', '')
    )
    if ($lower.Contains("_")) {
        $candidates += ($lower -replace '_', ' ')
    }
    foreach ($candidate in ($candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)) {
        if ($set.Contains([string]$candidate)) { return $true }
    }
    return $false
}

function Parse-IdQuantity($raw) {
    $text = ([string]$raw).Trim()
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    foreach ($delimiter in @("*", "|")) {
        $index = $text.IndexOf($delimiter)
        if ($index -gt 0) {
            return @{
                Id = $text.Substring(0, $index).Trim()
                Quantity = $text.Substring($index + 1).Trim()
            }
        }
    }
    $quantityMatch = [regex]::Match($text, "^(?<id>.+)x(?<quantity>\d+)$")
    if ($quantityMatch.Success) {
        return @{
            Id = $quantityMatch.Groups["id"].Value.Trim()
            Quantity = $quantityMatch.Groups["quantity"].Value.Trim()
        }
    }
    return @{ Id = $text; Quantity = "1" }
}

function Split-QuestPair($raw) {
    $parts = ([string]$raw).Split(":", 2)
    if ($parts.Count -lt 2) { return $null }
    $left = $parts[0].Trim()
    $right = $parts[1].Trim()
    if ([string]::IsNullOrWhiteSpace($left) -or [string]::IsNullOrWhiteSpace($right)) { return $null }
    return @{ QuestId = $left; Value = $right }
}

$hubs = Read-AssetJson "hubs.json"
$nodes = Read-AssetJson "hub_nodes.json"
$rooms = Read-AssetJson "rooms.json"
$items = Read-AssetJson "items.json"
$enemies = Read-AssetJson "enemies.json"
$npcs = Read-AssetJson "npcs.json"
$characters = Read-AssetJson "characters.json"
$quests = Read-AssetJson "quests.json"
$events = Read-AssetJson "events.json"
$dialogue = Read-AssetJson "dialogue.json"
$milestones = Read-AssetJson "milestones.json"
$cinematics = Read-AssetJson "cinematics.json"
$tutorials = Read-AssetJson "tutorial_scripts.json"
$audioCatalog = Read-AssetJson "audio_catalog.json"
$skills = Read-AssetJson "skills.json"
$shops = Read-AssetJson "shops.json"

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]
$questStartSources = New-StringSet
$questCompleteSources = New-StringSet
$questTaskDoneSources = New-StringSet
$world1ShopIds = New-StringSet

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

$roomIds = New-StringSet
foreach ($room in $rooms) { Add-SetValue $roomIds $room.id }
$itemIds = New-StringSet
$itemLookupKeys = New-StringSet
$itemGateKeys = New-StringSet
foreach ($item in $items) {
    Add-SetValue $itemIds $item.id
    foreach ($value in @($item.id, $item.name) + (As-Array $item.aliases)) {
        Add-ItemLookupValue $itemLookupKeys $value
    }
    Add-SetValue $itemGateKeys $item.id
    Add-SetValue $itemGateKeys $item.name
}
$enemyIds = New-StringSet
foreach ($enemy in $enemies) { Add-SetValue $enemyIds $enemy.id }
$milestoneIds = New-StringSet
foreach ($milestone in $milestones) { Add-SetValue $milestoneIds $milestone.id }
$cinematicIds = New-StringSet
foreach ($scene in $cinematics) { Add-SetValue $cinematicIds $scene.id }
$tutorialIds = New-StringSet
foreach ($tutorial in $tutorials) { Add-SetValue $tutorialIds $tutorial.id }
$audioIds = New-StringSet
foreach ($track in As-Array $audioCatalog.tracks) { Add-SetValue $audioIds $track.id }
foreach ($cue in As-Array $audioCatalog.cues) { Add-SetValue $audioIds $cue.id }
$skillIds = New-StringSet
foreach ($skill in $skills) { Add-SetValue $skillIds $skill.id }
$shopIds = New-StringSet
foreach ($property in $shops.PSObject.Properties) { Add-SetValue $shopIds $property.Name }
$playerActionTriggerIds = New-StringSet
foreach ($event in $events) {
    if ((Get-Prop $event.trigger "type") -eq "player_action") {
        Add-SetValue $playerActionTriggerIds (Get-Prop $event.trigger "action")
    }
}

$actorIds = New-StringSet
$actorNames = New-StringSet
$npcsById = @{}
foreach ($npc in $npcs) {
    Add-SetValue $actorIds $npc.id
    Add-SetValue $actorNames $npc.name
    if ($npc.id) { $npcsById[$npc.id] = $npc }
}
foreach ($character in $characters) {
    Add-SetValue $actorIds $character.id
    Add-SetValue $actorNames $character.name
}

$questIds = New-StringSet
$questStagesById = @{}
$questTasksById = @{}
foreach ($quest in $quests) {
    $questId = [string]$quest.id
    Add-SetValue $questIds $questId
    $stageSet = New-StringSet
    $taskSet = New-StringSet
    foreach ($stage in As-Array $quest.stages) {
        Add-SetValue $stageSet $stage.id
        foreach ($task in As-Array $stage.tasks) {
            Add-SetValue $taskSet $task.id
        }
    }
    $questStagesById[$questId] = $stageSet
    $questTasksById[$questId] = $taskSet
}

$dialogueIds = New-StringSet
$dialogueById = @{}
$dialogueSpeakers = New-StringSet
foreach ($line in $dialogue) {
    Add-SetValue $dialogueIds $line.id
    Add-SetValue $dialogueSpeakers $line.speaker
    if ($line.id) { $dialogueById[$line.id] = $line }
}

$cinematicsById = @{}
foreach ($scene in $cinematics) {
    if ($scene.id) { $cinematicsById[[string]$scene.id] = $scene }
}

function Add-MissingMilestone($context, $milestoneId) {
    if ([string]::IsNullOrWhiteSpace($milestoneId)) { return }
    if (-not (Has-SetValue $milestoneIds $milestoneId)) {
        $warnings.Add("$context references milestone '$milestoneId' without a definition in milestones.json.")
    }
}

function Validate-Quest($context, $questId) {
    if ([string]::IsNullOrWhiteSpace($questId)) { return }
    if (-not (Has-SetValue $questIds $questId)) {
        $errors.Add("$context references unknown quest '$questId'.")
    }
}

function Validate-QuestStage($context, $questId, $stageId) {
    Validate-Quest $context $questId
    if ([string]::IsNullOrWhiteSpace($questId) -or [string]::IsNullOrWhiteSpace($stageId)) { return }
    if ((Has-SetValue $questIds $questId) -and -not $questStagesById[$questId].Contains([string]$stageId)) {
        $errors.Add("$context references unknown stage '$stageId' on quest '$questId'.")
    }
}

function Validate-QuestTask($context, $questId, $taskId) {
    Validate-Quest $context $questId
    if ([string]::IsNullOrWhiteSpace($questId) -or [string]::IsNullOrWhiteSpace($taskId)) { return }
    if ((Has-SetValue $questIds $questId) -and -not $questTasksById[$questId].Contains([string]$taskId)) {
        $errors.Add("$context references unknown task '$taskId' on quest '$questId'.")
    }
}

function Validate-Room($context, $roomId) {
    if ([string]::IsNullOrWhiteSpace($roomId)) { return }
    if (-not (Has-SetValue $roomIds $roomId)) {
        $errors.Add("$context references unknown room '$roomId'.")
    }
}

function Validate-Item($context, $itemId) {
    if ([string]::IsNullOrWhiteSpace($itemId)) { return }
    if (-not (Has-SetValue $itemIds $itemId)) {
        $errors.Add("$context references unknown item '$itemId'.")
    }
}

function Validate-RequiredItem($context, $itemId) {
    if ([string]::IsNullOrWhiteSpace($itemId)) {
        $errors.Add("$context is missing an item id.")
        return
    }
    Validate-Item $context $itemId
}

function Validate-Integer($context, $raw, [int]$minimum) {
    if ($null -eq $raw -or [string]::IsNullOrWhiteSpace([string]$raw)) { return }
    $parsed = 0
    if (-not [int]::TryParse([string]$raw, [ref]$parsed)) {
        $errors.Add("$context has non-numeric value '$raw'.")
        return
    }
    if ($parsed -lt $minimum) {
        $errors.Add("$context has value '$parsed'; expected at least $minimum.")
    }
}

function Validate-RequiredInteger($context, $raw, [int]$minimum) {
    if ($null -eq $raw -or [string]::IsNullOrWhiteSpace([string]$raw)) {
        $errors.Add("$context is missing a numeric value.")
        return
    }
    Validate-Integer $context $raw $minimum
}

function Validate-OptionalPositiveQuantity($context, $quantity) {
    Validate-Integer "$context quantity" $quantity 1
}

function Validate-OptionalNonNegativeAmount($context, $amount) {
    Validate-Integer $context $amount 0
}

function Validate-Enemy($context, $enemyId) {
    if ([string]::IsNullOrWhiteSpace($enemyId)) { return }
    if (-not (Has-SetValue $enemyIds $enemyId)) {
        $errors.Add("$context references unknown enemy '$enemyId'.")
    }
}

function Validate-Cinematic($context, $sceneId) {
    if ([string]::IsNullOrWhiteSpace($sceneId)) { return }
    if (-not (Has-SetValue $cinematicIds $sceneId)) {
        $errors.Add("$context references unknown cinematic '$sceneId'.")
    }
}

function Validate-Tutorial($context, $tutorialId) {
    if ([string]::IsNullOrWhiteSpace($tutorialId)) { return }
    if (-not (Has-SetValue $tutorialIds $tutorialId)) {
        $errors.Add("$context references unknown tutorial '$tutorialId'.")
    }
}

function Validate-Skill($context, $skillId) {
    if ([string]::IsNullOrWhiteSpace($skillId)) { return }
    if (-not (Has-SetValue $skillIds $skillId)) {
        $errors.Add("$context references unknown skill '$skillId'.")
    }
}

function Validate-AudioCue($context, $cueId) {
    if ([string]::IsNullOrWhiteSpace($cueId)) { return }
    if (-not (Has-SetValue $audioIds $cueId)) {
        $errors.Add("$context references unknown audio cue '$cueId'.")
    }
}

function Validate-PlayerActionTrigger($context, $actionId) {
    if ([string]::IsNullOrWhiteSpace($actionId)) { return }
    if (-not (Has-SetValue $playerActionTriggerIds $actionId)) {
        $errors.Add("$context references player action '$actionId' but no event trigger handles it.")
    }
}

function Validate-Shop($context, $shopId) {
    if ([string]::IsNullOrWhiteSpace($shopId)) { return }
    if (-not (Has-SetValue $shopIds $shopId)) {
        $errors.Add("$context references unknown shop '$shopId'.")
    }
}

function Validate-ShopItemReference($context, $rawItem) {
    if ([string]::IsNullOrWhiteSpace($rawItem)) { return }
    if (-not (Has-ItemLookupValue $itemLookupKeys $rawItem)) {
        $errors.Add("$context references unknown shop item '$rawItem'.")
    }
}

function Validate-ShopDefinition($shopId) {
    if ([string]::IsNullOrWhiteSpace($shopId) -or -not (Has-SetValue $shopIds $shopId)) { return }
    $shop = Get-Prop $shops $shopId
    if ($null -eq $shop) { return }
    $context = "Shop '$shopId'"

    foreach ($itemRef in As-Array $shop.sells.items) {
        Validate-ShopItemReference "$context sells.items" $itemRef
    }
    foreach ($itemRef in As-Array $shop.sells.rotation_pool) {
        Validate-ShopItemReference "$context sells.rotation_pool" $itemRef
    }
    foreach ($gate in $shop.sells.gates.PSObject.Properties) {
        if (-not (Has-SetValue $itemGateKeys $gate.Name)) {
            $errors.Add("$context sells.gates references '$($gate.Name)', but gates must use an item id or exact item name.")
        }
        foreach ($milestoneId in As-Array $gate.Value.milestones) {
            Add-MissingMilestone "$context sells.gates '$($gate.Name)'" $milestoneId
        }
    }
    foreach ($itemRef in As-Array $shop.buys.blacklist) {
        Validate-ShopItemReference "$context buys.blacklist" $itemRef
    }
}

function Validate-Condition($condition, $context) {
    if ($null -eq $condition) { return }
    $type = ([string](Get-Prop $condition "type")).Trim().ToLowerInvariant()
    switch ($type) {
        { $_ -in @("quest_active", "quest_not_started", "quest_completed", "quest_not_completed", "quest_failed") } {
            Validate-Quest $context (Get-Prop $condition "quest_id")
        }
        { $_ -in @("quest_stage", "quest_stage_not") } {
            Validate-QuestStage $context (Get-Prop $condition "quest_id") (Get-Prop $condition "stage_id")
        }
        { $_ -in @("quest_task_done", "quest_task_not_done") } {
            Validate-QuestTask $context (Get-Prop $condition "quest_id") (Get-Prop $condition "task_id")
        }
        { $_ -in @("item", "item_not") } {
            $itemId = Get-Prop $condition "item_id"
            if ([string]::IsNullOrWhiteSpace($itemId)) { $itemId = Get-Prop $condition "item" }
            Validate-Item $context $itemId
            Validate-OptionalPositiveQuantity $context (Get-Prop $condition "quantity")
        }
        { $_ -in @("milestone_set", "milestone_not_set") } {
            Add-MissingMilestone $context (Get-Prop $condition "milestone")
        }
        { $_ -in @("tutorial_completed", "tutorial_not_completed") } {
            Validate-Tutorial $context (Get-Prop $condition "tutorial_id")
        }
    }
}

function Validate-Reward($reward, $context) {
    if ($null -eq $reward) { return }
    Validate-OptionalNonNegativeAmount "$context reward xp" (Get-Prop $reward "xp")
    Validate-OptionalNonNegativeAmount "$context reward credits" (Get-Prop $reward "credits")
    Validate-OptionalNonNegativeAmount "$context reward ap" (Get-Prop $reward "ap")
    foreach ($rewardItem in As-Array (Get-Prop $reward "items")) {
        $itemId = Get-Prop $rewardItem "item_id"
        Validate-RequiredItem "$context reward item" $itemId
        Validate-OptionalPositiveQuantity "$context reward item '$itemId'" (Get-Prop $rewardItem "quantity")
    }
}

function Validate-RewardItems($items, $context) {
    foreach ($rewardItem in As-Array $items) {
        $itemId = Get-Prop $rewardItem "item_id"
        Validate-RequiredItem "$context item" $itemId
        Validate-OptionalPositiveQuantity "$context item '$itemId'" (Get-Prop $rewardItem "quantity")
    }
}

function Validate-ActionList($actions, $context) {
    foreach ($action in As-Array $actions) {
        Validate-Action $action $context
    }
}

function Validate-Action($action, $context) {
    if ($null -eq $action) { return }
    $type = ([string](Get-Prop $action "type")).Trim().ToLowerInvariant()
    $actionContext = "$context action '$type'"

    switch ($type) {
        { $_ -in @("if_quest_active", "if_quest_not_started", "if_quest_completed", "if_quest_not_completed", "if_quest_failed", "advance_quest", "advance_quest_if_active", "track_quest", "fail_quest") } {
            Validate-Quest $actionContext (Get-Prop $action "quest_id")
        }
        "start_quest" {
            $questId = Get-Prop $action "start_quest"
            if ([string]::IsNullOrWhiteSpace($questId)) { $questId = Get-Prop $action "quest_id" }
            Validate-Quest $actionContext $questId
            Add-SetValue $questStartSources $questId
        }
        "complete_quest" {
            $questId = Get-Prop $action "complete_quest"
            if ([string]::IsNullOrWhiteSpace($questId)) { $questId = Get-Prop $action "quest_id" }
            Validate-Quest $actionContext $questId
            Add-SetValue $questCompleteSources $questId
        }
        "set_quest_task_done" {
            $questId = Get-Prop $action "quest_id"
            $taskId = Get-Prop $action "task_id"
            Validate-QuestTask $actionContext $questId $taskId
            if (-not [string]::IsNullOrWhiteSpace($questId) -and -not [string]::IsNullOrWhiteSpace($taskId)) {
                Add-SetValue $questTaskDoneSources "$($questId):$($taskId)"
            }
        }
        "advance_quest_stage" {
            Validate-QuestStage $actionContext (Get-Prop $action "quest_id") (Get-Prop $action "to_stage_id")
        }
        { $_ -in @("if_milestone_set", "if_milestone_not_set", "set_milestone", "clear_milestone") } {
            Add-MissingMilestone $actionContext (Get-Prop $action "milestone")
        }
        "if_milestones_set" {
            foreach ($milestoneId in As-Array (Get-Prop $action "milestones")) {
                Add-MissingMilestone $actionContext $milestoneId
            }
        }
        "set_milestones" {
            foreach ($milestoneId in As-Array (Get-Prop $action "set_milestones")) {
                Add-MissingMilestone $actionContext $milestoneId
            }
        }
        "clear_milestones" {
            foreach ($milestoneId in As-Array (Get-Prop $action "clear_milestones")) {
                Add-MissingMilestone $actionContext $milestoneId
            }
        }
        { $_ -in @("play_cinematic", "trigger_cutscene") } {
            Validate-Cinematic $actionContext (Get-Prop $action "scene_id")
        }
        "show_message" {
            if ([string]::IsNullOrWhiteSpace((Get-Prop $action "message"))) {
                $errors.Add("$actionContext is missing message text.")
            }
        }
        "unlock_skill" {
            $skillId = Get-Prop $action "skill_id"
            if ([string]::IsNullOrWhiteSpace($skillId)) { $skillId = Get-Prop $action "item_id" }
            if ([string]::IsNullOrWhiteSpace($skillId)) { $skillId = Get-Prop $action "item" }
            if ([string]::IsNullOrWhiteSpace($skillId)) {
                $errors.Add("$actionContext is missing skill_id.")
            } else {
                Validate-Skill $actionContext $skillId
            }
        }
        "narrate" {
            $message = Get-Prop $action "message"
            if ([string]::IsNullOrWhiteSpace($message)) { $message = Get-Prop $action "text" }
            if ([string]::IsNullOrWhiteSpace($message)) {
                $errors.Add("$actionContext is missing narration text.")
            }
        }
        "system_tutorial" {
            Validate-Tutorial $actionContext (Get-Prop $action "scene_id")
        }
        "start_dialogue" {
            if ([string]::IsNullOrWhiteSpace((Get-Prop $action "npc"))) {
                $errors.Add("$actionContext is missing npc.")
            }
        }
        { $_ -in @("begin_node", "set_room", "warp", "set_room_state", "toggle_room_state", "unlock_room_search") } {
            Validate-Room $actionContext (Get-Prop $action "room_id")
        }
        "spawn_encounter" {
            if ([string]::IsNullOrWhiteSpace((Get-Prop $action "encounter_id"))) {
                $errors.Add("$actionContext is missing encounter_id.")
            }
            Validate-Room $actionContext (Get-Prop $action "room_id")
        }
        { $_ -in @("give_item", "give_item_to_player", "take_item", "reveal_hidden_item", "spawn_item_on_ground") } {
            $itemId = Get-Prop $action "item_id"
            if ([string]::IsNullOrWhiteSpace($itemId)) { $itemId = Get-Prop $action "item" }
            $rewardItems = @(As-Array (Get-Prop $action "items"))
            if ([string]::IsNullOrWhiteSpace($itemId) -and ($type -notin @("give_item", "give_item_to_player") -or $rewardItems.Count -eq 0)) {
                $errors.Add("$actionContext is missing an item id.")
            } else {
                Validate-Item $actionContext $itemId
            }
            Validate-OptionalPositiveQuantity $actionContext (Get-Prop $action "quantity")
            Validate-RewardItems $rewardItems $actionContext
        }
        { $_ -in @("give_reward", "grant_reward") } {
            Validate-Reward (Get-Prop $action "reward") $actionContext
            Validate-RewardItems (Get-Prop $action "items") $actionContext
            Validate-OptionalNonNegativeAmount "$actionContext xp" (Get-Prop $action "xp")
            Validate-OptionalNonNegativeAmount "$actionContext credits" (Get-Prop $action "credits")
            Validate-OptionalNonNegativeAmount "$actionContext ap" (Get-Prop $action "ap")
        }
        "add_party_member" {
            $memberId = Get-Prop $action "item_id"
            if (-not [string]::IsNullOrWhiteSpace($memberId) -and -not (Has-SetValue $actorIds $memberId)) {
                $errors.Add("$actionContext references unknown party member '$memberId'.")
            }
        }
        "give_xp" {
            Validate-RequiredInteger "$actionContext xp" (Get-Prop $action "xp") 1
        }
        "player_action" {
            Validate-PlayerActionTrigger $actionContext (Get-Prop $action "action")
        }
        "audio_layer" {
            Validate-AudioCue $actionContext (Get-Prop $action "cue_id")
        }
        { $_ -in @("rebuild_ui", "wait_for_draw") } {
        }
        default {
            if (-not [string]::IsNullOrWhiteSpace($type)) {
                $errors.Add("$actionContext is not a supported event action type.")
            }
        }
    }

    Validate-ActionList (Get-Prop $action "do") $actionContext
    Validate-ActionList (Get-Prop $action "elseDo") $actionContext
    Validate-ActionList (Get-Prop $action "on_complete") $actionContext
}

function Validate-RoomAction($room, $action) {
    if ($null -eq $action) { return }
    $type = ([string](Get-Prop $action "type")).Trim().ToLowerInvariant()
    $name = Get-Prop $action "name"
    $context = "Room '$($room.id)' action '$name'"

    Validate-PlayerActionTrigger $context (Get-Prop $action "action_event")
    Validate-PlayerActionTrigger $context (Get-Prop $action "action_event_on")
    Validate-PlayerActionTrigger $context (Get-Prop $action "action_event_off")

    foreach ($milestoneId in As-Array (Get-Prop $action "requires_milestones")) {
        Add-MissingMilestone $context $milestoneId
    }
    Add-MissingMilestone $context (Get-Prop $action "requires_milestone")

    if ($type -eq "shop") {
        $shopId = Get-Prop $action "shop_id"
        Validate-Shop $context $shopId
        Add-SetValue $world1ShopIds $shopId
    }
    if ($type -eq "container") {
        foreach ($rawItem in As-Array (Get-Prop $action "items")) {
            $parsed = Parse-IdQuantity $rawItem
            if ($null -eq $parsed) {
                $errors.Add("$context has malformed container item value '$rawItem'.")
            } else {
                Validate-Item $context $parsed.Id
            }
        }
    }
}

function Validate-DialogueExpression($raw, $context) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return }
    foreach ($token in ([string]$raw).Split(",")) {
        $trimmed = $token.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) { continue }
        $parts = $trimmed.Split(":", 2)
        $type = $parts[0].Trim().ToLowerInvariant()
        $value = if ($parts.Count -gt 1) { $parts[1].Trim() } else { "" }
        $tokenContext = "$context token '$type'"

        switch ($type) {
            { $_ -in @("quest", "quest_active", "quest_completed", "quest_not_started", "quest_failed", "start_quest", "complete_quest", "fail_quest", "track_quest", "advance_quest") } {
                Validate-Quest $tokenContext $value
                if ($type -eq "start_quest") { Add-SetValue $questStartSources $value }
                if ($type -eq "complete_quest") { Add-SetValue $questCompleteSources $value }
            }
            { $_ -in @("quest_stage", "quest_stage_not", "advance_quest_stage") } {
                $pair = Split-QuestPair $value
                if ($null -eq $pair) {
                    $errors.Add("$tokenContext has malformed quest/stage value '$value'.")
                } else {
                    Validate-QuestStage $tokenContext $pair.QuestId $pair.Value
                }
            }
            { $_ -in @("quest_task_done", "quest_task_not_done", "set_quest_task_done") } {
                $pair = Split-QuestPair $value
                if ($null -eq $pair) {
                    $errors.Add("$tokenContext has malformed quest/task value '$value'.")
                } else {
                    Validate-QuestTask $tokenContext $pair.QuestId $pair.Value
                    if ($type -eq "set_quest_task_done") {
                        Add-SetValue $questTaskDoneSources "$($pair.QuestId):$($pair.Value)"
                    }
                }
            }
            { $_ -in @("milestone", "milestone_not_set", "set_milestone", "clear_milestone") } {
                Add-MissingMilestone $tokenContext $value
            }
            { $_ -in @("item", "item_not", "give_item", "take_item") } {
                $parsed = Parse-IdQuantity $value
                if ($null -eq $parsed) {
                    $errors.Add("$tokenContext has malformed item value '$value'.")
                } else {
                    Validate-Item $tokenContext $parsed.Id
                    if (($parsed.Id -like "*:*") -and $type -in @("give_item", "take_item")) {
                        $warnings.Add("$tokenContext uses ':' inside item value '$value'. Dialogue item quantities must use '*', 'x', or '|'.")
                    }
                }
            }
            "system_tutorial" {
                $tutorialId = ($value.Split("|", 2)[0]).Trim()
                Validate-Tutorial $tokenContext $tutorialId
            }
            "play_cinematic" {
                Validate-Cinematic $tokenContext $value
            }
            { $_ -in @("give_xp", "give_credits", "player_action", "untrack_quest") } {
                if ($type -eq "give_xp") { Validate-RequiredInteger "$tokenContext amount" $value 1 }
                if ($type -eq "give_credits") { Validate-RequiredInteger "$tokenContext amount" $value 1 }
            }
            default {
                $warnings.Add("$tokenContext is not recognized by the progression validator.")
            }
        }
    }
}

function Is-World1Event($event) {
    if ([string]$event.id -like "w1_*") { return $true }
    $json = $event | ConvertTo-Json -Depth 32 -Compress
    if ($json -match "w1_" -or $json -match "ms_w1_") { return $true }
    foreach ($roomId in $world1RoomIds) {
        if ($json.Contains("`"$roomId`"")) { return $true }
    }
    return $false
}

function Is-World1DialogueLine($line) {
    $text = "$($line.id) $($line.condition) $($line.trigger)"
    return ($text -match "w1_" -or $text -match "ms_w1_")
}

function Has-PlaceholderCopy($text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $false }
    return [regex]::IsMatch(
        [string]$text,
        "(?i)\b(placeholder|todo|tbd|fixme|lorem ipsum|demo)\b|\(default\)|\bdefault\):|'\`$scene_id'"
    )
}

function Validate-PolishedCopy($context, $text) {
    if (Has-PlaceholderCopy $text) {
        $errors.Add("$context contains placeholder or debug-facing copy: '$text'.")
    }
}

function Validate-RequiredPolishedCopy($context, $text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        $errors.Add("$context is missing display copy.")
        return
    }
    Validate-PolishedCopy $context $text
}

$world1CinematicIds = New-StringSet

foreach ($scene in $cinematics) {
    $sceneContext = "Cinematic '$($scene.id)'"
    Validate-PolishedCopy "$sceneContext title" $scene.title
    foreach ($step in As-Array $scene.steps) {
        Validate-PolishedCopy "$sceneContext step" $step.text
    }
}

foreach ($event in $events) {
    if (-not (Is-World1Event $event)) { continue }
    $context = "Event '$($event.id)'"
    $trigger = $event.trigger
    if ($trigger) {
        $triggerRoom = Get-Prop $trigger "room_id"
        if ([string]::IsNullOrWhiteSpace($triggerRoom)) { $triggerRoom = Get-Prop $trigger "room" }
        Validate-Room "$context trigger" $triggerRoom
        Validate-Quest "$context trigger" (Get-Prop $trigger "quest_id")
        $triggerItem = Get-Prop $trigger "item_id"
        if ([string]::IsNullOrWhiteSpace($triggerItem)) { $triggerItem = Get-Prop $trigger "item" }
        Validate-Item "$context trigger" $triggerItem
        foreach ($enemyId in As-Array (Get-Prop $trigger "enemies")) {
            Validate-Enemy "$context trigger" $enemyId
        }
    }
    foreach ($condition in As-Array $event.conditions) {
        Validate-Condition $condition "$context condition '$($condition.type)'"
    }
    Validate-ActionList $event.actions $context

    $actionStack = New-Object System.Collections.Generic.List[object]
    foreach ($action in As-Array $event.actions) { $actionStack.Add($action) }
    while ($actionStack.Count -gt 0) {
        $lastIndex = $actionStack.Count - 1
        $action = $actionStack[$lastIndex]
        $actionStack.RemoveAt($lastIndex)
        $sceneId = Get-Prop $action "scene_id"
        if ([string]::IsNullOrWhiteSpace($sceneId)) { $sceneId = Get-Prop $action "sceneId" }
        if (-not [string]::IsNullOrWhiteSpace($sceneId)) { Add-SetValue $world1CinematicIds $sceneId }
        foreach ($nested in As-Array (Get-Prop $action "do")) { $actionStack.Add($nested) }
        foreach ($nested in As-Array (Get-Prop $action "else_do")) { $actionStack.Add($nested) }
        foreach ($nested in As-Array (Get-Prop $action "elseDo")) { $actionStack.Add($nested) }
        foreach ($nested in As-Array (Get-Prop $action "on_complete")) { $actionStack.Add($nested) }
        foreach ($nested in As-Array (Get-Prop $action "onComplete")) { $actionStack.Add($nested) }
    }
}

foreach ($room in $rooms) {
    if (-not (Has-SetValue $world1RoomIds $room.id)) { continue }
    foreach ($action in As-Array $room.actions) {
        Validate-RoomAction $room $action
    }
}

$world1DialogueIds = New-StringSet
foreach ($line in $dialogue) {
    if (Is-World1DialogueLine $line) { Add-SetValue $world1DialogueIds $line.id }
}

$changed = $true
while ($changed) {
    $changed = $false
    foreach ($dialogueId in @($world1DialogueIds)) {
        $line = $dialogueById[$dialogueId]
        if ($null -eq $line) { continue }
        foreach ($nextId in @($line.next) + (As-Array $line.options | ForEach-Object { $_.next })) {
            if (-not [string]::IsNullOrWhiteSpace($nextId) -and -not $world1DialogueIds.Contains([string]$nextId)) {
                [void]$world1DialogueIds.Add([string]$nextId)
                $changed = $true
            }
        }
    }
}

foreach ($dialogueId in @($world1DialogueIds)) {
    $line = $dialogueById[$dialogueId]
    if ($null -eq $line) { continue }
    $context = "Dialogue '$($line.id)'"
    Validate-PolishedCopy "$context text" $line.text
    if (-not [string]::IsNullOrWhiteSpace($line.next) -and -not (Has-SetValue $dialogueIds $line.next)) {
        $errors.Add("$context points to missing next dialogue '$($line.next)'.")
    }
    Validate-DialogueExpression $line.condition "$context condition"
    Validate-DialogueExpression $line.trigger "$context trigger"
    foreach ($option in As-Array $line.options) {
        if (-not [string]::IsNullOrWhiteSpace($option.next) -and -not (Has-SetValue $dialogueIds $option.next)) {
            $errors.Add("$context option '$($option.id)' points to missing next dialogue '$($option.next)'.")
        }
        Validate-DialogueExpression $option.condition "$context option '$($option.id)' condition"
        Validate-DialogueExpression $option.trigger "$context option '$($option.id)' trigger"
    }
}

foreach ($sceneId in @($world1CinematicIds)) {
    $scene = $cinematicsById[$sceneId]
    if ($null -eq $scene) { continue }
    foreach ($step in As-Array $scene.steps) {
        Validate-PolishedCopy "Cinematic '$sceneId' step" $step.text
    }
}

foreach ($milestone in $milestones | Where-Object { $_.id -like "ms_w1_*" }) {
    $context = "Milestone '$($milestone.id)'"
    foreach ($skillId in As-Array $milestone.effects.unlock_abilities) {
        if (-not (Has-SetValue $skillIds $skillId)) {
            $errors.Add("$context unlocks unknown skill '$skillId'.")
        }
    }
    foreach ($roomId in As-Array $milestone.effects.unlock_areas) {
        Validate-Room "$context effect unlock_areas" $roomId
    }
    foreach ($exit in As-Array $milestone.effects.unlock_exits) {
        Validate-Room "$context effect unlock_exits" $exit.room_id
    }
}

$world1ActorIds = New-StringSet
foreach ($room in $rooms) {
    if (-not (Has-SetValue $world1RoomIds $room.id)) { continue }
    foreach ($npcId in As-Array $room.npcs) { Add-SetValue $world1ActorIds $npcId }
    foreach ($rule in As-Array $room.npc_presence) { Add-SetValue $world1ActorIds $rule.npc }
}

foreach ($actorId in @($world1ActorIds)) {
    $npc = $npcsById[$actorId]
    if ($null -eq $npc) { continue }
    if (-not (Has-SetValue $dialogueSpeakers $npc.name)) {
        $warnings.Add("World 1 NPC '$($npc.id)' ('$($npc.name)') has no dialogue speaker lines.")
    }
    foreach ($interaction in As-Array $npc.interactions) {
        $interactionContext = "World 1 NPC '$($npc.id)' interaction '$($interaction.label)'"
        if (([string]$interaction.type).Trim().ToLowerInvariant() -eq "shop") {
            Validate-Shop $interactionContext $interaction.shop_id
            Add-SetValue $world1ShopIds $interaction.shop_id
        }
        $dialogueId = Get-Prop $interaction "dialogue_id"
        if (-not [string]::IsNullOrWhiteSpace($dialogueId) -and -not (Has-SetValue $dialogueIds $dialogueId)) {
            $warnings.Add("World 1 NPC '$($npc.id)' interaction '$($interaction.label)' references missing dialogue_id '$dialogueId'.")
        }
        if (-not [string]::IsNullOrWhiteSpace($dialogueId) -and (Has-SetValue $dialogueIds $dialogueId)) {
            $line = $dialogueById[$dialogueId]
            if ($line) { Validate-PolishedCopy "World 1 NPC '$($npc.id)' interaction '$($interaction.label)' dialogue '$dialogueId'" $line.text }
        }
    }
}

foreach ($shopId in @($world1ShopIds)) {
    Validate-ShopDefinition $shopId
}

foreach ($quest in $quests | Where-Object { $_.id -like "w1_*" }) {
    $questId = [string]$quest.id
    Validate-RequiredPolishedCopy "World 1 quest '$questId' title" $quest.title
    Validate-RequiredPolishedCopy "World 1 quest '$questId' summary" $quest.summary
    Validate-RequiredPolishedCopy "World 1 quest '$questId' description" $quest.description
    Validate-PolishedCopy "World 1 quest '$questId' flavor" $quest.flavor
    Validate-PolishedCopy "World 1 quest '$questId' giver" $quest.giver

    if (-not (Has-SetValue $questStartSources $questId)) {
        $errors.Add("World 1 quest '$questId' has no start_quest source in validated events/dialogue.")
    }
    if (-not (Has-SetValue $questCompleteSources $questId)) {
        $errors.Add("World 1 quest '$questId' has no complete_quest source in validated events/dialogue.")
    }
    foreach ($stage in As-Array $quest.stages) {
        Validate-RequiredPolishedCopy "World 1 quest '$questId' stage '$($stage.id)' title" $stage.title
        Validate-RequiredPolishedCopy "World 1 quest '$questId' stage '$($stage.id)' description" $stage.description
        foreach ($task in As-Array $stage.tasks) {
            Validate-RequiredPolishedCopy "World 1 quest '$questId' task '$($task.id)' text" $task.text
            $taskKey = "$($questId):$($task.id)"
            if (-not (Has-SetValue $questTaskDoneSources $taskKey)) {
                $errors.Add("World 1 quest '$questId' task '$($task.id)' has no set_quest_task_done source in validated events/dialogue.")
            }
        }
    }
}

foreach ($warning in $warnings) { Write-Warning $warning }
foreach ($errorMessage in $errors) { Write-Error $errorMessage }

if ($errors.Count -gt 0 -or ($StrictMilestones -and $warnings.Count -gt 0)) {
    exit 1
}

Write-Host "Progression reference validation passed: $($events | Where-Object { Is-World1Event $_ } | Measure-Object | Select-Object -ExpandProperty Count) World 1 event(s), $($world1DialogueIds.Count) dialogue line(s). Warnings: $($warnings.Count)."
