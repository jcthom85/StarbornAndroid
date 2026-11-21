package com.example.starborn.feature.exploration.ui

import android.content.ContextWrapper
import android.view.KeyEvent as AndroidKeyEvent
import com.example.starborn.MainActivity
import com.example.starborn.ui.components.maestroTag
import com.example.starborn.shared.ui.typewriterText
import com.example.starborn.ui.cinematic.rememberCinematicAdvance

import com.example.starborn.ui.map.*
import com.example.starborn.ui.vfx.AndroidRoomEnvironmentalEffects
import com.example.starborn.ui.vfx.rememberEnvironmentalSettings

import com.example.starborn.feature.exploration.ui.components.*
import com.example.starborn.feature.exploration.ui.tabs.*
import com.example.starborn.feature.exploration.ui.hud.*
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.enemy.EnemyPresentationTier
import com.example.starborn.feature.enemy.enemyPresentationTier
import com.example.starborn.feature.enemy.explorationEnemySpriteScale
import com.example.starborn.domain.movement.EnemyAggressionState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.lerp as lerpColor

import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Book
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Phishing
import androidx.compose.material.icons.rounded.SportsEsports
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.starborn.feature.exploration.ui.tabs.TinkeringTutorialOverlay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.starborn.feature.exploration.viewmodel.MapNodeExitUi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlin.math.roundToInt
import com.example.starborn.R
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.cinematic.CinematicBackdrop
import com.example.starborn.domain.cinematic.CinematicCameraMotion
import com.example.starborn.domain.cinematic.CinematicCaptionStyle
import com.example.starborn.domain.cinematic.CinematicTransition
import com.example.starborn.domain.cinematic.CinematicPresentation
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.data.local.Theme
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.isRoomServiceAction
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.isFishingSpot
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.TravelAction
import com.example.starborn.domain.model.TuningPuzzleAction
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.serviceTag
import com.example.starborn.domain.theme.defaultWeatherForEnvironment
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.domain.tutorial.formattedTitle
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
import com.example.starborn.feature.exploration.viewmodel.QuestStageDetailUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import com.example.starborn.feature.exploration.viewmodel.TogglePromptUi
import com.example.starborn.feature.exploration.viewmodel.TuningPuzzleUi
import com.example.starborn.feature.exploration.viewmodel.VisualEnemyParty
import com.example.starborn.feature.exploration.viewmodel.SettingsUiState
import com.example.starborn.feature.exploration.viewmodel.SkillTreeBranchUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeNodeUi
import com.example.starborn.feature.exploration.viewmodel.SkillTreeOverlayUi
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import android.text.format.DateUtils
import java.util.LinkedHashSet
import kotlin.math.abs
import kotlin.math.max
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
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.overlay.ProgressToastOverlay
import com.example.starborn.ui.overlay.QuestBannerOverlay
import com.example.starborn.ui.overlay.QuestDetailOverlay
import com.example.starborn.ui.theme.MinimapTextStyle
import com.example.starborn.ui.theme.themeColor
import kotlin.math.min
import com.example.starborn.feature.exploration.ui.components.SkillTreeOverlay
import com.example.starborn.feature.exploration.ui.components.GearSelectionDialog
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import com.example.starborn.feature.exploration.ui.components.rememberItemIconPainter
import com.example.starborn.feature.exploration.ui.components.PartyMemberDetailsDialog
import com.example.starborn.feature.exploration.ui.components.EventAnnouncementOverlay
import com.example.starborn.feature.exploration.ui.components.TogglePromptDialog
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.exploration.ui.tabs.TinkerTabContent
import com.example.starborn.feature.exploration.ui.tabs.InventoryTabContent
import com.example.starborn.feature.exploration.ui.tabs.JournalTabContent
import com.example.starborn.feature.exploration.ui.tabs.MapTabContent
import com.example.starborn.feature.exploration.ui.tabs.StatsTabContent
import com.example.starborn.feature.exploration.ui.tabs.SettingsTabContent
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalRow
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalSectionCard
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalToggle
import com.example.starborn.feature.exploration.ui.tabs.QuestJournalPage
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import com.example.starborn.feature.exploration.ui.menu.ModernFieldMenu
import com.example.starborn.feature.exploration.ui.menu.ModernPartyPage
import com.example.starborn.feature.exploration.ui.tabs.InventoryEquipmentPreview
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.MenuDetailKind

