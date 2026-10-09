package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.ui.events.*
import kotlinx.coroutines.delay

/** Survives exploration leaving composition (combat, hubs and side activities). */
class DesktopQuestPresentationState {
    sealed interface Important {
        data class Detail(val event: UiEvent.ShowQuestDetail) : Important
        data class Summary(val event: UiEvent.ShowQuestSummary) : Important
    }
    val important = mutableStateListOf<Important>()
    val updates = mutableStateListOf<UiEvent.ShowQuestBanner>()
    val toasts = mutableStateListOf<UiEvent.ShowToast>()
    private val recentUpdates = mutableMapOf<String, Long>()
    private val started = mutableSetOf<Important>()
    var journalBadge by mutableIntStateOf(0)
        private set

    fun accept(event: UiEvent, now: Long = System.nanoTime() / 1_000_000) {
        when (event) {
            is UiEvent.ShowQuestDetail -> if (important.none {
                it is Important.Detail && it.event.questId == event.questId && it.event.type == event.type
            }) important.add(Important.Detail(event))
            is UiEvent.ShowQuestSummary -> if (event.entries.isNotEmpty() && important.none { it is Important.Summary && it.event == event }) important.add(Important.Summary(event))
            is UiEvent.ShowQuestBanner -> if (event.type == QuestBannerType.PROGRESS) {
                val key = "${event.type}:${event.questId}"
                if (recentUpdates[key]?.let { now - it <= 2000 } != true) {
                    recentUpdates[key] = now
                    updates.add(event)
                }
                recentUpdates.entries.removeAll { now - it.value > 2000 }
            }
            is UiEvent.ShowToast -> if (toasts.none { it.id == event.id && it.text == event.text }) toasts.add(event)
            is UiEvent.JournalBadgeDelta -> journalBadge = (journalBadge + event.delta).coerceAtLeast(0)
        }
    }
    fun markStarted(entry: Important) = started.add(entry)
    fun dismissImportant() { important.firstOrNull()?.let { started.remove(it); important.removeAt(0) } }
    fun readJournal() { journalBadge = 0 }
    fun clear() { important.clear(); updates.clear(); toasts.clear(); recentUpdates.clear(); started.clear(); journalBadge = 0 }
}

private fun questHeading(type: QuestBannerType) = when (type) {
    QuestBannerType.NEW -> "New Quest"
    QuestBannerType.COMPLETED -> "Quest Completed"
    QuestBannerType.FAILED -> "Quest Failed"
    QuestBannerType.PROGRESS -> "Quest Updated"
}

