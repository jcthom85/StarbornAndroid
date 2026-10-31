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
@Composable
fun GladePuffball(
    trust: Int,
    playToken: Int,
    enabled: Boolean,
    reducedMotion: Boolean,
    onInspect: () -> Unit,
    onPlayFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var busy by remember { mutableStateOf(false) }
    var lastToken by remember { mutableStateOf(playToken) }
    val roll = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val finish by rememberUpdatedState(onPlayFinished)
    val cancel by rememberUpdatedState(onCancel)
    DisposableEffect(Unit) { onDispose { cancel() } }
    LaunchedEffect(Unit) {
        busy = true
        if (!reducedMotion) roll.animateTo(1f, tween(1100))
        busy = false
    }
    LaunchedEffect(playToken) {
        if (playToken == lastToken) return@LaunchedEffect
        lastToken = playToken
        busy = true
        if (!reducedMotion) {
            roll.animateTo(2f, tween(380))
            roll.snapTo(0f)
            delay(if (trust == 0) 650L else 150L)
            roll.animateTo(if (trust == 0) 1f else 1.08f, tween(if (trust == 0) 1350 else if (trust == 1) 900 else 650))
            if (trust > 0) roll.animateTo(1f, tween(180))
        }
        busy = false
        finish()
    }
    // The lane uses the scene width. Only the ball receives input; the lane never clips it.
    BoxWithConstraints(modifier.fillMaxWidth().height(112.dp).testTag("glade-puffball-lane")) {
        val travel = maxWidth / 2 + 64.dp
        val u = roll.value
        val bounce = if (reducedMotion) 0f else abs(sin(u * PI * 3)).toFloat() * (1f - u).coerceIn(0f, 1f) * 22f
                Canvas(Modifier.align(Alignment.BottomCenter)
                    .offset(x = travel * (1f - u), y = (-bounce).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .rotate(if (reducedMotion) 0f else (1f - u) * 300f)
                    .testTag("glade-puffball")
                    .semantics { contentDescription = if (trust == 0) "Spore puffball. Tap to play gently." else "Charlie's puffball. Tap to play." }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled && !busy) { onInspect() }) {
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
}
