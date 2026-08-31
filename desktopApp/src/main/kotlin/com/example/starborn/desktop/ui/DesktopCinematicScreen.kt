package com.example.starborn.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.cinematic.CinematicCameraMotion
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicStep
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.delay

@Composable
fun DesktopCinematicScreen(
    services: DesktopAppServices,
    scene: CinematicScene,
    onComplete: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = scene.steps.getOrNull(currentStepIndex) ?: CinematicStep(
        type = CinematicStepType.DIALOGUE,
        speaker = "SYSTEM",
        text = "Initializing orbital telemetry..."
    )

    // Camera Motion Animation
    val cameraTransition = rememberInfiniteTransition(label = "cameraMotion")
    val panProgress by cameraTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pan"
    )

    // Typewriter effect state
    var displayedChars by remember(currentStepIndex) { mutableStateOf(0) }
    val fullText = currentStep.text

    LaunchedEffect(currentStepIndex) {
        displayedChars = 0
        while (displayedChars < fullText.length) {
            delay(22)
            displayedChars++
        }
    }

    // Audio cue trigger
    LaunchedEffect(currentStepIndex) {
        currentStep.musicCue?.let { cue ->
            services.audioDriver.execute(AudioCommand.Play(AudioCueType.MUSIC, cue, loop = true))
        }
        currentStep.audioCue?.let { cue ->
            services.audioDriver.execute(AudioCommand.Play(AudioCueType.UI, cue, loop = false))
        }
        currentStep.voiceCue?.let { cue ->
            services.audioDriver.execute(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false))
        }
    }

    fun advanceStep() {
        if (displayedChars < fullText.length) {
            displayedChars = fullText.length // complete typewriter immediately
        } else {
            if (currentStepIndex + 1 < scene.steps.size) {
                currentStepIndex++
            } else {
                onComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { advanceStep() }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Spacebar, Key.Enter -> {
                            advanceStep()
                            true
                        }
                        Key.Escape -> {
                            if (scene.skippable) onComplete()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Cinematic Visual Art Canvas with Motion Dynamics
        val bgImage = currentStep.imagePath ?: "bg_station"
        val bgPainter = rememberDesktopAssetPainter(bgImage, services.assetProvider)

        val motionScale = when (currentStep.cameraMotion) {
            CinematicCameraMotion.SLOW_PUSH -> 1.0f + (panProgress * 0.08f)
            else -> 1.02f
        }
        val motionTranslateX = when (currentStep.cameraMotion) {
            CinematicCameraMotion.DRIFT_LEFT -> -30f * panProgress
            CinematicCameraMotion.DRIFT_RIGHT -> 30f * panProgress
            else -> 0f
        }

        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = motionScale
                    scaleY = motionScale
                    translationX = motionTranslateX
                },
            contentScale = ContentScale.Crop
        )

        // 2. Cinematic Letterbox Borders (21:9 anamorphic presentation)
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Top Letterbox Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.Black)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scene.title?.uppercase() ?: "RECORDED ARCHIVE // TRANSMISSION",
                        color = FieldMenuDesign.gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[SPACE] ADVANCE  •  [ESC] SKIP",
                        color = FieldMenuDesign.textMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom Letterbox Bar & Dialogue Deck
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 36.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speaker Portrait if present
                    if (currentStep.portrait != null) {
                        val portraitPainter = rememberDesktopAssetPainter(currentStep.portrait, services.assetProvider)
                        Image(
                            painter = portraitPainter,
                            contentDescription = currentStep.speaker,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(2.dp, FieldMenuDesign.cyan), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (currentStep.speaker != null) {
                            Text(
                                text = currentStep.speaker.uppercase(),
                                color = FieldMenuDesign.cyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = fullText.take(displayedChars),
                            color = FieldMenuDesign.text,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
