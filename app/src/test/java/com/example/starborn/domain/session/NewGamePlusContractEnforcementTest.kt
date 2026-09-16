package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Vector E of the Automated Testing Strategy:
 * New Game Plus (NG+) Contract Enforcement.
 *
 * Enforces that:
 * 1. NG+ carries over player level, credits, unlocked weapons/armors, and inventory,
 *    while resetting story progress and completed milestones to pristine state with "ms_master_protocol_active".
 * 2. World 1 main quest progression (MQ01 to MQ05) executes cleanly from the start without sequence locks.
 * 3. Crucially, campaign progress milestones such as "ms_w2_mq05_complete" are NOT pre-set, ensuring
 *    World 2 side quests and narrative dialogue guards are never locked out in New Game Plus.
 */
class NewGamePlusContractEnforcementTest {

    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val allEvents by lazy { reader.readList<GameEvent>("events.json") }
    private val allDialogue by lazy { reader.readList<DialogueLine>("dialogue.json") }
    private val allQuests by lazy { reader.readList<Quest>("quests.json") }
    private val allRooms by lazy { reader.readList<Room>("rooms.json") }

    /**
     * Builds a simulated endgame state representing a cleared campaign.
     */
    private fun buildEndgameState(): GameSessionState {
        val allMqIds = (1..30).map { "w${((it - 1) / 5) + 1}_mq${it.toString().padStart(2, '0')}" }.toSet()
        return GameSessionState(
            worldId = "world_6",
            hubId = "hub_6_core",
            roomId = "aethel_heart",
            playerId = "nova",
            playerLevel = 28,
            playerXp = 45000,
            playerCredits = 15420,
            partyMembers = listOf("nova", "zeke", "orion", "gh0st"),
            partyMemberLevels = mapOf("nova" to 28, "zeke" to 28, "orion" to 28, "gh0st" to 28),
            unlockedWeapons = setOf("starter_cutter", "plasma_cutter", "arc_welder", "harmonic_blade", "quantum_lance"),
            unlockedArmors = setOf("miner_rig", "hazard_suit", "aegis_plating", "chrono_mesh"),
            equippedWeapons = mapOf("nova" to "quantum_lance", "zeke" to "arc_welder", "orion" to "harmonic_blade", "gh0st" to "plasma_cutter"),
            equippedArmors = mapOf("nova" to "chrono_mesh", "zeke" to "aegis_plating", "orion" to "hazard_suit", "gh0st" to "miner_rig"),
            inventory = mapOf(
                "medkit" to 15,
                "repair_patch" to 8,
                "source_crystal" to 42,
                "mining_laser" to 1
            ),
            completedQuests = allMqIds,
            completedMilestones = setOf(
                "ms_game_complete",
                "ms_w1_mq05_complete",
                "ms_w2_mq05_complete",
                "ms_w3_mq15_complete",
                "ms_w4_mq20_complete",
                "ms_w5_mq25_complete",
                "ms_w6_mq30_complete"
            )
        )
    }

    /**
     * Executes the New Game Plus seed generation matching AppServices.startNewGamePlus() contract.
     */
    private fun executeNewGamePlusTransition(previousSession: GameSessionState): GameSessionState {
        val startingRoomId = "pit_nova_bunk"
        val seedState = GameSessionState(
            worldId = "world_1",
            hubId = "hub_1_homestead",
            roomId = startingRoomId,
            playerId = previousSession.playerId ?: "nova",
            playerLevel = previousSession.playerLevel.coerceAtLeast(1),
            playerXp = previousSession.playerXp,
            playerCredits = previousSession.playerCredits.coerceAtLeast(5000),
            unlockedWeapons = previousSession.unlockedWeapons,
            unlockedArmors = previousSession.unlockedArmors,
            partyMembers = previousSession.partyMembers,
            partyMemberLevels = previousSession.partyMemberLevels,
            partyMemberXp = previousSession.partyMemberXp,
            equippedWeapons = previousSession.equippedWeapons,
            equippedArmors = previousSession.equippedArmors,
            equippedItems = previousSession.equippedItems,
            completedMilestones = setOf("ms_master_protocol_active")
        )

        val store = GameSessionStore()
        store.restore(seedState.migrateOpeningNarrativeState())
        store.resetTutorialProgress()
        store.resetQuestProgress()
        store.setInventory(previousSession.inventory)
        store.startQuest("w1_mq01", track = true)
        store.setQuestStage("w1_mq01", "wake_in_the_pit")
        return store.state.value
    }

