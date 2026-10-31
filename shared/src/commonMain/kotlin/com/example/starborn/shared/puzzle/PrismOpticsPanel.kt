package com.example.starborn.shared.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PrismOpticsPanel(dials: List<SignalDial>, onChange: (String, Float) -> Unit,
    onCapture: () -> Unit, modifier: Modifier = Modifier) {
    val optics = PrismOptics(dials)
    Surface(modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = Color(0xFF081923), contentColor = Color(0xFFE5F1F3)) {
    Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bring the light into focus at the circular receiver.")
        Canvas(Modifier.fillMaxWidth().height(108.dp).semantics { contentDescription = optics.feedback }) {
            drawRect(Color(0xFF081923))
            val prism = Offset(size.width * .25f, size.height * .72f)
            val receiver = Offset(size.width * .72f, size.height * .25f)
            val hit = receiver + Offset(optics.aimOffset * size.width * .7f, 0f)
            val color = when {
                !optics.glow.matched && optics.glow.value > optics.glow.target -> Color(0xFFFF977D)
                optics.glow.matched -> Color(0xFF8BE6AC)
                else -> Color(0xFF527869)
            }
            drawCircle(Color(0xFFFFDA8A), 13.dp.toPx(), prism)
            drawCircle(Color(0xFFB9F7FF), 16.dp.toPx(), receiver, style = Stroke(2.dp.toPx()))
            val spread = optics.fringe * size.height * .4f
            drawLine(Color(0xFFCC93FF), prism, hit + Offset(0f, spread), 3.dp.toPx())
            drawLine(Color(0xFF7FE6FF), prism, hit - Offset(0f, spread), 3.dp.toPx())
            drawLine(color, prism, hit, 4.dp.toPx())
            drawCircle(color, 6.dp.toPx(), hit)
        }
        Text(optics.feedback, style = MaterialTheme.typography.bodyMedium)
        for ((dial, label) in listOf(optics.angle to "Refraction", optics.focus to "Crystal spacing", optics.glow to "Glow")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(label, modifier = Modifier.width(78.dp), style = MaterialTheme.typography.labelMedium)
            Slider(value = dial.value, onValueChange = { onChange(dial.id, it) }, valueRange = dial.min..dial.max,
                modifier = Modifier.weight(1f).semantics { contentDescription = "$label control" },
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFDA8A), activeTrackColor = Color(0xFF7FE6FF)))
            }
        }
        }
        Button(onClick = onCapture, enabled = optics.ready, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDA8A), contentColor = Color(0xFF081923))) { Text("Capture light") }
    }
}
}
