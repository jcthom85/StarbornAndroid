package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.platform.AudioDriver
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.audio.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopTitleLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun titleScreenRequestsAuthoredThemeAndRestoresItAfterReturningFromGameplay() {
        val directory = Files.createTempDirectory("starborn-title-audio-").toFile()
        val audio = RecordingAudioDriver()
        val services = DesktopAppServices(directory, audio)
        val titleVisible = mutableStateOf(true)
        try {
            compose.setContent { DesktopStarbornTheme(services) {
                if (titleVisible.value) DesktopMainMenuScreen(services, onStartGame = {}, onQuit = {})
            } }
            compose.waitForIdle()
            val first = audio.commands.filterIsInstance<AudioCommand.Play>().single { it.type == AudioCueType.MUSIC }
            assertTrue(first.cueId == "music_title_theme" && first.loop)
            compose.runOnIdle {
                titleVisible.value = false
                services.audioDriver.executeAll(services.audioRouter.commandsForRoom("hub_1_homestead", "pit_nova_bunk"))
            }
            compose.waitForIdle()
            audio.commands.clear()
            compose.runOnIdle { titleVisible.value = true }
            compose.waitForIdle()
            assertTrue(audio.commands.any { it is AudioCommand.Stop && it.type == AudioCueType.AMBIENT })
            assertTrue(audio.commands.any { it is AudioCommand.Play && it.cueId == "music_title_theme" && it.loop })
            assertTrue(audio.commands.any { it is AudioCommand.Restore && it.type == AudioCueType.MUSIC })
        } finally { services.close(); directory.deleteRecursively() }
    }
    @Test fun largerLogoAndCleanTitleRenderAtTwoWindowSizes() {
        val directory = Files.createTempDirectory("starborn-title-layout-").toFile()
        val services = DesktopAppServices(directory, object : AudioDriver {
            override fun execute(command: AudioCommand) {}
            override fun setUserGain(type: AudioCueType, gain: Float) {}
            override fun release() {}
        })
        val narrow = mutableStateOf(false)
        try {
            compose.setContent { DesktopStarbornTheme { Box(Modifier.size(if (narrow.value) 900.dp else 1280.dp, if (narrow.value) 600.dp else 720.dp)) {
                DesktopMainMenuScreen(services, onStartGame = {}, onQuit = {})
            } } }
            compose.waitForIdle()
            compose.onNodeWithText("DEEP-SPACE TACTICAL RPG").assertDoesNotExist()
            compose.onNodeWithText("New Game").assertIsDisplayed()
            assertTrue(compose.onNodeWithContentDescription("Starborn").fetchSemanticsNode().boundsInRoot.height > 250)
            capture("title-large-logo")
            compose.runOnIdle { narrow.value = true }
            compose.onNodeWithText("New Game").assertIsDisplayed()
            compose.onNodeWithContentDescription("Starborn").assertIsDisplayed()
            capture("title-large-logo-narrow")
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun existingSaveRendersContinuePrimaryWithSlotInfo() {
        val directory = Files.createTempDirectory("starborn-title-continue-").toFile()
        val services = DesktopAppServices(directory, object : AudioDriver {
            override fun execute(command: AudioCommand) {}
            override fun setUserGain(type: AudioCueType, gain: Float) {}
            override fun release() {}
        })
        try {
            assertTrue(services.startNewGame())
            services.saveManager.saveGame(-1, services.sessionStore.state.value, "Engineering Bay")
            compose.setContent {
                DesktopStarbornTheme {
                    Box(Modifier.size(1280.dp, 720.dp)) {
                        DesktopMainMenuScreen(services, onStartGame = {}, onQuit = {})
                    }
                }
            }
            compose.waitForIdle()
            compose.onNodeWithText("Continue").assertIsDisplayed()
            compose.onNodeWithText("QUICKSAVE").assertIsDisplayed()
            compose.onNodeWithText("New Game").assertIsDisplayed()
        } finally { services.close(); directory.deleteRecursively() }
    }
    private fun capture(name: String) {
        val target = File("build/reports/desktop/screenshots/$name.png")
        target.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", target)
    }
}
