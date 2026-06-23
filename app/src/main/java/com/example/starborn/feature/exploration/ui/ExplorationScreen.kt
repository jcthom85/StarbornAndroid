package com.example.starborn.feature.exploration.ui

import com.example.starborn.feature.exploration.ui.components.*
import com.example.starborn.feature.exploration.ui.tabs.*
import com.example.starborn.feature.exploration.ui.hud.*

import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable as CoreAnimatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlin.math.roundToInt
import com.example.starborn.R
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.FirstAidAction
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.serviceTag
import com.example.starborn.domain.theme.defaultWeatherForEnvironment
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.components.ItemTargetSelectionDialog
import com.example.starborn.ui.components.SaveLoadDialog
import com.example.starborn.ui.components.TargetSelectionOption
import com.example.starborn.ui.vfx.ThemeBandOverlay
import com.example.starborn.ui.vfx.VignetteOverlay
import com.example.starborn.ui.vfx.WeatherOverlay
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import com.example.starborn.feature.exploration.viewmodel.BlockedPrompt
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorStatus
import com.example.starborn.feature.exploration.viewmodel.DirectionIndicatorUi
import com.example.starborn.feature.exploration.viewmodel.EnemyCompositeIconUi
import com.example.starborn.feature.exploration.viewmodel.EnemyIconUi
import com.example.starborn.feature.exploration.viewmodel.ExplorationEvent
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState
import com.example.starborn.feature.exploration.viewmodel.ExplorationViewModel
import com.example.starborn.feature.exploration.viewmodel.LevelUpPrompt
import com.example.starborn.feature.exploration.viewmodel.InventoryPreviewItemUi
import com.example.starborn.feature.exploration.viewmodel.MilestoneBandUi
import com.example.starborn.feature.exploration.viewmodel.FullMapUiState
import com.example.starborn.feature.exploration.viewmodel.MinimapCellUi
import com.example.starborn.feature.exploration.viewmodel.MinimapService
import com.example.starborn.feature.exploration.viewmodel.MenuTab
import com.example.starborn.feature.exploration.viewmodel.MinimapUiState
import com.example.starborn.feature.exploration.viewmodel.NarrationPrompt
import com.example.starborn.feature.exploration.viewmodel.PartyMemberDetailsUi
import com.example.starborn.feature.exploration.viewmodel.CharacterStatValueUi
import com.example.starborn.feature.exploration.viewmodel.PartyMemberStatusUi
import com.example.starborn.feature.exploration.viewmodel.PartyStatusUi
import com.example.starborn.feature.exploration.viewmodel.EventAnnouncementUi
import com.example.starborn.feature.exploration.viewmodel.QuestLogEntryUi
import com.example.starborn.feature.exploration.viewmodel.QuestSummaryUi
import com.example.starborn.feature.exploration.viewmodel.QuestDetailUi
import com.example.starborn.feature.exploration.viewmodel.QuestObjectiveUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import com.example.starborn.feature.exploration.viewmodel.TogglePromptUi
import com.example.starborn.feature.exploration.viewmodel.VisualEnemyParty
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.exploration.viewmodel.SkillTreeBranchUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeNodeUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeOverlayUi
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import android.text.format.DateUtils
import java.util.LinkedHashSet
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.text.buildString
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import java.util.Locale
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import com.example.starborn.ui.overlay.ProgressToastOverlay
import com.example.starborn.ui.overlay.QuestBannerOverlay
import com.example.starborn.ui.overlay.QuestDetailOverlay
import com.example.starborn.ui.overlay.QuestSummaryOverlay
import com.example.starborn.ui.theme.MinimapTextStyle
import com.example.starborn.ui.theme.themeColor
import kotlin.math.min
import com.example.starborn.feature.exploration.ui.components.SkillTreeOverlay
import com.example.starborn.feature.exploration.ui.components.GearSelectionDialog
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import com.example.starborn.feature.exploration.ui.components.PartyMemberDetailsDialog
import com.example.starborn.feature.exploration.ui.components.EventAnnouncementOverlay
import com.example.starborn.feature.exploration.ui.components.TogglePromptDialog
import com.example.starborn.feature.exploration.ui.tabs.InventoryTabContent
import com.example.starborn.feature.exploration.ui.tabs.JournalTabContent
import com.example.starborn.feature.exploration.ui.tabs.MapTabContent
import com.example.starborn.feature.exploration.ui.tabs.StatsTabContent
import com.example.starborn.feature.exploration.ui.tabs.SettingsTabContent
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalToggle
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalPage




@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    audioCuePlayer: AudioCuePlayer,
    uiEventBus: UiEventBus,
    modifier: Modifier = Modifier,
    onEnemySelected: (List<String>) -> Unit = {},
    onOpenTinkering: (String?) -> Unit = {},
    onOpenFirstAid: (String?) -> Unit = {},
    onOpenFishing: (String?) -> Unit = {},
    onOpenShop: (String) -> Unit = {},
    onReturnToHub: () -> Unit = {},
    fxEvents: Flow<String>? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = viewModel.uiState.value
    )
    var pendingInventoryItem by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var showInventoryTargetDialog by remember { mutableStateOf(false) }
    val fxBursts = remember { mutableStateListOf<UiFxBurst>() }
    var saveLoadMode by remember { mutableStateOf<String?>(null) } // "save" or "load"
    var slotSummaries by remember { mutableStateOf<List<SaveSlotSummary>>(emptyList()) }
    var debugWeatherOverride by remember { mutableStateOf<String?>(null) }
    val weatherCycles = remember {
        listOf(null, "dust", "rain", "storm", "snow", "cave_drip", "starfall", "steam", "fog", "gas", "resonance", "sparks")
    }
    val coroutineScope = rememberCoroutineScope()
    val blockingOverlayActive =
        uiState.isMenuOverlayVisible ||
            uiState.togglePrompt != null ||
            uiState.activeDialogue != null ||
            uiState.shopGreeting != null ||
            uiState.narrationPrompt != null ||
            uiState.cinematic != null ||
            uiState.skillTreeOverlay != null ||
            uiState.partyMemberDetails != null ||
            uiState.prompt != null ||
            uiState.isMilestoneGalleryVisible ||
            uiState.eventAnnouncement != null ||
            uiState.levelUpPrompt != null ||
            uiState.isQuestLogVisible ||
            uiState.isFullMapVisible ||
            uiState.isMapLegendVisible ||
            uiState.tutorialState.current != null

    DisposableEffect(Unit) {
        viewModel.setExplorationVisible(true)
        viewModel.setExplorationInteractionBlocked(false)
        onDispose {
            viewModel.setExplorationVisible(false)
            viewModel.setExplorationInteractionBlocked(true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> onEnemySelected(event.enemyIds)
                is ExplorationEvent.PlayCinematic -> Unit
                is ExplorationEvent.ShowMessage -> viewModel.showStatusMessage(event.message)
                is ExplorationEvent.ShowToast -> uiEventBus.tryEmit(
                    UiEvent.ShowToast(
                        id = event.id,
                        text = event.message
                    )
                )
                is ExplorationEvent.RewardGranted -> viewModel.showStatusMessage(formatRewardMessage(event.reward))
                is ExplorationEvent.ItemGranted -> viewModel.showStatusMessage("Received ${event.quantity} x ${event.itemName}")
                is ExplorationEvent.XpGained -> viewModel.showStatusMessage("Gained ${event.amount} XP")
                is ExplorationEvent.QuestAdvanced -> viewModel.showStatusMessage("${event.questId ?: "Quest"} advanced")
                is ExplorationEvent.QuestUpdated -> viewModel.showStatusMessage("Quest log updated")
                is ExplorationEvent.RoomStateChanged -> viewModel.showStatusMessage(formatRoomStateMessage(event.stateKey, event.value))
                is ExplorationEvent.SpawnEncounter -> viewModel.showStatusMessage("Encounter ${event.encounterId ?: "unknown"} triggered")
                is ExplorationEvent.BeginNode -> Unit
                is ExplorationEvent.TutorialRequested -> Unit
                is ExplorationEvent.GroundItemSpawned -> viewModel.showStatusMessage(event.message)
                is ExplorationEvent.RoomSearchUnlocked -> viewModel.showStatusMessage(event.note ?: "A hidden stash is now accessible")
                is ExplorationEvent.ItemUsed -> viewModel.showStatusMessage(event.message ?: formatItemUseResult(event.result))
                is ExplorationEvent.OpenTinkering -> onOpenTinkering(event.shopId)
                is ExplorationEvent.OpenFirstAid -> onOpenFirstAid(event.stationId)
                is ExplorationEvent.OpenFishing -> onOpenFishing(event.zoneId)
                is ExplorationEvent.OpenShop -> onOpenShop(event.shopId)
                is ExplorationEvent.CombatOutcome -> viewModel.showStatusMessage(event.message)
                is ExplorationEvent.AudioCommands -> audioCuePlayer.execute(event.commands)
                is ExplorationEvent.AudioSettingsChanged -> {
                    audioCuePlayer.setUserMusicGain(event.musicVolume)
                    audioCuePlayer.setUserSfxGain(event.sfxVolume)
                }
                is ExplorationEvent.ReturnToHub -> onReturnToHub()
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

    LaunchedEffect(uiState.isMenuOverlayVisible) {
        if (!uiState.isMenuOverlayVisible) {
            pendingInventoryItem = null
            showInventoryTargetDialog = false
        }
    }

    val menuPartyMembers = uiState.partyStatus.members
    val onUsePreviewItem: (InventoryPreviewItemUi) -> Unit = { item ->
        val effect = item.effect
        if (effect == null) {
            viewModel.showStatusMessage("${item.name} can't be used right now.")
        } else {
            val targetMode = effect.target?.lowercase(Locale.getDefault()) ?: "any"
            if (targetMode == "party" || menuPartyMembers.isEmpty()) {
                viewModel.useInventoryItem(item.id, null)
            } else {
                pendingInventoryItem = item
                showInventoryTargetDialog = true
            }
        }
    }

    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    val backgroundPainter = rememberRoomBackgroundPainter(uiState.currentRoom?.backgroundImage)
    val fadeCommand = uiState.fadeOverlay
    val fadeOverlayAnim = remember(uiState.forceBlackScreen) {
        CoreAnimatable(if (uiState.forceBlackScreen) 1f else 0f)
    }

    LaunchedEffect(fadeCommand?.id) {
        val command = fadeCommand ?: return@LaunchedEffect
        fadeOverlayAnim.snapTo(command.fromAlpha)
        fadeOverlayAnim.animateTo(
            targetValue = command.toAlpha,
            animationSpec = tween(durationMillis = command.durationMillis)
        )
        viewModel.onFadeOverlayFinished(command.id)
    }

    BackHandler {
        if (uiState.isMenuOverlayVisible) {
            viewModel.closeMenuOverlay()
        }
    }

    val baseModifier = Modifier
        .fillMaxSize()
        .pointerInput(uiState.availableConnections, uiState.blockedDirections, blockingOverlayActive) {
            if (blockingOverlayActive) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                    }
                )
            } else {
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
        }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = baseModifier) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

        val currentRoom = uiState.currentRoom
        val isWeatherLab = currentRoom?.id == "weather_lab"
        fun cycleDebugWeather(offset: Int) {
            val currentIndex = weatherCycles.indexOf(debugWeatherOverride).takeIf { it >= 0 } ?: 0
            val nextIndex = Math.floorMod(currentIndex + offset, weatherCycles.size)
            debugWeatherOverride = weatherCycles[nextIndex]
        }
        val isRoomDark = remember(currentRoom, uiState.roomState, uiState.mineGeneratorOnline, uiState.darkCapableRooms) {
            val darkState = uiState.roomState["dark"]
            val lightState = uiState.roomState["light_on"]
            var resolved = when {
                darkState == true -> true
                darkState == false -> false
                lightState == false -> true
                lightState == true -> false
                else -> currentRoom?.dark == true
            }
            val isMineRoom = currentRoom?.env.equals("mine", ignoreCase = true)
            val isDarkCapable = currentRoom?.id?.let { uiState.darkCapableRooms.contains(it) } == true
            if (!isDarkCapable) {
                resolved = false
            } else if (isMineRoom && uiState.mineGeneratorOnline) {
                resolved = false
            }
            resolved
        }
        val baseRoomDescription = remember(currentRoom, uiState.roomState, isRoomDark) {
            currentRoom?.let { room ->
                if (isRoomDark) {
                    "It's too dark to make out the room."
                } else {
                    room.description
                }
            }
        }

        val darknessAlpha by animateFloatAsState(
            targetValue = if (isRoomDark) 0.9f else 0f,
            animationSpec = tween(durationMillis = 320)
        )
        if (darknessAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B18).copy(alpha = darknessAlpha))
            )
        }
        val activeWeatherId = debugWeatherOverride ?: currentRoom?.weather ?: defaultWeatherForEnvironment(currentRoom?.env)
        WeatherOverlay(
            weatherId = activeWeatherId,
            modifier = Modifier.fillMaxSize()
        )
        val vignetteIntensity = if (isRoomDark) 0.2f else 0.0f
        VignetteOverlay(
            visible = uiState.settings.vignetteEnabled && vignetteIntensity > 0f,
            intensity = vignetteIntensity,
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        )
        val actionHints = uiState.actionHints
        val activeTheme = uiState.theme.takeUnless { isRoomDark }
        val activeThemeStyle = uiState.themeStyle.takeUnless { isRoomDark }
        val panelBorderColor = themeColor(activeTheme?.border, Color.White.copy(alpha = 0.6f))
        val panelBackgroundColor = themeColor(activeTheme?.bg, Color(0xFF050A12)).copy(alpha = 0.5f)
        val roomTextColor = if (isRoomDark) {
            Color.White.copy(alpha = 0.92f)
        } else {
            themeColor(activeTheme?.fg, Color.White.copy(alpha = 0.92f))
        }
        val inlinePlan = remember(baseRoomDescription, uiState.actions, actionHints, currentRoom) {
            buildInlineActionPlan(
                description = baseRoomDescription,
                actions = uiState.actions,
                hints = actionHints,
                room = currentRoom
            )
        }
        val descriptionForPanel = baseRoomDescription
        val visibleNpcs = if (isRoomDark) emptyList() else uiState.npcs
        val visibleGroundItems = if (isRoomDark) emptyMap() else uiState.groundItems
        val serviceQuickActions = remember(uiState.actions, uiState.actionHints, currentRoom?.id, inlinePlan) {
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
        val hasRoomEntities = visibleNpcs.isNotEmpty() ||
            visibleGroundItems.isNotEmpty() ||
            serviceQuickActions.isNotEmpty()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            val actionAccentColor = themeColor(activeTheme?.accent, Color(0xFF80E0FF))
            ThemeBandOverlay(
                theme = activeTheme,
                bandStyle = activeThemeStyle?.bands,
                darkness = if (isRoomDark) 0.85f else 0f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(96.dp)
            )
            val minimapSize = 78.dp
            val titleColor = themeColor(activeTheme?.accent, Color(0xFFBEE9FF))
            val warmTitleColor = Color(0xFFFF9F2E)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RoomHeaderPanel(
                    roomTitle = uiState.currentRoom?.title ?: "Unknown area",
                    isDark = isRoomDark,
                    titleColor = titleColor,
                    warmTitleColor = warmTitleColor,
                    minimap = uiState.minimap,
                    minimapSize = minimapSize,
                    onTitleClick = {
                        val nextIdx = (weatherCycles.indexOf(debugWeatherOverride) + 1) % weatherCycles.size
                        debugWeatherOverride = weatherCycles[nextIdx]
                    },
                    onMapClick = {
                        viewModel.selectMenuTab(MenuTab.MAP)
                        viewModel.openMenuOverlay(MenuTab.MAP)
                    }
                )

                if (!isRoomDark) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoomDescriptionPanel(
                            currentRoom = currentRoom,
                            description = descriptionForPanel,
                            plan = inlinePlan,
                            isDark = false,
                            onAction = { action -> viewModel.onActionSelected(action) },
                            onNpcClick = { name -> viewModel.onNpcInteraction(name) },
                            onEnemyClick = { enemyId -> viewModel.engageEnemy(enemyId) },
                            borderColor = panelBorderColor,
                            accentColor = actionAccentColor,
                            textColor = roomTextColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 104.dp, max = 280.dp)
                        )
                        if (hasRoomEntities) {
                            RoomEntitySection(
                                npcs = visibleNpcs,
                                npcPresenceNames = uiState.npcPresenceNames,
                                npcPortraitPaths = uiState.npcPortraitPaths,
                                groundItems = visibleGroundItems,
                                serviceActions = serviceQuickActions,
                                itemDisplayName = { itemId -> viewModel.itemDisplayName(itemId) },
                                itemDetailLabel = { itemId -> viewModel.roomItemDetailLabel(itemId) },
                                itemIsEquipment = { itemId -> viewModel.roomItemIsEquipment(itemId) },
                                accentColor = actionAccentColor,
                                borderColor = panelBorderColor,
                                isDark = false,
                                onNpcClick = { name -> viewModel.onNpcInteraction(name) },
                                onCollectItem = { itemId -> viewModel.collectGroundItem(itemId) },
                                onCollectAll = { viewModel.collectAllGroundItems() },
                                onAction = { action -> viewModel.onActionSelected(action) }
                            )
                        }
                    }
                }
            }

            if (!isRoomDark && uiState.visualEnemyParties.isNotEmpty()) {
                EnemyPresenceStage(
                    visualParties = uiState.visualEnemyParties,
                    enemyTiers = uiState.enemyTiers,
                    enemyIcons = uiState.enemyIcons,
                    accentColor = actionAccentColor,
                    isDark = isRoomDark,
                    roomId = uiState.currentRoom?.id,
                    onPartyClick = { enemyId -> viewModel.engageEnemy(enemyId) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(min = 250.dp, max = 410.dp)
                        .padding(start = 4.dp, end = 4.dp, bottom = 64.dp)
                )
            }


            if (isWeatherLab) {
                WeatherLabPanel(
                    activeWeatherId = activeWeatherId,
                    onPrevious = { cycleDebugWeather(-1) },
                    onNext = { cycleDebugWeather(1) },
                    accentColor = actionAccentColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp)
                        .zIndex(8f)
                )
            }
        }
        uiState.narrationPrompt?.let { narration ->
            InspectionOverlay(
                prompt = narration,
                theme = activeTheme,
                onDismiss = { viewModel.dismissNarration() },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
            )
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
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
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

        val menuScrimColor = themeColor(activeTheme?.bg, Color.Black)
        val menuScrimInteraction = remember { MutableInteractionSource() }
        val scrimAlpha by animateFloatAsState(
            targetValue = if (uiState.isMenuOverlayVisible) 0.75f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "menuScrimAlpha"
        )
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .zIndex(0.5f)
                    .background(menuScrimColor.copy(alpha = scrimAlpha))
                    .clickable(
                        enabled = uiState.isMenuOverlayVisible,
                        indication = null,
                        interactionSource = menuScrimInteraction
                    ) { viewModel.closeMenuOverlay() }
            )
        }
        AnimatedVisibility(
            visible = uiState.isMenuOverlayVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            val trackedQuest = uiState.questLogActive.firstOrNull { it.id == uiState.trackedQuestId }
            MenuOverlay(
                selectedTab = uiState.menuTab,
                onSelectTab = { viewModel.selectMenuTab(it) },
                onClose = { viewModel.closeMenuOverlay() },
                onOpenInventory = {
                    viewModel.openMenuOverlay(MenuTab.INVENTORY)
                },
                onOpenJournal = {
                    viewModel.closeMenuOverlay()
                    viewModel.openQuestLog()
                },
                onOpenMapLegend = {
                    viewModel.openMapLegend()
                },
                onOpenFullMap = {
                    viewModel.openFullMapOverlay()
                },
                settings = uiState.settings,
                onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                onSfxVolumeChange = { viewModel.updateSfxVolume(it) },
                onToggleTutorials = { viewModel.updateTutorialsEnabled(it) },
                onToggleVignette = { viewModel.setVignetteEnabled(it) },
                onQuickSave = { viewModel.quickSave() },
                onSaveGame = {
                    coroutineScope.launch {
                        slotSummaries = viewModel.fetchSaveSlots()
                        saveLoadMode = "save"
                    }
                },
                onLoadGame = {
                    coroutineScope.launch {
                        slotSummaries = viewModel.fetchSaveSlots()
                        saveLoadMode = "load"
                    }
                },
                partyStatus = uiState.partyStatus,
                onShowSkillTree = { memberId -> viewModel.openSkillTree(memberId) },
                onShowDetails = { memberId -> viewModel.openPartyMemberDetails(memberId) },
                statusMessage = uiState.statusMessage,
                trackedQuest = trackedQuest,
                activeQuests = uiState.questLogActive,
                completedQuests = uiState.questLogCompleted,
                minimap = uiState.minimap,
                fullMap = uiState.fullMap,
                theme = activeTheme,
                isCurrentRoomDark = isRoomDark,
                onMenuAction = { viewModel.onMenuActionInvoked() },
                creditsLabel = uiState.progressionSummary.creditsLabel,
                inventoryItems = uiState.inventoryPreview,
                equippedItems = uiState.equippedItems,
                completedMilestones = uiState.completedMilestones,
                unlockedWeapons = uiState.unlockedWeapons,
                equippedWeapons = uiState.equippedWeapons,
                unlockedArmors = uiState.unlockedArmors,
                equippedArmors = uiState.equippedArmors,
                onEquipItem = { slot, itemId, characterId ->
                    viewModel.equipInventoryItem(slot, itemId, characterId)
                },
                onEquipMod = { slot, itemId, characterId ->
                    viewModel.equipInventoryMod(slot, itemId, characterId)
                },
                onEquipWeapon = { characterId, weaponId ->
                    viewModel.equipWeapon(characterId, weaponId)
                },
                resolveWeaponItem = viewModel::weaponItem,
                onEquipArmor = { characterId, armorId ->
                    viewModel.equipArmor(characterId, armorId)
                },
                resolveArmorItem = viewModel::armorItem,
                onUseInventoryItem = onUsePreviewItem,
                onShowQuestDetails = { questId ->
                    viewModel.openQuestDetails(questId)
                },
                modifier = Modifier.statusBarsPadding()
            )
        }

        if (showInventoryTargetDialog && pendingInventoryItem != null) {
            val targetOptions = menuPartyMembers.map {
                TargetSelectionOption(
                    id = it.id,
                    name = it.name,
                    detail = it.hpLabel
                )
            }
            ItemTargetSelectionDialog(
                itemName = pendingInventoryItem!!.name,
                targets = targetOptions,
                onSelect = { targetId ->
                    viewModel.useInventoryItem(pendingInventoryItem!!.id, targetId)
                    pendingInventoryItem = null
                    showInventoryTargetDialog = false
                },
                onDismiss = {
                    pendingInventoryItem = null
                    showInventoryTargetDialog = false
                },
                backgroundColor = themeColor(activeTheme?.bg, Color.Black).copy(alpha = 0.7f),
                borderColor = themeColor(activeTheme?.border, Color.White.copy(alpha = 0.3f)),
                textColor = themeColor(activeTheme?.fg, Color.White),
                accentColor = themeColor(activeTheme?.accent, Color.White)
            )
        }
        if (saveLoadMode != null) {
            SaveLoadDialog(
                mode = saveLoadMode!!,
                slots = slotSummaries,
                onSave = { slot ->
                    coroutineScope.launch {
                        viewModel.saveGame(slot)
                        slotSummaries = viewModel.fetchSaveSlots()
                        saveLoadMode = null
                    }
                },
                onLoad = { slot ->
                    coroutineScope.launch {
                        viewModel.loadGame(slot)
                        saveLoadMode = null
                    }
                },
                onDelete = { slot ->
                    coroutineScope.launch {
                        viewModel.deleteGame(slot)
                        slotSummaries = viewModel.fetchSaveSlots()
                    }
                },
                onRefresh = {
                    coroutineScope.launch {
                        slotSummaries = viewModel.fetchSaveSlots()
                    }
                },
                onDismiss = { saveLoadMode = null },
                accentColor = themeColor(activeTheme?.accent, Color(0xFF7BE4FF)),
                panelColor = themeColor(activeTheme?.bg, Color(0xFF0B111A)).copy(alpha = 0.96f),
                borderColor = themeColor(activeTheme?.border, Color.White.copy(alpha = 0.16f)),
                textColor = themeColor(activeTheme?.fg, Color.White)
            )
        }

        if (fadeOverlayAnim.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeOverlayAnim.value))
                    .align(Alignment.Center)
                    .zIndex(10f)
            )
        }

        if (uiState.prompt == null && !blockingOverlayActive && !uiState.isMenuOverlayVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.canReturnToHub) {
                    ReturnHubButton(
                        onClick = { viewModel.requestReturnToHub() }
                    )
                } else {
                    Spacer(modifier = Modifier.width(116.dp))
                }
                MenuToggleButton(
                    isOpen = uiState.isMenuOverlayVisible,
                    onToggle = {
                        if (uiState.isMenuOverlayVisible) {
                            viewModel.closeMenuOverlay()
                        } else {
                            viewModel.openMenuOverlay()
                        }
                    },
                    enabled = !blockingOverlayActive
                )
            }
        }

        uiState.cinematic?.let { cinematic ->
            CinematicOverlay(
                state = cinematic,
                onAdvance = { viewModel.advanceCinematic() },
                modifier = Modifier.fillMaxSize()
            )
        }

        uiState.skillTreeOverlay?.let { overlay ->
            SkillTreeOverlay(
                overlay = overlay,
                theme = activeTheme,
                onClose = { viewModel.closeSkillTreeOverlay() },
                onUnlockSkill = { viewModel.unlockSkillNode(it) }
            )
        }

        uiState.partyMemberDetails?.let { details ->
            PartyMemberDetailsDialog(
                details = details,
                onDismiss = { viewModel.closePartyMemberDetails() }
            )
        }

        uiState.eventAnnouncement?.let { announcement ->
            EventAnnouncementOverlay(
                announcement = announcement,
                theme = activeTheme,
                onDismiss = { viewModel.dismissEventAnnouncement() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(60f)
            )
        }

        uiState.levelUpPrompt?.let { prompt ->
            LevelUpOverlay(
                prompt = prompt,
                onDismiss = { viewModel.dismissLevelUpPrompt() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
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
                onSelectQuest = { questId ->
                    viewModel.openQuestDetails(questId)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (uiState.isFullMapVisible) {
            FullMapOverlay(
                fullMap = uiState.fullMap,
                onClose = { viewModel.closeFullMapOverlay() }
            )
        }

        if (uiState.isMapLegendVisible) {
            MapLegendOverlay(
                onClose = { viewModel.closeMapLegend() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .zIndex(50f),
                accentColor = themeColor(activeTheme?.accent, Color(0xFF7BE4FF)),
                panelColor = themeColor(activeTheme?.bg, Color(0xFF050A12)).copy(alpha = 0.94f),
                borderColor = themeColor(activeTheme?.border, Color.White.copy(alpha = 0.25f)),
                textColor = themeColor(activeTheme?.fg, Color.White)
            )
        }

        if (uiState.isMilestoneGalleryVisible) {
            MilestoneGalleryOverlay(
                history = uiState.milestoneHistory,
                currentRoom = uiState.currentRoom,
                onClose = { viewModel.closeMilestoneGallery() }
            )
        }

        if (uiState.eventAnnouncement == null && !uiState.isMenuOverlayVisible) {
            UIPromptOverlay(
                prompt = uiState.prompt,
                onDismiss = { viewModel.dismissPrompt() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        val questAccentColor = themeColor(activeTheme?.accent, Color(0xFF80E0FF))

        QuestBannerOverlay(
            uiEventBus = uiEventBus,
            deferShowing = blockingOverlayActive,
            accentColor = questAccentColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        ProgressToastOverlay(
            uiEventBus = uiEventBus,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 32.dp)
        )

        QuestDetailOverlay(
            uiEventBus = uiEventBus,
            gradientColor = questAccentColor,
            outlineColor = panelBorderColor,
            deferShowing = blockingOverlayActive,
            onShowDetails = { questId ->
                viewModel.openQuestDetails(questId)
            }
        )

        QuestSummaryOverlay(
            uiEventBus = uiEventBus,
            isSceneBlocking = blockingOverlayActive,
            modifier = Modifier.align(Alignment.Center),
            onShowDetails = { questId ->
                viewModel.openQuestDetails(questId)
            }
        )

        uiState.questDetail?.let { detail ->
            QuestDetailSheet(
                detail = detail,
                accentColor = questAccentColor,
                onClose = { viewModel.closeQuestDetails() },
                onToggleTrack = { questId -> viewModel.toggleQuestTracking(questId) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .zIndex(2f)
            )
        }

        CraftingFxOverlay(
            bursts = fxBursts,
            onExpired = { id -> fxBursts.removeAll { it.id == id } },
            modifier = Modifier.fillMaxSize()
        )

        DirectionIndicatorsOverlay(
            indicators = uiState.directionIndicators,
            onTravel = viewModel::travel,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp)
        )
    }
}
}

@Composable
private fun RoomHeaderPanel(
    roomTitle: String,
    isDark: Boolean,
    titleColor: Color,
    warmTitleColor: Color,
    minimap: MinimapUiState?,
    minimapSize: Dp,
    onTitleClick: () -> Unit,
    onMapClick: () -> Unit
) {
    if (isDark) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF02060D).copy(alpha = 0.76f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTitleClick() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Dark Room",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "It's too dark to see what's here.",
                    color = Color.White.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    val shape = RoundedCornerShape(10.dp)
    val panelColor = Color(0xFF061018).copy(alpha = 0.54f)
    val borderColor = titleColor.copy(alpha = 0.36f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = panelColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            warmTitleColor.copy(alpha = 0.08f),
                            titleColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTitleClick() },
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = roomTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.62f),
                            offset = Offset(0f, 1.25f),
                            blurRadius = 1.5f
                        )
                    ),
                    color = warmTitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    warmTitleColor.copy(alpha = 0.76f),
                                    titleColor.copy(alpha = 0.34f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            MinimapWidget(
                minimap = minimap,
                onLegend = onMapClick,
                obscured = isDark,
                modifier = Modifier.requiredSize(minimapSize)
            )
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

@Composable
private fun QuestDetailSheet(
    detail: QuestDetailUi,
    accentColor: Color,
    onClose: () -> Unit,
    onToggleTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xF00A111E),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Stage ${detail.stageIndex + 1} of ${detail.totalStages}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            detail.summary.takeIf { it.isNotBlank() }?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            detail.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            detail.stageTitle?.let { stageTitle ->
                Text(
                    text = stageTitle,
                    style = MaterialTheme.typography.titleMedium.copy(color = accentColor),
                    color = accentColor
                )
            }
            detail.stageDescription?.takeIf { it.isNotBlank() }?.let { stageDescription ->
                Text(
                    text = stageDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Objectives",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                if (detail.objectives.isEmpty()) {
                    Text(
                        text = "No objectives listed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    detail.objectives.forEach { objective ->
                        QuestObjectiveRow(objective = objective, accentColor = accentColor)
                    }
                }
            }
            if (detail.rewards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rewards",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    detail.rewards.forEach { reward ->
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = reward,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val trackLabel = if (detail.tracked) "Stop Tracking" else "Track Quest"
                Button(
                    onClick = { onToggleTrack(detail.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.tracked) accentColor else Color.Transparent,
                        contentColor = if (detail.tracked) Color(0xFF010308) else accentColor
                    ),
                    border = if (detail.tracked) null else BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
                ) {
                    Text(trackLabel)
                }
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun QuestObjectiveRow(
    objective: QuestObjectiveUi,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val icon = if (objective.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked
        val tint = if (objective.completed) accentColor else Color.White.copy(alpha = 0.7f)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = objective.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
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

fun DrawScope.drawDarkRoomOverlay(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    overlayColor: Color,
    hatchColor: Color,
    hatchSpacing: Float
) {
    if (size.width <= 0f || size.height <= 0f) return
    val roundRect = RoundRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        cornerRadius = cornerRadius
    )
    val shapePath = Path().apply { addRoundRect(roundRect) }
    drawPath(shapePath, color = overlayColor)
    val canvas = drawContext.canvas
    canvas.save()
    canvas.clipPath(shapePath, ClipOp.Intersect)
    val strokeWidth = 1.dp.toPx().coerceAtLeast(0.5f)
    val spacing = hatchSpacing.coerceAtLeast(strokeWidth * 3f)
    var offset = -size.height
    while (offset < size.width + size.height) {
        val start = Offset(topLeft.x + offset, topLeft.y)
        val end = Offset(start.x + size.height * 1.2f, topLeft.y + size.height * 1.2f)
        drawLine(
            color = hatchColor,
            start = start,
            end = end,
            strokeWidth = strokeWidth
        )
        offset += spacing
    }
    canvas.restore()
}



@Composable
private fun MenuOverlay(
    selectedTab: MenuTab,
    onSelectTab: (MenuTab) -> Unit,
    onClose: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit,
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    partyStatus: PartyStatusUi,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    statusMessage: String?,
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    theme: Theme?,
    isCurrentRoomDark: Boolean,
    onMenuAction: () -> Unit,
    creditsLabel: String,
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseInventoryItem: (InventoryPreviewItemUi) -> Unit,
    onShowQuestDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeBase = themeColor(theme?.bg, Color(0xFF071018))
    val panelColor = Color(
        red = themeBase.red * 0.45f,
        green = themeBase.green * 0.45f,
        blue = themeBase.blue * 0.45f,
        alpha = 0.97f
    )
    val panelBorder = Color(0xFF7FE6FF)
    val accentColor = Color(0xFF7FE6FF)
    val warmAccent = Color(0xFFFFC857)
    val sheetScroll = rememberScrollState()
    val cornerRadius = 14.dp
    val borderWidth = 1.dp
    val borderColor = panelBorder.copy(alpha = 0.62f)
    val contentMaxWidth = 860.dp

    Surface(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val strokeWidth = borderWidth.toPx()
                val halfStroke = strokeWidth / 2f
                val left = halfStroke
                val top = halfStroke
                val right = size.width - halfStroke
                val bottom = size.height - halfStroke
                if (right > left && bottom > top) {
                    val radiusMax = min(right - left, bottom - top) / 2f
                    val radius = min(cornerRadius.toPx(), radiusMax).coerceAtLeast(0f)
                    val path = Path().apply {
                        moveTo(left, bottom)
                        lineTo(left, top + radius)
                        if (radius > 0f) {
                            arcTo(
                                rect = Rect(left, top, left + 2 * radius, top + 2 * radius),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(right - radius, top)
                            arcTo(
                                rect = Rect(right - 2 * radius, top, right, top + 2 * radius),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                        } else {
                            lineTo(left, top)
                            lineTo(right, top)
                        }
                        lineTo(right, bottom)
                    }
                    drawPath(
                        path = path,
                        color = borderColor,
                        style = Stroke(width = strokeWidth)
                    )
                }
            },
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        color = panelColor,
        border = null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.72f),
                                warmAccent.copy(alpha = 0.34f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = contentMaxWidth)
                        .verticalScroll(sheetScroll),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "FIELD MENU",
                                style = MaterialTheme.typography.labelMedium,
                                color = warmAccent.copy(alpha = 0.74f)
                            )
                            Text(
                                text = selectedTab.label(),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 34.sp),
                                color = warmAccent
                            )
                        }
                        Surface(
                            onClick = onClose,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF061018).copy(alpha = 0.42f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    MenuTabRow(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab,
                        accentColor = accentColor,
                        borderColor = panelBorder
                    )

                    MenuTabContentArea(
                        tab = selectedTab,
                        accentColor = accentColor,
                        borderColor = panelBorder,
                        isCurrentRoomDark = isCurrentRoomDark,
                        statusMessage = statusMessage,
                        partyStatus = partyStatus,
                        trackedQuest = trackedQuest,
                        activeQuests = activeQuests,
                        completedQuests = completedQuests,
                        minimap = minimap,
                        fullMap = fullMap,
                        settings = settings,
                        onMenuAction = onMenuAction,
                        onOpenInventory = onOpenInventory,
                        onOpenJournal = onOpenJournal,
                        onOpenMapLegend = onOpenMapLegend,
                        onOpenFullMap = onOpenFullMap,
                        onMusicVolumeChange = onMusicVolumeChange,
                        onSfxVolumeChange = onSfxVolumeChange,
                        onToggleTutorials = onToggleTutorials,
                        onToggleVignette = onToggleVignette,
                        onQuickSave = onQuickSave,
                        onSaveGame = onSaveGame,
                        onLoadGame = onLoadGame,
                        onShowSkillTree = onShowSkillTree,
                        onShowDetails = onShowDetails,
                        inventoryItems = inventoryItems,
                        equippedItems = equippedItems,
                        completedMilestones = completedMilestones,
                        unlockedWeapons = unlockedWeapons,
                        equippedWeapons = equippedWeapons,
                        unlockedArmors = unlockedArmors,
                        equippedArmors = equippedArmors,
                        onEquipItem = onEquipItem,
                        onEquipMod = onEquipMod,
                        onEquipWeapon = onEquipWeapon,
                        resolveWeaponItem = resolveWeaponItem,
                        onEquipArmor = onEquipArmor,
                        resolveArmorItem = resolveArmorItem,
                        onUseInventoryItem = onUseInventoryItem,
                        onShowQuestDetails = onShowQuestDetails,
                        creditsLabel = creditsLabel
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = contentMaxWidth)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                    .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.09f),
                                    warmAccent.copy(alpha = 0.04f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun MenuTabRow(
    selectedTab: MenuTab,
    onSelectTab: (MenuTab) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .widthIn(max = 1200.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF061018).copy(alpha = 0.38f))
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(3.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        MenuTab.values().forEach { tab ->
            MenuTabChip(
                tab = tab,
                isSelected = tab == selectedTab,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { onSelectTab(tab) }
            )
        }
    }
}

@Composable
private fun MenuTabChip(
    tab: MenuTab,
    isSelected: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val background = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.28f),
                Color(0xFFFFC857).copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
    val contentColor = if (isSelected) Color.White else accentColor.copy(alpha = 0.78f)
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isSelected) 0.72f else 0.0f)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .widthIn(min = 92.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.label(),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MenuTabContentArea(
    tab: MenuTab,
    accentColor: Color,
    borderColor: Color,
    isCurrentRoomDark: Boolean,
    statusMessage: String?,
    partyStatus: PartyStatusUi,
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    settings: SettingsUiState,
    onMenuAction: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseInventoryItem: (InventoryPreviewItemUi) -> Unit,
    onShowQuestDetails: (String) -> Unit,
    creditsLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (tab) {
            MenuTab.INVENTORY -> InventoryTabContent(
                inventoryItems = inventoryItems,
                equippedItems = equippedItems,
                completedMilestones = completedMilestones,
                unlockedWeapons = unlockedWeapons,
                equippedWeapons = equippedWeapons,
                unlockedArmors = unlockedArmors,
                equippedArmors = equippedArmors,
                partyMembers = partyStatus.members,
                accentColor = accentColor,
                borderColor = borderColor,
                onEquipItem = onEquipItem,
                onEquipMod = onEquipMod,
                onEquipWeapon = onEquipWeapon,
                resolveWeaponItem = resolveWeaponItem,
                onEquipArmor = onEquipArmor,
                resolveArmorItem = resolveArmorItem,
                onUseConsumable = onUseInventoryItem,
                creditsLabel = creditsLabel
            )
            MenuTab.JOURNAL -> JournalTabContent(
                trackedQuest = trackedQuest,
                activeQuests = activeQuests,
                completedQuests = completedQuests,
                accentColor = accentColor,
                borderColor = borderColor,
                onQuestSelected = onShowQuestDetails
            )
            MenuTab.MAP -> MapTabContent(
                minimap = minimap,
                fullMap = fullMap,
                isCurrentRoomDark = isCurrentRoomDark,
                accentColor = accentColor,
                borderColor = borderColor,
                onMenuAction = onMenuAction,
                onOpenMapLegend = onOpenMapLegend,
                onOpenFullMap = onOpenFullMap
            )
            MenuTab.STATS -> StatsTabContent(
                partyStatus = partyStatus,
                accentColor = accentColor,
                borderColor = borderColor,
                onShowSkillTree = onShowSkillTree,
                onShowDetails = onShowDetails
            )
            MenuTab.SETTINGS -> SettingsTabContent(
                settings = settings,
                accentColor = accentColor,
                borderColor = borderColor,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette,
                onQuickSave = onQuickSave,
                onSaveGame = onSaveGame,
                onLoadGame = onLoadGame
            )
        }
    }
}









@Composable
fun MenuSectionCard(
    title: String,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF071018).copy(alpha = 0.48f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.42f)),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title.uppercase(Locale.getDefault()),
                    color = accentColor.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.5f),
                                    Color(0xFFFFC857).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                content()
            }
        }
    }
}





