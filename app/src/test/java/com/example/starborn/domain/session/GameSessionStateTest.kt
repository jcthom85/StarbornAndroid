package com.example.starborn.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GameSessionStateTest {

    @Test
    fun fingerprintStableWhenQuestOrderingChanges() {
        val baseline = GameSessionState(
            worldId = "world_a",
            hubId = "hub_a",
            roomId = "room_1",
            activeQuests = setOf("quest_intro", "quest_second"),
            questStageById = mapOf("quest_intro" to "stage_one"),
            inventory = mapOf("item_health" to 2, "item_core" to 1)
        )
        val reordered = baseline.copy(
            activeQuests = setOf("quest_second", "quest_intro"),
            inventory = mapOf("item_core" to 1, "item_health" to 2)
        )

        assertEquals(baseline.fingerprint(), reordered.fingerprint())
    }

    @Test
    fun fingerprintChangesWhenStateDiffers() {
        val stateA = GameSessionState(
            worldId = "world_a",
            hubId = "hub_a",
            roomId = "room_1",
            playerCredits = 120,
            questStageById = mapOf("quest_intro" to "stage_one"),
            questTasksCompleted = mapOf("quest_intro" to setOf("task_a")),
            inventory = mapOf("item_health" to 2)
        )
        val stateB = stateA.copy(
            roomId = "room_2",
            questStageById = mapOf("quest_intro" to "stage_two"),
            questTasksCompleted = mapOf("quest_intro" to setOf("task_a", "task_b"))
        )

        assertNotEquals(stateA.fingerprint(), stateB.fingerprint())
    }
}
