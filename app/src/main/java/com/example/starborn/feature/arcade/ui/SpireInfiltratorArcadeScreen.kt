package com.example.starborn.feature.arcade.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.feature.arcade.domain.ArcadeIds
import com.example.starborn.feature.arcade.domain.ArcadeRunSubmission
import com.example.starborn.feature.arcade.domain.ArcadeService
import com.example.starborn.feature.arcade.games.spireinfiltrator.*
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val NeonCyan = Color(0xFF00E5FF)
private val NeonMagenta = Color(0xFFFF007F)
private val MainframeCabinet = Color(0xFF0D0B14)
private val GridFloor = Color(0xFF06050A)
private val CircuitBlue = Color(0xFF1A3A5E)
private val DataGold = Color(0xFFFFD700)
private val VulnerableBlue = Color(0xFF3A86FF)
private val WarmIvory = Color(0xFFFFE7B0)

@Composable
fun SpireInfiltratorArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit
) {
    val engine = remember { SpireInfiltratorEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(InfiltratorInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.SPIRE_INFILTRATOR).highScore, submitted?.highScore ?: 0)

    BackHandler {
        if (!paused && !snapshot.gameOver) paused = true else onBack()
    }

    LaunchedEffect(engine, paused, tutorial) {
        var previous = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previous != 0L && !paused && !tutorial) {
                    engine.advance((now - previous) / 1_000_000_000f, input)
                    snapshot = engine.snapshot()
                }
                previous = now
            }
        }
    }

    LaunchedEffect(snapshot.gameOver) {
        if (snapshot.gameOver && submitted == null) {
            submitted = arcadeService.submitSpireInfiltratorScore(snapshot.score)
            onPlayCue("sfx_arcade_game_over")
        }
    }

    LaunchedEffect(snapshot.audioEvent) {
        when (snapshot.audioEvent) {
            SpireAudioEvent.EAT_NODE -> onPlayCue("sfx_arcade_laser")
            SpireAudioEvent.OVERCLOCK_START -> onPlayCue("confirm")
            SpireAudioEvent.EAT_SENTINEL -> onPlayCue("sfx_arcade_jump")
            SpireAudioEvent.ROUND_CLEAR -> onPlayCue("confirm")
            SpireAudioEvent.DEATH -> onPlayCue("action_inspect")
            SpireAudioEvent.GAME_OVER -> onPlayCue("sfx_arcade_game_over")
            SpireAudioEvent.NONE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040308))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpireMarquee()
        Spacer(Modifier.height(4.dp))
        SpireScoreConsole(snapshot.score, highScore, snapshot.multiplier, snapshot.lives, snapshot.round)
        Spacer(Modifier.height(4.dp))
        OverclockGauge(snapshot.overclockSeconds)
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MainframeCabinet, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF332052), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            SpireCanvas(
                snapshot = snapshot,
                reducedFlashes = reducedFlashes,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("spire_infiltrator_viewport")
            )

            if (tutorial) {
                SpireOverlay(
                    title = "HOW TO INFILTRATE",
                    lines = listOf(
                        "Use the 4-Way D-Pad to buffer turns ahead of corners.",
                        "Harvest all encrypted Data Nodes across the server grid.",
                        "Evade the 4 patrolling ICE Sentinel security routines.",
                        "Snag OVERCLOCK Nodes to dereference vulnerable Sentinels!",
                        "Clear all nodes in the sector to access the next mainframe layer."
                    ),
                    action = "START INFILTRATION",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                SpireOverlay("RUN PAUSED", listOf("Data packet holding connection."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                SpireOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE: ${snapshot.score}")
                        add("SECTOR LAYER: ROUND ${snapshot.round}")
                        if (tiers.isNotBlank()) add("REWARD UNLOCKED: $tiers")
                        else add("Drop one more coin to beat the high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = InfiltratorInput()
                        onPlayCue("confirm")
                    }
                )
            }
        }

        SpireStatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(4.dp))
        SpireControls(input, largeTouchTargets) { input = it }
        Spacer(Modifier.height(4.dp))
        SpireServiceKey { paused = true }
    }
}

