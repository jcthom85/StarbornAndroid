package com.example.starborn.feature.exploration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueSession
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.model.BlockedDirection
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.CookingAction
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.FirstAidAction
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Requirement
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.quest.QuestJournalEntry
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.navigation.CombatResultPayload
import java.util.Locale
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_PORTRAIT = "communicator_portrait"
private const val MILESTONE_BAND_LIMIT = 3

private data class ShopDialogueSession(
    val shopId: String,
    val baseLines: List<ShopDialogueLineUi>,
    val topics: Map<String, ShopDialogueTopicState>,
    val tradeLabel: String,
    val leaveLabel: String,
    val visitedTopics: MutableSet<String> = mutableSetOf()
)

private data class ShopDialogueTopicState(
    val id: String,
    val label: String,
    val responseLines: List<ShopDialogueLineUi>,
    val voiceCue: String?
)

class ExplorationViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val sessionStore: GameSessionStore,
    private val dialogueService: DialogueService,
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val cinematicService: CinematicService,
    private val questRepository: QuestRepository,
    private val questRuntimeManager: QuestRuntimeManager,
    private val milestoneManager: MilestoneRuntimeManager,
    private val audioRouter: AudioRouter,
    private val shopRepository: ShopRepository,
    private val levelingManager: LevelingManager,
    private val tutorialManager: TutorialRuntimeManager,
    private val promptManager: UIPromptManager,
    private val fishingService: FishingService,
    eventDefinitions: List<GameEvent>,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val eventsById: Map<String, GameEvent> = eventDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(ExplorationUiState())
    val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorationEvent>()
    val events: SharedFlow<ExplorationEvent> = _events.asSharedFlow()

    private var charactersById: Map<String, Player> = emptyMap()
    private var skillsById: Map<String, Skill> = emptyMap()

    init {
        inventoryService.loadItems()
    }

    private val eventManager = EventManager(
        events = eventDefinitions,
        sessionStore = sessionStore,
        eventHooks = EventHooks(
            onPlayCinematic = { sceneId ->
                startCinematic(sceneId)
                emitEvent(ExplorationEvent.PlayCinematic(sceneId))
            },
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
                    questRuntimeManager.markTaskComplete(questId, taskId)
                }
            },
            onQuestStageAdvanced = { questId, stageId ->
                if (!questId.isNullOrEmpty() && !stageId.isNullOrEmpty()) {
                    questRuntimeManager.setStage(questId, stageId)
                }
            },
            onQuestStarted = { questId ->
                questId?.let { questRuntimeManager.resetQuest(it) }
            },
            onQuestCompleted = { questId ->
                questId?.let { questRuntimeManager.markQuestCompleted(it) }
            },
            onQuestFailed = { questId, reason ->
                questId?.let { questRuntimeManager.markQuestFailed(it, reason) }
            },
            onBeginNode = { roomId ->
                roomId?.let { roomsById[it]?.let { room -> markDiscovered(room) } }
                emitEvent(ExplorationEvent.BeginNode(roomId))
            },
            onSystemTutorial = { sceneId, context ->
                val message = buildString {
                    append("Tutorial")
                    sceneId?.takeIf { it.isNotBlank() }?.let {
                        append(": ")
                        append(
                            it.replace('_', ' ').replaceFirstChar { c ->
                                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
                            }
                        )
                    }
                    context?.takeIf { it.isNotBlank() }?.let {
                        append("\n")
                        append(it)
                    }
                }.ifBlank { "Tutorial available" }.trim()

                val key = sceneId?.takeIf { it.isNotBlank() } ?: message
                tutorialManager.showOnce(
                    key = key,
                    message = message,
                    context = context,
                    metadata = mapOf("source" to "system")
                )
                emitEvent(ExplorationEvent.TutorialRequested(sceneId, context))
            },
            onMilestoneSet = { milestone ->
                val message = formatMilestoneMessage(milestone)
                milestoneManager.handleMilestone(milestone, message)
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
    private val visitedRooms: MutableSet<String> = mutableSetOf()
    private val discoveredRooms: MutableSet<String> = mutableSetOf()
    private val blockedCinematicsShown: MutableSet<String> = mutableSetOf()
    private var roomsByPosition: Map<Pair<Int, Int>, Room> = emptyMap()
    private val portraitBySpeaker: MutableMap<String, String> = mutableMapOf()
    private val portraitOverrides: Map<String, String> = mapOf(
        "pasha" to DEFAULT_PORTRAIT,
        "jed" to DEFAULT_PORTRAIT,
        "maddie" to DEFAULT_PORTRAIT,
        "ollie" to "ollie_portrait"
    )
    private var activeShopDialogue: ShopDialogueSession? = null
    private var lastDialogueVoiceCue: String? = null
    private var pendingStateMessage: String? = null
    private var suppressNextStateMessage: Boolean = false
    private val levelUpQueue: ArrayDeque<LevelUpPrompt> = ArrayDeque()
    private val cinematicQueue: ArrayDeque<CinematicScene> = ArrayDeque()
    private var activeCinematic: CinematicScene? = null
    private var cinematicStepIndex: Int = 0
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
    private val acknowledgedMilestones: MutableSet<String> = mutableSetOf()
    private val unlockedDirections: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private var activeDialogueSession: DialogueSession? = null

    private fun emitEvent(event: ExplorationEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    private fun emitAudioCommands(commands: List<AudioCommand>) {
        if (commands.isEmpty()) return
        emitEvent(ExplorationEvent.AudioCommands(commands))
    }

    private fun playRoomAudio(hubId: String?, roomId: String?) {
        emitAudioCommands(audioRouter.commandsForRoom(hubId, roomId))
    }

    private fun playUiCue(key: String) {
        emitAudioCommands(audioRouter.commandsForUi(key))
    }

    fun onCombatVictory(result: CombatResultPayload) {
        val enemyIds = result.enemyIds
        if (enemyIds.isEmpty()) return
        val currentRoom = _uiState.value.currentRoom
        if (currentRoom != null && currentRoom.enemies.isNotEmpty()) {
            val remaining = currentRoom.enemies.filterNot { it in enemyIds }
            if (remaining.size != currentRoom.enemies.size) {
                val updatedRoom = currentRoom.copy(enemies = remaining)
                roomsById = roomsById + (updatedRoom.id to updatedRoom)
                _uiState.update {
                    it.copy(
                        currentRoom = updatedRoom,
                        enemies = remaining
                    )
                }
            }
        }
        eventManager.handleTrigger(
            type = "encounter_victory",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        refreshCurrentRoomBlockedDirections()
        val rewardParts = mutableListOf<String>()
        if (result.rewardXp > 0) rewardParts += "${result.rewardXp} XP"
        if (result.rewardAp > 0) rewardParts += "${result.rewardAp} AP"
        if (result.rewardCredits > 0) rewardParts += "${result.rewardCredits} credits"
        result.rewardItems.forEach { (itemId, quantity) ->
            val name = inventoryService.itemDisplayName(itemId)
            rewardParts += "$quantity x $name"
        }
        val outcomeMessage = if (rewardParts.isNotEmpty()) {
            "Victory reward: ${rewardParts.joinToString(", ")}"
        } else {
            "Encounter cleared."
        }
        postStatus(outcomeMessage)
        enqueueLevelUps(result.levelUps)
        showCombatOutcome(
            outcome = CombatResultPayload.Outcome.VICTORY,
            enemyIds = enemyIds,
            message = outcomeMessage
        )
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.VICTORY,
                enemyIds = enemyIds,
                message = outcomeMessage
            )
        )
        playRoomAudio(sessionStore.state.value.hubId, _uiState.value.currentRoom?.id)
    }

    fun onCombatDefeat(enemyIds: List<String>) {
        eventManager.handleTrigger(
            type = "encounter_defeat",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.DEFEAT
            )
        )
        val message = "Overwhelmed by the enemy. Regroup and recover."
        postStatus(message)
        refreshCurrentRoomBlockedDirections()
        showCombatOutcome(
            outcome = CombatResultPayload.Outcome.DEFEAT,
            enemyIds = enemyIds,
            message = message
        )
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.DEFEAT,
                enemyIds = enemyIds,
                message = message
            )
        )
        playRoomAudio(sessionStore.state.value.hubId, _uiState.value.currentRoom?.id)
    }

    fun onCombatRetreat(enemyIds: List<String>) {
        eventManager.handleTrigger(
            type = "encounter_retreat",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.RETREAT
            )
        )
        val message = "Retreated from combat."
        postStatus(message)
        refreshCurrentRoomBlockedDirections()
        showCombatOutcome(
            outcome = CombatResultPayload.Outcome.RETREAT,
            enemyIds = enemyIds,
            message = message
        )
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.RETREAT,
                enemyIds = enemyIds,
                message = message
            )
        )
        playRoomAudio(sessionStore.state.value.hubId, _uiState.value.currentRoom?.id)
    }

    private fun postStatus(message: String) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(statusMessage = message) }
        }
    }

    private fun enqueueLevelUps(summaries: List<LevelUpSummary>) {
        if (summaries.isEmpty()) return
        summaries.forEach { summary ->
            levelUpQueue.add(
                buildLevelUpPrompt(
                    summary = summary,
                    charactersById = charactersById,
                    skillsById = skillsById
                )
            )
        }
        if (_uiState.value.levelUpPrompt == null) {
            promoteNextLevelUp()
        }
    }

    private fun promoteNextLevelUp() {
        val next = if (levelUpQueue.isEmpty()) null else levelUpQueue.removeFirst()
        _uiState.update { it.copy(levelUpPrompt = next) }
    }

    fun dismissLevelUpPrompt() {
        promoteNextLevelUp()
    }

    private fun showCombatOutcome(
        outcome: CombatResultPayload.Outcome,
        enemyIds: List<String>,
        message: String
    ) {
        _uiState.update {
            it.copy(
                combatOutcome = CombatOutcomeUi(
                    outcome = outcome,
                    enemyIds = enemyIds,
                    message = message
                )
            )
        }
    }

    fun dismissCombatOutcome() {
        _uiState.update { it.copy(combatOutcome = null) }
    }

    private fun startCinematic(sceneId: String?) {
        val scene = cinematicService.scene(sceneId)
        if (scene == null || scene.steps.isEmpty()) {
            if (!sceneId.isNullOrBlank()) {
                postStatus("Cinematic $sceneId is not available yet.")
            }
            return
        }
        if (activeCinematic == null) {
            activeCinematic = scene
            cinematicStepIndex = 0
            pushCinematicStep(scene, 0)
        } else {
            cinematicQueue.add(scene)
        }
    }

    fun advanceCinematic() {
        val scene = activeCinematic
        if (scene == null) {
            if (cinematicQueue.isNotEmpty()) {
                promoteNextCinematicScene()
            }
            return
        }
        val nextIndex = cinematicStepIndex + 1
        if (nextIndex >= scene.steps.size) {
            promoteNextCinematicScene()
        } else {
            cinematicStepIndex = nextIndex
            pushCinematicStep(scene, nextIndex)
        }
    }

    private fun promoteNextCinematicScene() {
        val nextScene = if (cinematicQueue.isEmpty()) null else cinematicQueue.removeFirst()
        if (nextScene == null) {
            activeCinematic = null
            cinematicStepIndex = 0
            _uiState.update { it.copy(cinematic = null) }
        } else {
            activeCinematic = nextScene
            cinematicStepIndex = 0
            pushCinematicStep(nextScene, 0)
        }
    }

    private fun pushCinematicStep(scene: CinematicScene, index: Int) {
        val step = scene.steps.getOrNull(index) ?: return
        val stepUi = CinematicStepUi(
            type = step.type,
            speaker = step.speaker,
            text = step.text
        )
        _uiState.update {
            it.copy(
                cinematic = CinematicUiState(
                    sceneId = scene.id,
                    title = scene.title,
                    stepIndex = index,
                    stepCount = scene.steps.size,
                    step = stepUi
                )
            )
        }
    }

    private fun updateMinimap(currentRoom: Room?) {
        currentRoom?.let { markDiscovered(it) }
        val minimap = currentRoom?.let { buildMinimapState(it) }
        _uiState.update { it.copy(minimap = minimap) }
    }

    private fun buildMinimapState(currentRoom: Room): MinimapUiState {
        val currentPos = roomPosition(currentRoom)
        val currentBlocked = computeBlockedDirections(currentRoom)
        val openConnections = currentRoom.connections.mapNotNull { (direction, targetId) ->
            val normalized = direction.lowercase(Locale.getDefault())
            val dest = targetId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (normalized in currentBlocked) return@mapNotNull null
            normalized to dest
        }.toMap()

        val cells = roomsByPosition.mapNotNull { (pos, room) ->
            val dx = pos.first - currentPos.first
            val dy = pos.second - currentPos.second
            if (abs(dx) > 1 || abs(dy) > 1) return@mapNotNull null
            val connections = room.connections.keys.map { it.lowercase(Locale.getDefault()) }.toSet()
            val blocked = computeBlockedDirections(room)
            val pathHints = when (room.id) {
                currentRoom.id -> openConnections.keys
                else -> {
                    val incoming = openConnections.entries.firstOrNull { it.value == room.id }?.key
                    incoming?.let { listOfNotNull(oppositeDirection(it)).toSet() } ?: emptySet()
                }
            }
            val services = parseActions(room).mapNotNull { action ->
                when (action) {
                    is ShopAction -> MinimapService.SHOP
                    is CookingAction -> MinimapService.COOKING
                    is FirstAidAction -> MinimapService.FIRST_AID
                    is TinkeringAction -> MinimapService.TINKERING
                    else -> null
                }
            }.toSet()
            MinimapCellUi(
                roomId = room.id,
                offsetX = dx,
                offsetY = dy,
                visited = visitedRooms.contains(room.id),
                discovered = discoveredRooms.contains(room.id),
                isCurrent = room.id == currentRoom.id,
                hasEnemies = room.enemies.isNotEmpty(),
                blockedDirections = blocked,
                connections = connections,
                pathHints = pathHints,
                services = services
            )
        }
        return MinimapUiState(cells = cells)
    }

    private fun roomPosition(room: Room): Pair<Int, Int> {
        val x = room.pos.getOrNull(0) ?: 0
        val y = room.pos.getOrNull(1) ?: 0
        return x to y
    }

    private fun buildDialogueUi(line: DialogueLine?): DialogueUi? {
        if (line == null) return null
        val portrait = resolvePortraitKey(line.speaker)
            ?: line.portrait?.takeIf { it.isNotBlank() }?.let { sanitizePortraitName(it) }
        return DialogueUi(
            line = line,
            portrait = portrait,
            voiceCue = line.voiceCue
        )
    }

    private fun handleBlockedDirectionCinematic(roomId: String, direction: String, block: BlockedDirection) {
        val key = "$roomId:$direction"
        if (!blockedCinematicsShown.add(key)) return
        val sceneId = sceneIdForBlock(block) ?: return
        startCinematic(sceneId)
    }

    private fun sceneIdForBlock(block: BlockedDirection?): String? {
        val type = block?.type?.lowercase(Locale.getDefault()) ?: return null
        return when (type) {
            "lock" -> "scene_blocked_lock"
            "enemy" -> "scene_blocked_enemy"
            else -> null
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

    private fun triggerPlayerAction(actionId: String?, itemId: String? = null) {
        if (actionId.isNullOrBlank()) return
        eventManager.handleTrigger("player_action", EventPayload.Action(actionId, itemId))
    }

    init {
        viewModelScope.launch(dispatchers.main) {
            sessionStore.state.collect { newState ->
                val partyStatus = buildPartyStatusUi(
                    sessionState = newState,
                    charactersById = charactersById,
                    levelingManager = levelingManager,
                    skillsById = skillsById
                )
                val progressionSummary = buildProgressionSummaryUi(
                    sessionState = newState,
                    charactersById = charactersById,
                    levelingManager = levelingManager
                )
                _uiState.update {
                    it.copy(
                        completedMilestones = newState.completedMilestones,
                        partyStatus = partyStatus,
                        progressionSummary = progressionSummary
                    )
                }
                updateActionHints(_uiState.value.currentRoom)
            }
        }

        viewModelScope.launch(dispatchers.main) {
            milestoneManager.history.collect { history ->
                _uiState.update { current ->
                    val bands = history.takeLast(MILESTONE_BAND_LIMIT)
                        .asReversed()
                        .map { event -> MilestoneBandUi(event.id, event.message) }
                    current.copy(
                        milestoneHistory = history,
                        milestoneBands = bands
                    )
                }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            var previousActive: Set<String> = questRuntimeManager.state.value.activeQuestIds
            var previousCompleted: Set<String> = questRuntimeManager.state.value.completedQuestIds
            var firstEmission = true
            questRuntimeManager.state.collect { questState ->
                val activeChanged = questState.activeQuestIds != previousActive
                val completedChanged = questState.completedQuestIds != previousCompleted
                _uiState.update { current ->
                    val relevantLogs = questState.recentLog.filter { entry ->
                        questState.trackedQuestId.isNullOrBlank() || entry.questId.equals(questState.trackedQuestId, ignoreCase = true)
                    }
                    var updated = current.copy(
                        activeQuests = questState.activeQuestIds,
                        completedQuests = questState.completedQuestIds,
                        failedQuests = questState.failedQuestIds,
                        trackedQuestId = questState.trackedQuestId,
                        questLogEntries = relevantLogs.map { it.toUiEntry(questRepository) },
                        questLogActive = questState.activeJournal.map { it.toUiSummary() },
                        questLogCompleted = questState.completedJournal.map { it.toUiSummary() }
                    )
                    updated
                }
                if (!firstEmission && (activeChanged || completedChanged)) {
                    emitEvent(ExplorationEvent.QuestUpdated(questState.activeQuestIds, questState.completedQuestIds))
                    postStatus("Quest log updated")
                    updateActionHints(_uiState.value.currentRoom)
                }
                previousActive = questState.activeQuestIds
                previousCompleted = questState.completedQuestIds
                firstEmission = false
            }
        }

        viewModelScope.launch(dispatchers.main) {
            promptManager.state.collect { promptState ->
                _uiState.update { it.copy(prompt = promptState.current) }
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
            val skills = worldAssets.loadSkills()
            val npcs = worldAssets.loadNpcs()

            charactersById = players.associateBy { it.id }
            skillsById = skills.associateBy { it.id }

            roomsById = rooms.associateBy { it.id }
            roomsByPosition = rooms.associateBy { roomPosition(it) }
            initializeRoomStates(rooms)
            initializeUnlockedDirections(rooms)
            initializeGroundItems(rooms)
            val player = players.firstOrNull()
            val initialWorld = worlds.firstOrNull()
            val initialHub = hubs.firstOrNull()
            val initialRoom = rooms.find { it.id == "living" } ?: rooms.firstOrNull()

            portraitBySpeaker.clear()
            players.forEach { character ->
                registerPortrait(character.miniIconPath, character.name, character.id)
            }
            players.firstOrNull()?.let { registerPortrait(it.miniIconPath, "player") }
            npcs.forEach { npc ->
                val keys = mutableListOf<String?>()
                keys += npc.name
                keys += npc.id
                keys.addAll(npc.aliases)
                keys += npc.name.substringAfterLast(' ', missingDelimiterValue = npc.name)
                keys += npc.name.substringBefore(' ', missingDelimiterValue = npc.name)
                registerPortrait(npc.portrait, *keys.toTypedArray())
            }

            visitedRooms.clear()
            discoveredRooms.clear()
            initialRoom?.let {
                visitedRooms.add(it.id)
                markDiscovered(it)
            }
            blockedCinematicsShown.clear()
            sessionStore.resetTutorialProgress()
            acknowledgedMilestones.clear()

            sessionStore.setWorld(initialWorld?.id)
            sessionStore.setHub(initialHub?.id)
            sessionStore.setRoom(initialRoom?.id)
            sessionStore.setPlayer(player?.id)
            sessionStore.setPlayerLevel(player?.level ?: 1)
            sessionStore.setPlayerXp(player?.xp ?: 0)
            sessionStore.setPartyMembers(listOfNotNull(player?.id))

            val sessionState = sessionStore.state.value
            val partyStatus = buildPartyStatusUi(
                sessionState = sessionState,
                charactersById = charactersById,
                levelingManager = levelingManager,
                skillsById = skillsById
            )
            val progressionSummary = buildProgressionSummaryUi(
                sessionState = sessionState,
                charactersById = charactersById,
                levelingManager = levelingManager
            )
            val initialActions = parseActions(initialRoom)
            _uiState.update {
                ExplorationUiState(
                    isLoading = false,
                    currentWorld = initialWorld,
                    currentHub = initialHub,
                    currentRoom = initialRoom,
                    availableConnections = initialRoom?.connections.orEmpty(),
                    npcs = initialRoom?.npcs.orEmpty(),
                    actions = initialActions,
                    actionHints = buildActionHints(initialRoom, initialActions),
                    enemies = initialRoom?.enemies.orEmpty(),
                    blockedDirections = computeBlockedDirections(initialRoom),
                    roomState = getRoomStateSnapshot(initialRoom?.id),
                    groundItems = getGroundItemsSnapshot(initialRoom?.id),
                    activeDialogue = null,
                    activeQuests = sessionState.activeQuests,
                    completedQuests = sessionState.completedQuests,
                    completedMilestones = sessionState.completedMilestones,
                    partyStatus = partyStatus,
                    progressionSummary = progressionSummary
                )
            }
            initialRoom?.let { handleRoomEntryTutorials(it) }
            playRoomAudio(initialHub?.id, initialRoom?.id)
            updateMinimap(initialRoom)
            initialRoom?.let { eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(it.id)) }
        }
    }

    fun travel(direction: String) {
        val currentRoom = _uiState.value.currentRoom ?: return
        val evaluation = evaluateDirection(currentRoom, direction, DirectionEvaluationMode.ATTEMPT)
        if (evaluation.blocked) {
            val message = evaluation.message ?: "The path is blocked."
            showBlockedDirectionPrompt(direction, message, evaluation.block)
            return
        }
        evaluation.message?.let { postStatus(it) }
        dismissBlockedPrompt()
        playUiCue("confirm")

        val nextRoomId = getConnection(currentRoom, direction) ?: return
        val nextRoom = roomsById[nextRoomId] ?: return

        sessionStore.setRoom(nextRoom.id)
        visitedRooms.add(nextRoom.id)
        markDiscovered(nextRoom)
        sessionStore.markTutorialCompleted("swipe_move")
        tutorialManager.cancel("swipe_hint")

        val sessionState = sessionStore.state.value
        val nextActions = parseActions(nextRoom)
        _uiState.update {
            it.copy(
                currentRoom = nextRoom,
                availableConnections = nextRoom.connections,
                npcs = nextRoom.npcs,
                actions = nextActions,
                actionHints = buildActionHints(nextRoom, nextActions),
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
        updateMinimap(nextRoom)
        playRoomAudio(sessionState.hubId, nextRoom.id)
        handleRoomEntryTutorials(nextRoom)
        eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(nextRoom.id))
    }

    fun onNpcInteraction(npcName: String) {
        val session = dialogueService.startDialogue(npcName)
        activeDialogueSession = session
        playUiCue("click")
        lastDialogueVoiceCue = null
        _uiState.update {
            it.copy(
                activeDialogue = buildDialogueUi(session?.current()),
                dialogueChoices = session?.choices()?.map { option ->
                    DialogueChoiceUi(option.id, option.text)
                } ?: emptyList(),
                statusMessage = session?.let { null } ?: "No dialogue available for $npcName yet."
            )
        }
        session?.current()?.voiceCue?.let { playVoiceCue(it) }
        eventManager.handleTrigger("talk_to", EventPayload.TalkTo(npcName))
        eventManager.handleTrigger("npc_interaction", EventPayload.TalkTo(npcName))
    }

    fun advanceDialogue() {
        val session = activeDialogueSession ?: return
        if (session.choices().isNotEmpty()) return
        val nextLine = session.advance()
        if (nextLine == null) {
            activeDialogueSession = null
            _uiState.update { it.copy(activeDialogue = null, dialogueChoices = emptyList()) }
            lastDialogueVoiceCue = null
        } else {
            _uiState.update {
                it.copy(
                    activeDialogue = buildDialogueUi(nextLine),
                    dialogueChoices = session.choices().map { option ->
                        DialogueChoiceUi(option.id, option.text)
                    }
                )
            }
            nextLine.voiceCue?.let { playVoiceCue(it) }
        }
    }

    fun onDialogueChoiceSelected(optionId: String) {
        val session = activeDialogueSession ?: return
        playUiCue("confirm")
        val nextLine = session.choose(optionId)
        if (nextLine == null) {
            activeDialogueSession = null
            _uiState.update { it.copy(activeDialogue = null, dialogueChoices = emptyList()) }
            lastDialogueVoiceCue = null
        } else {
            _uiState.update {
                it.copy(
                    activeDialogue = buildDialogueUi(nextLine),
                    dialogueChoices = session.choices().map { option ->
                        DialogueChoiceUi(option.id, option.text)
                    }
                )
            }
            nextLine.voiceCue?.let { playVoiceCue(it) }
        }
    }

    fun onDialogueVoiceRequested(cueId: String) {
        if (cueId.isBlank()) return
        playVoiceCue(cueId, force = true)
    }

    fun onActionSelected(action: RoomAction) {
        playUiCue("click")
        dismissBlockedPrompt()
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
            is TinkeringAction -> handleTinkeringAction(
                label = action.name,
                shopId = action.shopId,
                lockedMessage = action.conditionUnmetMessage
            )
            is ShopAction -> handleShopAction(action)
            is CookingAction -> handleCookingAction(action)
            is FirstAidAction -> handleFirstAidAction(action)
            is GenericAction -> handleGenericAction(action)
        }
        updateActionHints(_uiState.value.currentRoom)
    }

    fun clearStatusMessage() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(statusMessage = null) }
        }
    }

    fun dismissBlockedPrompt() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(blockedPrompt = null) }
        }
    }

    fun onBlockedPromptHint(sceneId: String) {
        if (sceneId.isBlank()) return
        startCinematic(sceneId)
    }

    fun dismissPrompt() {
        promptManager.dismissCurrent()
    }

    fun dismissNarration() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(narrationPrompt = null) }
        }
    }

    fun onFishingResult(result: FishingResultPayload) {
        if (result.success && !result.itemId.isNullOrBlank()) {
            val itemId = result.itemId
            val quantity = (result.quantity ?: 1).coerceAtLeast(1)
            inventoryService.addItem(itemId, quantity)
            val displayName = inventoryService.itemDisplayName(itemId)
            val message = result.message?.takeIf { it.isNotBlank() } ?: "Caught $displayName!"
            postStatus(message)
            emitEvent(ExplorationEvent.ItemGranted(displayName, quantity))
            triggerPlayerAction("fishing_success", itemId)
        } else {
            val message = result.message?.takeIf { it.isNotBlank() } ?: "The fish got away."
            postStatus(message)
            triggerPlayerAction("fishing_failed")
        }
        emitAudioCommands(audioRouter.restoreLayer(AudioCueType.MUSIC))
        emitAudioCommands(audioRouter.restoreLayer(AudioCueType.AMBIENT))
        if (result.success) {
            tutorialManager.playScript("fishing_success")
        } else {
            tutorialManager.playScript("fishing_failure")
        }
    }

    fun openMinimapLegend() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMinimapLegendVisible = true) }
        }
    }

    fun closeMinimapLegend() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMinimapLegendVisible = false) }
        }
    }

    fun openQuestLog() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isQuestLogVisible = true) }
        }
    }

    fun closeQuestLog() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isQuestLogVisible = false) }
        }
    }

    fun openMilestoneGallery() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMilestoneGalleryVisible = true) }
        }
    }

    fun closeMilestoneGallery() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMilestoneGalleryVisible = false) }
        }
    }


    fun openServicesMenu() {
        viewModelScope.launch(dispatchers.main) {
            val menu = buildServicesMenu()
            if (menu != null) {
                playUiCue("click")
                _uiState.update { it.copy(radialMenu = menu) }
            } else {
                postStatus("No services available here.")
            }
        }
    }

    fun dismissServicesMenu() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("cancel")
            _uiState.update { it.copy(radialMenu = null) }
        }
    }

    fun selectRadialMenuItem(id: String): RadialMenuAction {
        playUiCue("confirm")
        _uiState.update { it.copy(radialMenu = null) }
        return when {
            id == "inventory" -> RadialMenuAction.Inventory
            id == "questlog" -> {
                openQuestLog()
                RadialMenuAction.None
            }
            id.startsWith("shop:") -> {
                val shopId = id.substringAfter(':')
                emitEvent(ExplorationEvent.OpenShop(shopId))
                RadialMenuAction.None
            }
            id.startsWith("tinker:") -> {
                val shopId = id.substringAfter(':').ifBlank { null }
                emitEvent(ExplorationEvent.OpenTinkering(shopId))
                RadialMenuAction.None
            }
            id.startsWith("cook:") -> {
                val stationId = id.substringAfter(':').ifBlank { null }
                emitEvent(ExplorationEvent.OpenCooking(stationId))
                RadialMenuAction.None
            }
            id.startsWith("firstaid:") -> {
                val stationId = id.substringAfter(':').ifBlank { null }
                emitEvent(ExplorationEvent.OpenFirstAid(stationId))
                RadialMenuAction.None
            }
            id.startsWith("fish:") -> {
                val zoneId = id.substringAfter(':').ifBlank { null }
                emitEvent(ExplorationEvent.OpenFishing(zoneId))
                RadialMenuAction.None
            }
            id == "milestones" -> {
                openMilestoneGallery()
                RadialMenuAction.None
            }
            else -> RadialMenuAction.None
        }
    }

    private fun buildServicesMenu(): RadialMenuUi? {
        val actions = _uiState.value.actions
        val hints = _uiState.value.actionHints
        val items = mutableListOf<RadialMenuItemUi>()
        fun addUnique(id: String, label: String, description: String? = null) {
            if (items.none { it.id == id }) {
                items += RadialMenuItemUi(id = id, label = label, description = description)
            }
        }
        addUnique("inventory", "Inventory")
        addUnique("questlog", "Quest Log")
        addUnique("milestones", "Milestones", "History")
        actions.forEach { action ->
            if (hints[action.actionKey()]?.locked == true) return@forEach
            when (action) {
                is ShopAction -> action.shopId?.let { addUnique("shop:${it}", action.name, "Shop") }
                is TinkeringAction -> addUnique("tinker:${action.shopId.orEmpty()}", action.name, "Tinkering")
                is CookingAction -> addUnique("cook:${action.stationId.orEmpty()}", action.name, "Cooking")
                is FirstAidAction -> addUnique("firstaid:${action.stationId.orEmpty()}", action.name, "First Aid")
                is GenericAction -> {
                    val type = action.type.lowercase(Locale.getDefault())
                    if (type.contains("fish")) {
                        val zone = action.zoneId ?: _uiState.value.currentRoom?.id
                        addUnique("fish:${zone.orEmpty()}", action.name, "Fishing")
                    }
                }
                else -> Unit
            }
        }
        if (items.isEmpty()) return null
        return RadialMenuUi(
            title = _uiState.value.currentRoom?.title?.let { "${it} Services" } ?: "Services",
            items = items
        )
    }
    fun engageEnemy(enemyId: String) {
        val roomEnemies = _uiState.value.currentRoom?.enemies.orEmpty()
        val ordered = buildList {
            if (enemyId.isNotBlank()) add(enemyId)
            roomEnemies.filter { it.isNotBlank() && it != enemyId }.forEach { add(it) }
        }.mapNotNull { it.takeIf { id -> id.isNotBlank() } }
        val encounterIds = ordered.ifEmpty { listOfNotNull(enemyId.takeIf { it.isNotBlank() }) }
        if (encounterIds.isNotEmpty()) {
            emitEvent(ExplorationEvent.EnterCombat(encounterIds.distinct()))
        }
    }

    private fun handleRoomEntryTutorials(room: Room) {
        tutorialManager.cancel("light_switch_hint")
        tutorialManager.cancel("swipe_hint")
        tutorialManager.markRoomVisited(room.id)
        if (room.id.equals("town_9", ignoreCase = true)) {
            val lightOn = booleanValueOf(room.state["light_on"]) ?: false
            if (!tutorialManager.hasCompleted("light_switch_touch") && !lightOn) {
                tutorialManager.showOnce(
                    key = "light_switch_hint",
                    message = "Tap the light switch to brighten the room.",
                    context = room.title,
                    delayMs = 14_000L
                )
            }
        }
    }

    private fun onRoomStateChanged(roomId: String, stateKey: String, value: Boolean) {
        if (roomId.equals("town_9", ignoreCase = true) && stateKey.equals("light_on", ignoreCase = true) && value) {
            sessionStore.markTutorialCompleted("light_switch_touch")
            tutorialManager.markCompleted("light_switch_touch")
            tutorialManager.cancel("light_switch_hint")
            tutorialManager.showOnce(
                key = "swipe_hint",
                message = "Swipe across the screen to explore the house.",
                context = "Movement",
                delayMs = 3_000L
            )
        }
    }

    private fun currentEnemies(): List<String> = _uiState.value.currentRoom?.enemies.orEmpty()

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

    fun onTinkeringClosed() {
        triggerPlayerAction("tinkering_screen_closed")
    }

    fun onTinkeringCrafted(itemId: String?) {
        val normalized = itemId?.trim()
        if (normalized.isNullOrEmpty()) return
        triggerPlayerAction("tinkering_craft", normalized)
    }

    fun openTinkeringShortcut() {
        handleTinkeringAction(
            label = "Tinkering Table",
            shopId = null,
            lockedMessage = null
        )
    }

    fun collectGroundItem(itemId: String) {
        val roomId = _uiState.value.currentRoom?.id ?: return
        val items = roomGroundItems[roomId] ?: return
        val quantity = items[itemId] ?: return
        playUiCue("confirm")
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
        playUiCue("confirm")
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
        onRoomStateChanged(roomId, stateKeySafe, value)
        emitStateStatusIfNeeded(stateKeySafe, value)
        updateActionHints(_uiState.value.currentRoom)
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
        onRoomStateChanged(roomId, stateKeySafe, newValue)
        emitStateStatusIfNeeded(stateKeySafe, newValue)
        updateActionHints(_uiState.value.currentRoom)
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
        return milestoneMessages[milestone] ?: milestoneManager.messageFor(milestone)
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

    private fun markDiscovered(room: Room) {
        if (discoveredRooms.add(room.id)) {
            // newly discovered current room
        }
        room.connections.values.forEach { connectionId ->
            if (!connectionId.isNullOrBlank()) {
                discoveredRooms.add(connectionId)
            }
        }
    }

    private fun registerPortrait(portraitPath: String?, vararg rawKeys: String?) {
        val sanitized = sanitizePortraitName(portraitPath)
        val resolved = sanitized ?: DEFAULT_PORTRAIT
        rawKeys.filterNotNull()
            .map { it.normalizedKey() }
            .filter { it.isNotEmpty() }
            .forEach { key ->
                val existing = portraitBySpeaker[key]
                if (existing == null || sanitized != null) {
                    portraitBySpeaker[key] = resolved
                }
            }
    }

    private fun resolvePortraitKey(speaker: String): String? {
        val normalized = speaker.normalizedKey()
        portraitBySpeaker[normalized]?.let { return it }
        val lastSegment = normalized.substringAfterLast(' ', normalized)
        portraitBySpeaker[lastSegment]?.let { return it }
        return null
    }

    private fun sanitizePortraitName(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val base = path.substringAfterLast('/').substringBeforeLast('.')
        val normalized = base.replace('-', '_').lowercase(Locale.getDefault())
        val remapped = portraitOverrides[normalized] ?: normalized
        return remapped.takeIf { it.isNotBlank() }
    }

    private fun String.normalizedKey(): String = trim().lowercase(Locale.getDefault())

    private fun playVoiceCue(cueId: String?, force: Boolean = false) {
        val normalized = cueId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val dedupeKey = normalized.lowercase(Locale.getDefault()).replace('-', '_')
        if (!force && dedupeKey == lastDialogueVoiceCue) return
        lastDialogueVoiceCue = dedupeKey
        emitAudioCommands(listOf(AudioCommand.Play(AudioCueType.UI, dedupeKey, loop = false, fadeMs = 0L)))
    }

    private fun isTinkeringUnlocked(): Boolean {
        val milestones = sessionStore.state.value.completedMilestones
        return milestones.any {
            it.equals("ms_tinkering_prompt_active", ignoreCase = true) ||
                it.equals("ms_tinkering_tutorial_done", ignoreCase = true)
        }
    }

    private fun handleTinkeringAction(
        label: String,
        shopId: String?,
        lockedMessage: String?
    ) {
        if (!isTinkeringUnlocked()) {
            val message = lockedMessage?.takeIf { it.isNotBlank() }
                ?: "The tinkering table isn't ready yet."
            postStatus(message)
            return
        }
        val display = label.ifBlank { "Tinkering Table" }
        postStatus("Opening $display")
        triggerPlayerAction("tinkering_screen_entered")
        emitEvent(ExplorationEvent.OpenTinkering(shopId))
    }

    private fun handleShopAction(action: ShopAction) {
        val required = collectRequiredMilestones(action.requiresMilestone, action.requiresMilestones)
        val completed = sessionStore.state.value.completedMilestones
        val locked = required.any { it !in completed }
        if (locked) {
            val message = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
                ?: "That shop is not available yet."
            postStatus(message)
            return
        }
        val shopId = action.shopId?.takeIf { it.isNotBlank() }
        if (shopId == null) {
            postStatus("Shop data missing.")
            return
        }
        val shop = shopRepository.shopById(shopId)
        if (shop == null) {
            postStatus("Shop data unavailable.")
            return
        }
        action.actionEvent?.takeIf { it.isNotBlank() }?.let { triggerPlayerAction(it) }
        val displayName = shop.name.ifBlank { "Shop" }
        val greeting = shop.greeting?.takeIf { it.isNotBlank() }
        if (greeting != null) {
            postStatus("Chatting with $displayName")
            showShopGreeting(shop, greeting)
        } else {
            openShop(shop, displayName)
        }
    }

    fun enterPendingShop() {
        val shopId = _uiState.value.pendingShopId ?: return
        val shop = shopRepository.shopById(shopId)
        if (shop != null) {
            openShop(shop, shop.name.ifBlank { "Shop" })
        } else {
            activeShopDialogue = null
            _uiState.update { it.copy(shopGreeting = null, pendingShopId = null) }
        }
    }

    fun onShopChoiceSelected(choiceId: String) {
        val greeting = _uiState.value.shopGreeting ?: return
        val choice = greeting.choices.firstOrNull { it.id == choiceId } ?: return
        if (!choice.enabled && choice.action == ShopDialogueAction.SMALLTALK) {
            playUiCue("error")
            return
        }
        val shop = shopRepository.shopById(greeting.shopId)
        when (choice.action) {
            ShopDialogueAction.ENTER_SHOP -> {
                if (shop != null) {
                    openShop(shop, shop.name.ifBlank { "Shop" })
                } else {
                    dismissShopGreeting()
                }
            }
            ShopDialogueAction.SMALLTALK -> {
                if (shop != null) {
                    handleShopSmalltalk(shop, choiceId)
                }
            }
            ShopDialogueAction.LEAVE -> dismissShopGreeting()
        }
    }

    fun dismissShopGreeting() {
        activeShopDialogue = null
        _uiState.update { it.copy(shopGreeting = null, pendingShopId = null) }
    }

    private fun handleCookingAction(action: CookingAction) {
        val required = collectRequiredMilestones(action.requiresMilestone, action.requiresMilestones)
        val completed = sessionStore.state.value.completedMilestones
        val locked = required.any { it !in completed }
        if (locked) {
            val message = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
                ?: "The kitchen isn't ready yet."
            postStatus(message)
            return
        }
        action.actionEvent?.takeIf { it.isNotBlank() }?.let { triggerPlayerAction(it) }
        postStatus("Opening ${action.name.ifBlank { "Kitchen" }}")
        emitEvent(ExplorationEvent.OpenCooking(action.stationId))
    }

    private fun handleFirstAidAction(action: FirstAidAction) {
        val required = collectRequiredMilestones(action.requiresMilestone, action.requiresMilestones)
        val completed = sessionStore.state.value.completedMilestones
        val locked = required.any { it !in completed }
        if (locked) {
            val message = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
                ?: "You haven't unlocked this med station yet."
            postStatus(message)
            return
        }
        action.actionEvent?.takeIf { it.isNotBlank() }?.let { triggerPlayerAction(it) }
        postStatus("Opening ${action.name.ifBlank { "Med Station" }}")
        emitEvent(ExplorationEvent.OpenFirstAid(action.stationId))
    }

    private fun openShop(shop: ShopDefinition, displayName: String) {
        playShopCue(shop)
        activeShopDialogue = null
        _uiState.update { it.copy(shopGreeting = null, pendingShopId = null) }
        postStatus("Opening $displayName")
        emitEvent(ExplorationEvent.OpenShop(shop.id))
    }

    private fun showShopGreeting(shop: ShopDefinition, fallbackGreeting: String) {
        val dialogue = shop.dialogue
        val baseLines = if (dialogue?.preface.isNullOrEmpty()) {
            listOf(
                ShopDialogueLineUi(
                    id = "${shop.id}_preface_0",
                    speaker = lineSpeakerFallback(shop.name),
                    text = fallbackGreeting,
                    voiceCue = shop.voCue
                )
            )
        } else {
            dialogue.preface.mapIndexed { index, line ->
                ShopDialogueLineUi(
                    id = "${shop.id}_preface_$index",
                    speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shop.name.takeIf { it.isNotBlank() },
                    text = line.text,
                    voiceCue = line.voiceCue
                )
            }
        }
        val topics = dialogue?.smalltalk.orEmpty().associate { topic ->
            topic.id to ShopDialogueTopicState(
                id = topic.id,
                label = topic.label,
                responseLines = topic.response.mapIndexed { idx, line ->
                    ShopDialogueLineUi(
                        id = "${shop.id}_${topic.id}_$idx",
                        speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shop.name.takeIf { it.isNotBlank() },
                        text = line.text,
                        voiceCue = line.voiceCue
                    )
                },
                voiceCue = topic.voiceCue
            )
        }
        val tradeLabel = dialogue?.tradeLabel?.takeIf { it.isNotBlank() } ?: "Browse stock"
        val leaveLabel = dialogue?.leaveLabel?.takeIf { it.isNotBlank() } ?: "Not now"
        val session = ShopDialogueSession(
            shopId = shop.id,
            baseLines = baseLines,
            topics = topics,
            tradeLabel = tradeLabel,
            leaveLabel = leaveLabel
        )
        activeShopDialogue = session
        _uiState.update {
            it.copy(
                shopGreeting = ShopGreetingUi(
                    shopId = shop.id,
                    shopName = shop.name.ifBlank { "Shopkeeper" },
                    portraitPath = shop.portrait,
                    lines = baseLines,
                    choices = buildShopChoices(session)
                ),
                pendingShopId = shop.id
            )
        }
        playShopCue(shop)
        baseLines.firstOrNull { !it.voiceCue.isNullOrBlank() }?.voiceCue?.let { playShopVoice(it) }
    }

    private fun playShopCue(shop: ShopDefinition) {
        val cue = shop.voCue?.takeIf { it.isNotBlank() } ?: return
        emitAudioCommands(audioRouter.commandsForUi(cue))
    }

    private fun playShopVoice(cueId: String) {
        val normalized = cueId.trim()
        if (normalized.isEmpty()) return
        emitAudioCommands(audioRouter.commandsForUi(normalized))
    }

    private fun handleShopSmalltalk(shop: ShopDefinition, topicId: String) {
        val session = activeShopDialogue ?: return
        val topic = session.topics[topicId] ?: return
        session.visitedTopics += topicId
        val updatedLines = session.baseLines + topic.responseLines
        _uiState.update { state ->
            val current = state.shopGreeting ?: return@update state
            state.copy(
                shopGreeting = current.copy(
                    lines = updatedLines,
                    choices = buildShopChoices(session)
                )
            )
        }
        val cue = topic.voiceCue
            ?: topic.responseLines.firstOrNull { !it.voiceCue.isNullOrBlank() }?.voiceCue
        cue?.let { playShopVoice(it) }
        if (session.topics.size == session.visitedTopics.size && session.topics.isNotEmpty()) {
            val speaker = shop.name.ifBlank { "the shopkeeper" }
            postStatus("You've heard the latest from $speaker.")
        }
    }

    private fun buildShopChoices(session: ShopDialogueSession): List<ShopDialogueChoiceUi> {
        val tradeChoice = ShopDialogueChoiceUi(
            id = "enter_shop_${session.shopId}",
            label = session.tradeLabel,
            action = ShopDialogueAction.ENTER_SHOP
        )
        val topicChoices = session.topics.values
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .map { topic ->
                ShopDialogueChoiceUi(
                    id = topic.id,
                    label = topic.label,
                    action = ShopDialogueAction.SMALLTALK,
                    enabled = topic.id !in session.visitedTopics
                )
            }
        val leaveChoice = ShopDialogueChoiceUi(
            id = "leave_shop_${session.shopId}",
            label = session.leaveLabel,
            action = ShopDialogueAction.LEAVE
        )
        return buildList {
            add(tradeChoice)
            addAll(topicChoices)
            add(leaveChoice)
        }
    }

    private fun lineSpeakerFallback(shopName: String): String =
        shopName.takeIf { it.isNotBlank() } ?: "Shopkeeper"

    private fun collectRequiredMilestones(single: String?, many: List<String>?): Set<String> {
        val combined = mutableSetOf<String>()
        single?.takeIf { it.isNotBlank() }?.let { combined += it }
        many.orEmpty().forEach { milestone ->
            if (milestone.isNotBlank()) {
                combined += milestone
            }
        }
        return combined
    }

    private fun handleGenericAction(action: GenericAction) {
        when (action.type.lowercase(Locale.getDefault())) {
            "fishing" -> handleFishingAction(action)
            else -> {
                val status = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
                    ?: "Triggered ${action.name}"
                postStatus(status)
                triggerPlayerAction(action.actionEvent?.takeIf { it.isNotBlank() })
            }
        }
    }

    private fun handleFishingAction(action: GenericAction) {
        val zoneId = action.zoneId?.takeIf { it.isNotBlank() } ?: _uiState.value.currentRoom?.id
        val status = action.conditionUnmetMessage?.takeIf { it.isNotBlank() }
            ?: if (action.name.isNotBlank()) action.name else "Time to fish"
        postStatus(status)
        triggerPlayerAction(action.actionEvent?.takeIf { it.isNotBlank() })
        emitEvent(ExplorationEvent.OpenFishing(zoneId))
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



    private fun showNarration(message: String, tapToDismiss: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(narrationPrompt = NarrationPrompt(message, tapToDismiss))
            }
        }
    }

    private fun showBlockedDirectionPrompt(direction: String, message: String, block: BlockedDirection?) {
        postStatus(message)
        val sceneId = sceneIdForBlock(block)
        val keyLabel = block?.keyId?.takeIf { it.isNotBlank() }?.let { keyId ->
            "Requires ${inventoryService.itemDisplayName(keyId)}"
        }
        viewModelScope.launch(dispatchers.main) {
            _uiState.update {
                it.copy(
                    blockedPrompt = BlockedPrompt(
                        direction = direction,
                        message = message,
                        sceneId = sceneId,
                        requiresItemLabel = keyLabel
                    )
                )
            }
        }
    }

    private fun buildActionHints(room: Room?, actions: List<RoomAction>): Map<String, ActionHintUi> {
        if (room == null || actions.isEmpty()) return emptyMap()
        val hints = mutableMapOf<String, ActionHintUi>()
        actions.forEach { action ->
            val hint = when (action) {
                is ToggleAction -> deriveToggleHint(action)
                else -> null
            }
            if (hint != null) {
                hints[action.actionKey()] = hint
            }
        }
        return hints
    }

    private fun updateActionHints(room: Room?) {
        val hints = buildActionHints(room, _uiState.value.actions)
        _uiState.update { it.copy(actionHints = hints) }
    }

    private fun deriveToggleHint(action: ToggleAction): ActionHintUi? {
        val eventId = action.actionEventOn?.takeIf { !it.isNullOrBlank() } ?: return null
        val event = eventsById[eventId] ?: return null
        return findToggleHint(event.actions)
    }

    private fun findToggleHint(actions: List<EventAction>?): ActionHintUi? {
        if (actions.isNullOrEmpty()) return null
        val completed = sessionStore.state.value.completedMilestones
        for (action in actions) {
            when (action.type.lowercase(Locale.getDefault())) {
                "if_milestone_set" -> {
                    val milestone = action.milestone?.takeIf { it.isNotBlank() }
                    if (milestone != null && milestone !in completed) {
                        val label = formatMilestoneLabel(milestone)
                        return ActionHintUi(locked = true, message = "Requires $label")
                    }
                }
                "if_milestones_set" -> {
                    val milestones = action.milestones.orEmpty()
                    val missing = milestones.filterNot { it in completed }
                    if (missing.isNotEmpty()) {
                        val label = missing.joinToString(", ") { formatMilestoneLabel(it) }
                        return ActionHintUi(locked = true, message = "Requires $label")
                    }
                }
                "if_milestone_not_set" -> {
                    val milestone = action.milestone?.takeIf { it.isNotBlank() }
                    if (milestone != null && milestone in completed) {
                        return ActionHintUi(locked = true, message = "Already completed")
                    }
                }
                "if_milestones_not_set" -> {
                    val milestones = action.milestones.orEmpty()
                    val alreadySet = milestones.filter { it in completed }
                    if (alreadySet.isNotEmpty()) {
                        return ActionHintUi(locked = true, message = "Already completed")
                    }
                }
            }
            findToggleHint(action.`do`)?.let { return it }
            findToggleHint(action.elseDo)?.let { return it }
        }
        return null
    }

    private fun formatMilestoneLabel(milestoneId: String): String {
        milestoneMessages[milestoneId]?.let { message ->
            val trimmed = message.trim().removeSuffix(".")
            if (trimmed.isNotEmpty()) return trimmed
        }
        val cleaned = milestoneId
            .removePrefix("ms_")
            .replace('_', ' ')
            .replace('-', ' ')
        return cleaned.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
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
        updateActionHints(_uiState.value.currentRoom)
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
            currentRoom?.let { updateMinimap(it) }
        }
    }

    private enum class DirectionEvaluationMode { CHECK, ATTEMPT }

    private data class DirectionEvaluation(
        val blocked: Boolean,
        val message: String? = null,
        val block: BlockedDirection? = null
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
                    if (mode == DirectionEvaluationMode.ATTEMPT) {
                        handleBlockedDirectionCinematic(room.id, normalized, block)
                    }
                    DirectionEvaluation(true, block.messageLocked ?: "Enemies block the path.", block)
                } else {
                    if (mode == DirectionEvaluationMode.ATTEMPT) {
                        unlockDirection(room.id, normalized, block, silent = true)
                    }
                    DirectionEvaluation(blocked = false)
                }
            }
            "lock" -> {
                if (!requirementsMet(block.requires)) {
                    if (mode == DirectionEvaluationMode.ATTEMPT) {
                        handleBlockedDirectionCinematic(room.id, normalized, block)
                    }
                    return DirectionEvaluation(true, block.messageLocked ?: "It won't budge.", block)
                }
                val keyId = block.keyId?.takeIf { it.isNotBlank() }
                if (keyId != null && !inventoryService.hasItem(keyId)) {
                    val requirementMessage = block.messageLocked ?: "Requires ${inventoryService.itemDisplayName(keyId)}."
                    if (mode == DirectionEvaluationMode.ATTEMPT) {
                        handleBlockedDirectionCinematic(room.id, normalized, block)
                    }
                    return DirectionEvaluation(true, requirementMessage, block)
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
            else -> DirectionEvaluation(true, block.messageLocked ?: "The path is blocked.", block)
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
                markDiscovered(destRoom)
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
                    shopId = action["shop_id"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    conditionUnmetMessage = action["condition_unmet_message"].asStringOrNull()
                )
                "cooking" -> CookingAction(
                    name = name,
                    stationId = action["station_id"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    requiresMilestones = action["requires_milestones"].asListOrNull(),
                    requiresMilestone = action["requires_milestone"].asStringOrNull(),
                    conditionUnmetMessage = action["condition_unmet_message"].asStringOrNull(),
                    actionEvent = action["action_event"].asStringOrNull()
                )
                "first_aid", "firstaid" -> FirstAidAction(
                    name = name,
                    stationId = action["station_id"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    requiresMilestones = action["requires_milestones"].asListOrNull(),
                    requiresMilestone = action["requires_milestone"].asStringOrNull(),
                    conditionUnmetMessage = action["condition_unmet_message"].asStringOrNull(),
                    actionEvent = action["action_event"].asStringOrNull()
                )
                "shop" -> ShopAction(
                    name = name,
                    shopId = action["shop_id"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    requiresMilestones = action["requires_milestones"].asListOrNull(),
                    requiresMilestone = action["requires_milestone"].asStringOrNull(),
                    actionEvent = action["action_event"].asStringOrNull(),
                    conditionUnmetMessage = action["condition_unmet_message"].asStringOrNull()
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

private fun QuestJournalEntry.toUiSummary(): QuestSummaryUi = QuestSummaryUi(
    id = id,
    title = title,
    summary = summary,
    stageTitle = stageTitle,
    stageDescription = stageDescription,
    objectives = objectives.map { objective ->
        val status = if (objective.completed) "✓" else "○"
        "$status ${objective.text}"
    },
    completed = completed,
    stageIndex = stageIndex,
    totalStages = totalStages
)

private fun QuestLogEntry.toUiEntry(questRepository: QuestRepository): QuestLogEntryUi {
    val quest = questRepository.questById(questId)
    val stage = stageId?.let { id -> quest?.stages?.firstOrNull { it.id == id } }
    return QuestLogEntryUi(
        questId = questId,
        message = message,
        stageId = stageId,
        stageTitle = stage?.title,
        timestamp = timestamp
    )
}

private fun TutorialEntry.toUiPrompt(): TutorialPrompt = TutorialPrompt(this)

sealed interface RadialMenuAction {
    object None : RadialMenuAction
    object Inventory : RadialMenuAction
    object Milestones : RadialMenuAction
}

sealed interface ExplorationEvent {
    data class EnterCombat(val enemyIds: List<String>) : ExplorationEvent
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
    data class OpenTinkering(val shopId: String?) : ExplorationEvent
    data class OpenCooking(val stationId: String?) : ExplorationEvent
    data class OpenFirstAid(val stationId: String?) : ExplorationEvent
    data class OpenFishing(val zoneId: String?) : ExplorationEvent
    data class OpenShop(val shopId: String) : ExplorationEvent
    data class CombatOutcome(
        val outcome: CombatResultPayload.Outcome,
        val enemyIds: List<String>,
        val message: String
    ) : ExplorationEvent
    data class AudioCommands(val commands: List<AudioCommand>) : ExplorationEvent
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
