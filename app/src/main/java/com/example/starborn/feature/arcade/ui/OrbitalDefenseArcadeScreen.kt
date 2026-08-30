package com.example.starborn.feature.arcade.ui

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
import com.example.starborn.feature.arcade.games.orbitaldefense.*
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val CobaltBlue = Color(0xFF00B4D8)
private val ElectricCyan = Color(0xFF90E0EF)
private val SpaceBlack = Color(0xFF03071E)
private val StarfighterWhite = Color(0xFFCAF0F8)
private val LaserBlue = Color(0xFF0077B6)
private val HazardOrange = Color(0xFFFF9E00)
private val MysteryGold = Color(0xFFFFD60A)
private val WarmIvory = Color(0xFFFFE7B0)

@Composable
fun OrbitalDefenseArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit
) {
    val engine = remember { OrbitalDefenseEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(OrbitalDefenseInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.ORBITAL_DEFENSE).highScore, submitted?.highScore ?: 0)

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
            submitted = arcadeService.submitOrbitalDefenseScore(snapshot.score)
            onPlayCue("sfx_arcade_game_over")
        }
    }

    LaunchedEffect(snapshot.audioEvent) {
        when (snapshot.audioEvent) {
            OrbitalAudioEvent.PLAYER_FIRE -> onPlayCue("sfx_arcade_laser")
            OrbitalAudioEvent.ENEMY_EXPLODE -> onPlayCue("sfx_arcade_laser")
            OrbitalAudioEvent.MYSTERY_SPAWN -> onPlayCue("confirm")
            OrbitalAudioEvent.EMP_BLAST -> onPlayCue("sfx_arcade_jump")
            OrbitalAudioEvent.PLAYER_HIT -> onPlayCue("action_inspect")
            OrbitalAudioEvent.ROUND_CLEAR -> onPlayCue("confirm")
            OrbitalAudioEvent.GAME_OVER -> onPlayCue("sfx_arcade_game_over")
            OrbitalAudioEvent.NONE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02040A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OrbitalMarquee()
        Spacer(Modifier.height(4.dp))
        OrbitalScoreConsole(snapshot.score, highScore, snapshot.multiplier, snapshot.lives, snapshot.empBombs, snapshot.round)
        Spacer(Modifier.height(4.dp))
        EmpStatusGauge(snapshot.empActiveSeconds, snapshot.empBombs)
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SpaceBlack, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF14213D), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            OrbitalCanvas(
                snapshot = snapshot,
                reducedFlashes = reducedFlashes,
                onDirectTouch = { input = input.copy(shipTargetX = it) },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("orbital_defense_viewport")
            )

            if (tutorial) {
                OrbitalOverlay(
                    title = "ORBITAL DEFENSE BRIEF",
                    lines = listOf(
                        "Steer your starfighter across the orbital defense line.",
                        "Hold FIRE to unleash Twin Plasma Cannons.",
                        "Destroy Flagships, Cruisers, and Swarm Drones.",
                        "Shoot diving enemies for 2× BONUS points!",
                        "Intercept the high-value Mystery Mothership.",
                        "Trigger EMP BOMBS to clear all enemy plasma volleys."
                    ),
                    action = "ENGAGE ENEMY FLEET",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                OrbitalOverlay("PATROL PAUSED", listOf("Starfighter holding orbital perimeter."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                OrbitalOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE: ${snapshot.score}")
                        add("SECTOR DEFENDED: ROUND ${snapshot.round}")
                        if (tiers.isNotBlank()) add("REWARD UNLOCKED: $tiers")
                        else add("Drop one more coin to beat the high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = OrbitalDefenseInput()
                        onPlayCue("confirm")
                    }
                )
            }
        }

        OrbitalStatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(4.dp))
        OrbitalControls(input, snapshot.empBombs, largeTouchTargets) { input = it }
        Spacer(Modifier.height(4.dp))
        OrbitalServiceKey { paused = true }
    }
}

