package com.example.starborn.feature.arcade.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.example.starborn.feature.arcade.games.deepmine.DeepMineAudioEvent
import com.example.starborn.feature.arcade.games.deepmine.DeepMineEngine
import com.example.starborn.feature.arcade.games.deepmine.DeepMineInput
import com.example.starborn.feature.arcade.games.deepmine.DeepMineSnapshot
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

private val Phosphor = Color(0xFFFFC857)
private val Cabinet = Color(0xFF171014)
private val Screen = Color(0xFF07110E)
private val Brass = Color(0xFFB88937)
private val WarmIvory = Color(0xFFFFE7B0)
private val InstrumentGlass = Color(0xFF090E0C)
private val FissureCyan = Color(0xFF7CF7D4)
private val FissureNeon = Color(0xFF00E5FF)

@Composable
fun DeepMineArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit
) {
    val engine = remember { DeepMineEngine() }
    val context = LocalContext.current
    val mineBackdrop = remember {
        context.assets.open("images/arcade/deep_mine/mine_shaft_v1.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }
    val probeSheet = remember {
        context.assets.open("images/arcade/deep_mine/mining_probe_sheet_v1.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(DeepMineInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.DEEP_MINE).highScore, submitted?.highScore ?: 0)

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
            submitted = arcadeService.submitDeepMineScore(snapshot.score)
            onPlayCue("sfx_arcade_game_over")
        }
    }

    LaunchedEffect(snapshot.audioEvent) {
        when (snapshot.audioEvent) {
            DeepMineAudioEvent.PICKUP -> onPlayCue("sfx_arcade_jump")
            DeepMineAudioEvent.THRUST -> onPlayCue("sfx_arcade_laser")
            DeepMineAudioEvent.CLEAN_LANDING -> onPlayCue("confirm")
            DeepMineAudioEvent.HARD_LANDING -> onPlayCue("action_inspect")
            DeepMineAudioEvent.STAGE_CLEAR -> onPlayCue("confirm")
            DeepMineAudioEvent.CRASH -> onPlayCue("sfx_arcade_game_over")
            DeepMineAudioEvent.NONE, DeepMineAudioEvent.DRILL, DeepMineAudioEvent.ALARM -> Unit
        }
    }

    LaunchedEffect(snapshot.fuel < 20f) {
        if (snapshot.fuel < 20f && !snapshot.gameOver) onPlayCue("error")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070709))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArcadeMarquee()
        Spacer(Modifier.height(5.dp))
        ScoreConsole(snapshot.score, highScore, snapshot.multiplier)
        Spacer(Modifier.height(4.dp))
        FlightInstruments(snapshot)
        Spacer(Modifier.height(5.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Cabinet, RoundedCornerShape(14.dp))
                .padding(9.dp),
            contentAlignment = Alignment.Center
        ) {
            DeepMineCanvas(
                snapshot = snapshot,
                input = input,
                mineBackdrop = mineBackdrop,
                probeSheet = probeSheet,
                reducedFlashes = reducedFlashes,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("deep_mine_viewport")
            )
            if (tutorial) {
                ArcadeOverlay(
                    title = "HOW TO DRILL",
                    lines = listOf(
                        "Use PORT and STBD thrusters to bank and steer.",
                        "Feather MAIN BOOST to control descent speed.",
                        "Land gently on Amber Veins or High-Yield Fissures.",
                        "Hold DRILL to extract ore and recharge fuel.",
                        "Snag drifting Coolant Pods for instant +22% fuel!"
                    ),
                    action = "START RUN",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                ArcadeOverlay("RUN PAUSED", listOf("Your probe is holding position in the shaft."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                ArcadeOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE ${snapshot.score}")
                        add("DEPTH REACHED: ${snapshot.depth}M")
                        if (tiers.isNotBlank()) add("NEW REWARD UNLOCKED: $tiers")
                        else add("Drop one more coin to beat high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = DeepMineInput()
                        onPlayCue("confirm")
                    }
                )
            }
        }
        StatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(5.dp))
        ArcadeControls(input, largeTouchTargets) { input = it }
        Spacer(Modifier.height(5.dp))
        ServiceKey { paused = true }
    }
}

@Composable
private fun ArcadeMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF24180D), RoundedCornerShape(9.dp))
            .border(2.dp, Color(0xFF725426), RoundedCornerShape(9.dp))
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DEEP MINE", color = Phosphor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
        Text("ASTEROID DRILL", color = WarmIvory, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text("HYPERION AMUSEMENTS // CABINET 01", color = Color(0xFFBCA878), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .8.sp)
    }
}

