package com.example.starborn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.feature.exploration.ui.AreaEntryOverlay
import com.example.starborn.feature.exploration.viewmodel.AreaEntryUi
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class AreaEntryPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstArrivalShowsAreaAndDiscoveryMarkerWithoutFlavorCopy() = checkCard(true)
    @Test fun revisitShowsOnlyAreaName() = checkCard(false)

    private fun checkCard(firstVisit: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent { StarbornTheme {
            AreaEntryOverlay(AreaEntryUi(1L, "Old Mining Road", "The Mines", "Dust drifts across an abandoned road.", firstVisit)) {}
        } }
        compose.mainClock.advanceTimeBy(220)
        compose.onNodeWithText("Old Mining Road").assertIsDisplayed()
        if (firstVisit) compose.onNodeWithText("NEW AREA").assertIsDisplayed()
        else compose.onNodeWithText("NEW AREA").assertDoesNotExist()
        compose.onNodeWithText("The Mines", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Dust drifts across an abandoned road.").assertDoesNotExist()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.getExternalFilesDir(null), "area-entry-review").apply { mkdirs() }
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(output, if (firstVisit) "new-area.png" else "revisit.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
