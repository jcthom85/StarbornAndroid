package com.example.starborn.feature.exploration.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.model.CookingAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.FirstAidAction
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.serviceTag
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.quest.QuestLogEntryType
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.ui.background.rememberAssetPainter
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
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import com.example.starborn.feature.exploration.viewmodel.TogglePromptUi
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.exploration.viewmodel.SkillTreeBranchUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeNodeUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeOverlayUi
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
import com.example.starborn.ui.theme.MinimapTextStyle
import com.example.starborn.ui.theme.themeColor




@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    audioCuePlayer: AudioCuePlayer,
    modifier: Modifier = Modifier,
    onEnemySelected: (List<String>) -> Unit = {},
    onOpenInventory: (InventoryLaunchOptions) -> Unit = {},
    onOpenTinkering: (String?) -> Unit = {},
    onOpenCooking: (String?) -> Unit = {},
    onOpenFirstAid: (String?) -> Unit = {},
    onOpenFishing: (String?) -> Unit = {},
    onOpenShop: (String) -> Unit = {},
    fxEvents: Flow<String>? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = ExplorationUiState())
    val fxBursts = remember { mutableStateListOf<UiFxBurst>() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> onEnemySelected(event.enemyIds)
                is ExplorationEvent.PlayCinematic -> Unit
                is ExplorationEvent.ShowMessage -> viewModel.showStatusMessage(event.message)
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
        val vignetteIntensity = if (isRoomDark) 0.6f else 0.0f
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
        val matchedActionKeys = inlinePlan?.segments
            ?.mapNotNull { (it.target as? InlineActionTarget.Room)?.action?.actionKey() }
            ?.toSet()
            .orEmpty()
        val fallbackActions = remember(uiState.actions, matchedActionKeys) {
            uiState.actions.filterNot { matchedActionKeys.contains(it.actionKey()) }
        }
        val matchedNpcNames = inlinePlan?.segments
            ?.mapNotNull { (it.target as? InlineActionTarget.Npc)?.name }
            ?.toSet()
            .orEmpty()
        val matchedEnemyIds = inlinePlan?.segments
            ?.mapNotNull { (it.target as? InlineActionTarget.Enemy)?.id }
            ?.toSet()
            .orEmpty()
        val fallbackNpcs = currentRoom?.npcs.orEmpty().filter { it.isNotBlank() && !matchedNpcNames.contains(it) }
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
                    onLegend = { viewModel.openMinimapLegend() },
                    modifier = Modifier
                        .requiredSize(minimapSize)
                        .constrainAs(minimapRef) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        }
                )
                val titleColor = themeColor(activeTheme?.accent, Color(0xFFBEE9FF))
                val underlinePainter = painterResource(id = R.drawable.room_title_underline)
                Text(
                    text = uiState.currentRoom?.title ?: "Unknown area",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        textIndent = TextIndent(restLine = 14.sp)
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
                Image(
                    painter = underlinePainter,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(titleColor.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .padding(top = 0.dp, end = 16.dp)
                        .width(resolvedUnderlineWidth)
                        .height(underlineHeight)
                        .constrainAs(underlineRef) {
                            start.linkTo(titleTextRef.start)
                            top.linkTo(titleTextRef.bottom)
                        }
                )
            }

            RoomDescriptionPanel(
                currentRoom = currentRoom,
                description = baseRoomDescription,
                plan = inlinePlan,
                isDark = isRoomDark,
                onAction = { action -> viewModel.onActionSelected(action) },
                onNpcClick = { name -> viewModel.onNpcInteraction(name) },
                onEnemyClick = { enemyId -> viewModel.engageEnemy(enemyId) },
                borderColor = panelBorderColor,
                backgroundColor = panelBackgroundColor,
                fallbackActions = fallbackActions,
                fallbackNpcs = fallbackNpcs,
                actionHints = actionHints,
                accentColor = actionAccentColor,
                textColor = roomTextColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = descriptionTopPadding)
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 240.dp, max = 480.dp)
            )

            if (uiState.groundItems.isNotEmpty()) {
                GroundItemsPanel(
                    items = uiState.groundItems,
                    onCollect = { itemId -> viewModel.collectGroundItem(itemId) },
                    onCollectAll = { viewModel.collectAllGroundItems() },
                    backgroundColor = panelBackgroundColor.copy(alpha = if (isRoomDark) 0.75f else 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 120.dp)
                )
            }

            if (serviceQuickActions.isNotEmpty()) {
                ServiceActionTray(
                    actions = serviceQuickActions,
                    onAction = { viewModel.onActionSelected(it) },
                    backgroundColor = panelBackgroundColor.copy(alpha = if (isRoomDark) 0.65f else 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                )
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
                    .padding(bottom = 32.dp)
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

            AnimatedVisibility(
                visible = uiState.isMenuOverlayVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
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
                        viewModel.closeMenuOverlay()
                        viewModel.openMinimapLegend()
                    },
                    onOpenFullMap = {
                        viewModel.closeMenuOverlay()
                        viewModel.openFullMapOverlay()
                    },
                    settings = uiState.settings,
                    onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                    onSfxVolumeChange = { viewModel.updateSfxVolume(it) },
                    onToggleVignette = { viewModel.setVignetteEnabled(it) },
                    partyStatus = uiState.partyStatus,
                    onShowSkillTree = { memberId -> viewModel.openSkillTree(memberId) },
                    onShowDetails = { memberId -> viewModel.openPartyMemberDetails(memberId) },
                    statusMessage = uiState.statusMessage,
                    trackedQuest = trackedQuest,
                    minimap = uiState.minimap,
                    fullMap = uiState.fullMap,
                    theme = activeTheme,
                    onMenuAction = { viewModel.onMenuActionInvoked() },
                    inventoryItems = uiState.inventoryPreview,
                    equippedItems = uiState.equippedItems,
                    modifier = Modifier.fillMaxSize()
                )
            }

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

        if (uiState.isFullMapVisible) {
            FullMapOverlay(
                fullMap = uiState.fullMap,
                onClose = { viewModel.closeFullMapOverlay() }
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

        CraftingFxOverlay(
            bursts = fxBursts,
            onExpired = { id -> fxBursts.removeAll { it.id == id } },
            modifier = Modifier.fillMaxSize()
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
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onLegend),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
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

                    val isCurrent = cell.isCurrent
                    val pipColor = if (isCurrent) clrTileGlow else clrTile
                    val pipSize = g * if (isCurrent) 0.9f else 0.6f

                    drawCircle(
                        color = pipColor,
                        radius = pipSize / 2,
                        center = Offset(px, py)
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
    onOpenInventory: (InventoryLaunchOptions) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMapLegend: () -> Unit,
    onOpenFullMap: () -> Unit,
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    partyStatus: PartyStatusUi,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    statusMessage: String?,
    trackedQuest: QuestSummaryUi?,
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
    theme: Theme?,
    onMenuAction: () -> Unit,
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val panelColor = themeColor(theme?.bg, MaterialTheme.colorScheme.surface).copy(alpha = 0.95f)
    val panelBorder = themeColor(theme?.border, Color.White.copy(alpha = 0.4f))
    val accentColor = themeColor(theme?.accent, MaterialTheme.colorScheme.primary)
    val scrimColor = themeColor(theme?.bg, Color.Black).copy(alpha = 0.75f)
    val scrimInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    indication = null,
                    interactionSource = scrimInteraction
                ) { onClose() }
        )
    val sheetScroll = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = panelColor,
        border = BorderStroke(1.dp, panelBorder.copy(alpha = 0.8f))
    ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
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
                    statusMessage = statusMessage,
                    partyStatus = partyStatus,
                    trackedQuest = trackedQuest,
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
                    onToggleVignette = onToggleVignette,
                    onShowSkillTree = onShowSkillTree,
                    onShowDetails = onShowDetails,
                    inventoryItems = inventoryItems,
                    equippedItems = equippedItems
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
    statusMessage: String?,
    partyStatus: PartyStatusUi,
    trackedQuest: QuestSummaryUi?,
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
    onToggleVignette: (Boolean) -> Unit,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (tab) {
            MenuTab.INVENTORY -> InventoryTabContent(
                inventoryItems = inventoryItems,
                equippedItems = equippedItems,
                accentColor = accentColor,
                borderColor = borderColor,
                onOpenInventory = onOpenInventory
            )
            MenuTab.JOURNAL -> JournalTabContent(
                trackedQuest = trackedQuest,
                accentColor = accentColor,
                borderColor = borderColor,
                onMenuAction = onMenuAction,
                onOpenJournal = onOpenJournal
            )
            MenuTab.MAP -> MapTabContent(
                minimap = minimap,
                fullMap = fullMap,
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
                onToggleVignette = onToggleVignette
            )
        }
    }
}

