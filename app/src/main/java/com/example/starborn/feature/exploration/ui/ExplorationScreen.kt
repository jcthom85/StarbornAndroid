package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModelFactory
import java.util.Locale
import kotlinx.coroutines.flow.collect

@Composable
fun ExplorationScreen(
    modifier: Modifier = Modifier,
    onEnemySelected: (String) -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenTinkering: () -> Unit = {},
    viewModel: ExplorationViewModel = viewModel(
        factory = ExplorationViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = ExplorationUiState())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> onEnemySelected(event.enemyId)
                is ExplorationEvent.PlayCinematic -> {
                    snackbarHostState.showSnackbar("Cinematic ${event.sceneId} coming soon")
                }
                is ExplorationEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ExplorationEvent.RewardGranted -> {
                    snackbarHostState.showSnackbar(formatRewardMessage(event.reward))
                }
                is ExplorationEvent.ItemGranted -> {
                    val label = "${event.quantity} x ${event.itemName}"
                    snackbarHostState.showSnackbar("Received $label")
                }
                is ExplorationEvent.XpGained -> {
                    snackbarHostState.showSnackbar("Gained ${event.amount} XP")
                }
                is ExplorationEvent.QuestAdvanced -> {
                    val quest = event.questId ?: "Quest"
                    snackbarHostState.showSnackbar("$quest advanced")
                }
                is ExplorationEvent.QuestUpdated -> {
                    snackbarHostState.showSnackbar("Quest log updated")
                }
                is ExplorationEvent.RoomStateChanged -> {
                    snackbarHostState.showSnackbar(formatRoomStateMessage(event.stateKey, event.value))
                }
                is ExplorationEvent.SpawnEncounter -> {
                    snackbarHostState.showSnackbar("Encounter ${event.encounterId ?: "unknown"} triggered")
                }
                is ExplorationEvent.BeginNode -> {
                    snackbarHostState.showSnackbar("Entering node ${event.roomId ?: "unknown"}")
                }
                is ExplorationEvent.TutorialRequested -> Unit
                is ExplorationEvent.GroundItemSpawned -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ExplorationEvent.RoomSearchUnlocked -> {
                    snackbarHostState.showSnackbar(event.note ?: "A hidden stash is now accessible")
                }
                is ExplorationEvent.ItemUsed -> {
                    val message = event.message ?: formatItemUseResult(event.result)
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            uiState.currentRoom?.let { room ->
                val backgroundRes = remember(room.backgroundImage) {
                    val assetName = room.backgroundImage
                        .substringAfterLast("/")
                        .substringBeforeLast(".")
                    context.resources.getIdentifier(
                        assetName,
                        "drawable",
                        context.packageName
                    ).takeIf { it != 0 }
                }
                backgroundRes?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = onOpenInventory) {
                            Text("Inventory")
                        }
                        if (uiState.completedMilestones.contains("ms_tinkering_prompt_active")) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onOpenTinkering) {
                                Text("Tinkering")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.openQuestLog() }) {
                            Text("Quest Log")
                        }
                    }
                    Text(
                        text = room.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.npcs.isNotEmpty()) {
                        Text(
                            text = "Characters",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.npcs.forEach { npc ->
                                Button(onClick = { viewModel.onNpcInteraction(npc) }) {
                                    Text("Talk to $npc")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.enemies.isNotEmpty()) {
                        Text(
                            text = "Enemies",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.enemies.forEach { enemyId ->
                                Button(onClick = { viewModel.engageEnemy(enemyId) }) {
                                    Text("Engage $enemyId")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.actions.isNotEmpty()) {
                        Text(
                            text = "Points of Interest",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.actions.forEach { action ->
                                val (buttonLabel, subtitle) = when (action) {
                                    is ToggleAction -> {
                                        val isActive = uiState.roomState[action.stateKey] ?: false
                                        val label = if (isActive) action.labelOn else action.labelOff
                                        val secondary = action.name
                                            .takeUnless { it.equals(label, ignoreCase = true) }
                                        label to secondary
                                    }
                                    is ContainerAction -> {
                                        val opened = action.stateKey?.let { uiState.roomState[it] == true } ?: false
                                        val primary = if (opened) "Inspect ${action.name}" else "Open ${action.name}"
                                        val secondary = if (opened) "Already searched" else null
                                        primary to secondary
                                    }
                                    else -> action.name to null
                                }
                                Button(onClick = { viewModel.onActionSelected(action) }) {
                                    if (subtitle != null) {
                                        Column {
                                            Text(buttonLabel)
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    } else {
                                        Text(buttonLabel)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.groundItems.isNotEmpty()) {
                        Text(
                            text = "Items Nearby",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.groundItems.forEach { (itemId, quantity) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val label = buildString {
                                        append(viewModel.itemDisplayName(itemId))
                                        if (quantity > 1) append(" ×$quantity")
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Button(onClick = { viewModel.collectGroundItem(itemId) }) {
                                        Text("Take")
                                    }
                                }
                            }
                            Button(onClick = { viewModel.collectAllGroundItems() }) {
                                Text("Take All")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.availableConnections.isNotEmpty()) {
                        Text(
                            text = "Exits",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.availableConnections.keys.forEach { direction ->
                                val isBlocked = uiState.blockedDirections.contains(direction.lowercase(Locale.getDefault()))
                                val directionLabel = buildString {
                                    append(direction.formatDirectionLabel())
                                    if (isBlocked) append(" (Locked)")
                                }
                                Button(onClick = { viewModel.travel(direction) }) {
                                    Text(directionLabel)
                                }
                            }
                        }
                    }

                    uiState.statusMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    if (uiState.activeQuests.isNotEmpty() || uiState.completedQuests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        QuestSummaryCard(
                            activeQuests = uiState.activeQuests,
                            completedQuests = uiState.completedQuests
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "No room data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                )
            }
        }

        uiState.activeDialogue?.let { line ->
            DialogueOverlay(
                line = line,
                onAdvance = { viewModel.advanceDialogue() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        uiState.tutorialPrompt?.let { prompt ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(min = 240.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = prompt.message,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Button(onClick = { viewModel.dismissTutorial() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        uiState.milestonePrompt?.let { prompt ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                color = Color(0xFF263238).copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Milestone",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF80DEEA)
                        )
                        Text(
                            text = prompt.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Button(onClick = { viewModel.dismissMilestonePrompt() }) {
                        Text("Ok")
                    }
                }
            }
        }

        uiState.narrationPrompt?.let { prompt ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 96.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = prompt.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Button(onClick = { viewModel.dismissNarration() }) {
                        Text(if (prompt.tapToDismiss) "Dismiss" else "Close")
                    }
                }
            }
        }

        if (uiState.isQuestLogVisible) {
            QuestLogOverlay(
                activeQuests = uiState.questLogActive,
                completedQuests = uiState.questLogCompleted,
                onClose = { viewModel.closeQuestLog() },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        )
    }
}

private fun String.formatDirectionLabel(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

@Composable
private fun QuestSummaryCard(
    activeQuests: Set<String>,
    completedQuests: Set<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeQuests.isNotEmpty()) {
                Text(
                    text = "Active Quests",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                QuestList(activeQuests)
            }
            if (completedQuests.isNotEmpty()) {
                Text(
                    text = "Completed Quests",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                QuestList(completedQuests, completed = true)
            }
        }
    }
}

private fun formatRewardMessage(reward: EventReward): String {
    val parts = mutableListOf<String>()
    reward.xp?.takeIf { it > 0 }?.let { parts += "$it XP" }
    reward.ap?.takeIf { it > 0 }?.let { parts += "$it AP" }
    reward.credits?.takeIf { it > 0 }?.let { parts += "$it credits" }
    reward.items.filter { it.itemId.isNotBlank() }.forEach { item ->
        val qty = item.quantity ?: 1
        parts += "$qty x ${item.itemId}"
    }
    return if (parts.isEmpty()) {
        "Reward collected"
    } else {
        "Reward: ${parts.joinToString(", ")}"
    }
}

private fun formatRoomStateMessage(stateKey: String, value: Boolean): String {
    val label = stateKey.replace('_', ' ').replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
    }
    val stateText = if (value) "enabled" else "disabled"
    return "$label $stateText"
}

@Composable
private fun QuestLogOverlay(
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(0.85f)
            .fillMaxHeight(0.75f),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Quest Log",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                if (activeQuests.isEmpty() && completedQuests.isEmpty()) {
                    Text(
                        text = "No quests tracked yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    if (activeQuests.isNotEmpty()) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeQuests.forEach { quest ->
                                QuestLogEntry(summary = quest)
                            }
                        }
                    }
                    if (completedQuests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            completedQuests.forEach { quest ->
                                QuestLogEntry(summary = quest)
                            }
                        }
                    }
                }
            }
            Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun QuestLogEntry(summary: QuestSummaryUi) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (summary.completed) Color(0xFF8BC34A) else Color.White
            )
            if (summary.completed) {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8BC34A)
                )
            }
        }
        Text(
            text = summary.summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
        summary.stageTitle?.let { stageTitle ->
            Text(
                text = stageTitle,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
        summary.stageDescription?.let { stageDesc ->
            Text(
                text = stageDesc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        if (summary.objectives.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.objectives.forEach { objective ->
                    Text(
                        text = objective,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun formatItemUseResult(result: ItemUseResult): String = when (result) {
    is ItemUseResult.None -> "Used ${result.item.name}"
    is ItemUseResult.Restore -> buildString {
        append("Restored ")
        val entries = mutableListOf<String>()
        if (result.hp > 0) entries += "${result.hp} HP"
        if (result.rp > 0) entries += "${result.rp} RP"
        append(entries.joinToString(" & ").ifBlank { "item" })
    }
    is ItemUseResult.Damage -> "${result.item.name} dealt ${result.amount} damage"
    is ItemUseResult.Buff -> {
        val buffs = result.buffs.joinToString { "${it.stat}+${it.value}" }
        "Buffs applied: $buffs"
    }
    is ItemUseResult.LearnSchematic -> "Learned schematic ${result.schematicId}"
}

@Composable
private fun QuestList(quests: Set<String>, completed: Boolean = false) {
    val display = quests.asSequence().sorted().take(3).toList()
    display.forEach { questId ->
        Text(
            text = questId,
            style = MaterialTheme.typography.bodySmall,
            color = if (completed) Color(0xFF9EB0A0) else Color.White
        )
    }
    val remaining = quests.size - display.size
    if (remaining > 0) {
        Text(
            text = "+$remaining more",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
