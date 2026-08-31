package com.example.starborn.ios.platform

import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * iOS implementation of AudioDriver that uses AVAudioPlayer.
 */
class IosAudioDriver : AudioDriver {

    private val activePlayers = mutableMapOf<Pair<AudioCueType, String>, AVAudioPlayer>()
    private var musicVolume: Float = 1f
    private var sfxVolume: Float = 1f

    override fun execute(command: AudioCommand) {
        when (command) {
            is AudioCommand.Play -> play(command)
            is AudioCommand.Stop -> stop(command)
            is AudioCommand.Duck -> setGain(command.type, command.gain)
            is AudioCommand.Restore -> setGain(command.type, 1f)
        }
    }

    override fun setUserGain(type: AudioCueType, gain: Float) {
        when (type) {
            AudioCueType.MUSIC -> musicVolume = gain.coerceIn(0f, 1f)
            else -> sfxVolume = gain.coerceIn(0f, 1f)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun play(command: AudioCommand.Play) {
        val cue = command.cueId.trim().lowercase().replace('-', '_')
        if (cue.isBlank()) return

        val url = findAudioUrl(cue) ?: return
        val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
        player.numberOfLoops = if (command.loop) -1 else 0
        player.volume = (if (command.type == AudioCueType.MUSIC) musicVolume else sfxVolume) * command.gain
        player.prepareToPlay()
        player.play()

        activePlayers[command.type to cue] = player
    }

    private fun stop(command: AudioCommand.Stop) {
        val cue = command.cueId.trim().lowercase().replace('-', '_')
        val player = activePlayers.remove(command.type to cue)
        player?.stop()
    }

    private fun setGain(type: AudioCueType, multiplier: Float) {
        activePlayers.forEach { (key, player) ->
            if (key.first == type) {
                player.volume = (if (type == AudioCueType.MUSIC) musicVolume else sfxVolume) * multiplier
            }
        }
    }

    private fun findAudioUrl(cueId: String): NSURL? {
        val extensions = listOf("wav", "mp3", "m4a", "caf")
        for (ext in extensions) {
            val url = NSBundle.mainBundle.URLForResource(cueId, withExtension = ext)
            if (url != null) return url
        }
        return null
    }

    override fun release() {
        activePlayers.values.forEach { it.stop() }
        activePlayers.clear()
    }
}