@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    audioCuePlayer: AudioCuePlayer,
    uiEventBus: UiEventBus,
    questPresentations: com.example.starborn.ui.events.QuestPresentationState,
    modifier: Modifier = Modifier,
    craftingViewModel: CraftingViewModel? = null,
    onEnemySelected: (List<String>) -> Unit = {},
    onOpenTinkering: (String?) -> Unit = {},
    onOpenCooking: (String?) -> Unit = {},
    onOpenFieldKit: () -> Unit = {},
    onOpenFishing: (String?) -> Unit = {},
    onOpenArcade: (String) -> Unit = {},
    onOpenShop: (String) -> Unit = {},
    onReturnToHub: () -> Unit = {},
    onReturnToTitle: () -> Unit = {},
    onPlayAudio: (String) -> Unit = {},
    fxEvents: Flow<String>? = null,
    onDemoRootBack: (() -> Unit)? = null,
    hubAreaEntry: com.example.starborn.feature.exploration.viewmodel.AreaEntryUi? = null,
    onHubAreaEntryFinished: () -> Unit = {},
    isTestSession: Boolean = false,
    combatTransitionVisible: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bossBottomAllowance = if ("administrator_boss" in
        uiState.visualEnemyParties.singleOrNull()?.enemies.orEmpty()) 28.dp else 0.dp
    val areaEntry = uiState.areaEntry ?: hubAreaEntry
    var finishedAreaEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val areaEntryActive = areaEntry != null && areaEntry.id != finishedAreaEntryId
    val environmentSettings=rememberEnvironmentalSettings()
    val environmentMemory=remember { com.example.starborn.domain.environment.EnvironmentalSceneMemory() }
    val environmentSession by viewModel.environmentalSession.collectAsStateWithLifecycle()
    val environmentRooms=remember(viewModel) { viewModel.environmentalAssets.loadRooms().associateBy { it.id } }
    var pendingModMove by remember { mutableStateOf<com.example.starborn.domain.inventory.ModTransferRequest?>(null) }
    pendingModMove?.let { request ->
        com.example.starborn.feature.crafting.ui.ModTransferDialog(request, uiState.inventoryPreview.firstOrNull { it.id == request.itemId }?.name ?: request.itemId.replace('_', ' '), { pendingModMove = null }) { source ->
            pendingModMove = null
            viewModel.requestEquipInventoryMod(request.target.slotId, request.itemId, request.target.characterId, source)
        }
    }
    var pendingInventoryItem by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var showInventoryTargetDialog by remember { mutableStateOf(false) }
    var pendingMealReplacement by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var replacingMeal by remember { mutableStateOf(false) }
    val fxBursts = remember { mutableStateListOf<UiFxBurst>() }
    var saveLoadMode by remember { mutableStateOf<String?>(null) } // "save" or "load"
    var saveDialogBusy by remember { mutableStateOf(false) }
    var walkthroughVisible by rememberSaveable { mutableStateOf(false) }
    var slotSummaries by remember { mutableStateOf<List<SaveSlotSummary>>(emptyList()) }
    var debugWeatherOverride by remember { mutableStateOf<String?>(null) }
    var environmentalPreview by remember { mutableStateOf(false) }
    var importantQuestPopupVisible by remember { mutableStateOf(false) }
    var restRecoveryNotice by remember { mutableStateOf<RestRecoveryNotice?>(null) }
    val weatherCycles = remember {
        listOf(null, "dust", "rain", "storm", "snow", "cave_drip", "starfall", "steam", "fog", "gas", "resonance", "sparks")
    }
    var approachingCombat by remember { mutableStateOf(false) }
    var approachingAmbush by remember { mutableStateOf(false) }
    val hostileApproachScale by animateFloatAsState(
        targetValue = when {
            !approachingCombat || uiState.settings.disableScreenshake -> 1f
            approachingAmbush -> 1.24f
            else -> 1.06f
        },
        animationSpec = tween(if (approachingAmbush) 280 else 300, easing = FastOutSlowInEasing),
        label = "hostileApproach"
    )
    val ambushFlashAlpha by animateFloatAsState(
        targetValue = if (approachingCombat && approachingAmbush) 0.42f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "ambushFlash"
    )
    val gateReveal = com.example.starborn.ui.exploration.rememberTempleGateReveal(
        uiState.currentRoom?.id, uiState.roomState["temple_open"] == true, uiState.settings.disableScreenshake)
    val gateRevealActive = gateReveal.active
    val scriptedTravelActive = uiState.roomTransition?.scriptedStyle != null
    val coroutineScope = rememberCoroutineScope()
    val blockingOverlayActive = scriptedTravelActive || uiState.gladeBallAnimating || gateRevealActive || areaEntryActive || environmentalPreview || restRecoveryNotice != null ||
        walkthroughVisible || uiState.isMenuOverlayVisible ||
            uiState.blockedPrompt != null || uiState.roomActionsVisible || uiState.roomObjectOffer != null || uiState.togglePrompt != null ||
            uiState.tuningPuzzle != null ||
            uiState.activeDialogue != null ||
            uiState.shopGreeting != null ||
            uiState.narrationPrompt != null ||
            uiState.cinematic != null ||
            uiState.skillTreeOverlay != null ||
            uiState.partyMemberDetails != null ||
            uiState.prompt != null ||
            uiState.isMilestoneGalleryVisible || uiState.isCatchLogVisible || uiState.astraFacility != null || uiState.isAstraRecreationVisible ||
            uiState.eventAnnouncement != null ||
            uiState.levelUpPrompt != null ||
            uiState.isQuestLogVisible ||
            uiState.isMapLegendVisible ||
            uiState.tutorialState.current != null
    val questDetailBlockingOverlayActive =
        scriptedTravelActive || gateRevealActive || uiState.gladeBallAnimating || restRecoveryNotice != null || walkthroughVisible || uiState.isMenuOverlayVisible ||
            uiState.blockedPrompt != null || uiState.roomActionsVisible || uiState.roomObjectOffer != null || uiState.togglePrompt != null ||
            uiState.tuningPuzzle != null ||
            uiState.activeDialogue != null ||
            uiState.shopGreeting != null ||
            uiState.narrationPrompt != null ||
            uiState.cinematic != null ||
            uiState.skillTreeOverlay != null ||
            uiState.partyMemberDetails != null ||
            uiState.isMilestoneGalleryVisible || uiState.isCatchLogVisible || uiState.astraFacility != null || uiState.isAstraRecreationVisible ||
            uiState.eventAnnouncement != null ||
            uiState.levelUpPrompt != null ||
            uiState.isQuestLogVisible ||
            uiState.isMapLegendVisible ||
            uiState.prompt != null ||
            uiState.tutorialState.current != null

    val queuedNotice = explorationNotice(uiState,
        externalModal = scriptedTravelActive || gateRevealActive || areaEntryActive || environmentalPreview || restRecoveryNotice != null || walkthroughVisible ||
            saveLoadMode != null || showInventoryTargetDialog)

    LaunchedEffect(scriptedTravelActive, importantQuestPopupVisible, areaEntryActive, environmentalPreview, restRecoveryNotice, walkthroughVisible,
        saveLoadMode, showInventoryTargetDialog) {
        viewModel.setNoticePresentationBlocked("android-presentation",
            scriptedTravelActive || importantQuestPopupVisible || areaEntryActive || environmentalPreview || restRecoveryNotice != null ||
                walkthroughVisible || saveLoadMode != null || showInventoryTargetDialog)
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.setNoticePresentationBlocked("android-presentation", false) }
    }

    val explorationLifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, explorationLifecycle) {
        explorationLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.setExplorationVisible(true)
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                viewModel.setExplorationVisible(false)
            }
        }
    }

    val eventWorldId by rememberUpdatedState(uiState.currentWorld?.id)
    // Keep collecting across warps so the destination's audio commands reach the player.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExplorationEvent.EnterCombat -> {
                    if (event.isAmbush || eventWorldId == "world_2") {
                        approachingAmbush = event.isAmbush
                        approachingCombat = true
                        viewModel.setExplorationInteractionBlocked(true)
                        if (event.isAmbush) {
                            audioCuePlayer.execute(
                                listOf(
                                    AudioCommand.Play(
                                        type = AudioCueType.UI,
                                        cueId = "sfx_combat_transition_slam",
                                        loop = false,
                                        fadeMs = 0L,
                                        triggerHaptic = true
                                    )
                                )
                            )
                        }
                        delay(if (event.isAmbush) 380L else 350L)
                    }
                    onEnemySelected(event.enemyIds)
                    approachingCombat = false
                    approachingAmbush = false
                }
                is ExplorationEvent.PlayCinematic -> Unit
                is ExplorationEvent.ShowMessage -> Unit
                is ExplorationEvent.ShowToast -> uiEventBus.tryEmit(
                    UiEvent.ShowToast(
                        id = event.id,
                        text = event.message
                    )
                )
                is ExplorationEvent.RewardGranted -> viewModel.showStatusMessage(formatRewardMessage(event.reward))
                is ExplorationEvent.ItemGranted -> Unit
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
                is ExplorationEvent.RestRecovered -> {
                    val message = event.title
                    restRecoveryNotice = RestRecoveryNotice(
                        id = System.nanoTime(),
                        title = message,
                        detail = event.message?.takeIf { it.isNotBlank() }
                            ?: if (event.partySize == 1) {
                                "Nova rests for a while. The colony hum softens, and her breathing steadies."
                            } else {
                                "The party rests for a while. Wounds settle, nerves loosen, and the room feels safe enough to breathe."
                            }
                    )
                }
                is ExplorationEvent.OpenTinkering -> onOpenTinkering(event.sourceId)
                is ExplorationEvent.OpenCooking -> onOpenCooking(event.sourceId)
                is ExplorationEvent.OpenFishing -> onOpenFishing(event.zoneId)
                is ExplorationEvent.OpenArcade -> onOpenArcade(event.cabinetId)
                is ExplorationEvent.OpenShop -> onOpenShop(event.shopId)
                is ExplorationEvent.CombatOutcome -> viewModel.showStatusMessage(event.message)
                is ExplorationEvent.AudioCommands -> audioCuePlayer.execute(event.commands)
                is ExplorationEvent.AudioSettingsChanged -> {
                    audioCuePlayer.setUserMusicGain(event.musicVolume)
                    audioCuePlayer.setUserSfxGain(event.sfxVolume)
                    audioCuePlayer.setUserVoiceGain(event.voiceVolume)
                }
                is ExplorationEvent.TriggerVisualFx -> {
                    val visual = fxVisualInfo(event.fxId)
                    val message = event.message ?: visual.label
                    fxBursts += UiFxBurst(System.nanoTime(), message, visual.color)
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
            pendingMealReplacement = null
            replacingMeal = false
            pendingInventoryItem = null
            showInventoryTargetDialog = false
        }
    }

    LaunchedEffect(craftingViewModel) {
        craftingViewModel?.craftResults?.collect { outcome ->
            viewModel.onTinkeringCrafted(outcome.itemId)
        }
    }

    val menuPartyMembers = uiState.partyStatus.members
    val beginInventoryUse: (InventoryPreviewItemUi, Boolean) -> Unit = { item, replace ->
        val effect = item.effect
        if (effect == null) {
            viewModel.showStatusMessage("${item.name} can't be used right now.")
        } else {
            val targetMode = effect.target?.lowercase(Locale.getDefault()) ?: "any"
            if (targetMode in listOf("party", "all_allies") || menuPartyMembers.isEmpty()) {
                viewModel.useInventoryItem(item.id, null, replaceMeal = replace)
            } else {
                pendingInventoryItem = item
                replacingMeal = replace
                showInventoryTargetDialog = true
            }
        }
    }
    val onUsePreviewItem: (InventoryPreviewItemUi) -> Unit = { item ->
        if (viewModel.isPreparedMeal(item.id) && uiState.activeMeal != null) pendingMealReplacement = item
        else beginInventoryUse(item, false)
    }

    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    val keyboardFocusRequester = remember { FocusRequester() }

    val resolvedBackgroundPath = resolveRoomBackground(
        room = uiState.currentRoom,
        roomState = uiState.roomState,
        completedMilestones = uiState.completedMilestones
    )
    val backgroundPainter = rememberRoomBackgroundPainter(resolvedBackgroundPath)
    com.example.starborn.ui.background.PrefetchRoomBackgrounds(
        remember(uiState.currentRoom?.id, uiState.completedMilestones) {
            uiState.currentRoom?.connections?.values.orEmpty().mapNotNull { id ->
                environmentRooms[id]?.let { next -> resolveRoomBackground(next, emptyMap(), uiState.completedMilestones) }
            }
        }
    )
    val roomTransition = uiState.roomTransition
    val outgoingBackgroundPath = roomTransition?.fromBackgroundImage
    var roomTransitionActive by remember { mutableStateOf(false) }
    val roomTransitionProgress = remember(roomTransition?.id) {
        CoreAnimatable(if (roomTransition == null) 1f else 0f)
    }
    val roomTransitionInProgress = roomTransitionActive || roomTransitionProgress.value < 1f
    val patrolInteractionBlocked = blockingOverlayActive || importantQuestPopupVisible || approachingCombat || roomTransitionInProgress
    LaunchedEffect(viewModel, patrolInteractionBlocked) {
        viewModel.setExplorationInteractionBlocked(patrolInteractionBlocked)
    }

    val outgoingBackgroundPainter = rememberRoomBackgroundPainter(outgoingBackgroundPath)
    val backdropProgress = if (scriptedTravelActive) {
        if (roomTransitionProgress.value < .5f) 0f else 1f
    } else roomTransitionProgress.value

    LaunchedEffect(roomTransition?.id) {
        roomTransition ?: return@LaunchedEffect
        roomTransitionActive = true
        try {
            val style = roomTransition.scriptedStyle
            if (style == null) {
                audioCuePlayer.play("ui_room_move")
                roomTransitionProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
            } else {
                com.example.starborn.ui.exploration.animateScriptedTravel(roomTransitionProgress, style,
                    uiState.settings.disableScreenshake, audioCuePlayer::play) {
                    viewModel.onScriptedRoomRevealed(roomTransition.id)
                }
                viewModel.onScriptedTravelFinished(roomTransition.id)
            }
        } finally {
            roomTransitionActive = false
        }
    }
    val actionAccentColor = themeColor(uiState.theme?.accent, Color(0xFF80E0FF))
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

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val isAnyOverlayOpen = restRecoveryNotice != null || uiState.isMenuOverlayVisible ||
        uiState.isTapeDeckVisible ||
        uiState.isSimulationDeckVisible ||
        uiState.isAstraNavConsoleVisible ||
        uiState.showBurgQuestAstraExitDialog ||
        uiState.isMilestoneGalleryVisible || uiState.isCatchLogVisible || uiState.astraFacility != null || uiState.isAstraRecreationVisible ||
        uiState.isQuestLogVisible ||
        uiState.skillTreeOverlay != null ||
        uiState.blockedPrompt != null || uiState.roomActionsVisible || uiState.roomObjectOffer != null || uiState.togglePrompt != null ||
        uiState.tuningPuzzle != null ||
        uiState.narrationPrompt != null ||
        uiState.prompt != null ||
        saveLoadMode != null ||
        showExitConfirmDialog ||
        showInventoryTargetDialog
    val keyboardNavigationReady = !blockingOverlayActive && !isAnyOverlayOpen &&
        !roomTransitionInProgress && !importantQuestPopupVisible && !uiState.isLoading &&
        !approachingCombat && !combatTransitionVisible
    fun handleMovementKey(event: AndroidKeyEvent): Boolean {
        if (!keyboardNavigationReady || event.action != AndroidKeyEvent.ACTION_DOWN ||
            !event.hasNoModifiers()) return false
        val direction = when (event.keyCode) {
            AndroidKeyEvent.KEYCODE_W, AndroidKeyEvent.KEYCODE_DPAD_UP -> "north"
            AndroidKeyEvent.KEYCODE_S, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> "south"
            AndroidKeyEvent.KEYCODE_A, AndroidKeyEvent.KEYCODE_DPAD_LEFT -> "west"
            AndroidKeyEvent.KEYCODE_D, AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> "east"
            else -> return false
        }
        val target = uiState.availableConnections.keys.firstOrNull { it.equals(direction, ignoreCase = true) }
            ?: return false
        if (event.repeatCount == 0) viewModel.travel(target)
        return true
    }

    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<MainActivity>().firstOrNull()
    }
    val movementKeys by rememberUpdatedState(::handleMovementKey)
    LaunchedEffect(activity, explorationLifecycle) {
        if (activity == null) return@LaunchedEffect
        explorationLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Route room movement even when the Compose exploration root has lost keyboard focus.
            val handler: (AndroidKeyEvent) -> Boolean = { movementKeys(it) }
            activity.explorationMovementKeys = handler
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                if (activity.explorationMovementKeys === handler) activity.explorationMovementKeys = null
            }
        }
    }
    LaunchedEffect(viewModel, keyboardNavigationReady) {
        if (keyboardNavigationReady) {
            // A dialog can dispose its focused node a frame after it closes.
            withFrameNanos { }
            keyboardFocusRequester.requestFocus()
        }
    }

    fun handleExplorationBack(allowTitleExit: Boolean = false) {
        when {
            scriptedTravelActive || areaEntryActive -> Unit
            restRecoveryNotice != null -> restRecoveryNotice = null
            showExitConfirmDialog -> showExitConfirmDialog = false
            environmentalPreview -> environmentalPreview = false
            uiState.showBurgQuestAstraExitDialog -> viewModel.dismissBurgQuestAstraExitDialog()
            saveLoadMode != null -> { if (!saveDialogBusy) saveLoadMode = null }
            showInventoryTargetDialog -> {
                showInventoryTargetDialog = false
                pendingInventoryItem = null
            }
            uiState.isTapeDeckVisible -> viewModel.dismissTapeDeck()
            uiState.isSimulationDeckVisible -> viewModel.dismissSimulationDeck()
            uiState.isAstraNavConsoleVisible -> viewModel.dismissAstraNavConsole()
            uiState.isAstraRecreationVisible -> viewModel.closeAstraRecreation()
            uiState.astraFacility != null -> viewModel.closeAstraFacility()
            uiState.isCatchLogVisible -> viewModel.closeCatchLog()
            uiState.isMilestoneGalleryVisible -> viewModel.closeMilestoneGallery()
            uiState.isMapLegendVisible -> viewModel.closeMapLegend()
            uiState.isQuestLogVisible -> viewModel.closeQuestLog()
            uiState.questDetail != null -> viewModel.closeQuestDetails()
            uiState.partyMemberDetails != null -> viewModel.closePartyMemberDetails()
            uiState.skillTreeOverlay != null -> viewModel.closeSkillTreeOverlay()
            queuedNotice == ExplorationNotice.ANNOUNCEMENT -> viewModel.dismissEventAnnouncement()
            uiState.levelUpPrompt != null && canPresentLevelUp(uiState, restRecoveryNotice != null) -> viewModel.dismissLevelUpPrompt()
            uiState.shopGreeting != null -> viewModel.dismissShopGreeting()
            uiState.blockedPrompt != null -> viewModel.dismissBlockedPrompt()
            uiState.roomObjectOffer != null -> viewModel.dismissRoomObject()
            uiState.roomActionsVisible -> viewModel.dismissRoomActions()
            uiState.togglePrompt != null -> viewModel.dismissTogglePrompt()
            uiState.tuningPuzzle != null -> viewModel.dismissTuningPuzzle()
            queuedNotice == ExplorationNotice.NARRATION -> { if (uiState.narrationPrompt?.tapToDismiss == true) viewModel.dismissNarration() }
            queuedNotice == ExplorationNotice.PROMPT -> viewModel.dismissPrompt(uiState.prompt?.id)
            uiState.isMenuOverlayVisible -> viewModel.closeMenuOverlay()
            else -> if (!blockingOverlayActive && !roomTransitionInProgress && !importantQuestPopupVisible && !uiState.isLoading) {
                when {
                    onDemoRootBack != null -> onDemoRootBack()
                    uiState.canReturnToHub -> viewModel.requestReturnToHub()
                    allowTitleExit -> showExitConfirmDialog = true
                }
            }
        }
    }

    BackHandler(enabled = true) { handleExplorationBack() }

    val baseModifier = Modifier.fillMaxSize()
    val swipeGestureModifier = Modifier
        .pointerInput(uiState.availableConnections, uiState.blockedDirections, blockingOverlayActive, roomTransitionInProgress, queuedNotice, uiState.prompt?.id) {
            if (blockingOverlayActive || roomTransitionInProgress) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                    },
                    onTap = {
                        if (queuedNotice == ExplorationNotice.NARRATION) {
                            if (uiState.narrationPrompt?.tapToDismiss == true) viewModel.dismissNarration()
                        } else if (queuedNotice == ExplorationNotice.PROMPT) {
                            viewModel.dismissPrompt(uiState.prompt?.id)
                        }
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
                            if (targetDir != null) {
                                viewModel.travel(targetDir)
                            }
                        }
                    }
                )
            }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(keyboardFocusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (scriptedTravelActive || areaEntryActive) return@onPreviewKeyEvent true
                handleMovementKey(keyEvent.nativeKeyEvent)
            }
            .onKeyEvent { keyEvent ->
                if (uiState.activeDialogue != null || uiState.cinematic != null || importantQuestPopupVisible) return@onKeyEvent false
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.ButtonB, Key.Escape -> {
                            handleExplorationBack(allowTitleExit = true)
                            return@onKeyEvent true
                        }
                        Key.ButtonA, Key.Enter, Key.Spacebar -> {
                            when {
                                restRecoveryNotice != null -> { restRecoveryNotice = null; return@onKeyEvent true }
                                uiState.levelUpPrompt != null && canPresentLevelUp(uiState, restRecoveryNotice != null) -> { viewModel.dismissLevelUpPrompt(); return@onKeyEvent true }
                                queuedNotice == ExplorationNotice.ANNOUNCEMENT -> { viewModel.dismissEventAnnouncement(); return@onKeyEvent true }
                                uiState.blockedPrompt != null -> { viewModel.dismissBlockedPrompt(); return@onKeyEvent true }
                                queuedNotice == ExplorationNotice.NARRATION -> {
                                    if (uiState.narrationPrompt?.tapToDismiss == true) viewModel.dismissNarration()
                                    return@onKeyEvent true
                                }
                                queuedNotice == ExplorationNotice.PROMPT -> {
                                    viewModel.dismissPrompt(uiState.prompt?.id)
                                    return@onKeyEvent true
                                }
                                !blockingOverlayActive && !roomTransitionInProgress && !uiState.isMenuOverlayVisible -> {
                                    val action = uiState.actions.firstOrNull()
                                    if (action != null) {
                                        viewModel.onActionSelected(action)
                                        return@onKeyEvent true
                                    }
                                }
                            }
                        }
                        Key.ButtonX -> {
                            if (!blockingOverlayActive && !roomTransitionInProgress) {
                                onOpenFieldKit()
                                return@onKeyEvent true
                            }
                        }
                        Key.ButtonY -> {
                            if (!blockingOverlayActive && !roomTransitionInProgress && !uiState.isMenuOverlayVisible) {
                                if (uiState.isAstraNavConsoleVisible) {
                                    viewModel.dismissAstraNavConsole()
                                    return@onKeyEvent true
                                }
                            }
                        }
                        else -> {}
                    }

                }
                false
            }
            .focusable()
    ) {
        Box(modifier = baseModifier) {
            val transitionDirection = roomTransition?.direction
            val enterX = when (transitionDirection) {
                "east" -> -1f
                "west" -> 1f
                else -> 0f
            }
            val enterY = when (transitionDirection) {
                "north" -> 1f
                "south" -> -1f
                else -> 0f
            }
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val remaining = 1f - backdropProgress
                        translationX = enterX * size.width * 0.12f * remaining
                        translationY = enterY * size.height * 0.12f * remaining
                        scaleX = 0.99f + (0.01f * backdropProgress)
                        scaleY = scaleX
                    },
                contentScale = ContentScale.Crop
            )
            if (outgoingBackgroundPath != null) {
                val exitX = when (transitionDirection) {
                    "east" -> 1f
                    "west" -> -1f
                    else -> 0f
                }
                val exitY = when (transitionDirection) {
                    "north" -> -1f
                    "south" -> 1f
                    else -> 0f
                }
                Image(
                    painter = outgoingBackgroundPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(
                            (6f * (backdropProgress / 0.55f).coerceIn(0f, 1f)).dp
                        )
                        .graphicsLayer {
                            val exitProgress = backdropProgress
                            translationX = exitX * size.width * 0.78f * exitProgress
                            translationY = exitY * size.height * 0.78f * exitProgress
                            alpha = (1f - (exitProgress / 0.82f)).coerceIn(0f, 1f)
                            scaleX = 1f - (0.025f * exitProgress)
                            scaleY = scaleX
                        }
                        .zIndex(0.5f),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(swipeGestureModifier)
            )

        val currentRoom = uiState.currentRoom
        val isWeatherLab = currentRoom?.id == "weather_lab"
        LaunchedEffect(currentRoom?.id) {
            if (currentRoom?.id != "weather_lab") {
                debugWeatherOverride = null
            }
        }
        fun cycleDebugWeather(offset: Int) {
            val currentIndex = weatherCycles.indexOf(debugWeatherOverride).takeIf { it >= 0 } ?: 0
            val nextIndex = Math.floorMod(currentIndex + offset, weatherCycles.size)
            debugWeatherOverride = weatherCycles[nextIndex]
        }
        val isRoomDark = remember(
            currentRoom,
            uiState.roomState,
            uiState.mineGeneratorOnline,
            uiState.darkCapableRooms,
            uiState.generatorLitRooms
        ) {
            val darkState = uiState.roomState["dark"]
            val lightState = uiState.roomState["light_on"]
            var resolved = when {
                darkState == true -> true
                darkState == false -> false
                lightState == false -> true
                lightState == true -> false
                else -> currentRoom?.dark == true
            }
            val isDarkCapable = currentRoom?.id?.let { uiState.darkCapableRooms.contains(it) } == true
            // Area power lights only the rooms the generator is wired to (its node), never every
            // room sharing the generator's art environment.
            val isGeneratorLit = currentRoom?.id?.let { uiState.generatorLitRooms.contains(it) } == true
            if (!isDarkCapable) {
                resolved = false
            } else if (isGeneratorLit && uiState.mineGeneratorOnline) {
                resolved = false
            }
            resolved
        }
        val baseRoomDescription = remember(currentRoom, uiState.roomState, uiState.completedMilestones, uiState.completedEvents, isRoomDark, uiState.isBurgQuestSession) {
            if (uiState.isBurgQuestSession && currentRoom?.id == "astra_common_room") {
                com.example.starborn.feature.exploration.presentation.BurgQuestPresentation.commonRoomDescription
            } else {
                resolveRoomDescription(
                    room = currentRoom,
                    roomState = uiState.roomState,
                    completedMilestones = uiState.completedMilestones,
                    isRoomDark = isRoomDark,
                    completedEvents = uiState.completedEvents
                )
            }
        }

        val activeWeatherId = debugWeatherOverride ?: currentRoom?.weather ?: defaultWeatherForEnvironment(currentRoom?.env)

        val darknessAlpha by animateFloatAsState(
            targetValue = if (isRoomDark) 1f else 0f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
        if (darknessAlpha > 0.01f) {
            DarkRoomAtmosphereOverlay(
                darkness = darknessAlpha,
                modifier = Modifier.fillMaxSize()
            )
        }
        val effectProgress=backdropProgress.coerceIn(0f,1f)
        val effectsMoving=outgoingBackgroundPath!=null && effectProgress<1f
        val effectEnterX=when(roomTransition?.direction) { "east"->-1f;"west"->1f;else->0f }
        val effectEnterY=when(roomTransition?.direction) { "north"->1f;"south"->-1f;else->0f }
        if(effectsMoving) {
            val outgoingRoom=environmentRooms[roomTransition?.fromRoomId]
            val outgoingState=outgoingRoom?.state.orEmpty().mapNotNull { (k,v)->(v as? Boolean)?.let { k to it } }.toMap()+environmentSession.roomStates[outgoingRoom?.id].orEmpty()
            AndroidRoomEnvironmentalEffects(outgoingRoom,outgoingBackgroundPainter.intrinsicSize,outgoingState,
                uiState.completedMilestones,environmentSettings,viewModel.environmentalAssets,
                dark=outgoingState["dark"] ?: outgoingRoom?.dark ?: false,paused=blockingOverlayActive || uiState.isMenuOverlayVisible,
                translationX=-effectEnterX*.78f*effectProgress,translationY=-effectEnterY*.78f*effectProgress,
                scale=1-.025f*effectProgress,opacity=(1-effectProgress/.82f).coerceIn(0f,1f),transition=true,sceneMemory=environmentMemory)
        }
        AndroidRoomEnvironmentalEffects(
            room=if(debugWeatherOverride!=null) currentRoom?.copy(weather=activeWeatherId,environmentalEffects=null) else currentRoom,
            artworkSize=backgroundPainter.intrinsicSize,roomState=uiState.roomState,
            milestones=uiState.completedMilestones,settings=environmentSettings,assets=viewModel.environmentalAssets,
            dark=isRoomDark,paused=blockingOverlayActive || uiState.isMenuOverlayVisible,
            translationX=effectEnterX*.12f*(1-effectProgress),translationY=effectEnterY*.12f*(1-effectProgress),
            scale=.99f+.01f*effectProgress,opacity=if(effectsMoving) effectProgress else 1f,
            transition=effectsMoving,sceneMemory=environmentMemory)
        val vignetteIntensity = 0.48f * darknessAlpha
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
        val travelActions = uiState.actions.mapNotNull { action ->
            val label = when (action) {
                is TravelAction -> action.name
                is GenericAction -> action.travelLabel
                else -> null
            }
            label?.let { action to it }
        }
        val inlinePlan = remember(baseRoomDescription, uiState.actions, actionHints, currentRoom, uiState.roomState) {
            buildInlineActionPlan(
                description = baseRoomDescription,
                actions = uiState.actions.filterNot { it is TravelAction },
                hints = actionHints,
                room = currentRoom,
                inspectionState = uiState.roomState
            )
        }
        val descriptionForPanel = baseRoomDescription
        val visibleNpcs = if (isRoomDark) emptyList() else uiState.npcs
        val visibleGroundItems = if (isRoomDark) emptyMap() else uiState.groundItems
        val hasLargeEnemy = !isRoomDark && uiState.visualEnemyParties.any { party ->
            party.enemies.any { enemyPresentationTier(uiState.enemyTiers[it]) != EnemyPresentationTier.STANDARD }
        }
        val serviceQuickActions = remember(uiState.actions, isRoomDark) {
            if (isRoomDark) {
                emptyList()
            } else {
                uiState.actions.filter { it.isRoomServiceAction() || it.isFishingSpot() }.map { action ->
                    when (action) {
                        is ShopAction -> QuickMenuAction(
                            label = action.name.replaceFirstChar { it.titlecase() },
                            detail = "Shop",
                            roomAction = action,
                            imageVector = Icons.Filled.ShoppingCart
                        )
                        is RestStopAction -> QuickMenuAction(
                            label = restStopServiceLabel(action),
                            detail = null,
                            roomAction = action,
                            imageVector = if (action.cookSource != null) Icons.Rounded.Restaurant else Icons.Rounded.Hotel
                        )
                        is TinkeringAction -> QuickMenuAction(
                            label = action.name.replaceFirstChar { it.titlecase() },
                            detail = "Workbench",
                            roomAction = action,
                            imageVector = Icons.Filled.Build
                        )
                        else -> QuickMenuAction(
                            label = if (action.isFishingSpot()) "Fish" else action.name.replaceFirstChar { it.titlecase() },
                            roomAction = action,
                            imageVector = Icons.Rounded.Phishing.takeIf { action.isFishingSpot() }
                        )
                    }
                }
            }
        }
        val hasRoomEntities = visibleNpcs.isNotEmpty() ||
            visibleGroundItems.isNotEmpty() ||
            serviceQuickActions.isNotEmpty() ||
            uiState.canReturnToHub || (!isRoomDark && travelActions.isNotEmpty())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = ExplorationControlSpacing.sceneBottomClearance - bossBottomAllowance)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .clipToBounds()
        ) {
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
            if (uiState.currentRoom != null && !uiState.isLoading) {
            Column(
                modifier = Modifier
                    .maestroTag("room-${uiState.currentRoom?.id.orEmpty()}")
                    // Room controls take priority over enemy art and its hit targets.
                    .zIndex(2f)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val currentCell = uiState.minimap?.cells?.firstOrNull { it.isCurrent }
                val sectorLabel = remember(uiState.currentHub?.title, uiState.currentWorld?.title) {
                    uiState.currentHub?.title?.takeIf { it.isNotBlank() }
                        ?: uiState.currentWorld?.title?.takeIf { it.isNotBlank() }
                        ?: "SECTOR"
                }
                RoomHeaderPanel(
                    roomId = uiState.currentRoom?.id.orEmpty(),
                    worldId = uiState.currentWorld?.id.orEmpty(),
                    sectorLabel = sectorLabel,
                    isGateway = currentCell?.hubExit != null || currentCell?.nodeExits?.isNotEmpty() == true,
                    hasHostiles = uiState.enemies.isNotEmpty() && !isRoomDark,
                    roomTitle = uiState.currentRoom?.title ?: "",
                    isDark = isRoomDark,
                    obscureTitle = isRoomDark && uiState.currentRoom?.revealTitleWhenDark != true,
                    reduceMotion = uiState.settings.disableFlashes || uiState.settings.disableScreenshake,
                    titleColor = titleColor,
                    warmTitleColor = warmTitleColor,
                    minimap = uiState.minimap,
                    minimapSize = minimapSize,
                    onTitleClick = {
                        if (isWeatherLab) {
                            environmentalPreview=true
                        }
                    },
                    onMapClick = {
                        if (!isRoomDark) {
                            viewModel.selectMenuTab(MenuTab.MAP)
                            viewModel.openMenuOverlay(MenuTab.MAP)
                        }
                    }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomDescriptionPanel(
                        currentRoom = currentRoom,
                        animateHighlights = !uiState.settings.disableFlashes && !uiState.settings.disableScreenshake,
                        description = descriptionForPanel,
                        plan = inlinePlan,
                        isDark = isRoomDark,
                        onAction = { action -> viewModel.inspectRoomAction(action) },
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
                            npcTradeKeys = uiState.npcTradeKeys,
                            groundItems = visibleGroundItems,
                            serviceActions = serviceQuickActions,
                            travelActions = if (isRoomDark) emptyList() else travelActions,
                            canReturnToHub = uiState.canReturnToHub,
                            gatewayEnabled = !blockingOverlayActive && !importantQuestPopupVisible && !roomTransitionInProgress,
                            sectorTitle = if (uiState.currentHub?.id == "hub_astra") "Cargo ramp" else uiState.currentHub?.title,
                            isDisembarking = uiState.currentHub?.id == "hub_astra",
                            onReturnToHub = { viewModel.requestReturnToHub() },
                            itemDisplayName = { itemId -> viewModel.itemDisplayName(itemId) },
                            itemDetailLabel = { itemId -> viewModel.roomItemDetailLabel(itemId) },
                            itemIsEquipment = { itemId -> viewModel.roomItemIsEquipment(itemId) },
                            accentColor = actionAccentColor,
                            borderColor = panelBorderColor,
                            isDark = isRoomDark,
                            hasLargeEnemy = hasLargeEnemy,
                            onNpcClick = { name -> viewModel.onNpcInteraction(name) },
                            onCollectItem = { itemId -> viewModel.collectGroundItem(itemId) },
                            onCollectAll = { viewModel.collectAllGroundItems() },
                            onAction = { action -> viewModel.onActionSelected(action) },
                            onTravel = viewModel::inspectRoomAction
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
                    enemyAggression = if (!patrolInteractionBlocked) uiState.enemyAggression else null,
                    accentColor = actionAccentColor,
                    isDark = isRoomDark,
                    roomId = uiState.currentRoom?.id,
                    onPartyClick = { enemyId -> if (!approachingCombat) viewModel.engageEnemy(enemyId) },
                    modifier = Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            scaleX = hostileApproachScale
                            scaleY = hostileApproachScale
                            if (approachingCombat && approachingAmbush && !uiState.settings.disableScreenshake) {
                                translationY = 18f * density
                            }
                        }
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(min = 250.dp, max = 410.dp)
                        .padding(horizontal = 4.dp)
                )
            }

            if (ambushFlashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFDC2626).copy(alpha = ambushFlashAlpha)
                                )
                            )
                        )
                )
            }

            if (!patrolInteractionBlocked) {
                val aggression = uiState.enemyAggression
                if (aggression != null) {
                    val aggroSecond = ((aggression.remainingMs + 999L) / 1_000L).coerceAtLeast(0L)
                    LaunchedEffect(aggression.partyId, aggroSecond) {
                        if (aggroSecond in 1L..3L) {
                            audioCuePlayer.execute(
                                listOf(
                                    AudioCommand.Play(
                                        type = AudioCueType.UI,
                                        cueId = if (aggroSecond == 1L) "sfx_ui_error" else "ui_click",
                                        loop = false,
                                        fadeMs = 0L,
                                        triggerHaptic = aggroSecond <= 2L
                                    )
                                )
                            )
                        }
                    }
                    com.example.starborn.ui.exploration.PatrolWarning(
                        aggression,
                        Modifier
                            .zIndex(4f)
                            .align(Alignment.TopCenter)
                            .padding(top = 84.dp, start = 16.dp, end = 16.dp)
                    )
                } else {
                    uiState.enemyMovementNotice?.let { notice ->
                        Surface(
                            Modifier
                                .zIndex(4f)
                                .align(Alignment.TopCenter)
                                .padding(top = 84.dp, start = 16.dp, end = 16.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xE2101A23),
                            border = BorderStroke(1.dp, Color(0x66FFD8B0))
                        ) {
                            Text(notice, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFFFFD8B0), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (approachingCombat) {
                Surface(
                    modifier = Modifier
                        .zIndex(5f)
                        .align(Alignment.TopCenter)
                        .padding(top = 92.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (approachingAmbush) Color(0xF2330D12) else Color(0xEE24191D),
                    border = BorderStroke(
                        width = if (approachingAmbush) 1.5.dp else 1.dp,
                        color = if (approachingAmbush) Color(0xFFEF4444) else Color(0xFFE5A36B)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = if (approachingAmbush) "⚠ AMBUSH!" else "Hostiles closing in",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        color = if (approachingAmbush) Color(0xFFFFE4E6) else Color(0xFFFFD8B0),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = if (approachingAmbush) 1.2.sp else 0.3.sp
                        )
                    )
                }
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

        if (uiState.currentRoom?.id == com.example.starborn.feature.exploration.presentation.GLADE_BALL_ROOM && !isRoomDark) {
            com.example.starborn.ui.exploration.GladePuffball(
                trust = com.example.starborn.feature.exploration.presentation.gladeBallTrust(uiState.completedMilestones),
                playToken = uiState.gladeBallPlayToken,
                enabled = !blockingOverlayActive && !importantQuestPopupVisible && !roomTransitionInProgress,
                reducedMotion = uiState.settings.disableScreenshake,
                onInspect = viewModel::inspectGladeBall, onPlayFinished = viewModel::playWithGladeBall,
                onCancel = viewModel::cancelGladeBallPlay,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .padding(bottom = ExplorationControlSpacing.sceneBottomClearance + 24.dp)
            )
        }
        if (gateRevealActive) {
            com.example.starborn.ui.exploration.TempleGateReveal(
                gateReveal.progress, uiState.settings.disableFlashes,
                Modifier.fillMaxSize().zIndex(45f).pointerInput(Unit) {
                    awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                }, closedPainter = rememberRoomBackgroundPainter(com.example.starborn.ui.exploration.TEMPLE_GATE_CLOSED_ART),
                cropArtwork = true)
        }

        RestRecoveryOverlay(
            notice = restRecoveryNotice.takeIf { uiState.activeDialogue == null && uiState.cinematic == null && uiState.shopGreeting == null },
            accentColor = actionAccentColor,
            onDismiss = { restRecoveryNotice = null },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(46f)
        )
        uiState.narrationPrompt?.takeIf { queuedNotice == ExplorationNotice.NARRATION }?.let { narration ->
            InspectionOverlay(
                prompt = narration,
                theme = activeTheme,
                onDismiss = { viewModel.dismissNarration() },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
            )
        }
        uiState.blockedPrompt?.let { prompt ->
            InspectionOverlay(
                prompt = NarrationPrompt(prompt.message, tapToDismiss = true),
                theme = activeTheme,
                onDismiss = viewModel::dismissBlockedPrompt,
                modifier = Modifier.fillMaxSize().zIndex(40f)
            )
        }
        if (uiState.roomActionsVisible) {
            AlertDialog(onDismissRequest = viewModel::dismissRoomActions,
                title = { Text("Room actions") },
                text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    uiState.actions.distinctBy { it.actionKey() }.forEach { action ->
                        TextButton(onClick = { viewModel.inspectRoomAction(action) }, modifier = Modifier.fillMaxWidth().maestroTag("room-action-${action.name}")) {
                            Text(roomActionLabel(action))
                        }
                    }
                    if (!com.example.starborn.feature.exploration.presentation.resolveRoomDarkness(uiState)) {
                        uiState.npcs.distinct().forEach { npc ->
                            TextButton(onClick = { viewModel.dismissRoomActions(); viewModel.onNpcInteraction(npc) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Talk to $npc")
                            }
                        }
                        uiState.enemies.distinct().forEach { enemy ->
                            TextButton(onClick = { viewModel.dismissRoomActions(); viewModel.engageEnemy(enemy) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Engage ${uiState.enemyIcons[enemy]?.displayName ?: enemy.replace('_', ' ')}")
                            }
                        }
                    }
                } }, confirmButton = { TextButton(onClick = viewModel::dismissRoomActions) { Text("Close") } })
        }
        uiState.roomObjectOffer?.let { offer ->
            RoomObjectOfferDialog(offer, activeTheme,
                onConfirm = viewModel::confirmRoomObject, onDismiss = viewModel::dismissRoomObject)
        }
        uiState.togglePrompt?.let { prompt ->
            TogglePromptDialog(
                prompt = prompt,
                onSelect = { enable -> viewModel.onTogglePromptSelection(enable) },
                onDismiss = { viewModel.dismissTogglePrompt() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }
        uiState.tuningPuzzle?.let { puzzle ->
            TuningPuzzleDialog(
                puzzle = puzzle,
                onSliderChange = { sliderId, value -> viewModel.updateTuningSlider(sliderId, value) },
                onSubmit = { viewModel.submitTuningPuzzle() },
                onDismiss = { viewModel.dismissTuningPuzzle() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }
        if (uiState.isSimulationDeckVisible) {
            SimulationDeckDialog(
                programs = uiState.availableSimulations,
                onSelectSimulation = { enemyIds -> viewModel.launchSimulationCombat(enemyIds) },
                onDismiss = { viewModel.dismissSimulationDeck() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }
        if (uiState.isAstraNavConsoleVisible) {
            AstraNavConsoleDialog(
                destinations = uiState.astraDestinations,
                dockedTitle = uiState.astraDockedTitle,
                routeHint = uiState.astraRouteHint,
                dockedWorldId = uiState.astraDockedWorldId,
                transitTitle = uiState.astraTransitTitle,
                arrivalTitle = uiState.astraArrivalTitle,
                onDisembark = { viewModel.disembarkAstra() },
                onSelectDestination = { worldId, hubId, roomId, nodeId ->
                    viewModel.travelToWorldFromAstra(worldId, hubId, roomId, nodeId)
                },
                onDismiss = { viewModel.dismissAstraNavConsole() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }
        if (uiState.isTapeDeckVisible) {
            TapeDeckDialog(
                inventory = uiState.inventoryPreview,
                playingTapeId = uiState.playingTapeId,
                onPlayTrack = { tapeId, cueId -> viewModel.playTapeTrack(tapeId, cueId) },
                onStopTrack = { viewModel.stopTapeTrack() },
                onDismiss = { viewModel.dismissTapeDeck() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }
        uiState.activeDialogue?.takeIf { uiState.cinematic == null && !roomTransitionInProgress && !areaEntryActive && !uiState.isLoading }?.let { dialogue ->
            DialogueOverlay(
                dialogue = dialogue,
                choices = uiState.dialogueChoices,
                onAdvance = { viewModel.advanceDialogue() },
                onChoice = { viewModel.onDialogueChoiceSelected(it) },
                onPlayVoice = { viewModel.onDialogueVoiceRequested(it) },
                onPlayMurmur = { cueId, pitch ->
                    audioCuePlayer.execute(
                        listOf(
                            AudioCommand.Play(
                                AudioCueType.VOICE,
                                cueId,
                                loop = false,
                                fadeMs = 0L,
                                gain = 0.50f,
                                pitch = pitch
                            )
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
                    .zIndex(50f)
            )
        }

        uiState.shopGreeting?.let { greeting ->
            ShopGreetingOverlay(
                greeting = greeting,
                onChoice = { viewModel.onShopChoiceSelected(it) },
                onDismiss = { viewModel.dismissShopGreeting() },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
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
                // Bundled guides remain available in normal release builds, independently of debug tools.
                onOpenWalkthrough = { walkthroughVisible = true },
                isTinkeringTutorialActive = uiState.isTinkeringTutorialActive,
                isGearTutorialActive = uiState.isGearTutorialActive,
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
                onOpenFieldKit = {
                    viewModel.closeMenuOverlay()
                    onOpenFieldKit()
                },
                settings = uiState.settings,
                onMusicVolumeChange = { viewModel.updateMusicVolume(it) },
                onSfxVolumeChange = { viewModel.updateSfxVolume(it) },
                onVoiceVolumeChange = { viewModel.updateVoiceVolume(it) },
                onToggleTutorials = { viewModel.updateTutorialsEnabled(it) },
                onToggleVignette = { viewModel.setVignetteEnabled(it) },
                onToggleHighContrast = { viewModel.setHighContrastMode(it) },
                onToggleModernFieldMenu = { viewModel.setModernFieldMenu(it) },
                onToggleLargeTouchTargets = { viewModel.setLargeTouchTargets(it) },
                onToggleScreenshakeDisabled = { viewModel.setScreenshakeDisabled(it) },
                onToggleFlashesDisabled = { viewModel.setFlashesDisabled(it) },
                onToggleHapticsDisabled = { viewModel.setHapticsDisabled(it) },
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
                onReturnToTitle = if (uiState.settings.modernFieldMenu) ({ viewModel.quickSaveAndReturnToTitle(onReturnToTitle) }) else onReturnToTitle,
                partyStatus = uiState.partyStatus,
                onShowSkillTree = { memberId -> viewModel.openSkillTree(memberId) },
                onShowDetails = { memberId -> viewModel.openPartyMemberDetails(memberId) },
                statusMessage = if (uiState.settings.modernFieldMenu) uiState.menuFeedback else uiState.statusMessage,
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
                equippedWeapons = uiState.equippedWeapons,
                equippedArmors = uiState.equippedArmors,
                onEquipItem = { slot, itemId, characterId ->
                    viewModel.equipInventoryItem(slot, itemId, characterId)
                },
                onEquipMod = { slot, itemId, characterId ->
                    val result = viewModel.requestEquipInventoryMod(slot, itemId, characterId)
                    if (result is com.example.starborn.domain.inventory.ModEquipResult.TransferRequired && itemId != null) {
                        val owner = characterId ?: result.sources.firstOrNull()?.characterId
                        if (owner != null) pendingModMove = com.example.starborn.domain.inventory.ModTransferRequest(
                            com.example.starborn.domain.inventory.ModLocation(owner, slot), itemId, result.sources)
                    }
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
                questDetail = uiState.questDetail,
                onCloseQuestDetails = { viewModel.closeQuestDetails() },
                onToggleQuestTracking = { viewModel.toggleQuestTracking(it) },
                partyMemberDetails = uiState.partyMemberDetails,
                onClosePartyMemberDetails = { viewModel.closePartyMemberDetails() },
                skillTreeOverlay = uiState.skillTreeOverlay,
                onCloseSkillTree = { viewModel.closeSkillTreeOverlay() },
                onUnlockSkill = { viewModel.unlockSkillNode(it) },
                onResetSkillBuild = viewModel::resetPrototypeBuild,
                onCloseMapLegend = { viewModel.closeMapLegend() },
                craftingViewModel = craftingViewModel,
                onTinkerTutorialStep = { viewModel.onTinkerTutorialStep(it) },
                onEnvironmentalPreview = if(com.example.starborn.BuildConfig.DEBUG) ({viewModel.closeMenuOverlay();environmentalPreview=true}) else null,
                onDebugTinkeringTutorial = if (com.example.starborn.BuildConfig.DEBUG) ({ viewModel.debugTriggerTinkeringTutorial() }) else null,
                onPlayAudio = onPlayAudio,
                modifier = Modifier.statusBarsPadding()
            )
        }

        pendingMealReplacement?.let { item ->
            AlertDialog(onDismissRequest = { pendingMealReplacement = null },
                title = { Text("Replace your meal?") },
                text = { Text("Eating ${item.name} replaces ${uiState.activeMeal?.recipeName ?: "your active meal"} and starts a new three-battle party bonus.") },
                confirmButton = { TextButton(onClick = {
                    pendingMealReplacement = null
                    beginInventoryUse(item, true)
                }) { Text("Replace meal") } },
                dismissButton = { TextButton(onClick = { pendingMealReplacement = null }) { Text("Keep current meal") } })
        }
        if (showInventoryTargetDialog && pendingInventoryItem != null) {
            val targetOptions = menuPartyMembers.map {
                TargetSelectionOption(
                    id = it.id,
                    name = it.name,
                    detail = it.hpLabel
                )
            }
            val pendingItem = pendingInventoryItem
            if (pendingItem != null) {
                ItemTargetSelectionDialog(
                    itemName = pendingItem.name,
                    targets = targetOptions,
                    onSelect = { targetId ->
                        viewModel.useInventoryItem(pendingItem.id, targetId, replaceMeal = replacingMeal)
                        pendingInventoryItem = null
                        showInventoryTargetDialog = false
                    },
                    onDismiss = {
                        pendingInventoryItem = null
                        showInventoryTargetDialog = false
                    },
                    backgroundColor = FieldMenuDesign.panel.copy(alpha = 0.98f),
                    borderColor = FieldMenuDesign.border.copy(alpha = 0.55f),
                    textColor = FieldMenuDesign.text,
                    accentColor = FieldMenuDesign.cyan
                )
            }
        }
        var saveDialogError by remember { mutableStateOf<String?>(null) }
        if (saveLoadMode != null) {
            SaveLoadDialog(
                mode = saveLoadMode!!,
                slots = slotSummaries,
                onSave = { slot ->
                    coroutineScope.launch {
                        if (!saveDialogBusy) {
                            saveDialogBusy = true
                            saveDialogError = null
                            try {
                                if (viewModel.saveGame(slot)) {
                                    slotSummaries = viewModel.fetchSaveSlots()
                                    saveLoadMode = null
                                } else saveDialogError = "Save failed. Try again."
                            } finally { saveDialogBusy = false }
                        }
                    }
                },
                onLoad = { slot ->
                    saveLoadMode = null
                    viewModel.closeMenuOverlay()
                    viewModel.loadGame(slot)
                },
                onDelete = { slot ->
                    coroutineScope.launch {
                        viewModel.deleteGame(slot)
                        slotSummaries = viewModel.fetchSaveSlots()
                    }
                },
                onDismiss = { if (!saveDialogBusy) { saveLoadMode = null; saveDialogError = null } },
                accentColor = FieldMenuDesign.cyan,
                panelColor = FieldMenuDesign.panel,
                borderColor = FieldMenuDesign.border.copy(alpha = 0.45f),
                textColor = FieldMenuDesign.text,
                modern = uiState.settings.modernFieldMenu,
                largeTargets = uiState.settings.largeTouchTargets,
                highContrast = uiState.settings.highContrastMode,
                confirmLoad = true,
                busy = saveDialogBusy,
                errorMessage = saveDialogError,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(90f)
            )
        }

        if (walkthroughVisible) {
            com.example.starborn.feature.playtest.WalkthroughViewer(
                worldId = uiState.currentWorld?.id,
                roomId = uiState.currentRoom?.id,
                roomTitle = uiState.currentRoom?.title,
                questId = uiState.trackedQuestId,
                questTitle = uiState.questLogActive.firstOrNull { it.id == uiState.trackedQuestId }?.title,
                isTestSession = isTestSession,
                onReturnToGame = { walkthroughVisible = false; viewModel.closeMenuOverlay() }
            )
        }

        if (showExitConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                title = {
                    Text(
                        text = "Return to Title?",
                        style = MaterialTheme.typography.titleMedium,
                        color = FieldMenuDesign.text
                    )
                },
                text = {
                    Text(
                        text = "Any unsaved progress since your last checkpoint will be lost.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FieldMenuDesign.textMuted
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showExitConfirmDialog = false
                            onReturnToTitle()
                        }
                    ) {
                        Text("Exit to Title", color = FieldMenuDesign.cyan)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showExitConfirmDialog = false }
                    ) {
                        Text("Stay", color = FieldMenuDesign.textMuted)
                    }
                },
                containerColor = FieldMenuDesign.panel,
                modifier = Modifier.zIndex(100f)
            )
        }

        if (uiState.showBurgQuestAstraExitDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.dismissBurgQuestAstraExitDialog() },
                title = {
                    Text(
                        text = "Astra Docked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = FieldMenuDesign.gold
                    )
                },
                text = {
                    Text(
                        text = "Disembarking is unavailable during the BurgQuest demo. Feel free to explore the ship, try out the simulation deck or workbench, or use the Demo menu to finish your visit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FieldMenuDesign.text
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.dismissBurgQuestAstraExitDialog() }
                    ) {
                        Text("Stay Aboard", color = FieldMenuDesign.cyan)
                    }
                },
                containerColor = FieldMenuDesign.panel,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(FieldMenuDesign.cardRadius),
                modifier = Modifier.zIndex(100f)
            )
        }

        roomTransition?.scriptedStyle?.let { style ->
            com.example.starborn.ui.exploration.ScriptedTravelOverlay(style, roomTransition.destinationTitle,
                roomTransitionProgress.value, uiState.settings.disableScreenshake, Modifier.zIndex(125f))
        }
        if (areaEntryActive) {
            AreaEntryOverlay(areaEntry.copy(region = areaEntry.region ?: uiState.currentHub?.title)) {
                finishedAreaEntryId = areaEntry.id
                if (areaEntry.id == hubAreaEntry?.id) onHubAreaEntryFinished()
            }
        }
        if (fadeOverlayAnim.value > 0.01f && uiState.cinematic == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeOverlayAnim.value))
                    .align(Alignment.Center)
                    .zIndex(150f)
            )
        }

        if (!uiState.isMenuOverlayVisible && uiState.cinematic == null) {
            ExplorationBottomControls(
                isTutorialBeacon = uiState.isTinkeringTutorialActive || uiState.isGearTutorialActive,
                onOpenMenu = { viewModel.openMenuOverlay() },
                enabled = !blockingOverlayActive && !importantQuestPopupVisible && !roomTransitionInProgress && !uiState.isLoading,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        uiState.cinematic?.let { cinematic ->
            CinematicOverlay(
                state = cinematic,
                onAdvance = { viewModel.advanceCinematic() },
                onSkip = { viewModel.skipCinematic() },
                audioCuePlayer = audioCuePlayer,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(200f)
            )
        }

        uiState.skillTreeOverlay?.takeUnless { uiState.isMenuOverlayVisible }?.let { overlay ->
            SkillTreeOverlay(
                overlay = overlay,
                theme = null,
                onClose = { viewModel.closeSkillTreeOverlay() },
                onUnlockSkill = { viewModel.unlockSkillNode(it) },
                onResetBuild = { viewModel.resetPrototypeBuild(overlay.characterId) }
            )
        }

        uiState.partyMemberDetails?.takeUnless { uiState.isMenuOverlayVisible }?.let { details ->
            PartyMemberDetailsDialog(
                details = details,
                accentColor = FieldMenuDesign.cyan,
                borderColor = FieldMenuDesign.border,
                onDismiss = { viewModel.closePartyMemberDetails() }
            )
        }

        uiState.eventAnnouncement?.takeIf { queuedNotice == ExplorationNotice.ANNOUNCEMENT }?.let { announcement ->
            EventAnnouncementOverlay(
                announcement = announcement,
                theme = activeTheme,
                onDismiss = { viewModel.dismissEventAnnouncement() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(60f)
            )
        }

        uiState.levelUpPrompt?.takeIf { canPresentLevelUp(uiState, gateRevealActive || restRecoveryNotice != null || areaEntryActive || environmentalPreview || saveLoadMode != null || showInventoryTargetDialog) }?.let { prompt ->
            LevelUpOverlay(
                prompt = prompt,
                onDismiss = { viewModel.dismissLevelUpPrompt() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .zIndex(65f)
            )
        }

        val questAccentColor = themeColor(activeTheme?.accent, Color(0xFF80E0FF))

        if (uiState.isQuestLogVisible) {
            QuestJournalOverlay(
                trackedQuest = uiState.questLogActive.firstOrNull { it.id == uiState.trackedQuestId },
                activeQuests = uiState.questLogActive,
                completedQuests = uiState.questLogCompleted,
                failedQuests = uiState.failedQuests,
                questLog = uiState.questLogEntries,
                accentColor = questAccentColor,
                onClose = { viewModel.closeQuestLog() },
                onSelectQuest = { questId ->
                    viewModel.openQuestDetails(questId)
                },
                modifier = Modifier.align(Alignment.Center).zIndex(45f)
            )
        }

        if (uiState.isMapLegendVisible && !uiState.isMenuOverlayVisible) {
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

        if (uiState.isAstraRecreationVisible) {
            com.example.starborn.feature.exploration.ui.components.AstraRecreationDialog(uiState.astraArcades,
                uiState.astraPrizesAvailable, viewModel::openRecreationArcade, viewModel::openRecreationFilms,
                viewModel::openRecreationPrizes, viewModel::closeAstraRecreation)
        }

        uiState.astraFacility?.let { facility ->
            androidx.compose.ui.window.Dialog(onDismissRequest = viewModel::closeAstraFacility,
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(Modifier.fillMaxWidth().padding(16.dp), color = FieldMenuDesign.shell,
                    contentColor = FieldMenuDesign.text, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(facility.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = viewModel::closeAstraFacility) { Text("Close") }
                        }
                        if (facility.title == "Cargo Manifest" || facility.title == "Relic Array") {
                            com.example.starborn.feature.exploration.ui.components.CargoRecordTabs(
                                if (facility.title == "Relic Array") "relics" else "cargo", viewModel::selectCargoRecord)
                        }
                        facility.entries.forEach { entry ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(entry.title + if (entry.isNew) " - New" else "", color = FieldMenuDesign.cyan,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCatchLogVisible) {
            androidx.compose.ui.window.Dialog(onDismissRequest = viewModel::closeCatchLog,
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(Modifier.fillMaxWidth().padding(16.dp), color = FieldMenuDesign.shell,
                    contentColor = FieldMenuDesign.text, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Astra Catch Log", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = viewModel::closeCatchLog) { Text("Close") }
                        }
                        com.example.starborn.feature.exploration.ui.components.CargoRecordTabs("catch", viewModel::selectCargoRecord)
                        Text("Cargo Bay specimen archive. Records remain after cooking or selling.", style = MaterialTheme.typography.bodySmall)
                        com.example.starborn.feature.fishing.ui.FishingJournal(uiState.fishingJournal, initiallyExpanded = true)
                    }
                }
            }
        }

        if (uiState.isMilestoneGalleryVisible) {
            MilestoneGalleryOverlay(
                history = uiState.milestoneHistory,
                currentRoom = uiState.currentRoom,
                onClose = { viewModel.closeMilestoneGallery() }
            )
        }

        if (queuedNotice == ExplorationNotice.PROMPT) {
            val displayedPrompt = uiState.prompt
            UIPromptOverlay(
                prompt = displayedPrompt,
                onDismiss = { viewModel.dismissPrompt(displayedPrompt?.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
                    .zIndex(200f)
            )
        }

        QuestBannerOverlay(
            state = questPresentations,
            deferShowing = blockingOverlayActive || importantQuestPopupVisible,
            accentColor = questAccentColor,
            reducedMotion = uiState.settings.disableScreenshake,
            showPulse = !uiState.settings.disableFlashes,
            onObjectiveChecked = { viewModel.playQuestPresentationCue("quest_update") },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(30f)
        )

        ProgressToastOverlay(
            state = questPresentations,
            deferShowing = blockingOverlayActive || importantQuestPopupVisible,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = ExplorationControlSpacing.noticeBottomClearance)
                .zIndex(180f)
        )

        QuestDetailOverlay(
            state = questPresentations,
            gradientColor = questAccentColor,
            outlineColor = panelBorderColor,
            deferShowing = questDetailBlockingOverlayActive || areaEntryActive || environmentalPreview || saveLoadMode != null || showInventoryTargetDialog,
            onPresentationVisibleChanged = { importantQuestPopupVisible = it },
            onPresentationStarted = { type ->
                val cueKey = when (type) {
                    QuestBannerType.NEW -> "quest_new"
                    QuestBannerType.COMPLETED -> "quest_complete"
                    QuestBannerType.PROGRESS -> "quest_update"
                    QuestBannerType.FAILED -> "error"
                }
                viewModel.playQuestPresentationCue(cueKey)
            },
            onShowDetails = { questId ->
                viewModel.openQuestDetails(questId)
            },
            modifier = Modifier.zIndex(30f)
        )



        uiState.questDetail?.takeUnless { uiState.isMenuOverlayVisible }?.let { detail ->
            QuestDetailSheet(
                detail = detail,
                accentColor = questAccentColor,
                onClose = { viewModel.closeQuestDetails() },
                onToggleTrack = { questId -> viewModel.toggleQuestTracking(questId) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .zIndex(55f)
            )
        }

        CraftingFxOverlay(
            bursts = fxBursts,
            onExpired = { id -> fxBursts.removeAll { it.id == id } },
            modifier = Modifier.fillMaxSize()
        )

        if(environmentalPreview && uiState.currentRoom!=null) {
            val previewRoom=uiState.currentRoom!!
            val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
            com.example.starborn.ui.environment.EnvironmentalEffectsPreview(previewRoom,
                viewModel.environmentalAssets.environmentalEffectsCatalog,rememberRoomBackgroundPainter(previewRoom.backgroundImage),
                com.example.starborn.ui.vfx.rememberAndroidEnvironmentSteam(true),uiState.roomState,uiState.completedMilestones,
                onCopy={clipboard.setText(androidx.compose.ui.text.AnnotatedString(it))},onClose={environmentalPreview=false})
        }
        if ((!blockingOverlayActive || uiState.activeDialogue != null) && !showInventoryTargetDialog && saveLoadMode == null) {
            DirectionIndicatorsOverlay(
                indicators = uiState.directionIndicators,
                enabled = !blockingOverlayActive && !importantQuestPopupVisible && !roomTransitionInProgress,
                onTravel = { direction ->
                    if (!roomTransitionInProgress) viewModel.travel(direction)
                },
                bottomClearance = ExplorationControlSpacing.southExitBottomClearance,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .zIndex(1f)
            )
        }
    }
}
}

private data class WorldHeaderPalette(
    val titleTint: Color,
    val accentTint: Color
)

private fun worldHeaderPalette(worldId: String, fallbackWarm: Color, fallbackAccent: Color): WorldHeaderPalette {
    val id = worldId.lowercase(Locale.ROOT)
    return when {
        id.contains("world_2") || id.contains("sector9") -> WorldHeaderPalette(
            titleTint = Color(0xFFE7F5C8),
            accentTint = Color(0xFF4BE3C8)
        )
        id.contains("world_3") || id.contains("spire") -> WorldHeaderPalette(
            titleTint = Color(0xFFF3E8FF),
            accentTint = Color(0xFF56E1FF)
        )
        id.contains("world_4") || id.contains("foundry") -> WorldHeaderPalette(
            titleTint = Color(0xFFFFD599),
            accentTint = Color(0xFFFF7A3D)
        )
        id.contains("world_5") || id.contains("orbital") -> WorldHeaderPalette(
            titleTint = Color(0xFFEAF4FF),
            accentTint = Color(0xFF7BC8FF)
        )
        id.contains("world_6") || id.contains("source") -> WorldHeaderPalette(
            titleTint = Color(0xFFFFE7A3),
            accentTint = Color(0xFFB58BFF)
        )
        else -> WorldHeaderPalette(
            titleTint = fallbackWarm,
            accentTint = fallbackAccent
        )
    }
}

@Composable
private fun RoomHeaderPanel(
    roomId: String,
    worldId: String,
    sectorLabel: String,
    isGateway: Boolean,
    hasHostiles: Boolean,
    roomTitle: String,
    isDark: Boolean,
    obscureTitle: Boolean,
    reduceMotion: Boolean,
    titleColor: Color,
    warmTitleColor: Color,
    minimap: MinimapUiState?,
    minimapSize: Dp,
    onTitleClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val palette = remember(worldId, warmTitleColor, titleColor) {
        worldHeaderPalette(worldId, warmTitleColor, titleColor)
    }
    val arrivalProgress = remember(roomId) { CoreAnimatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(roomId, reduceMotion) {
        if (reduceMotion) {
            arrivalProgress.snapTo(1f)
        } else {
            arrivalProgress.snapTo(0f)
            arrivalProgress.animateTo(1f, animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing))
        }
    }
    val hostilePulseTransition = rememberInfiniteTransition(label = "roomHeaderHostilePulse")
    val hostilePulse by hostilePulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hostilePulseAlpha"
    )

    val shape = RoundedCornerShape(12.dp)
    val panelColor = Color(0xFF061018).copy(alpha = if (isDark) 0.72f else 0.58f)
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.20f)
    } else if (hasHostiles) {
        Color(0xFFFF746D).copy(alpha = 0.45f)
    } else {
        palette.accentTint.copy(alpha = 0.36f)
    }
    val rulePrimary = if (isDark) Color.White.copy(alpha = 0.45f) else palette.titleTint
    val ruleSecondary = if (isDark) Color.White.copy(alpha = 0.16f) else palette.accentTint

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = panelColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle world-tinted atmospheric gradient wash + corner tactical bracket ticks
            Canvas(modifier = Modifier.matchParentSize()) {
                val tick = 9.dp.toPx()
                val inset = 4.dp.toPx()
                val stroke = 1.4.dp.toPx()
                val bracketColor = (if (isDark) Color.White.copy(alpha = 0.25f) else palette.accentTint.copy(alpha = 0.55f))
                // Top-left bracket
                drawLine(bracketColor, Offset(inset, inset), Offset(inset + tick, inset), stroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(inset, inset), Offset(inset, inset + tick), stroke, StrokeCap.Round)
                // Bottom-left bracket
                drawLine(bracketColor, Offset(inset, size.height - inset), Offset(inset + tick, size.height - inset), stroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(inset, size.height - inset), Offset(inset, size.height - inset - tick), stroke, StrokeCap.Round)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                palette.titleTint.copy(alpha = if (isDark) 0.04f else 0.09f),
                                palette.accentTint.copy(alpha = if (isDark) 0.02f else 0.05f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(start = 15.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTitleClick() },
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Diegetic sector eyebrow + subtle status pip (rewarding gateways/threats without shrinking title)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.45f) else palette.accentTint
                                )
                        )
                        Text(
                            text = sectorLabel.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            ),
                            color = (if (isDark) Color.White.copy(alpha = 0.52f) else palette.accentTint.copy(alpha = 0.85f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        when {
                            hasHostiles -> {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFFFF746D).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFFF746D).copy(alpha = 0.55f * (if (reduceMotion) 1f else hostilePulse)))
                                ) {
                                    Text(
                                        text = "HOSTILE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.9.sp
                                        ),
                                        color = Color(0xFFFF8E88),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            isGateway -> {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = palette.accentTint.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, palette.accentTint.copy(alpha = 0.48f))
                                ) {
                                    Text(
                                        text = "EXIT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.9.sp
                                        ),
                                        color = palette.accentTint,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Full-presence atmospheric room title on EVERY room with smooth sweep-in
                    val p = arrivalProgress.value
                    Text(
                        text = if (obscureTitle) "???" else roomTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.72f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 2.5f
                            )
                        ),
                        color = if (isDark) Color.White.copy(alpha = 0.92f) else palette.titleTint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.45f + 0.55f * p
                            translationX = (1f - p) * (-14f)
                        }
                    )

                    // Sweeping world-tinted signature hairline rule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .graphicsLayer {
                                scaleX = p.coerceAtLeast(0.01f)
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        rulePrimary.copy(alpha = if (isDark) 0.42f else 0.85f),
                                        ruleSecondary.copy(alpha = if (isDark) 0.18f else 0.42f),
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
@OptIn(ExperimentalLayoutApi::class)
private fun QuestDetailSheet(
    detail: QuestDetailUi,
    accentColor: Color,
    onClose: () -> Unit,
    onToggleTrack: (String) -> Unit,
    embedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val sections = detail.stages.ifEmpty {
        listOf(
            QuestStageDetailUi(
                id = "current",
                title = detail.stageTitle ?: "Current Stage",
                description = detail.stageDescription,
                objectives = detail.objectives,
                stageIndex = detail.stageIndex,
                current = !detail.completed,
                completed = detail.completed
            )
        )
    }
    Surface(
        modifier = if (embedded) modifier.fillMaxWidth() else modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF0061018),
        border = BorderStroke(1.dp, if (embedded && LocalModernFieldMenu.current) Color.White.copy(alpha = .1f) else accentColor.copy(alpha = 0.48f)),
        shadowElevation = if (embedded && LocalModernFieldMenu.current) 0.dp else 14.dp
    ) {
        Column(
            modifier = Modifier
                .then(if (embedded && LocalModernFieldMenu.current) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(18.dp)
                .then(if (embedded && LocalModernFieldMenu.current) Modifier else Modifier.verticalScroll(scrollState)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "QUEST FILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor.copy(alpha = 0.88f),
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuestStatusPill(
                                text = if (detail.completed) "COMPLETED" else "ACTIVE",
                                accentColor = accentColor,
                                filled = detail.completed
                            )
                            if (!detail.completed) {
                                QuestStatusPill(
                                    text = "STAGE ${detail.stageIndex + 1}/${detail.totalStages}",
                                    accentColor = Color.White.copy(alpha = 0.7f),
                                    filled = false
                                )
                            }
                            if (detail.tracked) {
                                QuestStatusPill(
                                    text = "TRACKED",
                                    accentColor = accentColor,
                                    filled = false
                                )
                            }
                        }
                    }
                }
                if (!embedded) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.74f)
                        )
                    }
                }
            }
            HorizontalDivider(color = accentColor.copy(alpha = 0.22f))
            detail.summary.takeIf { it.isNotBlank() }?.let { summary ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.58f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                }
            }
            detail.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.62f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (detail.completed) "STAGE ARCHIVE" else "MISSION STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor.copy(alpha = 0.9f),
                    letterSpacing = 0.8.sp
                )
                if (sections.all { it.objectives.isEmpty() }) {
                    Text(
                        text = "No objectives listed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    sections.forEachIndexed { index, stage ->
                        QuestStageSection(stage = stage, accentColor = accentColor)
                        if (index != sections.lastIndex) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            }
            if (detail.rewards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "REWARDS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp
                    )
                    detail.rewards.forEach { reward ->
                        Surface(
                            color = Color.White.copy(alpha = 0.045f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val trackLabel = if (detail.tracked) "Stop Tracking" else "Track Quest"
                Button(
                    onClick = { onToggleTrack(detail.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (detail.tracked) accentColor else Color.Transparent,
                        contentColor = if (detail.tracked) Color(0xFF010308) else accentColor
                    ),
                    border = if (detail.tracked) null else BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
                ) {
                    Text(trackLabel)
                }
                if (!embedded) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestStatusPill(
    text: String,
    accentColor: Color,
    filled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (filled) accentColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (filled) 0.38f else 0.24f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (filled) accentColor else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun QuestStageSection(
    stage: QuestStageDetailUi,
    accentColor: Color
) {
    val statusLabel = when {
        stage.current -> "CURRENT"
        stage.completed -> "COMPLETE"
        else -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (stage.current) accentColor.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.035f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (stage.current) accentColor else Color.White.copy(alpha = 0.38f),
                            CircleShape
                        )
                )
                Text(
                    text = "Stage ${stage.stageIndex + 1}: ${stage.title}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (stage.current) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (stage.current) accentColor else Color.White
                )
            }
            if (statusLabel.isNotBlank()) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (stage.current) accentColor else Color.White.copy(alpha = 0.56f)
                )
            }
        }
        stage.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = if (stage.current) 0.72f else 0.56f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stage.objectives.forEach { objective ->
                QuestObjectiveRow(
                    objective = objective,
                    accentColor = accentColor,
                    subdued = stage.completed && !stage.current
                )
            }
        }
    }
}

@Composable
private fun QuestObjectiveRow(
    objective: QuestObjectiveUi,
    accentColor: Color,
    subdued: Boolean = false
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
            tint = tint.copy(alpha = if (subdued) 0.62f else 1f),
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = objective.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = if (subdued) 0.62f else 0.9f)
        )
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
internal fun MenuOverlay(
    selectedTab: MenuTab,
    onOpenRegionMap: (() -> Unit)? = null,
    onOpenWalkthrough: (() -> Unit)? = null,
    onSelectTab: (MenuTab) -> Unit,
    onClose: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenMapLegend: () -> Unit,
    onOpenFieldKit: () -> Unit,
    settings: SettingsUiState,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleModernFieldMenu: (Boolean) -> Unit = {},
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null,
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
    equippedWeapons: Map<String, String>,
    equippedArmors: Map<String, String>,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseInventoryItem: (InventoryPreviewItemUi) -> Unit,
    onShowQuestDetails: (String) -> Unit,
    questDetail: QuestDetailUi?,
    onCloseQuestDetails: () -> Unit,
    onToggleQuestTracking: (String) -> Unit,
    partyMemberDetails: PartyMemberDetailsUi?,
    onClosePartyMemberDetails: () -> Unit,
    skillTreeOverlay: SkillTreeOverlayUi?,
    onCloseSkillTree: () -> Unit,
    onUnlockSkill: (String) -> Unit,
    onResetSkillBuild: (String) -> Unit,
    onCloseMapLegend: () -> Unit,
    craftingViewModel: CraftingViewModel? = null,
    onTinkerTutorialStep: ((com.example.starborn.feature.crafting.TinkeringTutorialStep) -> Unit)? = null,
    isTinkeringTutorialActive: Boolean = false,
    isGearTutorialActive: Boolean = false,
    onEnvironmentalPreview: (() -> Unit)? = null,
    onDebugTinkeringTutorial: (() -> Unit)? = null,
    onPlayAudio: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val panelColor = FieldMenuDesign.shell.copy(alpha = 0.98f)
    val panelBorder = FieldMenuDesign.border
    val accentColor = FieldMenuDesign.cyan
    val warmAccent = FieldMenuDesign.gold
    val roomAtmosphere = themeColor(theme?.accent, accentColor)
    var detailKind by rememberSaveable { mutableStateOf<MenuDetailKind?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailItem by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    var rootScrollPosition by rememberSaveable { mutableIntStateOf(0) }
    val sheetScroll = rememberScrollState()
    val menuScope = rememberCoroutineScope()

    fun openDetail(
        kind: MenuDetailKind,
        id: String? = null,
        item: InventoryPreviewItemUi? = null,
        onOpen: () -> Unit = {}
    ) {
        rootScrollPosition = sheetScroll.value
        detailKind = kind
        detailId = id
        detailItem = item
        menuScope.launch { sheetScroll.scrollTo(0) }
        onOpen()
    }

    fun closeDetail() {
        when (detailKind) {
            MenuDetailKind.QUEST -> onCloseQuestDetails()
            MenuDetailKind.PARTY_MEMBER -> onClosePartyMemberDetails()
            MenuDetailKind.SKILL_TREE -> onCloseSkillTree()
            MenuDetailKind.MAP_LEGEND -> onCloseMapLegend()
            else -> Unit
        }
        detailKind = null
        detailId = null
        detailItem = null
        menuScope.launch { sheetScroll.scrollTo(rootScrollPosition) }
    }

    var selectedPartyId by rememberSaveable { mutableStateOf<String?>(null) }
    val activePartyId = selectedPartyId?.takeIf { id -> partyStatus.members.any { it.id == id } }
        ?: partyStatus.members.firstOrNull()?.id
    val pageContent: @Composable () -> Unit = {
                    if (detailKind == null && selectedTab == MenuTab.MAP && onOpenRegionMap != null) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Region map", style = MaterialTheme.typography.headlineSmall)
                            Text("Return to the region map to select a destination. Your current selection is preserved.")
                            Button(onClick = onOpenRegionMap) { Text("Return to region map") }
                        }
                    } else if (detailKind == null) MenuTabContentArea(
                        tab = selectedTab,
                        modern = settings.modernFieldMenu,
                        selectedMemberId = activePartyId,
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
                        onOpenMapLegend = {
                            openDetail(MenuDetailKind.MAP_LEGEND, onOpen = onOpenMapLegend)
                        },
                        onOpenFieldKit = onOpenFieldKit,
                        onMusicVolumeChange = onMusicVolumeChange,
                        onSfxVolumeChange = onSfxVolumeChange,
                        onVoiceVolumeChange = onVoiceVolumeChange,
                        onToggleTutorials = onToggleTutorials,
                        onToggleVignette = onToggleVignette,
                        onToggleModernFieldMenu = onToggleModernFieldMenu,
                        onToggleHighContrast = onToggleHighContrast,
                        onToggleLargeTouchTargets = onToggleLargeTouchTargets,
                        onToggleScreenshakeDisabled = onToggleScreenshakeDisabled,
                        onToggleFlashesDisabled = onToggleFlashesDisabled,
                        onToggleHapticsDisabled = onToggleHapticsDisabled,
                        onQuickSave = onQuickSave,
                        onSaveGame = onSaveGame,
                        onLoadGame = onLoadGame,
                        onReturnToTitle = onReturnToTitle,
                        onShowSkillTree = { memberId ->
                            openDetail(MenuDetailKind.SKILL_TREE, id = memberId) {
                                onShowSkillTree(memberId)
                            }
                        },
                        onShowDetails = { memberId ->
                            openDetail(MenuDetailKind.PARTY_MEMBER, id = memberId) {
                                onShowDetails(memberId)
                            }
                        },
                        inventoryItems = inventoryItems,
                        equippedItems = equippedItems,
                        completedMilestones = completedMilestones,
                        equippedWeapons = equippedWeapons,
                        equippedArmors = equippedArmors,
                        onEquipItem = onEquipItem,
                        onEquipMod = onEquipMod,
                        onEquipWeapon = onEquipWeapon,
                        resolveWeaponItem = resolveWeaponItem,
                        onEquipArmor = onEquipArmor,
                        resolveArmorItem = resolveArmorItem,
                        onUseInventoryItem = onUseInventoryItem,
                        onShowQuestDetails = { questId ->
                            openDetail(MenuDetailKind.QUEST, id = questId) {
                                onShowQuestDetails(questId)
                            }
                        },
                        onShowItemDetails = { item ->
                            openDetail(MenuDetailKind.INVENTORY_ITEM, id = item.id, item = item)
                        },
                        craftingViewModel = craftingViewModel,
                        onTinkerTutorialStep = onTinkerTutorialStep,
                        onEnvironmentalPreview = onEnvironmentalPreview,
                        onDebugTinkeringTutorial = onDebugTinkeringTutorial,
                        onOpenWalkthrough = onOpenWalkthrough,
                        onPlayAudio = onPlayAudio,
                        creditsLabel = creditsLabel,
                        isGearTutorialActive = isGearTutorialActive
                    ) else MenuNestedDetailContent(
                        detailKind = detailKind,
                        detailId = detailId,
                        detailItem = detailItem?.let { saved ->
                            inventoryItems.firstOrNull { it.id == saved.id } ?: saved.copy(quantity = 0)
                        },
                        questDetail = questDetail,
                        partyMemberDetails = partyMemberDetails,
                        skillTreeOverlay = skillTreeOverlay,
                        fullMap = fullMap,
                        accentColor = accentColor,
                        borderColor = panelBorder,
                        onBack = ::closeDetail,
                        onToggleQuestTracking = onToggleQuestTracking,
                        onUnlockSkill = onUnlockSkill,
                        onResetSkillBuild = onResetSkillBuild,
                        onUseItem = { item ->
                            onUseInventoryItem(item)
                        }
                    )
    }

    BackHandler(enabled = detailKind != null) { closeDetail() }
    val cornerRadius = 14.dp
    val borderWidth = 1.dp
    val borderColor = panelBorder.copy(alpha = 0.62f)
    val contentMaxWidth = 860.dp

    Surface(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (settings.modernFieldMenu) return@drawWithContent
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
            if (settings.modernFieldMenu) {
                androidx.compose.runtime.CompositionLocalProvider(LocalModernFieldMenu provides true,
                    LocalFieldMenuHighContrast provides settings.highContrastMode,
                    LocalFieldMenuLargeTargets provides settings.largeTouchTargets,
                    com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuReducedMotion provides (settings.disableFlashes || settings.disableScreenshake)) {
                    ModernFieldMenu(
                        tab = selectedTab,
                        detailTitle = detailKind?.let { menuDetailTitle(it, selectedTab) },
                        party = partyStatus,
                        selectedMemberId = activePartyId,
                        credits = when {
                            detailKind == MenuDetailKind.SKILL_TREE -> "${skillTreeOverlay?.availableAp ?: 0} AP"
                            selectedTab == MenuTab.INVENTORY && detailKind == null -> creditsLabel
                            else -> null
                        },
                        scroll = sheetScroll,
                        tinkerTutorial = isTinkeringTutorialActive,
                        gearTutorial = isGearTutorialActive,
                        roomAccent = roomAtmosphere,
                        statusMessage = statusMessage,
                        onSelectTab = { tab ->
                            if (detailKind != null) closeDetail()
                            menuScope.launch { sheetScroll.scrollTo(0) }
                            onSelectTab(tab)
                        },
                        onSelectMember = { memberId ->
                            selectedPartyId = memberId
                            when (detailKind) {
                                MenuDetailKind.PARTY_MEMBER -> {
                                    detailId = memberId
                                    onShowDetails(memberId)
                                }
                                MenuDetailKind.SKILL_TREE -> {
                                    detailId = memberId
                                    onShowSkillTree(memberId)
                                }
                                else -> Unit
                            }
                        },
                        onBack = ::closeDetail,
                        onSave = onQuickSave,
                        onClose = { if (detailKind != null) closeDetail(); onClose() },
                        tutorialGuide = {
                            if (isTinkeringTutorialActive && selectedTab == MenuTab.FIELD_KIT && craftingViewModel != null) {
                                val craftState by craftingViewModel.uiState.collectAsState()
                                val step = craftState.tutorialStep
                                LaunchedEffect(step) {
                                    if (step != null) onPlayAudio("sfx_hub_node_select")
                                }
                            }
                        },
                        content = pageContent
                    )
                }
            } else {
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
                                roomAtmosphere.copy(alpha = 0.06f),
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
                                text = menuDetailTitle(
                                    detailKind = detailKind,
                                    selectedTab = selectedTab
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 34.sp),
                                color = warmAccent
                            )
                        }
                        Surface(
                            onClick = {
                                if (detailKind != null) closeDetail()
                                onClose()
                            },
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

                    if (detailKind == null) {
                        MenuTabRow(
                            selectedTab = selectedTab,
                            onSelectTab = onSelectTab,
                            accentColor = accentColor,
                            borderColor = panelBorder,
                            isTinkeringTutorialActive = isTinkeringTutorialActive,
                            isGearTutorialActive = isGearTutorialActive
                        )
                    } else {
                        MenuDetailNavigationBar(
                            parentLabel = selectedTab.label(),
                            onBack = ::closeDetail,
                            accentColor = accentColor
                        )
                    }

                    pageContent()
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

            // SEPARATE POPUP OVERTOP THE ENTIRE FIELD MENU DIALOG
            if (!settings.modernFieldMenu && isTinkeringTutorialActive && selectedTab == MenuTab.FIELD_KIT && craftingViewModel != null) {
                val craftState by craftingViewModel.uiState.collectAsState()
                val currentStep = craftState.tutorialStep
                LaunchedEffect(currentStep) {
                    if (currentStep != null) {
                        onPlayAudio("sfx_hub_node_select")
                    }
                }
                if (craftState.isTutorialActive && currentStep != null) {
                    TinkeringTutorialOverlay(
                        step = currentStep,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .zIndex(999f)
                    )
                }
            }
        }
    }
}

