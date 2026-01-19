package com.example.starborn.feature.exploration.ui

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
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
import com.example.starborn.R
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.CookingAction
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
import com.example.starborn.feature.inventory.ui.InventoryLaunchOptions
import com.example.starborn.feature.inventory.ui.InventoryTab
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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




@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    audioCuePlayer: AudioCuePlayer,
    uiEventBus: UiEventBus,
    modifier: Modifier = Modifier,
    onEnemySelected: (List<String>) -> Unit = {},
    onOpenInventory: (InventoryLaunchOptions) -> Unit = {},
    onOpenTinkering: (String?) -> Unit = {},
    onOpenCooking: (String?) -> Unit = {},
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
            uiState.isMapLegendVisible

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
                is ExplorationEvent.OpenCooking -> onOpenCooking(event.stationId)
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
                if (isRoomDark && !room.descriptionDark.isNullOrBlank()) room.descriptionDark else room.description
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
        WeatherOverlay(
            weatherId = currentRoom?.weather,
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
        val fallbackNpcs = emptyList<String>()
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            val descriptionTopPadding = remember(maxHeight) { maxHeight * 0.2f }
            var titleMaxLineWidthPx by remember(currentRoom?.title) { mutableStateOf(0f) }
            val density = LocalDensity.current
            val underlineWidth = remember(titleMaxLineWidthPx, density) {
                if (titleMaxLineWidthPx > 0f) with(density) { titleMaxLineWidthPx.toDp() } else 0.dp
            }
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
            val minimapSize = 88.dp
            val headerMaxWidth = (maxWidth - minimapSize - 16.dp).coerceAtLeast(0.dp)
            ConstraintLayout(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                val (titleTextRef, underlineRef, minimapRef) = createRefs()
                MinimapWidget(
                minimap = uiState.minimap,
                    onLegend = {
                        viewModel.selectMenuTab(MenuTab.MAP)
                        viewModel.openMenuOverlay(MenuTab.MAP)
                    },
                    obscured = isRoomDark,
                    modifier = Modifier
                        .requiredSize(minimapSize)
                        .constrainAs(minimapRef) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        }
                )
                val titleColor = themeColor(activeTheme?.accent, Color(0xFFBEE9FF))
                val underlinePainter = painterResource(id = R.drawable.underline_4)
                Text(
                    text = uiState.currentRoom?.title ?: "Unknown area",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        textIndent = TextIndent(restLine = 14.sp),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(0f, 1.25f),
                            blurRadius = 1.5f
                        )
                    ),
                    color = titleColor,
                    onTextLayout = { result: TextLayoutResult ->
                        var widest = 0f
                        for (i in 0 until result.lineCount) {
                            val width = result.getLineRight(i) - result.getLineLeft(i)
                            if (width > widest) widest = width
                        }
                        titleMaxLineWidthPx = widest
                    },
                    modifier = Modifier
                        .widthIn(max = headerMaxWidth.coerceAtMost(360.dp))
                        .padding(end = 16.dp)
                        .constrainAs(titleTextRef) {
                            start.linkTo(parent.start)
                            end.linkTo(minimapRef.start)
                            width = Dimension.preferredWrapContent
                            centerVerticallyTo(minimapRef)
                        }
                )
                val resolvedUnderlineWidth = underlineWidth.takeIf { it > 0.dp } ?: 140.dp
                val painterSize = underlinePainter.intrinsicSize
                val underlineHeight = 32.dp
                Box(
                    modifier = Modifier
                        .padding(top = 0.dp, end = 16.dp)
                        .width(resolvedUnderlineWidth)
                        .height(underlineHeight)
                        .constrainAs(underlineRef) {
                            start.linkTo(titleTextRef.start)
                            top.linkTo(titleTextRef.bottom)
                        }
                ) {
                    Image(
                        painter = underlinePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = 1.dp)
                    )
                    Image(
                        painter = underlinePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.tint(titleColor.copy(alpha = 0.85f)),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            RoomDescriptionPanel(
                currentRoom = currentRoom,
                description = descriptionForPanel,
                plan = inlinePlan,
                isDark = isRoomDark,
                onAction = { action -> viewModel.onActionSelected(action) },
                onNpcClick = { name -> viewModel.onNpcInteraction(name) },
                onEnemyClick = { enemyId -> viewModel.engageEnemy(enemyId) },
                groundItems = uiState.groundItems,
                itemDisplayName = { itemId -> viewModel.itemDisplayName(itemId) },
                onCollectItem = { itemId -> viewModel.collectGroundItem(itemId) },
                onCollectAll = { viewModel.collectAllGroundItems() },
                enemyTiers = uiState.enemyTiers,
                enemyIcons = uiState.enemyIcons,
                borderColor = panelBorderColor,
                backgroundColor = panelBackgroundColor,
                fallbackNpcs = fallbackNpcs,
                accentColor = actionAccentColor,
                textColor = roomTextColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = descriptionTopPadding)
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 240.dp, max = 480.dp)
            )

            if (serviceQuickActions.isNotEmpty() || uiState.canReturnToHub) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 112.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (serviceQuickActions.isNotEmpty()) {
                        ServiceActionTray(
                            actions = serviceQuickActions,
                            onAction = { viewModel.onActionSelected(it) },
                            backgroundColor = panelBackgroundColor.copy(alpha = if (isRoomDark) 0.65f else 0.8f),
                            accentColor = actionAccentColor,
                            modifier = Modifier
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    if (uiState.canReturnToHub) {
                        ReturnHubButton(
                            onClick = { viewModel.requestReturnToHub() },
                            modifier = Modifier
                                .offset(y = (-8).dp)
                                .padding(bottom = 4.dp),
                            size = 96.dp
                        )
                    }
                }
            }
        }
        uiState.narrationPrompt?.let { narration ->
            NarrationCard(
                prompt = narration,
                theme = activeTheme,
                onDismiss = { viewModel.dismissNarration() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp)
                    .zIndex(2f)
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
                onOpenInventory = { options ->
                    viewModel.closeMenuOverlay()
                    onOpenInventory(options)
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
                }
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

        MenuToggleButton(
            isOpen = uiState.isMenuOverlayVisible,
            onToggle = {
                if (uiState.isMenuOverlayVisible) {
                    viewModel.closeMenuOverlay()
                } else {
                    viewModel.openMenuOverlay()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            enabled = !blockingOverlayActive
        )

        uiState.cinematic?.let { cinematic ->
            CinematicOverlay(
                state = cinematic,
                onAdvance = { viewModel.advanceCinematic() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
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
                modifier = Modifier.align(Alignment.Center)
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

        if (uiState.eventAnnouncement == null) {
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
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(12.dp)
        )
    }
}
}
@Composable
private fun MinimapWidget(
    minimap: MinimapUiState?,
    onLegend: () -> Unit,
    modifier: Modifier = Modifier,
    obscured: Boolean = false
) {
    val clrBackground = Color(0.05f, 0.1f, 0.15f, 0.85f)
    val clrBorder = Color(0.3f, 0.8f, 1.0f, 1.0f)
    val clrBorderAccent = Color(0.6f, 0.9f, 1.0f, 0.8f)
    val clrGrid = Color(0.3f, 0.8f, 1.0f, 0.15f)
    val clrTile = Color(0.6f, 0.85f, 1.0f, 0.7f)
    val clrTileGlow = Color(0.9f, 1.0f, 1.0f, 1.0f)
    val clrPlayer = Color(1.0f, 0.9f, 0.3f, 1.0f)

    val playerPulse = remember { CoreAnimatable(1f) }
    LaunchedEffect(Unit) {
        playerPulse.animateTo(
            targetValue = 0.1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onLegend),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                val cellsInViewport = state.cells.filter {
                    abs(it.offsetX) <= 1 && abs(it.offsetY) <= 1 && (it.discovered || it.isCurrent)
                }
                val idToCell = state.cells.associateBy { it.roomId }

                // --- Connection lines (only where rooms are truly connected) ---
                cellsInViewport.forEach { cell ->
                    // Only draw connections to east and north to avoid duplicates and redundant checks
                    for (direction in setOf("east", "north")) {
                        val connectedRoomId = cell.connections[direction]
                        if (connectedRoomId != null) {
                            val neighbor = idToCell[connectedRoomId]
                            if (neighbor != null &&
                                abs(neighbor.offsetX) <= 1 &&
                                abs(neighbor.offsetY) <= 1 &&
                                (neighbor.discovered || neighbor.isCurrent)
                            ) {
                                val x1 = cx + cell.offsetX * step
                                val y1 = cy - cell.offsetY * step
                                val x2 = cx + neighbor.offsetX * step
                                val y2 = cy - neighbor.offsetY * step
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
                    val px = cx + cell.offsetX * step
                    val py = cy - cell.offsetY * step
                    val center = Offset(px, py)

                    val isCurrent = cell.isCurrent
                    val baseColor = if (isCurrent) clrTileGlow else clrTile
                    val pipColor = baseColor.copy(alpha = if (cell.isDark) baseColor.alpha * 0.6f else baseColor.alpha)
                    val pipSize = g * if (isCurrent) 0.9f else 0.6f

                    drawCircle(
                        color = pipColor,
                        radius = pipSize / 2,
                        center = center
                    )

                    if (cell.isDark) {
                        val overlaySize = Size(pipSize * 1.05f, pipSize * 1.05f)
                        val topLeft = Offset(
                            x = center.x - overlaySize.width / 2f,
                            y = center.y - overlaySize.height / 2f
                        )
                        drawDarkRoomOverlay(
                            topLeft = topLeft,
                            size = overlaySize,
                            cornerRadius = CornerRadius(overlaySize.width * 0.35f, overlaySize.height * 0.35f),
                            overlayColor = Color.Black.copy(alpha = 0.75f),
                            hatchColor = Color.White.copy(alpha = 0.08f),
                            hatchSpacing = overlaySize.width / 4f
                        )
                    }
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
        if (obscured) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0f, 0f, 0f, 0.96f),
                            Color(0.03f, 0.02f, 0.08f, 0.9f),
                            Color(0f, 0f, 0f, 0.94f)
                        )
                    )
                )
                val hatchSpacing = size.minDimension / 9f
                var offset = -size.height
                while (offset < size.width + size.height) {
                    drawLine(
                        color = Color(0.07f, 0.05f, 0.12f, 0.35f),
                        start = Offset(offset, 0f),
                        end = Offset(offset + size.height * 1.4f, size.height),
                        strokeWidth = 2f
                    )
                    offset += hatchSpacing
                }
            }
        }
        }
    }
}

@Composable
private fun DirectionIndicatorsOverlay(
    indicators: Map<String, DirectionIndicatorUi>,
    modifier: Modifier = Modifier
) {
    if (indicators.isEmpty()) return
    Box(modifier = modifier) {
        indicators.values.forEach { indicator ->
            val direction = indicator.direction.lowercase(Locale.getDefault())
            val (alignment, padding) = when (direction) {
                "north" -> Alignment.TopCenter to PaddingValues(top = 4.dp)
                "south" -> Alignment.BottomCenter to PaddingValues(bottom = 4.dp)
                "east" -> Alignment.CenterEnd to PaddingValues(end = 4.dp)
                "west" -> Alignment.CenterStart to PaddingValues(start = 4.dp)
                else -> Alignment.Center to PaddingValues(0.dp)
            }
            val loop = rememberInfiniteTransition(label = "dirPulse-$direction")
            val pulse by loop.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (indicator.status == DirectionIndicatorStatus.ENEMY) 900 else 1200,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse-$direction"
            )
            val baseAlphaRange = if (indicator.status == DirectionIndicatorStatus.ENEMY) 0.55f..0.98f else 0.65f..0.95f
            val alpha = lerp(baseAlphaRange.start, baseAlphaRange.endInclusive, pulse)
            val scale = 1f + 0.08f * pulse
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(padding)
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                DirectionIndicatorIcon(
                    status = indicator.status,
                    direction = direction
                )
            }
        }
    }
}

