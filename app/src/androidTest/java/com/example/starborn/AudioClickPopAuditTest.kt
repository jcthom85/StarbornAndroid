package com.example.starborn

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.audio.AudioCueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

private const val AUDIT_TAG = "AudioClickPopAudit"

@OptIn(UnstableApi::class)
class AudioClickPopAuditTest {

    private fun main(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    private fun await(timeoutMs: Long = 10_000L, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            var done = false
            main { done = condition() }
            if (done) return true
            SystemClock.sleep(20)
        }
        return false
    }

    @Test
    fun verifyMusicCrossfadeStaysPlayingAndAmbientFadesIn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var driver: AudioCuePlayer
        val musicField = AudioCuePlayer::class.java.getDeclaredField("musicPlayer").apply { isAccessible = true }
        val standbyField = AudioCuePlayer::class.java.getDeclaredField("standbyMusicPlayer").apply { isAccessible = true }
        val ambientField = AudioCuePlayer::class.java.getDeclaredField("ambientPlayers").apply { isAccessible = true }
        fun musicPlayer(): ExoPlayer = musicField.get(driver) as ExoPlayer
        fun standbyPlayer(): ExoPlayer = standbyField.get(driver) as ExoPlayer
        @Suppress("UNCHECKED_CAST")
        fun ambientPlayers(): Map<String, ExoPlayer> = ambientField.get(driver) as Map<String, ExoPlayer>

