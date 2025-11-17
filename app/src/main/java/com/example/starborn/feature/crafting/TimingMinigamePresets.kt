package com.example.starborn.feature.crafting

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

internal data class TimingMinigamePreset(
    val successWidth: Float,
    val perfectWidth: Float,
    val cycleDurationMs: Long
)

internal data class TimingMinigameConfig(
    val successWidth: Float,
    val perfectWidth: Float,
    val cycleDurationMs: Long,
    val durationMs: Long
) {
    val track: TimingTrackWindow = TimingMinigamePreset(
        successWidth = successWidth,
        perfectWidth = perfectWidth,
        cycleDurationMs = cycleDurationMs
    ).toTrackWindow()
}

internal data class TimingTrackWindow(
    val successStart: Float,
    val successEnd: Float,
    val perfectStart: Float,
    val perfectEnd: Float
)

internal fun MinigameDifficulty.defaultPreset(): TimingMinigamePreset = when (this) {
    MinigameDifficulty.EASY -> TimingMinigamePreset(
        successWidth = 0.72f, // 0.30 * 2 + 0.12 from Python timing_bar preset
        perfectWidth = 0.12f,
        cycleDurationMs = (1.4f * 2_000f).roundToLong()
    )
    MinigameDifficulty.NORMAL -> TimingMinigamePreset(
        successWidth = 0.52f, // 0.22 * 2 + 0.08
        perfectWidth = 0.08f,
        cycleDurationMs = (1.9f * 2_000f).roundToLong()
    )
    MinigameDifficulty.HARD -> TimingMinigamePreset(
        successWidth = 0.37f, // 0.16 * 2 + 0.05
        perfectWidth = 0.05f,
        cycleDurationMs = (2.5f * 2_000f).roundToLong()
    )
}

internal fun TimingMinigamePreset.withOverrides(
    successOverride: Float?,
    perfectOverride: Float?
): TimingMinigamePreset {
    val successWidth = successOverride?.coerceIn(0.2f, 0.92f) ?: successWidth
    val clampedPerfect = perfectOverride
        ?.coerceIn(0.05f, successWidth * 0.7f)
        ?: perfectWidth
    val perfectWidth = min(clampedPerfect, successWidth * 0.7f)
    return copy(successWidth = successWidth, perfectWidth = perfectWidth)
}

internal fun TimingMinigamePreset.toTrackWindow(): TimingTrackWindow {
    val successStart = ((1f - successWidth).coerceAtLeast(0f)) / 2f
    val successEnd = (successStart + successWidth).coerceAtMost(1f)
    val perfectWidth = perfectWidth.coerceIn(0.01f, successWidth)
    val perfectStart = (0.5f - perfectWidth / 2f).coerceIn(successStart, successEnd - perfectWidth)
    val perfectEnd = min(successEnd, perfectStart + perfectWidth)
    return TimingTrackWindow(
        successStart = successStart,
        successEnd = successEnd,
        perfectStart = perfectStart,
        perfectEnd = perfectEnd
    )
}

internal fun progressForElapsed(elapsedMs: Long, cycleDurationMs: Long): Float {
    val cycle = max(cycleDurationMs, 600L)
    val halfCycle = cycle / 2f
    val position = (elapsedMs % cycle).toFloat()
    return if (position <= halfCycle) {
        position / halfCycle
    } else {
        1f - ((position - halfCycle) / halfCycle)
    }
}
