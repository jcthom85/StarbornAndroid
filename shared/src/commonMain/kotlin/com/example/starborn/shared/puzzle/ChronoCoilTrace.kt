package com.example.starborn.shared.puzzle

import kotlin.math.abs

/**
 * Coupled diagnostic model for Synchronize Chrono Coils (`w3_chrono_flux_tune`).
 * Damping and carrier drift interact directly on the live trace:
 * - Off-target damping pulls the visible carrier trace vertically (`effectiveDrift`).
 * - Off-target carrier drift adds residual harmonic jitter (`jitter`) back into the trace.
 * - Phase dilation slides the return echo pulse horizontally across the lock gate.
 */
class ChronoCoilTrace(val dials: List<SignalDial>) {
    val damping = dials.first { it.id == "damping" }
    val drift = dials.first { it.id == "pitch" }
    val dilation = dials.first { it.id == "dilation" }

    val carrierStable: Boolean get() = damping.matched && drift.matched
    val ready: Boolean get() = carrierStable && dilation.matched

    private val dampingDelta: Float get() = damping.value - damping.target
    private val driftDelta: Float get() = drift.value - drift.target

    /** Normalized vertical trace offset (-1..1): damping pulls the carrier trace off-center. */
    val effectiveDriftOffset: Float
        get() = if (carrierStable) 0f else {
            val coupled = driftDelta + 0.6f * dampingDelta
            (coupled / 65f).coerceIn(-1f, 1f)
        }

    /** Normalized trace jitter (0..1): damping cuts primary jitter, while drift destabilizes it. */
    val jitter: Float
        get() = if (carrierStable) 0f else {
            val primary = abs(dampingDelta) / 45f
            val coupled = 0.4f * abs(driftDelta) / 55f
            (primary + coupled).coerceIn(0f, 1f)
        }

    /** Normalized horizontal return-echo offset (-1..1) relative to the phase lock gate. */
    val phaseOffset: Float
        get() = if (dilation.matched) 0f else {
            ((dilation.value - dilation.target) / 140f).coerceIn(-1f, 1f)
        }

    val feedback: String
        get() = when {
            ready -> "Carrier flat, jitter gone, return echo inside the gate. Lock tachyon phase."
            !damping.matched && !drift.matched -> {
                val pullDir = if (effectiveDriftOffset > 0.08f) "high" else if (effectiveDriftOffset < -0.08f) "low" else "unstable"
                val dampCue = if (damping.value < damping.target) "raise damping" else "ease damping"
                val driftCue = if (drift.value < drift.target) "raise carrier drift" else "lower carrier drift"
                "Coupled trace running $pullDir with heavy jitter—$dampCue while you $driftCue to settle the band."
            }
            !damping.matched -> {
                val dampCue = if (damping.value < damping.target) "Increase flux damping" else "Ease flux damping"
                "$dampCue—unbalanced damping is pulling the carrier trace off-center."
            }
            !drift.matched -> {
                val driftCue = if (drift.value < drift.target) "Raise carrier drift" else "Lower carrier drift"
                "$driftCue to pull the carrier into the green corridor and kill residual jitter."
            }
            !dilation.matched -> {
                val phaseCue = if (dilation.value < dilation.target) "Return echo left of gate—advance phase dilation."
                else "Return echo right of gate—pull phase dilation back."
                "Carrier locked! $phaseCue"
            }
            else -> "Lock tachyon phase."
        }
}
