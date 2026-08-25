package com.example.starborn.data.assets

import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.viewmodel.helpers.generatorLitRoomIds
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the rule that the Stellarium generator lights the area it powers -- the node it sits in --
 * and not every room that merely shares its art environment.
 *
 * World 1 themes almost all of its interiors as env "mine": the Pit, the workshop, the med-bay and
 * the checkpoint all use the mine tileset. Scoping area power by env therefore switched off the
 * darkness puzzles in Nova's bunk, the Pit supply closet, Jed's basement and med-bay storage the
 * moment MQ03 restored power, silently deleting four authored mechanics mid-world.
 */
class GeneratorLightingIntegrityTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val generatorRoomId = "mine_junction"

    /** Rooms whose darkness is worked by their own switch and must survive MQ03. */
    private val locallyLitRooms = listOf(
        "pit_nova_bunk",
        "pit_storage",
        "workshop_basement",
        "medbay_storage"
    )

    private val rooms: List<Room> by lazy {
        val type = Types.newParameterizedType(List::class.java, Room::class.java)
        moshi.adapter<List<Room>>(type)
            .fromJson(File("src/main/assets/rooms.json").readText())
            .orEmpty()
    }

    private val nodeIdByRoomId: Map<String, String> by lazy {
        val type = Types.newParameterizedType(List::class.java, HubNodeRooms::class.java)
        moshi.adapter<List<HubNodeRooms>>(type)
            .fromJson(File("src/main/assets/hub_nodes.json").readText())
            .orEmpty()
            .flatMap { node -> node.rooms.orEmpty().map { it to node.id } }
            .toMap()
    }

    private val darkCapableRoomIds: Set<String> by lazy {
        rooms.filter { it.dark == true || it.state["dark"] == true }.map { it.id }.toSet()
    }

    @Test
    fun generatorLightsTheMineAndNothingElse() {
        val lit = generatorLitRoomIds(generatorRoomId, nodeIdByRoomId, darkCapableRoomIds)

        assertEquals(
            "The generator must live in the mine node for area lighting to resolve.",
            "deep_mine",
            nodeIdByRoomId[generatorRoomId]
        )

        // Every dark room in the mine is on the generator: restoring power is the payoff.
        listOf("mine_gas", "mine_shunt", "mine_conveyor", "mine_checkpoint").forEach { roomId ->
            assertTrue(
                "Restoring mine power must light $roomId.",
                lit.contains(roomId)
            )
        }

        locallyLitRooms.forEach { roomId ->
            assertTrue(
                "$roomId is authored dark; the test is meaningless if it is not.",
                darkCapableRoomIds.contains(roomId)
            )
            assertFalse(
                "$roomId must not be lit by the mine generator -- its darkness is local to the room.",
                lit.contains(roomId)
            )
        }
    }

    @Test
    fun envScopingWouldReachPastTheMineWhichIsWhyTheRuleIsNodeScoped() {
        // The regression guard: all four locally-lit rooms are themed env "mine", so any rule that
        // scopes area power by environment silently swallows them. If this ever stops holding, the
        // node-scoped rule above has lost the distinction it exists to draw.
        val generatorEnv = rooms.single { it.id == generatorRoomId }.env
        val envScoped = rooms
            .filter { it.env.equals(generatorEnv, ignoreCase = true) && darkCapableRoomIds.contains(it.id) }
            .map { it.id }
            .toSet()

        locallyLitRooms.forEach { roomId ->
            assertTrue(
                "$roomId shares the generator's env, so env scoping would wrongly light it.",
                envScoped.contains(roomId)
            )
        }

        val nodeScoped = generatorLitRoomIds(generatorRoomId, nodeIdByRoomId, darkCapableRoomIds)
        assertEquals(
            "Node scoping must exclude exactly the locally-lit rooms that env scoping swallows.",
            locallyLitRooms.toSet(),
            envScoped - nodeScoped
        )
    }
}

private data class HubNodeRooms(
    val id: String,
    val rooms: List<String>? = null
)
