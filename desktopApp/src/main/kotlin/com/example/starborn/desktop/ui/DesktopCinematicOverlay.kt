package com.example.starborn.desktop.ui

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
            LaunchedEffect(state.sceneId, state.stepIndex) {
                state.step.audioCue?.let { services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.UI, it))) }
                state.step.voiceCue?.let { services.audioDriver.executeAll(listOf(AudioCommand.Play(AudioCueType.VOICE, it))) }
                state.step.durationSeconds?.takeIf { it > 0 }?.let { delay((it * 1000).toLong()); onAdvance() }
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (state.backdrop == CinematicBackdrop.BLACK) 1f else .72f)), contentAlignment = Alignment.BottomCenter) {
                Surface(Modifier.padding(32.dp).widthIn(max = 960.dp).fillMaxWidth(), color = Color(0xF007111A), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(24.dp)) {
                        state.step.speaker?.let { Text(it, color = Color(0xFF7BE8FF)) }
                        Text(state.step.text, color = Color.White)
                        Row { TextButton(onClick = onAdvance) { Text("Continue") }; if (state.skippable) TextButton(onClick = onSkip) { Text("Skip") } }
                    }
                }
            }
        }
        }
    }
}
/** The last frame drawn by [IllustratedCinematicOverlay], held so the next step can dissolve over it. */
private data class RenderedCinematicFrame(
    val imagePath: String?,
    val scale: Float,
    val translationX: Float,
    val translationY: Float
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
    // Survives step changes on purpose: it is the outgoing frame for the next dissolve.
    val lastRenderedFrame = remember { mutableStateOf<RenderedCinematicFrame?>(null) }
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
            onAdvance()
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
    val rawScale = cinematicLerp(
        state.step.cameraStartScale?.toFloat() ?: legacyStartScale,
        state.step.cameraEndScale?.toFloat() ?: legacyEndScale,
        progress
    )
    val scale = rawScale.coerceAtLeast(1.06f)
    val density = LocalDensity.current
    val startX = with(density) { (state.step.cameraStartX ?: 0.0).toFloat().dp.toPx() }
    val endX = with(density) { (state.step.cameraEndX?.toFloat() ?: legacyEndX).dp.toPx() }
    val startY = with(density) { (state.step.cameraStartY ?: 0.0).toFloat().dp.toPx() }
    val endY = with(density) { (state.step.cameraEndY ?: 0.0).toFloat().dp.toPx() }
    val driftX = cinematicLerp(startX, endX, progress)
    val driftY = cinematicLerp(startY, endY, progress)
    // Cross-dissolve, not a re-fade. Previously the frame was a single Image whose alpha came
    // from an animateFloatAsState that was NOT keyed to the step, so on a step change the target
    // dipped to 0 for one frame and recovered before the eased tween had moved: every authored
    // "fade" rendered as a hard cut with a one-frame flicker, and the whole cold open played as a
    // slideshow. The outgoing frame is now held underneath, frozen at its end transform, and the
    // incoming frame dissolves over it.
    val transitionMs = if (state.step.transition == CinematicTransition.CUT) 0 else 520
    val outgoing = remember(stepKey) { lastRenderedFrame.value }
    val enter = remember(stepKey) { CoreAnimatable(if (transitionMs == 0) 1f else 0f) }
    LaunchedEffect(stepKey) {
        if (transitionMs > 0) {
            enter.animateTo(1f, animationSpec = tween(durationMillis = transitionMs, easing = FastOutSlowInEasing))
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
            // 1. Initial high-energy violent transient slam
            launch {
                impactShakeX.snapTo(initialAmp)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
                // 2. Residual structural tremor decaying alongside the acoustic reverberation tail
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
            // Clean pneumatic tube launch impulse without repetitive dragging conduit shakes
            val launchAmp = with(density) { 5.dp.toPx() }
            launch {
                impactShakeX.snapTo(launchAmp)
                impactShakeY.snapTo(-launchAmp * 0.4f)
                impactShakeX.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                impactShakeY.animateTo(0f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
        }
        if (cue == "sfx_intro_beast_strike") {
            launch {
                impactFlash.snapTo(0.7f)
                impactFlash.animateTo(0f, animationSpec = tween(durationMillis = 240, easing = LinearEasing))
            }
        }
    }

    SideEffect {
        lastRenderedFrame.value = RenderedCinematicFrame(
            imagePath = state.step.imagePath,
            scale = (state.step.cameraEndScale?.toFloat() ?: legacyEndScale).coerceAtLeast(1.06f),
            translationX = endX,
            translationY = endY
        )
    }
    val advanceOrReveal: () -> Unit = {
        if (waitsForTextReveal && !captionRevealFinished) {
            revealAllRequest += 1
        } else {
            onAdvance()
        }
    }
    val isIntroPrologue = state.sceneId == "intro_prologue"
    val cinematicTapInteraction = remember(stepKey) { MutableInteractionSource() }
    val cinematicInteractionModifier = if (isIntroPrologue || state.step.captionStyle == CinematicCaptionStyle.DIALOGUE) {
        Modifier
    } else {
        Modifier
            .semantics {
                contentDescription = buildString {
                    state.step.speaker?.takeIf { it.isNotBlank() }?.let { append("$it. ") }
                    append(state.step.text)
                }
                onClick(label = "Advance cinematic") {
                    advanceOrReveal()
                    true
                }
            }
            .desktopPointerHover()
            .clickable(
                interactionSource = cinematicTapInteraction,
                indication = null,
                onClick = advanceOrReveal
            )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(cinematicInteractionModifier)
    ) {
        // Outgoing frame, frozen where the previous step left it. Drawn only while the
        // dissolve is running, and skipped entirely on a cut.
        if (outgoing != null && contentAlpha < 1f && !outgoing.imagePath.isNullOrBlank()) {
            val outgoingPainter = rememberAssetPainter(
                outgoing.imagePath,
                fallback = ColorPainter(Color.Black),
                async = false
            )
            DesktopPortraitBackdrop(outgoingPainter, null,
                modifier = Modifier.graphicsLayer { alpha = (1f - enter.value) * dimAlpha },
                extensionScale = outgoing.scale,
                extensionTranslationX = outgoing.translationX,
                extensionTranslationY = outgoing.translationY, animateCenter = true)
        }
        DesktopPortraitBackdrop(painter, null,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            extensionScale = scale,
            extensionTranslationX = driftX + impactShakeX.value,
            extensionTranslationY = driftY + impactShakeY.value, animateCenter = true)

        // Impact flash on violent beast strike
        if (impactFlash.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = impactFlash.value }
                    .background(Color.White)
            )
        }

        // Atmospheric Layer 1: Emergency breach alarm strobe & telemetry scanlines
        val isEmergency = state.step.captionStyle == CinematicCaptionStyle.SYSTEM ||
            state.step.imagePath?.contains("breach") == true
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
            // Frost creeping in across the entire viewport during stasis freeze on dialogue step,
            // then holding steady when the pod is sealed so it doesn't jarringly clear and re-freeze.
            val isInitialStasisDialogue = state.step.captionStyle == CinematicCaptionStyle.DIALOGUE ||
                !state.step.speaker.isNullOrBlank()
            val frostAlpha = if (isInitialStasisDialogue) {
                (progress * 0.45f).coerceIn(0f, 0.45f)
            } else {
                0.45f
            }
            // Cryogenic fog and frost covering the entire screen seamlessly from top to bottom edge
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
            DesktopIllustratedCinematicCaption(
                state = state,
                revealAllRequest = revealAllRequest,
                onRevealFinished = { captionRevealFinished = true },
                onAdvance = onAdvance,
                services = services,
                modifier = Modifier
                    // Only follows the frame on the way out; captions keep their own
                    // reveal animation on the way in.
                    .graphicsLayer { alpha = if (contentDimmed) contentAlpha else 1f }
                    .align(Alignment.BottomStart)
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
                        contentDescription = "Skip intro"
                        onClick(label = "Skip intro") {
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
        val isIntro = state.sceneId == "intro_prologue"
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
            onAdvance = if (isIntro) ({}) else onAdvance,
            onChoice = { onAdvance() },
            onPlayVoice = {},
            canDismissByTap = !isIntro,
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
            modifier = modifier
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
            showTapToContinue = state.sceneId != "intro_prologue",
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
                .fillMaxWidth()
                .widthIn(max = 720.dp),
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
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "cinematicCardAlpha"
    )
    val glowAlpha by rememberInfiniteTransition(label = "cinematicGlow").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinematicGlowPulse"
    )
    val hintAlpha by animateFloatAsState(
        targetValue = if (revealFinished) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "cinematicHintAlpha"
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
            .fillMaxWidth(0.88f)
            .graphicsLayer {
                alpha = cardAlpha
                translationY = (1f - cardAlpha) * 48f
            }
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
                        text = displayedText,
                        color = Color.White.copy(alpha = 0.92f),
                        style = narrationStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showTapToContinue) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(hintAlpha)
                            .then(if (revealFinished) Modifier else Modifier.clearAndSetSemantics { }),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦  ",
                            color = accentColor.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (isLastStep) "Click or Enter to continue" else "Click or Enter to continue ▸",
                            color = accentColor.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp)
                        )
                    }
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

