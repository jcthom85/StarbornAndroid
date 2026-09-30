package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.dialogue.*
import com.example.starborn.domain.event.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import com.squareup.moshi.Types

/** Real authored events/dialogue and disk saves; presentation and reward delivery are headless. */
class World1RecentChangesTest {
    @Test fun retiredRelayEventsCannotGrantRewards() {
        val assets = readAssets<GameEvent>("events.json")
        assertFalse(assets.any { it.id.startsWith("qpack_evt_relay_stage") ||
            it.id in setOf("qpack_evt_collapse_ambush_victory", "qpack_evt_drone_sweep_victory") })
        // Stale quest state in an old save must not revive the retired reward chain.
        val h = Harness(GameSessionState(activeQuests = setOf("q_mine_relay_scramble")))
        h.events.handleTrigger("encounter_victory", EventPayload.EnemyVictory(listOf("acoustic_bulwark")))
        h.win("admin_security")
        for (id in listOf("enc_collapse_ambush", "enc_drone_sweep")) {
            h.events.handleTrigger("encounter_victory", EventPayload.EncounterOutcome(
                listOf("acoustic_bulwark"), EventPayload.EncounterOutcome.Outcome.VICTORY,
                encounterId = id))
        }
        h.events.handleTrigger("quest_stage_complete", EventPayload.QuestStage("q_mine_relay_scramble"))
        assertEquals(0, h.state.playerXp)
        assertTrue(h.state.inventory.isEmpty())
        assertFalse("q_mine_relay_scramble" in h.state.completedQuests)
    }

    @Test fun earlyDrillVictoryRegistersThroughBoggsOnce() {
        val h = Harness(GameSessionState(activeQuests = setOf("w1_mq03"), completedQuests = setOf("w1_mq01")))
        h.win("admin_security")
        assertTrue("ms_w1_guard_drill_won" in h.state.completedMilestones)
        assertFalse("w1_sq03" in h.state.completedQuests)
        val session = requireNotNull(h.dialogue.startDialogue("Foreman Boggs"))
        assertEquals("bogs_w1_sq03_prior_drill", session.current()?.id)
        h.finish(session)
        assertCertified(h)
        assertEquals(100, h.state.playerXp)
        val before = h.state
        h.action("w1_sq03_register_drill")
        h.win("admin_security")
        assertEquals(before.inventory, h.state.inventory)
        assertEquals(before.playerXp, h.state.playerXp)
    }

    @Test fun registeringWithoutADrillResultDoesNotGrantCertification() {
        val h = Harness(GameSessionState(activeQuests = setOf("w1_sq03")))
        h.action("w1_sq03_register_drill")
        assertFalse("w1_sq03" in h.state.completedQuests)
        assertFalse("ms_w1_guardbreak_trained" in h.state.completedMilestones)
        assertEquals(0, h.state.playerXp)
        assertTrue(h.state.inventory.isEmpty())
    }

    @Test fun authoredRiotObjectiveIsOptionalAndThresholdAdvancesWithoutIt() {
        val quest = readAssets<Quest>("quests.json").single { it.id == "w1_mq03" }
        val stage = quest.stages.single { it.id == "deep_mine_descent" }
        assertTrue(stage.tasks.single { it.id == "break_riot_guard" }.optional)
        assertFalse(stage.tasks.single { it.id == "reach_architect_threshold" }.optional)
        val h = Harness(GameSessionState(activeQuests = setOf("w1_mq03"),
            questStageById = mapOf("w1_mq03" to "deep_mine_descent")))
        h.events.handleTrigger("enter_room", EventPayload.EnterRoom("mine_threshold"))
        assertEquals("relic_sync", h.state.questStageById["w1_mq03"])
        assertFalse("break_riot_guard" in h.state.questTasksCompleted["w1_mq03"].orEmpty())
    }

    @Test fun normalAndLegacyDrillsCompleteOnlyInTheirRooms() {
        for (room in listOf("admin_security", "workshop_dock")) {
            val h = Harness(GameSessionState(activeQuests = setOf("w1_sq03")))
            h.win("mine_checkpoint")
            assertFalse("w1_sq03" in h.state.completedQuests)
            assertFalse("ms_w1_guard_drill_won" in h.state.completedMilestones)
            h.win(room)
            assertCertified(h)
            assertEquals(100, h.state.playerXp)
            h.action("w1_sq03_register_drill")
            assertEquals(100, h.state.playerXp)
        }
    }

    @Test fun oldClearedTrainerSaveRecoversWithoutRepeatingFight() {
        val old = GameSessionState(
            activeQuests = setOf("w1_sq03"),
            roomStates = mapOf("admin_security" to mapOf("encounter_cleared:acoustic_bulwark" to true)),
            inventory = mapOf("scrap_metal" to 3), playerXp = 42
        )
        val restored = diskRoundTrip(old).migrateOpeningNarrativeState()
        assertEquals(restored, restored.migrateOpeningNarrativeState())
        assertEquals(old.inventory, restored.inventory)
        assertEquals(old.playerXp, restored.playerXp)
        val h = Harness(restored)
        h.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_security"))
        assertCertified(h)
        assertEquals(142, h.state.playerXp)
        val replay = Harness(diskRoundTrip(h.state).migrateOpeningNarrativeState())
        replay.events.handleTrigger("enter_room", EventPayload.EnterRoom("admin_security"))
        replay.action("w1_sq03_register_drill")
        assertEquals(h.state.inventory, replay.state.inventory)
        assertEquals(h.state.playerXp, replay.state.playerXp)
    }

