package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.Room
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpecialExitIntegrityTest {
    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val rooms by lazy { readRooms() }
    private val cardinal = setOf("north", "south", "east", "west")
    private val opposite = mapOf(
        "northeast" to "southwest",
        "southwest" to "northeast",
        "northwest" to "southeast",
        "southeast" to "northwest",
        "up" to "down",
        "down" to "up"
    )

    @Test
    fun `ordinary world two movement is entirely cardinal`() {
        val nonCardinal = rooms.filter { it.id.startsWith("sector9_") }.flatMap { room ->
            room.connections.keys.filterNot { it in cardinal }.map { "${room.id}:$it" }
        }
        assertTrue("World 2 must use swipeable NSEW routes: $nonCardinal", nonCardinal.isEmpty())
    }

    @Test
    fun `every exceptional connection has a named reciprocal interaction`() {
        val byId = rooms.associateBy { it.id }
        val exceptional = rooms.flatMap { room ->
            room.connections.filterKeys { it !in cardinal }
                .map { (direction, target) -> Triple(room, direction, target) }
        }

        assertEquals("The reviewed exception budget changed", 38, exceptional.size)
        exceptional.forEach { (room, direction, targetId) ->
            assertFalse("${room.id}:$direction needs a visible travel label", room.specialExits[direction].isNullOrBlank())
            val target = byId[targetId]
            assertNotNull("${room.id}:$direction points to missing $targetId", target)
            if (direction == "down" && NavigationIntegrityTest.oneWayExits[room.id] == targetId) {
                assertTrue(room.specialExits.getValue(direction).contains("one way"))
                return@forEach
            }
            val reverse = opposite.getValue(direction)
            assertEquals("${room.id}:$direction must remain reciprocal", room.id, target?.connections?.get(reverse))
            assertFalse("$targetId:$reverse needs a visible return label", target?.specialExits?.get(reverse).isNullOrBlank())
        }
    }

    @Test
    fun `special exit metadata names only real exceptional connections`() {
        rooms.forEach { room ->
            assertEquals(
                "${room.id} special exits must exactly cover its non-cardinal connections",
                room.connections.keys.filterNot { it in cardinal }.toSet(),
                room.specialExits.keys
            )
            assertEquals(
                "${room.id} travel labels must be unique",
                room.specialExits.values.size,
                room.specialExits.values.map { it.lowercase() }.distinct().size
            )
        }
    }

    @Test
    fun `world two landing positions align with cardinal links`() {
        val byId = rooms.associateBy { it.id }
        assertEquals(listOf(1, -1), byId.getValue("sector9_landing_brush").pos)
        assertEquals(listOf(1, 0), byId.getValue("sector9_landing_stream").pos)
        assertEquals(listOf(2, -1), byId.getValue("sector9_landing_drop").pos)
    }

    private fun readRooms(): List<Room> {
        val type = Types.newParameterizedType(List::class.java, Room::class.java)
        return MoshiProvider.instance.adapter<List<Room>>(type)
            .fromJson(File(assets, "rooms.json").readText())
            .orEmpty()
    }
}