@Composable
private fun SpireMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A0E2E), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF4C2782), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SPIRE INFILTRATOR", color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
        Text("EXEC LOUNGE // CABINET 03", color = NeonMagenta, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SpireScoreConsole(score: Int, highScore: Int, multiplier: Int, lives: Int, round: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF120B22), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF2E1954), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(56.dp), NeonCyan)
        ScoreCell("PACKETS", "◆".repeat(lives.coerceAtLeast(0)), Modifier.width(62.dp), NeonMagenta)
        ScoreCell("LAYER", "$round", Modifier.width(46.dp), DataGold)
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = NeonCyan) {
    Column(
        modifier
            .background(Color(0xFF080410), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF261245), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF8E7AA6), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .6.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun OverclockGauge(overclockSeconds: Float) {
    val active = overclockSeconds > 0f
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF100A1C), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF2C164D), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("OVERCLOCK", color = if (active) NeonMagenta else Color(0xFF796590), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF080410), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((overclockSeconds / 8.0f).coerceIn(0f, 1f))
                    .background(if (active) NeonMagenta else Color(0xFF3B1E63), RoundedCornerShape(3.dp))
            )
        }
        Text(if (active) "%.1fS".format(overclockSeconds) else "READY", color = if (active) NeonCyan else WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpireStatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF100A1C), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF2F1852), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (failure) Color(0xFFFF4D4D) else NeonCyan, RoundedCornerShape(50))
        )
        Text(
            message,
            color = if (failure) Color(0xFFFF8E8E) else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun SpireCanvas(
    snapshot: SpireInfiltratorSnapshot,
    reducedFlashes: Boolean,
    modifier: Modifier
) {
    Canvas(modifier.background(GridFloor, RoundedCornerShape(8.dp))) {
        val sx = size.width / (SpireInfiltratorEngine.COLS * SpireInfiltratorEngine.TILE_SIZE + 20f)
        val sy = size.height / (SpireInfiltratorEngine.ROWS * SpireInfiltratorEngine.TILE_SIZE + 40f)

        // 1. Draw Maze Circuit Grid
        for (r in 0 until SpireInfiltratorEngine.ROWS) {
            for (c in 0 until SpireInfiltratorEngine.COLS) {
                val tileX = (c * SpireInfiltratorEngine.TILE_SIZE + SpireInfiltratorEngine.OFFSET_X) * sx
                val tileY = (r * SpireInfiltratorEngine.TILE_SIZE + SpireInfiltratorEngine.OFFSET_Y) * sy
                val tileW = SpireInfiltratorEngine.TILE_SIZE * sx
                val tileH = SpireInfiltratorEngine.TILE_SIZE * sy

                val char = snapshot.grid.getOrNull(r)?.getOrNull(c) ?: ' '
                when (char) {
                    '#' -> {
                        // Neon Wall
                        drawRect(Color(0xFF110B22), topLeft = Offset(tileX, tileY), size = Size(tileW, tileH))
                        drawRect(CircuitBlue, topLeft = Offset(tileX + 1f, tileY + 1f), size = Size(tileW - 2f, tileH - 2f), style = Stroke(1f))
                    }
                    '.' -> {
                        // Data Node
                        drawCircle(DataGold, radius = 2.2f * sx, center = Offset(tileX + tileW / 2f, tileY + tileH / 2f))
                    }
                    'O' -> {
                        // Overclock Node
                        val pulse = (4.5f + sin(snapshot.elapsedSeconds * 10f) * 1.5f) * sx
                        drawCircle(Color(0x66FF007F), radius = pulse * 1.4f, center = Offset(tileX + tileW / 2f, tileY + tileH / 2f))
                        drawCircle(NeonMagenta, radius = pulse, center = Offset(tileX + tileW / 2f, tileY + tileH / 2f))
                        drawCircle(Color.White, radius = 2f * sx, center = Offset(tileX + tileW / 2f, tileY + tileH / 2f))
                    }
                }
            }
        }

        // 2. Render Sentinels
        snapshot.sentinels.forEach { s ->
            val sxPos = s.pixelX * sx
            val syPos = s.pixelY * sy
            val sRadius = 7.5f * sx

            if (s.state == SentinelState.DEREFERENCED) {
                // Ghost Address
                drawCircle(Color(0x3300E5FF), radius = sRadius * 0.7f, center = Offset(sxPos, syPos))
            } else if (s.state == SentinelState.VULNERABLE) {
                // Vulnerable Sentinel (Blue/Cyan Glitching)
                drawCircle(VulnerableBlue, radius = sRadius, center = Offset(sxPos, syPos))
                drawCircle(Color.White, radius = sRadius * 0.4f, center = Offset(sxPos, syPos))
            } else {
                // Active Sentinel
                val color = when (s.name) {
                    "Red" -> Color(0xFFFF3366)
                    "Cyan" -> NeonCyan
                    "Gold" -> DataGold
                    else -> Color(0xFFBD00FF)
                }
                drawCircle(color, radius = sRadius, center = Offset(sxPos, syPos))
                // Eyes
                drawCircle(Color.White, radius = 2.2f * sx, center = Offset(sxPos - 2.5f * sx, syPos - 1.5f * sy))
                drawCircle(Color.White, radius = 2.2f * sx, center = Offset(sxPos + 2.5f * sx, syPos - 1.5f * sy))
                drawCircle(Color.Black, radius = 1.2f * sx, center = Offset(sxPos - 2.5f * sx, syPos - 1.5f * sy))
                drawCircle(Color.Black, radius = 1.2f * sx, center = Offset(sxPos + 2.5f * sx, syPos - 1.5f * sy))
            }
        }

        // 3. Render Player Data Packet
        val px = snapshot.playerPixelX * sx
        val py = snapshot.playerPixelY * sy
        val pRadius = 8f * sx

        // Glowing core diamond
        drawCircle(Color(0x4400E5FF), radius = pRadius * 1.5f, center = Offset(px, py))
        drawCircle(NeonCyan, radius = pRadius, center = Offset(px, py))
        drawCircle(Color.White, radius = pRadius * 0.5f, center = Offset(px, py))

        // Trailing bits
        if (snapshot.playerDir != InfiltratorDir.NONE) {
            val tailX = px - snapshot.playerDir.dx * 10f * sx
            val tailY = py - snapshot.playerDir.dy * 10f * sy
            drawCircle(Color(0x8800E5FF), radius = 3.5f * sx, center = Offset(tailX, tailY))
        }

        // Scanlines
        for (i in 0..18) {
            val y = i * 20f * sy
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .06f else .16f), Offset(0f, y), Offset(size.width, y), 1f)
        }
    }
}

