package com.example.starborn.domain.leveling

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelingManagerTest {

    private val data = LevelingData(
        levelCurve = mapOf(
            "1" to 0,
            "2" to 100,
            "3" to 250,
            "4" to 450
        ),
        xpSources = mapOf(
            "fishing" to mapOf(
                "junk" to 1,
                "rare" to 30
            )
        )
    )

    private val manager = LevelingManager(data)

    @Test
    fun `levelForXp returns correct level thresholds`() {
        assertEquals(1, manager.levelForXp(0))
        assertEquals(1, manager.levelForXp(50))
        assertEquals(2, manager.levelForXp(100))
        assertEquals(3, manager.levelForXp(300))
        assertEquals(4, manager.levelForXp(600))
    }

    @Test
    fun `levelBounds exposes start and next thresholds`() {
        assertEquals(0 to 100, manager.levelBounds(1))
        assertEquals(100 to 250, manager.levelBounds(2))
        assertEquals(450 to null, manager.levelBounds(4))
    }

    @Test
    fun `xpForSource returns source specific values`() {
        assertEquals(1, manager.xpForSource("fishing", "junk"))
        assertEquals(30, manager.xpForSource("fishing", "rare"))
        assertEquals(0, manager.xpForSource("combat", "rare"))
    }
}