@Composable
private fun ScoreConsole(score: Int, highScore: Int, multiplier: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF181512), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF56472F), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(64.dp), Color(0xFF7CF7D4))
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = Phosphor) {
    Column(
        modifier
            .background(InstrumentGlass, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3B3428), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF92866B), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .8.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun FlightInstruments(snapshot: DeepMineSnapshot) {
    val dangerous = snapshot.velocityY > 22f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Instrument("FUEL", "${snapshot.fuel.toInt()}%", if (snapshot.fuel < 20f) Color(0xFFFF6B52) else Color(0xFF7CF7D4), Modifier.weight(1f))
        Instrument("DEPTH", "${snapshot.depth} M", Phosphor, Modifier.weight(1f))
        Instrument("TILT", "%+.0f°".format(snapshot.tiltAngle), if (kotlin.math.abs(snapshot.tiltAngle) > 8f) Color(0xFFFFC857) else WarmIvory, Modifier.weight(1f))
        Instrument("V/S", if (snapshot.landed) "LOCKED" else "%+d".format(snapshot.velocityY.toInt()), if (dangerous) Color(0xFFFF6B52) else WarmIvory, Modifier.weight(1f))
    }
}

@Composable
private fun Instrument(label: String, value: String, color: Color, modifier: Modifier) {
    Row(
        modifier
            .background(Color(0xFF101311), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF403A30), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF948A76), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF15130F), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF4B3C26), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(if (failure) Color(0xFFFF5D4A) else Phosphor, RoundedCornerShape(50))
        )
        Text(
            message,
            color = if (failure) Color(0xFFFF8A76) else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun DeepMineCanvas(
    snapshot: DeepMineSnapshot,
    input: DeepMineInput,
    mineBackdrop: ImageBitmap?,
    probeSheet: ImageBitmap?,
    reducedFlashes: Boolean,
    modifier: Modifier
) {
    Canvas(modifier.background(Screen, RoundedCornerShape(10.dp))) {
        val sx = size.width / DeepMineEngine.WORLD_WIDTH
        val sy = size.height / DeepMineEngine.WORLD_HEIGHT
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)

        // Backdrop
        drawRect(Color(0xFF10231C))
        mineBackdrop?.let { backdrop ->
            drawImage(
                image = backdrop,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(backdrop.width, backdrop.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                alpha = .72f,
                filterQuality = FilterQuality.Low
            )
            drawRect(Color(0xFF06130F).copy(alpha = .18f))
        }

        // Scanlines
        for (i in 0..18) {
            val y = i * 20f * sy
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .08f else .2f), Offset(0f, y), Offset(size.width, y), 1f)
        }

        // Falling Slag Hazard Warning & Slag Rock
        if (snapshot.slagWarning) {
            val warningX = snapshot.slagX * sx
            drawLine(Color(0xFFFF5D4A), Offset(warningX - 7f, 8f), Offset(warningX + 7f, 8f), 3f)
            drawLine(Color(0xFFFF5D4A), Offset(warningX, 5f), Offset(warningX, 18f), 2f)
        }
        if (snapshot.slagY > -15f) {
            val slag = p(snapshot.slagX, snapshot.slagY)
            drawCircle(Color(0xFF241812), radius = 7f * sx, center = slag)
            drawCircle(Color(0xFFFF7A32), radius = 2.2f * sx, center = Offset(slag.x - 1f, slag.y + 1f))
            drawLine(Color(0x99FF9B45), Offset(slag.x, slag.y - 8f), Offset(slag.x, slag.y - 18f), 2f)
        }

        // Ambient Dust Particles
        repeat(18) { index ->
            val phase = snapshot.elapsedSeconds * (7f + index % 4) + index * 19.7f
            val dustX = ((index * 47f + phase * 3f) % DeepMineEngine.WORLD_WIDTH) * sx
            val dustY = ((index * 73f + phase * 5f) % DeepMineEngine.WORLD_HEIGHT) * sy
            drawCircle(Color(0x55FFD58A), radius = 0.8f + (index % 3) * .35f, center = Offset(dustX, dustY))
        }

        // Floating Coolant Pod Pickup
        snapshot.pickup?.takeIf { !it.collected }?.let { pickup ->
            val pickupCenter = p(pickup.x, pickup.y)
            val ringRadius = (7.5f + sin(snapshot.elapsedSeconds * 6f) * 1.5f) * sx
            drawCircle(Color(0x4400E5FF), radius = ringRadius, center = pickupCenter)
            drawCircle(FissureNeon, radius = 4f * sx, center = pickupCenter)
            drawCircle(Color.White, radius = 1.8f * sx, center = pickupCenter)
            drawLine(Color(0x8800E5FF), Offset(pickupCenter.x, pickupCenter.y - 10f), Offset(pickupCenter.x, pickupCenter.y + 10f), 1.5f)
        }

        // Shaft Ground Base
        drawRect(Color(0xCC0B0C09), topLeft = p(0f, DeepMineEngine.GROUND_Y), size = Size(size.width, size.height - DeepMineEngine.GROUND_Y * sy))

        // Render All Landing Pads (Safe Vein + Optional Rich Fissure)
        snapshot.pads.forEach { pad ->
            val padLeft = p(pad.x, DeepMineEngine.GROUND_Y - 5f)
            val padSize = Size(pad.width * sx, 7f * sy)
            val isTarget = snapshot.activePad?.id == pad.id

            if (pad.isFissure) {
                // High-Yield Crystal Fissure
                val padBaseColor = if (isTarget) Color(0xFF003840) else Color(0xFF002228)
                val padGlowColor = if (isTarget) FissureNeon else FissureCyan
                drawRect(padBaseColor, topLeft = padLeft, size = padSize)
                drawRect(padGlowColor, topLeft = p(pad.x, DeepMineEngine.GROUND_Y - 4f), size = Size(pad.width * sx, 3f * sy))

                // Fissure Hologram Marker
                val beaconCenter = p(pad.x + pad.width / 2f, DeepMineEngine.GROUND_Y - 14f)
                drawCircle(Color(0x8800E5FF), radius = 2.5f * sx, center = beaconCenter)
                drawLine(Color(0x8800E5FF), Offset(beaconCenter.x, beaconCenter.y + 3f), Offset(beaconCenter.x, beaconCenter.y + 10f), 1f)

                // Ore Fill Level Bar
                val fillWidth = (pad.ore / pad.maxOre) * pad.width * sx
                drawRect(Color(0xAA7CF7D4), topLeft = p(pad.x, DeepMineEngine.GROUND_Y - 1f), size = Size(fillWidth, 2f * sy))
            } else {
                // Standard Ore Pad
                drawRect(Color(0xFF3A2B17), topLeft = p(pad.x - 2f, DeepMineEngine.GROUND_Y - 5f), size = Size((pad.width + 4f) * sx, 7f * sy))
                drawRect(Phosphor, topLeft = p(pad.x, DeepMineEngine.GROUND_Y - 4f), size = Size(pad.width * sx, 3f * sy))

                // Ore Fill Level Bar
                val fillWidth = (pad.ore / pad.maxOre) * pad.width * sx
                drawRect(Color(0xAAFFC857), topLeft = p(pad.x, DeepMineEngine.GROUND_Y - 1f), size = Size(fillWidth, 2f * sy))
            }
        }

        // Drilling Particle Sparks & Splinters
        if (snapshot.landed && input.drill && !snapshot.gameOver) {
            val isFissure = snapshot.activePad?.isFissure == true
            repeat(14) { index ->
                val pulse = (snapshot.elapsedSeconds * (32f + index * 1.5f) + index * 2.1f) % 15f
                val angleSign = if (index % 2 == 0) -1f else 1f
                val origin = p(snapshot.shipX, snapshot.shipY + 22f)
                val particleColor = if (isFissure) {
                    if (index % 2 == 0) FissureNeon else Color.White
                } else {
                    if (index % 3 == 0) Color(0xFF7CF7D4) else Color(0xFFFFA640)
                }
                drawCircle(
                    color = particleColor,
                    radius = (2.5f - pulse * .13f).coerceAtLeast(.6f),
                    center = Offset(origin.x + angleSign * pulse * sx * 1.2f, origin.y - (pulse * 0.4f).coerceAtLeast(0f) * sy)
                )
            }
        }

        // Draw Mining Probe with Rotational Hull Tilt and Exhaust Plumes
        val probePos = p(snapshot.shipX, snapshot.shipY)
        rotate(degrees = snapshot.tiltAngle, pivot = probePos) {
            // Main Thruster Plume Jet
            if (input.boost && !snapshot.gameOver && snapshot.fuel > 0f) {
                val flameLength = (16f + (snapshot.elapsedSeconds * 40f) % 6f) * sy
                val flamePath = Path().apply {
                    moveTo(probePos.x - 5f * sx, probePos.y + 12f * sy)
                    lineTo(probePos.x + 5f * sx, probePos.y + 12f * sy)
                    lineTo(probePos.x, probePos.y + 12f * sy + flameLength)
                    close()
                }
                drawPath(flamePath, Color(0xFFFF5D4A))
                val coreFlame = Path().apply {
                    moveTo(probePos.x - 2.5f * sx, probePos.y + 12f * sy)
                    lineTo(probePos.x + 2.5f * sx, probePos.y + 12f * sy)
                    lineTo(probePos.x, probePos.y + 12f * sy + flameLength * 0.6f)
                    close()
                }
                drawPath(coreFlame, Color(0xFFFFE7B0))
            }

            // Lateral Thruster Jets
            if (input.left && !snapshot.gameOver && snapshot.fuel > 0f) {
                drawLine(Color(0xFF7CF7D4), Offset(probePos.x + 8f * sx, probePos.y), Offset(probePos.x + 16f * sx, probePos.y), 2.5f)
            }
            if (input.right && !snapshot.gameOver && snapshot.fuel > 0f) {
                drawLine(Color(0xFF7CF7D4), Offset(probePos.x - 8f * sx, probePos.y), Offset(probePos.x - 16f * sx, probePos.y), 2.5f)
            }

            // Sprite Sheet or Vector Fallback
            val frame = when {
                snapshot.gameOver -> 7
                snapshot.landed && input.drill -> 6
                snapshot.landed -> 4
                input.boost -> 3
                input.left -> 1
                input.right -> 2
                else -> 0
            }
            probeSheet?.let { sheet ->
                val cellW = sheet.width / 4
                val cellH = sheet.height / 2
                val drawW = (58f * sx).toInt()
                val drawH = (72f * sy).toInt()
                drawImage(
                    image = sheet,
                    srcOffset = IntOffset((frame % 4) * cellW, (frame / 4) * cellH),
                    srcSize = IntSize(cellW, cellH),
                    dstOffset = IntOffset((snapshot.shipX * sx - drawW / 2).toInt(), (snapshot.shipY * sy - drawH / 2).toInt()),
                    dstSize = IntSize(drawW, drawH),
                    filterQuality = FilterQuality.Low
                )
            } ?: run {
                val shipPath = Path().apply {
                    moveTo(probePos.x, probePos.y - 10f * sy)
                    lineTo(probePos.x - 8f * sx, probePos.y + 8f * sy)
                    lineTo(probePos.x + 8f * sx, probePos.y + 8f * sy)
                    close()
                }
                drawPath(shipPath, Color(0xFFE8F5E9))
                drawPath(shipPath, Phosphor, style = Stroke(1.5f))
            }
        }
    }
}

