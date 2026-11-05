package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.viewmodel.helpers.MapStateBuilder
import com.squareup.moshi.Types
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NavigationIntegrityTest {
    companion object {
        // All playable local links now have direct opposite-direction returns.
        val oneWayExits = emptyMap<String, String>()
        val namedVerticalExits = mapOf(
            ("spire_underrail_platform" to "down") to "spire_transit_plaza",
            ("spire_transit_plaza" to "up") to "spire_underrail_platform",
            ("spire_service_lift" to "down") to "spire_archive_vault",
            ("spire_archive_vault" to "up") to "spire_service_lift",
            ("orbital_service_shaft" to "down") to "orbital_server_farm",
            ("orbital_server_farm" to "up") to "orbital_service_shaft",
            ("deep_firewall_gamma" to "down") to "deep_anchor_chamber",
            ("deep_anchor_chamber" to "up") to "deep_firewall_gamma"
        )
    }

    private val assets = listOf(File("app/src/main/assets"), File("src/main/assets"))
        .first { it.isDirectory }
    private val rooms: Map<String, Room> by lazy {
        val type = Types.newParameterizedType(List::class.java, Room::class.java)
        MoshiProvider.instance.adapter<List<Room>>(type)
            .fromJson(File(assets, "rooms.json").readText()).orEmpty().associateBy { it.id }
    }
    private val vectors = mapOf(
        "north" to (0 to 1), "south" to (0 to -1), "east" to (1 to 0), "west" to (-1 to 0),
        "northeast" to (1 to 1), "southwest" to (-1 to -1),
        "northwest" to (-1 to 1), "southeast" to (1 to -1)
    )

    @Test
    fun everyExitReturnsInTheOppositeDirectionExceptNamedShortcuts() {
        val exceptions = mutableMapOf<String, String>()
        rooms.values.filterNot { it.id == "launch_rail" }.forEach { source ->
            source.connections.forEach { (direction, targetId) ->
                val target = rooms.getValue(targetId)
                if (MapStateBuilder.reciprocalDirection(source, direction, target) == null) {
                    exceptions[source.id] = targetId
                    assertEquals("${source.id} must use an explicit descent", "down", direction)
                    assertTrue(source.specialExits.getValue(direction).contains("one way"))
                    assertTrue("$targetId needs an escape route back to ${source.id}", canReach(targetId, source.id))
                }
            }
        }
        assertEquals(oneWayExits, exceptions)
    }

    @Test
    fun worldThreeUsesCardinalWalkingAndReviewedVerticalAlternatives() {
        val spire = rooms.values.filter { it.id.startsWith("spire_") }
        val opposite = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        assertEquals("Keep all existing World 3 content", 70, spire.size)
        spire.forEach { room ->
            assertTrue("${room.id} must expose at most four swipe exits", room.connections.keys.count { it in opposite } in 1..4)
            val alternatives = room.connections.filterKeys { it !in opposite }
            assertTrue("${room.id} has an unreviewed alternative", alternatives.all { (d, t) -> namedVerticalExits[room.id to d] == t })
            assertEquals("Only named alternatives hide compass arrows", alternatives.keys, room.specialExits.keys)
            room.connections.forEach { (direction, destination) ->
                assertEquals("${room.id}:$direction needs a direct swipe back", room.id,
                    rooms.getValue(destination).connections[if (direction == "down") "up" else if (direction == "up") "down" else opposite.getValue(direction)])
            }
        }
    }

    @Test
    fun bothWorldThreeFloorsRemainFullyWalkableWithoutBypassingTheHeistTransfer() {
        val nodes = JSONArray(File(assets, "hub_nodes.json").readText())
        for ((hub, entrance) in listOf("hub_5_lower_city" to "spire_sewers_landing", "hub_6_upper_city" to "spire_laundry_service")) {
            val expected = (0 until nodes.length()).map { nodes.getJSONObject(it) }
                .filter { it.getString("hub_id") == hub }
                .flatMap { node -> node.getJSONArray("rooms").let { list -> (0 until list.length()).map { list.getString(it) } } }.toSet()
            val visited = mutableSetOf<String>()
            val pending = ArrayDeque<String>().apply { add(entrance) }
            while (pending.isNotEmpty()) {
                val id = pending.removeFirst()
                if (visited.add(id)) pending.addAll(rooms.getValue(id).connections.filterKeys { it in setOf("north", "south", "east", "west") }.values)
            }
            assertEquals("$hub must be one complete physical floor, with no premature route to the other floor", expected, visited)
        }
    }

    @Test
    fun worldFourRoutesAreCardinalReciprocalAndKeepEveryRoom() {
        val foundry = rooms.values.filter { it.id.startsWith("foundry_") }
        val opposite = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        assertEquals(74, foundry.size)
        foundry.forEach { room ->
            assertTrue("${room.id} must fit NSEW", room.connections.size in 1..4)
            assertTrue("${room.id} must use swipe directions", opposite.keys.containsAll(room.connections.keys))
            assertTrue("${room.id} must expose its compass exits", room.specialExits.isEmpty())
            room.connections.forEach { (direction, target) ->
                assertEquals("${room.id}:$direction must have an immediate return", room.id,
                    rooms.getValue(target).connections[opposite.getValue(direction)])
            }
        }
        val visited = mutableSetOf<String>()
        val pending = ArrayDeque<String>().apply { add("foundry_slag_landing") }
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (visited.add(id)) pending.addAll(rooms.getValue(id).connections.values)
        }
        assertEquals(foundry.map { it.id }.toSet(), visited)
        for ((source, direction) in listOf("foundry_pipe_gallery" to "east", "foundry_reject_bay" to "west")) {
            val gate = requireNotNull(rooms.getValue(source).blockedDirections?.get(direction))
            assertEquals("lock", gate.type)
            assertTrue(gate.requires.orEmpty().any { it.roomId == "foundry_pipe_gallery" && it.stateKey == "assembly_route_open" && it.value })
        }
    }

    @Test
    fun worldFiveKeepsCardinalWalkingAndReviewedVerticalAlternatives() {
        val ring = rooms.values.filter { it.id.startsWith("orbital_") || it.id.startsWith("deep_") }
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        assertEquals(81, ring.size)
        ring.forEach { room ->
            assertTrue("${room.id} must fit NSEW", room.connections.keys.count { it in reverse } in 1..4)
            val alternatives = room.connections.filterKeys { it !in reverse }
            assertTrue("${room.id} has an unreviewed alternative", alternatives.all { (d, t) -> namedVerticalExits[room.id to d] == t })
            assertEquals(alternatives.keys, room.specialExits.keys)
            room.connections.forEach { (d, t) -> assertEquals("${room.id}:$d must return immediately", room.id,
                rooms.getValue(t).connections[if (d == "down") "up" else if (d == "up") "down" else reverse.getValue(d)]) }
        }
        val visited = mutableSetOf<String>(); val pending = ArrayDeque<String>().apply { add("orbital_executive_dock") }
        while (pending.isNotEmpty()) { val id = pending.removeFirst(); if (visited.add(id)) pending.addAll(rooms.getValue(id).connections.filterKeys { it in reverse }.values) }
        assertEquals(ring.map { it.id }.toSet(), visited)
        for ((room, direction) in listOf("orbital_server_farm" to "east", "deep_anchor_chamber" to "west")) {
            val gate = requireNotNull(rooms.getValue(room).blockedDirections?.get(direction))
            assertTrue(gate.requires.orEmpty().any { it.roomId == "deep_anchor_chamber" && it.stateKey == "firewall_access_open" && it.value })
        }
    }

    @Test
    fun worldSixUsesReciprocalCardinalPathsAndPreservesAllRooms() {
        val source = rooms.values.filter { it.id.startsWith("source_") }
        val reverse = mapOf("north" to "south", "south" to "north", "east" to "west", "west" to "east")
        assertEquals(65, source.size)
        source.forEach { room ->
            assertTrue("${room.id} needs swipeable exits", room.connections.size in 1..4 && reverse.keys.containsAll(room.connections.keys))
            assertTrue(room.specialExits.isEmpty())
            room.connections.forEach { (d, to) -> assertEquals("${room.id}:$d needs a return", room.id, rooms.getValue(to).connections[reverse.getValue(d)]) }
        }
        val visited = mutableSetOf<String>(); val pending = ArrayDeque<String>().apply { add("source_campfire") }
        while (pending.isNotEmpty()) { val id = pending.removeFirst(); if (visited.add(id)) pending.addAll(rooms.getValue(id).connections.values) }
        assertEquals(source.map { it.id }.toSet(), visited)
        assertEquals("source_zeke_nightmare", rooms.getValue("source_campfire").connections["west"])
        assertEquals("source_orion_nightmare", rooms.getValue("source_campfire_node_scale_01").connections["east"])
        for ((id, key) in listOf("source_memory_bridge" to "bridge_built", "source_memory_threshold" to "crossing_open",
            "source_memory_stair" to "ascent_open", "source_spire_arena" to "center_path_open")) {
            assertTrue(rooms.getValue(id).blockedDirections?.get("east")?.requires.orEmpty().any { it.roomId == id && it.stateKey == key && it.value })
            val east = rooms.getValue(id).connections.getValue("east")
            assertTrue("Retreat cannot be gated by forward progress", rooms.getValue(east).blockedDirections?.get("west") == null)
        }
    }

    @Test
    fun retainedLaunchRailCannotBeEnteredThroughAPlayableExit() {
        assertEquals(mapOf("south" to "launch_pod"), rooms.getValue("launch_rail").connections)
        assertTrue(rooms.values.none { "launch_rail" in it.connections.values })
    }

    @Test
    fun cardinalArrivalsNeverCompeteForTheSameReturnSlot() {
        val inbound = mutableMapOf<Pair<String, String>, String>()
        rooms.values.forEach { source ->
            source.connections.filterKeys { it in setOf("north", "south", "east", "west") }
                .forEach { (direction, target) ->
                    val previous = inbound.put(target to direction, source.id)
                    assertTrue("$previous and ${source.id} both enter $target via $direction", previous == null)
                }
        }
    }

    @Test
    fun everyNodeMapAgreesWithItsCompassWithoutOverlappingRooms() {
        val nodes = JSONArray(File(assets, "hub_nodes.json").readText())
        for (i in 0 until nodes.length()) {
            val node = nodes.getJSONObject(i)
            val ids = node.getJSONArray("rooms").let { array ->
                (0 until array.length()).map { array.getString(it) }.toSet()
            }
            assertEquals("${node.getString("id")} has overlapping rooms", ids.size,
                ids.map { rooms.getValue(it).pos }.toSet().size)
            ids.forEach { id ->
                val source = rooms.getValue(id)
                source.connections.filterValues { it in ids }.forEach connection@{ (direction, targetId) ->
                    val vector = vectors[direction] ?: return@connection
                    val target = rooms.getValue(targetId)
                    val dx = target.pos[0] - source.pos[0]
                    val dy = target.pos[1] - source.pos[1]
                    fun agrees(delta: Int, sign: Int) = if (sign == 0) delta == 0 else delta * sign > 0
                    assertTrue("$id $direction -> $targetId has delta ($dx,$dy)",
                        agrees(dx, vector.first) && agrees(dy, vector.second))
                    if (vector.first == 0 || vector.second == 0) {
                        ids.filter { it != id && it != targetId }.forEach { otherId ->
                            val other = rooms.getValue(otherId)
                            val onCorridor = if (vector.first == 0) {
                                other.pos[0] == source.pos[0] && other.pos[1] > minOf(source.pos[1], target.pos[1]) &&
                                    other.pos[1] < maxOf(source.pos[1], target.pos[1])
                            } else {
                                other.pos[1] == source.pos[1] && other.pos[0] > minOf(source.pos[0], target.pos[0]) &&
                                    other.pos[0] < maxOf(source.pos[0], target.pos[0])
                            }
                            assertTrue("$id -> $targetId is drawn through unrelated $otherId", !onCorridor)
                        }
                    }
                }
            }
        }
    }

    private fun canReach(start: String, goal: String): Boolean {
        val pending = ArrayDeque<String>().apply { add(start) }
        val seen = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (id == goal) return true
            if (seen.add(id)) pending.addAll(rooms.getValue(id).connections.values)
        }
        return false
    }
}
