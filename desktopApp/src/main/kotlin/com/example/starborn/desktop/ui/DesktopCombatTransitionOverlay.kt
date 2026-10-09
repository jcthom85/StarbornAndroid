package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.starborn.data.local.Theme
import kotlin.random.Random

internal enum class TransitionMode {
    FULL,
    ENTER,
    EXIT
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
    mainText: String = "HOSTILES",
    subText: String = "INCOMING!",
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val progress = remember(mode) { Animatable(if (mode == TransitionMode.EXIT) 0.85f else 0f) }

    val defaultAccent = Color(0xFFE91E63)
    val defaultBg = Color(0xFF121212)

    val accentColor = themeColor(theme?.accent, defaultAccent)
    val bgColor = themeColor(theme?.bg, defaultBg)
    val borderColor = themeColor(theme?.border, Color.Gray)

    LaunchedEffect(visible, mode) {
        val start = if (mode == TransitionMode.EXIT) 0.85f else 0f
        val end = if (mode == TransitionMode.ENTER) 0.85f else 1f
        val duration = if (mode == TransitionMode.EXIT) 250 else 1400
        val adjustedDuration = if (mode == TransitionMode.ENTER) (1400 * 0.85).toInt() else duration

        progress.snapTo(start)
        progress.animateTo(
            targetValue = end,
            animationSpec = tween(durationMillis = adjustedDuration, easing = LinearEasing)
        )
        onFinished()
    }

    val t = progress.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) { awaitPointerEvent() }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // --- Phase 1: The Slash (Wipe) ---
            val slashT = (t / 0.3f).coerceIn(0f, 1f)
            val slashEase = FastOutSlowInEasing.transform(slashT)

            rotate(degrees = -15f, pivot = center) {
                val barWidth = w * 2.5f
                val barHeight = h * 0.8f
                val barLeft = center.x - barWidth / 2f

                // Top Half Slam
                val topTargetY = center.y - barHeight
                val topStartY = -h * 1.5f
                val topCurrentY = lerp(topStartY, topTargetY, slashEase)

                drawRect(
                    color = bgColor,
                    topLeft = Offset(barLeft, topCurrentY),
                    size = Size(barWidth, barHeight)
                )
                drawRect(
                    color = accentColor,
                    topLeft = Offset(barLeft, topCurrentY + barHeight - 20f),
                    size = Size(barWidth, 20f)
                )

                // Bottom Half Slam
                val bottomTargetY = center.y
                val bottomStartY = h * 2.5f
                val bottomCurrentY = lerp(bottomStartY, bottomTargetY, slashEase)

                drawRect(
                    color = bgColor,
                    topLeft = Offset(barLeft, bottomCurrentY),
                    size = Size(barWidth, barHeight)
                )
                drawRect(
                    color = accentColor,
                    topLeft = Offset(barLeft, bottomCurrentY),
                    size = Size(barWidth, 20f)
                )
            }

            // --- Phase 2: The Hold (Full Coverage) ---
            if (t > 0.25f && (mode == TransitionMode.ENTER || t <= 0.85f)) {
                drawRect(color = bgColor)

                val lineT = ((t - 0.25f) / 0.6f).coerceIn(0f, 1f)
                val lineOffset = w * lineT

                rotate(degrees = -15f, pivot = center) {
                    drawRect(
                        color = borderColor.copy(alpha = 0.3f),
                        topLeft = Offset(lineOffset, -h * 0.2f),
                        size = Size(50f, h * 2f)
                    )
                    drawRect(
                        color = accentColor.copy(alpha = 0.2f),
                        topLeft = Offset(w - lineOffset, -h * 0.2f),
                        size = Size(20f, h * 2f)
                    )
                }
            }

            // --- Phase 3: The Reveal (Exit) ---
            if (t > 0.85f && mode != TransitionMode.ENTER) {
                val exitT = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
                val exitEase = LinearOutSlowInEasing.transform(exitT)

                val openHeight = (h / 2f) * exitEase
                val barWidth = w * 2.5f
                val barHeight = h * 0.8f
                val barLeft = center.x - barWidth / 2f

                rotate(degrees = -15f, pivot = center) {
                    val topTargetY = center.y - barHeight
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(barLeft, topTargetY - openHeight),
                        size = Size(barWidth, barHeight)
                    )
                    val bottomTargetY = center.y
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(barLeft, bottomTargetY + openHeight),
                        size = Size(barWidth, barHeight)
                    )

                    if (!suppressFlashes && exitT < 0.5f) {
                        val flashAlpha = (1f - (exitT * 2f))
                        val flashHeight = 40f * (1f - exitT)
                        drawRect(
                            color = Color.White.copy(alpha = flashAlpha),
                            topLeft = Offset(barLeft, topTargetY + barHeight - openHeight - flashHeight),
                            size = Size(barWidth, flashHeight)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = flashAlpha),
                            topLeft = Offset(barLeft, bottomTargetY + openHeight),
                            size = Size(barWidth, flashHeight)
                        )
                    }
                }
            }
        }

        // --- Typography: HOSTILES INCOMING / Outcome text ---
        if (t > 0.25f && t < 0.95f && (mainText.isNotBlank() || subText.isNotBlank())) {
            val textEnterT = ((t - 0.25f) / 0.1f).coerceIn(0f, 1f)
            val textExitT = ((t - 0.85f) / 0.1f).coerceIn(0f, 1f)

            val scale = 2f - (1f * textEnterT) + (0.5f * textExitT)
            val alpha = textEnterT * (1f - textExitT)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        if (!suppressFlashes) {
                            translationX = (Random.nextFloat() * 10 - 5)
                            translationY = (Random.nextFloat() * 10 - 5)
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mainText.isNotBlank()) {
                        Box {
                            Text(
                                text = mainText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 4.sp,
                                    shadow = Shadow(
                                        color = borderColor,
                                        offset = Offset(4f, 4f),
                                        blurRadius = 0f
                                    )
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = mainText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 4.sp
                                ),
                                modifier = Modifier.graphicsLayer {
                                    translationX = 6f
                                    translationY = -3f
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (subText.isNotBlank()) {
                        Box(modifier = Modifier.graphicsLayer { translationY = -10f }) {
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 4.sp,
                                    shadow = Shadow(
                                        color = borderColor,
                                        offset = Offset(4f, 4f),
                                        blurRadius = 0f
                                    )
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = 4.sp
                                ),
                                modifier = Modifier.graphicsLayer {
                                    translationX = -6f
                                    translationY = -3f
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
