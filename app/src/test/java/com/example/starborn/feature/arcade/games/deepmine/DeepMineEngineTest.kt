package com.example.starborn.feature.arcade.games.deepmine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepMineEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministicAcrossRenderCadences() {
        val a = DeepMineEngine(42)
        val b = DeepMineEngine(42)
        val input = DeepMineInput(right = true, boost = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val first = a.snapshot()
        val second = b.snapshot()
        assertEquals(first.shipX, second.shipX, .001f)
        assertEquals(first.shipY, second.shipY, .001f)
        assertEquals(first.fuel, second.fuel, .001f)
        assertEquals(first.score, second.score)
    }

    @Test
    fun uncontrolledProbe_eventuallyCrashes() {
        val engine = DeepMineEngine(7)
        repeat(600) { engine.advance(1f / 60f, DeepMineInput()) }
        assertTrue(engine.snapshot().gameOver)
    }

    @Test
    fun reset_restoresFreshRun() {
        val engine = DeepMineEngine(7)
        repeat(600) { engine.advance(1f / 60f, DeepMineInput()) }
        engine.reset()
        val state = engine.snapshot()
        assertFalse(state.gameOver)
        assertEquals(100f, state.fuel, 0f)
        assertEquals(0, state.score)
        assertEquals(1, state.multiplier)
    }
}
