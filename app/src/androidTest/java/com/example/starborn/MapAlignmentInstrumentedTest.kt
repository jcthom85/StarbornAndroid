package com.example.starborn

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.feature.exploration.ui.InteractiveMapPanel
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.ui.map.*
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class MapAlignmentInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectingHubExitShowsItsDestinationWithoutMovingThePlayer() {
        val cells = listOf(
            cell("bunk", 0).copy(isCurrent = true, title = "Nova's Bunk", connections = mapOf("east" to "exit"), services = setOf(MinimapService.REST)),
            cell("exit", 1).copy(title = "Colony Gate", connections = mapOf("west" to "bunk"), hubExit = MapHubExitUi("Homestead Quarter", true), services = setOf(MinimapService.EXIT), blockedDirections = setOf("west")),
            cell("dark", -1).copy(title = "Dark room", isDark = true)
        )
        compose.setContent { StarbornTheme {
            Column(Modifier.fillMaxSize().background(MapPalette.background).padding(16.dp)) {
                InteractiveMapPanel(FullMapUiState(cells))
                Spacer(Modifier.height(12.dp))
                AreaMapCanvas(cells, Modifier.size(140.dp), minimap = true)
            }
        } }
        compose.onNodeWithText("Nova's Bunk").assertIsDisplayed()
        compose.onNodeWithTag("area-map").performTouchInput { click(center + Offset(58.dp.toPx(), 0f)) }
        compose.onNodeWithText("Colony Gate").assertIsDisplayed()
        compose.onNodeWithText("Exit to Homestead Quarter — blocked").assertIsDisplayed()
        compose.onNodeWithTag("area-map").assert(hasContentDescription("Nova's Bunk. You are here", substring = true))
        capture("android-map-alignment.png")
        compose.onNodeWithText("You are here").performClick()
        compose.onNodeWithText("Nova's Bunk").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), name)
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        // Keep visual QA artifacts after Gradle uninstalls the test app.
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${output.absolutePath} /sdcard/Download/$name")
        ).use { it.readBytes() }
    }

    private fun cell(id: String, x: Int) = MinimapCellUi(id, x, 0, x, 0,
        visited = true, discovered = true, isCurrent = false, hasEnemies = false,
        blockedDirections = emptySet(), connections = emptyMap())
}
