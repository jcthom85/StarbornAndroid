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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.example.starborn.feature.arcade.games.harmonicpulse.*
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val LaneCyan = Color(0xFF00F0FF)
private val LaneGold = Color(0xFFFFD600)
private val LaneMagenta = Color(0xFFFF007F)
private val LaneEmerald = Color(0xFF00FF66)
private val SourceViolet = Color(0xFF9D4EDD)
private val SpaceObsidian = Color(0xFF07040D)
private val WarmIvory = Color(0xFFFFE7B0)

@Composable
fun HarmonicPulseArcadeScreen(
    arcadeService: ArcadeService,
    onBack: () -> Unit,
    largeTouchTargets: Boolean,
    reducedFlashes: Boolean,
    onPlayCue: (String) -> Unit
) {
    val engine = remember { HarmonicPulseEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var input by remember { mutableStateOf(HarmonicPulseInput()) }
    var paused by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf<ArcadeRunSubmission?>(null) }
    var tutorial by remember { mutableStateOf(true) }
    val highScore = maxOf(arcadeService.progress(ArcadeIds.HARMONIC_PULSE).highScore, submitted?.highScore ?: 0)

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
            submitted = arcadeService.submitHarmonicPulseScore(snapshot.score)
            onPlayCue("sfx_arcade_game_over")
        }
    }

    LaunchedEffect(snapshot.audioEvent) {
        when (snapshot.audioEvent) {
            PulseAudioEvent.HIT_NOTE -> onPlayCue("sfx_arcade_laser")
            PulseAudioEvent.HIT_PERFECT -> onPlayCue("sfx_arcade_jump")
            PulseAudioEvent.OVERDRIVE_TRIGGER -> onPlayCue("confirm")
            PulseAudioEvent.MISS_NOTE -> onPlayCue("action_inspect")
            PulseAudioEvent.TRACK_CLEAR -> onPlayCue("confirm")
            PulseAudioEvent.GAME_OVER -> onPlayCue("sfx_arcade_game_over")
            PulseAudioEvent.NONE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040208))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HarmonicMarquee()
        Spacer(Modifier.height(4.dp))
        HarmonicScoreConsole(snapshot.score, highScore, snapshot.multiplier, snapshot.combo, snapshot.round)
        Spacer(Modifier.height(4.dp))
        HarmonyGauge(snapshot.harmony, snapshot.overdriveSeconds)
        Spacer(Modifier.height(4.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SpaceObsidian, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF3C096C), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            HarmonicCanvas(
                snapshot = snapshot,
                reducedFlashes = reducedFlashes,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("harmonic_pulse_viewport")
            )

            if (tutorial) {
                HarmonicOverlay(
                    title = "HARMONIC RESONANCE BRIEF",
                    lines = listOf(
                        "Tap the 4 frequency pads as nodes reach the strike line.",
                        "Time your taps with precision for PERFECT (+300) hits.",
                        "Maintain combo streaks to raise the score multiplier.",
                        "Reach 100% Harmony to ignite HARMONIC OVERDRIVE (2× multiplier)!",
                        "Missing nodes or tapping empty lanes destabilizes the core.",
                        "Clear all 3 cadence tracks to achieve resonance mastery."
                    ),
                    action = "START RESONANCE",
                    onAction = { tutorial = false; onPlayCue("confirm") }
                )
            } else if (paused) {
                HarmonicOverlay("CADENCE PAUSED", listOf("Prismatic tuning fork held in stasis."), "RESUME") { paused = false }
            } else if (snapshot.gameOver) {
                val tiers = submitted?.newlyClaimed.orEmpty().joinToString()
                HarmonicOverlay(
                    title = snapshot.message,
                    lines = buildList {
                        add("FINAL SCORE: ${snapshot.score}")
                        add("MAX COMBO: ${snapshot.maxCombo}")
                        add("LAYER REACHED: CADENCE ${snapshot.round}")
                        if (tiers.isNotBlank()) add("REWARD UNLOCKED: $tiers")
                        else add("Drop one more coin to beat the high score.")
                    },
                    action = "TRY AGAIN",
                    onAction = {
                        engine.reset()
                        snapshot = engine.snapshot()
                        submitted = null
                        input = HarmonicPulseInput()
                        onPlayCue("confirm")
                    }
                )
            }
        }

        HarmonicStatusLamp(snapshot.message, snapshot.gameOver)
        Spacer(Modifier.height(4.dp))
        HarmonicStrikeDeck(input, largeTouchTargets) { input = it }
        Spacer(Modifier.height(4.dp))
        HarmonicServiceKey { paused = true }
    }
}

