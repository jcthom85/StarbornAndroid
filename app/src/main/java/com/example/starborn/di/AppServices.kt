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
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
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
    val levelingManager = LevelingManager(worldDataSource.loadLevelingData() ?: LevelingData())
    val progressionData: ProgressionData = worldDataSource.loadProgressionData() ?: ProgressionData()
    val cinematicService = CinematicService(cinematicDataSource)
    private val audioBindings: AudioBindings = assetReader.readObject<AudioBindings>("audio_bindings.json") ?: AudioBindings()
    private val audioCatalog: AudioCatalog = assetReader.readObject<AudioCatalog>("audio_catalog.json") ?: AudioCatalog()
    val audioCuePlayer = AudioCuePlayer(context)
    val audioRouter = AudioRouter(audioBindings, audioCatalog)
    val uiFxBus = UiFxBus()
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

    companion object {
        private const val AUTOSAVE_INTERVAL_MS = 90_000L
        private const val AUTOSAVE_SLOT = 0
    }

    val promptManager = UIPromptManager()
    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            isDialogueConditionMet(condition, sessionStore.state.value, inventoryService)
        },
        DialogueTriggerHandler { trigger ->
            handleDialogueTrigger(trigger, sessionStore)
        }
    )
    val questRuntimeManager = QuestRuntimeManager(questRepository, sessionStore, promptManager, runtimeScope)
    val milestoneManager = MilestoneRuntimeManager(milestoneRepository, sessionStore, promptManager, runtimeScope)
    val tutorialManager = TutorialRuntimeManager(sessionStore, promptManager, tutorialScripts, runtimeScope)

    init {
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

    suspend fun saveSlot(slot: Int) {
        sessionPersistence.writeSlot(slot, sessionStore.state.value)
    }

    suspend fun loadSlot(slot: Int): Boolean {
        var info = sessionPersistence.slotInfo(slot)
        if (info == null || info.state.needsFallbackImport()) {
            if (importLegacySlotFromAssets(slot)) {
                info = sessionPersistence.slotInfo(slot)
            }
        }
        val state = info?.state ?: return false
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        return true
    }

    suspend fun clearSlot(slot: Int) {
        sessionPersistence.clearSlot(slot)
    }

    suspend fun slotState(slot: Int): GameSessionState? = slotInfo(slot)?.state

    suspend fun slotInfo(slot: Int): GameSessionSlotInfo? {
        var info = sessionPersistence.slotInfo(slot)
        if ((info == null || info.state.needsFallbackImport()) && importLegacySlotFromAssets(slot)) {
            info = sessionPersistence.slotInfo(slot)
        }
        return info
    }

    suspend fun loadAutosave(): Boolean {
        val info = sessionPersistence.autosaveInfo() ?: return false
        val state = info.state
        sessionStore.restore(state)
        inventoryService.restore(state.inventory)
        return true
    }

    suspend fun autosaveState(): GameSessionState? = sessionPersistence.autosaveInfo()?.state

    suspend fun autosaveInfo(): GameSessionSlotInfo? = sessionPersistence.autosaveInfo()

    suspend fun clearAutosave() {
        sessionPersistence.writeAutosave(GameSessionState())
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

    private fun GameSessionState.needsFallbackImport(): Boolean {
        val isPartyEmpty = playerId.isNullOrBlank() && partyMembers.isEmpty()
        val noProgress = worldId.isNullOrBlank() && hubId.isNullOrBlank() && roomId.isNullOrBlank()
        val noInventory = inventory.isEmpty() && playerCredits == 0
        return isPartyEmpty && noProgress && noInventory
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
        val seedState = GameSessionState(
            worldId = "nova_prime",
            hubId = "mining_colony",
            roomId = null,
            playerId = playerId,
            playerLevel = baseLevel,
            playerXp = baseXp,
            partyMembers = playerId?.let { listOf(it) } ?: emptyList(),
            partyMemberLevels = playerId?.let { mapOf(it to baseLevel) } ?: emptyMap(),
            partyMemberXp = playerId?.let { mapOf(it to baseXp) } ?: emptyMap()
        )
        sessionStore.restore(seedState)
        sessionStore.resetTutorialProgress()
        inventoryService.restore(emptyMap())
        questRuntimeManager.resetAll()

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

    private fun scheduleAutosave(state: GameSessionState) {
        autosaveJob?.cancel()
        val now = System.currentTimeMillis()
        val elapsed = now - lastAutosaveTimestamp
        val delayMs = if (elapsed >= AUTOSAVE_INTERVAL_MS) 0L else AUTOSAVE_INTERVAL_MS - elapsed
        autosaveJob = persistenceScope.launch {
            if (delayMs > 0) delay(delayMs)
            sessionPersistence.writeAutosave(state)
            lastAutosaveTimestamp = System.currentTimeMillis()
        }
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
            "milestone" -> value in state.completedMilestones
            "milestone_not_set" -> value !in state.completedMilestones
            "item" -> value.isNotBlank() && inventoryService.hasItem(value)
            "item_not" -> value.isNotBlank() && !inventoryService.hasItem(value)
            else -> true
        }
        if (!met) return false
    }
    return true
}

internal fun handleDialogueTrigger(trigger: String, sessionStore: GameSessionStore) {
    if (trigger.isBlank()) return
    trigger.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) return@forEach
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        when (type) {
            "start_quest" -> sessionStore.startQuest(value)
            "complete_quest" -> sessionStore.completeQuest(value)
            "set_milestone" -> sessionStore.setMilestone(value)
            "clear_milestone" -> sessionStore.clearMilestone(value)
            "recruit" -> sessionStore.addPartyMember(value)
        }
    }
}
