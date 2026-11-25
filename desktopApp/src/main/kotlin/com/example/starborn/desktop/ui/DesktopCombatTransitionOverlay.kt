package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.starborn.data.local.Theme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal enum class TransitionMode {
    FULL,
    ENTER,
    EXIT,
    QUICK_CLOSE,
    QUICK_OPEN
}

private fun themeColor(values: List<Float>?, fallback: Color): Color {
    if (values == null || values.size < 3) return fallback
    val alpha = values.getOrNull(3) ?: 1f
    return Color(
        red = values[0].coerceIn(0f, 1f),
        green = values[1].coerceIn(0f, 1f),
        blue = values[2].coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

@Composable
internal fun DesktopCombatTransitionOverlay(
    visible: Boolean,
    theme: Theme? = null,
    suppressFlashes: Boolean = false,
    highContrastMode: Boolean = false,
    mode: TransitionMode = TransitionMode.FULL,
    mainText: String = "HOSTILE CONTACT",
    subText: String = "",
    holdUntilReady: Boolean = false,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val initialValue = when (mode) {
        TransitionMode.EXIT -> 0.72f
        TransitionMode.QUICK_OPEN -> 1f
        else -> 0f
    }
    val progress = remember(mode) { Animatable(initialValue) }

    val defaultAccent = Color(0xFF5CE1FF)
    val defaultBg = Color(0xFF060D18)
    val defaultBorder = Color(0xFF2B7A9B)

    val accentColor = themeColor(theme?.accent, defaultAccent)
    val bgColor = themeColor(theme?.bg, defaultBg)
    val borderColor = themeColor(theme?.border, defaultBorder)
    val wavePath = remember { Path() }
    val headerFont = LocalStarbornFonts.current.orbitron

    LaunchedEffect(visible, mode, holdUntilReady) {
        when (mode) {
            TransitionMode.ENTER -> {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 0.72f,
                    animationSpec = tween(durationMillis = 580, easing = LinearEasing)
                )
                onFinished()
            }
            TransitionMode.EXIT -> {
                progress.snapTo(0.72f)
                if (holdUntilReady) return@LaunchedEffect
                // Let one frame settle so newly composed battle sprites are warm on Skia/GPU.
                withFrameNanos { }
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                onFinished()
            }
            TransitionMode.FULL -> {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 880, easing = LinearEasing)
                )
                onFinished()
            }
            TransitionMode.QUICK_CLOSE -> {
                progress.snapTo(0f)
                val closeDuration = if (mainText.isNotBlank()) 680 else 250
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = closeDuration, easing = FastOutSlowInEasing)
                )
                onFinished()
            }
            TransitionMode.QUICK_OPEN -> {
                progress.snapTo(1f)
                withFrameNanos { }
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 230, easing = LinearOutSlowInEasing)
                )
                onFinished()
            }
        }
    }

    val t = progress.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("desktop-combat-transition")
            .graphicsLayer()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
    ) {
        if (mode == TransitionMode.QUICK_CLOSE || mode == TransitionMode.QUICK_OPEN) {
            val coverAlpha = t.coerceIn(0f, 1f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cy = h * 0.5f
                drawRect(color = bgColor.copy(alpha = coverAlpha))
                val seamAlpha = (sin(coverAlpha * PI.toFloat()) * 0.85f).coerceIn(0f, 1f)
                if (seamAlpha > 0.02f) {
                    val halfSpan = w * (0.18f + 0.32f * sin(coverAlpha * PI.toFloat()))
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = seamAlpha),
                                Color.Transparent
                            ),
                            startX = (w * 0.5f - halfSpan).coerceAtLeast(0f),
                            endX = (w * 0.5f + halfSpan).coerceAtMost(w)
                        ),
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            if (mainText.isNotBlank()) {
                val textAlpha = ((coverAlpha - 0.2f) / 0.35f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .graphicsLayer { alpha = textAlpha }
                ) {
                    Text(
                        text = mainText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Box
        }

        val closeFrac = (t / 0.34f).coerceIn(0f, 1f)
        val closeEase = FastOutSlowInEasing.transform(closeFrac)
        val openFrac = ((t - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val openEase = FastOutSlowInEasing.transform(openFrac)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val halfH = h * 0.5f + 2f

            if (!suppressFlashes && t < 0.34f) {
                val pulseR = lerp(minOf(w, h) * 0.08f, maxOf(w, h) * 0.68f, closeEase)
                val pulseAlpha = (1f - closeFrac) * 0.32f
                drawCircle(
                    color = accentColor.copy(alpha = pulseAlpha),
                    radius = pulseR,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            if (t <= 0.34f) {
                val shutterH = halfH * closeEase
                drawRect(
                    color = bgColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, shutterH)
                )
                drawRect(
                    color = bgColor,
                    topLeft = Offset(0f, h - shutterH),
                    size = Size(w, shutterH)
                )
                val edgeThickness = 2.dp.toPx()
                val glowThickness = 14.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.28f)),
                        startY = (shutterH - glowThickness).coerceAtLeast(0f),
                        endY = shutterH
                    ),
                    topLeft = Offset(0f, (shutterH - glowThickness).coerceAtLeast(0f)),
                    size = Size(w, glowThickness.coerceAtMost(shutterH))
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.9f),
                    start = Offset(0f, shutterH),
                    end = Offset(w, shutterH),
                    strokeWidth = edgeThickness
                )
                val botY = h - shutterH
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
                        startY = botY,
                        endY = (botY + glowThickness).coerceAtMost(h)
                    ),
                    topLeft = Offset(0f, botY),
                    size = Size(w, glowThickness.coerceAtMost(shutterH))
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.9f),
                    start = Offset(0f, botY),
                    end = Offset(w, botY),
                    strokeWidth = edgeThickness
                )
            } else if (t <= 0.72f || mode == TransitionMode.ENTER) {
                drawRect(color = bgColor)

                val holdT = ((t - 0.34f) / 0.38f).coerceIn(0f, 1f)
                val cy = h * 0.5f
                val env = sin(holdT * PI.toFloat()).coerceIn(0f, 1f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (highContrastMode) 0.08f else 0.14f * (0.4f + 0.6f * env)),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, cy),
                        radius = minOf(w, h) * 0.55f
                    ),
                    radius = minOf(w, h) * 0.55f,
                    center = Offset(w * 0.5f, cy)
                )

                val segments = 72
                val amp = if (suppressFlashes) h * 0.012f else h * 0.032f * (0.25f + 0.75f * env)
                val phase = holdT * 10f
                wavePath.reset()
                for (i in 0..segments) {
                    val u = i.toFloat() / segments.toFloat()
                    val x = u * w
                    val centered = (u - 0.5f) * 2f
                    val window = exp(-4.5f * centered * centered)
                    val y = cy + sin(u * 22f - phase) * cos(u * 7f + phase * 0.4f) * amp * window
                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            borderColor.copy(alpha = 0.75f),
                            accentColor.copy(alpha = 0.95f),
                            borderColor.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )

                val ruleOffset = 44.dp.toPx()
                val ruleHalfW = minOf(w * 0.36f, 340.dp.toPx()) * (0.62f + 0.38f * FastOutSlowInEasing.transform(holdT))
                val cx = w * 0.5f
                for (sign in intArrayOf(-1, 1)) {
                    val ry = cy + sign * ruleOffset
                    drawLine(
                        color = borderColor.copy(alpha = 0.55f),
                        start = Offset(cx - ruleHalfW, ry),
                        end = Offset(cx + ruleHalfW, ry),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            } else {
                val remainH = halfH * (1f - openEase)
                if (remainH > 0.5f) {
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, remainH)
                    )
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(0f, h - remainH),
                        size = Size(w, remainH)
                    )
                    val edgeAlpha = (1f - openFrac) * 0.85f
                    val edgeW = 2.dp.toPx()
                    drawLine(
                        color = accentColor.copy(alpha = edgeAlpha),
                        start = Offset(0f, remainH),
                        end = Offset(w, remainH),
                        strokeWidth = edgeW
                    )
                    drawLine(
                        color = accentColor.copy(alpha = edgeAlpha),
                        start = Offset(0f, h - remainH),
                        end = Offset(w, h - remainH),
                        strokeWidth = edgeW
                    )
                }
            }
        }

        if (mainText.isNotBlank() && t > 0.28f && t < 0.76f) {
            val enterAlpha = ((t - 0.28f) / 0.10f).coerceIn(0f, 1f)
            val exitAlpha = if (mode == TransitionMode.ENTER) 1f else (1f - ((t - 0.68f) / 0.08f).coerceIn(0f, 1f))
            val combinedAlpha = enterAlpha * exitAlpha
            val settleScale = lerp(1.06f, 1.0f, FastOutLinearInEasing.transform(enterAlpha))

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = combinedAlpha
                        scaleX = settleScale
                        scaleY = settleScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mainText.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = headerFont,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.2.sp,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
                if (subText.isNotBlank()) {
                    Text(
                        text = subText.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = headerFont,
                            color = accentColor.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.4.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