private fun menuDetailTitle(
    detailKind: MenuDetailKind?,
    selectedTab: MenuTab
): String = when (detailKind) {
    MenuDetailKind.QUEST -> "Quest Details"
    MenuDetailKind.INVENTORY_ITEM -> "Item Details"
    MenuDetailKind.PARTY_MEMBER -> "Party Details"
    MenuDetailKind.SKILL_TREE -> "Skill Tree"
    MenuDetailKind.MAP_LEGEND -> "Map Legend"
    null -> selectedTab.label()
}

@Composable
private fun MenuDetailNavigationBar(
    parentLabel: String,
    onBack: () -> Unit,
    accentColor: Color
) {
    Surface(
        onClick = onBack,
        shape = RoundedCornerShape(FieldMenuDesign.controlRadius),
        color = FieldMenuDesign.panel.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("‹", color = accentColor, style = MaterialTheme.typography.titleLarge)
            Column {
                Text("BACK", color = accentColor, style = MaterialTheme.typography.labelSmall)
                Text(parentLabel, color = FieldMenuDesign.text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MenuNestedDetailContent(
    detailKind: MenuDetailKind?,
    detailId: String?,
    detailItem: InventoryPreviewItemUi?,
    questDetail: QuestDetailUi?,
    partyMemberDetails: PartyMemberDetailsUi?,
    skillTreeOverlay: SkillTreeOverlayUi?,
    fullMap: FullMapUiState?,
    accentColor: Color,
    borderColor: Color,
    onBack: () -> Unit,
    onToggleQuestTracking: (String) -> Unit,
    onUnlockSkill: (String) -> Unit,
    onResetSkillBuild: (String) -> Unit,
    onUseItem: (InventoryPreviewItemUi) -> Unit
) {
    when (detailKind) {
        MenuDetailKind.QUEST -> if (questDetail?.id.equals(detailId, ignoreCase = true)) {
            QuestDetailSheet(
                detail = questDetail!!,
                accentColor = accentColor,
                onClose = onBack,
                onToggleTrack = onToggleQuestTracking,
                embedded = true,
                modifier = if (LocalModernFieldMenu.current) Modifier else Modifier.heightIn(max = 700.dp)
            )
        } else MenuDetailLoading("Loading quest file…", accentColor)

        MenuDetailKind.INVENTORY_ITEM -> if (detailItem != null) {
            InventoryItemDetailsContent(
                item = detailItem,
                accentColor = accentColor,
                borderColor = borderColor,
                onUse = detailItem.effect?.takeIf { detailItem.quantity > 0 && !detailItem.combatOnly }?.let { { onUseItem(detailItem) } }
            )
        } else MenuDetailLoading("Item details unavailable.", accentColor)

        MenuDetailKind.PARTY_MEMBER -> if (partyMemberDetails?.id.equals(detailId, ignoreCase = true)) {
            PartyMemberDetailsContent(
                details = partyMemberDetails!!,
                accentColor = accentColor,
                borderColor = borderColor
            )
        } else MenuDetailLoading("Loading party file…", accentColor)

        MenuDetailKind.SKILL_TREE -> if (skillTreeOverlay?.characterId.equals(detailId, ignoreCase = true)) {
            SkillTreeContent(
                overlay = skillTreeOverlay!!,
                accentColor = accentColor,
                borderColor = borderColor,
                onUnlockSkill = onUnlockSkill,
                onResetBuild = { onResetSkillBuild(skillTreeOverlay.characterId) },
                modifier = if (LocalModernFieldMenu.current) Modifier else Modifier.heightIn(max = 700.dp)
            )
        } else MenuDetailLoading("Loading skill tree…", accentColor)

        MenuDetailKind.MAP_LEGEND -> MapLegendOverlay(
            onClose = onBack,
            accentColor = accentColor,
            panelColor = FieldMenuDesign.panel,
            borderColor = borderColor.copy(alpha = 0.62f),
            textColor = FieldMenuDesign.text,
            embedded = true
        )
        null -> Unit
    }
}

@Composable
private fun MenuDetailLoading(message: String, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FieldMenuDesign.cardRadius),
        color = FieldMenuDesign.panel,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = message,
            color = FieldMenuDesign.textMuted,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun MenuTabRow(
    selectedTab: MenuTab,
    onSelectTab: (MenuTab) -> Unit,
    accentColor: Color,
    borderColor: Color,
    isTinkeringTutorialActive: Boolean = false,
    isGearTutorialActive: Boolean = false
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedTab) {
        val tabIndex = selectedTab.ordinal
        val offset = tabIndex * 220
        scrollState.animateScrollTo(offset)
    }

    Row(
        modifier = Modifier
            .widthIn(max = 1200.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF061018).copy(alpha = 0.38f))
            .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(3.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        MenuTab.values().forEach { tab ->
            val isBeacon = (isTinkeringTutorialActive && tab == MenuTab.FIELD_KIT && tab != selectedTab) ||
                (isGearTutorialActive && tab == MenuTab.INVENTORY && tab != selectedTab)
            MenuTabChip(
                tab = tab,
                isSelected = tab == selectedTab,
                isTutorialBeacon = isBeacon,
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
    isTutorialBeacon: Boolean = false,
    onSelect: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "tab_beacon_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val background = when {
        isSelected -> {
            Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.28f),
                    Color(0xFFFFC857).copy(alpha = 0.12f)
                )
            )
        }
        isTutorialBeacon -> {
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFC857).copy(alpha = 0.25f * pulseAlpha),
                    Color(0xFFFFC857).copy(alpha = 0.08f)
                )
            )
        }
        else -> {
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Transparent)
            )
        }
    }

    val chipBorderColor = when {
        isSelected -> borderColor.copy(alpha = 0.72f)
        isTutorialBeacon -> Color(0xFFFFC857).copy(alpha = pulseAlpha)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> Color.White
        isTutorialBeacon -> Color(0xFFFFC857)
        else -> accentColor.copy(alpha = 0.78f)
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, chipBorderColor),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .widthIn(min = 92.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isTutorialBeacon) {
                    Text(
                        text = "●",
                        color = Color(0xFFFFC857),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = tab.label(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    fontWeight = if (isSelected || isTutorialBeacon) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TuningPuzzleDialog(
    puzzle: TuningPuzzleUi,
    onSliderChange: (String, Float) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val choicePanel = com.example.starborn.shared.puzzle.ChoicePuzzlePanel.forPresentation(puzzle.presentation)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(puzzle.title) },
        text = {
            if (choicePanel != null) {
                com.example.starborn.shared.puzzle.PuzzleChoices(choicePanel, puzzle.prompt, puzzle.sliders.map {
                    com.example.starborn.shared.puzzle.SignalDial(it.id, it.value, it.min, it.max, it.target, it.tolerance)
                }, puzzle.feedback, onSliderChange)
            } else if (puzzle.presentation == "counter_tune") {
                com.example.starborn.shared.puzzle.CounterTunePanel(
                    dials = puzzle.sliders.map {
                        com.example.starborn.shared.puzzle.SignalDial(it.id, it.value, it.min, it.max, it.target, it.tolerance)
                    },
                    onChange = onSliderChange,
                    onHandshake = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    feedback = puzzle.feedback
                )
            } else if (puzzle.presentation == "prism_optics") {
                com.example.starborn.shared.puzzle.PrismOpticsPanel(
                    dials = puzzle.sliders.map {
                        com.example.starborn.shared.puzzle.SignalDial(it.id, it.value, it.min, it.max, it.target, it.tolerance)
                    }, onChange = onSliderChange, onCapture = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(puzzle.prompt)
                    puzzle.feedback?.takeIf { it.isNotBlank() }?.let { feedback ->
                        Text(
                            text = feedback,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    val panelColor = MaterialTheme.colorScheme.surfaceVariant
                    val accent = MaterialTheme.colorScheme.primary
                    puzzle.sliders.forEach { slider ->
                        val aligned = kotlin.math.abs(slider.value - slider.target) <= slider.tolerance
                        val readoutColor = if (aligned) Color(0xFF81E7B1) else accent
                        val unit = when (slider.unit) {
                            "deg" -> "°"
                            "BPM" -> " BPM"
                            else -> slider.unit.orEmpty()
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(panelColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(slider.label, style = MaterialTheme.typography.labelLarge)
                                Text(when {
                                    aligned -> "In range"
                                    slider.value < slider.target -> "Too low"
                                    else -> "Too high"
                                }, color = readoutColor, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onSliderChange(slider.id, (slider.value - 1f).coerceIn(slider.min, slider.max)) },
                                    enabled = slider.value > slider.min,
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.semantics { contentDescription = "Decrease ${slider.label}" }
                                ) { Text("−") }
                                Text(
                                    text = slider.value.roundToInt().toString() + unit,
                                    color = readoutColor,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                OutlinedButton(
                                    onClick = { onSliderChange(slider.id, (slider.value + 1f).coerceIn(slider.min, slider.max)) },
                                    enabled = slider.value < slider.max,
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.semantics { contentDescription = "Increase ${slider.label}" }
                                ) { Text("+") }
                            }
                            Slider(
                                value = slider.value,
                                onValueChange = { onSliderChange(slider.id, it.roundToInt().toFloat()) },
                                valueRange = slider.min..slider.max,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = readoutColor, activeTrackColor = readoutColor
                                ),
                                modifier = Modifier.semantics { contentDescription = "${slider.label} control" }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (puzzle.presentation !in setOf("counter_tune", "prism_optics")) {
                Button(onClick = onSubmit, enabled = choicePanel == null || puzzle.sliders.all { it.value >= 0f }) {
                    Text(choicePanel?.submitLabel ?: "Engage Handshake")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    onOpenFieldKit: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onToggleTutorials: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleModernFieldMenu: (Boolean) -> Unit = {},
    onToggleHighContrast: (Boolean) -> Unit = {},
    onToggleLargeTouchTargets: (Boolean) -> Unit = {},
    onToggleScreenshakeDisabled: (Boolean) -> Unit = {},
    onToggleFlashesDisabled: (Boolean) -> Unit = {},
    onToggleHapticsDisabled: (Boolean) -> Unit = {},
    onQuickSave: () -> Unit,
    onSaveGame: () -> Unit,
    onLoadGame: () -> Unit,
    onReturnToTitle: (() -> Unit)? = null,
    onShowSkillTree: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    equippedWeapons: Map<String, String>,
    equippedArmors: Map<String, String>,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseInventoryItem: (InventoryPreviewItemUi) -> Unit,
        onShowQuestDetails: (String) -> Unit,
    onShowItemDetails: (InventoryPreviewItemUi) -> Unit,
    craftingViewModel: CraftingViewModel? = null,
    onTinkerTutorialStep: ((com.example.starborn.feature.crafting.TinkeringTutorialStep) -> Unit)? = null,
    onEnvironmentalPreview: (() -> Unit)? = null,
    onDebugTinkeringTutorial: (() -> Unit)? = null,
    onOpenWalkthrough: (() -> Unit)? = null,
    onPlayAudio: (String) -> Unit = {},
    creditsLabel: String,
    isGearTutorialActive: Boolean = false,
    modern: Boolean = false,
    selectedMemberId: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (tab) {
            MenuTab.INVENTORY -> InventoryTabContent(
                inventoryItems = inventoryItems,
                equippedItems = equippedItems,
                completedMilestones = completedMilestones,
                equippedWeapons = equippedWeapons,
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
                onShowItemDetails = onShowItemDetails,
                creditsLabel = creditsLabel,
                isGearTutorialActive = isGearTutorialActive
            )
            MenuTab.FIELD_KIT -> {
                if (craftingViewModel != null) {
                    TinkerTabContent(
                        craftingViewModel = craftingViewModel,
                        onTutorialStepChanged = onTinkerTutorialStep,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onPlayAudio = onPlayAudio
                    )
                } else {
                    FieldKitTabContent(
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onOpenFieldKit = onOpenFieldKit
                    )
                }
            }
            MenuTab.JOURNAL -> JournalTabContent(
                trackedQuest = trackedQuest,
                activeQuests = activeQuests,
                completedQuests = completedQuests,
                accentColor = accentColor,
                borderColor = borderColor,
                onQuestSelected = onShowQuestDetails
            )
            MenuTab.MAP -> MapTabContent(
                objective = (trackedQuest ?: activeQuests.firstOrNull())?.let { it.currentObjective ?: it.stageDescription },
                minimap = minimap,
                fullMap = fullMap,
                isCurrentRoomDark = isCurrentRoomDark,
                accentColor = accentColor,
                borderColor = borderColor,
                onMenuAction = onMenuAction,
                onOpenMapLegend = onOpenMapLegend,
            )
            MenuTab.STATS -> if (modern) {
                val member = partyStatus.members.firstOrNull { it.id == selectedMemberId }
                    ?: partyStatus.members.firstOrNull()
                ModernPartyPage(
                    member = member,
                    onDetails = onShowDetails,
                    onSkills = onShowSkillTree,
                    showEquipmentHint = isGearTutorialActive && member?.id == "nova"
                ) {
                    InventoryEquipmentPreview(
                        inventoryItems = inventoryItems,
                        equippedItems = equippedItems,
                        completedMilestones = completedMilestones,
                        equippedWeapons = equippedWeapons,
                        equippedArmors = equippedArmors,
                        partyMembers = listOfNotNull(member),
                        borderColor = borderColor,
                        accentColor = accentColor,
                        onEquipItem = onEquipItem,
                        onEquipMod = onEquipMod,
                        onEquipWeapon = onEquipWeapon,
                        resolveWeaponItem = resolveWeaponItem,
                        onEquipArmor = onEquipArmor,
                        resolveArmorItem = resolveArmorItem,
                        isGearTutorialActive = isGearTutorialActive && member?.id == "nova"
                    )
                }
            } else {
                StatsTabContent(
                partyStatus = partyStatus,
                accentColor = accentColor,
                borderColor = borderColor,
                onShowSkillTree = onShowSkillTree,
                onShowDetails = onShowDetails
            )
            }
            MenuTab.SETTINGS -> SettingsTabContent(
                settings = settings,
                onToggleModernFieldMenu = onToggleModernFieldMenu,
                accentColor = accentColor,
                borderColor = borderColor,
                onMusicVolumeChange = onMusicVolumeChange,
                onSfxVolumeChange = onSfxVolumeChange,
                onVoiceVolumeChange = onVoiceVolumeChange,
                onToggleTutorials = onToggleTutorials,
                onToggleVignette = onToggleVignette,
                onToggleHighContrast = onToggleHighContrast,
                onToggleLargeTouchTargets = onToggleLargeTouchTargets,
                onToggleScreenshakeDisabled = onToggleScreenshakeDisabled,
                onToggleFlashesDisabled = onToggleFlashesDisabled,
                onToggleHapticsDisabled = onToggleHapticsDisabled,
                onQuickSave = onQuickSave,
                onSaveGame = onSaveGame,
                onLoadGame = onLoadGame,
                onReturnToTitle = onReturnToTitle,
                onEnvironmentalPreview = onEnvironmentalPreview,
                onDebugTinkeringTutorial = onDebugTinkeringTutorial,
                onOpenWalkthrough = onOpenWalkthrough
            )
        }
    }
}

@Composable
private fun SimulationDeckDialog(
    programs: List<com.example.starborn.feature.exploration.presentation.SimulationProgramDefinition>,
    onSelectSimulation: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("TRAINING PROTOCOLS") }

    val allPrograms = programs.map {
        SimulationProgram(it.title, it.category, it.enemyIds, it.description, Color(it.accentArgb))
    }

    val categories = listOf("THE RESONANCE CRUCIBLE", "APEX BOSS ARCHIVES", "TRAINING PROTOCOLS")
    val currentPrograms = remember(selectedCategory, programs) {
        allPrograms.filter { it.category == selectedCategory }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF070F18),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASTRA SIMULATION DECK",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFE0F7FA)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = com.example.starborn.feature.exploration.presentation.AstraCatalog.simulationRules,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0C1B2A),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                text = when (cat) {
                                    "THE RESONANCE CRUCIBLE" -> "CRUCIBLE"
                                    "APEX BOSS ARCHIVES" -> "APEX BOSS"
                                    else -> "TRAINING"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                if (currentPrograms.isEmpty()) Text("More programs unlock as the crew completes campaign encounters.", color = Color.White.copy(alpha = .75f))
                currentPrograms.forEach { program ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectSimulation(program.enemyIds) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0C1B2A),
                        border = BorderStroke(1.dp, program.accentColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = program.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = program.accentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = program.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = program.accentColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, program.accentColor.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = "ENGAGE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = program.accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SimulationProgram(
    val title: String,
    val category: String,
    val enemyIds: List<String>,
    val description: String,
    val accentColor: Color
)

@Composable
private fun AstraNavConsoleDialog(
    routeHint: String,
    destinations: List<com.example.starborn.domain.session.AstraDestination>,
    dockedTitle: String,
    dockedWorldId: String?,
    transitTitle: String?,
    arrivalTitle: String?,
    onDisembark: () -> Unit,
    onSelectDestination: (worldId: String, hubId: String, roomId: String, nodeId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF070F18),
            border = BorderStroke(1.5.dp, Color(0xFFFFB703).copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASTRA NAVIGATION TERMINAL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFFFE082)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = transitTitle == null,
                        modifier = Modifier.size(28.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        transitTitle != null -> "IN TRANSIT\nSetting course for $transitTitle…"
                        arrivalTitle != null -> "ARRIVAL COMPLETE\nDocked at $arrivalTitle. Disembark to explore the regional map, or stay aboard."
                        else -> "Currently docked at: $dockedTitle\nChoose a route and set course. Use the cargo ramp to disembark."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                if (transitTitle != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (arrivalTitle != null) {
                    Button(onClick = onDisembark, modifier = Modifier.fillMaxWidth()) { Text("Disembark") }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Stay aboard") }
                } else destinations.forEach { dest ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = dest.worldId != dockedWorldId) { onSelectDestination(dest.worldId, dest.hubId, dest.roomId, dest.nodeId) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141F2C),
                        border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dest.title.substringAfter(": "),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFD54F)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dest.desc,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFB703).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = if (dest.worldId == dockedWorldId) "DOCKED" else "SET COURSE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
private fun TapeDeckDialog(
    inventory: List<InventoryPreviewItemUi>,
    playingTapeId: String?,
    onPlayTrack: (String, String) -> Unit,
    onStopTrack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ownedTapeIds = remember(inventory) { inventory.map { it.id }.toSet() }

    val tapes = com.example.starborn.feature.exploration.presentation.AstraCatalog.films

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141210),
            border = BorderStroke(1.5.dp, Color(0xFFFFB300).copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "THE GREAT FRONTIER // FILM ARCHIVE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "Recovered: ${tapes.count { it.first in ownedTapeIds }} / 10 Films",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Currently Playing Status Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF211E1A),
                    border = BorderStroke(1.dp, if (playingTapeId != null) Color(0xFFFFB300) else Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (playingTapeId != null) "▶ NOW SCREENING:" else "■ CINEMA STANDBY:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (playingTapeId != null) Color(0xFFFFB300) else Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = tapes.firstOrNull { it.first == playingTapeId }?.third?.first ?: "No film loaded",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (playingTapeId != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF5252).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.7f)),
                                modifier = Modifier.clickable { onStopTrack() }
                            ) {
                                Text(
                                    text = "EJECT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF8A80),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Tape List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tapes) { (tapeId, cueId, info) ->
                        val (title, location) = info
                        val isOwned = tapeId in ownedTapeIds
                        val isPlaying = playingTapeId == tapeId

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isOwned) Modifier.clickable {
                                        if (isPlaying) onStopTrack() else onPlayTrack(tapeId, cueId)
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isPlaying -> Color(0xFF332711)
                                isOwned -> Color(0xFF1E1B17)
                                else -> Color(0xFF161514)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isPlaying -> Color(0xFFFFD54F)
                                    isOwned -> Color(0xFFFFB300).copy(alpha = 0.4f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isOwned) title else "[Unindexed Film Cassette]",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isOwned) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.35f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isOwned) location else "Hidden in: $location",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isOwned) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isOwned) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPlaying) Color(0xFFFFB300).copy(alpha = 0.3f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f))
                                    ) {
                                        Text(
                                            text = if (isPlaying) "STOP" else "WATCH",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFD54F),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "LOCKED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.padding(horizontal = 8.dp)
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
private fun FieldKitTabContent(
    accentColor: Color,
    borderColor: Color,
    onOpenFieldKit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF061018).copy(alpha = 0.44f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Field Tinkering Kit",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Assemble weapon modifications, reinforce suits, and repair scrap components directly in the field.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0AEC0)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onOpenFieldKit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.22f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Tinkering")
            }
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
    if (LocalModernFieldMenu.current) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title !in setOf("Inventory Overview", "Party Status", "Quest Journal", "Tinkering", "Map")) {
                Text(title, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
        return
    }
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

private enum class MapPoiFilter(val label: String) {
    ALL("All"),
    SERVICES("Services"),
    THREATS("Threats"),
    EXITS("Exits")
}

@Composable
internal fun InteractiveMapPanel(
    fullMap: FullMapUiState?,
    onOpenMapLegend: (() -> Unit)? = null
) {
    val cells = fullMap?.cells.orEmpty()
    val mapMaxStepPx = with(LocalDensity.current) { 58.dp.toPx() }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableStateOf(MapPoiFilter.ALL) }
    var viewportScale by remember { mutableFloatStateOf(1f) }
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val selected = cells.firstOrNull { it.roomId == selectedId } ?: cells.firstOrNull { it.isCurrent }

    val visitedCount = remember(cells) { cells.count { it.visited || it.isCurrent } }
    val serviceIds = remember(cells) {
        cells.filter { !it.isDark && !it.isPreview && it.services.any { s -> s != MinimapService.EXIT } }
            .map { it.roomId }.toSet()
    }
    val threatIds = remember(cells) {
        cells.filter { (it.hasEnemies || it.hasNearbyPatrol) && !it.isDark && !it.isPreview }.map { it.roomId }.toSet()
    }
    val exitIds = remember(cells) {
        cells.filter { com.example.starborn.ui.map.mapRoomHasExit(it) }.map { it.roomId }.toSet()
    }
    val highlightedIds: Set<String>? = when (activeFilter) {
        MapPoiFilter.ALL -> null
        MapPoiFilter.SERVICES -> serviceIds
        MapPoiFilter.THREATS -> threatIds
        MapPoiFilter.EXITS -> exitIds
    }

    fun clampOffset(raw: Offset, scale: Float): Offset {
        val maxX = viewportSize.width * scale / 2f
        val maxY = viewportSize.height * scale / 2f
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    fun centerOnRoom(room: MinimapCellUi?) {
        val target = room ?: return
        if (viewportSize.width <= 0 || viewportSize.height <= 0 || cells.isEmpty()) return
        val minX = cells.minOf { it.gridX }; val maxX = cells.maxOf { it.gridX }
        val minY = cells.minOf { it.gridY }; val maxY = cells.maxOf { it.gridY }
        val step = minOf(
            viewportSize.width.toFloat() / (maxX - minX + 3),
            viewportSize.height.toFloat() / (maxY - minY + 3),
            mapMaxStepPx
        ) * viewportScale
        viewportOffset = clampOffset(
            Offset(((minX + maxX) / 2f - target.gridX) * step, (target.gridY - (minY + maxY) / 2f) * step),
            viewportScale
        )
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (cells.isEmpty()) {
            Text(
                "Survey more rooms to reveal the map.",
                style = MaterialTheme.typography.bodySmall,
                color = FieldMenuDesign.textMuted
            )
        } else {
            // POI Filter strip (proportional weights so Services/Threats/Exits never crowd on narrow phones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                MapPoiFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    val count = when (filter) {
                        MapPoiFilter.ALL -> null
                        MapPoiFilter.SERVICES -> serviceIds.size
                        MapPoiFilter.THREATS -> threatIds.size
                        MapPoiFilter.EXITS -> exitIds.size
                    }
                    val accent = when (filter) {
                        MapPoiFilter.THREATS -> MapPalette.threat
                        MapPoiFilter.EXITS -> MapPalette.gateway
                        else -> FieldMenuDesign.gold
                    }
                    val pillWeight = if (filter == MapPoiFilter.ALL) 0.68f else 1.11f
                    Surface(
                        modifier = Modifier
                            .weight(pillWeight)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeFilter = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accent.copy(alpha = 0.18f) else FieldMenuDesign.panel,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accent.copy(alpha = 0.72f) else FieldMenuDesign.subtleBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.heightIn(min = 36.dp).padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color.White else FieldMenuDesign.textMuted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (count != null && count > 0) {
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (isSelected) accent else accent.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Framed tactical blueprint map viewport with floating glass HUD controls
            Surface(
                modifier = Modifier.fillMaxWidth().height(330.dp),
                shape = RoundedCornerShape(16.dp),
                color = MapPalette.background,
                border = BorderStroke(1.dp, MapPalette.gateway.copy(alpha = 0.30f)),
                shadowElevation = 6.dp
            ) {
                Box(Modifier.fillMaxSize()) {
                    AreaMapCanvas(
                        cells = cells,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(cells) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (viewportScale * zoom).coerceIn(0.6f, 3.5f)
                                    viewportScale = nextScale
                                    viewportOffset = clampOffset(viewportOffset + pan, nextScale)
                                }
                            },
                        scale = viewportScale,
                        offset = viewportOffset,
                        selectedId = selected?.roomId,
                        onSelect = { selectedId = it.roomId },
                        highlightedRoomIds = highlightedIds
                    )

                    // Top-left survey badge
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF061018).copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, MapPalette.visited.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "$visitedCount / ${cells.size} rooms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    // Top-right compact compass badge
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        shape = CircleShape,
                        color = Color(0xFF061018).copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, MapPalette.gateway.copy(alpha = 0.40f))
                    ) {
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "N",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                                color = MapPalette.gateway
                            )
                        }
                    }

                    // Bottom-right floating glass control cluster (-, +, Fit, Center on Me)
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapViewportActionButton(
                            icon = Icons.Default.Remove,
                            contentDescription = "Zoom out"
                        ) {
                            val next = (viewportScale / 1.3f).coerceIn(0.6f, 3.5f)
                            viewportScale = next
                            viewportOffset = clampOffset(viewportOffset, next)
                        }
                        MapViewportActionButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Zoom in"
                        ) {
                            val next = (viewportScale * 1.3f).coerceIn(0.6f, 3.5f)
                            viewportScale = next
                            viewportOffset = clampOffset(viewportOffset, next)
                        }
                        MapViewportActionButton(
                            icon = Icons.Default.ZoomOutMap,
                            contentDescription = "Fit map"
                        ) {
                            viewportScale = 1f
                            viewportOffset = Offset.Zero
                        }
                        MapViewportActionButton(
                            icon = Icons.Default.MyLocation,
                            contentDescription = "Center on current room",
                            tint = MapPalette.current
                        ) {
                            val current = cells.firstOrNull { it.isCurrent }
                            selectedId = current?.roomId
                            viewportScale = 1.25f
                            centerOnRoom(current)
                        }
                    }
                }
            }

            // Upgraded Room Inspector Card (with tappable adjacent room chips!)
            selected?.let { room ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = FieldMenuDesign.panel,
                    border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
                ) {
                    MapRoomDetails(
                        cell = room,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        cells = cells,
                        onSelectRoom = { targetId ->
                            selectedId = targetId
                            centerOnRoom(cells.firstOrNull { it.roomId == targetId })
                        }
                    )
                }
            }

            // Compact inline visual key strip + optional Legend button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineMapKeyDot(MapPalette.current, "You")
                    InlineMapKeyDot(MapPalette.gateway, "Exit")
                    InlineMapKeyDot(MapPalette.threat, "Hostiles")
                    InlineMapKeyDot(MapPalette.blocked, "Locked")
                }
                if (onOpenMapLegend != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onOpenMapLegend() },
                        shape = RoundedCornerShape(999.dp),
                        color = FieldMenuDesign.panel,
                        border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Info, null, Modifier.size(13.dp), tint = MapPalette.gateway)
                            Text(
                                "Legend",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapViewportActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White.copy(alpha = 0.9f),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061018).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun InlineMapKeyDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = FieldMenuDesign.textMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun MapLegendOverlay(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color,
    panelColor: Color,
    borderColor: Color,
    textColor: Color,
    embedded: Boolean = false
) {
    Box(modifier = if (embedded) modifier.fillMaxWidth() else modifier.fillMaxSize().background(Color.Black.copy(alpha = .65f)).padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(color = MapPalette.background, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MapPalette.visited.copy(alpha=.4f))) {
            Column(Modifier.widthIn(max=560.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Map Legend", style = MaterialTheme.typography.titleMedium, color = Color.White)
                MapLegend()
                if (!embedded) TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End).heightIn(min=48.dp)) { Text("Close") }
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
    AreaMapCanvas(fullMap.cells, modifier.fillMaxWidth().height(260.dp), scale = scale, offset = offset)
}




@Composable
internal fun MenuToggleButton(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isTutorialBeacon: Boolean = false
) {
    val description = if (isOpen) "Close menu" else "Open menu"
    val baseAlpha = if (isOpen) 0.75f else 1f
    val alpha = if (enabled) baseAlpha else baseAlpha * 0.35f
    val accentColor = Color(0xFF7FE6FF)
    val warmColor = Color(0xFFFFC857)

    val pulseTransition = rememberInfiniteTransition(label = "menu_btn_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val showBeacon = isTutorialBeacon && !isOpen

    Surface(
        modifier = modifier
            .height(54.dp)
            .widthIn(min = 106.dp)
            .semantics { contentDescription = description }
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled, onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(
            if (showBeacon) 1.5.dp else 1.dp,
            if (showBeacon) warmColor.copy(alpha = pulseAlpha) else accentColor.copy(alpha = if (isOpen) 0.86f else 0.48f)
        )
    ) {
        Row(
            modifier = Modifier
                .height(54.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = if (showBeacon) {
                            listOf(
                                warmColor.copy(alpha = 0.22f * pulseAlpha),
                                accentColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                accentColor.copy(alpha = if (isOpen) 0.18f else 0.10f),
                                warmColor.copy(alpha = if (isOpen) 0.08f else 0.04f),
                                Color.Transparent
                            )
                        }
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
                        color = if (showBeacon) warmColor else accentColor,
                        start = Offset(startX, size.height * yFactor),
                        end = Offset(endX, size.height * yFactor),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(
                text = "MENU",
                color = if (showBeacon) warmColor else Color.White.copy(alpha = if (enabled) 0.94f else 0.55f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    }
}

private data class RestRecoveryNotice(val id: Long, val title: String, val detail: String)

@Composable
private fun RestRecoveryOverlay(
    notice: RestRecoveryNotice?,
    accentColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(350, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        val currentNotice = notice ?: return@AnimatedVisibility
        val detailScroll = remember(currentNotice.id) { androidx.compose.foundation.ScrollState(0) }
        val transition = rememberInfiniteTransition(label = "restRecoveryBreath")
        val breath by transition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "restRecoveryScale"
        )
        val glow by transition.animateFloat(
            initialValue = 0.40f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "restRecoveryGlow"
        )
        val sweep by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1400, easing = FastOutSlowInEasing),
            label = "restRecoverySweep"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6020509))
                .clickable { onDismiss() }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xF5060D15),
                border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.52f + glow * 0.22f)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    }
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.22f + glow * 0.14f),
                                    Color(0xFF0F1E2E).copy(alpha = 0.50f),
                                    Color.Transparent
                                ),
                                radius = 700f
                            )
                        )
                        .padding(horizontal = 26.dp, vertical = 28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f + glow * 0.15f),
                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.60f + glow * 0.30f)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Hotel,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer {
                                            scaleX = 0.95f + glow * 0.08f
                                            scaleY = 0.95f + glow * 0.08f
                                            translationY = -1.5f + glow * 2.5f
                                        }
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentNotice.title,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "PARTY VITALS RESTORED",
                                color = accentColor.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = currentNotice.detail,
                            modifier = Modifier.heightIn(max = 220.dp).verticalScroll(detailScroll),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x3310B981),
                                border = BorderStroke(1.dp, Color(0x6610B981))
                            ) {
                                Text(
                                    text = "✦ 100% HP",
                                    color = Color(0xFF34D399),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x3306B6D4),
                                border = BorderStroke(1.dp, Color(0x6606B6D4))
                            ) {
                                Text(
                                    text = "✦ Neural Static Cleared",
                                    color = Color(0xFF22D3EE),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(sweep)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                accentColor.copy(alpha = 0.45f + glow * 0.35f),
                                                Color.White.copy(alpha = 0.35f + glow * 0.25f)
                                            )
                                        )
                                    )
                            )
                        }

                    }
                }
            }
        }
    }
}

