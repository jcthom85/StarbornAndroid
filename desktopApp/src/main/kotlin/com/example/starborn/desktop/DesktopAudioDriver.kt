package com.example.starborn.desktop

import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.*
import java.io.BufferedInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.*
import kotlin.math.log10

/**
 * High-performance desktop audio driver.
 * - Dedicated music/ambience worker prevents music changes from blocking UI and battle SFX.
 * - Dedicated SFX worker handles immediate feedback cues and voice.
 * - Decoupled background audio loader ensures music/cue decoding never stalls gain fading or the event loop.
 * - Seamless crossfading for music tracks: outgoing music gracefully fades to 0 over fadeMs while incoming track fades in from 0 to target gain.
 * - In-memory pre-decoded PCM cache for small sound cues eliminates repeated disk & decode latency.
 * - Automatic pre-warming of frequent UI/combat SFX at startup.
 * - Supports master volume scaling and instantaneous muting when the application window is unfocused.
 */
class DesktopAudioDriver(
    private val assetProvider: AssetProvider,
    prewarmDefaults: Boolean = true
) : AudioDriver {
    private data class Playing(val clip: Clip, val cueGain: Float, val loop: Boolean, var envelope: Float = 1f, var fadeVersion: Long = 0)
    private data class CachedPcm(val format: AudioFormat, val bytes: ByteArray)

    private val musicWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "starborn-music-worker").apply { isDaemon = true }
    }
    private val sfxWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "starborn-sfx-worker").apply { isDaemon = true }
    }
    private val audioLoader: ExecutorService = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "starborn-audio-loader").apply { isDaemon = true }
    }

    private val active = ConcurrentHashMap<Pair<AudioCueType, String>, Playing>()
    private val fadingOutClips = ConcurrentHashMap.newKeySet<Clip>()
    private val sfxPcmCache = ConcurrentHashMap<String, CachedPcm>()
    private val userGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerVersions = ConcurrentHashMap<AudioCueType, Long>()
    private val reported = ConcurrentHashMap.newKeySet<String>()
    private val musicGeneration = AtomicLong(0)

    @Volatile private var masterGain: Float = 1f
    @Volatile private var isMuted: Boolean = false
    @Volatile private var released = false

    init {
        val cleanupTask = Runnable {
            active.entries.toList().forEach { (key, playing) ->
                if (!playing.loop && !playing.clip.isRunning && playing.clip.framePosition >= playing.clip.frameLength) {
                    if (active.remove(key, playing)) {
                        try {
                            playing.clip.close()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        sfxWorker.scheduleAtFixedRate(cleanupTask, 1, 1, TimeUnit.SECONDS)
        musicWorker.scheduleAtFixedRate(cleanupTask, 1, 1, TimeUnit.SECONDS)

        if (prewarmDefaults) {
            prewarm(DEFAULT_PREWARM_CUES)
        }
    }

    private fun workerFor(type: AudioCueType): ScheduledExecutorService =
        if (type == AudioCueType.MUSIC || type == AudioCueType.AMBIENT) musicWorker else sfxWorker

    fun setMasterGain(gain: Float) {
        masterGain = gain.coerceIn(0f, 1f)
        updateAllGains()
    }

    fun setMuted(muted: Boolean) {
        if (isMuted != muted) {
            isMuted = muted
            updateAllGains()
        }
    }

    private fun updateAllGains() {
        active.forEach { (key, playing) -> applyGain(key.first, playing) }
    }

    override fun execute(command: AudioCommand) {
        val worker = when (command) {
            is AudioCommand.Play -> workerFor(command.type)
            is AudioCommand.Stop -> workerFor(command.type)
            is AudioCommand.Duck -> workerFor(command.type)
            is AudioCommand.Restore -> workerFor(command.type)
        }
        enqueue(worker) {
            when (command) {
                is AudioCommand.Play -> play(command)
                is AudioCommand.Stop -> stopCue(command)
                is AudioCommand.Duck -> fadeLayer(command.type, command.gain, command.fadeMs)
                is AudioCommand.Restore -> fadeLayer(command.type, 1f, command.fadeMs)
            }
        }
    }

    private fun stopCue(command: AudioCommand.Stop) {
        val key = command.type to normalize(command.cueId)
        val playing = active.remove(key) ?: return
        if (command.fadeMs > 0) {
            fadingOutClips.add(playing.clip)
            fade(command.type, playing, 0f, command.fadeMs) {
                fadingOutClips.remove(playing.clip)
                try {
                    playing.clip.stop()
                    playing.clip.close()
                } catch (_: Exception) {}
            }
        } else {
            try {
                playing.clip.stop()
                playing.clip.close()
            } catch (_: Exception) {}
        }
    }

    override fun setUserGain(type: AudioCueType, gain: Float) {
        enqueue(workerFor(type)) {
            userGains[type] = gain.coerceIn(0f, 1f)
            active.forEach { (key, playing) -> if (key.first == type) applyGain(type, playing) }
        }
    }

    private fun enqueue(worker: ScheduledExecutorService, action: () -> Unit) {
        if (released) return
        try {
            worker.execute { if (!released) action() }
        } catch (_: java.util.concurrent.RejectedExecutionException) { }
    }

    private fun normalize(cue: String) = cue.trim().lowercase().replace('-', '_')

    fun prewarm(cues: Collection<String>) {
        enqueue(sfxWorker) {
            cues.forEach { rawCue ->
                val cue = normalize(rawCue)
                if (cue.isNotEmpty() && !sfxPcmCache.containsKey(cue) && !released) {
                    try {
                        loadPcm(cue)?.let { pcm ->
                            sfxPcmCache[cue] = pcm
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun resolvePath(cue: String): String? {
        val candidates = listOf(
            "raw/$cue.mp3", "raw/$cue.wav", "raw/$cue.ogg",
            "$cue.mp3", "$cue.wav",
            "audio/$cue.wav", "audio/$cue.mp3", "audio/$cue.ogg"
        )
        return candidates.firstOrNull(assetProvider::exists)
    }

    private fun loadPcm(cue: String): CachedPcm? {
        val path = resolvePath(cue) ?: return null
        return assetProvider.open(path)?.use { raw ->
            AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                val format = encoded.format
                val pcmFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.sampleRate, 16,
                    format.channels, format.channels * 2,
                    format.sampleRate, false
                )
                if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                    val bytes = encoded.readAllBytes()
                    if (bytes.size <= 2_500_000) CachedPcm(format, bytes) else null
                } else {
                    AudioSystem.getAudioInputStream(pcmFormat, encoded).use { decoded ->
                        val bytes = decoded.readAllBytes()
                        if (bytes.size <= 2_500_000) CachedPcm(pcmFormat, bytes) else null
                    }
                }
            }
        }
    }

    private fun play(command: AudioCommand.Play) {
        val cue = normalize(command.cueId)
        if (cue.isEmpty()) return
        val key = command.type to cue
        val existing = active[key]
        if (command.loop && existing?.loop == true && existing.clip.isRunning) return

        if (command.type == AudioCueType.MUSIC) {
            playMusicCrossfaded(command, key, cue)
        } else {
            playSfxOrVoice(command, key, cue)
        }
    }

    private fun playMusicCrossfaded(command: AudioCommand.Play, key: Pair<AudioCueType, String>, cue: String) {
        val currentGen = musicGeneration.incrementAndGet()

        // 1. Immediately extract and crossfade-out any other active music
        val outgoing = active.entries.filter { it.key.first == AudioCueType.MUSIC && it.key != key }
        outgoing.forEach { (oldKey, oldPlaying) ->
            if (active.remove(oldKey, oldPlaying)) {
                val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 400L
                fadingOutClips.add(oldPlaying.clip)
                fade(AudioCueType.MUSIC, oldPlaying, 0f, fadeOutMs) {
                    fadingOutClips.remove(oldPlaying.clip)
                    try {
                        oldPlaying.clip.stop()
                        oldPlaying.clip.close()
                    } catch (_: Exception) {}
                }
            }
        }

        // If same track was active but non-looping or restarting:
        active.remove(key)?.let { oldSame ->
            try {
                oldSame.clip.stop()
                oldSame.clip.close()
            } catch (_: Exception) {}
        }

        // 2. Offload music stream decoding to audioLoader so musicWorker's fade ticks remain uninterrupted
        if (released) return
        try {
            audioLoader.execute {
                if (released || musicGeneration.get() != currentGen) return@execute
                var loadedClip: Clip? = null
                try {
                    val path = resolvePath(cue) ?: return@execute
                    assetProvider.open(path)?.use { raw ->
                        AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                            val format = encoded.format
                            val pcmFormat = AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                format.sampleRate, 16,
                                format.channels, format.channels * 2,
                                format.sampleRate, false
                            )
                            val clip = AudioSystem.getClip()
                            loadedClip = clip
                            if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                                clip.open(encoded)
                            } else {
                                AudioSystem.getAudioInputStream(pcmFormat, encoded).use { decoded ->
                                    clip.open(decoded)
                                }
                            }
                        }
                    }

                    if (released || musicGeneration.get() != currentGen) {
                        try { loadedClip?.close() } catch (_: Exception) {}
                        return@execute
                    }

                    val readyClip = loadedClip ?: return@execute
                    try {
                        musicWorker.execute {
                            if (released || musicGeneration.get() != currentGen) {
                                try { readyClip.close() } catch (_: Exception) {}
                                return@execute
                            }

                            // Extra safety check for any remaining outgoing music
                            active.entries.filter { it.key.first == AudioCueType.MUSIC && it.key != key }.forEach { (oldKey, oldPlaying) ->
                                if (active.remove(oldKey, oldPlaying)) {
                                    val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 400L
                                    fadingOutClips.add(oldPlaying.clip)
                                    fade(AudioCueType.MUSIC, oldPlaying, 0f, fadeOutMs) {
                                        fadingOutClips.remove(oldPlaying.clip)
                                        try {
                                            oldPlaying.clip.stop()
                                            oldPlaying.clip.close()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }

                            val startEnvelope = if (command.fadeMs > 0) 0f else 1f
                            val playing = Playing(readyClip, command.gain, command.loop, envelope = startEnvelope)
                            active[key] = playing
                            applyGain(AudioCueType.MUSIC, playing)
                            if (command.loop) readyClip.loop(Clip.LOOP_CONTINUOUSLY) else readyClip.start()
                            if (command.fadeMs > 0) {
                                fade(AudioCueType.MUSIC, playing, 1f, command.fadeMs)
                            }
                        }
                    } catch (_: java.util.concurrent.RejectedExecutionException) {
                        try { readyClip.close() } catch (_: Exception) {}
                    }
                } catch (error: Exception) {
                    try { loadedClip?.close() } catch (_: Exception) {}
                    if (reported.add(cue)) System.err.println("Starborn audio '$cue': ${error.message}")
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {}
    }

    private fun playSfxOrVoice(command: AudioCommand.Play, key: Pair<AudioCueType, String>, cue: String) {
        val isSfx = command.type == AudioCueType.UI || command.type == AudioCueType.BATTLE || command.type == AudioCueType.VOICE
        var clip: Clip? = null
        try {
            if (command.type == AudioCueType.VOICE) {
                active.entries.filter { it.key.first == AudioCueType.VOICE }.forEach { (oldKey, old) ->
                    active.remove(oldKey, old)
                    try {
                        old.clip.stop()
                        old.clip.close()
                    } catch (_: Exception) {}
                }
            }
            active.remove(key)?.let {
                try {
                    it.clip.stop()
                    it.clip.close()
                } catch (_: Exception) {}
            }

            val cached = if (isSfx) sfxPcmCache[cue] else null
            if (cached != null) {
                clip = AudioSystem.getClip()
                val pcmBytes = if (command.pitch != 1.0f) resamplePcm(cached, command.pitch) else cached.bytes
                clip.open(cached.format, pcmBytes, 0, pcmBytes.size)
            } else {
                val path = resolvePath(cue) ?: return
                assetProvider.open(path)?.use { raw ->
                    AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                        val format = encoded.format
                        val pcmFormat = AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            format.sampleRate, 16,
                            format.channels, format.channels * 2,
                            format.sampleRate, false
                        )
                        clip = AudioSystem.getClip()
                        if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) {
                            if (isSfx) {
                                val bytes = encoded.readAllBytes()
                                if (bytes.size <= 2_500_000) sfxPcmCache[cue] = CachedPcm(format, bytes)
                                val pcmBytes = if (command.pitch != 1.0f) resamplePcm(CachedPcm(format, bytes), command.pitch) else bytes
                                clip.open(format, pcmBytes, 0, pcmBytes.size)
                            } else {
                                clip.open(encoded)
                            }
                        } else {
                            AudioSystem.getAudioInputStream(pcmFormat, encoded).use { decoded ->
                                if (isSfx) {
                                    val bytes = decoded.readAllBytes()
                                    if (bytes.size <= 2_500_000) sfxPcmCache[cue] = CachedPcm(pcmFormat, bytes)
                                    val pcmBytes = if (command.pitch != 1.0f) resamplePcm(CachedPcm(pcmFormat, bytes), command.pitch) else bytes
                                    clip.open(pcmFormat, pcmBytes, 0, pcmBytes.size)
                                } else {
                                    clip.open(decoded)
                                }
                            }
                        }
                    }
                }
            }

            if (released) {
                try { clip?.close() } catch (_: Exception) {}
                return
            }

            val readyClip = clip ?: return
            val playing = Playing(readyClip, command.gain, command.loop, if (command.fadeMs > 0) 0f else 1f)
            active[key] = playing
            applyGain(command.type, playing)
            if (command.loop) readyClip.loop(Clip.LOOP_CONTINUOUSLY) else readyClip.start()
            if (command.fadeMs > 0) {
                fade(command.type, playing, 1f, command.fadeMs)
            }
        } catch (error: Exception) {
            try { clip?.close() } catch (_: Exception) {}
            if (reported.add(cue)) System.err.println("Starborn audio '$cue': ${error.message}")
        }
    }

    private fun fade(type: AudioCueType, playing: Playing, target: Float, duration: Long, done: () -> Unit = {}) {
        val version = ++playing.fadeVersion
        val start = playing.envelope
        val steps = (duration.coerceIn(0, 10_000) / 25).toInt().coerceAtLeast(1)
        val worker = workerFor(type)
        for (step in 1..steps) {
            try {
                worker.schedule({
                    if (!released && playing.fadeVersion == version && playing.clip.isOpen) {
                        playing.envelope = start + (target - start) * step / steps
                        applyGain(type, playing)
                        if (step == steps) done()
                    }
                }, duration.coerceIn(0, 10_000) * step / steps, TimeUnit.MILLISECONDS)
            } catch (_: java.util.concurrent.RejectedExecutionException) {}
        }
    }

    private fun fadeLayer(type: AudioCueType, target: Float, duration: Long) {
        val version = (layerVersions[type] ?: 0) + 1
        layerVersions[type] = version
        val start = layerGains[type] ?: 1f
        val steps = (duration.coerceIn(0, 10_000) / 25).toInt().coerceAtLeast(1)
        val worker = workerFor(type)
        for (step in 1..steps) {
            try {
                worker.schedule({
                    if (!released && layerVersions[type] == version) {
                        layerGains[type] = start + (target.coerceIn(0f, 1f) - start) * step / steps
                        active.forEach { (key, playing) -> if (key.first == type) applyGain(type, playing) }
                    }
                }, duration.coerceIn(0, 10_000) * step / steps, TimeUnit.MILLISECONDS)
            } catch (_: java.util.concurrent.RejectedExecutionException) {}
        }
    }

    private fun applyGain(type: AudioCueType, playing: Playing) {
        val clip = playing.clip
        if (!clip.isOpen || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val master = if (isMuted) 0f else masterGain
        val volume = (master * (userGains[type] ?: 1f) * (layerGains[type] ?: 1f) * playing.cueGain * playing.envelope).coerceIn(0f, 1f)
        control.value = if (volume <= .0001f) control.minimum else (20 * log10(volume)).coerceIn(control.minimum, control.maximum)
    }

    fun isPlaying(type: AudioCueType, cueId: String): Boolean {
        val key = type to normalize(cueId)
        val p = active[key] ?: return false
        return p.clip.isOpen && p.clip.isRunning
    }

    fun activeCues(type: AudioCueType): List<String> =
        active.keys.filter { it.first == type }.map { it.second }

    fun isPcmCached(cueId: String): Boolean =
        sfxPcmCache.containsKey(normalize(cueId))

    fun fadingOutCount(): Int = fadingOutClips.size

    override fun release() {
        released = true
        musicWorker.shutdownNow()
        sfxWorker.shutdownNow()
        audioLoader.shutdownNow()
        active.values.forEach {
            try {
                it.clip.stop()
                it.clip.close()
            } catch (_: Exception) {}
        }
        active.clear()
        fadingOutClips.forEach { clip ->
            try {
                clip.stop()
                clip.close()
            } catch (_: Exception) {}
        }
        fadingOutClips.clear()
        sfxPcmCache.clear()
    }

    private fun resamplePcm(cached: CachedPcm, pitch: Float): ByteArray {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        if (kotlin.math.abs(clampedPitch - 1.0f) < 0.01f) {
            return cached.bytes
        }
        val format = cached.format
        val channels = format.channels
        val bytesPerSample = 2 // 16-bit signed PCM
        val frameSize = channels * bytesPerSample
        val totalFrames = cached.bytes.size / frameSize
        if (totalFrames <= 1) return cached.bytes

        val outFrames = (totalFrames / clampedPitch).toInt().coerceAtLeast(1)
        val outBytes = ByteArray(outFrames * frameSize)

        val srcShorts = ShortArray(totalFrames * channels)
        var byteIdx = 0
        for (i in srcShorts.indices) {
            val b0 = cached.bytes[byteIdx].toInt() and 0xFF
            val b1 = cached.bytes[byteIdx + 1].toInt()
            srcShorts[i] = ((b1 shl 8) or b0).toShort()
            byteIdx += 2
        }

        var outByteIdx = 0
        for (f in 0 until outFrames) {
            val srcPos = f * clampedPitch
            val srcFrame0 = srcPos.toInt().coerceIn(0, totalFrames - 1)
            val srcFrame1 = (srcFrame0 + 1).coerceIn(0, totalFrames - 1)
            val fraction = (srcPos - srcFrame0).coerceIn(0.0f, 1.0f)

            for (ch in 0 until channels) {
                val s0 = srcShorts[srcFrame0 * channels + ch].toFloat()
                val s1 = srcShorts[srcFrame1 * channels + ch].toFloat()
                val interpolated = (s0 + (s1 - s0) * fraction).toInt().coerceIn(-32768, 32767)

                outBytes[outByteIdx] = (interpolated and 0xFF).toByte()
                outBytes[outByteIdx + 1] = ((interpolated ushr 8) and 0xFF).toByte()
                outByteIdx += 2
            }
        }
        return outBytes
    }

    companion object {
        val DEFAULT_PREWARM_CUES = listOf(
            "ui_click", "ui_back", "ui_confirm", "ui_error", "ui_room_move", "ui_title_start",
            "sfx_ui_button_click", "sfx_ui_cancel", "sfx_ui_confirm", "sfx_ui_tab_switch",
            "sfx_ui_item_pickup", "sfx_ui_equip_item", "sfx_ui_error",
            "sfx_door_airlock_open", "sfx_door_airlock_close", "sfx_door_unlock",
            "shield_block", "shield_break", "battle_start",
            "voice_murmur_nova_01", "voice_murmur_nova_02", "voice_murmur_nova_03", "voice_murmur_nova_04",
            "voice_murmur_orion_01", "voice_murmur_orion_02", "voice_murmur_orion_03", "voice_murmur_orion_04",
            "voice_murmur_zeke_01", "voice_murmur_zeke_02", "voice_murmur_zeke_03", "voice_murmur_zeke_04",
            "voice_murmur_gh0st_01", "voice_murmur_gh0st_02", "voice_murmur_gh0st_03", "voice_murmur_gh0st_04",
            "voice_murmur_female_01", "voice_murmur_female_02", "voice_murmur_female_03", "voice_murmur_female_04",
            "voice_murmur_male_01", "voice_murmur_male_02", "voice_murmur_male_03", "voice_murmur_male_04"
        )
    }
}
