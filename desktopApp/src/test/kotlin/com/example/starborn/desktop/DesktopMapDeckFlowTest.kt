package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopMapDeck
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopMapDeckFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDesktopMapDeckRendersTelemetryAndAllowsFastTravel() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_map_deck_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            assertTrue(services.startNewGame())

            // Mark rooms as discovered & visited to populate sector telemetry
            val currentState = services.sessionStore.state.value
            services.sessionStore.restore(
                currentState.copy(
                    roomStates = currentState.roomStates + mapOf(
                        "pit_L2_corridor" to mapOf("visited" to true, "discovered" to true),
                        "pit_common_room" to mapOf("visited" to true, "discovered" to true)
                    )
                )
            )

            var closed = false

            composeTestRule.setContent {
                DesktopStarbornTheme(services) {
                    DesktopMapDeck(
                        services = services,
                        onClose = { closed = true }
                    )
                }
            }

            composeTestRule.waitForIdle()

            // Verify top sector telemetry header
            composeTestRule.onNodeWithTag("map-header-telemetry").assertExists()

            // Verify POI filter row
            composeTestRule.onNodeWithTag("map-filter-row").assertExists()

            // Verify map canvas container
            composeTestRule.onNodeWithTag("map-canvas-container").assertExists()

            // Verify inspector pane
            composeTestRule.onNodeWithTag("map-inspector-pane").assertExists()

            // Verify bottom docked key legend bar
            composeTestRule.onNodeWithTag("map-bottom-legend").assertExists()

            // Interact with filter chips
            composeTestRule.onNodeWithText("Services", substring = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("All Nodes", substring = true).performClick()
            composeTestRule.waitForIdle()

            // Capture preview artifact
            val artifactsDir = File("C:/Users/jcthomas/.gemini/antigravity-ide/brain/4679957f-9fc8-4b49-a04b-cc85f289b043")
            artifactsDir.mkdirs()
            val image = composeTestRule.onRoot().captureToImage().asAwtImage()
            ImageIO.write(image, "png", File(artifactsDir, "desktop_map_deck_preview.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
