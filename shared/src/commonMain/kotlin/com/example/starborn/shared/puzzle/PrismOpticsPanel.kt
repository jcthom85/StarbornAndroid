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
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Guide light through the prism into the circular receiver. Close the colored fringe, then balance the glow.")
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = optics.feedback }) {
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
            Text(label, style = MaterialTheme.typography.labelLarge)
            Slider(value = dial.value, onValueChange = { onChange(dial.id, it) }, valueRange = dial.min..dial.max,
                modifier = Modifier.semantics { contentDescription = "$label control" })
        }
        Button(onClick = onCapture, enabled = optics.ready, modifier = Modifier.fillMaxWidth()) { Text("Capture light") }
    }
}
