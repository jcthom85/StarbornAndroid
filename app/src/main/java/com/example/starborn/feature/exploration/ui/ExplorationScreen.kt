package com.example.starborn.feature.exploration.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.CookingAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.FirstAidAction
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.serviceTag
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import com.example.starborn.feature.exploration.viewmodel.BlockedPrompt
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.CombatOutcomeUi
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.feature.exploration.viewmodel.LevelUpPrompt
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import com.example.starborn.feature.exploration.viewmodel.MinimapService
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.NarrationPrompt
import com.example.starborn.feature.exploration.viewmodel.QuestLogEntryUi
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import com.example.starborn.feature.exploration.viewmodel.MilestoneBandUi
import com.example.starborn.feature.exploration.viewmodel.RadialMenuAction
import com.example.starborn.feature.exploration.viewmodel.RadialMenuUi
import com.example.starborn.feature.exploration.viewmodel.ProgressionSummaryUi
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.tutorial.TutorialEntry
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    audioCuePlayer: AudioCuePlayer,
    modifier: Modifier = Modifier,
    onEnemySelected: (List<String>) -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenTinkering: (String?) -> Unit = {},
    onOpenCooking: (String?) -> Unit = {},
    onOpenFirstAid: (String?) -> Unit = {},
    onOpenFishing: (String?) -> Unit = {},
    onOpenShop: (String) -> Unit = {},
    fxEvents: Flow<String>? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = ExplorationUiState())
    val snackbarHostState = remember { SnackbarHostState() }
    val fxBursts = remember { mutableStateListOf<UiFxBurst>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> onEnemySelected(event.enemyIds)
                is ExplorationEvent.PlayCinematic -> snackbarHostState.showSnackbar("Cinematic ${event.sceneId} queued")
                is ExplorationEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is ExplorationEvent.RewardGranted -> snackbarHostState.showSnackbar(formatRewardMessage(event.reward))
                is ExplorationEvent.ItemGranted -> snackbarHostState.showSnackbar("Received ${event.quantity} x ${event.itemName}")
                is ExplorationEvent.XpGained -> snackbarHostState.showSnackbar("Gained ${event.amount} XP")
                is ExplorationEvent.QuestAdvanced -> snackbarHostState.showSnackbar("${event.questId ?: "Quest"} advanced")
                is ExplorationEvent.QuestUpdated -> snackbarHostState.showSnackbar("Quest log updated")
                is ExplorationEvent.RoomStateChanged -> snackbarHostState.showSnackbar(formatRoomStateMessage(event.stateKey, event.value))
                is ExplorationEvent.SpawnEncounter -> snackbarHostState.showSnackbar("Encounter ${event.encounterId ?: "unknown"} triggered")
                is ExplorationEvent.BeginNode -> snackbarHostState.showSnackbar("Entering node ${event.roomId ?: "unknown"}")
                is ExplorationEvent.TutorialRequested -> Unit
                is ExplorationEvent.GroundItemSpawned -> snackbarHostState.showSnackbar(event.message)
                is ExplorationEvent.RoomSearchUnlocked -> snackbarHostState.showSnackbar(event.note ?: "A hidden stash is now accessible")
                is ExplorationEvent.ItemUsed -> snackbarHostState.showSnackbar(event.message ?: formatItemUseResult(event.result))
                is ExplorationEvent.OpenTinkering -> onOpenTinkering(event.shopId)
                is ExplorationEvent.OpenCooking -> onOpenCooking(event.stationId)
                is ExplorationEvent.OpenFirstAid -> onOpenFirstAid(event.stationId)
                is ExplorationEvent.OpenFishing -> onOpenFishing(event.zoneId)
                is ExplorationEvent.OpenShop -> onOpenShop(event.shopId)
                is ExplorationEvent.CombatOutcome -> snackbarHostState.showSnackbar(event.message)
                is ExplorationEvent.AudioCommands -> audioCuePlayer.execute(event.commands)
            }
        }
    }

    LaunchedEffect(fxEvents) {
        fxEvents?.let { stream ->
            stream.collect { fxId ->
                val visual = fxVisualInfo(fxId)
                fxBursts += UiFxBurst(System.nanoTime(), visual.label, visual.color)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.main_menu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderRow(
                roomTitle = uiState.currentRoom?.title ?: "Unknown area",
                onInventory = onOpenInventory,
                onTinkering = { onOpenTinkering(null) },
                onCooking = { onOpenCooking(null) },
                onFirstAid = { onOpenFirstAid(null) },
                onFishing = { onOpenFishing(uiState.currentRoom?.id) },
                onServices = { viewModel.openServicesMenu() },
                summarisedProgress = uiState.progressionSummary
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.currentRoom?.let { room ->
                        SectionCard(title = room.title.ifBlank { "Current Area" }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = room.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                if (room.state.isNotEmpty()) {
                                    val stateSummary = room.state.entries.joinToString(separator = " · ") { (key, value) ->
                                        val flag = (value as? Boolean) ?: false
                                        "${key.replace('_', ' ')}: ${if (flag) "ON" else "off"}"
                                    }
                                    Text(
                                        text = stateSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    MinimapWidget(
                        minimap = uiState.minimap,
                        onLegend = { viewModel.openMinimapLegend() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.actions.isNotEmpty()) {
                        SectionCard(title = "Points of Interest") {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.actions) { action ->
                                    val hint = uiState.actionHints[action.actionKey()]
                                    val service = action.serviceTag()
                                    val label = buildString {
                                        append(action.name)
                                        if (service != null) append(" · $service")
                                    }
                                    val subtitle = hint?.message
                                    Button(
                                        onClick = { viewModel.onActionSelected(action) },
                                        enabled = hint?.locked != true,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(label)
                                            subtitle?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.groundItems.isNotEmpty()) {
                        SectionCard(title = "Items Nearby") {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(uiState.groundItems.toList()) { (itemId, quantity) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "$itemId ×$quantity", color = Color.White)
                                        Button(onClick = { viewModel.collectGroundItem(itemId) }) {
                                            Text("Take")
                                        }
                                    }
                                }
                                item {
                                    Button(onClick = { viewModel.collectAllGroundItems() }) {
                                        Text("Take All")
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.statusMessage?.let { message ->
                        SectionCard(title = "Status") {
                            Text(text = message, color = Color.White)
                        }
                    }

                    val partyMembers = uiState.partyStatus.members
                    if (partyMembers.isNotEmpty()) {
                        SectionCard(title = "Party") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                partyMembers.forEach { member ->
                                    Column {
                                        Text(
                                            text = "${member.name} · Lv ${member.level}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            member.hpLabel?.let {
                                                Text(
                                                    text = "HP: $it",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                            member.rpLabel?.let {
                                                Text(
                                                    text = "RP: $it",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                            Text(
                                                text = "XP: ${member.xpLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val trackedQuest = uiState.questLogActive.firstOrNull { it.id == uiState.trackedQuestId }
                    val hasQuestData = trackedQuest != null ||
                        uiState.questLogActive.isNotEmpty() ||
                        uiState.questLogCompleted.isNotEmpty() ||
                        uiState.failedQuests.isNotEmpty() ||
                        uiState.questLogEntries.isNotEmpty()
                    if (hasQuestData) {
                        QuestSummaryCard(
                            trackedQuest = trackedQuest,
                            activeQuests = uiState.questLogActive,
                            completedQuests = uiState.questLogCompleted,
                            failedQuests = uiState.failedQuests,
                            questLog = uiState.questLogEntries
                        )
                    }

                    val recentMilestones = uiState.milestoneHistory.takeLast(5).asReversed()
                    if (recentMilestones.isNotEmpty()) {
                        MilestoneHistoryCard(recentMilestones)
                    }
                }
            }
        }

        uiState.activeDialogue?.let { dialogue ->
            DialogueOverlay(
                dialogue = dialogue,
                choices = uiState.dialogueChoices,
                onAdvance = { viewModel.advanceDialogue() },
                onChoice = { viewModel.onDialogueChoiceSelected(it) },
                onPlayVoice = { viewModel.onDialogueVoiceRequested(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        uiState.shopGreeting?.let { greeting ->
            ShopGreetingOverlay(
                greeting = greeting,
                onChoice = { viewModel.onShopChoiceSelected(it) },
                onDismiss = { viewModel.dismissShopGreeting() },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        uiState.radialMenu?.let { menu ->
            RadialMenuOverlay(
                menu = menu,
                onSelect = { id ->
                    when (viewModel.selectRadialMenuItem(id)) {
                        RadialMenuAction.Inventory -> onOpenInventory()
                        RadialMenuAction.Milestones -> Unit
                        RadialMenuAction.None -> Unit
                    }
                },
                onDismiss = { viewModel.dismissServicesMenu() }
            )
        }

        if (uiState.isMilestoneGalleryVisible) {
            MilestoneGalleryOverlay(
                history = uiState.milestoneHistory,
                onClose = { viewModel.closeMilestoneGallery() }
            )
        }

        UIPromptOverlay(
            prompt = uiState.prompt,
            onDismiss = { viewModel.dismissPrompt() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        CraftingFxOverlay(
            bursts = fxBursts,
            onExpired = { id -> fxBursts.removeAll { it.id == id } },
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        )
    }
}

@Composable
private fun HeaderRow(
    roomTitle: String,
    onInventory: () -> Unit,
    onTinkering: () -> Unit,
    onCooking: () -> Unit,
    onFirstAid: () -> Unit,
    onFishing: () -> Unit,
    onServices: () -> Unit,
    summarisedProgress: ProgressionSummaryUi
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = roomTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onInventory) { Text("Inventory") }
            Button(onClick = onTinkering) { Text("Tinkering") }
            Button(onClick = onCooking) { Text("Cooking") }
            Button(onClick = onFirstAid) { Text("First Aid") }
            Button(onClick = onFishing) { Text("Fishing") }
            Button(onClick = onServices) { Text("Services") }
        }
        Text(
            text = buildString {
                append("Level ${summarisedProgress.playerLevel}")
                append(" • ${summarisedProgress.creditsLabel}")
                summarisedProgress.xpToNextLabel?.let { append(" • $it") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun MinimapWidget(
    minimap: MinimapUiState?,
    onLegend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.2f))
            .padding(12.dp),
        color = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Local Map", color = Color.White, fontWeight = FontWeight.SemiBold)
                Button(onClick = onLegend) { Text("Legend") }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                val state = minimap ?: return@Canvas
                val cellSize = size.minDimension / 4f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                state.cells.forEach { cell ->
                    val cx = centerX + cell.offsetX * cellSize
                    val cy = centerY - cell.offsetY * cellSize
                    val baseColor = when {
                        cell.isCurrent -> Color(0xFFFFD54F)
                        cell.visited -> Color(0xFF64B5F6)
                        cell.discovered -> Color(0xFF1F2F3C)
                        else -> Color(0xFF0A0F14)
                    }
                    drawRect(
                        color = baseColor,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - cellSize / 2f, cy - cellSize / 2f),
                        size = androidx.compose.ui.geometry.Size(cellSize * 0.9f, cellSize * 0.9f)
                    )
                    if (cell.services.isNotEmpty()) {
                        val offsets = serviceOffsets(cell.services.size, cellSize * 0.25f)
                        cell.services.sortedBy { it.ordinal }.forEachIndexed { index, service ->
                            drawServiceGlyph(
                                service = service,
                                centerX = cx + offsets[index].first,
                                centerY = cy + offsets[index].second,
                                size = cellSize * 0.18f
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun serviceOffsets(count: Int, spacing: Float): List<Pair<Float, Float>> = when (count) {
    1 -> listOf(0f to 0f)
    2 -> listOf(-spacing to 0f, spacing to 0f)
    3 -> listOf(-spacing to 0f, 0f to -spacing * 0.9f, spacing to 0f)
    else -> listOf(
        -spacing to -spacing,
        spacing to -spacing,
        -spacing to spacing,
        spacing to spacing
    )
}

private fun DrawScope.drawServiceGlyph(service: MinimapService, centerX: Float, centerY: Float, size: Float) {
    val center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    val color = minimapServiceColor(service)
    when (service) {
        MinimapService.SHOP -> {
            drawCircle(color, radius = size, center = center, style = Stroke(width = size * 0.6f))
            drawLine(
                color = color,
                start = center.copy(y = center.y - size * 0.6f),
                end = center.copy(y = center.y + size * 0.6f),
                strokeWidth = size * 0.3f
            )
        }
        MinimapService.COOKING -> {
            val path = Path().apply {
                moveTo(center.x - size, center.y + size)
                lineTo(center.x + size, center.y + size)
                lineTo(center.x, center.y - size)
                close()
            }
            drawPath(path, color)
        }
        MinimapService.FIRST_AID -> {
            drawRect(
                color = color,
                topLeft = center.copy(x = center.x - size * 0.3f, y = center.y - size),
                size = androidx.compose.ui.geometry.Size(size * 0.6f, size * 2f)
            )
            drawRect(
                color = color,
                topLeft = center.copy(x = center.x - size, y = center.y - size * 0.3f),
                size = androidx.compose.ui.geometry.Size(size * 2f, size * 0.6f)
            )
        }
        MinimapService.TINKERING -> {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size, center.y)
                lineTo(center.x, center.y + size)
                lineTo(center.x - size, center.y)
                close()
            }
            drawPath(path, color, style = Stroke(width = size * 0.3f))
        }
    }
}

@Composable
private fun RadialMenuOverlay(
    menu: RadialMenuUi,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = menu.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            BoxWithConstraints(
                modifier = Modifier.size(280.dp)
            ) {
                val density = LocalDensity.current
                val smallerSide = if (maxWidth < maxHeight) maxWidth else maxHeight
                val radiusDp = (smallerSide / 2f) - 56.dp
                val safeRadiusPx = with(density) { (if (radiusDp > 32.dp) radiusDp else 32.dp).toPx() }
                Box(modifier = Modifier.fillMaxSize()) {
                    val itemCount = menu.items.size.coerceAtLeast(1)
                    menu.items.forEachIndexed { index, item ->
                        val angle = (-90.0 + index * (360.0 / itemCount)) * (PI / 180.0)
                        val offsetX = (cos(angle) * safeRadiusPx).roundToInt()
                        val offsetY = (sin(angle) * safeRadiusPx).roundToInt()
                        FilledTonalButton(
                            onClick = { onSelect(item.id) },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset(offsetX, offsetY) }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(item.label, textAlign = TextAlign.Center)
                                item.description?.let { desc ->
                                    Text(
                                        desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestSummaryCard(
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    failedQuests: Set<String>,
    questLog: List<QuestLogEntryUi>
) {
    SectionCard(title = "Quest Log") {
        trackedQuest?.let { quest ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Stage ${quest.stageIndex + 1} of ${quest.totalStages}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (!quest.stageDescription.isNullOrBlank()) {
                    Text(
                        text = quest.stageDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                quest.objectives.take(3).forEach { objective ->
                    Text(objective, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                }
                val remainingObjectives = quest.objectives.size - 3
                if (remainingObjectives > 0) {
                    Text("+$remainingObjectives more objectives", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (questLog.isNotEmpty()) {
            Text("Recent Progress", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            questLog.take(3).forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(entry.message, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    entry.stageTitle?.let { stageTitle ->
                        Text(stageTitle, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        val otherActive = activeQuests.filter { it.id != trackedQuest?.id }
        if (otherActive.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Active Quests", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            otherActive.take(3).forEach { quest ->
                Text("• ${quest.title}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            if (otherActive.size > 3) {
                Text("+${otherActive.size - 3} more", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (failedQuests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Failed", color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
            failedQuests.take(2).forEach { questId ->
                Text("• $questId", color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            if (failedQuests.size > 2) {
                Text("+${failedQuests.size - 2} more", color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (completedQuests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Completed", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            completedQuests.take(3).forEach { quest ->
                Text("• ${quest.title}", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
            if (completedQuests.size > 3) {
                Text("+${completedQuests.size - 3} more", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
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
    return if (parts.isEmpty()) "Reward collected" else "Reward: ${parts.joinToString(", ")}"
}

private fun formatRoomStateMessage(stateKey: String, value: Boolean): String {
    val label = stateKey.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val stateText = if (value) "enabled" else "disabled"
    return "$label $stateText"
}

private fun formatItemUseResult(result: ItemUseResult): String = when (result) {
    is ItemUseResult.Restore -> {
        val segments = buildList {
            if (result.hp > 0) add("${result.hp} HP")
            if (result.rp > 0) add("${result.rp} RP")
        }
        "Restored ${segments.joinToString(" & ").ifBlank { "item" }}"
    }
    is ItemUseResult.Damage -> "${result.item.name} dealt ${result.amount} damage"
    is ItemUseResult.Buff -> "Buffs applied: ${result.buffs.joinToString { "${it.stat}+${it.value}" }}"
    is ItemUseResult.LearnSchematic -> "Learned schematic ${result.schematicId}"
    is ItemUseResult.None -> "Used ${result.item.name}"
}


@Composable
private fun MilestoneHistoryCard(
    history: List<MilestoneEvent>
) {
    SectionCard(title = "Recent Milestones") {
        history.forEachIndexed { index, event ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = if (index != history.lastIndex) 8.dp else 0.dp)) {
                Text(
                    text = event.title.ifBlank { "Milestone ${event.id}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            if (index != history.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun MilestoneGalleryOverlay(
    history: List<MilestoneEvent>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Milestone Gallery", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onClose) { Text("Close") }
                }
                if (history.isEmpty()) {
                    Text("No milestones recorded yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        history.asReversed().forEachIndexed { index, event ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    event.title.ifBlank { "Milestone ${event.id}" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    event.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            if (index != history.lastIndex) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(color = Color.Black.copy(alpha = 0.55f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun TutorialBanner(
    prompt: com.example.starborn.domain.tutorial.TutorialEntry,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0D1C2C).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = prompt.message,
                    color = Color.White
                )
            }
            Button(onClick = onDismiss) { Text("Got it") }
        }
    }
}

@Composable
fun MilestoneBanner(
    prompt: com.example.starborn.domain.milestone.MilestoneEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF263238).copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Milestone", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            Text(prompt.message, color = Color.White)
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Dismiss") }
        }
    }
}

@Composable
fun NarrationCard(
    prompt: NarrationPrompt,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .width(260.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(prompt.message, color = Color.White, textAlign = TextAlign.Start)
            Button(onClick = onDismiss) { Text(if (prompt.tapToDismiss) "Dismiss" else "Close") }
        }
    }
}

@Composable
fun LevelUpOverlay(
    prompt: LevelUpPrompt,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF102030).copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Level Up!", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("${prompt.characterName} reached level ${prompt.newLevel}", color = Color.White)
            if (prompt.statChanges.isNotEmpty()) {
                prompt.statChanges.forEach { Text("${it.label}: ${it.value}", color = Color.White.copy(alpha = 0.85f)) }
            }
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Continue") }
        }
    }
}

@Composable
fun CombatOutcomeOverlay(
    outcome: CombatOutcomeUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (outcome.outcome) {
                    com.example.starborn.navigation.CombatResultPayload.Outcome.VICTORY -> "Victory!"
                    com.example.starborn.navigation.CombatResultPayload.Outcome.DEFEAT -> "Defeat"
                    com.example.starborn.navigation.CombatResultPayload.Outcome.RETREAT -> "Retreat"
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(outcome.message, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            Button(onClick = onDismiss) { Text("Continue") }
        }
    }
}

@Composable
fun BlockedPromptCard(
    prompt: BlockedPrompt,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF261818).copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Path Blocked", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(prompt.message, color = Color.White.copy(alpha = 0.85f))
            prompt.requiresItemLabel?.let { Text(it, color = Color.White.copy(alpha = 0.7f)) }
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Okay") }
        }
    }
}

@Composable
fun CinematicOverlay(
    state: CinematicUiState,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.title?.let { Text(it, color = Color.White, fontWeight = FontWeight.SemiBold) }
            Text(state.step.text, color = Color.White)
            Button(onClick = onAdvance, modifier = Modifier.align(Alignment.End)) { Text("Next") }
        }
    }
}

@Composable
fun ShopGreetingOverlay(
    greeting: ShopGreetingUi,
    onChoice: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.88f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(greeting.shopName, color = Color.White, style = MaterialTheme.typography.titleMedium)
            greeting.lines.forEach { line ->
                Column(horizontalAlignment = Alignment.Start) {
                    line.speaker?.let { Text(it, color = Color.White.copy(alpha = 0.75f)) }
                    Text(line.text, color = Color.White)
                }
            }
            greeting.choices.forEach { choice ->
                Button(
                    onClick = { onChoice(choice.id) },
                    enabled = choice.enabled
                ) {
                    Text(choice.label)
                }
                if (!choice.enabled && choice.action == ShopDialogueAction.SMALLTALK) {
                    Text("Already discussed", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
        }
    }
}

@Composable
private fun CraftingFxOverlay(
    bursts: List<UiFxBurst>,
    onExpired: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        bursts.forEach { burst ->
            FxBurstView(burst = burst, onExpired = onExpired)
        }
    }
}

@Composable
private fun FxBurstView(
    burst: UiFxBurst,
    onExpired: (Long) -> Unit
) {
    val radius = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(burst.id) {
        radius.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, delayMillis = 200)
        )
        onExpired(burst.id)
    }

    AnimatedVisibility(visible = alpha.value > 0.01f) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(96.dp)
            ) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = burst.color.copy(alpha = alpha.value * 0.45f),
                    radius = maxRadius * radius.value
                )
            }
            Text(
                text = burst.label,
                color = burst.color.copy(alpha = alpha.value),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private data class UiFxBurst(
    val id: Long,
    val label: String,
    val color: Color
)

private data class FxVisualInfo(val label: String, val color: Color)

private fun fxVisualInfo(fxId: String): FxVisualInfo {
    val normalized = fxId.trim().lowercase(Locale.getDefault())
    return when (normalized) {
        "craft_first_aid_success" -> FxVisualInfo("Med kit assembled", Color(0xFF66BB6A))
        "craft_first_aid_perfect" -> FxVisualInfo("Perfect med kit!", Color(0xFF81C784))
        "craft_first_aid_failure" -> FxVisualInfo("First aid failed", Color(0xFFE53935))
        "craft_cooking_success" -> FxVisualInfo("Dish complete", Color(0xFFFFB74D))
        "craft_cooking_perfect" -> FxVisualInfo("Perfect dish!", Color(0xFFFFE082))
        "craft_cooking_failure" -> FxVisualInfo("Cooking failed", Color(0xFFEF5350))
        else -> FxVisualInfo(
            label = normalized.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            color = Color(0xFF90CAF9)
        )
    }
}

private fun minimapServiceColor(service: MinimapService): Color = when (service) {
    MinimapService.SHOP -> Color(0xFFFFC107)
    MinimapService.COOKING -> Color(0xFFFF8A65)
    MinimapService.FIRST_AID -> Color(0xFF66BB6A)
    MinimapService.TINKERING -> Color(0xFFBA68C8)
}

@Composable
fun QuestUpdateBanner(
    prompt: com.example.starborn.domain.quest.QuestLogEntry,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0D1C2C).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = prompt.message,
                    color = Color.White
                )
            }
            Button(onClick = onDismiss) { Text("Got it") }
        }
    }
}