    @Test fun oldCompletedCertificationMigrationDoesNotPayAgain() {
        val old = GameSessionState(completedQuests = setOf("w1_sq03"), playerXp = 100,
            inventory = mapOf("heavy_gear" to 1, "hydraulic_fluid" to 1))
        val migrated = old.migrateOpeningNarrativeState()
        assertEquals(migrated, migrated.migrateOpeningNarrativeState())
        assertTrue("nova_hydraulic_kick" in migrated.unlockedSkills)
        val h = Harness(migrated)
        h.action("w1_sq03_register_drill")
        h.win("admin_security")
        assertEquals(old.inventory, h.state.inventory)
        assertEquals(old.playerXp, h.state.playerXp)
    }

    @Test fun trainerPresentationChangesNoCombatStatsOrRewards() {
        val enemy = readAssets<Enemy>("enemies.json").single { it.id == "acoustic_bulwark" }
        assertEquals("Acoustic Bulwark", enemy.name)
        for (room in listOf("admin_security", "workshop_dock")) {
            val drill = enemy.forEncounterRoom(room)
            assertEquals("Shield Trainer", drill.name)
            assertEquals(enemy, drill.copy(name = enemy.name, flavor = enemy.flavor, description = enemy.description))
        }
        assertEquals(enemy, enemy.forEncounterRoom("launch_lift"))
        assertEquals(enemy, enemy.forEncounterRoom("mine_checkpoint"))
    }

    @Test fun cartAllowsCorrectRouteWithoutInspectionAndStaysOpenOnDisk() {
        val h = Harness()
        h.action("w1_cart_release")
        assertFalse(h.breached())
        assertTrue(h.messages.last().contains("Divert"))
        h.action("w1_cart_track_diverted")
        assertFalse("ms_w1_cart_rails_read" in h.state.completedMilestones)
        h.action("w1_cart_release")
        assertTrue(h.breached())
        h.action("w1_cart_track_reset")
        assertTrue(h.breached())
        assertFalse("ms_w1_cart_route_set" in h.state.completedMilestones)
        val restored = Harness(diskRoundTrip(h.state))
        restored.action("w1_cart_track_diverted")
        assertTrue(restored.messages.last().contains("crashed cart"))
        val count = restored.messages.size
        restored.action("w1_cart_release")
        assertEquals(count, restored.messages.size)
        assertTrue(restored.breached())
        val gate = readAssets<Room>("rooms.json").single { it.id == "mine_junction" }.blockedDirections!!.getValue("west")
        assertTrue(gate.requires.orEmpty().all { restored.state.roomStates[it.roomId]?.get(it.stateKey) == it.value })
    }

    @Test fun inspectingRailsDoesNotPermitAnUnroutedCartRelease() {
        val h = Harness()
        h.action("w1_cart_inspect_rails")
        h.action("w1_cart_release")
        assertFalse(h.breached())
        assertTrue(h.messages.last().contains("diverter"))
        h.action("w1_cart_track_diverted")
        h.action("w1_cart_track_reset")
        h.action("w1_cart_release")
        assertFalse(h.breached())
    }

    @Test fun oldCartBreachSurvivesResetSwitchMigration() {
        val old = GameSessionState(completedMilestones = setOf("ms_w1_cart_track_diverted"),
            roomStates = mapOf("mine_junction" to mapOf("track_diverted" to false)))
        val migrated = diskRoundTrip(old).migrateOpeningNarrativeState()
        assertEquals(migrated, migrated.migrateOpeningNarrativeState())
        assertTrue(Harness(migrated).breached())
    }

    @Test fun protocolRequiresThawAndWrongOutputsOrDisconnectDoNotReward() {
        val h = Harness()
        h.action("w1_sq04_compile_spoof")
        assertEquals(0, h.state.playerXp)
        h.action("start_hack_sq04")
        h.action("w1_sq04_compile_spoof")
        assertEquals(0, h.state.playerXp)
        h.action("w1_sq04_thaw_console")
        h.action("start_hack_sq04")
        var session = requireNotNull(h.pendingDialogue)
        session.advance()
        for (wrong in listOf("network", "account")) {
            session.choose(wrong)
            assertFalse("w1_sq04" in h.state.completedQuests)
            assertEquals(0, h.state.inventory.getOrDefault("circuit_board", 0))
            assertEquals(0, h.state.playerXp)
            session.advance()
            assertTrue(session.choices().any { it.id == "board" })
        }
        session.choose("leave")
        assertTrue(session.isFinished())
        h.action("start_hack_sq04")
        session = requireNotNull(h.pendingDialogue)
        session.advance()
        session.choose("board")
        assertTrue("w1_sq04" in h.state.completedQuests)
        assertEquals(1, h.state.inventory["circuit_board"])
        assertEquals(110, h.state.playerXp)
        val restored = Harness(diskRoundTrip(h.state))
        restored.action("w1_sq04_compile_spoof")
        restored.action("start_hack_sq04")
        assertNull(restored.pendingDialogue)
        assertEquals(h.state.inventory, restored.state.inventory)
        assertEquals(h.state.playerXp, restored.state.playerXp)
    }

