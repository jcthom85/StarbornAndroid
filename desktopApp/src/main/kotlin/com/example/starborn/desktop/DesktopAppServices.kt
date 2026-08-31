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
import java.io.File

/**
 * Service container for Desktop (Windows) environment.
 */
class DesktopAppServices(
    val saveDirectory: File = File(System.getProperty("user.home"), ".starborn")
) {
    init {
        saveDirectory.mkdirs()
    }

    val assetProvider: AssetProvider = DesktopAssetProvider()
    val moshi = MoshiProvider.instance
    val assetReader = AssetJsonReader(assetProvider, moshi)

    val worldDataSource = WorldAssetDataSource(assetReader)
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
    val arcadeService = ArcadeService(sessionStore, inventoryService)

    val playtestTelemetry = LocalPlaytestTelemetry(File(saveDirectory, "playtest")).apply {
        startSession("desktop_app_launch")
    }

    val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
    val events: List<GameEvent> = eventDataSource.loadEvents()
    val statusRegistry = StatusRegistry(worldDataSource.loadStatuses())
    val combatEngine = CombatEngine(statusRegistry = statusRegistry)
    val encounterCoordinator = EncounterCoordinator()
    val levelingManager = LevelingManager(worldDataSource.loadLevelingData() ?: LevelingData())
    val progressionData: ProgressionData = worldDataSource.loadProgressionData() ?: ProgressionData()

    val cinematicService = CinematicService(cinematicDataSource)
    val cinematicCoordinator = CinematicCoordinator(cinematicService)

    val audioBindings: AudioBindings = assetReader.readObject<AudioBindings>("audio_bindings.json") ?: AudioBindings()
    val audioCatalog: AudioCatalog = assetReader.readObject<AudioCatalog>("audio_catalog.json") ?: AudioCatalog()
    val audioDriver: AudioDriver = DesktopAudioDriver(assetProvider)
    val audioRouter = AudioRouter(audioBindings, audioCatalog)

    val uiFxBus = UiFxBus()
    val uiEventBus = UiEventBus()
    val promptManager = UIPromptManager()

    val fishingService = FishingService(fishingDataSource, inventoryService)
    val tutorialScripts = TutorialScriptRepository(assetReader)
    val userSettingsStore = DesktopUserSettingsStore(saveDirectory)

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

    val dialogueService: DialogueService = DialogueService(
        dialogueDataSource.loadDialogue(),
        DialogueConditionEvaluator { condition ->
            com.example.starborn.domain.dialogue.isDialogueConditionMet(condition, sessionStore.state.value, inventoryService)
        },
        DialogueTriggerHandler { trigger ->
            com.example.starborn.domain.dialogue.handleDialogueTrigger(
                trigger = trigger,
                sessionStore = sessionStore,
                questRuntimeManager = questRuntimeManager,
                inventoryService = inventoryService,
                onMilestoneSet = { milestone ->
                    milestoneManager.handleMilestone(milestone, null)
                    milestoneManager.applyEffectsFor(milestone)
                }
            )
        }
    )

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
