package com.example.starborn.feature.arcade.games.spireinfiltrator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpireInfiltratorEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministic() {
        val a = SpireInfiltratorEngine(42)
        val b = SpireInfiltratorEngine(42)
        val input = InfiltratorInput(left = true, boost = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val snapA = a.snapshot()
        val snapB = b.snapshot()
        assertEquals(snapA.playerGridX, snapB.playerGridX)
        assertEquals(snapA.playerGridY, snapB.playerGridY)
        assertEquals(snapA.score, snapB.score)
    }

    @Test
    fun movingAndEatingNode_increasesScore() {
        val engine = SpireInfiltratorEngine(7)
        val initialScore = engine.snapshot().score
        // Player starts at (7, 11), moves left towards data nodes
        repeat(30) { engine.advance(1f / 60f, InfiltratorInput(left = true)) }
        assertTrue(engine.snapshot().score > initialScore)
    }

    @Test
    fun reset_restoresFreshInfiltrationRun() {
        val engine = SpireInfiltratorEngine(7)
        repeat(60) { engine.advance(1f / 60f, InfiltratorInput(up = true)) }
        engine.reset()
        val snap = engine.snapshot()
        assertFalse(snap.gameOver)
        assertEquals(3, snap.lives)
        assertEquals(0, snap.score)
        assertEquals(1, snap.multiplier)
        assertEquals(7, snap.playerGridX)
        assertEquals(11, snap.playerGridY)
    }
}