private fun String.toActionTitleCase(): String {
    return this.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
}

internal data class QuickMenuAction(
    @DrawableRes val iconRes: Int? = null,
    val imageVector: ImageVector? = null,
    val label: String,
    val detail: String? = null,
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
    accentColor: Color,
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
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF0061018),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUEST JOURNAL",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "$totalCount quests",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Close",
                    color = accentColor.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            QuestJournalToggle(
                current = page,
                onSelect = { page = it },
                accentColor = accentColor,
                borderColor = accentColor.copy(alpha = 0.34f)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (page) {
                    QuestJournalPage.ACTIVE -> {
                        trackedQuest?.let { quest ->
                            item {
                                QuestJournalSectionCard(title = "Tracked Quest") {
                                    QuestJournalRow(
                                        quest = quest,
                                        accentColor = accentColor,
                                        borderColor = accentColor.copy(alpha = 0.34f),
                                        onClick = { onSelectQuest(quest.id) }
                                    )
                                }
                            }
                        } ?: item {
                            QuestJournalSectionCard(title = "Tracked Quest") {
                                Text(
                                    text = "No quest tracked. Select one below.",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (recentLog.isNotEmpty()) {
                            item {
                                QuestJournalSectionCard(title = "Recent Updates") {
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
                                QuestJournalSectionCard(title = "Active Quests") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        otherActive.forEach { quest ->
                                            QuestJournalRow(
                                                quest = quest,
                                                accentColor = accentColor,
                                                borderColor = accentColor.copy(alpha = 0.34f),
                                                onClick = { onSelectQuest(quest.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                QuestJournalSectionCard(title = "Active Quests") {
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
                                QuestJournalSectionCard(title = "Completed Quests") {
                                    com.example.starborn.feature.exploration.ui.tabs.CompletedQuestGroups(
                                        completedQuests, accentColor, accentColor.copy(alpha = .34f), onSelectQuest)

                                }
                            }
                        } else {
                            item {
                                QuestJournalSectionCard(title = "Completed Quests") {
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
                                QuestJournalSectionCard(title = "Failed Quests") {
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
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = Color(0xFF0F1B2A).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${action.label} action"
                onClick(label = action.label) {
                    onClick()
                    true
                }
            }
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServiceActionIcon(
                action = action,
                accentColor = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = action.label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}


private val PresenceChipMinWidth = 100.dp

@Composable
internal fun RoomEntitySection(
    npcs: List<String>,
    npcPresenceNames: Map<String, String>,
    npcPortraitPaths: Map<String, String>,
    npcTradeKeys: Set<String> = emptySet(),
    groundItems: Map<String, Int>,
    serviceActions: List<QuickMenuAction> = emptyList(),
    travelActions: List<Pair<RoomAction, String>> = emptyList(),
    canReturnToHub: Boolean = false,
    gatewayEnabled: Boolean = true,
    sectorTitle: String? = null,
    isDisembarking: Boolean = false,
    onReturnToHub: () -> Unit = {},
    itemDisplayName: (String) -> String,
    itemDetailLabel: (String) -> String?,
    itemIsEquipment: (String) -> Boolean,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    onNpcClick: (String) -> Unit,
    onCollectItem: (String) -> Unit,
    onCollectAll: () -> Unit,
    onAction: (RoomAction) -> Unit,
    onTravel: (RoomAction) -> Unit = onAction,
    hasLargeEnemy: Boolean = false
) {
    val itemEntries = remember(groundItems, itemDisplayName, itemIsEquipment) {
        groundItems.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { itemIsEquipment(it.key) }
                .thenBy { itemDisplayName(it.key).lowercase(Locale.getDefault()) }
        )
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF050A10).copy(alpha = if (isDark) 0.52f else 0.34f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = if (isDark) 0.22f else 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            travelActions.forEach { (action, label) ->
                OverworldGatewayCard(
                    sectorTitle = null, isDark = isDark, enabled = gatewayEnabled,
                    onClick = { onTravel(action) }, routeLabel = label,
                    routeAccent = accentColor
                )
            }
            if (canReturnToHub) {
                OverworldGatewayCard(
                    sectorTitle = sectorTitle,
                    isDisembarking = isDisembarking,
                    isDark = isDark,
                    onClick = onReturnToHub,
                    enabled = gatewayEnabled
                )
            }
            if (npcs.isNotEmpty()) {
                PresenceRailRow {
                    npcs.forEach { npc ->
                        NpcPresenceChip(
                            npc = npc,
                            label = npcPresenceNames[npc.normalizedNpcKey()] ?: npcDisplayLabel(npc),
                            portraitPath = npcPortraitPaths[npc.normalizedNpcKey()],
                            offersTrade = npc.normalizedNpcKey() in npcTradeKeys,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            isDark = isDark,
                            softenEdges = hasLargeEnemy,
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
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF071018).copy(alpha = if (isDark) 0.86f else 0.72f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.62f else 0.50f)),
                            modifier = Modifier
                                .widthIn(min = 94.dp, max = 118.dp)
                                .height(46.dp)
                                .semantics { contentDescription = "Pick up all nearby items" }
                        ) {
                            Box(
                                modifier = Modifier.background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = if (isDark) 0.20f else 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Take all",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = Color(0xFF071018).copy(alpha = if (isDark) 0.86f else 0.72f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.58f else 0.46f)),
        modifier = modifier
            .widthIn(min = PresenceChipMinWidth)
            .heightIn(min = 46.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${action.label} action"
                onClick(label = action.label) {
                    onClick()
                    true
                }
            }
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.18f else 0.14f),
                            borderColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.62f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ServiceActionIcon(
                        action = action,
                        accentColor = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = action.label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                action.detail?.let { detail ->
                    Text(
                        text = detail,
                        color = Color.White.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OverworldGatewayCard(
    sectorTitle: String?,
    isDisembarking: Boolean = false,
    isDark: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    routeLabel: String? = null,
    routeAccent: Color? = null
) {
    val shape = RoundedCornerShape(10.dp)
    val mapCyan = routeAccent ?: Color(0xFF00E5FF)
    val destination = sectorTitle?.takeIf { it.isNotBlank() } ?: "Colony Map"
    val label = routeLabel ?: when {
        isDisembarking -> "Disembark"
        sectorTitle.isNullOrBlank() -> "Colony Map"
        else -> "$destination map"
    }
    val accessibilityLabel = if (isDisembarking) "Disembark from the Astra" else label
    Surface(
        shape = shape,
        color = Color(0xFF060F17).copy(alpha = if (isDark) 0.88f else 0.76f),
        border = BorderStroke(1.dp, mapCyan.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
                if (enabled) onClick(label = label) {
                    onClick()
                    true
                }
            }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            mapCyan.copy(alpha = 0.08f),
                            Color(0xFF061018).copy(alpha = 0.40f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = mapCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, mapCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (routeLabel == null) Icons.Outlined.Map else Icons.Filled.ChevronRight, contentDescription = null,
                            tint = mapCyan.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = label,
                    color = routeAccent ?: Color.White.copy(alpha = 0.94f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                tint = mapCyan.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ServiceActionIcon(
    action: QuickMenuAction,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    action.imageVector?.let { vector ->
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = accentColor,
            modifier = modifier
        )
    } ?: action.iconRes?.let { res ->
        Icon(
            painter = painterResource(id = res),
            contentDescription = null,
            tint = accentColor,
            modifier = modifier
        )
    }
}

@Composable
private fun PresenceRailRow(
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val fadeWidth = 22.dp.toPx()
                if (scrollState.value > 0) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF050A10), Color.Transparent),
                            startX = 0f,
                            endX = fadeWidth
                        ),
                        size = Size(fadeWidth, size.height)
                    )
                }
                if (scrollState.value < scrollState.maxValue) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF050A10)),
                            startX = size.width - fadeWidth,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - fadeWidth, 0f),
                        size = Size(fadeWidth, size.height)
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            content()
        }
        if (scrollState.value < scrollState.maxValue) Text("›",
            color = Color.White, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterEnd).background(Color(0xFF050A10)).padding(horizontal = 3.dp))
    }
}

@Composable
private fun NpcPresenceChip(
    npc: String,
    label: String,
    portraitPath: String?,
    offersTrade: Boolean,
    accentColor: Color,
    borderColor: Color,
    isDark: Boolean,
    softenEdges: Boolean,
    onClick: () -> Unit
) {
    var visibleState by remember(npc) { mutableStateOf(false) }
    val shimmer = remember(npc) { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(npc) {
        visibleState = true
        shimmer.snapTo(0f)
        shimmer.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }

    val animAlpha by animateFloatAsState(
        targetValue = if (visibleState) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "npcChipAlpha"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = if (visibleState) 0f else 6f,
        animationSpec = tween(durationMillis = 320, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "npcChipOffsetY"
    )

    val halo = (kotlin.math.sin(shimmer.value * Math.PI.toFloat())).coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071018).copy(alpha = when {
            isDark -> 0.88f
            softenEdges -> 0.48f
            else -> 0.76f
        }),
        border = BorderStroke(
            1.dp,
            accentColor.copy(alpha = if (isDark) (0.68f + 0.30f * halo).coerceAtMost(1f) else (0.54f + 0.38f * halo).coerceAtMost(1f))
        ),
        modifier = Modifier
            .widthIn(min = PresenceChipMinWidth)
            .heightIn(min = 46.dp)
            .graphicsLayer {
                alpha = animAlpha
                translationY = animOffsetY * density
            }
            .maestroTag("npc-$npc")
            .semantics { contentDescription = "Talk to $label" }
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (softenEdges) Modifier.background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.06f to Color(0xE6071018),
                            0.94f to Color(0xE6071018),
                            1f to Color.Transparent
                        )
                    ) else Modifier
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.22f else 0.18f),
                            accentColor.copy(alpha = (0.28f * halo)),
                            borderColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border((1f + 0.5f * halo).dp, accentColor.copy(alpha = (0.72f + 0.28f * halo).coerceAtMost(1f)), CircleShape)
                    .padding(2.dp)
            ) {
                if (portraitPath.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f + 0.12f * halo)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                } else {
                    Image(
                        painter = rememberAssetPainter(
                            portraitPath,
                            painterResource(R.drawable.inventory_icon)
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.55f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (offersTrade) "Talk • 🛒 Shop" else "Talk",
                    color = if (offersTrade) Color(0xFFFFCA28) else accentColor.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
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
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071018).copy(alpha = if (isDark) 0.84f else 0.70f),
        border = BorderStroke(
            1.dp,
            if (isEquipment) accentColor.copy(alpha = if (isDark) 0.62f else 0.50f) else borderColor.copy(alpha = if (isDark) 0.38f else 0.28f)
        ),
        modifier = Modifier
            .widthIn(min = PresenceChipMinWidth)
            .heightIn(min = 46.dp)
            .semantics {
                contentDescription = if (quantity > 1) {
                    "Pick up $quantity $name"
                } else {
                    "Pick up $name"
                }
            }
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isEquipment) accentColor.copy(alpha = if (isDark) 0.20f else 0.15f) else borderColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isEquipment) accentColor.copy(alpha = 0.17f) else Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, if (isEquipment) accentColor.copy(alpha = 0.62f) else borderColor.copy(alpha = 0.32f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(roomPresenceItemIconRes(detail, isEquipment)),
                        contentDescription = null,
                        tint = if (isEquipment) accentColor.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    is RestStopAction -> "Rest"
    is TuningPuzzleAction -> "Tune"
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
    enemyAggression: EnemyAggressionState?,
    accentColor: Color,
    isDark: Boolean,
    roomId: String?,
    onPartyClick: (String) -> Unit
) {
    if (visualParties.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth()) {
        val stageWidth = maxWidth
        val baseMemberSize = when {
            stageWidth < 380.dp -> 160.dp
            stageWidth < 520.dp -> 185.dp
            else -> 210.dp
        }
        val soloBossParty = visualParties.singleOrNull()?.takeIf { party ->
            party.enemies.any { enemyPresentationTier(enemyTiers[it]) == EnemyPresentationTier.BOSS }
        }
        val largeBossSize = if (soloBossParty != null) {
            minOf(stageWidth * if (soloBossParty.enemies.size == 1) 0.78f else 0.70f, 320.dp)
        } else {
            baseMemberSize
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

        val singlePartyCentered = orderedParties.size == 1
        val placements = orderedParties.mapNotNull { party ->
            slotAssignments[party.id]?.let { slotIndex ->
                val slot = if (party == soloBossParty || singlePartyCentered) EnemyPartySlot(0.5f, row = 0)
                    else ENEMY_PARTY_SLOTS[slotIndex]
                Triple(party, slot, slotIndex)
            }
        }
        val stageHeight = maxOf(baseMemberSize + laneRise, largeBossSize) + 28.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            val renderPlacements = placements.sortedWith(
                compareBy<Triple<VisualEnemyParty, EnemyPartySlot, Int>> { (party, _, _) ->
                    if (party.enteringFrom != null || party.leavingTo != null) 0 else 1
                }.thenBy { (_, _, slotIndex) -> slotIndex }
            )
            renderPlacements.forEach { (party, slot, slotIndex) ->
                val memberSize = if (party == soloBossParty) largeBossSize else baseMemberSize
                val width = if (party == soloBossParty) memberSize else clusterWidth(party.enemies, memberSize)
                val centerX = stageWidth * slot.centerFraction
                val xOffset = centerX - (width / 2f)
                val yOffset = -laneRise * slot.row
                val offscreenPadding = 72.dp
                fun transitionOffsetX(direction: String?): Float = when (direction) {
                    "east" -> (stageWidth - xOffset + offscreenPadding).value
                    "west" -> -(xOffset + width + offscreenPadding).value
                    else -> 0f
                }
                fun transitionOffsetY(direction: String?): Float = when (direction) {
                    "north" -> -(stageHeight + yOffset + offscreenPadding).value
                    "south" -> (memberSize + 28.dp - yOffset + offscreenPadding).value
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
                val partyAggression = enemyAggression?.takeIf { agg ->
                    party.id == "moving_${agg.partyId}" || party.id == agg.partyId
                }
                EnemyPartyCluster(
                    party = if (party == soloBossParty) party.copy(enemies = party.enemies.sortedByDescending {
                        enemyPresentationTier(enemyTiers[it]) == EnemyPresentationTier.BOSS
                    }) else party,
                    centerBoss = party == soloBossParty,
                    enemyTiers = enemyTiers,
                    enemyIcons = enemyIcons,
                    partyAggression = partyAggression,
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
    enemyAggression: EnemyAggressionState?,
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
                enemyAggression = enemyAggression,
                accentColor = accentColor,
                isDark = isDark,
                roomId = roomId,
                onPartyClick = onPartyClick
            )
        }
    }
}

private val EnemyPresenceShadowDrop = 0.dp

@Composable
private fun EnemyPartyCluster(
    party: VisualEnemyParty,
    centerBoss: Boolean = false,
    enemyTiers: Map<String, String>,
    enemyIcons: Map<String, EnemyIconUi>,
    partyAggression: EnemyAggressionState? = null,
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
    val rawUrgency = partyAggression?.let { agg ->
        val total = agg.totalMs.coerceAtLeast(1L).toFloat()
        (1f - agg.remainingMs.toFloat() / total).coerceIn(0f, 1f)
    } ?: 0f
    val aggressionUrgency by animateFloatAsState(
        targetValue = rawUrgency,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "partyAggroUrgency-${party.id}"
    )
    val motionCycle = if (transitionActive) {
        0f
    } else {
        val motion = rememberInfiniteTransition(label = "hostilesMotion-${party.id}")
        val cycle by motion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (partyAggression != null) 2_600 else 6_400,
                    easing = LinearEasing
                ),
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
                        animX.animateTo(0f, animationSpec = tween(460, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                    }
                    launch {
                        animY.animateTo(0f, animationSpec = tween(460, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                    }
                }
            }
            else -> {
                animAlpha.snapTo(1f)
                animX.snapTo(0f)
                animY.snapTo(0f)
            }
        }
    }

    val aggroSwell = 1f + aggressionUrgency * 0.09f
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = animX.value * density.density
                translationY = animY.value * density.density
                scaleX = aggroSwell
                scaleY = aggroSwell
                alpha = animAlpha.value
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val memberCount = party.enemies.size
        val spreadFactor = (1f - overlapFraction).coerceIn(0.42f, 0.72f)
        party.enemies.forEachIndexed { index, enemyId ->
            val step = (index + 1) / 2
            val isLeft = index % 2 == 1
            val offsetX = when {
                centerBoss && index == 0 -> 0.dp
                centerBoss -> memberSize * (if (isLeft) 0.38f else -0.38f) * step
                memberCount == 2 -> memberSize * (if (index == 0) -0.22f else 0.26f)
                index == 0 -> 0.dp
                else -> memberSize * (if (isLeft) -0.36f else 0.36f) * (spreadFactor / 0.64f) * step
            }
            val offsetY = when {
                centerBoss || index == 0 -> 0.dp
                memberCount == 2 -> (-12).dp
                else -> (-14).dp * step
            }
            val scale = when {
                centerBoss || index == 0 -> 1f
                memberCount == 2 -> 0.87f
                else -> (1f - 0.12f * step).coerceAtLeast(0.76f)
            }

            EnemyPartyStandee(
                enemyId = enemyId,
                instanceKey = "${party.id}-$enemyId-$index",
                tier = enemyPresentationTier(enemyTiers[enemyId]),
                motionCycle = motionCycle,
                accentColor = accentColor,
                isDark = isDark,
                transitionActive = transitionActive,
                icon = enemyIcons[enemyId],
                size = if (centerBoss && index > 0) memberSize * 0.52f else memberSize,
                showFightBadge = (index == 0),
                squadSize = memberCount,
                isAggressive = party.isAggressive,
                aggressionUrgency = if (index == 0) aggressionUrgency else 0f,
                modifier = Modifier
                    .zIndex(if (centerBoss) index.toFloat() else (party.enemies.size - index).toFloat())
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
    tier: EnemyPresentationTier,
    motionCycle: Float,
    accentColor: Color,
    isDark: Boolean,
    transitionActive: Boolean,
    icon: EnemyIconUi?,
    size: Dp,
    showFightBadge: Boolean = true,
    squadSize: Int = 1,
    isAggressive: Boolean = false,
    aggressionUrgency: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember(instanceKey) { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "standeePress-$instanceKey"
    )
    val aggressivePulse = if ((isAggressive || aggressionUrgency > 0f) && !transitionActive) {
        val pulseTransition = rememberInfiniteTransition(label = "aggroPulse-$instanceKey")
        val pulse by pulseTransition.animateFloat(
            initialValue = 0.50f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (aggressionUrgency > 0.6f) 340 else 760,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aggroPulseAlpha-$instanceKey"
        )
        pulse
    } else {
        1f
    }

    val displayName = icon?.displayName?.takeIf { it.isNotBlank() } ?: enemyDisplayLabel(enemyId)
    val tierAccent = enemyTierAccent(tier, accentColor)
    val borderColor = when {
        aggressionUrgency > 0f -> lerpColor(Color(0xFFF59E0B), Color(0xFFEF4444), aggressionUrgency).copy(alpha = aggressivePulse)
        isAggressive -> Color(0xFFFF6B6B).copy(alpha = aggressivePulse)
        tier == EnemyPresentationTier.BOSS -> Color(0xFFFFD54F).copy(alpha = 0.92f)
        tier == EnemyPresentationTier.ELITE -> Color(0xFFFFB74D).copy(alpha = 0.88f)
        else -> Color(0xFFEF4444).copy(alpha = 0.72f)
    }
    val pillBg = when {
        aggressionUrgency > 0f -> Color(0xEC2E0F14)
        isAggressive -> Color(0xE8281115)
        tier == EnemyPresentationTier.BOSS -> Color(0xEC1B1508)
        else -> Color(0xE011161F)
    }

    Box(
        modifier = modifier
            .width(size)
            .height(size + 28.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .maestroTag("engage-$enemyId")
            .semantics { contentDescription = "Engage $displayName" }
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
            accentColor = if (isAggressive || aggressionUrgency > 0f) Color(0xFFFF3B30) else accentColor,
            isDark = isDark,
            transitionActive = transitionActive,
            icon = icon,
            iconSize = size,
            aggressionUrgency = aggressionUrgency,
            aggressionPulse = aggressivePulse
        )
        if (!transitionActive && showFightBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp),
                shape = RoundedCornerShape(999.dp),
                color = pillBg,
                border = BorderStroke(if (isAggressive || tier != EnemyPresentationTier.STANDARD) 1.2.dp else 1.dp, borderColor),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        isAggressive || aggressionUrgency > 0f -> Text(
                            text = "⚠",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (aggressionUrgency > 0.5f) Color(0xFFFF6B6B) else Color(0xFFFFD591)
                        )
                        tier == EnemyPresentationTier.BOSS -> Text(
                            text = "☠",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = tierAccent
                        )
                        tier == EnemyPresentationTier.ELITE -> Text(
                            text = "★",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = tierAccent
                        )
                    }
                    Text(
                        text = displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color(0xFFFDF8F4)
                    )
                    if (squadSize > 1) {
                        Text(
                            text = "+${squadSize - 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color(0xFFFFC1A6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyPartyLeaderIcon(
    enemyId: String,
    instanceKey: String,
    tier: EnemyPresentationTier,
    motionCycle: Float,
    accentColor: Color,
    isDark: Boolean,
    transitionActive: Boolean,
    icon: EnemyIconUi?,
    iconSize: Dp = 80.dp,
    aggressionUrgency: Float = 0f,
    aggressionPulse: Float = 1f
) {
    val iconPath = icon?.spritePath ?: remember(enemyId) { "images/enemies/${enemyId}_combat.png" }
    val painter = rememberAssetPainter(iconPath, ColorPainter(Color.Transparent), async = transitionActive)
    val density = LocalDensity.current
    val phaseOffset = remember(instanceKey) {
        (abs(instanceKey.lowercase(Locale.getDefault()).hashCode()) % 1000) / 1000f
    }
    val t = (motionCycle + phaseOffset) % 1f
    val wave = if (transitionActive) 0f else kotlin.math.sin(t * Math.PI * 2.0).toFloat()
    val bobAmplitude = when {
        aggressionUrgency > 0f -> 2.8.dp
        isDark -> 1.2.dp
        else -> 2.0.dp
    }
    val bobPx = with(density) { bobAmplitude.toPx() } * wave
    val scale = 1f + (if (isDark) 0.012f else 0.02f) * wave
    val spriteScale = explorationEnemySpriteScale(tier)
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        if (!transitionActive) {
            EnemyPresenceShadow(
                isDark = isDark,
                threatRingColor = if (aggressionUrgency > 0f) {
                    lerpColor(Color(0xFFF59E0B), Color(0xFFEF4444), aggressionUrgency)
                        .copy(alpha = (0.35f + 0.55f * aggressionUrgency) * aggressionPulse)
                } else null,
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
        }
    }
}

@Composable
private fun EnemyTierBadge(
    tier: EnemyPresentationTier,
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
            EnemyPresentationTier.ELITE -> {
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
            EnemyPresentationTier.BOSS -> {
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
            EnemyPresentationTier.STANDARD -> Unit
        }
    }
}

@Composable
private fun EnemyPresenceShadow(
    isDark: Boolean,
    threatRingColor: Color? = null,
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
        if (threatRingColor != null) {
            drawOval(
                color = threatRingColor,
                topLeft = Offset(size.width * 0.03f, size.height * 0.06f),
                size = Size(size.width * 0.94f, size.height * 0.88f),
                style = Stroke(width = size.height * 0.14f)
            )
        }
    }
}

private fun enemyTierAccent(tier: EnemyPresentationTier, accentColor: Color): Color = when (tier) {
    EnemyPresentationTier.BOSS -> Color(0xFFFFD54F)
    EnemyPresentationTier.ELITE -> Color(0xFFFFB74D)
    EnemyPresentationTier.STANDARD -> accentColor
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
                painter = rememberAssetPainter(part.spritePath, ColorPainter(Color.Transparent), async = true),
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
                        text = quest.stageDescription.orEmpty(),
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF061018).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title.uppercase(Locale.getDefault()),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun DarkRoomAtmosphereOverlay(
    darkness: Float,
    modifier: Modifier = Modifier
) {
    val clampedDarkness = darkness.coerceIn(0f, 1f)
    val scanlineSpacing = with(LocalDensity.current) { 7.dp.toPx() }
    val scanlineStroke = with(LocalDensity.current) { 1.dp.toPx() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val longestSide = max(width, height)
        val shortestSide = min(width, height)
        val center = Offset(width * 0.5f, height * 0.42f)

        drawRect(
            color = Color(0xFF02050A).copy(alpha = 0.62f * clampedDarkness)
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF02050A).copy(alpha = 0.80f * clampedDarkness)
                ),
                center = center,
                radius = longestSide * 0.74f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3C6A88).copy(alpha = 0.10f * clampedDarkness),
                    Color.Transparent
                ),
                center = Offset(width * 0.52f, height * 0.34f),
                radius = shortestSide * 0.52f
            ),
            radius = shortestSide * 0.52f,
            center = Offset(width * 0.52f, height * 0.34f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFA44A).copy(alpha = 0.08f * clampedDarkness),
                    Color.Transparent
                ),
                center = Offset(width * 0.58f, height * 0.38f),
                radius = shortestSide * 0.28f
            ),
            radius = shortestSide * 0.28f,
            center = Offset(width * 0.58f, height * 0.38f)
        )

        var y = 0f
        while (y < height) {
            drawLine(
                color = Color.White.copy(alpha = 0.025f * clampedDarkness),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = scanlineStroke
            )
            y += scanlineSpacing
        }
    }
}

@Composable
fun TutorialBanner(
    prompt: com.example.starborn.domain.tutorial.TutorialEntry,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPrimaryAction: (() -> Unit)? = null
) {
    val resolvedTitle = prompt.formattedTitle()
    PromptBanner(
        title = resolvedTitle,
        message = prompt.message,
        accentColor = Color(0xFF7BE8FF),
        actionLabel = prompt.metadata["action_label"] ?: "Continue",
        onAction = onPrimaryAction ?: onDismiss,
        modifier = modifier,
        tapToDismiss = onPrimaryAction == null,
        secondaryActionLabel = if (onPrimaryAction != null) prompt.metadata["dismiss_label"] ?: "Later" else null,
        onSecondaryAction = onDismiss,
        primaryTag = if (onPrimaryAction == null) "prompt-continue" else "tutorial-action"
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
        actionLabel = "Dismiss",
        onAction = onDismiss,
        modifier = modifier,
        showTitle = false,
        tapToDismiss = true
    )
}

private fun itemCategoryAccentColor(category: String?): Color {
    val norm = category?.lowercase(Locale.getDefault())?.trim().orEmpty()
    return when {
        norm.contains("schematic") -> Color(0xFF7BE8FF)
        norm.contains("key") || norm.contains("relic") || norm.contains("core") || norm.contains("quest") || norm.contains("currency") || norm.contains("credit") -> Color(0xFFFFD54F) // Golden Amber
        norm.contains("weapon") || norm.contains("gun") || norm.contains("sword") || norm.contains("armor") || norm.contains("gear") || norm.contains("accessory") -> Color(0xFF7BE8FF) // Cyber Cyan
        norm.contains("consumable") || norm.contains("food") || norm.contains("ration") || norm.contains("heal") || norm.contains("stim") || norm.contains("firstaid") -> Color(0xFF81C784) // Medical Jade
        norm.contains("void") || norm.contains("erosion") || norm.contains("anomaly") -> Color(0xFFCE93D8) // Void Violet
        norm.contains("material") || norm.contains("ingredient") || norm.contains("scrap") || norm.contains("ore") || norm.contains("fish") -> Color(0xFFB0BEC5) // Industrial Steel Slate
        else -> Color(0xFFA5D6A7)
    }
}

@Composable
fun ItemGrantedBanner(
    prompt: com.example.starborn.domain.prompt.ItemGrantedPrompt,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(prompt.category, prompt.itemId) { itemCategoryAccentColor(prompt.category) }
    val shape = RoundedCornerShape(18.dp)
    val category = prompt.category
        ?.replace('_', ' ')
        ?.replace('-', ' ')
        ?.trim()
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?.takeIf { it.isNotBlank() }
        ?: "Item"
    val itemLabel = if (prompt.quantity > 1) "${prompt.itemName} ×${prompt.quantity}" else prompt.itemName
    val hasSequence = prompt.sequenceTotal > 1 && !prompt.sequenceId.isNullOrBlank()
    val iconPainter = rememberItemIconPainter(prompt.itemId, fallbackType = prompt.category)

    Surface(
        onClick = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(min = 300.dp, max = 560.dp)
            .maestroTag("item-granted-continue")
            .semantics {
                contentDescription = prompt.acquisitionDescription
            },
        color = Color(0xFF060B13).copy(alpha = 0.98f),
        contentColor = Color.White,
        shape = shape,
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.60f)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color(0xFF060B13)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = prompt.announcementHeading,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                ) {
                    if (prompt.category == "arcade_schematic") {
                        Icon(Icons.Default.VideogameAsset, null, Modifier.padding(10.dp), tint = accentColor)
                    } else {
                    Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = itemLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.90f),
                        fontWeight = FontWeight.SemiBold
                    )
                    prompt.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = if (prompt.category == "arcade_schematic") Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemBatchGrantedBanner(
    summary: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PromptBanner(
        title = "Items Obtained",
        message = "Acquired $summary",
        accentColor = Color(0xFF7BE8FF),
        actionLabel = "Dismiss",
        onAction = onDismiss,
        modifier = modifier,
        showTitle = false,
        tapToDismiss = true
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
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    tapToDismiss: Boolean = false,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    primaryTag: String = "prompt-continue"
) {
    val shape = RoundedCornerShape(if (showTitle) 24.dp else 18.dp)
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
            .fillMaxWidth()
            .padding(horizontal = if (showTitle) 8.dp else 16.dp)
            .widthIn(min = if (showTitle) 300.dp else 260.dp, max = if (showTitle) 620.dp else 520.dp),
        color = Color.Transparent,
        shape = shape,
        shadowElevation = if (showTitle) 8.dp else 12.dp
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .clickable(
                    enabled = tapToDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAction
                )
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            backgroundColor.copy(alpha = 0.96f),
                            backgroundColor.copy(alpha = 0.92f),
                            if (showTitle) backgroundColor.copy(alpha = 0.96f) else accentColor.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(if (showTitle) 1.dp else 1.dp, accentColor.copy(alpha = if (showTitle) 0.50f else 0.42f), shape)
        ) {
            if (showTitle) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.55f),
                                            accentColor.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = accentColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
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
                    }
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (secondaryActionLabel != null && onSecondaryAction != null) {
                            TextButton(onClick = onSecondaryAction, modifier = Modifier.maestroTag("tutorial-dismiss")) { Text(secondaryActionLabel, color = accentColor) }
                        }
                        Button(
                            onClick = onAction,
                            modifier = Modifier.maestroTag(primaryTag),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.Black.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = actionLabel.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.42f),
                                        accentColor.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onAction, modifier = Modifier.maestroTag(primaryTag)) {
                        Text(
                            text = actionLabel.uppercase(Locale.getDefault()),
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                    onClick = {
                        if (prompt.tapToDismiss) onDismiss()
                    }
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
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null
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
            title?.let { Text(it, color = accentColor, style = MaterialTheme.typography.titleMedium) }
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
            if (actions != null) actions() else if (!prompt.tapToDismiss) TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .maestroTag("narration-continue")
                    .align(Alignment.End)
                    .defaultMinSize(minWidth = 92.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text(
                    text = "Continue",
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
        onClick = onDismiss,
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
            Button(onClick = onDismiss, modifier = Modifier.maestroTag("level-up-continue").align(Alignment.End)) { Text("Continue") }
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
        onClick = onDismiss,
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
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
        }
    }
}

@Composable
fun CinematicOverlayHost(
    state: CinematicUiState?,
    onAdvance: () -> Unit,
    audioCuePlayer: AudioCuePlayer? = null,
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
                audioCuePlayer = audioCuePlayer,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CinematicOverlay(
    state: CinematicUiState,
    onAdvance: () -> Unit,
    onSkip: () -> Unit = {},
    audioCuePlayer: AudioCuePlayer? = null,
    modifier: Modifier = Modifier
) {
    if (state.presentation == CinematicPresentation.ILLUSTRATED) {
        IllustratedCinematicOverlay(
            state = state,
            onAdvance = onAdvance,
            onSkip = onSkip,
            audioCuePlayer = audioCuePlayer,
            modifier = modifier
        )
        return
    }

    val advanceOnce = rememberCinematicAdvance(state.sceneId, state.stepIndex, onAdvance)
    val stepKey = remember(state.sceneId, state.stepIndex) { "${state.sceneId}_${state.stepIndex}" }
    LaunchedEffect(stepKey, audioCuePlayer) {
        state.step.audioCue?.takeIf { it.isNotBlank() }?.let { cue ->
            val isHeavyImpact = cue == "sfx_intro_beast_strike" ||
                cue == "sfx_intro_door_collapse" ||
                cue == "sfx_combat_plasma_arc" ||
                cue == "sfx_distant_gate_rumble" ||
                cue == "wpn_orion_bowl_resonance" ||
                cue == "sfx_door_airlock_close" ||
                cue == "sfx_pod_launch" ||
                cue == "sfx_combat_shield_deflect" ||
                cue == "sfx_crash_impact" ||
                cue == "sfx_w2_astra_launch" ||
                cue == "sfx_w2_stasis_release" ||
                cue == "sfx_security_lockdown" ||
                cue == "sfx_w3_shield_anchor"
            audioCuePlayer?.execute(
                listOf(
                    AudioCommand.Play(
                        type = AudioCueType.UI,
                        cueId = cue,
                        loop = false,
                        fadeMs = 0L,
                        triggerHaptic = isHeavyImpact
                    )
                )
            )
        }
    }

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
                .background(
                    if (state.backdrop == CinematicBackdrop.BLACK) Color.Black
                    else Color.Black.copy(alpha = 0.72f)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            DialogueOverlay(
                dialogue = dialogueUi,
                choices = emptyList(),
                onAdvance = advanceOnce,
                onChoice = { advanceOnce() },
                onPlayVoice = {},
                onPlayMurmur = { cue, pitch ->
                    audioCuePlayer?.execute(
                        listOf(
                            AudioCommand.Play(
                                AudioCueType.VOICE,
                                cue,
                                loop = false,
                                fadeMs = 0L,
                                gain = 0.50f,
                                pitch = pitch
                            )
                        )
                    )
                },
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
            if (revealFinished) return@LaunchedEffect
            revealedCount = i
            delay(28L)
        }
        revealFinished = true
    }

    val displayedText = if (revealFinished) fullText else fullText.substring(0, revealedCount)

    // Scrim fade-in
    val scrimAlpha by animateFloatAsState(
        targetValue = if (state.backdrop == CinematicBackdrop.BLACK) 1f else 0.72f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cinematicScrimAlpha"
    )

    val tapInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .maestroTag("cinematic-advance")
            .clickable(
                interactionSource = tapInteraction,
                indication = null
            ) {
                if (revealFinished) {
                    advanceOnce()
                } else {
                    revealedCount = fullText.length
                    revealFinished = true
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        CinematicNarrationCard(
            fullText = fullText,
            displayedText = displayedText,
            revealFinished = revealFinished,
            isLastStep = state.stepIndex + 1 >= state.stepCount,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 54.dp)
        )
    }
}

@Composable
private fun CinematicNarrationCard(
    fullText: String,
    displayedText: String,
    revealFinished: Boolean,
    isLastStep: Boolean,
    showTapToContinue: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowAlpha by rememberInfiniteTransition(label = "cinematicGlow").animateFloat(
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
    val narrationStyle = MaterialTheme.typography.bodyLarge.copy(
        fontStyle = FontStyle.Italic,
        lineHeight = 28.sp,
        letterSpacing = 0.3.sp
    )

    Box(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(0.88f)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = glowAlpha }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
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
                CinematicAccentBar(accentColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fullText,
                        color = Color.Transparent,
                        style = narrationStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics { }
                    )
                    Text(
                        text = typewriterText(fullText, displayedText.length),
                        color = Color.White.copy(alpha = 0.92f),
                        style = narrationStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CinematicAccentBar(accentColor)
            }
        }
    }
}

@Composable
private fun CinematicAccentBar(accentColor: Color) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0f),
                        accentColor.copy(alpha = 0.7f),
                        accentColor.copy(alpha = 0f)
                    )
                )
            )
    )
}

/** The last frame drawn by [IllustratedCinematicOverlay], held so the next step can dissolve over it. */
private data class RenderedCinematicFrame(
    val imagePath: String?,
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val alpha: Float = 1f
)

@Composable
internal fun IllustratedCinematicOverlay(
    state: CinematicUiState,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    audioCuePlayer: AudioCuePlayer?,
    modifier: Modifier = Modifier,
    showDebugHud: Boolean = false
) {
    val stepKey = "${state.sceneId}_${state.stepIndex}"
    val advanceOnce = rememberCinematicAdvance(state.sceneId, state.stepIndex, onAdvance)
    // Capture the previous step once, before updating its replacement's visible transform.
    val lastRenderedFrame = remember(state.sceneId) { mutableStateOf<RenderedCinematicFrame?>(null) }
    val outgoing = remember(stepKey) { lastRenderedFrame.value }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val durationMs = ((state.step.durationSeconds ?: 4.0) * 1000.0).toLong().coerceAtLeast(500L)
    val waitsForTextReveal = state.step.captionStyle == CinematicCaptionStyle.DIALOGUE ||
        state.step.captionStyle == CinematicCaptionStyle.NARRATION
    var captionRevealFinished by remember(stepKey) { mutableStateOf(!waitsForTextReveal) }
    var revealAllRequest by remember(stepKey) { mutableIntStateOf(0) }

    state.preloadImages.forEach { imagePath ->
        rememberAssetPainter(imagePath, fallback = ColorPainter(Color.Black), async = true)
    }

    DisposableEffect(state.sceneId, state.ambientCue, audioCuePlayer) {
        val cue = state.ambientCue
        if (!cue.isNullOrBlank()) {
            audioCuePlayer?.execute(
                listOf(AudioCommand.Play(AudioCueType.AMBIENT, cue, loop = true, fadeMs = 350L))
            )
        }
        onDispose {
            if (!cue.isNullOrBlank()) {
                audioCuePlayer?.execute(
                    listOf(AudioCommand.Stop(AudioCueType.AMBIENT, cue, fadeMs = 450L))
                )
            }
        }
    }

    LaunchedEffect(stepKey, audioCuePlayer) {
        state.step.audioCue?.takeIf { it.isNotBlank() }?.let { cue ->
            val isHeavyImpact = cue == "sfx_intro_beast_strike" ||
                cue == "sfx_intro_door_collapse" ||
                cue == "sfx_combat_plasma_arc" ||
                cue == "sfx_distant_gate_rumble" ||
                cue == "wpn_orion_bowl_resonance" ||
                cue == "sfx_door_airlock_close" ||
                cue == "sfx_pod_launch" ||
                cue == "sfx_combat_shield_deflect" ||
                cue == "sfx_crash_impact" ||
                cue == "sfx_w2_astra_launch" ||
                cue == "sfx_w2_stasis_release" ||
                cue == "sfx_security_lockdown" ||
                cue == "sfx_w3_shield_anchor"
            audioCuePlayer?.execute(
                listOf(
                    AudioCommand.Play(
                        type = AudioCueType.UI,
                        cueId = cue,
                        loop = false,
                        fadeMs = 0L,
                        triggerHaptic = isHeavyImpact
                    )
                )
            )
        }
        state.step.voiceCue?.takeIf { it.isNotBlank() }?.let { cue ->
            audioCuePlayer?.execute(
                listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L))
            )
        }
    }

    // Dims the frame over the tail of the step. Steps otherwise only fade in, so the
    // last step of a scene would pop off in a single frame.
    val fadeOutMs = ((state.step.fadeOutSeconds ?: 0.0) * 1000.0)
        .toLong()
        .coerceIn(0L, durationMs)
    var contentDimmed by remember(stepKey) { mutableStateOf(false) }

    LaunchedEffect(stepKey, durationMs, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay((durationMs - fadeOutMs).coerceAtLeast(0L))
            if (fadeOutMs > 0L) {
                contentDimmed = true
                delay(fadeOutMs)
            }
            advanceOnce()
        }
    }

    if (
        state.step.captionStyle == CinematicCaptionStyle.NONE &&
        state.step.imagePath.isNullOrBlank() &&
        state.backdrop == CinematicBackdrop.ROOM
    ) {
        val reveal = remember(stepKey) { CoreAnimatable(1f) }
        LaunchedEffect(stepKey) {
            reveal.animateTo(0f, animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing))
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = reveal.value))
                .semantics { contentDescription = "Room fading in" }
        )
        return
    }

    val painter = rememberAssetPainter(
        state.step.imagePath,
        fallback = ColorPainter(Color.Black),
        async = false
    )
    val motion = remember(stepKey) { CoreAnimatable(0f) }
    LaunchedEffect(stepKey, durationMs) {
        motion.animateTo(1f, animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing))
    }
    val progress = motion.value
    val legacyStartScale = when (state.step.cameraMotion) {
        CinematicCameraMotion.SLOW_PUSH -> 1.02f
        CinematicCameraMotion.DRIFT_LEFT,
        CinematicCameraMotion.DRIFT_RIGHT -> 1.05f
        CinematicCameraMotion.NONE -> 1f
    }
    val legacyEndScale = when (state.step.cameraMotion) {
        CinematicCameraMotion.SLOW_PUSH -> 1.08f
        CinematicCameraMotion.DRIFT_LEFT,
        CinematicCameraMotion.DRIFT_RIGHT -> 1.05f
        CinematicCameraMotion.NONE -> 1f
    }
    val legacyEndX = when (state.step.cameraMotion) {
        CinematicCameraMotion.DRIFT_LEFT -> -32f
        CinematicCameraMotion.DRIFT_RIGHT -> 32f
        else -> 0f
    }
    // Enforce a minimum safe bleed scale (1.06f) so camera drift Y and screen shake
    // never pull the cropped image edge away from the top or bottom on tall aspect ratio displays.
    val density = LocalDensity.current
    val endX = with(density) { (state.step.cameraEndX?.toFloat() ?: legacyEndX).dp.toPx() }
    val endY = with(density) { (state.step.cameraEndY ?: 0.0).toFloat().dp.toPx() }
    // Consecutive captions on the same artwork should continue the camera move
    // from the visible framing, even when the player advances early.
    val continuousShot = outgoing?.takeIf {
        state.sceneId != "intro_prologue" && it.imagePath != null && it.imagePath == state.step.imagePath
    }
    val safeBleed = if (state.step.imagePath?.contains("title_card") == true) 1.0f else 1.06f
    val startScale = continuousShot?.let { it.scale / safeBleed }
        ?: state.step.cameraStartScale?.toFloat()
        ?: legacyStartScale
    val endScale = state.step.cameraEndScale?.toFloat() ?: legacyEndScale
    val startX = continuousShot?.translationX ?: with(density) { (state.step.cameraStartX ?: 0.0).toFloat().dp.toPx() }
    val startY = continuousShot?.translationY ?: with(density) { (state.step.cameraStartY ?: 0.0).toFloat().dp.toPx() }
    val scale = lerp(startScale, endScale, progress) * safeBleed
    val driftX = lerp(startX, endX, progress)
    val driftY = lerp(startY, endY, progress)
    // Keep authored cuts between shots. During a dissolve, draw the outgoing frame
    // at full opacity beneath the incoming one so black doesn't show through.
    val transitionMs = if (continuousShot != null || state.step.transition == CinematicTransition.CUT) 0 else 520
    val enter = remember(stepKey) { CoreAnimatable(if (transitionMs == 0) 1f else 0f) }
    var advanceRequested by remember(stepKey) { mutableStateOf(false) }
    // A second tap may come in mid-dissolve. Finish that blend, then advance once.
    val requestAdvance: () -> Unit = {
        if (enter.value >= 1f) advanceOnce() else advanceRequested = true
    }
    LaunchedEffect(stepKey) {
        if (transitionMs > 0) {
            enter.animateTo(1f, animationSpec = tween(durationMillis = transitionMs, easing = FastOutSlowInEasing))
            if (advanceRequested) advanceOnce()
        }
    }
    val dimAlpha by animateFloatAsState(
        targetValue = if (contentDimmed) 0f else 1f,
        // Linear on the way out: an eased dim reads as the image stalling at the
        // top of the curve rather than settling into black.
        animationSpec = tween(durationMillis = fadeOutMs.toInt().coerceAtLeast(1), easing = LinearEasing),
        label = "illustratedCinematicDim"
    )
    val contentAlpha = enter.value * dimAlpha

    val impactShakeX = remember(stepKey) { CoreAnimatable(0f) }
    val impactShakeY = remember(stepKey) { CoreAnimatable(0f) }
    val impactFlash = remember(stepKey) { CoreAnimatable(0f) }
    val impactFlashColor = remember(stepKey) {
        when (state.step.audioCue) {
            "sfx_combat_plasma_arc" -> Color(0xFF80F8FF)
            "wpn_orion_bowl_resonance" -> Color(0xFFB3F7FF)
            "sfx_w2_stasis_release" -> Color(0xFFC8F4FF)
            "sfx_security_lockdown" -> Color(0xFFFF1744)
            "sfx_w3_shield_anchor" -> Color(0xFFFFE57F)
            "sfx_combat_shield_deflect" -> Color(0xFFFFE0E0)
            else -> Color.White
        }
    }
    LaunchedEffect(stepKey) {
        val cue = state.step.audioCue
        if (cue == "sfx_intro_door_buckle") {
            // 5 accelerating, escalating pounding impacts against the reinforced blast doors
            val buckleHits = listOf(
                240L to 7,
                1009L to 9,
                1845L to 11,
                2296L to 13,
                2690L to 18
            )
            for ((atMs, ampDp) in buckleHits) {
                launch {
                    delay(atMs)
                    val amp = with(density) { ampDp.dp.toPx() }
                    impactShakeX.snapTo(amp)
                    impactShakeY.snapTo(-amp * 0.45f)
                    launch {
                        impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                        val cycles = if (ampDp >= 18) 6 else 3
                        val tremorAmp = with(density) { (ampDp * 0.22f).dp.toPx() }
                        for (cycle in cycles downTo 1) {
                            val decayRatio = cycle.toFloat() / cycles
                            val sign = if (cycle % 2 == 0) 1f else -1f
                            impactShakeX.animateTo(
                                targetValue = tremorAmp * decayRatio * sign,
                                animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                            )
                            impactShakeX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                            )
                        }
                    }
                    launch {
                        impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                    }
                }
            }
        } else if (cue == "sfx_intro_door_collapse" || cue == "sfx_intro_beast_strike") {
            val isBeast = cue == "sfx_intro_beast_strike"
            val initialAmp = with(density) { (if (isBeast) 18.dp else 13.dp).toPx() }
            val tremorAmp = with(density) { (if (isBeast) 4.5.dp else 3.dp).toPx() }
            // 1. Initial high-energy violent transient slam
            launch {
                impactShakeX.snapTo(initialAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                // 2. Residual structural tremor decaying alongside the acoustic reverberation tail
                val tremorCycles = 8
                val cycleDuration = 120
                for (cycle in tremorCycles downTo 1) {
                    val decayRatio = cycle.toFloat() / tremorCycles
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(
                        targetValue = tremorAmp * decayRatio * sign,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                    impactShakeX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                }
            }
            launch {
                impactShakeY.snapTo(-initialAmp * 0.45f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                val tremorCycles = 8
                val cycleDuration = 120
                for (cycle in tremorCycles downTo 1) {
                    val decayRatio = cycle.toFloat() / tremorCycles
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(
                        targetValue = tremorAmp * 0.4f * decayRatio * sign,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                    impactShakeY.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                }
            }
        } else if (cue == "sfx_intro_chime_launch") {
            // Clean pneumatic tube launch impulse without repetitive dragging conduit shakes
            val launchAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(launchAmp)
                impactShakeY.snapTo(-launchAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_combat_plasma_arc") {
            // Sharp electrical surge kick + rapid high-frequency buzz tremor
            val arcAmp = with(density) { 10.dp.toPx() }
            val buzzAmp = with(density) { 3.dp.toPx() }
            launch {
                impactShakeX.snapTo(arcAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(buzzAmp * decay * sign, animationSpec = tween(durationMillis = 36, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 36, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(-arcAmp * 0.5f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_distant_gate_rumble") {
            // Deep subterranean tremor as the buried chamber seal cracks below the workshop
            val rumbleAmp = with(density) { 3.5.dp.toPx() }
            launch {
                delay(180L)
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rumbleAmp * decay * sign, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                }
            }
            launch {
                delay(180L)
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeX.animateTo(rumbleAmp * 0.45f * decay * sign, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_w2_stasis_release") {
            // Pneumatic frost-seal release hiss + soft settling tremor
            val sealAmp = with(density) { 4.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(sealAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 5 downTo 1) {
                    val decay = cycle / 5f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(sealAmp * 0.35f * decay * sign, animationSpec = tween(durationMillis = 60, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 60, easing = LinearEasing))
                }
            }
            launch {
                impactFlash.snapTo(0.4f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 360, easing = LinearEasing))
            }
        } else if (cue == "sfx_security_lockdown") {
            // Heavy Spire blast-shutter slam + crimson alarm flash
            val slamAmp = with(density) { 12.dp.toPx() }
            val rattleAmp = with(density) { 3.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(slamAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
                for (cycle in 5 downTo 1) {
                    val decay = cycle / 5f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rattleAmp * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(slamAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            }
            launch {
                impactFlash.snapTo(0.45f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 340, easing = LinearEasing))
            }
        } else if (cue == "sfx_w3_shield_anchor") {
            // Resonant Forge Anvil chime impact + golden Construct flash
            val anvilAmp = with(density) { 6.dp.toPx() }
            launch {
                impactShakeY.snapTo(anvilAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing))
                for (cycle in 4 downTo 1) {
                    val decay = cycle / 4f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(anvilAmp * 0.35f * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
            launch {
                impactFlash.snapTo(0.42f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 380, easing = LinearEasing))
            }
        } else if (cue == "sfx_door_airlock_close") {
            // Heavy blast-shutter slam + brief metallic latch rattle
            val slamAmp = with(density) { 11.dp.toPx() }
            val rattleAmp = with(density) { 2.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(slamAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
                for (cycle in 3 downTo 1) {
                    val decay = cycle / 3f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rattleAmp * decay * sign, animationSpec = tween(durationMillis = 45, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 45, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(slamAmp * 0.35f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_pod_launch" || cue == "sfx_w2_astra_launch") {
            // Rail-launch / starship liftoff thrust kick + sustained engine rumble
            val isAstra = cue == "sfx_w2_astra_launch"
            val kickAmp = with(density) { (if (isAstra) 7.dp else 9.dp).toPx() }
            val thrustAmp = with(density) { (if (isAstra) 2.2.dp else 2.8.dp).toPx() }
            val cycles = if (isAstra) 16 else 12
            launch {
                impactShakeY.snapTo(-kickAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in cycles downTo 1) {
                    val decay = cycle / cycles.toFloat()
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(thrustAmp * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(kickAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in cycles downTo 1) {
                    val decay = cycle / cycles.toFloat()
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeX.animateTo(thrustAmp * 0.6f * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_combat_shield_deflect") {
            // Violent Planetary Shield rejection jolt + stabilizer shear shudder
            val hitAmp = with(density) { 16.dp.toPx() }
            val shudderAmp = with(density) { 4.2.dp.toPx() }
            launch {
                impactShakeX.snapTo(-hitAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(shudderAmp * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(hitAmp * 0.6f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(shudderAmp * 0.55f * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_crash_impact") {
            // Violent canopy & ground crash slam + heavy structural reverberation
            val crashAmp = with(density) { 20.dp.toPx() }
            val aftershockAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(crashAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(aftershockAmp * decay * sign, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(-crashAmp * 0.65f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(aftershockAmp * 0.6f * decay * sign, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                }
            }
        }
        if (cue == "sfx_intro_beast_strike") {
            launch {
                impactFlash.snapTo(0.7f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 240, easing = LinearEasing))
            }
        } else if (cue == "sfx_combat_plasma_arc") {
            launch {
                impactFlash.snapTo(0.55f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = LinearEasing))
            }
        } else if (cue == "wpn_orion_bowl_resonance") {
            launch {
                impactFlash.snapTo(0.35f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = LinearEasing))
            }
        } else if (cue == "sfx_combat_shield_deflect") {
            launch {
                impactFlash.snapTo(0.85f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 300, easing = LinearEasing))
            }
        } else if (cue == "sfx_crash_impact") {
            launch {
                impactFlash.snapTo(0.9f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 340, easing = LinearEasing))
            }
        }
    }

    SideEffect {
        lastRenderedFrame.value = RenderedCinematicFrame(
            imagePath = state.step.imagePath,
            scale = scale,
            translationX = driftX + impactShakeX.value,
            translationY = driftY + impactShakeY.value,
            alpha = contentAlpha
        )
    }
    val advanceOrReveal: () -> Unit = {
        if (waitsForTextReveal && !captionRevealFinished) {
            revealAllRequest += 1
        } else {
            requestAdvance()
        }
    }
    val isIntroPrologue = state.sceneId == "intro_prologue"
    // Illustrated cinematics play on their authored step durations so timed scores and SFX
    // stay locked to the visuals; players can always use HOLD TO SKIP.
    val cinematicInteractionModifier = Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(cinematicInteractionModifier)
    ) {
        // Outgoing frame, frozen where the previous step left it. Drawn only while the
        // dissolve is running, and skipped entirely on a cut.
        if (outgoing != null && outgoing.alpha > 0.001f && enter.value < 1f && !outgoing.imagePath.isNullOrBlank()) {
            val outgoingPainter = rememberAssetPainter(
                outgoing.imagePath,
                fallback = ColorPainter(Color.Black),
                async = false
            )
            Image(
                painter = outgoingPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = outgoing.alpha * dimAlpha
                        scaleX = outgoing.scale
                        scaleY = outgoing.scale
                        translationX = outgoing.translationX
                        translationY = outgoing.translationY
                    }
            )
        }
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = scale
                    scaleY = scale
                    translationX = driftX + impactShakeX.value
                    translationY = driftY + impactShakeY.value
                }
        )

        // Impact flash on violent beast strike or electrical surge
        if (impactFlash.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = impactFlash.value }
                    .background(impactFlashColor)
            )
        }

        // Atmospheric Layer 3: Live plasma cutter welding-arc flicker
        val isCutterArc = state.step.imagePath?.contains("cutter_surge_arc") == true
        if (isCutterArc) {
            val arcTransition = rememberInfiniteTransition(label = "cutter_arc_flicker")
            val arcAlpha by arcTransition.animateFloat(
                initialValue = 0.06f,
                targetValue = 0.26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arc_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = arcAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF80F8FF).copy(alpha = 0.72f),
                                Color(0xFFFFB300).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(520f, 1550f),
                            radius = 950f
                        )
                    )
            )
        }
        val isCutterAftermath = state.step.imagePath?.contains("cutter_surge_steady") == true
        if (isCutterAftermath) {
            val aftermathTransition = rememberInfiniteTransition(label = "cutter_aftermath_glow")
            val conduitPulse by aftermathTransition.animateFloat(
                initialValue = 0.06f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "conduit_pulse"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = conduitPulse * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF40E8FF).copy(alpha = 0.65f),
                                Color(0xFF00B8D4).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(580f, 1560f),
                            radius = 780f
                        )
                    )
            )
        }

        // Atmospheric Layer 4: Relic discharge & levitating Fork resonance glows
        val isRelicDischarge = state.step.imagePath?.contains("relic_sync_discharge") == true
        if (isRelicDischarge) {
            val dischargeTransition = rememberInfiniteTransition(label = "relic_discharge_glow")
            val dischargeAlpha by dischargeTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "discharge_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dischargeAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFB74D).copy(alpha = 0.65f),
                                Color(0xFF40E8FF).copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 1750f),
                            radius = 950f
                        )
                    )
            )
        }
        val isRelicFork = state.step.imagePath?.contains("relic_sync_fork") == true
        if (isRelicFork) {
            val forkTransition = rememberInfiniteTransition(label = "relic_fork_halo")
            val forkAlpha by forkTransition.animateFloat(
                initialValue = 0.10f,
                targetValue = 0.26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fork_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = forkAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF40E8FF).copy(alpha = 0.60f),
                                Color(0xFF00B8D4).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(660f, 680f),
                            radius = 820f
                        )
                    )
            )
        }

        // Atmospheric Layer 5: Planetary Shield impact glare & red alarm pulse
        val isPodShieldImpact = state.step.imagePath?.contains("pod_shield_impact") == true
        if (isPodShieldImpact) {
            val shieldTransition = rememberInfiniteTransition(label = "pod_shield_glare")
            val shieldAlpha by shieldTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shield_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shieldAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFE5E5).copy(alpha = 0.65f),
                                Color(0xFFFF2A2A).copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 520f),
                            radius = 1050f
                        )
                    )
            )
        }

        // Atmospheric Layer 6: Storm-tossed pod red cabin alarm + cold lightning sheen
        val isPodBraceStorm = state.step.imagePath?.contains("pod_brace_storm") == true
        if (isPodBraceStorm) {
            val stormTransition = rememberInfiniteTransition(label = "pod_storm_alarm")
            val alarmAlpha by stormTransition.animateFloat(
                initialValue = 0.05f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alarm_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = alarmAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF1744).copy(alpha = 0.65f),
                                Color(0xFFD50000).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(180f, 180f),
                            radius = 720f
                        )
                    )
            )
        }

        // Atmospheric Layer 7: Anchor Drill Bridge overload & grounded circuit resonance
        val isAnchorBuckle = state.step.imagePath?.contains("anchor_drill_buckle") == true
        if (isAnchorBuckle) {
            val buckleTransition = rememberInfiniteTransition(label = "anchor_buckle_glow")
            val buckleAlpha by buckleTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "buckle_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = buckleAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.65f),
                                Color(0xFF40E8FF).copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(520f, 1280f),
                            radius = 860f
                        )
                    )
            )
        }
        val isAnchorCircuit = state.step.imagePath?.contains("anchor_drill_circuit") == true
        if (isAnchorCircuit) {
            val circuitTransition = rememberInfiniteTransition(label = "anchor_circuit_halo")
            val circuitAlpha by circuitTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "circuit_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = circuitAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF40E8FF).copy(alpha = 0.55f),
                                Color(0xFF00B8D4).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 1280f),
                            radius = 960f
                        )
                    )
            )
        }

        // Atmospheric Layer 8: Spire approach interference & Astra bridge warning pulse
        val isSpireInterference = (state.sceneId == "scene_w2_astra_departure" && state.stepIndex in 3..6)
        if (isSpireInterference) {
            val interferenceTransition = rememberInfiniteTransition(label = "spire_interference_pulse")
            val interferenceAlpha by interferenceTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "interference_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = interferenceAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF1744).copy(alpha = 0.58f),
                                Color(0xFFD50000).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 460f),
                            radius = 900f
                        )
                    )
            )
        }

        // Atmospheric Layer 9: Orion stasis unseal cryo-resonance glow
        val isStasisUnseal = state.step.imagePath?.contains("w2_orion_stasis_unseal") == true
        if (isStasisUnseal) {
            val stasisTransition = rememberInfiniteTransition(label = "stasis_unseal_glow")
            val stasisAlpha by stasisTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "stasis_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = stasisAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB3F7FF).copy(alpha = 0.50f),
                                Color(0xFFFFD54F).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 760f),
                            radius = 920f
                        )
                    )
            )
        }

        // Atmospheric Layer 10: Spire Lens prism refraction, lockdown alarm & Foundry magma glow
        val isLensVaultClaim = state.step.imagePath?.contains("w3_lens_vault_claim") == true
        if (isLensVaultClaim) {
            val prismTransition = rememberInfiniteTransition(label = "lens_prism_glow")
            val prismAlpha by prismTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "prism_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = prismAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFE57F).copy(alpha = 0.55f),
                                Color(0xFF40E8FF).copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 520f),
                            radius = 900f
                        )
                    )
            )
        }
        val isSpireBurnNotice = state.step.imagePath?.contains("w3_spire_burn_notice") == true
        if (isSpireBurnNotice) {
            val burnTransition = rememberInfiniteTransition(label = "burn_notice_strobe")
            val burnAlpha by burnTransition.animateFloat(
                initialValue = 0.06f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "burn_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = burnAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF1744).copy(alpha = 0.55f),
                                Color(0xFFD50000).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 580f),
                            radius = 980f
                        )
                    )
            )
        }
        val isFoundryDescent = state.step.imagePath?.contains("w3_foundry_descent") == true
        if (isFoundryDescent) {
            val magmaTransition = rememberInfiniteTransition(label = "foundry_magma_glow")
            val magmaAlpha by magmaTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "magma_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = magmaAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6D00).copy(alpha = 0.52f),
                                Color(0xFFFF3D00).copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 1280f),
                            radius = 1040f
                        )
                    )
            )
        }

        // Atmospheric Layer 11: Foundry Anvil Construct, Rylos Titan Dock embers & Outer Shield breach
        val isAnvilConstruct = state.step.imagePath?.contains("w4_anvil_construct") == true
        if (isAnvilConstruct) {
            val anvilTransition = rememberInfiniteTransition(label = "anvil_construct_glow")
            val anvilAlpha by anvilTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "anvil_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = anvilAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD740).copy(alpha = 0.56f),
                                Color(0xFF40E8FF).copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(520f, 1020f),
                            radius = 900f
                        )
                    )
            )
        }
        val isRylosSpared = state.step.imagePath?.contains("w4_rylos_spared") == true
        if (isRylosSpared) {
            val dockTransition = rememberInfiniteTransition(label = "rylos_dock_embers")
            val dockAlpha by dockTransition.animateFloat(
                initialValue = 0.06f,
                targetValue = 0.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dock_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dockAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6D00).copy(alpha = 0.45f),
                                Color(0xFF40E8FF).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(480f, 1180f),
                            radius = 960f
                        )
                    )
            )
        }
        val isOuterShieldBreach = state.step.imagePath?.contains("w4_outer_shield_breach") == true
        if (isOuterShieldBreach) {
            val breachTransition = rememberInfiniteTransition(label = "outer_shield_breach_glow")
            val breachAlpha by breachTransition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breach_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = breachAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF80F8FF).copy(alpha = 0.56f),
                                Color(0xFFFFD54F).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(560f, 640f),
                            radius = 920f
                        )
                    )
            )
        }

        // Atmospheric Layer 1: Emergency breach alarm strobe & telemetry scanlines
        val isEmergency = state.step.captionStyle == CinematicCaptionStyle.SYSTEM ||
            (state.step.imagePath?.contains("breach") == true &&
                state.step.captionStyle != CinematicCaptionStyle.LOCATION)
        if (isEmergency) {
            val strobeTransition = rememberInfiniteTransition(label = "emergency_strobe")
            val strobeAlpha by strobeTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "strobe_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = strobeAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3D00).copy(alpha = 0.55f),
                                Color(0xFFFF5722).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 900f
                        )
                    )
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.15f * contentAlpha }
            ) {
                val stepPx = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += stepPx
                }
            }
        }

        // Atmospheric Layer 2: Cryogenic stasis chamber mist & frost creep
        val isStasisPod = state.step.imagePath?.contains("stasis") == true
        if (isStasisPod) {
            val mistTransition = rememberInfiniteTransition(label = "stasis_mist")
            val mistPulse by mistTransition.animateFloat(
                initialValue = 0.18f,
                targetValue = 0.38f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mist_pulse"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = mistPulse * contentAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.22f),
                                Color(0xFF4DD0E1).copy(alpha = 0.16f),
                                Color(0xFF00E5FF).copy(alpha = 0.38f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            // Frost creeping in across the entire viewport during stasis freeze on dialogue step,
            // then holding steady when the pod is sealed so it doesn't jarringly clear and re-freeze.
            val isInitialStasisDialogue = state.step.captionStyle == CinematicCaptionStyle.DIALOGUE ||
                !state.step.speaker.isNullOrBlank()
            val frostAlpha = if (isInitialStasisDialogue) {
                (progress * 0.45f).coerceIn(0f, 0.45f)
            } else {
                0.45f
            }
            // Cryogenic fog and frost covering the entire screen seamlessly from top to bottom edge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = frostAlpha * contentAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE0F7FA).copy(alpha = 0.65f),
                                Color(0xFFB2EBF2).copy(alpha = 0.40f),
                                Color(0xFF80DEEA).copy(alpha = 0.35f),
                                Color(0xFFB2EBF2).copy(alpha = 0.55f),
                                Color(0xFFE0F7FA).copy(alpha = 0.75f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        if (state.step.captionStyle != CinematicCaptionStyle.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isStasisPod) Color.Transparent else Color.Black.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }

        if (state.step.captionStyle != CinematicCaptionStyle.NONE) {
            val captionEntrance = remember(stepKey) { CoreAnimatable(if (isIntroPrologue) 1f else 0f) }
            LaunchedEffect(stepKey) {
                captionEntrance.animateTo(1f, tween(durationMillis = 150))
            }
            IllustratedCinematicCaption(
                state = state,
                revealAllRequest = revealAllRequest,
                onRevealFinished = { captionRevealFinished = true },
                onAdvance = requestAdvance,
                audioCuePlayer = audioCuePlayer,
                modifier = Modifier
                    .graphicsLayer { alpha = captionEntrance.value * dimAlpha }
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, bottom = 54.dp)
            )
        }

        if (showDebugHud) {
            val elapsedSec = (progress * (durationMs / 1000f)).coerceIn(0f, durationMs / 1000f)
            val totalSec = durationMs / 1000f
            val imageName = state.step.imagePath?.substringAfterLast('/') ?: "none"
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "${state.sceneId} · Step ${state.stepIndex + 1}/${state.stepCount}",
                        color = Color(0xFF63E6FF),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        String.format(Locale.US, "%.1fs / %.1fs · %s", elapsedSec, totalSec, imageName),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (!state.step.audioCue.isNullOrBlank() || !state.step.musicCue.isNullOrBlank()) {
                        Text(
                            "sfx: ${state.step.audioCue ?: "-"} | mus: ${state.step.musicCue ?: "-"}",
                            color = Color(0xFFFFC857),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (state.skippable) {
            Surface(
                color = Color.Black.copy(alpha = 0.52f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(18.dp)
                    .semantics {
                        contentDescription = if (state.sceneId == "intro_prologue") "Skip intro" else "Skip cinematic"
                        onClick(label = "Skip cinematic") {
                            onSkip()
                            true
                        }
                    }
                    .pointerInput(state.sceneId) {
                        detectTapGestures(onLongPress = { onSkip() })
                    }
            ) {
                Text(
                    "HOLD TO SKIP",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun IllustratedCinematicCaption(
    state: CinematicUiState,
    revealAllRequest: Int,
    onRevealFinished: () -> Unit,
    onAdvance: () -> Unit,
    audioCuePlayer: AudioCuePlayer? = null,
    modifier: Modifier = Modifier
) {
    if (state.step.captionStyle == CinematicCaptionStyle.DIALOGUE) {
        val speaker = state.step.speaker.orEmpty()
        DialogueOverlay(
            dialogue = DialogueUi(
                line = DialogueLine(
                    id = "${state.sceneId}_${state.stepIndex}",
                    speaker = speaker,
                    text = state.step.text,
                    portrait = state.step.portrait,
                    voiceCue = null
                ),
                portrait = state.step.portrait,
                voiceCue = null
            ),
            choices = emptyList(),
            onAdvance = {},
            onChoice = {},
            onPlayVoice = {},
            canDismissByTap = false,
            onPlayMurmur = { cue, pitch ->
                audioCuePlayer?.execute(
                    listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L, gain = 0.50f, pitch = pitch))
                )
            },
            onRevealFinished = onRevealFinished,
            revealAllRequest = revealAllRequest,
            modifier = modifier
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.NARRATION) {
        IllustratedNarrationCaption(
            text = state.step.text,
            stepKey = "${state.sceneId}_${state.stepIndex}",
            revealAllRequest = revealAllRequest,
            onRevealFinished = onRevealFinished,
            isLastStep = state.stepIndex + 1 >= state.stepCount,
            showTapToContinue = false,
            modifier = modifier
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.LOCATION) {
        IllustratedLocationPlate(
            text = state.step.text,
            modifier = modifier
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.SYSTEM) {
        val alert = Color(0xFFFFA45B)
        Surface(
            modifier = modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            color = Color(0xF2070B10),
            contentColor = Color.White,
            border = BorderStroke(1.dp, alert.copy(alpha = 0.72f)),
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 15.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(alert)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.step.speaker?.takeIf { it.isNotBlank() }?.let { speaker ->
                        Text(
                            text = speaker.uppercase(Locale.getDefault()),
                            color = alert,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                    Text(
                        text = state.step.text,
                        color = Color.White.copy(alpha = 0.96f),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
            }
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.step.speaker?.takeIf { it.isNotBlank() }?.let { speaker ->
            Text(
                text = speaker.uppercase(),
                color = Color(0xFF7BE8FF),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            )
        }
        Text(
            text = state.step.text,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun IllustratedLocationPlate(
    text: String,
    modifier: Modifier = Modifier
) {
    val parts = remember(text) { text.split("//", limit = 2).map(String::trim) }
    val location = parts.firstOrNull().orEmpty()
    val time = parts.getOrNull(1).orEmpty()
    val accent = Color(0xFF7BE8FF)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xE8060B14),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.52f)),
            shadowElevation = 14.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = location,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, accent.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                )
                if (time.isNotBlank()) {
                    Text(
                        text = time,
                        color = accent.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IllustratedNarrationCaption(
    text: String,
    stepKey: String,
    revealAllRequest: Int,
    onRevealFinished: () -> Unit,
    isLastStep: Boolean,
    showTapToContinue: Boolean = true,
    modifier: Modifier = Modifier
) {
    var revealedCount by remember(stepKey) { mutableIntStateOf(0) }
    val revealFinished = revealedCount >= text.length
    val displayedText = text.take(revealedCount.coerceIn(0, text.length))

    LaunchedEffect(stepKey, text) {
        revealedCount = 0
        if (text.isBlank()) {
            revealedCount = text.length
            return@LaunchedEffect
        }
        for (index in text.indices) {
            if (revealedCount >= text.length) return@LaunchedEffect
            revealedCount = index + 1
            delay(12L)
        }
    }
    LaunchedEffect(stepKey, revealAllRequest) {
        if (revealAllRequest > 0) revealedCount = text.length
    }
    LaunchedEffect(stepKey, revealFinished) {
        if (revealFinished) onRevealFinished()
    }

    CinematicNarrationCard(
        fullText = text,
        displayedText = displayedText,
        revealFinished = revealFinished,
        isLastStep = isLastStep,
        showTapToContinue = showTapToContinue,
        modifier = modifier
    )
}



@Composable
fun ShopGreetingOverlay(
    greeting: ShopGreetingUi,
    onChoice: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.widthIn(max = 480.dp).heightIn(max = 600.dp),
            color = Color.Black.copy(alpha = 0.88f)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                greeting.portraitPath?.let { portrait ->
                    Image(
                        painter = rememberAssetPainter(portrait, fallback = painterResource(R.drawable.shop_icon)),
                        contentDescription = greeting.shopName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                }
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
            }
        }
    }
}


private data class UiFxBurst(
    val id: Long,
    val label: String,
    val color: Color
)

@Composable
private fun CraftingFxOverlay(
    bursts: List<UiFxBurst>,
    onExpired: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        bursts.forEach { burst ->
            GenericFxBurstView(burst = burst, onExpired = onExpired)
        }
    }
}

@Composable
private fun GenericFxBurstView(
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

private data class FxVisualInfo(val label: String, val color: Color)

private fun fxVisualInfo(fxId: String): FxVisualInfo {
    val normalized = fxId.trim().lowercase(Locale.getDefault())
    return when (normalized) {
        "electric_sparks" -> FxVisualInfo("⚡ Conduit Grounded", Color(0xFF80D8FF))
        else -> FxVisualInfo(
            label = normalized.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            color = Color(0xFF90CAF9)
        )
    }
}
