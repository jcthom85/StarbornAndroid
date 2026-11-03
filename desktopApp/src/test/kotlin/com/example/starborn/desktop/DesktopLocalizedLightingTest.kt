package com.example.starborn.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.data.local.UserSettings
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.environment.*
import com.example.starborn.ui.environment.EnvironmentalEffectsOverlay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopLocalizedLightingTest {
    @get:Rule val compose = createComposeRule()
    private val provider = DesktopAssetProvider()
    private val assets = WorldAssetDataSource(AssetJsonReader(provider, MoshiProvider.instance))
    private val out = File("build/reports/localized-lighting-motion").apply { mkdirs() }

    @Test fun authoredLightsFollowArtworkInDesktopFitAndPhoneCrop() {
        var id by mutableStateOf("pit_nova_bunk")
        var crop by mutableStateOf(false)
        var enabled by mutableStateOf(false)
        var time by mutableStateOf(2f)
        var loaded = ""
        compose.setContent {
            val room = assets.loadRooms().single { it.id == id }
            val painter = rememberDesktopAssetPainter(room.backgroundImage, provider)
            SideEffect { loaded = if (painter is DesktopBitmapPainter) id else "" }
            val effects = assets.environmentalEffectResolver.resolve(room, mapOf("light_on" to true), emptySet(), UserSettings())
                .filter { it.presetId.startsWith("lighting.") }
            BoxWithConstraints(Modifier.size(if (crop) 360.dp else 900.dp, if (crop) 640.dp else 600.dp).background(Color.Black).testTag("scene")) {
                Image(painter, null, Modifier.fillMaxSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit)
                val density = LocalDensity.current
                val geometry = EnvironmentalGeometry.fit(with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() }, painter.intrinsicSize.width, painter.intrinsicSize.height, crop)
                EnvironmentalEffectsOverlay(room.id, if (enabled) effects else emptyList(), geometry, previewTimeSeconds = time)
            }
        }
        for (room in listOf("pit_nova_bunk", "sector9_landing_glade", "source_campfire")) {
            compose.runOnIdle { id = room }
            compose.waitUntil(10000) { loaded == room }
            for (phone in listOf(false, true)) {
                compose.runOnIdle { crop = phone; enabled = false }
                val before = compose.onNodeWithTag("scene").captureToImage().asAwtImage()
                ImageIO.write(before, "png", File(out, "$room-${if (phone) "phone" else "desktop"}-off.png"))
                var changed = 0
                var firstMotionFrame: java.awt.image.BufferedImage? = null
                var mostMovingPixels = 0
                for (t in listOf(.6f, 2f, 4f, 6f, 8f)) {
                    compose.runOnIdle { enabled = true; time = t }
                    val after = compose.onNodeWithTag("scene").captureToImage().asAwtImage()
                    ImageIO.write(after, "png", File(out, "$room-${if (phone) "phone" else "desktop"}-$t.png"))
                    if (firstMotionFrame == null) firstMotionFrame = after
                    val first = requireNotNull(firstMotionFrame)
                    var movingPixels = 0
                    for (y in 0 until after.height) for (x in 0 until after.width) {
                        val a = first.getRGB(x,y); val b = after.getRGB(x,y)
                        val delta = maxOf(kotlin.math.abs((a shr 16 and 255) - (b shr 16 and 255)),
                            kotlin.math.abs((a shr 8 and 255) - (b shr 8 and 255)), kotlin.math.abs((a and 255) - (b and 255)))
                        if (delta >= 8) movingPixels++
                    }
                    mostMovingPixels = maxOf(mostMovingPixels, movingPixels)
                    if (t == 2f) {
                        for (y in 0 until before.height) for (x in 0 until before.width)
                            if (before.getRGB(x,y) != after.getRGB(x,y)) changed++
                        assertTrue("$room must visibly illuminate its surfaces", changed > 100)
                        if (!phone) assertEquals("light must not wash the desktop side margin", before.getRGB(10, before.height/2), after.getRGB(10, after.height/2))
                    }
                }
                assertTrue("$room needs perceptible temporal lighting, not just a few changed pixels: $mostMovingPixels",
                    mostMovingPixels > before.width * before.height * .01)
                assertTrue("$room must not tint the entire viewport", changed < before.width * before.height * .75)
                if (phone) {
                    val frames = File(out, "frames-$room").apply { mkdirs() }
                    repeat(48) { frame ->
                        compose.runOnIdle { time = .35f + frame / 8f }
                        ImageIO.write(compose.onNodeWithTag("scene").captureToImage().asAwtImage(), "png", File(frames, "%03d.png".format(frame)))
                    }
                }
            }
        }
    }

    @Test fun flashDisabledLightIsSteadyAndDoesNotInterceptAnAction() {
        val room = assets.loadRooms().single { it.id == "source_campfire" }
        val effects = assets.environmentalEffectResolver.resolve(room, emptyMap(), emptySet(), UserSettings(disableFlashes = true))
            .filter { it.presetId.startsWith("lighting.") }
        var time by mutableStateOf(2f)
        var clicks = 0
        compose.setContent {
            Box(Modifier.size(600.dp, 500.dp).background(Color(0xFF163139)).testTag("scene")) {
                Button(onClick = { clicks++ }, modifier = Modifier.testTag("rest")) { Text("Rest") }
                EnvironmentalEffectsOverlay(room.id, effects, EnvironmentalGeometry(600f,500f), previewTimeSeconds = time)
            }
        }
        val before = compose.onNodeWithTag("scene").captureToImage().asAwtImage()
        compose.runOnIdle { time = 8f }
        val after = compose.onNodeWithTag("scene").captureToImage().asAwtImage()
        for (y in 0 until before.height) for (x in 0 until before.width) assertEquals(before.getRGB(x,y), after.getRGB(x,y))
        compose.onNodeWithTag("rest").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
