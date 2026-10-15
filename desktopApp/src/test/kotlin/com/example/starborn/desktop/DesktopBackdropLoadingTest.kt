package com.example.starborn.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DesktopBackdropLoadingTest {
    @get:Rule val compose = createComposeRule()
    private fun image(first: Color, second: Color): ImageBitmap = ImageBitmap(108,192).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawRect(Rect(0f,0f,108f,192f), Paint().apply { color=first })
        for (y in 0 until 192 step 8) for (x in 0 until 108 step 8)
            if ((x/8+y/8)%2==0) canvas.drawRect(Rect(x.toFloat(),y.toFloat(),x+8f,y+8f),Paint().apply { color=second })
    }
    @Test fun coldRoomChangesNeverExposeSharpReflectionsWhileBlurIsPending() {
        val rooms = listOf(image(Color.White,Color.Black),image(Color.White,Color.Red))
        val gates = rooms.associateWith { CompletableDeferred<ImageBitmap>() }
        val blurred = image(Color.DarkGray,Color.DarkGray)
        var selected by mutableStateOf(rooms.first())
        compose.setContent {
            CompositionLocalProvider(LocalBackdropBlurLoader provides { bitmap, _ -> gates.getValue(bitmap).await() }) {
                Box(Modifier.size(1024.dp,576.dp).testTag("stage")) { DesktopPortraitBackdrop(DesktopBitmapPainter(selected),"Room") }
            }
        }
        rooms.forEach { bitmap ->
            compose.runOnIdle { selected = bitmap }
            fun assertCovered(phase: String) {
                val shot=compose.onNodeWithTag("stage").captureToImage().asAwtImage()
                val file=java.io.File("build/reports/desktop/screenshots/backdrop-loading/room-${rooms.indexOf(bitmap)}-$phase.png").apply {parentFile.mkdirs()}
                javax.imageio.ImageIO.write(shot,"png",file)
                for(x in listOf(80,160,240,780,860,940)) for(y in 40 until 536 step 8) {
                    val color=java.awt.Color(shot.getRGB(x,y))
                    assertTrue("Sharp room detail leaked into side atmosphere at $x,$y",color.red<180 && color.green<180 && color.blue<180)
                }
                assertTrue("The sharp central artwork must remain visible",(0 until 192).any { y ->
                    val c=java.awt.Color(shot.getRGB(512,y*3)); c.red>220 && c.green>220 && c.blue>220
                })
            }
            assertCovered("pending")
            compose.runOnIdle { gates.getValue(bitmap).complete(blurred) }
            compose.waitForIdle()
            assertCovered("ready")
        }
    }
    @Test fun translatingTheCenterCannotDrawOverEitherSideExtension() {
        val red=image(Color.Red,Color.Red)
        var shift by mutableStateOf(-180f)
        compose.setContent {
            Box(Modifier.size(1024.dp,576.dp).background(Color.Blue).testTag("stage")) {
                DesktopPortraitBackdrop(DesktopBitmapPainter(red),null,showExtensions=false,
                    centerModifier=Modifier.graphicsLayer { translationX=shift })
            }
        }
        val bounds=portraitBackdropLayout(Size(1024f,576f),Size(108f,192f)).center
        listOf(-180f,180f).forEach { direction ->
            compose.runOnIdle { shift=direction }
            val shot=compose.onNodeWithTag("stage").captureToImage().asAwtImage()
            for(x in listOf(bounds.left.toInt()-40,bounds.right.toInt()+40))
                assertEquals("Translated sharp artwork escaped its fixed viewport",0x0000FF,shot.getRGB(x,288) and 0xFFFFFF)
        }
    }
}
