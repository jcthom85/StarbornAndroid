package com.example.starborn.desktop.ui

import com.example.starborn.shared.ui.typewriterText
import com.example.starborn.ui.cinematic.rememberCinematicAdvance

import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable as CoreAnimatable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.domain.model.DialogueLine
import java.util.Locale
import androidx.compose.ui.unit.*
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.audio.*
import com.example.starborn.domain.cinematic.*
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun cinematicLerp(a: Float, b: Float, t: Float) = a + (b - a) * t
@Composable
private fun rememberAssetPainter(path: String?, fallback: ColorPainter, async: Boolean) =
    rememberDesktopAssetPainter(path, LocalCinematicServices.current.assetProvider)
val LocalDesktopForeground = staticCompositionLocalOf { true }
private val LocalCinematicServices = staticCompositionLocalOf<DesktopAppServices> { error("Cinematic services missing") }

@Composable
fun DesktopCinematicOverlay(state: CinematicUiState, services: DesktopAppServices, onAdvance: () -> Unit, onSkip: () -> Unit) {
    CompositionLocalProvider(LocalCinematicServices provides services) {
        // The movie covers the exploration controls; clicks must not reach them.
        Box(Modifier.fillMaxSize().pointerInput(state.sceneId) { detectTapGestures(onTap = {}) }) {
            if (state.presentation == CinematicPresentation.ILLUSTRATED) {
                DesktopIllustratedCinematicOverlay(state, onAdvance, onSkip, services)
            } else {
                DesktopStandardCinematicOverlay(state, services, onAdvance, onSkip)
            }
        }
    }
}

@Composable
private fun DesktopStandardCinematicOverlay(
    state: CinematicUiState,
    services: DesktopAppServices,
    onAdvance: () -> Unit,
    onSkip: () -> Unit
) {
    val advanceOnce = rememberCinematicAdvance(state.sceneId, state.stepIndex, onAdvance)
    val stepKey = "${state.sceneId}_${state.stepIndex}"
    val speaker = state.step.speaker?.takeIf { it.isNotBlank() }
    val focus = remember { FocusRequester() }

    LaunchedEffect(stepKey) {
        focus.requestFocus()
        state.step.audioCue?.takeIf { it.isNotBlank() }?.let {
            services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.UI, it)))
        }
        state.step.voiceCue?.takeIf { it.isNotBlank() }?.let {
            services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.VOICE, it)))
        }
        state.step.durationSeconds?.takeIf { it > 0 }?.let {
            delay((it * 1000).toLong())
            advanceOnce()
        }
    }

    if (speaker != null) {
        var dialogueRevealed by remember(stepKey) { mutableStateOf(false) }
        var revealAllRequest by remember(stepKey) { mutableIntStateOf(0) }
        val advanceOrReveal = {
            if (!dialogueRevealed) {
                revealAllRequest += 1
            } else {
                advanceOnce()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (state.backdrop == CinematicBackdrop.BLACK) 1f else 0.72f))
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter || event.key == Key.Spacebar) {
                        if (event.type == KeyEventType.KeyUp) advanceOrReveal()
                        true
                    } else {
                        false
                    }
                }
                .focusable()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = advanceOrReveal
                )
        ) {
            DesktopAuthoredDialogueOverlay(
                services = services,
                dialogue = DialogueUi(
                    line = DialogueLine(
                        id = stepKey,
                        speaker = speaker,
                        text = state.step.text,
                        portrait = state.step.portrait,
                        voiceCue = state.step.voiceCue
                    ),
                    portrait = state.step.portrait,
                    voiceCue = state.step.voiceCue
                ),
                choices = emptyList(),
                onAdvance = advanceOnce,
                onChoice = { advanceOnce() },
                onPlayVoice = { cue ->
                    services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L)))
                },
                onPlayMurmur = { cue ->
                    services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L)))
                },
                onPlayMurmurWithPitch = { cue, pitch ->
                    services.audioDriver.executeAll(
                        listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L, pitch = pitch))
                    )
                },
                onRevealFinished = { dialogueRevealed = true },
                revealAllRequest = revealAllRequest,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 28.dp, end = 28.dp, bottom = 32.dp)
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
            )
            if (state.skippable) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp)
                ) {
                    Text("Skip", color = Color.White.copy(alpha = 0.82f))
                }
            }
        }
        return
    }

    val text = state.step.text
    var revealed by remember(stepKey) { mutableIntStateOf(0) }
    val advanceOrReveal = { if (revealed < text.length) revealed = text.length else advanceOnce() }
    LaunchedEffect(stepKey) {
        while (revealed < text.length) {
            delay(28)
            revealed = (revealed + 1).coerceAtMost(text.length)
        }
    }

    val accentColor = Color(0xFF7BE8FF)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (state.backdrop == CinematicBackdrop.BLACK) 1f else 0.72f))
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Spacebar) {
                    if (event.type == KeyEventType.KeyUp) advanceOrReveal()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = advanceOrReveal
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 28.dp, end = 28.dp, bottom = 48.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            color = Color(0xFF060B14).copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.12f),
                        accentColor.copy(alpha = 0.30f)
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CinematicAccentBar(accentColor)
                Text(
                    text = typewriterText(text, revealed),
                    color = Color.White.copy(alpha = 0.94f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 28.sp,
                        letterSpacing = 0.3.sp
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = advanceOrReveal) { Text("Continue") }
                    if (state.skippable) {
                        TextButton(onClick = onSkip) { Text("Skip") }
                    }
                }
            }
        }
    }
}

