package com.example.starborn.feature.combat.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.R
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.CombatLogEntry
import com.example.starborn.domain.model.Player
import com.example.starborn.feature.combat.ui.CombatNameFont
import com.example.starborn.feature.combat.viewmodel.CombatFxEvent
import com.example.starborn.feature.combat.viewmodel.CombatViewModel.TimedPromptState
import com.example.starborn.ui.background.rememberAssetPainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

private const val ATTACK_FX_DURATION_MS = 500L

@Composable
fun CombatFxOverlay(
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,
    showKnockout: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    supportFx: List<SupportFxUi> = emptyList(),
    telegraphFx: List<TelegraphFxUi> = emptyList()
) {
    val showSupportHighlight = supportFx.isNotEmpty()
    val showTelegraphHighlight = telegraphFx.isNotEmpty()
    val activeTelegraph = telegraphFx.firstOrNull()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showSupportHighlight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color(0xFF81C784).copy(alpha = 0.16f))
                    .border(BorderStroke(2.dp, Color(0xFF81C784).copy(alpha = 0.65f)), shape)
            )
        }
        if (showTelegraphHighlight && activeTelegraph != null) {
            val isAethel = activeTelegraph.skillName.lowercase(Locale.getDefault()).contains("aethel") ||
                activeTelegraph.skillName.lowercase(Locale.getDefault()).contains("resonance")
            val baseColor = if (isAethel) Color(0xFFBA68C8) else Color(0xFFE57373)
            val borderPulse = rememberInfiniteTransition(label = "telegraph_pulse")
            val alphaPulse by borderPulse.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.82f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "telegraph_alpha"
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(baseColor.copy(alpha = 0.08f))
                    .border(BorderStroke(2.2.dp, baseColor.copy(alpha = alphaPulse)), shape)
            )
        }
        if (showKnockout) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
        damageFx.forEach { fx ->
            ImpactBurst(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(50f)
            )
            DamageNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(100f)
            )
        }
        shieldBreakFx.forEach { fx ->
            ShieldBreakBurst(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(60f)
            )
        }
        statusFx.forEach { fx ->
            key(fx.id) {
                StatusPulse(
                    fx = fx,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).zIndex(80f)
                )
                StatusImpactFlash(
                    fx = fx,
                    modifier = Modifier.matchParentSize().zIndex(35f)
                )
            }
        }
        attackFx.forEach { fx ->
            AttackHitFx(
                fx = fx,
                modifier = Modifier.matchParentSize().zIndex(40f)
            )
        }
        healFx.forEach { fx ->
            HealNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(100f)
            )
        }
    }
}

fun attackFxStyleFor(sourceId: String): AttackFxStyle? = when (sourceId) {
    "nova" -> AttackFxStyle.NOVA_LASER
    "zeke" -> AttackFxStyle.ZEKE_PUNCH
    "orion" -> AttackFxStyle.ORION_JEWEL
    "gh0st", "ghost" -> AttackFxStyle.GHOST_SLASH
    else -> null
}

fun resolveAttackStyle(
    sourceId: String,
    combatState: CombatState?,
    playerIdSet: Set<String>
): AttackFxStyle? {
    val normalized = normalizeAttackSourceId(sourceId)
    val direct = attackFxStyleFor(normalized) ?: attackFxStyleForName(normalized)
    if (direct != null) return direct
    val combatantMatch = combatState?.combatants?.get(sourceId)
        ?: combatState?.combatants?.get(normalized)
        ?: combatState?.combatants?.values?.firstOrNull { state ->
            val id = state.combatant.id.lowercase(Locale.getDefault())
            val name = state.combatant.name.lowercase(Locale.getDefault())
            normalized == id || normalized.contains(id) || normalized.contains(name)
        }
    val fromCombatant = combatantMatch?.combatant?.name?.let { attackFxStyleForName(it) }
        ?: combatantMatch?.combatant?.id?.lowercase(Locale.getDefault())?.let { attackFxStyleFor(it) }
    if (fromCombatant != null) return fromCombatant
    val matchedPlayer = playerIdSet.firstOrNull { playerId ->
        normalized.startsWith(playerId) || normalized.contains(playerId)
    }
    return matchedPlayer?.let { attackFxStyleFor(it) }
}

