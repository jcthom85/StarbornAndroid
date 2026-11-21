package com.example.starborn.ui.exploration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.movement.EnemyAggressionState

@Composable
fun PatrolWarning(aggression: EnemyAggressionState, modifier: Modifier = Modifier) {
    val totalMs = aggression.totalMs.coerceAtLeast(1L)
    val rawFraction = (aggression.remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val smoothFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "patrolWarningFraction"
    )
    val urgency = (1f - smoothFraction).coerceIn(0f, 1f)
    val critical = aggression.remainingMs <= 1_500L
    val seconds = ((aggression.remainingMs + 999L) / 1_000L).coerceAtLeast(0L)

    val pulseTransition = rememberInfiniteTransition(label = "patrolWarningPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (critical) 320 else 720,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "patrolWarningPulseAlpha"
    )

    val accentColor = lerp(Color(0xFFF59E0B), Color(0xFFEF4444), urgency)
    val barBrush = Brush.horizontalGradient(
        listOf(
            lerp(Color(0xFFFBBF24), Color(0xFFF87171), urgency),
            lerp(Color(0xFFEA580C), Color(0xFFDC2626), urgency)
        )
    )
    val headline = if (aggression.remainingMs <= 700L) {
        "INCOMING AMBUSH!"
    } else {
        "SPOTTED · ATTACK IN ${seconds}s"
    }

    Surface(
        modifier = modifier
            .widthIn(min = 210.dp, max = 310.dp)
            .testTag("patrol-warning"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xEC1B1014),
        border = BorderStroke(
            width = if (critical) 1.5.dp else 1.dp,
            color = accentColor.copy(alpha = pulse)
        ),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚠",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = headline,
                    color = Color(0xFFFFF1E6),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(smoothFraction.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(barBrush)
                )
            }
            Text(
                text = "Tap first for +1 Momentum · Move to evade Ambush",
                color = Color(0xFFD6C2B8),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            )
        }
    }
}
