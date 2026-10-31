package com.example.starborn.ui.exploration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import kotlin.math.*

/** Resonance wakes the glyphs, separates the stone, then washes out across the whole scene. */
@Composable
fun TempleGateReveal(progress: Float, subdued: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().testTag("temple-gate-reveal")) {
        val p = progress.coerceIn(0f, 1f)
        val opening = ((p - .28f) / .58f).coerceIn(0f, 1f)
        val fade = ((1f - p) / .22f).coerceIn(0f, 1f)
        val charge = (p / .28f).coerceIn(0f, 1f)
        val c = Offset(size.width / 2, size.height * .51f)
        val extent = hypot(size.width, size.height)
        val cyan = Color(0xFF8BF1E6)
        val gold = Color(0xFFE8CC87)
        val strength = if (subdued) .24f else .72f
        val gap = size.width * .54f * opening
        val stone = Brush.horizontalGradient(listOf(Color(0xFF12262A), Color(0xFF293B3D), Color(0xFF12262A)))
        drawRect(stone, Offset(-gap, 0f), Size(size.width / 2, size.height), alpha = .9f * fade)
        drawRect(stone, Offset(size.width / 2 + gap, 0f), Size(size.width / 2, size.height), alpha = .9f * fade)
        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = strength * charge * fade), Color.Transparent), c,
            size.width * (.18f + opening * .8f)), radius = size.width * (.18f + opening * .8f), center = c)
        for (side in listOf(-1, 1)) {
            val x = c.x + side * gap
            drawLine(cyan.copy(alpha = charge * fade), Offset(x, 0f), Offset(x, size.height),
                if (subdued) 2f else 5f)
            repeat(5) { ring ->
                val radius = size.width * (.12f + ring * .07f)
                val center = c + Offset(side * gap, 0f)
                drawArc(if (ring % 2 == 0) cyan.copy(alpha = strength * charge * fade) else gold.copy(alpha = strength * charge * fade),
                    ring * 23f + p * 35f * side, 230f, false,
                    center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(1.5f + (ring % 2)))
                repeat(12) { rune ->
                    val a = rune * PI / 6 + ring * .13
                    val at = center + Offset(cos(a).toFloat() * radius, sin(a).toFloat() * radius)
                    val tangent = Offset(-sin(a).toFloat(), cos(a).toFloat()) * (7f + ring)
                    drawLine(gold.copy(alpha = strength * charge * fade), at - tangent, at + tangent, 2f)
                }
            }
        }
        if (opening > 0f) {
            repeat(3) { wave ->
                val q = ((opening * 1.3f - wave * .16f).coerceIn(0f, 1f))
                drawCircle(cyan.copy(alpha = strength * (1 - q) * fade), radius = extent * q, center = c,
                    style = Stroke(if (subdued) 1f else 3f))
            }
            if (!subdued) repeat(56) { i ->
                val a = i * 2.39996f
                val d = extent * opening * ((i % 9 + 1) / 12f)
                val point = c + Offset(cos(a) * d, sin(a) * d * .7f)
                drawCircle(if (i % 3 == 0) gold.copy(alpha = fade * .6f) else cyan.copy(alpha = fade * .45f),
                    1.3f + i % 3, point)
            }
        }
    }
}
