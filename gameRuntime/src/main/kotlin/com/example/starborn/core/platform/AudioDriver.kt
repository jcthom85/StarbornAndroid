package com.example.starborn.core.platform

import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType

/**
 * Cross-platform audio playback engine interface.
 */
interface AudioDriver {
    fun execute(command: AudioCommand)

    fun executeAll(commands: List<AudioCommand>) {
        commands.forEach(::execute)
    }

    fun setUserGain(type: AudioCueType, gain: Float)

    /** Clear session-only music positions when loading a save or starting a new game. */
    fun resetMusicPlayback() {}

    /** Returns true when [cueId] resolves to a playable audio asset on this platform. */
    fun hasCue(cueId: String): Boolean = false

    /** Returns the active loop-wrapped playback position in milliseconds for [expectedCueId], if open. */
    fun currentMusicPositionMs(expectedCueId: String? = null): Long? = null

    /** Seeks the active [expectedCueId] music track back to 0ms without crossfade delay. */
    fun restartCurrentMusicFromStart(expectedCueId: String? = null) {}

    /** Pauses or resumes a specific active [expectedCueId] music track while an in-game overlay is open. */
    fun setMusicPausedForOverlay(paused: Boolean, expectedCueId: String? = null) {}

    fun release()
}
