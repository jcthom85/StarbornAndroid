package com.example.starborn.ui.exploration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import kotlin.math.*

/** Same geometry as Android's centered Crop and desktop's centered Fit artwork. */
fun templeGateArtworkFrame(viewport: Size, artwork: Size, crop: Boolean): Rect {
    val image = if (artwork.width.isFinite() && artwork.height.isFinite() && artwork.width > 0 && artwork.height > 0) artwork else Size(1088f, 1920f)
    val scale = if (crop) max(viewport.width / image.width, viewport.height / image.height) else min(viewport.width / image.width, viewport.height / image.height)
    val w = image.width * scale
    val h = image.height * scale
    return Rect((viewport.width - w) / 2, (viewport.height - h) / 2, (viewport.width + w) / 2, (viewport.height + h) / 2)
}

/** Wake the painted lock, dissolve its seal, and leave the matching exterior passage open. */
@Composable
fun TempleGateReveal(progress: Float, subdued: Boolean, modifier: Modifier = Modifier,
    closedPainter: Painter? = null, cropArtwork: Boolean = false) {
    Canvas(modifier.fillMaxSize().testTag("temple-gate-reveal")) {
        val p = progress.coerceIn(0f, 1f)
        val opening = ((p - .28f) / .50f).coerceIn(0f, 1f)
        val opened = opening * opening * (3 - 2 * opening)
        val fade = ((1f - p) / .22f).coerceIn(0f, 1f)
        val charge = (p / .28f).coerceIn(0f, 1f)
        val frame = templeGateArtworkFrame(size, closedPainter?.intrinsicSize ?: Size.Unspecified, cropArtwork)
        closedPainter?.let { painter ->
            translate(frame.left, frame.top) { with(painter) { draw(Size(frame.width, frame.height), alpha = 1 - opened) } }
        }
        // The lock plate is at (0.50, 0.43) in temple_gate.webp, not at the screen center.
        val c = Offset(frame.left + frame.width * .50f, frame.top + frame.height * .43f)
        val extent = hypot(size.width, size.height)
        val cyan = Color(0xFF8BF1E6)
        val gold = Color(0xFFE8CC87)
        val strength = if (subdued) .24f else .68f
        val glowRadius = frame.width * (.20f + opened * .65f)
        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = strength * charge * fade), Color.Transparent), c, glowRadius), glowRadius, c)
        repeat(4) { ring ->
            val radius = frame.width * (.12f + ring * .065f)
            drawArc(if (ring % 2 == 0) cyan.copy(alpha = strength * charge * fade) else gold.copy(alpha = strength * charge * fade),
                ring * 23f + p * 35f, 230f, false, c - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(1.5f + ring % 2))
            repeat(12) { rune ->
                val a = rune * PI / 6 + ring * .13
                val at = c + Offset(cos(a).toFloat() * radius, sin(a).toFloat() * radius)
                val tangent = Offset(-sin(a).toFloat(), cos(a).toFloat()) * (frame.width * .008f)
                drawLine(gold.copy(alpha = strength * charge * fade), at - tangent, at + tangent, 2f)
            }
        }
        val gap = frame.width * .24f * opened
        for (side in listOf(-1, 1)) {
            val x = c.x + side * gap
            drawLine(cyan.copy(alpha = charge * fade * (1 - opened)), Offset(x, frame.top + frame.height * .22f),
                Offset(x, frame.top + frame.height * .61f), if (subdued) 2f else 4f)
        }
        if (opening > 0f) {
            repeat(3) { wave ->
                val q = (opening * 1.3f - wave * .16f).coerceIn(0f, 1f)
                drawCircle(cyan.copy(alpha = strength * (1 - q) * fade), extent * q, c, style = Stroke(if (subdued) 1f else 3f))
            }
            if (!subdued) repeat(56) { i ->
                val a = i * 2.39996f
                val d = extent * opened * ((i % 9 + 1) / 12f)
                drawCircle(if (i % 3 == 0) gold.copy(alpha = fade * .6f) else cyan.copy(alpha = fade * .45f),
                    1.3f + i % 3, c + Offset(cos(a) * d, sin(a) * d * .7f))
            }
        }
    }
}
