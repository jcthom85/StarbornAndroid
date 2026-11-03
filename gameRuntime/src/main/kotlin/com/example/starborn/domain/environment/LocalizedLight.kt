package com.example.starborn.domain.environment

import kotlin.math.sin

fun isLocalizedLightStyle(style: String): Boolean = when (style) {
    "lamplight", "bioluminescence", "firelight", "canopy" -> true
    else -> false
}

data class LocalizedLightMotion(
    val strength: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)

/** Motion is relative to a receiving surface, never an unanchored moving lamp or mushroom. */
fun localizedLightMotion(style: String, seconds: Float, seed: Int, source: Int, animate: Boolean): LocalizedLightMotion {
    if (!animate) return LocalizedLightMotion(.86f)
    val phase = (seed.mod(97) / 97f) * 6.283185f + source * 2.39996f
    val t = if (seconds.isFinite()) seconds.coerceAtLeast(0f) else 0f
    return when (style) {
        "lamplight" -> {
            // A struggling fixture settles. Its position does not sway or perpetually breathe.
            val settling = (1f - t / 1.8f).coerceIn(0f, 1f)
            val dip = (.18f + .17f * sin(t * 5.2f)) * settling
            LocalizedLightMotion((.95f - dip + .005f * sin(t * .7f + phase)).coerceIn(.5f, 1f),
                scaleX = 1f - dip * .5f, scaleY = 1f - dip * .5f)
        }
        "bioluminescence" -> {
            val breath = sin(t * .63f + phase)
            LocalizedLightMotion((.74f + .22f * breath + .05f * sin(t * .21f + phase)).coerceIn(.5f, 1f),
                scaleX = 1f + .12f * breath, scaleY = 1f + .12f * breath)
        }
        "firelight" -> LocalizedLightMotion(
            (.77f + .14f * sin(t * 2.1f + phase) + .08f * sin(t * 3.7f + phase * .8f) + .035f * sin(t * 6.3f + phase)).coerceIn(.5f, 1f),
            .20f * sin(t * 1.4f + phase * .3f), .10f * sin(t * 1.9f + phase),
            1f + .18f * sin(t * 2f + phase), 1f + .12f * sin(t * 1.3f + phase))
        "canopy" -> LocalizedLightMotion(1f, .065f * sin(t * .48f + phase), .035f * sin(t * .37f + phase),
            1f + .06f * sin(t * .43f), 1f)
        else -> LocalizedLightMotion(.86f)
    }
}

fun localizedLightStrength(style: String, seconds: Float, seed: Int, source: Int, animate: Boolean): Float =
    localizedLightMotion(style, seconds, seed, source, animate).strength
