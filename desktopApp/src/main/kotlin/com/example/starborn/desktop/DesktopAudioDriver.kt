package com.example.starborn.desktop

import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.domain.audio.*
import java.io.BufferedInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.*
import kotlin.math.log10
import kotlin.math.min

/**
 * High-performance desktop audio driver.
 * - Streams long MUSIC and AMBIENT MP3/WAV/OGG tracks via 4 KB SourceDataLine chunks (~20 KB RAM vs ~42 MB full-PCM Clip buffers).
 * - Dedicated music/ambience worker prevents track changes from blocking UI and battle SFX.
 * - Dedicated SFX worker + in-memory PCM cache + 4-voice polyphony for zero-latency UI, combat, and resampled voice cues.
 * - Seamless equal-power crossfading for music and ambience with track-position memory and gapless loop points.
 */
class DesktopAudioDriver private constructor(
    private val assetProvider: AssetProvider,
    prewarmDefaults: Boolean,
    private val customClipFactory: ((AudioFormat) -> Clip)?,
    private val customStreamLineFactory: ((AudioFormat) -> SourceDataLine)?
) : AudioDriver {

    constructor(
        assetProvider: AssetProvider,
        prewarmDefaults: Boolean = true,
        clipFactory: ((AudioFormat) -> Clip)? = null
    ) : this(
        assetProvider = assetProvider,
        prewarmDefaults = prewarmDefaults,
        customClipFactory = clipFactory,
        customStreamLineFactory = null
    )

    private interface AudioPlaybackHandle {
        val isOpen: Boolean
        val isRunning: Boolean
        val isStreaming: Boolean
        val framePosition: Long
        val frameLength: Long
        var microsecondPosition: Long
        val microsecondLength: Long
        fun start()
        fun loopContinuously(loopStartMs: Long? = null, loopEndMs: Long? = null)
        fun stop()
        fun close()
        fun applyLinearVolume(volume: Float)
    }

    private class ClipPlaybackHandle(val clip: Clip) : AudioPlaybackHandle {
        override val isOpen: Boolean get() = clip.isOpen
        override val isRunning: Boolean get() = clip.isRunning
        override val isStreaming: Boolean get() = false
        override val framePosition: Long get() = clip.framePosition.toLong()
        override val frameLength: Long get() = clip.frameLength.toLong()
        override var microsecondPosition: Long
            get() = clip.microsecondPosition
            set(value) { clip.microsecondPosition = value }
        override val microsecondLength: Long get() = clip.microsecondLength

        override fun start() {
            clip.start()
        }

        override fun loopContinuously(loopStartMs: Long?, loopEndMs: Long?) {
            applyClipLoopPoints(clip, loopStartMs, loopEndMs)
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        }

        override fun stop() {
            clip.stop()
        }

        override fun close() {
            clip.close()
        }

        override fun applyLinearVolume(volume: Float) {
            if (!clip.isOpen || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
            val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val clamped = volume.coerceIn(0f, 1f)
            control.value = if (clamped <= 0.0001f) control.minimum else (20f * log10(clamped)).coerceIn(control.minimum, control.maximum)
        }

        companion object {
            private fun applyClipLoopPoints(clip: Clip, loopStartMs: Long?, loopEndMs: Long?) {
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
                } catch (_: Exception) {}
            }
        }
    }

    private class StreamingPlaybackHandle(
        private val cue: String,
        private val pcmFormat: AudioFormat,
        private val line: SourceDataLine,
        private val openDecodedStream: () -> AudioInputStream?
    ) : AudioPlaybackHandle {
        private val sampleRate = pcmFormat.sampleRate.takeIf { it > 0f } ?: 44100f
        private val frameSize = pcmFormat.frameSize.takeIf { it > 0 } ?: (pcmFormat.channels.coerceAtLeast(1) * 2)
        private val stateLock = Object()
        private val currentFrame = AtomicLong(0L)
        private val knownTotalFrames = AtomicLong(-1L)
        private val pendingSeekFrame = AtomicLong(-1L)

        @Volatile private var closed = false
        @Volatile private var running = false
        @Volatile private var looping = false
        @Volatile private var loopStartFrame = 0L
        @Volatile private var loopEndFrame = Long.MAX_VALUE
        @Volatile private var softwareLinearGain = 1f
        @Volatile private var pumpThread: Thread? = null

        private val hasHardwareGain: Boolean = runCatching {
            line.isOpen && line.isControlSupported(FloatControl.Type.MASTER_GAIN)
        }.getOrDefault(false)

        override val isOpen: Boolean get() = !closed && line.isOpen
        override val isRunning: Boolean get() = !closed && running
        override val isStreaming: Boolean get() = true
        override val framePosition: Long get() = currentFrame.get()
        override val frameLength: Long get() = knownTotalFrames.get().coerceAtLeast(1L)

        override var microsecondPosition: Long
            get() = ((currentFrame.get().toDouble() * 1_000_000.0) / sampleRate.toDouble()).toLong().coerceAtLeast(0L)
            set(value) {
                val targetFrame = ((value.coerceAtLeast(0L).toDouble() * sampleRate.toDouble()) / 1_000_000.0).toLong().coerceAtLeast(0L)
                currentFrame.set(targetFrame)
                pendingSeekFrame.set(targetFrame)
                try {
                    line.flush()
                } catch (_: Exception) {}
                synchronized(stateLock) { stateLock.notifyAll() }
            }

        override val microsecondLength: Long
            get() {
                val total = knownTotalFrames.get()
                return if (total > 0L) ((total.toDouble() * 1_000_000.0) / sampleRate.toDouble()).toLong() else -1L
            }

        override fun start() {
            if (closed) return
            looping = false
            running = true
            ensurePumpStarted()
            synchronized(stateLock) { stateLock.notifyAll() }
        }

        override fun loopContinuously(loopStartMs: Long?, loopEndMs: Long?) {
            if (closed) return
            loopStartFrame = loopStartMs?.let { ((it.coerceAtLeast(0L) * sampleRate) / 1000f).toLong() } ?: 0L
            loopEndFrame = loopEndMs?.let { ((it.coerceAtLeast(1L) * sampleRate) / 1000f).toLong().coerceAtLeast(loopStartFrame + 1L) } ?: Long.MAX_VALUE
            looping = true
            running = true
            ensurePumpStarted()
            synchronized(stateLock) { stateLock.notifyAll() }
        }

        override fun stop() {
            running = false
            try {
                line.stop()
            } catch (_: Exception) {}
            synchronized(stateLock) { stateLock.notifyAll() }
        }

        override fun close() {
            if (closed) return
            closed = true
            running = false
            synchronized(stateLock) { stateLock.notifyAll() }
            pumpThread?.interrupt()
            try {
                line.stop()
                line.flush()
                line.close()
            } catch (_: Exception) {}
        }

        override fun applyLinearVolume(volume: Float) {
            val clamped = volume.coerceIn(0f, 1f)
            softwareLinearGain = clamped
            if (hasHardwareGain && line.isOpen) {
                try {
                    val control = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    control.value = if (clamped <= 0.0001f) control.minimum else (20f * log10(clamped)).coerceIn(control.minimum, control.maximum)
                } catch (_: Exception) {}
            }
        }

        private fun ensurePumpStarted() {
            synchronized(stateLock) {
                if (pumpThread != null || closed) return
                val thread = Thread(::pumpLoop, "starborn-audio-stream-$cue").apply { isDaemon = true }
                pumpThread = thread
                thread.start()
            }
        }

        private fun skipFrames(stream: AudioInputStream, targetFrames: Long) {
            if (targetFrames <= 0L) return
            var remainingBytes = targetFrames * frameSize.toLong()
            val skipBuf = ByteArray(STREAM_CHUNK_BYTES)
            while (remainingBytes > 0L && !closed) {
                val skipped = stream.skip(remainingBytes)
                if (skipped > 0L) {
                    remainingBytes -= skipped
                } else {
                    val toRead = min(skipBuf.size.toLong(), remainingBytes).toInt()
                    val read = stream.read(skipBuf, 0, toRead)
                    if (read <= 0) break
                    remainingBytes -= read.toLong()
                }
            }
        }

        private fun pumpLoop() {
            val readBuffer = ByteArray(STREAM_CHUNK_BYTES)
            val scaledBuffer = ByteArray(STREAM_CHUNK_BYTES)
            var stream: AudioInputStream? = null
            try {
                while (!closed) {
                    if (!running) {
                        synchronized(stateLock) {
                            if (!running && !closed) {
                                try {
                                    stateLock.wait(100L)
                                } catch (_: InterruptedException) {}
                            }
                        }
                        continue
                    }

                    val seekFrame = pendingSeekFrame.getAndSet(-1L)
                    if (seekFrame >= 0L || stream == null) {
                        try { stream?.close() } catch (_: Exception) {}
                        stream = openDecodedStream()
                        if (stream == null) {
                            running = false
                            continue
                        }
                        val target = if (seekFrame >= 0L) seekFrame else currentFrame.get().coerceAtLeast(0L)
                        skipFrames(stream, target)
                        currentFrame.set(target)
                    }

                    val activeStream = stream ?: continue
                    val framesBeforeEnd = if (looping && loopEndFrame < Long.MAX_VALUE) {
                        (loopEndFrame - currentFrame.get()).coerceAtLeast(0L)
                    } else Long.MAX_VALUE

                    if (framesBeforeEnd == 0L) {
                        knownTotalFrames.compareAndSet(-1L, currentFrame.get())
                        try { activeStream.close() } catch (_: Exception) {}
                        stream = null
                        pendingSeekFrame.set(loopStartFrame)
                        continue
                    }

                    val maxBytesToRead = min(
                        readBuffer.size.toLong(),
                        if (framesBeforeEnd == Long.MAX_VALUE) readBuffer.size.toLong() else framesBeforeEnd * frameSize.toLong()
                    ).toInt().coerceAtLeast(frameSize)

                    val bytesRead = activeStream.read(readBuffer, 0, maxBytesToRead)
                    if (bytesRead <= 0) {
                        knownTotalFrames.compareAndSet(-1L, currentFrame.get().coerceAtLeast(1L))
                        try { activeStream.close() } catch (_: Exception) {}
                        stream = null
                        if (looping && !closed) {
                            pendingSeekFrame.set(loopStartFrame)
                        } else {
                            try {
                                line.drain()
                                line.stop()
                            } catch (_: Exception) {}
                            running = false
                        }
                        continue
                    }

                    val alignedBytes = bytesRead - (bytesRead % frameSize)
                    if (alignedBytes <= 0) continue

                    val outBytes = if (!hasHardwareGain) {
                        val gain = softwareLinearGain
                        var i = 0
                        while (i + 1 < alignedBytes) {
                            val lo = readBuffer[i].toInt() and 0xFF
                            val hi = readBuffer[i + 1].toInt()
                            val sample = ((hi shl 8) or lo).toShort().toInt()
                            val scaled = (sample * gain).toInt().coerceIn(-32768, 32767)
                            scaledBuffer[i] = (scaled and 0xFF).toByte()
                            scaledBuffer[i + 1] = ((scaled ushr 8) and 0xFF).toByte()
                            i += 2
                        }
                        scaledBuffer
                    } else {
                        readBuffer
                    }

                    if (!closed && running) {
                        if (!line.isRunning) {
                            line.start()
                        }
                        val written = line.write(outBytes, 0, alignedBytes)
                        if (written > 0) {
                            currentFrame.addAndGet((written / frameSize).toLong())
                        }
                    }
                }
            } catch (_: InterruptedException) {
                // Normal shutdown
            } catch (_: Exception) {
                running = false
            } finally {
                try { stream?.close() } catch (_: Exception) {}
            }
        }
    }

    private data class Playing(
        val handle: AudioPlaybackHandle,
        val cueGain: Float,
        val loop: Boolean,
        val loopStartMs: Long? = null,
        val loopEndMs: Long? = null,
        var envelope: Float = 1f,
        var fadeVersion: Long = 0
    )

    private data class CachedPcm(val format: AudioFormat, val bytes: ByteArray)

    private val useStreamingTracks: Boolean = customStreamLineFactory != null || customClipFactory == null
    private val effectiveClipFactory: (AudioFormat) -> Clip = customClipFactory ?: { format ->
        AudioSystem.getLine(DataLine.Info(Clip::class.java, format)) as Clip
    }
    private val effectiveStreamLineFactory: (AudioFormat) -> SourceDataLine = customStreamLineFactory ?: { format ->
        AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
    }

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
    private val polyphonicSfx = ConcurrentHashMap<Pair<AudioCueType, String>, CopyOnWriteArrayList<Playing>>()
    private val fadingOutHandles = ConcurrentHashMap.newKeySet<AudioPlaybackHandle>()
    private val sfxPcmCache = ConcurrentHashMap<String, CachedPcm>()
    private val userGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerGains = ConcurrentHashMap<AudioCueType, Float>()
    private val layerVersions = ConcurrentHashMap<AudioCueType, Long>()
    private val reported = ConcurrentHashMap.newKeySet<String>()
    private val musicGeneration = AtomicLong(0)
    private val ambientGeneration = AtomicLong(0)
    private var pendingMusicCue: String? = null
    private var pendingAmbientCue: String? = null
    private val musicPositions = mutableMapOf<String, Long>()
    private val overlayPausedMusicCues = ConcurrentHashMap.newKeySet<String>()
    private var backgroundPaused = false

    @Volatile private var masterGain: Float = 1f
    @Volatile private var isMuted: Boolean = false
    @Volatile private var released = false

    init {
        val cleanupTask = Runnable {
            active.entries.toList().forEach { (key, playing) ->
                if (!playing.loop && !playing.handle.isRunning && playing.handle.framePosition >= playing.handle.frameLength) {
                    if (active.remove(key, playing)) {
                        try {
                            playing.handle.close()
                        } catch (_: Exception) {}
                    }
                }
            }
            polyphonicSfx.entries.toList().forEach { (key, voices) ->
                voices.toList().forEach { playing ->
                    if (!playing.loop && !playing.handle.isRunning && playing.handle.framePosition >= playing.handle.frameLength) {
                        if (voices.remove(playing)) {
                            try {
                                playing.handle.close()
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
                fadingOutHandles.add(voice.handle)
                fade(command.type, voice, 0f, command.fadeMs) {
                    fadingOutHandles.remove(voice.handle)
                    try {
                        voice.handle.stop()
                        voice.handle.close()
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    voice.handle.stop()
                    voice.handle.close()
                } catch (_: Exception) {}
            }
        }
        val playing = active.remove(key) ?: return
        if (command.type == AudioCueType.MUSIC) {
            overlayPausedMusicCues.remove(key.second)
            musicPositions[key.second] = playing.handle.microsecondPosition
            refreshAmbientCoexistenceGains()
        }
        if (command.fadeMs > 0) {
            fadingOutHandles.add(playing.handle)
            fade(command.type, playing, 0f, command.fadeMs) {
                fadingOutHandles.remove(playing.handle)
                try {
                    playing.handle.stop()
                    playing.handle.close()
                } catch (_: Exception) {}
            }
        } else {
            try {
                playing.handle.stop()
                playing.handle.close()
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
                    playing.handle.stop()
                    playing.handle.close()
                }
            }
            musicPositions.clear()
            refreshAmbientCoexistenceGains()
        }
    }

    fun hasCue(cueId: String): Boolean {
        val normalized = normalize(cueId)
        return normalized.isNotEmpty() && resolvePath(normalized) != null
    }

    fun currentMusicPositionMs(expectedCueId: String? = null): Long? {
        val playing = if (expectedCueId != null) {
            val normalized = normalize(expectedCueId)
            if (normalized.isEmpty()) return null
            active[AudioCueType.MUSIC to normalized]
        } else {
            active.entries.firstOrNull { it.key.first == AudioCueType.MUSIC }?.value
        } ?: return null
        return try {
            if (!playing.handle.isOpen) {
                null
            } else {
                val lenUs = playing.handle.microsecondLength
                val posUs = playing.handle.microsecondPosition
                val wrappedUs = if (lenUs > 0L) Math.floorMod(posUs, lenUs) else posUs.coerceAtLeast(0L)
                wrappedUs / 1_000L
            }
        } catch (_: Exception) {
            null
        }
    }

    fun restartCurrentMusicFromStart(expectedCueId: String? = null) {
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
                if (playing.handle.isOpen) {
                    playing.handle.microsecondPosition = 0L
                    if (!backgroundPaused && !overlayPausedMusicCues.contains(cueKey) && !playing.handle.isRunning) {
                        if (playing.loop) playing.handle.loopContinuously(playing.loopStartMs, playing.loopEndMs) else playing.handle.start()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun setMusicPausedForOverlay(paused: Boolean, expectedCueId: String? = null) {
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
                    playing.handle.stop()
                } else if (!backgroundPaused && playing.handle.isOpen && !playing.handle.isRunning) {
                    if (playing.loop) playing.handle.loopContinuously(playing.loopStartMs, playing.loopEndMs) else playing.handle.start()
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
                    playing.handle.stop()
                } else if (!overlayPausedMusicCues.contains(key.second)) {
                    if (playing.loop) playing.handle.loopContinuously(playing.loopStartMs, playing.loopEndMs) else playing.handle.start()
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
        } catch (_: java.util.concurrent.RejectedExecutionException) {}
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

    private fun openTrackHandle(cue: String, path: String): AudioPlaybackHandle? {
        if (useStreamingTracks) {
            val probeFormat = assetProvider.open(path)?.use { raw ->
                AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                    createPcmFormat(encoded.format)
                }
            } ?: return null

            val line = effectiveStreamLineFactory(probeFormat)
            line.open(probeFormat, STREAM_LINE_BUFFER_BYTES)
            val streamOpener: () -> AudioInputStream? = {
                val raw = assetProvider.open(path)
                if (raw == null) null else {
                    try {
                        val encoded = AudioSystem.getAudioInputStream(BufferedInputStream(raw))
                        if (encoded.format.encoding == AudioFormat.Encoding.PCM_SIGNED &&
                            encoded.format.sampleSizeInBits == 16 &&
                            !encoded.format.isBigEndian
                        ) {
                            encoded
                        } else {
                            AudioSystem.getAudioInputStream(probeFormat, encoded)
                        }
                    } catch (e: Exception) {
                        try { raw.close() } catch (_: Exception) {}
                        null
                    }
                }
            }
            return StreamingPlaybackHandle(cue, probeFormat, line, streamOpener)
        } else {
            var loadedClip: Clip? = null
            try {
                assetProvider.open(path)?.use { raw ->
                    AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                        val format = encoded.format
                        val pcmFormat = createPcmFormat(format)
                        val clip = effectiveClipFactory(if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) format else pcmFormat)
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
                return loadedClip?.let(::ClipPlaybackHandle)
            } catch (e: Exception) {
                try { loadedClip?.close() } catch (_: Exception) {}
                throw e
            }
        }
    }

    private fun play(command: AudioCommand.Play) {
        val cue = normalize(command.cueId)
        if (cue.isEmpty()) return
        val key = command.type to cue
        val existing = active[key]
        if (!command.restartFromBeginning && command.loop && existing?.loop == true && existing.handle.isRunning) return

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
                fadingOutHandles.add(oldPlaying.handle)
                fade(AudioCueType.AMBIENT, oldPlaying, 0f, fadeOutMs) {
                    fadingOutHandles.remove(oldPlaying.handle)
                    try {
                        oldPlaying.handle.stop()
                        oldPlaying.handle.close()
                    } catch (_: Exception) {}
                }
            }
        }

        active.remove(key)?.let { oldSame ->
            try {
                oldSame.handle.stop()
                oldSame.handle.close()
            } catch (_: Exception) {}
        }

        if (released) return
        try {
            audioLoader.execute {
                if (released || ambientGeneration.get() != currentGen) return@execute
                var handle: AudioPlaybackHandle? = null
                try {
                    val path = resolvePath(cue) ?: return@execute
                    handle = openTrackHandle(cue, path)
                    if (released || ambientGeneration.get() != currentGen) {
                        try { handle?.close() } catch (_: Exception) {}
                        return@execute
                    }
                    val readyHandle = handle ?: return@execute
                    try {
                        musicWorker.execute {
                            if (released || ambientGeneration.get() != currentGen) {
                                try { readyHandle.close() } catch (_: Exception) {}
                                return@execute
                            }
                            pendingAmbientCue = null

                            active.entries.filter { it.key.first == AudioCueType.AMBIENT && it.key != key }.forEach { (oldKey, oldPlaying) ->
                                if (active.remove(oldKey, oldPlaying)) {
                                    val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 280L
                                    fadingOutHandles.add(oldPlaying.handle)
                                    fade(AudioCueType.AMBIENT, oldPlaying, 0f, fadeOutMs) {
                                        fadingOutHandles.remove(oldPlaying.handle)
                                        try {
                                            oldPlaying.handle.stop()
                                            oldPlaying.handle.close()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }

                            val startEnvelope = if (command.fadeMs > 0) 0f else 1f
                            val playing = Playing(
                                handle = readyHandle,
                                cueGain = command.gain,
                                loop = command.loop,
                                loopStartMs = command.loopStartMs,
                                loopEndMs = command.loopEndMs,
                                envelope = startEnvelope
                            )
                            active[key] = playing
                            applyGain(AudioCueType.AMBIENT, playing)
                            if (command.loop) {
                                readyHandle.loopContinuously(command.loopStartMs, command.loopEndMs)
                            } else {
                                readyHandle.start()
                            }
                            if (command.fadeMs > 0) {
                                fade(AudioCueType.AMBIENT, playing, 1f, command.fadeMs)
                            }
                        }
                    } catch (_: java.util.concurrent.RejectedExecutionException) {
                        try { readyHandle.close() } catch (_: Exception) {}
                    }
                } catch (error: Exception) {
                    try { handle?.close() } catch (_: Exception) {}
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

        val outgoing = active.entries.filter { it.key.first == AudioCueType.MUSIC && it.key != key }
        outgoing.forEach { (oldKey, oldPlaying) ->
            if (active.remove(oldKey, oldPlaying)) {
                musicPositions[oldKey.second] = oldPlaying.handle.microsecondPosition
                val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 400L
                fadingOutHandles.add(oldPlaying.handle)
                fade(AudioCueType.MUSIC, oldPlaying, 0f, fadeOutMs) {
                    fadingOutHandles.remove(oldPlaying.handle)
                    try {
                        oldPlaying.handle.stop()
                        oldPlaying.handle.close()
                    } catch (_: Exception) {}
                }
            }
        }

        active.remove(key)?.let { oldSame ->
            musicPositions[cue] = oldSame.handle.microsecondPosition
            try {
                oldSame.handle.stop()
                oldSame.handle.close()
            } catch (_: Exception) {}
        }

        if (released) return
        try {
            audioLoader.execute {
                if (released || musicGeneration.get() != currentGen) return@execute
                var handle: AudioPlaybackHandle? = null
                try {
                    val path = resolvePath(cue) ?: return@execute
                    handle = openTrackHandle(cue, path)
                    if (released || musicGeneration.get() != currentGen) {
                        try { handle?.close() } catch (_: Exception) {}
                        return@execute
                    }
                    val readyHandle = handle ?: return@execute
                    try {
                        musicWorker.execute {
                            if (released || musicGeneration.get() != currentGen) {
                                try { readyHandle.close() } catch (_: Exception) {}
                                return@execute
                            }
                            pendingMusicCue = null

                            active.entries.filter { it.key.first == AudioCueType.MUSIC && it.key != key }.forEach { (oldKey, oldPlaying) ->
                                if (active.remove(oldKey, oldPlaying)) {
                                    val fadeOutMs = if (command.fadeMs > 0) command.fadeMs else 400L
                                    fadingOutHandles.add(oldPlaying.handle)
                                    fade(AudioCueType.MUSIC, oldPlaying, 0f, fadeOutMs) {
                                        fadingOutHandles.remove(oldPlaying.handle)
                                        try {
                                            oldPlaying.handle.stop()
                                            oldPlaying.handle.close()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }

                            val startEnvelope = if (command.fadeMs > 0) 0f else 1f
                            val playing = Playing(
                                handle = readyHandle,
                                cueGain = command.gain,
                                loop = command.loop,
                                loopStartMs = command.loopStartMs,
                                loopEndMs = command.loopEndMs,
                                envelope = startEnvelope
                            )
                            active[key] = playing
                            applyGain(AudioCueType.MUSIC, playing)
                            if (command.restartFromBeginning) musicPositions.remove(cue)
                            readyHandle.microsecondPosition = if (command.loop && !command.restartFromBeginning)
                                musicPositions[cue] ?: 0L else 0L
                            if (!backgroundPaused) {
                                if (command.loop) {
                                    readyHandle.loopContinuously(command.loopStartMs, command.loopEndMs)
                                } else {
                                    readyHandle.start()
                                }
                            }
                            if (command.fadeMs > 0) {
                                fade(AudioCueType.MUSIC, playing, 1f, command.fadeMs)
                            }
                            refreshAmbientCoexistenceGains()
                        }
                    } catch (_: java.util.concurrent.RejectedExecutionException) {
                        try { readyHandle.close() } catch (_: Exception) {}
                    }
                } catch (error: Exception) {
                    try { handle?.close() } catch (_: Exception) {}
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
                        old.handle.stop()
                        old.handle.close()
                    } catch (_: Exception) {}
                }
            }
            if (!allowPolyphony) {
                active.remove(key)?.let {
                    try {
                        it.handle.stop()
                        it.handle.close()
                    } catch (_: Exception) {}
                }
            }

            val cached = if (isSfx) sfxPcmCache[cue] else null
            if (cached != null) {
                clip = effectiveClipFactory(cached.format)
                val pcmBytes = if (command.pitch != 1.0f) resamplePcm(cached, command.pitch) else cached.bytes
                clip.open(cached.format, pcmBytes, 0, pcmBytes.size)
            } else {
                val path = resolvePath(cue) ?: return
                assetProvider.open(path)?.use { raw ->
                    AudioSystem.getAudioInputStream(BufferedInputStream(raw)).use { encoded ->
                        val format = encoded.format
                        val pcmFormat = createPcmFormat(format)
                        clip = effectiveClipFactory(if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) format else pcmFormat)
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
            val handle = ClipPlaybackHandle(readyClip)
            val playing = Playing(
                handle = handle,
                cueGain = command.gain,
                loop = command.loop,
                loopStartMs = command.loopStartMs,
                loopEndMs = command.loopEndMs,
                envelope = if (command.fadeMs > 0) 0f else 1f
            )
            if (allowPolyphony) {
                val voices = polyphonicSfx.computeIfAbsent(key) { CopyOnWriteArrayList() }
                voices.toList().forEach { prev ->
                    if (!prev.handle.isRunning && prev.handle.framePosition >= prev.handle.frameLength) {
                        if (voices.remove(prev)) {
                            try { prev.handle.close() } catch (_: Exception) {}
                        }
                    }
                }
                while (voices.size >= MAX_SFX_VOICES_PER_CUE) {
                    val oldest = voices.removeAt(0)
                    try {
                        oldest.handle.stop()
                        oldest.handle.close()
                    } catch (_: Exception) {}
                }
                voices.add(playing)
            } else {
                active[key] = playing
            }
            applyGain(command.type, playing)
            if (command.loop) handle.loopContinuously(command.loopStartMs, command.loopEndMs) else handle.start()
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
        val equalPower = type == AudioCueType.MUSIC || type == AudioCueType.AMBIENT
        for (step in 1..steps) {
            try {
                worker.schedule({
                    if (!released && playing.fadeVersion == version && playing.handle.isOpen) {
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
        if (!playing.handle.isOpen) return
        val master = if (isMuted) 0f else masterGain
        val coexistenceTrim = if (type == AudioCueType.AMBIENT && active.keys.any { it.first == AudioCueType.MUSIC }) 0.50f else 1f
        val volume = (master * (userGains[type] ?: 1f) * (layerGains[type] ?: 1f) * playing.cueGain * playing.envelope * coexistenceTrim).coerceIn(0f, 1f)
        playing.handle.applyLinearVolume(volume)
    }

    fun isPlaying(type: AudioCueType, cueId: String): Boolean {
        val key = type to normalize(cueId)
        val single = active[key]
        if (single != null && single.handle.isOpen && single.handle.isRunning) return true
        return polyphonicSfx[key]?.any { it.handle.isOpen && it.handle.isRunning } == true
    }

    fun isStreamingActive(type: AudioCueType, cueId: String): Boolean {
        val key = type to normalize(cueId)
        val single = active[key] ?: return false
        return single.handle.isStreaming && single.handle.isOpen && single.handle.isRunning
    }

    fun activeVoiceCount(type: AudioCueType, cueId: String): Int {
        val key = type to normalize(cueId)
        val polyCount = polyphonicSfx[key]?.count { it.handle.isOpen && it.handle.isRunning } ?: 0
        val singleCount = if (active[key]?.let { it.handle.isOpen && it.handle.isRunning } == true) 1 else 0
        return polyCount + singleCount
    }

    fun activeCues(type: AudioCueType): List<String> =
        (active.keys.filter { it.first == type }.map { it.second } +
            polyphonicSfx.entries.filter { it.key.first == type && it.value.any { v -> v.handle.isOpen && v.handle.isRunning } }.map { it.key.second })
            .distinct()

    fun isPcmCached(cueId: String): Boolean =
        sfxPcmCache.containsKey(normalize(cueId))

    fun fadingOutCount(): Int = fadingOutHandles.size

    override fun release() {
        released = true
        musicWorker.shutdownNow()
        sfxWorker.shutdownNow()
        audioLoader.shutdownNow()
        active.values.forEach {
            try {
                it.handle.stop()
                it.handle.close()
            } catch (_: Exception) {}
        }
        active.clear()
        polyphonicSfx.values.forEach { voices ->
            voices.forEach {
                try {
                    it.handle.stop()
                    it.handle.close()
                } catch (_: Exception) {}
            }
        }
        polyphonicSfx.clear()
        fadingOutHandles.forEach { handle ->
            try {
                handle.stop()
                handle.close()
            } catch (_: Exception) {}
        }
        fadingOutHandles.clear()
        sfxPcmCache.clear()
    }

    private fun resamplePcm(cached: CachedPcm, pitch: Float): ByteArray {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        if (kotlin.math.abs(clampedPitch - 1.0f) < 0.01f) {
            return cached.bytes
        }
        val format = cached.format
        val channels = format.channels
        val bytesPerSample = 2
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
        const val STREAM_CHUNK_BYTES = 4_096
        private const val STREAM_LINE_BUFFER_BYTES = 16_384

        internal fun createStreamingForTest(
            assetProvider: AssetProvider,
            streamLineFactory: (AudioFormat) -> SourceDataLine,
            clipFactory: ((AudioFormat) -> Clip)? = null
        ): DesktopAudioDriver = DesktopAudioDriver(
            assetProvider = assetProvider,
            prewarmDefaults = false,
            customClipFactory = clipFactory,
            customStreamLineFactory = streamLineFactory
        )

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

internal fun AudioDriver.hasCue(cueId: String): Boolean =
    (this as? DesktopAudioDriver)?.hasCue(cueId) ?: false

internal fun AudioDriver.currentMusicPositionMs(expectedCueId: String? = null): Long? =
    (this as? DesktopAudioDriver)?.currentMusicPositionMs(expectedCueId)

internal fun AudioDriver.restartCurrentMusicFromStart(expectedCueId: String? = null) {
    (this as? DesktopAudioDriver)?.restartCurrentMusicFromStart(expectedCueId)
}

internal fun AudioDriver.setMusicPausedForOverlay(paused: Boolean, expectedCueId: String? = null) {
    (this as? DesktopAudioDriver)?.setMusicPausedForOverlay(paused, expectedCueId)
}
