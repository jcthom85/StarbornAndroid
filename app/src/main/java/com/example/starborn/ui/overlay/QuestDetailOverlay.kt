package com.example.starborn.ui.overlay

import com.example.starborn.ui.components.maestroTag
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay

@Composable
fun QuestDetailOverlay(
    state: com.example.starborn.ui.events.QuestPresentationState,
    gradientColor: Color,
    outlineColor: Color,
    deferShowing: Boolean = false,
    modifier: Modifier = Modifier,
    onPresentationVisibleChanged: (Boolean) -> Unit = {},
    onPresentationStarted: (QuestBannerType) -> Unit = {},
    onShowDetails: (String) -> Unit
) {
    val current = state.important.firstOrNull()
    val visible = current != null && !deferShowing
    val entrance = remember(current) { Animatable(0f) }
    LaunchedEffect(current, visible) {
        if (visible) { entrance.snapTo(0f); entrance.animateTo(1f, tween(650)) }
    }
    LaunchedEffect(current, visible) {
        onPresentationVisibleChanged(visible)
        if (visible && current != null && state.markStarted(current) &&
            current is com.example.starborn.ui.events.QuestPresentationState.Important.Detail) {
            onPresentationStarted(current.event.type)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onPresentationVisibleChanged(false) }
    }
    fun dismiss() { current?.let(state::dismissImportant) }
    androidx.activity.compose.BackHandler(enabled = visible) { dismiss() }
    // Unmount immediately when a scene takes focus; an exit animation must not cover dialogue.
    if (visible) Box(
        modifier = modifier.fillMaxSize().graphicsLayer { alpha = entrance.value }.background(Color.Black.copy(alpha = .65f))
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        when (current) {
            is com.example.starborn.ui.events.QuestPresentationState.Important.Detail -> QuestDetailCard(
                detail = current.event, gradientColor = gradientColor, outlineColor = outlineColor,
                onDismiss = ::dismiss,
                onShowDetails = onShowDetails)
            is com.example.starborn.ui.events.QuestPresentationState.Important.Summary -> QuestSummaryCard(
                data = current.event, accentColor = gradientColor,
                onDismiss = ::dismiss,
                onShowDetails = { id -> dismiss(); onShowDetails(id) })
            null -> Unit
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
    val shimmerSweep = remember(detail.questId, detail.type) { Animatable(-1.55f) }

    LaunchedEffect(isNewQuest, detail.questId, detail.type) {
        if (isNewQuest) {
            while (true) {
                shimmerSweep.snapTo(-1.55f)
                shimmerSweep.animateTo(
                    targetValue = 2.55f,
                    animationSpec = tween(durationMillis = 2300, easing = LinearEasing)
                )
                delay(850)
            }
        }
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
            .clickable(onClick = onDismiss)
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
                            val sweep = shimmerSweep.value
                            val shimmerAlpha = when {
                                sweep < -1.05f -> 0f
                                sweep < -0.60f -> ((sweep + 1.05f) / 0.45f).coerceIn(0f, 1f)
                                sweep > 2.05f -> 0f
                                sweep > 1.60f -> ((2.05f - sweep) / 0.45f).coerceIn(0f, 1f)
                                else -> 1f
                            }
                            if (shimmerAlpha > 0f) {
                                val x = size.width * sweep
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            accent.copy(alpha = 0.13f * shimmerAlpha),
                                            Color.White.copy(alpha = 0.12f * shimmerAlpha),
                                            accent.copy(alpha = 0.09f * shimmerAlpha),
                                            Color.Transparent
                                        ),
                                        start = Offset(x - size.width * 0.30f, 0f),
                                        end = Offset(x + size.width * 0.16f, size.height)
                                    )
                                )
                            }
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
                    modifier = Modifier.maestroTag("quest-continue-${detail.type.name.lowercase()}-${detail.questId}"),
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


