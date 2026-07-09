package com.example.starborn.feature.exploration.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi

/**
 * Compact always-visible chip with the tracked quest's next objective.
 * Tapping opens the quest log.
 */
@Composable
fun ObjectiveHud(
    quest: QuestSummaryUi?,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (quest == null) return
    val incompleteMarker = 0x25CB.toChar().toString()
    val completeMarker = 0x2713.toChar().toString()
    val firstIncompleteObjective = quest.objectives
        .firstOrNull { it.startsWith(incompleteMarker) }
        ?.removePrefix(incompleteMarker)
        ?.trim()
        ?: quest.objectives
            .firstOrNull { !it.startsWith(completeMarker) }
            ?.trimStart(0x25CB.toChar(), 0x2713.toChar())
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    val objective = firstIncompleteObjective
        ?: quest.stageDescription
        ?: quest.stageTitle
    Surface(
        onClick = onClick,
        color = Color(0xFF061018).copy(alpha = if (isDark) 0.76f else 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.72f else 0.42f)),
        modifier = modifier
            .widthIn(max = 620.dp)
            .semantics {
                contentDescription = listOfNotNull("Current objective", quest.title, objective)
                    .joinToString(": ")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Flag,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = quest.title,
                    color = accentColor.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!objective.isNullOrBlank()) {
                    Text(
                        text = objective,
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
