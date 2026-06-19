package com.example.starborn.feature.exploration.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            val shape = RoundedCornerShape(16.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable { onQuestSelected(quest.id) },
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                shape = shape
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    quest.stageTitle?.takeIf { it.isNotBlank() }?.let { stage ->
                        Text(
                            text = stage,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor.copy(alpha = 0.85f)
                        )
                    }
                    val objectives = quest.objectives.take(2)
                    if (objectives.isNotEmpty()) {
                        objectives.forEach { obj ->
                            Text(
                                text = "• $obj",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    } else {
                        quest.summary.takeIf { it.isNotBlank() }?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }
                    if (quest.completed) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
