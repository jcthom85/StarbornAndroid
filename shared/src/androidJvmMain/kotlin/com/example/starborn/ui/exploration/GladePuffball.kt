package com.example.starborn.ui.exploration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A quiet, finite animation: no portrait, idle loop, timer challenge or motion-only control. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GladePuffball(
    trust: Int,
    feedback: String?,
    enabled: Boolean,
    reducedMotion: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var choices by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val roll = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        busy = true
        if (!reducedMotion) roll.animateTo(1f, tween(1100))
        busy = false
    }
    Surface(modifier.widthIn(max = 520.dp), shape = RoundedCornerShape(18.dp),
        color = Color.Transparent, contentColor = Color(0xFFE6EAD6)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(86.dp)) {
                val travel = maxWidth / 2 + 40.dp
                val u = roll.value
                val bounce = if (reducedMotion) 0f else abs(sin(u * PI * 3)).toFloat() * (1f - u).coerceIn(0f, 1f) * 22f
                Canvas(Modifier.align(Alignment.BottomCenter)
                    .offset(x = travel * (1f - u), y = (-bounce).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .rotate(if (reducedMotion) 0f else (1f - u) * 300f)
                    .testTag("glade-puffball")
                    .semantics { contentDescription = if (trust == 0) "Spore puffball. Tap to play gently." else "Charlie's puffball. Tap to play." }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled && !busy) { choices = !choices }) {
                    val c = center
                    val radius = size.minDimension * .32f
                    drawCircle(Color(0xFFB4A9BE).copy(alpha = .12f), radius * 1.25f, c)
                    repeat(36) { i ->
                        val a = i * PI * 2 / 36
                        val edge = Offset(c.x + cos(a).toFloat() * radius, c.y + sin(a).toFloat() * radius)
                        drawCircle(Color(0xFFB8A5BF), radius * .14f, edge)
                    }
                    drawCircle(Color(0xFF8E7799), radius, c)
                    drawCircle(Color(0xFFC8B9CB), radius * .76f, c - Offset(radius * .16f, radius * .18f))
                    repeat(22) { i ->
                        val a = i * 2.39996f
                        val d = radius * .74f * kotlin.math.sqrt(i / 22f)
                        drawCircle(Color(0xFF89718E).copy(alpha = .5f), 1.2.dp.toPx(),
                            c + Offset(cos(a) * d, sin(a) * d))
                    }
                }
            }
            Text(if (trust == 0) "A puffball rolls out of the ferns." else "A familiar puffball waits in the moss.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color(0xD6102421), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp).background(Color(0xED102421), RoundedCornerShape(12.dp)).padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (choices) {
                FlowRow(modifier = Modifier.background(Color(0xED102421), RoundedCornerShape(12.dp)), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled = enabled && !busy, modifier = Modifier.testTag("glade-play"), onClick = {
                        choices = false
                        scope.launch {
                            busy = true
                            if (!reducedMotion) {
                                roll.animateTo(2f, tween(380))
                                roll.snapTo(0f)
                                delay(if (trust == 0) 650L else 150L)
                                roll.animateTo(if (trust == 0) 1f else 1.08f, tween(if (trust == 0) 1350 else if (trust == 1) 900 else 650))
                                if (trust > 0) roll.animateTo(1f, tween(180))
                            }
                            onPlay()
                            busy = false
                        }
                    }) { Text(if (trust == 1) "Give it a gentle toss" else "Roll it back", color = Color(0xFFEACB91)) }
                    TextButton(onClick = { choices = false }) { Text("Leave it be", color = Color(0xFFD0D9CA)) }
                }
            }
        }
    }
}
