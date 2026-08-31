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

    fun release()
}
