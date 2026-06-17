package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.QuestObjectiveStatus
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import com.example.starborn.ui.haptics.HapticType
import com.example.starborn.ui.haptics.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentHashMap

private data class Banner(
    val type: QuestBannerType,
    val questId: String,
    val title: String,
    val objectives: List<QuestObjectiveStatus>
)

private const val QUEST_BANNER_AUTO_DISMISS_MS = 5_000L

@Composable
fun QuestBannerOverlay(
    uiEventBus: UiEventBus,
    deferShowing: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val queue = remember { mutableStateListOf<Banner>() }
    var current by remember { mutableStateOf<Banner?>(null) }
    var isShowing by remember { mutableStateOf(false) }
    val recent = remember { ConcurrentHashMap<String, Long>() }
    val dedupeWindowMs = 2000L
    val context = LocalContext.current

    LaunchedEffect(uiEventBus) {
        uiEventBus.events.collectLatest { ev ->
            if (ev is UiEvent.ShowQuestBanner) {
                val key = "${ev.type}_${ev.questId}"
                val now = System.currentTimeMillis()
                val last = recent[key]
                if (last == null || now - last > dedupeWindowMs) {
                    recent[key] = now
                    queue.add(Banner(ev.type, ev.questId, ev.questTitle, ev.objectives))
                }
            }
        }
    }

    LaunchedEffect(deferShowing) {
        if (deferShowing && current != null) {
            queue.add(0, current!!)
            current = null
            isShowing = false
        }
    }

    LaunchedEffect(queue.size, deferShowing, current, isShowing) {
        if (current == null && queue.isNotEmpty() && !deferShowing) {
            current = queue.removeAt(0)
            isShowing = true
        }
    }

    LaunchedEffect(current) {
        current?.let { banner ->
            val type = when (banner.type) {
                QuestBannerType.NEW -> HapticType.TICK
                QuestBannerType.COMPLETED -> HapticType.SUCCESS
                QuestBannerType.FAILED -> HapticType.ALERT
                QuestBannerType.PROGRESS -> HapticType.TICK
            }
            Haptics.pulse(context, type)
        }
    }

    LaunchedEffect(current, isShowing) {
        val banner = current
        if (banner != null && isShowing) {
            delay(QUEST_BANNER_AUTO_DISMISS_MS)
            if (current == banner && isShowing) {
                isShowing = false
            }
        }
    }

    LaunchedEffect(isShowing) {
        if (!isShowing && current != null) {
            delay(180)
            current = null
        }
    }

    val visible = isShowing && current != null

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 56.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
        ) {
            current?.let { banner ->
                QuestBannerCard(
                    banner = banner,
                    accentColor = accentColor,
                    onDismiss = { isShowing = false }
                )
            }
        }
    }
}

@Composable
private fun QuestBannerCard(
    banner: Banner,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val accent = when (banner.type) {
        QuestBannerType.NEW -> accentColor
        QuestBannerType.COMPLETED -> accentColor.copy(alpha = 0.9f)
        QuestBannerType.FAILED -> accentColor.copy(alpha = 0.8f)
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

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 520.dp)
            .semantics { contentDescription = "Quest Banner" },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (banner.type == QuestBannerType.PROGRESS && banner.objectives.isNotEmpty()) {
                    banner.objectives.take(2).forEach { objective ->
                        BannerObjectiveRow(objective = objective, accentColor = accent)
                    }
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss Quest Banner",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
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
            text = objective.text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = if (objective.completed) 0.8f else 1f),
                fontWeight = if (objective.completed) FontWeight.Medium else FontWeight.SemiBold,
                textDecoration = if (objective.completed) TextDecoration.LineThrough else TextDecoration.None
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


