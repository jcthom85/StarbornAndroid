package com.example.starborn.feature.exploration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueSession
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.BlockedDirection
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.Requirement
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExplorationViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val sessionStore: GameSessionStore,
    private val dialogueService: DialogueService,
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val questRepository: QuestRepository,
    eventDefinitions: List<GameEvent>,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val eventsById: Map<String, GameEvent> = eventDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(ExplorationUiState())
    val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorationEvent>()
    val events: SharedFlow<ExplorationEvent> = _events.asSharedFlow()

    init {
        inventoryService.loadItems()
    }

    private val eventManager = EventManager(
        events = eventDefinitions,
        sessionStore = sessionStore,
        eventHooks = EventHooks(
            onPlayCinematic = { sceneId -> emitEvent(ExplorationEvent.PlayCinematic(sceneId)) },
            onMessage = { message ->
                postStatus(message)
                emitEvent(ExplorationEvent.ShowMessage(message))
            },
            onReward = { reward ->
                handleReward(reward)
                emitEvent(ExplorationEvent.RewardGranted(reward))
            },
            onSetRoomState = { roomId, stateKey, value ->
                val applied = setRoomStateValue(roomId, stateKey, value)
                if (applied != null) {
                    val targetRoom = roomId ?: _uiState.value.currentRoom?.id
                    postStatus(formatRoomStateStatus(stateKey, applied))
                    emitEvent(ExplorationEvent.RoomStateChanged(targetRoom, stateKey, applied))
                }
            },
            onToggleRoomState = { roomId, stateKey ->
                val newValue = toggleRoomStateValue(roomId, stateKey)
                if (newValue != null) {
                    val targetRoom = roomId ?: _uiState.value.currentRoom?.id
                    postStatus(formatRoomStateStatus(stateKey, newValue))
                    emitEvent(ExplorationEvent.RoomStateChanged(targetRoom, stateKey, newValue))
                }
            },
            onSpawnEncounter = { encounterId, roomId ->
                emitEvent(ExplorationEvent.SpawnEncounter(encounterId, roomId))
            },
            onGiveItem = { itemId, quantity ->
                inventoryService.addItem(itemId, quantity)
                val name = inventoryService.itemDisplayName(itemId)
                postStatus("Received $quantity x $name")
                refreshCurrentRoomBlockedDirections()
                emitEvent(ExplorationEvent.ItemGranted(name, quantity))
            },
            onGiveXp = { amount ->
                postStatus("Gained $amount XP")
                emitEvent(ExplorationEvent.XpGained(amount))
            },
            onAdvanceQuest = { questId ->
                emitEvent(ExplorationEvent.QuestAdvanced(questId))
            },
            onQuestUpdated = {},
            onQuestTaskUpdated = { questId, taskId ->
                if (!questId.isNullOrEmpty() && !taskId.isNullOrEmpty()) {
                    markQuestTaskComplete(questId, taskId)
                }
            },
            onQuestStageAdvanced = { questId, stageId ->
                if (!questId.isNullOrEmpty() && !stageId.isNullOrEmpty()) {
                    questStageProgress[questId] = stageId
                    if (_uiState.value.isQuestLogVisible) {
                        updateQuestSummaries(
                            _uiState.value.activeQuests,
                            _uiState.value.completedQuests
                        )
                    }
                }
            },
            onQuestStarted = { questId ->
                questId?.let { resetQuestProgress(it) }
            },
            onQuestCompleted = { questId ->
                questId?.let { markQuestCompleted(it) }
            },
            onBeginNode = { roomId ->
                emitEvent(ExplorationEvent.BeginNode(roomId))
            },
            onSystemTutorial = { sceneId, context ->
                showTutorial(sceneId, context)
                emitEvent(ExplorationEvent.TutorialRequested(sceneId, context))
            },
            onMilestoneSet = { milestone ->
                val message = formatMilestoneMessage(milestone)
                showMilestone(milestone, message)
                postStatus(message)
            },
            onNarration = { message, tapToDismiss ->
                showNarration(message, tapToDismiss)
                if (!tapToDismiss) {
                    postStatus(message)
                }
            },
            onSpawnGroundItem = { roomId, itemId, quantity ->
                handleGroundItemSpawn(roomId, itemId, quantity)
            },
            onUnlockRoomSearch = { roomId, note ->
                val targetRoom = roomId ?: _uiState.value.currentRoom?.id
                val message = note ?: "Something useful is now revealed."
                postStatus(message)
                emitEvent(ExplorationEvent.RoomSearchUnlocked(targetRoom, message))
            }
        )
    )

    private var roomsById: Map<String, Room> = emptyMap()
    private val roomStates: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()
    private val roomGroundItems: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    private var pendingStateMessage: String? = null
    private var suppressNextStateMessage: Boolean = false
    private val questTaskProgress: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val questStageProgress: MutableMap<String, String> = mutableMapOf()
    private val milestoneMessages: Map<String, String> = mapOf(
        "ms_kasey_projector_collected" to "Recovered Kasey's broken projector.",
        "ms_maddie_grinder_collected" to "Recovered Maddie's coffee grinder.",
        "ms_ellie_display_collected" to "Recovered Ellie's arcade display.",
        "ms_repair_bundle_delivered" to "Jed received the bundle of broken devices.",
        "ms_tinkering_prompt_active" to "Jed unlocked tinkering. Head to the table to continue.",
        "ms_talked_to_jed" to "Jed's briefing is complete.",
        "ms_mine_power_on" to "The Stellarium generator roars to life.",
        "ms_breaker_alert_shown" to "You hear machinery unlocking nearby."
    )
    private val unlockedDirections: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private var activeDialogueSession: DialogueSession? = null

    private fun emitEvent(event: ExplorationEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    private fun postStatus(message: String) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(statusMessage = message) }
        }
    }

    private fun handleReward(reward: EventReward) {
        val parts = mutableListOf<String>()
        reward.xp?.takeIf { it > 0 }?.let {
            parts.add("$it XP")
            emitEvent(ExplorationEvent.XpGained(it))
        }
        reward.ap?.takeIf { it > 0 }?.let { parts.add("$it AP") }
        reward.credits?.takeIf { it > 0 }?.let { parts.add("$it credits") }
        reward.items.filter { it.itemId.isNotBlank() }.forEach { item ->
            val qty = item.quantity ?: 1
            inventoryService.addItem(item.itemId, qty)
            val name = inventoryService.itemDisplayName(item.itemId)
            parts.add("$qty x $name")
            emitEvent(ExplorationEvent.ItemGranted(name, qty))
        }
        if (parts.isNotEmpty()) {
            postStatus("Reward: ${parts.joinToString(", ")}")
        }
        refreshCurrentRoomBlockedDirections()
    }

    private fun triggerPlayerAction(actionId: String?) {
        if (!actionId.isNullOrBlank()) {
            eventManager.handleTrigger("player_action", EventPayload.Action(actionId))
        }
    }

    init {
        viewModelScope.launch(dispatchers.main) {
            var previousState = sessionStore.state.value
            sessionStore.state.collect { newState ->
                _uiState.update {
                    it.copy(
                        activeQuests = newState.activeQuests,
                        completedQuests = newState.completedQuests,
                        completedMilestones = newState.completedMilestones
                    )
                }
                newState.activeQuests.forEach { ensureQuestDefaults(it) }
                newState.completedQuests.forEach { ensureQuestDefaults(it) }
                if (newState.activeQuests != previousState.activeQuests ||
                    newState.completedQuests != previousState.completedQuests
                ) {
                    emitEvent(ExplorationEvent.QuestUpdated(newState.activeQuests, newState.completedQuests))
                    postStatus("Quest log updated")
                    if (_uiState.value.isQuestLogVisible) {
                        updateQuestSummaries(newState.activeQuests, newState.completedQuests)
                    }
                }
                previousState = newState
            }
        }
    }

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch(dispatchers.io) {
            val worlds = worldAssets.loadWorlds()
            val hubs = worldAssets.loadHubs()
            val rooms = worldAssets.loadRooms().map { room ->
                val envPrefix = room.id.substringBefore('_', room.env)
                if (envPrefix.isNotBlank() && envPrefix != room.env) {
                    room.copy(env = envPrefix)
                } else {
                    room
                }
            }
            val players = worldAssets.loadCharacters()

            roomsById = rooms.associateBy { it.id }
            initializeRoomStates(rooms)
            initializeUnlockedDirections(rooms)
            initializeGroundItems(rooms)
            val player = players.firstOrNull()
            val initialWorld = worlds.firstOrNull()
            val initialHub = hubs.firstOrNull()
            val initialRoom = rooms.find { it.id == "living" } ?: rooms.firstOrNull()

            sessionStore.setWorld(initialWorld?.id)
            sessionStore.setHub(initialHub?.id)
            sessionStore.setRoom(initialRoom?.id)
            sessionStore.setPlayer(player?.id)

            val sessionState = sessionStore.state.value
            _uiState.update {
                ExplorationUiState(
                    isLoading = false,
                    currentWorld = initialWorld,
                    currentHub = initialHub,
                    currentRoom = initialRoom,
                    player = player,
                    availableConnections = initialRoom?.connections.orEmpty(),
                    npcs = initialRoom?.npcs.orEmpty(),
                    actions = parseActions(initialRoom),
                    enemies = initialRoom?.enemies.orEmpty(),
                    blockedDirections = computeBlockedDirections(initialRoom),
                    roomState = getRoomStateSnapshot(initialRoom?.id),
                    groundItems = getGroundItemsSnapshot(initialRoom?.id),
                    activeQuests = sessionState.activeQuests,
                    completedQuests = sessionState.completedQuests,
                    completedMilestones = sessionState.completedMilestones
                )
            }
            initialRoom?.let { eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(it.id)) }
        }
    }

    fun travel(direction: String) {
        val currentRoom = _uiState.value.currentRoom ?: return
        val evaluation = evaluateDirection(currentRoom, direction, DirectionEvaluationMode.ATTEMPT)
        if (evaluation.blocked) {
            evaluation.message?.let { postStatus(it) }
            return
        }
        evaluation.message?.let { postStatus(it) }

        val nextRoomId = getConnection(currentRoom, direction) ?: return
        val nextRoom = roomsById[nextRoomId] ?: return

        sessionStore.setRoom(nextRoom.id)

        val sessionState = sessionStore.state.value
        _uiState.update {
            it.copy(
                currentRoom = nextRoom,
                availableConnections = nextRoom.connections,
                npcs = nextRoom.npcs,
                actions = parseActions(nextRoom),
                enemies = nextRoom.enemies,
                blockedDirections = computeBlockedDirections(nextRoom),
                roomState = getRoomStateSnapshot(nextRoom.id),
                groundItems = getGroundItemsSnapshot(nextRoom.id),
                activeDialogue = null,
                activeQuests = sessionState.activeQuests,
                completedQuests = sessionState.completedQuests,
                statusMessage = null
            )
        }
        activeDialogueSession = null
        eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(nextRoom.id))
    }

    fun onNpcInteraction(npcName: String) {
        val session = dialogueService.startDialogue(npcName)
        activeDialogueSession = session
        _uiState.update {
            it.copy(
                activeDialogue = session?.current(),
                statusMessage = session?.let { null } ?: "No dialogue available for $npcName yet."
            )
        }
        eventManager.handleTrigger("talk_to", EventPayload.TalkTo(npcName))
        eventManager.handleTrigger("npc_interaction", EventPayload.TalkTo(npcName))
    }

    fun advanceDialogue() {
        val session = activeDialogueSession ?: return
        val nextLine = session.advance()
        if (nextLine == null) {
            activeDialogueSession = null
            _uiState.update { it.copy(activeDialogue = null) }
        } else {
            _uiState.update { it.copy(activeDialogue = nextLine) }
        }
    }

    fun onActionSelected(action: RoomAction) {
        when (action) {
            is ToggleAction -> {
                val isActive = _uiState.value.roomState[action.stateKey] ?: false
                val eventId = if (isActive) {
                    action.actionEventOff?.takeIf { it.isNotBlank() }
                } else {
                    action.actionEventOn?.takeIf { it.isNotBlank() }
                }
                if (eventId != null) {
                    val event = eventsById[eventId]
                    val toggledOn = !isActive
                    val message = when {
                        toggledOn -> event?.onMessage
                        else -> event?.offMessage
                    } ?: action.popupTitle?.takeIf { it.isNotBlank() }
                        ?: if (toggledOn) action.labelOff else action.labelOn
                    pendingStateMessage = message
                    suppressNextStateMessage = true
                    triggerPlayerAction(eventId)
                } else {
                    val fallback = action.popupTitle?.takeIf { it.isNotBlank() }
                        ?: if (isActive) action.labelOff else action.labelOn
                    postStatus(fallback)
                }
            }
            is ContainerAction -> handleContainerAction(action)
            is TinkeringAction -> {
                postStatus("Tinkering station ${action.name} not yet implemented")
            }
            is GenericAction -> {
                val status = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
                    ?: "Triggered ${action.name}"
                postStatus(status)
                triggerPlayerAction(action.actionEvent?.takeIf { it.isNotBlank() })
            }
        }
    }

    fun clearStatusMessage() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    fun dismissTutorial() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(tutorialPrompt = null) }
        }
    }

    fun dismissMilestonePrompt() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(milestonePrompt = null) }
        }
    }

    fun dismissNarration() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(narrationPrompt = null) }
        }
    }

    fun openQuestLog() {
        viewModelScope.launch(dispatchers.main) {
            val active = _uiState.value.activeQuests
            val completed = _uiState.value.completedQuests
            active.forEach { ensureQuestDefaults(it) }
            val summaries = buildQuestSummaries(active, completed)
            _uiState.update {
                it.copy(
                    isQuestLogVisible = true,
                    questLogActive = summaries.first,
                    questLogCompleted = summaries.second
                )
            }
        }
    }

    fun closeQuestLog() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(
                    isQuestLogVisible = false,
                    questLogActive = emptyList(),
                    questLogCompleted = emptyList()
                )
            }
        }
    }

    fun engageEnemy(enemyId: String) {
        emitEvent(ExplorationEvent.EnterCombat(enemyId))
    }

    fun itemDisplayName(itemId: String): String = inventoryService.itemDisplayName(itemId)

    fun useInventoryItem(itemId: String) {
        val result = inventoryService.useItem(itemId)
        if (result == null) {
            postStatus("You don't have $itemId")
            return
        }
        val message = when (result) {
            is ItemUseResult.None -> "Used ${result.item.name}"
            is ItemUseResult.Restore -> {
                val parts = mutableListOf<String>()
                if (result.hp > 0) parts.add("${result.hp} HP")
                if (result.rp > 0) parts.add("${result.rp} RP")
                if (parts.isEmpty()) "Used ${result.item.name}" else "Restored ${parts.joinToString(" and ")}"
            }
            is ItemUseResult.Damage -> "${result.item.name} deals ${result.amount} damage (not yet implemented)"
            is ItemUseResult.Buff -> {
                val buffs = result.buffs.joinToString { "${it.stat}+${it.value}" }
                "Buffs applied: $buffs"
            }
            is ItemUseResult.LearnSchematic -> {
                val learned = craftingService.learnSchematic(result.schematicId)
                if (learned) {
                    "Learned schematic ${result.schematicId}"
                } else {
                    "Already know schematic ${result.schematicId}"
                }
            }
        }
        postStatus(message)
        emitEvent(ExplorationEvent.ItemUsed(result, message))
    }

    fun collectGroundItem(itemId: String) {
        val roomId = _uiState.value.currentRoom?.id ?: return
        val items = roomGroundItems[roomId] ?: return
        val quantity = items[itemId] ?: return
        inventoryService.addItem(itemId, quantity)
        val displayName = inventoryService.itemDisplayName(itemId)
        val message = if (quantity > 1) {
            "Collected $quantity × $displayName."
        } else {
            "Collected $displayName."
        }
        postStatus(message)
        items.remove(itemId)
        if (items.isEmpty()) {
            roomGroundItems.remove(roomId)
        }
        _uiState.update {
            it.copy(groundItems = getGroundItemsSnapshot(roomId))
        }
        emitEvent(ExplorationEvent.ItemGranted(displayName, quantity))
    }

    fun collectAllGroundItems() {
        val roomId = _uiState.value.currentRoom?.id ?: return
        val items = roomGroundItems[roomId] ?: return
        if (items.isEmpty()) return
        val collectedMessages = mutableListOf<String>()
        items.entries.toList().forEach { (itemId, quantity) ->
            inventoryService.addItem(itemId, quantity)
            val displayName = inventoryService.itemDisplayName(itemId)
            collectedMessages += if (quantity > 1) "$quantity × $displayName" else displayName
            emitEvent(ExplorationEvent.ItemGranted(displayName, quantity))
        }
        items.clear()
        roomGroundItems.remove(roomId)
        _uiState.update {
            it.copy(groundItems = emptyMap())
        }
        if (collectedMessages.isNotEmpty()) {
            postStatus("Collected ${collectedMessages.joinToString(", ")}.")
        }
    }

    private fun initializeRoomStates(rooms: List<Room>) {
        rooms.forEach { room ->
            val stateSnapshot = roomStates.getOrPut(room.id) { mutableMapOf() }
            room.state.forEach { (key, value) ->
                if (key.isNotBlank()) {
                    booleanValueOf(value)?.let { stateSnapshot.putIfAbsent(key, it) }
                }
            }
            room.dark?.let { stateSnapshot.putIfAbsent("dark", it) }
        }
    }

    private fun initializeGroundItems(rooms: List<Room>) {
        rooms.forEach { room ->
            if (room.items.isNotEmpty()) {
                val counts = roomGroundItems.getOrPut(room.id) { mutableMapOf() }
                room.items.forEach { itemId ->
                    if (itemId.isNotBlank()) {
                        counts[itemId] = (counts[itemId] ?: 0) + 1
                    }
                }
            }
        }
    }

    private fun initializeUnlockedDirections(rooms: List<Room>) {
        rooms.forEach { room ->
            reevaluateBlockedDirections(room.id, silent = true)
        }
    }

    private fun getRoomStateSnapshot(roomId: String?): Map<String, Boolean> =
        roomId?.let { id -> roomStates[id]?.toMap() }.orEmpty()

    private fun getGroundItemsSnapshot(roomId: String?): Map<String, Int> =
        roomId?.let { id -> roomGroundItems[id]?.filterValues { it > 0 }?.toMap() }.orEmpty()

    private fun setRoomStateValue(
        roomIdOrNull: String?,
        stateKey: String?,
        value: Boolean
    ): Boolean? {
        val stateKeySafe = stateKey?.takeUnless { it.isBlank() } ?: return null
        val roomId = roomIdOrNull ?: _uiState.value.currentRoom?.id ?: return null
        val states = roomStates.getOrPut(roomId) { mutableMapOf() }
        states[stateKeySafe] = value
        updateRoomModelState(roomId, stateKeySafe, value)
        reevaluateBlockedDirections(roomId, silent = true)
        emitStateStatusIfNeeded(stateKeySafe, value)
        return value
    }

    private fun toggleRoomStateValue(
        roomIdOrNull: String?,
        stateKey: String?
    ): Boolean? {
        val stateKeySafe = stateKey?.takeUnless { it.isBlank() } ?: return null
        val roomId = roomIdOrNull ?: _uiState.value.currentRoom?.id ?: return null
        val states = roomStates.getOrPut(roomId) { mutableMapOf() }
        val newValue = !(states[stateKeySafe] ?: false)
        states[stateKeySafe] = newValue
        updateRoomModelState(roomId, stateKeySafe, newValue)
        reevaluateBlockedDirections(roomId, silent = true)
        emitStateStatusIfNeeded(stateKeySafe, newValue)
        return newValue
    }

    private fun updateRoomModelState(roomId: String, stateKey: String, value: Boolean) {
        val room = roomsById[roomId] ?: return
        val updatedState = room.state.toMutableMap()
        updatedState[stateKey] = value
        val updatedRoom = room.copy(state = updatedState)
        roomsById = roomsById.toMutableMap().apply { put(roomId, updatedRoom) }
        if (_uiState.value.currentRoom?.id == roomId) {
            val snapshot = roomStates[roomId]?.toMap().orEmpty()
            _uiState.update {
                it.copy(
                    currentRoom = updatedRoom,
                    roomState = snapshot,
                    blockedDirections = computeBlockedDirections(updatedRoom)
                )
            }
        }
    }

    private fun booleanValueOf(value: Any?): Boolean? = when (value) {
        null -> null
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> {
            val normalized = value.trim().lowercase(Locale.getDefault())
            when (normalized) {
                "true", "yes", "on", "enabled" -> true
                "false", "no", "off", "disabled" -> false
                else -> normalized.toIntOrNull()?.let { it != 0 }
            }
        }
        else -> null
    }

    private fun formatRoomStateStatus(stateKey: String, value: Boolean): String {
        val label = formatStateKey(stateKey)
        val stateText = if (value) "enabled" else "disabled"
        return "$label $stateText"
    }

    private fun formatStateKey(stateKey: String): String =
        stateKey.replace('_', ' ').replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }

    private fun formatMilestoneMessage(milestone: String): String {
        milestoneMessages[milestone]?.let { return it }
        val cleaned = milestone.removePrefix("ms_").removePrefix("MS_")
        val label = cleaned.replace('_', ' ').replace('-', ' ').replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
        return "$label milestone updated"
    }

    private fun emitStateStatusIfNeeded(stateKey: String, value: Boolean) {
        val pending = pendingStateMessage
        if (pending != null) {
            pendingStateMessage = null
            suppressNextStateMessage = false
            postStatus(pending)
            return
        }
        if (suppressNextStateMessage) {
            suppressNextStateMessage = false
            return
        }
        postStatus(formatRoomStateStatus(stateKey, value))
    }

    private fun updateQuestSummaries(active: Set<String>, completed: Set<String>) {
        val (activeSummaries, completedSummaries) = buildQuestSummaries(active, completed)
        _uiState.update {
            if (it.isQuestLogVisible) {
                it.copy(
                    questLogActive = activeSummaries,
                    questLogCompleted = completedSummaries
                )
            } else it
        }
    }

    private fun buildQuestSummaries(
        active: Set<String>,
        completed: Set<String>
    ): Pair<List<QuestSummaryUi>, List<QuestSummaryUi>> {
        val activeSummaries = active.mapNotNull { id ->
            questRepository.questById(id)?.let { quest ->
                val stageId = questStageProgress[id] ?: quest.stages.firstOrNull()?.id
                val stage = quest.stages.firstOrNull { it.id == stageId } ?: quest.stages.firstOrNull()
                val completedTasks = questTaskProgress[id].orEmpty()
                QuestSummaryUi(
                    id = quest.id,
                    title = quest.title,
                    summary = quest.summary,
                    stageTitle = stage?.title,
                    stageDescription = stage?.description,
                    objectives = stage?.tasks?.map { task ->
                        val status = if (completedTasks.contains(task.id)) "✓" else "○"
                        "$status ${task.text}"
                    }.orEmpty(),
                    completed = false
                )
            }
        }
        val completedSummaries = completed.mapNotNull { id ->
            questRepository.questById(id)?.let { quest ->
                QuestSummaryUi(
                    id = quest.id,
                    title = quest.title,
                    summary = quest.summary,
                    stageTitle = quest.stages.lastOrNull()?.title,
                    stageDescription = quest.stages.lastOrNull()?.description,
                    objectives = quest.stages.lastOrNull()?.tasks?.map { "✓ ${it.text}" }.orEmpty(),
                    completed = true
                )
            }
        }
        return activeSummaries to completedSummaries
    }

    private fun ensureQuestDefaults(questId: String) {
        if (!questStageProgress.containsKey(questId)) {
            questRepository.questById(questId)?.stages?.firstOrNull()?.let { stage ->
                questStageProgress[questId] = stage.id
            }
        }
    }

    private fun markQuestTaskComplete(questId: String, taskId: String) {
        val progress = questTaskProgress.getOrPut(questId) { mutableSetOf() }
        if (progress.add(taskId) && _uiState.value.isQuestLogVisible) {
            updateQuestSummaries(_uiState.value.activeQuests, _uiState.value.completedQuests)
        }
    }

    private fun resetQuestProgress(questId: String) {
        questTaskProgress.remove(questId)
        questRepository.questById(questId)?.stages?.firstOrNull()?.let { stage ->
            questStageProgress[questId] = stage.id
        }
        if (_uiState.value.isQuestLogVisible) {
            updateQuestSummaries(_uiState.value.activeQuests, _uiState.value.completedQuests)
        }
    }

    private fun markQuestCompleted(questId: String) {
        questRepository.questById(questId)?.let { quest ->
            questStageProgress[questId] = quest.stages.lastOrNull()?.id ?: questStageProgress[questId].orEmpty()
            val progress = questTaskProgress.getOrPut(questId) { mutableSetOf() }
            quest.stages.flatMap { it.tasks }.forEach { progress.add(it.id) }
        }
        if (_uiState.value.isQuestLogVisible) {
            updateQuestSummaries(_uiState.value.activeQuests, _uiState.value.completedQuests)
        }
    }

    private fun handleGroundItemSpawn(roomId: String?, itemId: String, quantity: Int) {
        val targetRoomId = roomId ?: _uiState.value.currentRoom?.id ?: return
        val normalizedQuantity = quantity.coerceAtLeast(1)
        val items = roomGroundItems.getOrPut(targetRoomId) { mutableMapOf() }
        items[itemId] = (items[itemId] ?: 0) + normalizedQuantity

        val message = if (normalizedQuantity > 1) {
            "${inventoryService.itemDisplayName(itemId)} ×$normalizedQuantity has appeared nearby."
        } else {
            "${inventoryService.itemDisplayName(itemId)} has appeared nearby."
        }
        postStatus(message)

        if (_uiState.value.currentRoom?.id == targetRoomId) {
            _uiState.update {
                it.copy(groundItems = getGroundItemsSnapshot(targetRoomId))
            }
        }

        emitEvent(ExplorationEvent.GroundItemSpawned(targetRoomId, itemId, normalizedQuantity, message))
    }

    private fun showTutorial(sceneId: String?, context: String?) {
        val message = buildString {
            append("Tutorial")
            sceneId?.takeIf { it.isNotBlank() }?.let {
                append(": ")
                append(it.replace('_', ' ').replaceFirstChar { c ->
                    if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
                })
            }
            context?.takeIf { it.isNotBlank() }?.let {
                append("\n")
                append(it)
            }
        }.ifBlank { "Tutorial available" }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(tutorialPrompt = TutorialPrompt(sceneId, context, message))
            }
        }
    }

    private fun showMilestone(milestoneId: String, message: String) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(milestonePrompt = MilestonePrompt(milestoneId, message))
            }
        }
    }

    private fun showNarration(message: String, tapToDismiss: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(narrationPrompt = NarrationPrompt(message, tapToDismiss))
            }
        }
    }

    private fun handleContainerAction(action: ContainerAction) {
        val roomId = _uiState.value.currentRoom?.id ?: return
        val stateKey = action.stateKey?.takeUnless { it.isBlank() }
        val alreadyOpen = stateKey?.let { _uiState.value.roomState[it] == true } ?: false
        if (alreadyOpen) {
            val message = action.alreadyOpenMessage ?: "You've already searched ${action.name}."
            postStatus(message)
            return
        }

        stateKey?.let { setRoomStateValue(roomId, it, true) }

        val grantedItems = action.items.mapNotNull { itemId ->
            if (itemId.isBlank()) return@mapNotNull null
            inventoryService.addItem(itemId, 1)
            val displayName = inventoryService.itemDisplayName(itemId)
            emitEvent(ExplorationEvent.ItemGranted(displayName, 1))
            displayName
        }

        val message = when {
            grantedItems.isEmpty() -> action.popupTitle ?: "The ${action.name.lowercase(Locale.getDefault())} is empty."
            grantedItems.size == 1 -> "Found ${grantedItems.first()}."
            else -> {
                val prefix = grantedItems.dropLast(1).joinToString(", ")
                val suffix = grantedItems.last()
                "Found $prefix and $suffix."
            }
        }
        postStatus(message)

        triggerPlayerAction(action.actionEvent?.takeIf { it.isNotBlank() })
    }

    private fun refreshCurrentRoomBlockedDirections() {
        val room = _uiState.value.currentRoom ?: return
        val blocked = computeBlockedDirections(room)
        _uiState.update { it.copy(blockedDirections = blocked) }
    }

    private fun computeBlockedDirections(room: Room?): Set<String> {
        if (room == null) return emptySet()
        return room.connections.keys.mapNotNull { direction ->
            val evaluation = evaluateDirection(room, direction, DirectionEvaluationMode.CHECK)
            if (evaluation.blocked) direction.lowercase(Locale.getDefault()) else null
        }.toSet()
    }

    private fun reevaluateBlockedDirections(roomId: String, silent: Boolean) {
        val room = roomsById[roomId] ?: return
        val blocks = room.blockedDirections ?: return
        blocks.forEach { (direction, block) ->
            val normalized = direction.lowercase(Locale.getDefault())
            if (isDirectionUnlocked(roomId, normalized)) return@forEach
            when (block.type.lowercase(Locale.getDefault())) {
                "lock" -> {
                    val requirementsMet = requirementsMet(block.requires)
                    val needsKey = !block.keyId.isNullOrBlank()
                    if (requirementsMet && !needsKey) {
                        unlockDirection(roomId, normalized, block, silent)
                    }
                }
                "enemy" -> {
                    if (!hasBlockingEnemies(room, normalized)) {
                        unlockDirection(roomId, normalized, block, true)
                    }
                }
            }
        }
        if (_uiState.value.currentRoom?.id == roomId) {
            val currentRoom = roomsById[roomId]
            val blocked = computeBlockedDirections(currentRoom)
            _uiState.update {
                it.copy(
                    currentRoom = currentRoom ?: it.currentRoom,
                    blockedDirections = blocked,
                    roomState = getRoomStateSnapshot(roomId)
                )
            }
        }
    }

    private enum class DirectionEvaluationMode { CHECK, ATTEMPT }

    private data class DirectionEvaluation(
        val blocked: Boolean,
        val message: String? = null
    )

    private fun evaluateDirection(
        room: Room,
        direction: String,
        mode: DirectionEvaluationMode
    ): DirectionEvaluation {
        val normalized = direction.lowercase(Locale.getDefault())
        if (isDirectionUnlocked(room.id, normalized)) {
            return DirectionEvaluation(blocked = false)
        }
        val block = room.blockedDirections?.get(normalized)
        if (block == null) {
            return DirectionEvaluation(blocked = false)
        }
        return when (block.type.lowercase(Locale.getDefault())) {
            "enemy" -> {
                val hasEnemies = hasBlockingEnemies(room, normalized)
                if (hasEnemies) {
                    DirectionEvaluation(true, block.messageLocked ?: "Enemies block the path.")
                } else {
                    if (mode == DirectionEvaluationMode.ATTEMPT) {
                        unlockDirection(room.id, normalized, block, silent = true)
                    }
                    DirectionEvaluation(blocked = false)
                }
            }
            "lock" -> {
                if (!requirementsMet(block.requires)) {
                    return DirectionEvaluation(true, block.messageLocked ?: "It won't budge.")
                }
                val keyId = block.keyId?.takeIf { it.isNotBlank() }
                if (keyId != null && !inventoryService.hasItem(keyId)) {
                    val requirementMessage = block.messageLocked ?: "Requires ${inventoryService.itemDisplayName(keyId)}."
                    return DirectionEvaluation(true, requirementMessage)
                }
                if (mode == DirectionEvaluationMode.ATTEMPT) {
                    val consumedName = if (keyId != null && block.consume == true) {
                        val display = inventoryService.itemDisplayName(keyId)
                        inventoryService.removeItem(keyId)
                        display
                    } else null
                    val unlockMessage = unlockDirection(room.id, normalized, block, silent = false)
                    val message = unlockMessage ?: consumedName?.let { "Used $it" }
                    return DirectionEvaluation(blocked = false, message = message)
                }
                DirectionEvaluation(blocked = false)
            }
            else -> DirectionEvaluation(true, block.messageLocked ?: "The path is blocked.")
        }
    }

    private fun requirementsMet(requirements: List<Requirement>?): Boolean {
        if (requirements.isNullOrEmpty()) return true
        return requirements.all { requirement ->
            val roomId = requirement.roomId
            val stateKey = requirement.stateKey
            if (roomId.isBlank() || stateKey.isBlank()) return@all false
            val value = roomStates[roomId]?.get(stateKey)
                ?: roomsById[roomId]?.state?.get(stateKey)?.let { booleanValueOf(it) }
            value == requirement.value
        }
    }

    private fun hasBlockingEnemies(room: Room, direction: String): Boolean {
        if (room.enemies.isNotEmpty()) return true
        val destId = getConnection(room, direction) ?: return false
        val destRoom = roomsById[destId] ?: return false
        return destRoom.enemies.isNotEmpty()
    }

    private fun getConnection(room: Room, direction: String): String? {
        val normalized = direction.lowercase(Locale.getDefault())
        return room.connections.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.value
    }

    private fun oppositeDirection(direction: String): String? = when (direction.lowercase(Locale.getDefault())) {
        "north" -> "south"
        "south" -> "north"
        "east" -> "west"
        "west" -> "east"
        else -> null
    }

    private fun isDirectionUnlocked(roomId: String, direction: String): Boolean {
        val normalized = direction.lowercase(Locale.getDefault())
        return unlockedDirections[roomId]?.contains(normalized) == true
    }

    private fun unlockDirection(
        roomId: String,
        direction: String,
        block: BlockedDirection?,
        silent: Boolean
    ): String? {
        val normalized = direction.lowercase(Locale.getDefault())
        val unlocked = unlockedDirections.getOrPut(roomId) { mutableSetOf() }
        if (!unlocked.add(normalized)) {
            return null
        }

        val message = block?.messageUnlock?.takeIf { !silent && !it.isNullOrBlank() }

        val currentRoom = roomsById[roomId]
        if (currentRoom != null) {
            val updatedBlocked = currentRoom.blockedDirections?.toMutableMap()?.apply { remove(normalized) }
            roomsById = roomsById.toMutableMap().apply {
                put(roomId, currentRoom.copy(blockedDirections = updatedBlocked))
            }
        }

        val destId = currentRoom?.let { getConnection(it, normalized) }
        val opposite = oppositeDirection(normalized)
        if (destId != null && opposite != null) {
            unlockedDirections.getOrPut(destId) { mutableSetOf() }.add(opposite)
            val destRoom = roomsById[destId]
            if (destRoom != null) {
                val updatedBlocked = destRoom.blockedDirections?.toMutableMap()?.apply { remove(opposite) }
                roomsById = roomsById.toMutableMap().apply {
                    put(destId, destRoom.copy(blockedDirections = updatedBlocked))
                }
            }
        }

        if (_uiState.value.currentRoom?.id == roomId) {
            val updatedRoom = roomsById[roomId]
            val blocked = computeBlockedDirections(updatedRoom)
            _uiState.update {
                it.copy(
                    currentRoom = updatedRoom ?: it.currentRoom,
                    blockedDirections = blocked
                )
            }
        }

        return message
    }

    private fun parseActions(room: Room?): List<RoomAction> {
        if (room == null) return emptyList()
        return room.actions.mapNotNull { action ->
            val type = action["type"].asStringOrNull()?.lowercase()
            val name = action["name"].asStringOrNull() ?: return@mapNotNull null
            when (type) {
                "toggle" -> ToggleAction(
                    name = name,
                    stateKey = action["state_key"].asStringOrNull().orEmpty(),
                    popupTitle = action["popup_title"].asStringOrNull(),
                    labelOn = action["label_on"].asStringOrNull() ?: "Toggle On",
                    labelOff = action["label_off"].asStringOrNull() ?: "Toggle Off",
                    actionEventOn = action["action_event_on"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    actionEventOff = action["action_event_off"].asStringOrNull()?.takeIf { it.isNotBlank() }
                )
                "container" -> ContainerAction(
                    name = name,
                    stateKey = action["state_key"].asStringOrNull(),
                    items = action["items"]?.asListOrNull() ?: emptyList(),
                    actionEvent = action["action_event"].asStringOrNull(),
                    alreadyOpenMessage = action["already_open_message"].asStringOrNull(),
                    popupTitle = action["popup_title"].asStringOrNull()
                )
                "tinkering" -> TinkeringAction(
                    name = name,
                    shopId = action["shop_id"].asStringOrNull()?.takeIf { it.isNotBlank() }
                )
                else -> GenericAction(
                    name = name,
                    type = type ?: "generic",
                    actionEvent = action["action_event"].asStringOrNull(),
                    zoneId = action["zone_id"].asStringOrNull(),
                    conditionUnmetMessage = action["condition_unmet_message"].asStringOrNull()
                )
            }
        }
    }
}

