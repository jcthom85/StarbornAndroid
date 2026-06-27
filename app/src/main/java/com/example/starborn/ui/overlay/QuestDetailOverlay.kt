package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QuestDetailOverlay(
    uiEventBus: UiEventBus,
    gradientColor: Color,
    outlineColor: Color,
    deferShowing: Boolean = false,
    modifier: Modifier = Modifier,
    onPresentationVisibleChanged: (Boolean) -> Unit = {},
    onPresentationStarted: (QuestBannerType) -> Unit = {},
    onShowDetails: (String) -> Unit
) {
    val queue = remember { mutableStateListOf<UiEvent.ShowQuestDetail>() }
    var current by remember { mutableStateOf<UiEvent.ShowQuestDetail?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(uiEventBus) {
        uiEventBus.events.collectLatest { event ->
            if (event is UiEvent.ShowQuestDetail) {
                val alreadyPending =
                    current?.questId == event.questId && current?.type == event.type ||
                        queue.any { it.questId == event.questId && it.type == event.type }
                if (alreadyPending) return@collectLatest
                if (current == null) {
                    current = event
                } else {
                    queue.add(event)
                }
            }
        }
    }

    LaunchedEffect(current, deferShowing, visible) {
        if (current != null && !visible && !deferShowing) {
            onPresentationStarted(current!!.type)
            visible = true
        }
    }

    LaunchedEffect(visible, current) {
        onPresentationVisibleChanged(visible && current != null)
    }

    AnimatedVisibility(
        visible = visible && current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            current?.let { detail ->
                QuestDetailCard(
                    detail = detail,
                    gradientColor = gradientColor,
                    outlineColor = outlineColor,
                    onDismiss = {
                        visible = false
                        current = if (queue.isNotEmpty()) queue.removeAt(0) else null
                    },
                    onShowDetails = onShowDetails
                )
            }
        }
    }
}

@Composable
private fun QuestDetailObjectiveRow(
    text: String,
    completed: Boolean,
    accentColor: Color
) {
    val icon = if (completed) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
    val iconTint = if (completed) accentColor else Color.White.copy(alpha = 0.70f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (completed) "Completed objective" else "Incomplete objective",
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = if (completed) 0.78f else 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuestDetailCard(
    detail: UiEvent.ShowQuestDetail,
    gradientColor: Color,
    outlineColor: Color,
    onDismiss: () -> Unit,
    onShowDetails: (String) -> Unit
) {
    val accent = gradientColor
    val isNewQuest = detail.type == QuestBannerType.NEW
    val shimmerSweep = if (isNewQuest) {
        val transition = rememberInfiniteTransition(label = "new_quest_shimmer")
        transition.animateFloat(
            initialValue = -1.25f,
            targetValue = -1.25f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3400
                    -1.25f at 0
                    -1.25f at 420
                    2.25f at 2320
                    2.25f at 3400
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "new_quest_shimmer_sweep"
        ).value
    } else {
        0f
    }
    val icon = when (detail.type) {
        QuestBannerType.NEW -> Icons.Filled.Star
        QuestBannerType.COMPLETED -> Icons.Filled.CheckCircle
        QuestBannerType.FAILED -> Icons.Filled.Warning
        QuestBannerType.PROGRESS -> Icons.Filled.CheckCircle
    }
    val heading = when (detail.type) {
        QuestBannerType.NEW -> "New Quest"
        QuestBannerType.COMPLETED -> "Quest Completed"
        QuestBannerType.FAILED -> "Quest Failed"
        QuestBannerType.PROGRESS -> "Quest Updated"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 520.dp)
            .semantics { contentDescription = "Quest Detail Popup" },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061018).copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
                .then(
                    if (isNewQuest) {
                        Modifier.graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isNewQuest) {
                        Modifier.drawWithContent {
                            drawContent()
                            val x = size.width * shimmerSweep
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        accent.copy(alpha = 0.13f),
                                        Color.White.copy(alpha = 0.12f),
                                        accent.copy(alpha = 0.09f),
                                        Color.Transparent
                                    ),
                                    start = Offset(x - size.width * 0.30f, 0f),
                                    end = Offset(x + size.width * 0.16f, size.height)
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = heading.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = detail.questTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = detail.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
            if (detail.objectives.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.objectives.forEach { line ->
                        QuestDetailObjectiveRow(
                            text = line,
                            completed = detail.type == QuestBannerType.COMPLETED,
                            accentColor = accent
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onShowDetails(detail.questId)
                    }
                ) {
                    Text("Details", color = accent)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Continue")
                }
            }
        }
    }
}