@Composable
private fun StatusTicker(
    statusMessage: String?,
    accentColor: Color
) {
    val displayText = statusMessage ?: "All systems normal."
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(accentColor, CircleShape)
        )
        Text(
            text = displayText,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun WeatherLabPanel(
    activeWeatherId: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val label = activeWeatherId
        ?.replace('_', ' ')
        ?.uppercase(Locale.getDefault())
        ?: "NONE"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xE6071018),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.62f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.72f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("PREV")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WEATHER LAB",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(
                onClick = onNext,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                )
            ) {
                Text("NEXT")
            }
        }
    }
}

@Composable
private fun QuickContextActions(
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onMenuAction: () -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Shortcuts",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ThemedMenuButton(
                label = "Inventory",
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    onMenuAction()
                    onOpenInventory()
                }
            )
            ThemedMenuButton(
                label = "Journal",
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    onMenuAction()
                    onOpenJournal()
                }
            )
        }
    }
}

@Composable
private fun QuestPreviewCard(
    quest: QuestSummaryUi?,
    onQuestSelected: ((QuestSummaryUi) -> Unit)? = null
) {
    if (quest == null) {
        Text(
            text = "No quest tracked. Open the Journal to select one.",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = if (onQuestSelected != null) {
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .clickable { onQuestSelected(quest) }
                .padding(14.dp)
        } else Modifier
    ) {
        Text(
            text = quest.title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
        )
        quest.stageTitle?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        quest.stageDescription?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}



@Composable
private fun StatBar(
    progress: Float,
    label: String,
    accentColor: Color
) {
    Column {
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}



@Composable
fun ThemedMenuButton(
    label: String,
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.52f else 0.22f)),
        color = if (enabled) Color(0xFF061018).copy(alpha = 0.45f) else Color.Gray.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = if (enabled) 0.35f else 0.15f),
                            Color(0xFFFFC857).copy(alpha = if (enabled) 0.10f else 0f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FullMapCard(
    fullMap: FullMapUiState?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Full Map",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (fullMap == null || fullMap.cells.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Text(
                        text = "Full map data unavailable yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FullMapCanvas(fullMap = fullMap)
            }
        }
    }
}