@Composable
private fun DirectionIndicatorIcon(
    status: DirectionIndicatorStatus,
    direction: String
) {
    val baseSize = 36.dp
    val color = when (status) {
        DirectionIndicatorStatus.UNEXPLORED -> Color(0.56f, 0.78f, 1.0f)
        DirectionIndicatorStatus.LOCKED -> Color(0.70f, 0.72f, 0.80f)
        DirectionIndicatorStatus.ENEMY -> Color(1.0f, 0.32f, 0.32f)
    }
    when (status) {
        DirectionIndicatorStatus.UNEXPLORED -> DirectionArrowIndicator(direction, baseSize, color)
        DirectionIndicatorStatus.LOCKED -> DirectionLockIndicator(direction, baseSize, color)
        DirectionIndicatorStatus.ENEMY -> DirectionEnemyIndicator(direction, baseSize, color)
    }
}

@Composable
private fun DirectionArrowIndicator(direction: String, size: Dp, color: Color) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "east" -> 90f
        "south" -> 180f
        "west" -> 270f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val inset = w * 0.28f
        val tipY = inset
        val baseY = w - inset
        val midX = w / 2f
        val wing = w * 0.26f
        val path = Path().apply {
            moveTo(midX, tipY)
            lineTo(midX + wing, baseY)
            lineTo(midX, baseY - wing * 0.8f)
            lineTo(midX - wing, baseY)
            close()
        }
        // Glow
        drawCircle(color = color.copy(alpha = 0.25f), radius = w * 0.55f, center = Offset(midX, midX))
        drawCircle(color = color.copy(alpha = 0.35f), radius = w * 0.42f, center = Offset(midX, midX))
        drawPath(path, color = color)
        drawPath(path, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = w * 0.05f))
    }
}

@Composable
private fun DirectionLockIndicator(direction: String, size: Dp, color: Color) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "east" -> 90f
        "south" -> 180f
        "west" -> 270f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val bodyWidth = w * 0.62f
        val bodyHeight = w * 0.44f
        val bodyTop = w * 0.48f
        val bodyLeft = (w - bodyWidth) / 2f
        val corner = CornerRadius(w * 0.12f, w * 0.12f)

        // Glow
        drawCircle(color = color.copy(alpha = 0.2f), radius = w * 0.52f, center = Offset(w / 2f, w / 2f))
        drawCircle(color = color.copy(alpha = 0.3f), radius = w * 0.38f, center = Offset(w / 2f, w / 2f))

        // Body
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner,
            style = Stroke(width = w * 0.05f)
        )

        // Shackle
        val shackleThickness = w * 0.12f
        val shackleWidth = w * 0.6f
        val shackleHeight = w * 0.46f
        val shackleLeft = (w - shackleWidth) / 2f
        val shackleTop = w * 0.06f
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(shackleLeft, shackleTop),
            size = Size(shackleWidth, shackleHeight),
            style = Stroke(width = shackleThickness, cap = StrokeCap.Round)
        )

        // Keyhole
        val keyholeRadius = w * 0.075f
        val keyholeCenter = Offset(w / 2f, bodyTop + bodyHeight * 0.32f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.7f),
            radius = keyholeRadius,
            center = keyholeCenter
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(keyholeCenter.x - keyholeRadius * 0.45f, keyholeCenter.y - keyholeRadius * 0.05f),
            size = Size(keyholeRadius * 0.9f, keyholeRadius * 1.6f),
            cornerRadius = CornerRadius(keyholeRadius * 0.4f, keyholeRadius * 0.4f)
        )
    }
}

@Composable
private fun DirectionEnemyIndicator(direction: String, size: Dp, color: Color) {
    val rotation = when (direction.lowercase(Locale.getDefault())) {
        "east" -> 90f
        "south" -> 180f
        "west" -> 270f
        else -> 0f
    }
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val w = size.toPx()
        val center = Offset(w / 2f, w / 2f)
        val radius = w * 0.36f
        val diamond = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }

        // Glow halo
        drawCircle(color = color.copy(alpha = 0.22f), radius = w * 0.55f, center = center)
        drawCircle(color = color.copy(alpha = 0.32f), radius = w * 0.42f, center = center)

        // Core diamond
        drawPath(diamond, color = color.copy(alpha = 0.92f))
        drawPath(diamond, color = Color.White.copy(alpha = 0.4f), style = Stroke(width = w * 0.05f))

        // Inner bevel
        drawPath(diamond, color = Color.White.copy(alpha = 0.08f), style = Stroke(width = w * 0.14f))

        // Exclamation mark
        val barHeight = radius * 0.95f
        val barWidth = w * 0.09f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(center.x - barWidth / 2f, center.y - barHeight * 0.55f),
            size = Size(barWidth, barHeight * 0.7f),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
        drawCircle(
            color = Color.White,
            radius = barWidth * 0.65f,
            center = Offset(center.x, center.y + barHeight * 0.35f)
        )
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

