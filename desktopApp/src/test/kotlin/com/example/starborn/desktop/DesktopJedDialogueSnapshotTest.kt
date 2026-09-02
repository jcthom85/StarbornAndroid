package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopDialogueOverlay
import com.example.starborn.desktop.ui.DesktopExplorationScreen
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

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            // Move to Jed's bunk
            services.sessionStore.setRoom("pit_jed_bunk")

            composeTestRule.setContent {
                DesktopDialogueOverlay(
                    speakerName = "Jed",
                    speakerRole = "Senior Pit Mechanic",
                    portraitId = "jed_portrait",
                    text = "You look awful. Sit down.",
                    choices = emptyList(),
                    services = services,
                    onSelectChoice = {},
                    onAdvance = {},
                    onClose = {}
                )
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_jed_dialogue.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
