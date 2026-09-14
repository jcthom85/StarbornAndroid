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
        // Existing shortcuts retain their original destinations and indirect returns.
        val oneWayExits = mapOf(
            "spire_underrail_platform" to "spire_transit_plaza",
            "spire_service_lift" to "spire_archive_vault",
            "spire_glasswalk" to "spire_archive_vault",
            "deep_firewall_gamma" to "deep_anchor_chamber"
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
        rooms.values.forEach { source ->
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
