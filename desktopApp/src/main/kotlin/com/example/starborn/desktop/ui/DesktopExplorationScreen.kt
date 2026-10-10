@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.starborn.desktop.ui

import com.example.starborn.domain.environment.EnvironmentalGeometry

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import java.util.Locale
import kotlinx.coroutines.flow.collect

private val TitleWarmColor = Color(0xFFFF9F2E)
private val TitleCyanColor = Color(0xFF63E6FF)
private val HealthGreen = Color(0xFF00E676)
private val ShieldBlue = Color(0xFF2979FF)
private val NeonPink = Color(0xFFFF007F)

data class ActiveDialogueSession(
    val npcName: String,
    val npcRole: String,
    val portraitId: String,
    val text: String,
    val choices: List<String>
)

@Composable
fun DesktopExplorationScreen(
    services: DesktopAppServices,
    onEnterCombat: (List<String>) -> Unit,
    onOpenHub: () -> Unit,
    onOpenFieldKit: () -> Unit,
    onOpenFishing: () -> Unit,
    onOpenArcade: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    DesktopExplorationTheme(services) {
        DesktopExplorationContent(services, onEnterCombat, onOpenHub, onOpenFieldKit, onOpenFishing, onOpenArcade, onReturnToMenu)
    }
}

@Composable
private fun DesktopExplorationContent(services: DesktopAppServices, onEnterCombat: (List<String>) -> Unit,
    onOpenHub: () -> Unit, onOpenFieldKit: () -> Unit, onOpenFishing: () -> Unit,
    onOpenArcade: () -> Unit, onReturnToMenu: () -> Unit) {
    val drawer = remember { ExplorationDrawerState() }
    val settings = LocalExplorationSettings.current
    val environmentMemory=remember { com.example.starborn.domain.environment.EnvironmentalSceneMemory() }
    val environmentSession by services.sessionStore.state.collectAsState()
    val runtime = remember(services) { services.exploration }
    val ui by runtime.uiState.collectAsState()
    val menuOpen = ui.isMenuOverlayVisible
    val foreground = LocalDesktopForeground.current
    var dialogueRevealRequest by remember(ui.activeDialogue?.line?.id) { mutableIntStateOf(0) }
    var dialogueRevealed by remember(ui.activeDialogue?.line?.id) { mutableStateOf(false) }
    var shopId by remember { mutableStateOf<String?>(null) }
    var cookingSource by remember { mutableStateOf<String?>(null) }
    var controlsOpen by remember { mutableStateOf(false) }
    var environmentPreview by remember { mutableStateOf(false) }
    val questPresentations = services.questPresentations
    val fadeAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val keyboardFocus = remember { FocusRequester() }
    DisposableEffect(runtime, foreground) {
        runtime.setExplorationVisible(foreground)
        onDispose { runtime.setExplorationVisible(false) }
    }
    LaunchedEffect(runtime) {
        runtime.events.collect { event ->
            when (event) {
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.EnterCombat -> onEnterCombat(event.enemyIds)
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.AudioCommands -> services.audioDriver.executeAll(event.commands)
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.AudioSettingsChanged -> {
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.MUSIC, event.musicVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.UI, event.sfxVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.BATTLE, event.sfxVolume)
                    services.audioDriver.setUserGain(com.example.starborn.domain.audio.AudioCueType.VOICE, event.voiceVolume)
                }
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.OpenShop -> shopId = event.shopId
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.OpenTinkering -> { services.openTinkeringOnNextFieldKit = true; onOpenFieldKit() }
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.OpenCooking -> cookingSource = event.sourceId ?: "Cooking"
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.OpenFishing -> {
                    services.activeFishingZone = event.zoneId; onOpenFishing()
                }
                is com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.OpenArcade -> {
                    services.activeArcadeCabinet = event.cabinetId; onOpenArcade()
                }
                com.example.starborn.feature.exploration.viewmodel.ExplorationEvent.ReturnToHub -> onOpenHub()
                else -> Unit
            }
        }
    }
    LaunchedEffect(ui.fadeOverlay?.id) {
        ui.fadeOverlay?.let { fade ->
            fadeAlpha.snapTo(fade.fromAlpha)
            fadeAlpha.animateTo(fade.toAlpha, androidx.compose.animation.core.tween(fade.durationMillis))
            runtime.onFadeOverlayFinished(fade.id)
        }
    }
    val moveProgress = remember(ui.roomTransition?.id) { Animatable(if (ui.roomTransition == null) 1f else 0f) }
    LaunchedEffect(ui.roomTransition?.id) {
        if (ui.roomTransition != null) {
            services.audioDriver.executeAll(listOf(com.example.starborn.domain.audio.AudioCommand.Play(com.example.starborn.domain.audio.AudioCueType.UI, "ui_room_move", fadeMs = 0)))
            moveProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }
    val revealingRoom = ui.fadeOverlay?.let { it.toAlpha < it.fromAlpha } == true
    val sceneBlocked = environmentPreview || menuOpen || shopId != null || controlsOpen || cookingSource != null || ui.activeDialogue != null || ui.cinematic != null || ui.prompt != null || ui.narrationPrompt != null || ui.togglePrompt != null || ui.tuningPuzzle != null || ui.blockedPrompt != null || ui.shopGreeting != null || ui.eventAnnouncement != null || (ui.forceBlackScreen && !revealingRoom) || ui.isAstraNavConsoleVisible || ui.isSimulationDeckVisible || ui.isTapeDeckVisible || ui.showBurgQuestAstraExitDialog || ui.isMilestoneGalleryVisible || ui.levelUpPrompt != null || ui.skillTreeOverlay != null || ui.questDetail != null || ui.partyMemberDetails != null || ui.tutorialState.current != null
    val questPopupVisible = !sceneBlocked && questPresentations.important.isNotEmpty()
    val blocked = sceneBlocked || questPopupVisible || moveProgress.value < 1f
    LaunchedEffect(blocked, ui.currentRoom?.id, drawer.panel) {
        runtime.setExplorationInteractionBlocked(blocked)
        if (!blocked) {
            // A closing dialog clears focus during disposal; restore it on the next frame.
            withFrameNanos { }
            keyboardFocus.requestFocus()
        }
    }
    fun travelIfAvailable(direction: String) {
        if (blocked || ui.blockedDirections.any { it.equals(direction, true) }) return
        ui.availableConnections.entries.firstOrNull { it.key.equals(direction, true) && it.value != null }?.let { runtime.travel(it.key) }
    }
    fun interactFirst() { if (!blocked) ui.actions.firstOrNull { ui.actionHints[it.actionKey()]?.locked != true }?.let(runtime::onActionSelected) }

    CompositionLocalProvider(LocalExplorationDrawer provides drawer) {
    BoxWithConstraints(Modifier.fillMaxSize().focusRequester(keyboardFocus).pointerInput(controlsOpen, environmentPreview, drawer.panel, ui.prompt, ui.narrationPrompt, ui.eventAnnouncement) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                    when {
                        controlsOpen -> { controlsOpen = false; event.changes.forEach { it.consume() } }
                        environmentPreview -> { environmentPreview = false; event.changes.forEach { it.consume() } }
                        drawer.panel != null -> { drawer.panel = null; event.changes.forEach { it.consume() } }
                        ui.prompt != null -> { runtime.dismissPrompt(); event.changes.forEach { it.consume() } }
                        ui.narrationPrompt != null -> { runtime.dismissNarration(); event.changes.forEach { it.consume() } }
                        ui.eventAnnouncement != null -> { runtime.dismissEventAnnouncement(); event.changes.forEach { it.consume() } }
                    }
                }
            }
        }
    }.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false
        else if (menuOpen || questPopupVisible) false
        else if (event.key == Key.Escape && drawer.panel != null) { drawer.panel = null; true }
        else if (event.key == Key.Tab) false
        else if (ui.activeDialogue != null) {
            val index = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine).indexOf(event.key)
            when {
                dialogueRevealed && index >= 0 && index < ui.dialogueChoices.size -> { runtime.onDialogueChoiceSelected(ui.dialogueChoices[index].id); true }
                event.key == Key.Enter -> { if (!dialogueRevealed) dialogueRevealRequest++ else if (ui.dialogueChoices.isEmpty()) runtime.advanceDialogue(); true }
                else -> false
            }
        }
        else if (event.key == Key.Enter && ui.cinematic == null && ui.tuningPuzzle == null) {
            when {
                ui.narrationPrompt != null -> { runtime.dismissNarration(); true }
                ui.eventAnnouncement != null -> { runtime.dismissEventAnnouncement(); true }
                ui.prompt != null -> { runtime.dismissPrompt(); true }
                else -> false
            }
        }
        else if (blocked) false else when (event.key) {
            Key.W, Key.DirectionUp -> { travelIfAvailable("north"); true }
            Key.S, Key.DirectionDown -> { travelIfAvailable("south"); true }
            Key.A, Key.DirectionLeft -> { travelIfAvailable("west"); true }
            Key.D, Key.DirectionRight -> { travelIfAvailable("east"); true }
            Key.E -> { interactFirst(); true }
            Key.Escape -> { runtime.openMenuOverlay(); true }
            Key.I -> { runtime.openMenuOverlay(com.example.starborn.feature.exploration.viewmodel.MenuTab.INVENTORY); true }
            Key.M -> { runtime.openMenuOverlay(com.example.starborn.feature.exploration.viewmodel.MenuTab.MAP); true }
            Key.F5 -> { runtime.quickSave(); true }
            Key.F8 -> { if (services.isDebugEnabled) { environmentPreview = true; true } else false }
            Key.H -> { controlsOpen = true; true }
            else -> false
        }
    }.focusable()) {
        val room = ui.currentRoom
        val isDark = resolveRoomDarkness(ui)
        val description = if (ui.isBurgQuestSession && room?.id == "astra_common_room")
            com.example.starborn.feature.exploration.presentation.BurgQuestPresentation.commonRoomDescription
            else resolveRoomDescription(room, ui.roomState, ui.completedMilestones, isDark)
        val inlinePlan = remember(description, ui.actions, ui.actionHints, room) { buildInlineActionPlan(description, ui.actions, ui.actionHints, room) }
        DesktopRoomTransitionBackdrop(services, ui, moveProgress.value,
            resolveRoomBackground(room, ui.roomState, ui.completedMilestones)) { layout ->
            if (isDark) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .85f)))
            val density = androidx.compose.ui.platform.LocalDensity.current
            val geometry = EnvironmentalGeometry(with(density) { maxWidth.toPx() }, with(density) { maxHeight.toPx() },
                layout.center.left,layout.center.top,layout.center.width,layout.center.height)
            val progress=moveProgress.value.coerceIn(0f,1f)
            val moving=ui.roomTransition?.fromBackgroundImage!=null && progress<1f
            val moveSignX = when (ui.roomTransition?.direction?.lowercase()) { "east" -> 1f; "west" -> -1f; else -> 0f }
            val moveSignY = when (ui.roomTransition?.direction?.lowercase()) { "south" -> 1f; "north" -> -1f; else -> 0f }
            fun sceneGeometry(scale: Float, x: Float, y: Float, base: EnvironmentalGeometry = geometry) = base.copy(
                imageX = base.imageX + base.imageWidth * (1 - scale) / 2 + base.imageWidth * x,
                imageY = base.imageY + base.imageHeight * (1 - scale) / 2 + base.imageHeight * y,
                imageWidth = base.imageWidth * scale, imageHeight = base.imageHeight * scale)
            val incomingGeometry = if (settings.disableScreenshake || !moving) geometry else sceneGeometry(.98f + .02f * progress, moveSignX * .28f * (1 - progress), moveSignY * .28f * (1 - progress))
            if (moving) {
                val outgoingRoom = services.roomDefinitions[ui.roomTransition?.fromRoomId]
                val outgoingState = outgoingRoom?.state.orEmpty().mapNotNull { (k, v) -> (v as? Boolean)?.let { k to it } }.toMap() + environmentSession.roomStates[outgoingRoom?.id].orEmpty()
                val outgoingArtwork = rememberDesktopAssetPainter(ui.roomTransition?.fromBackgroundImage, services.assetProvider).intrinsicSize
                val outgoingBase = EnvironmentalGeometry.fit(geometry.width, geometry.height, outgoingArtwork.width, outgoingArtwork.height)
                val outgoingGeometry = if (settings.disableScreenshake) outgoingBase else sceneGeometry(1 - .02f * progress, -moveSignX * .28f * progress, -moveSignY * .28f * progress, outgoingBase)
                DesktopEnvironmentalEffects(services, outgoingRoom, outgoingGeometry, outgoingState, ui.completedMilestones,
                    dark = (outgoingState["dark"] ?: outgoingRoom?.dark ?: false), paused = !foreground || sceneBlocked,
                    opacity = if (settings.disableScreenshake) 1 - progress * progress * (3 - 2 * progress) else 1f - progress,
                    transition = true, sceneMemory = environmentMemory)
            }
            DesktopEnvironmentalEffects(services, room, incomingGeometry, ui.roomState, ui.completedMilestones,
                dark = isDark, paused = !foreground || sceneBlocked, opacity = if (moving) progress else 1f,
                transition = moving, sceneMemory = environmentMemory)
            if (!isDark) DesktopExplorationEnemyStage(services, ui, layout, blocked)
            DesktopExplorationPanels(
                services = services,
                ui = ui,
                layout = layout,
                description = description,
                plan = inlinePlan,
                isDark = isDark,
                blocked = blocked,
                transitionProgress = progress,
                onMenu = { tab ->
                    runtime.openMenuOverlay(if (tab == DesktopMenuTab.STATS) null else com.example.starborn.feature.exploration.viewmodel.MenuTab.valueOf(tab.name))
                },
                onOpenControls = { controlsOpen = true },
                onInteractFirst = ::interactFirst
            )
        }
        if (ui.forceBlackScreen && ui.fadeOverlay == null) Box(Modifier.fillMaxSize().background(Color.Black))
        if (ui.fadeOverlay != null) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = fadeAlpha.value.coerceIn(0f, 1f))))
        if (menuOpen) DesktopFieldMenuDialog(services = services, currentRoomTitle = room?.title, initialTab = DesktopMenuTab.valueOf(ui.menuTab.name),
            onOpenFieldKit = { runtime.closeMenuOverlay(); onOpenFieldKit() },
            onReturnToTitle = { runtime.quickSaveAndReturnToTitle(onReturnToMenu) }, onDismiss = runtime::closeMenuOverlay,
            onOpenControls = { runtime.closeMenuOverlay(); controlsOpen = true })
        if(environmentPreview && room!=null) {
            val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
            val frames=rememberDesktopEnvironmentSteam(services,true)
            com.example.starborn.ui.environment.EnvironmentalEffectsPreview(room,
                services.worldDataSource.environmentalEffectsCatalog,
                rememberDesktopAssetPainter(room.backgroundImage,services.assetProvider),frames,ui.roomState,ui.completedMilestones,
                onCopy={clipboard.setText(androidx.compose.ui.text.AnnotatedString(it))},onClose={environmentPreview=false})
        }
        if (controlsOpen) DesktopControlsDialog(onDismiss = { controlsOpen = false })
        shopId?.let { id -> DesktopShopDialog(services, id) { shopId = null } }
        ui.togglePrompt?.let { toggle ->
            DesktopExplorationModal(toggle.message, runtime::dismissTogglePrompt, toggle.title) {
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = runtime::dismissTogglePrompt) { Text("Cancel") }
                    Button(onClick = { runtime.onTogglePromptSelection(!toggle.isOn) }) {
                        Text(if (toggle.isOn) toggle.disableLabel else toggle.enableLabel)
                    }
                }
            }
        }
        ui.tuningPuzzle?.let { puzzle ->
            val definition = remember(puzzle.id) { services.worldDataSource.loadTuningPuzzles().firstOrNull { it.id == puzzle.id } }
            if (definition != null) DesktopTuningPuzzleDialog(services, definition,
                onSuccess = runtime::submitTuningPuzzle, onDismiss = runtime::dismissTuningPuzzle,
                onValues = { values -> values.forEach { (id, value) -> runtime.updateTuningSlider(id, value) } })
        }
        ui.blockedPrompt?.let { prompt ->
            DesktopExplorationModal(prompt.message, runtime::dismissBlockedPrompt) {
                Button(onClick = runtime::dismissBlockedPrompt, modifier = Modifier.align(Alignment.End)) { Text("OK") }
            }
        }
        ui.shopGreeting?.let { greeting ->
            DesktopExplorationModal(greeting.lines.joinToString("\n") { it.text }, runtime::dismissShopGreeting) {
                greeting.choices.forEach { choice ->
                    DesktopExplorationActionRow(choice.label, "inspect", choice.enabled) { runtime.onShopChoiceSelected(choice.id) }
                }
                if (greeting.choices.isEmpty()) Button(onClick = runtime::enterPendingShop, modifier = Modifier.align(Alignment.End)) { Text("Enter shop") }
            }
        }
        ui.activeDialogue?.let { dialogue ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}, contentAlignment = Alignment.BottomCenter) {
                DesktopAuthoredDialogueOverlay(services = services, dialogue = dialogue, choices = ui.dialogueChoices,
                    onChoice = runtime::onDialogueChoiceSelected, onAdvance = runtime::advanceDialogue,
                    onPlayVoice = { services.audioDriver.executeAll(listOf(com.example.starborn.domain.audio.AudioCommand.Play(com.example.starborn.domain.audio.AudioCueType.VOICE, it, fadeMs = 0))) },
                    onPlayMurmur = { services.audioDriver.executeAll(listOf(com.example.starborn.domain.audio.AudioCommand.Play(com.example.starborn.domain.audio.AudioCueType.VOICE, it, fadeMs = 0))) },
                    onPlayMurmurWithPitch = { cue, pitch ->
                        services.audioDriver.executeAll(listOf(com.example.starborn.domain.audio.AudioCommand.Play(com.example.starborn.domain.audio.AudioCueType.VOICE, cue, fadeMs = 0, pitch = pitch)))
                    },
                    onRevealFinished = { dialogueRevealed = true }, revealAllRequest = dialogueRevealRequest,
                    modifier = Modifier.padding(32.dp).widthIn(max = 960.dp).fillMaxWidth())
            }
        }
        ui.cinematic?.let { cinematic ->
            DesktopCinematicOverlay(cinematic, services, runtime::advanceCinematic, runtime::skipCinematic)
        }
        if (ui.cinematic == null && ui.activeDialogue == null && ui.tuningPuzzle == null) {
            // One foreground inspection at a time. Supporting notifications stay banners.
            val narration = ui.narrationPrompt
            val announcement = ui.eventAnnouncement
            when {
                narration != null -> DesktopInspectionPrompt(narration.message, runtime::dismissNarration, narration.tapToDismiss)
                announcement != null -> DesktopInspectionPrompt(announcement.message, runtime::dismissEventAnnouncement,
                    true, announcement.title, Color(announcement.accentColor))
                ui.prompt != null -> DesktopExplorationPromptBanner(services, requireNotNull(ui.prompt), runtime::dismissPrompt,
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 76.dp))
            }
        }
        cookingSource?.let { source ->
            DesktopCookingScreen(services.craftingService, services.inventoryService, source,
                onBack = { cookingSource = null }, characterName = { services.characterDefinitions[it]?.name ?: it })
        }
        DesktopRuntimeOverlays(services, runtime, ui)
        DesktopQuestPresentation(services, questPresentations, sceneBlocked) { questId ->
            runtime.openQuestDetails(questId)
            runtime.openMenuOverlay(com.example.starborn.feature.exploration.viewmodel.MenuTab.JOURNAL)
        }
    }
    }
}