@Composable
private fun HarmonicMarquee() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E0836), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF5A189A), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HARMONIC PULSE", color = SourceViolet, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
        Text("THE SOURCE PRIMORDIAL CORE // CABINET 06", color = LaneCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun HarmonicScoreConsole(score: Int, highScore: Int, multiplier: Int, combo: Int, round: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF140526), RoundedCornerShape(7.dp))
            .border(1.dp, Color(0xFF38105C), RoundedCornerShape(7.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScoreCell("SCORE", score.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("HIGH SCORE", highScore.toString().padStart(6, '0'), Modifier.weight(1f))
        ScoreCell("COMBO", "$combo", Modifier.width(52.dp), LaneMagenta)
        ScoreCell("MULT", "×$multiplier", Modifier.width(48.dp), LaneGold)
        ScoreCell("LAYER", "$round", Modifier.width(44.dp), LaneEmerald)
    }
}

@Composable
private fun ScoreCell(label: String, value: String, modifier: Modifier, valueColor: Color = SourceViolet) {
    Column(
        modifier
            .background(Color(0xFF090214), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF220A38), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF9F86C0), fontFamily = FontFamily.Monospace, fontSize = 7.sp, letterSpacing = .6.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun HarmonyGauge(harmony: Float, overdriveSeconds: Float) {
    val inOverdrive = overdriveSeconds > 0f
    val danger = harmony < 25f
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF110420), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3C096C), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (inOverdrive) "OVERDRIVE ⚡" else "HARMONY",
            color = if (inOverdrive) LaneGold else if (danger) Color(0xFFFF3366) else LaneCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF070110), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((harmony / 100f).coerceIn(0f, 1f))
                    .background(
                        if (inOverdrive) Brush.horizontalGradient(listOf(LaneCyan, LaneMagenta, LaneGold, LaneEmerald))
                        else if (danger) Brush.horizontalGradient(listOf(Color(0xFFFF3366), Color(0xFFFF007F)))
                        else Brush.horizontalGradient(listOf(LaneCyan, SourceViolet)),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
        Text(
            if (inOverdrive) "%.1fS".format(overdriveSeconds) else "${harmony.toInt()}%",
            color = if (inOverdrive) LaneGold else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HarmonicStatusLamp(message: String, failure: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF100320), RoundedCornerShape(5.dp))
            .border(1.dp, Color(0xFF330959), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(if (failure) Color(0xFFFF3366) else SourceViolet, RoundedCornerShape(50))
        )
        Text(
            message,
            color = if (failure) Color(0xFFFF9EBE) else WarmIvory,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun HarmonicCanvas(
    snapshot: HarmonicPulseSnapshot,
    reducedFlashes: Boolean,
    modifier: Modifier
) {
    val laneColors = listOf(LaneCyan, LaneGold, LaneMagenta, LaneEmerald)

    Canvas(modifier.background(Color(0xFF07020E), RoundedCornerShape(8.dp))) {
        val w = size.width
        val h = size.height
        val laneWidth = w / 4f
        val strikeY = HarmonicPulseEngine.STRIKE_LINE_Y * h

        // 1. Overdrive Rainbow Ambient Aura
        if (snapshot.overdriveSeconds > 0f) {
            val alpha = (snapshot.overdriveSeconds / 6f * 0.18f)
            drawRect(SourceViolet.copy(alpha = alpha), topLeft = Offset(0f, 0f), size = Size(w, h))
        }

        // 2. Draw 4 Frequency Highway Lanes
        repeat(4) { lane ->
            val lx = lane * laneWidth
            val color = laneColors[lane]

            // Lane Divider
            if (lane > 0) {
                drawLine(Color(0xFF220A38), Offset(lx, 0f), Offset(lx, h), 1.5f)
            }

            // Lane subtle stream glow
            drawRect(color.copy(alpha = 0.03f), topLeft = Offset(lx, 0f), size = Size(laneWidth, h))
        }

        // 3. Receptor Strike Line
        drawLine(Color(0xFF5A189A), Offset(0f, strikeY), Offset(w, strikeY), 2.5f)
        repeat(4) { lane ->
            val lx = lane * laneWidth
            val color = laneColors[lane]
            drawCircle(color.copy(alpha = 0.4f), radius = 16.dp.toPx(), center = Offset(lx + laneWidth / 2f, strikeY), style = Stroke(2f))
            drawCircle(color.copy(alpha = 0.8f), radius = 4.dp.toPx(), center = Offset(lx + laneWidth / 2f, strikeY))
        }

        // 4. Falling Rhythm Nodes
        snapshot.notes.forEach { note ->
            val color = laneColors[note.lane]
            val nx = note.lane * laneWidth + laneWidth / 2f
            val ny = note.y * strikeY
            val nr = 14.dp.toPx()

            // Outer pulse halo
            val pulse = (sin(snapshot.elapsedSeconds * 16f + note.lane) * 2f).coerceAtLeast(0f)
            drawCircle(color.copy(alpha = 0.35f), radius = nr + pulse, center = Offset(nx, ny))
            drawCircle(color, radius = nr * 0.7f, center = Offset(nx, ny))
            drawCircle(Color.White, radius = nr * 0.3f, center = Offset(nx, ny))
        }

        // 5. Active Hit Rating Indicator
        if (snapshot.ratingTimer > 0f && snapshot.lastRating != null) {
            val ratingColor = when (snapshot.lastRating) {
                HitRating.PERFECT -> LaneGold
                HitRating.GREAT -> LaneCyan
                HitRating.OK -> LaneEmerald
                HitRating.MISS -> Color(0xFFFF3366)
            }
            val alpha = (snapshot.ratingTimer / 0.5f).coerceIn(0f, 1f)
            drawCircle(ratingColor.copy(alpha = alpha * 0.3f), radius = 32.dp.toPx(), center = Offset(w / 2f, strikeY - 40.dp.toPx()))
        }

        // 6. Scanlines
        for (i in 0..18) {
            val y = i * 20.dp.toPx()
            drawLine(Color.Black.copy(alpha = if (reducedFlashes) .06f else .16f), Offset(0f, y), Offset(w, y), 1f)
        }
    }
}

@Composable
private fun HarmonicStrikeDeck(
    input: HarmonicPulseInput,
    large: Boolean,
    onInput: (HarmonicPulseInput) -> Unit
) {
    val deckHeight = if (large) 72.dp else 60.dp

    Row(
        Modifier
            .fillMaxWidth()
            .height(deckHeight)
            .background(Color(0xFF0F031E), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF38105C), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StrikePad("ALPHA", LaneCyan, input.tapLane0, Modifier.weight(1f)) { onInput(input.copy(tapLane0 = it)) }
        StrikePad("THETA", LaneGold, input.tapLane1, Modifier.weight(1f)) { onInput(input.copy(tapLane1 = it)) }
        StrikePad("GAMMA", LaneMagenta, input.tapLane2, Modifier.weight(1f)) { onInput(input.copy(tapLane2 = it)) }
        StrikePad("DELTA", LaneEmerald, input.tapLane3, Modifier.weight(1f)) { onInput(input.copy(tapLane3 = it)) }
    }
}

@Composable
private fun StrikePad(
    label: String,
    accent: Color,
    pressed: Boolean,
    modifier: Modifier,
    onHeld: (Boolean) -> Unit
) {
    Box(
        modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = if (pressed) .92f else 1f
                scaleY = if (pressed) .92f else 1f
            }
            .background(Color(0xFF06010D), RoundedCornerShape(8.dp))
            .border(2.dp, if (pressed) accent else accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (pressed) accent.copy(alpha = 0.4f) else Color(0xFF140526), RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onHeld(true)
                        waitForUpOrCancellation()
                        onHeld(false)
                    }
                }
                .testTag("strike_pad_$label"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▼", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(label, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HarmonicServiceKey(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F031E), contentColor = Color(0xFFBE9FE1)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(30.dp)
            .border(1.dp, Color(0xFF38105C), RoundedCornerShape(4.dp))
    ) {
        Text("SERVICE  •  PAUSE / EXIT", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun HarmonicOverlay(title: String, lines: List<String>, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(.85f)
            .background(Color(0xF20F031E), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF5A189A), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("// CADENCE BRIEF //", color = Color(0xFF9F86C0), fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, letterSpacing = 1.sp)
        Text(title, color = SourceViolet, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("0${index + 1}", color = LaneGold, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(line, color = WarmIvory, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, lineHeight = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = SourceViolet, contentColor = Color(0xFF07020E)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .height(38.dp)
        ) {
            Text(action, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}
