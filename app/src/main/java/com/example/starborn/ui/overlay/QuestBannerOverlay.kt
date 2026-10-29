package com.example.starborn.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.QuestObjectiveStatus
import com.example.starborn.ui.events.QuestObjectiveRole
import com.example.starborn.ui.haptics.HapticType
import com.example.starborn.ui.haptics.Haptics
import kotlinx.coroutines.delay

private data class Banner(
    val type: QuestBannerType,
    val questId: String,
    val title: String,
    val objectives: List<QuestObjectiveStatus>,
    val remainingObjectiveCount: Int
)

private const val QUEST_BANNER_AUTO_DISMISS_MS = 3_200L

@Composable
fun QuestBannerOverlay(
    state: com.example.starborn.ui.events.QuestPresentationState,
    accentColor: Color,
    modifier: Modifier = Modifier,
    deferShowing: Boolean = false
) {
    val current = state.updates.firstOrNull()
    val visible = current != null && !deferShowing && state.important.isEmpty()
    val context = LocalContext.current
    fun dismiss() { if (state.updates.firstOrNull() == current) state.updates.removeAt(0) }
    LaunchedEffect(current, visible) {
        if (visible) {
            Haptics.pulse(context, HapticType.TICK)
            delay(QUEST_BANNER_AUTO_DISMISS_MS)
            dismiss()
        }
    }
    if (visible && current != null) Box(
        modifier = modifier.statusBarsPadding().padding(top = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        QuestBannerCard(Banner(current.type, current.questId, current.questTitle,
            current.objectives, current.remainingObjectiveCount), accentColor, ::dismiss)
    }
}

@Composable
private fun QuestBannerCard(
    banner: Banner,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val accent = when (banner.type) {
        QuestBannerType.NEW -> Color(0xFFFFB300)
        QuestBannerType.COMPLETED -> accentColor.copy(alpha = 0.9f)
        QuestBannerType.FAILED -> Color(0xFFFF5252)
        QuestBannerType.PROGRESS -> accentColor.copy(alpha = 0.85f)
    }
    val heading = when (banner.type) {
        QuestBannerType.NEW -> "New Quest"
        QuestBannerType.COMPLETED -> "Quest Completed"
        QuestBannerType.FAILED -> "Quest Failed"
        QuestBannerType.PROGRESS -> "Quest Updated"
    }
    val icon = when (banner.type) {
        QuestBannerType.NEW -> Icons.Filled.Star
        QuestBannerType.COMPLETED -> Icons.Filled.CheckCircle
        QuestBannerType.FAILED -> Icons.Filled.Warning
        QuestBannerType.PROGRESS -> Icons.Filled.CheckCircle
    }

    var swipeDistance by remember(banner.questId, banner.type) { mutableStateOf(0f) }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 500.dp)
            .graphicsLayer { translationX = swipeDistance }
            .pointerInput(banner.questId, banner.type) {
                detectHorizontalDragGestures(onDragStart = { swipeDistance = 0f },
                    onDragCancel = { swipeDistance = 0f },
                    onDragEnd = { if (kotlin.math.abs(swipeDistance) >= 56.dp.toPx()) onDismiss() else swipeDistance = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); swipeDistance += amount })
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    if (delta < -12f) onDismiss()
                },
                orientation = Orientation.Vertical
            )
            .clickable(onClick = onDismiss)
            .semantics { contentDescription = "Quest Banner" },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061018).copy(alpha = 0.98f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = heading.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if ((banner.type == QuestBannerType.PROGRESS || banner.type == QuestBannerType.NEW) && banner.objectives.isNotEmpty()) {
                    banner.objectives.forEach { objective ->
                        BannerObjectiveRow(objective = objective, accentColor = accent)
                    }
                    if (banner.remainingObjectiveCount > 0) {
                        Text(
                            text = "+${banner.remainingObjectiveCount} more objectives",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss Quest Banner",
                    tint = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BannerObjectiveRow(
    objective: QuestObjectiveStatus,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = if (objective.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked
        val tint = if (objective.completed) accentColor else Color.White.copy(alpha = 0.7f)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = when (objective.role) {
                QuestObjectiveRole.JUST_COMPLETED -> "Completed: ${objective.text}"
                QuestObjectiveRole.NEXT -> "Next: ${objective.text}"
                QuestObjectiveRole.STANDARD -> objective.text
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = if (objective.completed) 0.74f else 0.96f),
                fontWeight = if (objective.completed) FontWeight.Medium else FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


