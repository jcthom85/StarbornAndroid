package com.example.starborn

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.data.local.UserSettings
import com.example.starborn.domain.environment.EnvironmentalGeometry
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.environment.EnvironmentalEffectsOverlay
import com.example.starborn.ui.vfx.rememberAndroidEnvironmentAssets
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LocalizedLightingInstrumentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun threeAuthoredRoomsRenderOnAndroidAndLightingDoesNotBlockRoomActions() {
        var id by mutableStateOf("pit_nova_bunk")
        var lightOn by mutableStateOf(false)
        var disabled by mutableStateOf(false)
        var clicks = 0
        var resolvedIds = emptyList<String>()
        compose.setContent {
            com.example.starborn.ui.theme.StarbornTheme {
                val assets = rememberAndroidEnvironmentAssets()
                val room = assets.loadRooms().single { it.id == id }
                val painter = rememberRoomBackgroundPainter(room.backgroundImage)
                val effects = assets.environmentalEffectResolver.resolve(room, mapOf("light_on" to lightOn), emptySet(), UserSettings(disableFlashes = disabled))
                    .filter { it.presetId.startsWith("lighting.") }
                SideEffect { resolvedIds = effects.map { it.id } }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    val density = LocalDensity.current
                    val geometry = EnvironmentalGeometry.fit(with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() }, painter.intrinsicSize.width, painter.intrinsicSize.height, true)
                    Button(onClick = { clicks++; lightOn = true }, modifier = Modifier.testTag("room-action")) { Text("Room action") }
                    EnvironmentalEffectsOverlay(room.id, effects, geometry, previewTimeSeconds = 4f)
                }
            }
        }
        compose.runOnIdle { assertTrue(resolvedIds.isEmpty()) }
        compose.onNodeWithTag("room-action").performClick()
        compose.runOnIdle { assertEquals(listOf("bunk_lamplight"), resolvedIds) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "localized-lighting").apply { mkdirs() }
        for (roomId in listOf("pit_nova_bunk", "sector9_landing_glade", "source_campfire")) {
            compose.runOnIdle { id = roomId }
            compose.waitForIdle()
            compose.runOnIdle { assertTrue(resolvedIds.isNotEmpty()) }
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(folder, "$roomId.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            compose.onNodeWithTag("room-action").performClick()
        }
        compose.runOnIdle { disabled = true }
        compose.onNodeWithTag("room-action").performClick()
        compose.runOnIdle { assertEquals(5, clicks) }
    }
}
