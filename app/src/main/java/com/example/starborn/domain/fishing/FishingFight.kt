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
class FishingFight(private val behavior: FishBehaviorDefinition?, rod: FishingRod) {
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
    private val power = ((rod.fishingPower - 1.0) / 2.05).coerceIn(0.0, 1.0).toFloat()
    private val forgiveness = (1f + ((rod.stability - 1.0) / 1.8).coerceIn(0.0, 1.0).toFloat() * 0.3f)
    private val strength = (((behavior?.basePull ?: 0.4) - 0.28) / 0.34).coerceIn(0.0, 1.0).toFloat()
    private val cycleMs = when (behavior?.pattern) {
        FishPattern.BURST -> 5_200L
        FishPattern.LINEAR -> 6_200L
        else -> 5_600L
    }
    val successful: Boolean get() = progress >= 1f && tension < 1f
    val failed: Boolean get() = tension >= 1f || progress <= 0f
    val perfect: Boolean get() = successful && peakTension < 0.85f &&
        surgeTicks > 0 && protectedSurgeTicks >= surgeTicks * 0.8f

    fun tick(reeling: Boolean) {
        if (successful || failed) return
        elapsedMs += TICK_MS
        val position = (elapsedMs % cycleMs).toFloat() / cycleMs
        phase = when {
            position < 0.36f -> FishingFightPhase.CALM
            position < 0.53f -> FishingFightPhase.PULL
            position < 0.65f -> FishingFightPhase.WARNING
            position < 0.86f -> FishingFightPhase.SURGE
            else -> FishingFightPhase.RECOVERY
        }
        val enduranceMs = (behavior?.stamina ?: 12.0).coerceIn(8.0, 24.0) * 1_000.0
        staminaRemaining = (1.0 - elapsedMs / enduranceMs).coerceIn(0.0, 1.0).toFloat()
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
