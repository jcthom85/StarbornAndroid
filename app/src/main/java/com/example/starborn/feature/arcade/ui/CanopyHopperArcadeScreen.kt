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
import androidx.compose.ui.graphics.Path
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
import com.example.starborn.feature.arcade.games.canopyhopper.*
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val EmeraldPhosphor = Color(0xFF00FF9D)
private val SwampCabinet = Color(0xFF0F1A15)
private val SwampWater = Color(0xFF071B14)
private val MossGreen = Color(0xFF2C6B4F)
private val GoldSpore = Color(0xFFFFD700)
private val VoltCyan = Color(0xFF00E5FF)
private val DangerOrange = Color(0xFFFF6B52)
private val WarmIvory = Color(0xFFFFE7B0)

@Composable
fun CanopyHopperArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit
) {
    val engine = remember { CanopyHopperEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(CanopyHopperInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.CANOPY_HOPPER).highScore, submitted?.highScore ?: 0)

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
            submitted = arcadeService.submitCanopyHopperScore(snapshot.score)
            onPlayCue("sfx_arcade_game_over")
        }
    }

    LaunchedEffect(snapshot.audioEvent) {
        when (snapshot.audioEvent) {
            CanopyHopperAudioEvent.HOP -> onPlayCue("sfx_arcade_jump")
            CanopyHopperAudioEvent.NEST_CLAIMED -> onPlayCue("confirm")
            CanopyHopperAudioEvent.PICKUP -> onPlayCue("sfx_arcade_laser")
            CanopyHopperAudioEvent.ROUND_CLEAR -> onPlayCue("confirm")
            CanopyHopperAudioEvent.SPLAT, CanopyHopperAudioEvent.SPLASH -> onPlayCue("action_inspect")
            CanopyHopperAudioEvent.GAME_OVER -> onPlayCue("sfx_arcade_game_over")
            CanopyHopperAudioEvent.NONE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050C09))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CanopyMarquee()
        Spacer(Modifier.height(4.dp))
        CanopyScoreConsole(snapshot.score, highScore, snapshot.multiplier, snapshot.lives, snapshot.round)
        Spacer(Modifier.height(4.dp))
        FogBar(snapshot.poisonFog)
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SwampCabinet, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF1F4A38), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            CanopyCanvas(
                snapshot = snapshot,
                reducedFlashes = reducedFlashes,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("canopy_hopper_viewport")
            )

            if (tutorial) {
                CanopyOverlay(
                    title = "HOW TO HOP",
                    lines = listOf(
                        "Tap D-Pad or HOP button to leap across lanes.",
                        "Cross the Predator Thicket without getting crushed.",
                        "Rest on the Mud Bank and snag Golden Spores.",
                        "Ride logs and lilypads across the rapids.",
                        "Avoid flashing Volt Eels and reach the Canopy Nests!",
                        "Fill all 5 nests before Poison Fog fills the swamp."
                    ),
                    action = "START RUN",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                CanopyOverlay("RUN PAUSED", listOf("Bog hopper is holding position."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                CanopyOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE: ${snapshot.score}")
                        add("TIER REACHED: ROUND ${snapshot.round}")
                        if (tiers.isNotBlank()) add("REWARD UNLOCKED: $tiers")
                        else add("Drop one more coin to beat the high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = CanopyHopperInput()
                        onPlayCue("confirm")
                    }
                )
            }
        }

        CanopyStatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(4.dp))
        CanopyControls(input, largeTouchTargets) { input = it }
        Spacer(Modifier.height(4.dp))
        CanopyServiceKey { paused = true }
    }
}

