package com.example.starborn.feature.exploration.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.components.maestroTag

/** Overlays the viewport edge, so displaying the cue cannot create its own overflow. */
@Composable
fun ScrollContinuationCue(
    visible: Boolean,
    accentColor: Color,
    description: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val transition = rememberInfiniteTransition(label = "scrollContinuation")
    val pulse by transition.animateFloat(.65f, 1f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scrollCueGlow")
    Box(modifier.fillMaxWidth().height(28.dp)
        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0061018)))),
        contentAlignment = Alignment.BottomCenter) {
        Surface(onClick = onClick, shape = RoundedCornerShape(999.dp),
            color = Color(0xFF091725), border = BorderStroke(1.dp, accentColor.copy(alpha = pulse)),
            shadowElevation = 8.dp,
            modifier = Modifier.heightIn(min = 28.dp).maestroTag(tag)
                .semantics { contentDescription = description }) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MORE", style = MaterialTheme.typography.labelSmall, color = accentColor)
                Icon(Icons.Default.KeyboardDoubleArrowDown, null, tint = accentColor,
                    modifier = Modifier.size(16.dp).graphicsLayer { translationY = (pulse - .65f) * 8.dp.toPx() })
            }
        }
    }
}