private enum class InventoryCarouselPage { ITEMS, EQUIPMENT }

@Composable
private fun InventoryTabContent(
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    onOpenInventory: (InventoryLaunchOptions) -> Unit
) {
    var page by rememberSaveable { mutableStateOf(InventoryCarouselPage.ITEMS) }
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
            InventoryCarouselPage.ITEMS -> InventoryItemsPreview(
                items = inventoryItems,
                borderColor = borderColor
            )
            InventoryCarouselPage.EQUIPMENT -> InventoryEquipmentPreview(
                equippedItems = equippedItems,
                borderColor = borderColor,
                accentColor = accentColor,
                onOpenInventory = onOpenInventory
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
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
            label = "Items",
            selected = current == InventoryCarouselPage.ITEMS,
            onClick = { onSelect(InventoryCarouselPage.ITEMS) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        InventoryCarouselButton(
            label = "Equipment",
            selected = current == InventoryCarouselPage.EQUIPMENT,
            onClick = { onSelect(InventoryCarouselPage.EQUIPMENT) },
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
    borderColor: Color
) {
    if (items.isEmpty()) {
        Text(
            text = "No items collected yet. Explore rooms to gather supplies.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(6).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.35f)), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "x${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        if (items.size > 6) {
            Text(
                text = "+${items.size - 6} more items",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun InventoryEquipmentPreview(
    equippedItems: Map<String, String>,
    borderColor: Color,
    accentColor: Color,
    onOpenInventory: (InventoryLaunchOptions) -> Unit
) {
    val slots = remember(equippedItems) {
        val defaults = listOf("weapon", "armor", "accessory")
        (defaults + equippedItems.keys.map { it.lowercase(Locale.getDefault()) })
            .distinct()
    }
    if (slots.isEmpty()) {
        Text(
            text = "No equipment assigned. Visit the equipment screen to gear up.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEach { slot ->
            val normalized = slot.lowercase(Locale.getDefault())
            val equippedName = equippedItems[normalized] ?: "Unequipped"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.35f)), RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onOpenInventory(
                            InventoryLaunchOptions(
                                initialTab = InventoryTab.EQUIPMENT,
                                focusSlot = normalized
                            )
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slotLabel(slot),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = equippedName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (equippedItems[normalized].isNullOrBlank()) accentColor.copy(alpha = 0.7f) else Color.White
                )
            }
        }
    }
}

private fun slotLabel(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
            }
        }

@Composable
private fun JournalTabContent(
    trackedQuest: QuestSummaryUi?,
    accentColor: Color,
    borderColor: Color,
    onMenuAction: () -> Unit,
    onOpenJournal: () -> Unit
) {
    MenuSectionCard(
        title = trackedQuest?.title ?: "Active Quest",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        QuestPreviewCard(
            quest = trackedQuest,
            accentColor = accentColor,
            onOpenJournal = onOpenJournal,
            onMenuAction = onMenuAction
        )
    }
}

@Composable
private fun MapTabContent(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
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
    onToggleVignette: (Boolean) -> Unit
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
            onToggleVignette = onToggleVignette
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
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Image(
                painter = portraitPainter,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            )
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
                        text = member.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "Lv ${member.level}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemedMenuButton(
                        label = "Details",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onShowDetails(member.id) }
                    )
                    ThemedMenuButton(
                        label = "Skill Tree",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onShowSkillTree(member.id) }
                    )
                }
            }
        }
        member.hpLabel?.let { label ->
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        member.hpProgress?.let { progress ->
            MiniStatBar(
                label = member.hpLabel ?: "HP",
                progress = progress,
                accentColor = accentColor
            )
        }
        MiniStatBar(
            label = "XP",
            progress = member.xpProgress,
            accentColor = Color(0xFFFFC857)
        )
        member.xpLabel.takeIf { !it.isNullOrBlank() }?.let { xpText ->
            Text(
                text = xpText,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MiniStatBar(
    label: String,
    progress: Float,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall
        )
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
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
                        details.focusLabel?.let {
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
                .widthIn(max = 540.dp),
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
                    .padding(horizontal = 32.dp, vertical = 28.dp),
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
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.4f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Continue")
                }
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
    accentColor: Color,
    onOpenJournal: () -> Unit,
    onMenuAction: () -> Unit
) {
    if (quest == null) {
        Text(
            text = "No quest tracked. Tap below to select one.",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        ThemedMenuButton(
            label = "Open Journal",
            accentColor = accentColor,
            onClick = {
                onMenuAction()
                onOpenJournal()
            }
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        ThemedMenuButton(
            label = "View Journal",
            accentColor = accentColor,
            onClick = {
                onMenuAction()
                onOpenJournal()
            }
        )
    }
}

@Composable
private fun SettingsPanel(
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onToggleVignette: (Boolean) -> Unit
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
    }
}

@Composable
private fun MapPreviewPanel(
    minimap: MinimapUiState?,
    fullMap: FullMapUiState?,
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
                        onLegend = onMapLegend,
                        modifier = Modifier.size(140.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemedMenuButton(
                label = "Map Legend",
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    onMenuAction()
                    onMapLegend()
                }
            )
            ThemedMenuButton(
                label = "Open Full Map",
                accentColor = accentColor,
                enabled = fullMapAvailable,
                modifier = Modifier.weight(1f),
                onClick = {
                    onMenuAction()
                    onOpenFullMap()
                }
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
    val highlightColor = if (isDark) Color(0xFF7BE8FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
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
                        Triple(clr, FontWeight.SemiBold, if (segment.locked) TextDecoration.None else TextDecoration.Underline)
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
    modifier: Modifier = Modifier
) {
    val painter = rememberAssetPainter("images/ui/menu_button.png")
    val description = if (isOpen) "Close menu" else "Open menu"
    Image(
        painter = painter,
        contentDescription = description,
        modifier = modifier
            .size(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer { alpha = if (isOpen) 0.75f else 1f }
            .clickable(onClick = onToggle)
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
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                Surface(
                    modifier = Modifier
                        .widthIn(min = 76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onAction(action.roomAction) },
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
    borderColor: Color,
    backgroundColor: Color,
    fallbackActions: List<RoomAction>,
    fallbackNpcs: List<String>,
    actionHints: Map<String, ActionHintUi>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (currentRoom == null && description.isNullOrBlank()) return
    Surface(
        modifier = modifier,
        color = if (isDark) Color.Black.copy(alpha = 0.74f) else backgroundColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.4.dp, borderColor.copy(alpha = 0.9f))
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
                textColor = textColor,
                onAction = onAction,
                onNpcClick = onNpcClick,
                onEnemyClick = onEnemyClick,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
            if (fallbackActions.isNotEmpty()) {
                Text(
                    text = "Interactions",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    fallbackActions.forEach { action ->
                        val key = action.actionKey()
                        val locked = actionHints[key]?.locked == true
                        RoomActionChip(
                            label = action.name.ifBlank { "Interact" },
                            accentColor = accentColor,
                            locked = locked,
                            onClick = { onAction(action) }
                        )
                    }
                }
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
            }
            val enemyFlavor = currentRoom?.enemyFlavor.orEmpty()
            val enemyLines = currentRoom?.enemies.orEmpty().mapNotNull { id ->
                enemyFlavor[id]?.let { id to it }
            }
            if (enemyLines.isNotEmpty()) {
                EnemyFlavorBlock(
                    entries = enemyLines,
                    accentColor = accentColor,
                    onEnemyClick = onEnemyClick
                )
            }
            val itemFlavor = currentRoom?.itemFlavor.orEmpty()
            val itemLines = currentRoom?.items.orEmpty().mapNotNull { itemFlavor[it] }
            if (itemLines.isNotEmpty()) {
                FlavorBlock(title = "Discoveries", lines = itemLines)
            }
        }
    }
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
private fun GroundItemsPanel(
    items: Map<String, Int>,
    onCollect: (String) -> Unit,
    onCollectAll: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Surface(
        modifier = modifier,
        color = backgroundColor,
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
                ConstraintLayout(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val (labelRef, buttonRef) = createRefs()
                    Text(
                        text = "$itemId ×$quantity",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        modifier = Modifier.constrainAs(labelRef) {
                            start.linkTo(parent.start)
                            end.linkTo(buttonRef.start, margin = 12.dp)
                            width = Dimension.fillToConstraints
                        }
                    )
                    Button(
                        onClick = { onCollect(itemId) },
                        modifier = Modifier.constrainAs(buttonRef) {
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                    ) {
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
                FlavorBlock(title = "Discoveries", lines = itemLines)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                }
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
    val (title, accent, body) = when (prompt.type) {
        QuestLogEntryType.NEW_QUEST -> Triple(
            "New Quest",
            Color(0xFF64B5F6),
            prompt.questTitle ?: prompt.message.removePrefix("New Quest:").trim().ifEmpty { prompt.message }
        )
        QuestLogEntryType.QUEST_COMPLETED -> Triple(
            "Quest Complete",
            Color(0xFFFFC857),
            prompt.questTitle ?: prompt.message.removePrefix("Quest Completed:").trim().ifEmpty { prompt.message }
        )
        QuestLogEntryType.UPDATE -> Triple(
            "Quest Update",
            Color(0xFFFFC857),
            prompt.message
        )
    }
    PromptBanner(
        title = title,
        message = body,
        accentColor = accent,
        actionLabel = "OK",
        onAction = onDismiss,
        modifier = modifier
    )
}