/** The last frame drawn by [DesktopIllustratedCinematicOverlay], held so the next step can dissolve over it. */
private data class RenderedCinematicFrame(
    val imagePath: String?,
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val alpha: Float = 1f
)

@Composable
fun DesktopIllustratedCinematicOverlay(
    state: CinematicUiState,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    services: DesktopAppServices,
    modifier: Modifier = Modifier
) {
    val foreground = LocalDesktopForeground.current
    val stepKey = "${state.sceneId}_${state.stepIndex}"
    val advanceOnce = rememberCinematicAdvance(state.sceneId, state.stepIndex, onAdvance)
    // Capture the previous step once, before updating its replacement's visible transform.
    val lastRenderedFrame = remember(state.sceneId) { mutableStateOf<RenderedCinematicFrame?>(null) }
    val outgoing = remember(stepKey) { lastRenderedFrame.value }
    val durationMs = ((state.step.durationSeconds ?: 4.0) * 1000.0).toLong().coerceAtLeast(500L)
    val waitsForTextReveal = state.step.captionStyle == CinematicCaptionStyle.DIALOGUE ||
        state.step.captionStyle == CinematicCaptionStyle.NARRATION
    var captionRevealFinished by remember(stepKey) { mutableStateOf(!waitsForTextReveal) }
    var revealAllRequest by remember(stepKey) { mutableIntStateOf(0) }

    state.preloadImages.forEach { imagePath ->
        rememberAssetPainter(imagePath, fallback = ColorPainter(Color.Black), async = true)
    }

    DisposableEffect(state.sceneId, state.ambientCue, services) {
        val cue = state.ambientCue
        if (!cue.isNullOrBlank()) {
            services.audioDriver.executeAll(
                listOf(AudioCommand.Play(AudioCueType.AMBIENT, cue, loop = true, fadeMs = 350L))
            )
        }
        onDispose {
            if (!cue.isNullOrBlank()) {
                services.audioDriver.executeAll(
                    listOf(AudioCommand.Stop(AudioCueType.AMBIENT, cue, fadeMs = 450L))
                )
            }
        }
    }

    LaunchedEffect(stepKey, services) {
        state.step.audioCue?.takeIf { it.isNotBlank() }?.let { cue ->
            services.audioDriver.executeAll(
                listOf(AudioCommand.Play(AudioCueType.UI, cue, loop = false, fadeMs = 0L))
            )
        }
        state.step.voiceCue?.takeIf { it.isNotBlank() }?.let { cue ->
            services.audioDriver.executeAll(
                listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L))
            )
        }
    }

    // Dims the frame over the tail of the step. Steps otherwise only fade in, so the
    // last step of a scene would pop off in a single frame.
    val fadeOutMs = ((state.step.fadeOutSeconds ?: 0.0) * 1000.0)
        .toLong()
        .coerceIn(0L, durationMs)
    var contentDimmed by remember(stepKey) { mutableStateOf(false) }

    LaunchedEffect(stepKey, durationMs, foreground) {
        if (foreground) {
            delay((durationMs - fadeOutMs).coerceAtLeast(0L))
            if (fadeOutMs > 0L) {
                contentDimmed = true
                delay(fadeOutMs)
            }
            advanceOnce()
        }
    }

    if (
        state.step.captionStyle == CinematicCaptionStyle.NONE &&
        state.step.imagePath.isNullOrBlank() &&
        state.backdrop == CinematicBackdrop.ROOM
    ) {
        val reveal = remember(stepKey) { CoreAnimatable(1f) }
        LaunchedEffect(stepKey) {
            reveal.animateTo(0f, animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing))
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = reveal.value))
                .semantics { contentDescription = "Room fading in" }
        )
        return
    }

    val painter = rememberAssetPainter(
        state.step.imagePath,
        fallback = ColorPainter(Color.Black),
        async = false
    )
    val motion = remember(stepKey) { CoreAnimatable(0f) }
    LaunchedEffect(stepKey, durationMs) {
        motion.animateTo(1f, animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing))
    }
    val progress = motion.value
    val legacyStartScale = when (state.step.cameraMotion) {
        CinematicCameraMotion.SLOW_PUSH -> 1.02f
        CinematicCameraMotion.DRIFT_LEFT,
        CinematicCameraMotion.DRIFT_RIGHT -> 1.05f
        CinematicCameraMotion.NONE -> 1f
    }
    val legacyEndScale = when (state.step.cameraMotion) {
        CinematicCameraMotion.SLOW_PUSH -> 1.08f
        CinematicCameraMotion.DRIFT_LEFT,
        CinematicCameraMotion.DRIFT_RIGHT -> 1.05f
        CinematicCameraMotion.NONE -> 1f
    }
    val legacyEndX = when (state.step.cameraMotion) {
        CinematicCameraMotion.DRIFT_LEFT -> -32f
        CinematicCameraMotion.DRIFT_RIGHT -> 32f
        else -> 0f
    }
    // Enforce a minimum safe bleed scale (1.06f) so camera drift Y and screen shake
    // never pull the cropped image edge away from the top or bottom on tall aspect ratio displays.
    val density = LocalDensity.current
    val endX = with(density) { (state.step.cameraEndX?.toFloat() ?: legacyEndX).dp.toPx() }
    val endY = with(density) { (state.step.cameraEndY ?: 0.0).toFloat().dp.toPx() }
    // Consecutive captions on the same artwork should continue the camera move
    // from the visible framing, even when the player advances early.
    val continuousShot = outgoing?.takeIf {
        state.sceneId != "intro_prologue" && it.imagePath != null && it.imagePath == state.step.imagePath
    }
    val startScale = continuousShot?.scale ?: state.step.cameraStartScale?.toFloat() ?: legacyStartScale
    val endScale = state.step.cameraEndScale?.toFloat() ?: legacyEndScale
    val startX = continuousShot?.translationX ?: with(density) { (state.step.cameraStartX ?: 0.0).toFloat().dp.toPx() }
    val startY = continuousShot?.translationY ?: with(density) { (state.step.cameraStartY ?: 0.0).toFloat().dp.toPx() }
    val scale = cinematicLerp(startScale, endScale, progress).coerceAtLeast(1.06f)
    val driftX = cinematicLerp(startX, endX, progress)
    val driftY = cinematicLerp(startY, endY, progress)
    // Keep authored cuts between shots. During a dissolve, draw the outgoing frame
    // at full opacity beneath the incoming one so black doesn't show through.
    val transitionMs = if (continuousShot != null || state.step.transition == CinematicTransition.CUT) 0 else 520
    val enter = remember(stepKey) { CoreAnimatable(if (transitionMs == 0) 1f else 0f) }
    var advanceRequested by remember(stepKey) { mutableStateOf(false) }
    // A second tap may come in mid-dissolve. Finish that blend, then advance once.
    val requestAdvance: () -> Unit = {
        if (enter.value >= 1f) advanceOnce() else advanceRequested = true
    }
    LaunchedEffect(stepKey) {
        if (transitionMs > 0) {
            enter.animateTo(1f, animationSpec = tween(durationMillis = transitionMs, easing = FastOutSlowInEasing))
            if (advanceRequested) advanceOnce()
        }
    }
    val dimAlpha by animateFloatAsState(
        targetValue = if (contentDimmed) 0f else 1f,
        // Linear on the way out: an eased dim reads as the image stalling at the
        // top of the curve rather than settling into black.
        animationSpec = tween(durationMillis = fadeOutMs.toInt().coerceAtLeast(1), easing = LinearEasing),
        label = "illustratedCinematicDim"
    )
    val contentAlpha = enter.value * dimAlpha

    val impactShakeX = remember(stepKey) { CoreAnimatable(0f) }
    val impactShakeY = remember(stepKey) { CoreAnimatable(0f) }
    val impactFlash = remember(stepKey) { CoreAnimatable(0f) }
    val impactFlashColor = remember(stepKey) {
        when (state.step.audioCue) {
            "sfx_combat_plasma_arc" -> Color(0xFF80F8FF)
            "wpn_orion_bowl_resonance" -> Color(0xFFB3F7FF)
            "sfx_w2_stasis_release" -> Color(0xFFC8F4FF)
            "sfx_security_lockdown" -> Color(0xFFFF1744)
            "sfx_w3_shield_anchor" -> Color(0xFFFFE57F)
            "sfx_intro_stasis_lock" -> Color(0xFFB3F7FF)
            "sfx_combat_enemy_void_blast" -> Color(0xFFD1A6FF)
            "sfx_combat_shield_deflect" -> Color(0xFFFFE0E0)
            else -> Color.White
        }
    }
    LaunchedEffect(stepKey) {
        val cue = state.step.audioCue
        if (cue == "sfx_intro_door_buckle") {
            // 5 accelerating, escalating pounding impacts against the reinforced blast doors
            val buckleHits = listOf(
                240L to 7,
                1009L to 9,
                1845L to 11,
                2296L to 13,
                2690L to 18
            )
            for ((atMs, ampDp) in buckleHits) {
                launch {
                    delay(atMs)
                    val amp = with(density) { ampDp.dp.toPx() }
                    impactShakeX.snapTo(amp)
                    impactShakeY.snapTo(-amp * 0.45f)
                    launch {
                        impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                        val cycles = if (ampDp >= 18) 6 else 3
                        val tremorAmp = with(density) { (ampDp * 0.22f).dp.toPx() }
                        for (cycle in cycles downTo 1) {
                            val decayRatio = cycle.toFloat() / cycles
                            val sign = if (cycle % 2 == 0) 1f else -1f
                            impactShakeX.animateTo(
                                targetValue = tremorAmp * decayRatio * sign,
                                animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                            )
                            impactShakeX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 35, easing = LinearEasing)
                            )
                        }
                    }
                    launch {
                        impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                    }
                }
            }
        } else if (cue == "sfx_intro_door_collapse" || cue == "sfx_intro_beast_strike") {
            val isBeast = cue == "sfx_intro_beast_strike"
            val initialAmp = with(density) { (if (isBeast) 18.dp else 13.dp).toPx() }
            val tremorAmp = with(density) { (if (isBeast) 4.5.dp else 3.dp).toPx() }
            launch {
                impactShakeX.snapTo(initialAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                val tremorCycles = 8
                val cycleDuration = 120
                for (cycle in tremorCycles downTo 1) {
                    val decayRatio = cycle.toFloat() / tremorCycles
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(
                        targetValue = tremorAmp * decayRatio * sign,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                    impactShakeX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                }
            }
            launch {
                impactShakeY.snapTo(-initialAmp * 0.45f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                val tremorCycles = 8
                val cycleDuration = 120
                for (cycle in tremorCycles downTo 1) {
                    val decayRatio = cycle.toFloat() / tremorCycles
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(
                        targetValue = tremorAmp * 0.4f * decayRatio * sign,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                    impactShakeY.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = cycleDuration / 2, easing = LinearEasing)
                    )
                }
            }
        } else if (cue == "sfx_intro_chime_launch") {
            val launchAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(launchAmp)
                impactShakeY.snapTo(-launchAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_combat_plasma_arc") {
            val arcAmp = with(density) { 10.dp.toPx() }
            val buzzAmp = with(density) { 3.dp.toPx() }
            launch {
                impactShakeX.snapTo(arcAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(buzzAmp * decay * sign, animationSpec = tween(durationMillis = 36, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 36, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(-arcAmp * 0.5f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_distant_gate_rumble") {
            val rumbleAmp = with(density) { 3.5.dp.toPx() }
            launch {
                delay(180L)
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rumbleAmp * decay * sign, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                }
            }
            launch {
                delay(180L)
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeX.animateTo(rumbleAmp * 0.45f * decay * sign, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 85, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_w2_stasis_release") {
            val sealAmp = with(density) { 4.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(sealAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 5 downTo 1) {
                    val decay = cycle / 5f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(sealAmp * 0.35f * decay * sign, animationSpec = tween(durationMillis = 60, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 60, easing = LinearEasing))
                }
            }
            launch {
                impactFlash.snapTo(0.4f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 360, easing = LinearEasing))
            }
        } else if (cue == "sfx_security_lockdown") {
            val slamAmp = with(density) { 12.dp.toPx() }
            val rattleAmp = with(density) { 3.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(slamAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
                for (cycle in 5 downTo 1) {
                    val decay = cycle / 5f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rattleAmp * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(slamAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            }
            launch {
                impactFlash.snapTo(0.45f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 340, easing = LinearEasing))
            }
        } else if (cue == "sfx_w3_shield_anchor") {
            val anvilAmp = with(density) { 6.dp.toPx() }
            launch {
                impactShakeY.snapTo(anvilAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing))
                for (cycle in 4 downTo 1) {
                    val decay = cycle / 4f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(anvilAmp * 0.35f * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
            launch {
                impactFlash.snapTo(0.42f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 380, easing = LinearEasing))
            }
        } else if (cue == "sfx_intro_stasis_lock") {
            val snapAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(snapAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing))
            }
            launch {
                impactFlash.snapTo(0.55f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 420, easing = LinearEasing))
            }
        } else if (cue == "sfx_combat_enemy_void_blast") {
            val riftAmp = with(density) { 13.dp.toPx() }
            val shudderAmp = with(density) { 4.dp.toPx() }
            launch {
                impactShakeX.snapTo(riftAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in 6 downTo 1) {
                    val decay = cycle / 6f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(shudderAmp * decay * sign, animationSpec = tween(durationMillis = 52, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 52, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(-riftAmp * 0.5f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            launch {
                impactFlash.snapTo(0.52f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 380, easing = LinearEasing))
            }
        } else if (cue == "sfx_door_airlock_close") {
            val slamAmp = with(density) { 11.dp.toPx() }
            val rattleAmp = with(density) { 2.5.dp.toPx() }
            launch {
                impactShakeY.snapTo(slamAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
                for (cycle in 3 downTo 1) {
                    val decay = cycle / 3f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(rattleAmp * decay * sign, animationSpec = tween(durationMillis = 45, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 45, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(slamAmp * 0.35f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
            }
        } else if (cue == "sfx_pod_launch" || cue == "sfx_w2_astra_launch") {
            val isAstra = cue == "sfx_w2_astra_launch"
            val kickAmp = with(density) { (if (isAstra) 7.dp else 9.dp).toPx() }
            val thrustAmp = with(density) { (if (isAstra) 2.2.dp else 2.8.dp).toPx() }
            val cycles = if (isAstra) 16 else 12
            launch {
                impactShakeY.snapTo(-kickAmp)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in cycles downTo 1) {
                    val decay = cycle / cycles.toFloat()
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeY.animateTo(thrustAmp * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
            launch {
                impactShakeX.snapTo(kickAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in cycles downTo 1) {
                    val decay = cycle / cycles.toFloat()
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeX.animateTo(thrustAmp * 0.6f * decay * sign, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 55, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_combat_shield_deflect") {
            val hitAmp = with(density) { 16.dp.toPx() }
            val shudderAmp = with(density) { 4.2.dp.toPx() }
            launch {
                impactShakeX.snapTo(-hitAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(shudderAmp * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(hitAmp * 0.6f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                for (cycle in 7 downTo 1) {
                    val decay = cycle / 7f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(shudderAmp * 0.55f * decay * sign, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 48, easing = LinearEasing))
                }
            }
        } else if (cue == "sfx_crash_impact") {
            val crashAmp = with(density) { 20.dp.toPx() }
            val aftershockAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(crashAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) 1f else -1f
                    impactShakeX.animateTo(aftershockAmp * decay * sign, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                    impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                }
            }
            launch {
                impactShakeY.snapTo(-crashAmp * 0.65f)
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                for (cycle in 10 downTo 1) {
                    val decay = cycle / 10f
                    val sign = if (cycle % 2 == 0) -1f else 1f
                    impactShakeY.animateTo(aftershockAmp * 0.6f * decay * sign, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                    impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 65, easing = LinearEasing))
                }
            }
        }
        when (cue) {
            "sfx_intro_beast_strike" -> launch {
                impactFlash.snapTo(0.7f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 240, easing = LinearEasing))
            }
            "sfx_combat_plasma_arc" -> launch {
                impactFlash.snapTo(0.55f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = LinearEasing))
            }
            "wpn_orion_bowl_resonance" -> launch {
                impactFlash.snapTo(0.35f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = LinearEasing))
            }
            "sfx_combat_shield_deflect" -> launch {
                impactFlash.snapTo(0.85f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 300, easing = LinearEasing))
            }
            "sfx_crash_impact" -> launch {
                impactFlash.snapTo(0.9f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 340, easing = LinearEasing))
            }
        }
    }

    SideEffect {
        lastRenderedFrame.value = RenderedCinematicFrame(
            imagePath = state.step.imagePath,
            scale = scale,
            translationX = driftX + impactShakeX.value,
            translationY = driftY + impactShakeY.value,
            alpha = contentAlpha
        )
    }
    val isIntroPrologue = state.sceneId == "intro_prologue"
    // Illustrated cinematics play on their authored step durations so timed scores and SFX
    // stay locked to the visuals; players can always use HOLD TO SKIP.
    val cinematicInteractionModifier = Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(cinematicInteractionModifier)
    ) {
        // Outgoing frame, frozen where the previous step left it. Drawn only while the
        // dissolve is running, and skipped entirely on a cut.
        if (outgoing != null && outgoing.alpha > 0.001f && enter.value < 1f && !outgoing.imagePath.isNullOrBlank()) {
            val outgoingPainter = rememberAssetPainter(
                outgoing.imagePath,
                fallback = ColorPainter(Color.Black),
                async = false
            )
            DesktopPortraitBackdrop(outgoingPainter, null,
                modifier = Modifier.graphicsLayer { alpha = outgoing.alpha * dimAlpha },
                extensionScale = outgoing.scale,
                extensionTranslationX = outgoing.translationX,
                extensionTranslationY = outgoing.translationY, animateCenter = true)
        }
        DesktopPortraitBackdrop(painter, null,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            extensionScale = scale,
            extensionTranslationX = driftX + impactShakeX.value,
            extensionTranslationY = driftY + impactShakeY.value, animateCenter = true)

        // Impact flash on violent beast strike or electrical surge
        if (impactFlash.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = impactFlash.value }
                    .background(impactFlashColor)
            )
        }

        // Atmospheric Layers 3–13 synced from Android (scaled responsively to widescreen viewport)
        val imagePath = state.step.imagePath.orEmpty()
        when {
            "cutter_surge_arc" in imagePath -> CinematicPulsingRadialGlow(
                label = "cutter_arc_flicker", minAlpha = 0.06f, maxAlpha = 0.26f, durationMillis = 140,
                contentAlpha = contentAlpha, normX = 0.48f, normY = 0.81f, radiusFraction = 0.55f,
                primary = Color(0xFF80F8FF).copy(alpha = 0.72f), secondary = Color(0xFFFFB300).copy(alpha = 0.28f)
            )
            "cutter_surge_steady" in imagePath -> CinematicPulsingRadialGlow(
                label = "cutter_aftermath_glow", minAlpha = 0.06f, maxAlpha = 0.24f, durationMillis = 720,
                contentAlpha = contentAlpha, normX = 0.54f, normY = 0.81f, radiusFraction = 0.46f,
                primary = Color(0xFF40E8FF).copy(alpha = 0.65f), secondary = Color(0xFF00B8D4).copy(alpha = 0.20f)
            )
            "relic_sync_discharge" in imagePath -> CinematicPulsingRadialGlow(
                label = "relic_discharge_glow", minAlpha = 0.08f, maxAlpha = 0.28f, durationMillis = 190,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.90f, radiusFraction = 0.55f,
                primary = Color(0xFFFFB74D).copy(alpha = 0.65f), secondary = Color(0xFF40E8FF).copy(alpha = 0.30f)
            )
            "relic_sync_fork" in imagePath -> CinematicPulsingRadialGlow(
                label = "relic_fork_halo", minAlpha = 0.10f, maxAlpha = 0.26f, durationMillis = 1600,
                contentAlpha = contentAlpha, normX = 0.56f, normY = 0.36f, radiusFraction = 0.48f,
                primary = Color(0xFF40E8FF).copy(alpha = 0.60f), secondary = Color(0xFF00B8D4).copy(alpha = 0.20f)
            )
            "pod_shield_impact" in imagePath -> CinematicPulsingRadialGlow(
                label = "pod_shield_glare", minAlpha = 0.08f, maxAlpha = 0.28f, durationMillis = 260,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.28f, radiusFraction = 0.62f,
                primary = Color(0xFFFFE5E5).copy(alpha = 0.65f), secondary = Color(0xFFFF2A2A).copy(alpha = 0.32f)
            )
            "pod_brace_storm" in imagePath -> CinematicPulsingRadialGlow(
                label = "pod_storm_alarm", minAlpha = 0.05f, maxAlpha = 0.22f, durationMillis = 520,
                contentAlpha = contentAlpha, normX = 0.28f, normY = 0.12f, radiusFraction = 0.44f,
                primary = Color(0xFFFF1744).copy(alpha = 0.65f), secondary = Color(0xFFD50000).copy(alpha = 0.20f)
            )
            "anchor_drill_buckle" in imagePath -> CinematicPulsingRadialGlow(
                label = "anchor_buckle_glow", minAlpha = 0.08f, maxAlpha = 0.28f, durationMillis = 180,
                contentAlpha = contentAlpha, normX = 0.48f, normY = 0.67f, radiusFraction = 0.50f,
                primary = Color(0xFFFFD54F).copy(alpha = 0.65f), secondary = Color(0xFF40E8FF).copy(alpha = 0.30f)
            )
            "anchor_drill_circuit" in imagePath -> CinematicPulsingRadialGlow(
                label = "anchor_circuit_halo", minAlpha = 0.08f, maxAlpha = 0.22f, durationMillis = 1400,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.67f, radiusFraction = 0.56f,
                primary = Color(0xFF40E8FF).copy(alpha = 0.55f), secondary = Color(0xFF00B8D4).copy(alpha = 0.18f)
            )
            "w2_orion_stasis_unseal" in imagePath -> CinematicPulsingRadialGlow(
                label = "stasis_unseal_glow", minAlpha = 0.08f, maxAlpha = 0.22f, durationMillis = 1600,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.40f, radiusFraction = 0.54f,
                primary = Color(0xFFB3F7FF).copy(alpha = 0.50f), secondary = Color(0xFFFFD54F).copy(alpha = 0.20f)
            )
            "w3_lens_vault_claim" in imagePath -> CinematicPulsingRadialGlow(
                label = "lens_prism_glow", minAlpha = 0.08f, maxAlpha = 0.24f, durationMillis = 1500,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.28f, radiusFraction = 0.52f,
                primary = Color(0xFFFFE57F).copy(alpha = 0.55f), secondary = Color(0xFF40E8FF).copy(alpha = 0.30f)
            )
            "w3_spire_burn_notice" in imagePath -> CinematicPulsingRadialGlow(
                label = "burn_notice_strobe", minAlpha = 0.06f, maxAlpha = 0.24f, durationMillis = 480,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.31f, radiusFraction = 0.58f,
                primary = Color(0xFFFF1744).copy(alpha = 0.55f), secondary = Color(0xFFD50000).copy(alpha = 0.20f)
            )
            "w3_foundry_descent" in imagePath -> CinematicPulsingRadialGlow(
                label = "foundry_magma_glow", minAlpha = 0.08f, maxAlpha = 0.20f, durationMillis = 1800,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.67f, radiusFraction = 0.60f,
                primary = Color(0xFFFF6D00).copy(alpha = 0.52f), secondary = Color(0xFFFF3D00).copy(alpha = 0.20f)
            )
            "w4_anvil_construct" in imagePath -> CinematicPulsingRadialGlow(
                label = "anvil_construct_glow", minAlpha = 0.08f, maxAlpha = 0.24f, durationMillis = 1350,
                contentAlpha = contentAlpha, normX = 0.49f, normY = 0.53f, radiusFraction = 0.52f,
                primary = Color(0xFFFFD740).copy(alpha = 0.56f), secondary = Color(0xFF40E8FF).copy(alpha = 0.26f)
            )
            "w4_rylos_spared" in imagePath -> CinematicPulsingRadialGlow(
                label = "rylos_dock_embers", minAlpha = 0.06f, maxAlpha = 0.18f, durationMillis = 1900,
                contentAlpha = contentAlpha, normX = 0.47f, normY = 0.62f, radiusFraction = 0.56f,
                primary = Color(0xFFFF6D00).copy(alpha = 0.45f), secondary = Color(0xFF40E8FF).copy(alpha = 0.16f)
            )
            "w4_outer_shield_breach" in imagePath -> CinematicPulsingRadialGlow(
                label = "outer_shield_breach_glow", minAlpha = 0.08f, maxAlpha = 0.26f, durationMillis = 680,
                contentAlpha = contentAlpha, normX = 0.52f, normY = 0.34f, radiusFraction = 0.54f,
                primary = Color(0xFF80F8FF).copy(alpha = 0.56f), secondary = Color(0xFFFFD54F).copy(alpha = 0.28f)
            )
            "w5_stasis_lunge" in imagePath -> CinematicPulsingRadialGlow(
                label = "w5_stasis_lock_glow", minAlpha = 0.08f, maxAlpha = 0.24f, durationMillis = 1200,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.36f, radiusFraction = 0.54f,
                primary = Color(0xFF80F8FF).copy(alpha = 0.56f), secondary = Color(0xFF2979FF).copy(alpha = 0.22f)
            )
            "w5_vale_soloist_tear" in imagePath || "w5_tear_hands" in imagePath -> CinematicPulsingRadialGlow(
                label = "w5_tear_rift_glow", minAlpha = 0.08f, maxAlpha = 0.24f, durationMillis = 1100,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.34f, radiusFraction = 0.56f,
                primary = Color(0xFFB388FF).copy(alpha = 0.54f), secondary = Color(0xFF40E8FF).copy(alpha = 0.28f)
            )
            "w6_campfire_reunion" in imagePath || "w5_tear_nova_wakes" in imagePath -> CinematicPulsingRadialGlow(
                label = "w6_source_fire_glow", minAlpha = 0.06f, maxAlpha = 0.18f, durationMillis = 1500,
                contentAlpha = contentAlpha, normX = 0.49f, normY = 0.60f, radiusFraction = 0.50f,
                primary = Color(0xFF40E8FF).copy(alpha = 0.52f), secondary = Color(0xFF00B8D4).copy(alpha = 0.20f)
            )
            "w6_silence_rises" in imagePath -> CinematicPulsingRadialGlow(
                label = "w6_silence_radiance", minAlpha = 0.08f, maxAlpha = 0.26f, durationMillis = 1200,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.28f, radiusFraction = 0.58f,
                primary = Color(0xFFFFFFFF).copy(alpha = 0.48f), secondary = Color(0xFFB388FF).copy(alpha = 0.30f)
            )
        }
        if (state.sceneId == "scene_w2_astra_departure" && state.stepIndex in 3..6) {
            CinematicPulsingRadialGlow(
                label = "spire_interference_pulse", minAlpha = 0.04f, maxAlpha = 0.20f, durationMillis = 440,
                contentAlpha = contentAlpha, normX = 0.50f, normY = 0.24f, radiusFraction = 0.52f,
                primary = Color(0xFFFF1744).copy(alpha = 0.58f), secondary = Color(0xFFD50000).copy(alpha = 0.18f)
            )
        }

        // Atmospheric Layer 1: Emergency breach alarm strobe & telemetry scanlines
        val isEmergency = state.step.captionStyle == CinematicCaptionStyle.SYSTEM ||
            (state.step.imagePath?.contains("breach") == true &&
                state.step.captionStyle != CinematicCaptionStyle.LOCATION)
        if (isEmergency) {
            val strobeTransition = rememberInfiniteTransition(label = "emergency_strobe")
            val strobeAlpha by strobeTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "strobe_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = strobeAlpha * contentAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3D00).copy(alpha = 0.55f),
                                Color(0xFFFF5722).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 900f
                        )
                    )
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.15f * contentAlpha }
            ) {
                val stepPx = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += stepPx
                }
            }
        }

        // Atmospheric Layer 2: Cryogenic stasis chamber mist & frost creep
        val isStasisPod = state.step.imagePath?.contains("stasis") == true
        if (isStasisPod) {
            val mistTransition = rememberInfiniteTransition(label = "stasis_mist")
            val mistPulse by mistTransition.animateFloat(
                initialValue = 0.18f,
                targetValue = 0.38f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mist_pulse"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = mistPulse * contentAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.22f),
                                Color(0xFF4DD0E1).copy(alpha = 0.16f),
                                Color(0xFF00E5FF).copy(alpha = 0.38f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            val isInitialStasisDialogue = state.step.captionStyle == CinematicCaptionStyle.DIALOGUE ||
                !state.step.speaker.isNullOrBlank()
            val frostAlpha = if (isInitialStasisDialogue) {
                (progress * 0.45f).coerceIn(0f, 0.45f)
            } else {
                0.45f
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = frostAlpha * contentAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE0F7FA).copy(alpha = 0.65f),
                                Color(0xFFB2EBF2).copy(alpha = 0.40f),
                                Color(0xFF80DEEA).copy(alpha = 0.35f),
                                Color(0xFFB2EBF2).copy(alpha = 0.55f),
                                Color(0xFFE0F7FA).copy(alpha = 0.75f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        if (state.step.captionStyle != CinematicCaptionStyle.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isStasisPod) Color.Transparent else Color.Black.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }

        if (state.step.captionStyle != CinematicCaptionStyle.NONE) {
            val captionEntrance = remember(stepKey) { CoreAnimatable(if (isIntroPrologue) 1f else 0f) }
            LaunchedEffect(stepKey) {
                captionEntrance.animateTo(1f, tween(durationMillis = 150))
            }
            DesktopIllustratedCinematicCaption(
                state = state,
                revealAllRequest = revealAllRequest,
                onRevealFinished = { captionRevealFinished = true },
                onAdvance = requestAdvance,
                services = services,
                modifier = Modifier
                    .graphicsLayer { alpha = captionEntrance.value * dimAlpha }
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, bottom = 54.dp)
            )
        }

        if (state.skippable) {
            Surface(
                color = Color.Black.copy(alpha = 0.52f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(18.dp)
                    .semantics {
                        contentDescription = if (state.sceneId == "intro_prologue") "Skip intro" else "Skip cinematic"
                        onClick(label = "Skip cinematic") {
                            onSkip()
                            true
                        }
                    }
                    .pointerInput(state.sceneId) {
                        detectTapGestures(onLongPress = { onSkip() })
                    }
            ) {
                Text(
                    "HOLD TO SKIP",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CinematicPulsingRadialGlow(
    label: String,
    minAlpha: Float,
    maxAlpha: Float,
    durationMillis: Int,
    contentAlpha: Float,
    normX: Float,
    normY: Float,
    radiusFraction: Float,
    primary: Color,
    secondary: Color
) {
    val transition = rememberInfiniteTransition(label = label)
    val pulse by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "${label}_alpha"
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pulse * contentAlpha }
    ) {
        val radius = maxOf(size.width, size.height) * radiusFraction
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primary, secondary, Color.Transparent),
                center = Offset(size.width * normX, size.height * normY),
                radius = radius.coerceAtLeast(1f)
            )
        )
    }
}

@Composable
private fun DesktopIllustratedCinematicCaption(
    state: CinematicUiState,
    revealAllRequest: Int,
    onRevealFinished: () -> Unit,
    onAdvance: () -> Unit,
    services: DesktopAppServices,
    modifier: Modifier = Modifier
) {
    if (state.step.captionStyle == CinematicCaptionStyle.DIALOGUE) {
        val speaker = state.step.speaker.orEmpty()
        DesktopAuthoredDialogueOverlay(
            services = services,
            dialogue = DialogueUi(
                line = DialogueLine(
                    id = "${state.sceneId}_${state.stepIndex}",
                    speaker = speaker,
                    text = state.step.text,
                    portrait = state.step.portrait,
                    voiceCue = null
                ),
                portrait = state.step.portrait,
                voiceCue = null
            ),
            choices = emptyList(),
            onAdvance = {},
            onChoice = {},
            onPlayVoice = {},
            canDismissByTap = false,
            onPlayMurmur = { cue ->
                services.audioDriver.executeAll(
                    listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L))
                )
            },
            onPlayMurmurWithPitch = { cue, pitch ->
                services.audioDriver.executeAll(
                    listOf(AudioCommand.Play(AudioCueType.VOICE, cue, loop = false, fadeMs = 0L, pitch = pitch))
                )
            },
            onRevealFinished = onRevealFinished,
            revealAllRequest = revealAllRequest,
            modifier = modifier.widthIn(max = 920.dp)
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.NARRATION) {
        IllustratedNarrationCaption(
            text = state.step.text,
            stepKey = "${state.sceneId}_${state.stepIndex}",
            revealAllRequest = revealAllRequest,
            onRevealFinished = onRevealFinished,
            isLastStep = state.stepIndex + 1 >= state.stepCount,
            showTapToContinue = false,
            modifier = modifier
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.LOCATION) {
        IllustratedLocationPlate(
            text = state.step.text,
            modifier = modifier
        )
        return
    }

    if (state.step.captionStyle == CinematicCaptionStyle.SYSTEM) {
        val alert = Color(0xFFFFA45B)
        Surface(
            modifier = modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            color = Color(0xF2070B10),
            contentColor = Color.White,
            border = BorderStroke(1.dp, alert.copy(alpha = 0.72f)),
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 15.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(alert)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.step.speaker?.takeIf { it.isNotBlank() }?.let { speaker ->
                        Text(
                            text = speaker.uppercase(Locale.getDefault()),
                            color = alert,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                    Text(
                        text = state.step.text,
                        color = Color.White.copy(alpha = 0.96f),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
            }
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.step.speaker?.takeIf { it.isNotBlank() }?.let { speaker ->
            Text(
                text = speaker.uppercase(),
                color = Color(0xFF7BE8FF),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            )
        }
        Text(
            text = state.step.text,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun IllustratedLocationPlate(
    text: String,
    modifier: Modifier = Modifier
) {
    val parts = remember(text) { text.split("//", limit = 2).map(String::trim) }
    val location = parts.firstOrNull().orEmpty()
    val time = parts.getOrNull(1).orEmpty()
    val accent = Color(0xFF7BE8FF)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xE8060B14),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.52f)),
            shadowElevation = 14.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = location,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, accent.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                )
                if (time.isNotBlank()) {
                    Text(
                        text = time,
                        color = accent.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IllustratedNarrationCaption(
    text: String,
    stepKey: String,
    revealAllRequest: Int,
    onRevealFinished: () -> Unit,
    isLastStep: Boolean,
    showTapToContinue: Boolean = true,
    modifier: Modifier = Modifier
) {
    var revealedCount by remember(stepKey) { mutableIntStateOf(0) }
    val revealFinished = revealedCount >= text.length
    val displayedText = text.take(revealedCount.coerceIn(0, text.length))

    LaunchedEffect(stepKey, text) {
        revealedCount = 0
        if (text.isBlank()) {
            revealedCount = text.length
            return@LaunchedEffect
        }
        for (index in text.indices) {
            if (revealedCount >= text.length) return@LaunchedEffect
            revealedCount = index + 1
            delay(12L)
        }
    }
    LaunchedEffect(stepKey, revealAllRequest) {
        if (revealAllRequest > 0) revealedCount = text.length
    }
    LaunchedEffect(stepKey, revealFinished) {
        if (revealFinished) onRevealFinished()
    }

    CinematicNarrationCard(
        fullText = text,
        displayedText = displayedText,
        revealFinished = revealFinished,
        isLastStep = isLastStep,
        showTapToContinue = showTapToContinue,
        modifier = modifier
    )
}



@Composable
private fun CinematicNarrationCard(
    fullText: String,
    displayedText: String,
    revealFinished: Boolean,
    isLastStep: Boolean,
    showTapToContinue: Boolean = true,
    modifier: Modifier = Modifier
) {
    val glowAlpha by rememberInfiniteTransition(label = "cinematicGlow").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinematicGlowPulse"
    )
    val accentColor = Color(0xFF7BE8FF)
    val cardShape = RoundedCornerShape(28.dp)
    val narrationStyle = MaterialTheme.typography.bodyLarge.copy(
        fontStyle = FontStyle.Italic,
        lineHeight = 28.sp,
        letterSpacing = 0.3.sp
    )

    Box(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(0.88f)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = glowAlpha }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                        radius = 400f
                    ),
                    shape = cardShape
                )
        )
        Surface(
            color = Color(0xFF060B14).copy(alpha = 0.96f),
            shape = cardShape,
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.12f),
                        accentColor.copy(alpha = 0.30f)
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CinematicAccentBar(accentColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fullText,
                        color = Color.Transparent,
                        style = narrationStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics { }
                    )
                    Text(
                        text = typewriterText(fullText, displayedText.length),
                        color = Color.White.copy(alpha = 0.92f),
                        style = narrationStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CinematicAccentBar(accentColor)
            }
        }
    }
}

@Composable
private fun CinematicAccentBar(accentColor: Color) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0f),
                        accentColor.copy(alpha = 0.7f),
                        accentColor.copy(alpha = 0f)
                    )
                )
            )
    )
}

