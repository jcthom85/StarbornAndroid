package com.example.starborn.domain.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.example.starborn.core.platform.AudioDriver
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "AudioCuePlayer"
private const val MISSING_SOUND_ID = -1
private const val MICRO_FADE_MS = 20L
private const val SHORT_STOP_FADE_MS = 16L
private const val MAX_TRACKED_STREAMS_PER_CUE = 4
private const val AMBIENT_UNDER_MUSIC_SCALE = 0.50f
private const val AMBIENT_COEXISTENCE_FADE_MS = 450L

private data class PendingShortCue(
    val type: AudioCueType,
    val cueId: String,
    val command: AudioCommand.Play
)

private data class ActiveShortStream(
    val streamId: Int,
    val baseGain: Float,
    val loop: Boolean
)

@OptIn(UnstableApi::class)
class AudioCuePlayer(
    private val context: Context,
    preloadCueIds: Collection<String> = emptyList()
) : AudioDriver {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(16)
        .build()

    /**
     * Disable ExoPlayer's internal per-player audio focus handling so that dual-deck
     * music crossfades (musicPlayer + standbyMusicPlayer) never steal focus from each
     * other and hard-pause the outgoing deck mid-fade. AudioFocus is managed once at
     * the AudioCuePlayer level below.
     */
    private fun createMusicExoPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_GAME)
                .build(),
            /* handleAudioFocus = */ false
        )
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (mediaItemCount > 1 && currentMediaItemIndex == mediaItemCount - 1) {
                        repeatMode = Player.REPEAT_MODE_ONE
                    }
                }
            })
        }

    private fun createAmbientExoPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_GAME)
                .build(),
            /* handleAudioFocus = */ false
        )
        .build()

    private fun createStingerExoPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                .setUsage(C.USAGE_GAME)
                .build(),
            /* handleAudioFocus = */ false
        )
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var musicPlayer: ExoPlayer = createMusicExoPlayer()
    private var standbyMusicPlayer: ExoPlayer = createMusicExoPlayer()
    private var outgoingMusicAnimator: ValueAnimator? = null
    private var stingerPlayer: ExoPlayer? = null
    private var stingerCueId: String? = null
    private var stingerType: AudioCueType = AudioCueType.UI
    private var stingerBaseGain: Float = 1f
    private var stingerAnimator: ValueAnimator? = null
    private var resumeStingerAfterBackground: Boolean = false
    private val ambientPlayers = ConcurrentHashMap<String, ExoPlayer>()
    private val ambientBaseGains = ConcurrentHashMap<String, Float>()
    private val ambientAnimators = ConcurrentHashMap<String, ValueAnimator>()

    private val soundCache = ConcurrentHashMap<String, Int>()
    private val pendingShortCues = ConcurrentHashMap<Int, MutableList<PendingShortCue>>()
    private val activeShortStreams = ConcurrentHashMap<Pair<AudioCueType, String>, MutableList<ActiveShortStream>>()
    private val streamFadeAnimators = ConcurrentHashMap<Int, ValueAnimator>()
    private val fadeAnimators = EnumMap<AudioCueType, ValueAnimator>(AudioCueType::class.java)
    private val layerGains = EnumMap<AudioCueType, Float>(AudioCueType::class.java).apply {
        AudioCueType.values().forEach { put(it, 1f) }
    }

    private var currentMusicCue: String? = null
    private var currentMusicLoopStartMs: Long? = null
    private var currentMusicInLoopBodyOnly: Boolean = false
    private val musicTrackPositions = ConcurrentHashMap<String, Long>()
    private var musicGain: Float = 1f
    private var ambientGain: Float = 1f
    private var focusDuckMultiplier: Float = 1f
    private var hasAudioFocus: Boolean = false
    private var pausedByFocusLoss: Boolean = false
    private var userMusicGain: Float = 1f
    private var userSfxGain: Float = 1f
    private var userVoiceGain: Float = 1f
    private var hapticsEnabled: Boolean = true
    private var pausedForBackground: Boolean = false
    private var resumeMusicAfterBackground: Boolean = false
    private var resumeAmbientAfterBackground: Boolean = false
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausedByFocusLoss = true
                musicPlayer.pause()
                standbyMusicPlayer.pause()
                stingerPlayer?.pause()
                ambientPlayers.values.forEach { it.pause() }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                focusDuckMultiplier = 0.3f
                applyStreamingVolumes()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusDuckMultiplier = 1f
                applyStreamingVolumes()
                if (pausedByFocusLoss && !pausedForBackground) {
                    pausedByFocusLoss = false
                    if (currentMusicCue != null) {
                        musicPlayer.play()
                    }
                    ambientPlayers.values.forEach { it.play() }
                }
            }
        }
    }

    private val audioFocusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    } else {
        null
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val pending = pendingShortCues.remove(sampleId).orEmpty()
            if (status != 0) {
                pending.forEach { Log.d(TAG, "Unable to load audio cue '${it.cueId}'") }
                return@setOnLoadCompleteListener
            }
            if (pending.isNotEmpty()) {
                mainHandler.post {
                    pending.forEach { playLoadedShort(sampleId, it.type, it.cueId, it.command) }
                }
            }
        }
        preloadCueIds.forEach { cueId ->
            cueId.trim()
                .lowercase()
                .replace('-', '_')
                .takeIf { it.isNotBlank() && !shouldStreamShortCue(it) }
                ?.let(::ensureSound)
        }
    }

    private fun ensureAppAudioFocus() {
        if (hasAudioFocus) return
        val manager = audioManager ?: return
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            manager.requestAudioFocus(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun abandonAppAudioFocus() {
        if (!hasAudioFocus) return
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            manager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
        pausedByFocusLoss = false
        focusDuckMultiplier = 1f
    }

    private fun effectiveMusicVolume(gain: Float): Float =
        (gain * userMusicGain * focusDuckMultiplier).coerceIn(0f, 1f)

    private fun effectiveAmbientVolume(gain: Float): Float {
        val coexistenceTrim = if (currentMusicCue != null) AMBIENT_UNDER_MUSIC_SCALE else 1f
        return effectiveMusicVolume(gain * coexistenceTrim)
    }

    private fun syncAmbientCoexistenceVolume(fadeMs: Long = AMBIENT_COEXISTENCE_FADE_MS) {
        ambientPlayers.forEach { (cueId, player) ->
            val base = ambientBaseGains[cueId] ?: 1f
            val targetVol = effectiveAmbientVolume(base * ambientGain)
            scheduleAmbientCueGain(cueId, player, targetVol, fadeMs, equalPowerIn = false)
        }
    }

    private fun applyStreamingVolumes() {
        musicPlayer.volume = effectiveMusicVolume(musicGain)
        if (stingerAnimator == null) {
            stingerPlayer?.volume = effectiveStingerVolume()
        }
        ambientPlayers.forEach { (cueId, player) ->
            if (!ambientAnimators.containsKey(cueId)) {
                val base = ambientBaseGains[cueId] ?: ambientGain
                player.volume = effectiveAmbientVolume(base * ambientGain)
            }
        }
    }

    fun play(cueId: String) {
        val normalized = cueId.trim().lowercase().replace('-', '_')
        if (normalized.isBlank()) return
        execute(listOf(AudioCommand.Play(AudioCueType.UI, normalized, loop = false, fadeMs = 0L)))
    }

    fun execute(commands: List<AudioCommand>) {
        commands.forEach { command ->
            when (command) {
                is AudioCommand.Play -> playCommand(command)
                is AudioCommand.Stop -> stopCommand(command)
                is AudioCommand.Duck -> scheduleGain(command.type, command.gain, command.fadeMs)
                is AudioCommand.Restore -> scheduleGain(command.type, 1f, command.fadeMs)
            }
        }
    }

    private fun playCommand(command: AudioCommand.Play) {
        val normalized = command.cueId.trim().lowercase()
        if (normalized.isBlank()) return
        when (command.type) {
            AudioCueType.MUSIC -> playStreaming(musicPlayer, AudioCueType.MUSIC, normalized, command)
            AudioCueType.AMBIENT -> playAmbientStreaming(normalized, command)
            else -> playShort(command.type, normalized, command)
        }
    }

    private fun stopCommand(command: AudioCommand.Stop) {
        val normalized = command.cueId.trim().lowercase()
        if (normalized.isBlank()) return
        when (command.type) {
            AudioCueType.MUSIC -> stopStreaming(musicPlayer, AudioCueType.MUSIC, command.fadeMs)
            AudioCueType.AMBIENT -> stopAmbientStreaming(normalized, command.fadeMs)
            else -> {
                if (shouldStreamShortCue(normalized)) {
                    stopStingerStreaming(normalized, command.fadeMs)
                    return
                }
                val key = command.type to normalized
                val streams = activeShortStreams.remove(key).orEmpty()
                val fadeDuration = if (command.fadeMs > 0L) command.fadeMs else SHORT_STOP_FADE_MS
                streams.forEach { stream ->
                    fadeOutAndStopShortStream(command.type, stream, fadeDuration)
                }
            }
        }
    }

    private fun shouldStreamShortCue(cueId: String): Boolean =
        cueId.startsWith("stinger_")

    private fun effectiveStingerVolume(): Float {
        val layerGain = layerGains[stingerType] ?: 1f
        val userGain = userGainForShort(stingerType)
        return (stingerBaseGain * layerGain * userGain * focusDuckMultiplier).coerceIn(0f, 1f)
    }

    private fun playStingerStreaming(type: AudioCueType, cueId: String, command: AudioCommand.Play) {
        val uri = resolveStreamingUri(cueId) ?: return
        ensureAppAudioFocus()
        stingerAnimator?.cancel()
        stingerAnimator = null
        val player = stingerPlayer ?: createStingerExoPlayer().also { stingerPlayer = it }
        stingerCueId = cueId
        stingerType = type
        stingerBaseGain = command.gain.coerceIn(0f, 1f)
        player.stop()
        player.repeatMode = if (command.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        val targetVol = effectiveStingerVolume()
        if (command.fadeMs > 0L) {
            player.volume = 0f
            player.play()
            val anim = ValueAnimator.ofFloat(0f, targetVol).apply {
                duration = command.fadeMs
                addUpdateListener { a ->
                    stingerPlayer?.volume = (a.animatedValue as Float).coerceIn(0f, 1f)
                }
                addListener(object : AnimatorListenerAdapter() {
                    private var cancelled = false
                    override fun onAnimationCancel(animation: Animator) {
                        cancelled = true
                        if (stingerAnimator === animation) stingerAnimator = null
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        if (stingerAnimator === animation) stingerAnimator = null
                        if (!cancelled) stingerPlayer?.volume = effectiveStingerVolume()
                    }
                })
            }
            stingerAnimator = anim
            anim.start()
        } else {
            player.volume = targetVol
            player.play()
        }
        triggerHapticIfNeeded(command)
    }

    private fun stopStingerStreaming(cueId: String, fadeMs: Long) {
        val player = stingerPlayer ?: return
        if (stingerCueId != cueId) return
        stingerAnimator?.cancel()
        stingerAnimator = null
        val effectiveFadeMs = if (fadeMs > 0L) fadeMs else if (player.volume > 0.001f) SHORT_STOP_FADE_MS else 0L
        if (effectiveFadeMs > 0L) {
            val startVol = player.volume.coerceIn(0f, 1f)
            val anim = ValueAnimator.ofFloat(startVol, 0f).apply {
                duration = effectiveFadeMs
                addUpdateListener { a ->
                    stingerPlayer?.volume = (a.animatedValue as Float).coerceIn(0f, 1f)
                }
                addListener(object : AnimatorListenerAdapter() {
                    private var cancelled = false
                    override fun onAnimationCancel(animation: Animator) {
                        cancelled = true
                        if (stingerAnimator === animation) stingerAnimator = null
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        if (stingerAnimator === animation) stingerAnimator = null
                        if (!cancelled) {
                            player.volume = 0f
                            player.stop()
                            if (stingerCueId == cueId) stingerCueId = null
                        }
                    }
                })
            }
            stingerAnimator = anim
            anim.start()
        } else {
            player.volume = 0f
            player.stop()
            stingerCueId = null
        }
    }

    private fun playShort(type: AudioCueType, cueId: String, command: AudioCommand.Play) {
        if (shouldStreamShortCue(cueId)) {
            playStingerStreaming(type, cueId, command)
            return
        }
        val soundId = ensureSound(cueId) ?: return
        playLoadedShort(soundId, type, cueId, command)
    }

    private fun playLoadedShort(
        soundId: Int,
        type: AudioCueType,
        cueId: String,
        command: AudioCommand.Play
    ) {
        val key = type to cueId
        // Only choke/replace an existing stream of the same cue if it is looping.
        // One-shot SFX and rapid retriggers (UI clicks, voice murmurs, combat hits)
        // play polyphonically through SoundPool so active waveforms are never hard-cut mid-cycle.
        if (command.loop) {
            val previous = activeShortStreams.remove(key).orEmpty()
            previous.forEach { fadeOutAndStopShortStream(type, it, SHORT_STOP_FADE_MS) }
        }
        val loopMode = if (command.loop) -1 else 0
        val baseGain = command.gain.coerceIn(0f, 1f)
        val layerGain = layerGains[type] ?: 1f
        val userGain = userGainForShort(type)
        val scaledGain = (baseGain * layerGain * userGain).coerceIn(0f, 1f)
        val streamPriority = when (type) {
            AudioCueType.VOICE -> 3
            AudioCueType.BATTLE, AudioCueType.UI -> 2
            else -> 1
        }
        val pitchRate = command.pitch.coerceIn(0.5f, 2.0f)
        val streamId = soundPool.play(soundId, scaledGain, scaledGain, streamPriority, loopMode, pitchRate)
        if (streamId == 0) {
            queuePendingShort(soundId, type, cueId, command)
            return
        }
        activeShortStreams.compute(key) { _, existing ->
            val list = existing ?: mutableListOf()
            list += ActiveShortStream(streamId = streamId, baseGain = baseGain, loop = command.loop)
            while (list.size > MAX_TRACKED_STREAMS_PER_CUE) {
                list.removeAt(0)
            }
            list
        }
        triggerHapticIfNeeded(command)
    }

    private fun fadeOutAndStopShortStream(
        type: AudioCueType,
        stream: ActiveShortStream,
        fadeMs: Long
    ) {
        streamFadeAnimators.remove(stream.streamId)?.cancel()
        val startVol = (stream.baseGain * (layerGains[type] ?: 1f) * userGainForShort(type)).coerceIn(0f, 1f)
        if (fadeMs <= 0L || startVol <= 0.001f) {
            soundPool.stop(stream.streamId)
            return
        }
        val animator = ValueAnimator.ofFloat(startVol, 0f).apply {
            duration = fadeMs
            addUpdateListener { anim ->
                val v = (anim.animatedValue as Float).coerceIn(0f, 1f)
                soundPool.setVolume(stream.streamId, v, v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    streamFadeAnimators.remove(stream.streamId)
                    soundPool.stop(stream.streamId)
                }

                override fun onAnimationCancel(animation: Animator) {
                    streamFadeAnimators.remove(stream.streamId)
                    soundPool.stop(stream.streamId)
                }
            })
        }
        streamFadeAnimators[stream.streamId] = animator
        animator.start()
    }

    private fun userGainForShort(type: AudioCueType): Float = when (type) {
        AudioCueType.VOICE -> userVoiceGain
        else -> userSfxGain
    }

    private fun queuePendingShort(
        soundId: Int,
        type: AudioCueType,
        cueId: String,
        command: AudioCommand.Play
    ) {
        pendingShortCues.compute(soundId) { _, current ->
            val list = current ?: mutableListOf()
            if (list.none { it.type == type && it.cueId == cueId }) {
                list += PendingShortCue(type, cueId, command)
            }
            list
        }
    }

    private fun absoluteMusicPosition(player: ExoPlayer): Long {
        val ls = currentMusicLoopStartMs ?: 0L
        if (ls <= 0L) return player.currentPosition
        val inLoopBody = currentMusicInLoopBodyOnly || (player.mediaItemCount > 1 && player.currentMediaItemIndex == 1)
        return if (inLoopBody) ls + player.currentPosition else player.currentPosition
    }

    private fun playStreaming(player: ExoPlayer, type: AudioCueType, cueId: String, command: AudioCommand.Play) {
        val uri = resolveStreamingUri(cueId) ?: return
        ensureAppAudioFocus()
        cancelFade(type)
        if (type == AudioCueType.MUSIC) {
            val previousCue = currentMusicCue
            val wasPlaying = musicPlayer.isPlaying || musicPlayer.playWhenReady
            if (previousCue != null && previousCue != cueId) {
                musicTrackPositions[previousCue] = absoluteMusicPosition(musicPlayer)
            }
            if (previousCue != null && wasPlaying && musicPlayer.volume > 0.001f) {
                val outFadeMs = if (command.fadeMs > 0L) command.fadeMs else MICRO_FADE_MS
                handOffMusicToStandbyFade(outFadeMs)
            } else {
                musicPlayer.volume = 0f
                musicPlayer.stop()
            }
            val musicWasInactive = previousCue == null
            currentMusicCue = cueId
            if (musicWasInactive) {
                syncAmbientCoexistenceVolume()
            }
        } else {
            player.volume = 0f
            player.stop()
        }

        val activePlayer = if (type == AudioCueType.MUSIC) musicPlayer else player
        val loopStartMs = command.loopStartMs?.coerceAtLeast(0L)
        val loopEndMs = command.loopEndMs?.takeIf { it > (loopStartMs ?: 0L) }
        val startPosition = if (type == AudioCueType.MUSIC && command.loop) {
            if (command.restartFromBeginning) musicTrackPositions.remove(cueId)
            if (command.restartFromBeginning) 0L else musicTrackPositions[cueId] ?: 0L
        } else 0L

        if (type == AudioCueType.MUSIC && command.loop && ((loopStartMs ?: 0L) > 0L || loopEndMs != null)) {
            val ls = loopStartMs ?: 0L
            val le = loopEndMs ?: C.TIME_END_OF_SOURCE
            currentMusicLoopStartMs = ls.takeIf { it > 0L }
            if (ls > 0L && startPosition < ls) {
                currentMusicInLoopBodyOnly = false
                val introItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(0L)
                            .setEndPositionMs(ls)
                            .build()
                    )
                    .build()
                val loopItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(ls)
                            .setEndPositionMs(le)
                            .build()
                    )
                    .build()
                activePlayer.setMediaItems(listOf(introItem, loopItem))
                activePlayer.repeatMode = Player.REPEAT_MODE_OFF
            } else {
                currentMusicInLoopBodyOnly = ls > 0L
                val loopItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(ls)
                            .setEndPositionMs(le)
                            .build()
                    )
                    .build()
                activePlayer.setMediaItem(loopItem)
                activePlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        } else {
            if (type == AudioCueType.MUSIC) {
                currentMusicLoopStartMs = null
                currentMusicInLoopBodyOnly = false
            }
            activePlayer.setMediaItem(MediaItem.fromUri(uri))
            activePlayer.repeatMode = if (command.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }

        val effectiveInFadeMs = if (command.fadeMs > 0L) command.fadeMs else 0L
        setLayerGainImmediate(type, if (effectiveInFadeMs > 0L) 0f else command.gain)
        activePlayer.prepare()
        if (type == AudioCueType.MUSIC && command.loop) {
            val ls = loopStartMs ?: 0L
            val seekTarget = if (ls > 0L && startPosition >= ls) (startPosition - ls).coerceAtLeast(0L) else startPosition
            activePlayer.seekTo(seekTarget)
        }
        activePlayer.playWhenReady = !pausedForBackground
        if (pausedForBackground) resumeMusicAfterBackground = true
        if (effectiveInFadeMs > 0L) scheduleGain(type, command.gain, effectiveInFadeMs)
    }

    private fun handOffMusicToStandbyFade(fadeMs: Long) {
        outgoingMusicAnimator?.cancel()
        outgoingMusicAnimator = null
        standbyMusicPlayer.volume = 0f
        standbyMusicPlayer.stop()
        standbyMusicPlayer.clearMediaItems()

        val outgoingDeck = musicPlayer
        musicPlayer = standbyMusicPlayer
        standbyMusicPlayer = outgoingDeck

        val startVol = outgoingDeck.volume
        if (fadeMs <= 0L || startVol <= 0.001f) {
            outgoingDeck.volume = 0f
            outgoingDeck.stop()
            outgoingDeck.clearMediaItems()
            return
        }
        outgoingMusicAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = fadeMs
            addUpdateListener { anim ->
                val frac = (anim.animatedValue as Float).coerceIn(0f, 1f)
                val curve = cos(frac.toDouble() * Math.PI / 2.0).toFloat()
                outgoingDeck.volume = (startVol * curve).coerceIn(0f, 1f)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (outgoingMusicAnimator === animation) {
                        outgoingMusicAnimator = null
                    }
                    if (!cancelled) {
                        outgoingDeck.volume = 0f
                        outgoingDeck.stop()
                        outgoingDeck.clearMediaItems()
                    }
                }
            })
            start()
        }
    }

    override fun resetMusicPlayback() {
        cancelFade(AudioCueType.MUSIC)
        outgoingMusicAnimator?.cancel()
        outgoingMusicAnimator = null
        musicPlayer.volume = 0f
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        standbyMusicPlayer.volume = 0f
        standbyMusicPlayer.stop()
        standbyMusicPlayer.clearMediaItems()
        val hadMusic = currentMusicCue != null
        currentMusicCue = null
        musicTrackPositions.clear()
        resumeMusicAfterBackground = false
        if (hadMusic) {
            syncAmbientCoexistenceVolume(0L)
        }
    }

    private fun playAmbientStreaming(cueId: String, command: AudioCommand.Play) {
        val uri = resolveStreamingUri(cueId) ?: return
        ensureAppAudioFocus()
        ambientAnimators.remove(cueId)?.cancel()
        val player = ambientPlayers.getOrPut(cueId) { createAmbientExoPlayer() }
        player.volume = 0f
        player.stop()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = if (command.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        val baseGain = command.gain.coerceIn(0f, 1f)
        ambientBaseGains[cueId] = baseGain
        val targetVol = effectiveAmbientVolume(baseGain * ambientGain)
        val fadeInMs = if (command.fadeMs > 0L) command.fadeMs else MICRO_FADE_MS
        player.volume = 0f
        player.prepare()
        player.playWhenReady = !pausedForBackground
        if (pausedForBackground) resumeAmbientAfterBackground = true
        scheduleAmbientCueGain(cueId, player, targetVol, fadeInMs, equalPowerIn = true)
    }

    private fun stopStreaming(player: ExoPlayer, type: AudioCueType, fadeMs: Long) {
        cancelFade(type)
        if (type == AudioCueType.MUSIC) {
            currentMusicCue?.let { cue ->
                musicTrackPositions[cue] = absoluteMusicPosition(musicPlayer)
            }
            val effectiveFadeMs = if (fadeMs > 0L) fadeMs else if (musicPlayer.volume > 0.001f) MICRO_FADE_MS else 0L
            if (effectiveFadeMs > 0L && (musicPlayer.isPlaying || musicPlayer.playWhenReady)) {
                handOffMusicToStandbyFade(effectiveFadeMs)
            } else {
                musicPlayer.volume = 0f
                musicPlayer.stop()
            }
            val hadMusic = currentMusicCue != null
            currentMusicCue = null
            currentMusicLoopStartMs = null
            currentMusicInLoopBodyOnly = false
            musicGain = 0f
            if (hadMusic) {
                syncAmbientCoexistenceVolume()
            }
            return
        }
        val effectiveFadeMs = if (fadeMs > 0L) fadeMs else if (player.volume > 0.001f) MICRO_FADE_MS else 0L
        if (effectiveFadeMs > 0L) {
            scheduleGain(type, 0f, effectiveFadeMs) {
                player.volume = 0f
                player.stop()
            }
        } else {
            player.volume = 0f
            player.stop()
            setLayerGainImmediate(type, 0f)
        }
    }

    private fun stopAmbientStreaming(cueId: String, fadeMs: Long) {
        val player = ambientPlayers[cueId] ?: return
        ambientAnimators.remove(cueId)?.cancel()
        val effectiveFadeMs = if (fadeMs > 0L) fadeMs else if (player.volume > 0.001f) MICRO_FADE_MS else 0L
        if (effectiveFadeMs > 0L) {
            scheduleAmbientCueGain(cueId, player, 0f, effectiveFadeMs, equalPowerIn = false) {
                player.volume = 0f
                player.stop()
                player.release()
                ambientPlayers.remove(cueId)
                ambientBaseGains.remove(cueId)
            }
        } else {
            player.volume = 0f
            player.stop()
            player.release()
            ambientPlayers.remove(cueId)
            ambientBaseGains.remove(cueId)
        }
    }

    override fun release() {
        abandonAppAudioFocus()
        streamFadeAnimators.values.forEach(ValueAnimator::cancel)
        streamFadeAnimators.clear()
        soundPool.release()
        outgoingMusicAnimator?.cancel()
        outgoingMusicAnimator = null
        musicPlayer.release()
        standbyMusicPlayer.release()
        stingerAnimator?.cancel()
        stingerAnimator = null
        stingerPlayer?.stop()
        stingerPlayer?.release()
        stingerPlayer = null
        stingerCueId = null
        ambientAnimators.values.forEach(ValueAnimator::cancel)
        ambientAnimators.clear()
        ambientPlayers.values.forEach { player ->
            player.stop()
            player.release()
        }
        ambientPlayers.clear()
        ambientBaseGains.clear()
        fadeAnimators.values.forEach(ValueAnimator::cancel)
        fadeAnimators.clear()
        soundCache.clear()
        pendingShortCues.clear()
        activeShortStreams.clear()
    }

    fun pauseForBackground() {
        if (pausedForBackground) return
        pausedForBackground = true
        resumeMusicAfterBackground = musicPlayer.playWhenReady && currentMusicCue != null
        resumeAmbientAfterBackground = ambientPlayers.values.any { it.playWhenReady }
        resumeStingerAfterBackground = stingerPlayer?.isPlaying == true
        musicPlayer.pause()
        standbyMusicPlayer.pause()
        stingerPlayer?.pause()
        ambientPlayers.values.forEach { it.pause() }
        soundPool.autoPause()
        abandonAppAudioFocus()
    }

    fun resumeFromBackground() {
        if (!pausedForBackground) return
        pausedForBackground = false
        if ((resumeMusicAfterBackground && currentMusicCue != null) || resumeAmbientAfterBackground || resumeStingerAfterBackground) {
            ensureAppAudioFocus()
        }
        if (resumeMusicAfterBackground && currentMusicCue != null) {
            musicPlayer.play()
        }
        if (resumeAmbientAfterBackground) {
            ambientPlayers.values.forEach { it.play() }
        }
        if (resumeStingerAfterBackground) {
            stingerPlayer?.play()
        }
        soundPool.autoResume()
        resumeMusicAfterBackground = false
        resumeAmbientAfterBackground = false
        resumeStingerAfterBackground = false
    }

    private fun scheduleGain(type: AudioCueType, targetGain: Float, durationMs: Long, onComplete: (() -> Unit)? = null) {
        val clampedTarget = targetGain.coerceIn(0f, 1f)
        val start = currentGain(type)
        if (durationMs <= 0L) {
            cancelFade(type)
            setLayerGainImmediate(type, clampedTarget)
            onComplete?.invoke()
            return
        }
        if (start == clampedTarget) {
            onComplete?.invoke()
            return
        }
        val equalPower = type == AudioCueType.MUSIC || type == AudioCueType.AMBIENT
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animation ->
                val frac = (animation.animatedValue as Float).coerceIn(0f, 1f)
                val curve = if (equalPower) {
                    if (clampedTarget >= start) sin(frac.toDouble() * Math.PI / 2.0).toFloat()
                    else (1.0 - cos(frac.toDouble() * Math.PI / 2.0)).toFloat()
                } else {
                    frac
                }
                val gain = (start + (clampedTarget - start) * curve).coerceIn(0f, 1f)
                setLayerGainImmediate(type, gain)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationEnd(animation: Animator) {
                    if (fadeAnimators[type] === animation) {
                        fadeAnimators.remove(type)
                    }
                    if (!cancelled) {
                        setLayerGainImmediate(type, clampedTarget)
                        onComplete?.invoke()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                    if (fadeAnimators[type] === animation) {
                        fadeAnimators.remove(type)
                    }
                }
            })
        }
        cancelFade(type)
        fadeAnimators[type] = animator
        animator.start()
    }

    private fun cancelFade(type: AudioCueType) {
        fadeAnimators.remove(type)?.cancel()
    }

    private fun currentGain(type: AudioCueType): Float = when (type) {
        AudioCueType.MUSIC -> musicGain
        AudioCueType.AMBIENT -> ambientGain
        else -> layerGains[type] ?: 1f
    }

    private fun setLayerGainImmediate(type: AudioCueType, gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        layerGains[type] = clamped
        when (type) {
            AudioCueType.MUSIC -> {
                musicGain = clamped
                musicPlayer.volume = effectiveMusicVolume(clamped)
            }
            AudioCueType.AMBIENT -> {
                ambientGain = clamped
                ambientPlayers.forEach { (cueId, player) ->
                    if (!ambientAnimators.containsKey(cueId)) {
                        val base = ambientBaseGains[cueId] ?: 1f
                        player.volume = effectiveAmbientVolume(base * clamped)
                    }
                }
            }
            else -> activeShortStreams.forEach { (key, streams) ->
                if (key.first == type) {
                    val userGain = userGainForShort(type)
                    streams.forEach { stream ->
                        if (!streamFadeAnimators.containsKey(stream.streamId)) {
                            val scaled = (stream.baseGain * clamped * userGain).coerceIn(0f, 1f)
                            soundPool.setVolume(stream.streamId, scaled, scaled)
                        }
                    }
                }
            }
        }
    }

    private val assetAudioFiles: Map<String, String> by lazy {
        runCatching {
            context.assets.list("audio").orEmpty().associateBy { fileName ->
                fileName.substringBeforeLast('.').lowercase()
            }
        }.getOrDefault(emptyMap())
    }

    private fun resolveAssetAudioPath(name: String): String? {
        val fileName = assetAudioFiles[name.lowercase()] ?: return null
        return "audio/$fileName"
    }

    fun hasCue(cueId: String): Boolean {
        val normalized = cueId.trim().lowercase().replace('-', '_')
        if (normalized.isBlank()) return false
        return resolveAssetAudioPath(normalized) != null ||
            resolveRawResourceId(normalized, logMissing = false) != 0
    }

    fun currentMusicPositionMs(expectedCueId: String? = null): Long? {
        val activeCue = currentMusicCue ?: return null
        if (expectedCueId != null) {
            val expected = expectedCueId.trim().lowercase().replace('-', '_')
            if (activeCue != expected) return null
        }
        if (!musicPlayer.isPlaying && !musicPlayer.playWhenReady) return null
        return absoluteMusicPosition(musicPlayer).coerceAtLeast(0L)
    }

    fun restartCurrentMusicFromStart(expectedCueId: String? = null) {
        val activeCue = currentMusicCue ?: return
        if (expectedCueId != null) {
            val expected = expectedCueId.trim().lowercase().replace('-', '_')
            if (activeCue != expected) return
        }
        musicTrackPositions[activeCue] = 0L
        if (musicPlayer.mediaItemCount > 1) {
            musicPlayer.seekTo(0, 0L)
            musicPlayer.repeatMode = Player.REPEAT_MODE_OFF
            currentMusicInLoopBodyOnly = false
        } else {
            musicPlayer.seekTo(0L)
        }
        if (!pausedForBackground && !pausedByFocusLoss) {
            musicPlayer.play()
        }
    }

    fun setMusicPausedForOverlay(paused: Boolean, expectedCueId: String? = null) {
        val activeCue = currentMusicCue ?: return
        if (expectedCueId != null) {
            val expected = expectedCueId.trim().lowercase().replace('-', '_')
            if (activeCue != expected) return
        }
        if (paused) {
            musicPlayer.pause()
        } else if (!pausedForBackground && !pausedByFocusLoss) {
            musicPlayer.play()
        }
    }

    private fun ensureSound(name: String): Int? {
        val cached = soundCache[name]
        if (cached != null) {
            return cached.takeUnless { it == MISSING_SOUND_ID }
        }
        val resId = resolveRawResourceId(name, logMissing = false)
        if (resId != 0) {
            val loadedId = soundPool.load(context, resId, /*priority*/ 1)
            soundCache[name] = loadedId
            return loadedId
        }
        val assetPath = resolveAssetAudioPath(name)
        if (assetPath != null) {
            val loadedId = runCatching {
                context.assets.openFd(assetPath).use { afd ->
                    soundPool.load(afd, /*priority*/ 1)
                }
            }.getOrNull()
            if (loadedId != null && loadedId != 0) {
                soundCache[name] = loadedId
                return loadedId
            }
        }
        Log.d(TAG, "Missing audio cue resource for '$name'")
        soundCache[name] = MISSING_SOUND_ID
        return null
    }

    private fun resolveRawResourceId(name: String, logMissing: Boolean = true): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0 && logMissing) {
            Log.d(TAG, "Missing audio cue resource for '$name'")
        }
        return resId
    }

    @UnstableApi
    private fun resolveStreamingUri(name: String): android.net.Uri? {
        val assetPath = resolveAssetAudioPath(name)
        if (assetPath != null) {
            return android.net.Uri.parse("asset:///$assetPath")
        }
        val resId = resolveRawResourceId(name)
        if (resId == 0) return null
        return RawResourceDataSource.buildRawResourceUri(resId)
    }

    private fun perceptualGain(sliderValue: Float): Float {
        val clamped = sliderValue.coerceIn(0f, 1f)
        return clamped * clamped
    }

    fun setUserMusicGain(gain: Float) {
        userMusicGain = perceptualGain(gain)
        applyStreamingVolumes()
    }

    private fun scheduleAmbientCueGain(
        cueId: String,
        player: ExoPlayer,
        targetVolume: Float,
        durationMs: Long,
        equalPowerIn: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        val clampedTarget = targetVolume.coerceIn(0f, 1f)
        val start = player.volume.coerceIn(0f, 1f)
        if (durationMs <= 0L || start == clampedTarget) {
            player.volume = clampedTarget
            onComplete?.invoke()
            return
        }
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animation ->
                val frac = (animation.animatedValue as Float).coerceIn(0f, 1f)
                val curve = if (equalPowerIn) {
                    sin(frac.toDouble() * Math.PI / 2.0).toFloat()
                } else {
                    (1.0 - cos(frac.toDouble() * Math.PI / 2.0)).toFloat()
                }
                val vol = (start + (clampedTarget - start) * curve).coerceIn(0f, 1f)
                ambientPlayers[cueId]?.volume = vol
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                    ambientAnimators.remove(cueId, animation)
                }
                override fun onAnimationEnd(animation: Animator) {
                    ambientAnimators.remove(cueId, animation)
                    if (!cancelled) {
                        ambientPlayers[cueId]?.volume = clampedTarget
                        onComplete?.invoke()
                    }
                }
            })
        }
        ambientAnimators[cueId] = animator
        animator.start()
    }

    fun setUserSfxGain(gain: Float) {
        userSfxGain = perceptualGain(gain)
        if (stingerType != AudioCueType.VOICE && stingerAnimator == null) {
            stingerPlayer?.volume = effectiveStingerVolume()
        }
        activeShortStreams.forEach { (key, streams) ->
            if (key.first != AudioCueType.MUSIC && key.first != AudioCueType.AMBIENT && key.first != AudioCueType.VOICE) {
                val layerGain = layerGains[key.first] ?: 1f
                streams.forEach { stream ->
                    if (!streamFadeAnimators.containsKey(stream.streamId)) {
                        val scaled = (stream.baseGain * layerGain * userSfxGain).coerceIn(0f, 1f)
                        soundPool.setVolume(stream.streamId, scaled, scaled)
                    }
                }
            }
        }
    }

    fun setUserVoiceGain(gain: Float) {
        userVoiceGain = perceptualGain(gain)
        if (stingerType == AudioCueType.VOICE && stingerAnimator == null) {
            stingerPlayer?.volume = effectiveStingerVolume()
        }
        activeShortStreams.forEach { (key, streams) ->
            if (key.first == AudioCueType.VOICE) {
                val layerGain = layerGains[AudioCueType.VOICE] ?: 1f
                streams.forEach { stream ->
                    if (!streamFadeAnimators.containsKey(stream.streamId)) {
                        val scaled = (stream.baseGain * layerGain * userVoiceGain).coerceIn(0f, 1f)
                        soundPool.setVolume(stream.streamId, scaled, scaled)
                    }
                }
            }
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    fun triggerHapticSample() {
        performHapticPulse()
    }

    private fun triggerHapticIfNeeded(command: AudioCommand.Play) {
        if (!command.triggerHaptic || !hapticsEnabled) return
        performHapticPulse()
    }

    private fun performHapticPulse() {
        if (!hapticsEnabled) return
        val vibrator = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(32L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(32L)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to trigger haptic feedback", t)
        }
    }

    override fun execute(command: AudioCommand) {
        execute(listOf(command))
    }

    override fun setUserGain(type: AudioCueType, gain: Float) {
        when (type) {
            AudioCueType.MUSIC, AudioCueType.AMBIENT -> setUserMusicGain(gain)
            AudioCueType.VOICE -> setUserVoiceGain(gain)
            AudioCueType.UI, AudioCueType.BATTLE -> setUserSfxGain(gain)
        }
    }
}
