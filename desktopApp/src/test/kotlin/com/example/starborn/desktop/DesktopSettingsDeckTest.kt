package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopSettingsContent
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class DesktopSettingsDeckTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settingsDeckRendersAllCategoriesAndSupportsLiveDialogueChatterTest() {
        val tempDir = Files.createTempDirectory("starborn-settings-test-").toFile()
        val services = DesktopAppServices(tempDir)
        try {
            assertTrue(services.startNewGame())
            val userSettings = com.example.starborn.data.local.UserSettings()

            compose.setContent {
                DesktopStarbornTheme {
                    Box(Modifier.size(1024.dp, 768.dp)) {
                        DesktopSettingsContent(
                            services = services,
                            userSettings = userSettings,
                            currentRoomTitle = "Nova's Bunk",
                            onReturnToTitle = {},
                            onOpenControls = {}
                        )
                    }
                }
            }

            compose.waitForIdle()

            // 1. Verify Audio Mixer deck
            compose.onNodeWithText("Audio Mixer Deck").assertExists()
            compose.onNodeWithText("Master volume").assertExists()
            compose.onNodeWithText("Voice").assertExists()
            compose.onNodeWithText("Dynamic Dialogue Chatter").assertExists()

            // 2. Verify Dialogue Chatter test button
            compose.onNodeWithText("Test Dialogue Chatter").assertExists()
            compose.onNodeWithText("Test Dialogue Chatter").performClick()

            // 3. Verify section switching
            compose.onNodeWithText("Display").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Display & Video Deck").assertExists()
            compose.onNodeWithText("Screen Mode").assertExists()

            compose.onNodeWithText("Controls").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Tactical Keyboard Reference").assertExists()

            compose.onNodeWithText("Save Archive").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Open save archive").assertExists()
            compose.onNodeWithText("Save and return to title").assertExists()

            // 4. Return to All Decks
            compose.onNodeWithText("All Decks").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Audio Mixer Deck").assertExists()

        } finally {
            services.close()
            tempDir.deleteRecursively()
        }
    }
}
