package com.example.starborn.domain.fishing

import kotlin.math.sin

enum class FishingFightPhase(val instruction: String) {
    CALM("The fish settles — reel steadily."),
    PULL("The fish is pulling — watch your tension."),
    WARNING("A surge is coming — release the reel."),
    SURGE("The fish surges — give it slack."),
    RECOVERY("The fish rests — reel it closer.")
}

/** Fixed-step, deterministic fight. Equipment adds forgiveness without removing fish tells. */
class FishingFight(private val behavior: FishBehaviorDefinition?, rod: FishingRod, val salvage: Boolean = false) {
    var progress = 0.2f
        private set
    var tension = 0.15f
        private set
    var elapsedMs = 0L
        private set
    var peakTension = tension
        private set
    var phase = FishingFightPhase.CALM
        private set
    var staminaRemaining = 1f
        private set
    private var surgeTicks = 0
    private var protectedSurgeTicks = 0
    private var effortMs = 0L
    private val power = ((rod.fishingPower - 1.0) / 2.05).coerceIn(0.0, 1.0).toFloat()
    private val forgiveness = (1f + ((rod.stability - 1.0) / 1.8).coerceIn(0.0, 1.0).toFloat() * 0.3f)
    private val strength = (((behavior?.basePull ?: 0.4) - 0.28) / 0.34).coerceIn(0.0, 1.0).toFloat()
    private val style = behavior?.fightStyle ?: when (behavior?.pattern) {
        FishPattern.BURST -> "dart"
        FishPattern.LINEAR -> "steady"
        else -> "gentle"
    }
    // Every surge has at least 840 ms of warning. Variation changes the rhythm,
    // never removes the tell that makes releasing fair on a touch screen.
    private fun rhythm(alternate: Boolean): List<Pair<FishingFightPhase, Long>> {
        val c = FishingFightPhase.CALM
        val p = FishingFightPhase.PULL
        val w = FishingFightPhase.WARNING
        val s = FishingFightPhase.SURGE
        val r = FishingFightPhase.RECOVERY
        return when (style) {
            "steady" -> listOf(c to 1440L, p to 1920L, w to 840L, s to 960L, r to 1440L)
            "dart" -> listOf(c to if (alternate) 2160L else 1320L, p to 720L, w to 840L,
                s to if (alternate) 1080L else 720L, r to 1200L)
            "drift" -> listOf(c to 960L, p to 2400L, w to 960L, s to 840L, r to 1440L)
            "wave" -> listOf(c to 1440L, p to 840L, w to 840L, s to 720L,
                r to 600L, w to 840L, s to 720L, r to 1200L)
            "ether" -> listOf(c to if (alternate) 960L else 1800L, p to 600L, w to 1080L,
                s to 1200L, r to if (alternate) 1800L else 1080L)
            else -> listOf(c to 2160L, p to 720L, w to 840L, s to 720L, r to 1440L)
        }
    }
    private val rhythms = listOf(rhythm(false), rhythm(true))
    private val fullCycleMs = rhythms.sumOf { sequence -> sequence.sumOf { it.second } }
    private fun phaseAt(timeMs: Long): FishingFightPhase {
        var remaining = timeMs % fullCycleMs
        for (sequence in rhythms) for ((step, duration) in sequence) {
            if (remaining < duration) return step
            remaining -= duration
        }
        return FishingFightPhase.CALM
    }
    val successful: Boolean get() = progress >= 1f && tension < 1f
    val failed: Boolean get() = tension >= 1f || progress <= 0f
    val perfect: Boolean get() = !salvage && successful && peakTension < 0.85f &&
        surgeTicks > 0 && protectedSurgeTicks >= surgeTicks * 0.8f

    fun tick(reeling: Boolean) {
        if (successful || failed) return
        elapsedMs += TICK_MS
        if (salvage) {
            phase = FishingFightPhase.CALM
            progress += if (reeling) 0.024f * (1f + power * 0.18f) else -0.001f
            tension += if (reeling) 0.012f / forgiveness else -0.045f
            progress = progress.coerceIn(0f, 1f)
            tension = tension.coerceIn(0f, 1.1f)
            peakTension = maxOf(peakTension, tension)
            return
        }
        phase = phaseAt(elapsedMs)
        val enduranceMs = (behavior?.stamina ?: 12.0).coerceIn(8.0, 24.0) * 1_000.0
        // Tire the fish by gaining ground or absorbing its surge with slack.
        // Waiting through calm water no longer exhausts it for free.
        if ((reeling && phase !in listOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE)) ||
            (!reeling && phase == FishingFightPhase.SURGE)) effortMs += TICK_MS
        staminaRemaining = (1.0 - effortMs / enduranceMs).coerceIn(0.0, 1.0).toFloat()
        val fatigue = 1f - staminaRemaining
        // Smooth wobble uses elapsed seconds, not raw nanoseconds as radians.
        val wobble = if (behavior?.pattern == FishPattern.SINE)
            (sin(elapsedMs / 1_000.0 * 2.0) * 0.002).toFloat() else 0f
        if (phase == FishingFightPhase.SURGE) {
            surgeTicks++
            if (!reeling) protectedSurgeTicks++
        }
        if (reeling) {
            val gain = when (phase) {
                FishingFightPhase.CALM, FishingFightPhase.RECOVERY -> 0.021f
                FishingFightPhase.PULL -> 0.011f
                FishingFightPhase.WARNING -> 0.006f
                FishingFightPhase.SURGE -> -0.004f
            }
            progress += gain * (1f + power * 0.18f + fatigue * 0.25f) / (1f + strength * 0.18f)
            val load = when (phase) {
                FishingFightPhase.CALM, FishingFightPhase.RECOVERY -> 0.007f
                FishingFightPhase.PULL -> 0.021f
                FishingFightPhase.WARNING -> 0.035f
                FishingFightPhase.SURGE -> 0.13f + (behavior?.burstPull ?: 0.1).toFloat() * 0.04f
            }
            tension += (load * (1f + strength * 0.2f) * (1f - fatigue * 0.15f) + wobble) / forgiveness
        } else {
            // Slack protects the catch during a surge; it is not a large progress penalty.
            progress -= if (phase == FishingFightPhase.SURGE) 0.0005f else 0.001f
            tension -= 0.045f
        }
        progress = progress.coerceIn(0f, 1f)
        tension = tension.coerceIn(0f, 1.1f)
        peakTension = maxOf(peakTension, tension)
    }

    companion object { const val TICK_MS = 120L }
}
