package com.example.starborn.di

import android.content.Context
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CinematicAssetDataSource
import com.example.starborn.data.assets.CraftingAssetDataSource
import com.example.starborn.data.assets.DialogueAssetDataSource
import com.example.starborn.data.assets.EventAssetDataSource
import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.assets.MilestoneAssetDataSource
import com.example.starborn.data.assets.ThemeAssetDataSource
import com.example.starborn.data.assets.ThemeStyleAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioCatalog
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.MilestoneEffects
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.fingerprint
import com.example.starborn.domain.session.importLegacySave
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.fx.UiFxBus
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.tutorial.TutorialScriptRepository
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Locale
import java.io.File

class AppServices(context: Context) {
    private val appContext = context.applicationContext
    private val moshi = MoshiProvider.instance
    private val assetReader = AssetJsonReader(context, moshi)

    val worldDataSource = WorldAssetDataSource(assetReader)
    private val themeDataSource = ThemeAssetDataSource(assetReader)
    private val themeStyleDataSource = ThemeStyleAssetDataSource(assetReader)
    private val dialogueDataSource = DialogueAssetDataSource(assetReader)
    private val eventDataSource = EventAssetDataSource(assetReader)
    val itemRepository = ItemRepository(ItemAssetDataSource(assetReader))
    private val craftingDataSource = CraftingAssetDataSource(assetReader)
    private val cinematicDataSource = CinematicAssetDataSource(assetReader, moshi)
    private val shopDataSource = ShopAssetDataSource(assetReader)
    private val fishingDataSource = FishingAssetDataSource(assetReader)
    private val milestoneDataSource = MilestoneAssetDataSource(assetReader)
    val questRepository: QuestRepository = QuestRepository(QuestAssetDataSource(assetReader)).apply { load() }
    val shopRepository: ShopRepository = ShopRepository(shopDataSource).apply { load() }
    val milestoneRepository: MilestoneRepository = MilestoneRepository(milestoneDataSource).apply { load() }
    val themeRepository: ThemeRepository = ThemeRepository(
        themeDataSource,
        themeStyleDataSource
    ).apply { load() }
    val environmentThemeManager = EnvironmentThemeManager(themeRepository)

    val inventoryService = InventoryService(itemRepository).apply { loadItems() }
    val sessionStore = GameSessionStore()
    private val sessionPersistence = GameSessionPersistence(context)
    val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
    val events: List<GameEvent> = eventDataSource.loadEvents()
    val statusRegistry = StatusRegistry(worldDataSource.loadStatuses())
    val combatEngine = CombatEngine(statusRegistry = statusRegistry)
    val encounterCoordinator = EncounterCoordinator()
    val levelingManager = LevelingManager(worldDataSource.loadLevelingData() ?: LevelingData())
    val progressionData: ProgressionData = worldDataSource.loadProgressionData() ?: ProgressionData()
    val cinematicService = CinematicService(cinematicDataSource)
    val cinematicCoordinator = CinematicCoordinator(cinematicService)
    val cinematicState = cinematicCoordinator.state
    fun playCinematic(sceneId: String?, onComplete: () -> Unit = {}): Boolean =
        cinematicCoordinator.play(sceneId, onComplete)

    fun cinematicStateFlow() = cinematicCoordinator.state
    private val audioBindings: AudioBindings = assetReader.readObject<AudioBindings>("audio_bindings.json") ?: AudioBindings()
    private val audioCatalog: AudioCatalog = assetReader.readObject<AudioCatalog>("audio_catalog.json") ?: AudioCatalog()
    val audioCuePlayer = AudioCuePlayer(context)
    val audioRouter = AudioRouter(audioBindings, audioCatalog)
    val uiFxBus = UiFxBus()
    val uiEventBus = UiEventBus()
    private var dialogueTriggerListener: ((String) -> Boolean)? = null
    val fishingService = FishingService(fishingDataSource, inventoryService)
    val tutorialScripts = TutorialScriptRepository(assetReader)
    val userSettingsStore = UserSettingsStore(appContext)
    private val bootstrapCinematics: ArrayDeque<String> = ArrayDeque()
    private val bootstrapPlayerActions: ArrayDeque<String> = ArrayDeque()

    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val voiceoverController = VoiceoverController(
        audioRouter = audioRouter,
        dispatchCommands = audioCuePlayer::execute,
        scope = runtimeScope,
        dispatcher = Dispatchers.Main
    )
    private var autosaveJob: Job? = null
    private var lastAutosaveTimestamp: Long = 0L
    private var lastAutosaveFingerprint: String? = null

