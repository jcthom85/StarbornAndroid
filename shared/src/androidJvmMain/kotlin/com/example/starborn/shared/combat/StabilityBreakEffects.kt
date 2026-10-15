package com.example.starborn.shared.combat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/** Mobile-authored Break effects, shared by Android and desktop. */
@Composable
fun StabilityBrokenField(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB388FF),
    flashesDisabled: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "broken_field")
    val animatedPulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "broken_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "broken_sweep"
    )
    val pulse = if (flashesDisabled) 0.15f else animatedPulse
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val maskBlend = BlendMode.SrcAtop
        val intensity = 0.6f + 0.4f * pulse
        val center = Offset(size.width * 0.5f, size.height * 0.6f)
        val radius = size.maxDimension * 0.95f
        val base = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.16f * intensity),
                color.copy(alpha = 0.08f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = base, blendMode = maskBlend)

        val bandWidth = size.minDimension * 0.45f
        val looped = (sin(sweep) + 1f) * 0.5f
        val x = (size.width + bandWidth) * looped - bandWidth
        val fissure = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.22f + 0.18f * pulse),
                Color.Transparent
            ),
            start = Offset(x - bandWidth, 0f),
            end = Offset(x + bandWidth, size.height)
        )
        drawRect(brush = fissure, blendMode = maskBlend)

        val crackStroke = size.minDimension * 0.024f
        val crackColor = Color(0xFFF3E5F5).copy(alpha = 0.48f + 0.28f * pulse)
        val crackHighlight = Color.White.copy(alpha = 0.28f + 0.2f * pulse)

        val shardCenter = Offset(size.width * 0.46f, size.height * 0.44f)
        val mainStart = Offset(size.width * 0.12f, size.height * 0.08f)
        val mainEnd = Offset(size.width * 0.9f, size.height * 0.86f)
        drawLine(color = crackColor, start = mainStart, end = shardCenter, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackColor, start = shardCenter, end = mainEnd, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = mainStart, end = shardCenter, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = shardCenter, end = mainEnd, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)

        val branchStroke = crackStroke * 0.7f
        val branches = listOf(
            shardCenter to shardCenter + Offset(size.width * 0.26f, size.height * -0.14f),
            shardCenter to shardCenter + Offset(size.width * -0.18f, size.height * 0.26f),
            shardCenter to shardCenter + Offset(size.width * 0.12f, size.height * 0.3f)
        )
        branches.forEach { (start, end) ->
            drawLine(color = crackColor, start = start, end = end, strokeWidth = branchStroke, blendMode = maskBlend)
            drawLine(color = crackHighlight, start = start, end = end, strokeWidth = branchStroke * 0.55f, blendMode = maskBlend)
        }

        val burstRadius = size.minDimension * 0.08f
        val burstCount = 7
        repeat(burstCount) { idx ->
            val angle = (idx / burstCount.toFloat()) * 360f + 12f
            val length = burstRadius * (0.7f + 0.5f * (idx % 2))
            val rad = Math.toRadians(angle.toDouble())
            val end = Offset(
                shardCenter.x + (cos(rad) * length).toFloat(),
                shardCenter.y + (sin(rad) * length).toFloat()
            )
            drawLine(color = crackHighlight, start = shardCenter, end = end, strokeWidth = crackStroke * 0.45f, blendMode = maskBlend)
        }

        val glintRadius = size.minDimension * 0.18f
        val glintBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.12f + 0.08f * pulse),
                Color.Transparent,
                Color.Transparent
            ),
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            radius = glintRadius
        )
        drawCircle(
            brush = glintBrush,
            radius = glintRadius,
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            blendMode = maskBlend
        )

        // Orbiting dizzy / dazed stars around the top of the combatant
        val haloCenter = Offset(size.width * 0.5f, size.height * 0.16f)
        val orbitRadiusX = size.width * 0.32f
        val orbitRadiusY = size.height * 0.08f
        val starCount = 3
        repeat(starCount) { i ->
            val starAngle = sweep + (i * 2f * Math.PI.toFloat() / starCount)
            val starX = haloCenter.x + cos(starAngle) * orbitRadiusX
            val starY = haloCenter.y + sin(starAngle) * orbitRadiusY
            val starSize = size.minDimension * 0.038f
            val starAlpha = if (flashesDisabled) 0.5f else (0.5f + 0.5f * sin(starAngle)).coerceIn(0.2f, 1f)
            val starColor = Color(0xFFFFD54F).copy(alpha = starAlpha)
            // Draw 4-point spark
            drawLine(
                color = starColor,
                start = Offset(starX - starSize, starY),
                end = Offset(starX + starSize, starY),
                strokeWidth = crackStroke * 0.6f
            )
            drawLine(
                color = starColor,
                start = Offset(starX, starY - starSize),
                end = Offset(starX, starY + starSize),
                strokeWidth = crackStroke * 0.6f
            )
            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = starSize * 0.35f,
                center = Offset(starX, starY)
            )
        }
    }
}

