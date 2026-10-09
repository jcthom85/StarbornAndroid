package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.*
import com.example.starborn.domain.cinematic.*
import com.example.starborn.feature.exploration.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO

class DesktopCinematicSnapshotTest {
    @get:Rule val compose = createComposeRule()
    private class RecordingAudio : AudioDriver {
        val commands = CopyOnWriteArrayList<AudioCommand>()
        override fun execute(command: AudioCommand) { commands += command }
        override fun setUserGain(type: AudioCueType, gain: Float) {}
        override fun release() {}
    }

    @Test fun authoredIntroRunsAutomaticallyThroughTheLiveExplorationRoute() {
        val directory = Files.createTempDirectory("starborn-cinematic-").toFile()
        val audio = RecordingAudio()
        val services = DesktopAppServices(directory, audio)
        try {
            assertTrue(services.startNewGame())
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme { DesktopExplorationScreen(services, {}, {}, {}, {}, {}, {}) } }
            compose.waitUntil(15_000) { compose.mainClock.advanceTimeBy(16); services.exploration.uiState.value.cinematic?.sceneId == "intro_prologue" }
            compose.onNodeWithText("Menu [Esc]").performClick()
            assertFalse("Movie clicks must not open the underlying menu", services.exploration.uiState.value.isMenuOverlayVisible)
            compose.onAllNodesWithText("Continue", useUnmergedTree = true).assertCountEquals(0)
            // Image loading/click synchronization can outlast a frame. Verify forward playback,
            // without requiring the test to catch exactly the second authored frame.
            compose.waitUntil(8_000) { compose.mainClock.advanceTimeBy(250); (services.exploration.uiState.value.cinematic?.stepIndex ?: 0) > 0 }
            val screenshots = File("build/reports/desktop/screenshots").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(screenshots, "intro-breach-fullscreen.png"))
            compose.waitUntil(45_000) { compose.mainClock.advanceTimeBy(100); services.exploration.uiState.value.cinematic?.sceneId != "intro_prologue" }
            val plays = audio.commands.filterIsInstance<AudioCommand.Play>()
            assertTrue(plays.any { it.cueId == "amb_intro_containment_pressure" && it.type == AudioCueType.AMBIENT && it.loop })
            listOf("sfx_intro_door_buckle", "sfx_intro_door_collapse", "sfx_intro_chime_launch", "sfx_intro_stasis_seal", "sfx_intro_stasis_lock", "sfx_intro_beast_strike").forEach { cue ->
                assertTrue("Missing authored sound: $cue", plays.any { it.cueId == cue && it.type == AudioCueType.UI })
            }
            assertTrue(plays.any { it.cueId == "music_intro_breach" && it.type == AudioCueType.MUSIC })
            compose.waitForIdle()
            assertTrue(audio.commands.filterIsInstance<AudioCommand.Stop>().any { it.cueId == "amb_intro_containment_pressure" })
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun illustratedVoiceAndRoomRevealAreHandled() {
        val directory = Files.createTempDirectory("starborn-cinematic-default-").toFile()
        val audio = RecordingAudio()
        val services = DesktopAppServices(directory, audio)
        var advances = 0
        try {
            val state = CinematicUiState("fixture", null, CinematicBackdrop.ROOM,
                presentation = CinematicPresentation.ILLUSTRATED, stepIndex = 0, stepCount = 1,
                step = CinematicStepUi(CinematicStepType.NARRATION, null, "", durationSeconds = .5,
                    voiceCue = "fixture_voice", captionStyle = CinematicCaptionStyle.NONE))
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme { DesktopCinematicOverlay(state, services, { advances++ }, {}) } }
            compose.waitUntil(3_000) { compose.mainClock.advanceTimeBy(16); advances == 1 }
            compose.onNodeWithContentDescription("Room fading in").assertExists()
            assertTrue(audio.commands.filterIsInstance<AudioCommand.Play>().any { it.type == AudioCueType.VOICE && it.cueId == "fixture_voice" })
            assertEquals(1, advances)
        } finally { services.close(); directory.deleteRecursively() }
    }
}