@Composable
private fun FullMapOverlay(
    fullMap: FullMapUiState?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 10.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Full Map",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
                if (fullMap == null || fullMap.cells.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Map data unavailable.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val cells = fullMap.cells
                    var viewportScale by remember(cells) { mutableStateOf(1f) }
                    var viewportOffset by remember(cells) { mutableStateOf(Offset.Zero) }
                    val transformModifier = Modifier.pointerInput(cells) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewportScale = (viewportScale * zoom).coerceIn(0.4f, 4f)
                            viewportOffset += pan
                        }
                    }
                    FullMapCanvas(
                        fullMap = fullMap,
                        scale = viewportScale,
                        offset = viewportOffset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .then(transformModifier)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                viewportScale = 1f
                                viewportOffset = Offset.Zero
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text("Recenter")
                        }
                        Button(
                            onClick = onClose,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLegendOverlay(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color,
    panelColor: Color,
    borderColor: Color,
    textColor: Color
) {
    val visitedColor = textColor.copy(alpha = 0.7f)
    val discoveredColor = textColor.copy(alpha = 0.4f)
    val unexploredColor = textColor.copy(alpha = 0.16f)
    val connectionColor = textColor.copy(alpha = 0.5f)
    val playerIndicatorColor = Color(1f, 0.92f, 0.35f, 1f)
    val darkOverlayColor = Color.Black.copy(alpha = 0.72f)
    val darkHatchColor = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = panelColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Map Legend",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor
                )
                Text(
                    text = "Symbols for the map and minimap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.78f)
                )

                LegendEntry(
                    title = "Current room",
                    description = "You are here.",
                    textColor = textColor
                ) {
                    LegendCell(
                        fillColor = accentColor,
                        borderColor = borderColor
                    )
                }

                LegendEntry(
                    title = "Visited room",
                    description = "Rooms you've already explored.",
                    textColor = textColor
                ) {
                    LegendCell(
                        fillColor = visitedColor,
                        borderColor = borderColor
                    )
                }

                LegendEntry(
                    title = "Discovered room",
                    description = "Revealed but not yet entered.",
                    textColor = textColor
                ) {
                    LegendCell(
                        fillColor = discoveredColor,
                        borderColor = borderColor
                    )
                }

                LegendEntry(
                    title = "Undiscovered slot",
                    description = "Future cells that appear when paths open.",
                    textColor = textColor
                ) {
                    LegendCell(
                        fillColor = unexploredColor,
                        borderColor = borderColor.copy(alpha = 0.4f)
                    )
                }

                LegendEntry(
                    title = "Dark room",
                    description = "Unlit areas are hatched until you light them.",
                    textColor = textColor
                ) {
                    LegendCell(
                        fillColor = visitedColor,
                        borderColor = borderColor,
                        overlayColor = darkOverlayColor,
                        hatchColor = darkHatchColor
                    )
                }

                LegendEntry(
                    title = "Connections",
                    description = "Lines show adjacency between rooms.",
                    textColor = textColor
                ) {
                    LegendConnectionsGraphic(
                        lineColor = connectionColor,
                        nodeColor = textColor
                    )
                }

                LegendEntry(
                    title = "Player marker",
                    description = "The glowing dot on the minimap is you.",
                    textColor = textColor
                ) {
                    LegendPlayerMarker(
                        color = playerIndicatorColor
                    )
                }

                LegendEntry(
                    title = "Minimap grid",
                    description = "5×5 grid around you with a highlighted center tile.",
                    textColor = textColor
                ) {
                    LegendMiniGrid(
                        gridColor = textColor.copy(alpha = 0.35f),
                        dotColor = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Close", color = textColor)
                }
            }
        }
    }
}

@Composable
private fun LegendEntry(
    title: String,
    description: String,
    textColor: Color,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            leading()
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun LegendCell(
    fillColor: Color,
    borderColor: Color,
    overlayColor: Color? = null,
    hatchColor: Color? = null
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        drawRoundRect(
            color = fillColor,
            cornerRadius = corner
        )
        drawRoundRect(
            color = borderColor,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
        if (overlayColor != null && hatchColor != null) {
            drawDarkRoomOverlay(
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = corner,
                overlayColor = overlayColor,
                hatchColor = hatchColor,
                hatchSpacing = size.width / 3f
            )
        }
    }
}

@Composable
private fun LegendConnectionsGraphic(lineColor: Color, nodeColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val y = size.height / 2f
        val startX = 6.dp.toPx()
        val endX = size.width - 6.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = nodeColor, radius = 3.dp.toPx(), center = Offset(startX, y))
        drawCircle(color = nodeColor, radius = 3.dp.toPx(), center = Offset(endX, y))
    }
}

@Composable
private fun LegendPlayerMarker(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(color = color.copy(alpha = 0.3f), radius = size.minDimension / 2.8f, center = center)
        drawCircle(color = color, radius = size.minDimension / 5f, center = center)
    }
}

