package com.example.starborn.ui.exploration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.viewmodel.ScriptedTravelStyle
import kotlin.math.sin

suspend fun animateScriptedTravel(
    progress: Animatable<Float, AnimationVector1D>,
    style: ScriptedTravelStyle,
    reducedMotion: Boolean,
    playCue: (String) -> Unit,
    revealRoom: () -> Unit
) {
    val duration = if (reducedMotion) minOf(650, style.durationMs) else style.durationMs
    playCue(style.departureCue)
    progress.animateTo(.5f, tween(duration / 2, easing = LinearEasing))
    revealRoom()
    style.arrivalCue?.let(playCue)
    progress.animateTo(1f, tween(duration - duration / 2, easing = LinearEasing))
}

@Composable
fun ScriptedTravelOverlay(
    style: ScriptedTravelStyle,
    destination: String?,
    progress: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val cover = (1f - kotlin.math.abs(progress * 2f - 1f)).coerceIn(0f, 1f)
    val dark = Color(0xFF080D12)
    val accent = when (style) {
        ScriptedTravelStyle.FOUNDRY -> Color(0xFFE4A878)
        ScriptedTravelStyle.LAUNCH -> Color(0xFFA7DBF5)
        ScriptedTravelStyle.SOURCE -> Color(0xFFC5B3E9)
        else -> Color(0xFFA3C9CD)
    }
    Box(modifier.fillMaxSize().testTag("scripted-travel").pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
    }) {
        Canvas(Modifier.fillMaxSize()) {
            if (reducedMotion) {
                drawRect(dark.copy(alpha = cover))
                return@Canvas
            }

            when (style) {
                ScriptedTravelStyle.LIFT, ScriptedTravelStyle.AIRLOCK -> {
                    val panelWidth = size.width * .5f * cover
                    val left = Brush.horizontalGradient(listOf(Color(0xFF263940), dark), 0f, size.width * .5f)
                    val right = Brush.horizontalGradient(listOf(dark, Color(0xFF263940)), size.width * .5f, size.width)
                    drawRect(left, size = Size(panelWidth, size.height))
                    drawRect(right, Offset(size.width - panelWidth, 0f), Size(panelWidth, size.height))
                    for (edge in listOf(panelWidth, size.width - panelWidth)) {
                        drawLine(accent.copy(alpha = .4f * cover), Offset(edge, 0f), Offset(edge, size.height), 2.dp.toPx())
                    }
                }
                ScriptedTravelStyle.FOUNDRY -> {
                    drawRect(dark.copy(alpha = cover))
                    repeat(4) { band ->
                        val y = size.height * (.9f - progress * .6f + band * .09f)
                        val x = size.width * (.5f + .16f * sin(band + progress * 4f))
                        val steam = Brush.radialGradient(
                            listOf(accent.copy(alpha = .18f * cover), Color.Transparent),
                            center = Offset(x, y), radius = size.width * .72f
                        )
                        drawRect(steam)
                    }
                }
                ScriptedTravelStyle.LAUNCH, ScriptedTravelStyle.SOURCE -> {
                    drawRect(dark.copy(alpha = cover))
                    val center = Offset(size.width * .5f, size.height * .47f)
                    repeat(18) { ray ->
                        val x = ((ray * 37 % 101) / 100f - .5f) * size.width
                        val y = ((ray * 61 % 103) / 102f - .5f) * size.height
                        val vector = Offset(x, y)
                        val distance = .16f + (progress + ray * .09f) % .72f
                        drawLine(accent.copy(alpha = .32f * cover), center + vector * distance,
                            center + vector * (distance + .16f * cover), 1.5.dp.toPx())
                    }
                }
                ScriptedTravelStyle.PASSAGE -> drawRect(dark.copy(alpha = cover))
            }
        }
        if (destination != null && progress >= .5f) {
            val labelAlpha = minOf((progress - .5f) / .12f, (1f - progress) / .16f, 1f)
            Text(destination, color = Color.White, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                    .graphicsLayer { alpha = labelAlpha }
                    .background(dark.copy(alpha = .88f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp, vertical = 9.dp))
        }
    }
}
