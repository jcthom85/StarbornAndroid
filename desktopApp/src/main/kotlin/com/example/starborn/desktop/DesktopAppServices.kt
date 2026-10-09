package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.data.repository.*
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioCatalog
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.dialogue.isDialogueConditionMet
import com.example.starborn.domain.dialogue.handleDialogueTrigger
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.fx.UiFxBus
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.leveling.LevelingData
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.MilestoneEffects
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.telemetry.LocalPlaytestTelemetry
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.tutorial.TutorialScriptRepository
import com.example.starborn.ui.events.UiEventBus
import com.example.starborn.feature.arcade.domain.ArcadeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import com.example.starborn.domain.session.SessionInventoryBridge
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import java.io.File

/**
 * Service container for Desktop (Windows) environment.
 */
class DesktopAppServices(
    val saveDirectory: File = File(System.getProperty("user.home"), ".starborn"),
    audioDriverOverride: AudioDriver? = null
) {
    init {
        saveDirectory.mkdirs()
    }

    val isDebugEnabled: Boolean = System.getProperty("starborn.debug") == "true" || System.getenv("STARBORN_DEBUG") == "true"

    val assetProvider: AssetProvider = DesktopAssetProvider()
    val moshi = MoshiProvider.instance
    val assetReader = AssetJsonReader(assetProvider, moshi)

    val worldDataSource = WorldAssetDataSource(assetReader)
    val characterDefinitions by lazy { worldDataSource.loadCharacters().associateBy { it.id } }
    val enemyDefinitions by lazy { worldDataSource.loadEnemies().associateBy { it.id } }
    val skillDefinitions by lazy { worldDataSource.loadSkills().associateBy { it.id } }
    val roomDefinitions by lazy { worldDataSource.loadRooms().associateBy { it.id } }
    fun roomTitle(id: String): String = roomDefinitions[id]?.title ?: id
    val worldDefinitions by lazy { worldDataSource.loadWorlds().associateBy { it.id } }
    val themeDataSource = ThemeAssetDataSource(assetReader)
    val themeStyleDataSource = ThemeStyleAssetDataSource(assetReader)
    val dialogueDataSource = DialogueAssetDataSource(assetReader)
    val eventDataSource = EventAssetDataSource(assetReader)
    val itemRepository = ItemRepository(ItemAssetDataSource(assetReader))
    val craftingDataSource = CraftingAssetDataSource(assetReader)
    val cinematicDataSource = CinematicAssetDataSource(assetReader, moshi)
    val shopDataSource = ShopAssetDataSource(assetReader)
    val fishingDataSource = FishingAssetDataSource(assetReader)
    val milestoneDataSource = MilestoneAssetDataSource(assetReader)

    val questRepository = QuestRepository(QuestAssetDataSource(assetReader)).apply { load() }
    val shopRepository = ShopRepository(shopDataSource).apply { load() }
    val milestoneRepository = MilestoneRepository(milestoneDataSource).apply { load() }
    val themeRepository = ThemeRepository(themeDataSource, themeStyleDataSource).apply { load() }
    val environmentThemeManager = EnvironmentThemeManager(themeRepository)

    val inventoryService = InventoryService(itemRepository).apply { loadItems() }
    val sessionStore = GameSessionStore()
    private val inventoryBridge = SessionInventoryBridge(sessionStore, inventoryService)
    val arcadeService = ArcadeService(sessionStore, inventoryService)

    val playtestTelemetry = LocalPlaytestTelemetry(File(saveDirectory, "playtest")).apply {
        startSession("desktop_app_launch")
    }

    val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
    val events: List<GameEvent> = eventDataSource.loadEvents()
    val statusRegistry = StatusRegistry(worldDataSource.loadStatuses())
    private val schematicNames by lazy { craftingDataSource.loadTinkeringRecipes().associate { it.id to it.name } }
    fun contentName(id: String): String = statusRegistry.definition(id)?.let { it.displayName ?: it.name }
        ?: schematicNames[id] ?: itemRepository.findItem(id)?.name ?: id
    val combatEngine = CombatEngine(statusRegistry = statusRegistry)
    val encounterCoordinator = EncounterCoordinator()
    val levelingManager = LevelingManager(worldDataSource.loadLevelingData() ?: LevelingData())
    val progressionData: ProgressionData = worldDataSource.loadProgressionData() ?: ProgressionData()

    val cinematicService = CinematicService(cinematicDataSource)
    val cinematicCoordinator = CinematicCoordinator(cinematicService)

    val audioBindings: AudioBindings = assetReader.readObject<AudioBindings>("audio_bindings.json") ?: AudioBindings()
    val audioCatalog: AudioCatalog = assetReader.readObject<AudioCatalog>("audio_catalog.json") ?: AudioCatalog()
    val audioDriver: AudioDriver = audioDriverOverride ?: DesktopAudioDriver(assetProvider)
    val audioRouter = AudioRouter(audioBindings, audioCatalog)

    val uiFxBus = UiFxBus()
    val uiEventBus = UiEventBus()
    val questPresentations = com.example.starborn.desktop.ui.DesktopQuestPresentationState()
    val promptManager = UIPromptManager()

    val fishingService = FishingService(fishingDataSource, inventoryService)
    val tutorialScripts = TutorialScriptRepository(assetReader)
    val userSettingsStore = DesktopUserSettingsStore(saveDirectory)
    val saveManager = DesktopSaveManager(saveDirectory) { id ->
        worldDataSource.loadRooms().firstOrNull { it.id == id }?.title ?: id
    }

    @Volatile private var sessionActive = false
    private var lastAutosaveAt = 0L
    var playIntroOnStart: Boolean = false
        private set

    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val voiceoverController = VoiceoverController(
        audioRouter = audioRouter,
        dispatchCommands = { audioDriver.executeAll(it) },
        scope = runtimeScope,
        dispatcher = Dispatchers.Default
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

    private var dialogueTriggerListener: ((String) -> Boolean)? = null

    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            com.example.starborn.domain.dialogue.isDialogueConditionMet(condition, sessionStore.state.value, inventoryService)
        },
        DialogueTriggerHandler { trigger ->
            if (dialogueTriggerListener?.invoke(trigger) != true) {
                com.example.starborn.domain.dialogue.handleDialogueTrigger(
                    trigger = trigger,
                    sessionStore = sessionStore,
                    questRuntimeManager = questRuntimeManager,
                    inventoryService = inventoryService,
                    onMilestoneSet = { milestone: String ->
                        milestoneManager.handleMilestone(milestone, null)
                        milestoneManager.applyEffectsFor(milestone)
                    }
                )
            }
        }
    )

    private var explorationInstance: com.example.starborn.feature.exploration.viewmodel.ExplorationController? = null
    private val bootstrapCinematics = mutableListOf<String>()
    private val bootstrapActions = mutableListOf<String>()
    var activeFishingZone: String? = null
    var activeArcadeCabinet: String? = null
    var openTinkeringOnNextFieldKit = false

    private val runtimeSaves = object : com.example.starborn.domain.session.SessionSaveRepository {
        override suspend fun slotInfos() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { (1..3).map { it to saveManager.slotInfo(it) } }
        override suspend fun save(slot: Int) {
            val snapshot = inventoryBridge.snapshot()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                check(saveManager.saveGame(slot, snapshot)) { saveManager.lastError.value.orEmpty() }
            }
        }
        override suspend fun load(slot: Int) = loadSlot(slot, resetRuntime = false)
        override suspend fun quickSave(): Boolean {
            val snapshot = inventoryBridge.snapshot()
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { saveManager.saveGame(-1, snapshot) }
        }
        override suspend fun loadQuickSave() = loadSlot(-1, resetRuntime = false)
        override suspend fun quickSaveInfo() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { saveManager.slotInfo(-1) }
        override suspend fun clearQuickSave() = clear(-1)
        override suspend fun clearAutosave() = clear(0)
        override suspend fun clear(slot: Int) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { check(saveManager.deleteSlot(slot)) } }
    }

    val exploration: com.example.starborn.feature.exploration.viewmodel.ExplorationController
        get() = explorationInstance ?: com.example.starborn.feature.exploration.viewmodel.ExplorationController(
            worldAssets = worldDataSource, sessionStore = sessionStore, dialogueService = dialogueService,
            inventoryService = inventoryService, craftingService = craftingService,
            cinematicCoordinator = cinematicCoordinator, questRepository = questRepository,
            questRuntimeManager = questRuntimeManager, milestoneManager = milestoneManager,
            audioRouter = audioRouter, voiceoverController = voiceoverController,
            shopRepository = shopRepository, themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager, levelingManager = levelingManager,
            tutorialManager = tutorialManager, promptManager = promptManager, fishingService = fishingService,
            saveRepository = runtimeSaves, encounterCoordinator = encounterCoordinator,
            userSettingsStore = userSettingsStore, eventDefinitions = events,
            bootstrapCinematics = bootstrapCinematics.toList(), bootstrapActions = bootstrapActions.toList(),
            parentScope = CoroutineScope(runtimeScope.coroutineContext + Dispatchers.Main.immediate),
            dialogueTriggerBinder = { dialogueTriggerListener = it },
            telemetry = playtestTelemetry
        ).also { explorationInstance = it; bootstrapCinematics.clear(); bootstrapActions.clear(); recoverMissedJedReward() }

    private fun recoverMissedJedReward() {
        val state = sessionStore.state.value
        if ("ms_w1_mq01_jed_talked" in state.completedMilestones &&
            "w1_mq01_receive_starter_kit" !in state.completedEvents) {
            dialogueTriggerListener?.invoke("player_action:w1_mq01_receive_starter_kit")
        }
    }

    private fun resetExploration() {
        questPresentations.clear()
        explorationInstance?.close()
        explorationInstance = null
        cinematicCoordinator.cancelAll()
        bootstrapCinematics.clear()
        bootstrapActions.clear()
    }

    fun hasExistingSave(): Boolean {
        return (-1..3).any { saveManager.hasSave(it) }
    }

    fun completedGameForNewGamePlus(): GameSessionState? {
        sessionStore.state.value.takeIf { "ms_game_complete" in it.completedMilestones }?.let { return it }
        return (-1..3).mapNotNull { saveManager.slotInfo(it) }
            .filter { "ms_game_complete" in it.state.completedMilestones }.maxByOrNull { it.savedAtMillis ?: 0L }?.state
    }

    fun startNewGamePlus(): Boolean {
        val previous = completedGameForNewGamePlus() ?: return false
        if (!startNewGame()) return false
        val seed = com.example.starborn.domain.session.GameBootstrap.newGamePlus(worldDataSource.loadCharacters(), itemRepository.allItems().toList(), previous)
        inventoryBridge.restore(seed.migrateOpeningNarrativeState())
        sessionStore.resetTutorialProgress()
        sessionStore.resetQuestProgress()
        questRuntimeManager.resetAll()
        sessionStore.startQuest(com.example.starborn.domain.session.GameBootstrap.startingQuest, track = true)
        sessionStore.setQuestStage(com.example.starborn.domain.session.GameBootstrap.startingQuest, com.example.starborn.domain.session.GameBootstrap.startingStage)
        saveManager.saveGame(0, inventoryBridge.snapshot())
        return true
    }

    fun startNewGame(debugFullInventory: Boolean = false): Boolean {
        sessionActive = false
        resetExploration()
        lastAutosaveAt = System.currentTimeMillis()
        promptManager.dismissCurrent()
        tutorialManager.cancelAllScheduled()
        milestoneManager.clearHistory()

        val players = runCatching { worldDataSource.loadCharacters() }.getOrNull().orEmpty()
        val seedState = com.example.starborn.domain.session.GameBootstrap.newGame(players, itemRepository.allItems().toList(), debugFullInventory)
        val party = seedState.partyMembers
        val baseLevel = seedState.playerLevel
        inventoryBridge.restore(seedState.migrateOpeningNarrativeState())
        sessionStore.resetTutorialProgress()
        sessionStore.resetQuestProgress()
        inventoryService.restore(emptyMap())
        if (debugFullInventory) {
            inventoryService.restore(itemRepository.allItems().associate { it.id to 99 })
            sessionStore.addCredits(50_000)
        }
        com.example.starborn.domain.session.GameBootstrap.startingSkills(party, players, progressionData, baseLevel,
            worldDataSource.loadSkills(), debugFullInventory).forEach(sessionStore::unlockSkill)
        questRuntimeManager.resetAll()
        sessionStore.startQuest(com.example.starborn.domain.session.GameBootstrap.startingQuest, track = true)
        sessionStore.setQuestStage(com.example.starborn.domain.session.GameBootstrap.startingQuest, com.example.starborn.domain.session.GameBootstrap.startingStage)

        sessionActive = true
        playIntroOnStart = true
        when {
            cinematicService.scene("intro_prologue") != null -> bootstrapCinematics.add("intro_prologue")
            cinematicService.scene("new_game_intro") != null -> bootstrapCinematics.add("new_game_intro")
        }
        bootstrapActions.add("new_game_spawn_player_and_fade")
        saveManager.saveGame(0, inventoryBridge.snapshot(), "Nova's Bunk")
        return true
    }

    fun startDebugScenario(scenarioId: String): Boolean {
        val scenario = com.example.starborn.feature.mainmenu.DebugScenarioCatalog.scenarios.firstOrNull { it.id == scenarioId } ?: return false
        val hubs = worldDataSource.loadHubs()
        val nodes = worldDataSource.loadHubNodes()
        val startingRoom = desktopDebugRooms[scenarioId]
            ?: hubs.firstOrNull { it.id == scenarioId }?.let { hub -> nodes.firstOrNull { it.hubId == hub.id }?.entryRoom }
            ?: return false
        if (startingRoom !in roomDefinitions) return false
        val node = nodes.firstOrNull { startingRoom == it.entryRoom || startingRoom in it.rooms }
        val hub = hubs.firstOrNull { it.id == node?.hubId }
            ?: if (scenarioId == "weather_lab") hubs.firstOrNull { it.id == "hub_1_homestead" } else null
        if (hub == null) return false
        val worldId = hub.worldId
        val hubId = hub.id
        val party = when (worldId) {
            "world_6", "world_5", "world_4" -> listOf("nova", "zeke", "orion", "gh0st")
            "world_3" -> listOf("nova", "zeke", "orion")
            "world_2" -> if (scenarioId in listOf("w2_hunter_canopy", "w2_source_gate", "w2_astra_repair")) listOf("nova", "zeke", "orion") else listOf("nova", "zeke")
            else -> if (scenarioId == "tut_party_combat") listOf("nova", "zeke") else listOf("nova")
        }
        val level = when (worldId) {
            "world_6" -> 25
            "world_5" -> 20
            "world_4" -> 15
            "world_3" -> 10
            "world_2" -> 5
            else -> 2
        }

        sessionActive = false
        resetExploration()
        val initialState = GameSessionState(
            worldId = worldId,
            hubId = hubId,
            roomId = startingRoom,
            playerId = party.firstOrNull() ?: "nova",
            playerLevel = level,
            partyMembers = party,
            partyMemberLevels = party.associateWith { level },
            partyMemberXp = party.associateWith { 0 },
            partyMemberHp = party.associateWith { characterDefinitions[it]?.hp ?: 100 },
            playerCredits = 500,
            unlockedWeapons = setOf("mining_pistol", "nova_laser_blaster", "zeke_shock_fists", "orion_prism_focus", "gh0st_whisperblade"),
            unlockedArmors = setOf("nova_flux_liner", "zeke_surge_harness", "orion_channeler_mantle", "gh0st_phaseweave_jacket"),
            equippedWeapons = party.mapNotNull { id -> com.example.starborn.domain.session.GameBootstrap.defaultWeapons[id]?.let { id to it } }.toMap(),
            equippedArmors = party.mapNotNull { id -> com.example.starborn.domain.session.GameBootstrap.defaultArmors[id]?.let { id to it } }.toMap()
        )
        inventoryBridge.restore(initialState.migrateOpeningNarrativeState())
        node?.let { sessionStore.visitNode(it.id) }
        inventoryService.restore(emptyMap())
        inventoryService.addItem("medkit", 5)
        sessionActive = false
        playIntroOnStart = false
        return true
    }

    fun loadSlot(slotIndex: Int, resetRuntime: Boolean = true): Boolean {
        val loaded = saveManager.loadGame(slotIndex) ?: return false
        if (resetRuntime) resetExploration()
        sessionActive = false
        lastAutosaveAt = System.currentTimeMillis()
        promptManager.dismissCurrent()
        tutorialManager.cancelAllScheduled()
        inventoryBridge.restore(loaded)
        if (explorationInstance != null) recoverMissedJedReward()
        sessionActive = true
        playIntroOnStart = false
        return true
    }

    init {
        runtimeScope.launch(Dispatchers.Main.immediate, start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            uiEventBus.events.collect { questPresentations.accept(it) }
        }
        runtimeScope.launch(Dispatchers.IO) {
            userSettingsStore.settings.collectLatest { settings ->
                (audioDriver as? DesktopAudioDriver)?.setMasterGain(settings.masterVolume)
                audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.MUSIC, settings.musicVolume)
                audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.AMBIENT, settings.ambienceVolume)
                audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.UI, settings.sfxVolume)
                audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.BATTLE, settings.sfxVolume)
                audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.VOICE, settings.voiceVolume)
            }
        }
        runtimeScope.launch(Dispatchers.IO) {
            sessionStore.state.collectLatest { state ->
                if (!sessionActive || state.worldId == null) return@collectLatest
                val remaining = (90_000 - (System.currentTimeMillis() - lastAutosaveAt)).coerceAtLeast(0)
                delay(remaining)
                if (sessionActive && saveManager.saveGame(0, inventoryBridge.snapshot())) {
                    lastAutosaveAt = System.currentTimeMillis()
                }
            }
        }
    }

    fun close() {
        val shouldSave = sessionActive
        sessionActive = false
        resetExploration()
        runtimeScope.cancel()
        if (shouldSave && sessionStore.state.value.worldId != null) {
            saveManager.saveGame(0, inventoryBridge.snapshot())
        }
        inventoryBridge.close()
        userSettingsStore.close()
        audioDriver.release()
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
}
