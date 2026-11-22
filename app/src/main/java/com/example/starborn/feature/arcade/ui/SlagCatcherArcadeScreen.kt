package com.example.starborn.feature.arcade.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.arcade.domain.ArcadeIds
import com.example.starborn.feature.arcade.domain.ArcadeRunSubmission
import com.example.starborn.feature.arcade.domain.ArcadeService
import com.example.starborn.feature.arcade.games.slagcatcher.*
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val MoltenOrange = Color(0xFFFF7B00)
private val BlastYellow = Color(0xFFFFD000)
private val FoundryCabinet = Color(0xFF19100C)
private val CrucibleSteel = Color(0xFF382C26)
private val IngotCyan = Color(0xFF00E5FF)
private val HazardRed = Color(0xFFFF3344)
private val SteamWhite = Color(0xFFE2E8F0)
private val WarmIvory = Color(0xFFFFE7B0)

@Composable
fun SlagCatcherArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit,
    hapticsDisabled: Boolean = false
) {
    val engine = remember { SlagCatcherEngine() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val backdrop = remember {
        runCatching {
            context.assets.open("images/arcade/slag_catcher/foundry_smelter_v1.png").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(SlagCatcherInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.SLAG_CATCHER).highScore, submitted?.highScore ?: 0)

    BackHandler {
        if (!paused && !snapshot.gameOver) paused = true else onBack()
    }

    LaunchedEffect(engine, paused, tutorial) {
        var previous = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previous != 0L && !paused && !tutorial) {
                    engine.advance((now - previous) / 1_000_000_000f, input)
                    val next = engine.snapshot()
                    snapshot = next
                    when (next.audioEvent) {
                        SlagAudioEvent.CATCH_DROPLET -> {
                            onPlayCue("sfx_arcade_laser")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        SlagAudioEvent.CATCH_INGOT -> {
                            onPlayCue("confirm")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        SlagAudioEvent.CATCH_CORE -> {
                            onPlayCue("sfx_arcade_jump")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        SlagAudioEvent.HIT_BOMB, SlagAudioEvent.BUCKET_LOST -> {
                            onPlayCue("action_inspect")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        SlagAudioEvent.VENT_STEAM -> {
                            onPlayCue("sfx_arcade_laser")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        SlagAudioEvent.ROUND_CLEAR -> {
                            onPlayCue("confirm")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        SlagAudioEvent.GAME_OVER -> {
                            onPlayCue("sfx_arcade_game_over")
                            if (!hapticsDisabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        SlagAudioEvent.NONE -> Unit
                    }
                }
                previous = now
            }
        }
    }

    LaunchedEffect(snapshot.gameOver) {
        if (snapshot.gameOver && submitted == null) {
            submitted = arcadeService.submitSlagCatcherScore(snapshot.score)
            if (snapshot.audioEvent != SlagAudioEvent.GAME_OVER) {
                onPlayCue("sfx_arcade_game_over")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0503))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlagMarquee()
        Spacer(Modifier.height(4.dp))
        SlagScoreConsole(snapshot.score, highScore, snapshot.multiplier, snapshot.buckets, snapshot.round)
        Spacer(Modifier.height(4.dp))
        HeatGauge(snapshot.heat, snapshot.itemsRemainingInShift)
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(FoundryCabinet, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF4A2B18), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            SlagCanvas(
                snapshot = snapshot,
                backdrop = backdrop,
                reducedFlashes = reducedFlashes,
                onDirectTouch = { targetX -> input = input.copy(paddleTargetX = targetX) },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("slag_catcher_viewport")
            )

            if (tutorial) {
                SlagOverlay(
                    title = "HOW TO SMELT",
                    lines = listOf(
                        "Drag or steer the Crucible Bucket left & right.",
                        "Catch Molten Slag (+100) and Titanium Ingots (+300).",
                        "Snag Prismatic Cores (+1,000) to cool the crucible.",
                        "DODGE Volatile Slag Bombs to prevent bucket loss!",
                        "Tap VENT STEAM when heat rises to score big bonuses.",
                        "Don't let valuable slag drop into the waste drain."
                    ),
                    action = "START SHIFT",
                    onAction = { tutorial = false; onPlayCue("sfx_arcade_coin_insert") },
                    secondaryAction = "LEAVE CABINET",
                    onSecondaryAction = onBack
                )
            } else if (paused) {
                SlagOverlay(
                    title = "SHIFT PAUSED",
                    lines = listOf("Crucible locked on maintenance rail."),
                    action = "RESUME",
                    onAction = { paused = false },
                    secondaryAction = "LEAVE CABINET",
                    onSecondaryAction = onBack
                )
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                val tokens = submitted?.tokensEarned ?: 0
                SlagOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE: ${snapshot.score}")
                        add("SHIFT REACHED: ROUND ${snapshot.round}")
                        if (tokens > 0) add("+$tokens ARCADE ${if (tokens == 1) "TOKEN" else "TOKENS"} EARNED")
                        if (tiers.isNotBlank()) add("REWARD UNLOCKED: $tiers")
                        else if (tokens == 0) add("Drop one more coin to beat the high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = SlagCatcherInput()
                        onPlayCue("sfx_arcade_coin_insert")
                    },
                    secondaryAction = "LEAVE CABINET",
                    onSecondaryAction = onBack
                )
            }
        }

        SlagStatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(4.dp))
        SlagControls(input, snapshot.heat, largeTouchTargets) { transform -> input = transform(input) }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(.88f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SlagServiceKey(text = "SERVICE  •  PAUSE", onClick = { paused = true }, modifier = Modifier.weight(1f))
            SlagServiceKey(text = "LEAVE CABINET", onClick = onBack, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlagMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF261208), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF5C2D16), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SLAG CATCHER", color = MoltenOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
        Text("FOUNDRY SMELTER // CABINET 04", color = BlastYellow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SlagScoreConsole(score: Int, highScore: Int, multiplier: Int, buckets: Int, round: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E0E07), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF452210), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(56.dp), IngotCyan)
        ScoreCell("CRUCIBLES", "⛛".repeat(buckets.coerceAtLeast(0)), Modifier.width(66.dp), MoltenOrange)
        ScoreCell("SHIFT", "$round", Modifier.width(46.dp), BlastYellow)
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = MoltenOrange) {
    Column(
        modifier
            .background(Color(0xFF0E0603), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF33160A), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF9E7C68), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .6.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun HeatGauge(heat: Float, itemsRemaining: Int) {
    val overheating = heat > 75f
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF140804), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3B180A), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("CRUCIBLE HEAT", color = if (overheating) HazardRed else Color(0xFF9E7C68), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF0A0402), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((heat / 100f).coerceIn(0f, 1f))
                    .background(if (overheating) HazardRed else MoltenOrange, RoundedCornerShape(3.dp))
            )
        }
        Text("${heat.toInt()}%", color = if (overheating) HazardRed else WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("REMAINING: $itemsRemaining", color = IngotCyan, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SlagStatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF140905), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF38190B), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (failure) HazardRed else MoltenOrange, RoundedCornerShape(50))
        )
        Text(
            message,
            color = if (failure) Color(0xFFFF9E8E) else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun SlagCanvas(
    snapshot: SlagCatcherSnapshot,
    backdrop: ImageBitmap?,
    reducedFlashes: Boolean,
    onDirectTouch: (Float) -> Unit,
    modifier: Modifier
) {
    val latestDirectTouch by rememberUpdatedState(onDirectTouch)
    Canvas(
        modifier
            .background(Color(0xFF0D0604), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    latestDirectTouch(fraction)
                }
            }
    ) {
        val w = size.width
        val h = size.height

        backdrop?.let { bg ->
            drawImage(
                image = bg,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bg.width, bg.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w.toInt(), h.toInt()),
                alpha = 0.72f,
                filterQuality = FilterQuality.Low
            )
            drawRect(Color(0xFF0A0402).copy(alpha = 0.25f))
        }

        // 1. Top Blast Furnace Chute Header
        drawRect(Color(0xFF261208), topLeft = Offset(0f, 0f), size = Size(w, 24.dp.toPx()))
        drawLine(Color(0xFF5C2D16), Offset(0f, 24.dp.toPx()), Offset(w, 24.dp.toPx()), 2f)

        // Blast spark glow
        repeat(8) { i ->
            val sparkX = ((i * 47f + snapshot.elapsedSeconds * 30f) % 320f) / 320f * w
            drawCircle(MoltenOrange.copy(alpha = 0.6f), radius = 2f, center = Offset(sparkX, 12.dp.toPx()))
        }

        // 2. Falling Objects
        snapshot.fallingItems.forEach { item ->
            val ix = item.x * w
            val iy = item.y * h
            val ir = item.radius * w

            when (item.type) {
                SlagItemType.SLAG_DROPLET -> {
                    // Molten Droplet
                    drawCircle(MoltenOrange.copy(alpha = 0.5f), radius = ir * 1.5f, center = Offset(ix, iy))
                    drawCircle(MoltenOrange, radius = ir, center = Offset(ix, iy))
                    drawCircle(BlastYellow, radius = ir * 0.5f, center = Offset(ix, iy))
                }
                SlagItemType.TITANIUM_INGOT -> {
                    // Solid Ingot
                    val rw = ir * 2.2f
                    val rh = ir * 1.4f
                    drawRoundRect(IngotCyan, topLeft = Offset(ix - rw / 2f, iy - rh / 2f), size = Size(rw, rh), cornerRadius = CornerRadius(3f, 3f))
                    drawRoundRect(Color.White, topLeft = Offset(ix - rw / 2f + 2f, iy - rh / 2f + 2f), size = Size(rw - 4f, 4f), cornerRadius = CornerRadius(2f, 2f))
                }
                SlagItemType.PRISMATIC_CORE -> {
                    // Prismatic Alloy Core
                    val pulse = (sin(snapshot.elapsedSeconds * 12f) * 2f).coerceAtLeast(0f)
                    drawCircle(Color(0x7700FF9D), radius = ir * 1.8f + pulse, center = Offset(ix, iy))
                    drawCircle(Color(0xFF00FF9D), radius = ir, center = Offset(ix, iy))
                    drawCircle(Color.White, radius = ir * 0.4f, center = Offset(ix, iy))
                }
                SlagItemType.VOLATILE_BOMB -> {
                    // Dark Crimson Hazard Bomb
                    val blink = if ((snapshot.elapsedSeconds * 8f).toInt() % 2 == 0) HazardRed else Color(0xFF6B0F1A)
                    drawCircle(blink, radius = ir * 1.2f, center = Offset(ix, iy))
                    drawCircle(Color.Black, radius = ir * 0.6f, center = Offset(ix, iy))
                    drawCircle(HazardRed, radius = ir * 0.25f, center = Offset(ix, iy))
                }
            }
        }

        // 3. The Crucible Bucket Paddle
        val pw = snapshot.paddleWidth * w
        val ph = 24.dp.toPx()
        val px = snapshot.paddleX * w - pw / 2f
        val py = 0.88f * h

        // Steam Vent Plume
        if (snapshot.steamVentingSeconds > 0f) {
            val alpha = (snapshot.steamVentingSeconds / 0.6f).coerceIn(0f, 0.8f)
            drawCircle(SteamWhite.copy(alpha = alpha), radius = pw * 0.7f, center = Offset(px + pw / 2f, py - 10.dp.toPx()))
            drawCircle(Color.White.copy(alpha = alpha * 0.6f), radius = pw * 0.4f, center = Offset(px + pw / 2f, py - 20.dp.toPx()))
        }

        // Stacked Crucible Buckets
        repeat(snapshot.buckets) { bIndex ->
            val bucketY = py + bIndex * 6.dp.toPx()
            val bucketW = pw - bIndex * 6.dp.toPx()
            val bucketX = snapshot.paddleX * w - bucketW / 2f

            drawRoundRect(CrucibleSteel, topLeft = Offset(bucketX, bucketY), size = Size(bucketW, ph), cornerRadius = CornerRadius(6f, 6f))
            drawRoundRect(Color(0xFF523F36), topLeft = Offset(bucketX, bucketY), size = Size(bucketW, ph), cornerRadius = CornerRadius(6f, 6f), style = Stroke(1.5f))

            // Molten core inside top bucket
            if (bIndex == 0) {
                val heatAlpha = (snapshot.heat / 100f).coerceIn(0.2f, 1f)
                drawRoundRect(MoltenOrange.copy(alpha = heatAlpha), topLeft = Offset(bucketX + 3f, bucketY + 3f), size = Size(bucketW - 6f, 6.dp.toPx()), cornerRadius = CornerRadius(3f, 3f))
            }
        }

        // 4. Waste Drain Bottom Lip
        drawLine(Color(0xFF33160A), Offset(0f, 0.96f * h), Offset(w, 0.96f * h), 3f)

        // Scanlines
        for (i in 0..18) {
            val y = i * 20.dp.toPx()
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .06f else .16f), Offset(0f, y), Offset(w, y), 1f)
        }
    }
}

