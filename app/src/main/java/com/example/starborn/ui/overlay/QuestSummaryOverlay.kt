package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.SummaryType
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.flow.collect

@Composable
fun QuestSummaryOverlay(
    uiEventBus: UiEventBus,
    isSceneBlocking: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onShowDetails: (String) -> Unit
) {
    var summary by remember { mutableStateOf<UiEvent.ShowQuestSummary?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(uiEventBus) {
        uiEventBus.events.collect { event ->
            if (event is UiEvent.ShowQuestSummary) {
                summary = event
                visible = true
            }
        }
    }

    AnimatedVisibility(
        visible = visible && !isSceneBlocking && summary != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        summary?.let { data ->
            Card(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
                    .padding(16.dp)
                    .semantics { contentDescription = "Quest Summary Popup" },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF061018).copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "QUEST SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    data.entries.forEach { entry ->
                        QuestSummaryEntryRow(
                            type = entry.type,
                            questTitle = entry.questTitle,
                            objectiveTitle = entry.objectiveTitle,
                            accentColor = accentColor,
                            onDetails = {
                                visible = false
                                onShowDetails(entry.questId)
                                summary = null
                            }
                        )
                    }
                    Button(
                        onClick = {
                            visible = false
                            summary = null
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.22f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestSummaryEntryRow(
    type: SummaryType,
    questTitle: String,
    objectiveTitle: String?,
    accentColor: Color,
    onDetails: () -> Unit
) {
    val accent = accentColor
    val icon = when (type) {
        SummaryType.ACCEPTED -> Icons.Filled.Flag
        SummaryType.UPDATED -> Icons.Filled.CheckCircle
        SummaryType.COMPLETED -> Icons.Filled.CheckCircle
        SummaryType.FAILED -> Icons.Filled.Warning
    }
    val label = when (type) {
        SummaryType.ACCEPTED -> "Accepted"
        SummaryType.UPDATED -> "Updated"
        SummaryType.COMPLETED -> "Completed"
        SummaryType.FAILED -> "Failed"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = questTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            objectiveTitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        TextButton(onClick = onDetails) {
            Text("Details", color = accent)
        }
    }
}




