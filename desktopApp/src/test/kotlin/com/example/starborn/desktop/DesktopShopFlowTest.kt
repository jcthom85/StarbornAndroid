package com.example.starborn.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopShopDialog
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DesktopShopFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shopDialogRendersBuySellAndPerformsTransactions() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_shop_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            assertTrue(services.startNewGame())

            // Give player initial credits and scrap metal to sell
            services.sessionStore.addCredits(500)
            services.inventoryService.addItem("scrap_metal", 5)

            var dismissed = false

            composeTestRule.setContent {
                DesktopStarbornTheme(services) {
                    DesktopShopDialog(
                        services = services,
                        shopId = "mechanic_shop",
                        onDismiss = { dismissed = true }
                    )
                }
            }

            // Assert Shop Title and credits balance
            composeTestRule.onNodeWithText("Mechanic's Wares").assertExists()
            composeTestRule.onNodeWithText("CREDITS", substring = true).assertExists()

            // Verify wares in Buy tab
            composeTestRule.onNodeWithTag("shop-item-medkit").assertExists()
            composeTestRule.onNodeWithText("Medkit").assertExists()

            // Purchase 1 Medkit
            val initialCredits = services.sessionStore.state.value.playerCredits
            composeTestRule.onNodeWithTag("buy-button-medkit").performClick()

            // Confirm credits reduced and medkit added
            composeTestRule.waitForIdle()
            val afterBuyCredits = services.sessionStore.state.value.playerCredits
            assertTrue("Credits should decrease after purchase", afterBuyCredits < initialCredits)
            assertTrue("Medkit should be in inventory", services.inventoryService.hasItem("medkit", 1))

            // Switch to SELL tab
            composeTestRule.onNodeWithText("SELL", substring = true).performClick()

            // Verify Scrap Metal is listed in Sell tab
            composeTestRule.onNodeWithTag("sell-item-scrap_metal").assertExists()

            // Sell 1 Scrap Metal
            composeTestRule.onNodeWithTag("sell-button-scrap_metal").performClick()

            // Confirm credits increased and scrap metal quantity decreased
            composeTestRule.waitForIdle()
            val afterSellCredits = services.sessionStore.state.value.playerCredits
            assertTrue("Credits should increase after sale", afterSellCredits > afterBuyCredits)
            assertEquals(4, services.inventoryService.snapshot()["scrap_metal"] ?: 0)

            // Switch to CHAT tab
            composeTestRule.onNodeWithText("CHAT", substring = true).performClick()
            composeTestRule.onNodeWithText("INQUIRIES & RUMORS").assertExists()

            // Dismiss via close button
            composeTestRule.onNodeWithText("Close").performClick()
            assertTrue("Shop dialog should call onDismiss", dismissed)
        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