@Composable
private fun ExplorationInteraction(label: String, onClick: () -> Unit, danger: Boolean = false, enabled: Boolean = true) {
    Surface(Modifier.fillMaxWidth().desktopPointerHover(enabled).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp), color = Color(0xFF13232E),
        border = BorderStroke(1.dp, if (danger) Color(0xFFAA704C) else Color(0xFF354D5D))) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, modifier = Modifier.weight(1f), color = Color(0xFFE2E8ED), style = MaterialTheme.typography.bodyMedium)
            Text("›", color = if (danger) TitleWarmColor else TitleCyanColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun DesktopDialogueOverlay(
    speakerName: String,
    speakerRole: String,
    portraitId: String,
    text: String,
    choices: List<String>,
    services: DesktopAppServices,
    onSelectChoice: (Int) -> Unit,
    onAdvance: () -> Unit,
    onClose: () -> Unit
) {
    val portraitPainter = rememberDesktopAssetPainter(portraitId, services.assetProvider)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = {
                if (choices.isEmpty()) onAdvance() else onClose()
            }),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxWidth(0.88f)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                .background(FieldMenuDesign.shell.copy(alpha = 0.98f))
                .border(BorderStroke(1.5.dp, TitleCyanColor), RoundedCornerShape(FieldMenuDesign.cardRadius))
                .clickable(enabled = false) {}
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = portraitPainter,
                    contentDescription = speakerName,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.5.dp, TitleCyanColor), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = speakerName.uppercase() + if (speakerRole.isNotBlank()) " · ${speakerRole.uppercase()}" else "",
                            color = TitleWarmColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = LocalStarbornFonts.current.orbitron
                        )

                        Text(
                            text = if (choices.isEmpty()) "CONTINUE" else "CHOOSE A RESPONSE",
                            color = FieldMenuDesign.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.desktopPointerHover(choices.isEmpty()).clickable(enabled = choices.isEmpty(), onClick = onAdvance)
                        )
                    }

                    Text(
                        text = text,
                        color = FieldMenuDesign.text,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (choices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            choices.forEachIndexed { index, choiceText ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
                                        .background(FieldMenuDesign.elevatedPanel)
                                        .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.35f)), RoundedCornerShape(FieldMenuDesign.controlRadius))
                                        .desktopPointerHover()
                                        .clickable { onSelectChoice(index) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "◆ [${index + 1}] $choiceText",
                                        color = TitleCyanColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            DesktopMinimalPillButton(
                                text = "Continue [Enter]",
                                onClick = onAdvance
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopMinimalPillButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .desktopPointerHover()
            .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
            .background(FieldMenuDesign.panel.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.45f)), RoundedCornerShape(FieldMenuDesign.controlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = FieldMenuDesign.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalStarbornFonts.current.orbitron
        )
    }
}
