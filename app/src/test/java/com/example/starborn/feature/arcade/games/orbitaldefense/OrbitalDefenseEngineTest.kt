package com.example.starborn.feature.arcade.games.orbitaldefense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitalDefenseEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministic() {
        val a = OrbitalDefenseEngine(42)
        val b = OrbitalDefenseEngine(42)
        val input = OrbitalDefenseInput(moveRight = true, fire = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val snapA = a.snapshot()
        val snapB = b.snapshot()
        assertEquals(snapA.playerX, snapB.playerX, 0.001f)
        assertEquals(snapA.score, snapB.score)
        assertEquals(snapA.projectiles.size, snapB.projectiles.size)
    }

    @Test
    fun firingProjectiles_createsBullets() {
        val engine = OrbitalDefenseEngine(7)
        engine.advance(1f / 60f, OrbitalDefenseInput(fire = true))
        assertTrue(engine.snapshot().projectiles.isNotEmpty())
    }

    @Test
    fun reset_restoresFreshDefenseSector() {
        val engine = OrbitalDefenseEngine(7)
        repeat(60) { engine.advance(1f / 60f, OrbitalDefenseInput(moveLeft = true, fire = true)) }
        engine.reset()
        val snap = engine.snapshot()
        assertFalse(snap.gameOver)
        assertEquals(3, snap.lives)
        assertEquals(0, snap.score)
        assertEquals(1, snap.multiplier)
        assertEquals(1, snap.empBombs)
        assertEquals(18, snap.enemies.size)
    }
}
