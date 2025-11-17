package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QuestDetailOverlay(
    uiEventBus: UiEventBus,
    gradientColor: Color,
    outlineColor: Color,
    autoDismissMillis: Long = 8000L
) {
    var current by remember { mutableStateOf<UiEvent.ShowQuestDetail?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(uiEventBus) {
        uiEventBus.events.collectLatest { event ->
            if (event is UiEvent.ShowQuestDetail) {
                current = event
                visible = true
            }
        }
    }

    LaunchedEffect(current, visible) {
        if (visible && current != null) {
            delay(autoDismissMillis)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            current?.let { detail ->
                QuestDetailCard(
                    detail = detail,
                    gradientColor = gradientColor,
                    outlineColor = outlineColor,
                    onDismiss = { visible = false }
                )
            }
        }
    }
}

@Composable
private fun QuestDetailCard(
    detail: UiEvent.ShowQuestDetail,
    gradientColor: Color,
    outlineColor: Color,
    onDismiss: () -> Unit
) {
    val accent = gradientColor
    val icon = when (detail.type) {
        QuestBannerType.NEW -> Icons.Filled.Star
        QuestBannerType.COMPLETED -> Icons.Filled.CheckCircle
        QuestBannerType.FAILED -> Icons.Filled.CheckCircle
        QuestBannerType.PROGRESS -> Icons.Filled.CheckCircle
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(18.dp),
        border = BorderStroke(1.5.dp, outlineColor),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF03070F))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .padding(horizontal = 28.dp, vertical = 26.dp)
                .widthIn(min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = accent)
                    Text(
                        text = when (detail.type) {
                            QuestBannerType.NEW -> "New Quest"
                            QuestBannerType.COMPLETED -> "Quest Completed"
                            QuestBannerType.FAILED -> "Quest Failed"
                            QuestBannerType.PROGRESS -> "Quest Updated"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White)
                }
            }
            Text(
                text = detail.questTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = detail.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (detail.objectives.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.objectives.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Continue")
            }
        }
    }
}