private fun DrawScope.drawDarkRoomOverlay(
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
    onOpenInventory: (InventoryLaunchOptions) -> Unit,
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
    val panelColor = themeColor(theme?.bg, MaterialTheme.colorScheme.surface).copy(alpha = 0.95f)
    val panelBorder = themeColor(theme?.border, Color.White.copy(alpha = 0.4f))
    val accentColor = themeColor(theme?.accent, MaterialTheme.colorScheme.primary)
    val sheetScroll = rememberScrollState()
    val cornerRadius = 28.dp
    val borderWidth = 2.dp
    val borderColor = panelBorder.copy(alpha = 0.8f)
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
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.titleLarge,
                            color = accentColor
                        )
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onClose() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                                    accentColor.copy(alpha = 0.05f),
                                    accentColor.copy(alpha = 0.12f)
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
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    val background = if (isSelected) accentColor.copy(alpha = 0.28f) else Color.Transparent
    val contentColor = if (isSelected) Color.White else accentColor.copy(alpha = 0.9f)
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isSelected) 0.9f else 0.4f)),
        color = background
    ) {
        Text(
            text = tab.label(),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )
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
    onOpenInventory: (InventoryLaunchOptions) -> Unit,
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

private enum class InventoryCarouselPage { SUPPLIES, GEAR, KEY_ITEMS }

private enum class QuestJournalPage { ACTIVE, COMPLETED }

@Composable
private fun InventoryTabContent(
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatusUi>,
    accentColor: Color,
    borderColor: Color,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseConsumable: (InventoryPreviewItemUi) -> Unit,
    creditsLabel: String
) {
    var page by rememberSaveable { mutableStateOf(InventoryCarouselPage.SUPPLIES) }
    val supplies = remember(inventoryItems) {
        inventoryItems.filterNot { it.isKeyItem() }
    }
    val keyItems = remember(inventoryItems) { inventoryItems.filter { it.isKeyItem() } }
    MenuSectionCard(
        title = "Inventory Overview",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        InventoryCarouselToggle(
            current = page,
            onSelect = { page = it },
            accentColor = accentColor,
            borderColor = borderColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (page) {
            InventoryCarouselPage.SUPPLIES -> InventoryItemsPreview(
                items = supplies,
                borderColor = borderColor,
                emptyMessage = "No supplies collected yet. Explore rooms to gather materials.",
                onItemClick = onUseConsumable
            )
            InventoryCarouselPage.GEAR -> InventoryEquipmentPreview(
                inventoryItems = inventoryItems,
                equippedItems = equippedItems,
                unlockedWeapons = unlockedWeapons,
                equippedWeapons = equippedWeapons,
                unlockedArmors = unlockedArmors,
                equippedArmors = equippedArmors,
                partyMembers = partyMembers,
                borderColor = borderColor,
                accentColor = accentColor,
                onEquipItem = onEquipItem,
                onEquipMod = onEquipMod,
                onEquipWeapon = onEquipWeapon,
                resolveWeaponItem = resolveWeaponItem,
                onEquipArmor = onEquipArmor,
                resolveArmorItem = resolveArmorItem
            )
            InventoryCarouselPage.KEY_ITEMS -> InventoryItemsPreview(
                items = keyItems,
                borderColor = borderColor,
                emptyMessage = "No key items collected yet."
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        WalletPill(
            creditsLabel = creditsLabel,
            accentColor = accentColor,
            borderColor = borderColor,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun InventoryCarouselToggle(
    current: InventoryCarouselPage,
    onSelect: (InventoryCarouselPage) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InventoryCarouselButton(
            label = "Supplies",
            selected = current == InventoryCarouselPage.SUPPLIES,
            onClick = { onSelect(InventoryCarouselPage.SUPPLIES) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        InventoryCarouselButton(
            label = "Gear",
            selected = current == InventoryCarouselPage.GEAR,
            onClick = { onSelect(InventoryCarouselPage.GEAR) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        InventoryCarouselButton(
            label = "Key Items",
            selected = current == InventoryCarouselPage.KEY_ITEMS,
            onClick = { onSelect(InventoryCarouselPage.KEY_ITEMS) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InventoryCarouselButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
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
            modifier = Modifier
                .padding(vertical = 10.dp),
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
private fun InventoryItemsPreview(
    items: List<InventoryPreviewItemUi>,
    borderColor: Color,
    emptyMessage: String,
    onItemClick: ((InventoryPreviewItemUi) -> Unit)? = null
) {
    if (items.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 6.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val isUsable = onItemClick != null && item.effect != null
            val shape = RoundedCornerShape(18.dp)
            val iconRes = remember(item.id + item.type) { previewItemIconRes(item.type) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (isUsable) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.35f)), shape)
                    .clickable(enabled = isUsable) { onItemClick?.invoke(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                    )
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = if (isUsable) 0.7f else 0.5f)
                        )
                    }
                }
                Text(
                    text = "x${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WalletPill(
    creditsLabel: String,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val compactLabel = remember(creditsLabel) {
        creditsLabel.replace(Regex("\\s*credits", RegexOption.IGNORE_CASE), " c").trim()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = compactLabel,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
        }
    }
}

private fun InventoryPreviewItemUi.isKeyItem(): Boolean {
    val normalized = type.lowercase(Locale.getDefault())
    return normalized == "key" || normalized == "key_item" || normalized == "quest"
}

@Composable
private fun InventoryEquipmentPreview(
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatusUi>,
    borderColor: Color,
    accentColor: Color,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?
) {
    var gearPicker by remember { mutableStateOf<Pair<String, String>?>(null) }
    var modPicker by remember { mutableStateOf<Pair<String, String>?>(null) }
    val itemNames = remember(inventoryItems) {
        inventoryItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val weaponOptionIds = remember(unlockedWeapons, equippedWeapons) {
        val equippedIds = equippedWeapons.values.filter { it.isNotBlank() }
        (unlockedWeapons + equippedIds)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val weaponItems = remember(weaponOptionIds, resolveWeaponItem) {
        weaponOptionIds.mapNotNull { weaponId ->
            resolveWeaponItem(weaponId)
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    val weaponItemById = remember(weaponItems) {
        weaponItems.associateBy { it.id.lowercase(Locale.getDefault()) }
    }
    val weaponOptions = remember(weaponItems) {
        weaponItems.map { item ->
            InventoryPreviewItemUi(
                id = item.id,
                name = item.name,
                quantity = 1,
                type = item.type,
                effect = item.effect,
                equipment = item.equipment
            )
        }
    }
    val weaponNames = remember(weaponItems) {
        weaponItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val armorOptionIds = remember(unlockedArmors, equippedArmors) {
        val equippedIds = equippedArmors.values.filter { it.isNotBlank() }
        (unlockedArmors + equippedIds)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val armorItems = remember(armorOptionIds, resolveArmorItem) {
        armorOptionIds.mapNotNull { armorId ->
            resolveArmorItem(armorId)
        }.filter { item ->
            item.equipment?.slot?.equals("armor", true) == true ||
                item.type.equals("armor", ignoreCase = true)
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    val armorItemById = remember(armorItems) {
        armorItems.associateBy { it.id.lowercase(Locale.getDefault()) }
    }
    val armorNames = remember(armorItems) {
        armorItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val modOptions = remember(inventoryItems) {
        inventoryItems.filter { it.isModItem() }
    }
    val slots = remember { GearRules.equipSlots }
    if (partyMembers.isEmpty()) {
        Text(
            text = "No party members yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        partyMembers.forEach { member ->
            val portraitPainter = rememberAssetPainter(member.portraitPath, R.drawable.main_menu_background)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = "${member.name} portrait",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.forEach { slot ->
                        val normalized = slot.lowercase(Locale.getDefault())
                        val normalizedMemberId = member.id.lowercase(Locale.getDefault())
                        val scopedKey = "$normalizedMemberId:$normalized"
                        val equippedId = when (normalized) {
                            "weapon" -> equippedWeapons[normalizedMemberId].orEmpty()
                            "armor" -> equippedArmors[normalizedMemberId].orEmpty()
                            else -> equippedItems[scopedKey].orEmpty()
                        }
                        val equippedName = when (normalized) {
                            "weapon" -> weaponNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "No weapon equipped" }
                            "armor" -> armorNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "No armor equipped" }
                            else -> itemNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "Unequipped" }
                        }
                        val iconRes = when (normalized) {
                            "weapon" -> {
                                val weaponItem = weaponItemById[equippedId.lowercase(Locale.getDefault())]
                                weaponItem?.let { weaponIconRes(it) }
                                    ?: weaponTypeIconRes(GearRules.allowedWeaponTypeFor(normalizedMemberId))
                            }
                            "armor" -> {
                                val armorItem = armorItemById[equippedId.lowercase(Locale.getDefault())]
                                armorItem?.let { armorIconRes(it) } ?: slotIconRes(normalized)
                            }
                            else -> slotIconRes(normalized)
                        }
                        val modSlots = modSlotsForBaseSlot(normalized)
                        val modNames = modSlots.associateWith { modSlot ->
                            val modKey = "$normalizedMemberId:$modSlot"
                            val modId = equippedItems[modKey].orEmpty()
                            itemNames[modId.lowercase(Locale.getDefault())] ?: modId.ifBlank { "Empty" }
                        }
                        GearSlotTile(
                            slot = normalized,
                            equippedName = equippedName,
                            modsLocked = modSlots.isNotEmpty() && equippedId.isBlank(),
                            modNames = modNames,
                            iconRes = iconRes,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            onSelectSlot = { gearPicker = member.id to normalized },
                            onSelectMod = { modSlot ->
                                modPicker = member.id to modSlot
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    val picker = gearPicker
    if (picker != null) {
        val (characterId, slotId) = picker
        val normalizedSlot = slotId.lowercase(Locale.getDefault())
        val options = remember(inventoryItems, weaponOptions, picker) {
            when (normalizedSlot) {
                "weapon" -> filterGearOptionsForPreview(weaponOptions, normalizedSlot, characterId)
                "armor" -> emptyList()
                else -> filterGearOptionsForPreview(inventoryItems, normalizedSlot, characterId)
            }
        }
        val weaponItemsForCharacter = remember(weaponItems, options, picker) {
            if (normalizedSlot != "weapon") emptyList() else {
                val allowedIds = options.map { it.id.lowercase(Locale.getDefault()) }.toSet()
                weaponItems.filter { allowedIds.contains(it.id.lowercase(Locale.getDefault())) }
            }
        }
        val normalizedCharacter = characterId.lowercase(Locale.getDefault())
        val armorItemsForCharacter = remember(armorItems, picker, normalizedCharacter) {
            if (normalizedSlot != "armor") {
                emptyList()
            } else {
                val expectedType = GearRules.allowedArmorTypeFor(normalizedCharacter)
                if (expectedType == null) armorItems else armorItems.filter { item ->
                    item.type.trim().lowercase(Locale.getDefault()) == expectedType
                }
            }
        }
        val characterName = partyMembers.firstOrNull { it.id == characterId }?.name ?: characterId
        val equippedKey = "$normalizedCharacter:$normalizedSlot"
        val equippedId = when (normalizedSlot) {
            "weapon" -> equippedWeapons[normalizedCharacter].orEmpty()
            "armor" -> equippedArmors[normalizedCharacter].orEmpty()
            else -> equippedItems[equippedKey].orEmpty()
        }
        if (normalizedSlot == "weapon") {
            WeaponSelectionDialog(
                characterName = characterName,
                weapons = weaponItemsForCharacter,
                equippedWeaponId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipWeapon(characterId, selection)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        } else if (normalizedSlot == "armor") {
            ArmorSelectionDialog(
                characterName = characterName,
                armors = armorItemsForCharacter,
                equippedArmorId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipArmor(characterId, selection)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        } else {
            GearSelectionDialog(
                characterName = characterName,
                slotLabel = slotLabel(slotId),
                options = options,
                equippedId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipItem(normalizedSlot, selection, characterId)
                    gearPicker = null
                },
                onUnequip = {
                    onEquipItem(normalizedSlot, null, characterId)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        }
    }

    val modSelection = modPicker
    if (modSelection != null) {
        val (characterId, slotId) = modSelection
        val normalizedSlot = slotId.lowercase(Locale.getDefault())
        val equippedKey = "${characterId.lowercase(Locale.getDefault())}:$normalizedSlot"
        val equippedId = equippedItems[equippedKey].orEmpty()
        val characterName = partyMembers.firstOrNull { it.id == characterId }?.name ?: characterId
        GearSelectionDialog(
            characterName = characterName,
            slotLabel = slotLabel(slotId),
            options = modOptions,
            equippedId = equippedId,
            accentColor = accentColor,
            borderColor = borderColor,
            onSelect = { selection ->
                onEquipMod(normalizedSlot, selection, characterId)
                modPicker = null
            },
            onUnequip = {
                onEquipMod(normalizedSlot, null, characterId)
                modPicker = null
            },
            onDismiss = { modPicker = null }
        )
    }
}

@Composable
private fun GearSlotTile(
    slot: String,
    equippedName: String,
    modsLocked: Boolean,
    modNames: Map<String, String>,
    @DrawableRes iconRes: Int,
    accentColor: Color,
    borderColor: Color,
    onSelectSlot: () -> Unit,
    onSelectMod: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val background = Color.Black.copy(alpha = 0.2f)
    val minHeight = if (modNames.isEmpty()) 76.dp else 108.dp
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .clickable { onSelectSlot() },
        color = background,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotLabel(slot),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                    Text(
                        text = equippedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (equippedName == "Unequipped" || equippedName == "No weapon equipped") {
                            accentColor.copy(alpha = 0.65f)
                        } else if (equippedName == "No armor equipped") {
                            accentColor.copy(alpha = 0.65f)
                        } else {
                            Color.White
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (modNames.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (modsLocked) 0.6f else 1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modNames.entries.forEach { (slotId, name) ->
                        ModChip(
                            label = if (slotId.endsWith("1")) "M1" else "M2",
                            name = name,
                            enabled = !modsLocked,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            onClick = { onSelectMod(slotId) }
                        )
                    }
                }
                if (modsLocked) {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun ModChip(
    label: String,
    name: String,
    enabled: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(10.dp))
            .let { base -> if (enabled) base.clickable { onClick() } else base },
        color = Color.Black.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@DrawableRes
private fun slotIconRes(slot: String): Int {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> R.drawable.item_icon_sword
        "armor" -> R.drawable.item_icon_armor
        "accessory" -> R.drawable.item_icon_accessory
        "snack" -> R.drawable.item_icon_food
        else -> R.drawable.item_icon_generic
    }
}

private fun modSlotsForBaseSlot(slot: String): List<String> {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> listOf("weapon_mod1", "weapon_mod2")
        "armor" -> listOf("armor_mod1", "armor_mod2")
        else -> emptyList()
    }
}

private fun InventoryPreviewItemUi.isModItem(): Boolean {
    val normalizedType = type.lowercase(Locale.getDefault())
    return normalizedType == "mod" || equipment?.slot?.equals("mod", true) == true
}

private fun slotLabel(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
            }
        }

private fun filterGearOptionsForPreview(
    items: List<InventoryPreviewItemUi>,
    slotId: String,
    characterId: String
): List<InventoryPreviewItemUi> {
    val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
    return items.mapNotNull { item ->
        val equipment = item.equipment ?: synthesizePreviewEquipment(item) ?: return@mapNotNull null
        if (GearRules.matchesSlot(equipment, normalizedSlot, characterId, item.type)) item else null
    }.sortedBy { it.name.lowercase(Locale.getDefault()) }
}

private fun synthesizePreviewEquipment(item: InventoryPreviewItemUi): Equipment? {
    val normalizedType = item.type.trim().lowercase(Locale.getDefault())
    val slot = when {
        GearRules.equipSlots.contains(normalizedType) -> normalizedType
        GearRules.isWeaponType(normalizedType) -> "weapon"
        else -> null
    } ?: return null
    val weaponType = normalizedType.takeIf { GearRules.isWeaponType(it) }
    return Equipment(slot = slot, weaponType = weaponType)
}

@DrawableRes
private fun weaponIconRes(item: Item): Int {
    val weaponType = item.equipment?.weaponType?.lowercase(Locale.getDefault())
        ?: item.type.lowercase(Locale.getDefault())
    return when {
        weaponType.contains("gun") -> R.drawable.item_icon_gun
        weaponType.contains("glove") -> R.drawable.item_icon_gloves
        weaponType.contains("jewel") || weaponType.contains("pendant") -> R.drawable.item_icon_pendant
        weaponType.contains("sword") || weaponType.contains("blade") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}

@DrawableRes
private fun weaponTypeIconRes(type: String?): Int {
    val normalized = type?.lowercase(Locale.getDefault()) ?: return R.drawable.item_icon_generic
    return when {
        normalized.contains("gun") -> R.drawable.item_icon_gun
        normalized.contains("glove") -> R.drawable.item_icon_gloves
        normalized.contains("jewel") || normalized.contains("pendant") -> R.drawable.item_icon_pendant
        normalized.contains("sword") || normalized.contains("blade") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}

@DrawableRes
private fun armorIconRes(item: Item): Int {
    val name = item.name.lowercase(Locale.getDefault())
    return when {
        name.contains("pendant") -> R.drawable.item_icon_pendant
        name.contains("glove") -> R.drawable.item_icon_gloves
        else -> R.drawable.item_icon_armor
    }
}

private fun weaponAbilityLines(item: Item): List<String> {
    val equipment = item.equipment ?: return listOf("No weapon data available.")
    val lines = mutableListOf<String>()
    equipment.weaponType?.takeIf { it.isNotBlank() }?.let { lines += "Type: ${slotLabel(it)}" }
    val min = equipment.damageMin
    val max = equipment.damageMax
    if (min != null || max != null) {
        val minLabel = min?.toString() ?: "?"
        val maxLabel = max?.toString() ?: "?"
        lines += "Damage: $minLabel-$maxLabel"
    }
    attackStyleLabel(equipment.attackStyle)?.let { lines += "Style: $it" }
    equipment.attackPowerMultiplier?.let { lines += "Power: ${formatMultiplier(it)}" }
    equipment.attackElement?.takeIf { it.isNotBlank() }?.let { lines += "Element: ${slotLabel(it)}" }
    equipment.attackChargeTurns?.takeIf { it > 0 }?.let { turns ->
        lines += "Charge: $turns turn${if (turns == 1) "" else "s"}"
    }
    equipment.attackSplashMultiplier?.let { lines += "Splash: ${formatPercent(it)}" }
    equipment.statusOnHit?.takeIf { it.isNotBlank() }?.let { status ->
        val chance = equipment.statusChance?.takeIf { it > 0 }?.let { " (${formatPercent(it)})" } ?: ""
        lines += "Status: ${slotLabel(status)}$chance"
    }
    equipment.accuracy?.let { lines += "Accuracy: ${formatSignedNumber(it)}" }
    equipment.critRate?.let { lines += "Crit: ${formatPercent(it)}" }
    equipment.statMods?.forEach { (stat, value) ->
        val label = slotLabel(stat)
        val sign = if (value >= 0) "+" else ""
        lines += "$label: $sign$value"
    }
    return lines.ifEmpty { listOf("Standard issue weapon.") }
}

private fun armorAbilityLines(item: Item): List<String> {
    val equipment = item.equipment ?: return listOf("No armor data available.")
    val lines = mutableListOf<String>()
    equipment.defense?.let { lines += "Defense: ${if (it >= 0) "+$it" else it}" }
    equipment.hpBonus?.let { lines += "HP Bonus: ${if (it >= 0) "+$it" else it}" }
    equipment.accuracy?.let { lines += "Accuracy: ${formatSignedNumber(it)}" }
    equipment.critRate?.let { lines += "Crit Rate: ${formatPercent(it)}" }
    equipment.statMods?.forEach { (stat, value) ->
        val label = slotLabel(stat)
        val sign = if (value >= 0) "+" else ""
        lines += "$label: $sign$value"
    }
    return lines.ifEmpty { listOf("Standard issue armor.") }
}

private fun attackStyleLabel(raw: String?): String? {
    val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
    return when (normalized) {
        "single" -> "Single Target"
        "all" -> "All Enemies"
        "spread" -> "Spread Shot"
        "rocket" -> "Rocket Salvo"
        "charged_splash" -> "Charged Splash"
        else -> null
    }
}

private fun formatMultiplier(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "${text}x"
}

private fun formatPercent(value: Double): String {
    val percent = if (value <= 1.0) value * 100 else value
    return "${percent.roundToInt()}%"
}

private fun formatSignedNumber(value: Double): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.getDefault(), "%.1f", value)
    return if (value >= 0) "+$rounded" else rounded
}

@Composable
private fun WeaponSelectionDialog(
    characterName: String,
    weapons: List<Item>,
    equippedWeaponId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedWeaponId) { equippedWeaponId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            color = Color(0xFF060C14),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A1422), Color(0xFF050A11))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weapon Loadout",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = characterName.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    if (weapons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No weapons unlocked yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(weapons, key = { it.id }) { weapon ->
                                val isEquipped = weapon.id.lowercase(Locale.getDefault()) == equippedNormalized
                                WeaponFeatureCard(
                                    weapon = weapon,
                                    isEquipped = isEquipped,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    onSelect = { onSelect(weapon.id) }
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
private fun ArmorSelectionDialog(
    characterName: String,
    armors: List<Item>,
    equippedArmorId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedArmorId) { equippedArmorId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            color = Color(0xFF060C14),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A1422), Color(0xFF050A11))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Armor Loadout",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = characterName.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    if (armors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No armor unlocked yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(armors, key = { it.id }) { armor ->
                                val isEquipped = armor.id.lowercase(Locale.getDefault()) == equippedNormalized
                                ArmorFeatureCard(
                                    armor = armor,
                                    isEquipped = isEquipped,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    onSelect = { onSelect(armor.id) }
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
private fun WeaponFeatureCard(
    weapon: Item,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val iconRes = remember(weapon.id) { weaponIconRes(weapon) }
    val abilities = remember(weapon.id) { weaponAbilityLines(weapon) }
    val description = weapon.description?.takeIf { it.isNotBlank() } ?: "No description available."
    val shape = RoundedCornerShape(22.dp)
    val background = if (isEquipped) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val outline = if (isEquipped) accentColor else borderColor.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onSelect() },
        color = background,
        border = BorderStroke(1.dp, outline),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = weapon.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    if (isEquipped) {
                        Surface(
                            color = accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "EQUIPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Abilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    abilities.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmorFeatureCard(
    armor: Item,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val iconRes = remember(armor.id) { armorIconRes(armor) }
    val abilities = remember(armor.id) { armorAbilityLines(armor) }
    val description = armor.description?.takeIf { it.isNotBlank() } ?: "No description available."
    val shape = RoundedCornerShape(22.dp)
    val background = if (isEquipped) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val outline = if (isEquipped) accentColor else borderColor.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onSelect() },
        color = background,
        border = BorderStroke(1.dp, outline),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = armor.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    if (isEquipped) {
                        Surface(
                            color = accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "EQUIPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Abilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    abilities.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GearSelectionDialog(
    characterName: String,
    slotLabel: String,
    options: List<InventoryPreviewItemUi>,
    equippedId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onUnequip: () -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedId) { equippedId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Color(0xFF0A0F18),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$characterName - $slotLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUnequip() },
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Text("Unequip")
                    }
                }

                if (options.isEmpty()) {
                    Text(
                        text = "No $slotLabel available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            val normalizedId = option.id.lowercase(Locale.getDefault())
                            val isEquipped = normalizedId == equippedNormalized
                            val shape = RoundedCornerShape(14.dp)
                            val iconRes = remember(option.id + option.type) { previewItemIconRes(option.type) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .clickable { onSelect(option.id) },
                                color = if (isEquipped) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isEquipped) accentColor else borderColor.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = option.type,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isEquipped) {
                                        Surface(
                                            color = accentColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "EQUIPPED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

@DrawableRes
private fun previewItemIconRes(type: String?): Int {
    val normalized = type?.lowercase(Locale.getDefault()) ?: return R.drawable.item_icon_generic
    return when {
        normalized.contains("food") || normalized == "snack" -> R.drawable.item_icon_food
        normalized in setOf("consumable", "medicine", "tonic", "drink") -> R.drawable.item_icon_consumable
        normalized.contains("fish") -> R.drawable.item_icon_fish
        normalized.contains("fishing") -> R.drawable.item_icon_fishing
        normalized.contains("lure") -> R.drawable.item_icon_lure
        normalized.contains("ingredient") ||
            normalized.contains("material") -> R.drawable.item_icon_ingredient
        normalized.contains("component") ||
            normalized.contains("resource") ||
            normalized.contains("part") -> R.drawable.item_icon_material
        normalized.contains("armor") -> R.drawable.item_icon_armor
        normalized.contains("accessory") -> R.drawable.item_icon_accessory
        normalized.contains("weapon") || normalized.contains("gear") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}

@Composable
private fun JournalTabContent(
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

@Composable
private fun MapTabContent(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    isCurrentRoomDark: Boolean,
    accentColor: Color,
    borderColor: Color,
    onMenuAction: () -> Unit,
    onOpenMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit
) {
    MenuSectionCard(
        title = "Navigation",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        MapPreviewPanel(
            minimap = minimap,
            fullMap = fullMap,
            isCurrentRoomDark = isCurrentRoomDark,
            accentColor = accentColor,
            onMenuAction = onMenuAction,
            onMapLegend = onOpenMapLegend,
            onOpenFullMap = onOpenFullMap
        )
    }
}

@Composable
private fun StatsTabContent(
    partyStatus: PartyStatusUi,
    accentColor: Color,
    borderColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    MenuSectionCard(
        title = "Party Status",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        PartyStatusPanel(
            partyStatus = partyStatus,
            accentColor = accentColor,
            onShowSkillTree = onShowSkillTree,
            onShowDetails = onShowDetails
        )
    }
}

@Composable
private fun SettingsTabContent(
    settings: SettingsUiState,
    accentColor: Color,
    borderColor: Color,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit
) {
    MenuSectionCard(
        title = "Audio & Display",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        SettingsPanel(
            settings = settings,
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

@Composable
private fun MenuSectionCard(
    title: String,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PartyStatusPanel(
    partyStatus: PartyStatusUi,
    accentColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    if (partyStatus.members.isEmpty()) {
        Text(
            text = "No party members yet.",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        partyStatus.members.forEach { member ->
            PartyMemberCard(
                member = member,
                accentColor = accentColor,
                onShowSkillTree = onShowSkillTree,
                onShowDetails = onShowDetails
            )
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun PartyMemberCard(
    member: PartyMemberStatusUi,
    accentColor: Color,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit
) {
    val portraitPainter = rememberAssetPainter(
        imagePath = member.portraitPath,
        fallbackRes = R.drawable.inventory_icon
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.22f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = portraitPainter,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = member.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Lv ${member.level}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            ThemedMenuButton(
                label = "Details",
                accentColor = accentColor,
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 120.dp)
                    .heightIn(min = 40.dp),
                onClick = { onShowDetails(member.id) }
            )
        }

        val bars = buildList {
            member.hpProgress?.let { add(Triple(member.hpLabel ?: "HP", it, Color(0xFFFF5252))) }
            add(Triple("XP", member.xpProgress, Color(0xFF7C4DFF)))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEach { (label, progress, color) ->
                MiniStatBar(
                    label = label,
                    progress = progress,
                    accentColor = color
                )
            }
        }
    }
}

@Composable
private fun MiniStatBar(
    label: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall
        )
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun PartyMemberDetailsDialog(
    details: PartyMemberDetailsUi,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val onSurface = MaterialTheme.colorScheme.onSurface
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
        title = {
            Text(
                text = "${details.name} • Lv ${details.level}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val portraitPath = details.portraitPath
                    if (!portraitPath.isNullOrBlank()) {
                        Image(
                            painter = rememberAssetPainter(portraitPath, R.drawable.inventory_icon),
                            contentDescription = details.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, onSurface.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = details.xpLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface
                        )
                        details.hpLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                StatSection(title = "Attributes", stats = details.primaryStats)
                StatSection(title = "Combat Stats", stats = details.combatStats)
                if (details.unlockedSkills.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Unlocked Skills",
                            style = MaterialTheme.typography.titleSmall,
                            color = onSurface
                        )
                        details.unlockedSkills.forEach { skill ->
                            Text(
                                text = "• $skill",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun EventAnnouncementOverlay(
    announcement: EventAnnouncementUi,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventAccent = Color(announcement.accentColor)
    val accentColor = themeColor(theme?.accent, eventAccent)
    val outlineColor = themeColor(theme?.border, eventAccent.copy(alpha = 0.8f))
    val backgroundColor = themeColor(theme?.bg, Color(0xFF040914)).copy(alpha = 0.95f)
    val hasTitle = !announcement.title.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .clickable(onClick = onDismiss),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, outlineColor),
            color = backgroundColor,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(start = 32.dp, top = 28.dp, end = 32.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasTitle) {
                    Text(
                        text = announcement.title!!,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            shadow = Shadow(
                                color = accentColor.copy(alpha = 0.65f),
                                blurRadius = 18f
                            )
                        ),
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(color = accentColor.copy(alpha = 0.4f))
                }
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = if (hasTitle) 8.dp else 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatSection(
    title: String,
    stats: List<CharacterStatValueUi>
) {
    if (stats.isEmpty()) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            stats.forEach { stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface
                    )
                }
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
private fun QuickContextActions(
    onOpenInventory: (InventoryLaunchOptions) -> Unit,
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
                    onOpenInventory(InventoryLaunchOptions())
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
private fun SettingsPanel(
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Music Volume ${ (settings.musicVolume * 100).roundToInt() }%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = settings.musicVolume,
                onValueChange = onMusicVolumeChange,
                valueRange = 0f..1f
            )
        }
        Column {
            Text(
                text = "Effects Volume ${ (settings.sfxVolume * 100).roundToInt() }%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = settings.sfxVolume,
                onValueChange = onSfxVolumeChange,
                valueRange = 0f..1f
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Room Vignette", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.vignetteEnabled) "Enabled" else "Disabled",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.vignetteEnabled,
                onCheckedChange = onToggleVignette
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tutorials", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (settings.tutorialsEnabled) "Shown" else "Hidden",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = settings.tutorialsEnabled,
                onCheckedChange = onToggleTutorials
            )
        }
        Button(
            onClick = onSaveGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
        Button(
            onClick = onLoadGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load")
        }
        Button(
            onClick = onQuickSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quick Save")
        }
        Text(
            text = "Quicksave writes your current progress immediately without leaving the game.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MapPreviewPanel(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    isCurrentRoomDark: Boolean,
    accentColor: Color,
    onMenuAction: () -> Unit,
    onMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit
) {
    val fullMapAvailable = fullMap?.cells?.isNotEmpty() == true
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (fullMapAvailable && fullMap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                FullMapCanvas(
                    fullMap = fullMap,
                    scale = 1f,
                    offset = Offset.Zero,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Text(
                text = "Survey more rooms in this node to unlock the full map.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            minimap?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MinimapWidget(
                        minimap = it,
                        onLegend = {
                            onMenuAction()
                            onOpenFullMap()
                        },
                        obscured = isCurrentRoomDark,
                        modifier = Modifier.size(140.dp)
                    )
                }
            }
        }
        ThemedMenuButton(
            label = "Map Legend",
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onMenuAction()
                onMapLegend()
            }
        )
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
private fun SkillTreeOverlay(
    overlay: SkillTreeOverlayUi,
    theme: Theme?,
    onClose: () -> Unit,
    onUnlockSkill: (String) -> Unit
) {
    val accentColor = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = 0.65f))
    val scrimColor = Color.Black.copy(alpha = 0.82f)
    val scrollState = rememberScrollState()
    val portraitPainter = rememberAssetPainter(
        imagePath = overlay.portraitPath,
        fallbackRes = R.drawable.inventory_icon
    )
    var selectedBranchIndex by rememberSaveable(overlay.characterId) { mutableStateOf(0) }
    if (overlay.branches.isNotEmpty()) {
        val maxIndex = overlay.branches.lastIndex
        if (selectedBranchIndex > maxIndex) {
            selectedBranchIndex = maxIndex
        } else if (selectedBranchIndex < 0) {
            selectedBranchIndex = 0
        }
    } else if (selectedBranchIndex != 0) {
        selectedBranchIndex = 0
    }
    val selectedBranch = overlay.branches.getOrNull(selectedBranchIndex)
    var selectedNodeId by rememberSaveable(overlay.characterId, selectedBranchIndex) {
        mutableStateOf(selectedBranch?.nodes?.firstOrNull()?.id)
    }
    val selectedNode = selectedBranch?.nodes?.firstOrNull { it.id == selectedNodeId }
        ?: selectedBranch?.nodes?.firstOrNull()

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .padding(20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF02070E).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = overlay.characterName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${overlay.characterName} – Skills",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "AP Available: ${overlay.availableAp}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        TextButton(onClick = onClose) {
                            Text("Close", color = accentColor)
                        }
                    }

                    SkillTreeBranchTabs(
                        branches = overlay.branches,
                        selectedIndex = selectedBranchIndex,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onSelect = { selectedBranchIndex = it }
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
                        color = Color.White.copy(alpha = 0.02f)
                    ) {
                        if (selectedBranch != null && selectedBranch.nodes.isNotEmpty()) {
                            SkillTreeGrid(
                                branch = selectedBranch,
                                accentColor = accentColor,
                                selectedNodeId = selectedNode?.id,
                                onSelectNode = { node ->
                                    selectedNodeId = node.id
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No skills available for this branch yet.",
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    SkillTreeNodeDetails(
                        node = selectedNode,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onUnlock = { onUnlockSkill(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTreeBranchTabs(
    branches: List<SkillTreeBranchUi>,
    selectedIndex: Int,
    accentColor: Color,
    borderColor: Color,
    onSelect: (Int) -> Unit
) {
    if (branches.isEmpty()) {
        Text(
            text = "No skill data available.",
            color = Color.White.copy(alpha = 0.7f)
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        branches.forEachIndexed { index, branch ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) accentColor else borderColor.copy(alpha = 0.7f)
                ),
                color = if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent
            ) {
                Text(
                    text = branch.title,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SkillTreeGrid(
    branch: SkillTreeBranchUi,
    accentColor: Color,
    selectedNodeId: String?,
    onSelectNode: (SkillTreeNodeUi) -> Unit
) {
    val nodesByPosition = remember(branch) {
        branch.nodes.associateBy { it.row to it.column }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { col ->
                    val node = nodesByPosition[row to col]
                    SkillTreeGridCell(
                        node = node,
                        accentColor = accentColor,
                        selected = node?.id == selectedNodeId,
                        onSelect = onSelectNode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTreeGridCell(
    node: SkillTreeNodeUi?,
    accentColor: Color,
    selected: Boolean,
    onSelect: (SkillTreeNodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (node == null) {
        Box(
            modifier = modifier.aspectRatio(1f)
        )
        return
    }
    val status = node.status
    val background = when {
        status.unlocked -> accentColor.copy(alpha = 0.25f)
        status.canPurchase -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.03f)
    }
    val borderColor = when {
        selected -> accentColor
        status.unlocked -> accentColor.copy(alpha = 0.9f)
        status.canPurchase -> accentColor.copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.3f)
    }
    Surface(
        onClick = { onSelect(node) },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        color = background,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = node.name,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2
            )
            Text(
                text = when {
                    status.unlocked -> "Learned"
                    else -> "Cost: ${node.costAp} AP"
                },
                color = Color.White.copy(alpha = if (status.canPurchase) 0.9f else 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SkillTreeNodeDetails(
    node: SkillTreeNodeUi?,
    accentColor: Color,
    borderColor: Color,
    onUnlock: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        color = Color.White.copy(alpha = 0.02f)
    ) {
        if (node == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a skill node to see the details.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            return@Surface
        }
        val status = node.status
        val unmetLabels = status.unmetRequirements.mapNotNull { unmetId ->
            node.requirements.firstOrNull { it.id == unmetId }?.label
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = node.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Cost: ${node.costAp} AP",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
            val description = node.description ?: "Unique ability upgrade."
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
            node.requirements.takeIf { it.isNotEmpty() }?.let { requirements ->
                Text(
                    text = "Requires: ${requirements.joinToString(", ") { it.label }}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!status.meetsTierRequirement) {
                Text(
                    text = "Invest ${status.requiredApForTier} AP in this tree to unlock this tier.",
                    color = Color(0xFFFFD27F),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (unmetLabels.isNotEmpty()) {
                Text(
                    text = "Still needed: ${unmetLabels.joinToString(", ")}",
                    color = Color(0xFFFF9E8C),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onUnlock(node.id) },
                    enabled = status.canPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        text = when {
                            status.unlocked -> "Learned"
                            status.canPurchase -> "Unlock"
                            else -> "Locked"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemedMenuButton(
    label: String,
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        color = if (enabled) accentColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = if (enabled) 0.35f else 0.15f),
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
                    description = "3×3 grid around you with a highlighted center tile.",
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
private fun FullMapCanvas(
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
private fun RoomDescription(
    plan: InlineActionPlan?,
    description: String?,
    isDark: Boolean,
    textColor: Color,
    onAction: (RoomAction) -> Unit,
    onNpcClick: (String) -> Unit,
    onEnemyClick: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (description.isNullOrBlank()) {
        Text(
            text = "No description available.",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = 0.92f),
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }
    if (plan == null) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Start,
            modifier = modifier
        )
        return
    }

    val defaultColor = textColor
    val highlightColor = accentColor.copy(alpha = if (isDark) 0.78f else 0.92f)
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val enemyAccent = Color(0xFFFF8A80)

    val annotatedText = remember(plan, highlightColor, disabledColor, accentColor) {
        buildAnnotatedString {
            append(plan.description)
            plan.segments.forEach { segment ->
                val (color, weight, decoration) = when (segment.target) {
                    is InlineActionTarget.Npc -> Triple(accentColor, FontWeight.Bold, TextDecoration.Underline)
                    is InlineActionTarget.Enemy -> Triple(enemyAccent, FontWeight.SemiBold, TextDecoration.Underline)
                    is InlineActionTarget.Room -> {
                        val clr = if (segment.locked) disabledColor else highlightColor
                        Triple(clr, FontWeight.Bold, if (segment.locked) TextDecoration.None else TextDecoration.Underline)
                    }
                }
                addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = weight,
                        textDecoration = decoration
                    ),
                    start = segment.start,
                    end = segment.end
                )
                addStringAnnotation(
                    tag = ACTION_TAG,
                    annotation = segment.id,
                    start = segment.start,
                    end = segment.end
                )
            }
        }
    }
    val actionLookup = remember(plan) {
        plan.segments.associateBy { it.id }
    }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = defaultColor, textAlign = TextAlign.Start)
    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = bodyStyle
    ) { offset ->
        annotatedText.getStringAnnotations(ACTION_TAG, offset, offset).firstOrNull()?.let { annotation ->
            val segment = actionLookup[annotation.item] ?: return@ClickableText
            when (val target = segment.target) {
                is InlineActionTarget.Room -> if (!segment.locked) onAction(target.action)
                is InlineActionTarget.Npc -> onNpcClick(target.name)
                is InlineActionTarget.Enemy -> onEnemyClick(target.id)
            }
        }
    }
}

@Composable
private fun MenuToggleButton(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val painter = rememberAssetPainter("images/ui/menu_button.png")
    val description = if (isOpen) "Close menu" else "Open menu"
    val baseAlpha = if (isOpen) 0.75f else 1f
    val alpha = if (enabled) baseAlpha else baseAlpha * 0.35f
    Image(
        painter = painter,
        contentDescription = description,
        modifier = modifier
            .size(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled, onClick = onToggle)
    )
}

@Composable
private fun ReturnHubButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val painter = rememberAssetPainter("images/ui/return_hub_icon.png")
    Image(
        painter = painter,
        contentDescription = "Return to hub",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    )
}

private data class QuickMenuAction(
    val iconRes: Int,
    val label: String,
    val roomAction: RoomAction
)

@Composable
private fun ServiceActionTray(
    actions: List<QuickMenuAction>,
    onAction: (RoomAction) -> Unit,
    backgroundColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    val trayShape = RoundedCornerShape(22.dp)
    val borderColor = accentColor.copy(alpha = 0.4f)
    val iconBackground = remember(accentColor, backgroundColor) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.25f),
                backgroundColor.copy(alpha = 0.9f)
            )
        )
    }
    val motion = rememberInfiniteTransition(label = "serviceTrayMotion")
    val motionCycle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "serviceTrayMotionCycle"
    )
    Surface(
        modifier = modifier,
        shape = trayShape,
        color = backgroundColor.copy(alpha = 0.75f),
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                val density = LocalDensity.current
                val phaseOffset = remember(action.label, action.iconRes) {
                    (abs("${action.label}:${action.iconRes}".lowercase(Locale.getDefault()).hashCode()) % 1000) / 1000f
                }
                val t = (motionCycle + phaseOffset) % 1f
                val wave = kotlin.math.sin(t * Math.PI * 2.0).toFloat()
                val glow = (0.5f + 0.5f * wave).coerceIn(0f, 1f)
                val bobPx = with(density) { 1.2.dp.toPx() } * wave
                val scale = 1f + 0.012f * wave
                Column(
                    modifier = Modifier
                        .widthIn(min = 78.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onAction(action.roomAction) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                translationY = bobPx
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(iconBackground)
                            .border(BorderStroke(1.25.dp, borderColor.copy(alpha = 0.65f * (0.85f + 0.15f * glow))), CircleShape)
                            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.55f * (0.8f + 0.2f * glow))), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(action.iconRes),
                            contentDescription = action.label,
                            modifier = Modifier.size(34.dp),
                            colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.9f + 0.08f * glow))
                        )
                    }
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private sealed interface InlineActionTarget {
    data class Room(val action: RoomAction) : InlineActionTarget
    data class Npc(val name: String) : InlineActionTarget
    data class Enemy(val id: String, val label: String) : InlineActionTarget
}

private data class InlineActionSegment(
    val id: String,
    val target: InlineActionTarget,
    val start: Int,
    val end: Int,
    val locked: Boolean
)

private data class InlineActionPlan(
    val description: String,
    val segments: List<InlineActionSegment>
)

private const val ACTION_TAG = "action"
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
private fun RoomDescriptionPanel(
    currentRoom: Room?,
    description: String?,
    plan: InlineActionPlan?,
    isDark: Boolean,
    textColor: Color,
    onAction: (RoomAction) -> Unit,
    onNpcClick: (String) -> Unit,
    onEnemyClick: (String) -> Unit,
    groundItems: Map<String, Int>,
    itemDisplayName: (String) -> String,
    onCollectItem: (String) -> Unit,
    onCollectAll: () -> Unit,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    borderColor: Color,
    backgroundColor: Color,
    fallbackNpcs: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (currentRoom == null && description.isNullOrBlank()) return
    val scrollState = rememberScrollState()
    LaunchedEffect(currentRoom?.id) {
        scrollState.scrollTo(0)
    }
    Surface(
        modifier = modifier,
        color = if (isDark) Color.Black.copy(alpha = 0.74f) else backgroundColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.4.dp, borderColor.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoomDescription(
                plan = plan,
                description = description,
                isDark = isDark,
                textColor = textColor,
                onAction = onAction,
                onNpcClick = onNpcClick,
                onEnemyClick = onEnemyClick,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
            val enemyParties = currentRoom?.let { room ->
                val partyCandidates = room.enemyParties
                    .orEmpty()
                    .mapNotNull { party ->
                        party.mapNotNull { id -> id.trim().takeIf { trimmed -> trimmed.isNotBlank() } }
                            .takeIf { it.isNotEmpty() }
                    }
                if (partyCandidates.isNotEmpty()) {
                    partyCandidates
                } else {
                    room.enemies
                        .mapNotNull { id -> id.trim().takeIf { trimmed -> trimmed.isNotBlank() } }
                        .map { listOf(it) }
                }
            }.orEmpty()
            val itemFlavor = currentRoom?.itemFlavor.orEmpty()
            val hasFinds = groundItems.isNotEmpty()
            val hasAnySections = fallbackNpcs.isNotEmpty() || enemyParties.isNotEmpty() || hasFinds
            if (hasAnySections) {
                HorizontalDivider(color = borderColor.copy(alpha = if (isDark) 0.28f else 0.18f))
            }
            if (fallbackNpcs.isNotEmpty()) {
                Text(
                    text = "People",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    fallbackNpcs.forEach { npc ->
                        RoomActionChip(
                            label = npc,
                            accentColor = accentColor,
                            onClick = { onNpcClick(npc) }
                        )
                    }
                }
                if (enemyParties.isNotEmpty() || hasFinds) {
                    HorizontalDivider(color = borderColor.copy(alpha = if (isDark) 0.28f else 0.18f))
                }
            }
            val showEnemyFlavorText = false
            if (showEnemyFlavorText) {
                val enemyFlavor = currentRoom?.enemyFlavor.orEmpty()
                val partyLeaders = when {
                    currentRoom?.enemyParties.isNullOrEmpty() ->
                        currentRoom?.enemies.orEmpty()
                            .mapNotNull { enemyId -> enemyId.trim().takeIf { it.isNotBlank() } }
                    else ->
                        currentRoom?.enemyParties.orEmpty()
                            .mapNotNull { party -> party.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } }
                }
                val enemyLines = partyLeaders.mapNotNull { enemyId ->
                    enemyFlavor[enemyId]?.let { enemyId to it }
                }
                if (!isDark && enemyLines.isNotEmpty()) {
                    EnemyFlavorBlock(
                        entries = enemyLines,
                        accentColor = accentColor,
                        onEnemyClick = onEnemyClick
                    )
                }
            }
            if (enemyParties.isNotEmpty()) {
                EnemyPartyStrip(
                    parties = enemyParties,
                    enemyTiers = enemyTiers,
                    enemyIcons = enemyIcons,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    isDark = isDark,
                    onPartyClick = { leaderId ->
                        if (leaderId.isNotBlank()) onEnemyClick(leaderId)
                    }
                )
                if (hasFinds) {
                    HorizontalDivider(color = borderColor.copy(alpha = if (isDark) 0.28f else 0.18f))
                }
            }
            if (hasFinds) {
                RoomFindsBlock(
                    items = groundItems,
                    itemFlavor = itemFlavor,
                    itemDisplayName = itemDisplayName,
                    onCollectItem = onCollectItem,
                    onCollectAll = onCollectAll,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    isDark = isDark
                )
            }
        }
    }
}

private fun actionLabelFallback(action: RoomAction): String = when (action) {
    is ContainerAction -> "Search"
    is ToggleAction -> "Toggle"
    is TinkeringAction -> "Tinkering"
    is CookingAction -> "Cooking"
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
    parties: List<List<String>>,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onPartyClick: (String) -> Unit
) {
    val motion = rememberInfiniteTransition(label = "hostilesMotion")
    val motionCycle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hostilesMotionCycle"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                parties.forEach { party ->
                    val leaderId = party.firstOrNull()?.trim().orEmpty()
                    if (leaderId.isBlank()) return@forEach
                    val partySize = party.count { it.trim().isNotBlank() }.coerceAtLeast(1)
                    val tier = parseEnemyTier(enemyTiers[leaderId])
                    EnemyPartyChip(
                        leaderId = leaderId,
                        partySize = partySize,
                        tier = tier,
                        motionCycle = motionCycle,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        isDark = isDark,
                        icon = enemyIcons[leaderId],
                        onClick = { onPartyClick(leaderId) }
                )
            }
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

@Composable
private fun EnemyPartyChip(
    leaderId: String,
    partySize: Int,
    tier: EnemyTier,
    motionCycle: Float,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    icon: EnemyIconUi?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent
    ) {
        EnemyPartyLeaderIcon(
            enemyId = leaderId,
            partySize = partySize,
            tier = tier,
            motionCycle = motionCycle,
            accentColor = accentColor,
            borderColor = borderColor,
            isDark = isDark,
            icon = icon
        )
    }
}

@Composable
private fun EnemyPartyLeaderIcon(
    enemyId: String,
    partySize: Int,
    tier: EnemyTier,
    motionCycle: Float,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    icon: EnemyIconUi?
) {
    val iconPath = icon?.spritePath ?: remember(enemyId) { "images/enemies/${enemyId}_combat.png" }
    val painter = rememberAssetPainter(iconPath, R.drawable.inventory_icon)
    val iconTint = accentColor.copy(alpha = if (isDark) 0.72f else 0.92f)
    val tierAccent = when (tier) {
        EnemyTier.BOSS -> Color(0xFFFFD54F)
        EnemyTier.ELITE -> Color(0xFFFFB74D)
        EnemyTier.COMMON -> accentColor
    }
    val tierBorderWidth = when (tier) {
        EnemyTier.BOSS -> 3.2.dp
        EnemyTier.ELITE -> 2.4.dp
        EnemyTier.COMMON -> 0.dp
    }
    val tierLabel = when (tier) {
        EnemyTier.BOSS -> "☠"
        EnemyTier.ELITE -> "★"
        EnemyTier.COMMON -> null
    }
    val density = LocalDensity.current
    val phaseOffset = remember(enemyId) {
        (abs(enemyId.lowercase(Locale.getDefault()).hashCode()) % 1000) / 1000f
    }
    val t = (motionCycle + phaseOffset) % 1f
    val wave = kotlin.math.sin(t * Math.PI * 2.0).toFloat()
    val glow = (0.5f + 0.5f * wave).coerceIn(0f, 1f)
    val bobAmplitude = if (isDark) 1.2.dp else 2.0.dp
    val bobPx = with(density) { bobAmplitude.toPx() } * wave
    val scale = 1f + (if (isDark) 0.012f else 0.02f) * wave
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 12.dp, bottomEnd = 20.dp, bottomStart = 12.dp)
    val background = remember(accentColor, isDark, tierAccent, tier) {
        val highlight = if (tier == EnemyTier.COMMON) accentColor else tierAccent
        Brush.radialGradient(
            colors = listOf(
                highlight.copy(alpha = if (isDark) 0.26f else 0.38f),
                Color(0xFF050A10).copy(alpha = if (isDark) 0.92f else 0.88f)
            )
        )
    }
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    translationY = bobPx
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(background)
                .border(
                    BorderStroke(
                        1.35.dp,
                        borderColor.copy(alpha = (if (isDark) 0.75f else 0.55f) * (0.9f + 0.1f * glow))
                    ),
                    shape
                )
                .border(
                    BorderStroke(
                        1.15.dp,
                        accentColor.copy(alpha = (if (isDark) 0.45f else 0.65f) * (0.75f + 0.25f * glow))
                    ),
                    shape
                )
                .then(
                    if (tierBorderWidth > 0.dp) {
                        Modifier.border(
                            BorderStroke(
                                tierBorderWidth,
                                tierAccent.copy(alpha = if (tier == EnemyTier.BOSS) 0.9f else 0.8f)
                            ),
                            shape
                        )
                    } else {
                        Modifier
                    }
                ),
        contentAlignment = Alignment.Center
    ) {
            val iconModifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
            val composite = icon?.composite
            if (composite != null) {
                CompositeEnemyIcon(
                    composite = composite,
                    tint = iconTint,
                    modifier = iconModifier
                )
            } else {
                Image(
                    painter = painter,
                    contentDescription = enemyId,
                    modifier = iconModifier,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(iconTint)
                )
            }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = with(density) { 2.dp.toPx() }
            val inset = strokeWidth / 2f + with(density) { 1.dp.toPx() }
            val arcColor = accentColor.copy(alpha = (if (isDark) 0.12f else 0.18f) + 0.1f * glow)
                drawArc(
                    color = arcColor,
                    startAngle = t * 360f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        if (tier == EnemyTier.ELITE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = with(density) { 1.6.dp.toPx() }
                val inset = with(density) { 6.dp.toPx() }
                val mark = with(density) { 10.dp.toPx() }
                val markColor = tierAccent.copy(alpha = if (tier == EnemyTier.BOSS) 0.95f else 0.85f)
                drawLine(
                    color = markColor,
                    start = Offset(inset, inset + mark),
                    end = Offset(inset + mark, inset),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = markColor,
                    start = Offset(size.width - inset - mark, inset),
                    end = Offset(size.width - inset, inset + mark),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        if (partySize > 1 && tier != EnemyTier.BOSS) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.95f),
                modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Text(
                        text = partySize.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (tierLabel != null) {
            val badgeTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (tier == EnemyTier.BOSS) 20.sp else 11.sp
            )
            val badgePadding = if (tier == EnemyTier.BOSS) {
                PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            } else {
                PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tierAccent.copy(alpha = 0.98f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Text(
                    text = tierLabel,
                    style = badgeTextStyle,
                    color = Color.Black,
                    modifier = Modifier.padding(badgePadding)
                )
            }
        }
    }
}

}

@Composable
private fun CompositeEnemyIcon(
    composite: EnemyCompositeIconUi,
    tint: Color,
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
                painter = rememberAssetPainter(part.spritePath, R.drawable.inventory_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier
                    .size(width = partWidth, height = partHeight)
                    .offset(x = offsetX, y = offsetY)
                    .zIndex(part.z)
            )
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

private fun buildInlineActionPlan(
    description: String?,
    actions: List<RoomAction>,
    hints: Map<String, ActionHintUi>,
    room: Room?
): InlineActionPlan? {
    if (description.isNullOrBlank()) return null
    val lower = description.lowercase(Locale.getDefault())
    val segments = mutableListOf<InlineActionSegment>()
    val occupied = mutableListOf<IntRange>()

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
private fun PromptBanner(
    title: String,
    message: String,
    accentColor: Color,
    backgroundColor: Color = Color(0xFF060B13).copy(alpha = 0.96f),
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
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            backgroundColor.copy(alpha = 0.98f),
                            backgroundColor.copy(alpha = 0.94f),
                            backgroundColor.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(1.4.dp, accentColor.copy(alpha = 0.45f), shape)
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
fun NarrationCard(
    prompt: NarrationPrompt,
    theme: Theme?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = themeColor(theme?.accent, Color(0xFF8DE2FF))
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = 0.7f))
    val backgroundColor = Color(0xFF030910).copy(alpha = 0.95f)
    val shape = RoundedCornerShape(28.dp)
    val baseModifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(min = 360.dp, max = 720.dp)
        .let { mod ->
            if (prompt.tapToDismiss) {
                mod.clickable(onClick = onDismiss)
            } else {
                mod
            }
        }

    Surface(
        modifier = baseModifier.then(modifier),
        color = backgroundColor,
        shape = shape,
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.85f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Narration",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = prompt.message,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text(if (prompt.tapToDismiss) "Got it" else "Dismiss")
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
        DialogueOverlay(
            dialogue = dialogueUi,
            choices = emptyList(),
            onAdvance = onAdvance,
            onChoice = { onAdvance() },
            onPlayVoice = {},
            modifier = modifier
        )
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .navigationBarsPadding(),
        color = Color(0xFF03070F).copy(alpha = 0.95f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.title?.let {
                Text(
                    text = it,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            state.step.speaker?.takeIf { it.isNotBlank() }?.let { speaker ->
                Text(
                    text = speaker,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = state.step.text,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.stepIndex + 1} / ${state.stepCount}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Button(onClick = onAdvance) {
                    Text(if (state.stepIndex + 1 >= state.stepCount) "Continue" else "Next")
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