@Composable
private fun OrbitalMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B192C), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF1E3E62), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ORBITAL DEFENSE 2000", color = CobaltBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
        Text("ZENITH RING PERIMETER // CABINET 05", color = ElectricCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun OrbitalScoreConsole(score: Int, highScore: Int, multiplier: Int, lives: Int, empBombs: Int, round: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF071220), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF132B45), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(54.dp), ElectricCyan)
        ScoreCell("SHIELDS", "▲".repeat(lives.coerceAtLeast(0)), Modifier.width(60.dp), CobaltBlue)
        ScoreCell("SECTOR", "$round", Modifier.width(46.dp), MysteryGold)
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = CobaltBlue) {
    Column(
        modifier
            .background(Color(0xFF040A12), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF0D1E30), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF6B8CA8), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .6.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun EmpStatusGauge(empActiveSeconds: Float, empBombs: Int) {
    val active = empActiveSeconds > 0f
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF071220), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF152E4A), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("EMP CHARGES: ${"⚡".repeat(empBombs)}", color = if (empBombs > 0) ElectricCyan else Color(0xFF5E7B94), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF040A12), RoundedCornerShape(3.dp))
        ) {
            if (active) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((empActiveSeconds / 3.0f).coerceIn(0f, 1f))
                        .background(ElectricCyan, RoundedCornerShape(3.dp))
                )
            }
        }
        Text(if (active) "EMP ACTIVE %.1fS".format(empActiveSeconds) else "READY", color = if (active) ElectricCyan else WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OrbitalStatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF091422), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF1B3859), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (failure) Color(0xFFFF4D4D) else CobaltBlue, RoundedCornerShape(50))
        )
        Text(
            message,
            color = if (failure) Color(0xFFFF9E9E) else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun OrbitalCanvas(
    snapshot: OrbitalDefenseSnapshot,
    reducedFlashes: Boolean,
    onDirectTouch: (Float) -> Unit,
    modifier: Modifier
) {
    Canvas(
        modifier
            .background(Color(0xFF020409), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onDirectTouch(fraction)
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Parallax Starfield
        repeat(24) { i ->
            val starY = ((i * 31f + snapshot.elapsedSeconds * (15f + (i % 3) * 15f)) % 360f) / 360f * h
            val starX = ((i * 47f) % 320f) / 320f * w
            val alpha = (0.3f + (i % 4) * 0.2f)
            drawCircle(Color.White.copy(alpha = alpha), radius = 1.2f, center = Offset(starX, starY))
        }

        // EMP Blast Wave Effect
        if (snapshot.empActiveSeconds > 0f) {
            val empAlpha = (snapshot.empActiveSeconds / 3.0f * 0.25f)
            drawRect(ElectricCyan.copy(alpha = empAlpha), topLeft = Offset(0f, 0f), size = Size(w, h))
        }

        // 2. Render Projectiles
        snapshot.projectiles.forEach { p ->
            val px = p.x * w
            val py = p.y * h
            if (p.isPlayer) {
                // Player Twin Plasma Laser
                drawLine(LaserBlue, Offset(px, py), Offset(px, py + 8.dp.toPx()), 2.5f)
                drawLine(StarfighterWhite, Offset(px, py), Offset(px, py + 5.dp.toPx()), 1.2f)
            } else {
                // Enemy Energy Bolt
                drawCircle(HazardOrange, radius = 3.dp.toPx(), center = Offset(px, py))
                drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(px, py))
            }
        }

        // 3. Render Enemies
        snapshot.enemies.forEach { e ->
            val ex = e.x * w
            val ey = e.y * h
            val er = 10.dp.toPx()

            when (e.type) {
                EnemyType.FLAGSHIP -> {
                    // Command Flagship (Purple/Gold)
                    val shipPath = Path().apply {
                        moveTo(ex, ey + er)
                        lineTo(ex - er * 1.2f, ey - er * 0.6f)
                        lineTo(ex, ey - er * 0.2f)
                        lineTo(ex + er * 1.2f, ey - er * 0.6f)
                        close()
                    }
                    drawPath(shipPath, Color(0xFF7209B7))
                    drawPath(shipPath, MysteryGold, style = Stroke(1.5f))
                    drawCircle(MysteryGold, radius = 3f, center = Offset(ex, ey))
                }
                EnemyType.CRUISER -> {
                    // Laser Cruiser (Cyan/Blue)
                    val shipPath = Path().apply {
                        moveTo(ex, ey + er * 0.8f)
                        lineTo(ex - er, ey - er * 0.5f)
                        lineTo(ex + er, ey - er * 0.5f)
                        close()
                    }
                    drawPath(shipPath, CobaltBlue)
                    drawPath(shipPath, ElectricCyan, style = Stroke(1.2f))
                }
                EnemyType.SWARM -> {
                    // Swarm Fighter (Crimson/Orange)
                    drawCircle(Color(0xFFE63946), radius = er * 0.8f, center = Offset(ex, ey))
                    drawCircle(HazardOrange, radius = er * 0.8f, center = Offset(ex, ey), style = Stroke(1.2f))
                }
                EnemyType.MYSTERY -> {
                    // Mystery Mothership
                    drawRoundRect(MysteryGold, topLeft = Offset(ex - er * 1.8f, ey - er * 0.6f), size = Size(er * 3.6f, er * 1.2f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(Color.White, topLeft = Offset(ex - er * 1.2f, ey - er * 0.3f), size = Size(er * 2.4f, er * 0.6f), cornerRadius = CornerRadius(2f, 2f))
                }
            }
        }

        // 4. Render Player Starfighter
        val sx = snapshot.playerX * w
        val sy = OrbitalDefenseEngine.PLAYER_Y * h
        val sr = 12.dp.toPx()

        // Starfighter Chassis
        val fighterPath = Path().apply {
            moveTo(sx, sy - sr * 1.2f)
            lineTo(sx - sr, sy + sr * 0.8f)
            lineTo(sx - sr * 0.4f, sy + sr * 0.4f)
            lineTo(sx, sy + sr * 0.6f)
            lineTo(sx + sr * 0.4f, sy + sr * 0.4f)
            lineTo(sx + sr, sy + sr * 0.8f)
            close()
        }
        drawPath(fighterPath, StarfighterWhite)
        drawPath(fighterPath, CobaltBlue, style = Stroke(1.5f))
        drawCircle(ElectricCyan, radius = 3.5f, center = Offset(sx, sy))

        // Engine Plumes
        val engineGlow = (sin(snapshot.elapsedSeconds * 20f) * 2f).coerceAtLeast(0f)
        drawLine(ElectricCyan, Offset(sx - sr * 0.5f, sy + sr * 0.6f), Offset(sx - sr * 0.5f, sy + sr * 0.6f + 5.dp.toPx() + engineGlow), 2f)
        drawLine(ElectricCyan, Offset(sx + sr * 0.5f, sy + sr * 0.6f), Offset(sx + sr * 0.5f, sy + sr * 0.6f + 5.dp.toPx() + engineGlow), 2f)

        // 5. Scanlines
        for (i in 0..18) {
            val y = i * 20.dp.toPx()
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .06f else .16f), Offset(0f, y), Offset(w, y), 1f)
        }
    }
}

@Composable
private fun OrbitalControls(
    input: OrbitalDefenseInput,
    empBombs: Int,
    large: Boolean,
    onInput: (OrbitalDefenseInput) -> Unit
) {
    val buttonSize = if (large) 58.dp else 48.dp
    val actionSize = if (large) 76.dp else 64.dp

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF091422), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF18324F), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: Steer Left / Right
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SteerButton("◀", buttonSize, input.moveLeft) { onInput(input.copy(moveLeft = it, shipTargetX = null)) }
            SteerButton("▶", buttonSize, input.moveRight) { onInput(input.copy(moveRight = it, shipTargetX = null)) }
        }

        // CENTER DIVIDER
        Box(
            Modifier
                .width(1.5.dp)
                .height(buttonSize)
                .background(Color(0xFF132B45))
        )

        // RIGHT: FIRE + EMP Action Keys
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // EMP Button
            Box(
                Modifier
                    .size(actionSize)
                    .graphicsLayer {
                        scaleX = if (input.triggerEmp) .92f else 1f
                        scaleY = if (input.triggerEmp) .92f else 1f
                    }
                    .background(Color(0xFF040A12), RoundedCornerShape(12.dp))
                    .border(2.dp, if (empBombs > 0) ElectricCyan else Color(0xFF132B45), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (empBombs > 0) Color(0xFF154068) else Color(0xFF0A1826), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onInput(input.copy(triggerEmp = true))
                                waitForUpOrCancellation()
                                onInput(input.copy(triggerEmp = false))
                            }
                        }
                        .testTag("arcade_orbital_emp"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("EMP", color = if (empBombs > 0) WarmIvory else Color(0xFF4A6B88), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }

            // FIRE Button
            Box(
                Modifier
                    .size(actionSize)
                    .graphicsLayer {
                        scaleX = if (input.fire) .92f else 1f
                        scaleY = if (input.fire) .92f else 1f
                    }
                    .background(Color(0xFF040A12), RoundedCornerShape(12.dp))
                    .border(2.dp, CobaltBlue, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (input.fire) CobaltBlue.copy(alpha = 0.8f) else Color(0xFF0077B6), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onInput(input.copy(fire = true))
                                waitForUpOrCancellation()
                                onInput(input.copy(fire = false))
                            }
                        }
                        .testTag("arcade_orbital_fire"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FIRE", color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black)
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
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = if (pressed) .92f else 1f
                scaleY = if (pressed) .92f else 1f
            }
            .background(Color(0xFF040A12), RoundedCornerShape(6.dp))
            .border(1.dp, if (pressed) CobaltBlue else Color(0xFF132B45), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (pressed) Color(0xFF154068) else Color(0xFF0B1D30), RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onHeld(true)
                        waitForUpOrCancellation()
                        onHeld(false)
                    }
                }
                .testTag("steer_$glyph"),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, color = WarmIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OrbitalServiceKey(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF071220), contentColor = Color(0xFF8BB5DA)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(30.dp)
            .border(1.dp, Color(0xFF152E4A), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun OrbitalOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(.85f)
            .background(Color(0xF2050D17), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF1E3E62), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// PATROL BRIEF //", color = Color(0xFF6B8CA8), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = CobaltBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("0${index + 1}", color = MysteryGold, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, lineHeight = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue, contentColor = Color(0xFF02040A)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(38.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}
