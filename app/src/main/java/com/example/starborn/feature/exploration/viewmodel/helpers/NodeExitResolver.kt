package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.NodeTransition
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.node.NodeProgressionEvaluator
import com.example.starborn.domain.node.NodeVisibility
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi
import java.util.Locale

/** An exit describes the edge out of this area, never the neighboring area's rooms. */
class NodeExitResolver(nodes: List<HubNode>, transitions: List<NodeTransition>) {
    private val nodesById = nodes.associateBy { it.id }
    private val ownerByRoom = nodes.flatMap { node -> node.rooms.map { it to node.id } }.toMap()
    private val transitionsByRoom = transitions.groupBy { it.fromRoom }
    private val progression = NodeProgressionEvaluator()

    fun exits(room: Room, state: GameSessionState): List<MapNodeExitUi> =
        transitionsByRoom[room.id].orEmpty().mapNotNull { edge ->
            val direction = edge.direction.lowercase(Locale.ROOT)
            val destination = nodesById[edge.toNode] ?: return@mapNotNull null
            if (edge.fromNode == edge.toNode || ownerByRoom[room.id] != edge.fromNode ||
                ownerByRoom[edge.toRoom] != edge.toNode ||
                room.connections.entries.none { it.key.equals(direction, true) && it.value == edge.toRoom }
            ) return@mapNotNull null
            val known = progression.evaluate(destination, state).visibility != NodeVisibility.HIDDEN
            MapNodeExitUi(direction, destination.id, if (known) destination.title else "Unexplored area")
        }.distinctBy { it.direction }
}
