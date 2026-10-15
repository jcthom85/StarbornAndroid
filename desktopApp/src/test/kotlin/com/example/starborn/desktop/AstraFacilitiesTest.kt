package com.example.starborn.desktop

import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.presentation.*
import org.junit.Assert.*
import org.junit.Test

class AstraFacilitiesTest {
    @Test fun simulationsRespectDiscoveryAndNeverUnlockWardenBeforeItsQuest() {
        val fresh = AstraCatalog.availableSimulations(GameSessionState())
        assertEquals(setOf("sentinel_mki", "faulted_loader"), fresh.flatMap { it.enemyIds }.toSet())
        assertFalse(AstraCatalog.availableSimulations(GameSessionState(completedQuests = setOf("w1_mq04")))
            .any { "the_iron_warden" in it.enemyIds })
        assertTrue(AstraCatalog.availableSimulations(GameSessionState(completedMilestones = setOf("ms_w1_warden_defeated")))
            .any { "the_iron_warden" in it.enemyIds })
        val complete = GameSessionState(completedQuests = setOf("w1_mq05", "w2_mq05", "w3_mq15", "w4_mq20", "w5_mq25", "w6_mq30"))
        assertEquals(AstraCatalog.simulations, AstraCatalog.availableSimulations(complete))
    }

    @Test fun newCrewCheckinsPersistAndChangeWithStoryWithoutShowingAbsentCrew() {
        val first = astraFacility("crew", GameSessionState(partyMembers = listOf("nova")), { null })
        assertEquals(1, first.entries.size)
        assertTrue(first.entries.first().isNew)
        val read = GameSessionState(roomStates = mapOf("astra_common_room" to mapOf("table_checkin_1" to true)))
        assertFalse(astraFacility("crew", read, { null }).entries.first().isNew)
        val later = astraFacility("crew", read.copy(completedQuests = setOf("w3_mq15"), partyMembers = listOf("nova", "zeke")), { null })
        assertTrue(later.entries.first().isNew)
        assertNotEquals(first.entries.first().body, later.entries.first().body)
        assertEquals(2, later.entries.size)
    }

    @Test fun signalArchiveHidesFutureStoryEntries() {
        val initial = astraFacility("chime", GameSessionState(), { null }).entries
        assertFalse(initial.any { it.title == "Elara" })
        val recovered = astraFacility("chime", GameSessionState(completedMilestones = setOf("ms_w5_elara_found")), { null }).entries
        assertTrue(recovered.any { it.title == "Elara" })
        assertFalse(recovered.any { it.title == "A voice of her own" })
    }
}
