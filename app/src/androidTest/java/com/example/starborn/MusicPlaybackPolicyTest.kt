package com.example.starborn

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.domain.audio.*
import org.junit.Assert.*
import org.junit.Test

/** Uses real bundled tracks and ExoPlayer to check positions, not only command flags. */
class MusicPlaybackPolicyTest {
    @Test fun combatStartsFreshWhileExplorationResumesAndNewSessionsClearHistory() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var driver: AudioCuePlayer
        lateinit var player: ExoPlayer
        fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 10000
            while (SystemClock.uptimeMillis() < deadline) {
                var success = false
                main { success = condition() }
                if (success) return
                SystemClock.sleep(25)
            }
            fail(message)
        }
        main {
            driver = AudioCuePlayer(context)
            player = AudioCuePlayer::class.java.getDeclaredField("musicPlayer").apply { isAccessible = true }.get(driver) as ExoPlayer
        }
        fun play(cue: String, restart: Boolean = false) {
            main { driver.execute(AudioCommand.Play(AudioCueType.MUSIC, cue, loop = true, fadeMs = 0,
                restartFromBeginning = restart)) }
            await("Track ready: $cue") { player.playbackState == Player.STATE_READY && player.playWhenReady }
        }
        fun seek(position: Long) {
            main { player.seekTo(position) }
            await("Seek completed") { player.isPlaying && player.currentPosition >= position }
        }
        fun stop(cue: String) { main { driver.execute(AudioCommand.Stop(AudioCueType.MUSIC, cue, fadeMs = 0)) } }
        try {
            play("music_w1_homestead_explore")
            seek(10000)
            stop("music_w1_homestead_explore")
            play("music_w1_combat", restart = true)
            main { assertTrue("First fight starts at beginning", player.currentPosition < 2000) }
            seek(20000)
            stop("music_w1_combat")
            play("music_w1_homestead_explore")
            main { assertTrue("Exploration resumes after combat", player.currentPosition in 10000..18000) }
            main { driver.pauseForBackground() }
            var pausedAt = 0L
            main { pausedAt = player.currentPosition; assertFalse(player.playWhenReady) }
            SystemClock.sleep(150)
            main { assertTrue(kotlin.math.abs(player.currentPosition - pausedAt) < 100); driver.resumeFromBackground() }
            await("Foreground resumes playback") { player.isPlaying }
            stop("music_w1_homestead_explore")
            play("music_w1_combat", restart = true)
            main { assertTrue("Next fight ignores previous combat position", player.currentPosition < 2000) }
            seek(15000)
            play("music_w1_combat", restart = true)
            main { assertTrue("Back-to-back fights restart too", player.currentPosition < 2000) }
            main { driver.resetMusicPlayback() }
            play("music_w1_homestead_explore")
            main { assertTrue("Fresh session clears exploration position", player.currentPosition < 2000) }
        } finally { main { driver.release() } }
    }
}
