package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.NodeTransition
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.*
import org.junit.Test

class NodeExitResolverTest {
    private val source = Room("source", "test", "Source", "", description = "", npcs = emptyList(),
        items = emptyList(), enemies = emptyList(), connections = mapOf("north" to "target"),
        pos = listOf(0, 0), state = emptyMap(), actions = emptyList())
    private val nodes = listOf(
        HubNode("local", "hub", "Local", "source", rooms = listOf("source")),
        HubNode("remote", "hub", "Deep Mine", "target", rooms = listOf("target"), initialVisibility = "hidden")
    )
    private val edge = NodeTransition("edge", "local", "source", "north", "remote", "target")

    @Test fun `hidden destination is anonymous until revealed or visited`() {
        val resolver = NodeExitResolver(nodes, listOf(edge))
        assertEquals("Unexplored area", resolver.exits(source, GameSessionState()).single().destinationTitle)
        assertEquals("Deep Mine", resolver.exits(source, GameSessionState(revealedNodes = setOf("remote"))).single().destinationTitle)
        assertEquals("Deep Mine", resolver.exits(source, GameSessionState(visitedNodes = setOf("remote"))).single().destinationTitle)
    }

    @Test fun `stale intra node and missing destination records produce no marker`() {
        listOf(edge.copy(toNode = "local"), edge.copy(toNode = "missing"),
            edge.copy(toRoom = "missing"), edge.copy(direction = "south"), edge.copy(fromNode = "wrong")).forEach {
            assertTrue(NodeExitResolver(nodes, listOf(it)).exits(source, GameSessionState()).isEmpty())
        }
    }

    @Test fun `one way connection does not imply a return exit`() {
        val resolver = NodeExitResolver(nodes, listOf(edge))
        val target = source.copy(id = "target", connections = emptyMap())
        assertEquals("north", resolver.exits(source, GameSessionState()).single().direction)
        assertTrue(resolver.exits(target, GameSessionState()).isEmpty())
    }

    @Test fun `all authored node transitions resolve without importing neighboring rooms`() {
        val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
        val transitions = assets.loadNodeTransitions()
        val rooms = assets.loadRooms().associateBy { it.id }
        val resolver = NodeExitResolver(assets.loadHubNodes(), transitions)
        assertTrue(transitions.isNotEmpty())
        transitions.forEach { transition ->
            assertTrue("Missing ${transition.id}", resolver.exits(rooms.getValue(transition.fromRoom), GameSessionState())
                .any { it.direction == transition.direction && it.destinationNodeId == transition.toNode })
        }
    }
}
