package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopPortraitBackdropTest {
    @get:Rule val compose = createComposeRule()

    @Test fun layoutFitsIntrinsicAspectAndExposesSymmetricSideSpaces() {
        listOf(Size(1920f, 1080f), Size(1280f, 800f), Size(2560f, 1080f), Size(400f, 800f)).forEach { viewport ->
            listOf(Size(1088f, 1920f), Size(840f, 1871f), Size(1024f, 1536f)).forEach { image ->
                val layout = portraitBackdropLayout(viewport, image)
                assertEquals(image.width / image.height, layout.center.width / layout.center.height, .0001f)
                assertEquals(viewport.width / 2f, layout.center.center.x, .001f)
                assertEquals(viewport.height / 2f, layout.center.center.y, .001f)
                assertEquals(layout.left.width, layout.right.width, .001f)
                assertTrue(layout.center.left >= 0 && layout.center.top >= 0)
                assertTrue(layout.center.right <= viewport.width && layout.center.bottom <= viewport.height)
            }
        }
        val layout = portraitBackdropLayout(Size(1920f, 1080f), Size(1088f, 1920f))
        assertEquals(612f, layout.center.width, .001f)
        assertEquals(654f, layout.left.width, .001f)
        assertEquals(Rect(0f, 0f, 0f, 0f), portraitBackdropLayout(Size.Zero, Size.Unspecified).center)
    }

    @Test fun centerRetainsEveryEdgeAndStaysSharpWhenExtensionsMove() {
        val bitmap = ImageBitmap(108, 192)
        val canvas = Canvas(bitmap)
        canvas.drawRect(Rect(0f, 0f, 108f, 64f), Paint().apply { color = Color.Red })
        canvas.drawRect(Rect(0f, 64f, 108f, 128f), Paint().apply { color = Color.Green })
        canvas.drawRect(Rect(0f, 128f, 108f, 192f), Paint().apply { color = Color.Blue })
        val motion = mutableStateOf(false)
        compose.setContent {
            Box(Modifier.size(1024.dp, 576.dp).testTag("stage")) {
                DesktopPortraitBackdrop(BitmapPainter(bitmap), "Test art",
                    extensionScale = if (motion.value) 1.8f else 1f,
                    extensionTranslationX = if (motion.value) 100f else 0f)
            }
        }
        repeat(2) {
            compose.waitForIdle()
            val image = compose.onNodeWithTag("stage").captureToImage().asAwtImage()
            assertEquals(0xFF0000, image.getRGB(512, 2) and 0xFFFFFF)
            assertEquals(0x00FF00, image.getRGB(512, 288) and 0xFFFFFF)
            assertEquals(0x0000FF, image.getRGB(512, 573) and 0xFFFFFF)
            compose.runOnIdle { motion.value = true }
        }
    }

    @Test fun realRoomHubAndIntroBackdropsRenderAtFourWindowRatios() {
        val directory = Files.createTempDirectory("starborn-portrait-backdrop-").toFile()
        val services = DesktopAppServices(directory)
        val selectedPath = mutableStateOf("images/rooms/astra/bridge.webp")
        val dimensions = mutableStateOf(Size(1024f, 576f))
        var decodedPath: String? = null
        try {
            compose.setContent {
                val painter = rememberDesktopAssetPainter(selectedPath.value, services.assetProvider)
                decodedPath = selectedPath.value.takeIf { painter.intrinsicSize.width.isFinite() }
                Box(Modifier.size(dimensions.value.width.dp, dimensions.value.height.dp).testTag("stage")) {
                    DesktopPortraitBackdrop(painter, "Artwork")
                }
            }
            val screenshots = File("build/reports/desktop/screenshots/portrait-backdrops").apply { mkdirs() }
            val assets = listOf("room" to "images/rooms/astra/bridge.webp",
                "hub" to "images/hubs/astra_interior.webp",
                "intro" to "images/cinematics/intro_sector9_breach_v1.webp")
            val sizes = listOf("16x9" to Size(1024f, 576f), "16x10" to Size(1024f, 640f),
                "ultrawide" to Size(1008f, 432f), "narrow" to Size(480f, 768f))
            assets.forEach { (name, path) ->
                compose.runOnIdle { selectedPath.value = path }
                compose.waitUntil(10_000) { decodedPath == path }
                sizes.forEach { (ratio, size) ->
                    compose.runOnIdle { dimensions.value = size }
                    compose.waitForIdle()
                    ImageIO.write(compose.onNodeWithTag("stage").captureToImage().asAwtImage(), "png", File(screenshots, "$name-$ratio.png"))
                }
            }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun liveHubUsesPortraitBackdrop() {
        val directory = Files.createTempDirectory("starborn-hub-portrait-").toFile()
        val services = DesktopAppServices(directory)
        try {
            assertTrue(services.startNewGame())
            compose.setContent { DesktopStarbornTheme { DesktopHubScreen(services, {}, {}, {}) } }
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Homestead Quarter").fetchSemanticsNodes().isNotEmpty() }
            val screenshots = File("build/reports/desktop/screenshots/portrait-backdrops").apply { mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", File(screenshots, "live-hub.png"))
        } finally { services.close(); directory.deleteRecursively() }
    }
}
