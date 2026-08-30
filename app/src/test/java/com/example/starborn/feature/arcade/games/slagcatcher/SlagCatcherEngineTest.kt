package com.example.starborn.feature.arcade.games.slagcatcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlagCatcherEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministic() {
        val a = SlagCatcherEngine(42)
        val b = SlagCatcherEngine(42)
        val input = SlagCatcherInput(moveRight = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val snapA = a.snapshot()
        val snapB = b.snapshot()
        assertEquals(snapA.paddleX, snapB.paddleX, 0.001f)
        assertEquals(snapA.score, snapB.score)
        assertEquals(snapA.buckets, snapB.buckets)
    }

    @Test
    fun directTouchInput_steersPaddle() {
        val engine = SlagCatcherEngine(7)
        engine.advance(1f / 60f, SlagCatcherInput(paddleTargetX = 0.2f))
        assertTrue(engine.snapshot().paddleX < 0.5f)
    }

    @Test
    fun reset_restoresFreshShift() {
        val engine = SlagCatcherEngine(7)
        repeat(60) { engine.advance(1f / 60f, SlagCatcherInput(moveLeft = true)) }
        engine.reset()
        val snap = engine.snapshot()
        assertFalse(snap.gameOver)
        assertEquals(3, snap.buckets)
        assertEquals(0, snap.score)
        assertEquals(1, snap.multiplier)
        assertEquals(0.5f, snap.paddleX, 0.001f)
        assertEquals(0f, snap.heat, 0.001f)
    }
}
