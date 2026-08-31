package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Quest

private val NeonCyan = Color(0xFF00F5D4)
private val NeonAmber = Color(0xFFFFB703)
private val NeonPink = Color(0xFFFF007F)
private val GlassDark = Color(0xFF10141E)
private val PanelBorder = Color(0xFF222D42)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)

enum class QuestFilterCategory {
    ALL, MAIN, SIDE
}

@Composable
fun DesktopJournalDialog(
    services: DesktopAppServices,
    onDismiss: () -> Unit
) {
    val allQuests = remember { services.questRepository.allQuests().toList() }
    var filterCategory by remember { mutableStateOf(QuestFilterCategory.ALL) }
    var selectedQuestId by remember { mutableStateOf(allQuests.firstOrNull()?.id) }

    val filteredQuests = remember(filterCategory, allQuests) {
        when (filterCategory) {
            QuestFilterCategory.ALL -> allQuests
            QuestFilterCategory.MAIN -> allQuests.filter { it.id.startsWith("mq_") || it.id.startsWith("main_") || !it.id.startsWith("sq_") }
            QuestFilterCategory.SIDE -> allQuests.filter { it.id.startsWith("sq_") || it.id.startsWith("side_") }
        }
    }

    val selectedQuest = allQuests.firstOrNull { it.id == selectedQuestId } ?: filteredQuests.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(960.dp)
                .height(640.dp),
            shape = RoundedCornerShape(18.dp),
            color = GlassDark,
            border = BorderStroke(1.5.dp, NeonAmber.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MISSION LOG & DIRECTIVES TERMINAL // JOURNAL [J]",
                            color = NeonAmber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tracking ${allQuests.size} Planetary Missions • Tactical Field Intelligence",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Filter Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopJournalTabButton("ALL DIRECTIVES (${allQuests.size})", filterCategory == QuestFilterCategory.ALL) { filterCategory = QuestFilterCategory.ALL }
                    DesktopJournalTabButton("MAIN OBJECTIVES", filterCategory == QuestFilterCategory.MAIN) { filterCategory = QuestFilterCategory.MAIN }
                    DesktopJournalTabButton("SIDE CONTRACTS", filterCategory == QuestFilterCategory.SIDE) { filterCategory = QuestFilterCategory.SIDE }
                }

                // Main Split View
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Quest List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF090C12))
                            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredQuests) { quest ->
                            val isSelected = quest.id == selectedQuest?.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33FFB703) else Color(0xFF121722))
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) NeonAmber else PanelBorder),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedQuestId = quest.id }
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = quest.title,
                                            color = if (isSelected) NeonAmber else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val isMain = quest.id.startsWith("mq_") || quest.id.startsWith("main_") || !quest.id.startsWith("sq_")
                                        Text(
                                            text = if (isMain) "MAIN DIRECTIVE" else "SIDE CONTRACT",
                                            color = if (isMain) NeonPink else NeonCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = quest.description.ifBlank { quest.summary },
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Quest Briefing Telemetry
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF090C12))
                            .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(12.dp))
                            .padding(20.dp)
                    ) {
                        if (selectedQuest != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DIRECTIVE DOSSIER",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )
                                        Text(
                                            text = "ID: ${selectedQuest.id}",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Text(
                                        text = selectedQuest.title,
                                        color = TextWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    Text(
                                        text = selectedQuest.description.ifBlank { selectedQuest.summary },
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "TACTICAL OBJECTIVES & STAGES",
                                        color = NeonAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        selectedQuest.stages.forEachIndexed { idx, stage ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF131A27))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "◆ Stage ${idx + 1}: ${stage.title.ifBlank { stage.description.ifBlank { stage.id } }}",
                                                    color = TextWhite,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Reward Footer
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141F30))
                                        .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "REWARDS:",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Credits, XP & Milestone Progression",
                                            color = NeonAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopJournalTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonAmber.copy(alpha = 0.18f) else Color(0xFF141924))
            .border(
                BorderStroke(1.dp, if (isSelected) NeonAmber else PanelBorder),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonAmber else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
