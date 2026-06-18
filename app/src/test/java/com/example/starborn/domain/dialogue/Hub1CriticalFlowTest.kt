package com.example.starborn.domain.dialogue

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionPersistence
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class Hub1CriticalFlowTest {

    @Test
    fun newGameAndFirstRoomTransitionTeachHotspotsAndJournal() {
        val harness = Hub1Harness()

        harness.events.handleTrigger("player_action", EventPayload.Action("new_game_spawn_player_and_fade"))
        assertTrue(harness.tutorialRequests.contains("hotspot_actions" to "Nova's Bunk"))

        harness.events.handleTrigger("player_action", EventPayload.Action("w1_mq01_check_bunk"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_shaft"))
        assertTrue(harness.tutorialRequests.contains("scene_market_journal" to "Nova's Bunk"))
    }

    @Test
    fun wakeUpCallCompletesFromBunkToJedToCryoInductorRepair() {
        val harness = Hub1Harness()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_nova_bunk"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_mq01_check_bunk"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_shaft"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_jed_bunk"))

        var state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w1_mq01"].orEmpty().contains("find_jed"))
        assertTrue(!state.questTasksCompleted["w1_mq01"].orEmpty().contains("reach_workshop"))
        assertTrue(harness.tutorialRequests.contains("npc_talk" to "Jed's Bunk"))

        val jed = harness.dialogue.startDialogue("Jed")
        assertEquals("jed_w1_mq01_intro_1", jed?.current()?.id)
        jed?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.questTasksCompleted["w1_mq01"].orEmpty().contains("talk_to_jed"))
        assertTrue(state.questTasksCompleted["w1_mq01"].orEmpty().contains("equip_starter_gear"))
        assertTrue(state.inventory["cryo_inductor"].orZero() >= 1)
        assertTrue(state.inventory["nova_flux_liner"].orZero() >= 1)
        assertTrue(state.inventory["scrap_metal"].orZero() >= 2)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("pit_shaft"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("workshop_floor"))
        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_screen_entered"))

        val afterBenchEntry = harness.store.state.value
        assertTrue(afterBenchEntry.activeQuests.contains("w1_mq01"))
        assertTrue(!afterBenchEntry.completedQuests.contains("w1_mq01"))
        assertTrue(afterBenchEntry.questTasksCompleted["w1_mq01"].orEmpty().contains("reach_workshop"))
        assertTrue(!afterBenchEntry.questTasksCompleted["w1_mq01"].orEmpty().contains("use_tinkering_table"))

        harness.events.handleTrigger("player_action", EventPayload.Action("tinkering_craft", "functional_cryo_inductor"))

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq01"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq01_complete"))
        assertTrue(state.questTasksCompleted["w1_mq01"].orEmpty().contains("use_tinkering_table"))
        assertTrue(state.activeQuests.contains("w1_mq02"))
        assertEquals("w1_mq02", state.trackedQuestId)
        assertEquals("reach_checkpoint", state.questStageById["w1_mq02"])
        assertTrue(state.inventory["ration_pack"].orZero() >= 1)
    }

    @Test
    fun heavyLiftingStartsAtDockAndCompletesOnRiotGuardVictory() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val bogs = harness.dialogue.startDialogue("Foreman Bogs")
        assertEquals("bogs_w1_sq03_intro_1", bogs?.current()?.id)
        bogs?.advanceUntilFinished()

        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq03_start_loader"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq03_move_cargo"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("acoustic_bulwark"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq03"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq03"))
        assertTrue(state.completedMilestones.contains("ms_w1_guardbreak_trained"))
        assertTrue(completedTasks.contains("talk_to_bogs"))
        assertTrue(completedTasks.contains("start_loader"))
        assertTrue(completedTasks.contains("move_cargo"))
        assertTrue(completedTasks.contains("learn_hydraulic_kick"))
        assertTrue(completedTasks.contains("break_training_shield"))
        assertTrue(completedTasks.contains("rescue_trapped_workers"))
    }

    @Test
    fun bogsRedirectsToHeavyLiftingBeforeDeepMineIfGuardBreakIsUntrained() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")
        harness.store.completeQuest("w1_mq02")
        harness.store.setInventory(mapOf("mine_access_badge" to 1))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_lobby"))

        val bogs = harness.dialogue.startDialogue("Foreman Bogs")
        assertEquals("bogs_w1_mq03_guardbreak_gate_1", bogs?.current()?.id)
        bogs?.advanceUntilFinished()

        val state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w1_mq03"))
        assertTrue(state.activeQuests.contains("w1_sq03"))
        assertEquals("w1_sq03", state.trackedQuestId)
        assertTrue(state.completedMilestones.contains("ms_w1_sq03_started"))
        assertTrue(state.questTasksCompleted["w1_sq03"].orEmpty().contains("talk_to_bogs"))
        assertTrue(!state.completedMilestones.contains("ms_w1_mq03_bogs_talked"))
    }

    @Test
    fun scavengersStashMakesTradeRowPlayable() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val scrapper = harness.dialogue.startDialogue("Scrapper")
        assertEquals("scrapper_w1_sq01_intro_1", scrapper?.current()?.id)
        scrapper?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("trade_stash"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq01_open_rebel_cache"))

        val turnIn = harness.dialogue.startDialogue("Scrapper")
        assertEquals("scrapper_w1_sq01_turnin_1", turnIn?.current()?.id)
        turnIn?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq01"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq01"))
        assertTrue(state.completedMilestones.contains("ms_w1_sq01_complete"))
        assertTrue(completedTasks.contains("talk_to_scrapper"))
        assertTrue(completedTasks.contains("find_hidden_stash"))
        assertTrue(completedTasks.contains("open_rebel_cache"))
        assertTrue(completedTasks.contains("return_cache_proof"))
        assertTrue(state.inventory["pulse_grenade"].orZero() >= 2)
    }

    @Test
    fun systemFlushMakesMedBayPlayable() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        val doc = harness.dialogue.startDialogue("Doc")
        assertEquals("doc_w1_sq02_intro_1", doc?.current()?.id)
        doc?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("medbay_vents"))
        harness.events.handleTrigger("player_action", EventPayload.Action("w1_sq02_patch_vent"))

        val turnIn = harness.dialogue.startDialogue("Doc")
        assertEquals("doc_w1_sq02_turnin_1", turnIn?.current()?.id)
        turnIn?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq02"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq02"))
        assertTrue(state.completedMilestones.contains("ms_w1_sq02_complete"))
        assertTrue(completedTasks.contains("talk_to_doc"))
        assertTrue(completedTasks.contains("enter_ventilation_hub"))
        assertTrue(completedTasks.contains("clear_toxic_blockage"))
        assertTrue(completedTasks.contains("return_to_doc"))
        assertTrue(state.inventory["mod_corrosive_rounds"].orZero() >= 1)
    }

    @Test
    fun protocolOverrideTerminalCompletesServerRoomSideQuest() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")
        harness.store.completeQuest("w1_mq02")
        harness.store.completeQuest("w1_mq03")
        harness.store.setInventory(mapOf("mine_access_badge" to 1))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("server_hub"))
        harness.events.handleTrigger("player_action", EventPayload.Action("start_hack_sq04"))

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq04"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq04"))
        assertTrue(!state.activeQuests.contains("w1_sq04"))
        assertTrue(completedTasks.contains("enter_server_room"))
        assertTrue(completedTasks.contains("locate_hacked_terminal"))
        assertTrue(completedTasks.contains("restore_protocol_spoof"))
        assertTrue(state.inventory["circuit_board"].orZero() >= 1)
    }

    @Test
    fun lostShiftStartsAtMineShuntAndCompletesFromDatapad() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")
        harness.store.completeQuest("w1_mq02")
        harness.store.startQuest("w1_mq03")
        harness.store.setInventory(mapOf("mine_access_badge" to 1))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_shunt"))

        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w1_sq05"))
        assertEquals("w1_sq05", state.trackedQuestId)
        assertTrue(state.questTasksCompleted["w1_sq05"].orEmpty().contains("find_collapsed_tunnel"))

        harness.events.handleTrigger("player_action", EventPayload.Action("read_datapad_sq05"))

        state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_sq05"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_sq05"))
        assertTrue(!state.activeQuests.contains("w1_sq05"))
        assertTrue(completedTasks.contains("find_collapsed_tunnel"))
        assertTrue(completedTasks.contains("recover_datapad"))
        assertTrue(completedTasks.contains("read_final_letter"))
        assertTrue(state.inventory["recoil_dampener"].orZero() >= 1)
    }

    @Test
    fun paperworkDenialZekeOverrideAndBadgeGateProgress() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("checkpoint_queue"))
        val hank = harness.dialogue.startDialogue("Guard Hank")
        assertEquals("hank_w1_mq02_denial_1", hank?.current()?.id)
        hank?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("checkpoint_booth"))
        val zeke = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq02_override_1", zeke?.current()?.id)
        zeke?.advanceUntilFinished()

        val state = harness.store.state.value
        val completedTasks = state.questTasksCompleted["w1_mq02"].orEmpty()
        assertTrue(state.completedQuests.contains("w1_mq02"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq02_complete"))
        assertTrue(completedTasks.contains("approach_admin_gate"))
        assertTrue(completedTasks.contains("request_clearance"))
        assertTrue(completedTasks.contains("meet_zeke"))
        assertTrue(completedTasks.contains("spoof_liability_form"))
        assertTrue(completedTasks.contains("receive_mine_access_badge"))
        assertTrue(state.activeQuests.contains("w1_mq03"))
        assertEquals("w1_mq03", state.trackedQuestId)
        assertEquals("sector_four_assignment", state.questStageById["w1_mq03"])
        assertTrue(state.inventory["mine_access_badge"].orZero() >= 1)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_lobby"))
        val afterLobby = harness.store.state.value
        assertTrue(afterLobby.questTasksCompleted["w1_mq03"].orEmpty().contains("enter_logistics_sector"))
        assertTrue(harness.messages.contains("Entering Logistics advances the main story. Finish Homestead errands first if you want to complete them now."))
    }

    @Test
    fun world1CriticalPathReachesCrashSiteAfterLaunch() {
        val harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")
        harness.store.completeQuest("w1_mq02")
        harness.store.completeQuest("w1_sq03")
        harness.store.setMilestone("ms_w1_guardbreak_trained")
        harness.store.setInventory(mapOf("mine_access_badge" to 1))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_lobby"))
        var state = harness.store.state.value
        assertTrue(state.activeQuests.contains("w1_mq03"))
        assertTrue(state.questTasksCompleted["w1_mq03"].orEmpty().contains("enter_logistics_sector"))

        val bogs = harness.dialogue.startDialogue("Foreman Bogs")
        assertEquals("bogs_w1_mq03_intro_1", bogs?.current()?.id)
        bogs?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_landing"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("echo_borer"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("acoustic_bulwark"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_threshold"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("echo_gap"))
        harness.events.handleTrigger("player_action", EventPayload.Action("touch_relic"))

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq03"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq03_complete"))
        assertTrue(state.activeQuests.contains("w1_mq04"))
        assertTrue(state.questTasksCompleted["w1_mq04"].orEmpty().contains("survive_lockdown_broadcast"))
        assertTrue(state.inventory["tuning_fork"].orZero() >= 1)

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("echo_exit"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("acoustic_bulwark"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("launch_lift"))

        val jed = harness.dialogue.startDialogue("Jed")
        assertEquals("jed_w1_mq04_sacrifice_1", jed?.current()?.id)
        jed?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq04"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq04_complete"))
        assertTrue(state.activeQuests.contains("w1_mq05"))
        assertTrue(state.inventory["ghost_signal_cell"].orZero() >= 1)

        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("resonance_buoy"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("launch_bay"))

        val warden = harness.dialogue.startDialogue("The Warden")
        assertEquals("warden_boss_intro_1", warden?.current()?.id)
        warden?.advanceUntilFinished()
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("the_iron_warden"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )

        val zekeAtBay = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq05_defeat_warden_1", zekeAtBay?.current()?.id)
        zekeAtBay?.advanceUntilFinished()
        assertTrue(harness.store.state.value.completedMilestones.contains("ms_w1_zeke_directed_to_pod"))

        val zekeAtPod = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq05_pod_core_1", zekeAtPod?.current()?.id)
        zekeAtPod?.advanceUntilFinished()

        state = harness.store.state.value
        assertTrue(state.completedMilestones.contains("ms_w1_chime_spliced"))
        assertTrue(state.questTasksCompleted["w1_mq05"].orEmpty().contains("splice_chime"))
        assertTrue(state.inventory["ghost_signal_cell"].orZero() == 0)

        harness.events.handleTrigger("player_action", EventPayload.Action("use_nav_console"))

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq05"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq05_complete"))
        assertEquals("sector9_crash_site", state.roomId)
        assertTrue(state.activeQuests.contains("w2_mq01"))
    }

    @Test
    fun world1CriticalPathResumesAfterSaveAtLaunchLockdown() = runBlocking {
        var harness = Hub1Harness()
        harness.store.completeQuest("w1_mq01")
        harness.store.completeQuest("w1_mq02")
        harness.store.completeQuest("w1_sq03")
        harness.store.setMilestone("ms_w1_guardbreak_trained")
        harness.store.setInventory(mapOf("mine_access_badge" to 1))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_lobby"))
        val bogs = harness.dialogue.startDialogue("Foreman Bogs")
        assertEquals("bogs_w1_mq03_intro_1", bogs?.current()?.id)
        bogs?.advanceUntilFinished()

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_landing"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("echo_borer"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("acoustic_bulwark"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_threshold"))
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("echo_gap"))
        harness.events.handleTrigger("player_action", EventPayload.Action("touch_relic"))

        val savedState = harness.store.state.value.copy(
            worldId = "world_1",
            hubId = "hub_2_logistics",
            roomId = "echo_heart"
        )
        assertTrue(savedState.completedQuests.contains("w1_mq03"))
        assertTrue(savedState.activeQuests.contains("w1_mq04"))
        assertTrue(savedState.completedMilestones.contains("ms_w1_mq03_complete"))

        val restoredState = roundTripCriticalPathState(savedState)
        harness = Hub1Harness(restoredState)
        assertTrue(harness.store.state.value.completedQuests.contains("w1_mq03"))
        assertTrue(harness.store.state.value.activeQuests.contains("w1_mq04"))

        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("echo_exit"))
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("acoustic_bulwark"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("launch_lift"))

        val jed = harness.dialogue.startDialogue("Jed")
        assertEquals("jed_w1_mq04_sacrifice_1", jed?.current()?.id)
        jed?.advanceUntilFinished()

        var state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq04"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq04_complete"))
        assertTrue(state.activeQuests.contains("w1_mq05"))

        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("resonance_buoy"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("launch_bay"))

        val warden = harness.dialogue.startDialogue("The Warden")
        assertEquals("warden_boss_intro_1", warden?.current()?.id)
        warden?.advanceUntilFinished()
        harness.events.handleTrigger(
            "encounter_victory",
            EventPayload.EncounterOutcome(
                enemyIds = listOf("the_iron_warden"),
                outcome = EventPayload.EncounterOutcome.Outcome.VICTORY
            )
        )

        val zekeAtBay = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq05_defeat_warden_1", zekeAtBay?.current()?.id)
        zekeAtBay?.advanceUntilFinished()

        val zekeAtPod = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w1_mq05_pod_core_1", zekeAtPod?.current()?.id)
        zekeAtPod?.advanceUntilFinished()

        harness.events.handleTrigger("player_action", EventPayload.Action("use_nav_console"))

        state = harness.store.state.value
        assertTrue(state.completedQuests.contains("w1_mq05"))
        assertTrue(state.completedMilestones.contains("ms_w1_mq05_complete"))
        assertEquals("sector9_crash_site", state.roomId)
        assertTrue(state.activeQuests.contains("w2_mq01"))
    }

    @Test
    fun sector9CrashSiteAndStrangeCoastQuestFlow() {
        val harness = Hub1Harness()
        harness.store.startQuest("w2_mq01")

        // 1. Talk to Zeke at the crash site
        val zeke = harness.dialogue.startDialogue("Zeke")
        assertEquals("zeke_w2_crash_1", zeke?.current()?.id)
        zeke?.advanceUntilFinished()

        val stateAfterZeke = harness.store.state.value
        assertTrue(stateAfterZeke.questTasksCompleted["w2_mq01"].orEmpty().contains("check_on_zeke"))

        // 2. Examine the pod core
        harness.events.handleTrigger("player_action", EventPayload.Action("w2_mq01_examine_pod"))

        val stateAfterPod = harness.store.state.value
        assertTrue(stateAfterPod.questTasksCompleted["w2_mq01"].orEmpty().contains("examine_pod"))
        assertTrue(stateAfterPod.inventory["ghost_signal_cell"].orZero() >= 1)
        assertTrue(stateAfterPod.inventory["medkit"].orZero() >= 1)
        assertTrue(stateAfterPod.inventory["wooden_rod"].orZero() >= 1)
        assertTrue(stateAfterPod.inventory["basic_lure"].orZero() >= 1)

        // 3. Move to the stream
        harness.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_stream"))

        val finalState = harness.store.state.value
        assertTrue(finalState.completedQuests.contains("w2_mq01"))
        assertTrue(finalState.completedMilestones.contains("ms_w2_mq01_complete"))
    }

    private class Hub1Harness(initialState: GameSessionState? = null) {
        val store = GameSessionStore()
        val events: EventManager
        val dialogue: DialogueService
        val messages = mutableListOf<String>()
        val tutorialRequests = mutableListOf<Pair<String?, String?>>()

        init {
            initialState?.let(store::restore)
            events = EventManager(
                events = loadEvents(),
                sessionStore = store,
                eventHooks = EventHooks(
                    onMessage = { messages += it },
                    onSystemTutorial = { sceneId, context, done ->
                        tutorialRequests += sceneId to context
                        done()
                    },
                    onQuestTaskUpdated = { questId, taskId ->
                        if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                            store.setQuestTaskCompleted(questId, taskId, true)
                        }
                    },
                    onQuestStageAdvanced = { questId, stageId ->
                        if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                            store.setQuestStage(questId, stageId)
                        }
                    },
                    onGiveItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val next = current + (itemId to (current[itemId].orZero() + quantity.coerceAtLeast(1)))
                        store.setInventory(next)
                    },
                    onTakeItem = { itemId, quantity ->
                        val current = store.state.value.inventory
                        val available = current[itemId].orZero()
                        val requested = quantity.coerceAtLeast(1)
                        if (available < requested) {
                            false
                        } else {
                            val remaining = available - requested
                            val next = if (remaining > 0) {
                                current + (itemId to remaining)
                            } else {
                                current - itemId
                            }
                            store.setInventory(next)
                            true
                        }
                    },
                    onGiveXp = { amount ->
                        store.addXp(amount)
                    },
                    onQuestCompleted = ::handleQuestCompleted
                )
            )
            dialogue = DialogueService(
                loadDialogue(),
                DialogueConditionEvaluator { condition -> conditionMet(condition, store.state.value) },
                DialogueTriggerHandler { trigger -> events.performActions(DialogueTriggerParser.parse(trigger)) }
            )
        }

        private fun handleQuestCompleted(questId: String?) {
            if (!questId.isNullOrBlank()) {
                events.handleTrigger("quest_stage_complete", EventPayload.QuestStage(questId))
            }
        }
    }

    private fun DialogueSession.advanceUntilFinished() {
        while (!isFinished()) {
            advance()
        }
    }

    private suspend fun roundTripCriticalPathState(state: GameSessionState): GameSessionState {
        val baseDir = File(
            System.getProperty("java.io.tmpdir"),
            "starborn-critical-flow-${System.nanoTime()}"
        ).apply { mkdirs() }
        return try {
            val persistence = GameSessionPersistence(baseDir)
            persistence.writeSlot(1, state)
            val restored = persistence.readSlot(1)
            assertNotNull("Expected World 1 checkpoint save to restore", restored)
            restored!!
        } finally {
            baseDir.deleteRecursively()
        }
    }

    private companion object {
        private val moshi = MoshiProvider.instance

        private fun conditionMet(raw: String?, state: GameSessionState): Boolean {
            if (raw.isNullOrBlank()) return true
            return raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .all { token ->
                    val parts = token.split(':', limit = 2)
                    val type = parts[0].trim().lowercase()
                    val value = parts.getOrNull(1)?.trim().orEmpty()
                    when (type) {
                        "milestone" -> value in state.completedMilestones
                        "milestone_set" -> value in state.completedMilestones
                        "milestone_not_set" -> value !in state.completedMilestones
                        "quest", "quest_active" -> value in state.activeQuests
                        "quest_not_started" -> value !in state.activeQuests &&
                            value !in state.completedQuests &&
                            value !in state.failedQuests
                        "quest_completed" -> value in state.completedQuests
                        "quest_not_completed" -> value !in state.completedQuests
                        "quest_stage" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId != null && stageId != null &&
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
                        }
                        "quest_stage_not" -> {
                            val (questId, stageId) = parseQuestPair(value)
                            questId == null || stageId == null ||
                                state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
                        }
                        "quest_task_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId != null && taskId != null &&
                                state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "quest_task_not_done" -> {
                            val (questId, taskId) = parseQuestPair(value)
                            questId == null || taskId == null ||
                                !state.questTasksCompleted[questId].orEmpty().contains(taskId)
                        }
                        "item" -> value in state.inventory && state.inventory[value].orZero() > 0
                        "item_not" -> value !in state.inventory || state.inventory[value].orZero() <= 0
                        else -> true
                    }
                }
        }

        private fun parseQuestPair(raw: String): Pair<String?, String?> {
            val parts = raw.split(':', limit = 2)
            val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
            val value = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
            return questId to value
        }

        private fun Int?.orZero(): Int = this ?: 0

        private fun loadDialogue(): List<DialogueLine> {
            val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
            val adapter = moshi.adapter<List<DialogueLine>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/dialogue.json").readText()))
        }

        private fun loadEvents(): List<GameEvent> {
            val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
            val adapter = moshi.adapter<List<GameEvent>>(type)
            return requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText()))
        }
    }
}
