package com.example.starborn.ui.overlay

import com.example.starborn.ui.components.maestroTag
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.SummaryType
import com.example.starborn.ui.events.UiEvent

@Composable
internal fun QuestSummaryCard(
    data: UiEvent.ShowQuestSummary,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onShowDetails: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 520.dp)
            .semantics { contentDescription = "Quest Summary Popup" },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = .38f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061018)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("QUEST SUMMARY", style = MaterialTheme.typography.labelSmall, color = accentColor)
            data.entries.forEach { entry ->
                QuestSummaryEntryRow(entry.questId, entry.type, entry.questTitle, entry.objectiveTitle,
                    accentColor, onDetails = { onShowDetails(entry.questId) })
            }
            Button(onClick = onDismiss, modifier = Modifier.maestroTag("quest-summary-continue").align(Alignment.End)) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun QuestSummaryEntryRow(
    questId: String,
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
        modifier = Modifier.fillMaxWidth().maestroTag("quest-summary-${type.name.lowercase()}-$questId"),
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




