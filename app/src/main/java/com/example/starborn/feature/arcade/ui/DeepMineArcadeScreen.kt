package com.example.starborn.feature.arcade.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.starborn.feature.arcade.domain.ArcadeRunSubmission
import com.example.starborn.feature.arcade.domain.ArcadeService
import com.example.starborn.feature.arcade.domain.ArcadeIds
import com.example.starborn.feature.arcade.games.deepmine.DeepMineEngine
import com.example.starborn.feature.arcade.games.deepmine.DeepMineInput
import com.example.starborn.feature.arcade.games.deepmine.DeepMineSnapshot
import kotlinx.coroutines.isActive

private val Phosphor = Color(0xFFFFC857)
private val Cabinet = Color(0xFF171014)
private val Screen = Color(0xFF07110E)
private val Brass = Color(0xFFB88937)
private val WarmIvory = Color(0xFFFFE7B0)
private val InstrumentGlass = Color(0xFF090E0C)

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
            onPlayCue("error")
        }
    }

    LaunchedEffect(snapshot.message) {
        when {
            snapshot.gameOver -> Unit
            snapshot.message.startsWith("CLEAN LANDING") -> onPlayCue("confirm")
            snapshot.message.startsWith("LANDED") -> onPlayCue("action_inspect")
            snapshot.message.startsWith("DRILLING") -> onPlayCue("action_inspect")
            snapshot.message.startsWith("ORE SECURED") -> onPlayCue("confirm")
        }
    }

    LaunchedEffect(snapshot.fuel < 20f) {
        if (snapshot.fuel < 20f && !snapshot.gameOver) onPlayCue("error")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF070709)).statusBarsPadding().navigationBarsPadding()
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
            Modifier.fillMaxWidth().weight(1f).background(Cabinet, RoundedCornerShape(14.dp)).padding(9.dp),
            contentAlignment = Alignment.Center
        ) {
            DeepMineCanvas(snapshot, input, mineBackdrop, probeSheet, reducedFlashes, Modifier.fillMaxSize().testTag("deep_mine_viewport"))
            if (tutorial) {
                ArcadeOverlay(
                    title = "HOW TO DRILL",
                    lines = listOf(
                        "Thrusters change momentum.",
                        "Land slowly inside the amber pad.",
                        "Hold DRILL while settled.",
                        "Clean landings build your multiplier."
                    ),
                    action = "START RUN",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                ArcadeOverlay("RUN PAUSED", listOf("Your probe is holding position."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString { it.name }
                ArcadeOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE ${snapshot.score}")
                        if (tiers.isNotBlank()) add("NEW REWARD: $tiers")
                        else add("Best runs begin with one more coin.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset(); snapshot = engine.snapshot(); submitted = null
                        input = DeepMineInput(); onPlayCue("confirm")
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
        Modifier.fillMaxWidth().background(Color(0xFF24180D), RoundedCornerShape(9.dp))
            .border(2.dp, Color(0xFF725426), RoundedCornerShape(9.dp)).padding(vertical = 5.dp),
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
        Modifier.fillMaxWidth().background(Color(0xFF181512), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF56472F), RoundedCornerShape(7.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("CHAIN", "×$multiplier", Modifier.width(62.dp), Color(0xFF7CF7D4))
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = Phosphor) {
    Column(
        modifier.background(InstrumentGlass, RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF3B3428), RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF92866B), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .8.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun FlightInstruments(snapshot: DeepMineSnapshot) {
    val dangerous = snapshot.velocityY > 28f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Instrument("FUEL", "${snapshot.fuel.toInt()}%", if (snapshot.fuel < 20f) Color(0xFFFF6B52) else Color(0xFF7CF7D4), Modifier.weight(1f))
        Instrument("DEPTH", "${snapshot.depth} M", Phosphor, Modifier.weight(1f))
        Instrument("V/S", if (snapshot.landed) "LOCK" else "%+d".format(snapshot.velocityY.toInt()), if (dangerous) Color(0xFFFF6B52) else WarmIvory, Modifier.weight(1f))
    }
}

@Composable
private fun Instrument(label: String, value: String, color: Color, modifier: Modifier) {
    Row(
        modifier.background(Color(0xFF101311), RoundedCornerShape(5.dp)).border(1.dp, Color(0xFF403A30), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF948A76), fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF15130F), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF4B3C26), RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(7.dp).background(if (failure) Color(0xFFFF5D4A) else Phosphor, RoundedCornerShape(50)))
        Text(message, color = if (failure) Color(0xFFFF8A76) else WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
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
        for (i in 0..18) {
            val y = i * 20f * sy
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .08f else .2f), Offset(0f, y), Offset(size.width, y), 1f)
        }
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
        repeat(18) { index ->
            val phase = snapshot.elapsedSeconds * (7f + index % 4) + index * 19.7f
            val dustX = ((index * 47f + phase * 3f) % DeepMineEngine.WORLD_WIDTH) * sx
            val dustY = ((index * 73f + phase * 5f) % DeepMineEngine.WORLD_HEIGHT) * sy
            drawCircle(Color(0x55FFD58A), radius = 0.8f + (index % 3) * .35f, center = Offset(dustX, dustY))
        }
        if (snapshot.landed && input.drill && !snapshot.gameOver) {
            repeat(9) { index ->
                val pulse = (snapshot.elapsedSeconds * (28f + index) + index * 2.3f) % 12f
                val angleSign = if (index % 2 == 0) -1f else 1f
                val origin = p(snapshot.shipX, snapshot.shipY + 26f)
                drawCircle(
                    color = if (index % 3 == 0) Color(0xFF7CF7D4) else Color(0xFFFFA640),
                    radius = (2.4f - pulse * .12f).coerceAtLeast(.7f),
                    center = Offset(origin.x + angleSign * pulse * sx, origin.y + pulse * .55f * sy)
                )
            }
        }
        val pad = snapshot.pad
        drawRect(Color(0xCC0B0C09), topLeft = p(0f, DeepMineEngine.GROUND_Y), size = Size(size.width, size.height - DeepMineEngine.GROUND_Y * sy))
        drawRect(Color(0xFF3A2B17), topLeft = p(pad.x - 3f, DeepMineEngine.GROUND_Y - 5f), size = Size((pad.width + 6f) * sx, 7f * sy))
        drawRect(Phosphor, topLeft = p(pad.x, DeepMineEngine.GROUND_Y - 4f), size = Size(pad.width * sx, 3f * sy))
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
            val ship = p(snapshot.shipX, snapshot.shipY)
            val shipPath = Path().apply {
                moveTo(ship.x, ship.y - 10f * sy); lineTo(ship.x - 8f * sx, ship.y + 8f * sy)
                lineTo(ship.x + 8f * sx, ship.y + 8f * sy); close()
            }
            drawPath(shipPath, Color(0xFFE8F5E9)); drawPath(shipPath, Phosphor, style = Stroke(1.5f))
        }
    }
}

@Composable
private fun ArcadeControls(input: DeepMineInput, large: Boolean, onInput: (DeepMineInput) -> Unit) {
    val buttonSize = if (large) 70.dp else 58.dp
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF201A14), RoundedCornerShape(9.dp))
            .border(2.dp, Color(0xFF5D4A2C), RoundedCornerShape(9.dp)).padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CabinetControl("◀", "PORT", "THRUST", buttonSize, Color(0xFF254039), input.left) { onInput(input.copy(left = it)) }
        CabinetControl("▲", "MAIN", "BOOST", buttonSize, Color(0xFF8D352B), input.boost) { onInput(input.copy(boost = it)) }
        CabinetControl("◆", "ORE", "DRILL", buttonSize, Color(0xFF806116), input.drill) { onInput(input.copy(drill = it)) }
        CabinetControl("▶", "STBD", "THRUST", buttonSize, Color(0xFF254039), input.right) { onInput(input.copy(right = it)) }
    }
}

