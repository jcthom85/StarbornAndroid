package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopDialogueOverlay
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import androidx.compose.runtime.mutableStateOf
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopJedDialogueSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureJedDialogueSnapshot() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_jed_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        val services = DesktopAppServices(saveDirectory = tempSaveDir)
        var advanced = false
        val choiceMode = mutableStateOf(false)
        var picked = -1
        try {
            services.startNewGame()

            // Move to Jed's bunk
            services.sessionStore.setRoom("pit_jed_bunk")

            composeTestRule.setContent {
                DesktopStarbornTheme { DesktopDialogueOverlay(
                    speakerName = "Jed",
                    speakerRole = "Senior Pit Mechanic",
                    portraitId = "images/npcs/jed.webp",
                    text = "You look awful. Sit down. " .repeat(30),
                    choices = if (choiceMode.value) listOf("Ask about the mine", "Ask about the ship") else emptyList(),
                    services = services,
                    onSelectChoice = { picked = it },
                    onAdvance = { advanced = true },
                    onClose = {}
                ) }
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File("build/reports/desktop/screenshots")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_jed_dialogue.png"))
            composeTestRule.onNodeWithText("Continue [Enter]").performScrollTo().performClick()
            assertTrue(advanced)
            composeTestRule.runOnIdle { choiceMode.value = true }
            composeTestRule.onNodeWithText("◆ [1] Ask about the mine").performScrollTo().performClick()
            assertEquals(0, picked)

        } finally {
            services.close()
            tempSaveDir.deleteRecursively()
        }
    }
}
