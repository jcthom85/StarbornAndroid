package com.example.starborn.shared.puzzle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class ChoicePuzzlePanel(val id: String, val submitLabel: String) {
    HAZARD_CYCLE("hazard_cycle", "Run test"),
    CONVEYOR_OVERRIDE("conveyor_override", "Set override"),
    CHOIR_LOCK("choir_lock", "Join the Chorus"),
    GRAVITON_WELL("graviton_well", "Release test mass");

    companion object {
        fun forPresentation(presentation: String?): ChoicePuzzlePanel? =
            entries.firstOrNull { it.id == presentation }
    }
}

@Composable
fun PuzzleChoices(
    panel: ChoicePuzzlePanel,
    prompt: String,
    dials: List<SignalDial>,
    feedback: String?,
    onChange: (String, Float) -> Unit
) {
    when (panel) {
        ChoicePuzzlePanel.HAZARD_CYCLE -> HazardCyclePanel(prompt, dials, feedback, onChange)
        ChoicePuzzlePanel.CONVEYOR_OVERRIDE -> {
            val beat = dials.first { it.id == "beat" }
            val stop = dials.first { it.id == "stop" }
            ConveyorOverridePanel(prompt, beat, stop, feedback, onChange)
        }
        ChoicePuzzlePanel.CHOIR_LOCK -> ChoirLockPanel(prompt, dials.single(), feedback, onChange)
        ChoicePuzzlePanel.GRAVITON_WELL -> GravitonWellPanel(prompt,
            dials.first { it.id == "deflection" }, dials.first { it.id == "brake" }, feedback, onChange)
    }
}

