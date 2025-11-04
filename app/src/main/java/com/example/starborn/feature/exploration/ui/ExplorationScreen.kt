package com.example.starborn.feature.exploration.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.model.CookingAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.FirstAidAction
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.serviceTag
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.ui.vfx.ThemeBandOverlay
import com.example.starborn.ui.vfx.VignetteOverlay
import com.example.starborn.ui.vfx.WeatherOverlay
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import com.example.starborn.feature.exploration.viewmodel.BlockedPrompt
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.feature.exploration.viewmodel.CombatOutcomeUi
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.LevelUpPrompt
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import com.example.starborn.feature.exploration.viewmodel.MilestoneBandUi
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import com.example.starborn.feature.exploration.viewmodel.MinimapService
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.NarrationPrompt
import com.example.starborn.feature.exploration.viewmodel.PartyStatusUi
import com.example.starborn.feature.exploration.viewmodel.ProgressionSummaryUi
import com.example.starborn.feature.exploration.viewmodel.QuestLogEntryUi
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import com.example.starborn.feature.exploration.viewmodel.TogglePromptUi
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import android.text.format.DateUtils
import java.util.LinkedHashSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.text.buildString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.starborn.ui.theme.MinimapTextStyle




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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> onEnemySelected(event.enemyIds)
                is ExplorationEvent.PlayCinematic -> Unit
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
                is ExplorationEvent.AudioSettingsChanged -> {
                    audioCuePlayer.setUserMusicGain(event.musicVolume)
                    audioCuePlayer.setUserSfxGain(event.sfxVolume)
                }
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

    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    val backgroundPainter = rememberRoomBackgroundPainter(uiState.currentRoom?.backgroundImage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.availableConnections, uiState.blockedDirections) {
                detectDragGestures(
                    onDragStart = { dragDelta = Offset.Zero },
                    onDrag = { _, dragAmount -> dragDelta += dragAmount },
                    onDragEnd = {
                        val dx = dragDelta.x
                        val dy = dragDelta.y
                        val absDx = abs(dx)
                        val absDy = abs(dy)
                        var direction: String? = null
                        if (absDx > absDy && absDx > swipeThresholdPx) {
                            direction = if (dx < 0f) "west" else "east"
                        } else if (absDy > swipeThresholdPx) {
                            direction = if (dy < 0f) "north" else "south"
                        }
                        direction?.let { dir ->
                            val targetDir = uiState.availableConnections.keys.firstOrNull { key ->
                                key.equals(dir, ignoreCase = true)
                            }
                            val blocked = uiState.blockedDirections.any { it.equals(dir, ignoreCase = true) }
                            if (targetDir != null && !blocked) {
                                viewModel.travel(targetDir)
                            }
                        }
                    }
                )
            }
    ) {
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val currentRoom = uiState.currentRoom
        val isRoomDark = remember(currentRoom, uiState.roomState) {
            when {
                uiState.roomState["dark"] == true -> true
                uiState.roomState["light_on"] == false -> true
                else -> currentRoom?.dark == true
            }
        }
        val baseRoomDescription = remember(currentRoom, uiState.roomState, isRoomDark) {
            currentRoom?.let { room ->
                if (isRoomDark && !room.descriptionDark.isNullOrBlank()) room.descriptionDark else room.description
            }
        }

        val darknessAlpha by animateFloatAsState(
            targetValue = if (isRoomDark) 0.82f else 0f,
            animationSpec = tween(durationMillis = 320)
        )
        if (darknessAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B18).copy(alpha = darknessAlpha))
            )
        }
        WeatherOverlay(
            weatherId = currentRoom?.weather,
            modifier = Modifier.fillMaxSize()
        )
        val hasWeather = !currentRoom?.weather.isNullOrBlank()
        val vignetteIntensity = when {
            isRoomDark -> 0.6f
            hasWeather -> 0.4f
            else -> 0.0f
        }
        VignetteOverlay(
            visible = uiState.settings.vignetteEnabled && vignetteIntensity > 0f,
            intensity = vignetteIntensity,
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        )
        val actionHints = uiState.actionHints
        val inlinePlan = remember(baseRoomDescription, uiState.actions, actionHints) {
            buildInlineActionPlan(
                description = baseRoomDescription,
                actions = uiState.actions,
                hints = actionHints
            )
        }
        val inlineActionKeys = inlinePlan?.inlineKeys ?: emptySet()
        val remainingActions = remember(uiState.actions, inlineActionKeys) {
            uiState.actions.filterNot { inlineActionKeys.contains(it.actionKey()) }
        }

        val serviceQuickActions = remember(uiState.actions, uiState.actionHints, currentRoom?.id) {
            val unique = LinkedHashSet<String>()
            val items = mutableListOf<QuickMenuAction>()
            uiState.actions.forEach { action ->
                val hint = uiState.actionHints[action.actionKey()]
                if (hint?.locked == true) return@forEach
                when (action) {
                    is ShopAction -> {
                        val shopId = action.shopId ?: return@forEach
                        if (unique.add("shop:$shopId")) {
                            val label = action.name.takeIf { it.isNotBlank() } ?: "Shop"
                            items += QuickMenuAction(
                                iconRes = R.drawable.shop_icon,
                                label = label,
                                roomAction = action
                            )
                        }
                    }
                    is TinkeringAction -> {
                        val key = "tinkering:${action.shopId.orEmpty()}"
                        if (unique.add(key)) {
                            val label = action.name.takeIf { it.isNotBlank() } ?: "Tinkering"
                            items += QuickMenuAction(
                                iconRes = R.drawable.tinkering_icon,
                                label = label,
                                roomAction = action
                            )
                        }
                    }
                    is CookingAction -> {
                        val key = "cooking:${action.stationId.orEmpty()}"
                        if (unique.add(key)) {
                            val label = action.name.takeIf { it.isNotBlank() } ?: "Cooking"
                            items += QuickMenuAction(
                                iconRes = R.drawable.cooking_icon,
                                label = label,
                                roomAction = action
                            )
                        }
                    }
                    is FirstAidAction -> {
                        val key = "firstaid:${action.stationId.orEmpty()}"
                        if (unique.add(key)) {
                            val label = action.name.takeIf { it.isNotBlank() } ?: "First Aid"
                            items += QuickMenuAction(
                                iconRes = R.drawable.firstaid_icon,
                                label = label,
                                roomAction = action
                            )
                        }
                    }
                    is GenericAction -> {
                        val type = action.type.lowercase(Locale.getDefault())
                        if (type.contains("fish")) {
                            val zone = action.zoneId ?: currentRoom?.id.orEmpty()
                            val key = "fish:$zone"
                            if (unique.add(key)) {
                                val label = action.name.takeIf { it.isNotBlank() } ?: "Fishing"
                                items += QuickMenuAction(
                                    iconRes = R.drawable.fishing_icon,
                                    label = label,
                                    roomAction = action
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
            items
        }

        val quickMenuActions = remember(serviceQuickActions, uiState.milestoneHistory) {
            buildList {
                addAll(serviceQuickActions)
                add(
                    QuickMenuAction(
                        iconRes = R.drawable.inventory_icon,
                        label = "Inventory",
                        tab = MenuTab.INVENTORY
                    )
                )
                add(
                    QuickMenuAction(
                        iconRes = R.drawable.journal_icon,
                        label = "Journal",
                        tab = MenuTab.JOURNAL
                    )
                )
                add(
                    QuickMenuAction(
                        iconRes = R.drawable.stats_icon,
                        label = "Stats",
                        tab = MenuTab.STATS
                    )
                )
                add(
                    QuickMenuAction(
                        iconRes = R.drawable.settings_icon,
                        label = "Settings",
                        tab = MenuTab.SETTINGS
                    )
                )
                if (uiState.milestoneHistory.isNotEmpty()) {
                    add(
                        QuickMenuAction(
                            iconRes = R.drawable.milestone_icon,
                            label = "Milestones",
                            command = QuickMenuCommand.SHOW_MILESTONES
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            ThemeBandOverlay(
                env = currentRoom?.env,
                weather = currentRoom?.weather,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(96.dp)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = uiState.currentRoom?.title ?: "Unknown area",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                val progress = uiState.progressionSummary
                Text(
                    text = buildString {
                        append("Level ${progress.playerLevel}")
                        append(" • ${progress.creditsLabel}")
                        progress.xpToNextLabel?.let { append(" • $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.5f),
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            MinimapWidget(
                minimap = uiState.minimap,
                onLegend = { viewModel.openMinimapLegend() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(184.dp)
                    .height(184.dp)
            )

            RoomDescriptionPanel(
                currentRoom = currentRoom,
                description = baseRoomDescription,
                plan = inlinePlan,
                isDark = isRoomDark,
                onAction = { action -> viewModel.onActionSelected(action) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 240.dp, max = 480.dp)
            )

            if (remainingActions.isNotEmpty()) {
                ActionListPanel(
                    actions = remainingActions,
                    actionHints = uiState.actionHints,
                    onAction = { action -> viewModel.onActionSelected(action) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp)
                )
            }

            if (uiState.groundItems.isNotEmpty()) {
                GroundItemsPanel(
                    items = uiState.groundItems,
                    onCollect = { itemId -> viewModel.collectGroundItem(itemId) },
                    onCollectAll = { viewModel.collectAllGroundItems() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 120.dp)
                )
            }

            if (serviceQuickActions.isNotEmpty()) {
                ServiceActionTray(
                    actions = serviceQuickActions,
                    onAction = { quick ->
                        quick.roomAction?.let { viewModel.onActionSelected(it) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                )
            }

            uiState.statusMessage?.let { message ->
                StatusMessageChip(
                    message = message,
                    onDismiss = { viewModel.clearStatusMessage() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 96.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { viewModel.toggleQuickMenu() }) {
                    Text(if (uiState.isQuickMenuVisible) "Close" else "Menu")
                }
            }

            if (uiState.isQuickMenuVisible && quickMenuActions.isNotEmpty()) {
                QuickMenuOverlay(
                    actions = quickMenuActions,
                    onDismiss = { viewModel.toggleQuickMenu() },
                    onSelect = { action ->
                        when {
                            action.roomAction != null -> viewModel.onActionSelected(action.roomAction)
                            action.command == QuickMenuCommand.SHOW_MILESTONES -> viewModel.openMilestoneGallery()
                            action.tab != null -> viewModel.openMenuOverlay(action.tab)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                    anchorSpacing = 104.dp
                )
            }
        }
        uiState.togglePrompt?.let { prompt ->
            TogglePromptDialog(
                prompt = prompt,
                onSelect = { enable -> viewModel.onTogglePromptSelection(enable) },
                onDismiss = { viewModel.dismissTogglePrompt() },
                modifier = Modifier.align(Alignment.Center)
            )
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

            if (uiState.isMenuOverlayVisible) {
                MenuOverlay(
                    selectedTab = uiState.menuTab,
                    onSelectTab = { viewModel.selectMenuTab(it) },
                    onClose = { viewModel.closeMenuOverlay() },
                    onOpenInventory = {
                        viewModel.closeMenuOverlay()
                        onOpenInventory()
                    },
                    onOpenJournal = {
                        viewModel.closeMenuOverlay()
                        viewModel.openQuestLog()
                    },
                    onOpenMap = {
                        viewModel.closeMenuOverlay()
                        viewModel.openMinimapLegend()
                    },
                    settings = uiState.settings,
                    onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                    onSfxVolumeChange = { viewModel.updateSfxVolume(it) },
                    onToggleVignette = { viewModel.setVignetteEnabled(it) },
                    partyStatus = uiState.partyStatus,
                    minimap = uiState.minimap
                )
            }

        if (uiState.isQuestLogVisible) {
            QuestJournalOverlay(
                trackedQuest = uiState.questLogActive.firstOrNull { it.id == uiState.trackedQuestId },
                activeQuests = uiState.questLogActive,
                completedQuests = uiState.questLogCompleted,
                failedQuests = uiState.failedQuests,
                questLog = uiState.questLogEntries,
                onClose = { viewModel.closeQuestLog() },
                modifier = Modifier.align(Alignment.Center)
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
private fun MinimapWidget(
    minimap: MinimapUiState?,
    onLegend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clrBackground = Color(0.05f, 0.1f, 0.15f, 0.85f)
    val clrBorder = Color(0.3f, 0.8f, 1.0f, 1.0f)
    val clrBorderAccent = Color(0.6f, 0.9f, 1.0f, 0.8f)
    val clrGrid = Color(0.3f, 0.8f, 1.0f, 0.15f)
    val clrTile = Color(0.6f, 0.85f, 1.0f, 0.7f)
    val clrTileGlow = Color(0.9f, 1.0f, 1.0f, 1.0f)
    val clrPlayer = Color(1.0f, 0.9f, 0.3f, 1.0f)

    val playerPulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        playerPulse.animateTo(
            targetValue = 0.1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier.clickable(onClick = onLegend),
        color = Color.Transparent
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val base = minOf(w, h)
            val cx = w / 2
            val cy = h / 2

            val g = base / 7
            val pad = g / 2.7f
            val step = g + pad
            val radius = base * 0.15f
            val padding = base * 0.1f

            // Base Panel
            drawRoundRect(
                color = clrBackground,
                size = size,
                cornerRadius = CornerRadius(radius, radius)
            )

            // 3x3 Grid
            val gridStartX = cx - 1.5f * step
            val gridStartY = cy - 1.5f * step
            for (i in 0..3) {
                drawLine(
                    color = clrGrid,
                    start = Offset(gridStartX + i * step, gridStartY),
                    end = Offset(gridStartX + i * step, gridStartY + 3 * step),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = clrGrid,
                    start = Offset(gridStartX, gridStartY + i * step),
                    end = Offset(gridStartX + 3 * step, gridStartY + i * step),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Stylized Border
            drawRoundRect(
                color = clrBorder,
                size = Size(w - 2, h - 2),
                topLeft = Offset(1f, 1f),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 1.dp.toPx())
            )

            // Corner accent details
            val cornerSize = padding * 1.5f
            val accentStrokeWidth = 1.5f.dp.toPx()
            // Top-left
            drawLine(clrBorderAccent, Offset(padding, h - cornerSize), Offset(padding, h - padding), strokeWidth = accentStrokeWidth)
            drawLine(clrBorderAccent, Offset(padding, h - padding), Offset(cornerSize, h - padding), strokeWidth = accentStrokeWidth)
            // Top-right
            drawLine(clrBorderAccent, Offset(w - cornerSize, h - padding), Offset(w - padding, h - padding), strokeWidth = accentStrokeWidth)
            drawLine(clrBorderAccent, Offset(w - padding, h - padding), Offset(w - padding, h - cornerSize), strokeWidth = accentStrokeWidth)
            // Bottom-left
            drawLine(clrBorderAccent, Offset(padding, cornerSize), Offset(padding, padding), strokeWidth = accentStrokeWidth)
            drawLine(clrBorderAccent, Offset(padding, padding), Offset(cornerSize, padding), strokeWidth = accentStrokeWidth)
            // Bottom-right
            drawLine(clrBorderAccent, Offset(w - padding, cornerSize), Offset(w - padding, padding), strokeWidth = accentStrokeWidth)
            drawLine(clrBorderAccent, Offset(w - padding, padding), Offset(w - cornerSize, padding), strokeWidth = accentStrokeWidth)

            minimap?.let { state ->
                val cellsInViewport = state.cells.filter { abs(it.offsetX) <= 1 && abs(it.offsetY) <= 1 }
                val idToCell = state.cells.associateBy { it.roomId }

                // --- Connection lines (only where rooms are truly connected) ---
                cellsInViewport.forEach { cell ->
                    // Only draw connections to east and north to avoid duplicates and redundant checks
                    for (direction in setOf("east", "north")) {
                        val connectedRoomId = cell.connections[direction]
                        if (connectedRoomId != null) {
                            val neighbor = idToCell[connectedRoomId]
                            if (neighbor != null && abs(neighbor.offsetX) <= 1 && abs(neighbor.offsetY) <= 1) {
                                val x1 = cx + cell.offsetX * step
                                val y1 = cy + cell.offsetY * step
                                val x2 = cx + neighbor.offsetX * step
                                val y2 = cy + neighbor.offsetY * step
                                drawLine(
                                    color = clrGrid.copy(alpha = 0.6f),
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }
                }

                // Rooms
                cellsInViewport.forEach { cell ->
                    val px = cx - g / 2 + cell.offsetX * step
                    val py = cy - g / 2 + cell.offsetY * step

                    val isCurrent = cell.isCurrent
                    val pipColor = if (isCurrent) clrTileGlow else clrTile
                    val pipSize = g * if (isCurrent) 0.9f else 0.6f

                    drawCircle(
                        color = pipColor,
                        radius = pipSize / 2,
                        center = Offset(px + g / 2, py + g / 2)
                    )
                }
            }

            // Player Indicator
            val playerSize = g * 0.6f
            val glowSize = playerSize * (1 + playerPulse.value * 0.5f)
            drawCircle(
                color = clrPlayer.copy(alpha = 0.3f * (1 - playerPulse.value)),
                radius = glowSize / 2,
                center = Offset(cx, cy)
            )

            val hairLength = playerSize / 2
            val lineWidth = 2.dp.toPx()
            drawLine(clrPlayer, Offset(cx - hairLength, cy), Offset(cx + hairLength, cy), strokeWidth = lineWidth)
            drawLine(clrPlayer, Offset(cx, cy - hairLength), Offset(cx, cy + hairLength), strokeWidth = lineWidth)
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
private fun MenuOverlay(
    selectedTab: MenuTab,
    onSelectTab: (MenuTab) -> Unit,
    onClose: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMap: () -> Unit,
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    partyStatus: PartyStatusUi,
    minimap: MinimapUiState?
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.94f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onClose() }
                                .padding(4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuTab.values().forEach { tab ->
                            if (tab == selectedTab) {
                                Button(onClick = { onSelectTab(tab) }) {
                                    Text(tab.label())
                                }
                            } else {
                                OutlinedButton(onClick = { onSelectTab(tab) }) {
                                    Text(tab.label())
                                }
                            }
                        }
                    }

                    MenuTabContent(
                        tab = selectedTab,
                        settings = settings,
                        partyStatus = partyStatus,
                        minimap = minimap,
                        onClose = onClose,
                        onOpenInventory = onOpenInventory,
                        onOpenJournal = onOpenJournal,
                        onOpenMap = onOpenMap,
                        onMusicVolumeChange = onMusicVolumeChange,
                        onSfxVolumeChange = onSfxVolumeChange,
                        onToggleVignette = onToggleVignette
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuTabContent(
    tab: MenuTab,
    settings: SettingsUiState,
    partyStatus: PartyStatusUi,
    minimap: MinimapUiState?,
    onClose: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMap: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleVignette: (Boolean) -> Unit
) {
    when (tab) {
        MenuTab.INVENTORY -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Manage your gear, consumables, and equipment.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(onClick = {
                    onClose()
                    onOpenInventory()
                }) {
                    Text("Open Inventory")
                }
            }
        }
        MenuTab.JOURNAL -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Review active quests and milestones.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(onClick = {
                    onClose()
                    onOpenJournal()
                }) {
                    Text("Open Journal")
                }
            }
        }
        MenuTab.MAP -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (minimap != null) {
                    MinimapWidget(
                        minimap = minimap,
                        onLegend = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                    )
                } else {
                    Text(
                        text = "Map data unavailable in this area.",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(onClick = {
                    onClose()
                    onOpenMap()
                }) {
                    Text("Show Map Legend")
                }
            }
        }
        MenuTab.STATS -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (partyStatus.members.isEmpty()) {
                    Text(
                        text = "Party roster pending.",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    partyStatus.members.forEach { member ->
                        Column {
                            Text(
                                text = "${member.name} · Lv ${member.level}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                member.hpLabel?.let {
                                    Text("HP: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                member.rpLabel?.let {
                                    Text("RP: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("XP: ${member.xpLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        MenuTab.SETTINGS -> {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Music Volume: ${ (settings.musicVolume * 100).roundToInt() }%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.musicVolume,
                        onValueChange = onMusicVolumeChange,
                        valueRange = 0f..1f
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Effects Volume: ${ (settings.sfxVolume * 100).roundToInt() }%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.sfxVolume,
                        onValueChange = onSfxVolumeChange,
                        valueRange = 0f..1f
                    )
                }
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Light Vignette",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.vignetteEnabled) "Enabled" else "Disabled",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.vignetteEnabled,
                        onCheckedChange = onToggleVignette
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomDescription(
    plan: InlineActionPlan?,
    description: String?,
    isDark: Boolean,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (description.isNullOrBlank()) {
        Text(
            text = "No description available.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }
    if (plan == null) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }

    val defaultColor = Color.White.copy(alpha = 0.92f)
    val highlightColor = if (isDark) Color(0xFF7BE8FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    val annotatedText = remember(plan, highlightColor, disabledColor) {
        buildAnnotatedString {
            append(plan.description)
            plan.segments.forEach { segment ->
                addStyle(
                    SpanStyle(
                        color = if (segment.locked) disabledColor else highlightColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (segment.locked) TextDecoration.None else TextDecoration.Underline
                    ),
                    start = segment.start,
                    end = segment.end
                )
                addStringAnnotation(
                    tag = ACTION_TAG,
                    annotation = segment.actionId,
                    start = segment.start,
                    end = segment.end
                )
            }
        }
    }
    val actionLookup = remember(plan) {
        plan.segments.associateBy { it.actionId }
    }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = defaultColor, textAlign = TextAlign.Start)
    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = bodyStyle
    ) { offset ->
        annotatedText.getStringAnnotations(ACTION_TAG, offset, offset).firstOrNull()?.let { annotation ->
            val segment = actionLookup[annotation.item] ?: return@ClickableText
            if (!segment.locked) {
                onAction(segment.action)
            }
        }
    }
}

private data class QuickMenuAction(
    val iconRes: Int,
    val label: String,
    val tab: MenuTab? = null,
    val roomAction: RoomAction? = null,
    val command: QuickMenuCommand? = null
)

private enum class QuickMenuCommand {
    SHOW_MILESTONES
}

@Composable
private fun ServiceActionTray(
    actions: List<QuickMenuAction>,
    onAction: (QuickMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val serviceActions = remember(actions) { actions.filter { it.roomAction != null } }
    if (serviceActions.isEmpty()) return

    Surface(
        modifier = modifier,
        color = Color(0xE60D1C2C),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            serviceActions.forEach { action ->
                Surface(
                    modifier = Modifier
                        .widthIn(min = 76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onAction(action) },
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(action.iconRes),
                            contentDescription = action.label,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuOverlay(
    actions: List<QuickMenuAction>,
    onDismiss: () -> Unit,
    onSelect: (QuickMenuAction) -> Unit,
    modifier: Modifier = Modifier,
    anchorSpacing: Dp = 96.dp,
    radius: Dp = 168.dp,
    angleRange: Float = 140f
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val expansion = remember { Animatable(0f) }
    val angles = remember(actions.size, angleRange) {
        computeQuickMenuAngles(actions.size, angleRange)
    }

    LaunchedEffect(actions) {
        expansion.snapTo(0f)
        expansion.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onDismiss() }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = anchorSpacing)
        ) {
            val radiusPx = with(density) { radius.toPx() }
            actions.forEachIndexed { index, action ->
                val angleRad = Math.toRadians(angles.getOrElse(index) { 270f }.toDouble())
                val offsetX = (cos(angleRad) * radiusPx * expansion.value).roundToInt()
                val offsetY = (sin(angleRad) * radiusPx * expansion.value).roundToInt()
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .offset { IntOffset(offsetX, offsetY) }
                        .graphicsLayer(alpha = expansion.value)
                        .zIndex(1f),
                    shape = CircleShape,
                    color = Color(0xFF0D1C2C).copy(alpha = 0.95f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onSelect(action) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(action.iconRes),
                            contentDescription = action.label,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = action.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun computeQuickMenuAngles(
    count: Int,
    angleRange: Float,
    centerAngle: Float = 270f
): List<Float> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(centerAngle)
    val start = centerAngle - angleRange / 2f
    val step = angleRange / (count - 1)
    return List(count) { index -> start + step * index }
}

private data class InlineActionSegment(
    val actionId: String,
    val action: RoomAction,
    val start: Int,
    val end: Int,
    val locked: Boolean
)

private data class InlineActionPlan(
    val description: String,
    val segments: List<InlineActionSegment>
) {
    val inlineKeys: Set<String> = segments.mapTo(linkedSetOf()) { it.actionId }
}

private const val ACTION_TAG = "action"

@Composable
private fun QuestJournalOverlay(
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    failedQuests: Set<String>,
    questLog: List<QuestLogEntryUi>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otherActive = remember(trackedQuest, activeQuests) {
        activeQuests.filterNot { it.id == trackedQuest?.id }
    }
    val recentLog = remember(questLog) {
        questLog.sortedByDescending { it.timestamp }.take(12)
    }

    val totalCount = activeQuests.size + completedQuests.size + failedQuests.size

    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xF0102030)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quest Journal",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "$totalCount quests",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Close",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
                trackedQuest?.let { quest ->
                    item {
                        SectionCard(title = "Tracked Quest") {
                            QuestSummaryDetails(quest, emphasize = true)
                        }
                    }
                }

                if (recentLog.isNotEmpty()) {
                    item {
                        SectionCard(title = "Recent Updates") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentLog.forEach { entry ->
                                    QuestLogEntryRow(entry)
                                }
                            }
                        }
                    }
                }

                if (otherActive.isNotEmpty()) {
                    item {
                        SectionCard(title = "Active Quests") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                otherActive.forEach { quest ->
                                    QuestSummaryDetails(quest)
                                }
                            }
                        }
                    }
                }

                if (completedQuests.isNotEmpty()) {
                    item {
                        SectionCard(title = "Completed Quests") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                completedQuests.take(8).forEach { quest ->
                                    Text(
                                        text = "• ${quest.title}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                if (completedQuests.size > 8) {
                                    Text(
                                        text = "+${completedQuests.size - 8} more",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (failedQuests.isNotEmpty()) {
                    item {
                        SectionCard(title = "Failed Quests") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                failedQuests.take(8).forEach { questId ->
                                    Text(
                                        text = "• $questId",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    )
                                }
                                if (failedQuests.size > 8) {
                                    Text(
                                        text = "+${failedQuests.size - 8} more",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
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

@Composable
private fun QuestSummaryDetails(
    quest: QuestSummaryUi,
    emphasize: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = quest.title,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        quest.summary.takeIf { it.isNotBlank() }?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        Text(
            text = "Stage ${quest.stageIndex + 1} of ${quest.totalStages}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.7f)
        )
        quest.stageTitle?.takeIf { it.isNotBlank() }?.let { stageTitle ->
            Text(
                text = stageTitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        quest.stageDescription?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        if (quest.objectives.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                quest.objectives.forEach { objective ->
                    Text(
                        text = "• $objective",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestLogEntryRow(entry: QuestLogEntryUi) {
    val timeLabel = remember(entry.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            entry.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = Color.White
        )
        val subtitle = buildString {
            entry.stageTitle?.takeIf { it.isNotBlank() }?.let { append(it) }
            if (isNotEmpty()) append(" • ")
            append(timeLabel)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun MilestoneTimelineRow(event: MilestoneEvent) {
    val relativeTime = remember(event.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            event.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(28.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = event.title.ifBlank { "Milestone ${event.id}" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color.White
                )
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun RoomDescriptionPanel(
    currentRoom: Room?,
    description: String?,
    plan: InlineActionPlan?,
    isDark: Boolean,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentRoom == null && description.isNullOrBlank()) return
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoomDescription(
                plan = plan,
                description = description,
                isDark = isDark,
                onAction = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActionListPanel(
    actions: List<RoomAction>,
    actionHints: Map<String, ActionHintUi>,
    onAction: (RoomAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Points of Interest",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp)
            )
            actions.forEach { action ->
                val hint = actionHints[action.actionKey()]
                val locked = hint?.locked == true
                Button(
                    onClick = { onAction(action) },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            action.name.ifBlank { "Interact" },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Color.White
                        )
                        val subtitle = hint?.message?.takeIf { it.isNotBlank() }
                            ?: action.serviceTag()
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroundItemsPanel(
    items: Map<String, Int>,
    onCollect: (String) -> Unit,
    onCollectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Items Nearby",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp)
            )
            items.toList().forEach { (itemId, quantity) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$itemId ×$quantity",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        modifier = Modifier.weight(1f)
                    )
            Button(onClick = { onCollect(itemId) }) {
                Text("Take", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
            }
        }
    }
    Button(
        onClick = onCollectAll,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Take All", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
    }
        }
    }
}

@Composable
private fun TogglePromptDialog(
    prompt: TogglePromptUi,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(prompt.title) },
        text = { Text(prompt.message) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { onSelect(true) },
                    enabled = !prompt.isOn
                ) {
                    Text(prompt.enableLabel)
                }
                TextButton(
                    onClick = { onSelect(false) },
                    enabled = prompt.isOn
                ) {
                    Text(prompt.disableLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StatusMessageChip(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Dismiss",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(4.dp)
            )
        }
    }
}

private fun buildInlineActionPlan(
    description: String?,
    actions: List<RoomAction>,
    hints: Map<String, ActionHintUi>
): InlineActionPlan? {
    if (description.isNullOrBlank() || actions.isEmpty()) return null
    val lower = description.lowercase(Locale.getDefault())
    val segments = mutableListOf<InlineActionSegment>()
    val occupied = mutableListOf<IntRange>()

    actions.forEach { action ->
        val baseName = action.name
        if (baseName.isBlank()) return@forEach
        val variants = buildList {
            add(baseName)
            val normalizedDash = baseName.replace('-', ' ')
            if (normalizedDash != baseName) add(normalizedDash)
            val normalizedApostrophe = baseName.replace('’', '\'')
            if (normalizedApostrophe != baseName) add(normalizedApostrophe)
        }
            .map { variant -> variant to variant.lowercase(Locale.getDefault()) }
            .distinctBy { it.second }
            .sortedByDescending { it.first.length }

        var matchedRange: IntRange? = null
        var searchLength = 0

        for ((variantOriginal, variantLower) in variants) {
            var searchIndex = 0
            while (searchIndex <= lower.length - variantLower.length) {
                val index = lower.indexOf(variantLower, searchIndex)
                if (index < 0) break
                val rangeCandidate = index until index + variantLower.length
                if (occupied.none { rangesOverlap(it, rangeCandidate) }) {
                    matchedRange = rangeCandidate
                    searchLength = variantLower.length
                    break
                }
                searchIndex = index + 1
            }
            if (matchedRange != null) break
        }

        if (matchedRange != null) {
            occupied += matchedRange
            val key = action.actionKey()
            val locked = hints[key]?.locked == true
            segments += InlineActionSegment(
                actionId = key,
                action = action,
                start = matchedRange.first,
                end = matchedRange.first + searchLength,
                locked = locked
            )
        }
    }
    if (segments.isEmpty()) return null
    segments.sortBy { it.start }
    return InlineActionPlan(description = description, segments = segments)
}

private fun rangesOverlap(a: IntRange, b: IntRange): Boolean =
    a.first < b.last && b.first < a.last

@Composable
private fun rememberRoomBackgroundPainter(imagePath: String?): Painter {
    val context = LocalContext.current
    val defaultPainter = painterResource(R.drawable.main_menu_background)
    if (imagePath.isNullOrBlank()) return defaultPainter

    val (resId, assetPainter) = remember(imagePath) {
        val resourceName = imagePath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase(Locale.getDefault())
        val resolvedId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        runCatching {
            context.assets.open(imagePath).use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bitmap ->
                    BitmapPainter(bitmap.asImageBitmap())
                }
            }
        }.getOrNull().let { resolvedPainter ->
            resolvedId to resolvedPainter
        }
    }
    return when {
        resId != 0 -> painterResource(resId)
        assetPainter != null -> assetPainter
        else -> defaultPainter
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
    val ordered = remember(history) { history.sortedByDescending { it.timestamp } }
    val total = ordered.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = Color(0xF0102030),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Milestone Gallery",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = if (total == 0) "No milestones yet" else "$total milestone${if (total == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "Close",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClose() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (ordered.isEmpty()) {
                    Text(
                        text = "No milestones recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(ordered) { event ->
                            MilestoneTimelineRow(event)
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