    companion object {
        private const val AUTOSAVE_INTERVAL_MS = 90_000L
        private const val AUTOSAVE_SLOT = 0
        private const val SAMPLE_SLOT_ID = -1
        private val SAMPLE_PARTY = setOf("nova", "zeke", "orion", "gh0st")
        private const val SAMPLE_WORLD_ID = "nova_prime"
        private const val SAMPLE_HUB_ID = "mining_colony"
        private const val SAMPLE_ROOM_ID = "market_2"
    }

    val promptManager = UIPromptManager()
    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            isDialogueConditionMet(condition, sessionStore.state.value, inventoryService)
        },
        DialogueTriggerHandler { trigger ->
            val handled = dialogueTriggerListener?.invoke(trigger) == true
            if (!handled) {
                handleDialogueTrigger(trigger, sessionStore, questRuntimeManager, inventoryService)
            }
        }
    )
    val questRuntimeManager = QuestRuntimeManager(questRepository, sessionStore, runtimeScope, uiEventBus)
    val milestoneManager = MilestoneRuntimeManager(
        milestoneRepository,
        sessionStore,
        promptManager,
        runtimeScope,
        ::applyMilestoneEffects
    )
    val tutorialManager = TutorialRuntimeManager(sessionStore, promptManager, tutorialScripts, runtimeScope)

    init {
        questRuntimeManager.addQuestCompletionListener { questId ->
            milestoneManager.onQuestCompleted(questId)
        }
        persistenceScope.launch {
            sessionPersistence.sessionFlow.collect { stored ->
                sessionStore.restore(stored)
                inventoryService.restore(stored.inventory)
            }
        }
        persistenceScope.launch {
            sessionStore.state
                .drop(1)
                .distinctUntilChanged()
                .collect { state ->
                    sessionPersistence.persist(state)
                    scheduleAutosave(state)
                }
        }
        persistenceScope.launch {
            inventoryService.state.collect { entries ->
                val snapshot = entries.associate { it.item.id to it.quantity }.filterValues { it > 0 }
                if (snapshot != sessionStore.state.value.inventory) {
                    sessionStore.setInventory(snapshot)
                }
            }
        }
    }

    private fun applyMilestoneEffects(effects: MilestoneEffects) {
        effects.unlockAbilities.orEmpty().forEach { abilityId ->
            sessionStore.unlockSkill(abilityId)
        }
        effects.unlockAreas.orEmpty().forEach { areaId ->
            sessionStore.unlockArea(areaId)
        }
        effects.unlockExits.orEmpty().forEach { exit ->
            sessionStore.unlockExit(exit.roomId, exit.direction)
        }
    }

    suspend fun saveSlot(slot: Int) {
        sessionPersistence.writeSlot(slot, sessionStore.state.value)
    }

    suspend fun loadSlot(slot: Int): Boolean {
        val info = resolveSlotInfo(slot) ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        resetAutosaveThrottle()
        return true
    }

    suspend fun clearSlot(slot: Int) {
        sessionPersistence.clearSlot(slot)
    }

    suspend fun slotState(slot: Int): GameSessionState? = slotInfo(slot)?.state

    suspend fun slotInfo(slot: Int): GameSessionSlotInfo? = resolveSlotInfo(slot)

    suspend fun loadAutosave(): Boolean {
        val info = sessionPersistence.autosaveInfo() ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        recordAutosaveState(state)
        return true
    }

    suspend fun autosaveState(): GameSessionState? = sessionPersistence.autosaveInfo()?.state

    suspend fun autosaveInfo(): GameSessionSlotInfo? = sessionPersistence.autosaveInfo()

    suspend fun clearAutosave() {
        sessionPersistence.writeAutosave(GameSessionState())
        resetAutosaveThrottle()
    }

    suspend fun quickSave(): Boolean {
        sessionPersistence.writeQuickSave(sessionStore.state.value)
        return true
    }

    suspend fun loadQuickSave(): Boolean {
        val info = sessionPersistence.quickSaveInfo() ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        recordAutosaveState(state)
        return true
    }

    suspend fun quickSaveInfo(): GameSessionSlotInfo? = sessionPersistence.quickSaveInfo()

    suspend fun clearQuickSave() {
        sessionPersistence.clearQuickSave()
    }

    fun syncInventoryFromSession() {
        inventoryService.restore(sessionStore.state.value.inventory)
    }

    suspend fun importLegacySave(file: File): Boolean {
        val imported = sessionPersistence.importLegacySave(file, itemRepository) ?: return false
        sessionStore.restore(imported)
        inventoryService.restore(imported.inventory)
        return true
    }

    private suspend fun importLegacySlotFromAssets(slot: Int): Boolean {
        val assetName = "save$slot.json"
        if (!assetReader.assetExists(assetName)) return false
        val cacheFile = File(appContext.cacheDir, "legacy_slot_$slot.json")
        return try {
            appContext.assets.open(assetName).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            val imported = sessionPersistence.importLegacySave(cacheFile, itemRepository) ?: return false
            sessionPersistence.writeSlot(slot, imported)
            true
        } catch (t: Throwable) {
            false
        } finally {
            cacheFile.delete()
        }
    }

    suspend fun resetSlotFromAssets(slot: Int): Boolean =
        importLegacySlotFromAssets(slot)

    private suspend fun resolveSlotInfo(slot: Int): GameSessionSlotInfo? {
        var info = sessionPersistence.slotInfo(slot)
        if ((info == null || info.state.needsFallbackImport()) && importLegacySlotFromAssets(slot)) {
            info = sessionPersistence.slotInfo(slot)
        }
        if (slot == SAMPLE_SLOT_ID && (info == null || !info.state.matchesSampleSeed())) {
            if (importLegacySlotFromAssets(slot)) {
                info = sessionPersistence.slotInfo(slot)
            }
        }
        return info
    }

    private fun GameSessionState.needsFallbackImport(): Boolean {
        val isPartyEmpty = playerId.isNullOrBlank() && partyMembers.isEmpty()
        val noProgress = worldId.isNullOrBlank() && hubId.isNullOrBlank() && roomId.isNullOrBlank()
        val noInventory = inventory.isEmpty() && playerCredits == 0
        return isPartyEmpty && noProgress && noInventory
    }

    private fun GameSessionState.matchesSampleSeed(): Boolean {
        if (partyMembers.size != SAMPLE_PARTY.size) return false
        if (!partyMembers.containsAll(SAMPLE_PARTY)) return false
        if (!playerId.isNullOrBlank() && playerId !in SAMPLE_PARTY) return false
        val worldMatches = worldId.isNullOrBlank() || worldId == SAMPLE_WORLD_ID
        val hubMatches = hubId.isNullOrBlank() || hubId == SAMPLE_HUB_ID
        val roomMatches = roomId.isNullOrBlank() || roomId == SAMPLE_ROOM_ID
        return worldMatches && hubMatches && roomMatches
    }

    fun startNewGame() {
        bootstrapCinematics.clear()
        bootstrapPlayerActions.clear()
        promptManager.dismissCurrent()
        tutorialManager.cancelAllScheduled()
        milestoneManager.clearHistory()

        val defaultPlayer = runCatching { worldDataSource.loadCharacters().firstOrNull() }.getOrNull()
        val playerId = defaultPlayer?.id
        val baseLevel = defaultPlayer?.level ?: 1
        val baseXp = defaultPlayer?.xp ?: 0
        val startingRoomId = "town_9"
        val seedState = GameSessionState(
            worldId = "nova_prime",
            hubId = "mining_colony",
            roomId = startingRoomId,
            playerId = playerId,
            playerLevel = baseLevel,
            playerXp = baseXp,
            partyMembers = playerId?.let { listOf(it) } ?: emptyList(),
            partyMemberLevels = playerId?.let { mapOf(it to baseLevel) } ?: emptyMap(),
            partyMemberXp = playerId?.let { mapOf(it to baseXp) } ?: emptyMap()
        )
        sessionStore.restore(seedState)
        sessionStore.resetTutorialProgress()
        sessionStore.resetQuestProgress()
        inventoryService.restore(emptyMap())
        questRuntimeManager.resetAll()
        resetAutosaveThrottle()

        val introSceneId = when {
            cinematicService.scene("intro_prologue") != null -> "intro_prologue"
            cinematicService.scene("new_game_intro") != null -> "new_game_intro"
            else -> null
        }
        introSceneId?.let { bootstrapCinematics.add(it) }
        bootstrapPlayerActions.add("new_game_spawn_player_and_fade")
    }

    fun drainPendingCinematics(): List<String> =
        bootstrapCinematics.toList().also { bootstrapCinematics.clear() }

    fun drainPendingPlayerActions(): List<String> =
        bootstrapPlayerActions.toList().also { bootstrapPlayerActions.clear() }

    fun setDialogueTriggerListener(listener: ((String) -> Boolean)?) {
        dialogueTriggerListener = listener
    }

    private fun scheduleAutosave(state: GameSessionState) {
        val fingerprint = state.fingerprint()
        val now = System.currentTimeMillis()
        val elapsed = now - lastAutosaveTimestamp
        if (fingerprint == lastAutosaveFingerprint && elapsed < AUTOSAVE_INTERVAL_MS) {
            return
        }
        autosaveJob?.cancel()
        val delayMs = if (elapsed >= AUTOSAVE_INTERVAL_MS) 0L else AUTOSAVE_INTERVAL_MS - elapsed
        autosaveJob = persistenceScope.launch {
            if (delayMs > 0) delay(delayMs)
            sessionPersistence.writeAutosave(state)
            lastAutosaveTimestamp = System.currentTimeMillis()
            lastAutosaveFingerprint = fingerprint
        }
    }

    private fun recordAutosaveState(state: GameSessionState) {
        lastAutosaveFingerprint = state.fingerprint()
        lastAutosaveTimestamp = System.currentTimeMillis()
    }

    private fun resetAutosaveThrottle() {
        lastAutosaveFingerprint = null
        lastAutosaveTimestamp = 0L
    }
}

