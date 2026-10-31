package com.example.starborn.ui.fishing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.feature.fishing.viewmodel.*

val FishingInk = Color(0xFF081B25)
val FishingGold = Color(0xFFF2CF85)
val FishingMist = Color(0xFFD7EBED)
val FishingCoral = Color(0xFFFFA38D)

@Composable
fun FishingTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme.copy(
        background = FishingInk, surface = FishingInk, surfaceVariant = Color(0xFF18353F),
        onSurface = FishingMist, onSurfaceVariant = Color(0xFFBAD3D8),
        primary = FishingGold, onPrimary = FishingInk, primaryContainer = Color(0xFF30464B),
        onPrimaryContainer = FishingMist, secondary = Color(0xFF8ED9D3),
        tertiary = Color(0xFF8ED9D3), error = FishingCoral, outline = Color(0xFF66858D))
    MaterialTheme(colorScheme = colors, content = content)
}

/** Portrait scene shared by touch and desktop; mechanics and rewards stay in the controller. */
@Composable
fun FishingWaterScene(state: FishingUiState, onHook: () -> Unit, modifier: Modifier = Modifier) {
    val cue = fishingCue(state)
    val currentHook by rememberUpdatedState(onHook)
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val progress by animateFloatAsState(state.reelState?.progress ?: 0f, tween(120), label = "fishDistance")
    val tension = (state.reelState?.tension ?: 0f).coerceIn(0f, 1f)
    val lineColor = if (cue.urgent) FishingCoral else FishingGold
    Column(modifier.fillMaxSize().testTag("fishing-portrait-scene"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(cue.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            color = if (cue.urgent) FishingCoral else FishingGold)
        Text(cue.instruction, style = MaterialTheme.typography.bodyMedium, color = FishingMist)
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(FishingInk).semantics { contentDescription = "Fishing water. ${cue.title}. ${cue.instruction}" }
            .pointerInput(state.fishingState, threshold) {
                if (state.fishingState == FishingState.HOOKSET) {
                    var delta = Offset.Zero
                    detectDragGestures(onDragStart = { delta = Offset.Zero },
                        onDragCancel = { delta = Offset.Zero },
                        onDragEnd = { if (isUpwardHookSwipe(delta.x, delta.y, threshold)) currentHook() },
                        onDrag = { change, amount -> delta += amount; change.consume() })
                }
            }) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF294D57), Color(0xFF102F3D), FishingInk)))
                val w = size.width; val h = size.height
                // Shallow reflections and a quiet shoreline, without a looping flash animation.
                repeat(8) { index ->
                    val y = h * (.10f + index * .10f)
                    val x = w * (.08f + (index % 3) * .12f)
                    drawLine(Color(0xFF84BBB9).copy(alpha = .12f), Offset(x, y), Offset(w - x * .6f, y), 1.dp.toPx())
                }
                drawOval(Color(0xFF183C40), Offset(-w * .2f, h * .83f), Size(w * 1.4f, h * .5f))
                val fighting = state.fishingState == FishingState.REELING || state.fishingState == FishingState.READY
                val surge = state.reelState?.phase == FishingFightPhase.SURGE
                val fish = Offset(w * (if (surge) .32f else .46f), h * (.20f + progress * .45f))
                val float = if (fighting) fish else Offset(w * .46f, h * .38f)
                val tip = Offset(w * (.68f - tension * .12f), h * .72f)
                val rod = Path().apply {
                    moveTo(w * .90f, h * 1.02f)
                    quadraticTo(w * .72f, h * .79f, tip.x, tip.y)
                }
                drawPath(rod, FishingGold, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                val line = Path().apply {
                    moveTo(tip.x, tip.y)
                    quadraticTo(tip.x - w * .16f, float.y + h * .1f * (1f - tension), float.x, float.y)
                }
                drawPath(line, lineColor, style = Stroke((1.5f + tension).dp.toPx(), cap = StrokeCap.Round))
                for (ring in 1..3) {
                    val radius = (8f + ring * if (surge) 12f else 8f).dp.toPx()
                    drawOval(lineColor.copy(alpha = .26f / ring), float - Offset(radius, radius * .30f),
                        Size(radius * 2, radius * .60f), style = Stroke(1.dp.toPx()))
                }
                if (fighting) {
                    val r = minOf(w, h) * .055f
                    if (state.reelState?.salvage == true) {
                        drawRoundRect(FishingGold, fish - Offset(r, r), Size(r * 2, r * 2), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    } else {
                        drawOval(Color(0xFFB8DFD5), fish - Offset(r * 1.5f, r * .65f), Size(r * 3, r * 1.3f))
                        drawPath(Path().apply {
                            moveTo(fish.x - r, fish.y); lineTo(fish.x - r * 2.2f, fish.y - r)
                            lineTo(fish.x - r * 2.2f, fish.y + r); close()
                        }, Color(0xFF8EBFB9))
                        drawCircle(FishingInk, r * .12f, fish + Offset(r, -r * .12f))
                    }
                } else {
                    drawCircle(FishingMist, 4.dp.toPx(), float)
                    drawCircle(FishingGold, 2.dp.toPx(), float)
                    if (state.fishingState == FishingState.HOOKSET) {
                        drawLine(FishingGold, float + Offset(0f, -15.dp.toPx()), float + Offset(0f, -48.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                        drawLine(FishingGold, float + Offset(-8.dp.toPx(), -38.dp.toPx()), float + Offset(0f, -48.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                        drawLine(FishingGold, float + Offset(8.dp.toPx(), -38.dp.toPx()), float + Offset(0f, -48.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            if (state.fishingState == FishingState.HOOKSET && state.hookState?.practiceHook == true) {
                Text("FIRST BITE · NO RUSH", Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    style = MaterialTheme.typography.labelMedium, color = FishingGold)
            }
        }
        if (state.fishingState == FishingState.REELING) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.reelState?.fishName.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = FishingMist)
                Text(if (tension >= .75f) "Tight — release" else if (tension >= .4f) "Line loading" else "Line relaxed",
                    style = MaterialTheme.typography.labelMedium, color = lineColor)
            }
            LinearProgressIndicator(progress = { tension }, modifier = Modifier.fillMaxWidth().height(4.dp),
                color = lineColor, trackColor = Color(0xFF304D56))
        }
    }
}