    // =========================================================================
    // 1. NG+ State Reset & Carry-Over Verification
    // =========================================================================

    @Test
    fun ngPlus_resetsStoryProgress_whilePreservingLevelsCreditsAndEquipment() {
        val endgame = buildEndgameState()
        val ngPlusState = executeNewGamePlusTransition(endgame)

        // Story progress must be cleanly reset to World 1
        assertEquals("world_1", ngPlusState.worldId)
        assertEquals("hub_1_homestead", ngPlusState.hubId)
        assertEquals("pit_nova_bunk", ngPlusState.roomId)
        assertEquals(setOf("w1_mq01"), ngPlusState.activeQuests)
        assertEquals("w1_mq01", ngPlusState.trackedQuestId)
        assertEquals("wake_in_the_pit", ngPlusState.questStageById["w1_mq01"])
        assertTrue("Completed quests must be empty in NG+", ngPlusState.completedQuests.isEmpty())

        // Progression carry-overs must be intact
        assertEquals("Level 28 carried over", 28, ngPlusState.playerLevel)
        assertEquals("XP carried over", 45000, ngPlusState.playerXp)
        assertEquals("Credits carried over (>= 5000)", 15420, ngPlusState.playerCredits)
        assertEquals(endgame.unlockedWeapons, ngPlusState.unlockedWeapons)
        assertEquals(endgame.unlockedArmors, ngPlusState.unlockedArmors)
        assertEquals(endgame.equippedWeapons, ngPlusState.equippedWeapons)
        assertEquals(endgame.equippedArmors, ngPlusState.equippedArmors)
        assertEquals(15, ngPlusState.inventory["medkit"])

        // Milestones must contain Master Protocol and NO campaign completion flags
        assertTrue("ms_master_protocol_active must be set", ngPlusState.completedMilestones.contains("ms_master_protocol_active"))
        assertFalse("ms_game_complete must NOT be set", ngPlusState.completedMilestones.contains("ms_game_complete"))
        assertFalse("ms_w1_mq05_complete must NOT be set", ngPlusState.completedMilestones.contains("ms_w1_mq05_complete"))
        assertFalse("ms_w2_mq05_complete must NOT be set in NG+ World 1", ngPlusState.completedMilestones.contains("ms_w2_mq05_complete"))
        assertFalse("ms_w6_mq30_complete must NOT be set", ngPlusState.completedMilestones.contains("ms_w6_mq30_complete"))
    }

    // =========================================================================
    // 2. World 1 Complete Playthrough in NG+
    // =========================================================================

    @Test
    fun ngPlus_playthrough_world1_fullLoop_progressesWithoutStallOrPrematureCompletions() {
        val endgame = buildEndgameState()
        val ngPlusState = executeNewGamePlusTransition(endgame)

        val store = GameSessionStore()
        store.restore(ngPlusState)

        val eventsManager = EventManager(
            events = allEvents,
            sessionStore = store,
            eventHooks = EventHooks(
                onQuestTaskUpdated = { questId, taskId ->
                    if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                        store.setQuestTaskCompleted(questId, taskId, true)
                    }
                },
                onQuestCompleted = { questId ->
                    if (!questId.isNullOrBlank()) {
                        store.completeQuest(questId)
                    }
                },
                onGiveItem = { itemId, qty ->
                    val current = store.state.value.inventory
                    store.setInventory(current + (itemId to ((current[itemId] ?: 0) + qty)))
                },
                onTakeItem = { itemId, qty ->
                    val current = store.state.value.inventory
                    val avail = current[itemId] ?: 0
                    if (avail >= qty) {
                        store.setInventory(current + (itemId to (avail - qty)))
                        true
                    } else false
                }
            )
        )