internal fun isDialogueConditionMet(
    condition: String?,
    state: GameSessionState,
    inventoryService: InventoryService
): Boolean {
    if (condition.isNullOrBlank()) return true
    val tokens = condition.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    for (token in tokens) {
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) continue
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        val met = when (type) {
            "quest" -> value in state.activeQuests || value in state.completedQuests
            "quest_active" -> value in state.activeQuests
            "quest_completed" -> value in state.completedQuests
            "quest_not_started" -> value.isNotBlank() &&
                value !in state.activeQuests &&
                value !in state.completedQuests &&
                value !in state.failedQuests
            "quest_failed" -> value in state.failedQuests
            "quest_stage" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId != null && stageId != null &&
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
            }
            "quest_stage_not" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId == null || stageId == null ||
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
            }
            "milestone" -> value in state.completedMilestones
            "milestone_not_set" -> value !in state.completedMilestones
            "item" -> value.isNotBlank() && inventoryService.hasItem(value)
            "item_not" -> value.isNotBlank() && !inventoryService.hasItem(value)
            "event_completed" -> value in state.completedEvents
            "event_not_completed" -> value.isNotBlank() && value !in state.completedEvents
            "tutorial_completed" -> value in state.tutorialCompleted
            "tutorial_not_completed" -> value.isNotBlank() && value !in state.tutorialCompleted
            else -> true
        }
        if (!met) return false
    }
    return true
}

