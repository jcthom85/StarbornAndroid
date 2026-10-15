package com.example.starborn.feature.exploration.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.feature.exploration.viewmodel.AreaEntryUi
import kotlinx.coroutines.delay

@Composable
internal fun AreaEntryOverlay(entry: AreaEntryUi, onFinished: () -> Unit) {
    val opacity = remember(entry.id) { Animatable(1f) }
    val titleOpacity = remember(entry.id) { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    BackHandler {}
    LaunchedEffect(entry.id) {
        titleOpacity.animateTo(1f, tween(180))
        delay(if (entry.firstVisit) 900L else 550L)
        opacity.animateTo(0f, tween(300))
        finish()
    }
    Box(
        Modifier.fillMaxSize().zIndex(150f)
            .graphicsLayer { alpha = opacity.value }
            .background(Color(0xFF030712))
            .pointerInput(entry.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    do { val event = awaitPointerEvent(); event.changes.forEach { it.consume() } }
                    while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(32.dp).widthIn(max = 520.dp)
                .graphicsLayer { alpha = titleOpacity.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, textAlign = TextAlign.Center)
            Box(Modifier.width(120.dp).height(2.dp).background(Color(0xFF63E6FF)))
            if (entry.firstVisit) {
                Text("NEW AREA", color = Color(0xFF63E6FF), fontSize = 12.sp,
                    letterSpacing = 2.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
