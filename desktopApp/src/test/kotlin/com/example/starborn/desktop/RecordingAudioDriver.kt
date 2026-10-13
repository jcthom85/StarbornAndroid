package com.example.starborn.desktop

import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.*
import java.util.concurrent.CopyOnWriteArrayList

internal class RecordingAudioDriver : AudioDriver {
    val commands = CopyOnWriteArrayList<AudioCommand>()
    override fun execute(command: AudioCommand) { commands.add(command) }
    override fun setUserGain(type: AudioCueType, gain: Float) {}
    override fun release() {}
}
