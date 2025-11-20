package com.example.starborn.feature.exploration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.local.Theme
import com.example.starborn.data.local.ThemeStyle
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.EncounterDescriptor
import com.example.starborn.domain.combat.EncounterEnemyInstance
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.dialogue.DialogueSession
import com.example.starborn.domain.event.AudioLayerCommandSpec
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.inventory.normalizeLootItemId
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
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.QuestReward
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Requirement
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomEnemyInstance
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillTreeDefinition
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.quest.QuestJournalEntry
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.quest.QuestRuntimeState
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSaveRepository
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.domain.tutorial.TutorialRuntimeState
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.navigation.CombatResultPayload
import java.util.Locale
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_PORTRAIT = "communicator_portrait"
private const val MILESTONE_BAND_LIMIT = 3
private const val FULL_MAP_COLUMNS = 16
private const val FULL_MAP_ROWS = 8
private const val EVENT_ANNOUNCEMENT_ACCENT = 0xFF7BE4FF
private const val STELLARIUM_GENERATOR_ROOM_ID = "mines_2"
private const val STELLARIUM_ENV_ID = "mine"
private const val STELLARIUM_GENERATOR_ON_EVENT = "evt_mine_power_on"
private const val STELLARIUM_GENERATOR_OFF_EVENT = "evt_mine_power_off"
private const val STELLARIUM_GENERATOR_ON_MESSAGE_FALLBACK = "The Stellarium generator roars to life."
private const val STELLARIUM_GENERATOR_OFF_MESSAGE = "The Stellarium generator winds down into silence."
private const val STELLARIUM_GENERATOR_OFF_ACCENT = 0xFFFF5252
private const val EXIT_KEY_SEPARATOR = "::"
private const val BAG_TUTORIAL_ID = "bag_basics"

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

private class SystemTutorialCoordinator(
    private val tutorialManager: TutorialRuntimeManager
) {
    fun play(sceneId: String?, context: String?, onComplete: () -> Unit): Boolean {
        val normalizedScene = sceneId?.takeIf { it.isNotBlank() }
        if (normalizedScene != null) {
            val scheduled = tutorialManager.playScript(
                scriptId = normalizedScene,
                allowDuplicates = false,
                onComplete = onComplete
            )
            if (scheduled) return true
        }
        val message = buildMessage(normalizedScene, context)
        val key = normalizedScene ?: buildKey(context)
        tutorialManager.showOnce(
            entry = TutorialEntry(
                key = key,
                context = context,
                message = message,
                metadata = mapOf("source" to "system")
            ),
            allowDuplicates = false,
            onDismiss = onComplete
        )
        return true
    }

    private fun buildMessage(sceneId: String?, context: String?): String {
        val formattedScene = sceneId?.let { formatSceneId(it) }
        val parts = mutableListOf<String>()
        parts += formattedScene?.let { "Tutorial: $it" } ?: "Tutorial"
        context?.takeIf { it.isNotBlank() }?.let { parts += it }
        return parts.filter { it.isNotBlank() }.joinToString("\n").ifBlank { "Tutorial available" }
    }

    private fun buildKey(context: String?): String {
        return context?.takeIf { it.isNotBlank() }
            ?.lowercase(Locale.getDefault())
            ?.let { "system:$it" }
            ?: "system_tutorial"
    }

    private fun formatSceneId(sceneId: String): String =
        sceneId.replace('_', ' ').replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
}