@Composable
private fun ArcadeControls(
    input: DeepMineInput,
    large: Boolean,
    onInput: (DeepMineInput) -> Unit
) {
    val buttonSize = if (large) 70.dp else 58.dp
    val boostWidth = if (large) 84.dp else 70.dp

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1813), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF5D4A2C), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT DECK: Directional Thrusters for Left Thumb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("LATERAL THRUST", color = Color(0xFF948A76), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CabinetControl("◀", "PORT", "LEFT", buttonSize, Color(0xFF1F3D35), input.left) { onInput(input.copy(left = it)) }
                CabinetControl("▶", "STBD", "RIGHT", buttonSize, Color(0xFF1F3D35), input.right) { onInput(input.copy(right = it)) }
            }
        }

        // CENTER DIVIDER
        Box(
            Modifier
                .width(1.5.dp)
                .height(buttonSize + 10.dp)
                .background(Color(0xFF3D3222))
        )

        // RIGHT DECK: Boost + Drill for Right Thumb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("PRIMARY SYSTEMS", color = Color(0xFF948A76), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CabinetControl("▲", "MAIN", "BOOST", buttonSize, Color(0xFF8D352B), input.boost, customWidth = boostWidth) { onInput(input.copy(boost = it)) }
                CabinetControl("◆", "ORE", "DRILL", buttonSize, Color(0xFF8E6F18), input.drill) { onInput(input.copy(drill = it)) }
            }
        }
    }
}

