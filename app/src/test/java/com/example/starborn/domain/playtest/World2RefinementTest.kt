package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.*
import com.example.starborn.domain.event.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.*
import com.example.starborn.shared.puzzle.*
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class World2RefinementTest {
    @Test fun beastCanRetryAfterDefeatRetreatAndAnOldConsumedEvent() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq04"),
            questTasksCompleted = mapOf("w2_mq04" to setOf("confront_stalker")),
            completedEvents = setOf("w2_mq04_beast_ambush")))
        h.action("w2_mq04_beast_ambush")
        assertEquals(1, h.spawns)
        assertFalse("ms_w2_beast_defeated" in h.state.completedMilestones)
        h.outcome(EventPayload.EncounterOutcome.Outcome.DEFEAT)
        h.action("w2_mq04_beast_ambush")
        h.outcome(EventPayload.EncounterOutcome.Outcome.RETREAT)
        h.action("w2_mq04_beast_ambush")
        assertEquals(3, h.spawns)
        h.events.handleTrigger("encounter_victory", EventPayload.EncounterOutcome(
            listOf("the_beast"), EventPayload.EncounterOutcome.Outcome.VICTORY, "unrelated_room"))
        assertFalse("ms_w2_beast_defeated" in h.state.completedMilestones)
        h.outcome(EventPayload.EncounterOutcome.Outcome.VICTORY)
        assertTrue("ms_w2_beast_defeated" in h.state.completedMilestones)
        h.action("w2_mq04_beast_ambush")
        assertEquals(3, h.spawns)
    }

    @Test fun drillFinishesOnlyAfterItsSceneAndResumesFromDiskOnce() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq04"),
            questTasksCompleted = mapOf("w2_mq04" to setOf("defeat_the_beast"))))
        h.action("w2_mq04_anchor_drill")
        assertFalse("ms_w2_mq04_complete" in h.state.completedMilestones)
        assertFalse("nova_link" in h.state.unlockedSkills)
        assertEquals(setOf("scene_anchor_drill"), h.state.pendingEventCinematics)
        val resumed = Harness(roundTrip(h.state).migrateOpeningNarrativeState())
        resumed.events.resumePendingCinematics()
        resumed.scenes.getValue("scene_anchor_drill").invoke()
        assertTrue("w2_mq04" in resumed.state.completedQuests)
        assertTrue("nova_link" in resumed.state.unlockedSkills)
        assertEquals(300, resumed.state.playerXp)
        resumed.scenes.getValue("scene_anchor_drill").invoke()
        resumed.action("w2_mq04_anchor_drill")
        assertEquals(300, resumed.state.playerXp)
        assertTrue(resumed.state.pendingEventCinematics.isEmpty())
    }

    @Test fun migrationRepairsPrematureFlagsWithoutErasingEarnedProgress() {
        val premature = GameSessionState(activeQuests = setOf("w2_mq04"),
            completedMilestones = setOf("ms_w2_beast_defeated", "ms_w2_mq04_complete"),
            pendingEventCinematics = setOf("scene_anchor_drill"))
        val migrated = premature.migrateOpeningNarrativeState()
        assertFalse("ms_w2_beast_defeated" in migrated.completedMilestones)
        assertFalse("ms_w2_mq04_complete" in migrated.completedMilestones)
        assertEquals(premature.pendingEventCinematics, migrated.pendingEventCinematics)
        assertEquals(migrated, migrated.migrateOpeningNarrativeState())
        val earned = premature.copy(completedQuests = setOf("w2_mq04"))
            .migrateOpeningNarrativeState()
        assertTrue("nova_link" in earned.unlockedSkills)
        assertTrue("ms_w2_mq04_complete" in earned.completedMilestones)
    }

    @Test fun stasisRequiresCluesDecisionsConversationAndSeparateBridgeRecovery() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq03")))
        h.action("w2_mq03_align_complete")
        assertNull(h.pending)
        for (id in listOf("inspect_murals", "inspect_pod", "read_mural_overview", "stabilize_coolant"))
            h.action("w2_mq03_$id")
        h.action("w2_mq03_align_complete")
        val s = requireNotNull(h.pending)
        s.advance()
        s.choose("wrong"); s.advance()
        assertFalse("align_stasis_rings" in h.state.questTasksCompleted["w2_mq03"].orEmpty())
        repeat(3) { s.choose("correct") }
        assertTrue("ms_w2_stasis_awake" in h.state.completedMilestones)
        assertFalse("orion" in h.state.partyMembers)
        assertFalse("w2_mq03" in h.state.completedQuests)
        h.action("w2_mq03_recover_bridge")
        assertFalse("bridge_relic" in h.state.inventory)
        val resumed = Harness(roundTrip(h.state).migrateOpeningNarrativeState())
        val talk = requireNotNull(resumed.dialogue.startDialogue("Orion"))
        repeat(3) { talk.advance() }
        assertTrue("orion" in resumed.state.partyMembers)
        assertFalse("w2_mq03" in resumed.state.completedQuests)
        resumed.action("w2_mq03_recover_bridge")
        assertEquals(1, resumed.state.inventory["bridge_relic"])
        assertEquals(250, resumed.state.playerXp)
        assertTrue("w2_mq04" in resumed.state.activeQuests)
        resumed.action("w2_mq03_recover_bridge")
        assertEquals(250, resumed.state.playerXp)
    }

    @Test fun sourceGateSupportsWrongChoicesClosingAndReopeningWithoutOpeningEarly() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq05")))
        h.action("w2_mq05_bypass_gate")
        assertNull(h.pending)
        for (id in listOf("stabilize_horn", "ground_cup", "read_pressure_gauge", "overload_breakers"))
            h.action("w2_mq05_$id")
        h.action("w2_mq05_bypass_gate")
        val s = requireNotNull(h.pending); s.advance(); s.choose("wrong"); s.advance(); s.choose("leave")
        assertFalse("ms_w2_gate_bypassed" in h.state.completedMilestones)
        val resumed = Harness(roundTrip(h.state))
        resumed.solve("w2_mq05_bypass_gate", 3)
        assertEquals("sector9_hangar_bay", resumed.state.roomId)
        assertTrue("ms_w2_gate_bypassed" in resumed.state.completedMilestones)
        val before = resumed.state
        resumed.action("w2_mq05_resolve_gate")
        assertEquals(before, resumed.state)
    }

    @Test fun optionalMuralNeedsCrystalsAndCorrectSocketDecisions() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_sq04")))
        h.action("w2_sq04_complete"); assertNull(h.pending)
        for (id in listOf("west", "east", "north")) h.action("w2_sq04_crystal_$id")
        h.action("w2_sq04_complete")
        val s = requireNotNull(h.pending); s.advance(); s.choose("wrong"); s.advance()
        assertEquals(0, h.state.playerXp)
        repeat(2) { s.choose("correct") }
        assertTrue("w2_sq04" in h.state.completedQuests)
        assertEquals(150, h.state.playerXp)
        h.action("w2_sq04_restore_chorus")
        assertEquals(1, h.state.inventory["focus_conduit"])
        assertEquals(150, h.state.playerXp)
    }

    @Test fun ventPatternRequiresTheMaintenanceGapAndThenAllowsTheHatch() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_sq05")))
        h.action("w2_sq05_bypass_guards")
        assertFalse("ms_w2_guards_bypassed" in h.state.completedMilestones)
        h.action("w2_sq05_hack_grid")
        val s = requireNotNull(h.pending); s.advance(); s.choose("wrong"); s.advance()
        assertFalse("ms_w2_grid_hacked" in h.state.completedMilestones)
        s.choose("correct")
        h.action("w2_sq05_bypass_guards")
        assertTrue("ms_w2_guards_bypassed" in h.state.completedMilestones)
    }

    @Test fun departureCanBeCancelledAndPendingDepartureResumesWithoutDuplicateRewards() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq05"),
            questTasksCompleted = mapOf("w2_mq05" to setOf("reboot_bridge_relic"))))
        h.action("w2_mq05_launch"); requireNotNull(h.pending).choose("stay")
        assertTrue(h.scenes.isEmpty())
        h.action("w2_mq05_launch"); requireNotNull(h.pending).choose("depart")
        assertFalse("w2_mq05" in h.state.completedQuests)
        assertEquals(0, h.state.playerXp)
        val resumed = Harness(roundTrip(h.state))
        resumed.events.resumePendingCinematics()
        val done = resumed.scenes.getValue("scene_w2_astra_departure")
        done(); done()
        assertEquals("spire_sewers_landing", resumed.state.roomId)
        assertTrue("w3_mq11" in resumed.state.activeQuests)
        assertEquals(350, resumed.state.playerXp)
        resumed.action("w2_mq05_confirm_departure")
        assertEquals(350, resumed.state.playerXp)
    }

    @Test fun salvageReturnRouteRequiresDiscoveryAndRecoveredConduits() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_mq05"), roomId = "sector9_hangar_bay"))
        h.action("w2_mq05_return_to_pod"); assertEquals("sector9_hangar_bay", h.state.roomId)
        h.store.setQuestTaskCompleted("w2_mq05", "bypass_source_gate", true)
        h.action("w2_mq05_inspect_astra"); h.action("w2_mq05_return_to_pod")
        assertEquals("sector9_landing_drop", h.state.roomId)
        h.action("w2_mq05_return_to_hangar"); assertEquals("sector9_landing_drop", h.state.roomId)
        h.action("w2_mq05_collect_conduits"); h.action("w2_mq05_return_to_hangar")
        assertEquals("sector9_hangar_bay", h.state.roomId)
    }

    @Test fun tideglassDiscoveryUsesTheActualTidePools() {
        val h = Harness(GameSessionState(activeQuests = setOf("w2_sq03")))
        h.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_stream_pools"))
        assertFalse("visit_beach" in h.state.questTasksCompleted["w2_sq03"].orEmpty())
        h.events.handleTrigger("enter_room", EventPayload.EnterRoom("sector9_beach_pools"))
        assertTrue("visit_beach" in h.state.questTasksCompleted["w2_sq03"].orEmpty())
    }

    @Test fun prismVisualsAgreeWithAuthoredSubmissionTolerances() {
        val puzzle = readW2<TuningPuzzle>("tuning_puzzles.json").single { it.id == "w2_biolum_matrix_tune" }
        assertEquals("prism_optics", puzzle.presentation)
        fun optics(id: String? = null, offset: Float = 0f) = PrismOptics(puzzle.sliders.map {
            SignalDial(it.id, it.target + if (it.id == id) offset else 0f, it.min, it.max, it.target, it.tolerance)
        })
        assertTrue(optics().ready)
        for (dial in puzzle.sliders) {
            assertTrue(optics(dial.id, dial.tolerance).ready)
            assertFalse(optics(dial.id, dial.tolerance + 1f).ready)
        }
        assertTrue(optics("angle", -20f).aimOffset < 0f)
        assertTrue(optics("angle", 20f).aimOffset > 0f)
        assertTrue(optics("freq", 20f).fringe > 0f)
        assertTrue(optics("lux", 20f).feedback.contains("overloaded"))
    }

    private class Harness(initial: GameSessionState) {
        val store = GameSessionStore().apply { restore(initial) }
        val state get() = store.state.value
        var pending: DialogueSession? = null
        var spawns = 0
        val scenes = mutableMapOf<String, () -> Unit>()
        lateinit var dialogue: DialogueService
        val events = EventManager(readW2<GameEvent>("events.json"), store, EventHooks(
            onStartDialogue = { pending = dialogue.startDialogue(it) },
            onSpawnEncounter = { _, _ -> spawns++ },
            onPlayCinematic = { id, done -> scenes[id] = done },
            onGiveXp = { store.addXp(it) },
            onGiveItem = { id, qty -> store.setInventory(state.inventory + (id to (state.inventory.getOrDefault(id, 0) + qty))) },
            onQuestTaskUpdated = { q, t -> if (q != null && t != null) store.setQuestTaskCompleted(q, t, true) },
            onQuestStageAdvanced = { q, s -> if (q != null && s != null) store.setQuestStage(q, s) },
            onSetRoomState = { r, k, v -> if (r != null) store.setRoomState(r, k, v) }
        ))
        init {
            dialogue = DialogueService(readW2<DialogueLine>("dialogue.json"), DialogueConditionEvaluator { raw ->
                raw.isNullOrBlank() || raw.split(',').all { token ->
                    val type = token.substringBefore(':').trim()
                    val value = token.substringAfter(':').trim()
                    when (type) {
                        "quest_active" -> value in state.activeQuests
                        "quest_completed" -> value in state.completedQuests
                        "quest_not_started" -> value !in state.activeQuests && value !in state.completedQuests && value !in state.failedQuests
                        "quest_task_done" -> value.substringAfter(':') in state.questTasksCompleted[value.substringBefore(':')].orEmpty()
                        "quest_task_not_done" -> value.substringAfter(':') !in state.questTasksCompleted[value.substringBefore(':')].orEmpty()
                        "milestone", "milestone_set" -> value in state.completedMilestones
                        "milestone_not_set" -> value !in state.completedMilestones
                        else -> false
                    }
                }
            }, DialogueTriggerHandler { events.performActions(DialogueTriggerParser.parse(it)) })
        }
        fun action(id: String) = events.handleTrigger("player_action", EventPayload.Action(id))
        fun solve(id: String, count: Int) { action(id); val s = requireNotNull(pending); s.advance(); repeat(count) { s.choose("correct") } }
        fun outcome(outcome: EventPayload.EncounterOutcome.Outcome) = events.handleTrigger(
            "encounter_${outcome.name.lowercase()}", EventPayload.EncounterOutcome(listOf("the_beast"), outcome, "sector9_canopy_ridge"))
    }
}

private inline fun <reified T> readW2(name: String): List<T> {
    val root = if (File("src/main/assets").isDirectory) File("src/main/assets") else File("app/src/main/assets")
    return requireNotNull(MoshiProvider.instance.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java)).fromJson(File(root, name).readText()))
}

private fun roundTrip(state: GameSessionState): GameSessionState = runBlocking {
    val dir = Files.createTempDirectory("starborn-w2-refinement-").toFile()
    try { val save = GameSessionPersistence(dir); save.writeSlot(1, state); requireNotNull(save.readSlot(1)) }
    finally { dir.deleteRecursively() }
}
