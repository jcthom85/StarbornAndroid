package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import com.example.starborn.desktop.DesktopAppServices

@Composable
internal fun DesktopRuntimeJournalContent(services: DesktopAppServices) {
    val runtime = services.exploration
    val ui by runtime.uiState.collectAsState()
    var category by rememberSaveable { mutableStateOf(if (ui.questDetail?.completed == true) "Completed" else "Active") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(ui.questDetail?.id) }
    val quests = (if (category == "Completed") ui.questLogCompleted else ui.questLogActive)
        .filter { it.title.contains(query, true) || it.summary.contains(query, true) }
        .sortedWith(compareByDescending<com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi> { it.id == ui.trackedQuestId }.thenBy { it.title })
    val selected = quests.firstOrNull { it.id == selectedId } ?: quests.firstOrNull()
    LaunchedEffect(Unit) { services.questPresentations.readJournal() }
    LaunchedEffect(selected?.id, category) {
        if (category == "Active" || category == "Completed") selected?.let { runtime.openQuestDetails(it.id) } ?: runtime.closeQuestDetails()
        else runtime.closeQuestDetails()
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Active", "Completed", "History", "Milestones", "Fishing").forEach { tab ->
                FilterChip(category == tab, onClick = { category = tab; selectedId = null }, label = { Text(tab) })
            }
        }
        if (category == "Active" || category == "Completed") {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search quests") }, singleLine = true)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (quests.isEmpty()) item { Text("No quests match this view.") }
                    items(quests, key = { it.id }) { quest ->
                        Surface(Modifier.fillMaxWidth().clickable { selectedId = quest.id }, shape = RoundedCornerShape(12.dp),
                            color = if (selected?.id == quest.id) Color(0xFF173443) else Color(0xFF101D28),
                            border = BorderStroke(1.dp, if (selected?.id == quest.id) Color(0xFF63E6FF) else Color(0xFF354454))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(quest.title, style = MaterialTheme.typography.titleSmall)
                                Text(if (quest.completed) "Completed" else if (quest.id == ui.trackedQuestId) "TRACKED" else "Stage ${quest.stageIndex + 1} of ${quest.totalStages}",
                                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                selected?.let { quest ->
                    DesktopMenuCard(Modifier.weight(1.4f).fillMaxHeight()) {
                        DesktopMenuScrollPane(Modifier.fillMaxSize().padding(18.dp)) {
                            val authored = services.questRepository.questById(quest.id)
                            Text(if (quest.completed) "COMPLETED" else if (quest.id == ui.trackedQuestId) "TRACKED QUEST" else "ACTIVE QUEST", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelSmall)
                            Text(authored?.title ?: quest.title, style = MaterialTheme.typography.headlineSmall)
                            val summary = authored?.summary?.takeIf { it.isNotBlank() } ?: quest.summary
                            if (summary.isNotBlank()) Text(summary)
                            authored?.description?.takeIf { it.isNotBlank() && it != summary }?.let { Text(it) }
                            authored?.flavor?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                            val detail = ui.questDetail?.takeIf { it.id == quest.id }
                            if (detail == null) Text("Loading quest details...") else {
                                val stages = detail.stages
                                if (stages.isNotEmpty()) stages.forEachIndexed { index, stage ->
                                    HorizontalDivider()
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(shape = CircleShape, color = if (stage.completed) FieldMenuDesign.gold.copy(alpha = .18f) else if (stage.current) FieldMenuDesign.cyan.copy(alpha = .18f) else FieldMenuDesign.panel,
                                            border = BorderStroke(1.dp, if (stage.completed) FieldMenuDesign.gold else if (stage.current) FieldMenuDesign.cyan else FieldMenuDesign.border)) {
                                            Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (stage.completed) FieldMenuDesign.gold else FieldMenuDesign.text)
                                        }
                                        Text(stage.title, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(if (stage.completed) "Completed" else if (stage.current) "Current stage" else "Upcoming", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    stage.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                                    stage.objectives.forEach { objective ->
                                        Row(Modifier.semantics { contentDescription = "${if (objective.completed) "Completed" else "Incomplete"} objective: ${objective.text}" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(if (objective.completed) androidx.compose.material.icons.Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Default.RadioButtonUnchecked, null, Modifier.size(18.dp), tint = if (objective.completed) FieldMenuDesign.gold else FieldMenuDesign.cyan)
                                            Text(objective.text, Modifier.weight(1f), color = if (objective.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                } else {
                                    detail.stageTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                                    detail.stageDescription?.let { Text(it) }
                                    detail.objectives.forEach { objective ->
                                        Text("${if (objective.completed) "Completed" else "Incomplete"}: ${objective.text}")
                                    }
                                }
                                if (detail.rewards.isNotEmpty()) {
                                    HorizontalDivider(); Text("Rewards", style = MaterialTheme.typography.titleMedium)
                                    detail.rewards.forEach { Text(it) }
                                }
                            }
                            if (!quest.completed) Button(onClick = { runtime.toggleQuestTracking(quest.id) }) {
                                Text(if (quest.id == ui.trackedQuestId) "Stop tracking" else "Track this quest")
                            }
                        }
                    }
                }
            }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (category == "History") {
                if (ui.questLogEntries.isEmpty()) item { Text("Your quest history will appear here.") }
                items(ui.questLogEntries.sortedByDescending { it.timestamp }) { entry ->
                    DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.questTitle?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        Text(entry.message)
                    } }
                }
            } else if (category == "Fishing") {
                if (ui.fishingJournal.isEmpty()) item { Text("No fishing records yet.") }
                items(ui.fishingJournal, key = { it.zoneId }) { zone ->
                    DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(zone.name, style = MaterialTheme.typography.titleMedium)
                        zone.species.forEach { species ->
                            Text("${if (species.caught) species.name else "Undiscovered species"}: ${if (species.clean) "Clean catch" else if (species.caught) "Caught" else "Not caught"}")
                        }
                    } }
                }
            } else {
                if (ui.milestoneHistory.isEmpty()) item { Text("Your journey's milestones will appear here.") }
                items(ui.milestoneHistory) { milestone -> DesktopMenuCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(milestone.title, color = MaterialTheme.colorScheme.primary); Text(milestone.message)
                    }
                } }
            }
        }
    }
}

@Composable
internal fun DesktopRuntimeMapContent(services: DesktopAppServices) {
    DesktopMapDeck(services = services)
}

