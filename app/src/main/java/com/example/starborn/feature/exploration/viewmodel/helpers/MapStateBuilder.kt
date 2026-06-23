package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import com.example.starborn.feature.exploration.viewmodel.MinimapService
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.FullMapUiState
import java.util.Locale
import kotlin.math.abs

object MapStateBuilder {
    fun buildMinimapState(
        currentRoom: Room,
        roomsInContext: List<Room>,
        visitedRooms: Set<String>,
        discoveredRooms: Set<String>,
        isRoomDark: (Room) -> Boolean,
        roomHasEnemies: (Room) -> Boolean,
        computeBlockedDirections: (Room) -> Set<String>,
        parseRoomServices: (Room) -> Set<MinimapService>
    ): MinimapUiState {
        val currentPos = roomPosition(currentRoom)
        val currentBlocked = computeBlockedDirections(currentRoom)
        val openConnections = currentRoom.connections.mapNotNull { (direction, targetId) ->
            val normalized = direction.lowercase(Locale.getDefault())
            val dest = targetId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (normalized in currentBlocked) return@mapNotNull null
            normalized to dest
        }.toMap()

        val cells = roomsInContext.mapNotNull { room ->
            val pos = roomPosition(room)
            val dx = pos.first - currentPos.first
            val dy = pos.second - currentPos.second
            if (abs(dx) > 2 || abs(dy) > 2) return@mapNotNull null
            val isPreview = openConnections.values.contains(room.id) &&
                !visitedRooms.contains(room.id) &&
                !discoveredRooms.contains(room.id) &&
                !isRoomDark(room)
            val isVisible = room.id == currentRoom.id ||
                visitedRooms.contains(room.id) ||
                discoveredRooms.contains(room.id) ||
                isPreview
            if (!isVisible) return@mapNotNull null
            val connections = room.connections.mapNotNull { (direction, targetId) ->
                targetId?.let { direction.lowercase(Locale.getDefault()) to it }
            }.toMap()
            val blocked = computeBlockedDirections(room)
            val pathHints = when (room.id) {
                currentRoom.id -> openConnections.keys
                else -> {
                    val incoming = openConnections.entries.firstOrNull { it.value == room.id }?.key
                    incoming?.let { listOfNotNull(oppositeDirection(it)).toSet() } ?: emptySet()
                }
            }
            val services = parseRoomServices(room)
            MinimapCellUi(
                roomId = room.id,
                offsetX = dx,
                offsetY = dy,
                gridX = pos.first,
                gridY = pos.second,
                visited = visitedRooms.contains(room.id),
                discovered = discoveredRooms.contains(room.id),
                isCurrent = room.id == currentRoom.id,
                hasEnemies = roomHasEnemies(room),
                blockedDirections = blocked,
                connections = connections,
                pathHints = pathHints,
                services = services,
                isDark = isRoomDark(room),
                isPreview = isPreview
            )
        }
        return MinimapUiState(cells = cells)
    }

    fun buildFullMapState(
        currentRoom: Room,
        roomsInContext: List<Room>,
        visitedRooms: Set<String>,
        discoveredRooms: Set<String>,
        isRoomDark: (Room) -> Boolean,
        roomHasEnemies: (Room) -> Boolean,
        computeBlockedDirections: (Room) -> Set<String>,
        parseRoomServices: (Room) -> Set<MinimapService>
    ): FullMapUiState {
        val currentPos = roomPosition(currentRoom)
        val cells = roomsInContext.mapNotNull { room ->
            val isVisible = room.id == currentRoom.id ||
                visitedRooms.contains(room.id) ||
                discoveredRooms.contains(room.id)
            if (!isVisible) return@mapNotNull null
            val pos = roomPosition(room)
            val connections = room.connections.mapNotNull { (direction, targetId) ->
                targetId?.let { direction.lowercase(Locale.getDefault()) to it }
            }.toMap()
            val blocked = computeBlockedDirections(room)
            val services = parseRoomServices(room)
            MinimapCellUi(
                roomId = room.id,
                offsetX = pos.first - currentPos.first,
                offsetY = pos.second - currentPos.second,
                gridX = pos.first,
                gridY = pos.second,
                visited = visitedRooms.contains(room.id),
                discovered = discoveredRooms.contains(room.id),
                isCurrent = room.id == currentRoom.id,
                hasEnemies = roomHasEnemies(room),
                blockedDirections = blocked,
                connections = connections,
                services = services,
                isDark = isRoomDark(room)
            )
        }
        return FullMapUiState(cells = cells)
    }

    fun roomPosition(room: Room): Pair<Int, Int> {
        val x = room.pos.getOrNull(0) ?: 0
        val y = room.pos.getOrNull(1) ?: 0
        return Pair(x, y)
    }

    fun oppositeDirection(direction: String): String? = when (direction.lowercase(Locale.getDefault())) {
        "north" -> "south"
        "south" -> "north"
        "east" -> "west"
        "west" -> "east"
        "northeast" -> "southwest"
        "southwest" -> "northeast"
        "southeast" -> "northwest"
        "northwest" -> "southeast"
        "up" -> "down"
        "down" -> "up"
        else -> null
    }
}
