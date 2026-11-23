package com.example.starborn.shared.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ChronoCoilPanel(
    dials: List<SignalDial>,
    onChange: (String, Float) -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trace = ChronoCoilTrace(dials)
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = Color(0xFF0A1220),
        contentColor = Color(0xFFE5F1F3)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 460.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Damping and carrier drift pull against each other—flatten the coupled trace inside the corridor, then align the return echo in the gate.",
                    style = MaterialTheme.typography.bodySmall
                )
                Canvas(
                    Modifier.fillMaxWidth().height(116.dp)
                        .semantics { contentDescription = trace.feedback }
                ) {
                    drawRect(Color(0xFF070D18))
                    val midY = size.height * 0.5f
                    val corridorHalf = size.height * 0.14f
                    // Target horizontal carrier corridor
                    drawRect(
                        color = if (trace.carrierStable) Color(0x338BE6AC) else Color(0x267FE6FF),
                        topLeft = Offset(0f, midY - corridorHalf),
                        size = Size(size.width, corridorHalf * 2f)
                    )
                    // Center reference line
                    drawLine(
                        color = Color(0x557FE6FF),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Vertical target phase gate
                    val gateX = size.width * 0.5f
                    val gateHalf = size.width * 0.06f
                    drawRect(
                        color = if (trace.dilation.matched) Color(0x448BE6AC) else Color(0x33FFDA8A),
                        topLeft = Offset(gateX - gateHalf, 6.dp.toPx()),
                        size = Size(gateHalf * 2f, size.height - 12.dp.toPx())
                    )
                    drawRect(
                        color = if (trace.dilation.matched) Color(0xFF8BE6AC) else Color(0xFFFFDA8A),
                        topLeft = Offset(gateX - gateHalf, 6.dp.toPx()),
                        size = Size(gateHalf * 2f, size.height - 12.dp.toPx()),
                        style = Stroke(1.5.dp.toPx())
                    )

                    // Live coupled carrier waveform (drifts vertically with damping+drift, jitters with instability)
                    val baselineY = midY - trace.effectiveDriftOffset * size.height * 0.36f
                    val amp = trace.jitter * size.height * 0.28f
                    val traceColor = when {
                        trace.carrierStable -> Color(0xFF8BE6AC)
                        trace.jitter > 0.55f -> Color(0xFFFF977D)
                        else -> Color(0xFF7FE6FF)
                    }
                    val path = Path()
                    val steps = 48
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps
                        val x = t * size.width
                        val harmonic = sin(t * 10.0 * PI) * 0.65 + sin(t * 23.0 * PI) * 0.35
                        val y = (baselineY + harmonic.toFloat() * amp).coerceIn(6.dp.toPx(), size.height - 6.dp.toPx())
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = traceColor, style = Stroke(2.5.dp.toPx()))

                    // Return echo pulse marker (controlled by Phase Dilation)
                    val echoX = (gateX + trace.phaseOffset * size.width * 0.42f)
                        .coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
                    val echoColor = if (trace.dilation.matched) Color(0xFF8BE6AC) else Color(0xFFFFDA8A)
                    drawLine(
                        color = echoColor.copy(alpha = 0.65f),
                        start = Offset(echoX, 8.dp.toPx()),
                        end = Offset(echoX, size.height - 8.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(echoColor, 6.dp.toPx(), Offset(echoX, baselineY.coerceIn(12.dp.toPx(), size.height - 12.dp.toPx())))
                }

                Text(trace.feedback, style = MaterialTheme.typography.bodyMedium)

                val controls = listOf(
                    Triple(trace.damping, "Flux Damping", "%"),
                    Triple(trace.drift, "Carrier Drift", " kHz"),
                    Triple(trace.dilation, "Phase Dilation", "°")
                )
                for ((dial, label, unit) in controls) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(92.dp), style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = dial.value,
                            onValueChange = { onChange(dial.id, it.roundToInt().toFloat()) },
                            valueRange = dial.min..dial.max,
                            modifier = Modifier.weight(1f).semantics { contentDescription = "$label control" },
                            colors = SliderDefaults.colors(
                                thumbColor = if (dial.matched) Color(0xFF8BE6AC) else Color(0xFFFFDA8A),
                                activeTrackColor = if (dial.matched) Color(0xFF8BE6AC) else Color(0xFF7FE6FF)
                            )
                        )
                        Text(
                            "${dial.value.roundToInt()}$unit",
                            modifier = Modifier.width(52.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dial.matched) Color(0xFF8BE6AC) else Color(0xFFE5F1F3)
                        )
                    }
                }
            }
            Button(
                onClick = onLock,
                enabled = trace.ready,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8BE6AC),
                    contentColor = Color(0xFF0A1220)
                )
            ) {
                Text("Lock tachyon phase")
            }
        }
    }
}