sealed interface ExplorationEvent {
    data class EnterCombat(val enemyId: String) : ExplorationEvent
    data class PlayCinematic(val sceneId: String) : ExplorationEvent
    data class ShowMessage(val message: String) : ExplorationEvent
    data class RewardGranted(val reward: EventReward) : ExplorationEvent
    data class ItemGranted(val itemName: String, val quantity: Int) : ExplorationEvent
    data class XpGained(val amount: Int) : ExplorationEvent
    data class QuestAdvanced(val questId: String?) : ExplorationEvent
    data class QuestUpdated(val active: Set<String>, val completed: Set<String>) : ExplorationEvent
    data class RoomStateChanged(val roomId: String?, val stateKey: String, val value: Boolean) : ExplorationEvent
    data class SpawnEncounter(val encounterId: String?, val roomId: String?) : ExplorationEvent
    data class BeginNode(val roomId: String?) : ExplorationEvent
    data class TutorialRequested(val sceneId: String?, val context: String?) : ExplorationEvent
    data class GroundItemSpawned(val roomId: String?, val itemId: String, val quantity: Int, val message: String) : ExplorationEvent
    data class RoomSearchUnlocked(val roomId: String?, val note: String?) : ExplorationEvent
    data class ItemUsed(val result: ItemUseResult, val message: String? = null) : ExplorationEvent
}

private fun Any?.asStringOrNull(): String? = when (this) {
    null -> null
    is String -> this
    is Number -> toString()
    else -> this.toString()
}

private fun Any?.asListOrNull(): List<String>? = when (this) {
    is List<*> -> this.mapNotNull {
        when (it) {
            is String -> it.takeIf { value -> value.isNotBlank() }
            is Number -> it.toString()
            else -> null
        }
    }
    else -> null
}
