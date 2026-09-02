package com.example.starborn.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAwtImage
import com.example.starborn.desktop.ui.DesktopCinematicScreen
import com.example.starborn.domain.cinematic.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopCinematicSnapshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktopCinematicScene() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_cine_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            val testScene = CinematicScene(
                id = "intro_prologue",
                title = "PROLOGUE // DERELICT STATIONS OF SECTOR 4",
                backdrop = CinematicBackdrop.ROOM,
                presentation = CinematicPresentation.ILLUSTRATED,
                steps = listOf(
                    CinematicStep(
                        type = CinematicStepType.DIALOGUE,
                        speaker = "Nova",
                        portrait = "images/characters/nova_combat.png",
                        imagePath = "images/rooms/world_1/pit_L1_landing_v5.webp",
                        text = "Scanners are picking up heavy ionic anomalies deep inside the derelict core. Keep your weapons primed and check the perimeter."
                    )
                ),
                skippable = true
            )

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                DesktopCinematicScreen(
                    services = services,
                    scene = testScene,
                    onComplete = {}
                )
            }
            composeTestRule.mainClock.advanceTimeBy(300)

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_cinematic_preview.png"))
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
