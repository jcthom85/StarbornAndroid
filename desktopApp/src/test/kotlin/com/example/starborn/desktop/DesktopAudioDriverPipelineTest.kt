package com.example.starborn.desktop

import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DesktopAudioDriverPipelineTest {

    private lateinit var assetProvider: DesktopAssetProvider
    private lateinit var driver: DesktopAudioDriver

    @Before
    fun setUp() {
        assetProvider = DesktopAssetProvider()
        driver = DesktopAudioDriver(assetProvider, prewarmDefaults = true, clipFactory = { TestAudioClip().clip })
    }

    @After
    fun tearDown() {
        driver.release()
    }

    @Test
    fun prewarmLoadsCommonSfxCuesIntoPcmCache() {
        // Allow background prewarm worker a brief moment to process the cues
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline && !listOf("ui_click", "ui_room_move", "sfx_ui_button_click").all(driver::isPcmCached)) {
            Thread.sleep(50)
        }
        assertTrue("ui_click should be cached in PCM cache", driver.isPcmCached("ui_click"))
        assertTrue("ui_room_move should be cached in PCM cache", driver.isPcmCached("ui_room_move"))
        assertTrue("sfx_ui_button_click should be cached in PCM cache", driver.isPcmCached("sfx_ui_button_click"))
    }

    @Test
    fun musicPlaybackAndSmoothCrossfade() {
        // 1. Start initial music track
        driver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_title_theme", loop = true, fadeMs = 100))

        val deadline1 = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline1 && !driver.isPlaying(AudioCueType.MUSIC, "music_title_theme")) {
            Thread.sleep(50)
        }
        assertTrue("music_title_theme should be active and playing", driver.isPlaying(AudioCueType.MUSIC, "music_title_theme"))

        // 2. Play a second music track, initiating crossfade
        driver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_shop_cozy", loop = true, fadeMs = 200))

        // First track should immediately be removed from active cues
        val deadline2 = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline2 && !driver.isPlaying(AudioCueType.MUSIC, "music_shop_cozy")) {
            Thread.sleep(50)
        }
        assertTrue("music_shop_cozy should be active and playing", driver.isPlaying(AudioCueType.MUSIC, "music_shop_cozy"))
        assertFalse("music_title_theme should no longer be active", driver.activeCues(AudioCueType.MUSIC).contains("music_title_theme"))

        // 3. Wait for fade-out of previous track to conclude
        val deadline3 = System.currentTimeMillis() + 2000L
        while (System.currentTimeMillis() < deadline3 && driver.fadingOutCount() > 0) {
            Thread.sleep(50)
        }
        assertEquals("Outgoing tracks must be stopped and unlinked after fade completion", 0, driver.fadingOutCount())
    }

    @Test
    fun stopCueWithFadeGracefullyCompletes() {
        driver.execute(AudioCommand.Play(AudioCueType.AMBIENT, "amb_vent", loop = true, fadeMs = 50))
        val deadline1 = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline1 && !driver.isPlaying(AudioCueType.AMBIENT, "amb_vent")) {
            Thread.sleep(50)
        }
        assertTrue("amb_vent should be playing", driver.isPlaying(AudioCueType.AMBIENT, "amb_vent"))

        driver.execute(AudioCommand.Stop(AudioCueType.AMBIENT, "amb_vent", fadeMs = 150))
        val deadlineUnlist = System.currentTimeMillis() + 2000L
        while (System.currentTimeMillis() < deadlineUnlist && driver.activeCues(AudioCueType.AMBIENT).contains("amb_vent")) {
            Thread.sleep(25)
        }
        assertFalse("amb_vent should be unlisted from active cues", driver.activeCues(AudioCueType.AMBIENT).contains("amb_vent"))

        val deadline2 = System.currentTimeMillis() + 2000L
        while (System.currentTimeMillis() < deadline2 && driver.fadingOutCount() > 0) {
            Thread.sleep(50)
        }
        assertEquals("Fading out clips must be completely released", 0, driver.fadingOutCount())
    }

    @Test
    fun masterMuteTogglesInstantlyWithoutInterruptingActiveClips() {
        driver.release()
        val output = TestAudioClip()
        driver = DesktopAudioDriver(assetProvider, prewarmDefaults = false, clipFactory = { output.clip })
        driver.execute(AudioCommand.Play(AudioCueType.AMBIENT, "amb_vent", loop = true, fadeMs = 0))
        await { driver.isPlaying(AudioCueType.AMBIENT, "amb_vent") }
        driver.setMasterGain(0.5f)
        assertEquals(-6.02f, output.gain.value, .1f)
        driver.setMuted(true)
        assertEquals(output.gain.minimum, output.gain.value, .01f)
        assertTrue(output.running)
        driver.setMuted(false)
        assertEquals(-6.02f, output.gain.value, .1f)
        driver.setMasterGain(1f)
        assertEquals(0f, output.gain.value, .01f)
    }

    @Test
    fun stoppingMusicWhileItLoadsCannotStartItAfterwards() {
        driver.release()
        val loading = java.util.concurrent.CountDownLatch(1)
        val finishLoading = java.util.concurrent.CountDownLatch(1)
        val output = TestAudioClip()
        val requests = java.util.concurrent.atomic.AtomicInteger()
        driver = DesktopAudioDriver(assetProvider, prewarmDefaults = false, clipFactory = {
            if (requests.incrementAndGet() == 1) {
                loading.countDown()
                check(finishLoading.await(5, java.util.concurrent.TimeUnit.SECONDS))
                output.clip
            } else TestAudioClip().clip
        })
        try {
            driver.execute(AudioCommand.Play(AudioCueType.MUSIC, "music_title_theme", loop = true))
            assertTrue(loading.await(3, java.util.concurrent.TimeUnit.SECONDS))
            driver.execute(AudioCommand.Stop(AudioCueType.MUSIC, "music_title_theme"))
            // Ambience shares the command worker, so playback proves Stop has been processed.
            driver.execute(AudioCommand.Play(AudioCueType.AMBIENT, "amb_vent", loop = true))
            await { driver.isPlaying(AudioCueType.AMBIENT, "amb_vent") }
            finishLoading.countDown()
            await { output.closed }
            assertFalse(driver.isPlaying(AudioCueType.MUSIC, "music_title_theme"))
        } finally { finishLoading.countDown() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5000L
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue("Audio command did not complete", condition())
    }
}
