package com.example.starborn.feature.arcade.games.harmonicpulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarmonicPulseEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministic() {
        val a = HarmonicPulseEngine(42)
        val b = HarmonicPulseEngine(42)
        val input = HarmonicPulseInput(tapLane0 = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val snapA = a.snapshot()
        val snapB = b.snapshot()
        assertEquals(snapA.score, snapB.score)
        assertEquals(snapA.harmony, snapB.harmony, 0.001f)
    }

    @Test
    fun reset_restoresFreshTrack() {
        val engine = HarmonicPulseEngine(7)
        repeat(60) { engine.advance(1f / 60f, HarmonicPulseInput(tapLane1 = true)) }
        engine.reset()
        val snap = engine.snapshot()
        assertFalse(snap.gameOver)
        assertEquals(75f, snap.harmony, 0.001f)
        assertEquals(0, snap.score)
        assertEquals(1, snap.multiplier)
        assertEquals(0, snap.combo)
    }
}