private fun parseQuestStageCondition(raw: String): Pair<String?, String?> {
    if (raw.isBlank()) return null to null
    val parts = raw.split(':', limit = 2)
    val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
    val stageId = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
    return questId to stageId
}

internal fun handleDialogueTrigger(
    trigger: String,
    sessionStore: GameSessionStore,
    questRuntimeManager: QuestRuntimeManager,
    inventoryService: InventoryService? = null
) {
    val actions = DialogueTriggerParser.parse(trigger)
    if (actions.isEmpty()) return
    actions.forEach { action ->
        when (action.type.lowercase(Locale.getDefault())) {
            "start_quest" -> action.startQuest?.let {
                sessionStore.startQuest(it)
                questRuntimeManager.recordQuestStarted(it)
            }
            "complete_quest" -> action.completeQuest?.let {
                sessionStore.completeQuest(it)
                questRuntimeManager.markQuestCompleted(it)
                questRuntimeManager.recordQuestCompleted(it)
            }
            "fail_quest" -> action.questId?.let {
                sessionStore.failQuest(it)
                questRuntimeManager.markQuestFailed(it)
            }
            "set_milestone" -> action.milestone?.let { sessionStore.setMilestone(it) }
            "clear_milestone" -> action.milestone?.let { sessionStore.clearMilestone(it) }
            "track_quest" -> sessionStore.setTrackedQuest(action.questId)
            "untrack_quest" -> sessionStore.setTrackedQuest(null)
            "set_quest_task_done" -> {
                val questId = action.questId
                val taskId = action.taskId
                if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                    questRuntimeManager.markTaskComplete(questId, taskId)
                }
            }
            "advance_quest_stage" -> {
                val questId = action.questId
                val stageId = action.toStageId
                if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                    questRuntimeManager.setStage(questId, stageId)
                }
            }
            "give_xp" -> action.xp?.takeIf { it > 0 }?.let { sessionStore.addXp(it) }
            "give_reward" -> action.credits?.takeIf { it > 0 }?.let { sessionStore.addCredits(it) }
            "add_party_member" -> action.itemId?.let { sessionStore.addPartyMember(it) }
            "give_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.addItem(itemId, quantity)
                }
            }
            "take_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.removeItem(itemId, quantity)
                }
            }
        }
    }
}