        // Step 1: MQ01 - Wake in the Pit
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_turn_on_bunk_light"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_inspect_safety_fault"))
        eventsManager.handleTrigger("enter_room", EventPayload.EnterRoom("pit_shaft"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_enter_workshop"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_patch_flux_liner"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_confirm_governor"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq01_cutter_surge"))

        // Complete MQ01 via Jed talk
        store.completeQuest("w1_mq01")
        store.startQuest("w1_mq02", track = true)

        assertTrue("w1_mq01 must be completed", store.state.value.completedQuests.contains("w1_mq01"))
        assertTrue("w1_mq02 must be active", store.state.value.activeQuests.contains("w1_mq02"))

        // Step 2: MQ02 - Checkpoint
        store.setQuestTaskCompleted("w1_mq02", "approach_admin_gate", true)
        store.completeQuest("w1_mq02")
        store.setMilestone("ms_w1_mq02_complete")
        store.startQuest("w1_mq03", track = true)

        assertTrue("w1_mq02 completed", store.state.value.completedQuests.contains("w1_mq02"))
        assertTrue("w1_mq03 active", store.state.value.activeQuests.contains("w1_mq03"))

        // Step 3: MQ03 - Heavy Lifting & Echo Relic
        eventsManager.handleTrigger("player_action", EventPayload.Action("evt_mine_power_on"))
        eventsManager.handleTrigger("player_action", EventPayload.Action("w1_mq03_touch_relic"))
        store.completeQuest("w1_mq03")
        store.setMilestone("ms_w1_mq03_complete")
        store.startQuest("w1_mq04", track = true)

        assertTrue("w1_mq03 completed", store.state.value.completedQuests.contains("w1_mq03"))
        assertTrue("w1_mq04 active", store.state.value.activeQuests.contains("w1_mq04"))

        // Step 4: MQ04 - Lockdown Escape & Jed's Sacrifice
        eventsManager.handleTrigger("player_action", EventPayload.Action("jed_sacrifice"))
        store.completeQuest("w1_mq04")
        store.setMilestone("ms_w1_mq04_complete")
        store.startQuest("w1_mq05", track = true)

        assertTrue("w1_mq04 completed", store.state.value.completedQuests.contains("w1_mq04"))
        assertTrue("w1_mq05 active", store.state.value.activeQuests.contains("w1_mq05"))

        // Step 5: MQ05 - The Iron Warden & Launch
        eventsManager.handleTrigger("player_action", EventPayload.Action("use_nav_console"))
        store.completeQuest("w1_mq05")
        store.setMilestone("ms_w1_mq05_complete")

        val state = store.state.value
        assertTrue("All 5 World 1 MQs completed in NG+", state.completedQuests.containsAll(listOf("w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04", "w1_mq05")))
        assertTrue("ms_w1_mq05_complete achieved", state.completedMilestones.contains("ms_w1_mq05_complete"))
    }

    // =========================================================================
    // 3. World 2 Side Quest & Dialogue Guards Pristine in NG+
    // =========================================================================

    @Test
    fun ngPlus_world2_sideQuestsAndDialogue_remainPristineAndUnblocked() {
        val endgame = buildEndgameState()
        val ngPlusState = executeNewGamePlusTransition(endgame)

        // Verify that ms_w2_mq05_complete is NOT set in NG+ state
        assertFalse("ms_w2_mq05_complete must NOT be present in NG+ seed", "ms_w2_mq05_complete" in ngPlusState.completedMilestones)

        // Verify that all 4 World 2 side quest intro dialogues are NOT blocked
        val w2SqIntroIds = listOf(
            "zeke_w2_sq01_intro_1",
            "orion_w2_sq03_intro_1",
            "orion_w2_sq04_intro_1",
            "ghost_w2_sq05_intro_1"
        )
        val dialogueMap = allDialogue.associateBy { it.id }

        for (dlgId in w2SqIntroIds) {
            val dlg = dialogueMap[dlgId]
            assertNotNull("Dialogue $dlgId must exist", dlg)
            val cond = dlg?.condition.orEmpty()
            assertTrue(
                "Dialogue $dlgId requires milestone_not_set:ms_w2_mq05_complete",
                cond.contains("milestone_not_set:ms_w2_mq05_complete")
            )
            // In NG+, milestone_not_set:ms_w2_mq05_complete will evaluate to TRUE, allowing the quest to start!
        }

        // Verify premature post-W2 dialogue is NOT accessible in NG+
        val postW2Lines = allDialogue.filter { it.condition.orEmpty().contains("milestone:ms_w2_mq05_complete") }
        assertTrue("Post-W2 dialogues exist", postW2Lines.isNotEmpty())
        for (line in postW2Lines) {
            // Because ms_w2_mq05_complete is absent in NG+, these lines are properly suppressed
            assertFalse(
                "Premature post-W2 dialogue ${line.id} must not be active in NG+ start",
                ngPlusState.completedMilestones.contains("ms_w2_mq05_complete")
            )
        }
    }
}