    @Test fun partialProtocolSaveCanReopenChoiceAndFinish() {
        val h = Harness()
        h.action("start_hack_sq04")
        h.action("w1_sq04_thaw_console")
        val resumed = Harness(diskRoundTrip(h.state))
        resumed.action("start_hack_sq04")
        val session = requireNotNull(resumed.pendingDialogue)
        session.advance()
        session.choose("board")
        assertEquals(110, resumed.state.playerXp)
        assertEquals(1, resumed.state.inventory["circuit_board"])
    }

    private fun assertCertified(h: Harness) {
        assertTrue("w1_sq03" in h.state.completedQuests)
        assertTrue("ms_w1_guardbreak_trained" in h.state.completedMilestones)
        assertEquals(1, h.state.inventory["heavy_gear"])
        assertEquals(1, h.state.inventory["hydraulic_fluid"])
    }

    private class Harness(initial: GameSessionState = GameSessionState()) {
        val store = GameSessionStore().apply { restore(initial) }
        val state get() = store.state.value
        val messages = mutableListOf<String>()
        var pendingDialogue: DialogueSession? = null
        lateinit var dialogue: DialogueService
        val events = EventManager(readAssets<GameEvent>("events.json"), store, EventHooks(
            onMessage = { messages += it },
            onStartDialogue = { pendingDialogue = dialogue.startDialogue(it) },
            onGiveXp = { store.addXp(it) },
            onGiveItem = { id, count -> store.setInventory(state.inventory + (id to (state.inventory.getOrDefault(id, 0) + count))) },
            onQuestTaskUpdated = { q, t -> if (q != null && t != null) store.setQuestTaskCompleted(q, t, true) },
            onQuestStageAdvanced = { q, stage -> if (q != null && stage != null) store.setQuestStage(q, stage) },
            onSetRoomState = { room, key, value -> if (room != null) store.setRoomState(room, key, value) }
        ))
        init {
            dialogue = DialogueService(readAssets<DialogueLine>("dialogue.json"),
                DialogueConditionEvaluator { raw -> conditionMet(raw, state) },
                DialogueTriggerHandler { events.performActions(DialogueTriggerParser.parse(it)) })
        }
        fun action(id: String) = events.handleTrigger("player_action", EventPayload.Action(id))
        fun win(room: String) = events.handleTrigger("encounter_victory", EventPayload.EncounterOutcome(
            listOf("acoustic_bulwark"), EventPayload.EncounterOutcome.Outcome.VICTORY, room))
        fun breached() = state.roomStates["mine_junction"]?.get("bulkhead_breached") == true
        fun finish(session: DialogueSession) {
            repeat(30) {
                if (session.isFinished()) return
                check(session.choices().isEmpty()) { "Unexpected choice in linear certification dialogue" }
                session.advance()
            }
            error("Dialogue did not finish within 30 lines")
        }
    }
}

private inline fun <reified T> readAssets(name: String): List<T> {
    val root = if (File("src/main/assets").isDirectory) File("src/main/assets") else File("app/src/main/assets")
    val type = Types.newParameterizedType(List::class.java, T::class.java)
    return requireNotNull(MoshiProvider.instance.adapter<List<T>>(type).fromJson(File(root, name).readText()))
}

private fun diskRoundTrip(state: GameSessionState): GameSessionState = runBlocking {
    val dir = Files.createTempDirectory("starborn-w1-regression-").toFile()
    try {
        val persistence = GameSessionPersistence(dir)
        persistence.writeSlot(1, state)
        requireNotNull(persistence.readSlot(1))
    } finally { dir.deleteRecursively() }
}

// Fail closed for unsupported conditions; do not silently make unrelated roots eligible.
private fun conditionMet(raw: String?, state: GameSessionState): Boolean = raw.isNullOrBlank() || raw.split(',').all { token ->
    val kind = token.substringBefore(':').trim()
    val value = token.substringAfter(':').trim()
    val quest = value.substringBefore(':')
    val task = value.substringAfter(':')
    when (kind) {
        "quest_active" -> value in state.activeQuests
        "quest_completed" -> value in state.completedQuests
        "quest_not_started" -> value !in state.activeQuests && value !in state.completedQuests && value !in state.failedQuests
        "milestone", "milestone_set" -> value in state.completedMilestones
        "milestone_not_set" -> value !in state.completedMilestones
        "quest_task_done" -> task in state.questTasksCompleted[quest].orEmpty()
        "quest_task_not_done" -> task !in state.questTasksCompleted[quest].orEmpty()
        else -> false
    }
}