@Composable
private fun SpireControls(
    input: InfiltratorInput,
    large: Boolean,
    onInput: (InfiltratorInput) -> Unit
) {
    val buttonSize = if (large) 58.dp else 48.dp
    val boostSize = if (large) 76.dp else 64.dp

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0A1C), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF2B164D), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: 4-Way D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DpadButton("▲", buttonSize, input.up) { onInput(input.copy(up = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DpadButton("◀", buttonSize, input.left) { onInput(input.copy(left = it)) }
                DpadButton("▼", buttonSize, input.down) { onInput(input.copy(down = it)) }
                DpadButton("▶", buttonSize, input.right) { onInput(input.copy(right = it)) }
            }
        }

        // CENTER DIVIDER
        Box(
            Modifier
                .width(1.5.dp)
                .height(buttonSize * 2)
                .background(Color(0xFF281447))
        )

        // RIGHT: Boost / Dash Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("DATA OVERDRIVE", color = Color(0xFF8E7AA6), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(boostSize)
                    .graphicsLayer {
                        scaleX = if (input.boost) .92f else 1f
                        scaleY = if (input.boost) .92f else 1f
                        alpha = if (input.boost) .85f else 1f
                    }
                    .background(Color(0xFF080410), RoundedCornerShape(12.dp))
                    .border(2.dp, if (input.boost) NeonMagenta else Color(0xFF4C2782), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (input.boost) NeonMagenta.copy(alpha = 0.8f) else Color(0xFF331659), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onInput(input.copy(boost = true))
                                waitForUpOrCancellation()
                                onInput(input.copy(boost = false))
                            }
                        }
                        .testTag("arcade_spire_boost"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BOOST", color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DpadButton(
    glyph: String,
    size: Dp,
    pressed: Boolean,
    onHeld: (Boolean) -> Unit
) {
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = if (pressed) .92f else 1f
                scaleY = if (pressed) .92f else 1f
            }
            .background(Color(0xFF080410), RoundedCornerShape(6.dp))
            .border(1.dp, if (pressed) NeonCyan else Color(0xFF261445), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (pressed) Color(0xFF2E1954) else Color(0xFF140A26), RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onHeld(true)
                        waitForUpOrCancellation()
                        onHeld(false)
                    }
                }
                .testTag("dpad_$glyph"),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, color = WarmIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SpireServiceKey(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E071A), contentColor = Color(0xFF9E8DB8)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(30.dp)
            .border(1.dp, Color(0xFF2E1754), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun SpireOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(.85f)
            .background(Color(0xF20B0617), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF4C2782), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// MAINFRAME BRIEF //", color = Color(0xFF8E7AA6), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("0${index + 1}", color = NeonMagenta, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, lineHeight = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF06030D)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(38.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}
