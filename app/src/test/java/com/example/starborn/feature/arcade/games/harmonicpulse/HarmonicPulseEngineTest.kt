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

    @Test
    fun audioClockSync_convergesMonotonicallyWithoutBackwardJumps() {
        val engine = HarmonicPulseEngine(42)
        // Simulate an initial Bluetooth/ExoPlayer lead of +140ms during intro and steady playback
        var audioClock = 0.14f
        var previousSongTime = 0f
        repeat(180) {
            audioClock += 1f / 60f
            engine.advance(1f / 60f, HarmonicPulseInput(), audioSongTimeSeconds = audioClock)
            val currentSongTime = engine.snapshot().songTimeSeconds
            assertTrue(
                "Song time must advance monotonically ($currentSongTime < $previousSongTime)",
                currentSongTime >= previousSongTime
            )
            previousSongTime = currentSongTime
        }
        // Within 3 seconds of playback, engine songTime should be phase-locked within < 5ms of audioClock
        assertEquals(audioClock, engine.snapshot().songTimeSeconds, 0.005f)
    }

    @Test
    fun audioClockSync_ignoresStalePreSeekPositionAfterReset() {
        val engine = HarmonicPulseEngine(99)
        // Stale pre-seek 41.8s reported on the first frame after reset must not jump songTime to end of round
        engine.advance(1f / 60f, HarmonicPulseInput(), audioSongTimeSeconds = 41.8f)
        val snap = engine.snapshot()
        assertEquals(1, snap.round)
        assertTrue("Expected songTime near 1/60s, got ${snap.songTimeSeconds}", snap.songTimeSeconds < 0.1f)
    }
}
