package com.example.starborn.debug

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.*
import org.junit.Test

class DebugFixtureValidatorTest {
    private val hubs = listOf(Hub("hub", "world", "Hub", "", "", true))
    private val nodes = listOf(HubNode("node", "hub", "Node", "room", rooms = listOf("room")))
    private val baseline = GameSessionState(worldId = "world", hubId = "hub", roomId = "room",
        playerId = "nova", partyMembers = listOf("nova"))

    @Test fun validIdsInTheWrongLocationAreRejected() {
        val failures = DebugFixtureValidator.validate(baseline.copy(worldId = "other", roomId = "elsewhere"),
            hubs, nodes, setOf("room", "elsewhere"), emptyList(), emptySet())
        assertTrue(failures.any { "does not belong" in it })
        assertTrue(failures.any { "not in hub" in it })
    }

    @Test fun freshFixtureCannotInheritRecoveryOrInvalidInventory() {
        val failures = DebugFixtureValidator.validate(baseline.copy(pendingBattleJson = "old battle",
            inventory = mapOf("known" to -1)), hubs, nodes, setOf("room"), emptyList(), setOf("known"))
        assertEquals(2, failures.size)
        assertTrue(DebugFixtureValidator.validate(baseline, hubs, nodes, setOf("room"), emptyList(), emptySet()).isEmpty())
    }
}