@Composable
internal fun DesktopQuestPresentation(services: DesktopAppServices, state: DesktopQuestPresentationState,
    sceneBlocked: Boolean, onDetails: (String) -> Unit) {
    val current = state.important.firstOrNull()
    val visible = current != null && !sceneBlocked
    val focus = remember { FocusRequester() }
    val ui by services.exploration.uiState.collectAsState()
    val accent = ui.theme?.accent?.takeIf { it.size >= 3 }?.let { Color(it[0], it[1], it[2], it.getOrElse(3) { 1f }) } ?: Color(0xFF80E0FF)
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    LaunchedEffect(current, visible) {
        if (visible && current != null) {
            withFrameNanos { }
            focus.requestFocus()
            if (state.markStarted(current) && current is DesktopQuestPresentationState.Important.Detail) {
                services.exploration.playQuestPresentationCue(when (current.event.type) {
                    QuestBannerType.NEW -> "quest_new"
                    QuestBannerType.COMPLETED -> "quest_complete"
                    QuestBannerType.PROGRESS -> "quest_update"
                    QuestBannerType.FAILED -> "error"
                })
            }
        }
    }
    val sweep = remember(current) { Animatable(-1.55f) }
    LaunchedEffect(current, visible, settings.disableFlashes) {
        if (visible && !settings.disableFlashes && current is DesktopQuestPresentationState.Important.Detail && current.event.type == QuestBannerType.NEW) {
            while (true) {
                sweep.snapTo(-1.55f)
                sweep.animateTo(2.55f, tween(2300, easing = LinearEasing))
                delay(850)
            }
        }
    }
    Box(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent {
        if (visible && it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.Escape)) { state.dismissImportant(); true } else false
    }.focusable()) {
        AnimatedVisibility(visible, enter = slideInVertically { it / 3 } + fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center) {
                Surface(Modifier.padding(24.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = 700.dp), shape = RoundedCornerShape(14.dp), color = FieldMenuDesign.panel,
                    border = BorderStroke(1.dp, accent.copy(alpha = .5f))) {
                    Column(Modifier.background(Brush.horizontalGradient(listOf(accent.copy(alpha = .18f), Color.Transparent)))
                        .drawWithContent {
                            drawContent()
                            if (!settings.disableFlashes && current is DesktopQuestPresentationState.Important.Detail && current.event.type == QuestBannerType.NEW) {
                                val x = size.width * sweep.value
                                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .10f), Color.Transparent), Offset(x - size.width * .2f, 0f), Offset(x + size.width * .2f, size.height)))
                            }
                        }.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val detail = (current as? DesktopQuestPresentationState.Important.Detail)?.event
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (detail?.type) { QuestBannerType.NEW -> Icons.Default.Star; QuestBannerType.FAILED -> Icons.Default.Warning; else -> Icons.Default.CheckCircle }, null, tint = accent)
                            Spacer(Modifier.width(10.dp))
                            Text(detail?.type?.let(::questHeading) ?: "Quest Summary", Modifier.weight(1f), color = accent, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = state::dismissImportant) { Icon(Icons.Default.Close, "Dismiss quest popup") }
                        }
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (current) {
                                is DesktopQuestPresentationState.Important.Detail -> {
                                    Text(current.event.questTitle, style = MaterialTheme.typography.headlineSmall)
                                    Text(current.event.summary)
                                    current.event.objectives.forEach { line ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(if (current.event.type == QuestBannerType.COMPLETED) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(18.dp), tint = accent)
                                            Text(line, Modifier.weight(1f))
                                        }
                                    }
                                }
                                is DesktopQuestPresentationState.Important.Summary -> current.event.entries.forEach { entry ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${entry.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${entry.questTitle}", color = accent)
                                        entry.objectiveTitle?.let { Text(it) }
                                        TextButton(onClick = { state.dismissImportant(); onDetails(entry.questId) }) { Text("Details") }
                                    }
                                }
                                null -> Unit
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            detail?.let { TextButton(onClick = { state.dismissImportant(); onDetails(it.questId) }) { Text("Details") } }
                            Button(onClick = state::dismissImportant) { Text("Continue") }
                        }
                    }
                }
            }
        }
        val update = state.updates.firstOrNull()
        val showUpdate = update != null && !sceneBlocked && current == null
        var updateShowing by remember(update) { mutableStateOf(true) }
        LaunchedEffect(update, showUpdate) {
            if (showUpdate) { delay(3200); updateShowing = false }
        }
        AnimatedVisibility(showUpdate && updateShowing, Modifier.align(Alignment.TopCenter).padding(top = 20.dp, start = 24.dp, end = 24.dp),
            enter = slideInVertically(tween(220, easing = LinearOutSlowInEasing)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180, easing = FastOutLinearInEasing)) { -it } + fadeOut(tween(180))) {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = .5f))) {
                Row(Modifier.padding(18.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Quest Updated", color = accent, style = MaterialTheme.typography.labelLarge)
                        Text(update?.questTitle.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        update?.objectives?.forEach { objective ->
                            Text((if (objective.completed) "✓ " else "○ ") + when (objective.role) {
                                QuestObjectiveRole.JUST_COMPLETED -> "Completed: ${objective.text}"
                                QuestObjectiveRole.NEXT -> "Next: ${objective.text}"
                                QuestObjectiveRole.STANDARD -> objective.text
                            }, color = if (objective.completed) FieldMenuDesign.textMuted else FieldMenuDesign.text)
                        }
                        update?.remainingObjectiveCount?.takeIf { it > 0 }?.let { Text("+$it more objectives", color = FieldMenuDesign.textMuted) }
                    }
                    IconButton(onClick = { updateShowing = false }) { Icon(Icons.Default.Close, "Dismiss quest update") }
                }
            }
        }
        LaunchedEffect(updateShowing, showUpdate) {
            if (!updateShowing && showUpdate) { delay(180); if (state.updates.firstOrNull() == update) state.updates.removeAt(0) }
        }
        val toast = state.toasts.firstOrNull()
        val showToast = toast != null && !sceneBlocked && current == null && update == null
        LaunchedEffect(toast, showToast) { if (showToast) { delay(4000); if (state.toasts.firstOrNull() == toast) state.toasts.removeAt(0) } }
        if (showToast) Surface(Modifier.align(Alignment.BottomCenter).padding(24.dp).widthIn(max = 560.dp), color = FieldMenuDesign.panel, shape = RoundedCornerShape(10.dp)) {
            Text(toast!!.text, Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
