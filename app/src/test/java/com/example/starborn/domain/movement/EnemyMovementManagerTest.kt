package com.example.starborn.domain.movement

import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnemyMovementManagerTest {

    @Test
    fun patrolMovesAlongRouteAndReversesAtEndpoint() {
        val manager = manager()
        manager.restore(emptyMap(), activeSession())

        manager.tick(25_000, "junction", activeSession())
        assertEquals("sifter", manager.stateSnapshot().getValue("patrol").roomId)

        manager.tick(25_000, "junction", activeSession())
        assertEquals("shoring", manager.stateSnapshot().getValue("patrol").roomId)

        manager.tick(25_000, "junction", activeSession())
        assertEquals("sifter", manager.stateSnapshot().getValue("patrol").roomId)
    }

    @Test
    fun aggressivePartyCountsDownAndAutoEngages() {
        val manager = manager(startRoom = "junction")
        manager.restore(emptyMap(), activeSession())

        val first = manager.tick(4_000, "junction", activeSession())
        assertEquals(6_000L, first.aggression?.remainingMs)
        assertNull(first.autoEngagePartyId)

        val second = manager.tick(6_000, "junction", activeSession())
        assertEquals("patrol", second.autoEngagePartyId)
    }

    @Test
    fun leavingRoomResetsAggressionCountdown() {
        val manager = manager(startRoom = "junction")
        manager.restore(emptyMap(), activeSession())
        manager.tick(4_000, "junction", activeSession())

        manager.onPlayerRoomChanged("conveyor", activeSession())
        val returned = manager.onPlayerRoomChanged("junction", activeSession())

        assertEquals(10_000L, returned.aggression?.remainingMs)
    }

    @Test
    fun retreatGraceSuppressesAggressionUntilExpired() {
        val manager = manager(startRoom = "junction")
        manager.restore(emptyMap(), activeSession())
        manager.applyRetreatGrace("patrol", 15_000)

        assertNull(manager.tick(14_000, "junction", activeSession()).aggression)
        assertEquals(10_000L, manager.tick(1_000, "junction", activeSession()).aggression?.remainingMs)
    }

    @Test
    fun defeatedPartyIsRemovedAndPersistedStateRestoresExactly() {
        val manager = manager()
        manager.restore(emptyMap(), activeSession())
        manager.tick(7_000, "junction", activeSession())
        manager.markDefeated("patrol")
        val snapshot = manager.stateSnapshot()

        val restored = manager()
        restored.restore(snapshot, activeSession())

        assertTrue(restored.stateSnapshot().getValue("patrol").defeated)
        assertTrue(restored.partiesInRoom("conveyor", activeSession()).isEmpty())
    }

    @Test
    fun inactiveQuestDoesNotExposeOrAdvanceParty() {
        val manager = manager()
        manager.restore(emptyMap(), GameSessionState())

        val result = manager.tick(30_000, "conveyor", GameSessionState())

        assertTrue(manager.partiesInRoom("conveyor", GameSessionState()).isEmpty())
        assertFalse(result.states.containsKey("patrol"))
    }

    @Test
    fun patrolWaitsWhenDestinationAlreadyHasFiveEnemyParties() {
        val manager = manager(authoredPartiesByRoom = mapOf("sifter" to 5))
        manager.restore(emptyMap(), activeSession())

        manager.tick(25_000, "junction", activeSession())

        assertEquals("conveyor", manager.stateSnapshot().getValue("patrol").roomId)
    }

    private fun manager(startRoom: String = "conveyor"): EnemyMovementManager {
        val route = listOf("conveyor", "sifter", "shoring")
        val rooms = listOf(
            room("junction", mapOf("east" to "conveyor")),
            room("conveyor", mapOf("west" to "junction", "north" to "sifter")),
            room("sifter", mapOf("south" to "conveyor", "north" to "shoring")),
            room("shoring", mapOf("south" to "sifter"))
        )
        return EnemyMovementManager(
            EnemyMovementCatalog(
                zones = listOf(EnemyMovementZone("zone", route)),
                parties = listOf(
                    EnemyMovementParty(
                        id = "patrol",
                        zoneId = "zone",
                        enemies = listOf("pressure_hauler"),
                        startRoom = startRoom,
                        route = route,
                        behavior = "patrol",
                        aggression = "aggressive",
                        moveIntervalSeconds = 25,
                        engageDelaySeconds = 10,
                        requiresActiveQuest = "quest"
                    )
                )
            ),
            rooms
        )
    }

    private fun manager(
        startRoom: String = "conveyor",
        authoredPartiesByRoom: Map<String, Int>
    ): EnemyMovementManager {
        val route = listOf("conveyor", "sifter", "shoring")
        val rooms = listOf(
            room("junction", mapOf("east" to "conveyor")),
            room("conveyor", mapOf("west" to "junction", "north" to "sifter")),
            room(
                "sifter",
                mapOf("south" to "conveyor", "north" to "shoring"),
                authoredEnemyPartyCount = authoredPartiesByRoom["sifter"] ?: 0
            ),
            room("shoring", mapOf("south" to "sifter"))
        )
        return EnemyMovementManager(
            EnemyMovementCatalog(
                zones = listOf(EnemyMovementZone("zone", route)),
                parties = listOf(
                    EnemyMovementParty(
                        id = "patrol",
                        zoneId = "zone",
                        enemies = listOf("pressure_hauler"),
                        startRoom = startRoom,
                        route = route,
                        behavior = "patrol",
                        aggression = "aggressive",
                        moveIntervalSeconds = 25,
                        engageDelaySeconds = 10,
                        requiresActiveQuest = "quest"
                    )
                )
            ),
            rooms
        )
    }

    private fun activeSession() = GameSessionState(activeQuests = setOf("quest"))

    private fun room(
        id: String,
        connections: Map<String, String>,
        authoredEnemyPartyCount: Int = 0
    ) = Room(
        id = id,
        env = "mine",
        title = id,
        backgroundImage = "",
        description = "",
        npcs = emptyList(),
        items = emptyList(),
        enemies = emptyList(),
        enemyParties = List(authoredEnemyPartyCount) { index -> listOf("authored_$index") },
        connections = connections,
        pos = listOf(0, 0),
        state = emptyMap(),
        actions = emptyList()
    )
}