@Composable
private fun CabinetControl(
    glyph: String,
    label: String,
    function: String,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    pressed: Boolean,
    onHeld: (Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            Modifier.size(size).graphicsLayer {
                scaleX = if (pressed) .93f else 1f
                scaleY = if (pressed) .93f else 1f
                alpha = if (pressed) .88f else 1f
            }.background(Color(0xFF0E0D0B), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF56462E), RoundedCornerShape(8.dp)).padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize().background(if (pressed) color.copy(alpha = .7f) else color, RoundedCornerShape(6.dp))
                    .border(if (pressed) 1.dp else 2.dp, if (pressed) Color(0xFF5B4726) else Brass, RoundedCornerShape(6.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false); onHeld(true)
                            waitForUpOrCancellation(); onHeld(false)
                        }
                    }.testTag("arcade_${label.lowercase()}_${function.lowercase()}"),
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
        modifier = Modifier.height(32.dp).border(1.dp, Color(0xFF4B4240), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun ArcadeOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(.82f).background(Color(0xF20A0D0B), RoundedCornerShape(7.dp))
            .border(2.dp, Color(0xFF725426), RoundedCornerShape(7.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// OPERATOR BRIEF //", color = Color(0xFF8F8064), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = 1.sp)
        Text(title, color = Phosphor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(7.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("0${index + 1}", color = Brass, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = Phosphor, contentColor = Color(0xFF171014)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth(.72f).height(40.dp)
        ) { Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp) }
    }
}