fun attackFxStyleForName(name: String): AttackFxStyle? {
    val normalized = name.lowercase(Locale.getDefault())
    return when {
        "nova" in normalized -> AttackFxStyle.NOVA_LASER
        "zeke" in normalized -> AttackFxStyle.ZEKE_PUNCH
        "orion" in normalized -> AttackFxStyle.ORION_JEWEL
        "gh0st" in normalized || "ghost" in normalized -> AttackFxStyle.GHOST_SLASH
        else -> null
    }
}

fun attackFxDurationFor(style: AttackFxStyle): Long = when (style) {
    AttackFxStyle.NOVA_LASER -> ATTACK_FX_DURATION_MS
    AttackFxStyle.ZEKE_PUNCH -> ATTACK_FX_DURATION_MS
    AttackFxStyle.ORION_JEWEL -> ATTACK_FX_DURATION_MS
    AttackFxStyle.GHOST_SLASH -> ATTACK_FX_DURATION_MS
}

fun normalizeAttackSourceId(sourceId: String): String {
    val base = sourceId.substringBefore('#')
    val segment = base.substringAfterLast(':').substringAfterLast('/')
    return segment.lowercase(Locale.getDefault()).removePrefix("player_")
}

@Composable
fun AttackHitFx(
    fx: AttackHitFxUi,
    modifier: Modifier = Modifier
) {
    val durationMillis = attackFxDurationFor(fx.style)
    val progress = remember { Animatable(0f) }
    val zekeWords = remember { listOf("BAM!", "WHAM!", "CRACK!", "POW!", "SMASH!") }
    val zekeTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
    }
    val seed = remember(fx.id) { abs(fx.id.hashCode()) }
    val zekeWord = remember(fx.id) { zekeWords[seed % zekeWords.size] }
    LaunchedEffect(fx.id) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
        )
    }
    val t = progress.value
    val fadeStart = 0.7f
    val fadeProgress = ((t - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
    val alpha = 1f - fadeProgress
    val scale = 1f + 0.12f * (1f - t)

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sizeMin = size.minDimension
        val color = when (fx.style) {
            AttackFxStyle.NOVA_LASER -> Color(0xFF7BE4FF)
            AttackFxStyle.ZEKE_PUNCH -> Color(0xFFFFB74D)
            AttackFxStyle.ORION_JEWEL -> Color(0xFFB388FF)
            AttackFxStyle.GHOST_SLASH -> Color(0xFF80D8FF)
        }
        withTransform({
            scale(scale, scale, center)
        }) {
            drawCircle(
                color = color.copy(alpha = 0.16f + 0.08f * (1f - t)),
                radius = sizeMin * 0.48f,
                center = center
            )
            when (fx.style) {
                AttackFxStyle.NOVA_LASER -> {
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
                        size = Size(beamWidth, beamBottom - beamTop),
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
                        size = Size(beamCore, beamBottom - beamTop),
                        cornerRadius = CornerRadius(beamCore, beamCore)
                    )

                    val flareRadius = sizeMin * (0.16f + 0.26f * t)
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
                        style = Stroke(width = ringWidth)
                    )

                    val crossAlpha = (1f - t).coerceIn(0f, 1f)
                    val crossLength = sizeMin * (0.22f + 0.12f * sin(t * PI.toFloat()))
                    val crossWidth = sizeMin * 0.018f
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
                        strokeWidth = crossWidth * 0.75f,
                        cap = StrokeCap.Round
                    )

                    val sparkAlpha = (1f - t) * 0.6f
                    repeat(14) { index ->
                        val angle = (index * 26 + seed % 360) * (PI.toFloat() / 180f)
                        val distance = sizeMin * (0.12f + 0.42f * t)
                        val length = sizeMin * (0.05f + 0.04f * (1f - t))
                        val dir = Offset(cos(angle), sin(angle))
                        val start = center + dir * distance
                        val end = center + dir * (distance + length)
                        drawLine(
                            color = Color.White.copy(alpha = sparkAlpha),
                            start = start,
                            end = end,
                            strokeWidth = sizeMin * 0.012f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                AttackFxStyle.ZEKE_PUNCH -> {
                    val slamIn = (t / 0.22f).coerceIn(0f, 1f)
                    val slamOut = if (t < 0.6f) 1f else (1f - (t - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    val slamAlpha = slamIn * slamOut

                    val impactGlow = (1f - t).coerceIn(0f, 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f * impactGlow),
                                color.copy(alpha = 0.6f * impactGlow),
                                Color.Transparent
                            ),
                            center = center,
                            radius = sizeMin * 0.22f
                        ),
                        radius = sizeMin * 0.22f,
                        center = center
                    )

                    val shock = ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.18f + 0.56f * shock)
                    val ringWidth = sizeMin * (0.05f - 0.02f * shock)
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
                            strokeWidth = sizeMin * 0.02f,
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
                    val bubbleFill = Color(0xFFFFF3E0).copy(alpha = 0.95f * slamAlpha)
                    val bubbleStroke = color.copy(alpha = 0.9f * slamAlpha)
                    withTransform({
                        scale(bubbleScale, bubbleScale, center)
                        rotate(bubbleRotation, center)
                    }) {
                        drawPath(bubblePath, bubbleFill)
                        drawPath(
                            bubblePath,
                            bubbleStroke,
                            style = Stroke(width = sizeMin * 0.02f)
                        )
                    }

                    val baseTextSize = sizeMin * 0.16f * bubbleScale
                    val textColor = Color(0xFF1B1B1B).copy(alpha = slamAlpha)
                    zekeTextPaint.textSize = baseTextSize
                    zekeTextPaint.color = textColor.toArgb()
                    zekeTextPaint.setShadowLayer(
                        sizeMin * 0.02f,
                        0f,
                        sizeMin * 0.01f,
                        color.copy(alpha = 0.5f * slamAlpha).toArgb()
                    )
                    val maxTextWidth = bubbleInner * 2f
                    val measuredWidth = zekeTextPaint.measureText(zekeWord)
                    if (measuredWidth > maxTextWidth) {
                        zekeTextPaint.textSize = baseTextSize * (maxTextWidth / measuredWidth)
                    }
                    val fm = zekeTextPaint.fontMetrics
                    val textY = center.y - (fm.ascent + fm.descent) / 2f
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        nativeCanvas.save()
                        nativeCanvas.rotate(bubbleRotation, center.x, center.y)
                        nativeCanvas.drawText(zekeWord, center.x, textY, zekeTextPaint)
                        nativeCanvas.restore()
                    }
                }
                AttackFxStyle.ORION_JEWEL -> {
                    val pulse = 0.5f + 0.5f * sin(t * PI.toFloat() * 2f)
                    val auraAlpha = (1f - t) * 0.55f
                    val coreColor = lerp(color, Color.White, 0.4f)
                    val accentColor = lerp(color, Color(0xFF7C4DFF), 0.5f)

                    val auraRadius = sizeMin * (0.28f + 0.18f * pulse)
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
                    val ringWidth = sizeMin * (0.022f + 0.01f * (1f - t))
                    val ringRect = Rect(center = center, radius = ringRadius)
                    val segmentSweep = 38f
                    repeat(6) { index ->
                        val start = index * 60f + t * 120f
                        drawArc(
                            color = coreColor.copy(alpha = 0.6f * (1f - t)),
                            startAngle = start,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = ringRect.topLeft,
                            size = ringRect.size,
                            style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                        )
                    }

                    val beamLength = sizeMin * (0.34f + 0.16f * (1f - t))
                    val beamWidth = sizeMin * 0.022f
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
                        drawPath(
                            jewelPath,
                            color.copy(alpha = 0.4f),
                            style = Stroke(width = sizeMin * 0.012f)
                        )
                        drawPath(innerPath, Color.White.copy(alpha = 0.8f * (1f - t)))
                    }

                    val orbitBase = sizeMin * (0.2f + 0.06f * sin(t * PI.toFloat()))
                    repeat(12) { index ->
                        val angle = (index * 30 + seed % 360) * (PI.toFloat() / 180f) + t * 1.8f
                        val wobble = 0.85f + 0.2f * sin(t * PI.toFloat() * 2.6f + index)
                        val pos = center + Offset(cos(angle), sin(angle)) * (orbitBase * wobble)
                        val moteSize = sizeMin * (0.012f + 0.006f * (1f - t))
                        drawCircle(
                            color = accentColor.copy(alpha = 0.6f * (1f - t)),
                            radius = moteSize,
                            center = pos
                        )
                    }

                    val rippleRadius = sizeMin * (0.12f + 0.6f * t)
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f * (1f - t)),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = sizeMin * 0.018f)
                    )
                }
                AttackFxStyle.GHOST_SLASH -> {
                    val slashIn = (t / 0.18f).coerceIn(0f, 1f)
                    val slashOut = if (t < 0.55f) 1f else (1f - (t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    val slashAlpha = slashIn * slashOut
                    val angleJitter = ((seed % 9) - 4) * 2.2f
                    val angleDeg = -35f + angleJitter
                    val theta = angleDeg * (PI.toFloat() / 180f)
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

                    val outerWidth = sizeMin * (0.16f - 0.04f * t)
                    val coreWidth = sizeMin * (0.08f - 0.02f * t)
                    val highlightWidth = sizeMin * 0.024f
                    drawPath(
                        path = slashPath,
                        brush = trailBrush,
                        style = Stroke(width = outerWidth, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = slashPath,
                        brush = coreBrush,
                        style = Stroke(width = coreWidth, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = slashPath,
                        color = Color.White.copy(alpha = 0.75f * slashAlpha),
                        style = Stroke(width = highlightWidth, cap = StrokeCap.Round)
                    )

                    val afterOffset = normal * sizeMin * 0.08f * (1f - t)
                    val afterPath = Path().apply {
                        moveTo(start.x + afterOffset.x, start.y + afterOffset.y)
                        quadraticTo(
                            control.x + afterOffset.x,
                            control.y + afterOffset.y,
                            end.x + afterOffset.x,
                            end.y + afterOffset.y
                        )
                    }
                    drawPath(
                        path = afterPath,
                        color = color.copy(alpha = 0.2f * slashAlpha),
                        style = Stroke(width = sizeMin * 0.06f, cap = StrokeCap.Round)
                    )

                    val speedAlpha = (1f - t) * 0.4f
                    repeat(6) { index ->
                        val offset = (index - 2.5f) * sizeMin * 0.035f
                        val lineStart = center - dir * sizeMin * (0.22f + 0.06f * index) + normal * offset
                        val lineEnd = lineStart + dir * sizeMin * 0.12f
                        drawLine(
                            color = color.copy(alpha = speedAlpha),
                            start = lineStart,
                            end = lineEnd,
                            strokeWidth = sizeMin * 0.012f,
                            cap = StrokeCap.Round
                        )
                    }

                    val sparkBurst = ((t - 0.08f) / 0.35f).coerceIn(0f, 1f)
                    repeat(10) { index ->
                        val spread = -0.6f + index * (1.2f / 9f)
                        val sparkAngle = theta + spread
                        val sparkDir = Offset(cos(sparkAngle), sin(sparkAngle))
                        val sparkLen = sizeMin * (0.04f + 0.07f * (1f - t))
                        val sparkStart = end + sparkDir * sizeMin * 0.02f
                        val sparkEnd = sparkStart + sparkDir * sparkLen
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f * sparkBurst),
                            start = sparkStart,
                            end = sparkEnd,
                            strokeWidth = sizeMin * 0.008f,
                            cap = StrokeCap.Round
                        )
                    }

                    val glintAlpha = (1f - t) * slashAlpha
                    val glintSize = sizeMin * 0.06f
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f * glintAlpha),
                        start = end - normal * glintSize * 0.5f,
                        end = end + normal * glintSize * 0.5f,
                        strokeWidth = sizeMin * 0.012f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f * glintAlpha),
                        start = end - dir * glintSize * 0.35f,
                        end = end + dir * glintSize * 0.35f,
                        strokeWidth = sizeMin * 0.008f,
                        cap = StrokeCap.Round
                    )

                    val mistAlpha = (1f - t) * 0.25f
                    val mist = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f * mistAlpha),
                            color.copy(alpha = 0.12f * mistAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = sizeMin * 0.4f
                    )
                    drawCircle(brush = mist, radius = sizeMin * 0.4f, center = center)
                }
            }
        }
    }
}

