package com.example.starborn.domain.playtest

import com.example.starborn.shared.puzzle.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CounterTuneSignalTest {
    private fun signal(frequency: Float = 52f, cooling: Float = 35f, phase: Float = 0f) = CounterTuneSignal(listOf(
        SignalDial("frequency", frequency, 40f, 120f, 87f, 2f),
        SignalDial("coolant", cooling, 0f, 100f, 68f, 3f),
        SignalDial("ground_phase", phase, 0f, 360f, 180f, 6f)
    ))

    @Test fun stagesRequirePreviouslyMatchedSignalsAndCannotHandshakeEarly() {
        assertFalse(signal().ready(CounterTuneStep.PULSE))
        val sweep = signal(frequency = 87f)
        assertTrue(sweep.ready(CounterTuneStep.PULSE))
        assertFalse(sweep.ready(CounterTuneStep.COOLING))
        val cold = signal(frequency = 87f, cooling = 68f)
        assertTrue(cold.ready(CounterTuneStep.COOLING))
        assertFalse(cold.ready(CounterTuneStep.GROUND))
        assertTrue(signal(87f, 68f, 180f).ready(CounterTuneStep.GROUND))
        assertFalse(signal(80f, 68f, 180f).ready(CounterTuneStep.GROUND))
    }

    @Test fun canonicalCalibrationCancelsTheEntireTrace() {
        val quiet = signal(87f, 68f, 180f)
        for (i in 0..180) assertEquals(0f, quiet.sample(i / 180f, CounterTuneStep.GROUND).combined, 0.00001f)
        assertEquals(0f, quiet.residual, 0.00001f)
        assertTrue(signal(87f, 68f, 0f).residual > 1f)
    }

    @Test fun diagnosticDirectionsAgreeWithTraceAdjustments() {
        assertTrue(signal(frequency = 52f).diagnostic(CounterTuneStep.PULSE).contains("Speed up"))
        assertTrue(signal(frequency = 110f).diagnostic(CounterTuneStep.PULSE).contains("Slow down"))
        val hot = signal(87f, 35f)
        val balanced = signal(87f, 68f)
        val overcooled = signal(87f, 95f)
        assertTrue(hot.diagnostic(CounterTuneStep.COOLING).contains("Add cooling"))
        assertTrue(overcooled.diagnostic(CounterTuneStep.COOLING).contains("Ease"))
        val position = 0.08f
        assertTrue(abs(hot.sample(position, CounterTuneStep.COOLING).cutter) > abs(balanced.sample(position, CounterTuneStep.COOLING).cutter))
        assertTrue(abs(overcooled.sample(position, CounterTuneStep.COOLING).cutter) < abs(balanced.sample(position, CounterTuneStep.COOLING).cutter))
    }

    @Test fun tolerancesAreInclusiveAndRejectJustOutsideValues() {
        assertTrue(signal(85f, 65f, 174f).balanced)
        assertTrue(signal(89f, 71f, 186f).balanced)
        assertFalse(signal(84.9f, 68f, 180f).balanced)
        assertFalse(signal(87f, 64.9f, 180f).balanced)
        assertFalse(signal(87f, 68f, 173.9f).balanced)
    }
}
