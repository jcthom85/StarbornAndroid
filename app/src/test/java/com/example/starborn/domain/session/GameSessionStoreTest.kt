package com.example.starborn.domain.session

import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionStoreTest {

    @Test
    fun unlockExitNormalizesKey() {
        val store = GameSessionStore()
        store.unlockExit("Town_9", "North")

        val state = store.state.value
        assertTrue(state.unlockedExits.contains("town_9::north"))
    }
}
