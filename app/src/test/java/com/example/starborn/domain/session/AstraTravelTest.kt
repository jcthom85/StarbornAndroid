package com.example.starborn.domain.session

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import org.junit.Assert.*
import org.junit.Test

class AstraTravelTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))

    @Test fun `ship rooms share one node and all reach the cargo ramp`() {
        val ship = assets.loadHubNodes().single { it.hubId == AstraTravel.HUB_ID }
        val rooms = assets.loadRooms().associateBy { it.id }
        assertEquals(AstraTravel.NODE_ID, ship.id)
        assertEquals(AstraTravel.ENTRY_ROOM_ID, ship.entryRoom)
        assertEquals(5, ship.rooms.size)
        ship.rooms.forEach { start ->
            val reached = mutableSetOf(start)
            val queue = ArrayDeque<String>().apply { add(start) }
            while (queue.isNotEmpty()) {
                rooms.getValue(queue.removeFirst()).connections.values.forEach { next ->
                    assertTrue("Ship connection leaves its node: $next", next in ship.rooms)
                    if (reached.add(next)) queue.add(next)
                }
            }
            assertEquals(ship.rooms.toSet(), reached)
        }
        assertTrue(rooms.getValue(ship.entryRoom).actions.any { it["action_event"] == "astra_disembark" })
        assertFalse(assets.loadNodeTransitions().any { it.fromNode == ship.id || it.toNode == ship.id })
    }

    @Test fun `navigation targets match authored world hub node and room ownership`() {
        val hubs = assets.loadHubs().associateBy { it.id }
        val nodes = assets.loadHubNodes().associateBy { it.id }
        val rooms = assets.loadRooms().map { it.id }.toSet()
        AstraTravel.destinations.forEach { target ->
            assertEquals(target.worldId, hubs.getValue(target.hubId).worldId)
            assertEquals(target.hubId, nodes.getValue(target.nodeId).hubId)
            assertTrue(target.roomId in nodes.getValue(target.nodeId).rooms)
            assertTrue(target.roomId in rooms)
        }
    }

    @Test fun `story milestones unlock routes without revealing future worlds`() {
        val spire = GameSessionState(completedMilestones = setOf("ms_w2_mq05_complete"))
        assertEquals(listOf("world_1", "world_2", "world_3"), AstraTravel.availableDestinations(spire).map { it.worldId })
        val foundry = spire.copy(completedMilestones = spire.completedMilestones + "ms_w3_mq15_complete")
        assertEquals("world_4", AstraTravel.availableDestinations(foundry).last().worldId)
        val orbit = spire.copy(completedQuests = setOf("w4_mq20"))
        assertEquals("world_5", AstraTravel.availableDestinations(orbit).last().worldId)
        val source = spire.copy(completedMilestones = setOf("ms_w5_mq25_complete"))
        assertEquals(6, AstraTravel.availableDestinations(source).size)
    }

    @Test fun `disembark preserves exact boarding room and repairs inconsistent location ids`() {
        val state = GameSessionState(
            astraReturnWorldId = "world_astra", astraReturnHubId = "hub_astra",
            astraReturnRoomId = "spire_laundry_service"
        )
        val dock = AstraTravel.dockingLocation(state, assets.loadHubs(), assets.loadHubNodes())
        assertEquals("spire_laundry_service", dock.roomId)
        assertEquals("world_3", dock.worldId)
        assertEquals("hub_6_upper_city", dock.hubId)
    }

    @Test fun `missing invalid and self referencing docks recover to an unlocked landing`() {
        listOf(null, "removed_room", "astra_common_room").forEach { room ->
            val state = GameSessionState(
                worldId = "world_astra", hubId = "hub_astra", roomId = "astra_quarters",
                completedMilestones = setOf("ms_w3_mq15_complete"), astraReturnRoomId = room
            )
            val dock = AstraTravel.dockingLocation(state, assets.loadHubs(), assets.loadHubNodes())
            assertEquals("foundry_slag_landing", dock.roomId)
            assertEquals("world_4", dock.worldId)
        }
    }
}