@Composable
fun ImpactBurst(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (fx.critical) 280 else 220,
                easing = FastOutSlowInEasing
            )
        )
    }
    val t = anim.value
    val alpha = (1f - t).coerceIn(0f, 1f)
    val ringScale = if (fx.critical) 1.15f else 1f
    val (topColor, _) = damageNumberColors(fx.element, fx.critical, isHealing = false)
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val minSize = size.minDimension
        if (minSize <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = minSize * 0.16f
        val radius = baseRadius * (1f + 0.55f * t) * ringScale
        val stroke = (minSize * 0.02f).coerceAtLeast(2f)
        drawCircle(
            color = topColor.copy(alpha = 0.75f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        val shardLength = minSize * 0.08f * ringScale
        val shardStroke = stroke * 0.6f
        val angleOffset = t * 0.9f
        repeat(4) { index ->
            val angle = angleOffset + index * (PI.toFloat() / 2f)
            val dir = Offset(
                cos(angle.toDouble()).toFloat(),
                sin(angle.toDouble()).toFloat()
            )
            val start = center + dir * radius * 0.55f
            val end = center + dir * (radius * 0.55f + shardLength)
            drawLine(
                color = topColor.copy(alpha = 0.55f * alpha),
                start = start,
                end = end,
                strokeWidth = shardStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ShieldBreakBurst(
    fx: ShieldBreakFxUi,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        )
    }
    val t = anim.value.coerceIn(0f, 1f)
    val alpha = (1f - t).coerceIn(0f, 1f)
    val shieldColor = Color(0xFF42A5F5)
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val minSize = size.minDimension
        if (minSize <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minSize * (0.18f + 0.42f * t)
        val stroke = (minSize * 0.018f).coerceAtLeast(2f)

        drawCircle(
            color = shieldColor.copy(alpha = 0.65f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        val arcSize = Size(radius * 2f, radius * 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        drawArc(
            color = Color.White.copy(alpha = 0.35f * alpha),
            startAngle = 360f * t,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = stroke * 0.75f, cap = StrokeCap.Round)
        )

        val shardCount = 7
        repeat(shardCount) { index ->
            val angle = (index / shardCount.toFloat()) * (PI.toFloat() * 2f) + t * 1.1f
            val dir = Offset(cos(angle.toDouble()).toFloat(), sin(angle.toDouble()).toFloat())
            val start = center + dir * radius * 0.62f
            val end = center + dir * radius * (0.92f + 0.18f * t)
            drawLine(
                color = shieldColor.copy(alpha = 0.55f * alpha),
                start = start,
                end = end,
                strokeWidth = stroke * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun DamageNumberBubble(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val normalizedElement = fx.element?.trim()?.lowercase(Locale.getDefault())
    val isMiss = normalizedElement == "miss" && fx.amount == 0
    val isBlocked = normalizedElement == "blocked" && fx.amount == 0
    val isHealing = fx.amount < 0
    val displayAmount = abs(fx.amount)
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(1f) }
    val driftX = remember(fx.id, isMiss, isBlocked) { (Random.nextFloat() - 0.5f) * if (isMiss || isBlocked) 22f else 48f }
    val tilt = remember(fx.id, isHealing, isMiss, isBlocked) {
        when {
            isHealing -> 0f
            isMiss -> 0f
            isBlocked -> 0f
            else -> (Random.nextFloat() - 0.5f) * if (fx.critical) 18f else 10f
        }
    }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        val initialScale = when {
            isMiss -> 1.12f
            isBlocked -> 1.12f
            isHealing -> 1.05f
            fx.critical -> 1.25f
            else -> 1.1f
        }
        scaleAnim.snapTo(initialScale)
        launch {
            verticalOffset.animateTo(
                targetValue = when {
                    isMiss -> -64f
                    isBlocked -> -64f
                    isHealing -> -60f
                    fx.critical -> -96f
                    else -> -72f
                },
                animationSpec = tween(durationMillis = 680, easing = LinearEasing)
            )
        }
        launch {
            delay(350)
            alphaAnim.animateTo(0f, tween(durationMillis = 400, easing = LinearEasing))
        }
        launch {
            scaleAnim.animateTo(1f, tween(durationMillis = 420, easing = EaseOutBack))
        }
    }
    val headline = when {
        isMiss -> "MISS!"
        isBlocked -> "BLOCKED"
        isHealing -> "+$displayAmount"
        fx.critical -> "$displayAmount"
        else -> "$displayAmount"
    }
    val (topColor, bottomColor) = damageNumberColors(fx.element, fx.critical, isHealing)
    val bubbleModifier = modifier.graphicsLayer {
        translationY = verticalOffset.value
        translationX = driftX
        alpha = alphaAnim.value
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
        rotationZ = tilt
    }
    Box(
        modifier = bubbleModifier,
        contentAlignment = Alignment.Center
    ) {
        if (isBlocked) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = bottomColor.copy(alpha = 0.55f),
                modifier = Modifier.size(46.dp)
            )
            Canvas(modifier = Modifier.size(46.dp)) {
                val w = size.width
                val h = size.height
                val crackColor = Color.White.copy(alpha = 0.62f)
                val stroke = (size.minDimension * 0.06f).coerceAtLeast(2f)
                drawLine(
                    color = crackColor,
                    start = Offset(w * 0.58f, h * 0.22f),
                    end = Offset(w * 0.46f, h * 0.52f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crackColor,
                    start = Offset(w * 0.46f, h * 0.52f),
                    end = Offset(w * 0.62f, h * 0.78f),
                    strokeWidth = stroke * 0.8f,
                    cap = StrokeCap.Round
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center
        ) {
            val outlineColor = Color.Black.copy(alpha = 0.8f)
            val outlineOffset = 1.5.dp
            val textStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = if (fx.critical) 28.sp else 24.sp,
                fontFamily = CombatNameFont,
                letterSpacing = if (isMiss || isBlocked) 0.85.sp else 0.sp,
                fontStyle = if (isHealing) FontStyle.Italic else FontStyle.Normal,
                fontWeight = FontWeight.Black
            )
            val textModifier = if (isBlocked) {
                Modifier.requiredWidth(112.dp)
            } else {
                Modifier
            }

            // Outline layers
            listOf(
                Offset(-1f, -1f), Offset(1f, -1f),
                Offset(-1f, 1f), Offset(1f, 1f),
                Offset(0f, -1.2f), Offset(0f, 1.2f),
                Offset(-1.2f, 0f), Offset(1.2f, 0f)
            ).forEach { offset ->
                Text(
                    text = headline,
                    style = textStyle,
                    color = outlineColor,
                    modifier = textModifier.offset(
                        x = (offset.x * outlineOffset.value).dp,
                        y = (offset.y * outlineOffset.value).dp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            // Main Text
            Text(
                text = headline,
                style = textStyle.copy(
                    shadow = Shadow(
                        color = bottomColor.copy(alpha = 0.9f),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                ),
                color = topColor,
                modifier = textModifier,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun damageNumberColors(
    element: String?,
    critical: Boolean,
    isHealing: Boolean
): Pair<Color, Color> {
    if (isHealing) {
        val base = Color(0xFF81C784)
        val highlight = if (critical) 0.45f else 0.3f
        val shadow = if (critical) 0.18f else 0.12f
        val top = lerp(base, Color.White, highlight)
        val bottom = lerp(base, Color.Black, shadow)
        return top to bottom
    }
    val normalized = element?.trim()?.lowercase(Locale.getDefault())
    val base = when (normalized) {
        "burn", "fire" -> Color(0xFFFF7043)
        "freeze", "ice" -> Color(0xFF90CAF9)
        "shock", "lightning" -> Color(0xFF42A5F5)
        "acid", "poison" -> Color(0xFF81C784)
        "source", "harmonic", "psychic", "psionic", "void" -> Color(0xFFBA68C8)
        "physical" -> null
        "miss" -> Color(0xFFB0BEC5)
        "blocked" -> Color(0xFF90A4AE)
        else -> null
    }
    if (base == null) {
        val top = if (critical) Color(0xFFFFF59D) else Color(0xFFFFB74D)
        val bottom = if (critical) Color(0xFFFFD740) else Color(0xFFFF7043)
        return top to bottom
    }
    val highlight = if (critical) 0.35f else 0.2f
    val shadow = if (critical) 0.22f else 0.12f
    val top = lerp(base, Color.White, highlight)
    val bottom = lerp(base, Color.Black, shadow)
    return top to bottom
}

@Composable
fun HealNumberBubble(
    fx: HealFxUi,
    modifier: Modifier = Modifier
) {
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        launch {
            verticalOffset.animateTo(-52f, tween(durationMillis = 560, easing = LinearEasing))
        }
        launch {
            delay(350)
            alphaAnim.animateTo(0f, tween(durationMillis = 400, easing = LinearEasing))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            alpha = alphaAnim.value
        },
        contentAlignment = Alignment.Center
    ) {
        val text = "+${fx.amount}"
        val textStyle = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            fontFamily = CombatNameFont,
            fontWeight = FontWeight.Black
        )
        val outlineColor = Color.Black.copy(alpha = 0.75f)
        val outlineOffset = 1.2.dp

        // Outline
        listOf(
            Offset(-1f, -1f), Offset(1f, -1f),
            Offset(-1f, 1f), Offset(1f, 1f)
        ).forEach { offset ->
            Text(
                text = text,
                style = textStyle,
                color = outlineColor,
                modifier = Modifier.offset(
                    x = (offset.x * outlineOffset.value).dp,
                    y = (offset.y * outlineOffset.value).dp
                )
            )
        }

        Text(
            text = text,
            style = textStyle,
            color = Color(0xFF81C784)
        )
    }
}

@Composable
fun StatusPulse(
    fx: StatusFxUi,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(fx.id) {
        alphaAnim.snapTo(1f)
        alphaAnim.animateTo(0f, tween(durationMillis = 600, easing = LinearEasing))
    }
    Surface(
        modifier = modifier.graphicsLayer { alpha = alphaAnim.value },
        color = Color(0xFF4DD0E1).copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = fx.statusId.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusImpactFlash(
    fx: StatusFxUi,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 760, easing = FastOutSlowInEasing))
    }
    val normalized = fx.statusId.lowercase(Locale.getDefault())
    val isBlind = normalized == "blind"
    val statusColor = if (isBlind) Color(0xFFE9F5FF) else colorForStatus(fx.statusId)
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val t = progress.value.coerceIn(0f, 1f)
        val fade = 1f - t
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        val radius = size.minDimension * (0.18f + 0.72f * t)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isBlind) 0.62f * fade else 0.28f * fade),
                    statusColor.copy(alpha = if (isBlind) 0.34f * fade else 0.26f * fade),
                    Color.Transparent
                ),
                center = center,
                radius = radius.coerceAtLeast(1f)
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = statusColor.copy(alpha = 0.44f * fade),
            radius = size.minDimension * (0.28f + 0.44f * t),
            center = center,
            style = Stroke(width = size.minDimension * 0.035f)
        )
        if (isBlind) {
            val sweepY = size.height * (0.28f + 0.32f * t)
            drawLine(
                color = Color.White.copy(alpha = 0.62f * fade),
                start = Offset(size.width * 0.12f, sweepY),
                end = Offset(size.width * 0.88f, sweepY),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OutcomeOverlay(
    outcomeType: CombatFxEvent.CombatOutcomeFx.OutcomeType,
    party: List<Player>
) {
    val isVictory = outcomeType == CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY
    val isRetreat = outcomeType == CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT
    val accentColor = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> Color(0xFFFF922B)
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> Color(0xFFE65D5D)
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> Color(0xFF7CD8FF)
    }
    val eyebrow = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Combat Result"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "Party Status"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Tactical Exit"
    }
    val title = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Victory"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "Defeated"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Retreat"
    }
    val subtitle = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Hostile contact resolved"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "The party collapses"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Disengaged from combat"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isVictory) 0.62f else 0.52f))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF15100D).copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.72f)),
            shadowElevation = 18.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = CombatNameFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = if (isVictory) Icons.Filled.EmojiEvents else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.2.dp)
                        .background(accentColor.copy(alpha = 0.44f))
                )
                if (!isRetreat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        party.take(4).forEach { player ->
                            val emotePath = when (outcomeType) {
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY ->
                                    "images/characters/emotes/${player.id}_cool.png"
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT ->
                                    "images/characters/emotes/${player.id}_down.png"
                                else -> null
                            }
                            if (emotePath != null) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Black.copy(alpha = 0.32f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.36f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (isVictory) 96.dp else 82.dp)
                                ) {
                                    Box {
                                        Image(
                                            painter = rememberAssetPainter(emotePath, painterResource(R.drawable.main_menu_background)),
                                            contentDescription = player.name,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .graphicsLayer {
                                                    scaleX = if (isVictory) 1.18f else 1f
                                                    scaleY = if (isVictory) 1.18f else 1f
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TimedPromptOverlay(
    prompt: TimedPromptState,
    onTap: () -> Unit
) {
    var progress by remember(prompt.id) { mutableStateOf(1f) }
    LaunchedEffect(prompt.id) {
        val duration = prompt.durationMillis.coerceAtLeast(1L).toFloat()
        val start = prompt.startedAt
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - start
            val fraction = (elapsed / duration).coerceIn(0f, 1f)
            progress = 1f - fraction
            if (fraction >= 1f) break
            delay(16)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(prompt.id) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = prompt.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
                Text(
                    text = "Tap!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
    }
}
