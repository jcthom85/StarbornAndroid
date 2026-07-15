package com.example.starborn.feature.enemy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnemyPresentationSizingTest {

    @Test
    fun `raw enemy tiers resolve to three presentation classes`() {
        listOf("standard", "common", "normal", "hazard", null).forEach { tier ->
            assertEquals(EnemyPresentationTier.STANDARD, enemyPresentationTier(tier))
        }
        assertEquals(EnemyPresentationTier.ELITE, enemyPresentationTier("elite"))
        assertEquals(EnemyPresentationTier.ELITE, enemyPresentationTier("mini-boss"))
        assertEquals(EnemyPresentationTier.BOSS, enemyPresentationTier("boss"))
    }

    @Test
    fun `combat scale increases with threat class`() {
        val standard = combatEnemySpriteScale("standard")
        val elite = combatEnemySpriteScale("elite")
        val boss = combatEnemySpriteScale("boss")

        assertTrue(elite > standard)
        assertTrue(boss > elite)
    }

    @Test
    fun `exploration scale increases with threat class`() {
        val standard = explorationEnemySpriteScale(EnemyPresentationTier.STANDARD)
        val elite = explorationEnemySpriteScale(EnemyPresentationTier.ELITE)
        val boss = explorationEnemySpriteScale(EnemyPresentationTier.BOSS)

        assertTrue(elite > standard)
        assertTrue(boss > elite)
    }
}
