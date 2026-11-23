package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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

private val ScopeCyan = Color(0xFF00F5D4)
private val ScopeGreen = Color(0xFF00E676)
private val ScopeAmber = Color(0xFFFFB703)
private val TerminalDark = Color(0xFF060A12)

@Composable
fun DesktopTuningPuzzleDialog(
    services: DesktopAppServices,
    puzzle: TuningPuzzle,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    onValues: (Map<String, Float>) -> Unit = {}
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

    val choicePanel = com.example.starborn.shared.puzzle.ChoicePuzzlePanel.forPresentation(puzzle.presentation)
    if (choicePanel != null) {
        var feedback by remember(puzzle.id) { mutableStateOf<String?>(null) }
        var submitted by remember(puzzle.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(puzzle.title) },
            text = {
                com.example.starborn.shared.puzzle.PuzzleChoices(choicePanel, puzzle.prompt, puzzle.sliders.map {
                    com.example.starborn.shared.puzzle.SignalDial(
                        it.id, sliderValues[it.id] ?: it.initial, it.min, it.max, it.target, it.tolerance)
                }, feedback, onChange = { id, value ->
                    sliderValues[id] = value
                    feedback = null
                })
            },
            confirmButton = {
                Button(enabled = !submitted && sliderValues.values.all { it >= 0f }, onClick = {
                    if (isTuned) {
                        submitted = true
                        onValues(sliderValues.toMap())
                        onSuccess()
                    } else feedback = puzzle.failureHint
                }) { Text(choicePanel.submitLabel) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
        return
    }

    if (puzzle.presentation == "prism_optics") {
        var submitted by remember(puzzle.id) { mutableStateOf(false) }
        Dialog(onDismissRequest = onDismiss) {
            Surface(Modifier.width(560.dp).heightIn(max = 760.dp),
                shape = RoundedCornerShape(16.dp), color = TerminalDark) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(puzzle.title, style = MaterialTheme.typography.headlineSmall)
                    com.example.starborn.shared.puzzle.PrismOpticsPanel(
                        dials = puzzle.sliders.map {
                            com.example.starborn.shared.puzzle.SignalDial(
                                it.id, sliderValues[it.id] ?: it.initial, it.min, it.max, it.target, it.tolerance)
                        },
                        onChange = { id, value -> sliderValues[id] = value },
                        onCapture = {
                            if (isTuned && !submitted) {
                                submitted = true
                                puzzle.audioCue?.let { cue ->
                                    services.audioDriver.execute(AudioCommand.Play(AudioCueType.UI, cue, loop = false))
                                }
                                onValues(sliderValues.toMap())
                                onSuccess()
                            }
                        },
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                    )
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
        return
    }

    if (puzzle.presentation == "chrono_trace") {
        var submitted by remember(puzzle.id) { mutableStateOf(false) }
        Dialog(onDismissRequest = onDismiss) {
            Surface(Modifier.width(560.dp).heightIn(max = 760.dp),
                shape = RoundedCornerShape(16.dp), color = TerminalDark) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(puzzle.title, style = MaterialTheme.typography.headlineSmall)
                    com.example.starborn.shared.puzzle.ChronoCoilPanel(
                        dials = puzzle.sliders.map {
                            com.example.starborn.shared.puzzle.SignalDial(
                                it.id, sliderValues[it.id] ?: it.initial, it.min, it.max, it.target, it.tolerance)
                        },
                        onChange = { id, value -> sliderValues[id] = value },
                        onLock = {
                            if (isTuned && !submitted) {
                                submitted = true
                                puzzle.audioCue?.let { cue ->
                                    services.audioDriver.execute(AudioCommand.Play(AudioCueType.UI, cue, loop = false))
                                }
                                onValues(sliderValues.toMap())
                                onSuccess()
                            }
                        },
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                    )
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
        return
    }

    if (puzzle.presentation == "counter_tune") {
        var submitted by remember(puzzle.id) { mutableStateOf(false) }
        Dialog(onDismissRequest = onDismiss) {
            Surface(Modifier.width(560.dp).heightIn(max = 760.dp),
                shape = RoundedCornerShape(16.dp), color = TerminalDark) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(puzzle.title, style = MaterialTheme.typography.headlineSmall)
                    com.example.starborn.shared.puzzle.CounterTunePanel(
                        dials = puzzle.sliders.map {
                            com.example.starborn.shared.puzzle.SignalDial(
                                it.id, sliderValues[it.id] ?: it.initial, it.min, it.max, it.target, it.tolerance)
                        },
                        onChange = { id, value -> sliderValues[id] = value },
                        onHandshake = {
                            if (isTuned && !submitted) {
                                submitted = true
                                puzzle.audioCue?.let { cue ->
                                    services.audioDriver.execute(AudioCommand.Play(AudioCueType.UI, cue, loop = false))
                                }
                                onValues(sliderValues.toMap())
                                onSuccess()
                            }
                        },
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                    )
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
        return
    }

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
                            text = "CALIBRATION TERMINAL",
                            color = ScopeCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalStarbornFonts.current.orbitron,
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
                                text = if (isTuned) "READY TO CONFIRM" else "ADJUST CONTROLS",
                                color = if (isTuned) ScopeGreen else ScopeAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalStarbornFonts.current.orbitron
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
                                    fontFamily = LocalStarbornFonts.current.orbitron
                                )
                                val reading = when {
                                    inTolerance -> "In range"
                                    currentVal < slider.target -> "Too low"
                                    else -> "Too high"
                                }
                                Text(
                                    text = "${currentVal.toInt()} ${slider.unit.orEmpty()} · $reading",
                                    color = if (inTolerance) ScopeGreen else ScopeAmber,
                                    fontSize = 11.sp,
                                    fontFamily = LocalStarbornFonts.current.orbitron,
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
                                onValues(sliderValues.toMap())
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
                        Text(text = "ABORT", color = FieldMenuDesign.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
