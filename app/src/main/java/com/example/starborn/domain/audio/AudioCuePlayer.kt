package com.example.starborn.domain.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.content.getSystemService
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "AudioCuePlayer"
private const val MISSING_SOUND_ID = -1

@OptIn(UnstableApi::class)
class AudioCuePlayer(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(2)
        .build()

    private val musicPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val ambientPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val soundCache = ConcurrentHashMap<String, Int>()
    private val activeStreams = ConcurrentHashMap<Pair<AudioCueType, String>, Int>()
    private val fadeAnimators = EnumMap<AudioCueType, ValueAnimator>(AudioCueType::class.java)
    private var currentMusicCue: String? = null
    private var currentAmbientCue: String? = null
    private var musicGain: Float = 1f
    private var ambientGain: Float = 1f
    private var userMusicGain: Float = 1f
    private var userSfxGain: Float = 1f
    private var userVoiceGain: Float = 1f
    private var hapticsEnabled: Boolean = true
    private val vibrator: Vibrator? = context.getSystemService()

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
            AudioCueType.AMBIENT -> playStreaming(ambientPlayer, AudioCueType.AMBIENT, normalized, command)
            else -> playShort(command.type, normalized, command)
        }
    }

    private fun stopCommand(command: AudioCommand.Stop) {
        val normalized = command.cueId.trim().lowercase()
        if (normalized.isBlank()) return
        when (command.type) {
            AudioCueType.MUSIC -> stopStreaming(musicPlayer, AudioCueType.MUSIC, command.fadeMs)
            AudioCueType.AMBIENT -> stopStreaming(ambientPlayer, AudioCueType.AMBIENT, command.fadeMs)
            else -> {
                val key = command.type to normalized
                val stopAction: () -> Unit = {
                    activeStreams.remove(key)?.let { streamId ->
                        soundPool.stop(streamId)
                    }
                    Unit
                }
                if (command.fadeMs > 0) {
                    scheduleGain(command.type, 0f, command.fadeMs, stopAction)
                } else {
                    stopAction()
                }
            }
        }
    }

    private fun playShort(type: AudioCueType, cueId: String, command: AudioCommand.Play) {
        val soundId = ensureSound(cueId) ?: return
        val key = type to cueId
        activeStreams.remove(key)?.let { soundPool.stop(it) }
        val loopMode = if (command.loop) -1 else 0
        val baseGain = command.gain.coerceIn(0f, 1f)
        val userGain = when (type) {
            AudioCueType.VOICE -> userVoiceGain
            else -> userSfxGain
        }
        val scaledGain = (baseGain * userGain).coerceIn(0f, 1f)
        val streamId = soundPool.play(soundId, scaledGain, scaledGain, /*priority*/ 1, loopMode, /*rate*/ 1f)
        if (streamId == 0) {
            Log.d(TAG, "Unable to play audio cue '$cueId'")
            return
        }
        activeStreams[key] = streamId
        setLayerGainImmediate(type, baseGain)
        triggerHapticIfNeeded(command)
    }

    private fun playStreaming(player: ExoPlayer, type: AudioCueType, cueId: String, command: AudioCommand.Play) {
        val uri = resolveRawResourceUri(cueId) ?: return
        cancelFade(type)
        when (type) {
            AudioCueType.MUSIC -> currentMusicCue = cueId
            AudioCueType.AMBIENT -> currentAmbientCue = cueId
            else -> Unit
        }
        player.stop()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = if (command.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        setLayerGainImmediate(type, command.gain)
        player.prepare()
        player.playWhenReady = true
    }

    private fun stopStreaming(player: ExoPlayer, type: AudioCueType, fadeMs: Long) {
        cancelFade(type)
        if (fadeMs > 0) {
            scheduleGain(type, 0f, fadeMs) {
                player.stop()
                when (type) {
                    AudioCueType.MUSIC -> currentMusicCue = null
                    AudioCueType.AMBIENT -> currentAmbientCue = null
                    else -> Unit
                }
            }
        } else {
            player.stop()
            when (type) {
                AudioCueType.MUSIC -> currentMusicCue = null
                AudioCueType.AMBIENT -> currentAmbientCue = null
                else -> Unit
            }
            setLayerGainImmediate(type, 0f)
        }
    }

    fun release() {
        soundPool.release()
        musicPlayer.release()
        ambientPlayer.release()
        fadeAnimators.values.forEach(ValueAnimator::cancel)
        fadeAnimators.clear()
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
        val animator = ValueAnimator.ofFloat(start, clampedTarget).apply {
            duration = durationMs
            addUpdateListener { animation ->
                val gain = (animation.animatedValue as Float).coerceIn(0f, 1f)
                setLayerGainImmediate(type, gain)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeAnimators.remove(type)
                    setLayerGainImmediate(type, clampedTarget)
                    onComplete?.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    fadeAnimators.remove(type)
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
        else -> 1f
    }

    private fun setLayerGainImmediate(type: AudioCueType, gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        when (type) {
            AudioCueType.MUSIC -> {
                musicGain = clamped
                musicPlayer.volume = clamped * userMusicGain
            }
            AudioCueType.AMBIENT -> {
                ambientGain = clamped
                ambientPlayer.volume = clamped * userMusicGain
            }
            else -> activeStreams.forEach { (key, streamId) ->
                if (key.first == type) {
                    val scaled = when (type) {
                        AudioCueType.VOICE -> clamped * userVoiceGain
                        else -> clamped * userSfxGain
                    }
                    soundPool.setVolume(streamId, scaled, scaled)
                }
            }
        }
    }

    private fun ensureSound(name: String): Int? {
        val cached = soundCache[name]
        if (cached != null) {
            return cached.takeUnless { it == MISSING_SOUND_ID }
        }
        val resId = resolveRawResourceId(name)
        if (resId == 0) {
            soundCache[name] = MISSING_SOUND_ID
            return null
        }
        val loadedId = soundPool.load(context, resId, /*priority*/ 1)
        soundCache[name] = loadedId
        return loadedId
    }

    private fun resolveRawResourceId(name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0) {
            Log.d(TAG, "Missing audio cue resource for '$name'")
        }
        return resId
    }

    @UnstableApi
    private fun resolveRawResourceUri(name: String): android.net.Uri? {
        val resId = resolveRawResourceId(name)
        if (resId == 0) return null
        return RawResourceDataSource.buildRawResourceUri(resId)
    }

    fun setUserMusicGain(gain: Float) {
        userMusicGain = gain.coerceIn(0f, 1f)
        musicPlayer.volume = musicGain * userMusicGain
        ambientPlayer.volume = ambientGain * userMusicGain
    }

    fun setUserSfxGain(gain: Float) {
        userSfxGain = gain.coerceIn(0f, 1f)
        activeStreams.forEach { (key, streamId) ->
            if (key.first != AudioCueType.MUSIC && key.first != AudioCueType.AMBIENT) {
                val layerGain = if (key.first == AudioCueType.VOICE) userVoiceGain else userSfxGain
                soundPool.setVolume(streamId, layerGain, layerGain)
            }
        }
    }

    fun setUserVoiceGain(gain: Float) {
        userVoiceGain = gain.coerceIn(0f, 1f)
        activeStreams.forEach { (key, streamId) ->
            if (key.first == AudioCueType.VOICE) {
                soundPool.setVolume(streamId, userVoiceGain, userVoiceGain)
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

}
