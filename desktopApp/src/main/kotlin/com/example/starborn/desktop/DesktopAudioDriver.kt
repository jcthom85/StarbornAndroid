package com.example.starborn.desktop

import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

/**
 * Windows Desktop audio playback engine.
 * Directly integrates with JavaSound / Windows Audio Engine.
 */
class DesktopAudioDriver(
    private val assetProvider: AssetProvider
) : AudioDriver {

    private val activeClips = ConcurrentHashMap<Pair<AudioCueType, String>, Clip>()
    private var musicVolume = 1f
    private var sfxVolume = 1f
    private var voiceVolume = 1f
    private var ambientVolume = 1f

    override fun execute(command: AudioCommand) {
        when (command) {
            is AudioCommand.Play -> play(command)
            is AudioCommand.Stop -> stop(command)
            is AudioCommand.Duck -> setGain(command.type, command.gain)
            is AudioCommand.Restore -> setGain(command.type, 1f)
        }
    }

    override fun setUserGain(type: AudioCueType, gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        when (type) {
            AudioCueType.MUSIC -> musicVolume = clamped
            AudioCueType.AMBIENT -> ambientVolume = clamped
            AudioCueType.VOICE -> voiceVolume = clamped
            AudioCueType.UI, AudioCueType.BATTLE -> sfxVolume = clamped
        }
        updateActiveVolumes(type)
    }

    private fun play(command: AudioCommand.Play) {
        val normalizedCue = command.cueId.trim().lowercase().replace('-', '_')
        if (normalizedCue.isBlank()) return

        if (command.type == AudioCueType.MUSIC) {
            stopType(AudioCueType.MUSIC)
        }

        try {
            val stream = findAudioStream(normalizedCue) ?: return
            val audioStream = AudioSystem.getAudioInputStream(BufferedInputStream(stream))
            val clip = AudioSystem.getClip()
            clip.open(audioStream)

            applyGainToClip(clip, effectiveVolume(command.type, command.gain))

            if (command.loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY)
            } else {
                clip.start()
            }

            activeClips[command.type to normalizedCue] = clip
        } catch (_: Throwable) {
            // Audio loading/format fallback
        }
    }

    private fun stop(command: AudioCommand.Stop) {
        val normalizedCue = command.cueId.trim().lowercase().replace('-', '_')
        val clip = activeClips.remove(command.type to normalizedCue)
        clip?.stop()
        clip?.close()
    }

    private fun stopType(type: AudioCueType) {
        val toRemove = activeClips.keys.filter { it.first == type }
        toRemove.forEach { key ->
            val clip = activeClips.remove(key)
            clip?.stop()
            clip?.close()
        }
    }

    private fun setGain(type: AudioCueType, multiplier: Float) {
        activeClips.forEach { (key, clip) ->
            if (key.first == type) {
                applyGainToClip(clip, effectiveVolume(type, multiplier))
            }
        }
    }

    private fun updateActiveVolumes(type: AudioCueType) {
        activeClips.forEach { (key, clip) ->
            if (key.first == type) {
                applyGainToClip(clip, effectiveVolume(type, 1f))
            }
        }
    }

    private fun effectiveVolume(type: AudioCueType, multiplier: Float): Float {
        val userVol = when (type) {
            AudioCueType.MUSIC -> musicVolume
            AudioCueType.AMBIENT -> ambientVolume
            AudioCueType.VOICE -> voiceVolume
            AudioCueType.UI, AudioCueType.BATTLE -> sfxVolume
        }
        return (userVol * multiplier).coerceIn(0f, 1f)
    }

    private fun applyGainToClip(clip: Clip, linearVolume: Float) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val dB = if (linearVolume <= 0.0001f) {
                    gainControl.minimum
                } else {
                    (20.0 * Math.log10(linearVolume.toDouble())).toFloat().coerceIn(gainControl.minimum, gainControl.maximum)
                }
                gainControl.value = dB
            }
        } catch (_: Throwable) {
        }
    }

    private fun findAudioStream(cueId: String): InputStream? {
        val candidates = listOf(
            "raw/$cueId.mp3",
            "raw/$cueId.wav",
            "raw/$cueId.ogg",
            "$cueId.mp3",
            "$cueId.wav",
            "$cueId.ogg",
            "audio/$cueId.wav",
            "audio/$cueId.mp3",
            "audio/$cueId.ogg"
        )
        for (candidate in candidates) {
            val stream = assetProvider.open(candidate)
            if (stream != null) return stream
        }
        return null
    }

    override fun release() {
        activeClips.values.forEach { clip ->
            clip.stop()
            clip.close()
        }
        activeClips.clear()
    }
}
