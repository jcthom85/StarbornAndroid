package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.model.TuningPuzzle
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlin.math.abs
import kotlin.math.sin

private val ScopeCyan = Color(0xFF00F5D4)
private val ScopeGreen = Color(0xFF00E676)
private val ScopeAmber = Color(0xFFFFB703)
private val ScopeRed = Color(0xFFFF5252)
private val TerminalDark = Color(0xFF060A12)

@Composable
fun DesktopTuningPuzzleDialog(
    services: DesktopAppServices,
    puzzle: TuningPuzzle,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    // Slider current values
    val sliderValues = remember(puzzle) {
        mutableStateMapOf<String, Float>().apply {
            puzzle.sliders.forEach { slider ->
                put(slider.id, slider.initial)
            }
        }
    }

    // Check if all sliders are within their target tolerance
    val isTuned by remember(sliderValues.values.toList()) {
        derivedStateOf {
            puzzle.sliders.all { slider ->
                val current = sliderValues[slider.id] ?: slider.initial
                abs(current - slider.target) <= slider.tolerance
            }
        }
    }

    // Sine wave animated phase
    val transition = rememberInfiniteTransition(label = "scopePhase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    var hasTriggeredSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(820.dp)
                .wrapContentHeight()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
            shape = RoundedCornerShape(16.dp),
            color = TerminalDark,
            border = BorderStroke(1.5.dp, if (isTuned) ScopeGreen else ScopeCyan.copy(alpha = 0.8f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "RESONANCE FREQUENCY TUNER // OSCILLOSCOPE TERMINAL",
                            color = ScopeCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = puzzle.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTuned) ScopeGreen.copy(alpha = 0.20f) else ScopeAmber.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, if (isTuned) ScopeGreen else ScopeAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isTuned) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isTuned) ScopeGreen else ScopeAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isTuned) "HARMONIC LOCK ACHIEVED" else "CALIBRATING SIGNAL",
                                color = if (isTuned) ScopeGreen else ScopeAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Text(
                    text = puzzle.prompt,
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                // Central Oscilloscope Waveform Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF03060C))
                        .border(BorderStroke(1.dp, ScopeCyan.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        // Oscilloscope Grid Lines
                        val gridAlpha = 0.08f
                        var gx = 0f
                        while (gx < w) {
                            drawLine(ScopeCyan.copy(alpha = gridAlpha), Offset(gx, 0f), Offset(gx, h), 1f)
                            gx += 30.dp.toPx()
                        }
                        var gy = 0f
                        while (gy < h) {
                            drawLine(ScopeCyan.copy(alpha = gridAlpha), Offset(0f, gy), Offset(w, gy), 1f)
                            gy += 30.dp.toPx()
                        }

                        // Target Harmonic Wave (Ghost reference line)
                        val targetPath = Path()
                        val targetFreq = 0.035f
                        val targetAmp = 45f
                        for (x in 0..w.toInt()) {
                            val y = midY + targetAmp * sin(x * targetFreq + phase)
                            if (x == 0) targetPath.moveTo(0f, y) else targetPath.lineTo(x.toFloat(), y)
                        }
                        drawPath(
                            path = targetPath,
                            color = ScopeCyan.copy(alpha = 0.25f),
                            style = Stroke(width = 2f)
                        )

                        // Player Adjusted Resonator Wave
                        val firstSliderVal = sliderValues.values.firstOrNull() ?: 50f
                        val secondSliderVal = sliderValues.values.drop(1).firstOrNull() ?: 50f
                        val playerFreq = 0.015f + (firstSliderVal / 100f) * 0.04f
                        val playerAmp = 20f + (secondSliderVal / 100f) * 40f

                        val playerPath = Path()
                        for (x in 0..w.toInt()) {
                            val y = midY + playerAmp * sin(x * playerFreq + phase)
                            if (x == 0) playerPath.moveTo(0f, y) else playerPath.lineTo(x.toFloat(), y)
                        }

                        val activeWaveColor = if (isTuned) ScopeGreen else ScopeAmber
                        drawPath(
                            path = playerPath,
                            color = activeWaveColor,
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Telemetry corner tag
                    Text(
                        text = if (isTuned) "FREQUENCY MATCH: 100%" else "FREQUENCY DRIFT DETECTED",
                        color = if (isTuned) ScopeGreen else ScopeAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }

                // Tuning Sliders
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    puzzle.sliders.forEach { slider ->
                        val currentVal = sliderValues[slider.id] ?: slider.initial
                        val inTolerance = abs(currentVal - slider.target) <= slider.tolerance

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slider.label.uppercase(),
                                    color = if (inTolerance) ScopeGreen else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentVal.toInt()} ${slider.unit ?: "Hz"}  [TARGET: ${slider.target.toInt()} ±${slider.tolerance.toInt()}]",
                                    color = if (inTolerance) ScopeGreen else ScopeAmber,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = currentVal,
                                onValueChange = { sliderValues[slider.id] = it },
                                valueRange = slider.min..slider.max,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (inTolerance) ScopeGreen else ScopeCyan,
                                    activeTrackColor = if (inTolerance) ScopeGreen else ScopeCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isTuned && !hasTriggeredSuccess) {
                                hasTriggeredSuccess = true
                                puzzle.audioCue?.let { cue ->
                                    services.audioDriver.execute(
                                        AudioCommand.Play(AudioCueType.UI, cue, loop = false)
                                    )
                                }
                                onSuccess()
                            }
                        },
                        enabled = isTuned,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScopeGreen,
                            disabledContainerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = Color.Black,
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(
                            text = if (isTuned) "ENGAGE HARMONIC OVERRIDE" else "ALIGN FREQUENCIES TO UNLOCK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.elevatedPanel),
                        border = BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.45f).height(46.dp)
                    ) {
                        Text(text = "ABORT [ESC]", color = FieldMenuDesign.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