@Composable
private fun CabinetControl(
    glyph: String,
    label: String,
    function: String,
    size: Dp,
    color: Color,
    pressed: Boolean,
    customWidth: Dp? = null,
    onHeld: (Boolean) -> Unit
) {
    val effectiveWidth = customWidth ?: size
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier
                .width(effectiveWidth)
                .height(size)
                .graphicsLayer {
                    scaleX = if (pressed) .92f else 1f
                    scaleY = if (pressed) .92f else 1f
                    alpha = if (pressed) .85f else 1f
                }
                .background(Color(0xFF0E0D0B), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF56462E), RoundedCornerShape(8.dp))
                .padding(3.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (pressed) color.copy(alpha = .7f) else color, RoundedCornerShape(6.dp))
                    .border(if (pressed) 1.dp else 2.dp, if (pressed) Color(0xFF5B4726) else Brass, RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onHeld(true)
                        waitForUpOrCancellation()
                        onHeld(false)
                    }
                }
                .testTag("arcade_${label.lowercase()}_${function.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, color = WarmIvory, fontSize = if (glyph == "◆") 20.sp else 23.sp, fontWeight = FontWeight.Black)
            }
        }
        Text(label, color = Color(0xFFB7AA8E), fontFamily = FontFamily.Monospace, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
        Text(function, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
    }
}

@Composable
private fun ServiceKey(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17151A), contentColor = Color(0xFFC9BFA8)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(32.dp)
            .border(1.dp, Color(0xFF4B4240), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun ArcadeOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(.84f)
            .background(Color(0xF20A0D0B), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF725426), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// OPERATOR BRIEF //", color = Color(0xFF8F8064), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = Phosphor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(7.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("0${index + 1}", color = Brass, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Color(0xFF171014)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(40.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

