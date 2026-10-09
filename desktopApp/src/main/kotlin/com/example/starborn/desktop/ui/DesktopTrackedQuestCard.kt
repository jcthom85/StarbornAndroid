package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

@Composable
internal fun DesktopTrackedQuestCard(services: DesktopAppServices, ui: ExplorationUiState, blocked: Boolean, onOpen: () -> Unit) {
    val quest = ui.questLogActive.firstOrNull { it.id == ui.trackedQuestId } ?: return
    val runtime by services.questRuntimeManager.state.collectAsState()
    val entry = runtime.activeJournal.firstOrNull { it.id == quest.id }
    val objectives = entry?.objectives.orEmpty()
    val authored = services.questRepository.questById(quest.id)
    val signature = quest.stageIndex to objectives.map { it.id to it.completed }
    var previous by remember(quest.id) { mutableStateOf(signature) }
    val glow = remember(quest.id) { Animatable(0f) }
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    LaunchedEffect(signature) {
        if (signature != previous && !settings.disableFlashes) { glow.snapTo(1f); glow.animateTo(0f, tween(1100)) }
        previous = signature
    }
    val accent = ui.theme?.accent?.takeIf { it.size >= 3 }?.let { Color(it[0], it[1], it[2], it.getOrElse(3) { 1f }) } ?: Color(0xFF80E0FF)
    DesktopFieldMenuTheme(services) {
        DesktopTooltip("Click to inspect in Journal [Alt+4]") {
            Surface(onClick = { services.exploration.openQuestDetails(quest.id); onOpen() }, enabled = !blocked,
                modifier = Modifier.fillMaxWidth().explorationFeedback(!blocked, accent).testTag("hud-objective").semantics { contentDescription = "Open tracked objective in Journal" },
                shape = RoundedCornerShape(12.dp), color = if (settings.highContrastMode) Color.Black else Color(0xFA061018), border = BorderStroke(1.dp, if (settings.highContrastMode) Color.White else accent.copy(alpha = .42f + .25f * glow.value))) {
                Column(Modifier.background(Brush.horizontalGradient(listOf(accent.copy(alpha = .14f + .16f * glow.value), Color.Transparent)))
                    .padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = accent)
                    Text("TRACKED QUEST", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                    if (objectives.isNotEmpty()) Text("${objectives.count { it.completed }}/${objectives.size}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .6f))
                }
                Text(authored?.title ?: quest.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val active = objectives.filterNot { it.completed }
                val displayed = (active.take(2) + objectives.filter { it.completed }.take((3 - active.size.coerceAtMost(2)).coerceAtLeast(0)))
                if (displayed.isNotEmpty()) displayed.forEach { objective ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (objective.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(16.dp),
                            tint = if (objective.completed) Color(0xFFFFC857) else accent)
                        Text(objective.text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = if (objective.completed) .5f else .88f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } else (quest.currentObjective ?: quest.stageTitle)?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .88f))
                }
                if (active.size > 2) Text("+${active.size - 2} more objectives", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .55f))
            }
        }
    }
}
}
