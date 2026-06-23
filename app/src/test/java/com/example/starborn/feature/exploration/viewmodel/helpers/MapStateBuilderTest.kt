package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.Room
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStateBuilderTest {
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
