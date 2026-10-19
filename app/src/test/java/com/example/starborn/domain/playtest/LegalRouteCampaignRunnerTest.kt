package com.example.starborn.domain.playtest

import com.example.starborn.core.DispatcherProvider
import org.junit.Assert.assertNull
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CinematicAssetDataSource
import com.example.starborn.data.assets.CraftingAssetDataSource
import com.example.starborn.data.assets.DialogueAssetDataSource
import com.example.starborn.data.assets.EventAssetDataSource
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.assets.MilestoneAssetDataSource
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.data.assets.ThemeAssetDataSource
import com.example.starborn.data.assets.ThemeStyleAssetDataSource
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicService
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.SeededCombatRandom
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.leveling.ExplorationXpAwarder
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSaveRepository
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.telemetry.NoOpPlaytestTelemetry
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.feature.combat.viewmodel.CombatController as CombatViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationController as ExplorationViewModel
import com.example.starborn.feature.hub.viewmodel.HubViewModel
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.ui.events.UiEventBus
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.util.ArrayDeque
import java.util.Locale
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
/**
 * Shared-runtime campaign coverage through public movement, visible actions, real combat,
 * and save/reload checkpoints. Missing routes or actions fail without fallback.
 * Uses virtual time and authored answers; physical controls and human discovery need playtesting.
 */
class LegalRouteCampaignRunnerTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: CoroutineScope
    private lateinit var saveDir: File
    private val systemsAudit by lazy { CampaignSystemsRouteAudit(AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)) }
    private val assistance = mutableListOf<String>()
    private val publicCombats = mutableListOf<String>()
    private val prototypeSkillsUsed = mutableSetOf<String>()
    private var recordSystemsAudit = false
    private var useProductionCrewRules = false

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        saveDir = File(System.getProperty("java.io.tmpdir"), "starborn-legal-runner-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testScope.cancel()
        Dispatchers.resetMain()
        saveDir.deleteRecursively()
    }

    @Test
    fun crewBuildsAcrossSeedsProduceComparableCombatEvidence() {
        val reader = AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        val middle = requireNotNull(WorldAssetDataSource(reader).loadCrewCheckpoint("middle"))
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = middle)
        try {
            val runner = LegalCampaignAgent(h)
            runner.prepareWorld5Supplies()
            val prepared = h.saveSlot(8)
            assertEquals(middle.playerAp, prepared.playerAp)
            assertTrue(prepared.purchasedSkillNodes.isEmpty())
            assertEquals(6, prepared.inventory["medkit_plus"])
            assertEquals(6, prepared.activeMealBuff?.speedBonus)
            val preparationLog = publicCombats.toList()
            h.sessionStore.restore(requireNotNull(h.worldDataSource.loadCrewCheckpoint("early")))
            h.inventoryService.restore(h.sessionStore.state.value.inventory)
            h.settle()
            runner.prepareWorld4Rest()
            val early = h.saveSlot(8)
            h.sessionStore.restore(requireNotNull(h.worldDataSource.loadCrewCheckpoint("late")))
            h.inventoryService.restore(h.sessionStore.state.value.inventory)
            h.settle()
            h.explorationVm.useInventoryItem("orbital_glimmer_tonic")
            h.settle()
            h.restAtVisibleStop()
            val late = h.saveSlot(8)
            CrewBalanceExperiment(reader, testDispatcher).run(mapOf("early" to early, "middle" to prepared, "late" to late), preparationLog)
        } finally { h.close() }
    }

    @Test
    fun crewPrototypeProducesEarnedCheckpoints() {
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope, prototypeBranch = null)
        try {
            val runner = LegalCampaignAgent(h)
            runner.playWorld1()
            runner.playWorld2()
            runner.playWorld3()
            val checkpoints = linkedMapOf<String, GameSessionState>()
            fun capture(id: String, expectedAp: Int) {
                h.settle()
                val earned = h.saveSlot(8)
                var reset = earned.copy(progressionPrototypeVersion = 1,
                    playerAp = earned.playerAp + h.recordedApPurchases.values.sum(), purchasedSkillNodes = emptyMap(),
                    unlockedSkills = earned.unlockedSkills - h.recordedApPurchases.keys, progressionAwardClaims = emptySet())
                reset.completedQuests.forEach { reset = com.example.starborn.domain.leveling.NovaBuildPrototype.awardCompletedQuest(reset, it) }
                listOf("orion", "gh0st").filter { it in reset.partyMembers }.forEach { reset = com.example.starborn.domain.leveling.NovaBuildPrototype.awardRecruitment(reset, it) }
                reset = com.example.starborn.domain.leveling.CrewBuildPrototype.grantCore(reset)
                assertEquals("Earned AP at $id", expectedAp, reset.playerAp)
                assertTrue(reset.partyMembers.containsAll(listOf("nova", "zeke", "orion", "gh0st")))
                assertTrue(reset.pendingBattleJson.isBlank())
                assertNull(reset.battleCheckpoint)
                checkpoints[id] = reset
            }
            capture("early", 26)
            runner.playWorld4()
            capture("middle", 36)
            runner.playWorld5()
            capture("late", 44)
            assertTrue("Checkpoint routes must use real combat without assisted actions", assistance.isEmpty())
            val adapter = moshi.adapter(GameSessionState::class.java).indent("  ")
            val directory = File(assets, "playtest").apply { mkdirs() }
            checkpoints.forEach { (id, state) ->
                val json = adapter.toJson(state)
                assertEquals(state, adapter.fromJson(json))
                if (System.getenv("STARBORN_REGENERATE_CREW_CHECKPOINTS") == "1") {
                    File(directory, "crew_$id.json").writeText(json + "\n")
                } else {
                    val packaged = requireNotNull(h.worldDataSource.loadCrewCheckpoint(id))
                    assertEquals(state.playerAp, packaged.playerAp)
                    assertEquals(state.partyMemberLevels, packaged.partyMemberLevels)
                    assertEquals(state.partyMemberXp, packaged.partyMemberXp)
                    assertEquals(state.completedQuests, packaged.completedQuests)
                    assertEquals(state.completedMilestones, packaged.completedMilestones)
                    assertEquals(state.inventory, packaged.inventory)
                    assertEquals(state.equippedWeapons, packaged.equippedWeapons)
                    assertEquals(state.equippedArmors, packaged.equippedArmors)
                }
            }
            File("build/reports/progression-prototype/crew-checkpoints.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("Derived from the baseline public campaign route through World 5; seed 17; zero assisted actions.\n" +
                    "Opening fixture: 1 cryo_inductor + 2 scrap_metal for the mandatory recipe; no AP or XP grants.\n" +
                    "Snapshots refund only the two logged test-route AP purchases and backfill proposed rewards from completed quests/recruitments. These are experimental checkpoints, not prototype-route balance passes.\n" +
                    checkpoints.entries.joinToString("\n") { (id, s) -> "$id: room=${s.roomId} AP=${s.playerAp} levels=${s.partyMemberLevels} weapons=${s.equippedWeapons} armor=${s.equippedArmors} inventory=${s.inventory}" } +
                    "\n" + publicCombats.joinToString("\n"))
            }
        } finally { h.close() }
    }

    @Test
    fun `Nova saboteur earned world one and two route buys and uses real talents`() = playNovaPrototypeRoute("Saboteur")

    @Test
    fun `Nova improviser earned world one and two route buys and uses real talents`() = playNovaPrototypeRoute("Improviser")

    private fun playNovaPrototypeRoute(branch: String) {
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope, prototypeBranch = branch)
        try {
            val agent = LegalCampaignAgent(h)
            agent.playWorld1()
            val w1 = h.sessionStore.state.value
            assertTrue("Existing route includes Heavy Lifting optional training", "w1_sq03" in w1.completedQuests)
            assertEquals(8, w1.playerAp + w1.purchasedSkillNodes.values.sum())
            assertEquals(3, w1.purchasedSkillNodes.size)
            val w1Save = h.saveSlot(8)
            assertEquals(w1.purchasedSkillNodes, w1Save.purchasedSkillNodes)
            agent.playWorld2()
            val after = h.sessionStore.state.value
            assertTrue("Prototype routes require no assisted actions", assistance.isEmpty())
            assertEquals(16, after.playerAp + after.purchasedSkillNodes.values.sum())
            assertEquals(6, after.purchasedSkillNodes.values.sum())
            val expected = if (branch == "Saboteur") "nova_breaker_bolt" else "nova_scatter_charge"
            assertTrue("Earned move must be used through public combat controls: $expected", expected in prototypeSkillsUsed)
            val saved = h.saveSlot(8)
            val restored = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = saved)
            try {
                restored.loadSlot(8)
                restored.explorationVm.openSkillTree("nova")
                restored.settle()
                assertEquals(6, restored.explorationVm.uiState.value.skillTreeOverlay?.apInvested)
                assertEquals(saved.purchasedSkillNodes, restored.sessionStore.state.value.purchasedSkillNodes)
            } finally { restored.close() }
            File("build/reports/progression-prototype/${branch.lowercase()}.txt").apply {
                parentFile.mkdirs()
                writeText("Earned World 1/2 prototype route; seed 17; no assisted actions.\n" +
                    "AP earned=16 (14 main route + 2 optional training), paid=6, available=${after.playerAp}; purchases=${after.purchasedSkillNodes}\n" +
                    "Publicly used moves=$prototypeSkillsUsed\n" + publicCombats.joinToString("\n"))
            }
        } finally { h.close() }
    }

    @Test
    fun `public gear controls reject unowned gear and incompatible character weapons`() {
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            h.explorationVm.equipWeapon("nova", "mining_pistol")
            h.explorationVm.equipArmor("nova", "nova_flux_liner")
            h.explorationVm.equipWeapon("nova", "zeke_shock_fists")
            h.settle()
            assertEquals(null, h.sessionStore.state.value.equippedWeapons["nova"])
            assertEquals(null, h.sessionStore.state.value.equippedArmors["nova"])
            assertFalse("Rejected gear must not be unlocked", "mining_pistol" in h.sessionStore.state.value.unlockedWeapons)
        } finally { h.close() }
    }

    @Test
    fun `opening route earns combat victory and survives reload without assisted actions`() {
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            LegalCampaignAgent(h).playOpening()
            assertTrue("Opening must contain no fallback actions", assistance.isEmpty())
            assertTrue(h.sessionStore.state.value.completedQuests.contains("w1_mq01"))
            assertTrue(h.inventoryService.hasItem("functional_cryo_inductor"))
            val saved = h.saveSlot(7)
            val restored = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = saved)
            try {
                assertEquals(saved, restored.loadSlot(7))
                assertEquals(saved.inventory, restored.inventoryService.snapshot())
            } finally { restored.close() }
            File("build/reports/campaign-systems/public-opening-route.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("PASS: public movement, Jed dialogue grant, equipment, authored loader encounter, production ATB and AI, crafting, quest completion, save/reload.\nAssisted actions: 0\n" + publicCombats.joinToString("\n") + "\n")
            }
        } finally { h.close() }
    }

    @Test
    fun productionOpeningEarnsItsFirstApAndPreservesItsSingleSuit() = verifyProductionOpening(inspectLoader = true)

    @Test
    fun productionOpeningCanFinishWhenThePlayerEngagesTheLoaderBeforeReadingItsStrip() = verifyProductionOpening(inspectLoader = false)

    private fun verifyProductionOpening(inspectLoader: Boolean) {
        useProductionCrewRules = true
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            assertTrue(h.sessionStore.state.value.inventory.isEmpty())
            assertEquals(0, h.sessionStore.state.value.playerAp)
            LegalCampaignAgent(h).playOpening(inspectLoader)
            val state = h.sessionStore.state.value
            assertTrue(assistance.isEmpty())
            assertEquals(2, state.playerAp)
            assertEquals(1, state.inventory["nova_flux_liner"])
            assertEquals("nova_flux_liner", state.equippedArmors["nova"])
            val quest = requireNotNull(h.questRepository.questById("w1_mq01"))
            assertEquals(quest.stages.flatMap { it.tasks }.map { it.id }.toSet(), state.questTasksCompleted["w1_mq01"])
            assertEquals(inspectLoader, "ms_w1_mq01_loader_inspected" in state.completedMilestones)
            val saved = h.saveSlot(7)
            h.sessionStore.addAp(99)
            assertEquals(saved, h.loadSlot(7))
            assertEquals(2, h.sessionStore.state.value.playerAp)
            val branch = if (inspectLoader) "read-strip" else "fight-first"
            File("build/reports/world1-opening/$branch.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("Production crew; no seeded inventory/AP; real loader victory; first quest and every task complete; AP=2; one Flux Liner; save/reload; assisted actions=0.\n" + publicCombats.joinToString("\n"))
            }
        } finally { h.close() }
    }

    @Test
    fun productionWorld1ReachesTheWorld2HandoffWithEarnedResourcesAndSaveParity() {
        useProductionCrewRules = true
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            assertTrue(h.sessionStore.state.value.inventory.isEmpty())
            LegalCampaignAgent(h).playWorld1()
            val state = h.sessionStore.state.value
            assertTrue(assistance.isEmpty())
            assertTrue((1..5).all { "w1_mq0$it" in state.completedQuests })
            assertTrue("w1_sq03" in state.completedQuests)
            assertEquals(8, state.playerAp) // Six main-route AP plus the required certification's two.
            assertEquals("sector9_crash_site", state.roomId)
            assertTrue("w2_mq01" in state.activeQuests)
            val saved = h.saveSlot(7)
            h.sessionStore.addAp(99)
            assertEquals(saved, h.loadSlot(7))
            assertEquals(8, h.sessionStore.state.value.playerAp)
            File("build/reports/world1-opening/world1-handoff.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("Production crew; no seeded inventory/AP; five main quests plus required certification; actual combat; World 2 handoff; AP=8; save parity; assisted actions=0.\n" + publicCombats.joinToString("\n"))
            }
        } finally { h.close() }
    }

    @Test
    fun productionWorld1CanRecoverIfTheWardenIsDefeatedBeforeReturningToJed() {
        useProductionCrewRules = true
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            LegalCampaignAgent(h).playWorld1(wardenBeforeJed = true)
            assertEquals("sector9_crash_site", h.sessionStore.state.value.roomId)
            assertTrue("w1_mq05" in h.sessionStore.state.value.completedQuests)
            assertTrue(assistance.isEmpty())
            assertEquals(1, publicCombats.count { it.contains("enemies=[the_iron_warden]") })
        } finally { h.close() }
    }

    @Test
    fun productionWorld1CompletionistRouteFinishesAllTenQuestsAndTheCartDetour() {
        useProductionCrewRules = true
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            LegalCampaignAgent(h).playWorld1(completionist = true)
            val state = h.sessionStore.state.value
            assertTrue((1..5).all { "w1_mq0$it" in state.completedQuests && "w1_sq0$it" in state.completedQuests })
            assertEquals("sector9_crash_site", state.roomId)
            assertEquals(8, state.playerAp)
            assertTrue(state.roomStates["mine_junction"]?.get("bulkhead_breached") == true)
            assertTrue(assistance.isEmpty())
            val saved = h.saveSlot(7)
            assertEquals(saved, h.loadSlot(7))
            File("build/reports/world1-opening/completionist-handoff.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("Production crew; no seeded inventory/AP; all five main and five side quests; cart vault route; actual combat; World 2 handoff; AP=8; save parity; assisted actions=0. Authored puzzle answers are supplied.\n" + publicCombats.joinToString("\n"))
            }
        } finally { h.close() }
    }

    @Test
    fun productionCrewCampaignEarnsItsOpeningSuppliesAndReachesCredits() {
        useProductionCrewRules = true
        `fresh game legal route traverses Worlds 1 through 6 to ms_game_complete with save reload checkpoints`()
        File("build/reports/crew-production/campaign.txt").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("Crew rules; no seeded Cryo-Inductor or Scrap Metal; all 30 main quests; credits; world-boundary save/reload; zero assisted combat actions.\n" + publicCombats.joinToString("\n"))
        }
    }

    @Test
    fun `fresh game legal route traverses Worlds 1 through 6 to ms_game_complete with save reload checkpoints`() {
        recordSystemsAudit = true
        var harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            val runner = LegalCampaignAgent(harness)

            // --- WORLD 1: THE HOMESTEAD & MINING COLONY ---
            runner.playWorld1()

            // Checkpoint 1 Save / Reload
            val w1Saved = harness.saveSlot(1)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = w1Saved)
            runner.attach(harness)
            val w1Restored = harness.loadSlot(1)
            assertEquals("World 1 save/reload state parity", w1Saved, w1Restored)
            assertEquals("sector9_crash_site", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w2_mq01"))
            assertTrue(harness.sessionStore.state.value.completedQuests.contains("w1_mq05"))

            // --- WORLD 2: SECTOR 9 UNCHARTED WILDS ---
            runner.playWorld2()
            assertTrue("Worlds 1-2 must have no assisted actions or victories", assistance.isEmpty())
            File("build/reports/campaign-systems/public-worlds12-combat.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText(publicCombats.joinToString("\n") + "\nRemaining synthetic victories:\n" + assistance.joinToString("\n") + "\n")
            }

            // Checkpoint 2 Save / Reload
            val w2Saved = harness.saveSlot(2)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = w2Saved)
            runner.attach(harness)
            val w2Restored = harness.loadSlot(2)
            assertEquals("World 2 save/reload state parity", w2Saved, w2Restored)
            assertEquals("spire_sewers_landing", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.completedQuests.contains("w2_mq05"))
            assertTrue(harness.sessionStore.state.value.completedMilestones.contains("ms_w2_mq05_complete"))

            // --- WORLD 3: THE SPIRE ---
            runner.playWorld3()
            assertTrue("Worlds 1-3 must have no assisted actions or victories", assistance.isEmpty())
            File("build/reports/campaign-systems/public-worlds123-combat.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText(publicCombats.joinToString("\n") + "\nAssisted actions: 0\n")
            }

            // Checkpoint 3 Save / Reload
            val w3Saved = harness.saveSlot(3)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = w3Saved)
            runner.attach(harness)
            val w3Restored = harness.loadSlot(3)
            assertEquals("World 3 save/reload state parity", w3Saved, w3Restored)
            assertEquals("foundry_slag_landing", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.completedQuests.contains("w3_mq15"))
            assertTrue(harness.sessionStore.state.value.completedMilestones.contains("ms_w3_mq15_complete"))

            // --- WORLD 4: THE FOUNDRY ---
            runner.playWorld4()
            assertTrue("Worlds 1-4 must have no assisted actions or victories", assistance.isEmpty())
            File("build/reports/campaign-systems/public-worlds1234-combat.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText(publicCombats.joinToString("\n") + "\nAssisted actions: 0\n")
            }

            // Checkpoint 4 Save / Reload
            val w4Saved = harness.saveSlot(1)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = w4Saved)
            runner.attach(harness)
            val w4Restored = harness.loadSlot(1)
            assertEquals("World 4 save/reload state parity", w4Saved, w4Restored)
            assertEquals("orbital_executive_dock", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.completedQuests.contains("w4_mq20"))
            assertTrue(harness.sessionStore.state.value.completedMilestones.contains("ms_w5_access_unlocked"))

            // --- WORLD 5: THE ORBITAL RING ---
            runner.playWorld5()
            assertTrue("Worlds 1-5 must have no assisted actions or victories", assistance.isEmpty())
            File("build/reports/campaign-systems/public-worlds12345-combat.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText(publicCombats.joinToString("\n") + "\nAssisted actions: 0\n")
            }

            // Checkpoint 5 Save / Reload
            val w5Saved = harness.saveSlot(2)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = w5Saved)
            runner.attach(harness)
            val w5Restored = harness.loadSlot(2)
            assertEquals("World 5 save/reload state parity", w5Saved, w5Restored)
            assertEquals("source_campfire", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.completedQuests.contains("w5_mq25"))
            assertTrue(harness.sessionStore.state.value.completedMilestones.contains("ms_w6_access_unlocked"))

            // --- WORLD 6: THE SOURCE & ASCENDED FINALE ---
            runner.playWorld6()
            assertTrue("The entire campaign must use no assisted actions or victories", assistance.isEmpty())
            File("build/reports/campaign-systems/public-full-campaign-combat.txt").apply {
                requireNotNull(parentFile).mkdirs()
                writeText(publicCombats.joinToString("\n") + "\nAssisted actions: 0\n")
            }

            // Final Verification
            val finalState = harness.sessionStore.state.value
            assertEquals("source_new_world", finalState.roomId)
            assertTrue("Milestone ms_game_complete must be set", finalState.completedMilestones.contains("ms_game_complete"))
            assertTrue("Milestone ms_credits_seen must be set", finalState.completedMilestones.contains("ms_credits_seen"))
            assertTrue("Final skill source_art_tune_world must be unlocked", finalState.unlockedSkills.contains("source_art_tune_world"))
            assertTrue("w6_mq30 must be completed", finalState.completedQuests.contains("w6_mq30"))

            val allMainQuests = (1..6).flatMap { w ->
                when (w) {
                    1 -> (1..5).map { "w1_mq%02d".format(it) }
                    2 -> (1..5).map { "w2_mq%02d".format(it) }
                    3 -> (11..15).map { "w3_mq%02d".format(it) }
                    4 -> (16..20).map { "w4_mq%02d".format(it) }
                    5 -> (21..25).map { "w5_mq%02d".format(it) }
                    6 -> (26..30).map { "w6_mq%02d".format(it) }
                    else -> emptyList()
                }
            }.toSet()
            assertTrue("All 30 main quests must be completed. Missing: ${allMainQuests - finalState.completedQuests}",
                finalState.completedQuests.containsAll(allMainQuests))

            val completedSave = harness.saveSlot(3)
            harness.close()
            harness = ProductionCampaignHarness(saveDir, testDispatcher, testScope, initialState = completedSave)
            assertEquals("Completed ending must survive save/reload", completedSave, harness.loadSlot(3))
            assertTrue("Credits flag must persist", "ms_credits_seen" in harness.sessionStore.state.value.completedMilestones)

            systemsAudit.writeReports(assistance)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `unlocked public route buys a base catches components crafts and sells after Astra return`() {
        val h = ProductionCampaignHarness(saveDir, testDispatcher, testScope)
        try {
            val setup = LegalCampaignAgent(h)
            setup.playWorld1()
            setup.playWorld2()
            val assistedSetup = assistance.size
            val started = testDispatcher.scheduler.currentTime
            val log = mutableListOf("Setup completed through public World 2 route; procurement slice starts at Spire landing.")
            fun note(text: String) { log += "${testDispatcher.scheduler.currentTime - started} ms | ${h.sessionStore.state.value.roomId} | $text" }
            fun walk(target: String) {
                val start = requireNotNull(h.sessionStore.state.value.roomId)
                val path = requireNotNull(h.findBfsPath(start, target)) { "Public slice needs a connected route: $start -> $target" }
                for ((direction, nextRoom) in path) {
                    val named = h.explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.TravelAction>()
                        .firstOrNull { it.direction == direction }
                    if (named != null) h.explorationVm.onActionSelected(named) else h.explorationVm.travel(direction)
                    h.settle()
                    assertEquals("Public travel must succeed without a room-position fallback", nextRoom, h.sessionStore.state.value.roomId)
                }
                note("Walked to $target")
            }
            fun sail(world: String) {
                val room = requireNotNull(h.sessionStore.state.value.roomId)
                val node = h.hubNodes.first { room in it.rooms }
                walk(node.entryRoom)
                h.explorationVm.requestReturnToHub(); h.settle()
                h.hubVm.enterNode("astra_access") {}; h.settle()
                assertEquals("astra_cargo_bay", h.sessionStore.state.value.roomId)
                walk("astra_bridge")
                val console = h.explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.GenericAction>()
                    .first { it.actionEvent == "astra_nav_console" }
                h.explorationVm.onActionSelected(console); h.settle()
                val destination = com.example.starborn.domain.session.AstraTravel.availableDestinations(h.sessionStore.state.value).single { it.worldId == world }
                h.explorationVm.travelToWorldFromAstra(destination.worldId, destination.hubId, destination.roomId, destination.nodeId)
                h.settle(); testDispatcher.scheduler.advanceTimeBy(1401); h.settle()
                h.explorationVm.disembarkAstra(); h.settle()
                h.hubVm.enterNode(destination.nodeId) {}; h.settle()
                assertEquals(destination.roomId, h.sessionStore.state.value.roomId)
                note("Astra arrived in $world")
            }
            fun merchant(id: String): com.example.starborn.feature.shop.ShopController {
                val action = h.explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ShopAction>().single { it.shopId == id }
                h.explorationVm.onActionSelected(action); h.settle()
                h.explorationVm.enterPendingShop(); h.settle()
                return com.example.starborn.feature.shop.ShopController(id, h.shopRepository, h.itemRepository, h.inventoryService, h.sessionStore, testScope)
            }
            sail("world_2")
            walk("sector9_stream_falls")
            merchant("davids_provisions").use { shop ->
                val before = h.sessionStore.state.value.playerCredits
                val price = shop.uiState.value.itemsForSale.single { it.id == "fiberglass_rod" }.price
                shop.buyItem("fiberglass_rod", 1); h.settle()
                assertTrue(h.inventoryService.hasItem("fiberglass_rod"))
                assertEquals(before - price, h.sessionStore.state.value.playerCredits)
                note("Bought Resin Rod for $price credits")
            }
            walk("sector9_crash_site")
            sail("world_3")
            walk("spire_sewers_passage")
            val fishAction = h.explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.GenericAction>().single { it.zoneId == "spire_runoff" }
            h.explorationVm.onActionSelected(fishAction); h.settle()
            val fishing = com.example.starborn.feature.fishing.viewmodel.FishingController(h.fishingService, "spire_runoff", kotlin.random.Random(11), testScope)
            var casts = 0
            try {
                h.settle()
                fishing.selectRod(h.fishingService.getAvailableRods().single { it.id == "wooden_rod" })
                fishing.selectLure(h.fishingService.getAvailableLures().single { it.id == "basic_lure" })
                h.settle()
                while ((!h.inventoryService.hasItem("wiring_bundle") || !h.inventoryService.hasItem("circuit_board")) && casts++ < 300) {
                    fishing.startFishing(); h.settle()
                    var ticks = 0
                    while (fishing.uiState.value.fishingState == com.example.starborn.feature.fishing.viewmodel.FishingState.WAITING && ticks++ < 100) {
                        testDispatcher.scheduler.advanceTimeBy(50); h.settle()
                    }
                    fishing.onHookButtonPressed(); fishing.beginReeling(); h.settle()
                    ticks = 0
                    while (fishing.uiState.value.fishingState == com.example.starborn.feature.fishing.viewmodel.FishingState.REELING && ticks++ < 500) {
                        val reel = requireNotNull(fishing.uiState.value.reelState)
                        if (reel.phase !in setOf(com.example.starborn.domain.fishing.FishingFightPhase.WARNING, com.example.starborn.domain.fishing.FishingFightPhase.SURGE) && reel.tension < .65f)
                            fishing.onReelPressed() else fishing.onReelReleased()
                        testDispatcher.scheduler.advanceTimeBy(com.example.starborn.domain.fishing.FishingFight.TICK_MS); h.settle()
                    }
                    assertEquals(com.example.starborn.feature.fishing.viewmodel.FishingState.RESULT, fishing.uiState.value.fishingState)
                    assertTrue(requireNotNull(fishing.uiState.value.lastCatchResult).secured)
                    fishing.resetFishing(); h.settle()
                }
            } finally { fishing.close() }
            assertTrue("Public fishing must obtain both components", casts < 300)
            note("Caught upgrade components in $casts casts")
            h.explorationVm.openMenuOverlay(com.example.starborn.feature.exploration.viewmodel.MenuTab.FIELD_KIT); h.settle()
            val bench = com.example.starborn.feature.crafting.CraftingController(h.craftingService, h.inventoryService, h.sessionStore, testScope)
            try {
                bench.autoFill("gear_reinforced_resin_rod"); h.settle()
                assertTrue(bench.uiState.value.bench.canCraftSelection)
                bench.craftFromBench(); h.settle()
                assertTrue(h.inventoryService.hasItem("reinforced_resin_rod"))
                assertFalse(h.inventoryService.hasItem("fiberglass_rod"))
                h.explorationVm.onTinkeringCrafted("reinforced_resin_rod")
            } finally { bench.close() }
            h.explorationVm.closeMenuOverlay(); h.settle()
            note("Crafted Reinforced Resin Rod through Field Kit")
            walk("spire_sewers_landing")
            sail("world_2")
            walk("sector9_stream_falls")
            merchant("davids_provisions").use { shop ->
                var banked = 0
                for (sale in shop.uiState.value.sellInventory.filter { it.canSell && it.id in setOf("raw_glowfish", "chime_minnow", "stellarium_eel") }) {
                    val before = h.sessionStore.state.value.playerCredits
                    val owned = h.inventoryService.snapshot().getValue(sale.id)
                    val quantity = (sale.quantity - if (sale.id == "raw_glowfish") 2 else 0).coerceAtLeast(0)
                    if (quantity == 0) continue
                    shop.sellItem(sale.id, quantity); h.settle()
                    assertEquals(before + sale.price * quantity, h.sessionStore.state.value.playerCredits)
                    assertEquals(owned - quantity, h.inventoryService.snapshot()[sale.id] ?: 0)
                    banked += sale.price * quantity
                }
                assertTrue(banked > 0)
                note("Banked $banked credits by selling caught native fish after return trip")
            }
            walk("sector9_temple_lock_chamber")
            merchant("sentinel_scraps").use { shop ->
                shop.buyItem("fiery_pepper", 2); shop.buyItem("herb", 1); h.settle()
                assertTrue(h.inventoryService.hasItem("fiery_pepper", 2))
                assertTrue(h.inventoryService.hasItem("herb"))
                note("Bought cooking ingredients from Sentinel Scraps")
            }
            walk("sector9_ridge_plateau")
            val stove = h.explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.RestStopAction>().first { it.cookSource != null }
            h.explorationVm.onActionSelected(stove); h.settle()
            assertEquals(true, h.sessionStore.state.value.roomStates["sector9_ridge_plateau"]?.get("cooking_discovered"))
            assertTrue(h.craftingService.cookMeal("snack_pulse_citrus", "nova") is CraftingOutcome.Success)
            assertTrue(h.inventoryService.hasItem("pulse_citrus"))
            assertFalse(h.inventoryService.hasItem("fiery_pepper"))
            note("Discovered the ridge cookfire and cooked Pulse Citrus")
            val saved = h.saveSlot(3)
            assertEquals(saved, h.loadSlot(3))
            assertTrue(h.inventoryService.hasItem("reinforced_resin_rod"))
            assertTrue(h.inventoryService.hasItem("pulse_citrus"))
            note("Save/reload preserved purchases, recipes, rewards and crafted items")
            assertEquals("Public procurement slice must add no assisted actions", assistedSetup, assistance.size)
            assertTrue(h.inventoryService.hasItem("wooden_rod"))
            assertTrue(h.inventoryService.hasItem("basic_lure"))
            File("build/reports/campaign-systems/public-procurement-route.txt").apply { requireNotNull(parentFile).mkdirs(); writeText(log.joinToString("\n") + "\n") }
        } finally { h.close() }
    }

    private inner class LegalCampaignAgent(private var harness: ProductionCampaignHarness) {

        fun attach(newHarness: ProductionCampaignHarness) {
            this.harness = newHarness
        }

        fun travelDirection(direction: String) {
            val beforeRoom = harness.explorationVm.uiState.value.currentRoom?.id
            harness.explorationVm.travel(direction)
            harness.settle()
            if (harness.sessionStore.state.value.roomStates["admin_elevator"]
                    ?.get("mine_map_reveal_pending") == true &&
                "deep_mine" !in harness.sessionStore.state.value.visitedNodes) {
                harness.hubVm.finishNodeReveal("deep_mine")
                harness.hubVm.enterNode("deep_mine") { }
                harness.settle()
            }
            val afterRoom = harness.explorationVm.uiState.value.currentRoom?.id
            check(afterRoom != null && afterRoom != beforeRoom) {
                "travel($direction) failed from room '$beforeRoom': blocked or no connection. status=${harness.explorationVm.uiState.value.statusMessage}; prompt=${harness.explorationVm.uiState.value.blockedPrompt}; session=${harness.sessionStore.state.value.roomId}; hub=${harness.sessionStore.state.value.hubId}; pending=${harness.sessionStore.state.value.roomStates["admin_elevator"]}; visited=${harness.sessionStore.state.value.visitedNodes}"
            }
        }

        fun navigateLegalPath(targetRoomId: String) {
            var currentRoomId = harness.explorationVm.uiState.value.currentRoom?.id
                ?: harness.sessionStore.state.value.roomId
                ?: error("No current room")
            if (currentRoomId == targetRoomId) return

            val path = harness.findBfsPath(currentRoomId, targetRoomId)
            if (path != null) {
                for ((dir, expectedRoom) in path) {
                    travelDirection(dir)
                    val arrived = harness.sessionStore.state.value.roomId
                    if (arrived == targetRoomId) return
                    if (arrived != expectedRoom) {
                        navigateLegalPath(targetRoomId)
                        return
                    }
                }
                assertEquals("Public navigation must reach its target", targetRoomId, harness.sessionStore.state.value.roomId)
                return
            }

            // If no intra-connection path, check if we need to return to Hub and enter target node
            val targetNode = harness.hubNodes.firstOrNull { targetRoomId in it.rooms }
            if (targetNode != null) {
                val currentNode = harness.hubNodes.firstOrNull { currentRoomId in it.rooms }
                if (currentNode != null && !harness.explorationVm.uiState.value.canReturnToHub) {
                    val pathToReturn = harness.findBfsPath(currentRoomId, currentNode.entryRoom)
                    if (pathToReturn != null) {
                        for ((dir, _) in pathToReturn) {
                            travelDirection(dir)
                        }
                    }
                }
                if (harness.explorationVm.uiState.value.canReturnToHub) {
                    harness.explorationVm.requestReturnToHub()
                    harness.settle()
                    harness.hubVm.enterNode(targetNode.id) { }
                    harness.settle()
                    currentRoomId = harness.explorationVm.uiState.value.currentRoom?.id
                        ?: harness.sessionStore.state.value.roomId
                        ?: error("Failed to enter node ${targetNode.id}")
                    if (currentRoomId == targetRoomId) return
                    val pathInNode = harness.findBfsPath(currentRoomId, targetRoomId)
                    if (pathInNode != null) {
                        for ((dir, _) in pathInNode) {
                            travelDirection(dir)
                        }
                        return
                    }
                }
            }

            error("No public route from $currentRoomId to $targetRoomId")
        }

        fun performAction(actionId: String) {
            val matchingAction = harness.explorationVm.uiState.value.actions.firstOrNull { action ->
                action.actionKey().contains(actionId, ignoreCase = true) ||
                    (action is com.example.starborn.domain.model.GenericAction && action.actionEvent == actionId) ||
                    (action is com.example.starborn.domain.model.ToggleAction &&
                        (action.actionEventOn == actionId || action.actionEventOff == actionId))
            }
            if (matchingAction != null) {
                harness.explorationVm.onActionSelected(matchingAction)
            } else {
                error("No visible action $actionId in ${harness.sessionStore.state.value.roomId}")
            }
            while (harness.cinematicCoordinator.state.value != null) {
                harness.explorationVm.skipCinematic()
                harness.settle()
            }
            harness.settle()
        }

        fun finishDialogue(choiceId: String) {
            var steps = 0
            while (harness.explorationVm.uiState.value.activeDialogue != null) {
                check(++steps <= 30) { "Puzzle dialogue stalled" }
                val choices = harness.explorationVm.uiState.value.dialogueChoices
                if (choices.isEmpty()) harness.explorationVm.advanceDialogue()
                else harness.explorationVm.onDialogueChoiceSelected(choices.single { it.id == choiceId }.id)
                harness.settle()
            }
        }

        fun talkToNpc(npcName: String, choiceId: String? = null) {
            harness.explorationVm.onNpcInteraction(npcName)
            harness.settle()
            var steps = 0
            while (harness.explorationVm.uiState.value.activeDialogue != null) {
                check(++steps <= 100) { "Dialogue stalled with $npcName" }
                val choices = harness.explorationVm.uiState.value.dialogueChoices
                if (choices.isNotEmpty()) {
                    val choiceToPick = if (choiceId != null) {
                        choices.firstOrNull { it.id == choiceId } ?: choices.first()
                    } else {
                        choices.first()
                    }
                    harness.explorationVm.onDialogueChoiceSelected(choiceToPick.id)
                } else {
                    harness.explorationVm.advanceDialogue()
                }
                harness.settle()
            }
        }

        fun craftItem(recipeId: String, resultItemId: String) {
            harness.explorationVm.openMenuOverlay(com.example.starborn.feature.exploration.viewmodel.MenuTab.FIELD_KIT)
            harness.settle()
            val bench = com.example.starborn.feature.crafting.CraftingController(
                harness.craftingService, harness.inventoryService, harness.sessionStore, testScope)
            try {
                bench.autoFill(recipeId)
                harness.settle()
                assertTrue("Bench selection for $recipeId must be craftable", bench.uiState.value.bench.canCraftSelection)
                bench.craftFromBench()
                harness.settle()
                assertTrue("Crafting $recipeId must produce $resultItemId", harness.inventoryService.hasItem(resultItemId))
            } finally { bench.close() }
            harness.explorationVm.onTinkeringCrafted(resultItemId)
            harness.explorationVm.onTinkeringClosed()
            harness.explorationVm.closeMenuOverlay()
            harness.settle()
        }

        fun playWorld1(wardenBeforeJed: Boolean = false, completionist: Boolean = false) {
            playOpening()
            if (completionist) {
                navigateLegalPath("trade_scrapper")
                talkToNpc("Scrapper")
                navigateLegalPath("trade_stash")
                performAction("w1_sq01_open_rebel_cache")
                navigateLegalPath("trade_scrapper")
                talkToNpc("Scrapper")
                assertTrue("w1_sq01" in harness.sessionStore.state.value.completedQuests)
                navigateLegalPath("medbay_exam1")
                talkToNpc("Doc")
                navigateLegalPath("medbay_vents")
                performAction("w1_sq02_patch_vent")
                performAction("w1_sq02_reroute_airflow")
                performAction("w1_sq02_clear_toxic_blockage")
                navigateLegalPath("medbay_exam1")
                talkToNpc("Doc")
                assertTrue("w1_sq02" in harness.sessionStore.state.value.completedQuests)
            }

            // Navigate to Checkpoint Queue
            navigateLegalPath("checkpoint_queue")
            talkToNpc("Guard Hank")

            navigateLegalPath("checkpoint_booth")
            talkToNpc("Zeke", choiceId = "zeke_w1_mq02_choose_grid_instability")
            assertTrue("w1_mq02 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq02"))
            assertTrue("w1_mq03 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq03"))
            assertTrue("Mine access badge acquired", (harness.sessionStore.state.value.inventory["mine_access_badge"] ?: 0) >= 1)

            playWorld1AfterCheckpoint(wardenBeforeJed, completionist)
        }

        fun playOpening(inspectLoader: Boolean = true) {
            assertEquals("pit_nova_bunk", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w1_mq01"))

            // Wake up and isolate safety fault to unlock bunk exit
            performAction("w1_mq01_turn_on_bunk_light")
            performAction("w1_mq01_inspect_safety_fault")

            // Navigate legal path to Jed's bunk
            navigateLegalPath("pit_jed_bunk")
            talkToNpc("Jed")
            assertTrue("Starter weapon comes from Jed dialogue", harness.inventoryService.hasItem("mining_pistol"))
            harness.explorationVm.equipWeapon("nova", "mining_pistol")
            harness.explorationVm.equipArmor("nova", "nova_flux_liner")
            harness.settle()
            assertEquals("mining_pistol", harness.sessionStore.state.value.equippedWeapons["nova"])
            assertEquals("nova_flux_liner", harness.sessionStore.state.value.equippedArmors["nova"])
            assertTrue("Cryo-Inductor must persist after Jed's kit: inventory=${harness.inventoryService.snapshot()}, session=${harness.sessionStore.state.value.inventory}",
                (harness.sessionStore.state.value.inventory["cryo_inductor"] ?: 0) >= 1)

            // Navigate to workshop yard and encounter faulted loader
            navigateLegalPath("workshop_yard")
            assertFalse("Mira must not appear while the faulted loader is active",
                "mechanic_mira" in harness.explorationVm.uiState.value.npcs)
            if (inspectLoader) performAction("w1_mq01_inspect_loader_relay")
            harness.playPublicEncounter("faulted_loader")
            assertTrue("Mira becomes available after the loader is cleared",
                "mechanic_mira" in harness.explorationVm.uiState.value.npcs)

            // Enter workshop floor and craft the functional cryo-inductor via production CraftingService
            navigateLegalPath("workshop_floor")
            craftItem("repair_cryo_inductor", "functional_cryo_inductor")
            performAction("w1_mq01_patch_flux_liner")
            performAction("w1_mq01_confirm_governor")
            performAction("w1_mq01_cutter_surge")
            assertTrue("w1_mq01 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq01"))
            assertTrue("w1_mq02 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq02"))

        }

        private fun playWorld1AfterCheckpoint(wardenBeforeJed: Boolean = false, completionist: Boolean = false) {
            // Navigate through checkpoint gate to Admin Lobby
            navigateLegalPath("admin_lobby")
            talkToNpc("Foreman Boggs")
            if (completionist) {
                navigateLegalPath("server_hub")
                performAction("start_hack_sq04")
                performAction("w1_sq04_thaw_console")
                performAction("w1_sq04_read_ports")
                performAction("start_hack_sq04")
                finishDialogue("board")
                assertTrue("w1_sq04" in harness.sessionStore.state.value.completedQuests)
            }
            navigateLegalPath("admin_security")
            harness.playPublicEncounter("acoustic_bulwark")
            navigateLegalPath("admin_lobby")
            talkToNpc("Foreman Boggs")

            // Navigate into deep mine and restore auxiliary power at Cavern Junction
            navigateLegalPath("mine_landing")
            navigateLegalPath("mine_alpha")
            harness.playPublicEncounter("echo_borer")
            navigateLegalPath("mine_junction")
            performAction("evt_mine_power_on")
            if (completionist) {
                navigateLegalPath("mine_shunt")
                performAction("read_datapad_sq05")
                assertTrue("w1_sq05" in harness.sessionStore.state.value.completedQuests)
                navigateLegalPath("mine_junction")
                performAction("w1_cart_track_diverted")
                performAction("w1_cart_release")
                navigateLegalPath("mine_sealed_munitions_vault")
                harness.explorationVm.collectAllGroundItems()
                harness.settle()
            }

            // Traverse powered mine rooms to Echo Gap and claim Relic
            navigateLegalPath("echo_gap")
            navigateLegalPath("echo_heart")
            val tuningAction = harness.explorationVm.uiState.value.actions.single {
                it is com.example.starborn.domain.model.TuningPuzzleAction && it.puzzleId == "w1_echo_counter_tune"
            }
            harness.explorationVm.onActionSelected(tuningAction)
            harness.settle()
            val puzzle = requireNotNull(harness.explorationVm.uiState.value.tuningPuzzle)
            puzzle.sliders.forEach { harness.explorationVm.updateTuningSlider(it.id, it.target) }
            harness.explorationVm.submitTuningPuzzle()
            harness.settle()
            while (harness.cinematicCoordinator.state.value != null) {
                harness.explorationVm.skipCinematic()
                harness.settle()
            }
            assertTrue("w1_mq03 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq03"))
            assertTrue("w1_mq04 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq04"))

            // Escape lockdown to launch lift
            navigateLegalPath("launch_lift")
            harness.restAtVisibleStop()
            harness.playPublicEncounter("acoustic_bulwark")
            if (wardenBeforeJed) {
                navigateLegalPath("launch_checkpoint")
                harness.explorationVm.collectAllGroundItems()
                harness.settle()
                harness.playPublicEncounter("dominion_dampener")
                navigateLegalPath("launch_bay")
                harness.playPublicEncounter("the_iron_warden")
                navigateLegalPath("launch_lift")
            }
            talkToNpc("Jed")
            assertTrue("w1_mq04 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq04"))
            assertTrue("w1_mq05 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq05"))

            // Launch Bay confrontation
            harness.restAtVisibleStop()
            navigateLegalPath("launch_checkpoint")
            harness.explorationVm.collectAllGroundItems()
            harness.settle()
            if (!wardenBeforeJed) harness.playPublicEncounter("dominion_dampener")
            navigateLegalPath("launch_bay")
            if (!wardenBeforeJed) {
                talkToNpc("The Warden")
                harness.playPublicEncounter("the_iron_warden")
            }
            assertTrue("A real Warden victory must be credited even if it preceded Jed's handoff",
                "ms_w1_warden_defeated" in harness.sessionStore.state.value.completedMilestones)
            talkToNpc("Zeke")
            navigateLegalPath("launch_pod")
            assertFalse("The rail is a launch sequence, not a walkable pre-launch exit",
                "north" in harness.explorationVm.uiState.value.availableConnections)
            harness.explorationVm.travel("north")
            harness.settle()
            assertEquals("launch_pod", harness.sessionStore.state.value.roomId)
            talkToNpc("Zeke")
            performAction("use_nav_console")

            assertEquals("sector9_crash_site", harness.sessionStore.state.value.roomId)
            assertTrue("w1_mq05 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq05"))
            assertTrue("w2_mq01 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq01"))
        }

        fun playWorld2() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w2_mq01"))
            talkToNpc("Zeke")
            performAction("w2_mq01_examine_pod")
            navigateLegalPath("sector9_pod_interior")
            performAction("w2_mq01_stabilize_zeke")
            navigateLegalPath("sector9_landing_stream")
            assertTrue("w2_mq01 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq01"))

            // MQ02: The Signal
            talkToNpc("Zeke")
            navigateLegalPath("sector9_canopy")
            harness.playPublicEncounter("stalker_vine", retreat = true)
            navigateLegalPath("sector9_stream_cave_depths")
            harness.playPublicEncounter("echo_borer")
            navigateLegalPath("sector9_temple_gate")
            performAction("w2_mq02_use_chime")
            assertTrue("w2_mq02 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq02"))
            assertTrue("w2_mq03 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq03"))

            // MQ03: Sleeping Giant (Orion joins)
            performAction("w2_mq03_inspect_murals")
            navigateLegalPath("sector9_stasis_chamber")
            performAction("w2_mq03_inspect_pod")
            navigateLegalPath("sector9_stasis_observation")
            performAction("w2_mq03_read_mural_overview")
            navigateLegalPath("sector9_stasis_coolant_lines")
            performAction("w2_mq03_stabilize_coolant")
            navigateLegalPath("sector9_stasis_ring_array")
            performAction("w2_mq03_align_complete")
            finishDialogue("correct")
            navigateLegalPath("sector9_stasis_chamber")
            talkToNpc("Orion")
            performAction("w2_mq03_recover_bridge")
            assertTrue("w2_mq03 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq03"))
            assertTrue("Orion joins party", harness.sessionStore.state.value.partyMembers.contains("orion"))
            assertTrue("w2_mq04 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq04"))

            // MQ04: The Hunter (Source Beast & Gh0st joins)
            navigateLegalPath("sector9_ridge_plateau")
            harness.restAtVisibleStop()
            navigateLegalPath("sector9_canopy_ridge")
            performAction("w2_mq04_confront")
            performAction("w2_mq04_beast_ambush")
            harness.playPublicEncounter("the_beast", pending = true)
            performAction("w2_mq04_anchor_drill")
            assertTrue("w2_mq04 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq04"))
            assertTrue("Gh0st joins party", harness.sessionStore.state.value.partyMembers.contains("gh0st"))
            assertTrue("w2_mq05 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq05"))

            // MQ05: Liftoff to Spire
            navigateLegalPath("sector9_source_gate")
            navigateLegalPath("sector9_gate_emitter_left")
            performAction("w2_mq05_stabilize_horn")
            navigateLegalPath("sector9_gate_emitter_right")
            performAction("w2_mq05_ground_cup")
            navigateLegalPath("sector9_power_valve")
            performAction("w2_mq05_read_pressure_gauge")
            navigateLegalPath("sector9_power_breaker_deck")
            performAction("w2_mq05_overload_breakers")
            navigateLegalPath("sector9_source_gate")
            performAction("w2_mq05_bypass_gate")
            finishDialogue("correct")
            performAction("w2_mq05_inspect_astra")
            navigateLegalPath("sector9_landing_drop")
            performAction("w2_mq05_collect_conduits")
            performAction("w2_mq05_return_to_hangar")
            performAction("w2_mq05_reboot")
            performAction("w2_mq05_launch")
            finishDialogue("depart")
            while (harness.cinematicCoordinator.state.value != null) {
                harness.explorationVm.skipCinematic()
                harness.settle()
            }
            assertTrue("w2_mq05 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq05"))
            assertEquals("spire_sewers_landing", harness.sessionStore.state.value.roomId)
        }

        fun playWorld3() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w3_mq11"))
            navigateLegalPath("spire_sewers_scale_01")
            harness.playPublicEncounter("sewer_crawler")
            navigateLegalPath("spire_vent_output")
            navigateLegalPath("spire_zekes_apartment")
            talkToNpc("Zeke")
            assertTrue("w3_mq11 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq11"))
            assertTrue("w3_mq12 active", harness.sessionStore.state.value.activeQuests.contains("w3_mq12"))

            // MQ12: Heist Prep
            navigateLegalPath("spire_the_static")
            talkToNpc("Jax", "jax_w3_mq12_request_codes")
            navigateLegalPath("spire_underrail_platform")
            performAction("w3_mq12_map_patrols")
            navigateLegalPath("spire_security_kiosk")
            performAction("w3_mq12_interrogate_guard")
            navigateLegalPath("spire_elevator_service_gate")
            performAction("w3_mq12_copy_badges")
            navigateLegalPath("spire_safehouse_roof")
            performAction("w3_mq12_source_disguises")
            navigateLegalPath("spire_zekes_apartment")
            performAction("w3_mq12_hack_blueprints")
            performAction("w3_mq12_assemble_planning")
            assertTrue("w3_mq12 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq12"))

            // Upper City Infiltration
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w3_mq13"))
            navigateLegalPath("spire_skypark_dome")
            performAction("w3_mq13_blend_in")
            navigateLegalPath("spire_laundry_service")
            performAction("w3_mq13_disable_sensors")
            performAction("w3_mq13_enter_lobby")
            assertTrue("w3_mq13 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq13"))
            assertTrue("w3_mq14 active", harness.sessionStore.state.value.activeQuests.contains("w3_mq14"))

            navigateLegalPath("spire_archive_vault")
            performAction("w3_mq14_read_containment_field")
            navigateLegalPath("spire_prism_gallery")
            performAction("w3_mq14_read_prism_shutters")
            navigateLegalPath("spire_archive_vault")
            performAction("w3_mq14_trace_command_tethers")
            navigateLegalPath("spire_prism_gallery")
            performAction("w3_mq14_solve_light_puzzle")
            navigateLegalPath("spire_archive_vault")
            talkToNpc("Zeke")
            assertTrue("w3_mq14 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq14"))
            assertTrue("The Lens acquired", (harness.sessionStore.state.value.inventory["the_lens"] ?: 0) >= 1)

            navigateLegalPath("spire_skypark_scale_02")
            harness.restAtVisibleStop()
            navigateLegalPath("spire_exec_lounge_bar")
            harness.buyAtVisibleShop("upper_city_lounge", "medkit", 6)
            navigateLegalPath("spire_landing_pad_roof")
            harness.playPublicEncounter("heavy_mech")
            navigateLegalPath("spire_skypark_scale_02")
            harness.restAtVisibleStop()
            navigateLegalPath("spire_landing_pad_roof")
            harness.playPublicEncounter("administrator_boss")
            performAction("w3_scan_shield_gap")
            performAction("w3_mq15_launch_astra")
            assertTrue("w3_mq15 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq15"))
            assertEquals("foundry_slag_landing", harness.sessionStore.state.value.roomId)
        }

        fun prepareWorld4Rest() {
            performAction("w4_mq16_land_shelf")
            navigateLegalPath("foundry_obsidian_shelf_scale_02")
            harness.restAtVisibleStop()
        }

        fun playWorld4() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w4_mq16"))
            performAction("w4_mq16_land_shelf")
            navigateLegalPath("foundry_slag_river")
            performAction("w4_mq16_cross_slag_river")
            navigateLegalPath("foundry_waste_intake")
            performAction("w4_mq16_hack_cooling_vents")
            navigateLegalPath("foundry_service_airlock")
            performAction("w4_mq16_enter_airlock")
            navigateLegalPath("foundry_waste_intake")
            performAction("w4_mq17_access_phantom_terminal")
            navigateLegalPath("foundry_cooling_springs")
            performAction("w4_mq17_regroup_springs")
            navigateLegalPath("foundry_conveyor_belt")
            performAction("w4_mq18_navigate_conveyors")
            navigateLegalPath("foundry_conditioning_chamber")
            performAction("w4_mq18_defeat_prototypes")
            navigateLegalPath("foundry_forge_anvil")
            performAction("w4_mq18_overload_matrix")

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w4_mq19"))
            navigateLegalPath("foundry_conveyor_belt_scale_01")
            performAction("w4_mq19_read_pulse_board")
            navigateLegalPath("foundry_conveyor_belt_scale_03")
            performAction("w4_mq19_read_grease_marks")
            navigateLegalPath("foundry_conveyor_belt_scale_04")
            performAction("w4_mq19_throw_override_paddle")
            navigateLegalPath("foundry_forge_scale_01")
            performAction("w4_mq19_starve_breath_pistons")
            navigateLegalPath("foundry_forge_anvil")
            performAction("w4_mq19_open_anvil_cradle")
            talkToNpc("Nova")
            assertTrue("w4_mq19 completed", harness.sessionStore.state.value.completedQuests.contains("w4_mq19"))
            assertTrue("The Anvil acquired", (harness.sessionStore.state.value.inventory["the_anvil"] ?: 0) >= 1)
            assertTrue("w4_mq20 active", harness.sessionStore.state.value.activeQuests.contains("w4_mq20"))

            // MQ20: Confront Rylos & Titan Walker Boss
            navigateLegalPath("foundry_forge_control_alcove")
            harness.restAtVisibleStop()
            navigateLegalPath("foundry_titan_dock")
            talkToNpc("Rylos")
            harness.playPublicEncounter("titan_walker_boss")
            assertTrue("Real Titan victory must advance the quest", "ms_w4_titan_defeated" in harness.sessionStore.state.value.completedMilestones)
            performAction("w4_mq20_escape_launch")
            assertEquals("Escape must wait for both stolen components", "foundry_titan_dock", harness.sessionStore.state.value.roomId)
            assertTrue("Unprepared launch must not finish the quest", "w4_mq20" in harness.sessionStore.state.value.activeQuests)
            assertFalse("Unprepared launch must not unlock World 5", "ms_w5_access_unlocked" in harness.sessionStore.state.value.completedMilestones)
            navigateLegalPath("foundry_power_core")
            performAction("w4_mq20_steal_engine")
            performAction("w4_mq20_steal_arrays")
            assertTrue(harness.inventoryService.hasItem("deep_core_engine"))
            assertTrue(harness.inventoryService.hasItem("phase_cutter_arrays"))
            navigateLegalPath("foundry_titan_dock")
            performAction("w4_mq20_escape_launch")
            assertTrue("w4_mq20 completed", harness.sessionStore.state.value.completedQuests.contains("w4_mq20"))
            assertTrue("ms_w5_access_unlocked set", harness.sessionStore.state.value.completedMilestones.contains("ms_w5_access_unlocked"))
            assertEquals("orbital_executive_dock", harness.sessionStore.state.value.roomId)
        }

        fun prepareWorld5Supplies() {
            navigateLegalPath("orbital_customs_lounge")
            harness.explorationVm.equipArmor("orion", "heat_liner")
            harness.settle()
            assertEquals("Earned liner must equip through public controls", "heat_liner", harness.sessionStore.state.value.equippedArmors["orion"])
            harness.sellAtVisibleShop("orbital_dispensary", "power_cell", 7)
            harness.sellAtVisibleShop("orbital_dispensary", "composite_plate", 4)
            harness.buyAtVisibleShop("orbital_dispensary", "medkit_plus", 6)
            harness.buyAtVisibleShop("orbital_dispensary", "orbital_glimmer_tonic", 1)
            harness.explorationVm.useInventoryItem("orbital_glimmer_tonic", "zeke")
            harness.settle()
            assertEquals("orbital_glimmer_tonic", harness.sessionStore.state.value.activeMealBuff?.recipeId)
            assertEquals(6, harness.sessionStore.state.value.activeMealBuff?.speedBonus)
            assertFalse("Eating must consume the purchased portion", harness.inventoryService.hasItem("orbital_glimmer_tonic"))
            harness.restAtVisibleStop()
            navigateLegalPath("orbital_executive_dock")
        }

        fun playWorld5() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w5_mq21"))
            if (harness.sessionStore.state.value.progressionPrototypeVersion == 1) {
                val crew = com.example.starborn.domain.leveling.CrewBuildPrototype
                val purchases = listOf("nova" to listOf(com.example.starborn.domain.leveling.NovaBuildPrototype.STEPS),
                    "zeke" to listOf(crew.COVER, crew.REINFORCED_HARNESS, crew.STEADY_COVER, crew.TRAINING_SESSION),
                    "orion" to listOf(crew.FIELD_RESILIENCE, crew.FIELD_TREATMENT, crew.TRIAGE, crew.CLEAR_FOCUS),
                    "gh0st" to listOf(crew.SCOPE, crew.STEADY_AIM, crew.EXECUTE, crew.QUICK_POSITION))
                purchases.forEach { (owner, ids) -> ids.forEach { harness.learnAtVisibleSkillTree(owner, it) } }
            } else {
                harness.learnAtVisibleSkillTree("nova", "nova_quiet_steps")
                harness.learnAtVisibleSkillTree("zeke", "zeke_motivational_speech")
            }
            prepareWorld5Supplies()
            performAction("w5_mq21_force_dock")
            assertFalse("Docking must wait for the real fighter victory", "ms_w5_forced_dock" in harness.sessionStore.state.value.completedMilestones)
            harness.playPublicEncounter("orbital_fighter")
            assertTrue("ms_w5_fighter_screen_broken" in harness.sessionStore.state.value.completedMilestones)
            performAction("w5_mq21_force_dock")
            performAction("w5_mq21_hack_airlock")
            navigateLegalPath("orbital_solarium")
            performAction("w5_mq22_cross_solarium")
            navigateLegalPath("orbital_grand_concourse")
            harness.playPublicEncounter("compliance_officer")
            navigateLegalPath("orbital_service_shaft")
            performAction("w5_mq22_traverse_shaft")
            navigateLegalPath("orbital_server_farm")
            performAction("w5_mq22_access_mainframe")
            navigateLegalPath("deep_mainframe_nave")
            performAction("w5_mq22_find_thorne")

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w5_mq23"))
            navigateLegalPath("orbital_server_farm")
            performAction("w5_mq23_navigate_maze")
            navigateLegalPath("deep_firewall_alpha")
            performAction("w5_mq23_firewall_alpha")
            navigateLegalPath("deep_firewall_beta")
            performAction("w5_mq23_firewall_beta")
            navigateLegalPath("deep_firewall_gamma")
            performAction("w5_mq23_firewall_gamma")
            assertTrue("Firewall controls alone must not complete the quest", "w5_mq23" in harness.sessionStore.state.value.activeQuests)
            harness.playPublicEncounter("firewall_construct")
            assertTrue("w5_mq23 completed", harness.sessionStore.state.value.completedQuests.contains("w5_mq23"))
            assertTrue("w5_mq24 active", harness.sessionStore.state.value.activeQuests.contains("w5_mq24"))

            navigateLegalPath("deep_anchor_chamber")
            performAction("w5_mq24_find_elara")
            performAction("w5_mq24_take_anchor")
            assertTrue("w5_mq24 completed", harness.sessionStore.state.value.completedQuests.contains("w5_mq24"))
            assertTrue("The Anchor acquired", (harness.sessionStore.state.value.inventory["anchor_relic"] ?: 0) >= 1)
            assertTrue("w5_mq25 active", harness.sessionStore.state.value.activeQuests.contains("w5_mq25"))

            navigateLegalPath("deep_throne_room")
            performAction("w5_mq25_soloist")
            navigateLegalPath("deep_tear")
            performAction("w5_mq25_enter_tear")
            assertEquals("The breach must wait for the Avatar victory", "deep_tear", harness.sessionStore.state.value.roomId)
            assertFalse("Uncleared breach must not unlock World 6", "ms_w6_access_unlocked" in harness.sessionStore.state.value.completedMilestones)
            harness.playPublicEncounter("compliance_avatar")
            navigateLegalPath("orbital_customs_lounge")
            harness.sellAtVisibleShop("orbital_dispensary", "grav_boots", 1)
            harness.buyAtVisibleShop("orbital_dispensary", "medkit_plus", 6)
            harness.buyAtVisibleShop("orbital_dispensary", "orbital_glimmer_tonic", 3)
            navigateLegalPath("deep_tear")
            performAction("w5_mq25_enter_tear")
            assertTrue("w5_mq25 completed", harness.sessionStore.state.value.completedQuests.contains("w5_mq25"))
            assertTrue("ms_w6_access_unlocked set", harness.sessionStore.state.value.completedMilestones.contains("ms_w6_access_unlocked"))
            assertEquals("source_campfire", harness.sessionStore.state.value.roomId)
        }

        fun playWorld6() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w6_mq26"))
            if (useProductionCrewRules) {
                // Spend earned late-game AP through the public tree before the Source fights.
                // The historical route's two-node build is not a crew campaign allocation.
                val nova = com.example.starborn.domain.leveling.NovaBuildPrototype
                listOf(nova.GROUNDED, nova.VENT, nova.BREAKER, nova.SEAM).forEach {
                    harness.learnAtVisibleSkillTree("nova", it)
                }
            }
            harness.explorationVm.useInventoryItem("orbital_glimmer_tonic")
            harness.settle()
            harness.restAtVisibleStop()
            navigateLegalPath("source_zeke_review_loop")
            harness.playPublicEncounter("manager_projection")
            navigateLegalPath("source_campfire")
            harness.restAtVisibleStop()
            navigateLegalPath("source_gh0st_nightmare_node_scale_01")
            harness.playPublicEncounter("endless_war_echo")
            navigateLegalPath("source_campfire")
            harness.restAtVisibleStop()
            navigateLegalPath("source_orion_tide_well")
            harness.playPublicEncounter("silent_shore_wraith")
            navigateLegalPath("source_campfire")
            performAction("w6_mq26_reassemble")

            navigateLegalPath("source_echo_mines")
            navigateLegalPath("source_echo_patrol")
            performAction("w6_mq27_evade_manager")

            navigateLegalPath("source_memory_bridge")
            listOf("anchor_zeke", "anchor_ghost", "anchor_orion", "anchor_nova", "build_bridge")
                .forEach { performAction("w6_mq28_$it") }
            navigateLegalPath("source_memory_bridge_span")
            performAction("w6_mq28_final_banter")
            harness.explorationVm.useInventoryItem("orbital_glimmer_tonic")
            harness.settle()
            harness.restAtVisibleStop()
            navigateLegalPath("source_memory_threshold")
            performAction("w6_mq28_reach_singularity")

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w6_mq29"))
            navigateLegalPath("source_memory_stair")
            performAction("w6_mq29_refuse_jed_revision")
            performAction("w6_mq29_refuse_astra_revision")
            performAction("w6_mq29_refuse_foundry_revision")
            performAction("w6_mq29_climb_stair")
            navigateLegalPath("source_memory_fragments")
            harness.playPublicEncounter("source_shadow")
            navigateLegalPath("source_memory_bridge_span")
            harness.restAtVisibleStop()
            navigateLegalPath("source_spire_arena")
            harness.playPublicEncounter("memory_leak")

            navigateLegalPath("source_center")
            assertTrue("w6_mq29 completed", harness.sessionStore.state.value.completedQuests.contains("w6_mq29"))
            assertTrue("w6_mq30 active", harness.sessionStore.state.value.activeQuests.contains("w6_mq30"))

            // Finale: Confront Vale, defeat Ascended Vale & Ascended God
            performAction("w6_mq30_confront_vale")
            val lockedTuning = harness.explorationVm.uiState.value.actions.single {
                it is com.example.starborn.domain.model.TuningPuzzleAction && it.puzzleId == "source_harmonic_focus"
            }
            harness.explorationVm.onActionSelected(lockedTuning)
            harness.settle()
            assertTrue("Final tuning must remain locked before both victories", harness.explorationVm.uiState.value.tuningPuzzle == null)
            assertFalse("Premature tuning must not complete the game", "ms_game_complete" in harness.sessionStore.state.value.completedMilestones)
            harness.playPublicEncounter("ascended_vale")
            harness.explorationVm.onActionSelected(lockedTuning)
            harness.settle()
            assertTrue("Vale victory alone must not unlock final tuning", harness.explorationVm.uiState.value.tuningPuzzle == null)
            navigateLegalPath("source_memory_bridge_span")
            harness.restAtVisibleStop()
            navigateLegalPath("source_center_node_scale_03")
            harness.playPublicEncounter("ascended_god")
            navigateLegalPath("source_center")
            val finalTuning = harness.explorationVm.uiState.value.actions.single {
                it is com.example.starborn.domain.model.TuningPuzzleAction && it.puzzleId == "source_harmonic_focus"
            }
            harness.explorationVm.onActionSelected(finalTuning)
            harness.settle()
            requireNotNull(harness.explorationVm.uiState.value.tuningPuzzle).sliders.forEach {
                harness.explorationVm.updateTuningSlider(it.id, it.target)
            }
            harness.explorationVm.submitTuningPuzzle()
            harness.settle()
            while (harness.cinematicCoordinator.state.value != null) {
                harness.explorationVm.skipCinematic()
                harness.settle()
            }

            assertTrue("w6_mq30 completed", harness.sessionStore.state.value.completedQuests.contains("w6_mq30"))
        }
    }

    private inner class ProductionCampaignHarness(
        private val saveFolder: File,
        private val dispatcher: TestDispatcher,
        private val coroutineScope: CoroutineScope,
        initialState: GameSessionState? = null,
        private val prototypeBranch: String? = null
    ) {
        val moshi = MoshiProvider.instance
        private val reader = AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        val worldDataSource = WorldAssetDataSource(reader)
        val itemRepository = ItemRepository(ItemAssetDataSource(reader)).apply { load() }
        val questRepository = QuestRepository(QuestAssetDataSource(reader)).apply { load() }
        val milestoneRepository = MilestoneRepository(MilestoneAssetDataSource(reader)).apply { load() }
        val themeRepository = ThemeRepository(ThemeAssetDataSource(reader), ThemeStyleAssetDataSource(reader)).apply { load() }
        val craftingDataSource = CraftingAssetDataSource(reader)
        val cinematicDataSource = CinematicAssetDataSource(reader, moshi)
        val dialogueDataSource = DialogueAssetDataSource(reader)
        val eventDataSource = EventAssetDataSource(reader)

        val rooms = worldDataSource.loadRooms()
        val roomsById = rooms.associateBy { it.id }
        val hubNodes = worldDataSource.loadHubNodes()

        val sessionStore = GameSessionStore().apply {
            restore(initialState ?: com.example.starborn.domain.session.GameBootstrap.newGame(
                worldDataSource.loadCharacters(), itemRepository.allItems().toList()).copy(
                worldId = "world_1",
                hubId = "hub_1_homestead",
                roomId = "pit_nova_bunk",
                progressionPrototypeVersion = if (prototypeBranch != null || useProductionCrewRules) 1 else 0,
                playerId = "nova",
                partyMembers = listOf("nova"),
                unlockedSkills = setOf("nova_arc_tether"),
                activeQuests = setOf("w1_mq01"),
                trackedQuestId = "w1_mq01",
                questStageById = mapOf("w1_mq01" to "wake_in_the_pit"),
                inventory = if (useProductionCrewRules) emptyMap() else mapOf("cryo_inductor" to 1, "scrap_metal" to 2)
            ))
        }

        val inventoryService = InventoryService(itemRepository).apply {
            loadItems()
            restore(sessionStore.state.value.inventory)
        }
        // Match player-session publication; historical prepared fixtures keep their original wiring.
        private val productionInventoryBridge = if (useProductionCrewRules)
            com.example.starborn.domain.session.SessionInventoryBridge(sessionStore, inventoryService) else null
        val craftingService = CraftingService(craftingDataSource, inventoryService, sessionStore)
        val levelingData = requireNotNull(worldDataSource.loadLevelingData())
        val levelingManager = LevelingManager(levelingData)
        val progressionData = requireNotNull(worldDataSource.loadProgressionData())
        val xpAwarder = ExplorationXpAwarder(sessionStore, levelingManager, progressionData)
        val uiEventBus = UiEventBus()
        val promptManager = UIPromptManager()
        val questRuntimeManager = QuestRuntimeManager(questRepository, sessionStore, coroutineScope, uiEventBus)
        val milestoneManager = MilestoneRuntimeManager(milestoneRepository, sessionStore, promptManager, coroutineScope)
        val environmentThemeManager = EnvironmentThemeManager(themeRepository)
        val cinematicService = CinematicService(cinematicDataSource)
        val cinematicCoordinator = CinematicCoordinator(cinematicService)
        val audioRouter = AudioRouter(AudioBindings())
        val voiceoverController = mock<VoiceoverController>()
        val shopRepository = ShopRepository(ShopAssetDataSource(reader)).apply { load() }
        val fishingService = FishingService(com.example.starborn.data.assets.FishingAssetDataSource(reader), inventoryService, kotlin.random.Random(42), sessionStore, craftingService)
        val encounterCoordinator = EncounterCoordinator()
        val userSettingsFlow = MutableStateFlow(UserSettings())
        val userSettingsStore = mock<UserSettingsStore> {
            on { settings } doReturn userSettingsFlow
        }
        val persistence = GameSessionPersistence(saveFolder)
        val saveRepository = mock<GameSaveRepository>()

        val rawEvents = eventDataSource.loadEvents()
        val rawDialogue = dialogueDataSource.loadDialogue()
        var dialogueTriggerListener: ((String) -> Boolean)? = null

        val dialogueService = DialogueService(
            rawDialogue,
            DialogueConditionEvaluator { cond -> conditionEvaluator(cond, sessionStore.state.value) },
            DialogueTriggerHandler { trig ->
                dialogueTriggerListener?.invoke(trig) ?: false
            }
        )

        val dispatcherProvider = object : DispatcherProvider {
            override val io = dispatcher
            override val default = dispatcher
            override val main = dispatcher
        }

        val explorationVm: ExplorationViewModel = ExplorationViewModel(
            worldAssets = worldDataSource,
            sessionStore = sessionStore,
            dialogueService = dialogueService,
            inventoryService = inventoryService,
            craftingService = craftingService,
            cinematicCoordinator = cinematicCoordinator,
            questRepository = questRepository,
            questRuntimeManager = questRuntimeManager,
            milestoneManager = milestoneManager,
            audioRouter = audioRouter,
            voiceoverController = voiceoverController,
            shopRepository = shopRepository,
            themeRepository = themeRepository,
            environmentThemeManager = environmentThemeManager,
            levelingManager = levelingManager,
            tutorialManager = TutorialRuntimeManager(sessionStore, promptManager, null, coroutineScope),
            promptManager = promptManager,
            fishingService = fishingService,
            saveRepository = saveRepository,
            encounterCoordinator = encounterCoordinator,
            userSettingsStore = userSettingsStore,
            eventDefinitions = rawEvents,
            dispatchers = dispatcherProvider,
            telemetry = NoOpPlaytestTelemetry,
            dialogueTriggerBinder = { listener -> dialogueTriggerListener = listener }
        )

        val hubVm: HubViewModel = HubViewModel(
            worldAssets = worldDataSource,
            questRepository = questRepository,
            sessionStore = sessionStore,
            dispatchers = dispatcherProvider
        )

        init {
            settle()
        }

        private var lastPrototypeProbe: Triple<Int, Int, Set<String>>? = null
        fun settle() {
            dispatcher.scheduler.runCurrent()
            val branch = prototypeBranch
            val snapshot = sessionStore.state.value
            val probe = Triple(snapshot.playerAp, snapshot.purchasedSkillNodes.size, snapshot.completedMilestones)
            if (branch != null && probe != lastPrototypeProbe && snapshot.playerAp > 0) {
                explorationVm.openSkillTree("nova")
                dispatcher.scheduler.runCurrent()
                while (true) {
                    val next = explorationVm.uiState.value.skillTreeOverlay?.branches?.firstOrNull { it.title == branch }
                        ?.nodes?.firstOrNull { it.status.canPurchase } ?: break
                    explorationVm.unlockSkillNode(next.id)
                    dispatcher.scheduler.runCurrent()
                    check(next.id in sessionStore.state.value.purchasedSkillNodes)
                }
                explorationVm.closeSkillTreeOverlay()
                dispatcher.scheduler.runCurrent()
                lastPrototypeProbe = sessionStore.state.value.let { Triple(it.playerAp, it.purchasedSkillNodes.size, it.completedMilestones) }
            }
            if (recordSystemsAudit) systemsAudit.capture(explorationVm, sessionStore, inventoryService, craftingService, assistance.size, coroutineScope)
        }
        fun close() { productionInventoryBridge?.close() }

        fun saveSlot(slot: Int): GameSessionState = runBlocking {
            persistence.writeSlot(slot, sessionStore.state.value)
            requireNotNull(persistence.readSlot(slot))
        }

        fun loadSlot(slot: Int): GameSessionState = runBlocking {
            val loaded = requireNotNull(persistence.readSlot(slot))
            sessionStore.restore(loaded)
            inventoryService.restore(loaded.inventory)
            settle()
            loaded
        }

        fun findBfsPath(startRoomId: String, targetRoomId: String): List<Pair<String, String>>? {
            if (startRoomId == targetRoomId) return emptyList()
            val queue = ArrayDeque<Pair<String, List<Pair<String, String>>>>()
            queue.add(startRoomId to emptyList())
            val visited = mutableSetOf(startRoomId)

            while (queue.isNotEmpty()) {
                val (current, currentPath) = queue.removeFirst()
                if (current == targetRoomId) return currentPath
                val room = roomsById[current] ?: continue
                for ((dir, nextRoom) in room.connections) {
                    if (nextRoom !in visited && nextRoom in roomsById) {
                        visited.add(nextRoom)
                        queue.add(nextRoom to (currentPath + (dir to nextRoom)))
                    }
                }
            }
            return null
        }

        fun restAtVisibleStop() {
            val stop = explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.RestStopAction>()
                .first { it.cookSource == null }
            explorationVm.onActionSelected(stop)
            settle()
        }

        fun buyAtVisibleShop(shopId: String, itemId: String, quantity: Int) {
            val action = explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ShopAction>()
                .single { it.shopId == shopId }
            explorationVm.onActionSelected(action)
            settle()
            explorationVm.enterPendingShop()
            settle()
            com.example.starborn.feature.shop.ShopController(shopId, shopRepository, itemRepository, inventoryService, sessionStore, coroutineScope).use { shop ->
                val listing = shop.uiState.value.itemsForSale.single { it.id == itemId }
                val credits = sessionStore.state.value.playerCredits
                val owned = inventoryService.snapshot()[itemId] ?: 0
                assertTrue("Earned credits must cover supplies", credits >= listing.price * quantity)
                shop.buyItem(itemId, quantity)
                settle()
                assertEquals(owned + quantity, inventoryService.snapshot()[itemId])
                assertEquals(credits - listing.price * quantity, sessionStore.state.value.playerCredits)
                publicCombats += "shop=$shopId bought=$itemId quantity=$quantity cost=${listing.price * quantity} remainingCredits=${sessionStore.state.value.playerCredits}"
            }
        }

        val recordedApPurchases = linkedMapOf<String, Int>()
        fun learnAtVisibleSkillTree(characterId: String, nodeId: String) {
            explorationVm.openSkillTree(characterId)
            settle()
            val overlay = requireNotNull(explorationVm.uiState.value.skillTreeOverlay)
            val node = overlay.branches.flatMap { it.nodes }.single { it.id == nodeId }
            assertTrue("Earned AP must make $nodeId purchasable", node.status.canPurchase)
            val before = sessionStore.state.value.playerAp
            explorationVm.unlockSkillNode(nodeId)
            settle()
            assertTrue(nodeId in sessionStore.state.value.unlockedSkills || nodeId in sessionStore.state.value.purchasedSkillNodes)
            assertEquals(before - node.costAp, sessionStore.state.value.playerAp)
            recordedApPurchases[nodeId] = node.costAp
            explorationVm.closeSkillTreeOverlay()
            settle()
            publicCombats += "skillTree=$characterId purchased=$nodeId apSpent=${node.costAp} remainingAp=${sessionStore.state.value.playerAp}"
        }

        fun sellAtVisibleShop(shopId: String, itemId: String, quantity: Int) {
            val action = explorationVm.uiState.value.actions.filterIsInstance<com.example.starborn.domain.model.ShopAction>()
                .single { it.shopId == shopId }
            explorationVm.onActionSelected(action)
            settle()
            explorationVm.enterPendingShop()
            settle()
            com.example.starborn.feature.shop.ShopController(shopId, shopRepository, itemRepository, inventoryService, sessionStore, coroutineScope).use { shop ->
                val listing = shop.uiState.value.sellInventory.single { it.id == itemId }
                val sold = if (prototypeBranch != null || useProductionCrewRules) minOf(quantity, listing.quantity) else quantity
                assertTrue("Owned $itemId must be sellable (requested=$quantity, available=${listing.quantity})", listing.canSell && listing.quantity >= sold)
                val credits = sessionStore.state.value.playerCredits
                val owned = inventoryService.snapshot()[itemId] ?: 0
                shop.sellItem(itemId, sold)
                settle()
                assertEquals(owned - sold, inventoryService.snapshot()[itemId] ?: 0)
                assertEquals(credits + listing.price * sold, sessionStore.state.value.playerCredits)
                publicCombats += "shop=$shopId sold=$itemId quantity=$sold credits=${listing.price * sold} remainingCredits=${sessionStore.state.value.playerCredits}"
            }
        }

        fun playPublicEncounter(enemyId: String, retreat: Boolean = false, pending: Boolean = false) {
            val room = requireNotNull(sessionStore.state.value.roomId)
            if (!pending) {
                assertTrue("Enemy must be present before engagement in $room", enemyId in explorationVm.uiState.value.enemies)
                explorationVm.engageEnemy(enemyId)
                settle()
            }
            val descriptor = requireNotNull(moshi.adapter(com.example.starborn.domain.combat.EncounterDescriptor::class.java)
                .fromJson(sessionStore.state.value.pendingBattleJson))
            val encounterIds = descriptor.enemies.map { it.enemyId }
            assertTrue("Public encounter must include $enemyId", enemyId in encounterIds)
            val registry = StatusRegistry(worldDataSource.loadStatuses())
            val combat = CombatViewModel(
                worldAssets = worldDataSource,
                combatEngine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(17)),
                statusRegistry = registry,
                sessionStore = sessionStore,
                inventoryService = inventoryService,
                itemCatalog = itemRepository,
                levelingManager = levelingManager,
                progressionData = progressionData,
                audioRouter = audioRouter,
                themeRepository = themeRepository,
                environmentThemeManager = environmentThemeManager,
                encounterCoordinator = encounterCoordinator,
                enemyIds = encounterIds,
                tutorialsEnabled = false,
                elapsedRealtime = { dispatcher.scheduler.currentTime },
                random = SeededCombatRandom(17)
            )
            try {
                combat.onScreenReady()
                File("build/reports/campaign-systems/encounter-$enemyId-before.txt").apply {
                    requireNotNull(parentFile).mkdirs()
                    val snapshot = sessionStore.state.value
                    writeText("room=$room enemies=$encounterIds credits=${snapshot.playerCredits}\ninventory=${snapshot.inventory}\nweapons=${snapshot.equippedWeapons}\narmor=${snapshot.equippedArmors}\nhp=${snapshot.partyMemberHp}\nskills=${snapshot.partyMembers.associateWith { combat.skillsForPlayer(it).map { s -> s.id } }}\nmeal=${snapshot.activeMealBuff}\nstats=${combat.combatState?.combatants?.mapValues { it.value.combatant.stats }}\n")
                }
                assertEquals(room, combat.encounterRoomId)
                if (enemyId == "faulted_loader") {
                    assertEquals("Authored HP and vitality overrides must reach combat", com.example.starborn.domain.combat.CombatFormulas.maxHp(45, 1),
                        requireNotNull(combat.combatState).combatants.values.single { it.combatant.side == com.example.starborn.domain.combat.CombatSide.ENEMY }.hp)
                }
                for (tick in 0 until 4800) {
                    dispatcher.scheduler.advanceTimeBy(50)
                    dispatcher.scheduler.runCurrent()
                    if (combat.lungeActorId.value != null) combat.onLungeFinished(combat.lungeToken.value)
                    if (combat.missLungeActorId.value != null) combat.onMissLungeFinished(combat.missLungeToken.value)
                    val state = requireNotNull(combat.combatState)
                    if (state.outcome != null) break
                    sessionStore.state.value.partyMembers.forEach(combat::selectReadyPlayer)
                    if (combat.awaitingAction.value != null) {
                        if (retreat) {
                            combat.attemptRetreat()
                            continue
                        }
                        val actor = requireNotNull(combat.awaitingAction.value)
                        val active = state.combatants.getValue(actor)
                        val livingEnemies = state.combatants.values.filter { it.combatant.side == com.example.starborn.domain.combat.CombatSide.ENEMY && it.isAlive }
                        val controlId = when {
                            (room.startsWith("orbital") || room.startsWith("deep") || room.startsWith("source")) && actor == "orion" && livingEnemies.any { enemy -> enemy.statusEffects.none { it.id == "weak" } } -> "orion_disruption_pulse"
                            else -> null
                        }
                        val control = combat.skillsForPlayer(actor).firstOrNull { it.id == controlId && combat.canUseSkill(actor, it) }
                        if (control != null) {
                            combat.useSkill(control)
                            continue
                        }
                        val injuredAllies = state.combatants.values.count {
                            it.combatant.side == com.example.starborn.domain.combat.CombatSide.PLAYER && it.isAlive && it.hp < it.combatant.stats.maxHp * .7
                        }
                        val supportId = when {
                            actor == "nova" && injuredAllies >= 2 -> "nova_link"
                            actor == "orion" && active.hp < active.combatant.stats.maxHp * .8 && active.statusEffects.none { it.id == "regen" } -> "orion_nano_repair"
                            actor == "zeke" && active.statusEffects.none { it.id == "guard" } -> "zeke_bulwark_stance"
                            else -> null
                        }
                        val support = combat.skillsForPlayer(actor).firstOrNull { it.id == supportId && combat.canUseSkill(actor, it) }
                        val wounded = state.combatants.values.filter {
                            it.combatant.side == com.example.starborn.domain.combat.CombatSide.PLAYER && it.isAlive &&
                                it.hp < it.combatant.stats.maxHp * .65
                        }.minByOrNull { it.hp.toDouble() / it.combatant.stats.maxHp }
                        val healing = combat.inventory.value.filter {
                            it.quantity > 0 && (it.item.effect?.restoreHp ?: 0) > 0 &&
                                it.item.type.equals("consumable", ignoreCase = true)
                        }.maxByOrNull { it.item.effect?.restoreHp ?: 0 }
                        if ((prototypeBranch != null || useProductionCrewRules) && actor == "orion" && wounded != null) {
                            val recovery = combat.skillsForPlayer(actor).firstOrNull {
                                it.id in setOf("orion_field_treatment", "orion_stasis_stitch") && combat.canUseSkill(actor, it)
                            }
                            if (recovery != null) {
                                combat.useSkill(recovery, listOf(wounded.combatant.id))
                                continue
                            }
                        }
                        if (support?.id == "nova_link") {
                            combat.useSkill(support)
                            continue
                        } else if (wounded != null && healing != null) {
                            combat.useItem(healing, wounded.combatant.id)
                            continue
                        } else if (support != null) {
                            combat.useSkill(support)
                            continue
                        }
                        val target = state.combatants.values.filter { it.combatant.side == com.example.starborn.domain.combat.CombatSide.ENEMY && it.isAlive }
                            .minBy { it.hp }.combatant.id
                        if ((prototypeBranch != null || useProductionCrewRules) && actor == "nova") {
                            val preferred = if (livingEnemies.size > 1) "nova_scatter_charge" else "nova_breaker_bolt"
                            val move = combat.skillsForPlayer(actor).firstOrNull { it.id == preferred && combat.canUseSkill(actor, it) }
                            if (move != null) {
                                combat.useSkill(move, if (livingEnemies.size > 1) livingEnemies.map { it.combatant.id } else listOf(target))
                                prototypeSkillsUsed += preferred
                                continue
                            }
                        }
                        val availableSkills = combat.skillsForPlayer(actor).filter {
                            (it.id in setOf("nova_arc_tether", "nova_blast_wave", "nova_hydraulic_kick", "zeke_shatter_blow", "orion_prism_lance", "gh0st_headshot") ||
                                ((room.startsWith("orbital") || room.startsWith("deep") || room.startsWith("source")) && it.id in setOf("nova_cryo_vent", "zeke_overload_fists", "gh0st_system_crash", "gh0st_venom_edge")) ||
                                (room.startsWith("source") && it.id == "source_art_stasis")) && combat.canUseSkill(actor, it)
                        }
                        val controlPriorities = if (livingEnemies.size > 1) listOf("nova_blast_wave", "source_art_stasis")
                            else listOf("source_art_stasis", "nova_blast_wave")
                        val priorities = controlPriorities + listOf("gh0st_system_crash", "nova_cryo_vent", "zeke_overload_fists", "gh0st_venom_edge", "nova_arc_tether", "zeke_shatter_blow", "gh0st_headshot", "orion_prism_lance")
                        val skill = if (room.startsWith("orbital") || room.startsWith("deep") || room.startsWith("source")) availableSkills.minByOrNull { priorities.indexOf(it.id) }
                            else availableSkills.maxByOrNull { it.basePower }
                        if (skill != null) {
                            if (combat.targetRequirementFor(skill) == com.example.starborn.feature.combat.viewmodel.TargetRequirement.NONE) {
                                combat.useSkill(skill)
                                val newEntries = requireNotNull(combat.combatState).log.drop(state.log.size)
                                val queued = newEntries
                                    .filterIsInstance<com.example.starborn.domain.combat.CombatLogEntry.ActionQueued>()
                                    .mapNotNull { it.action as? com.example.starborn.domain.combat.CombatAction.SkillUse }
                                    .firstOrNull { it.actorId == actor && it.skillId == skill.id }
                                if (queued == null) {
                                    assertTrue("Missing cast must be explained by an actual skipped turn", newEntries.any {
                                        it is com.example.starborn.domain.combat.CombatLogEntry.TurnSkipped && it.actorId == actor
                                    })
                                } else {
                                    assertEquals("Area attacks must use all living enemies", state.combatants.values.filter {
                                        it.combatant.side == com.example.starborn.domain.combat.CombatSide.ENEMY && it.isAlive
                                    }.map { it.combatant.id }.toSet(), queued.targetIds.toSet())
                                }
                            } else combat.useSkill(skill, listOf(target))
                        } else combat.playerAttack(target)
                    }
                }
                val state = requireNotNull(combat.combatState)
                if (retreat) {
                    assertEquals("Optional encounter must retreat through public command", com.example.starborn.domain.combat.CombatOutcome.Retreat, state.outcome)
                    explorationVm.onCombatRetreat(combat.encounterEnemyIds)
                    settle()
                    assertTrue("Retreat must preserve the undefeated enemy", enemyId in explorationVm.uiState.value.enemies)
                    publicCombats += "room=$room enemy=$enemyId seed=17 outcome=retreat; enemy remains undefeated"
                    return
                }
                val victory = state.outcome as? com.example.starborn.domain.combat.CombatOutcome.Victory
                    ?: error("Public combat failed in $room: ${state.outcome}; log=${state.log.takeLast(8)}")
                assertTrue("Victory must include real damage", state.log.any { it is com.example.starborn.domain.combat.CombatLogEntry.Damage && it.amount > 0 })
                val reward = victory.rewards
                val usedItems = state.log.filterIsInstance<com.example.starborn.domain.combat.CombatLogEntry.ActionQueued>()
                    .mapNotNull { (it.action as? com.example.starborn.domain.combat.CombatAction.ItemUse)?.itemId }
                    .groupingBy { it }.eachCount()
                val knockedOut = state.combatants.values.filter { it.combatant.side == com.example.starborn.domain.combat.CombatSide.PLAYER && !it.isAlive }
                    .map { it.combatant.id }
                publicCombats += "room=$room enemies=$encounterIds seed=17 damageEvents=${state.log.count { it is com.example.starborn.domain.combat.CombatLogEntry.Damage }} xp=${reward.xp} ap=${reward.ap} credits=${reward.credits} persistedHp=${sessionStore.state.value.partyMemberHp} knockedOut=$knockedOut usedItems=$usedItems"
                explorationVm.onCombatVictory(CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.VICTORY,
                    enemyIds = combat.encounterEnemyIds,
                    roomId = combat.encounterRoomId,
                    rewardXp = reward.xp, rewardAp = reward.ap, rewardCredits = reward.credits,
                    rewardItems = reward.drops.associate { it.itemId to it.quantity }
                ))
                settle()
            } finally { combat.close() }
        }

        private fun conditionEvaluator(raw: String?, state: GameSessionState): Boolean {
            if (raw.isNullOrBlank()) return true
            return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.all { token ->
                val parts = token.split(':', limit = 2)
                val type = parts[0].trim().lowercase(Locale.getDefault())
                val value = parts.getOrNull(1)?.trim().orEmpty()
                when (type) {
                    "room" -> state.roomId?.equals(value, ignoreCase = true) == true
                    "room_not" -> state.roomId?.equals(value, ignoreCase = true) != true
                    "milestone", "milestone_set" -> value in state.completedMilestones
                    "milestone_not_set" -> value !in state.completedMilestones
                    "quest", "quest_active" -> value in state.activeQuests
                    "quest_not_started" -> value.isNotBlank() &&
                        value !in state.activeQuests &&
                        value !in state.completedQuests &&
                        value !in state.failedQuests
                    "quest_completed" -> value in state.completedQuests
                    "quest_not_completed" -> value !in state.completedQuests
                    "quest_stage", "quest_stage_not" -> {
                        val p = value.split(':', limit = 2)
                        val matches = p.size == 2 && state.questStageById[p[0]] == p[1]
                        if (type == "quest_stage") matches else !matches
                    }
                    "quest_task_done" -> {
                        val p = value.split(':', limit = 2)
                        p.size == 2 && state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                    }
                    "quest_task_not_done" -> {
                        val p = value.split(':', limit = 2)
                        p.size == 2 && !state.questTasksCompleted[p[0]].orEmpty().contains(p[1])
                    }
                    "item" -> (state.inventory[value] ?: 0) > 0
                    "item_not" -> (state.inventory[value] ?: 0) <= 0
                    else -> error("Unsupported dialogue condition in campaign test: " + token)
                }
            }
        }
    }
}
