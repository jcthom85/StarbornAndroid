package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.DialogueConditionEvaluator
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerHandler
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class CampaignEventIntegrationTest {

    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val moshi = MoshiProvider.instance

    private fun Int?.orZero(): Int = this ?: 0

    @Test fun `scripted campaign includes production rewards and saves a pre Titan checkpoint`() {
        val combatDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(combatDispatcher)
        var harness = PlaytestHarness()
        val rewards = mutableListOf<com.example.starborn.domain.combat.CombatReward>()
        try {
            for (play in listOf<(PlaytestHarness) -> Unit>(::playWorld1, ::playWorld2, ::playWorld3)) {
                harness.onBattleVictory = { ids, room -> rewards += applyScriptedBattleReward(harness.store, ids, room) }
                play(harness)
                val saved = harness.roundTripSave(harness.store.state.value)
                assertEquals(harness.store.state.value, saved)
                harness.close()
                harness = PlaytestHarness(saved)
                harness.assertJournalRestored()
            }
            val checkpoint = harness.store.state.value
            assertEquals(5105 + rewards.sumOf { it.xp }, checkpoint.playerXp)
            assertEquals(rewards.sumOf { it.ap }, checkpoint.playerAp)
            assertEquals(rewards.sumOf { it.credits }, checkpoint.playerCredits)
            assertEquals(2, checkpoint.inventory["nova_flux_liner"])
            assertTrue("Battle rewards must contribute to progression", rewards.sumOf { it.xp } > 0)
            println("BATTLE_EARNED_CHECKPOINT xp=${checkpoint.playerXp} level=${checkpoint.playerLevel} ap=${checkpoint.playerAp} credits=${checkpoint.playerCredits} inventory=${checkpoint.inventory.toSortedMap()}")
            var beforeTitan: GameSessionState? = null
            harness.onBattleVictory = { ids, room ->
                if ("titan_walker_boss" in ids) {
                    beforeTitan = harness.roundTripSave(harness.store.state.value)
                    assertEquals(harness.store.state.value, beforeTitan)
                }
                rewards += applyScriptedBattleReward(harness.store, ids, room)
            }
            playWorld4(harness)
            val bossCheckpoint = requireNotNull(beforeTitan)
            assertEquals(10450, bossCheckpoint.playerXp)
            assertEquals(8, bossCheckpoint.playerLevel)
            assertTrue("Level-nine offense must not be invented", "zeke_overload_fists" !in bossCheckpoint.unlockedSkills)
            assertTrue(bossCheckpoint.activeQuests.contains("w4_mq20"))
            assertTrue(!bossCheckpoint.completedQuests.contains("w4_mq20"))
            assertEquals("Titan AP must not be paid before the fight", checkpoint.playerAp, bossCheckpoint.playerAp)
            val world = com.example.starborn.data.assets.WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi))
            val progression = requireNotNull(world.loadProgressionData())
            bossCheckpoint.partyMembers.forEach { id ->
                val level = requireNotNull(bossCheckpoint.partyMemberLevels[id])
                progression.levelUpSkills[id].orEmpty().forEach { (threshold, skillId) ->
                    if (threshold.toInt() <= level) assertTrue("Missing earned $skillId", skillId in bossCheckpoint.unlockedSkills)
                }
            }
            println("PRE_TITAN_CHECKPOINT xp=${bossCheckpoint.playerXp} level=${bossCheckpoint.playerLevel} ap=${bossCheckpoint.playerAp} credits=${bossCheckpoint.playerCredits} skills=${bossCheckpoint.unlockedSkills.sorted()}")
            val combatHarness = com.example.starborn.feature.combat.OpeningCombatRuntimeTest()
            combatHarness.setUp()
            try {
                combatHarness.measureEarnedTitanCheckpoint(bossCheckpoint).forEach(::println)
            } finally {
                combatHarness.tearDown()
                Dispatchers.setMain(combatDispatcher)
            }
            var beforeAvatar: GameSessionState? = null
            harness.onBattleVictory = { ids, room ->
                if ("compliance_avatar" in ids) {
                    beforeAvatar = harness.roundTripSave(harness.store.state.value)
                    assertEquals(harness.store.state.value, beforeAvatar)
                }
                rewards += applyScriptedBattleReward(harness.store, ids, room)
            }
            playWorld5(harness)
            val avatarCheckpoint = requireNotNull(beforeAvatar)
            assertEquals("Avatar's four AP must not enter its own checkpoint", 5, avatarCheckpoint.playerAp)
            println("PRE_AVATAR_CHECKPOINT xp=${avatarCheckpoint.playerXp} level=${avatarCheckpoint.playerLevel} ap=${avatarCheckpoint.playerAp} credits=${avatarCheckpoint.playerCredits}")
            val laterCombatHarness = com.example.starborn.feature.combat.OpeningCombatRuntimeTest()
            laterCombatHarness.setUp()
            try {
                laterCombatHarness.measureEarnedBossCheckpoint(avatarCheckpoint, "compliance_avatar").forEach(::println)
            } finally {
                laterCombatHarness.tearDown()
                Dispatchers.setMain(combatDispatcher)
            }
            var beforeFinal: GameSessionState? = null
            harness.onBattleVictory = { ids, room ->
                if ("ascended_vale" in ids) {
                    beforeFinal = harness.roundTripSave(harness.store.state.value)
                    assertEquals(harness.store.state.value, beforeFinal)
                }
                rewards += applyScriptedBattleReward(harness.store, ids, room)
            }
            playWorld6(harness)
            val finalCheckpoint = requireNotNull(beforeFinal)
            assertTrue("Final reward must not precede its battle", "source_art_tune_world" !in finalCheckpoint.unlockedSkills)
            println("PRE_FINAL_CHECKPOINT xp=${finalCheckpoint.playerXp} level=${finalCheckpoint.playerLevel} ap=${finalCheckpoint.playerAp} credits=${finalCheckpoint.playerCredits}")
            val finalCombatHarness = com.example.starborn.feature.combat.OpeningCombatRuntimeTest()
            finalCombatHarness.setUp()
            try { finalCombatHarness.measureFinalCheckpoint(finalCheckpoint).forEach(::println) }
            finally { finalCombatHarness.tearDown(); Dispatchers.setMain(combatDispatcher) }
        } finally { harness.close(); Dispatchers.resetMain() }
    }

    private fun applyScriptedBattleReward(store: GameSessionStore, ids: List<String>, room: String): com.example.starborn.domain.combat.CombatReward {
        val reader = AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        val world = com.example.starborn.data.assets.WorldAssetDataSource(reader)
        val catalog = com.example.starborn.data.repository.ItemRepository(com.example.starborn.data.assets.ItemAssetDataSource(reader)).apply { load() }
        val registry = com.example.starborn.domain.combat.StatusRegistry(world.loadStatuses())
        val themes = mock<com.example.starborn.data.repository.ThemeRepository>()
        store.setRoom(room)
        val vm = com.example.starborn.feature.combat.viewmodel.CombatViewModel(
            worldAssets = world, combatEngine = com.example.starborn.domain.combat.CombatEngine(statusRegistry = registry),
            statusRegistry = registry, sessionStore = store,
            inventoryService = com.example.starborn.domain.inventory.InventoryService(catalog).apply { loadItems(); restore(store.state.value.inventory) },
            itemCatalog = catalog, levelingManager = com.example.starborn.domain.leveling.LevelingManager(requireNotNull(world.loadLevelingData())),
            progressionData = requireNotNull(world.loadProgressionData()),
            audioRouter = com.example.starborn.domain.audio.AudioRouter(com.example.starborn.domain.audio.AudioBindings()),
            themeRepository = themes, environmentThemeManager = com.example.starborn.domain.theme.EnvironmentThemeManager(themes),
            encounterCoordinator = com.example.starborn.domain.combat.EncounterCoordinator(), enemyIds = ids,
            tutorialsEnabled = false, elapsedRealtime = { 0L }, random = com.example.starborn.domain.combat.SeededCombatRandom(17))
        try {
            // Outcome supplied by the route; execute production generation/payment,
            // not an invented reward formula or a claim that combat was simulated.
            val generate = vm.javaClass.getDeclaredMethod("victoryReward").apply { isAccessible = true }
            val reward = generate.invoke(vm) as com.example.starborn.domain.combat.CombatReward
            vm.javaClass.getDeclaredMethod("applyVictoryRewards", reward.javaClass).apply { isAccessible = true }.invoke(vm, reward)
            return reward
        } finally { vm.viewModelScope.cancel() }
    }

    @Test
    fun `all thirty main quests complete through continuous events with save resume between worlds`() {
        var harness = PlaytestHarness()
        val worlds = listOf<(PlaytestHarness) -> Unit>(::playWorld1, ::playWorld2, ::playWorld3, ::playWorld4, ::playWorld5, ::playWorld6)
        val mainQuests = readList<com.example.starborn.domain.model.Quest>("quests.json").filter { "_mq" in it.id }
        val checkpoints = mutableListOf<GameSessionState>()
        try {
            worlds.forEachIndexed { index, play ->
                val xpBefore = harness.store.state.value.playerXp
                play(harness)
                val state = harness.store.state.value
                val expected = mainQuests.filter { it.id.startsWith("w${index + 1}_") }.map { it.id }.toSet()
                assertTrue("World ${index + 1} skipped quests: ${expected - state.completedQuests}", state.completedQuests.containsAll(expected))
                if (index > 0) {
                    val advertisedXp = mainQuests.filter { it.id in expected }
                        .flatMap { it.rewards }.filter { it.type == "xp" }.sumOf { it.amount ?: 0 }
                    assertEquals("World ${index + 1} must pay its advertised main-quest XP", advertisedXp, state.playerXp - xpBefore)
                }
                val restored = harness.roundTripSave(state)
                assertEquals("World ${index + 1} save must preserve the whole session", state, restored)
                checkpoints += restored
                harness.close()
                harness = PlaytestHarness(restored)
                assertEquals("Quest runtime hydration must preserve saved progression", restored, harness.store.state.value)
                harness.assertJournalRestored()
            }
            assertTrue(harness.store.state.value.completedMilestones.contains("ms_game_complete"))
            writeCheckpointReport(checkpoints)
        } finally {
            harness.close()
        }
    }

    private fun writeCheckpointReport(checkpoints: List<GameSessionState>) {
        val report = buildString {
            appendLine("# Campaign event checkpoint ledger")
            appendLine()
            appendLine("Generated by CampaignEventIntegrationTest using production EventManager, DialogueService and QuestRuntimeManager, with protobuf save/resume after each world.")
            appendLine()
            appendLine("XP and credits below are event payouts on the scripted route, not total player earnings. Lead-player levels and level skills use production ExplorationXpAwarder. Battle rewards, milestone effects, purchases, optional routes beyond the scripted steps and equipment selection are not simulated. Craft success and battle victories are supplied. Quest catalog reward labels are not added as a second payout.")
            appendLine()
            appendLine("| World completed | Event XP (cumulative) | Lead level | Credits (cumulative) | Party | Unlocked skills |")
            appendLine("|---|---:|---:|---:|---|---|")
            checkpoints.forEachIndexed { index, state ->
                appendLine("| ${index + 1} | ${state.playerXp} | ${state.playerLevel} | ${state.playerCredits} | ${state.partyMembers.joinToString()} | ${state.unlockedSkills.sorted().joinToString()} |")
            }
            appendLine()
            appendLine("## Saved event-earned inventory and AP")
            appendLine()
            appendLine("These are actual restored event-harness snapshots, not full player budgets. Battle AP/loot and crafting costs remain excluded; the opening cryo-inductor is explicitly supplied by the harness.")
            checkpoints.forEachIndexed { index, state ->
                appendLine()
                appendLine("### After World ${index + 1}")
                appendLine()
                appendLine("Shared event AP: ${state.playerAp}. Inventory: ${state.inventory.toSortedMap().entries.joinToString { (id, qty) -> "$id x$qty" }}.")
            }
        }
        File(root, "reports/campaign").apply { mkdirs() }
            .resolve("event-checkpoints.md").writeText(report)
    }

    @Test fun `event earned pre World four armor can be equipped and preserved without buying replacement`() {
        var harness = PlaytestHarness()
        try {
            for (play in listOf<(PlaytestHarness) -> Unit>(::playWorld1, ::playWorld2, ::playWorld3)) {
                play(harness)
                val restored = harness.roundTripSave(harness.store.state.value)
                // Starter-kit reward bundle and later patch action each grant one.
                assertEquals("Both authored opening armor grants survive each world boundary", 2, restored.inventory["nova_flux_liner"])
                harness.close()
                harness = PlaytestHarness(restored)
            }
            val earned = harness.store.state.value
            // This route excludes battle XP/AP/credits: do not silently replace it
            // with the level-nine, 1,070-credit constructed combat checkpoint.
            assertEquals(5105, earned.playerXp)
            assertEquals(7, earned.playerLevel)
            assertEquals(0, earned.playerCredits)
            assertEquals(0, earned.playerAp)
            val equipped = earned.copy(equippedArmors = earned.equippedArmors + ("nova" to "nova_flux_liner"))
            val restored = harness.roundTripSave(equipped)
            assertEquals(equipped, restored)
            assertEquals(earned.inventory, restored.inventory)
            assertEquals(earned.playerCredits, restored.playerCredits)
            assertEquals("nova_flux_liner", restored.equippedArmors["nova"])
        } finally { harness.close() }
    }

    // Event integration only: navigation, successful crafts, and encounter outcomes
    // are supplied inputs. This test does not prove UI reachability or combat balance.
    private fun playWorld1(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)

        // 1. Initial State Verification
        assertEquals("pit_nova_bunk", harness.store.state.value.roomId)
        assertTrue(harness.store.state.value.activeQuests.contains("w1_mq01"))

        // 2. Playtester Solves Chapter 1 (w1_mq01: Wake in the Pit)
        agent.executeAction("w1_mq01_turn_on_bunk_light")
        agent.executeAction("w1_mq01_inspect_safety_fault")
        agent.navigateTo("pit_jed_bunk")
        agent.talkTo("Jed")
        agent.navigateTo("workshop_yard")
        agent.executeAction("w1_mq01_inspect_loader_relay")
        agent.winEncounter(listOf("faulted_loader"), "workshop_yard")
        agent.navigateTo("workshop_floor")
        agent.executeAction("tinkering_screen_entered")
        agent.executeAction("tinkering_craft", "functional_cryo_inductor")
        harness.store.setInventory(harness.store.state.value.inventory + ("functional_cryo_inductor" to 1))
        agent.executeAction("w1_mq01_patch_flux_liner")
        agent.executeAction("w1_mq01_confirm_governor")
        agent.executeAction("w1_mq01_cutter_surge")

        var state = harness.store.state.value
        assertTrue("w1_mq01 should be completed", state.completedQuests.contains("w1_mq01"))
        assertTrue("w1_mq02 should be active", state.activeQuests.contains("w1_mq02"))

        // 3. Save / Load Integrity Check
        val saved = harness.roundTripSave(state)
        assertEquals(state.completedQuests, saved.completedQuests)
        assertEquals(state.inventory, saved.inventory)

        // 4. Playtester Solves Chapter 2 (w1_mq02: Checkpoint & Transit Override)
        agent.navigateTo("checkpoint_queue")
        agent.talkTo("Guard Hank")
        agent.navigateTo("checkpoint_booth")
        agent.talkToZekeWithChoice("zeke_w1_mq02_choose_grid_instability")

        state = harness.store.state.value
        assertTrue("w1_mq02 should be completed", state.completedQuests.contains("w1_mq02"))
        assertTrue("w1_mq03 should be active", state.activeQuests.contains("w1_mq03"))

        // 5. Playtester Solves Chapter 3 (w1_mq03: Heavy Lifting & The Echo Relic)
        agent.navigateTo("admin_lobby")
        agent.talkTo("Foreman Boggs")
        agent.executeAction("w1_sq03_start_loader")
        agent.executeAction("w1_sq03_move_cargo")
        agent.winEncounter(listOf("acoustic_bulwark"), "workshop_dock")
        agent.talkTo("Foreman Boggs")
        agent.navigateTo("mine_landing")
        agent.winEncounter(listOf("echo_borer"), "mine_landing")
        agent.navigateTo("echo_gap")
        agent.executeAction("w1_mq03_touch_relic")

        state = harness.store.state.value
        assertTrue("w1_mq03 should be completed", state.completedQuests.contains("w1_mq03"))
        assertTrue("w1_mq04 should be active", state.activeQuests.contains("w1_mq04"))

        // 6. Playtester Solves Chapter 4 (w1_mq04: Lockdown Escape & Jed's Sacrifice)
        agent.navigateTo("launch_lift")
        agent.winEncounter(listOf("acoustic_bulwark"), "launch_lift")
        agent.talkTo("Jed")

        state = harness.store.state.value
        assertTrue("w1_mq04 should be completed", state.completedQuests.contains("w1_mq04"))
        assertTrue("w1_mq05 should be active", state.activeQuests.contains("w1_mq05"))

        // 7. Playtester Solves Chapter 5 (w1_mq05: The Launch & Iron Warden Boss)
        agent.winEncounter(listOf("resonance_buoy"), "launch_bay")
        agent.navigateTo("launch_bay")
        agent.talkTo("The Warden")
        agent.winEncounter(listOf("the_iron_warden"), "launch_bay")
        agent.talkTo("Zeke")
        agent.talkTo("Zeke")
        agent.executeAction("use_nav_console")

        val finalState = harness.store.state.value
        assertTrue("w1_mq05 should be completed", finalState.completedQuests.contains("w1_mq05"))
        assertEquals("sector9_crash_site", finalState.roomId)
        assertTrue("World 2 should now begin", finalState.activeQuests.contains("w2_mq01"))
        assertTrue("Final save round-trip must succeed", harness.roundTripSave(finalState).completedQuests.contains("w1_mq05"))
    }

    private fun playWorld2(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)
        assertTrue(harness.store.state.value.activeQuests.contains("w2_mq01"))
        agent.talkTo("Zeke")
        agent.executeAction("w2_mq01_examine_pod")
        agent.executeAction("w2_mq01_stabilize_zeke")
        agent.navigateTo("sector9_landing_stream")
        assertTrue(harness.store.state.value.completedQuests.contains("w2_mq01"))

        // MQ02: The Signal
        agent.talkTo("Zeke")
        agent.navigateTo("sector9_canopy")
        agent.winEncounter(listOf("echo_borer"), "sector9_canopy")
        agent.navigateTo("sector9_temple_gate")
        agent.executeAction("w2_mq02_use_chime")

        var state = harness.store.state.value
        assertTrue("w2_mq02 should be completed", state.completedQuests.contains("w2_mq02"))
        assertTrue("w2_mq03 should be active", state.activeQuests.contains("w2_mq03"))

        // MQ03: Sleeping Giant (Orion joins)
        agent.executeAction("w2_mq03_inspect_murals")
        agent.navigateTo("sector9_stasis_chamber")
        agent.executeAction("w2_mq03_inspect_pod")
        agent.executeAction("w2_mq03_read_mural_overview")
        agent.executeAction("w2_mq03_stabilize_coolant")
        agent.executeAction("w2_mq03_align_complete")

        state = harness.store.state.value
        assertTrue("w2_mq03 should be completed", state.completedQuests.contains("w2_mq03"))
        assertTrue("Orion must join party", state.partyMembers.contains("orion"))
        assertTrue("w2_mq04 should be active", state.activeQuests.contains("w2_mq04"))

        // MQ04: The Hunter (Defeat Source Beast & Gh0st joins)
        agent.navigateTo("sector9_canopy_ridge")
        agent.executeAction("w2_mq04_confront")
        agent.winEncounter(listOf("the_beast"), "sector9_canopy_ridge")
        agent.executeAction("w2_mq04_anchor_drill")

        state = harness.store.state.value
        assertTrue("w2_mq04 should be completed", state.completedQuests.contains("w2_mq04"))
        assertTrue("Gh0st must join party", state.partyMembers.contains("gh0st"))
        assertTrue("w2_mq05 should be active", state.activeQuests.contains("w2_mq05"))

        // MQ05: Liftoff to Spire
        agent.navigateTo("sector9_source_gate")
        agent.executeAction("w2_mq05_stabilize_horn")
        agent.executeAction("w2_mq05_ground_cup")
        agent.executeAction("w2_mq05_read_pressure_gauge")
        agent.executeAction("w2_mq05_overload_breakers")
        agent.executeAction("w2_mq05_bypass_gate")
        agent.executeAction("w2_mq05_inspect_astra")
        agent.executeAction("w2_mq05_collect_conduits")
        agent.executeAction("w2_mq05_reboot")
        agent.executeAction("w2_mq05_launch")

        state = harness.store.state.value
        assertTrue("w2_mq05 should be completed", state.completedQuests.contains("w2_mq05"))
        assertTrue("World 3 Spire unlock milestone achieved", state.completedMilestones.contains("ms_w2_mq05_complete"))
        assertEquals("spire_sewers_landing", state.roomId)
    }

    private fun playWorld3(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)
        assertTrue(harness.store.state.value.activeQuests.contains("w3_mq11"))

        // MQ11: Clear Landing & Safehouse
        agent.winEncounter(listOf("sewer_crawler"), "spire_sewers_landing")
        agent.navigateTo("spire_vent_output")
        agent.navigateTo("spire_zekes_apartment")
        agent.talkTo("Zeke")

        var state = harness.store.state.value
        assertTrue("w3_mq11 should be completed", state.completedQuests.contains("w3_mq11"))
        assertTrue("w3_mq12 should be active", state.activeQuests.contains("w3_mq12"))

        // MQ12: Heist Prep
        agent.executeAction("w3_mq12_talk_jax")
        agent.executeAction("w3_mq12_map_patrols")
        agent.executeAction("w3_mq12_interrogate_guard")
        agent.executeAction("w3_mq12_copy_badges")
        agent.executeAction("w3_mq12_source_disguises")
        agent.executeAction("w3_mq12_hack_blueprints")
        agent.executeAction("w3_mq12_assemble_planning")

        state = harness.store.state.value
        assertTrue("w3_mq12 should be completed", state.completedQuests.contains("w3_mq12"))

        // Follow the actual MQ12 -> MQ13 -> MQ14 handoff.
        assertTrue(harness.store.state.value.activeQuests.contains("w3_mq13"))
        agent.executeAction("w3_mq13_blend_in")
        agent.executeAction("w3_mq13_disable_sensors")
        agent.executeAction("w3_mq13_enter_lobby")
        assertTrue(harness.store.state.value.completedQuests.contains("w3_mq13"))
        assertTrue(harness.store.state.value.activeQuests.contains("w3_mq14"))
        agent.navigateTo("spire_archive_vault")
        agent.executeAction("w3_mq14_read_containment_field")
        agent.executeAction("w3_mq14_read_prism_shutters")
        agent.executeAction("w3_mq14_trace_command_tethers")
        agent.executeAction("w3_mq14_solve_light_puzzle")
        agent.talkTo("Zeke")

        state = harness.store.state.value
        assertTrue("w3_mq14 should be completed", state.completedQuests.contains("w3_mq14"))
        assertTrue("The Lens relic acquired", state.inventory["the_lens"].orZero() >= 1)
        agent.navigateTo("spire_landing_pad_roof")
        agent.winEncounter(listOf("aero_drone", "heavy_mech"), "spire_landing_pad_roof")
        agent.winEncounter(listOf("administrator_boss"), "spire_landing_pad_roof")
        agent.executeAction("w3_scan_shield_gap")
        agent.executeAction("w3_mq15_launch_astra")
        assertTrue(harness.store.state.value.completedQuests.contains("w3_mq15"))
    }

    private fun playWorld4(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)
        assertTrue(harness.store.state.value.activeQuests.contains("w4_mq16"))
        listOf("land_shelf", "cross_slag_river", "hack_cooling_vents", "enter_airlock")
            .forEach { agent.executeAction("w4_mq16_$it") }
        listOf("access_phantom_terminal", "regroup_springs")
            .forEach { agent.executeAction("w4_mq17_$it") }
        listOf("navigate_conveyors", "defeat_prototypes", "overload_matrix")
            .forEach { agent.executeAction("w4_mq18_$it") }
        assertTrue(harness.store.state.value.activeQuests.contains("w4_mq19"))
        agent.navigateTo("foundry_forge_anvil")
        agent.executeAction("w4_mq19_read_pulse_board")
        agent.executeAction("w4_mq19_read_grease_marks")
        agent.executeAction("w4_mq19_throw_override_paddle")
        agent.executeAction("w4_mq19_starve_breath_pistons")
        agent.executeAction("w4_mq19_open_anvil_cradle")
        agent.talkTo("Nova")

        var state = harness.store.state.value
        assertTrue("w4_mq19 should be completed", state.completedQuests.contains("w4_mq19"))
        assertTrue("The Anvil relic acquired", state.inventory["the_anvil"].orZero() >= 1)
        assertTrue("w4_mq20 should be active", state.activeQuests.contains("w4_mq20"))

        // MQ20: Confront Rylos & Titan Walker Boss
        agent.talkTo("Rylos")
        agent.winEncounter(listOf("titan_walker_boss"), "foundry_forge_anvil")
        agent.executeAction("w4_mq20_steal_engine")
        agent.executeAction("w4_mq20_steal_arrays")
        agent.executeAction("w4_mq20_escape_launch")

        state = harness.store.state.value
        assertTrue("w4_mq20 should be completed", state.completedQuests.contains("w4_mq20"))
        assertTrue("World 5 Orbital Station access unlocked", state.completedMilestones.contains("ms_w5_access_unlocked"))
    }

    private fun playWorld5(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)
        assertTrue(harness.store.state.value.activeQuests.contains("w5_mq21"))
        agent.navigateTo("orbital_executive_dock")
        agent.winEncounter(listOf("orbital_fighter"), "orbital_executive_dock")
        agent.executeAction("w5_mq21_force_dock")
        agent.executeAction("w5_mq21_hack_airlock")
        agent.executeAction("w5_mq22_cross_solarium")
        agent.winEncounter(listOf("compliance_officer", "null_g_drone"), "orbital_grand_concourse")
        listOf("traverse_shaft", "access_mainframe", "find_thorne")
            .forEach { agent.executeAction("w5_mq22_$it") }
        assertTrue(harness.store.state.value.activeQuests.contains("w5_mq23"))
        agent.executeAction("w5_mq23_navigate_maze")
        agent.executeAction("w5_mq23_firewall_alpha")
        agent.executeAction("w5_mq23_firewall_beta")
        agent.executeAction("w5_mq23_firewall_gamma")
        agent.winEncounter(listOf("firewall_construct"), "deep_firewall_gamma")

        var state = harness.store.state.value
        assertTrue("w5_mq23 should be completed", state.completedQuests.contains("w5_mq23"))
        assertTrue("w5_mq24 should be active", state.activeQuests.contains("w5_mq24"))

        // MQ24 & MQ25: Anchor & Compliance Avatar Climax
        agent.navigateTo("deep_anchor_chamber")
        agent.executeAction("w5_mq24_find_elara")
        agent.executeAction("w5_mq24_take_anchor")

        state = harness.store.state.value
        assertTrue("w5_mq24 should be completed", state.completedQuests.contains("w5_mq24"))
        assertTrue("The Anchor relic acquired", state.inventory["anchor_relic"].orZero() >= 1)
        assertTrue("w5_mq25 should be active", state.activeQuests.contains("w5_mq25"))

        agent.navigateTo("deep_throne_room")
        agent.executeAction("w5_mq25_soloist")
        agent.winEncounter(listOf("compliance_avatar"), "deep_throne_room")
        agent.executeAction("w5_mq25_enter_tear")

        state = harness.store.state.value
        assertTrue("w5_mq25 should be completed", state.completedQuests.contains("w5_mq25"))
        assertTrue("World 6 Source access unlocked", state.completedMilestones.contains("ms_w6_access_unlocked"))
        assertEquals("source_campfire", state.roomId)
    }

    private fun playWorld6(harness: PlaytestHarness) {
        val agent = HeadlessPlaytesterAgent(harness)
        assertTrue(harness.store.state.value.activeQuests.contains("w6_mq26"))
        agent.winEncounter(listOf("manager_projection"), "source_zeke_review_loop")
        agent.winEncounter(listOf("endless_war_echo"), "source_gh0st_kill_suite")
        agent.winEncounter(listOf("silent_shore_wraith"), "source_orion_tide_well")
        agent.executeAction("w6_mq26_reassemble")
        agent.navigateTo("source_echo_mines")
        agent.executeAction("w6_mq27_evade_manager")
        agent.navigateTo("source_echo_elevator")
        listOf("anchor_zeke", "anchor_ghost", "anchor_orion", "anchor_nova", "build_bridge", "final_banter", "reach_singularity")
            .forEach { agent.executeAction("w6_mq28_$it") }
        assertTrue(harness.store.state.value.activeQuests.contains("w6_mq29"))

        agent.executeAction("w6_mq29_refuse_jed_revision")
        agent.executeAction("w6_mq29_refuse_astra_revision")
        agent.executeAction("w6_mq29_refuse_foundry_revision")
        agent.executeAction("w6_mq29_climb_stair")
        agent.winEncounter(listOf("source_shadow", "distorted_sentinel", "glitch_hound"), "source_memory_stair")
        agent.winEncounter(listOf("memory_leak", "nightmare_guard"), "source_memory_stair")

        agent.navigateTo("source_center")
        var state = harness.store.state.value
        assertTrue("w6_mq29 should be completed", state.completedQuests.contains("w6_mq29"))
        assertTrue("w6_mq30 should be active", state.activeQuests.contains("w6_mq30"))

        // MQ30: Final Boss - Ascended Vale & Ascended God
        agent.executeAction("w6_mq30_confront_vale")
        agent.winEncounter(listOf("ascended_vale"), "source_center")
        agent.winEncounter(listOf("ascended_god"), "source_center")
        agent.executeAction("w6_mq30_tune_world")

        state = harness.store.state.value
        assertTrue("w6_mq30 should be completed", state.completedQuests.contains("w6_mq30"))
        assertTrue("Game complete milestone reached", state.completedMilestones.contains("ms_game_complete"))
        assertTrue("Credits seen milestone reached", state.completedMilestones.contains("ms_credits_seen"))
        assertTrue("Final Source Art unlocked", state.unlockedSkills.contains("source_art_tune_world"))
        assertEquals("source_new_world", state.roomId)
    }

    @Test
    fun `room connection targets exist in the catalog`() {
        val rooms = readList<Room>("rooms.json")
        val roomById = rooms.associateBy { it.id }

        assertTrue("Game must contain authored rooms", rooms.size >= 400)
        rooms.forEach { room ->
            room.connections.values.forEach { targetRoomId ->
                assertTrue(
                    "Room '${room.id}' has connection pointing to non-existent target '$targetRoomId'",
                    roomById.containsKey(targetRoomId)
                )
            }
        }
    }

    private inner class HeadlessPlaytesterAgent(private val harness: PlaytestHarness) {
        fun executeAction(actionId: String, payload: String? = null) {
            val xpBefore = harness.store.state.value.playerXp
            harness.events.handleTrigger("player_action", EventPayload.Action(actionId, payload))
            harness.settle()
            val xpAfter = harness.store.state.value.playerXp
            if (xpAfter > xpBefore) {
                harness.events.handleTrigger("player_action", EventPayload.Action(actionId, payload))
                harness.settle()
                assertEquals("Replaying $actionId must not pay twice", xpAfter, harness.store.state.value.playerXp)
                val saved = harness.roundTripSave(harness.store.state.value)
                val resumed = PlaytestHarness(saved)
                try {
                    resumed.events.handleTrigger("player_action", EventPayload.Action(actionId, payload))
                    resumed.settle()
                    assertEquals("Replaying $actionId after loading must not pay twice", xpAfter, resumed.store.state.value.playerXp)
                } finally { resumed.close() }
            }
        }

        fun navigateTo(targetRoomId: String) {
            check(targetRoomId in harness.roomIds) { "Unknown room: $targetRoomId" }
            harness.store.setRoom(targetRoomId)
            harness.events.handleTrigger("enter_room", EventPayload.EnterRoom(targetRoomId))
            harness.settle()
        }

        fun talkTo(npcName: String) {
            val session = checkNotNull(harness.dialogue.startDialogue(npcName)) { "No eligible dialogue for $npcName" }
            var steps = 0
            while (!session.isFinished()) {
                check(++steps <= 100) { "Dialogue stalled: $npcName at ${session.current()?.id}" }
                check(session.choices().isEmpty()) { "Choose explicitly for $npcName: ${session.choices().map { it.id }}" }
                session.advance()
                harness.settle()
            }
        }

        fun talkToZekeWithChoice(choiceId: String) {
            val session = checkNotNull(harness.dialogue.startDialogue("Zeke")) { "No eligible Zeke dialogue" }
            var steps = 0
            var chosen = false
            while (!session.isFinished()) {
                check(++steps <= 100) { "Zeke dialogue stalled at ${session.current()?.id}" }
                if (session.choices().any { it.id == choiceId }) {
                    session.choose(choiceId)
                    chosen = true
                } else {
                    session.advance()
                }
                harness.settle()
            }
            check(chosen) { "Choice was never offered: $choiceId" }
        }

        fun winEncounter(enemyIds: List<String>, roomId: String) {
            check(roomId in harness.roomIds) { "Unknown encounter room: $roomId" }
            check(enemyIds.all { it in harness.enemyIds }) { "Unknown encounter enemies: $enemyIds" }
            harness.onBattleVictory?.invoke(enemyIds, roomId)
            harness.events.handleTrigger(
                "encounter_victory",
                EventPayload.EncounterOutcome(
                    enemyIds = enemyIds,
                    outcome = EventPayload.EncounterOutcome.Outcome.VICTORY,
                    roomId = roomId
                )
            )
            harness.settle()
        }
    }

    private inner class PlaytestHarness(initial: GameSessionState? = null) {
        var onBattleVictory: ((List<String>, String) -> Unit)? = null
        val roomIds = readList<Room>("rooms.json").map { it.id }.toSet()
        val enemyIds = readList<com.example.starborn.domain.model.Enemy>("enemies.json").map { it.id }.toSet()
        val store = GameSessionStore().apply {
            restore(
                initial ?: GameSessionState(
                    worldId = "world_1",
                    hubId = "hub_1_homestead",
                    roomId = "pit_nova_bunk",
                    playerId = "nova",
                    partyMembers = listOf("nova"),
                    unlockedSkills = setOf("nova_arc_tether"),
                    activeQuests = setOf("w1_mq01"),
                    trackedQuestId = "w1_mq01",
                    questStageById = mapOf("w1_mq01" to "wake_in_the_pit")
                )
            )
        }
        val events: EventManager
        val dialogue: DialogueService
        private val dispatcher = StandardTestDispatcher()
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private val quests = QuestRepository(QuestAssetDataSource(
            AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi)
        )).apply { load() }
        private val questRuntime = QuestRuntimeManager(quests, store, scope, UiEventBus())
        private val progressionAssets = com.example.starborn.data.assets.WorldAssetDataSource(
            AssetJsonReader(DesktopAssetProvider(listOf(assets)), moshi))
        private val leveling = com.example.starborn.domain.leveling.LevelingManager(
            requireNotNull(progressionAssets.loadLevelingData()))
        private val xpAwarder = com.example.starborn.domain.leveling.ExplorationXpAwarder(
            store, leveling, requireNotNull(progressionAssets.loadProgressionData()))
        fun settle() = dispatcher.scheduler.runCurrent()
        fun close() = scope.cancel()
        fun assertJournalRestored() {
            val session = store.state.value
            val journal = questRuntime.state.value
            assertEquals(session.completedQuests, journal.completedQuestIds)
            assertEquals(session.activeQuests, journal.activeQuestIds)
            assertEquals(session.questStageById, journal.stageProgress)
            assertEquals(session.completedQuests, journal.completedJournal.map { it.id }.toSet())
            assertEquals("Quest XP must update the lead level without a battle", leveling.levelForXp(session.playerXp), session.playerLevel)
            session.partyMembers.forEach { id ->
                assertEquals("$id must keep pace with the scripted party", session.playerXp, session.partyMemberXp[id])
                assertEquals("$id level must update after quest XP", session.playerLevel, session.partyMemberLevels[id])
            }
        }

        init {
            val rawEvents = readList<GameEvent>("events.json")
            val rawDialogue = readList<DialogueLine>("dialogue.json")

            events = EventManager(
                events = rawEvents,
                sessionStore = store,
                eventHooks = EventHooks(
                    onPlayCinematic = { _, done -> done() },
                    onQuestStarted = { qId -> if (!qId.isNullOrBlank()) questRuntime.recordQuestStarted(qId) },
                    onQuestCompleted = { qId ->
                        if (!qId.isNullOrBlank()) {
                            questRuntime.markQuestCompleted(qId)
                            questRuntime.recordQuestCompleted(qId)
                            events.handleTrigger("quest_stage_complete", EventPayload.QuestStage(qId))
                        }
                    },
                    onMilestoneSet = { ms -> if (!ms.isNullOrBlank()) store.setMilestone(ms) },
                    onQuestTaskUpdated = { qId, tId -> if (!qId.isNullOrBlank() && !tId.isNullOrBlank()) questRuntime.markTaskComplete(qId, tId) },
                    onQuestStageAdvanced = { qId, sId -> if (!qId.isNullOrBlank() && !sId.isNullOrBlank()) questRuntime.setStage(qId, sId) },
                    onSetRoomState = { rId, k, v -> if (!rId.isNullOrBlank() && k.isNotBlank()) store.setRoomState(rId, k, v) },
                    onGiveItem = { itemId, qty ->
                        val inv = store.state.value.inventory
                        store.setInventory(inv + (itemId to ((inv[itemId] ?: 0) + qty.coerceAtLeast(1))))
                    },
                    onTakeItem = { itemId, qty ->
                        val inv = store.state.value.inventory
                        val current = inv[itemId] ?: 0
                        val req = qty.coerceAtLeast(1)
                        if (current < req) false
                        else {
                            store.setInventory(if (current - req > 0) inv + (itemId to (current - req)) else inv - itemId)
                            true
                        }
                    },
                    onGiveXp = xpAwarder::award,
                    onReward = { r ->
                        r.xp?.let(xpAwarder::award)
                        r.ap?.let(store::addAp)
                        r.credits?.let(store::addCredits)
                        r.items.forEach { item ->
                            val inv = store.state.value.inventory
                            store.setInventory(inv + (item.itemId to ((inv[item.itemId] ?: 0) + (item.quantity ?: 1))))
                        }
                    }
                )
            )

            dialogue = DialogueService(
                rawDialogue,
                DialogueConditionEvaluator { cond -> conditionEvaluator(cond, store.state.value) },
                DialogueTriggerHandler { trig -> events.performActions(DialogueTriggerParser.parse(trig)) }
            )
            settle()
        }

        fun roundTripSave(state: GameSessionState): GameSessionState = runBlocking {
            val tmp = File(System.getProperty("java.io.tmpdir"), "starborn-playtest-${System.nanoTime()}").apply { mkdirs() }
            try {
                val persistence = GameSessionPersistence(tmp)
                persistence.writeSlot(1, state)
                requireNotNull(persistence.readSlot(1))
            } finally {
                tmp.deleteRecursively()
            }
        }
    }

    private fun conditionEvaluator(raw: String?, state: GameSessionState): Boolean {
        if (raw.isNullOrBlank()) return true
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.all { token ->
            val parts = token.split(':', limit = 2)
            val type = parts[0].trim().lowercase()
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
                else -> error("Unsupported dialogue condition in campaign test: $token")
            }
        }
    }

    private inline fun <reified T> readList(name: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return requireNotNull(moshi.adapter<List<T>>(type).fromJson(File(assets, name).readText()))
    }
}
