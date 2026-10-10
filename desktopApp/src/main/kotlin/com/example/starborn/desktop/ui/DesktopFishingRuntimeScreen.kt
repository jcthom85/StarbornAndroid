package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.domain.fishing.FishingLure
import com.example.starborn.domain.fishing.FishingRod
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.feature.fishing.viewmodel.*

@Composable
fun DesktopFishingScreen(
    services: DesktopAppServices,
    zoneId: String = "sector9_stream",
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val runtime = remember(services, zoneId) {
        FishingController(services.fishingService, zoneId, parentScope = scope)
    }
    val state by runtime.uiState.collectAsState()
    val fonts = LocalStarbornFonts.current

    DisposableEffect(runtime) {
        onDispose { runtime.close() }
    }

    val cue: (String) -> Unit = { cueName ->
        services.audioDriver.executeAll(services.audioRouter.commandsForUi(cueName))
    }

    // Audio cue listener for fishing events
    LaunchedEffect(runtime) {
        runtime.events.collect { event ->
            when (event) {
                FishingController.FishingEvent.Cast -> cue("action_inspect")
                FishingController.FishingEvent.Nibble -> cue("sfx_button_toggle")
                FishingController.FishingEvent.Bite -> cue("confirm")
                FishingController.FishingEvent.Warning -> cue("sfx_stat_decrement")
                FishingController.FishingEvent.CatchSuccess -> cue("sfx_loot_cache")
                FishingController.FishingEvent.LineSnap -> cue("action_retreat")
            }
        }
    }

    fun acknowledgeCatch() {
        state.lastCatchResult?.let { result ->
            services.exploration.onFishingResult(
                FishingResultPayload(
                    result.itemId,
                    result.quantity,
                    result.message,
                    state.lastResult != MinigameResult.FAIL,
                    secured = true
                )
            )
        }
        runtime.resetFishing()
    }

    var showJournalDrawer by remember { mutableStateOf(false) }

    // Keyboard controls
    val keyboardFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboardFocus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040A12))
            .focusRequester(keyboardFocus)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    if (state.fishingState == FishingState.WAITING) {
                        runtime.cancelWaiting()
                    } else {
                        runtime.cancelFishing()
                        onClose()
                    }
                    true
                } else if (event.key == Key.Spacebar) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            when (state.fishingState) {
                                FishingState.SETUP -> {
                                    if (state.selectedRod != null && state.selectedLure != null && state.currentZone != null) {
                                        runtime.startFishing()
                                    }
                                }
                                FishingState.HOOKSET -> runtime.onHookButtonPressed()
                                FishingState.READY -> runtime.beginReeling()
                                FishingState.REELING -> runtime.onReelPressed()
                                FishingState.RESULT -> acknowledgeCatch()
                                else -> Unit
                            }
                        }
                        KeyEventType.KeyUp -> {
                            if (state.fishingState == FishingState.REELING) {
                                runtime.onReelReleased()
                            }
                        }
                    }
                    true
                } else false
            }
            .focusable()
    ) {
        // Deep water animated sonar canvas
        SonarAtmosphereCanvas(state.fishingState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Telemetry Header Bar
            FishingHeaderBar(
                zoneName = state.currentZone?.name ?: "Abyssal Waters",
                zoneDescription = state.currentZone?.description ?: "Deep water thermocline.",
                selectedRod = state.selectedRod,
                selectedLure = state.selectedLure,
                journalCount = state.journal.sumOf { it.species.count { s -> s.caught } },
                onToggleJournal = { showJournalDrawer = !showJournalDrawer },
                onClose = {
                    runtime.cancelFishing()
                    onClose()
                }
            )

            // Main Phase Cockpit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (state.fishingState) {
                    FishingState.SETUP -> {
                        FishingSetupView(
                            state = state,
                            onSelectRod = runtime::selectRod,
                            onSelectLure = runtime::selectLure,
                            onCast = runtime::startFishing
                        )
                    }
                    FishingState.WAITING -> {
                        FishingWaitingView(
                            waitingState = state.waitingState,
                            onCancel = runtime::cancelWaiting
                        )
                    }
                    FishingState.HOOKSET -> {
                        FishingHooksetView(
                            hookState = state.hookState,
                            onHook = runtime::onHookButtonPressed
                        )
                    }
                    FishingState.READY -> {
                        FishingReadyView(
                            reelState = state.reelState,
                            onBegin = runtime::beginReeling
                        )
                    }
                    FishingState.REELING -> {
                        FishingReelingCockpit(
                            reelState = state.reelState,
                            onReelPress = runtime::onReelPressed,
                            onReelRelease = runtime::onReelReleased
                        )
                    }
                    FishingState.RESULT -> {
                        FishingResultView(
                            result = state.lastCatchResult,
                            minigameResult = state.lastResult,
                            onContinue = ::acknowledgeCatch
                        )
                    }
                }
            }

            // Bottom Key Legend HUD Bar
            FishingBottomKeyLegend(
                fishingState = state.fishingState,
                onClose = {
                    runtime.cancelFishing()
                    onClose()
                }
            )
        }

        // Catch Journal Drawer Modal
        if (showJournalDrawer) {
            FishingJournalModal(
                journal = state.journal,
                onDismiss = { showJournalDrawer = false }
            )
        }
    }
}

