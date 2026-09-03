package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class TapeDeckEntry(
    val id: String,
    val audioCue: String,
    val title: String,
    val locationFound: String
)

@Composable
fun DesktopTapeDeckDialog(
    services: DesktopAppServices,
    onDismiss: () -> Unit
) {
    val tapes = remember {
        listOf(
            TapeDeckEntry("vhs_tape_01", "gf_01_unpayable_debt", "Film 01: Unpayable Debt", "Mining Pit - Supply Stash"),
            TapeDeckEntry("vhs_tape_02", "gf_02_memories_of_another_life", "Film 02: Memories of Another Life", "Colony - Jed's Office"),
            TapeDeckEntry("vhs_tape_03", "gf_03_showdown_in_the_rain", "Film 03: Showdown in the Rain", "Coast - Glow-Moss Cavern"),
            TapeDeckEntry("vhs_tape_04", "gf_04_the_road_at_night", "Film 04: The Road at Night", "Sector 9 - Ridge Plateau"),
            TapeDeckEntry("vhs_tape_05", "gf_05_the_black_city", "Film 05: The Black City", "Spire - Night Market"),
            TapeDeckEntry("vhs_tape_06", "gf_06_refuge", "Film 06: Refuge", "Spire - SkyPark Pavilion"),
            TapeDeckEntry("vhs_tape_07", "gf_07_reclamation", "Film 07: Reclamation", "Foundry - Smelter Waste"),
            TapeDeckEntry("vhs_tape_08", "gf_08_the_end_of_the_beginning", "Film 08: The End of the Beginning", "Foundry - Titan Dock"),
            TapeDeckEntry("vhs_tape_09", "gf_09_reconciliation", "Film 09: Reconciliation", "Void Ring - Solarium"),
            TapeDeckEntry("vhs_tape_10", "gf_10_shackles", "Film 10: Shackles", "Source - Memory Bridge")
        )
    }

    var activePlayingTape by remember { mutableStateOf<TapeDeckEntry?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(880.dp)
                .height(600.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF101216),
            border = BorderStroke(1.5.dp, Color(0xFFFFB300).copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "THE GREAT FRONTIER // FILM ARCHIVE",
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Astra Habitat Cinema Deck • Analog Widescreen Archive",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Cassette Player Visualizer & Turntable Deck
                DesktopCassetteReelVisualizer(
                    activeTape = activePlayingTape,
                    onStop = {
                        activePlayingTape?.let {
                            services.audioDriver.execute(AudioCommand.Stop(AudioCueType.MUSIC, it.audioCue))
                        }
                        activePlayingTape = null
                    }
                )

                // Tape List Container
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0C10))
                            .border(BorderStroke(1.dp, Color(0xFF222834)), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tapes) { tape ->
                            val isPlaying = activePlayingTape?.id == tape.id
                            DesktopTapeItemRow(
                                tape = tape,
                                isPlaying = isPlaying,
                                onPlayClick = {
                                    if (isPlaying) {
                                        services.audioDriver.execute(AudioCommand.Stop(AudioCueType.MUSIC, tape.audioCue))
                                        activePlayingTape = null
                                    } else {
                                        activePlayingTape?.let {
                                            services.audioDriver.execute(AudioCommand.Stop(AudioCueType.MUSIC, it.audioCue))
                                        }
                                        activePlayingTape = tape
                                        services.audioDriver.execute(
                                            AudioCommand.Play(
                                                type = AudioCueType.MUSIC,
                                                cueId = tape.audioCue,
                                                gain = 1f,
                                                loop = true
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopCassetteReelVisualizer(
    activeTape: TapeDeckEntry?,
    onStop: () -> Unit
) {
    val isSpinning = activeTape != null
    val infiniteTransition = rememberInfiniteTransition(label = "cassetteReelSpin")
    val reelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelAngle"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1D24),
        border = BorderStroke(1.dp, if (isSpinning) Color(0xFFFFB300) else Color(0xFF2E3440))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Animated Reel
            DesktopReelWheel(rotation = if (isSpinning) reelRotation else 0f)

            // Center Cassette Label Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isSpinning) "● FEATURE SCREENING" else "■ CINEMA STANDBY",
                    color = if (isSpinning) Color(0xFF00F5D4) else Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = activeTape?.title ?: "No Film Loaded",
                    color = if (isSpinning) Color(0xFFFFD54F) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = activeTape?.locationFound ?: "Select a recovered film from the Astra cinema archive below",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // Right Animated Reel & Stop Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DesktopReelWheel(rotation = if (isSpinning) reelRotation else 0f)

                if (isSpinning) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A1015))
                            .border(BorderStroke(1.dp, Color(0xFFFF3366)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop screening",
                            tint = Color(0xFFFF3366),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopReelWheel(rotation: Float) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color(0xFF0D0F14))
            .border(BorderStroke(2.dp, Color(0xFF3B4252)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp).rotate(rotation)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = 0.8f),
                radius = radius,
                style = Stroke(width = 3f)
            )

            // Spokes
            for (i in 0 until 3) {
                val angleRad = (i * 120f) * (PI / 180f).toFloat()
                val endX = center.x + kotlin.math.cos(angleRad) * radius
                val endY = center.y + kotlin.math.sin(angleRad) * radius
                drawLine(
                    color = Color(0xFFFFB300).copy(alpha = 0.6f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

@Composable
private fun DesktopTapeItemRow(
    tape: TapeDeckEntry,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) Color(0x33FFB300) else Color(0xFF14171E))
            .border(
                BorderStroke(1.dp, if (isPlaying) Color(0xFFFFB300) else Color(0xFF222834)),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = tape.title,
                color = if (isPlaying) Color(0xFFFFD54F) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Recovered: ${tape.locationFound}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFFFFB300) else Color(0xFF1F2430))
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Stop screening" else "Start screening",
                    tint = if (isPlaying) Color.Black else Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
