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
        driver = DesktopAudioDriver(assetProvider, prewarmDefaults = true)
    }

    @After
    fun tearDown() {
        driver.release()
    }

    @Test
    fun prewarmLoadsCommonSfxCuesIntoPcmCache() {
        // Allow background prewarm worker a brief moment to process the cues
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline && !driver.isPcmCached("ui_click")) {
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
        driver.execute(AudioCommand.Play(AudioCueType.UI, "ui_confirm", loop = false, fadeMs = 0))
        driver.setMuted(true)
        driver.setMuted(false)
        driver.setMasterGain(0.5f)
        driver.setUserGain(AudioCueType.UI, 0.8f)
    }
}