        main { driver = AudioCuePlayer(context) }
        try {
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.MUSIC,
                        cueId = "music_w1_homestead_explore",
                        loop = true,
                        fadeMs = 0L
                    )
                )
            }
            assertTrue("Initial music did not start", await { musicPlayer().isPlaying && musicPlayer().volume > 0.9f })
            SystemClock.sleep(150)

            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.MUSIC,
                        cueId = "music_w1_combat",
                        loop = true,
                        fadeMs = 800L,
                        restartFromBeginning = true
                    )
                )
            }

            var outgoingPlayingCount = 0
            var outgoingVolDropped = false
            for (step in 0..16) {
                var logLine = ""
                main {
                    val out = standbyPlayer()
                    val inc = musicPlayer()
                    if (out.isPlaying && out.playWhenReady) outgoingPlayingCount++
                    if (out.volume < 0.85f) outgoingVolDropped = true
                    logLine = "CROSSFADE t=${step * 25}ms | outgoing(playing=${out.isPlaying}, pwr=${out.playWhenReady}, vol=${"%.3f".format(out.volume)}) | incoming(playing=${inc.isPlaying}, pwr=${inc.playWhenReady}, vol=${"%.3f".format(inc.volume)})"
                }
                Log.i(AUDIT_TAG, logLine)
                SystemClock.sleep(25)
            }
            assertTrue(
                "Outgoing music deck must stay playing during crossfade (was playing in $outgoingPlayingCount/17 samples)",
                outgoingPlayingCount >= 12
            )
            assertTrue("Outgoing music deck volume should fade down smoothly", outgoingVolDropped)

            var ambientVolImmediate = -1f
            var ambientVolLater = -1f
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.AMBIENT,
                        cueId = "amb_w2_stasis_chamber",
                        loop = true,
                        fadeMs = 350L,
                        gain = 0.8f
                    )
                )
                ambientVolImmediate = ambientPlayers()["amb_w2_stasis_chamber"]?.volume ?: -1f
            }
            SystemClock.sleep(420)
            main {
                ambientVolLater = ambientPlayers()["amb_w2_stasis_chamber"]?.volume ?: -1f
            }
            Log.i(AUDIT_TAG, "AMBIENT_START fadeMs=350L -> immediateVol=${"%.3f".format(ambientVolImmediate)}, after420ms=${"%.3f".format(ambientVolLater)}")
            assertTrue("Ambient must start faded down (was $ambientVolImmediate)", ambientVolImmediate < 0.15f)
            assertTrue("Ambient must fade up to target (was $ambientVolLater)", ambientVolLater > 0.7f)
        } finally {
            main { driver.release() }
        }
    }

    @Test
    fun verifySfxRapidRetriggerOverlapAndMicroFadeStop() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preload = listOf(
            "voice_murmur_nova_01",
            "sfx_ui_button_click",
            "wpn_nova_laser",
            "sfx_combat_shield_deflect"
        )
        lateinit var driver: AudioCuePlayer
        val streamsField = AudioCuePlayer::class.java.getDeclaredField("activeShortStreams").apply { isAccessible = true }
        val layerGainsField = AudioCuePlayer::class.java.getDeclaredField("layerGains").apply { isAccessible = true }
        val fadeAnimsField = AudioCuePlayer::class.java.getDeclaredField("streamFadeAnimators").apply { isAccessible = true }

        main { driver = AudioCuePlayer(context, preloadCueIds = preload) }
        try {
            // Allow SoundPool preload to complete on Android
            SystemClock.sleep(400)

            // 1. Rapid retrigger of one-shot voice murmur (every 78ms like DialogueOverlay)
            repeat(4) {
                main {
                    driver.execute(
                        AudioCommand.Play(
                            type = AudioCueType.VOICE,
                            cueId = "voice_murmur_nova_01",
                            loop = false,
                            fadeMs = 0L,
                            gain = 0.9f
                        )
                    )
                }
                SystemClock.sleep(78)
            }
            var voiceStreamCount = 0
            main {
                @Suppress("UNCHECKED_CAST")
                val map = streamsField.get(driver) as Map<Pair<AudioCueType, String>, List<*>>
                voiceStreamCount = map[AudioCueType.VOICE to "voice_murmur_nova_01"]?.size ?: 0
            }
            Log.i(AUDIT_TAG, "SFX_RETRIGGER voice_murmur_nova_01 trackedStreams=$voiceStreamCount")
            assertTrue("Rapid one-shot retriggers should play polyphonically without choking (got $voiceStreamCount)", voiceStreamCount >= 2)

            // 2. Overlapping BATTLE SFX with different gains must NOT clobber layer gain
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.BATTLE,
                        cueId = "wpn_nova_laser",
                        loop = false,
                        fadeMs = 0L,
                        gain = 1.0f
                    )
                )
            }
            SystemClock.sleep(40)
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.BATTLE,
                        cueId = "sfx_combat_shield_deflect",
                        loop = false,
                        fadeMs = 0L,
                        gain = 0.25f
                    )
                )
            }
            var battleLayerGain = 0f
            main {
                @Suppress("UNCHECKED_CAST")
                val gains = layerGainsField.get(driver) as Map<AudioCueType, Float>
                battleLayerGain = gains[AudioCueType.BATTLE] ?: 0f
            }
            Log.i(AUDIT_TAG, "SFX_OVERLAP battleLayerGain=$battleLayerGain")
            assertEquals("Overlapping cue with lower gain must not clobber BATTLE layer gain", 1.0f, battleLayerGain, 0.001f)

            // 3. Immediate Stop (fadeMs = 0) on a looping SFX should trigger a short anti-click micro-fade
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.UI,
                        cueId = "sfx_ui_button_click",
                        loop = true,
                        fadeMs = 0L,
                        gain = 0.8f
                    )
                )
            }
            SystemClock.sleep(30)
            var activeFadeCount = 0
            main {
                driver.execute(AudioCommand.Stop(AudioCueType.UI, "sfx_ui_button_click", fadeMs = 0L))
                @Suppress("UNCHECKED_CAST")
                val anims = fadeAnimsField.get(driver) as Map<Int, *>
                activeFadeCount = anims.size
            }
            Log.i(AUDIT_TAG, "SFX_STOP_MICROFADE activeFadeAnimators=$activeFadeCount")
            assertTrue("Immediate SFX stop should use a short micro-fade to avoid pop", activeFadeCount >= 1)
            SystemClock.sleep(40)
        } finally {
            main { driver.release() }
        }
    }

    @Test
    fun verifyLoopWraparoundPcmOnAndroid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cues = listOf(
            "music_w3_boss_phantom",
            "music_w1_combat",
            "music_w1_homestead_explore",
            "amb_w2_stasis_chamber"
        )
        for (cue in cues) {
            val resId = context.resources.getIdentifier(cue, "raw", context.packageName)
            assertTrue("Missing raw resource $cue", resId != 0)
            val captured = ArrayList<Short>(44100 * 4)
            val lock = Any()
            var sampleRate = 44100
            var channelCount = 2
            val tee = TeeAudioProcessor(object : TeeAudioProcessor.AudioBufferSink {
                override fun flush(sampleRateHz: Int, channelCountArg: Int, encoding: Int) {
                    synchronized(lock) {
                        sampleRate = sampleRateHz
                        channelCount = channelCountArg
                    }
                }
                override fun handleBuffer(buffer: ByteBuffer) {
                    val dup = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
                    val shortBuf = dup.asShortBuffer()
                    val arr = ShortArray(shortBuf.remaining())
                    shortBuf.get(arr)
                    synchronized(lock) {
                        if (captured.size < 44100 * 8) {
                            for (s in arr) captured.add(s)
                        }
                    }
                }
            })

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink {
                    return DefaultAudioSink.Builder(context)
                        .setAudioProcessors(arrayOf(tee))
                        .build()
                }
            }

            lateinit var player: ExoPlayer
            main {
                player = ExoPlayer.Builder(context, renderersFactory)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_GAME)
                            .build(),
                        false
                    )
                    .build()
                player.setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(resId)))
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.prepare()
                player.playWhenReady = true
            }
            try {
                assertTrue("Player ready for $cue", await { player.playbackState == Player.STATE_READY && player.duration > 2000L })
                var dur = 0L
                main {
                    dur = player.duration
                    player.seekTo((dur - 700L).coerceAtLeast(0L))
                }
                SystemClock.sleep(150)
                synchronized(lock) { captured.clear() }
                val wrapped = await(5000L) { player.currentPosition in 200L..2000L }
                SystemClock.sleep(150)

                val pcm: ShortArray
                val ch: Int
                val sr: Int
                synchronized(lock) {
                    pcm = ShortArray(captured.size) { captured[it] }
                    ch = channelCount
                    sr = sampleRate
                }
                var maxDelta = 0
                var maxDeltaIdx = 0
                var maxZeroRun = 0
                var curZeroRun = 0
                val frames = pcm.size / ch
                for (i in 1 until frames) {
                    var frameSilent = true
                    for (c in 0 until ch) {
                        val cur = pcm[i * ch + c].toInt()
                        val prev = pcm[(i - 1) * ch + c].toInt()
                        if (abs(cur) > 16) frameSilent = false
                        val d = abs(cur - prev)
                        if (d > maxDelta) {
                            maxDelta = d
                            maxDeltaIdx = i
                        }
                    }
                    if (frameSilent) {
                        curZeroRun++
                        if (curZeroRun > maxZeroRun) maxZeroRun = curZeroRun
                    } else {
                        curZeroRun = 0
                    }
                }
                val maxDeltaNorm = maxDelta / 32768.0
                val zeroRunMs = (maxZeroRun * 1000.0) / sr
                Log.i(
                    AUDIT_TAG,
                    "LOOP_PCM_AFTER $cue | dur=${dur}ms | wrapped=$wrapped | frames=$frames | sr=$sr | maxStep=${"%.4f".format(maxDeltaNorm)} at frame=$maxDeltaIdx | maxSilentRun=${"%.1f".format(zeroRunMs)}ms"
                )
                assertTrue("$cue failed to loop wrap", wrapped)
                assertTrue("$cue has silent gap (${zeroRunMs}ms)", zeroRunMs < 15.0)
            } finally {
                main { player.release() }
            }
        }
    }

    @Test
    fun verifyStingerStreamingCancelledFadeAndPerceptualVolume() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var driver: AudioCuePlayer
        main { driver = AudioCuePlayer(context) }

        val stingerPlayerField = AudioCuePlayer::class.java.getDeclaredField("stingerPlayer").apply { isAccessible = true }
        val musicPlayerField = AudioCuePlayer::class.java.getDeclaredField("musicPlayer").apply { isAccessible = true }
        val soundCacheField = AudioCuePlayer::class.java.getDeclaredField("soundCache").apply { isAccessible = true }

        try {
            // 1. Long motif stinger (> 5.9s / > 1MB PCM) must stream via stingerPlayer, never SoundPool
            main {
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.UI,
                        cueId = "stinger_motif_jed",
                        loop = false,
                        fadeMs = 0L,
                        gain = 0.9f
                    )
                )
            }
            val stingerReady = await(3000L) {
                val sp = stingerPlayerField.get(driver) as? ExoPlayer
                sp != null && sp.playbackState == Player.STATE_READY && sp.duration > 8000L
            }
            assertTrue("stinger_motif_jed should stream via ExoPlayer with full >8s duration", stingerReady)
            var inSoundCache = true
            main {
                @Suppress("UNCHECKED_CAST")
                val cache = soundCacheField.get(driver) as Map<String, Int>
                inSoundCache = cache.containsKey("stinger_motif_jed")
            }
            assertTrue("stinger_motif_jed must not be loaded into SoundPool", !inSoundCache)

            // 2. Perceptual volume curve: 0.5 slider -> 0.25 effective gain (not 0.5 linear)
            main {
                driver.setUserMusicGain(0.5f)
                driver.execute(
                    AudioCommand.Play(
                        type = AudioCueType.MUSIC,
                        cueId = "music_w1_homestead_explore",
                        loop = true,
                        fadeMs = 0L,
                        gain = 1.0f
                    )
                )
            }
            SystemClock.sleep(80)
            var musicVolAtHalf = -1f
            main {
                val mp = musicPlayerField.get(driver) as ExoPlayer
                musicVolAtHalf = mp.volume
            }
            assertEquals("Perceptual 0.5 slider should map to 0.25 gain", 0.25f, musicVolAtHalf, 0.02f)

            // 3. Cancelled fade-out must NOT snap volume to 0 when interrupted by Restore
            main {
                driver.setUserMusicGain(1.0f)
                driver.execute(AudioCommand.Duck(AudioCueType.MUSIC, gain = 0.0f, fadeMs = 400L))
            }
            SystemClock.sleep(50)
            main {
                driver.execute(AudioCommand.Restore(AudioCueType.MUSIC, fadeMs = 0L))
            }
            SystemClock.sleep(50)
            var volAfterCancel = -1f
            main {
                val mp = musicPlayerField.get(driver) as ExoPlayer
                volAfterCancel = mp.volume
            }
            assertEquals("Cancelled fade must not snap volume back to 0", 1.0f, volAfterCancel, 0.02f)
        } finally {
            main { driver.release() }
        }
    }
}
