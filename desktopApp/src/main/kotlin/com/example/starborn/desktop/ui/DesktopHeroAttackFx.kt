package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.combat.CombatState
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal enum class DesktopAttackFxStyle {
    NOVA_LASER,
    ZEKE_PUNCH,
    ORION_JEWEL,
    GHOST_SLASH,
    OLLIE_TECH
}

internal fun desktopAttackFxStyleForId(sourceId: String): DesktopAttackFxStyle? = when (sourceId) {
    "nova" -> DesktopAttackFxStyle.NOVA_LASER
    "zeke" -> DesktopAttackFxStyle.ZEKE_PUNCH
    "orion" -> DesktopAttackFxStyle.ORION_JEWEL
    "gh0st", "ghost" -> DesktopAttackFxStyle.GHOST_SLASH
    "ollie" -> DesktopAttackFxStyle.OLLIE_TECH
    else -> null
}

internal fun desktopAttackFxStyleForName(name: String): DesktopAttackFxStyle? {
    val normalized = name.lowercase(Locale.ROOT)
    return when {
        "nova" in normalized -> DesktopAttackFxStyle.NOVA_LASER
        "zeke" in normalized -> DesktopAttackFxStyle.ZEKE_PUNCH
        "orion" in normalized -> DesktopAttackFxStyle.ORION_JEWEL
        "gh0st" in normalized || "ghost" in normalized -> DesktopAttackFxStyle.GHOST_SLASH
        "ollie" in normalized -> DesktopAttackFxStyle.OLLIE_TECH
        else -> null
    }
}

internal fun resolveDesktopAttackFxStyle(
    sourceId: String?,
    combatState: CombatState?,
    playerIdSet: Set<String>
): DesktopAttackFxStyle? {
    if (sourceId.isNullOrBlank()) return null
    val base = sourceId.substringBefore('#').substringAfterLast(':').substringAfterLast('/')
    val normalized = base.lowercase(Locale.ROOT).removePrefix("player_")
    val direct = desktopAttackFxStyleForId(normalized) ?: desktopAttackFxStyleForName(normalized)
    if (direct != null) return direct
    val combatantMatch = combatState?.combatants?.get(sourceId)
        ?: combatState?.combatants?.get(normalized)
        ?: combatState?.combatants?.values?.firstOrNull { state ->
            val id = state.combatant.id.lowercase(Locale.ROOT)
            val name = state.combatant.name.lowercase(Locale.ROOT)
            normalized == id || normalized.contains(id) || normalized.contains(name)
        }
    val fromCombatant = combatantMatch?.combatant?.name?.let(::desktopAttackFxStyleForName)
        ?: combatantMatch?.combatant?.id?.lowercase(Locale.ROOT)?.let(::desktopAttackFxStyleForId)
    if (fromCombatant != null) return fromCombatant
    val matchedPlayer = playerIdSet.firstOrNull { playerId ->
        normalized.startsWith(playerId) || normalized.contains(playerId)
    }
    return matchedPlayer?.let(::desktopAttackFxStyleForId)
}