class ExplorationViewModel(
    private val worldAssets: WorldAssetDataSource,
    private val sessionStore: GameSessionStore,
    private val dialogueService: DialogueService,
    private val inventoryService: InventoryService,
    private val craftingService: CraftingService,
    private val cinematicCoordinator: CinematicCoordinator,
    private val questRepository: QuestRepository,
    private val questRuntimeManager: QuestRuntimeManager,
    private val milestoneManager: MilestoneRuntimeManager,
    private val audioRouter: AudioRouter,
    private val voiceoverController: VoiceoverController,
    private val shopRepository: ShopRepository,
    private val themeRepository: ThemeRepository,
    private val environmentThemeManager: EnvironmentThemeManager,
    private val levelingManager: LevelingManager,
    private val tutorialManager: TutorialRuntimeManager,
    private val promptManager: UIPromptManager,
    private val fishingService: FishingService,
    private val saveRepository: GameSaveRepository,
    private val encounterCoordinator: EncounterCoordinator,
    private val userSettingsStore: UserSettingsStore,
    eventDefinitions: List<GameEvent>,
    bootstrapCinematics: List<String> = emptyList(),
    bootstrapActions: List<String> = emptyList(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val dialogueTriggerBinder: (((String) -> Boolean)?) -> Unit = {}
) : ViewModel() {

    private val eventsById: Map<String, GameEvent> = eventDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(ExplorationUiState())
    val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorationEvent>()
    val events: SharedFlow<ExplorationEvent> = _events.asSharedFlow()

    private var charactersById: Map<String, Player> = emptyMap()
    private var skillsById: Map<String, Skill> = emptyMap()
    private var skillTreesByCharacter: Map<String, SkillTreeDefinition> = emptyMap()
    private val itemUseController = ItemUseController(
        inventoryService = inventoryService,
        craftingService = craftingService,
        sessionStore = sessionStore,
        charactersProvider = { charactersById }
    )
    private val stageTutorialKeys: MutableSet<String> = mutableSetOf()
    private val inventoryAddListener: (String, Int) -> Unit = { itemId, _ ->
        handleItemAcquired(itemId)
    }
    private val systemTutorialCoordinator = SystemTutorialCoordinator(tutorialManager)
    private fun buildPreviewFromSnapshot(snapshot: Map<String, Int>): List<InventoryPreviewItemUi> {
        return snapshot
            .filterValues { it > 0 }
            .map { (id, qty) ->
                val item = inventoryService.itemDetail(id)
                InventoryPreviewItemUi(
                    id = item?.id ?: id,
                    name = item?.name ?: inventoryService.itemDisplayName(id),
                    quantity = qty,
                    type = item?.type ?: "misc",
                    effect = item?.effect
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    init {
        inventoryService.loadItems()
        inventoryService.addOnItemAddedListener(inventoryAddListener)
        dialogueTriggerBinder(::handleDialogueTrigger)
        cinematicCoordinator.setCallbacks(
            onSceneStart = { emitAudioCommands(audioRouter.duckForCinematic()) },
            onSceneEnd = { emitAudioCommands(audioRouter.restoreAfterCinematic()) }
        )
        observeUserSettings()
    }

    private val eventManager = EventManager(
        events = eventDefinitions,
        sessionStore = sessionStore,
        eventHooks = EventHooks(
            onPlayCinematic = { sceneId, onComplete ->
                if (startSpecialCinematic(sceneId, onComplete)) {
                    emitEvent(ExplorationEvent.PlayCinematic(sceneId))
                } else {
                    val started = startCinematic(sceneId, onComplete)
                    if (started) {
                        emitEvent(ExplorationEvent.PlayCinematic(sceneId))
                    } else {
                        onComplete()
                    }
                }
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
                    emitEvent(ExplorationEvent.RoomStateChanged(targetRoom, stateKey, applied))
                }
            },
            onToggleRoomState = { roomId, stateKey ->
                val newValue = toggleRoomStateValue(roomId, stateKey)
                if (newValue != null) {
                    val targetRoom = roomId ?: _uiState.value.currentRoom?.id
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
            onTakeItem = { itemId, quantity ->
                val normalizedQuantity = quantity.coerceAtLeast(1)
                if (!inventoryService.hasItem(itemId, normalizedQuantity)) {
                    false
                } else {
                    val name = inventoryService.itemDisplayName(itemId)
                    inventoryService.removeItem(itemId, normalizedQuantity)
                    postStatus("Gave $normalizedQuantity x $name")
                    refreshCurrentRoomBlockedDirections()
                    true
                }
            },
            onGiveXp = { amount ->
                if (amount > 0) {
                    sessionStore.addXp(amount)
                }
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
                questId?.let { questRuntimeManager.recordQuestStarted(it) }
            },
            onQuestCompleted = { questId ->
                questId?.let {
                    questRuntimeManager.markQuestCompleted(it)
                    questRuntimeManager.recordQuestCompleted(it)
                }
            },
            onQuestFailed = { questId, reason ->
                questId?.let { questRuntimeManager.markQuestFailed(it, reason) }
            },
            onBeginNode = { roomId ->
                roomId?.let { roomsById[it]?.let { room -> markDiscovered(room) } }
                emitEvent(ExplorationEvent.BeginNode(roomId))
            },
            onSystemTutorial = { sceneId, context, done ->
                val handled = systemTutorialCoordinator.play(sceneId, context) { done() }
                emitEvent(ExplorationEvent.TutorialRequested(sceneId, context))
                if (!handled) {
                    done()
                }
            },
            onMilestoneSet = { milestone ->
                milestoneManager.handleMilestone(milestone, null)
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
            },
            onEventCompleted = { eventId ->
                milestoneManager.onEventCompleted(eventId)
            },
            onPartyMemberJoined = { memberId ->
                handlePartyMemberJoined(memberId)
            },
            onAudioLayerCommand = { handleAudioLayerCommand(it) }
        )
    )

    private fun observeUserSettings() {
        viewModelScope.launch(dispatchers.io) {
            userSettingsStore.settings.collect { settings ->
                withContext(dispatchers.main) {
                    applyUserSettings(settings)
                }
            }
        }
    }

    private fun applyUserSettings(settings: UserSettings) {
        val newMusic = settings.musicVolume.coerceIn(0f, 1f)
        val newSfx = settings.sfxVolume.coerceIn(0f, 1f)
        val newVignette = settings.vignetteEnabled
        val musicChanged = abs(newMusic - userMusicVolume) > 0.001f
        val sfxChanged = abs(newSfx - userSfxVolume) > 0.001f
        userMusicVolume = newMusic
        userSfxVolume = newSfx
        isVignetteEnabled = newVignette
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    musicVolume = userMusicVolume,
                    sfxVolume = userSfxVolume,
                    vignetteEnabled = isVignetteEnabled
                )
            )
        }
        if (musicChanged || sfxChanged) {
            emitEvent(ExplorationEvent.AudioSettingsChanged(userMusicVolume, userSfxVolume))
        }
    }

    private var roomsById: Map<String, Room> = emptyMap()
    private var themeByRoomId: Map<String, Theme?> = emptyMap()
    private var themeStyleByRoomId: Map<String, ThemeStyle?> = emptyMap()
    private val roomStates: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()
    private val roomGroundItems: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    private val visitedRooms: MutableSet<String> = mutableSetOf()
    private val discoveredRooms: MutableSet<String> = mutableSetOf()
    private val blockedCinematicsShown: MutableSet<String> = mutableSetOf()
    private var roomsByEnvironment: Map<String, List<Room>> = emptyMap()
    private var roomsByNodeId: Map<String, List<Room>> = emptyMap()
    private var nodeIdByRoomId: Map<String, String> = emptyMap()
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
    private val bootstrapCinematicQueue: ArrayDeque<String> = ArrayDeque(bootstrapCinematics)
    private val bootstrapActionQueue: ArrayDeque<String> = ArrayDeque(bootstrapActions)
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
    private val unlockedAreaIds: MutableSet<String> = mutableSetOf()
    private val pendingUnlockedAreas: MutableSet<String> = mutableSetOf()
    private val unlockedExitKeys: MutableSet<String> = mutableSetOf()
    private val pendingUnlockedExitKeys: MutableSet<String> = mutableSetOf()
    private var activeDialogueSession: DialogueSession? = null
    private var darkCapableRoomIds: Set<String> = emptySet()
    private var entryRoomIds: Set<String> = emptySet()
    private val darkRoomEntryDirection: MutableMap<String, String> = mutableMapOf()
    private var fadeOverlayCommandId: Long = 0L
    private val pendingFadeCallbacks: MutableMap<Long, () -> Unit> = mutableMapOf()
    private var userMusicVolume: Float = 1f
    private var userSfxVolume: Float = 1f
    private var isVignetteEnabled: Boolean = true
    private val eventAnnouncementQueue: ArrayDeque<EventAnnouncementUi> = ArrayDeque()
    private var nextEventAnnouncementId: Long = 0L

    override fun onCleared() {
        inventoryService.removeOnItemAddedListener(inventoryAddListener)
        dialogueTriggerBinder(null)
        cinematicCoordinator.setCallbacks()
        super.onCleared()
    }

    private fun emitEvent(event: ExplorationEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    private fun emitAudioCommands(commands: List<AudioCommand>) {
        if (commands.isEmpty()) return
        emitEvent(ExplorationEvent.AudioCommands(commands))
    }

    private fun handleAudioLayerCommand(command: AudioLayerCommandSpec) {
        val layerType = command.layer?.let(::parseAudioLayer) ?: return
        val commands = audioRouter.commandsForLayerOverride(
            layer = layerType,
            cueId = command.cueId,
            gain = command.gain,
            fadeMs = command.fadeMs,
            loop = command.loop,
            stop = command.stop
        )
        emitAudioCommands(commands)
    }

    private fun parseAudioLayer(raw: String): AudioCueType? {
        val normalized = raw.trim().lowercase(Locale.getDefault())
        return when (normalized) {
            "music", "music_primary", "primary", "music_pad", "pad" -> AudioCueType.MUSIC
            "ambience", "ambient", "environment" -> AudioCueType.AMBIENT
            "ui", "interface" -> AudioCueType.UI
            "battle", "combat", "stinger" -> AudioCueType.BATTLE
            "voice", "voiceover", "vo" -> AudioCueType.VOICE
            else -> null
        }
    }

    private fun playRoomAudio(hubId: String?, roomId: String?) {
        val room = roomId?.let { roomsById[it] } ?: _uiState.value.currentRoom
        val weatherId = room?.weather?.takeUnless { it.isBlank() }
        val tags = buildSet {
            room?.env?.takeIf { it.isNotBlank() }?.let { add(it.lowercase(Locale.getDefault())) }
            weatherId?.takeIf { it.isNotBlank() }?.let { add(it.lowercase(Locale.getDefault())) }
        }
        emitAudioCommands(audioRouter.commandsForRoom(hubId, roomId, weatherId, tags))
    }

    private fun playUiCue(key: String) {
        emitAudioCommands(audioRouter.commandsForUi(key))
    }

    private fun shouldSuppressStateAnnouncement(stateKey: String?): Boolean =
        stateKey?.equals("light_on", ignoreCase = true) == true

    private fun enqueueEventAnnouncement(
        title: String?,
        message: String,
        accentColor: Long = EVENT_ANNOUNCEMENT_ACCENT
    ) {
        viewModelScope.launch(dispatchers.main) {
            val announcement = EventAnnouncementUi(
                id = ++nextEventAnnouncementId,
                title = title?.takeIf { it.isNotBlank() },
                message = message,
                accentColor = accentColor
            )
            if (_uiState.value.eventAnnouncement == null) {
                _uiState.update { it.copy(eventAnnouncement = announcement) }
            } else {
                eventAnnouncementQueue.addLast(announcement)
            }
        }
    }

    fun dismissEventAnnouncement() {
        viewModelScope.launch(dispatchers.main) {
            val next = if (eventAnnouncementQueue.isNotEmpty()) eventAnnouncementQueue.removeFirst() else null
            _uiState.update { it.copy(eventAnnouncement = next) }
        }
    }

    private fun announceMilestoneEvent(milestoneId: String, message: String) {
        if (!acknowledgedMilestones.add(milestoneId)) return
        postStatus(message)
    }

    private fun removeEnemiesFromCurrentRoom(enemyIds: List<String>) {
        if (enemyIds.isEmpty()) return
        val currentRoom = _uiState.value.currentRoom ?: return
        if (roomEnemyParties(currentRoom).isEmpty()) return
        val updatedRoom = removeDefeatedEnemies(currentRoom, enemyIds)
        if (updatedRoom.enemies != currentRoom.enemies || updatedRoom.enemyParties != currentRoom.enemyParties) {
            roomsById = roomsById + (updatedRoom.id to updatedRoom)
            _uiState.update {
                it.copy(
                    currentRoom = updatedRoom,
                    enemies = roomEnemyParties(updatedRoom).flatten(),
                    canReturnToHub = canReturnToHub(updatedRoom)
                )
            }
        }
    }

    fun onCombatVictoryEnemiesCleared(enemyIds: List<String>) {
        removeEnemiesFromCurrentRoom(enemyIds)
    }

    fun onCombatVictory(result: CombatResultPayload) {
        val enemyIds = result.enemyIds
        val rewardParts = mutableListOf<String>()
        if (result.rewardXp > 0) rewardParts += "${result.rewardXp} XP"
        if (result.rewardAp > 0) rewardParts += "${result.rewardAp} AP"
        if (result.rewardCredits > 0) rewardParts += "${result.rewardCredits} credits"
        val grantedItems = mutableListOf<String>()
        result.rewardItems.forEach { (itemId, quantity) ->
            val qty = quantity.coerceAtLeast(0)
            if (qty <= 0) return@forEach
            val canonicalId = normalizeLootItemId(itemId)
            val name = inventoryService.itemDisplayName(canonicalId)
            rewardParts += "$qty x $name"
            emitEvent(ExplorationEvent.ItemGranted(name, qty))
            inventoryService.addItem(canonicalId, qty)
            grantedItems += "$qty x $name"
        }
        if (grantedItems.isNotEmpty()) {
            sessionStore.setInventory(inventoryService.snapshot())
        }
        removeEnemiesFromCurrentRoom(enemyIds)
        eventManager.handleTrigger(
            type = "encounter_victory",
            payload = EventPayload.EncounterOutcome(
                enemyIds = enemyIds,
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        refreshCurrentRoomBlockedDirections()
        val outcomeMessage = if (rewardParts.isNotEmpty()) {
            "Victory reward: ${rewardParts.joinToString(", ")}"
        } else {
            "Encounter cleared."
        }
        postStatus(outcomeMessage)
        enqueueLevelUps(result.levelUps)
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
        emitEvent(
            ExplorationEvent.CombatOutcome(
                outcome = CombatResultPayload.Outcome.RETREAT,
                enemyIds = enemyIds,
                message = message
            )
        )
        playRoomAudio(sessionStore.state.value.hubId, _uiState.value.currentRoom?.id)
    }

    fun showStatusMessage(message: String) {
        postStatus(message)
    }

    private fun postStatus(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(statusMessage = trimmed) }
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

    private fun startSpecialCinematic(
        sceneId: String?,
        onComplete: () -> Unit
    ): Boolean {
        if (sceneId.isNullOrBlank()) return false
        return when (sceneId) {
            "new_game_fade_in" -> {
                triggerFadeOverlay(
                    fromAlpha = 1f,
                    toAlpha = 0f,
                    durationMillis = 1800,
                    onComplete = onComplete
                )
                true
            }
            else -> false
        }
    }

    private fun startCinematic(
        sceneId: String?,
        completion: (() -> Unit)? = null
    ): Boolean {
        val onComplete = completion ?: {}
        val started = cinematicCoordinator.play(sceneId) { onComplete() }
        if (!started && !sceneId.isNullOrBlank()) {
            postStatus("Cinematic $sceneId is not available yet.")
        }
        return started
    }

    private fun triggerFadeOverlay(
        fromAlpha: Float,
        toAlpha: Float,
        durationMillis: Int,
        onComplete: () -> Unit
    ) {
        val commandId = ++fadeOverlayCommandId
        pendingFadeCallbacks[commandId] = onComplete
        _uiState.update {
            it.copy(
                fadeOverlay = FadeOverlayCommand(
                    id = commandId,
                    fromAlpha = fromAlpha,
                    toAlpha = toAlpha,
                    durationMillis = durationMillis
                )
            )
        }
    }

    fun onFadeOverlayFinished(commandId: Long) {
        pendingFadeCallbacks.remove(commandId)?.invoke()
        _uiState.update { state ->
            if (state.fadeOverlay?.id == commandId) state.copy(fadeOverlay = null) else state
        }
    }

    private fun handlePartyMemberJoined(memberId: String) {
        val normalized = memberId.lowercase(Locale.getDefault())
        if (normalized == "ollie") {
            removeNpcFromRoom(
                roomId = "town_8",
                npcId = "Ollie",
                descriptionRemovals = listOf("Ollie waves to you.", "Ollie waves to you")
            )
        }
        val displayName = charactersById[memberId]?.name
            ?: memberId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val message = "${displayName.ifBlank { "A new ally" }} joined the party!"
        postStatus(message)
        enqueueEventAnnouncement(
            title = displayName,
            message = "Joined the party.",
            accentColor = EVENT_ANNOUNCEMENT_ACCENT
        )
    }

    private fun removeNpcFromRoom(roomId: String, npcId: String, descriptionRemovals: List<String> = emptyList()) {
        val room = roomsById[roomId] ?: return
        val filtered = room.npcs.filterNot { it.equals(npcId, ignoreCase = true) }
        if (filtered.size == room.npcs.size) return
        var updatedRoom = room.copy(npcs = filtered)
        if (descriptionRemovals.isNotEmpty()) {
            val updatedDescription = scrubDescription(updatedRoom.description, descriptionRemovals)
            val updatedDarkDescription = scrubDescription(updatedRoom.descriptionDark, descriptionRemovals)
            if (updatedDescription != null || updatedDarkDescription != null) {
                updatedRoom = updatedRoom.copy(
                    description = updatedDescription ?: updatedRoom.description,
                    descriptionDark = updatedDarkDescription ?: updatedRoom.descriptionDark
                )
            }
        }
        roomsById = roomsById + (roomId to updatedRoom)
        if (_uiState.value.currentRoom?.id == roomId) {
            _uiState.update {
                it.copy(
                    currentRoom = updatedRoom,
                    npcs = filtered
                )
            }
        }
    }

    private fun scrubDescription(text: String?, tokens: List<String>): String? {
        if (text.isNullOrBlank()) return null
        val source = requireNotNull(text)
        var updated = source
        tokens.forEach { token ->
            if (token.isNotBlank()) {
                updated = updated.replace(token, " ")
            }
        }
        val normalized = updated
            .replace(Regex("\\s{2,}"), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .trim()
        val original = source.trim()
        return if (normalized.isEmpty() || normalized == original) null else normalized
    }

    fun advanceCinematic() {
        cinematicCoordinator.advance()
    }

    private fun CinematicPlaybackState.toUiState(): CinematicUiState {
        val steps = scene.steps
        if (steps.isEmpty()) {
            return CinematicUiState(
                sceneId = scene.id,
                title = scene.title,
                stepIndex = 0,
                stepCount = 0,
                step = CinematicStepUi(
                    type = CinematicStepType.NARRATION,
                    speaker = null,
                    text = ""
                )
            )
        }
        val safeIndex = stepIndex.coerceIn(0, steps.lastIndex)
        val step = steps[safeIndex]
        val stepUi = CinematicStepUi(
            type = step.type,
            speaker = step.speaker,
            text = step.text
        )
        return CinematicUiState(
            sceneId = scene.id,
            title = scene.title,
            stepIndex = safeIndex,
            stepCount = steps.size,
            step = stepUi
        )
    }

    private fun processBootstrapQueues() {
        if (bootstrapCinematicQueue.isEmpty() && bootstrapActionQueue.isEmpty()) return
        viewModelScope.launch(dispatchers.main) {
            while (bootstrapCinematicQueue.isNotEmpty()) {
                startCinematic(bootstrapCinematicQueue.removeFirst())
            }
            while (bootstrapActionQueue.isNotEmpty()) {
                triggerPlayerAction(bootstrapActionQueue.removeFirst())
            }
        }
    }

    private fun updateMinimap(currentRoom: Room?) {
        currentRoom?.let { markDiscovered(it) }
        val minimap = currentRoom?.let { buildMinimapState(it) }
        val fullMap = currentRoom?.let { buildFullMapState(it) }
        _uiState.update { it.copy(minimap = minimap, fullMap = fullMap) }
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

        val roomsInContext = roomsForMaps(currentRoom)
        val cells = roomsInContext.mapNotNull { room ->
            val pos = roomPosition(room)
            val dx = pos.first - currentPos.first
            val dy = pos.second - currentPos.second
            if (abs(dx) > 1 || abs(dy) > 1) return@mapNotNull null
            val isVisible = room.id == currentRoom.id ||
                visitedRooms.contains(room.id) ||
                discoveredRooms.contains(room.id)
            if (!isVisible) return@mapNotNull null
            val connections = room.connections.mapNotNull { (direction, targetId) ->
                targetId?.let { direction.lowercase(Locale.getDefault()) to it }
            }.toMap()
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
                gridX = pos.first,
                gridY = pos.second,
                visited = visitedRooms.contains(room.id),
                discovered = discoveredRooms.contains(room.id),
                isCurrent = room.id == currentRoom.id,
                hasEnemies = roomEnemyParties(room).isNotEmpty(),
                blockedDirections = blocked,
                connections = connections,
                pathHints = pathHints,
                services = services,
                isDark = isRoomDark(room)
            )
        }
        return MinimapUiState(cells = cells)
    }

    private fun buildFullMapState(currentRoom: Room): FullMapUiState {
        val currentPos = roomPosition(currentRoom)
        val roomsInContext = roomsForMaps(currentRoom)
        val cells = roomsInContext.mapNotNull { room ->
            val isVisible = room.id == currentRoom.id ||
                visitedRooms.contains(room.id) ||
                discoveredRooms.contains(room.id)
            if (!isVisible) return@mapNotNull null
            val pos = roomPosition(room)
            val connections = room.connections.mapNotNull { (direction, targetId) ->
                targetId?.let { direction.lowercase(Locale.getDefault()) to it }
            }.toMap()
            val blocked = computeBlockedDirections(room)
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
                offsetX = pos.first - currentPos.first,
                offsetY = pos.second - currentPos.second,
                gridX = pos.first,
                gridY = pos.second,
                visited = visitedRooms.contains(room.id),
                discovered = discoveredRooms.contains(room.id),
                isCurrent = room.id == currentRoom.id,
                hasEnemies = roomEnemyParties(room).isNotEmpty(),
                blockedDirections = blocked,
                connections = connections,
                services = services,
                isDark = isRoomDark(room)
            )
        }
        return FullMapUiState(cells = cells)
    }

    private fun roomPosition(room: Room): Pair<Int, Int> {
        val x = room.pos.getOrNull(0) ?: 0
        val y = room.pos.getOrNull(1) ?: 0
        return x to y
    }

    private fun roomsForMaps(anchor: Room): List<Room> {
        val nodeId = nodeIdByRoomId[anchor.id]
        val rooms = when {
            nodeId != null -> roomsByNodeId[nodeId].orEmpty()
            else -> roomsByEnvironment[environmentKey(anchor.env)].orEmpty()
        }
        return if (rooms.isNotEmpty()) rooms else listOf(anchor)
    }

    private fun environmentKey(env: String?): String =
        env?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()) ?: "__default"

    private fun isRoomDark(room: Room): Boolean = isRoomDark(room.id, room)

    private fun isRoomDark(roomId: String, fallback: Room? = roomsById[roomId]): Boolean {
        val stateValues = roomStates[roomId]
        val explicitLight = stateValues?.get("light_on")?.let { booleanValueOf(it) }
        if (explicitLight == true) return false
        if (explicitLight == false) return true
        val explicitDark = stateValues?.get("dark")?.let { booleanValueOf(it) }
        if (explicitDark != null) return explicitDark
        val room = fallback ?: roomsById[roomId]
        room ?: return false
        val initialLight = room.state["light_on"]?.let { booleanValueOf(it) }
        if (initialLight == true) return false
        val initialDark = room.state["dark"]?.let { booleanValueOf(it) }
        return initialDark ?: (room.dark == true)
    }

    private fun visibleConnections(room: Room): Map<String, String> {
        if (!isRoomDark(room)) return room.connections
        val allowed = darkRoomEntryDirection[room.id]?.lowercase(Locale.getDefault())
        if (allowed.isNullOrBlank()) return emptyMap()
        return room.connections.filterKeys { it.equals(allowed, ignoreCase = true) }
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
        reward.xp?.takeIf { it > 0 }?.let { amount ->
            sessionStore.addXp(amount)
            parts.add("$amount XP")
            emitEvent(ExplorationEvent.XpGained(amount))
        }
        reward.ap?.takeIf { it > 0 }?.let { amount ->
            sessionStore.addAp(amount)
            parts.add("$amount AP")
        }
        reward.credits?.takeIf { it > 0 }?.let { amount ->
            sessionStore.addCredits(amount)
            parts.add("$amount credits")
        }
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

    private fun handleItemAcquired(itemId: String) {
        if (itemId.isBlank()) return
        eventManager.handleTrigger(
            type = "item_acquired",
            payload = EventPayload.ItemAcquired(itemId)
        )
    }

    private fun handleDialogueTrigger(trigger: String): Boolean {
        val actions = DialogueTriggerParser.parse(trigger)
        if (actions.isEmpty()) return false
        eventManager.performActions(actions)
        return true
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
                _uiState.update { current ->
                    val overlay = current.skillTreeOverlay?.characterId?.let { characterId ->
                        buildSkillTreeOverlay(characterId, newState)
                    }
                    current.copy(
                        completedMilestones = newState.completedMilestones,
                        partyStatus = partyStatus,
                        progressionSummary = progressionSummary,
                        equippedItems = newState.equippedItems,
                        skillTreeOverlay = overlay
                    )
                }
                updateActionHints(_uiState.value.currentRoom)
                updateUnlockedAreas(newState.unlockedAreas)
                updateUnlockedExits(newState.unlockedExits)
            }
        }

        viewModelScope.launch(dispatchers.main) {
            inventoryService.state.collect { entries ->
                val preview = entries
                    .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
                    .map { entry ->
                        InventoryPreviewItemUi(
                            id = entry.item.id,
                            name = entry.item.name,
                            quantity = entry.quantity,
                            type = entry.item.type,
                            effect = entry.item.effect
                        )
                    }
                _uiState.update { it.copy(inventoryPreview = preview) }
            }
        }

        // Seed inventory preview immediately from any already-loaded inventory so the UI isn't empty on first open.
        val initialEntries = inventoryService.state.value
        if (initialEntries.isNotEmpty()) {
            val initialPreview = initialEntries
                .sortedBy { it.item.name.lowercase(Locale.getDefault()) }
                .map { entry ->
                    InventoryPreviewItemUi(
                        id = entry.item.id,
                        name = entry.item.name,
                        quantity = entry.quantity,
                        type = entry.item.type,
                        effect = entry.item.effect
                    )
                }
            _uiState.update { it.copy(inventoryPreview = initialPreview) }
        } else if (sessionStore.state.value.inventory.isNotEmpty()) {
            val snapshotPreview = buildPreviewFromSnapshot(sessionStore.state.value.inventory)
            if (snapshotPreview.isNotEmpty()) {
                _uiState.update { it.copy(inventoryPreview = snapshotPreview) }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            // Ensure inventory is visible even when starting from a room without entering a hub first.
            var restoredFromSession = false
            sessionStore.state.collect { state ->
                if (!restoredFromSession && inventoryService.snapshot().isEmpty() && state.inventory.isNotEmpty()) {
                    inventoryService.restore(state.inventory)
                    restoredFromSession = true
                    val snapshotPreview = buildPreviewFromSnapshot(state.inventory)
                    if (snapshotPreview.isNotEmpty()) {
                        _uiState.update { it.copy(inventoryPreview = snapshotPreview) }
                    }
                } else if (_uiState.value.inventoryPreview.isEmpty() && state.inventory.isNotEmpty()) {
                    val snapshotPreview = buildPreviewFromSnapshot(state.inventory)
                    if (snapshotPreview.isNotEmpty()) {
                        _uiState.update { it.copy(inventoryPreview = snapshotPreview) }
                    }
                }
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
            environmentThemeManager.state.collect { themeState ->
                val environmentId = themeState.environmentId
                if (!environmentId.isNullOrBlank()) {
                    val resolvedTheme = themeState.theme ?: themeRepository.getTheme(environmentId)
                    val resolvedStyle = themeState.style ?: themeRepository.getStyle(environmentId)
                    _uiState.update {
                        it.copy(
                            theme = resolvedTheme ?: it.theme,
                            themeStyle = resolvedStyle ?: it.themeStyle
                        )
                    }
                }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            var previousActive: Set<String> = questRuntimeManager.state.value.activeQuestIds
            var previousCompleted: Set<String> = questRuntimeManager.state.value.completedQuestIds
            var previousStages: Map<String, String> = questRuntimeManager.state.value.stageProgress
            var firstEmission = true
            questRuntimeManager.state.collect { questState ->
                val activeChanged = questState.activeQuestIds != previousActive
                val completedChanged = questState.completedQuestIds != previousCompleted
                val stageChanges = if (firstEmission) {
                    emptyList()
                } else {
                    questState.stageProgress.mapNotNull { (questId, stageId) ->
                        val previousStage = previousStages[questId]
                        if (stageId != previousStage) {
                            questId to stageId
                        } else {
                            null
                        }
                    }
                }
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
                    current.questDetail?.id?.let { openId ->
                        val refreshed = buildQuestDetail(
                            questId = openId,
                            questState = questState,
                            questRepository = questRepository,
                            runtimeManager = questRuntimeManager,
                            inventoryService = inventoryService
                        )
                        updated = updated.copy(questDetail = refreshed)
                    }
                    updated
                }
                if (!firstEmission && (activeChanged || completedChanged)) {
                    emitEvent(ExplorationEvent.QuestUpdated(questState.activeQuestIds, questState.completedQuestIds))
                    postStatus("Quest log updated")
                    updateActionHints(_uiState.value.currentRoom)
                }
                if (!firstEmission) {
                    stageChanges.forEach { (questId, stageId) ->
                        scheduleQuestStageTutorials(questId, stageId)
                    }
                }
                previousActive = questState.activeQuestIds
                previousCompleted = questState.completedQuestIds
                previousStages = questState.stageProgress
                firstEmission = false
            }
        }

        viewModelScope.launch(dispatchers.main) {
            promptManager.state.collect { promptState ->
                _uiState.update { it.copy(prompt = promptState.current) }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            tutorialManager.runtimeState.collect { tutorialState ->
                _uiState.update { it.copy(tutorialState = tutorialState) }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            cinematicCoordinator.state.collect { playback ->
                _uiState.update { it.copy(cinematic = playback?.toUiState()) }
            }
        }
    }

    init {
        loadInitialState()
    }

    private fun scheduleQuestStageTutorials(questId: String, stageId: String) {
        val quest = questRepository.questById(questId) ?: return
        val stage = quest.stages.firstOrNull { it.id.equals(stageId, ignoreCase = true) } ?: return
        stage.tasks.forEach { task ->
            val tutorialId = task.tutorialId?.trim()?.lowercase(Locale.getDefault())
            if (!tutorialId.isNullOrEmpty()) {
                val key = listOf(questId, stageId, tutorialId).joinToString(":")
                if (stageTutorialKeys.add(key)) {
                    val scheduled = tutorialManager.playScript(tutorialId, allowDuplicates = false)
                    if (!scheduled) {
                        tutorialFallbackMessage(tutorialId)?.let { message ->
                            tutorialManager.showOnce(
                                key = key,
                                message = message,
                                context = stage.title.takeIf { it.isNotBlank() },
                                metadata = mapOf(
                                    "quest_id" to questId,
                                    "stage_id" to stageId
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildSkillTreeOverlay(
        characterId: String,
        sessionState: GameSessionState
    ): SkillTreeOverlayUi? {
        val tree = skillTreesByCharacter[characterId] ?: return null
        val character = charactersById[characterId]
        return buildSkillTreeOverlayUi(tree, character, sessionState)
    }

    private fun findSkillNode(characterId: String, nodeId: String): SkillTreeNode? =
        skillTreesByCharacter[characterId]
            ?.branches
            ?.values
            ?.flatten()
            ?.firstOrNull { it.id == nodeId }

    private fun tutorialFallbackMessage(tutorialId: String): String? {
        return when (tutorialId.lowercase(Locale.getDefault())) {
            "movement" -> "Swipe in the highlighted direction to move between rooms."
            "npc_talk" -> "Tap a character's name and choose Talk to start a conversation."
            "scene_light_switch_hint" -> "Turn on the light switch once power is restored to brighten Nova's House."
            "scene_scrap_run_tyson" -> "Head into Tyson's stall and tap the Talk prompt to hear his request."
            "scene_scrap_run_shop" -> "Loot the ration crate in Tyson's back room to collect his supplies."
            "scene_scrap_run_return" -> "Find Ollie in the plaza and hand over the ration pack."
            "scene_fixers_favor_jed" -> "Talk to Jed inside Scrap & Salvage so he can brief you on the repairs."
            "scene_fixers_favor_table" -> "Tap the tinkering table when Jed calls you over to begin the tutorial."
            "scene_fixers_favor_craft" -> "Follow Jed's prompts to slot parts and press Craft to seal the repair."
            "scene_fixers_favor_return" -> "Close the bench and talk to Jed again to wrap up the repair session."
            "scene_ollie_recruitment" -> "Tap ally portraits on the HUD to swap party members or hear their guidance."
            "scene_market_locator" -> "Follow the glowing minimap marker to reach Jed's stall in the market."
            "scene_market_journal" -> "Open your journal from the HUD to track errands and hub summaries."
            else -> null
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch(dispatchers.io) {
            val existingState = sessionStore.state.value
            val preselectedWorldId = existingState.worldId
            val preselectedHubId = existingState.hubId
            val preselectedRoomId = existingState.roomId
            unlockedAreaIds.clear()
            unlockedAreaIds.addAll(existingState.unlockedAreas)
            pendingUnlockedAreas.clear()
            pendingUnlockedAreas.addAll(unlockedAreaIds)
            unlockedExitKeys.clear()
            unlockedExitKeys.addAll(existingState.unlockedExits)
            pendingUnlockedExitKeys.clear()
            pendingUnlockedExitKeys.addAll(unlockedExitKeys)

            val worlds = worldAssets.loadWorlds()
            val hubs = worldAssets.loadHubs()
            val nodes = worldAssets.loadHubNodes()
            entryRoomIds = nodes.mapNotNull { node ->
                node.entryRoom.takeIf { it.isNotBlank() }
            }.toSet()
            val rooms = worldAssets.loadRooms().map { room ->
                if (room.env.isBlank()) {
                    val derivedEnv = room.id.substringBefore('_', room.id)
                    if (derivedEnv.isNotBlank()) {
                        room.copy(env = derivedEnv)
                    } else {
                        room
                    }
                } else {
                    room
                }
            }.map { normalizeRoomEnemies(it) }
            val players = worldAssets.loadCharacters()
            val skills = worldAssets.loadSkills()
            val skillTrees = worldAssets.loadSkillTrees()
            val npcs = worldAssets.loadNpcs()

            charactersById = players.associateBy { it.id }
            skillsById = skills.associateBy { it.id }
            skillTreesByCharacter = skillTrees.associateBy { it.character }

            roomsById = rooms.associateBy { it.id }
            themeByRoomId = rooms.associate { it.id to themeRepository.getTheme(it.env) }
            themeStyleByRoomId = rooms.associate { it.id to themeRepository.getStyle(it.env) }
            darkCapableRoomIds = rooms.filter { room ->
                room.dark == true || booleanValueOf(room.state["dark"]) == true
            }.map { it.id }.toSet()
            roomsByEnvironment = rooms.groupBy { environmentKey(it.env) }
            nodeIdByRoomId = nodes.flatMap { node -> node.rooms.map { it to node.id } }.toMap()
            roomsByNodeId = nodes.associate { node ->
                node.id to node.rooms.mapNotNull { roomId -> roomsById[roomId] }
            }
            flushPendingUnlockedExits()
            flushPendingUnlockedAreas()
            initializeRoomStates(rooms)
            sanitizeDarkStates()
            initializeUnlockedDirections(rooms)
            initializeGroundItems(rooms)
            val sessionSnapshot = sessionStore.state.value
            val player = sessionSnapshot.playerId?.let { id -> players.firstOrNull { it.id == id } }
                ?: players.firstOrNull()
            val initialWorld =
                preselectedWorldId?.let { id -> worlds.firstOrNull { it.id == id } }
                    ?: worlds.firstOrNull { it.id.equals("nova_prime", ignoreCase = true) }
                    ?: worlds.firstOrNull()
            val initialHub =
                preselectedHubId?.let { id -> hubs.firstOrNull { it.id == id } }
                    ?: initialWorld?.let { world -> hubs.firstOrNull { it.worldId == world.id } }
                    ?: hubs.firstOrNull()
            val initialRoom =
                preselectedRoomId?.let { id -> roomsById[id] }
                    ?: rooms.firstOrNull { it.id.equals("town_9", ignoreCase = true) }
                    ?: rooms.firstOrNull()
            val initialThemeId = initialRoom?.env
            val initialTheme = initialRoom?.let { themeByRoomId[it.id] }
            val initialThemeStyle = initialRoom?.let { themeStyleByRoomId[it.id] }
            environmentThemeManager.apply(initialThemeId, initialRoom?.weather)

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
            if (preselectedRoomId.isNullOrBlank()) {
                sessionStore.resetTutorialProgress()
            }
            acknowledgedMilestones.clear()
            eventAnnouncementQueue.clear()
            nextEventAnnouncementId = 0L

            sessionStore.setWorld(initialWorld?.id)
            sessionStore.setHub(initialHub?.id)
            sessionStore.setRoom(initialRoom?.id)
            sessionStore.setPlayer(player?.id)
            val resolvedPlayerLevel = sessionSnapshot.partyMemberLevels[player?.id] ?: player?.level ?: 1
            val resolvedPlayerXp = sessionSnapshot.partyMemberXp[player?.id] ?: player?.xp ?: 0
            sessionStore.setPlayerLevel(resolvedPlayerLevel)
            sessionStore.setPlayerXp(resolvedPlayerXp)
            val resolvedPartyMembers = sessionSnapshot.partyMembers.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(player?.id)
            sessionStore.setPartyMembers(resolvedPartyMembers)

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
            val initialConnections = initialRoom?.let { visibleConnections(it) }.orEmpty()
            _uiState.update {
                ExplorationUiState(
                    isLoading = false,
                    currentWorld = initialWorld,
                    currentHub = initialHub,
                    currentRoom = initialRoom,
                    availableConnections = initialConnections,
                    npcs = initialRoom?.npcs.orEmpty(),
                    actions = initialActions,
                    actionHints = buildActionHints(initialRoom, initialActions),
                    enemies = initialRoom?.let { roomEnemyParties(it).flatten() }.orEmpty(),
                    blockedDirections = computeBlockedDirections(initialRoom),
                    roomState = getRoomStateSnapshot(initialRoom?.id),
                    groundItems = getGroundItemsSnapshot(initialRoom?.id),
                    activeDialogue = null,
                    activeQuests = sessionState.activeQuests,
                    completedQuests = sessionState.completedQuests,
                    completedMilestones = sessionState.completedMilestones,
                    theme = initialTheme,
                    themeStyle = initialThemeStyle,
                    darkCapableRooms = darkCapableRoomIds,
                    canReturnToHub = canReturnToHub(initialRoom),
                    partyStatus = partyStatus,
                    progressionSummary = progressionSummary,
                    mineGeneratorOnline = isMineGeneratorOnline(),
                    settings = SettingsUiState(
                        musicVolume = userMusicVolume,
                        sfxVolume = userSfxVolume,
                        vignetteEnabled = isVignetteEnabled
                    )
                )
            }
            initialRoom?.let { room ->
                handleRoomEntryTutorials(room)
                if (!isRoomDark(room)) {
                    visitedRooms.add(room.id)
                    markDiscovered(room)
                }
            }
            playRoomAudio(initialHub?.id, initialRoom?.id)
            updateMinimap(initialRoom)
            initialRoom?.let { eventManager.handleTrigger("enter_room", EventPayload.EnterRoom(it.id)) }
            processBootstrapQueues()
        }
    }

    fun travel(direction: String) {
        val currentRoom = _uiState.value.currentRoom ?: return
        val normalizedDirection = direction.lowercase(Locale.getDefault())
        if (isRoomDark(currentRoom)) {
            val allowedDirection = darkRoomEntryDirection[currentRoom.id]?.lowercase(Locale.getDefault())
            if (allowedDirection != null && allowedDirection != normalizedDirection) {
                postStatus("It's too dark to feel your way in that direction.")
                playUiCue("error")
                return
            }
        }
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
        val nextRoomIsDark = isRoomDark(nextRoom)
        val nextTheme = themeByRoomId[nextRoom.id]
        val nextThemeStyle = themeStyleByRoomId[nextRoom.id]
        environmentThemeManager.apply(nextRoom.env, nextRoom.weather)

        sessionStore.setRoom(nextRoom.id)
        if (!nextRoomIsDark) {
            visitedRooms.add(nextRoom.id)
            markDiscovered(nextRoom)
            darkRoomEntryDirection.remove(nextRoom.id)
        } else {
            val allowedExit = oppositeDirection(normalizedDirection) ?: normalizedDirection
            darkRoomEntryDirection[nextRoom.id] = allowedExit.lowercase(Locale.getDefault())
        }
        darkRoomEntryDirection.remove(currentRoom.id)
        sessionStore.markTutorialCompleted("swipe_move")
        tutorialManager.cancel("swipe_hint")

        val sessionState = sessionStore.state.value
        val nextActions = parseActions(nextRoom)
        _uiState.update {
            it.copy(
                currentRoom = nextRoom,
                availableConnections = visibleConnections(nextRoom),
                npcs = nextRoom.npcs,
                actions = nextActions,
                actionHints = buildActionHints(nextRoom, nextActions),
                enemies = roomEnemyParties(nextRoom).flatten(),
                blockedDirections = computeBlockedDirections(nextRoom),
                roomState = getRoomStateSnapshot(nextRoom.id),
                groundItems = getGroundItemsSnapshot(nextRoom.id),
                activeDialogue = null,
                activeQuests = sessionState.activeQuests,
                completedQuests = sessionState.completedQuests,
                theme = nextTheme,
                themeStyle = nextThemeStyle,
                statusMessage = null,
                canReturnToHub = canReturnToHub(nextRoom)
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
            is ToggleAction -> handleToggleAction(action)
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
            else -> Unit
        }
        updateActionHints(_uiState.value.currentRoom)
    }

    private fun handleToggleAction(action: ToggleAction) {
        val stateKey = action.stateKey
        if (stateKey.isBlank()) {
            val fallback = action.popupTitle?.takeIf { it.isNotBlank() } ?: action.name
            if (fallback.isNotBlank()) {
                postStatus(fallback)
            }
            return
        }
        val currentState = _uiState.value.roomState[stateKey] ?: false
        val targetState = !currentState
        val title = action.popupTitle?.takeIf { it.isNotBlank() }
            ?: action.name.ifBlank { formatStateKey(stateKey) }
        val enableLabel = action.labelOn.ifBlank { "Turn on" }
        val disableLabel = action.labelOff.ifBlank { "Turn off" }
        val eventId = if (targetState) {
            action.actionEventOn?.takeIf { it.isNotBlank() }
        } else {
            action.actionEventOff?.takeIf { it.isNotBlank() }
        }
        if (eventId != null) {
            val event = eventsById[eventId]
            val message = if (targetState) {
                event?.onMessage ?: enableLabel
            } else {
                event?.offMessage ?: disableLabel
            }
            if (!shouldSuppressStateAnnouncement(stateKey)) {
                pendingStateMessage = message
                suppressNextStateMessage = true
            } else {
                pendingStateMessage = null
                suppressNextStateMessage = false
            }
            triggerPlayerAction(eventId)
        } else {
            val roomId = _uiState.value.currentRoom?.id
            val applied = setRoomStateValue(roomId, stateKey, targetState)
            if (applied != null && !shouldSuppressStateAnnouncement(stateKey)) {
                val message = if (targetState) enableLabel else disableLabel
                postStatus(message)
            }
        }
        showGeneratorAnnouncementIfNeeded(action, targetState, title)
    }

    private fun showGeneratorAnnouncementIfNeeded(action: ToggleAction, targetState: Boolean, title: String) {
        if (!isStellariumGeneratorToggle(action)) return
        val message = if (targetState) {
            milestoneMessages["ms_mine_power_on"] ?: STELLARIUM_GENERATOR_ON_MESSAGE_FALLBACK
        } else {
            STELLARIUM_GENERATOR_OFF_MESSAGE
        }
        val accentColor = if (targetState) EVENT_ANNOUNCEMENT_ACCENT else STELLARIUM_GENERATOR_OFF_ACCENT
        enqueueEventAnnouncement(title, message, accentColor)
    }

    private fun isStellariumGeneratorToggle(action: ToggleAction): Boolean {
        val titleContainsGenerator = action.popupTitle?.contains("generator", ignoreCase = true) == true
        val nameContainsGenerator = action.name.contains("generator", ignoreCase = true)
        val eventOnMatches = action.actionEventOn?.equals(STELLARIUM_GENERATOR_ON_EVENT, ignoreCase = true) == true
        val eventOffMatches = action.actionEventOff?.equals(STELLARIUM_GENERATOR_OFF_EVENT, ignoreCase = true) == true
        return action.stateKey.equals("power_on", ignoreCase = true) &&
            (titleContainsGenerator || nameContainsGenerator || eventOnMatches || eventOffMatches)
    }

    fun onTogglePromptSelection(enable: Boolean) {
        val prompt = _uiState.value.togglePrompt ?: return
        val currentState = _uiState.value.roomState[prompt.stateKey] ?: false
        if (enable == currentState) {
            val message = if (enable) {
                "${prompt.title} is already on."
            } else {
                "${prompt.title} is already off."
            }
            postStatus(message)
            _uiState.update { it.copy(togglePrompt = null) }
            return
        }
        val eventId = if (enable) prompt.eventOn else prompt.eventOff
        if (!eventId.isNullOrBlank()) {
            val event = eventsById[eventId]
            val message = if (enable) {
                prompt.onMessage ?: event?.onMessage ?: prompt.enableLabel
            } else {
                prompt.offMessage ?: event?.offMessage ?: prompt.disableLabel
            }
            if (!shouldSuppressStateAnnouncement(prompt.stateKey)) {
                pendingStateMessage = message
                suppressNextStateMessage = true
            } else {
                pendingStateMessage = null
                suppressNextStateMessage = false
            }
            triggerPlayerAction(eventId)
        } else {
            val roomId = prompt.roomId ?: _uiState.value.currentRoom?.id
            val applied = setRoomStateValue(roomId, prompt.stateKey, enable)
            if (applied != null) {
                if (!shouldSuppressStateAnnouncement(prompt.stateKey)) {
                    val message = if (enable) prompt.enableLabel else prompt.disableLabel
                    postStatus(message)
                }
            }
        }
        _uiState.update { it.copy(togglePrompt = null) }
    }

    fun dismissTogglePrompt() {
        _uiState.update { it.copy(togglePrompt = null) }
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

    fun openMapLegend() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMapLegendVisible = true) }
        }
    }

    fun closeMapLegend() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isMapLegendVisible = false) }
        }
    }

    fun openFullMapOverlay() {
        viewModelScope.launch(dispatchers.main) {
            val room = _uiState.value.currentRoom ?: return@launch
            playUiCue("click")
            val fullMap = buildFullMapState(room)
            _uiState.update { it.copy(isFullMapVisible = true, fullMap = fullMap) }
        }
    }

    fun closeFullMapOverlay() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("click")
            _uiState.update { it.copy(isFullMapVisible = false) }
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

    fun openQuestDetails(questId: String) {
        viewModelScope.launch(dispatchers.main) {
            val detail = buildQuestDetail(
                questId = questId,
                questState = questRuntimeManager.state.value,
                questRepository = questRepository,
                runtimeManager = questRuntimeManager,
                inventoryService = inventoryService
            )
            if (detail != null) {
                _uiState.update { it.copy(questDetail = detail) }
            } else {
                postStatus("Quest details unavailable.")
            }
        }
    }

    fun closeQuestDetails() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(questDetail = null) }
        }
    }

    fun toggleQuestTracking(questId: String) {
        viewModelScope.launch(dispatchers.main) {
            val current = sessionStore.state.value.trackedQuestId
            val next = if (current != null && current.equals(questId, ignoreCase = true)) {
                null
            } else questId
            questRuntimeManager.trackQuest(next)
            _uiState.update { state ->
                val updatedDetail = state.questDetail?.takeIf { it.id.equals(questId, ignoreCase = true) }
                    ?.copy(tracked = next != null)
                state.copy(questDetail = updatedDetail ?: state.questDetail)
            }
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


    private var lastMenuTab: MenuTab = MenuTab.INVENTORY

    fun openMenuOverlay(defaultTab: MenuTab? = null) {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("menu_open")
            val tab = defaultTab ?: lastMenuTab
            lastMenuTab = tab
            _uiState.update { it.copy(isMenuOverlayVisible = true, menuTab = tab) }
            maybeShowInventoryTutorial(tab)
        }
    }

    fun closeMenuOverlay() {
        viewModelScope.launch(dispatchers.main) {
            playUiCue("menu_close")
            _uiState.update { it.copy(isMenuOverlayVisible = false) }
        }
    }

    fun requestReturnToHub() {
        viewModelScope.launch(dispatchers.main) {
            val currentRoom = _uiState.value.currentRoom
            if (!canReturnToHub(currentRoom)) {
                playUiCue("error")
                postStatus("You can only return to the hub from the node entrance.")
                return@launch
            }
            playUiCue("menu_action")
            emitEvent(ExplorationEvent.ReturnToHub)
        }
    }

    fun openSkillTree(characterId: String) {
        viewModelScope.launch(dispatchers.main) {
            val overlay = buildSkillTreeOverlay(characterId, sessionStore.state.value)
            if (overlay != null) {
                playUiCue("menu_open")
                _uiState.update { it.copy(skillTreeOverlay = overlay) }
            } else {
                val name = charactersById[characterId]?.name ?: "that character"
                postStatus("Skill data unavailable for $name.")
            }
        }
    }

    fun closeSkillTreeOverlay() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(skillTreeOverlay = null) }
        }
    }

    fun openPartyMemberDetails(characterId: String) {
        viewModelScope.launch(dispatchers.main) {
            val character = charactersById[characterId] ?: run {
                postStatus("Unable to show details for that character.")
                return@launch
            }
            val sessionState = sessionStore.state.value
            val xp = sessionState.partyMemberXp[character.id]
                ?: sessionState.playerXp.takeIf { character.id == sessionState.playerId }
                ?: character.xp
            val level = sessionState.partyMemberLevels[character.id]
                ?: sessionState.playerLevel.takeIf { character.id == sessionState.playerId }
                ?: character.level
            val (startXp, nextXp) = levelingManager.levelBounds(level)
            val xpLabel = buildString {
                append(xp)
                append(" XP")
                nextXp?.let { next ->
                    val toNext = max(next - xp, 0)
                    append(" • ")
                    append(toNext)
                    append(" to next")
                }
            }
            val maxHp = character.hp.coerceAtLeast(0)
            val currentHp = (sessionState.partyMemberHp[character.id] ?: maxHp).coerceAtLeast(0)
            val hpLabel = if (maxHp > 0) "$currentHp / $maxHp HP" else null
            val maxFocus = character.focus.coerceAtLeast(0)
            val currentFocus = (sessionState.partyMemberRp[character.id] ?: maxFocus).coerceAtLeast(0)
            val focusLabel = if (maxFocus > 0) "$currentFocus / $maxFocus Focus" else null

            val primaryStats = listOf(
                CharacterStatValueUi("Strength", character.strength.toString()),
                CharacterStatValueUi("Vitality", character.vitality.toString()),
                CharacterStatValueUi("Agility", character.agility.toString()),
                CharacterStatValueUi("Focus", character.focus.toString()),
                CharacterStatValueUi("Luck", character.luck.toString())
            )
            val combatStats = listOf(
                CharacterStatValueUi("Attack", CombatFormulas.attackPower(character.strength).toString()),
                CharacterStatValueUi("Defense", CombatFormulas.defensePower(character.vitality).toString()),
                CharacterStatValueUi(
                    "Speed",
                    String.format(Locale.getDefault(), "%.1f", CombatFormulas.speed(0, character.agility))
                ),
                CharacterStatValueUi(
                    "Accuracy",
                    String.format(Locale.getDefault(), "%.1f%%", CombatFormulas.accuracy(character.focus))
                ),
                CharacterStatValueUi(
                    "Evasion",
                    String.format(Locale.getDefault(), "%.1f%%", CombatFormulas.evasion(character.agility))
                ),
                CharacterStatValueUi(
                    "Crit Rate",
                    String.format(Locale.getDefault(), "%.1f%%", CombatFormulas.critChance(character.focus))
                ),
                CharacterStatValueUi(
                    "Resistance",
                    CombatFormulas.generalResistance(character.focus).toString()
                )
            )
            val unlockedSkillNames = sessionState.unlockedSkills
                .mapNotNull { skillId ->
                    skillsById[skillId]?.takeIf { it.character == character.id }?.name
                }
                .sortedBy { it.lowercase(Locale.getDefault()) }

            playUiCue("menu_open")
            _uiState.update {
                it.copy(
                    partyMemberDetails = PartyMemberDetailsUi(
                        id = character.id,
                        name = character.name,
                        level = level,
                        xpLabel = xpLabel,
                        hpLabel = hpLabel,
                        focusLabel = focusLabel,
                        portraitPath = character.miniIconPath,
                        primaryStats = primaryStats,
                        combatStats = combatStats,
                        unlockedSkills = unlockedSkillNames
                    )
                )
            }
        }
    }

    fun closePartyMemberDetails() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(partyMemberDetails = null) }
        }
    }

    fun unlockSkillNode(nodeId: String) {
        viewModelScope.launch(dispatchers.main) {
            val overlay = _uiState.value.skillTreeOverlay ?: return@launch
            val branchNode = overlay.branches
                .flatMap { it.nodes }
                .firstOrNull { it.id == nodeId } ?: return@launch
            val status = branchNode.status
            if (status.unlocked) {
                postStatus("${branchNode.name} already learned.")
                return@launch
            }
            if (!status.canPurchase) {
                val unmetLabels = status.unmetRequirements.mapNotNull { unmetId ->
                    branchNode.requirements.firstOrNull { it.id == unmetId }?.label
                }
                val message = when {
                    !status.hasEnoughAp -> "Not enough Ability Points."
                    !status.meetsTierRequirement -> "Invest ${status.requiredApForTier} AP into this tree to reach the next tier."
                    unmetLabels.isNotEmpty() -> "Requires ${unmetLabels.joinToString(", ")}."
                    else -> "Cannot unlock ${branchNode.name} yet."
                }
                postStatus(message)
                return@launch
            }
            val definition = findSkillNode(overlay.characterId, nodeId) ?: return@launch
            val cost = definition.costAp.coerceAtLeast(0)
            if (!sessionStore.spendAp(cost)) {
                postStatus("Not enough Ability Points.")
                return@launch
            }
            sessionStore.unlockSkill(definition.id)
            playUiCue("confirm")
            postStatus("Learned ${branchNode.name}")
            val refreshed = buildSkillTreeOverlay(overlay.characterId, sessionStore.state.value)
            _uiState.update { it.copy(skillTreeOverlay = refreshed) }
        }
    }

    fun onMenuActionInvoked() {
        playUiCue("menu_action")
    }

    fun quickSave() {
        viewModelScope.launch(dispatchers.io) {
            val success = runCatching { saveRepository.quickSave() }.getOrElse { false }
            val message = if (success) "Quicksave complete." else "Unable to quicksave."
            postStatus(message)
        }
    }

    fun selectMenuTab(tab: MenuTab) {
        lastMenuTab = tab
        _uiState.update { it.copy(menuTab = tab) }
        maybeShowInventoryTutorial(tab)
    }

    fun updateMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (abs(clamped - userMusicVolume) < 0.001f) return
        userMusicVolume = clamped
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(musicVolume = clamped))
        }
        emitEvent(ExplorationEvent.AudioSettingsChanged(userMusicVolume, userSfxVolume))
        viewModelScope.launch(dispatchers.io) {
            userSettingsStore.setMusicVolume(clamped)
        }
    }

    fun updateSfxVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (abs(clamped - userSfxVolume) < 0.001f) return
        userSfxVolume = clamped
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(sfxVolume = clamped))
        }
        emitEvent(ExplorationEvent.AudioSettingsChanged(userMusicVolume, userSfxVolume))
        viewModelScope.launch(dispatchers.io) {
            userSettingsStore.setSfxVolume(clamped)
        }
    }

    private fun maybeShowInventoryTutorial(tab: MenuTab) {
        if (tab != MenuTab.INVENTORY) return
        if (tutorialManager.hasCompleted(BAG_TUTORIAL_ID)) return
        val scheduled = tutorialManager.playScript(
            scriptId = BAG_TUTORIAL_ID,
            allowDuplicates = false
        ) {
            tutorialManager.markCompleted(BAG_TUTORIAL_ID)
        }
        if (!scheduled) {
            tutorialManager.showOnce(
                entry = TutorialEntry(
                    key = BAG_TUTORIAL_ID,
                    context = "Inventory",
                    message = "Swipe up from the tray or tap the backpack icon to open your bag.\nUse the filters to switch between gear, consumables, and key items."
                ),
                allowDuplicates = false,
                onDismiss = { tutorialManager.markCompleted(BAG_TUTORIAL_ID) }
            )
        }
    }

    fun setVignetteEnabled(enabled: Boolean) {
        if (isVignetteEnabled == enabled) return
        isVignetteEnabled = enabled
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(vignetteEnabled = enabled))
        }
        viewModelScope.launch(dispatchers.io) {
            userSettingsStore.setVignetteEnabled(enabled)
        }
    }
    fun engageEnemy(enemyId: String) {
        val room = _uiState.value.currentRoom
        val parties = room?.let { roomEnemyParties(it) }.orEmpty()
        val encounterIds = when {
            parties.isEmpty() -> listOfNotNull(enemyId.takeIf { it.isNotBlank() })
            enemyId.isBlank() -> parties.firstOrNull().orEmpty()
            else -> parties.firstOrNull { party -> party.contains(enemyId) } ?: parties.firstOrNull().orEmpty()
        }
        if (encounterIds.isNotEmpty()) {
            room?.let { preparePendingEncounter(it, encounterIds) } ?: encounterCoordinator.clear()
            emitEvent(ExplorationEvent.EnterCombat(encounterIds))
        } else {
            encounterCoordinator.clear()
        }
    }

    private fun preparePendingEncounter(room: Room, encounterIds: List<String>) {
        val descriptor = buildEncounterDescriptor(room, encounterIds)
        encounterCoordinator.setPendingEncounter(descriptor)
    }

    private fun buildEncounterDescriptor(room: Room, encounterIds: List<String>): EncounterDescriptor {
        val overridesByEnemy = room.enemyInstances.orEmpty().groupBy { it.enemyId }
        if (overridesByEnemy.isEmpty()) {
            return EncounterDescriptor(encounterIds.map { enemyId ->
                EncounterEnemyInstance(enemyId = enemyId)
            })
        }
        val usedOverrides = mutableSetOf<RoomEnemyInstance>()
        val occurrenceCounters = mutableMapOf<String, Int>()
        val slots = encounterIds.map { enemyId ->
            val occurrence = (occurrenceCounters[enemyId] ?: 0) + 1
            occurrenceCounters[enemyId] = occurrence
            val candidates = overridesByEnemy[enemyId].orEmpty()
            val match = candidates.firstOrNull { it.occurrence == occurrence && it !in usedOverrides }
                ?: candidates.firstOrNull { it.occurrence == null && it !in usedOverrides }
            if (match != null) {
                usedOverrides += match
            }
            EncounterEnemyInstance(
                enemyId = enemyId,
                overrideDrops = match?.overrideDrops,
                extraDrops = match?.extraDrops.orEmpty()
            )
        }
        return EncounterDescriptor(slots)
    }

    private fun handleRoomEntryTutorials(room: Room) {
        tutorialManager.cancel("light_switch_hint")
        tutorialManager.cancel("swipe_hint")
        tutorialManager.markRoomVisited(room.id)
        if (room.id.equals("town_9", ignoreCase = true)) {
            val lightOn = booleanValueOf(room.state["light_on"]) ?: false
            if (!tutorialManager.hasCompleted("light_switch_touch") && !lightOn) {
                tutorialManager.scheduleScript(
                    key = "light_switch_hint",
                    scriptId = "scene_light_switch_hint",
                    delayMs = 14_000L
                )
            } else if (lightOn) {
                tutorialManager.cancel("light_switch_hint")
            }
        }
    }

    private fun onRoomStateChanged(roomId: String, stateKey: String, value: Boolean) {
        val currentRoom = _uiState.value.currentRoom
        if (roomId.equals("town_9", ignoreCase = true) && stateKey.equals("light_on", ignoreCase = true) && value) {
            sessionStore.markTutorialCompleted("light_switch_touch")
            tutorialManager.markCompleted("light_switch_touch")
            tutorialManager.cancel("light_switch_hint")
            if (!tutorialManager.hasCompleted("swipe_move")) {
                tutorialManager.scheduleScript(
                    key = "swipe_hint",
                    scriptId = "scene_swipe_movement",
                    delayMs = 3_000L
                )
            }
        }
        if (currentRoom?.id.equals(roomId, ignoreCase = true)) {
            val resolvedRoom = roomsById[roomId] ?: currentRoom ?: return
            if (stateKey.equals("light_on", ignoreCase = true) && value) {
                darkRoomEntryDirection.remove(roomId)
                if (!visitedRooms.contains(roomId)) {
                    visitedRooms.add(roomId)
                    markDiscovered(resolvedRoom)
                }
            }
            if (stateKey.equals("dark", ignoreCase = true)) {
                if (!value) {
                    darkRoomEntryDirection.remove(roomId)
                    if (!visitedRooms.contains(roomId)) {
                        visitedRooms.add(roomId)
                        markDiscovered(resolvedRoom)
                    }
                } else {
                    darkRoomEntryDirection.putIfAbsent(roomId, "")
                }
            }
            updateMinimap(resolvedRoom)
            _uiState.update { state ->
                state.copy(
                    availableConnections = visibleConnections(resolvedRoom),
                    canReturnToHub = canReturnToHub(resolvedRoom)
                )
            }
        }
    }

    private fun currentEnemies(): List<String> = _uiState.value.currentRoom?.let { roomEnemyParties(it).flatten() }.orEmpty()

    fun itemDisplayName(itemId: String): String = inventoryService.itemDisplayName(itemId)

    fun useInventoryItem(itemId: String, targetId: String? = null) {
        viewModelScope.launch(dispatchers.main) {
            when (val outcome = itemUseController.useItem(itemId, targetId)) {
                is ItemUseController.Result.Success -> {
                    postStatus(outcome.message)
                    emitEvent(ExplorationEvent.ItemUsed(outcome.result, outcome.message))
                }
                is ItemUseController.Result.Failure -> {
                    postStatus(outcome.message)
                }
            }
        }
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
        value: Boolean,
        notify: Boolean = true
    ): Boolean? {
        val stateKeySafe = stateKey?.takeUnless { it.isBlank() } ?: return null
        val roomId = roomIdOrNull ?: _uiState.value.currentRoom?.id ?: return null
        val changed = applyRoomStateValue(roomId, stateKeySafe, value)
        if (!changed) return value
        var darkStateChanged = false
        if (stateKeySafe.equals("light_on", ignoreCase = true)) {
            val targetDark = !value
            darkStateChanged = applyRoomStateValue(roomId, "dark", targetDark)
        }
        reevaluateBlockedDirections(roomId, silent = true)
        if (notify) {
            onRoomStateChanged(roomId, stateKeySafe, value)
            if (darkStateChanged) {
                onRoomStateChanged(roomId, "dark", !value)
            }
            emitStateStatusIfNeeded(stateKeySafe, value)
            updateActionHints(_uiState.value.currentRoom)
        }
        if (
            notify &&
            stateKeySafe.equals("power_on", ignoreCase = true) &&
            roomId.equals(STELLARIUM_GENERATOR_ROOM_ID, ignoreCase = true)
        ) {
            updateMineLighting(powerOn = value)
        }
        return value
    }

    private fun applyRoomStateValue(
        roomId: String,
        stateKey: String,
        value: Boolean
    ): Boolean {
        if (stateKey.equals("dark", ignoreCase = true) || stateKey.equals("light_on", ignoreCase = true)) {
            if (!isDarkCapableRoom(roomId)) {
                return false
            }
        }
        val states = roomStates.getOrPut(roomId) { mutableMapOf() }
        if (states[stateKey] == value) return false
        states[stateKey] = value
        updateRoomModelState(roomId, stateKey, value)
        return true
    }

    private fun isDarkCapableRoom(roomId: String?): Boolean =
        roomId != null && darkCapableRoomIds.contains(roomId)

    private fun sanitizeDarkStates() {
        val updated = roomsById.toMutableMap()
        roomsById.forEach { (roomId, room) ->
            if (!isDarkCapableRoom(roomId)) {
                val states = roomStates[roomId] ?: return@forEach
                var changed = false
                if (states.remove("dark") != null) changed = true
                if (states.remove("light_on") != null) changed = true
                if (changed) {
                    val newState = room.state.toMutableMap()
                    newState.remove("dark")
                    newState.remove("light_on")
                    updated[roomId] = room.copy(state = newState)
                }
            }
        }
        roomsById = updated
    }

    private fun updateMineLighting(powerOn: Boolean) {
        val generatorEnv = roomsById[STELLARIUM_GENERATOR_ROOM_ID]?.env ?: return
        val targetEnvKey = environmentKey(generatorEnv)
        val currentRoom = _uiState.value.currentRoom

        roomsById.values
            .filter { room ->
                environmentKey(room.env) == targetEnvKey &&
                    darkCapableRoomIds.contains(room.id)
            }
            .forEach { room ->
                setRoomStateValue(room.id, "dark", !powerOn, notify = false)
                setRoomStateValue(room.id, "light_on", powerOn, notify = false)
            }

        if (currentRoom != null && environmentKey(currentRoom.env) == targetEnvKey) {
            updateActionHints(currentRoom)
        }
        _uiState.update { it.copy(mineGeneratorOnline = powerOn) }
    }

    private fun isMineGeneratorOnline(): Boolean =
        roomStates[STELLARIUM_GENERATOR_ROOM_ID]?.get("power_on") == true

    private fun canReturnToHub(room: Room?): Boolean =
        room?.id?.let { entryRoomIds.contains(it) } == true

    private fun toggleRoomStateValue(
        roomIdOrNull: String?,
        stateKey: String?
    ): Boolean? {
        val stateKeySafe = stateKey?.takeUnless { it.isBlank() } ?: return null
        val roomId = roomIdOrNull ?: _uiState.value.currentRoom?.id ?: return null
        val states = roomStates.getOrPut(roomId) { mutableMapOf() }
        val newValue = !(states[stateKeySafe] ?: false)
        return setRoomStateValue(roomId, stateKeySafe, newValue)
    }

    private fun updateRoomModelState(roomId: String, stateKey: String, value: Boolean) {
        val room = roomsById[roomId] ?: return
        val updatedState = room.state.toMutableMap()
        updatedState[stateKey] = value
        val updatedRoom = room.copy(state = updatedState)
        roomsById = roomsById.toMutableMap().apply { put(roomId, updatedRoom) }
        if (_uiState.value.currentRoom?.id == roomId) {
            val snapshot = roomStates[roomId]?.toMap().orEmpty()
            val updatedActions = parseActions(updatedRoom)
            _uiState.update {
                it.copy(
                    currentRoom = updatedRoom,
                    roomState = snapshot,
                    actions = updatedActions,
                    actionHints = buildActionHints(updatedRoom, updatedActions),
                    availableConnections = visibleConnections(updatedRoom),
                    blockedDirections = computeBlockedDirections(updatedRoom),
                    canReturnToHub = canReturnToHub(updatedRoom)
                )
            }
            updateMinimap(updatedRoom)
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
    }

    private fun markDiscovered(room: Room) {
        if (isRoomDark(room)) return
        discoveredRooms.add(room.id)
    }

    private fun updateUnlockedAreas(areas: Set<String>) {
        val removed = unlockedAreaIds - areas
        if (removed.isNotEmpty()) {
            unlockedAreaIds.removeAll(removed)
            pendingUnlockedAreas.removeAll(removed)
        }
        val newIds = areas - unlockedAreaIds
        if (newIds.isNotEmpty()) {
            unlockedAreaIds.addAll(newIds)
            markUnlockedAreas(newIds)
        }
    }

    private fun markUnlockedAreas(areaIds: Collection<String>) {
        areaIds.forEach { areaId ->
            val room = roomsById[areaId]
            if (room != null) {
                markDiscovered(room)
                pendingUnlockedAreas.remove(areaId)
            } else {
                pendingUnlockedAreas.add(areaId)
            }
        }
    }

    private fun flushPendingUnlockedAreas() {
        if (pendingUnlockedAreas.isEmpty()) return
        val iterator = pendingUnlockedAreas.iterator()
        while (iterator.hasNext()) {
            val areaId = iterator.next()
            val room = roomsById[areaId]
            if (room != null) {
                markDiscovered(room)
                iterator.remove()
            }
        }
    }

    private fun updateUnlockedExits(exitKeys: Set<String>) {
        val removed = unlockedExitKeys - exitKeys
        if (removed.isNotEmpty()) {
            unlockedExitKeys.removeAll(removed)
            pendingUnlockedExitKeys.removeAll(removed)
        }
        val newKeys = exitKeys - unlockedExitKeys
        if (newKeys.isEmpty()) return
        val processedKeys = mutableSetOf<String>()
        newKeys.forEach { key ->
            val (roomId, direction) = parseExitKey(key) ?: return@forEach
            val room = roomsById[roomId]
            if (room == null) {
                pendingUnlockedExitKeys.add(key)
            } else {
                val block = room.blockedDirections?.get(direction.lowercase(Locale.getDefault()))
                unlockDirection(roomId, direction, block, silent = true)
                processedKeys.add(key)
            }
        }
        unlockedExitKeys.addAll(processedKeys)
    }

    private fun flushPendingUnlockedExits() {
        if (pendingUnlockedExitKeys.isEmpty()) return
        val iterator = pendingUnlockedExitKeys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val (roomId, direction) = parseExitKey(key) ?: continue
            val room = roomsById[roomId]
            if (room != null) {
                val block = room.blockedDirections?.get(direction.lowercase(Locale.getDefault()))
                unlockDirection(roomId, direction, block, silent = true)
                iterator.remove()
                unlockedExitKeys.add(key)
            }
        }
    }

    private fun parseExitKey(key: String): Pair<String, String>? {
        val parts = key.split(EXIT_KEY_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val roomId = parts[0].takeIf { it.isNotBlank() } ?: return null
        val direction = parts[1].takeIf { it.isNotBlank() } ?: return null
        return roomId to direction
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
        val unlocked = isTinkeringUnlocked()
        if (!unlocked && shopId != null) {
            val message = lockedMessage?.takeIf { it.isNotBlank() }
                ?: "The tinkering table isn't ready yet."
            postStatus(message)
            return
        }
        if (!unlocked && shopId == null) {
            sessionStore.setMilestone("ms_tinkering_tutorial_done")
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
                "if_milestone_not_set", "if_milestones_not_set" -> Unit
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
        if (grantedItems.isNotEmpty()) {
            val title = action.popupTitle?.takeIf { it.isNotBlank() }
                ?: action.name.ifBlank { "Supply Cache" }
            val announcement = if (grantedItems.size == 1) {
                "Recovered ${grantedItems.first()}."
            } else {
                val prefix = grantedItems.dropLast(1).joinToString(", ")
                "Recovered $prefix and ${grantedItems.last()}."
            }
            enqueueEventAnnouncement(title, announcement)
        }

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
                val resolvedRoom = currentRoom ?: it.currentRoom
                it.copy(
                    currentRoom = resolvedRoom,
                    blockedDirections = blocked,
                    roomState = getRoomStateSnapshot(roomId),
                    canReturnToHub = canReturnToHub(resolvedRoom)
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
        if (roomEnemyParties(room).isNotEmpty()) return true
        val destId = getConnection(room, direction) ?: return false
        val destRoom = roomsById[destId] ?: return false
        return roomEnemyParties(destRoom).isNotEmpty()
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
                val resolvedRoom = updatedRoom ?: it.currentRoom
                it.copy(
                    currentRoom = resolvedRoom,
                    blockedDirections = blocked,
                    canReturnToHub = canReturnToHub(resolvedRoom)
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
                    actionEventOn = action["action_event_on"].asStringOrNull()?.takeIf { it.isNotBlank() }
                        ?: action["action_event"].asStringOrNull()?.takeIf { it.isNotBlank() },
                    actionEventOff = action["action_event_off"].asStringOrNull()?.takeIf { it.isNotBlank() }
                        ?: action["action_event"].asStringOrNull()?.takeIf { it.isNotBlank() }
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

private fun buildQuestDetail(
    questId: String,
    questState: QuestRuntimeState,
    questRepository: QuestRepository,
    runtimeManager: QuestRuntimeManager,
    inventoryService: InventoryService
): QuestDetailUi? {
    val quest = questRepository.questById(questId) ?: return null
    val isCompleted = questState.completedQuestIds.contains(questId)
    val stage = if (isCompleted) {
        quest.stages.lastOrNull()
    } else {
        val stageId = questState.stageProgress[questId]
        quest.stages.firstOrNull { it.id == stageId } ?: quest.stages.firstOrNull()
    }
    val completedTasks = runtimeManager.completedTaskIds(questId)
    val objectives = stage?.tasks.orEmpty().map { task ->
        val done = isCompleted || task.done || completedTasks.contains(task.id)
        QuestObjectiveUi(
            id = task.id,
            text = task.text,
            completed = done
        )
    }
    val summary = quest.summary.takeIf { it.isNotBlank() } ?: quest.description.takeIf { it.isNotBlank() } ?: ""
    val rewards = quest.rewards.map { formatQuestReward(it, inventoryService) }
    val stageIndex = quest.stages.indexOf(stage).takeIf { it >= 0 } ?: 0
    return QuestDetailUi(
        id = quest.id,
        title = quest.title,
        summary = summary,
        description = quest.description.takeIf { it.isNotBlank() },
        stageTitle = stage?.title,
        stageDescription = stage?.description,
        objectives = objectives,
        rewards = rewards,
        stageIndex = stageIndex,
        totalStages = quest.stages.size.coerceAtLeast(1),
        tracked = questState.trackedQuestId?.equals(questId, ignoreCase = true) == true
    )
}

private fun formatQuestReward(reward: QuestReward, inventoryService: InventoryService): String {
    val amount = reward.amount ?: 0
    return when (reward.type.lowercase(Locale.getDefault())) {
        "xp" -> "+$amount XP"
        "credit", "credits" -> "+$amount credits"
        "item", "items" -> {
            val name = reward.itemId?.let { inventoryService.itemDisplayName(it) } ?: "Item"
            if (amount > 1) "${amount} x $name" else name
        }
        else -> reward.itemId?.takeIf { it.isNotBlank() } ?: reward.type
    }
}

private fun sanitizeEnemyList(source: List<String>): List<String> =
    source.map { it.trim() }.filter { it.isNotEmpty() }

private fun sanitizeEnemyParties(raw: List<List<String>>?): List<List<String>>? {
    if (raw.isNullOrEmpty()) return null
    val sanitized = raw.map { sanitizeEnemyList(it) }.filter { it.isNotEmpty() }
    return sanitized.takeIf { it.isNotEmpty() }
}

private fun normalizeRoomEnemies(room: Room): Room {
    val parties = sanitizeEnemyParties(room.enemyParties)
    val singles = sanitizeEnemyList(room.enemies)
    return when {
        parties != null -> room.copy(enemyParties = parties, enemies = parties.flatten())
        singles != room.enemies -> room.copy(enemies = singles)
        else -> room
    }
}

private fun roomEnemyParties(room: Room): List<List<String>> {
    val parties = sanitizeEnemyParties(room.enemyParties)
    if (!parties.isNullOrEmpty()) return parties
    val singles = sanitizeEnemyList(room.enemies)
    return singles.map { listOf(it) }
}

private fun removeDefeatedEnemies(room: Room, defeatedIds: List<String>): Room {
    val parties = roomEnemyParties(room)
    val targetIndex = parties.indexOfFirst { partiesMatch(it, defeatedIds) }
    val updatedParties = if (targetIndex >= 0) {
        parties.toMutableList().apply { removeAt(targetIndex) }
    } else {
        parties.map { party ->
            val remaining = party.toMutableList()
            defeatedIds.forEach { id ->
                val idx = remaining.indexOfFirst { candidate -> candidate == id }
                if (idx >= 0) {
                    remaining.removeAt(idx)
                }
            }
            remaining
        }.filter { it.isNotEmpty() }
    }
    val sanitized = sanitizeEnemyParties(updatedParties)
    return if (room.enemyParties != null) {
        room.copy(
            enemyParties = sanitized,
            enemies = sanitized?.flatten().orEmpty()
        )
    } else {
        room.copy(enemies = sanitized?.flatten().orEmpty())
    }
}

private fun partiesMatch(party: List<String>, defeatedIds: List<String>): Boolean {
    if (party.size != defeatedIds.size) return false
    val left = party.groupingBy { it }.eachCount()
    val right = defeatedIds.groupingBy { it }.eachCount()
    return left == right
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
    val resolvedTitle = questTitle ?: quest?.title
    return QuestLogEntryUi(
        questId = questId,
        message = message,
        stageId = stageId,
        stageTitle = stage?.title,
        timestamp = timestamp,
        type = type,
        questTitle = resolvedTitle
    )
}

private fun TutorialEntry.toUiPrompt(): TutorialPrompt = TutorialPrompt(this)

sealed interface ExplorationEvent {
    data class EnterCombat(val enemyIds: List<String>) : ExplorationEvent
    data class PlayCinematic(val sceneId: String) : ExplorationEvent
    data class ShowMessage(val message: String) : ExplorationEvent
    data class ShowToast(val id: String, val message: String) : ExplorationEvent
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
    data class AudioSettingsChanged(val musicVolume: Float, val sfxVolume: Float) : ExplorationEvent
    data object ReturnToHub : ExplorationEvent
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
