package com.example.starborn.feature.arcade.games.canopyhopper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanopyHopperEngineTest {
    @Test
    fun fixedStepSimulation_isDeterministicAcrossCadences() {
        val a = CanopyHopperEngine(42)
        val b = CanopyHopperEngine(42)
        val input = CanopyHopperInput(up = true, hop = true)

        repeat(60) { a.advance(1f / 60f, input) }
        repeat(30) { b.advance(1f / 30f, input) }

        val snapA = a.snapshot()
        val snapB = b.snapshot()
        assertEquals(snapA.hopperGridX, snapB.hopperGridX)
        assertEquals(snapA.hopperGridY, snapB.hopperGridY)
        assertEquals(snapA.score, snapB.score)
    }

    @Test
    fun forwardHop_increasesScore() {
        val engine = CanopyHopperEngine(7)
        val initialScore = engine.snapshot().score
        engine.advance(1f / 60f, CanopyHopperInput(up = true))
        assertTrue(engine.snapshot().score > initialScore)
        assertEquals(7, engine.snapshot().hopperGridY)
    }

    @Test
    fun reset_restoresFreshRun() {
        val engine = CanopyHopperEngine(7)
        repeat(60) { engine.advance(1f / 60f, CanopyHopperInput(up = true)) }
        engine.reset()
        val snap = engine.snapshot()
        assertFalse(snap.gameOver)
        assertEquals(3, snap.lives)
        assertEquals(0, snap.score)
        assertEquals(1, snap.multiplier)
        assertEquals(8, snap.hopperGridY)
        assertEquals(4, snap.hopperGridX)
    }
}