@Composable
private fun GravitonWellPanel(
    prompt: String,
    deflection: SignalDial,
    brake: SignalDial,
    feedback: String?,
    onChange: (String, Float) -> Unit
) {
    val cyan = Color(0xFFA0E7F4)
    val amber = Color(0xFFFFC78B)
    val panel = Color(0xFF142630)
    val caught = deflection.matched && brake.matched
    val crossAxis by animateFloatAsState(
        when (deflection.value.toInt()) {
            0 -> .50f
            2 -> .85f
            else -> .70f
        }, label = "gravity deflection"
    )
    val depth by animateFloatAsState(if (brake.matched) .71f else .92f, label = "mass stopping distance")
    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium)
        Surface(color = panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, cyan.copy(alpha = .5f))) {
            Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                val start = Offset(size.width * .5f, size.height * .14f)
                val receiver = Offset(size.width * .5f, size.height * .71f)
                val end = Offset(size.width * crossAxis, size.height * depth)
                val field = Offset(size.width * .82f, size.height * .33f)
                drawCircle(cyan.copy(alpha = .3f), 13.dp.toPx(), field, style = Stroke(width = 1.dp.toPx()))
                drawCircle(cyan.copy(alpha = .6f), 6.dp.toPx(), field)
                drawCircle(cyan, 11.dp.toPx(), receiver, style = Stroke(width = 2.dp.toPx()))
                drawPath(Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(start.x, size.height * .53f, end.x, end.y)
                }, if (caught) cyan else amber, style = Stroke(width = 2.dp.toPx()))
                drawCircle(if (caught) cyan else amber, 4.dp.toPx(), end)
            }
        }
        Text("Counter-field", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Left", "Neutral", "Right").forEachIndexed { i, label ->
                PuzzleTile(label, deflection.value.toInt() == i, cyan, panel, Modifier.weight(1f).testTag("mass-field-$i")) {
                    onChange(deflection.id, i.toFloat())
                }
            }
        }
        Text("Magnetic brake", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Off", "On").forEachIndexed { i, label ->
                PuzzleTile(label, brake.value.toInt() == i, cyan, panel, Modifier.weight(1f).testTag("mass-brake-$i")) {
                    onChange(brake.id, i.toFloat())
                }
            }
        }
        val forecast = when {
            !deflection.matched -> "The mass drifts right of the receiver."
            !brake.matched -> "The mass lines up but overshoots the receiver."
            else -> "The mass stops in the receiver."
        }
        Text(if (feedback == null) forecast else "Test missed. $forecast",
            color = if (caught) cyan else amber, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ConveyorOverridePanel(
    prompt: String,
    beat: SignalDial,
    stop: SignalDial,
    feedback: String?,
    onChange: (String, Float) -> Unit
) {
    val amber = Color(0xFFFFBE78)
    val green = Color(0xFF91E5C1)
    val rust = Color(0xFFF1987C)
    val panel = Color(0xFF261C17)
    val beatIndex = beat.value.toInt()
    val stopIndex = stop.value.toInt()
    val lift by animateFloatAsState(
        when {
            beat.matched -> .16f
            beatIndex == 0 -> .71f
            else -> .57f
        }, label = "crusher lift"
    )
    val carriage by animateFloatAsState(
        when (stopIndex) {
            0 -> .61f
            1 -> .90f
            else -> .09f
        }, label = "belt route"
    )

    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).testTag("conveyor-panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium)
        Surface(color = panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, amber.copy(alpha = .5f))) {
            Canvas(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 10.dp)) {
                val belt = size.height * .78f
                val jaw = size.height * lift
                val leftJaw = size.width * .31f
                val rightJaw = size.width * .47f
                drawLine(amber.copy(alpha = .5f), Offset(0f, belt), Offset(size.width, belt), 2.dp.toPx())
                drawLine(rust, Offset(leftJaw, jaw), Offset(rightJaw, jaw), 5.dp.toPx(), StrokeCap.Round)
                drawLine(rust.copy(alpha = .5f), Offset(leftJaw, 0f), Offset(leftJaw, jaw), 2.dp.toPx())
                drawLine(rust.copy(alpha = .5f), Offset(rightJaw, 0f), Offset(rightJaw, jaw), 2.dp.toPx())
                drawLine(green, Offset(size.width * .55f, belt), Offset(size.width * .67f, belt), 3.dp.toPx())
                drawLine(rust, Offset(size.width * .9f, belt - 9.dp.toPx()),
                    Offset(size.width * .9f, belt + 6.dp.toPx()), 3.dp.toPx())
                val canMove = beat.matched || carriage < .30f
                val carriageColor = when {
                    !canMove || stopIndex == 1 -> rust
                    stop.matched && beat.matched -> green
                    else -> amber
                }
                drawCircle(carriageColor, 5.dp.toPx(),
                    Offset(size.width * carriage.coerceAtMost(if (canMove) 1f else .26f), belt - 7.dp.toPx()))
            }
        }
        Text("Pause the belt on", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1 · Strike", "2 · Drag", "3 · Lift").forEachIndexed { i, label ->
                PuzzleTile(label, beatIndex == i, amber, panel, Modifier.weight(1f).testTag("belt-beat-$i")) {
                    onChange(beat.id, i.toFloat())
                }
            }
        }
        Text("Stop at", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Service notch", "Red scrape").forEachIndexed { i, label ->
                PuzzleTile(label, stopIndex == i, if (i == 0) green else rust, panel,
                    Modifier.weight(1f).testTag("belt-stop-$i")) {
                    onChange(stop.id, i.toFloat())
                }
            }
        }
        val routeStatus = when {
            beatIndex < 0 -> "Select a beat to see the crusher clearance."
            !beat.matched -> "Crusher teeth still block the lane."
            stopIndex < 0 -> "Crusher raised. Choose where to stop."
            !stop.matched -> "The rollers pinch shut at the red scrape."
            else -> "The belt can reach shelter in the service notch."
        }
        Text(if (feedback == null) routeStatus else "Override rejected. $routeStatus",
            color = if (beat.matched && stop.matched) green else amber,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChoirLockPanel(
    prompt: String,
    entry: SignalDial,
    feedback: String?,
    onChange: (String, Float) -> Unit
) {
    val voices = listOf("Orion", "Gh0st", "Open", "Zeke")
    val inks = listOf(Color(0xFF96C6FF), Color(0xFFE2B6FF), Color(0xFFACEACD), Color(0xFFFFCB91))
    val violet = Color(0xFFD9C0FF)
    val chosenBeat = entry.value.toInt()
    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).testTag("choir-panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium)
        Canvas(Modifier.fillMaxWidth().height(76.dp).testTag("choir-score")) {
            val beatWidth = size.width / voices.size
            val voiceLine = size.height * .31f
            val novaLine = size.height * .72f
            drawLine(violet.copy(alpha = .3f), Offset(0f, voiceLine), Offset(size.width, voiceLine), 1.dp.toPx())
            drawLine(violet.copy(alpha = .3f), Offset(0f, novaLine), Offset(size.width, novaLine), 1.dp.toPx())
            voices.indices.forEach { i ->
                val x = beatWidth * i
                if (i > 0) drawLine(violet.copy(alpha = .24f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                if (i.toFloat() != entry.target) {
                    drawPath(voicePhrase(x, beatWidth, voiceLine, size.height * .17f, i + 1), inks[i],
                        style = Stroke(width = 2.dp.toPx()))
                }
                if (chosenBeat == i) {
                    drawPath(voicePhrase(x, beatWidth, novaLine, size.height * .17f, 2),
                        if (entry.matched) inks[i] else Color(0xFFFF9A99), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            voices.forEachIndexed { i, voice ->
                PuzzleTile(voice, chosenBeat == i, inks[i], Color(0xFF241B36),
                    Modifier.weight(1f).testTag("choir-beat-$i")) {
                    onChange(entry.id, i.toFloat())
                }
            }
        }
        Text(when {
            chosenBeat < 0 -> "Choose where Nova's note enters."
            entry.matched -> "Nova has room. Every voice is still distinct."
            feedback != null -> "Nova's note covers ${voices[chosenBeat]}. Leave that voice its own beat."
            else -> "Nova overlaps ${voices[chosenBeat]}."
        }, style = MaterialTheme.typography.bodySmall,
            color = if (entry.matched) inks[entry.target.toInt()] else violet)
    }
}

private fun voicePhrase(x: Float, width: Float, baseline: Float, height: Float, cycles: Int): Path = Path().apply {
    moveTo(x, baseline)
    for (point in 1..24) {
        val t = point / 24f
        val envelope = sin(PI * t)
        val wave = sin(2 * PI * cycles * t) * envelope
        lineTo(x + width * t, baseline + (height * wave).toFloat())
    }
}

@Composable
private fun PuzzleTile(
    label: String,
    selected: Boolean,
    ink: Color,
    surface: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.RadioButton
            this.selected = selected
        },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) ink.copy(alpha = .21f) else surface,
        contentColor = ink,
        border = BorderStroke(1.dp, ink.copy(alpha = if (selected) 1f else .42f))
    ) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 3.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
