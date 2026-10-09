package com.example.starborn.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Pixel bounds shared with future side HUD layouts. The center always contains the whole asset. */
data class PortraitBackdropLayout(val center: Rect, val left: Rect, val right: Rect)

fun portraitBackdropLayout(viewport: Size, image: Size): PortraitBackdropLayout {
    val width = viewport.width.coerceAtLeast(0f)
    val height = viewport.height.coerceAtLeast(0f)
    val valid = image.width.isFinite() && image.height.isFinite() && image.width > 0 && image.height > 0
    val ratio = if (valid) min(width / image.width, height / image.height) else 0f
    val imageWidth = if (valid) image.width * ratio else width
    val imageHeight = if (valid) image.height * ratio else height
    val left = (width - imageWidth) / 2f
    val top = (height - imageHeight) / 2f
    return PortraitBackdropLayout(Rect(left, top, left + imageWidth, top + imageHeight),
        Rect(0f, 0f, left, height), Rect(left + imageWidth, 0f, width, height))
}

/** Fitted artwork and reflected atmosphere can be composed independently during room travel. */
@Composable
fun DesktopPortraitBackdrop(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    extensionScale: Float = 1f,
    extensionTranslationX: Float = 0f,
    extensionTranslationY: Float = 0f,
    animateCenter: Boolean = false,
    showCenter: Boolean = true,
    showExtensions: Boolean = true,
    centerModifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(PortraitBackdropLayout) -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds().background(if (showExtensions) Color(0xFF04060A) else Color.Transparent)) {
        val blurredPainter = rememberBlurredBackdropPainter(painter)
        val extensionPainter = blurredPainter ?: painter
        val seamPainter = rememberBlurredBackdropPainter(painter, 3f) ?: extensionPainter
        val extensionModifier = if (painter is DesktopBitmapPainter) Modifier else Modifier.blur(32.dp, BlurredEdgeTreatment.Unbounded)
        val density = LocalDensity.current
        val viewport = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val layout = portraitBackdropLayout(viewport, painter.intrinsicSize)
        val center = layout.center
        if (center.width > 0f && center.height > 0f) {
            if (showExtensions) {
            Canvas(Modifier.fillMaxSize().graphicsLayer {
                scaleX = extensionScale; scaleY = extensionScale
                translationX = extensionTranslationX; translationY = extensionTranslationY
            }.then(extensionModifier)) {
                // Overscan and reflect vertically too, so blur never fades into empty edges.
                val bleed = 96.dp.toPx()
                val firstX = floor((-bleed - center.left) / center.width).toInt()
                val lastX = ceil((size.width + bleed - center.left) / center.width).toInt()
                val firstY = floor((-bleed - center.top) / center.height).toInt()
                val lastY = ceil((size.height + bleed - center.top) / center.height).toInt()
                for (y in firstY..lastY) for (x in firstX..lastX) {
                    translate(center.left + x * center.width, center.top + y * center.height) {
                        scale(if (x % 2 == 0) 1f else -1f, if (y % 2 == 0) 1f else -1f,
                            pivot = Offset(center.width / 2f, center.height / 2f)) {
                            with(extensionPainter) { draw(Size(center.width, center.height)) }
                        }
                    }
                }
            }
            // Reflected detail fades into the broad blur, making the seam continuous.
            Canvas(Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                scaleX = extensionScale; scaleY = extensionScale
                translationX = extensionTranslationX; translationY = extensionTranslationY
            }) {
                val reach = min(96.dp.toPx(), center.width * .22f)
                for (x in listOf(-1, 1)) {
                    translate(center.left + x * center.width, center.top) {
                        scale(-1f, 1f, pivot = Offset(center.width / 2f, center.height / 2f)) {
                            with(seamPainter) { draw(Size(center.width, center.height)) }
                        }
                    }
                }
                drawRect(Brush.horizontalGradient(
                    0f to Color.Transparent,
                    ((center.left - reach) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                    (center.left / size.width) to Color.White,
                    (center.right / size.width) to Color.White,
                    ((center.right + reach) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                    1f to Color.Transparent), blendMode = BlendMode.DstIn)
            }
            // Darken only the extensions, easing the tint to zero at the sharp image edge.
            Canvas(Modifier.fillMaxSize()) {
                if (layout.left.width > 0) drawRect(Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = .36f), Color.Transparent), 0f, center.left),
                    size = Size(center.left, size.height))
                if (layout.right.width > 0) drawRect(Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = .36f)), center.right, size.width),
                    topLeft = Offset(center.right, 0f), size = Size(layout.right.width, size.height))
            }
            }
            if (showCenter) Image(painter, contentDescription,
                Modifier.offset { IntOffset(center.left.roundToInt(), center.top.roundToInt()) }
                    .size(with(density) { center.width.toDp() }, with(density) { center.height.toDp() })
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = true }
                    .drawWithContent {
                        drawContent()
                        val feather = min(20.dp.toPx(), size.width * .04f)
                        drawRect(Brush.horizontalGradient(
                            0f to Color.Transparent, (feather / size.width) to Color.White,
                            (1f - feather / size.width) to Color.White, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn)
                    }
                    .then(centerModifier)
                    .graphicsLayer {
                        // Exploration/hub art remains fitted; cinematic cameras opt into
                        // applying the same transform to the original and its reflections.
                        scaleX = if (animateCenter) extensionScale else 1f
                        scaleY = if (animateCenter) extensionScale else 1f
                        translationX = if (animateCenter) extensionTranslationX else 0f
                        translationY = if (animateCenter) extensionTranslationY else 0f
                    },
                contentScale = ContentScale.Fit)
        }
        content(layout)
    }
}
