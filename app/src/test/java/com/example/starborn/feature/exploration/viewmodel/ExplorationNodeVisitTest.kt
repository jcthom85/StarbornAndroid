package com.example.starborn.feature.exploration.viewmodel

import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorationNodeVisitTest {

    @Test
    fun enteringAdminLobbyMakesTransitionNodeAvailableForHubReturn() {
        val store = GameSessionStore()
        val nodeByRoom = mapOf(
            "checkpoint_tunnel" to "admin_gate",
            "admin_lobby" to "admin_concourse"
        )

        markContainingNodeVisited("admin_lobby", nodeByRoom, store)
        markContainingNodeVisited("admin_lobby", nodeByRoom, store)

        assertTrue("admin_concourse" in store.state.value.visitedNodes)
        assertTrue("admin_concourse" in store.state.value.unlockedNodes)
        assertTrue("admin_concourse" in store.state.value.revealedNodes)
        assertEquals(1, store.state.value.visitedNodes.size)
    }

    @Test
    fun roomWithoutHubNodeDoesNotChangeProgression() {
        val store = GameSessionStore()

        markContainingNodeVisited("debug_void", emptyMap(), store)

        assertTrue(store.state.value.visitedNodes.isEmpty())
    }
}
