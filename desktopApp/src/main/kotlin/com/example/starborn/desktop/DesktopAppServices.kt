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
    val saveManager = DesktopSaveManager(saveDirectory)

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
                onMilestoneSet = { milestone: String ->
                    milestoneManager.handleMilestone(milestone, null)
                    milestoneManager.applyEffectsFor(milestone)
                }
            )
        }
    )

    private val defaultArmorsByCharacter = mapOf(
        "nova" to "nova_flux_liner",
        "zeke" to "zeke_surge_harness",
        "orion" to "orion_channeler_mantle",
        "gh0st" to "gh0st_phaseweave_jacket",
        "ollie" to "basic_vest"
    )

    private val defaultWeaponsByCharacter = mapOf(
        "nova" to "nova_laser_blaster",
        "zeke" to "zeke_shock_fists",
        "orion" to "orion_prism_focus",
        "gh0st" to "gh0st_whisperblade"
    )

    fun hasExistingSave(): Boolean {
        return (0..3).any { saveManager.hasSave(it) }
    }

    fun startNewGame(debugFullInventory: Boolean = false): Boolean {
        promptManager.dismissCurrent()
        tutorialManager.cancelAllScheduled()
        milestoneManager.clearHistory()

        val players = runCatching { worldDataSource.loadCharacters() }.getOrNull().orEmpty()
        val defaultPlayer = players.firstOrNull()
        val playerId = defaultPlayer?.id ?: "nova"
        val baseLevel = defaultPlayer?.level ?: 1
        val baseXp = defaultPlayer?.xp ?: 0
        val startingRoomId = "pit_nova_bunk"
        val party = if (debugFullInventory) listOf("nova", "zeke", "orion", "gh0st") else listOf(playerId)

        val startingUnlockedWeapons = mutableSetOf("mining_pistol", "nova_laser_blaster")
        val startingEquippedWeapons = mutableMapOf("nova" to "mining_pistol")
        val startingUnlockedArmors = mutableSetOf("nova_flux_liner", "basic_vest")
        val startingEquippedArmors = mutableMapOf("nova" to "nova_flux_liner")

        val seedState = GameSessionState(
            worldId = "world_1",
            hubId = "hub_1_homestead",
            roomId = startingRoomId,
            playerId = playerId,
            playerLevel = baseLevel,
            playerXp = baseXp,
            unlockedWeapons = startingUnlockedWeapons,
            unlockedArmors = startingUnlockedArmors,
            partyMembers = party,
            partyMemberLevels = party.associateWith { baseLevel },
            partyMemberXp = party.associateWith { baseXp },
            partyMemberHp = party.associateWith { 100 },
            equippedWeapons = startingEquippedWeapons,
            equippedArmors = startingEquippedArmors
        )
        sessionStore.restore(seedState)
        sessionStore.resetTutorialProgress()
        sessionStore.resetQuestProgress()
        inventoryService.restore(emptyMap())
        inventoryService.addItem("medkit", 2)
        sessionStore.startQuest("w1_mq01", track = true)
        sessionStore.setQuestStage("w1_mq01", "wake_in_the_pit")

        saveManager.saveGame(0, sessionStore.state.value, "Nova's Bunk")
        return true
    }

    fun startDebugScenario(scenarioId: String): Boolean {
        val scenario = com.example.starborn.feature.mainmenu.DebugScenarioCatalog.scenarios.firstOrNull { it.id == scenarioId } ?: return false
        val startingRoom = when (scenarioId) {
            "tut_npc_dialogue" -> "pit_jed_bunk"
            "tut_gear_inventory" -> "pit_L2_corridor"
            "tut_tinkering", "tinkering_tutorial" -> "workshop_floor"
            "tut_save_system" -> "workshop_yard"
            "tut_journal_quests" -> "market_plaza"
            "tut_combat_loader", "first_combat" -> "deep_mine_entry"
            "tut_combat_guard", "deep_mine" -> "deep_mine_sublevel1"
            "tut_combat_snacks" -> "mine_descent_shaft"
            "tut_party_combat", "w2_crash_start" -> "sector9_crash_site"
            "tut_world2_debuffs" -> "sector9_canopy_path"
            "tut_source_blast_wave" -> "deep_mine_sublevel2"
            "red_alert" -> "sector4_concourse"
            "launch" -> "launch_bay_gantry"
            "w2_temple_gate" -> "architect_ruins_gate"
            "w2_stasis_chamber" -> "stasis_chamber"
            "w2_hunter_canopy" -> "canopy_ridge"
            "w2_source_gate" -> "source_gate_exterior"
            "w2_astra_repair" -> "astra_hangar"
            "w3_sewers_entry" -> "spire_sewers_entry"
            "w3_safehouse_plan" -> "zeke_safehouse"
            "w3_checkpoint_infiltration" -> "upper_city_checkpoint"
            "w3_lens_archive" -> "scholar_archive_entrance"
            "w3_lockdown_escape" -> "spire_rooftop_evac"
            "w4_foundry_start" -> "slag_pits_entry"
            "w5_docking_procedure" -> "orbital_dock"
            "w6_fractured_minds" -> "event_horizon_camp"
            "w6_finale" -> "singularity_core"
            else -> "pit_nova_bunk"
        }
        val worldId = when {
            scenarioId.startsWith("w2") || scenarioId in listOf("tut_party_combat", "tut_world2_debuffs") -> "world_2"
            scenarioId.startsWith("w3") -> "world_3"
            scenarioId.startsWith("w4") -> "world_4"
            scenarioId.startsWith("w5") -> "world_5"
            scenarioId.startsWith("w6") -> "world_6"
            else -> "world_1"
        }
        val hubId = when (worldId) {
            "world_2" -> "hub_3_sector9"
            "world_3" -> "hub_5_spire_lower"
            "world_4" -> "hub_7_foundry_slag"
            "world_5" -> "hub_9_orbital_ring"
            "world_6" -> "hub_11_event_horizon"
            else -> if (scenarioId in listOf("tut_combat_loader", "tut_combat_guard", "tut_combat_snacks", "first_combat", "deep_mine", "red_alert", "launch")) "hub_2_logistics" else "hub_1_homestead"
        }
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

        val initialState = GameSessionState(
            worldId = worldId,
            hubId = hubId,
            roomId = startingRoom,
            playerId = party.firstOrNull() ?: "nova",
            playerLevel = level,
            partyMembers = party,
            partyMemberLevels = party.associateWith { level },
            partyMemberXp = party.associateWith { 0 },
            partyMemberHp = party.associateWith { 100 },
            playerCredits = 500,
            unlockedWeapons = setOf("mining_pistol", "nova_laser_blaster", "zeke_shock_fists", "orion_prism_focus", "gh0st_whisperblade"),
            unlockedArmors = setOf("nova_flux_liner", "zeke_surge_harness", "orion_channeler_mantle", "gh0st_phaseweave_jacket"),
            equippedWeapons = party.mapNotNull { id -> defaultWeaponsByCharacter[id]?.let { id to it } }.toMap(),
            equippedArmors = party.mapNotNull { id -> defaultArmorsByCharacter[id]?.let { id to it } }.toMap()
        )
        sessionStore.restore(initialState)
        inventoryService.restore(emptyMap())
        inventoryService.addItem("medkit", 5)
        return true
    }

    fun loadSlot(slotIndex: Int): Boolean {
        val loaded = saveManager.loadGame(slotIndex) ?: return false
        sessionStore.restore(loaded)
        inventoryService.restore(loaded.inventory)
        return true
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