@Composable
private fun LegendMiniGrid(gridColor: Color, dotColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        val rows = 3
        val cols = 3
        val spacingX = size.width / cols
        val spacingY = size.height / rows
        for (i in 0..cols) {
            val x = i * spacingX
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (j in 0..rows) {
            val y = j * spacingY
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val px = col * spacingX + spacingX / 2f
                val py = row * spacingY + spacingY / 2f
                drawCircle(
                    color = if (row == 1 && col == 1) dotColor else gridColor.copy(alpha = 0.8f),
                    radius = if (row == 1 && col == 1) 3.5.dp.toPx() else 2.2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

@Composable
fun FullMapCanvas(
    fullMap: FullMapUiState,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    modifier: Modifier = Modifier
) {
    val cells = fullMap.cells
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val visitedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val discoveredColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val connectionColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val cellSizePx = with(density) { 18.dp.toPx() }
    val gapPx = with(density) { 6.dp.toPx() }
    val spacing = cellSizePx + gapPx
    val minX = cells.minOfOrNull { it.gridX } ?: 0
    val maxX = cells.maxOfOrNull { it.gridX } ?: 0
    val minY = cells.minOfOrNull { it.gridY } ?: 0
    val maxY = cells.maxOfOrNull { it.gridY } ?: 0
    val widthCells = (maxX - minX + 1).coerceAtLeast(1)
    val heightCells = (maxY - minY + 1).coerceAtLeast(1)
    val extraWidth = (FULL_MAP_COLUMNS - widthCells).coerceAtLeast(0)
    val extraHeight = (FULL_MAP_ROWS - heightCells).coerceAtLeast(0)
    val displayMinX = minX - (extraWidth + 1) / 2
    val displayMaxX = maxX + extraWidth / 2
    val displayMinY = minY - (extraHeight + 1) / 2
    val displayMaxY = maxY + extraHeight / 2

    val heightDp = with(density) { (spacing * FULL_MAP_ROWS).toDp() }.coerceAtLeast(200.dp)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, pivot = Offset(size.width / 2f, size.height / 2f))
        }) {
            val originX = (size.width - (displayMaxX - displayMinX + 1) * spacing) / 2f
            val originY = (size.height - (displayMaxY - displayMinY + 1) * spacing) / 2f

            cells.forEach { cell ->
                val posX = originX + (cell.gridX - displayMinX) * spacing
                val posY = originY + (displayMaxY - cell.gridY) * spacing
                val rectTopLeft = Offset(posX, posY)
                val fillColor = when {
                    cell.isCurrent -> primaryColor
                    cell.visited -> visitedColor
                    cell.discovered -> discoveredColor
                    else -> emptyColor
                }
                drawRoundRect(
                    color = fillColor,
                    topLeft = rectTopLeft,
                    size = Size(cellSizePx, cellSizePx),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = rectTopLeft,
                    size = Size(cellSizePx, cellSizePx),
                    style = Stroke(width = 1f)
                )
                if (cell.isDark) {
                    drawDarkRoomOverlay(
                        topLeft = rectTopLeft,
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(6f, 6f),
                        overlayColor = Color.Black.copy(alpha = 0.65f),
                        hatchColor = Color.White.copy(alpha = 0.08f),
                        hatchSpacing = cellSizePx / 3.5f
                    )
                }
            }

            cells.forEach { cell ->
                val startX = originX + (cell.gridX - displayMinX) * spacing + cellSizePx / 2f
                val startY = originY + (displayMaxY - cell.gridY) * spacing + cellSizePx / 2f
                cell.connections.forEach { (direction, targetId) ->
                    if (direction.equals("east", true) || direction.equals("north", true)) {
                        val target = cells.firstOrNull { it.roomId == targetId } ?: return@forEach
                        val endX = originX + (target.gridX - displayMinX) * spacing + cellSizePx / 2f
                        val endY = originY + (displayMaxY - target.gridY) * spacing + cellSizePx / 2f
                        drawLine(
                            color = connectionColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            cells.filter { it.isCurrent }.forEach { cell ->
                val centerX = originX + (cell.gridX - displayMinX) * spacing + cellSizePx / 2f
                val centerY = originY + (displayMaxY - cell.gridY) * spacing + cellSizePx / 2f
                drawCircle(
                    color = Color.White,
                    radius = cellSizePx / 6f,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

private const val FULL_MAP_COLUMNS = 16
private const val FULL_MAP_ROWS = 8



@Composable
private fun MenuToggleButton(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val description = if (isOpen) "Close menu" else "Open menu"
    val baseAlpha = if (isOpen) 0.75f else 1f
    val alpha = if (enabled) baseAlpha else baseAlpha * 0.35f
    val accentColor = Color(0xFF7FE6FF)
    val warmColor = Color(0xFFFFC857)
    Surface(
        modifier = modifier
            .height(54.dp)
            .widthIn(min = 106.dp)
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled, onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isOpen) 0.86f else 0.48f))
    ) {
        Row(
            modifier = Modifier
                .height(54.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isOpen) 0.18f else 0.10f),
                            warmColor.copy(alpha = if (isOpen) 0.08f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (enabled) 1f else 0.55f)
            ) {
                val strokeWidth = 2.4.dp.toPx()
                val startX = size.width * 0.12f
                val endX = size.width * 0.88f
                listOf(0.28f, 0.50f, 0.72f).forEach { yFactor ->
                    drawLine(
                        color = accentColor,
                        start = Offset(startX, size.height * yFactor),
                        end = Offset(endX, size.height * yFactor),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(
                text = "MENU",
                color = Color.White.copy(alpha = if (enabled) 0.94f else 0.55f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun ReturnHubButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFFFFC857)
    val dangerColor = Color(0xFFFF5D4F)
    Surface(
        modifier = modifier
            .height(54.dp)
            .widthIn(min = 116.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.52f))
    ) {
        Row(
            modifier = Modifier
                .height(54.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            dangerColor.copy(alpha = 0.16f),
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(22.dp)
            ) {
                val strokeWidth = 2.8.dp.toPx()
                val centerY = size.height * 0.5f
                drawLine(
                    color = accentColor,
                    start = Offset(size.width * 0.18f, centerY),
                    end = Offset(size.width * 0.84f, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accentColor,
                    start = Offset(size.width * 0.18f, centerY),
                    end = Offset(size.width * 0.46f, size.height * 0.24f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accentColor,
                    start = Offset(size.width * 0.18f, centerY),
                    end = Offset(size.width * 0.46f, size.height * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            Text(
                text = "HUB",
                color = Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    }
}

private data class QuickMenuAction(
    val iconRes: Int,
    val label: String,
    val roomAction: RoomAction
)


private const val ENEMY_FLAVOR_TAG = "enemy_flavor"

@Composable
private fun QuestJournalOverlay(
    trackedQuest: QuestSummaryUi?,
    activeQuests: List<QuestSummaryUi>,
    completedQuests: List<QuestSummaryUi>,
    failedQuests: Set<String>,
    questLog: List<QuestLogEntryUi>,
    onClose: () -> Unit,
    onSelectQuest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val otherActive = remember(trackedQuest, activeQuests) {
        activeQuests.filterNot { it.id == trackedQuest?.id }
    }
    val recentLog = remember(questLog) {
        questLog.sortedByDescending { it.timestamp }.take(12)
    }

    val totalCount = activeQuests.size + completedQuests.size + failedQuests.size

    var page by rememberSaveable { mutableStateOf(QuestJournalPage.ACTIVE) }

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

            QuestJournalToggle(
                current = page,
                onSelect = { page = it },
                accentColor = Color.White.copy(alpha = 0.9f),
                borderColor = Color.White.copy(alpha = 0.3f)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (page) {
                    QuestJournalPage.ACTIVE -> {
                        trackedQuest?.let { quest ->
                            item {
                                SectionCard(title = "Tracked Quest") {
                                    QuestSummaryDetails(
                                        quest = quest,
                                        emphasize = true,
                                        onClick = { onSelectQuest(quest.id) }
                                    )
                                }
                            }
                        } ?: item {
                            SectionCard(title = "Tracked Quest") {
                                Text(
                                    text = "No quest tracked. Select one below.",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                                            QuestSummaryDetails(
                                                quest = quest,
                                                onClick = { onSelectQuest(quest.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                SectionCard(title = "Active Quests") {
                                    Text(
                                        text = "No active quests yet.",
                                        color = Color.White.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    QuestJournalPage.COMPLETED -> {
                        if (completedQuests.isNotEmpty()) {
                            item {
                                SectionCard(title = "Completed Quests") {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        completedQuests.take(8).forEach { quest ->
                                            Text(
                                                text = "• ${quest.title}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                                color = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSelectQuest(quest.id) }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
                        } else {
                            item {
                                SectionCard(title = "Completed Quests") {
                                    Text(
                                        text = "You haven't finished any quests yet.",
                                        color = Color.White.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSelectQuest(questId) }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
    }
}

@Composable
private fun QuestJournalToggle(
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
private fun JournalToggleButton(
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
private fun QuestSummaryDetails(
    quest: QuestSummaryUi,
    emphasize: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val modifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(14.dp)
    } else {
        Modifier
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            val (iconRef, textRef, timeRef) = createRefs()
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier
                    .size(28.dp)
                    .constrainAs(iconRef) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
            )
            Column(
                modifier = Modifier.constrainAs(textRef) {
                    start.linkTo(iconRef.end, margin = 16.dp)
                    end.linkTo(timeRef.start, margin = 12.dp)
                    width = Dimension.fillToConstraints
                },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.constrainAs(timeRef) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}



@Composable
private fun CompactServiceChip(
    action: QuickMenuAction,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1B2A).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = action.iconRes),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = action.label.uppercase(Locale.getDefault()),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}


@Composable
private fun RoomEntitySection(
    npcs: List<String>,
    npcPresenceNames: Map<String, String>,
    npcPortraitPaths: Map<String, String>,
    groundItems: Map<String, Int>,
    serviceActions: List<QuickMenuAction> = emptyList(),
    itemDisplayName: (String) -> String,
    itemDetailLabel: (String) -> String?,
    itemIsEquipment: (String) -> Boolean,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onNpcClick: (String) -> Unit,
    onCollectItem: (String) -> Unit,
    onCollectAll: () -> Unit,
    onAction: (RoomAction) -> Unit
) {
    val itemEntries = remember(groundItems, itemDisplayName, itemIsEquipment) {
        groundItems.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { itemIsEquipment(it.key) }
                .thenBy { itemDisplayName(it.key).lowercase(Locale.getDefault()) }
        )
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF050A10).copy(alpha = if (isDark) 0.60f else 0.44f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.30f else 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Also here",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (npcs.isNotEmpty()) {
                PresenceRailRow {
                    npcs.forEach { npc ->
                        NpcPresenceChip(
                            npc = npc,
                            label = npcPresenceNames[npc.normalizedNpcKey()] ?: npcDisplayLabel(npc),
                            portraitPath = npcPortraitPaths[npc.normalizedNpcKey()],
                            borderColor = borderColor,
                            isDark = isDark,
                            onClick = { onNpcClick(npc) }
                        )
                    }
                }
            }
            if (serviceActions.isNotEmpty()) {
                PresenceRailRow {
                    serviceActions.forEach { action ->
                        ServicePresenceChip(
                            action = action,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            isDark = isDark,
                            onClick = { onAction(action.roomAction) }
                        )
                    }
                }
            }
            if (itemEntries.isNotEmpty()) {
                PresenceRailRow {
                    itemEntries.forEach { (itemId, quantity) ->
                        FindPresenceChip(
                            itemId = itemId,
                            quantity = quantity,
                            name = itemDisplayName(itemId),
                            detail = itemDetailLabel(itemId),
                            isEquipment = itemIsEquipment(itemId),
                            accentColor = accentColor,
                            borderColor = borderColor,
                            isDark = isDark,
                            onCollectItem = onCollectItem
                        )
                    }
                    if (itemEntries.size > 1) {
                        Surface(
                            onClick = onCollectAll,
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
                            modifier = Modifier
                                .width(54.dp)
                                .height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "All",
                                    color = Color.White.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServicePresenceChip(
    action: QuickMenuAction,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = if (isDark) 0.09f else 0.07f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.36f else 0.24f)),
        modifier = modifier
            .widthIn(min = 124.dp)
            .height(42.dp)
            .semantics { contentDescription = action.label }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.52f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = action.iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = action.label.uppercase(Locale.getDefault()),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PresenceRailRow(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            content()
        }
    }
}

@Composable
private fun NpcPresenceChip(
    npc: String,
    label: String,
    portraitPath: String?,
    borderColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val portraitPainter = rememberAssetPainter(
        portraitPath,
        painterResource(R.drawable.inventory_icon)
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = if (isDark) 0.09f else 0.07f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.36f else 0.24f)),
        modifier = Modifier
            .widthIn(min = 124.dp)
            .height(42.dp)
            .semantics { contentDescription = label }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portraitPainter,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun String.normalizedNpcKey(): String =
    trim().lowercase(Locale.getDefault())

@Composable
private fun FindPresenceChip(
    itemId: String,
    quantity: Int,
    name: String,
    detail: String?,
    isEquipment: Boolean,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onCollectItem: (String) -> Unit
) {
    Surface(
        onClick = { onCollectItem(itemId) },
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = if (isDark) 0.08f else 0.06f),
        border = BorderStroke(1.dp, if (isEquipment) accentColor.copy(alpha = 0.48f) else borderColor.copy(alpha = 0.22f)),
        modifier = Modifier
            .widthIn(min = 136.dp)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isEquipment) accentColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, if (isEquipment) accentColor.copy(alpha = 0.52f) else borderColor.copy(alpha = 0.22f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(roomPresenceItemIconRes(detail, isEquipment)),
                        contentDescription = null,
                        tint = if (isEquipment) accentColor.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (quantity > 1) EntityCountPill("x$quantity", accentColor)
                }
            }
        }
    }
}

@Composable
private fun EntityCountPill(label: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f))
    ) {
        Text(
            text = label,
            color = accentColor.copy(alpha = 0.92f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@DrawableRes
private fun roomPresenceItemIconRes(detail: String?, isEquipment: Boolean): Int {
    return previewItemIconRes(detail ?: if (isEquipment) "weapon" else null)
}

private fun npcDisplayLabel(npc: String): String = npc
    .replace('_', ' ')
    .split(' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part ->
        part.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }
    .ifBlank { npc }
private fun actionLabelFallback(action: RoomAction): String = when (action) {
    is ContainerAction -> "Search"
    is ToggleAction -> "Toggle"
    is TinkeringAction -> "Tinkering"
    is FirstAidAction -> "First Aid"
    is ShopAction -> "Shop"
    is GenericAction -> action.type.ifBlank { "Action" }.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }
    else -> "Action"
}

@Composable
private fun RoomActionChip(
    label: String,
    accentColor: Color,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    val background = if (locked) Color.White.copy(alpha = 0.05f) else Color.Transparent
    val borderColor = accentColor.copy(alpha = if (locked) 0.2f else 0.8f)
    Surface(
        onClick = onClick,
        enabled = !locked,
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label.ifBlank { "Interact" },
            color = if (locked) Color.White.copy(alpha = 0.6f) else Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FlavorBlock(
    title: String? = null,
    lines: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        lines.forEach { line ->
            Text(
                text = line,
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RoomFindsBlock(
    items: Map<String, Int>,
    itemFlavor: Map<String, String>,
    itemDisplayName: (String) -> String,
    onCollectItem: (String) -> Unit,
    onCollectAll: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean
) {
    if (items.isEmpty()) return
    val entries = items.entries.sortedBy { itemDisplayName(it.key).lowercase(Locale.getDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Finds",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelMedium
        )
        val containerShape = RoundedCornerShape(18.dp)
        Surface(
            shape = containerShape,
            color = Color.White.copy(alpha = if (isDark) 0.06f else 0.04f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.35f else 0.22f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                entries.forEach { (itemId, quantity) ->
                    val name = itemDisplayName(itemId)
                    val flavor = itemFlavor[itemId].orEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (quantity > 1) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = accentColor.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "x$quantity",
                                            color = accentColor.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (flavor.isNotBlank()) {
                                Text(
                                    text = flavor,
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCollectItem(itemId) }
                        ) {
                            Text(
                                text = "Pick Up",
                                color = accentColor.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (entries.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCollectAll() }
                    ) {
                        Text(
                            text = "Pick Up All",
                            color = accentColor.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyFlavorBlock(
    entries: List<Pair<String, String>>,
    accentColor: Color,
    onEnemyClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (enemyId, description) ->
            val displayName = remember(enemyId) {
                enemyId.replace('_', ' ').replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
            val annotated = remember(displayName, description, accentColor) {
                val trimmed = description
                val lowerText = trimmed.lowercase(Locale.getDefault())
                val lowerName = displayName.lowercase(Locale.getDefault())
                val matchIndex = lowerText.indexOf(lowerName)
                buildAnnotatedString {
                    append(trimmed)
                    val startIndex = if (matchIndex >= 0) {
                        matchIndex
                    } else {
                        trimmed.indexOf(' ').takeIf { it > 0 } ?: 0
                    }
                    val endIndex = if (matchIndex >= 0) {
                        (startIndex + lowerName.length).coerceAtMost(trimmed.length)
                    } else {
                        trimmed.indexOf(' ', startIndex + 1).takeIf { it > startIndex } ?: trimmed.length
                    }
                    addStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex.coerceAtLeast(0),
                        end = endIndex.coerceAtMost(trimmed.length)
                    )
                    addStringAnnotation(
                        tag = ENEMY_FLAVOR_TAG,
                        annotation = enemyId,
                        start = startIndex.coerceAtLeast(0),
                        end = endIndex.coerceAtMost(trimmed.length)
                    )
                }
            }
            ClickableText(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.92f)),
                onClick = { offset ->
                    annotated
                        .getStringAnnotations(ENEMY_FLAVOR_TAG, offset, offset)
                        .firstOrNull()
                        ?.let { onEnemyClick(it.item) }
                }
            )
        }
    }
}

@Composable
private fun EnemyPartyStrip(
    visualParties: List<VisualEnemyParty>,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    accentColor: Color,
    isDark: Boolean,
    roomId: String?,
    onPartyClick: (String) -> Unit
) {
    if (visualParties.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stageWidth = maxWidth
        val baseMemberSize = when {
            stageWidth < 380.dp -> 160.dp
            stageWidth < 520.dp -> 185.dp
            else -> 210.dp
        }
        val laneRise = (baseMemberSize * 0.72f).coerceAtLeast(88.dp)
        fun overlapFor(members: Int): Float = when {
            members >= 4 -> 0.42f
            members == 3 -> 0.36f
            members == 2 -> 0.28f
            else -> 0f
        }
        fun clusterWidthFactor(members: List<String>): Float {
            val count = members.size.coerceAtLeast(1)
            val overlap = overlapFor(count)
            return 1f + (count - 1) * (1f - overlap)
        }
        fun clusterWidth(members: List<String>, memberSize: Dp): Dp {
            return memberSize * clusterWidthFactor(members)
        }

        val slotAssignments = remember(roomId) { mutableMapOf<String, Int>() }
        val orderedParties = remember(visualParties) {
            visualParties
                .sortedWith(compareBy<VisualEnemyParty> { it.leavingTo != null }.thenBy { it.id })
        }
        val activePartyIds = orderedParties.map { it.id }.toSet()
        slotAssignments.keys.removeAll { it !in activePartyIds }

        val normalizedAssignments = mutableMapOf<String, Int>()
        slotAssignments.entries
            .sortedWith(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .forEach { (partyId, slotIndex) ->
                if (slotIndex in ENEMY_PARTY_SLOTS.indices && slotIndex !in normalizedAssignments.values) {
                    normalizedAssignments[partyId] = slotIndex
                }
            }
        slotAssignments.clear()
        slotAssignments.putAll(normalizedAssignments)

        orderedParties.forEach { party ->
            if (slotAssignments.containsKey(party.id)) return@forEach
            val preferredSlot = preferredEnemyPartySlot(party)
                .takeIf { it in ENEMY_PARTY_SLOTS.indices && it !in slotAssignments.values }
            val openSlot = preferredSlot ?: ENEMY_PARTY_SLOTS.indices.firstOrNull { it !in slotAssignments.values }
            if (openSlot != null) {
                slotAssignments[party.id] = openSlot
            }
        }

        val placements = orderedParties.mapNotNull { party ->
            slotAssignments[party.id]?.let { slotIndex ->
                Triple(party, ENEMY_PARTY_SLOTS[slotIndex], slotIndex)
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxWidth()
                .height(baseMemberSize + 28.dp + laneRise),
            contentAlignment = Alignment.BottomCenter
        ) {
            val renderPlacements = placements.sortedWith(
                compareBy<Triple<VisualEnemyParty, EnemyPartySlot, Int>> { (party, _, _) ->
                    if (party.enteringFrom != null || party.leavingTo != null) 0 else 1
                }.thenBy { (_, _, slotIndex) -> slotIndex }
            )
            renderPlacements.forEach { (party, slot, slotIndex) ->
                val memberSize = baseMemberSize
                val width = clusterWidth(party.enemies, memberSize)
                val centerX = stageWidth * slot.centerFraction
                val xOffset = centerX - (width / 2f)
                val yOffset = -laneRise * slot.row
                val stageHeight = baseMemberSize + 28.dp + laneRise
                val offscreenPadding = 72.dp
                fun transitionOffsetX(direction: String?): Float = when (direction) {
                    "east" -> (stageWidth - xOffset + offscreenPadding).value
                    "west" -> -(xOffset + width + offscreenPadding).value
                    else -> 0f
                }
                fun transitionOffsetY(direction: String?): Float = when (direction) {
                    "north" -> -(stageHeight + yOffset + offscreenPadding).value
                    "south" -> (baseMemberSize + 28.dp - yOffset + offscreenPadding).value
                    "east", "west" -> 0f
                    else -> if (slot.row == 0) 36f else -36f
                }
                val transitionDirection = party.leavingTo ?: party.enteringFrom
                val transitionOffsetX = transitionOffsetX(transitionDirection)
                val transitionOffsetY = transitionOffsetY(transitionDirection)
                val transitionZ = if (party.enteringFrom != null || party.leavingTo != null) {
                    0f
                } else {
                    (ENEMY_PARTY_SLOTS.size - slotIndex + 10).toFloat()
                }
                EnemyPartyCluster(
                    party = party,
                    enemyTiers = enemyTiers,
                    enemyIcons = enemyIcons,
                    accentColor = accentColor,
                    isDark = isDark,
                    memberSize = memberSize,
                    overlapFraction = overlapFor(party.enemies.size),
                    transitionDirection = transitionDirection,
                    transitionOffsetX = transitionOffsetX,
                    transitionOffsetY = transitionOffsetY,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = xOffset, y = yOffset)
                        .zIndex(transitionZ)
                        .width(width),
                    onEnemyClick = onPartyClick
                )
            }
        }
    }
}

private data class EnemyPartySlot(
    val centerFraction: Float,
    val row: Int
)

private val ENEMY_PARTY_SLOTS = listOf(
    EnemyPartySlot(centerFraction = 0.30f, row = 0),
    EnemyPartySlot(centerFraction = 0.70f, row = 0),
    EnemyPartySlot(centerFraction = 1f / 6f, row = 1),
    EnemyPartySlot(centerFraction = 0.50f, row = 1),
    EnemyPartySlot(centerFraction = 5f / 6f, row = 1)
)

private fun preferredEnemyPartySlot(party: VisualEnemyParty): Int {
    val staticIndex = party.id.removePrefix("static_").toIntOrNull()
    if (staticIndex != null) return staticIndex
    return abs(party.id.hashCode()) % ENEMY_PARTY_SLOTS.size
}

@Composable
private fun EnemyPresenceStage(
    visualParties: List<VisualEnemyParty>,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    accentColor: Color,
    isDark: Boolean,
    roomId: String?,
    onPartyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            EnemyPartyStrip(
                visualParties = visualParties,
                enemyTiers = enemyTiers,
                enemyIcons = enemyIcons,
                accentColor = accentColor,
                isDark = isDark,
                roomId = roomId,
                onPartyClick = onPartyClick
            )
        }
    }
}

private enum class EnemyTier { COMMON, ELITE, BOSS }

private fun parseEnemyTier(value: String?): EnemyTier {
    val normalized = value
        ?.trim()
        ?.lowercase(Locale.getDefault())
        ?.replace("_", "")
        ?.replace("-", "")
        ?.replace(" ", "")
        .orEmpty()
    return when (normalized) {
        "boss" -> EnemyTier.BOSS
        "elite", "miniboss" -> EnemyTier.ELITE
        else -> EnemyTier.COMMON
    }
}

private val EnemyPresenceShadowDrop = 0.dp

@Composable
private fun EnemyPartyCluster(
    party: VisualEnemyParty,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    accentColor: Color,
    isDark: Boolean,
    memberSize: Dp,
    overlapFraction: Float,
    transitionDirection: String?,
    transitionOffsetX: Float,
    transitionOffsetY: Float,
    modifier: Modifier = Modifier,
    onEnemyClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val verticalWiggle = transitionDirection == "north" || transitionDirection == "south"
    val transitionActive = party.enteringFrom != null || party.leavingTo != null
    val motionCycle = if (transitionActive) {
        0f
    } else {
        val motion = rememberInfiniteTransition(label = "hostilesMotion-${party.id}")
        val cycle by motion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6_400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hostilesMotionCycle-${party.id}"
        )
        cycle
    }
    val animX = remember(party.id) { androidx.compose.animation.core.Animatable(
        if (party.enteringFrom != null) transitionOffsetX else 0f
    ) }
    val animY = remember(party.id) { androidx.compose.animation.core.Animatable(
        if (party.enteringFrom != null) transitionOffsetY else 0f
    ) }
    val animAlpha = remember(party.id) { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(party.enteringFrom, party.leavingTo, transitionOffsetX, transitionOffsetY) {
        when {
            party.leavingTo != null -> {
                animAlpha.snapTo(1f)
                val wiggleSpeed = 52
                if (verticalWiggle) {
                    animY.animateTo(-10f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(10f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(-5f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(5f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(0f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                } else {
                    animX.animateTo(-10f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(10f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(-5f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(5f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(0f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                }
                coroutineScope {
                    launch {
                        animX.animateTo(transitionOffsetX, animationSpec = tween(520, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                    }
                    launch {
                        animY.animateTo(transitionOffsetY, animationSpec = tween(520, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                    }
                    launch {
                        animAlpha.animateTo(0f, animationSpec = tween(420, delayMillis = 80, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                    }
                }
            }
            party.enteringFrom != null -> {
                animAlpha.snapTo(1f)
                animX.snapTo(transitionOffsetX)
                animY.snapTo(transitionOffsetY)

                coroutineScope {
                    launch {
                        animX.animateTo(0f, animationSpec = tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                    }
                    launch {
                        animY.animateTo(0f, animationSpec = tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                    }
                }

                val wiggleSpeed = 60
                if (verticalWiggle) {
                    animY.animateTo(-12f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(12f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(-6f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(6f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animY.animateTo(0f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                } else {
                    animX.animateTo(-12f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(12f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(-6f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(6f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                    animX.animateTo(0f, animationSpec = tween(wiggleSpeed, easing = androidx.compose.animation.core.LinearEasing))
                }
            }
            else -> {
                animX.snapTo(0f)
                animY.snapTo(0f)
                animAlpha.snapTo(1f)
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = with(density) { animX.value.dp.toPx() }
                translationY = with(density) { animY.value.dp.toPx() }
                alpha = animAlpha.value
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        party.enemies.forEachIndexed { index, enemyId ->
            val step = (index + 1) / 2
            val isLeft = index % 2 == 1
            val offsetX = if (index == 0) 0.dp else (if (isLeft) -24.dp else 24.dp) * step
            val offsetY = if (index == 0) 0.dp else -16.dp * step
            val scale = if (index == 0) 1f else (1f - 0.08f * step).coerceAtLeast(0.8f)

            EnemyPartyStandee(
                enemyId = enemyId,
                instanceKey = "${party.id}-$enemyId-$index",
                tier = parseEnemyTier(enemyTiers[enemyId]),
                motionCycle = motionCycle,
                accentColor = accentColor,
                isDark = isDark,
                transitionActive = transitionActive,
                icon = enemyIcons[enemyId],
                size = memberSize,
                modifier = Modifier
                    .zIndex((party.enemies.size - index).toFloat())
                    .offset(x = offsetX, y = offsetY)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                onClick = { onEnemyClick(enemyId) }
            )
        }
    }
}

@Composable
private fun EnemyPartyStandee(
    enemyId: String,
    instanceKey: String,
    tier: EnemyTier,
    motionCycle: Float,
    accentColor: Color,
    isDark: Boolean,
    transitionActive: Boolean,
    icon: EnemyIconUi?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember(instanceKey) { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(size)
            .height(size + 28.dp)
            .semantics { contentDescription = "Engage ${enemyDisplayLabel(enemyId)}" }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        EnemyPartyLeaderIcon(
            enemyId = enemyId,
            instanceKey = instanceKey,
            tier = tier,
            motionCycle = motionCycle,
            accentColor = accentColor,
            isDark = isDark,
            transitionActive = transitionActive,
            icon = icon,
            iconSize = size
        )
    }
}

@Composable
private fun EnemyPartyLeaderIcon(
    enemyId: String,
    instanceKey: String,
    tier: EnemyTier,
    motionCycle: Float,
    accentColor: Color,
    isDark: Boolean,
    transitionActive: Boolean,
    icon: EnemyIconUi?,
    iconSize: Dp = 80.dp
) {
    val iconPath = icon?.spritePath ?: remember(enemyId) { "images/enemies/${enemyId}_combat.png" }
    val painter = rememberAssetPainter(iconPath, painterResource(R.drawable.inventory_icon), async = transitionActive)
    val tierAccent = enemyTierAccent(tier, accentColor)
    val tierBadge = when (tier) {
        EnemyTier.BOSS -> "☠"
        EnemyTier.ELITE -> "★"
        EnemyTier.COMMON -> null
    }
    val tierLabel = when (tier) {
        EnemyTier.BOSS -> "☠"
        EnemyTier.ELITE -> "★"
        EnemyTier.COMMON -> null
    }
    val density = LocalDensity.current
    val phaseOffset = remember(instanceKey) {
        (abs(instanceKey.lowercase(Locale.getDefault()).hashCode()) % 1000) / 1000f
    }
    val t = (motionCycle + phaseOffset) % 1f
    val wave = if (transitionActive) 0f else kotlin.math.sin(t * Math.PI * 2.0).toFloat()
    val glow = (0.5f + 0.5f * wave).coerceIn(0f, 1f)
    val bobAmplitude = if (isDark) 1.2.dp else 2.0.dp
    val bobPx = with(density) { bobAmplitude.toPx() } * wave
    val scale = 1f + (if (isDark) 0.012f else 0.02f) * wave
    val spriteScale = when (tier) {
        EnemyTier.BOSS -> 1.12f
        EnemyTier.ELITE -> 1.06f
        EnemyTier.COMMON -> 1.0f
    }
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 12.dp)
    val background = remember {
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        if (!transitionActive) {
            EnemyPresenceShadow(
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = EnemyPresenceShadowDrop)
                    .width(iconSize * 0.72f)
                    .height(iconSize * 0.18f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .graphicsLayer {
                    translationY = bobPx
                    scaleX = scale * spriteScale
                    scaleY = scale * spriteScale
                }
                .let { base ->
                    if (transitionActive) {
                        base
                    } else {
                        base
                            .clip(shape)
                            .background(background)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val iconModifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
            val composite = icon?.composite
            if (composite != null) {
                CompositeEnemyIcon(
                    composite = composite,
                    modifier = iconModifier
                )
            } else {
                Image(
                    painter = painter,
                    contentDescription = enemyId,
                    modifier = iconModifier,
                    contentScale = ContentScale.Fit
                )
            }
        if (tier != EnemyTier.COMMON && !transitionActive) {
            EnemyTierBadge(
                tier = tier,
                accentColor = tierAccent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(if (tier == EnemyTier.BOSS) 30.dp else 24.dp)
            )
        }
    }
}

}

@Composable
private fun EnemyTierBadge(
    tier: EnemyTier,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val outer = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.04f)
            lineTo(size.width * 0.92f, size.height * 0.28f)
            lineTo(size.width * 0.92f, size.height * 0.72f)
            lineTo(size.width * 0.50f, size.height * 0.96f)
            lineTo(size.width * 0.08f, size.height * 0.72f)
            lineTo(size.width * 0.08f, size.height * 0.28f)
            close()
        }
        drawPath(outer, Color(0xFF07131D).copy(alpha = 0.90f))
        drawPath(outer, accentColor.copy(alpha = 0.96f), style = stroke)
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * 0.27f,
            center = Offset(size.width * 0.50f, size.height * 0.50f)
        )
        when (tier) {
            EnemyTier.ELITE -> {
                val diamond = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.18f)
                    lineTo(size.width * 0.74f, size.height * 0.50f)
                    lineTo(size.width * 0.50f, size.height * 0.82f)
                    lineTo(size.width * 0.26f, size.height * 0.50f)
                    close()
                }
                drawPath(diamond, accentColor.copy(alpha = 0.34f))
                drawPath(diamond, Color.White.copy(alpha = 0.94f), style = stroke)
                drawLine(
                    color = accentColor.copy(alpha = 0.96f),
                    start = Offset(size.width * 0.35f, size.height * 0.50f),
                    end = Offset(size.width * 0.65f, size.height * 0.50f),
                    strokeWidth = size.minDimension * 0.08f,
                    cap = StrokeCap.Round
                )
            }
            EnemyTier.BOSS -> {
                val crest = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.72f)
                    lineTo(size.width * 0.30f, size.height * 0.32f)
                    lineTo(size.width * 0.44f, size.height * 0.56f)
                    lineTo(size.width * 0.50f, size.height * 0.20f)
                    lineTo(size.width * 0.56f, size.height * 0.56f)
                    lineTo(size.width * 0.70f, size.height * 0.32f)
                    lineTo(size.width * 0.78f, size.height * 0.72f)
                    close()
                }
                drawPath(crest, accentColor.copy(alpha = 0.34f))
                drawPath(crest, Color.White.copy(alpha = 0.94f), style = stroke)
                drawLine(
                    color = accentColor.copy(alpha = 0.98f),
                    start = Offset(size.width * 0.28f, size.height * 0.72f),
                    end = Offset(size.width * 0.72f, size.height * 0.72f),
                    strokeWidth = size.minDimension * 0.10f,
                    cap = StrokeCap.Round
                )
            }
            EnemyTier.COMMON -> Unit
        }
    }
}

@Composable
private fun EnemyPresenceShadow(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawOval(
            color = Color.Black.copy(alpha = if (isDark) 0.52f else 0.34f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
            size = Size(size.width * 0.84f, size.height * 0.58f)
        )
        drawOval(
            color = Color.Black.copy(alpha = if (isDark) 0.26f else 0.16f),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

private fun enemyTierAccent(tier: EnemyTier, accentColor: Color): Color = when (tier) {
    EnemyTier.BOSS -> Color(0xFFFFD54F)
    EnemyTier.ELITE -> Color(0xFFFFB74D)
    EnemyTier.COMMON -> accentColor
}

private fun enemyDisplayLabel(enemyId: String): String = enemyId
    .replace('_', ' ')
    .replace('-', ' ')
    .split(' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part ->
        part.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }
    .ifBlank { enemyId }

@Composable
private fun CompositeEnemyIcon(
    composite: EnemyCompositeIconUi,
    modifier: Modifier = Modifier
) {
    val parts = composite.parts
    if (parts.isEmpty()) return
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val paddingFactor = 0.12f
        val groupOffsetX = composite.groupOffsetX
        val groupOffsetY = composite.groupOffsetY
        val minX = parts.minOf { part ->
            (part.offsetX + groupOffsetX) - part.widthScale / 2f
        }
        val maxX = parts.maxOf { part ->
            (part.offsetX + groupOffsetX) + part.widthScale / 2f
        }
        val minY = parts.minOf { part ->
            (part.offsetY + groupOffsetY) - part.heightScale / 2f
        }
        val maxY = parts.maxOf { part ->
            (part.offsetY + groupOffsetY) + part.heightScale / 2f
        }
        val spanX = (maxX - minX).coerceAtLeast(0.1f)
        val spanY = (maxY - minY).coerceAtLeast(0.1f)
        val baseSize = minOf(
            maxWidth / (spanX * (1f + paddingFactor)),
            maxHeight / (spanY * (1f + paddingFactor))
        )
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        parts.sortedBy { it.z }.forEach { part ->
            val partWidth = baseSize * part.widthScale
            val partHeight = baseSize * part.heightScale
            val offsetX = baseSize * (part.offsetX + groupOffsetX - centerX)
            val offsetY = baseSize * (part.offsetY + groupOffsetY - centerY)
            Image(
                painter = rememberAssetPainter(part.spritePath, painterResource(R.drawable.inventory_icon), async = true),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = partWidth, height = partHeight)
                    .offset(x = offsetX, y = offsetY)
                    .zIndex(part.z)
            )
        }
    }
}



private fun buildInlineActionPlan(
    description: String?,
    actions: List<RoomAction>,
    hints: Map<String, ActionHintUi>,
    room: Room?
): InlineActionPlan? {
    if (description.isNullOrBlank()) return null
    val segments = mutableListOf<InlineActionSegment>()
    val occupied = mutableListOf<IntRange>()

    val markerPattern = Regex("""\[(npc):([^\]]+)]""", RegexOption.IGNORE_CASE)
    val parsedDescription = buildString {
        var cursor = 0
        markerPattern.findAll(description).forEach { match ->
            append(description, cursor, match.range.first)
            val label = match.groupValues[2].trim()
            val start = length
            append(label)
            val end = length
            if (label.isNotBlank()) {
                val range = start until end
                occupied += range
                segments += InlineActionSegment(
                    id = "npc-marker:$label:$start",
                    target = InlineActionTarget.Npc(label),
                    start = start,
                    end = end,
                    locked = false
                )
            }
            cursor = match.range.last + 1
        }
        append(description, cursor, description.length)
    }
    val lower = parsedDescription.lowercase(Locale.getDefault())

    fun variantsFor(label: String): List<String> {
        if (label.isBlank()) return emptyList()
        return buildList {
            add(label)
            val normalizedDash = label.replace('-', ' ')
            if (normalizedDash != label) add(normalizedDash)
            val normalizedApostrophe = label.replace('’', '\'')
            if (normalizedApostrophe != label) add(normalizedApostrophe)
        }.distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedByDescending { it.length }
    }

    fun findRange(label: String): IntRange? {
        val variants = variantsFor(label)
        if (variants.isEmpty()) return null
        for (variant in variants) {
            val needle = variant.lowercase(Locale.getDefault())
            var searchIndex = 0
            while (searchIndex <= lower.length - needle.length) {
                val index = lower.indexOf(needle, searchIndex)
                if (index < 0) break
                val rangeCandidate = index until index + needle.length
                if (occupied.none { rangesOverlap(it, rangeCandidate) }) {
                    return rangeCandidate
                }
                searchIndex = index + 1
            }
        }
        return null
    }

    actions.forEach { action ->
        val baseName = action.name
        if (baseName.isBlank()) return@forEach
        val range = findRange(baseName) ?: return@forEach
        occupied += range
        val key = action.actionKey()
        val locked = hints[key]?.locked == true
        segments += InlineActionSegment(
            id = key,
            target = InlineActionTarget.Room(action),
            start = range.first,
            end = range.last + 1,
            locked = locked
        )
    }

    room?.npcs.orEmpty()
        .filter { it.isNotBlank() }
        .forEach { npc ->
            val range = findRange(npc) ?: return@forEach
            occupied += range
            segments += InlineActionSegment(
                id = "npc:$npc",
                target = InlineActionTarget.Npc(npc),
                start = range.first,
                end = range.last + 1,
                locked = false
            )
        }

    room?.enemies.orEmpty()
        .filter { it.isNotBlank() }
        .forEach { enemyId ->
            val label = enemyId
            val range = findRange(label) ?: return@forEach
            occupied += range
            segments += InlineActionSegment(
                id = "enemy:$enemyId",
                target = InlineActionTarget.Enemy(enemyId, label),
                start = range.first,
                end = range.last + 1,
                locked = false
            )
        }

    if (segments.isEmpty()) return null
    segments.sortBy { it.start }
    return InlineActionPlan(description = parsedDescription, segments = segments)
}

private fun rangesOverlap(a: IntRange, b: IntRange): Boolean =
    a.first < b.last && b.first < a.last

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
    currentRoom: Room?,
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
            val itemFlavor = currentRoom?.itemFlavor.orEmpty()
            val itemLines = currentRoom?.items.orEmpty().mapNotNull { itemFlavor[it] }
            if (itemLines.isNotEmpty()) {
                FlavorBlock(title = "Finds", lines = itemLines)
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
    PromptBanner(
        title = "Tutorial",
        message = prompt.message,
        accentColor = Color(0xFF7BE8FF),
        actionLabel = "Got it",
        onAction = onDismiss,
        modifier = modifier
    )
}

@Composable
fun MilestoneBanner(
    prompt: com.example.starborn.domain.milestone.MilestoneEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PromptBanner(
        title = "Milestone",
        message = prompt.message,
        accentColor = Color(0xFFFFD27F),
        actionLabel = "Nice",
        onAction = onDismiss,
        modifier = modifier
    )
}

@Composable
fun ItemGrantedBanner(
    itemName: String,
    quantity: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = if (quantity > 1) {
        "Acquired $quantity x $itemName"
    } else {
        "Acquired $itemName"
    }
    PromptBanner(
        title = "Item Obtained",
        message = message,
        accentColor = Color(0xFFA5D6A7),
        actionLabel = "Got it",
        onAction = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun PromptBanner(
    title: String,
    message: String,
    accentColor: Color,
    backgroundColor: Color = Color(0xFF060B13).copy(alpha = 0.92f),
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val badgeLabel = title.uppercase(Locale.getDefault())
    val badgeIcon = remember(badgeLabel) {
        when {
            badgeLabel.contains("TUTORIAL") -> Icons.Outlined.School
            badgeLabel.contains("QUEST") -> Icons.Filled.Flag
            badgeLabel.contains("MILESTONE") -> Icons.Filled.EmojiEvents
            else -> Icons.Rounded.AutoAwesome
        }
    }

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .widthIn(min = 320.dp, max = 860.dp),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            backgroundColor.copy(alpha = 0.94f),
                            backgroundColor.copy(alpha = 0.90f),
                            backgroundColor.copy(alpha = 0.94f)
                        )
                    )
                )
                .border(1.2.dp, accentColor.copy(alpha = 0.50f), shape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.65f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = accentColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(
                            modifier = Modifier
                                .height(1.dp)
                                .weight(1f)
                                .background(accentColor.copy(alpha = 0.25f))
                        )
                    }
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(actionLabel.uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionOverlay(
    prompt: NarrationPrompt,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    if (prompt.tapToDismiss) onDismiss()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        NarrationCard(
            prompt = prompt,
            theme = theme,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 32.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                ),
            onDismiss = onDismiss
        )
    }
}

@Composable
fun NarrationCard(
    prompt: NarrationPrompt,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = themeColor(theme?.accent, Color(0xFF8DE2FF))
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = 0.72f))
    val backgroundColor = Color(0xFF050B12).copy(alpha = 0.97f)
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .widthIn(max = 620.dp),
        color = backgroundColor,
        shape = shape,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.82f)),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.85f))
                )
                Text(
                    text = prompt.message,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .defaultMinSize(minWidth = 92.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text(
                    text = if (prompt.tapToDismiss) "Got it" else "Dismiss",
                    style = MaterialTheme.typography.labelLarge
                )
            }
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
fun CinematicOverlayHost(
    state: CinematicUiState?,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedState by remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        if (state != null) {
            displayedState = state
        }
    }

    AnimatedVisibility(
        visible = state != null && displayedState != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        displayedState?.let { cinematic ->
            CinematicOverlay(
                state = cinematic,
                onAdvance = onAdvance,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CinematicOverlay(
    state: CinematicUiState,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speaker = state.step.speaker?.takeIf { it.isNotBlank() }
    if (speaker != null) {
        val dialogueLine = DialogueLine(
            id = "${state.sceneId}_${state.stepIndex}",
            speaker = speaker,
            text = state.step.text,
            portrait = state.step.portrait,
            voiceCue = null
        )
        val dialogueUi = DialogueUi(
            line = dialogueLine,
            portrait = state.step.portrait,
            voiceCue = null
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            DialogueOverlay(
                dialogue = dialogueUi,
                choices = emptyList(),
                onAdvance = onAdvance,
                onChoice = { onAdvance() },
                onPlayVoice = {},
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
            )
        }
        return
    }

    // --- Narration mode: cinematic popup ---

    // Typewriter text reveal
    val fullText = state.step.text
    val textKey = remember(state.sceneId, state.stepIndex) { "${state.sceneId}_${state.stepIndex}" }
    var revealedCount by remember(textKey) { mutableStateOf(0) }
    var revealFinished by remember(textKey) { mutableStateOf(false) }

    LaunchedEffect(textKey) {
        revealedCount = 0
        revealFinished = false
        for (i in 1..fullText.length) {
            revealedCount = i
            delay(28L)
        }
        revealFinished = true
    }

    val displayedText = if (revealFinished) fullText else fullText.substring(0, revealedCount)

    // Scrim fade-in
    val scrimAlpha by animateFloatAsState(
        targetValue = 0.72f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cinematicScrimAlpha"
    )

    // Card slide-up + fade-in
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "cinematicCardAlpha"
    )
    val cardOffsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "cinematicCardOffsetY"
    )

    // Accent glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "cinematicGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinematicGlowPulse"
    )

    val accentColor = Color(0xFF7BE8FF)
    val cardShape = RoundedCornerShape(28.dp)
    val tapInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = tapInteraction,
                indication = null
            ) {
                if (revealFinished) {
                    onAdvance()
                } else {
                    revealedCount = fullText.length
                    revealFinished = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .graphicsLayer {
                    alpha = cardAlpha
                    translationY = (1f - cardAlpha) * 48f + cardOffsetY
                }
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 400f
                        ),
                        shape = cardShape
                    )
            )

            Surface(
                color = Color(0xFF060B14).copy(alpha = 0.96f),
                shape = cardShape,
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.45f),
                            accentColor.copy(alpha = 0.12f),
                            accentColor.copy(alpha = 0.30f)
                        )
                    )
                ),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Decorative top accent bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.0f),
                                        accentColor.copy(alpha = 0.7f),
                                        accentColor.copy(alpha = 0.0f)
                                    )
                                )
                            )
                    )

                    // Narration text with typewriter reveal
                    Text(
                        text = displayedText,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 28.sp,
                            letterSpacing = 0.3.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tap hint with ornament
                    AnimatedVisibility(
                        visible = revealFinished,
                        enter = fadeIn(animationSpec = tween(durationMillis = 500))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✦  ",
                                color = accentColor.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = if (state.stepIndex + 1 >= state.stepCount) "Tap to continue" else "Tap to continue ▸",
                                color = accentColor.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    // Decorative bottom accent bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.0f),
                                        accentColor.copy(alpha = 0.7f),
                                        accentColor.copy(alpha = 0.0f)
                                    )
                                )
                            )
                    )
                }
            }
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
    val radius = remember { CoreAnimatable(0f) }
    val alpha = remember { CoreAnimatable(1f) }

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