@Composable
private fun SonarAtmosphereCanvas(state: FishingState) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background gradient
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF092032),
                            Color(0xFF040F1A),
                            Color(0xFF02070D)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.7f
                    )
                )

                // Concentric sonar radar rings
                val maxRadius = size.minDimension * 0.45f
                for (i in 1..4) {
                    val r = maxRadius * (i / 4f)
                    drawCircle(
                        color = Color(0x1500E5FF),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }

                // Expanding sonar pulse wave
                drawCircle(
                    color = Color(0x3300FF9D).copy(alpha = (1f - pulseRadius) * 0.25f),
                    radius = maxRadius * pulseRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
    )
}

@Composable
private fun FishingHeaderBar(
    zoneName: String,
    zoneDescription: String,
    selectedRod: FishingRod?,
    selectedLure: FishingLure?,
    journalCount: Int,
    onToggleJournal: () -> Unit,
    onClose: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xDD081524),
        border = BorderStroke(1.5.dp, Color(0xFF1B3850)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Zone Lore & Hydrophone Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E283D),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "SONAR",
                        color = Color(0xFF00E5FF),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = zoneName.uppercase(),
                        color = Color.White,
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = zoneDescription,
                        color = Color(0xFF8FB0C4),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tackles & Journal Badges + Exit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rod chip
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFF0E2233),
                    border = BorderStroke(1.dp, Color(0xFF1E3C56))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎣", fontSize = 12.sp)
                        Text(
                            text = selectedRod?.name ?: "No Rod",
                            color = if (selectedRod != null) Color(0xFF00E5FF) else Color(0xFF7897AC),
                            fontFamily = fonts.orbitron,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Lure chip
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFF0E2233),
                    border = BorderStroke(1.dp, Color(0xFF1E3C56))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🪱", fontSize = 12.sp)
                        Text(
                            text = selectedLure?.name ?: "No Lure",
                            color = if (selectedLure != null) Color(0xFFFFD54F) else Color(0xFF7897AC),
                            fontFamily = fonts.orbitron,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Journal Button
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0x2200FF9D),
                    border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .testTag("journal-toggle-button")
                        .desktopPointerHover()
                        .clickable(onClick = onToggleJournal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📖", fontSize = 12.sp)
                        Text(
                            text = "$journalCount SPECIES",
                            color = Color(0xFF00FF9D),
                            fontFamily = fonts.orbitron,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DesktopKeyBadge(
                    keyGlyph = "Esc",
                    label = "Leave Zone",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun FishingSetupView(
    state: FishingUiState,
    onSelectRod: (FishingRod) -> Unit,
    onSelectLure: (FishingLure) -> Unit,
    onCast: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val canCast = state.selectedRod != null && state.selectedLure != null && state.currentZone != null

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE07121E),
        border = BorderStroke(1.5.dp, Color(0xFF1B3A52)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "TACKLE & RIGGING PREPARATION",
                color = Color(0xFF00E5FF),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Rod Selection Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SELECT CASTING ROD",
                        color = Color(0xFF8FB0C4),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    if (state.availableRods.isEmpty()) {
                        EmptyGearNotice("No fishing rods in inventory.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.availableRods, key = { it.id }) { rod ->
                                val isSelected = rod == state.selectedRod
                                GearSelectionCard(
                                    title = rod.name,
                                    subtitle = "Power: ${rod.fishingPower} · Stability: ${rod.stability}",
                                    description = rod.description.orEmpty(),
                                    isSelected = isSelected,
                                    accentColor = Color(0xFF00E5FF),
                                    onClick = { onSelectRod(rod) }
                                )
                            }
                        }
                    }
                }

                // Lure Selection Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SELECT LURE / BAIT",
                        color = Color(0xFF8FB0C4),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    if (state.availableLures.isEmpty()) {
                        EmptyGearNotice("No fishing lures in inventory.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.availableLures, key = { it.id }) { lure ->
                                val isSelected = lure == state.selectedLure
                                GearSelectionCard(
                                    title = lure.name,
                                    subtitle = "Rarity Bonus: +${(lure.rarityBonus * 100).toInt()}%",
                                    description = lure.description.orEmpty(),
                                    isSelected = isSelected,
                                    accentColor = Color(0xFFFFD54F),
                                    onClick = { onSelectLure(lure) }
                                )
                            }
                        }
                    }
                }
            }

            // Big Cast Action CTA
            Button(
                onClick = onCast,
                enabled = canCast,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4),
                    disabledContainerColor = Color(0xFF132A3B),
                    contentColor = Color(0xFF04101A),
                    disabledContentColor = Color(0xFF4C6A7C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .desktopPointerHover(canCast)
                    .testTag("cast-line-button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎣", fontSize = 18.sp)
                    Text(
                        text = if (canCast) "CAST LINE INTO DEEP WATERS [Spacebar / Enter]" else "SELECT A ROD AND LURE TO CAST",
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GearSelectionCard(
    title: String,
    subtitle: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0xFF0B1B2A),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else Color(0xFF1C3A50)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gear-card-${title.lowercase().replace(' ', '_')}")
            .desktopPointerHover()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color(0xFFC7DEEC),
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (isSelected) {
                    Text(
                        text = "EQUIPPED ✓",
                        color = accentColor,
                        fontFamily = fonts.orbitron,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = subtitle,
                color = accentColor.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = Color(0xFF8FB0C4),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FishingWaitingView(
    waitingState: FishingWaitingState?,
    onCancel: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val infiniteTransition = rememberInfiniteTransition()
    val bobberY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE07121E),
        border = BorderStroke(1.5.dp, Color(0xFF1B3A52)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Bobber & Radar Sensor
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF092033))
                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔴",
                    fontSize = 32.sp,
                    modifier = Modifier.offset(y = bobberY.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "LURE SUBMERGED · SCANNING FOR THERMAL BITE...",
                color = Color(0xFF00E5FF),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Keep your finger on [Spacebar]. When a specimen strikes, set the hook immediately!",
                color = Color(0xFF8FB0C4),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.dp, Color(0xFF6F90A6)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("cancel-cast-button")
            ) {
                Text(
                    text = "Cancel Cast [Esc]",
                    color = Color(0xFF8FB0C4),
                    fontFamily = fonts.orbitron,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FishingHooksetView(
    hookState: FishingHookState?,
    onHook: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val infiniteTransition = rememberInfiniteTransition()
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE1E0B08),
        border = BorderStroke(2.dp, Color(0xFFFF5252).copy(alpha = alertAlpha)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠ STRIKE DETECTED! ⚠",
                color = Color(0xFFFF5252),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "A fish took the bait! Hook the line before it escapes!",
                color = Color(0xFFFFCDD2),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(20.dp))

            // Reaction Time Meter
            val remainingMs = hookState?.timeRemainingMs ?: 2000L
            LinearProgressIndicator(
                progress = { (remainingMs / 2400f).coerceIn(0f, 1f) },
                color = Color(0xFFFF5252),
                trackColor = Color(0xFF3E1616),
                modifier = Modifier
                    .width(360.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onHook,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .width(320.dp)
                    .height(60.dp)
                    .desktopPointerHover()
                    .testTag("hook-button")
            ) {
                Text(
                    text = "SET HOOK NOW! [Spacebar]",
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun FishingReadyView(
    reelState: FishingReelState?,
    onBegin: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE071524),
        border = BorderStroke(1.5.dp, Color(0xFF00FF9D)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HOOK CONFIRMED! SPECIMEN ON LINE",
                color = Color(0xFF00FF9D),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Hold [Spacebar] or click the winch pad to reel in.\nRelease when line tension enters the RED zone to prevent line snapping!",
                color = Color(0xFFC7DEEC),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = onBegin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF9D),
                    contentColor = Color(0xFF04101A)
                ),
                modifier = Modifier
                    .width(280.dp)
                    .height(52.dp)
                    .desktopPointerHover()
                    .testTag("begin-reel-button")
            ) {
                Text(
                    text = "ENGAGE REEL [Spacebar]",
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun FishingReelingCockpit(
    reelState: FishingReelState?,
    onReelPress: () -> Unit,
    onReelRelease: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val progress = reelState?.progress ?: 0f
    val tension = reelState?.tension ?: 0f
    val isTensionCritical = tension > 0.8f
    val phase = reelState?.phase ?: FishingFightPhase.CALM

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE07121E),
        border = BorderStroke(1.5.dp, if (isTensionCritical) Color(0xFFFF5252) else Color(0xFF1B3A52)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fish Telemetry & Phase Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ENGAGED BIO-SIGNAL: ${reelState?.fishName?.uppercase() ?: "UNKNOWN SPECIMEN"}",
                        color = Color.White,
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = phase.instruction,
                        color = when (phase) {
                            FishingFightPhase.CALM, FishingFightPhase.RECOVERY -> Color(0xFF00FF9D)
                            FishingFightPhase.PULL -> Color(0xFF00E5FF)
                            FishingFightPhase.WARNING -> Color(0xFFFFD54F)
                            FishingFightPhase.SURGE -> Color(0xFFFF5252)
                        },
                        fontFamily = fonts.orbitron,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Fish Stamina chip
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0F2233),
                    border = BorderStroke(1.dp, Color(0xFF1F3D56))
                ) {
                    Text(
                        text = "STAMINA: ${(reelState?.staminaRemaining?.times(100) ?: 100f).toInt()}%",
                        color = Color(0xFF8FB0C4),
                        fontFamily = fonts.orbitron,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF14293A))

            // Gauge 1: Catch Retrieval Progress (Distance to Boat)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RETRIEVAL PROGRESS",
                        color = Color(0xFF8FB0C4),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color(0xFF00E5FF),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF0F2538),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            }

            // Gauge 2: Line Tension Meter
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isTensionCritical) "⚠ LINE TENSION (CRITICAL OVERLOAD!)" else "LINE TENSION",
                        color = if (isTensionCritical) Color(0xFFFF5252) else Color(0xFF8FB0C4),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(tension * 100).toInt()}%",
                        color = when {
                            tension > 0.8f -> Color(0xFFFF5252)
                            tension > 0.5f -> Color(0xFFFFD54F)
                            else -> Color(0xFF00FF9D)
                        },
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { tension },
                    color = when {
                        tension > 0.8f -> Color(0xFFFF5252)
                        tension > 0.5f -> Color(0xFFFFD54F)
                        else -> Color(0xFF00FF9D)
                    },
                    trackColor = Color(0xFF1A1D24),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            }

            Spacer(Modifier.weight(1f))

            // Interactive Winch Pad
            val isReeling = reelState?.isReeling == true
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isReeling) Color(0xFF134230) else Color(0xFF0A1D2B),
                border = BorderStroke(2.dp, if (isReeling) Color(0xFF00FF9D) else Color(0xFF1E425E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .desktopPointerHover()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onReelPress()
                                try {
                                    awaitRelease()
                                } finally {
                                    onReelRelease()
                                }
                            }
                        )
                    }
                    .testTag("winch-action-pad")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isReeling) "⚙ WINCH REELING ENGAGED" else "⚙ HOLD [SPACEBAR] OR CLICK TO REEL",
                            color = if (isReeling) Color(0xFF00FF9D) else Color.White,
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isTensionCritical) "RELEASE WINCH NOW TO PREVENT LINE SNAP!" else "Keep tension in the safe zone until the specimen reaches 100%",
                            color = if (isTensionCritical) Color(0xFFFF8A80) else Color(0xFF8FB0C4),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingResultView(
    result: com.example.starborn.domain.fishing.FishingResult?,
    minigameResult: MinigameResult?,
    onContinue: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val isSuccess = minigameResult != MinigameResult.FAIL

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE07121E),
        border = BorderStroke(2.dp, if (isSuccess) Color(0xFFFFD54F) else Color(0xFFFF5252)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSuccess) "★ CATCH SECURED! ★" else "LINE SNAPPED!",
                color = if (isSuccess) Color(0xFFFFD54F) else Color(0xFFFF5252),
                fontFamily = fonts.orbitron,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = result?.message ?: if (isSuccess) "Specimen hauled aboard." else "The specimen broke the tension limit and escaped.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (isSuccess && result?.itemId != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x3300FF9D),
                    border = BorderStroke(1.dp, Color(0xFF00FF9D).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "+${result.quantity} ${result.itemId.replace('_', ' ').uppercase()} ACQUIRED",
                        color = Color(0xFF00FF9D),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuccess) Color(0xFFFFD54F) else Color(0xFF1E3A52),
                    contentColor = if (isSuccess) Color(0xFF07121E) else Color.White
                ),
                modifier = Modifier
                    .width(260.dp)
                    .height(50.dp)
                    .desktopPointerHover()
                    .testTag("fishing-continue-button")
            ) {
                Text(
                    text = "CONTINUE [Spacebar]",
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FishingJournalModal(
    journal: List<com.example.starborn.domain.fishing.FishingJournalEntry>,
    onDismiss: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF081524),
            border = BorderStroke(1.5.dp, Color(0xFF00FF9D)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .width(620.dp)
                .height(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📖 ABYSSAL FISHING JOURNAL",
                        color = Color(0xFF00FF9D),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    DesktopKeyBadge(
                        keyGlyph = "Esc",
                        label = "Close",
                        onClick = onDismiss
                    )
                }

                HorizontalDivider(color = Color(0xFF162D42))

                if (journal.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No species documented in this sector yet.",
                            color = Color(0xFF7897AC),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(journal, key = { it.zoneId }) { entry ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = entry.name.uppercase(),
                                    color = Color(0xFF00E5FF),
                                    fontFamily = fonts.orbitron,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                entry.species.forEach { sp ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0D1E2D),
                                        border = BorderStroke(1.dp, Color(0xFF1D3B54))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = sp.name,
                                                color = if (sp.caught) Color.White else Color(0xFF6F8F9F),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (sp.caught) Color(0x3300FF9D) else Color(0xFF142433)
                                            ) {
                                                Text(
                                                    text = if (sp.caught) "DOCUMENTED ✓" else "UNKNOWN",
                                                    color = if (sp.caught) Color(0xFF00FF9D) else Color(0xFF688899),
                                                    fontFamily = fonts.orbitron,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
}

@Composable
private fun FishingBottomKeyLegend(
    fishingState: FishingState,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC07111B),
        border = BorderStroke(1.dp, Color(0xFF182D3D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (fishingState) {
                    FishingState.SETUP -> DesktopKeyBadge("Space", "Cast Line", highlighted = true)
                    FishingState.WAITING -> DesktopKeyBadge("Waiting", "Watch Bobber")
                    FishingState.HOOKSET -> DesktopKeyBadge("Space", "Strike / Hook", highlighted = true)
                    FishingState.READY -> DesktopKeyBadge("Space", "Engage Reel", highlighted = true)
                    FishingState.REELING -> DesktopKeyBadge("Hold Space", "Winch Reel (Release to Slacken)", highlighted = true)
                    FishingState.RESULT -> DesktopKeyBadge("Space", "Continue", highlighted = true)
                }
            }

            DesktopKeyBadge(
                keyGlyph = "Esc",
                label = if (fishingState == FishingState.WAITING) "Cancel Cast" else "Exit",
                onClick = onClose
            )
        }
    }
}

@Composable
private fun EmptyGearNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF7897AC),
            fontSize = 12.sp
        )
    }
}