@Composable
fun StabilityBreakBurst(
    eventKey: Any,
    modifier: Modifier = Modifier,
    flashesDisabled: Boolean = false
) {
    val anim = remember(eventKey) { Animatable(0f) }
    LaunchedEffect(eventKey) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
    }
    val t = anim.value.coerceIn(0f, 1f)
    val alpha = (1f - t * t).coerceIn(0f, 1f) * if (flashesDisabled) 0.25f else 1f
    val shieldColor = Color(0xFF00E5FF)
    val coreColor = Color(0xFFE0F7FA)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize().graphicsLayer { this.alpha = alpha }) {
            val minSize = size.minDimension
            if (minSize <= 0f) return@Canvas
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Central Shockwave
            val shockwaveRadius = minSize * (0.15f + 0.65f * t)
            drawCircle(
                color = shieldColor.copy(alpha = 0.8f * (1f - t)),
                radius = shockwaveRadius,
                center = center,
                style = Stroke(width = (minSize * 0.035f * (1f - t * 0.5f)).coerceAtLeast(3f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f * (1f - t * 1.2f).coerceAtLeast(0f)),
                radius = shockwaveRadius * 0.85f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Polygonal Shattered Shards
            val shardCount = 12
            repeat(shardCount) { index ->
                val baseAngle = (index / shardCount.toFloat()) * (PI.toFloat() * 2f)
                val wobble = sin(index.toFloat() * 3.7f) * 0.35f
                val angle = baseAngle + wobble
                val dir = Offset(cos(angle), sin(angle))
                val flightDist = minSize * (0.25f + 0.55f * t)
                val shardCenter = center + dir * flightDist
                val shardSize = minSize * (0.045f + 0.02f * sin(index.toFloat())) * (1f - t * 0.6f)
                val rot = (index * 45f + t * 360f) * (PI.toFloat() / 180f)

                val p1 = shardCenter + Offset(cos(rot), sin(rot)) * shardSize
                val p2 = shardCenter + Offset(cos(rot + 2.1f), sin(rot + 2.1f)) * (shardSize * 0.8f)
                val p3 = shardCenter + Offset(cos(rot + 4.2f), sin(rot + 4.2f)) * (shardSize * 0.6f)

                val shardPath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(shardPath, color = coreColor.copy(alpha = 0.9f * alpha))
                drawPath(shardPath, color = shieldColor.copy(alpha = alpha), style = Stroke(width = 1.5.dp.toPx()))
            }
        }

        // 3. Floating "BREAK!" badge
        val badgeScale = (1f + 0.4f * sin(t * PI.toFloat())).coerceAtLeast(0.8f)
        val badgeOffset = (-36).dp * (t * 0.8f)
        Text(
            text = "SHIELD BREAK!",
            color = Color(0xFF00E5FF).copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .offset(y = badgeOffset)
                .graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                }
        )
    }
}
