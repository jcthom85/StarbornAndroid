package com.example.starborn.shared.puzzle

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

data class SignalDial(
    val id: String,
    val value: Float,
    val min: Float,
    val max: Float,
    val target: Float,
    val tolerance: Float
) {
    val matched: Boolean get() = abs(value - target) <= tolerance
}

enum class CounterTuneStep { PULSE, COOLING, GROUND }

data class SignalSample(val fork: Float, val cutter: Float) {
    val combined: Float get() = fork + cutter
}

/** The same signal drives the visual trace and diagnostics on both platforms. */
class CounterTuneSignal(val dials: List<SignalDial>) {
    val sweep = dials.first { it.id == "frequency" }
    val cooling = dials.first { it.id == "coolant" }
    val ground = dials.first { it.id == "ground_phase" }
    val balanced: Boolean get() = sweep.matched && cooling.matched && ground.matched

    fun ready(step: CounterTuneStep): Boolean = when (step) {
        CounterTuneStep.PULSE -> sweep.matched
        CounterTuneStep.COOLING -> sweep.matched && cooling.matched
        CounterTuneStep.GROUND -> balanced
    }

    fun sample(position: Float, step: CounterTuneStep): SignalSample {
        val angle = position * 2.75 * 2.0 * PI
        // Sweep changes pulse spacing. Cooling changes energy, with extra heat
        // from an unmatched sweep. Grounding finally reverses the cutter pulse.
        val amplitude = if (step == CounterTuneStep.PULSE) 1.0 else
            (1.0 + (cooling.target - cooling.value) / 50.0 +
                abs(sweep.value - sweep.target) / 100.0).coerceIn(0.1, 1.9)
        val phase = if (step == CounterTuneStep.GROUND)
            (ground.value - ground.target + 180.0) * PI / 180.0 else 0.0
        return SignalSample(
            fork = sin(angle).toFloat(),
            cutter = (sin(angle * sweep.value / sweep.target + phase) * amplitude).toFloat()
        )
    }

    val residual: Float get() {
        val meanSquare = (0..120).sumOf { index ->
            val value = sample(index / 120f, CounterTuneStep.GROUND).combined.toDouble()
            value * value
        } / 121.0
        return sqrt(meanSquare).toFloat()
    }

    fun diagnostic(step: CounterTuneStep): String = when (step) {
        CounterTuneStep.PULSE -> when {
            sweep.matched -> "Pulse spacing matched. Hold this sweep."
            sweep.value < sweep.target -> "Cutter pulses are too far apart. Speed up the sweep."
            else -> "Cutter pulses are too close together. Slow down the sweep."
        }
        CounterTuneStep.COOLING -> when {
            !sweep.matched -> "The sweep has drifted. Match pulse spacing first."
            cooling.matched -> "Signal heights balanced. The cold loop can hold this output."
            cooling.value < cooling.target -> "The cutter wave is too tall: heat is building. Add cooling."
            else -> "The cutter wave is too short. Ease the cooling to recover its output."
        }
        CounterTuneStep.GROUND -> when {
            !sweep.matched || !cooling.matched -> "Match pulse spacing and signal height before grounding."
            ground.matched -> "The waves oppose each other. The combined signal is quiet."
            else -> "Turn the ground phase until gold peaks face cyan troughs and the lower trace flattens."
        }
    }
}
