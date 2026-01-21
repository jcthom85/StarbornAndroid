package com.example.starborn.feature.exploration.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import com.example.starborn.data.local.Theme
import com.example.starborn.ui.theme.themeColor
import kotlin.math.hypot
import kotlin.random.Random

enum class TransitionMode {
    FULL,
    ENTER,
    EXIT
}

@Composable
fun CombatTransitionOverlay(
    visible: Boolean,
    theme: Theme?,
    suppressFlashes: Boolean,
    highContrastMode: Boolean,
    mode: TransitionMode = TransitionMode.FULL,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val progress = remember { Animatable(if (mode == TransitionMode.EXIT) 0.85f else 0f) }

    // Resolve Theme Colors
    val defaultAccent = Color(0xFFE91E63) // Default Fallback (Punchy Pink)
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
            // 0.0 -> 0.3: Slashes slam in quickly
            val slashT = (t / 0.3f).coerceIn(0f, 1f)
            val slashEase = FastOutSlowInEasing.transform(slashT)

            // Rotate the entire canvas for the "Angled Wipe" effect
            rotate(degrees = -15f, pivot = center) {
                // Oversized dimensions to ensure corner coverage during rotation
                val barWidth = w * 2.5f
                val barHeight = h * 0.8f 
                val barLeft = center.x - barWidth / 2f
                
                // Top Half Slam
                // Starts way up, slams down to cover center-top
                val topTargetY = center.y - barHeight
                val topStartY = -h * 1.5f
                val topCurrentY = androidx.compose.ui.util.lerp(topStartY, topTargetY, slashEase)
                
                drawRect(
                    color = bgColor,
                    topLeft = Offset(barLeft, topCurrentY),
                    size = Size(barWidth, barHeight)
                )
                // Glowing Edge (Top)
                drawRect(
                    color = accentColor,
                    topLeft = Offset(barLeft, topCurrentY + barHeight - 20f),
                    size = Size(barWidth, 20f)
                )

                // Bottom Half Slam
                // Starts way down, slams up to cover center-bottom
                val bottomTargetY = center.y
                val bottomStartY = h * 2.5f
                val bottomCurrentY = androidx.compose.ui.util.lerp(bottomStartY, bottomTargetY, slashEase)

                drawRect(
                    color = bgColor,
                    topLeft = Offset(barLeft, bottomCurrentY),
                    size = Size(barWidth, barHeight)
                )
                // Glowing Edge (Bottom)
                drawRect(
                    color = accentColor,
                    topLeft = Offset(barLeft, bottomCurrentY),
                    size = Size(barWidth, 20f)
                )
            }

            // --- Phase 2: The Hold (Full Coverage) ---
            // 0.25 -> 0.85: Screen is fully blocked
            if (t > 0.25f && t < 0.85f) {
                // Background fill to ensure no leaks
                drawRect(color = bgColor)
                
                // Dynamic "Speed Lines" in background
                val lineT = ((t - 0.25f) / 0.6f).coerceIn(0f, 1f)
                val lineOffset = w * lineT
                
                rotate(degrees = -15f, pivot = center) {
                    // Decorative accent lines passing through
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
            // 0.85 -> 1.0: Slashes retreat
            if (t > 0.85f) {
                val exitT = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
                val exitEase = androidx.compose.animation.core.LinearOutSlowInEasing.transform(exitT)
                
                // Instead of reversing, let's "split" open vertically
                val openHeight = (h / 2f) * exitEase
                
                // Oversized dimensions to match Phase 1
                val barWidth = w * 2.5f
                val barHeight = h * 0.8f 
                val barLeft = center.x - barWidth / 2f
                
                // Draw remaining blocks
                rotate(degrees = -15f, pivot = center) {
                    // Top Block Retreating Up
                    // Target was center.y - barHeight
                    val topTargetY = center.y - barHeight
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(barLeft, topTargetY - openHeight),
                        size = Size(barWidth, barHeight)
                    )
                    // Bottom Block Retreating Down
                    // Target was center.y
                    val bottomTargetY = center.y
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(barLeft, bottomTargetY + openHeight),
                        size = Size(barWidth, barHeight)
                    )
                    
                    // The Split Line (Flash)
                    if (exitT < 0.5f) {
                        val flashAlpha = (1f - (exitT * 2f))
                        val flashHeight = 40f * (1f - exitT)
                        // Top Flash
                        drawRect(
                            color = Color.White.copy(alpha = flashAlpha),
                            topLeft = Offset(barLeft, topTargetY + barHeight - openHeight - flashHeight),
                            size = Size(barWidth, flashHeight)
                        )
                        // Bottom Flash
                        drawRect(
                            color = Color.White.copy(alpha = flashAlpha),
                            topLeft = Offset(barLeft, bottomTargetY + openHeight),
                            size = Size(barWidth, flashHeight)
                        )
                    }
                }
            }
        }

        // --- Text: HOSTILES INCOMING! ---
        // Appears at 0.25, Holds, Glitches out at 0.95
        if (t > 0.25f && t < 0.95f) {
            val textEnterT = ((t - 0.25f) / 0.1f).coerceIn(0f, 1f)
            val textExitT = ((t - 0.85f) / 0.1f).coerceIn(0f, 1f)
            
            val scale = 2f - (1f * textEnterT) + (0.5f * textExitT) // Zoom in -> Hold -> Zoom out
            val alpha = textEnterT * (1f - textExitT)
            
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        // Tiny jitter
                        translationX = (Math.random() * 10 - 5).toFloat()
                        translationY = (Math.random() * 10 - 5).toFloat()
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Line 1: HOSTILES
                    Box {
                        Text(
                            text = "HOSTILES",
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
                        // Ghost Text
                        Text(
                            text = "HOSTILES",
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
                    
                    // Line 2: INCOMING!
                    Box(modifier = Modifier.graphicsLayer { translationY = -10f }) { 
                        Text(
                            text = "INCOMING!",
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
                        // Ghost Text
                        Text(
                            text = "INCOMING!",
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