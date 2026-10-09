package com.example.starborn.desktop

import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.*
import java.io.BufferedInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sound.sampled.*
import kotlin.math.log10

/** Decoding and clip operations run on one audio worker, away from the UI thread. */
class DesktopAudioDriver(private val assetProvider: AssetProvider) : AudioDriver {
    private data class Playing(val clip: Clip, val cueGain: Float, val loop: Boolean, var envelope: Float = 1f, var fadeVersion: Long = 0)
    private val worker = Executors.newSingleThreadScheduledExecutor { task -> Thread(task, "starborn-audio").apply { isDaemon = true } }
    private val active = ConcurrentHashMap<Pair<AudioCueType, String>, Playing>()
    private val userGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerVersions = mutableMapOf<AudioCueType, Long>()
    private val reported = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var released = false

    init {
        worker.scheduleAtFixedRate({
            active.entries.toList().forEach { (key, playing) ->
                if (!playing.loop && !playing.clip.isRunning && playing.clip.framePosition >= playing.clip.frameLength) {
                    if (active.remove(key, playing)) playing.clip.close()
                }
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    override fun execute(command: AudioCommand) = enqueue {
        when (command) {
            is AudioCommand.Play -> play(command)
            is AudioCommand.Stop -> active[command.type to normalize(command.cueId)]?.let { playing ->
                fade(command.type, playing, 0f, command.fadeMs) {
                    active.remove(command.type to normalize(command.cueId), playing)
                    playing.clip.stop(); playing.clip.close()
                }
            }
            is AudioCommand.Duck -> fadeLayer(command.type, command.gain, command.fadeMs)
            is AudioCommand.Restore -> fadeLayer(command.type, 1f, command.fadeMs)
        }
    }

    override fun setUserGain(type: AudioCueType, gain: Float) = enqueue {
        userGains[type] = gain.coerceIn(0f, 1f)
        active.forEach { (key, playing) -> if (key.first == type) applyGain(type, playing) }
    }

    private fun enqueue(action: () -> Unit) {
        if (released) return
        try { worker.execute { if (!released) action() } } catch (_: java.util.concurrent.RejectedExecutionException) { }
    }

    private fun normalize(cue: String) = cue.trim().lowercase().replace('-', '_')

    private fun play(command: AudioCommand.Play) {
        val cue = normalize(command.cueId)
        if (cue.isEmpty()) return
        val key = command.type to cue
        val existing = active[key]
        if (command.loop && existing?.loop == true && existing.clip.isRunning) return
        var clip: Clip? = null
        try {
            val path = listOf("raw/$cue.mp3", "raw/$cue.wav", "raw/$cue.ogg", "$cue.mp3", "$cue.wav", "audio/$cue.wav", "audio/$cue.mp3", "audio/$cue.ogg")
                .firstOrNull(assetProvider::exists) ?: return
            requireNotNull(assetProvider.open(path)).use { raw ->
                AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                    val format = encoded.format
                    val pcm = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels, format.channels * 2, format.sampleRate, false)
                    clip = AudioSystem.getClip()
                    if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) clip!!.open(encoded)
                    else AudioSystem.getAudioInputStream(pcm, encoded).use { decoded -> clip!!.open(decoded) }
                }
            }
            if (released) { clip?.close(); return }
            if (command.type == AudioCueType.MUSIC || command.type == AudioCueType.VOICE) {
                active.entries.filter { it.key.first == command.type }.forEach { (oldKey, old) -> active.remove(oldKey, old); old.clip.stop(); old.clip.close() }
            }
            active.remove(key)?.let { it.clip.stop(); it.clip.close() }
            val playing = Playing(requireNotNull(clip), command.gain, command.loop, if (command.fadeMs > 0) 0f else 1f)
            active[key] = playing
            applyGain(command.type, playing)
            if (command.loop) playing.clip.loop(Clip.LOOP_CONTINUOUSLY) else playing.clip.start()
            fade(command.type, playing, 1f, command.fadeMs)
        } catch (error: Exception) {
            clip?.close()
            if (reported.add(cue)) System.err.println("Starborn audio '$cue': ${error.message}")
        }
    }

    private fun fade(type: AudioCueType, playing: Playing, target: Float, duration: Long, done: () -> Unit = {}) {
        val version = ++playing.fadeVersion
        val start = playing.envelope
        val steps = (duration.coerceIn(0, 10_000) / 25).toInt().coerceAtLeast(1)
        for (step in 1..steps) worker.schedule({
            if (!released && playing.fadeVersion == version && playing.clip.isOpen) {
                playing.envelope = start + (target - start) * step / steps
                applyGain(type, playing)
                if (step == steps) done()
            }
        }, duration.coerceIn(0, 10_000) * step / steps, TimeUnit.MILLISECONDS)
    }

    private fun fadeLayer(type: AudioCueType, target: Float, duration: Long) {
        val version = (layerVersions[type] ?: 0) + 1
        layerVersions[type] = version
        val start = layerGains[type] ?: 1f
        val steps = (duration.coerceIn(0, 10_000) / 25).toInt().coerceAtLeast(1)
        for (step in 1..steps) worker.schedule({
            if (!released && layerVersions[type] == version) {
                layerGains[type] = start + (target.coerceIn(0f, 1f) - start) * step / steps
                active.forEach { (key, playing) -> if (key.first == type) applyGain(type, playing) }
            }
        }, duration.coerceIn(0, 10_000) * step / steps, TimeUnit.MILLISECONDS)
    }

    private fun applyGain(type: AudioCueType, playing: Playing) {
        val clip = playing.clip
        if (!clip.isOpen || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val volume = ((userGains[type] ?: 1f) * (layerGains[type] ?: 1f) * playing.cueGain * playing.envelope).coerceIn(0f, 1f)
        control.value = if (volume <= .0001f) control.minimum else (20 * log10(volume)).coerceIn(control.minimum, control.maximum)
    }

    override fun release() {
        released = true
        worker.shutdownNow()
        active.values.forEach { it.clip.stop(); it.clip.close() }
        active.clear()
    }
}
