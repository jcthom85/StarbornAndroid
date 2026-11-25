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
    prewarmDefaults: Boolean = true,
    private val clipFactory: (AudioFormat) -> Clip = { format ->
        AudioSystem.getLine(DataLine.Info(Clip::class.java, format)) as Clip
    }
) : AudioDriver {
    private data class Playing(val clip: Clip, val cueGain: Float, val loop: Boolean, var envelope: Float = 1f, var fadeVersion: Long = 0)
    private data class CachedPcm(val format: AudioFormat, val bytes: ByteArray)

    private val musicWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "starborn-music-worker").apply { isDaemon = true }
    }
    private val sfxWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "starborn-sfx-worker").apply { isDaemon = true }
    }
    private val audioLoader: ExecutorService = Executors.newFixedThreadPool(3) { task ->
        Thread(task, "starborn-audio-loader").apply { isDaemon = true }
    }

    private val active = ConcurrentHashMap<Pair<AudioCueType, String>, Playing>()
    private val polyphonicSfx = ConcurrentHashMap<Pair<AudioCueType, String>, java.util.concurrent.CopyOnWriteArrayList<Playing>>()
    private val fadingOutClips = ConcurrentHashMap.newKeySet<Clip>()
    private val sfxPcmCache = ConcurrentHashMap<String, CachedPcm>()
    private val userGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerVersions = ConcurrentHashMap<AudioCueType, Long>()
    private val reported = ConcurrentHashMap.newKeySet<String>()
    private val musicGeneration = AtomicLong(0)
    private val ambientGeneration = AtomicLong(0)
    private var pendingMusicCue: String? = null // Owned by musicWorker.
    private var pendingAmbientCue: String? = null // Owned by musicWorker.
    private val musicPositions = mutableMapOf<String, Long>() // Owned by musicWorker.
    private val overlayPausedMusicCues = ConcurrentHashMap.newKeySet<String>()
    private var backgroundPaused = false // Owned by musicWorker.

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
            polyphonicSfx.entries.toList().forEach { (key, voices) ->
                voices.toList().forEach { playing ->
                    if (!playing.loop && !playing.clip.isRunning && playing.clip.framePosition >= playing.clip.frameLength) {
                        if (voices.remove(playing)) {
                            try {
                                playing.clip.close()
                            } catch (_: Exception) {}
                        }
                    }
                }
                if (voices.isEmpty()) {
                    polyphonicSfx.remove(key, voices)
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
        polyphonicSfx.forEach { (key, voices) -> voices.forEach { applyGain(key.first, it) } }
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
        if (command.type == AudioCueType.MUSIC && pendingMusicCue == key.second) {
            musicGeneration.incrementAndGet()
            pendingMusicCue = null
        }
        if (command.type == AudioCueType.AMBIENT && pendingAmbientCue == key.second) {
            ambientGeneration.incrementAndGet()
            pendingAmbientCue = null
        }
        polyphonicSfx.remove(key)?.forEach { voice ->
            if (command.fadeMs > 0) {
                fadingOutClips.add(voice.clip)
                fade(command.type, voice, 0f, command.fadeMs) {
                    fadingOutClips.remove(voice.clip)
                    try {
                        voice.clip.stop()
                        voice.clip.close()
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    voice.clip.stop()
                    voice.clip.close()
                } catch (_: Exception) {}
            }
        }
        val playing = active.remove(key) ?: return
        if (command.type == AudioCueType.MUSIC) {
            overlayPausedMusicCues.remove(key.second)
            musicPositions[key.second] = playing.clip.microsecondPosition
            refreshAmbientCoexistenceGains()
        }
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

    override fun resetMusicPlayback() {
        enqueue(musicWorker) {
            musicGeneration.incrementAndGet()
            pendingMusicCue = null
            overlayPausedMusicCues.clear()
            active.entries.filter { it.key.first == AudioCueType.MUSIC }.forEach { (key, playing) ->
                if (active.remove(key, playing)) {
                    playing.clip.stop()
                    playing.clip.close()
                }
            }
            musicPositions.clear()
            refreshAmbientCoexistenceGains()
        }
    }

    override fun hasCue(cueId: String): Boolean {
        val normalized = normalize(cueId)
        return normalized.isNotEmpty() && resolvePath(normalized) != null
    }

    override fun currentMusicPositionMs(expectedCueId: String?): Long? {
        val playing = if (expectedCueId != null) {
            val normalized = normalize(expectedCueId)
            if (normalized.isEmpty()) return null
            active[AudioCueType.MUSIC to normalized]
        } else {
            active.entries.firstOrNull { it.key.first == AudioCueType.MUSIC }?.value
        } ?: return null
        return try {
            if (!playing.clip.isOpen) {
                null
            } else {
                val lenUs = playing.clip.microsecondLength
                val posUs = playing.clip.microsecondPosition
                val wrappedUs = if (lenUs > 0L) Math.floorMod(posUs, lenUs) else posUs.coerceAtLeast(0L)
                wrappedUs / 1_000L
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun restartCurrentMusicFromStart(expectedCueId: String?) {
        enqueue(musicWorker) {
            val targetEntry = if (expectedCueId != null) {
                val normalized = normalize(expectedCueId)
                if (normalized.isEmpty()) return@enqueue
                active.entries.firstOrNull { it.key == (AudioCueType.MUSIC to normalized) }
            } else {
                active.entries.firstOrNull { it.key.first == AudioCueType.MUSIC }
            } ?: return@enqueue
            val cueKey = targetEntry.key.second
            val playing = targetEntry.value
            musicPositions.remove(cueKey)
            try {
                if (playing.clip.isOpen) {
                    playing.clip.microsecondPosition = 0L
                    if (!backgroundPaused && !overlayPausedMusicCues.contains(cueKey) && !playing.clip.isRunning) {
                        if (playing.loop) playing.clip.loop(Clip.LOOP_CONTINUOUSLY) else playing.clip.start()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun setMusicPausedForOverlay(paused: Boolean, expectedCueId: String?) {
        enqueue(musicWorker) {
            val targetEntry = if (expectedCueId != null) {
                val normalized = normalize(expectedCueId)
                if (normalized.isEmpty()) return@enqueue
                active.entries.firstOrNull { it.key == (AudioCueType.MUSIC to normalized) }
            } else {
                active.entries.firstOrNull { it.key.first == AudioCueType.MUSIC }
            }
            val cueKey = targetEntry?.key?.second ?: expectedCueId?.let(::normalize)?.takeIf { it.isNotEmpty() } ?: return@enqueue
            if (paused) overlayPausedMusicCues.add(cueKey) else overlayPausedMusicCues.remove(cueKey)
            val playing = targetEntry?.value ?: return@enqueue
            try {
                if (paused) {
                    playing.clip.stop()
                } else if (!backgroundPaused && playing.clip.isOpen && !playing.clip.isRunning) {
                    if (playing.loop) playing.clip.loop(Clip.LOOP_CONTINUOUSLY) else playing.clip.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun setBackgroundPaused(paused: Boolean) {
        enqueue(musicWorker) {
            if (backgroundPaused == paused) return@enqueue
            backgroundPaused = paused
            active.entries.filter { it.key.first == AudioCueType.MUSIC }.forEach { (key, playing) ->
                if (paused) {
                    playing.clip.stop()
                } else if (!overlayPausedMusicCues.contains(key.second)) {
                    if (playing.loop) playing.clip.loop(Clip.LOOP_CONTINUOUSLY) else playing.clip.start()
                }
            }
        }
    }

    override fun setUserGain(type: AudioCueType, gain: Float) {
        enqueue(workerFor(type)) {
            userGains[type] = gain.coerceIn(0f, 1f)
            active.forEach { (key, playing) -> if (key.first == type) applyGain(type, playing) }
            polyphonicSfx.forEach { (key, voices) -> if (key.first == type) voices.forEach { applyGain(type, it) } }
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

    private fun createPcmFormat(format: AudioFormat): AudioFormat {
        val sampleRate = if (format.sampleRate > 0) format.sampleRate else 44100f
        val channels = if (format.channels > 0) format.channels else 2
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate, 16,
            channels, channels * 2,
            sampleRate, false
        )
    }

    private fun loadPcm(cue: String): CachedPcm? {
        val path = resolvePath(cue) ?: return null
        return assetProvider.open(path)?.use { raw ->
            AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                val format = encoded.format
                val pcmFormat = createPcmFormat(format)
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
        if (!command.restartFromBeginning && command.loop && existing?.loop == true && existing.clip.isRunning) return

        when (command.type) {
            AudioCueType.MUSIC -> playMusicCrossfaded(command, key, cue)
            AudioCueType.AMBIENT -> playAmbientCrossfaded(command, key, cue)
            else -> playSfxOrVoice(command, key, cue)
        }
    }

    private fun playAmbientCrossfaded(command: AudioCommand.Play, key: Pair<AudioCueType, String>, cue: String) {
        if (!command.restartFromBeginning && command.loop && pendingAmbientCue == cue) return
        val currentGen = ambientGeneration.incrementAndGet()
        pendingAmbientCue = cue

        val outgoing = active.entries.filter { it.key.first == AudioCueType.AMBIENT && it.key != key }
        outgoing.forEach { (oldKey, oldPlaying) ->
            if (active.remove(oldKey, oldPlaying)) {
                val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 280L
                fadingOutClips.add(oldPlaying.clip)
                fade(AudioCueType.AMBIENT, oldPlaying, 0f, fadeOutMs) {
                    fadingOutClips.remove(oldPlaying.clip)
                    try {
                        oldPlaying.clip.stop()
                        oldPlaying.clip.close()
                    } catch (_: Exception) {}
                }
            }
        }

        active.remove(key)?.let { oldSame ->
            try {
                oldSame.clip.stop()
                oldSame.clip.close()
            } catch (_: Exception) {}
        }

        if (released) return
        try {
            audioLoader.execute {
                if (released || ambientGeneration.get() != currentGen) return@execute
                var loadedClip: Clip? = null
                try {
                    val path = resolvePath(cue) ?: return@execute
                    assetProvider.open(path)?.use { raw ->
                        AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                            val format = encoded.format
                            val pcmFormat = createPcmFormat(format)
                            val clip = clipFactory(if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) format else pcmFormat)
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

                    if (released || ambientGeneration.get() != currentGen) {
                        try { loadedClip?.close() } catch (_: Exception) {}
                        return@execute
                    }

                    val readyClip = loadedClip ?: return@execute
                    try {
                        musicWorker.execute {
                            if (released || ambientGeneration.get() != currentGen) {
                                try { readyClip.close() } catch (_: Exception) {}
                                return@execute
                            }
                            pendingAmbientCue = null

                            active.entries.filter { it.key.first == AudioCueType.AMBIENT && it.key != key }.forEach { (oldKey, oldPlaying) ->
                                if (active.remove(oldKey, oldPlaying)) {
                                    val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 280L
                                    fadingOutClips.add(oldPlaying.clip)
                                    fade(AudioCueType.AMBIENT, oldPlaying, 0f, fadeOutMs) {
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
                            applyGain(AudioCueType.AMBIENT, playing)
                            if (command.loop) {
                                applyLoopPoints(readyClip, command.loopStartMs, command.loopEndMs)
                                readyClip.loop(Clip.LOOP_CONTINUOUSLY)
                            } else {
                                readyClip.start()
                            }
                            if (command.fadeMs > 0) {
                                fade(AudioCueType.AMBIENT, playing, 1f, command.fadeMs)
                            }
                        }
                    } catch (_: java.util.concurrent.RejectedExecutionException) {
                        try { readyClip.close() } catch (_: Exception) {}
                    }
                } catch (error: Exception) {
                    try { loadedClip?.close() } catch (_: Exception) {}
                    if (reported.add(cue)) System.err.println("Starborn audio '$cue': ${error.message}")
                } finally {
                    enqueue(musicWorker) {
                        if (ambientGeneration.get() == currentGen) pendingAmbientCue = null
                    }
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {}
    }

    private fun playMusicCrossfaded(command: AudioCommand.Play, key: Pair<AudioCueType, String>, cue: String) {
        if (!command.restartFromBeginning && command.loop && pendingMusicCue == cue) return
        val currentGen = musicGeneration.incrementAndGet()
        pendingMusicCue = cue

        // 1. Immediately extract and crossfade-out any other active music
        val outgoing = active.entries.filter { it.key.first == AudioCueType.MUSIC && it.key != key }
        outgoing.forEach { (oldKey, oldPlaying) ->
            if (active.remove(oldKey, oldPlaying)) {
                musicPositions[oldKey.second] = oldPlaying.clip.microsecondPosition
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
            musicPositions[cue] = oldSame.clip.microsecondPosition
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
                            val pcmFormat = createPcmFormat(format)
                            val clip = clipFactory(if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) format else pcmFormat)
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
                            pendingMusicCue = null

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
                            if (command.restartFromBeginning) musicPositions.remove(cue)
                            readyClip.microsecondPosition = if (command.loop && !command.restartFromBeginning)
                                musicPositions[cue] ?: 0L else 0L
                            if (!backgroundPaused) {
                                if (command.loop) {
                                    applyLoopPoints(readyClip, command.loopStartMs, command.loopEndMs)
                                    readyClip.loop(Clip.LOOP_CONTINUOUSLY)
                                } else {
                                    readyClip.start()
                                }
                            }
                            if (command.fadeMs > 0) {
                                fade(AudioCueType.MUSIC, playing, 1f, command.fadeMs)
                            }
                            refreshAmbientCoexistenceGains()
                        }
                    } catch (_: java.util.concurrent.RejectedExecutionException) {
                        try { readyClip.close() } catch (_: Exception) {}
                    }
                } catch (error: Exception) {
                    try { loadedClip?.close() } catch (_: Exception) {}
                    if (reported.add(cue)) System.err.println("Starborn audio '$cue': ${error.message}")
                } finally {
                    enqueue(musicWorker) {
                        if (musicGeneration.get() == currentGen) pendingMusicCue = null
                    }
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {}
    }

    private fun playSfxOrVoice(command: AudioCommand.Play, key: Pair<AudioCueType, String>, cue: String) {
        val isSfx = command.type == AudioCueType.UI || command.type == AudioCueType.BATTLE || command.type == AudioCueType.VOICE
        val allowPolyphony = !command.loop && (command.type == AudioCueType.UI || command.type == AudioCueType.BATTLE)
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
            if (!allowPolyphony) {
                active.remove(key)?.let {
                    try {
                        it.clip.stop()
                        it.clip.close()
                    } catch (_: Exception) {}
                }
            }

            val cached = if (isSfx) sfxPcmCache[cue] else null
            if (cached != null) {
                clip = clipFactory(cached.format)
                val pcmBytes = if (command.pitch != 1.0f) resamplePcm(cached, command.pitch) else cached.bytes
                clip.open(cached.format, pcmBytes, 0, pcmBytes.size)
            } else {
                val path = resolvePath(cue) ?: return
                assetProvider.open(path)?.use { raw ->
                    AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                        val format = encoded.format
                        val pcmFormat = createPcmFormat(format)
                        clip = clipFactory(if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) format else pcmFormat)
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
            if (allowPolyphony) {
                val voices = polyphonicSfx.computeIfAbsent(key) { java.util.concurrent.CopyOnWriteArrayList() }
                voices.toList().forEach { prev ->
                    if (!prev.clip.isRunning && prev.clip.framePosition >= prev.clip.frameLength) {
                        if (voices.remove(prev)) {
                            try { prev.clip.close() } catch (_: Exception) {}
                        }
                    }
                }
                while (voices.size >= MAX_SFX_VOICES_PER_CUE) {
                    val oldest = voices.removeAt(0)
                    try {
                        oldest.clip.stop()
                        oldest.clip.close()
                    } catch (_: Exception) {}
                }
                voices.add(playing)
            } else {
                active[key] = playing
            }
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

    private fun applyLoopPoints(clip: Clip, loopStartMs: Long?, loopEndMs: Long?) {
        if (loopStartMs == null && loopEndMs == null) return
        val frameLength = clip.frameLength
        if (frameLength <= 2) return
        val frameRate = clip.format.frameRate.takeIf { it > 0f } ?: 44100f
        val startFrame = (((loopStartMs ?: 0L).coerceAtLeast(0L) * frameRate) / 1000f)
            .toInt()
            .coerceIn(0, frameLength - 2)
        val endFrame = loopEndMs?.let {
            ((it * frameRate) / 1000f).toInt().coerceIn(startFrame + 1, frameLength - 1)
        } ?: -1
        try {
            clip.setLoopPoints(startFrame, endFrame)
        } catch (_: Exception) { }
    }

    private fun fade(type: AudioCueType, playing: Playing, target: Float, duration: Long, done: () -> Unit = {}) {
        val version = ++playing.fadeVersion
        val start = playing.envelope
        val steps = (duration.coerceIn(0, 10_000) / 25).toInt().coerceAtLeast(1)
        val worker = workerFor(type)
        val equalPower = type == AudioCueType.MUSIC || type == AudioCueType.AMBIENT
        for (step in 1..steps) {
            try {
                worker.schedule({
                    if (!released && playing.fadeVersion == version && playing.clip.isOpen) {
                        val progress = step.toDouble() / steps.toDouble()
                        val factor = if (equalPower) {
                            if (target >= start) kotlin.math.sin(progress * Math.PI / 2.0).toFloat()
                            else (1.0 - kotlin.math.cos(progress * Math.PI / 2.0)).toFloat()
                        } else {
                            progress.toFloat()
                        }
                        playing.envelope = start + (target - start) * factor
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
                        polyphonicSfx.forEach { (key, voices) -> if (key.first == type) voices.forEach { applyGain(type, it) } }
                    }
                }, duration.coerceIn(0, 10_000) * step / steps, TimeUnit.MILLISECONDS)
            } catch (_: java.util.concurrent.RejectedExecutionException) {}
        }
    }

    private fun refreshAmbientCoexistenceGains() {
        active.forEach { (key, playing) ->
            if (key.first == AudioCueType.AMBIENT) {
                applyGain(AudioCueType.AMBIENT, playing)
            }
        }
    }

    private fun applyGain(type: AudioCueType, playing: Playing) {
        val clip = playing.clip
        if (!clip.isOpen || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val master = if (isMuted) 0f else masterGain
        val coexistenceTrim = if (type == AudioCueType.AMBIENT && active.keys.any { it.first == AudioCueType.MUSIC }) 0.50f else 1f
        val volume = (master * (userGains[type] ?: 1f) * (layerGains[type] ?: 1f) * playing.cueGain * playing.envelope * coexistenceTrim).coerceIn(0f, 1f)
        control.value = if (volume <= .0001f) control.minimum else (20 * log10(volume)).coerceIn(control.minimum, control.maximum)
    }

    fun isPlaying(type: AudioCueType, cueId: String): Boolean {
        val key = type to normalize(cueId)
        val single = active[key]
        if (single != null && single.clip.isOpen && single.clip.isRunning) return true
        return polyphonicSfx[key]?.any { it.clip.isOpen && it.clip.isRunning } == true
    }

    fun activeVoiceCount(type: AudioCueType, cueId: String): Int {
        val key = type to normalize(cueId)
        val polyCount = polyphonicSfx[key]?.count { it.clip.isOpen && it.clip.isRunning } ?: 0
        val singleCount = if (active[key]?.let { it.clip.isOpen && it.clip.isRunning } == true) 1 else 0
        return polyCount + singleCount
    }

    fun activeCues(type: AudioCueType): List<String> =
        (active.keys.filter { it.first == type }.map { it.second } +
            polyphonicSfx.entries.filter { it.key.first == type && it.value.any { v -> v.clip.isOpen && v.clip.isRunning } }.map { it.key.second })
            .distinct()

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
        polyphonicSfx.values.forEach { voices ->
            voices.forEach {
                try {
                    it.clip.stop()
                    it.clip.close()
                } catch (_: Exception) {}
            }
        }
        polyphonicSfx.clear()
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
        const val MAX_SFX_VOICES_PER_CUE = 4
        val DEFAULT_PREWARM_CUES = listOf(
            "ui_click", "ui_back", "ui_confirm", "ui_error", "ui_room_move", "ui_title_start",
            "sfx_ui_button_click", "sfx_ui_cancel", "sfx_ui_confirm", "sfx_ui_tab_switch",
            "sfx_ui_item_pickup", "sfx_ui_equip_item", "sfx_ui_error",
            "sfx_door_airlock_open", "sfx_door_airlock_close", "sfx_door_unlock",
            "sfx_combat_transition_slam", "sfx_hit_kinetic", "sfx_hit_crit",
            "sfx_rest_chime", "sfx_chime_splice", "sfx_skill_source_harmonic", "sfx_arcade_game_over",
            "shield_block", "shield_break", "battle_start",
            "voice_murmur_nova_01", "voice_murmur_nova_02", "voice_murmur_nova_03", "voice_murmur_nova_04",
            "voice_murmur_orion_01", "voice_murmur_orion_02", "voice_murmur_orion_03", "voice_murmur_orion_04",
            "voice_murmur_zeke_01", "voice_murmur_zeke_02", "voice_murmur_zeke_03", "voice_murmur_zeke_04",
            "voice_murmur_gh0st_01", "voice_murmur_gh0st_02", "voice_murmur_gh0st_03", "voice_murmur_gh0st_04",
            "voice_murmur_female_01", "voice_murmur_female_02", "voice_murmur_female_03", "voice_murmur_female_04",
            "voice_murmur_male_01", "voice_murmur_male_02", "voice_murmur_male_03", "voice_murmur_male_04",
            "voice_murmur_machine_01", "voice_murmur_machine_02", "voice_murmur_machine_03", "voice_murmur_machine_04"
        )
    }
}
