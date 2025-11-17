package com.example.starborn.ui.vfx

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.BandStyle
import com.example.starborn.data.local.Theme
import com.example.starborn.ui.theme.themeColor

@Composable
fun ThemeBandOverlay(
    theme: Theme?,
    bandStyle: BandStyle?,
    darkness: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val rawHeight = remember(maxWidth, maxHeight) {
            val safeHeight = maxWidth * (16f / 9f)
            val leftover = (maxHeight - safeHeight)
            if (leftover <= 0.dp) 0.dp else leftover / 2f
        }
        val minBand = (bandStyle?.minHeightDp ?: 48f).dp
        val maxBand = (bandStyle?.maxHeightDp ?: 220f).dp
        val targetHeight = if (rawHeight <= 0.dp) {
            0.dp
        } else {
            rawHeight.coerceIn(minBand, maxBand)
        }
        val bandHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "themeBandHeight"
        )

        if (bandHeight <= 0.dp) return@BoxWithConstraints

        Box(modifier = Modifier.fillMaxSize()) {
            Band(
                theme = theme,
                bandStyle = bandStyle,
                darkness = darkness,
                isTop = true,
                height = bandHeight,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
            Band(
                theme = theme,
                bandStyle = bandStyle,
                darkness = darkness,
                isTop = false,
                height = bandHeight,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Band(
    theme: Theme?,
    bandStyle: BandStyle?,
    darkness: Float,
    isTop: Boolean,
    height: Dp,
    modifier: Modifier
) {
    val density = LocalDensity.current
    val baseColor = remember(theme) {
        themeColor(theme?.bg, if (isTop) Color(0xFF070B11) else Color(0xFF05070B))
    }
    val accent = remember(theme) {
        themeColor(theme?.accent, Color(0xFF6CD5FF))
    }
    val borderColor = remember(theme) {
        themeColor(theme?.border, accent.copy(alpha = 0.8f))
    }
    val baseAlpha = (bandStyle?.baseAlpha ?: 0.65f).coerceIn(0f, 1f)
    val darkBoost = (bandStyle?.darkBoost ?: 0.35f).coerceIn(0f, 1f)
    val highlightAlpha = (bandStyle?.highlightAlpha ?: 0.35f).coerceIn(0f, 1f)
    val noiseAlpha = (bandStyle?.noiseAlpha ?: 0.05f).coerceIn(0f, 1f)
    val overlayAlpha = (baseAlpha + darkness * darkBoost).coerceIn(0f, 1f)
    val fadeEnd = if (isTop) 0.2f else 0.8f
    Canvas(
        modifier = modifier.height(height)
    ) {
        val gradient = if (isTop) {
            Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = overlayAlpha),
                    baseColor.copy(alpha = overlayAlpha * 0.35f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = overlayAlpha * 0.35f),
                    baseColor.copy(alpha = overlayAlpha)
                )
            )
        }
        drawRect(brush = gradient, size = size)

        val accentHeight = with(density) { 16.dp.toPx() }
        val accentBrush = if (isTop) {
            Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = highlightAlpha),
                    accent.copy(alpha = highlightAlpha * 0.15f),
                    Color.Transparent
                ),
                startY = size.height - accentHeight,
                endY = size.height
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = highlightAlpha * 0.15f),
                    borderColor.copy(alpha = highlightAlpha)
                ),
                startY = 0f,
                endY = accentHeight
            )
        }
        drawRect(brush = accentBrush, size = size)

        val borderStroke = with(density) { 1.2.dp.toPx() }
        val borderY = if (isTop) size.height - borderStroke / 2 else borderStroke / 2
        drawLine(
            color = borderColor.copy(alpha = 0.65f + darkness * 0.25f),
            start = Offset(0f, borderY),
            end = Offset(size.width, borderY),
            strokeWidth = borderStroke
        )

        if (noiseAlpha > 0f) {
            val step = with(density) { 6.dp.toPx() }
            var y = 0f
            while (y < size.height) {
                val progress = if (isTop) 1f - (y / size.height) else (y / size.height)
                val alpha = (noiseAlpha * (0.2f + progress * 0.8f)).coerceIn(0f, 1f)
                drawLine(
                    color = accent.copy(alpha = alpha * 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        val fadeHeight = with(density) { 18.dp.toPx() }
        val overlayBrush = Brush.verticalGradient(
            colors = if (isTop) {
                listOf(
                    Color.Black.copy(alpha = darkness * 0.35f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = darkness * 0.35f)
                )
            }
        )
        val overlayStartY = if (isTop) size.height - fadeHeight else 0f
        drawRect(
            brush = overlayBrush,
            topLeft = Offset(0f, overlayStartY),
            size = Size(size.width, fadeHeight)
        )

        if (darkness > 0f) {
            val vignetteBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = overlayAlpha * 0.25f),
                    Color.Transparent
                ),
                startY = if (isTop) 0f else size.height * fadeEnd,
                endY = if (isTop) size.height * fadeEnd else size.height
            )
            drawRect(brush = vignetteBrush, size = size)
        }
    }
}
