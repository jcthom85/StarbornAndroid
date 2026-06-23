package com.example.starborn.domain.node

import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.NodeRequirement
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeProgressionEvaluatorTest {
    private val evaluator = NodeProgressionEvaluator()

    @Test
    fun hiddenTransitionNodeCannotBeEnteredFromHubUntilVisited() {
        val node = node(initialVisibility = "hidden", entryPolicy = "transition")

        val hidden = evaluator.evaluate(node, GameSessionState())
        val visited = evaluator.evaluate(
            node,
            GameSessionState(visitedNodes = setOf(node.id))
        )

        assertTrue(hidden.visibility == NodeVisibility.HIDDEN)
        assertFalse(hidden.canEnterFromHub)
        assertTrue(visited.visibility == NodeVisibility.REVEALED)
        assertTrue(visited.canEnterFromHub)
    }

    @Test
    fun revealedNodeUnlocksWhenMilestoneIsCompleted() {
        val node = node(
            initialVisibility = "revealed",
            unlockConditions = listOf(NodeRequirement(type = "milestone", id = "ms_ship_repaired"))
        )

        val locked = evaluator.evaluate(node, GameSessionState())
        val unlocked = evaluator.evaluate(
            node,
            GameSessionState(completedMilestones = setOf("ms_ship_repaired"))
        )

        assertTrue(locked.visibility == NodeVisibility.REVEALED)
        assertFalse(locked.unlocked)
        assertTrue(unlocked.unlocked)
        assertTrue(unlocked.canEnterFromHub)
    }

    private fun node(
        initialVisibility: String,
        entryPolicy: String = "hub",
        unlockConditions: List<NodeRequirement> = emptyList()
    ) = HubNode(
        id = "test_node",
        hubId = "test_hub",
        title = "Test Node",
        entryRoom = "test_room",
        rooms = listOf("test_room"),
        initialVisibility = initialVisibility,
        entryPolicy = entryPolicy,
        unlockConditions = unlockConditions
    )
}
