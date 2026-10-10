package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopFieldKitScreen
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import androidx.compose.ui.graphics.asAwtImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopFieldKitFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureDesktopFieldKitScreen() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_fieldkit_snap_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            assertTrue(services.startNewGame())
            services.sessionStore.addCredits(1450)
            services.inventoryService.addItem("medkit", 4)
            services.inventoryService.addItem("scrap_metal", 15)
            services.inventoryService.addItem("wooden_rod", 1)
            services.inventoryService.addItem("basic_lure", 2)

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                DesktopStarbornTheme(services) {
                    DesktopFieldKitScreen(
                        services = services,
                        onClose = {}
                    )
                }
            }
            // Switch to CARGO to show the multi-pane manifest
            composeTestRule.onNodeWithTag("tab-cargo").performClick()
            composeTestRule.mainClock.advanceTimeBy(300)

            val artifactsDir = File("C:/Users/jcthomas/.gemini/antigravity-ide/brain/4679957f-9fc8-4b49-a04b-cc85f289b043")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_field_kit_preview.png"))
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }

    @Test
    fun rendersFieldKitDeckAndSwitchesTabs() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_fieldkit_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            assertTrue(services.startNewGame())
            services.sessionStore.addCredits(250)

            var closed = false

            composeTestRule.setContent {
                DesktopStarbornTheme(services) {
                    DesktopFieldKitScreen(
                        services = services,
                        onClose = { closed = true }
                    )
                }
            }

            // Verify Header telemetry
            composeTestRule.onNodeWithText("FIELD KIT").assertExists()
            composeTestRule.onNodeWithText("TACTICAL FIELD DECK", substring = true).assertExists()
            composeTestRule.onNodeWithText("250 CREDITS").assertExists()

            // Verify Navigation Tab buttons exist
            composeTestRule.onNodeWithTag("tab-loadout").assertExists()
            composeTestRule.onNodeWithTag("tab-cargo").assertExists()
            composeTestRule.onNodeWithTag("tab-crafting").assertExists()

            // Switch to CARGO MANIFEST tab
            composeTestRule.onNodeWithTag("tab-cargo").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("cargo-search-input").assertExists()

            // Switch to TINKERING BENCH tab
            composeTestRule.onNodeWithTag("tab-crafting").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Workbench", substring = true).assertExists()

            // Switch back to LOADOUT tab
            composeTestRule.onNodeWithTag("tab-loadout").performClick()
            composeTestRule.waitForIdle()

            // Close Field Kit
            composeTestRule.onNodeWithTag("field-kit-close-button").performClick()
            assertTrue("Field kit should trigger onClose callback", closed)
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }

    @Test
    fun cargoManifestFiltersAndInspectsItems() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_cargo_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            assertTrue(services.startNewGame())

            // Add various items
            services.inventoryService.addItem("medkit", 3)
            services.inventoryService.addItem("scrap_metal", 10)

            composeTestRule.setContent {
                DesktopStarbornTheme(services) {
                    DesktopFieldKitScreen(
                        services = services,
                        onClose = {}
                    )
                }
            }

            // Switch to CARGO tab
            composeTestRule.onNodeWithTag("tab-cargo").performClick()
            composeTestRule.waitForIdle()

            // Verify both items listed in ALL category
            composeTestRule.onNodeWithTag("cargo-item-medkit").assertExists()
            composeTestRule.onNodeWithTag("cargo-item-scrap_metal").assertExists()

            // Click Consumables category filter
            composeTestRule.onNodeWithTag("cargo-category-consumables").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("cargo-item-medkit").assertExists()
            composeTestRule.onNodeWithTag("cargo-item-scrap_metal").assertDoesNotExist()

            // Click Materials category filter
            composeTestRule.onNodeWithTag("cargo-category-materials").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("cargo-item-scrap_metal").assertExists()
            composeTestRule.onNodeWithTag("cargo-item-medkit").assertDoesNotExist()

            // Switch back to All
            composeTestRule.onNodeWithTag("cargo-category-all").performClick()
            composeTestRule.waitForIdle()

            // Search filter
            composeTestRule.onNodeWithTag("cargo-search-input").performTextInput("scrap")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("cargo-item-scrap_metal").assertExists()
            composeTestRule.onNodeWithTag("cargo-item-medkit").assertDoesNotExist()

            // Clear search
            composeTestRule.onNodeWithTag("cargo-search-input").performTextClearance()
            composeTestRule.waitForIdle()

            // Select Medkit to inspect
            composeTestRule.onNodeWithTag("cargo-item-medkit").performClick()
            composeTestRule.waitForIdle()

            // Verify Tactical Item Inspector details
            composeTestRule.onNodeWithText("MEDKIT").assertExists()
            composeTestRule.onNodeWithText("QTY: 3").assertExists()
            composeTestRule.onNodeWithText("DEPLOY ON PARTY MEMBER:", substring = true).assertExists()
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
