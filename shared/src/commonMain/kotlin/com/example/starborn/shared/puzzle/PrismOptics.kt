package com.example.starborn.shared.puzzle

import kotlin.math.abs

/** Visual diagnostics use the same ranges and tolerances as puzzle submission. */
class PrismOptics(val dials: List<SignalDial>) {
    val angle = dials.first { it.id == "angle" }
    val focus = dials.first { it.id == "freq" }
    val glow = dials.first { it.id == "lux" }
    val ready get() = angle.matched && focus.matched && glow.matched
    val aimOffset get() = if (angle.matched) 0f else
        ((angle.value - angle.target) / (angle.max - angle.min)).coerceIn(-1f, 1f)
    val fringe get() = if (focus.matched) 0f else
        (abs(focus.value - focus.target) / (focus.max - focus.min)).coerceIn(0f, 1f)
    val feedback get() = when {
        !angle.matched -> if (angle.value < angle.target) "Beam left of receiver. Turn refraction up." else "Beam right of receiver. Turn refraction down."
        !focus.matched -> if (focus.value < focus.target) "Wide fringe. Increase crystal spacing." else "Split fringe. Reduce crystal spacing."
        !glow.matched -> if (glow.value < glow.target) "Receiver dim. Increase glow." else "Receiver overloaded. Reduce glow."
        else -> "Beam centered, fringe closed, receiver steady. Capture the light."
    }
}
