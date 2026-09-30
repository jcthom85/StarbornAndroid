package com.example.starborn.shared.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ForkCyan = Color(0xFF7FE6FF)
private val CutterGold = Color(0xFFFFC857)
private val QuietGreen = Color(0xFF8BE6AC)

@Composable
fun CounterTunePanel(
    dials: List<SignalDial>,
    onChange: (String, Float) -> Unit,
    onHandshake: () -> Unit,
    modifier: Modifier = Modifier,
    feedback: String? = null
) {
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val step = CounterTuneStep.entries[stepIndex]
    val signal = CounterTuneSignal(dials)
    val active = when (step) {
        CounterTuneStep.PULSE -> signal.sweep
        CounterTuneStep.COOLING -> signal.cooling
        CounterTuneStep.GROUND -> signal.ground
    }
    val ready = signal.ready(step)
    val title = when (step) {
        CounterTuneStep.PULSE -> "Find the pulse"
        CounterTuneStep.COOLING -> "Balance the cold loop"
        CounterTuneStep.GROUND -> "Quiet the feedback"
    }
    val instruction = when (step) {
        CounterTuneStep.PULSE -> "Cyan is the Fork. Gold is your cutter. Adjust the sweep until their peaks line up."
        CounterTuneStep.COOLING -> "Hold the sweep. Use cooling to bring the gold wave to the same height as cyan."
        CounterTuneStep.GROUND -> "Hold sweep and cooling. Oppose the Fork's wave with the suit ground; watch the combined signal below."
    }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("${stepIndex + 1} / 3 · $title", style = MaterialTheme.typography.titleMedium)
        Text(instruction, style = MaterialTheme.typography.bodyMedium)
        Surface(color = Color(0xFF061018), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fork — cyan     Cutter — gold", color = ForkCyan, style = MaterialTheme.typography.labelMedium)
                SignalTrace(signal, step, combined = false, modifier = Modifier.fillMaxWidth().height(112.dp))
                if (step == CounterTuneStep.GROUND) {
                    Text("Combined signal · ${(signal.residual * 100).roundToInt()} noise", color = QuietGreen,
                        style = MaterialTheme.typography.labelMedium)
                    SignalTrace(signal, step, combined = true, modifier = Modifier.fillMaxWidth().height(64.dp))
                }
            }
        }
        feedback?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Text(signal.diagnostic(step), style = MaterialTheme.typography.bodyMedium,
            color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        val controlLabel = when (step) {
            CounterTuneStep.PULSE -> "Phase sweep"
            CounterTuneStep.COOLING -> "Cold loop"
            CounterTuneStep.GROUND -> "Ground phase"
        }
        Text(controlLabel, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = active.value,
            onValueChange = { onChange(active.id, it) },
            valueRange = active.min..active.max,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$controlLabel control" }
        )
        val increment = if (step == CounterTuneStep.GROUND) 5f else 1f
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onChange(active.id, (active.value - increment).coerceIn(active.min, active.max)) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(if (step == CounterTuneStep.PULSE) "Slower" else if (step == CounterTuneStep.COOLING) "Less cooling" else "Turn left") }
            OutlinedButton(
                onClick = { onChange(active.id, (active.value + increment).coerceIn(active.min, active.max)) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(if (step == CounterTuneStep.PULSE) "Faster" else if (step == CounterTuneStep.COOLING) "More cooling" else "Turn right") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (stepIndex > 0) {
                TextButton(onClick = { stepIndex-- }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Previous step") }
            }
            Button(
                onClick = {
                    if (step == CounterTuneStep.GROUND) {
                        onHandshake()
                    } else {
                        // Holding a matched signal stabilizes it at the center of
                        // its tolerance so later stages cannot chase residual drift.
                        onChange(active.id, active.target)
                        stepIndex++
                    }
                },
                enabled = ready,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(when (step) {
                CounterTuneStep.PULSE -> "Hold sweep"
                CounterTuneStep.COOLING -> "Hold cold loop"
                CounterTuneStep.GROUND -> "Engage handshake"
            }) }
        }
    }
}

@Composable
private fun SignalTrace(
    signal: CounterTuneSignal,
    step: CounterTuneStep,
    combined: Boolean,
    modifier: Modifier
) {
    Canvas(modifier.background(Color(0xFF02070E)).semantics {
        contentDescription = if (combined) "Combined signal. ${signal.diagnostic(step)}" else signal.diagnostic(step)
    }) {
        val center = size.height / 2f
        val scale = size.height / 5f
        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, center), Offset(size.width, center))
        for (index in 1..5) {
            val x = size.width * index / 6f
            drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height))
        }
        fun trace(selector: (SignalSample) -> Float, color: Color) {
            val path = Path()
            for (index in 0..180) {
                val position = index / 180f
                val x = position * size.width
                val y = center - selector(signal.sample(position, step)) * scale
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        }
        if (combined) trace({ it.combined }, QuietGreen) else {
            trace({ it.fork }, ForkCyan)
            trace({ it.cutter }, CutterGold)
        }
    }
}
