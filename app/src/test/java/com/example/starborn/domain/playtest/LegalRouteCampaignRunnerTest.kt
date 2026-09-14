package com.example.starborn.domain.playtest

import com.example.starborn.core.DispatcherProvider
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
import com.example.starborn.domain.combat.CombatReward
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
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
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
 * Production-runtime progression coverage, not a strict UI-accessibility proof.
 *
 * The runner uses authored routes when available, but may seed room position, invoke a
 * production action trigger reflectively, and synthesize combat victories. It proves that
 * the campaign's runtime events can progress from a fresh state through the finale and
 * survive save/reload checkpoints; device playtesting must still prove player-reachable paths.
 */
class LegalRouteCampaignRunnerTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: CoroutineScope
    private lateinit var saveDir: File

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
    fun `fresh game legal route traverses Worlds 1 through 6 to ms_game_complete with save reload checkpoints`() {
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

        } finally {
            harness.close()
        }
    }

    private inner class LegalCampaignAgent(private var harness: ProductionCampaignHarness) {

        fun attach(newHarness: ProductionCampaignHarness) {
            this.harness = newHarness
        }

        fun travelDirection(direction: String) {
            val beforeRoom = harness.explorationVm.uiState.value.currentRoom?.id
            harness.explorationVm.travel(direction)
            harness.settle()
            val afterRoom = harness.explorationVm.uiState.value.currentRoom?.id
            check(afterRoom != null && afterRoom != beforeRoom) {
                "travel($direction) failed from room '$beforeRoom': blocked or no connection."
            }
        }

        fun navigateLegalPath(targetRoomId: String) {
            var currentRoomId = harness.explorationVm.uiState.value.currentRoom?.id
                ?: harness.sessionStore.state.value.roomId
                ?: error("No current room")
            if (currentRoomId == targetRoomId) return

            val path = harness.findBfsPath(currentRoomId, targetRoomId)
            if (path != null) {
                for ((dir, _) in path) {
                    travelDirection(dir)
                }
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

            harness.sessionStore.setRoom(targetRoomId)
            harness.settle()
        }

        fun performAction(actionId: String, payload: String? = null) {
            val matchingAction = harness.explorationVm.uiState.value.actions.firstOrNull { action ->
                action.actionKey().contains(actionId, ignoreCase = true) ||
                    (action is com.example.starborn.domain.model.GenericAction && action.actionEvent == actionId) ||
                    (action is com.example.starborn.domain.model.ToggleAction &&
                        (action.actionEventOn == actionId || action.actionEventOff == actionId))
            }
            if (matchingAction != null) {
                harness.explorationVm.onActionSelected(matchingAction)
            } else {
                val triggerMethod = harness.explorationVm.javaClass.getDeclaredMethod(
                    "triggerPlayerAction",
                    String::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                triggerMethod.invoke(harness.explorationVm, actionId, payload)
            }
            while (harness.cinematicCoordinator.state.value != null) {
                harness.explorationVm.skipCinematic()
                harness.settle()
            }
            harness.settle()
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
            val outcome = harness.craftingService.craftTinkering(recipeId)
            assertTrue("Crafting $recipeId must succeed", outcome is CraftingOutcome.Success)
            harness.explorationVm.onTinkeringCrafted(resultItemId)
            harness.explorationVm.onTinkeringClosed()
            harness.settle()
        }

        fun winCombat(enemyIds: List<String>, roomId: String) {
            val reward = harness.applyScriptedBattleReward(enemyIds, roomId)
            val payload = CombatResultPayload(
                outcome = CombatResultPayload.Outcome.VICTORY,
                enemyIds = enemyIds,
                roomId = roomId,
                rewardXp = reward.xp,
                rewardAp = reward.ap,
                rewardCredits = reward.credits,
                rewardItems = reward.drops.associate { it.itemId to it.quantity }
            )
            harness.explorationVm.onCombatVictoryEnemiesCleared(enemyIds)
            harness.explorationVm.onCombatVictory(payload)
            harness.settle()
        }

        fun playWorld1() {
            assertEquals("pit_nova_bunk", harness.sessionStore.state.value.roomId)
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w1_mq01"))

            // Wake up and isolate safety fault to unlock bunk exit
            performAction("w1_mq01_turn_on_bunk_light")
            performAction("w1_mq01_inspect_safety_fault")

            // Navigate legal path to Jed's bunk
            navigateLegalPath("pit_jed_bunk")
            talkToNpc("Jed")
            performAction("w1_mq01_receive_starter_kit")
            assertTrue("Cryo inductor received from Jed", (harness.sessionStore.state.value.inventory["cryo_inductor"] ?: 0) >= 1)

            // Navigate to workshop yard and encounter faulted loader
            navigateLegalPath("workshop_yard")
            performAction("w1_mq01_inspect_loader_relay")
            winCombat(listOf("faulted_loader"), "workshop_yard")

            // Enter workshop floor and craft the functional cryo-inductor via production CraftingService
            navigateLegalPath("workshop_floor")
            craftItem("repair_cryo_inductor", "functional_cryo_inductor")
            performAction("w1_mq01_patch_flux_liner")
            performAction("w1_mq01_confirm_governor")
            performAction("w1_mq01_cutter_surge")
            assertTrue("w1_mq01 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq01"))
            assertTrue("w1_mq02 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq02"))

            // Navigate to Checkpoint Queue
            navigateLegalPath("checkpoint_queue")
            talkToNpc("Guard Hank")

            navigateLegalPath("checkpoint_booth")
            talkToNpc("Zeke", choiceId = "zeke_w1_mq02_choose_grid_instability")
            assertTrue("w1_mq02 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq02"))
            assertTrue("w1_mq03 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq03"))
            assertTrue("Mine access badge acquired", (harness.sessionStore.state.value.inventory["mine_access_badge"] ?: 0) >= 1)

            // Navigate through checkpoint gate to Admin Lobby
            navigateLegalPath("admin_lobby")
            talkToNpc("Foreman Boggs")
            performAction("w1_sq03_start_loader")
            performAction("w1_sq03_move_cargo")
            winCombat(listOf("acoustic_bulwark"), "workshop_dock")
            talkToNpc("Foreman Boggs")

            // Navigate into deep mine and restore auxiliary power at Cavern Junction
            navigateLegalPath("mine_landing")
            winCombat(listOf("echo_borer"), "mine_landing")
            navigateLegalPath("mine_junction")
            performAction("evt_mine_power_on")

            // Traverse powered mine rooms to Echo Gap and claim Relic
            navigateLegalPath("echo_gap")
            performAction("w1_mq03_touch_relic")
            assertTrue("w1_mq03 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq03"))
            assertTrue("w1_mq04 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq04"))

            // Escape lockdown to launch lift
            navigateLegalPath("launch_lift")
            winCombat(listOf("acoustic_bulwark"), "launch_lift")
            talkToNpc("Jed")
            assertTrue("w1_mq04 completed", harness.sessionStore.state.value.completedQuests.contains("w1_mq04"))
            assertTrue("w1_mq05 active", harness.sessionStore.state.value.activeQuests.contains("w1_mq05"))

            // Launch Bay confrontation
            winCombat(listOf("resonance_buoy"), "launch_bay")
            navigateLegalPath("launch_bay")
            talkToNpc("The Warden")
            winCombat(listOf("the_iron_warden"), "launch_bay")
            talkToNpc("Zeke")
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
            performAction("w2_mq01_stabilize_zeke")
            navigateLegalPath("sector9_landing_stream")
            assertTrue("w2_mq01 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq01"))

            // MQ02: The Signal
            talkToNpc("Zeke")
            navigateLegalPath("sector9_canopy")
            winCombat(listOf("echo_borer"), "sector9_canopy")
            navigateLegalPath("sector9_temple_gate")
            performAction("w2_mq02_use_chime")
            assertTrue("w2_mq02 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq02"))
            assertTrue("w2_mq03 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq03"))

            // MQ03: Sleeping Giant (Orion joins)
            performAction("w2_mq03_inspect_murals")
            navigateLegalPath("sector9_stasis_chamber")
            performAction("w2_mq03_inspect_pod")
            performAction("w2_mq03_read_mural_overview")
            performAction("w2_mq03_stabilize_coolant")
            performAction("w2_mq03_align_complete")
            assertTrue("w2_mq03 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq03"))
            assertTrue("Orion joins party", harness.sessionStore.state.value.partyMembers.contains("orion"))
            assertTrue("w2_mq04 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq04"))

            // MQ04: The Hunter (Source Beast & Gh0st joins)
            navigateLegalPath("sector9_canopy_ridge")
            performAction("w2_mq04_confront")
            winCombat(listOf("the_beast"), "sector9_canopy_ridge")
            performAction("w2_mq04_anchor_drill")
            assertTrue("w2_mq04 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq04"))
            assertTrue("Gh0st joins party", harness.sessionStore.state.value.partyMembers.contains("gh0st"))
            assertTrue("w2_mq05 active", harness.sessionStore.state.value.activeQuests.contains("w2_mq05"))

            // MQ05: Liftoff to Spire
            navigateLegalPath("sector9_source_gate")
            performAction("w2_mq05_stabilize_horn")
            performAction("w2_mq05_ground_cup")
            performAction("w2_mq05_read_pressure_gauge")
            performAction("w2_mq05_overload_breakers")
            performAction("w2_mq05_bypass_gate")
            performAction("w2_mq05_inspect_astra")
            performAction("w2_mq05_collect_conduits")
            performAction("w2_mq05_reboot")
            performAction("w2_mq05_launch")
            assertTrue("w2_mq05 completed", harness.sessionStore.state.value.completedQuests.contains("w2_mq05"))
            assertEquals("spire_sewers_landing", harness.sessionStore.state.value.roomId)
        }

        fun playWorld3() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w3_mq11"))
            winCombat(listOf("sewer_crawler"), "spire_sewers_landing")
            navigateLegalPath("spire_vent_output")
            navigateLegalPath("spire_zekes_apartment")
            talkToNpc("Zeke")
            assertTrue("w3_mq11 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq11"))
            assertTrue("w3_mq12 active", harness.sessionStore.state.value.activeQuests.contains("w3_mq12"))

            // MQ12: Heist Prep
            performAction("w3_mq12_talk_jax")
            performAction("w3_mq12_map_patrols")
            performAction("w3_mq12_interrogate_guard")
            performAction("w3_mq12_copy_badges")
            performAction("w3_mq12_source_disguises")
            performAction("w3_mq12_hack_blueprints")
            performAction("w3_mq12_assemble_planning")
            assertTrue("w3_mq12 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq12"))

            // Upper City Infiltration
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w3_mq13"))
            performAction("w3_mq13_blend_in")
            performAction("w3_mq13_disable_sensors")
            performAction("w3_mq13_enter_lobby")
            assertTrue("w3_mq13 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq13"))
            assertTrue("w3_mq14 active", harness.sessionStore.state.value.activeQuests.contains("w3_mq14"))

            navigateLegalPath("spire_archive_vault")
            performAction("w3_mq14_read_containment_field")
            performAction("w3_mq14_read_prism_shutters")
            performAction("w3_mq14_trace_command_tethers")
            performAction("w3_mq14_solve_light_puzzle")
            talkToNpc("Zeke")
            assertTrue("w3_mq14 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq14"))
            assertTrue("The Lens acquired", (harness.sessionStore.state.value.inventory["the_lens"] ?: 0) >= 1)

            navigateLegalPath("spire_landing_pad_roof")
            winCombat(listOf("aero_drone", "heavy_mech"), "spire_landing_pad_roof")
            winCombat(listOf("administrator_boss"), "spire_landing_pad_roof")
            performAction("w3_scan_shield_gap")
            performAction("w3_mq15_launch_astra")
            assertTrue("w3_mq15 completed", harness.sessionStore.state.value.completedQuests.contains("w3_mq15"))
            assertEquals("foundry_slag_landing", harness.sessionStore.state.value.roomId)
        }

        fun playWorld4() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w4_mq16"))
            listOf("land_shelf", "cross_slag_river", "hack_cooling_vents", "enter_airlock")
                .forEach { performAction("w4_mq16_$it") }
            listOf("access_phantom_terminal", "regroup_springs")
                .forEach { performAction("w4_mq17_$it") }
            listOf("navigate_conveyors", "defeat_prototypes", "overload_matrix")
                .forEach { performAction("w4_mq18_$it") }

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w4_mq19"))
            navigateLegalPath("foundry_forge_anvil")
            performAction("w4_mq19_read_pulse_board")
            performAction("w4_mq19_read_grease_marks")
            performAction("w4_mq19_throw_override_paddle")
            performAction("w4_mq19_starve_breath_pistons")
            performAction("w4_mq19_open_anvil_cradle")
            talkToNpc("Nova")
            assertTrue("w4_mq19 completed", harness.sessionStore.state.value.completedQuests.contains("w4_mq19"))
            assertTrue("The Anvil acquired", (harness.sessionStore.state.value.inventory["the_anvil"] ?: 0) >= 1)
            assertTrue("w4_mq20 active", harness.sessionStore.state.value.activeQuests.contains("w4_mq20"))

            // MQ20: Confront Rylos & Titan Walker Boss
            talkToNpc("Rylos")
            winCombat(listOf("titan_walker_boss"), "foundry_forge_anvil")
            performAction("w4_mq20_steal_engine")
            performAction("w4_mq20_steal_arrays")
            performAction("w4_mq20_escape_launch")
            assertTrue("w4_mq20 completed", harness.sessionStore.state.value.completedQuests.contains("w4_mq20"))
            assertTrue("ms_w5_access_unlocked set", harness.sessionStore.state.value.completedMilestones.contains("ms_w5_access_unlocked"))
            assertEquals("orbital_executive_dock", harness.sessionStore.state.value.roomId)
        }

        fun playWorld5() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w5_mq21"))
            winCombat(listOf("orbital_fighter"), "orbital_executive_dock")
            performAction("w5_mq21_force_dock")
            performAction("w5_mq21_hack_airlock")
            performAction("w5_mq22_cross_solarium")
            navigateLegalPath("orbital_grand_concourse")
            winCombat(listOf("compliance_officer", "null_g_drone"), "orbital_grand_concourse")
            listOf("traverse_shaft", "access_mainframe", "find_thorne")
                .forEach { performAction("w5_mq22_$it") }

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w5_mq23"))
            performAction("w5_mq23_navigate_maze")
            performAction("w5_mq23_firewall_alpha")
            performAction("w5_mq23_firewall_beta")
            performAction("w5_mq23_firewall_gamma")
            winCombat(listOf("firewall_construct"), "deep_firewall_gamma")
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
            winCombat(listOf("compliance_avatar"), "deep_throne_room")
            performAction("w5_mq25_enter_tear")
            assertTrue("w5_mq25 completed", harness.sessionStore.state.value.completedQuests.contains("w5_mq25"))
            assertTrue("ms_w6_access_unlocked set", harness.sessionStore.state.value.completedMilestones.contains("ms_w6_access_unlocked"))
            assertEquals("source_campfire", harness.sessionStore.state.value.roomId)
        }

        fun playWorld6() {
            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w6_mq26"))
            winCombat(listOf("manager_projection"), "source_zeke_review_loop")
            winCombat(listOf("endless_war_echo"), "source_gh0st_kill_suite")
            winCombat(listOf("silent_shore_wraith"), "source_orion_tide_well")
            performAction("w6_mq26_reassemble")

            navigateLegalPath("source_echo_mines")
            performAction("w6_mq27_evade_manager")

            navigateLegalPath("source_echo_elevator")
            listOf("anchor_zeke", "anchor_ghost", "anchor_orion", "anchor_nova", "build_bridge", "final_banter", "reach_singularity")
                .forEach { performAction("w6_mq28_$it") }

            assertTrue(harness.sessionStore.state.value.activeQuests.contains("w6_mq29"))
            performAction("w6_mq29_refuse_jed_revision")
            performAction("w6_mq29_refuse_astra_revision")
            performAction("w6_mq29_refuse_foundry_revision")
            performAction("w6_mq29_climb_stair")
            winCombat(listOf("source_shadow", "distorted_sentinel", "glitch_hound"), "source_memory_stair")
            winCombat(listOf("memory_leak", "nightmare_guard"), "source_memory_stair")

            navigateLegalPath("source_center")
            assertTrue("w6_mq29 completed", harness.sessionStore.state.value.completedQuests.contains("w6_mq29"))
            assertTrue("w6_mq30 active", harness.sessionStore.state.value.activeQuests.contains("w6_mq30"))

            // Finale: Confront Vale, defeat Ascended Vale & Ascended God
            performAction("w6_mq30_confront_vale")
            winCombat(listOf("ascended_vale"), "source_center")
            winCombat(listOf("ascended_god"), "source_center")
            performAction("w6_mq30_tune_world")

            assertTrue("w6_mq30 completed", harness.sessionStore.state.value.completedQuests.contains("w6_mq30"))
        }
    }

    private inner class ProductionCampaignHarness(
        private val saveFolder: File,
        private val dispatcher: TestDispatcher,
        private val coroutineScope: CoroutineScope,
        initialState: GameSessionState? = null
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
            restore(initialState ?: GameSessionState(
                worldId = "world_1",
                hubId = "hub_1_homestead",
                roomId = "pit_nova_bunk",
                playerId = "nova",
                partyMembers = listOf("nova"),
                unlockedSkills = setOf("nova_arc_tether"),
                activeQuests = setOf("w1_mq01"),
                trackedQuestId = "w1_mq01",
                questStageById = mapOf("w1_mq01" to "wake_in_the_pit"),
                inventory = mapOf("cryo_inductor" to 1, "scrap_metal" to 2)
            ))
        }

        val inventoryService = InventoryService(itemRepository).apply {
            loadItems()
            restore(sessionStore.state.value.inventory)
        }
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
        val fishingService = mock<FishingService>()
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

        fun settle() = dispatcher.scheduler.runCurrent()
        fun close() = Unit

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

        fun applyScriptedBattleReward(ids: List<String>, room: String): CombatReward {
            sessionStore.setRoom(room)
            val registry = StatusRegistry(worldDataSource.loadStatuses())
            val themes = mock<ThemeRepository>()
            val vm = CombatViewModel(
                worldAssets = worldDataSource,
                combatEngine = CombatEngine(statusRegistry = registry),
                statusRegistry = registry,
                sessionStore = sessionStore,
                inventoryService = inventoryService,
                itemCatalog = itemRepository,
                levelingManager = levelingManager,
                progressionData = progressionData,
                audioRouter = AudioRouter(AudioBindings()),
                themeRepository = themes,
                environmentThemeManager = EnvironmentThemeManager(themes),
                encounterCoordinator = EncounterCoordinator(),
                enemyIds = ids,
                tutorialsEnabled = false,
                elapsedRealtime = { 0L },
                random = SeededCombatRandom(17)
            )
            try {
                val generate = vm.javaClass.getDeclaredMethod("victoryReward").apply { isAccessible = true }
                val reward = generate.invoke(vm) as CombatReward
                vm.javaClass.getDeclaredMethod("applyVictoryRewards", reward.javaClass).apply { isAccessible = true }.invoke(vm, reward)
                return reward
            } finally {
                vm.cancelAllJobs()
            }
        }

        private fun CombatViewModel.cancelAllJobs() {
            try {
                val scope = this.javaClass.getMethod("getViewModelScope").invoke(this) as CoroutineScope
                scope.cancel()
            } catch (e: Exception) { }
        }

        private fun conditionEvaluator(raw: String?, state: GameSessionState): Boolean {
            if (raw.isNullOrBlank()) return true
            return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.all { token ->
                val parts = token.split(':', limit = 2)
                val type = parts[0].trim().lowercase(Locale.getDefault())
                val value = parts.getOrNull(1)?.trim().orEmpty()
                when (type) {
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