@Composable
private fun SlagControls(
    input: SlagCatcherInput,
    heat: Float,
    large: Boolean,
    onUpdateInput: ((SlagCatcherInput) -> SlagCatcherInput) -> Unit
) {
    val buttonSize = if (large) 58.dp else 48.dp
    val ventSize = if (large) 76.dp else 64.dp
    val canVent = heat > 20f
    val latestUpdate by rememberUpdatedState(onUpdateInput)

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF140804), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF38190B), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: Steer Left / Right
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SteerButton("◀ STEER", buttonSize, input.moveLeft) { held -> onUpdateInput { it.copy(moveLeft = held, paddleTargetX = null) } }
            SteerButton("STEER ▶", buttonSize, input.moveRight) { held -> onUpdateInput { it.copy(moveRight = held, paddleTargetX = null) } }
        }

        // CENTER DIVIDER
        Box(
            Modifier
                .width(1.5.dp)
                .height(buttonSize)
                .background(Color(0xFF2E1308))
        )

        // RIGHT: Large Vent Steam Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("HEAT PURGE", color = if (canVent) SteamWhite else Color(0xFF7A5C4D), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(ventSize)
                    .graphicsLayer {
                        scaleX = if (input.ventSteam) .92f else 1f
                        scaleY = if (input.ventSteam) .92f else 1f
                        alpha = if (input.ventSteam) .85f else 1f
                    }
                    .background(Color(0xFF0A0402), RoundedCornerShape(12.dp))
                    .border(2.dp, if (canVent) SteamWhite else Color(0xFF38190B), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (canVent) Color(0xFF4A6B82) else Color(0xFF1F120B), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                latestUpdate { it.copy(ventSteam = true) }
                                waitForUpOrCancellation()
                                latestUpdate { it.copy(ventSteam = false) }
                            }
                        }
                        .testTag("arcade_slag_vent"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VENT", color = if (canVent) WarmIvory else Color(0xFF6B4E3D), fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SteerButton(
    glyph: String,
    size: Dp,
    pressed: Boolean,
    onHeld: (Boolean) -> Unit
) {
    val latestOnHeld by rememberUpdatedState(onHeld)
    Box(
        Modifier
            .width(size * 1.5f)
            .height(size)
            .graphicsLayer {
                scaleX = if (pressed) .92f else 1f
                scaleY = if (pressed) .92f else 1f
            }
            .background(Color(0xFF0E0603), RoundedCornerShape(6.dp))
            .border(1.dp, if (pressed) MoltenOrange else Color(0xFF3B180A), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (pressed) Color(0xFF451F0D) else Color(0xFF1F0C05), RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        latestOnHeld(true)
                        waitForUpOrCancellation()
                        latestOnHeld(false)
                    }
                }
                .testTag("steer_$glyph"),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SlagServiceKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140804), contentColor = Color(0xFFB89682)),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(32.dp)
            .border(1.dp, Color(0xFF38190B), RoundedCornerShape(4.dp)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun SlagOverlay(
    title: String,
    lines: List<String>,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth(.85f)
            .background(Color(0xF2120804), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF5C2D16), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// SMELTER BRIEF //", color = Color(0xFF9E7C68), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = MoltenOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("0${index + 1}", color = BlastYellow, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, lineHeight = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = MoltenOrange, contentColor = Color(0xFF0E0502)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(38.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
        if (secondaryAction != null && onSecondaryAction != null) {
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onSecondaryAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F0E07), contentColor = Color(0xFFE6CBB8)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth(.75f)
                    .height(36.dp)
                    .border(1.dp, Color(0xFF5C2D16), RoundedCornerShape(4.dp)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(secondaryAction, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = .8.sp)
            }
        }
    }
}
