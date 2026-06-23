package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi

enum class QuestJournalPage { ACTIVE, COMPLETED }

@Composable
fun JournalTabContent(
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    accentColor: Color,
    borderColor: Color,
    onQuestSelected: (String) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(QuestJournalPage.ACTIVE) }

    MenuSectionCard(
        title = "Quest Journal",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        QuestJournalToggle(
            current = page,
            onSelect = { page = it },
            accentColor = accentColor,
            borderColor = borderColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (page) {
            QuestJournalPage.ACTIVE -> {
                val items = buildList {
                    trackedQuest?.let { add(it) }
                    addAll(activeQuests.filter { it.id != trackedQuest?.id })
                }
                QuestListPanel(
                    quests = items,
                    emptyMessage = "No active quests yet.",
                    accentColor = accentColor,
                    borderColor = borderColor,
                    onQuestSelected = onQuestSelected
                )
            }
            QuestJournalPage.COMPLETED -> {
                QuestListPanel(
                    quests = completedQuests,
                    emptyMessage = "No completed quests yet.",
                    accentColor = accentColor,
                    borderColor = borderColor,
                    onQuestSelected = onQuestSelected
                )
            }
        }
    }
}

@Composable
fun QuestJournalToggle(
    current: QuestJournalPage,
    onSelect: (QuestJournalPage) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(50.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JournalToggleButton(
            label = "Active",
            selected = current == QuestJournalPage.ACTIVE,
            accentColor = accentColor,
            onClick = { onSelect(QuestJournalPage.ACTIVE) },
            modifier = Modifier.weight(1f)
        )
        JournalToggleButton(
            label = "Completed",
            selected = current == QuestJournalPage.COMPLETED,
            accentColor = accentColor,
            onClick = { onSelect(QuestJournalPage.COMPLETED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun JournalToggleButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .clickable { onClick() },
        color = background,
        shape = RoundedCornerShape(40.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun QuestListPanel(
    quests: List<QuestSummaryUi>,
    emptyMessage: String,
    accentColor: Color,
    borderColor: Color,
    onQuestSelected: (String) -> Unit
) {
    if (quests.isEmpty()) {
        Text(
            text = emptyMessage,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(quests, key = { it.id }) { quest ->
            QuestJournalRow(
                quest = quest,
                accentColor = accentColor,
                borderColor = borderColor,
                onClick = { onQuestSelected(quest.id) }
            )
        }
    }
}

@Composable
fun QuestJournalRow(
    quest: QuestSummaryUi,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val icon = if (quest.completed) Icons.Filled.CheckCircle else Icons.Filled.Flag
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.38f)),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = if (quest.completed) 0.12f else 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = if (quest.completed) 0.72f else 0.9f),
                modifier = Modifier.size(18.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (quest.completed) "COMPLETED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor.copy(alpha = if (quest.completed) 0.62f else 0.82f)
                    )
                }
                quest.stageTitle?.takeIf { it.isNotBlank() && !quest.completed }?.let { stage ->
                    Text(
                        text = stage,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val objectives = quest.objectives.take(3)
                if (objectives.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        objectives.forEach { objective ->
                            QuestObjectiveLine(
                                text = objective,
                                completed = quest.completed,
                                accentColor = accentColor
                            )
                        }
                    }
                } else {
                    quest.summary.takeIf { it.isNotBlank() }?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestObjectiveLine(
    text: String,
    completed: Boolean,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) accentColor.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.56f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = if (completed) 0.62f else 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuestJournalSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title.uppercase(),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
