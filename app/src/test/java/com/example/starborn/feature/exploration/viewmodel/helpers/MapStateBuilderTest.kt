package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.Room
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStateBuilderTest {
    @Test fun previewAndDarkRoomsHideServicesThreatsAndHubDestinations() {
        val current = room("current", mapOf("east" to "preview"), listOf(0, 0))
        val preview = room("preview", emptyMap(), listOf(1, 0))
        val exit = com.example.starborn.feature.exploration.viewmodel.MapHubExitUi("Homestead Quarter", true)
        val service = com.example.starborn.feature.exploration.viewmodel.MinimapService.EXIT
        val mini = MapStateBuilder.buildMinimapState(current, listOf(current, preview), setOf("current"), emptySet(),
            { false }, { true }, { emptySet() }, { setOf(service) }, hubExit = { exit })
        assertEquals(exit, mini.cells.first { it.isCurrent }.hubExit)
        val unknown = mini.cells.first { it.isPreview }
        assertTrue(unknown.services.isEmpty())
        assertFalse(unknown.hasEnemies)
        assertNull(unknown.hubExit)
        assertEquals("Unexplored room", unknown.title)
        val full = MapStateBuilder.buildFullMapState(current, listOf(current), setOf("current"), emptySet(),
            { true }, { true }, { emptySet() }, { setOf(service) }, hubExit = { exit })
        assertTrue(full.cells.single().services.isEmpty())
        assertFalse(full.cells.single().hasEnemies)
        assertNull(full.cells.single().hubExit)
        assertEquals("Dark room", full.cells.single().title)
    }

    @Test fun restAndCookingAreSeparateServices() {
        val shelf = com.example.starborn.domain.model.RestStopAction("sleeping shelf")
        val rest = com.example.starborn.feature.exploration.viewmodel.MinimapService.REST
        val cooking = com.example.starborn.feature.exploration.viewmodel.MinimapService.COOKING
        assertEquals(setOf(rest), mapServicesForActions(listOf(shelf), false, false))
        assertEquals(setOf(rest, cooking), mapServicesForActions(listOf(shelf.copy(cookSource = "campfire")), false, false))
    }
    @Test
    fun externalExitsStayVisibleWhenBlockedWithoutAddingRemoteRooms() {
        val current = room("current", mapOf("north" to "remote"), listOf(0, 0))
        val exit = com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi("north", "mine", "Deep Mine")
        for (blocked in listOf(true, false)) {
            val mini = MapStateBuilder.buildMinimapState(current, listOf(current), setOf("current"), emptySet(),
                { false }, { false }, { if (blocked) setOf("north") else emptySet() }, { emptySet() }, { listOf(exit) })
            val full = MapStateBuilder.buildFullMapState(current, listOf(current), setOf("current"), emptySet(),
                { false }, { false }, { if (blocked) setOf("north") else emptySet() }, { emptySet() }, { listOf(exit) })
            assertEquals(listOf("current"), mini.cells.map { it.roomId })
            assertEquals(listOf("current"), full.cells.map { it.roomId })
            assertEquals(exit.copy(blocked = blocked), mini.cells.single().nodeExits.single())
            assertEquals(mini.cells.single().nodeExits, full.cells.single().nodeExits)
        }
    }

    @Test
    fun undiscoveredPreviewAndDarkRoomsDoNotRevealTheirExternalExits() {
        val current = room("current", mapOf("east" to "preview"), listOf(0, 0))
        val preview = room("preview", mapOf("north" to "remote"), listOf(1, 0))
        val hidden = room("hidden", mapOf("west" to "remote"), listOf(-1, 0))
        val exit = com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi("north", "mine", "Deep Mine")
        val mini = MapStateBuilder.buildMinimapState(current, listOf(current, preview, hidden), setOf("current"), emptySet(),
            { false }, { false }, { emptySet() }, { emptySet() }, { listOf(exit) })
        assertTrue(mini.cells.single { it.roomId == "preview" }.nodeExits.isEmpty())
        assertFalse(mini.cells.any { it.roomId == "hidden" })
        val dark = MapStateBuilder.buildFullMapState(current, listOf(current), setOf("current"), emptySet(),
            { true }, { false }, { emptySet() }, { emptySet() }, { listOf(exit) })
        assertTrue(dark.cells.single().nodeExits.isEmpty())
    }

    @Test
    fun reciprocalDirectionDoesNotShareAnUnrelatedDestinationsGate() {
        val source = room("source", mapOf("east" to "target"), listOf(0, 0))
        val target = room("target", mapOf("west" to "other", "south" to "source"), listOf(1, 0))
        assertNull(MapStateBuilder.reciprocalDirection(source, "east", target))
        assertEquals("west", MapStateBuilder.reciprocalDirection(
            source, "east", target.copy(connections = mapOf("west" to "source"))
        ))
        assertNull(MapStateBuilder.reciprocalDirection(source, "north", target))
    }

    @Test
    fun oneWayArrivalDoesNotInventAReturnHint() {
        val source = room("source", mapOf("east" to "target"), listOf(0, 0))
        val target = room("target", mapOf("west" to "other"), listOf(1, 0))
        val state = MapStateBuilder.buildMinimapState(
            currentRoom = source, roomsInContext = listOf(source, target),
            visitedRooms = setOf("source"), discoveredRooms = emptySet(),
            isRoomDark = { false }, roomHasEnemies = { false },
            computeBlockedDirections = { emptySet() }, parseRoomServices = { emptySet() }
        )
        assertTrue(state.cells.single { it.roomId == "target" }.pathHints.isEmpty())
    }

    @Test
    fun buildMinimapState_showsOpenCurrentRoomNeighborsAsPreviewWithoutDiscoveringThem() {
        val current = room(
            id = "current",
            connections = mapOf("east" to "east_room"),
            pos = listOf(0, 0)
        )
        val eastRoom = room(
            id = "east_room",
            connections = mapOf("west" to "current"),
            pos = listOf(1, 0)
        )

        val state = MapStateBuilder.buildMinimapState(
            currentRoom = current,
            roomsInContext = listOf(current, eastRoom),
            visitedRooms = setOf("current"),
            discoveredRooms = setOf("current"),
            isRoomDark = { false },
            roomHasEnemies = { false },
            computeBlockedDirections = { emptySet() },
            parseRoomServices = { emptySet() }
        )

        val preview = state.cells.single { it.roomId == "east_room" }
        assertTrue(preview.isPreview)
        assertFalse(preview.visited)
        assertFalse(preview.discovered)
    }

    @Test
    fun buildMinimapState_doesNotPreviewBlockedCurrentRoomNeighbors() {
        val current = room(
            id = "current",
            connections = mapOf("east" to "east_room"),
            pos = listOf(0, 0)
        )
        val eastRoom = room(
            id = "east_room",
            connections = mapOf("west" to "current"),
            pos = listOf(1, 0)
        )

        val state = MapStateBuilder.buildMinimapState(
            currentRoom = current,
            roomsInContext = listOf(current, eastRoom),
            visitedRooms = setOf("current"),
            discoveredRooms = setOf("current"),
            isRoomDark = { false },
            roomHasEnemies = { false },
            computeBlockedDirections = { room -> if (room.id == "current") setOf("east") else emptySet() },
            parseRoomServices = { emptySet() }
        )

        assertFalse(state.cells.any { it.roomId == "east_room" })
    }

    private fun room(
        id: String,
        connections: Map<String, String>,
        pos: List<Int>
    ): Room = Room(
        id = id,
        env = "test",
        title = id,
        backgroundImage = "",
        description = "",
        npcs = emptyList(),
        items = emptyList(),
        enemies = emptyList(),
        connections = connections,
        pos = pos,
        state = emptyMap(),
        actions = emptyList()
    )
}