@Composable
private fun CanopyMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F261C), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF266147), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CANOPY HOPPER", color = EmeraldPhosphor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
        Text("SECTOR 9 SURVIVAL POD // CABINET 02", color = WarmIvory, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun CanopyScoreConsole(score: Int, highScore: Int, multiplier: Int, lives: Int, round: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1E16), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF1E4634), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(56.dp), VoltCyan)
        ScoreCell("LIVES", "♥".repeat(lives.coerceAtLeast(0)), Modifier.width(62.dp), DangerOrange)
        ScoreCell("ROUND", "$round", Modifier.width(46.dp), GoldSpore)
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = EmeraldPhosphor) {
    Column(
        modifier
            .background(Color(0xFF08140E), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1A382A), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF759E8B), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .6.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun FogBar(poisonFog: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C1913), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1F3D2E), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("POISON FOG", color = if (poisonFog > 70f) DangerOrange else Color(0xFF7CA692), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF07120C), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(poisonFog / 100f)
                    .background(if (poisonFog > 70f) DangerOrange else Color(0xFF7B2CBF), RoundedCornerShape(3.dp))
            )
        }
        Text("${poisonFog.toInt()}%", color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CanopyStatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1C15), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF224835), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (failure) DangerOrange else EmeraldPhosphor, RoundedCornerShape(50))
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
private fun CanopyCanvas(
    snapshot: CanopyHopperSnapshot,
    reducedFlashes: Boolean,
    modifier: Modifier
) {
    Canvas(modifier.background(Color(0xFF05100B), RoundedCornerShape(8.dp))) {
        val sx = size.width / CanopyHopperEngine.WORLD_WIDTH
        val sy = size.height / CanopyHopperEngine.WORLD_HEIGHT

        // 1. Lane 0: Canopy Nests (Top Safe Zone)
        drawRect(Color(0xFF0E2A1C), topLeft = Offset(0f, 0f), size = Size(size.width, 36f * sy))
        repeat(5) { i ->
            val nestX = (i * 2 + 1) * 32f * sx
            val nestW = 32f * sx
            val occupied = snapshot.nests.getOrElse(i) { false }
            drawRect(if (occupied) Color(0xFF1F5C3B) else Color(0xFF071A10), topLeft = Offset(nestX, 4f * sy), size = Size(nestW, 28f * sy))
            drawRect(if (occupied) EmeraldPhosphor else MossGreen, topLeft = Offset(nestX, 4f * sy), size = Size(nestW, 28f * sy), style = Stroke(1.5f))
            if (occupied) {
                drawCircle(EmeraldPhosphor, radius = 5f * sx, center = Offset(nestX + nestW / 2f, 18f * sy))
            }
        }

        // 2. Lanes 1, 2, 3: Swamp Rapids (Water)
        drawRect(SwampWater, topLeft = Offset(0f, 36f * sy), size = Size(size.width, 108f * sy))
        // Water ripples
        repeat(12) { index ->
            val rippleY = (36f + (index * 9f + snapshot.elapsedSeconds * 12f) % 108f) * sy
            val rippleX = ((index * 37f + snapshot.elapsedSeconds * 20f) % CanopyHopperEngine.WORLD_WIDTH) * sx
            drawLine(Color(0x3300FF9D), Offset(rippleX, rippleY), Offset(rippleX + 18f * sx, rippleY), 1f)
        }

        // Render River Platforms (Logs, Lilypads, Volt Eels)
        snapshot.platforms.forEach { plat ->
            val px = plat.x * sx
            val py = plat.lane * 36f * sy + 5f * sy
            val pw = plat.width * sx
            val ph = 26f * sy

            if (plat.isLilypad) {
                drawCircle(Color(0xFF1E5E3A), radius = pw / 2f, center = Offset(px + pw / 2f, py + ph / 2f))
                drawCircle(EmeraldPhosphor, radius = pw / 2f, center = Offset(px + pw / 2f, py + ph / 2f), style = Stroke(1.5f))
            } else if (plat.isEel) {
                val eelColor = if (plat.electrified) VoltCyan else Color(0xFF1E4D40)
                drawRoundRect(eelColor, topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                if (plat.electrified) {
                    drawRect(Color.White, topLeft = Offset(px + 4f, py + 4f), size = Size(pw - 8f, ph - 8f))
                }
            } else {
                // Moss Log
                drawRoundRect(Color(0xFF382312), topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                drawRect(MossGreen, topLeft = Offset(px, py), size = Size(pw, 6f * sy))
                drawRoundRect(Color(0xFF5E3C1F), topLeft = Offset(px, py), size = Size(pw, ph), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(1.5f))
            }
        }

        // 3. Lane 4: Mud Bank Island (Center Safe Zone)
        drawRect(Color(0xFF26190E), topLeft = Offset(0f, 144f * sy), size = Size(size.width, 36f * sy))
        drawLine(Color(0xFF4A3420), Offset(0f, 144f * sy), Offset(size.width, 144f * sy), 2f)
        drawLine(Color(0xFF4A3420), Offset(0f, 180f * sy), Offset(size.width, 180f * sy), 2f)

        // 4. Lanes 5, 6, 7: Predator Thicket (Road Hazards)
        drawRect(Color(0xFF101712), topLeft = Offset(0f, 180f * sy), size = Size(size.width, 108f * sy))
        // Thicket lines
        for (lane in 5..7) {
            val y = (lane * 36f + 35f) * sy
            drawLine(Color(0x333F6B52), Offset(0f, y), Offset(size.width, y), 1f)
        }

        // Render Hazards
        snapshot.hazards.forEach { h ->
            val hx = h.x * sx
            val hy = h.lane * 36f * sy + 6f * sy
            val hw = h.width * sx
            val hh = 24f * sy

            when (h.type) {
                "skimmer" -> {
                    drawRoundRect(Color(0xFF8B261D), topLeft = Offset(hx, hy), size = Size(hw, hh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                    drawRect(DangerOrange, topLeft = Offset(hx + 2f, hy + 2f), size = Size(hw - 4f, 4f * sy))
                }
                "centipede" -> {
                    drawRoundRect(Color(0xFF6B441D), topLeft = Offset(hx, hy), size = Size(hw, hh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                    drawCircle(DangerOrange, radius = 3f * sx, center = Offset(hx + 4f * sx, hy + hh / 2f))
                }
                else -> {
                    drawRect(Color(0xFF3B1E28), topLeft = Offset(hx, hy), size = Size(hw, hh))
                }
            }
        }

        // 5. Lane 8: Starting Shore (Bottom Safe Zone)
        drawRect(Color(0xFF1A2E22), topLeft = Offset(0f, 288f * sy), size = Size(size.width, 72f * sy))
        drawLine(Color(0xFF38664D), Offset(0f, 288f * sy), Offset(size.width, 288f * sy), 2f)

        // Pickups (Golden Spores & Fireflies)
        snapshot.pickups.filter { !it.collected }.forEach { pick ->
            val px = pick.x * sx
            val py = pick.lane * 36f * sy + 18f * sy
            if (pick.type == "golden_spore") {
                val glowR = (5f + sin(snapshot.elapsedSeconds * 8f) * 1.5f) * sx
                drawCircle(Color(0x66FFD700), radius = glowR, center = Offset(px, py))
                drawCircle(GoldSpore, radius = 3.5f * sx, center = Offset(px, py))
            } else {
                drawCircle(Color(0x7700FF9D), radius = 4.5f * sx, center = Offset(px, py))
                drawCircle(Color.White, radius = 2f * sx, center = Offset(px, py))
            }
        }

        // 6. The Bog Hopper (Player Character)
        val frogX = snapshot.hopperPixelX * sx
        val frogY = snapshot.hopperPixelY * sy
        val frogRadius = 9f * sx
        val isJumping = snapshot.hopProgress < 1f
        val jumpHeight = if (isJumping) sin(snapshot.hopProgress * 3.14159f) * 8f * sy else 0f

        // Hopper Shadow
        drawCircle(Color(0x55000000), radius = frogRadius * 0.9f, center = Offset(frogX, frogY))

        // Hopper Body
        val hopperCenter = Offset(frogX, frogY - jumpHeight)
        drawCircle(EmeraldPhosphor, radius = frogRadius, center = hopperCenter)
        drawCircle(Color(0xFF0F4D32), radius = frogRadius * 0.7f, center = hopperCenter)

        // Eyes based on facing
        val eyeOffset1 = when (snapshot.facing) {
            CanopyDirection.UP -> Offset(-4f * sx, -6f * sy)
            CanopyDirection.DOWN -> Offset(-4f * sx, 6f * sy)
            CanopyDirection.LEFT -> Offset(-6f * sx, -4f * sy)
            CanopyDirection.RIGHT -> Offset(6f * sx, -4f * sy)
        }
        val eyeOffset2 = when (snapshot.facing) {
            CanopyDirection.UP -> Offset(4f * sx, -6f * sy)
            CanopyDirection.DOWN -> Offset(4f * sx, 6f * sy)
            CanopyDirection.LEFT -> Offset(-6f * sx, 4f * sy)
            CanopyDirection.RIGHT -> Offset(6f * sx, 4f * sy)
        }
        drawCircle(Color.White, radius = 2.2f * sx, center = hopperCenter + eyeOffset1)
        drawCircle(Color.Black, radius = 1.2f * sx, center = hopperCenter + eyeOffset1)
        drawCircle(Color.White, radius = 2.2f * sx, center = hopperCenter + eyeOffset2)
        drawCircle(Color.Black, radius = 1.2f * sx, center = hopperCenter + eyeOffset2)

        // Scanlines
        for (i in 0..18) {
            val y = i * 20f * sy
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .06f else .16f), Offset(0f, y), Offset(size.width, y), 1f)
        }
    }
}

@Composable
private fun CanopyControls(
    input: CanopyHopperInput,
    large: Boolean,
    onInput: (CanopyHopperInput) -> Unit
) {
    val buttonSize = if (large) 58.dp else 48.dp
    val hopSize = if (large) 76.dp else 64.dp

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C1B14), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF1E4634), RoundedCornerShape(10.dp))
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
                .background(Color(0xFF1C3A2B))
        )

        // RIGHT: Large Glowing HOP Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("PRIMARY LEAP", color = Color(0xFF759E8B), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(hopSize)
                    .graphicsLayer {
                        scaleX = if (input.hop) .92f else 1f
                        scaleY = if (input.hop) .92f else 1f
                        alpha = if (input.hop) .85f else 1f
                    }
                    .background(Color(0xFF08140E), RoundedCornerShape(12.dp))
                    .border(2.dp, if (input.hop) EmeraldPhosphor else Color(0xFF2B704F), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (input.hop) Color(0xFF00FF9D).copy(alpha = 0.8f) else Color(0xFF1E5E3A), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onInput(input.copy(hop = true))
                                waitForUpOrCancellation()
                                onInput(input.copy(hop = false))
                            }
                        }
                        .testTag("arcade_canopy_hop"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("HOP", color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Black)
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
            .background(Color(0xFF08140E), RoundedCornerShape(6.dp))
            .border(1.dp, if (pressed) EmeraldPhosphor else Color(0xFF1E4634), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (pressed) Color(0xFF266147) else Color(0xFF11261D), RoundedCornerShape(4.dp))
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
private fun CanopyServiceKey(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C1913), contentColor = Color(0xFF8DB8A3)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(30.dp)
            .border(1.dp, Color(0xFF204232), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun CanopyOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(.85f)
            .background(Color(0xF2081510), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF266147), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// OPERATOR BRIEF //", color = Color(0xFF759E8B), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = EmeraldPhosphor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("0${index + 1}", color = GoldSpore, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, lineHeight = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPhosphor, contentColor = Color(0xFF08140E)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(38.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}