@Composable
internal fun DesktopHeroSignatureAttackFx(
    token: Long,
    style: DesktopAttackFxStyle,
    critical: Boolean,
    flashesDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember(token) { Animatable(0f) }
    val zekeWords = remember { listOf("BAM!", "WHAM!", "CRACK!", "POW!", "SMASH!") }
    val seed = remember(token) { abs(token.hashCode()) }
    val zekeWord = remember(token) { zekeWords[seed % zekeWords.size] }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(token) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    val t = progress.value
    val fadeStart = 0.7f
    val fadeProgress = ((t - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
    val flashScale = if (flashesDisabled) 0.42f else 1f
    val alpha = (1f - fadeProgress) * flashScale
    val animScale = 1f + 0.12f * (1f - t)

    Canvas(
        modifier = modifier
            .testTag("combat-hero-fx-${style.name.lowercase(Locale.ROOT)}")
            .graphicsLayer { this.alpha = alpha }
    ) {
        val sizeMin = size.minDimension
        if (sizeMin <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val color = when (style) {
            DesktopAttackFxStyle.NOVA_LASER -> Color(0xFF7BE4FF)
            DesktopAttackFxStyle.ZEKE_PUNCH -> Color(0xFFFFB74D)
            DesktopAttackFxStyle.ORION_JEWEL -> Color(0xFFB388FF)
            DesktopAttackFxStyle.GHOST_SLASH -> Color(0xFF80D8FF)
            DesktopAttackFxStyle.OLLIE_TECH -> Color(0xFFFFB74D)
        }
        withTransform({
            scale(animScale, animScale, center)
        }) {
            drawCircle(
                color = color.copy(alpha = 0.16f + 0.08f * (1f - t)),
                radius = sizeMin * 0.48f,
                center = center
            )
            when (style) {
                DesktopAttackFxStyle.NOVA_LASER -> {
                    val beamRamp = (t / 0.18f).coerceIn(0f, 1f)
                    val beamFade = if (t < 0.65f) 1f else (1f - (t - 0.65f) / 0.35f).coerceIn(0f, 1f)
                    val beamAlpha = beamRamp * beamFade
                    val beamWidth = sizeMin * (0.1f - 0.02f * t)
                    val beamCore = beamWidth * 0.35f
                    val beamHeight = size.height * (0.5f + 0.25f * (1f - t))
                    val beamTop = center.y - beamHeight
                    val beamBottom = center.y + sizeMin * (0.08f + 0.04f * (1f - t))
                    val beamOuter = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = 0.25f * beamAlpha),
                            color.copy(alpha = 0.75f * beamAlpha),
                            Color.Transparent
                        ),
                        startY = beamTop,
                        endY = beamBottom
                    )
                    drawRoundRect(
                        brush = beamOuter,
                        topLeft = Offset(center.x - beamWidth / 2f, beamTop),
                        size = Size(beamWidth, (beamBottom - beamTop).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(beamWidth, beamWidth)
                    )
                    val beamCoreBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.95f * beamAlpha),
                            color.copy(alpha = 0.45f * beamAlpha),
                            Color.Transparent
                        ),
                        startY = beamTop,
                        endY = beamBottom
                    )
                    drawRoundRect(
                        brush = beamCoreBrush,
                        topLeft = Offset(center.x - beamCore / 2f, beamTop),
                        size = Size(beamCore, (beamBottom - beamTop).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(beamCore, beamCore)
                    )
                    val flareRadius = (sizeMin * (0.16f + 0.26f * t)).coerceAtLeast(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f * (1f - t)),
                                color.copy(alpha = 0.65f * (1f - t)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = flareRadius
                        ),
                        radius = flareRadius,
                        center = center
                    )
                    val shock = ((t - 0.08f) / 0.92f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.12f + 0.56f * shock)
                    val ringWidth = sizeMin * (0.035f - 0.015f * shock)
                    drawCircle(
                        color = color.copy(alpha = 0.6f * (1f - shock)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringWidth.coerceAtLeast(1f))
                    )
                    val crossAlpha = (1f - t).coerceIn(0f, 1f)
                    val crossLength = sizeMin * (0.22f + 0.12f * sin(t * PI.toFloat()))
                    val crossWidth = (sizeMin * 0.018f).coerceAtLeast(1f)
                    val lineColor = lerp(color, Color.White, 0.4f)
                    drawLine(
                        color = lineColor.copy(alpha = 0.8f * crossAlpha),
                        start = Offset(center.x - crossLength, center.y),
                        end = Offset(center.x + crossLength, center.y),
                        strokeWidth = crossWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = lineColor.copy(alpha = 0.65f * crossAlpha),
                        start = Offset(center.x, center.y - crossLength),
                        end = Offset(center.x, center.y + crossLength),
                        strokeWidth = (crossWidth * 0.75f).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                    val sparkAlpha = (1f - t) * 0.6f
                    repeat(14) { index ->
                        val angle = (index * 26 + seed % 360) * (PI.toFloat() / 180f)
                        val distance = sizeMin * (0.12f + 0.42f * t)
                        val length = sizeMin * (0.05f + 0.04f * (1f - t))
                        val dir = Offset(cos(angle), sin(angle))
                        drawLine(
                            color = Color.White.copy(alpha = sparkAlpha),
                            start = center + dir * distance,
                            end = center + dir * (distance + length),
                            strokeWidth = (sizeMin * 0.012f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                DesktopAttackFxStyle.ZEKE_PUNCH -> {
                    val slamIn = (t / 0.22f).coerceIn(0f, 1f)
                    val slamOut = if (t < 0.6f) 1f else (1f - (t - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    val slamAlpha = slamIn * slamOut
                    val impactGlow = (1f - t).coerceIn(0f, 1f)
                    val glowRadius = (sizeMin * 0.22f).coerceAtLeast(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f * impactGlow),
                                color.copy(alpha = 0.6f * impactGlow),
                                Color.Transparent
                            ),
                            center = center,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = center
                    )
                    val shock = ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.18f + 0.56f * shock)
                    val ringWidth = (sizeMin * (0.05f - 0.02f * shock)).coerceAtLeast(1f)
                    drawCircle(
                        color = color.copy(alpha = 0.55f * (1f - shock)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringWidth)
                    )
                    val shardAlpha = (1f - shock) * 0.75f
                    repeat(12) { index ->
                        val angle = (index * 27 + seed % 360) * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        val start = center + dir * (sizeMin * (0.12f + 0.32f * shock))
                        val end = start + dir * (sizeMin * (0.08f + 0.06f * (1f - shock)))
                        drawLine(
                            color = Color.White.copy(alpha = shardAlpha),
                            start = start,
                            end = end,
                            strokeWidth = (sizeMin * 0.02f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                    repeat(6) { index ->
                        val angleDeg = (index * 60 + seed % 45).toFloat()
                        val angle = angleDeg * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        val tileSize = sizeMin * (0.045f + 0.015f * (1f - t))
                        val dist = sizeMin * (0.16f + 0.36f * shock)
                        val pos = center + dir * dist
                        withTransform({
                            translate(pos.x - tileSize / 2f, pos.y - tileSize / 2f)
                            rotate(angleDeg + 45f, pivot = Offset(tileSize / 2f, tileSize / 2f))
                        }) {
                            drawRoundRect(
                                color = color.copy(alpha = 0.35f * (1f - shock)),
                                topLeft = Offset.Zero,
                                size = Size(tileSize, tileSize),
                                cornerRadius = CornerRadius(tileSize * 0.2f, tileSize * 0.2f)
                            )
                        }
                    }
                    val bubbleScale = 0.85f + 0.22f * sin(t * PI.toFloat())
                    val bubbleRotation = -6f + 12f * sin(t * PI.toFloat() * 1.2f)
                    val bubbleOuter = sizeMin * 0.26f
                    val bubbleInner = bubbleOuter * 0.68f
                    val spikes = 14
                    val bubblePath = Path().apply {
                        for (i in 0 until spikes) {
                            val angle = (i * (2f * PI.toFloat() / spikes)) + t * 0.6f
                            val radius = if (i % 2 == 0) bubbleOuter else bubbleInner
                            val x = center.x + cos(angle) * radius
                            val y = center.y + sin(angle) * radius
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    withTransform({
                        scale(bubbleScale, bubbleScale, center)
                        rotate(bubbleRotation, center)
                    }) {
                        drawPath(bubblePath, Color(0xFFFFF3E0).copy(alpha = 0.95f * slamAlpha))
                        drawPath(
                            bubblePath,
                            color.copy(alpha = 0.9f * slamAlpha),
                            style = Stroke(width = (sizeMin * 0.02f).coerceAtLeast(1f))
                        )
                        val fontSizeSp = ((sizeMin * 0.11f) / density).coerceIn(10f, 22f).sp
                        val measured = textMeasurer.measure(
                            text = zekeWord,
                            style = TextStyle(
                                color = Color(0xFF1B1B1B).copy(alpha = slamAlpha),
                                fontSize = fontSizeSp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                center.x - measured.size.width / 2f,
                                center.y - measured.size.height / 2f
                            )
                        )
                    }
                }

                DesktopAttackFxStyle.ORION_JEWEL -> {
                    val pulse = 0.5f + 0.5f * sin(t * PI.toFloat() * 2f)
                    val auraAlpha = (1f - t) * 0.55f
                    val coreColor = lerp(color, Color.White, 0.4f)
                    val accentColor = lerp(color, Color(0xFF7C4DFF), 0.5f)
                    val auraRadius = (sizeMin * (0.28f + 0.18f * pulse)).coerceAtLeast(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = auraAlpha),
                                color.copy(alpha = 0.18f * auraAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraRadius
                        ),
                        radius = auraRadius,
                        center = center
                    )
                    val ringRadius = sizeMin * (0.18f + 0.48f * t)
                    val ringWidth = (sizeMin * (0.022f + 0.01f * (1f - t))).coerceAtLeast(1f)
                    val ringRect = Rect(center = center, radius = ringRadius)
                    repeat(6) { index ->
                        drawArc(
                            color = coreColor.copy(alpha = 0.6f * (1f - t)),
                            startAngle = index * 60f + t * 120f,
                            sweepAngle = 38f,
                            useCenter = false,
                            topLeft = ringRect.topLeft,
                            size = ringRect.size,
                            style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                        )
                    }
                    val beamLength = sizeMin * (0.34f + 0.16f * (1f - t))
                    val beamWidth = (sizeMin * 0.022f).coerceAtLeast(1f)
                    repeat(4) { index ->
                        val angle = (45f + index * 90f + t * 25f) * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        drawLine(
                            color = coreColor.copy(alpha = 0.7f * (1f - t)),
                            start = center - dir * (beamLength * 0.15f),
                            end = center + dir * beamLength,
                            strokeWidth = beamWidth,
                            cap = StrokeCap.Round
                        )
                    }
                    val jewelSize = sizeMin * (0.12f + 0.02f * pulse)
                    val innerSize = jewelSize * 0.55f
                    val jewelPath = Path().apply {
                        moveTo(center.x, center.y - jewelSize)
                        lineTo(center.x + jewelSize, center.y)
                        lineTo(center.x, center.y + jewelSize)
                        lineTo(center.x - jewelSize, center.y)
                        close()
                    }
                    val innerPath = Path().apply {
                        moveTo(center.x, center.y - innerSize)
                        lineTo(center.x + innerSize, center.y)
                        lineTo(center.x, center.y + innerSize)
                        lineTo(center.x - innerSize, center.y)
                        close()
                    }
                    withTransform({
                        rotate(18f + 140f * t, center)
                    }) {
                        drawPath(jewelPath, coreColor.copy(alpha = 0.85f))
                        drawPath(jewelPath, color.copy(alpha = 0.4f), style = Stroke(width = (sizeMin * 0.012f).coerceAtLeast(1f)))
                        drawPath(innerPath, Color.White.copy(alpha = 0.8f * (1f - t)))
                    }
                    val orbitBase = sizeMin * (0.2f + 0.06f * sin(t * PI.toFloat()))
                    repeat(12) { index ->
                        val angle = (index * 30 + seed % 360) * (PI.toFloat() / 180f) + t * 1.8f
                        val wobble = 0.85f + 0.2f * sin(t * PI.toFloat() * 2.6f + index)
                        val pos = center + Offset(cos(angle), sin(angle)) * (orbitBase * wobble)
                        drawCircle(
                            color = accentColor.copy(alpha = 0.6f * (1f - t)),
                            radius = (sizeMin * (0.012f + 0.006f * (1f - t))).coerceAtLeast(1f),
                            center = pos
                        )
                    }
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f * (1f - t)),
                        radius = sizeMin * (0.12f + 0.6f * t),
                        center = center,
                        style = Stroke(width = (sizeMin * 0.018f).coerceAtLeast(1f))
                    )
                }

                DesktopAttackFxStyle.GHOST_SLASH -> {
                    val slashIn = (t / 0.18f).coerceIn(0f, 1f)
                    val slashOut = if (t < 0.55f) 1f else (1f - (t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    val slashAlpha = slashIn * slashOut
                    val angleJitter = ((seed % 9) - 4) * 2.2f
                    val theta = (-35f + angleJitter) * (PI.toFloat() / 180f)
                    val dir = Offset(cos(theta), sin(theta))
                    val normal = Offset(-dir.y, dir.x)
                    val length = sizeMin * (0.95f + 0.08f * (1f - t))
                    val start = center - dir * length * 0.5f - normal * sizeMin * 0.1f
                    val end = center + dir * length * 0.5f + normal * sizeMin * 0.16f
                    val control = center + normal * sizeMin * 0.28f
                    val slashPath = Path().apply {
                        moveTo(start.x, start.y)
                        quadraticTo(control.x, control.y, end.x, end.y)
                    }
                    val trailBrush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to color.copy(alpha = 0.2f * slashAlpha),
                            1f to color.copy(alpha = 0.6f * slashAlpha)
                        ),
                        start = start,
                        end = end
                    )
                    val coreBrush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = 0.2f * slashAlpha),
                            0.6f to color.copy(alpha = 0.85f * slashAlpha),
                            1f to Color.White.copy(alpha = 0.95f * slashAlpha)
                        ),
                        start = start,
                        end = end
                    )
                    drawPath(slashPath, trailBrush, style = Stroke(width = (sizeMin * (0.16f - 0.04f * t)).coerceAtLeast(1f), cap = StrokeCap.Round))
                    drawPath(slashPath, coreBrush, style = Stroke(width = (sizeMin * (0.08f - 0.02f * t)).coerceAtLeast(1f), cap = StrokeCap.Round))
                    drawPath(slashPath, Color.White.copy(alpha = 0.75f * slashAlpha), style = Stroke(width = (sizeMin * 0.024f).coerceAtLeast(1f), cap = StrokeCap.Round))
                    val afterOffset = normal * sizeMin * 0.08f * (1f - t)
                    val afterPath = Path().apply {
                        moveTo(start.x + afterOffset.x, start.y + afterOffset.y)
                        quadraticTo(control.x + afterOffset.x, control.y + afterOffset.y, end.x + afterOffset.x, end.y + afterOffset.y)
                    }
                    drawPath(afterPath, color.copy(alpha = 0.2f * slashAlpha), style = Stroke(width = (sizeMin * 0.06f).coerceAtLeast(1f), cap = StrokeCap.Round))
                    val speedAlpha = (1f - t) * 0.4f
                    repeat(6) { index ->
                        val offset = (index - 2.5f) * sizeMin * 0.035f
                        val lineStart = center - dir * sizeMin * (0.22f + 0.06f * index) + normal * offset
                        drawLine(
                            color = color.copy(alpha = speedAlpha),
                            start = lineStart,
                            end = lineStart + dir * sizeMin * 0.12f,
                            strokeWidth = (sizeMin * 0.012f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                    val sparkBurst = ((t - 0.08f) / 0.35f).coerceIn(0f, 1f)
                    repeat(10) { index ->
                        val sparkAngle = theta + (-0.6f + index * (1.2f / 9f))
                        val sparkDir = Offset(cos(sparkAngle), sin(sparkAngle))
                        val sparkStart = end + sparkDir * sizeMin * 0.02f
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f * sparkBurst),
                            start = sparkStart,
                            end = sparkStart + sparkDir * (sizeMin * (0.04f + 0.07f * (1f - t))),
                            strokeWidth = (sizeMin * 0.008f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                    val glintAlpha = (1f - t) * slashAlpha
                    val glintSize = sizeMin * 0.06f
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f * glintAlpha),
                        start = end - normal * glintSize * 0.5f,
                        end = end + normal * glintSize * 0.5f,
                        strokeWidth = (sizeMin * 0.012f).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f * glintAlpha),
                        start = end - dir * glintSize * 0.35f,
                        end = end + dir * glintSize * 0.35f,
                        strokeWidth = (sizeMin * 0.008f).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                    val mistRadius = (sizeMin * 0.4f).coerceAtLeast(1f)
                    val mistAlpha = (1f - t) * 0.25f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f * mistAlpha),
                                color.copy(alpha = 0.12f * mistAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = mistRadius
                        ),
                        radius = mistRadius,
                        center = center
                    )
                }

                DesktopAttackFxStyle.OLLIE_TECH -> {
                    val pulseAlpha = (1f - t * 0.65f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.16f + 0.28f * t)
                    drawCircle(
                        color = Color(0xFFFFB74D).copy(alpha = 0.78f * pulseAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (sizeMin * (0.042f * (1f - 0.35f * t))).coerceAtLeast(1f))
                    )
                    drawCircle(
                        color = Color(0xFF4DD0E1).copy(alpha = 0.72f * pulseAlpha),
                        radius = ringRadius * 0.72f,
                        center = center,
                        style = Stroke(width = (sizeMin * 0.025f).coerceAtLeast(1f))
                    )
                    val baseAngle = t * 140f
                    val arcRectSize = Size((ringRadius * 1.75f).coerceAtLeast(1f), (ringRadius * 1.75f).coerceAtLeast(1f))
                    val arcTopLeft = Offset(center.x - arcRectSize.width / 2f, center.y - arcRectSize.height / 2f)
                    for (i in 0 until 4) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.85f * pulseAlpha),
                            startAngle = baseAngle + i * 90f,
                            sweepAngle = 42f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcRectSize,
                            style = Stroke(width = (sizeMin * 0.032f).coerceAtLeast(1f), cap = StrokeCap.Round)
                        )
                    }
                    val sparkCount = if (critical) 10 else 6
                    for (i in 0 until sparkCount) {
                        val angle = ((i.toFloat() / sparkCount) * 360f + 35f * t) * (PI / 180f)
                        val inner = sizeMin * (0.08f + 0.14f * t)
                        val outer = inner + sizeMin * (0.12f * (1f - 0.3f * t))
                        drawLine(
                            color = if (i % 2 == 0) Color(0xFFFFD54F).copy(alpha = pulseAlpha) else Color(0xFF80DEEA).copy(alpha = pulseAlpha),
                            start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                            end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                            strokeWidth = (sizeMin * 0.022f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopElementalImpactBurst(
    token: Long,
    element: String?,
    critical: Boolean,
    flashesDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val anim = remember(token) { Animatable(0f) }
    LaunchedEffect(token) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = if (critical) 280 else 220, easing = FastOutSlowInEasing)
        )
    }
    val t = anim.value
    val alpha = (1f - t).coerceIn(0f, 1f) * if (flashesDisabled) 0.35f else 1f
    val ringScale = if (critical) 1.15f else 1f
    val baseColor = when (element?.trim()?.lowercase(Locale.ROOT)) {
        "burn", "fire" -> Color(0xFFFF7043)
        "freeze", "ice", "cryo", "cold" -> Color(0xFF80DEEA)
        "shock", "lightning", "electric" -> Color(0xFF80D8FF)
        "acid", "poison", "corrosive", "erosion" -> Color(0xFFAED581)
        "source", "harmonic", "psychic", "psionic", "void", "aethel", "resonance" -> Color(0xFFBA68C8)
        else -> if (critical) Color(0xFFFFF59D) else Color(0xFFFFB74D)
    }
    val topColor = lerp(baseColor, Color.White, if (critical) 0.35f else 0.2f)
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val minSize = size.minDimension
        if (minSize <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (minSize * 0.16f) * (1f + 0.55f * t) * ringScale
        val stroke = (minSize * 0.02f).coerceAtLeast(2f)
        drawCircle(
            color = topColor.copy(alpha = 0.75f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        val shardLength = minSize * 0.08f * ringScale
        val angleOffset = t * 0.9f
        repeat(4) { index ->
            val angle = angleOffset + index * (PI.toFloat() / 2f)
            val dir = Offset(cos(angle), sin(angle))
            drawLine(
                color = topColor.copy(alpha = 0.55f * alpha),
                start = center + dir * radius * 0.55f,
                end = center + dir * (radius * 0.55f + shardLength),
                strokeWidth = (stroke * 0.6f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
internal fun DesktopStatusImpactFlash(
    token: Long,
    statusId: String?,
    flashesDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember(token) { Animatable(0f) }
    LaunchedEffect(token) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 760, easing = FastOutSlowInEasing))
    }
    val normalized = statusId?.lowercase(Locale.ROOT).orEmpty()
    val isBlind = normalized == "blind"
    val statusColor = when {
        isBlind -> Color(0xFFE9F5FF)
        "burn" in normalized || "scorch" in normalized -> Color(0xFFFF8A65)
        "freeze" in normalized || "chill" in normalized || "cryo" in normalized -> Color(0xFF80DEEA)
        "shock" in normalized || "stun" in normalized || "jam" in normalized -> Color(0xFFFFE082)
        "acid" in normalized || "corro" in normalized || "poison" in normalized -> Color(0xFFAED581)
        else -> Color(0xFFD3A1FF)
    }
    val flashScale = if (flashesDisabled) 0.35f else 1f
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val t = progress.value.coerceIn(0f, 1f)
        val fade = (1f - t) * flashScale
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        val radius = (size.minDimension * (0.18f + 0.72f * t)).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (if (isBlind) 0.62f else 0.28f) * fade),
                    statusColor.copy(alpha = (if (isBlind) 0.34f else 0.26f) * fade),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = statusColor.copy(alpha = 0.44f * fade),
            radius = size.minDimension * (0.28f + 0.44f * t),
            center = center,
            style = Stroke(width = (size.minDimension * 0.035f).coerceAtLeast(1f))
        )
        if (isBlind) {
            val sweepY = size.height * (0.28f + 0.32f * t)
            drawLine(
                color = Color.White.copy(alpha = 0.62f * fade),
                start = Offset(size.width * 0.12f, sweepY),
                end = Offset(size.width * 0.88f, sweepY),
                strokeWidth = (size.minDimension * 0.035f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
internal fun DesktopTargetedWarningBadge(
    skillName: String,
    flashesDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lower = skillName.lowercase(Locale.ROOT)
    val isAethel = "aethel" in lower || "resonance" in lower || "source" in lower
    val baseColor = if (isAethel) Color(0xFFBA68C8) else Color(0xFFFF5252)
    val pulse = if (flashesDisabled) {
        0.72f
    } else {
        val transition = rememberInfiniteTransition(label = "desktop_telegraph_pulse")
        val anim by transition.animateFloat(
            initialValue = 0.38f,
            targetValue = 0.90f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "telegraph_alpha"
        )
        anim
    }
    Box(modifier = modifier.fillMaxSize().testTag("combat-targeted-badge")) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(baseColor.copy(alpha = 0.08f))
                .border(BorderStroke(2.dp, baseColor.copy(alpha = pulse)), RoundedCornerShape(14.dp))
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1A0606).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, baseColor.copy(alpha = pulse)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
        ) {
            Text(
                text = "⚡ TARGETED",
                color = baseColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun DesktopBattleStageBackdrop(
    accentColor: Color,
    borderColor: Color,
    panelColor: Color,
    highContrastMode: Boolean,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val phase = if (animate) {
        val motion = rememberInfiniteTransition(label = "desktop_battle_stage_backdrop")
        val anim by motion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "battle_stage_phase"
        )
        anim
    } else {
        0.25f
    }
    Canvas(modifier = modifier.fillMaxSize().testTag("combat-stage-backdrop")) {
        if (size.minDimension <= 0f) return@Canvas
        val railAlpha = if (highContrastMode) 0.32f else 0.20f
        val centerY = size.height * 0.54f
        val laneHeight = size.height * 0.16f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    panelColor.copy(alpha = if (highContrastMode) 0.38f else 0.22f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, centerY - laneHeight / 2f),
            size = Size(size.width, laneHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        val pulse = 0.55f + 0.45f * sin(phase * 2f * PI).toFloat()
        drawLine(
            color = accentColor.copy(alpha = 0.16f + 0.10f * pulse),
            start = Offset(size.width * 0.06f, centerY),
            end = Offset(size.width * 0.94f, centerY),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.10f, size.height * 0.22f),
            end = Offset(size.width * 0.90f, size.height * 0.22f),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.10f, size.height * 0.80f),
            end = Offset(size.width * 0.90f, size.height * 0.80f),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val tickCount = 7
        repeat(tickCount) { index ->
            val x = size.width * (0.14f + index * 0.72f / (tickCount - 1).coerceAtLeast(1))
            val alpha = 0.07f + 0.08f * ((phase + index * 0.13f) % 1f)
            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = Offset(x, centerY - laneHeight * 0.26f),
                end = Offset(x, centerY + laneHeight * 0.26f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        val moteCount = 12
        repeat(moteCount) { i ->
            val moteSeed = i * 0.0833f
            val moteProgress = (phase + moteSeed) % 1f
            val moteX = size.width * (0.05f + 0.9f * ((moteSeed * 7.3f) % 1f)) +
                (sin(moteProgress * 2f * PI.toFloat() + i) * 14.dp.toPx())
            val moteY = size.height * (1f - moteProgress)
            val moteAlpha = (sin(moteProgress * PI.toFloat()) * (if (highContrastMode) 0.32f else 0.22f)).coerceIn(0f, 1f)
            val moteRadius = if (i % 3 == 0) 2.2.dp.toPx() else 1.4.dp.toPx()
            drawCircle(
                color = accentColor.copy(alpha = moteAlpha),
                radius = moteRadius,
                center = Offset(moteX, moteY)
            )
        }
    }
}
