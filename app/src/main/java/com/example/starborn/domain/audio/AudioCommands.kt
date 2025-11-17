package com.example.starborn.domain.audio

sealed interface AudioCommand {
    val type: AudioCueType

    data class Play(
        override val type: AudioCueType,
        val cueId: String,
        val loop: Boolean = false,
        val fadeMs: Long = DEFAULT_FADE_MS,
        val gain: Float = 1f,
        val triggerHaptic: Boolean = false
    ) : AudioCommand

    data class Stop(
        override val type: AudioCueType,
        val cueId: String,
        val fadeMs: Long = DEFAULT_FADE_MS
    ) : AudioCommand

    data class Duck(
        override val type: AudioCueType,
        val gain: Float,
        val fadeMs: Long = DEFAULT_FADE_MS
    ) : AudioCommand

    data class Restore(
        override val type: AudioCueType,
        val fadeMs: Long = DEFAULT_FADE_MS
    ) : AudioCommand

    companion object {
        private const val DEFAULT_FADE_MS = 400L
    }
}
