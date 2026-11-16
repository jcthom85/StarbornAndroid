package com.example.starborn.ui.fishing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.feature.fishing.viewmodel.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

val FishingInk = Color(0xFF081B25)
val FishingGold = Color(0xFFF2CF85)
val FishingMist = Color(0xFFEAF6F8)
val FishingCoral = Color(0xFFFFA38D)
val FishingButtonSurface = Color(0xFF1B4B5A)
val FishingButtonHighlight = Color(0xFF266272)

@Composable
fun FishingTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme.copy(
        background = FishingInk,
        surface = FishingInk,
        surfaceVariant = Color(0xFF15313B),
        onSurface = FishingMist,
        onSurfaceVariant = Color(0xFFBAD3D8),
        primary = FishingButtonHighlight,
        onPrimary = FishingMist,
        primaryContainer = Color(0xFF183B47),
        onPrimaryContainer = FishingMist,
        secondary = Color(0xFF8ED9D3),
        onSecondary = FishingMist,
        tertiary = FishingGold,
        onTertiary = FishingMist,
        error = FishingCoral,
        onError = FishingMist,
        outline = Color(0xFF66858D)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

data class FishingWaterBiomeStyle(
    val horizonGlow: Color,
    val shallowWater: Color,
    val midWater: Color,
    val deepWater: Color,
    val causticTint: Color,
    val bankColor: Color,
    val bankRimTint: Color,
    val moteColor: Color,
    val fishCoreTint: Color,
    val fishFinTint: Color
)

fun biomeStyleForZone(zoneId: String?): FishingWaterBiomeStyle = when (zoneId?.lowercase()) {
    "colony_pit_drain" -> FishingWaterBiomeStyle(
        horizonGlow = Color(0xFF3E2D1E),
        shallowWater = Color(0xFF212C31),
        midWater = Color(0xFF12191E),
        deepWater = Color(0xFF080D10),
        causticTint = Color(0xFFFFB74D),
        bankColor = Color(0xFF171E22),
        bankRimTint = Color(0xFFFFAB40),
        moteColor = Color(0xFFFFCC80),
        fishCoreTint = Color(0xFFCFE8E2),
        fishFinTint = Color(0xFF88B8B0)
    )
    "spire_runoff" -> FishingWaterBiomeStyle(
        horizonGlow = Color(0xFF2A1842),
        shallowWater = Color(0xFF18203C),
        midWater = Color(0xFF0E1326),
        deepWater = Color(0xFF060813),
        causticTint = Color(0xFFEA80FC),
        bankColor = Color(0xFF10152B),
        bankRimTint = Color(0xFF80D8FF),
        moteColor = Color(0xFFCE93D8),
        fishCoreTint = Color(0xFFD1C4E9),
        fishFinTint = Color(0xFF80D8FF)
    )
    "foundry_cooling_runoff" -> FishingWaterBiomeStyle(
        horizonGlow = Color(0xFF3D2219),
        shallowWater = Color(0xFF1E2D3A),
        midWater = Color(0xFF131B24),
        deepWater = Color(0xFF090D12),
        causticTint = Color(0xFFFF8A65),
        bankColor = Color(0xFF181F26),
        bankRimTint = Color(0xFFFF6E40),
        moteColor = Color(0xFFFFAB91),
        fishCoreTint = Color(0xFFDCEDC8),
        fishFinTint = Color(0xFF80CBC4)
    )
    "orbital_false_tide" -> FishingWaterBiomeStyle(
        horizonGlow = Color(0xFF1A4658),
        shallowWater = Color(0xFF13455C),
        midWater = Color(0xFF0B2B3B),
        deepWater = Color(0xFF05151E),
        causticTint = Color(0xFFFFE57F),
        bankColor = Color(0xFF0F2D3A),
        bankRimTint = Color(0xFF84FFFF),
        moteColor = Color(0xFFFFF59D),
        fishCoreTint = Color(0xFFE0F7FA),
        fishFinTint = Color(0xFF64FFDA)
    )
    "singularity_ether_well" -> FishingWaterBiomeStyle(
        horizonGlow = Color(0xFF2E1A52),
        shallowWater = Color(0xFF221442),
        midWater = Color(0xFF120926),
        deepWater = Color(0xFF070312),
        causticTint = Color(0xFFB388FF),
        bankColor = Color(0xFF160D2E),
        bankRimTint = Color(0xFFFFD54F),
        moteColor = Color(0xFFEA80FC),
        fishCoreTint = Color(0xFFEDE7F6),
        fishFinTint = Color(0xFFB388FF)
    )
    else -> FishingWaterBiomeStyle(
        // Starflower Retention Pond / Sector 9 Canopy Tide Pools default
        horizonGlow = Color(0xFF134852),
        shallowWater = Color(0xFF0E3942),
        midWater = Color(0xFF08232B),
        deepWater = Color(0xFF041217),
        causticTint = Color(0xFF64FFDA),
        bankColor = Color(0xFF0B282E),
        bankRimTint = Color(0xFF80CBC4),
        moteColor = Color(0xFFA7FFEB),
        fishCoreTint = Color(0xFFC8EFE6),
        fishFinTint = Color(0xFF7AD9C9)
    )
}

/**
 * Living multi-biome water viewport shared by Android portrait and Desktop fishing screens.
 * Driven deterministically from controller tick state with smooth tween interpolation so
 * Compose UI tests never block on infinite transitions.
 */
@Composable
fun FishingWaterScene(
    state: FishingUiState,
    onHook: () -> Unit,
    modifier: Modifier = Modifier,
    showHeaderText: Boolean = true,
    showTelemetryFooter: Boolean = true
) {
    val cue = fishingCue(state)
    val currentHook by rememberUpdatedState(onHook)
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val biome = remember(state.currentZone?.id) { biomeStyleForZone(state.currentZone?.id) }

    val rawProgress = state.reelState?.progress ?: 0f
    val progress by animateFloatAsState(rawProgress, tween(110), label = "fishDistance")
    val rawTension = (state.reelState?.tension ?: 0f).coerceIn(0f, 1f)
    val tension by animateFloatAsState(rawTension, tween(110), label = "lineTension")

    // Deterministic elapsed time in seconds derived from active phase state, interpolated smoothly.
    val rawPhaseSeconds = when (state.fishingState) {
        FishingState.WAITING -> (state.waitingState?.elapsedMs ?: 0L) / 1000f
        FishingState.HOOKSET -> (2400L - (state.hookState?.timeRemainingMs ?: 2400L)).coerceAtLeast(0L) / 1000f
        FishingState.REELING, FishingState.READY -> {
            val explicit = state.reelState?.elapsedMs ?: 0L
            if (explicit > 0L) explicit / 1000f
            else (rawProgress * 12f + rawTension * 4f)
        }
        FishingState.RESULT -> 2.4f
        FishingState.SETUP -> 0.8f
    }
    val timeSec by animateFloatAsState(rawPhaseSeconds, tween(120), label = "waterTimeSec")

    val fighting = state.fishingState == FishingState.REELING || state.fishingState == FishingState.READY
    val phase = state.reelState?.phase ?: FishingFightPhase.CALM
    val surge = phase == FishingFightPhase.SURGE
    val warning = phase == FishingFightPhase.WARNING
    val pull = phase == FishingFightPhase.PULL
    val fightStyle = state.reelState?.behavior?.fightStyle ?: "gentle"

    // Lateral weave target based on phase & fight style
    val targetFishXFraction = when {
        !fighting -> 0.48f
        surge -> 0.28f + 0.08f * sin(timeSec * 4.2f)
        warning -> 0.38f + 0.04f * sin(timeSec * 6.0f)
        pull -> 0.56f + 0.06f * sin(timeSec * 2.6f)
        else -> 0.48f + 0.05f * sin(timeSec * 1.8f)
    }
    val fishXFraction by animateFloatAsState(targetFishXFraction, tween(120), label = "fishLateralX")

    val lineColor = when {
        cue.urgent || tension >= 0.75f -> FishingCoral
        warning || tension >= 0.45f -> FishingGold
        fighting && state.reelState?.isReeling == true -> FishingGold
        else -> Color(0xFF8ED9D3)
    }

    Column(
        modifier = modifier.fillMaxSize().testTag("fishing-portrait-scene"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showHeaderText) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = cue.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (cue.urgent) FishingCoral else FishingGold
                    )
                    Text(
                        text = cue.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FishingMist
                    )
                }
                if (fighting && state.reelState?.salvage != true) {
                    val stamina = (state.reelState?.staminaRemaining ?: 1f).coerceIn(0f, 1f)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0D2630),
                        border = BorderStroke(1.dp, biome.causticTint.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "FISH STAMINA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = FishingMist.copy(alpha = 0.78f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(5) { idx ->
                                    val thresholdPip = (idx + 1) / 5f
                                    val filled = stamina >= thresholdPip - 0.1f
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (filled) biome.causticTint else Color.White.copy(alpha = 0.16f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(biome.deepWater)
                .border(
                    BorderStroke(
                        width = if (cue.urgent) 1.8.dp else 1.dp,
                        color = if (cue.urgent) FishingCoral.copy(alpha = 0.85f) else biome.bankRimTint.copy(alpha = 0.32f)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .semantics { contentDescription = "Fishing water. ${cue.title}. ${cue.instruction}" }
                .pointerInput(state.fishingState, threshold) {
                    if (state.fishingState == FishingState.HOOKSET) {
                        var delta = Offset.Zero
                        detectDragGestures(
                            onDragStart = { delta = Offset.Zero },
                            onDragCancel = { delta = Offset.Zero },
                            onDragEnd = { if (isUpwardHookSwipe(delta.x, delta.y, threshold)) currentHook() },
                            onDrag = { change, amount -> delta += amount; change.consume() }
                        )
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val minDim = min(w, h)

                // 1. Multi-stop perspective water depth gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to biome.horizonGlow,
                        0.16f to biome.shallowWater,
                        0.58f to biome.midWater,
                        1.00f to biome.deepWater
                    )
                )

                // 2. Top shoreline ledge & bank reflection rim
                val bankPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.13f)
                    quadraticTo(w * 0.68f, h * 0.17f, w * 0.34f, h * 0.12f)
                    quadraticTo(w * 0.14f, h * 0.09f, 0f, h * 0.14f)
                    close()
                }
                drawPath(bankPath, biome.bankColor.copy(alpha = 0.88f))
                drawPath(
                    bankPath,
                    biome.bankRimTint.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 3. Perspective caustic wave ribbons across the water basin
                repeat(9) { idx ->
                    val depthFrac = idx / 8f
                    // Perspective compression near top shoreline, wider in foreground
                    val yBase = h * (0.17f + depthFrac * depthFrac * 0.64f)
                    val wavePhase = timeSec * (0.9f + idx * 0.12f) + idx * 1.3f
                    val sway = sin(wavePhase) * w * 0.03f
                    val ribbonWidth = w * (0.36f + (idx % 3) * 0.16f)
                    val xStart = w * (0.06f + ((idx * 37) % 40) / 100f) + sway
                    val wavePath = Path().apply {
                        moveTo(xStart, yBase)
                        quadraticTo(
                            xStart + ribbonWidth * 0.5f,
                            yBase + sin(wavePhase + 1.1f) * (2f + depthFrac * 5f).dp.toPx(),
                            (xStart + ribbonWidth).coerceAtMost(w * 0.94f),
                            yBase
                        )
                    }
                    drawPath(
                        path = wavePath,
                        color = biome.causticTint.copy(alpha = (0.08f + (1f - depthFrac) * 0.10f)),
                        style = Stroke(width = (1f + depthFrac * 1.4f).dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Drifting bioluminescent spore / bubble / drip motes
                repeat(12) { i ->
                    val seedX = ((i * 73 + 19) % 88) / 100f + 0.06f
                    val seedY = ((i * 41 + 11) % 68) / 100f + 0.18f
                    val driftX = sin(timeSec * 0.8f + i * 1.7f) * w * 0.018f
                    val driftY = cos(timeSec * 0.6f + i * 2.3f) * h * 0.015f
                    val moteCenter = Offset(w * seedX + driftX, h * seedY + driftY)
                    val moteR = (1.5f + (i % 3) * 1.1f).dp.toPx()
                    drawCircle(biome.moteColor.copy(alpha = 0.08f), radius = moteR * 3.2f, center = moteCenter)
                    drawCircle(biome.moteColor.copy(alpha = 0.34f), radius = moteR, center = moteCenter)
                }

                // 5. Foreground shallows & landing net zone arc at bottom
                drawOval(
                    color = biome.bankColor.copy(alpha = 0.92f),
                    topLeft = Offset(-w * 0.18f, h * 0.82f),
                    size = Size(w * 1.36f, h * 0.42f)
                )
                if (fighting) {
                    // Subtle landing zone guide arc at y ~ 0.74h (where progress == 1.0 lands)
                    drawArc(
                        color = biome.causticTint.copy(alpha = if (progress >= 0.78f) 0.55f else 0.22f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(w * 0.18f, h * 0.70f),
                        size = Size(w * 0.64f, h * 0.16f),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                    )
                }

                // Determine focal point (bobber or hooked fish)
                val waitRemainingMs = (state.waitingState?.targetMs ?: 3000L) - (state.waitingState?.elapsedMs ?: 0L)
                val isNibbleWindow = state.fishingState == FishingState.WAITING && waitRemainingMs in 1..1500
                val nibblePulse = if (isNibbleWindow) sin(timeSec * 10.5f).coerceAtLeast(0f) else 0f

                val fishPos = Offset(
                    x = w * fishXFraction,
                    y = h * (0.24f + progress * 0.50f)
                )
                val floatPos = when (state.fishingState) {
                    FishingState.REELING, FishingState.READY -> fishPos
                    FishingState.HOOKSET -> Offset(w * 0.48f, h * 0.42f + 8.dp.toPx())
                    FishingState.WAITING -> Offset(
                        x = w * 0.48f + sin(timeSec * 1.6f) * 3.dp.toPx(),
                        y = h * 0.40f + cos(timeSec * 2.1f) * 2.dp.toPx() + nibblePulse * 6.dp.toPx()
                    )
                    else -> Offset(w * 0.52f, h * 0.44f)
                }

                // Subsurface glow under focal point
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (warning || surge) FishingCoral else biome.causticTint).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = floatPos,
                        radius = minDim * 0.28f
                    ),
                    radius = minDim * 0.28f,
                    center = floatPos
                )

                // 6. Subsurface circling shadow during WAITING nibble window
                if (state.fishingState == FishingState.WAITING && waitRemainingMs <= 1800L) {
                    val approach = (1f - (waitRemainingMs / 1800f).coerceIn(0f, 1f))
                    val angle = timeSec * 2.2f
                    val shadowOffset = Offset(
                        x = floatPos.x - cos(angle) * (28f * (1f - approach * 0.55f)).dp.toPx(),
                        y = floatPos.y + 12.dp.toPx() + sin(angle) * 8.dp.toPx()
                    )
                    drawSubsurfaceFishSilhouette(
                        center = shadowOffset,
                        scale = 0.72f,
                        minDim = minDim,
                        fightStyle = "gentle",
                        tailSwing = sin(timeSec * 5f),
                        bodyColor = Color(0xFF030D12).copy(alpha = 0.45f + approach * 0.30f),
                        finColor = biome.causticTint.copy(alpha = 0.18f),
                        nodeColor = biome.causticTint.copy(alpha = 0.25f * approach),
                        facingLeft = sin(angle) < 0f
                    )
                }

                // 7. Ripple Rings around float / fish
                val ringCount = if (surge || state.fishingState == FishingState.HOOKSET) 4 else 3
                for (ring in 1..ringCount) {
                    val animExpand = ((timeSec * (if (surge) 2.4f else 1.1f) + ring * 0.33f) % 1f)
                    val baseR = when {
                        state.fishingState == FishingState.HOOKSET -> (10f + ring * 13f).dp.toPx()
                        surge -> (10f + ring * 14f + animExpand * 8f).dp.toPx()
                        isNibbleWindow -> (7f + ring * 9f + nibblePulse * 6f).dp.toPx()
                        else -> (6f + ring * 7f).dp.toPx()
                    }
                    val ringAlpha = ((0.34f / ring) * (1f - animExpand * 0.35f)).coerceIn(0.05f, 0.45f)
                    drawOval(
                        color = lineColor.copy(alpha = ringAlpha),
                        topLeft = floatPos - Offset(baseR, baseR * 0.34f),
                        size = Size(baseR * 2f, baseR * 0.68f),
                        style = Stroke(width = (if (ring == 1 && (surge || warning)) 1.8f else 1.1f).dp.toPx())
                    )
                }

                // 8. Active Fight: Subsurface Fish / Salvage + Coral Warning Telegraph Ring
                if (fighting) {
                    val fishScale = 0.78f + progress * 0.36f
                    val r = minDim * 0.058f * fishScale

                    // Coral Warning / Surge Telegraph Ring (borrowed from Combat TelegraphFxUi)
                    if (warning || surge) {
                        val pulseR = r * (if (surge) 2.6f else 2.2f)
                        drawOval(
                            color = FishingCoral.copy(alpha = if (surge) 0.55f else 0.42f),
                            topLeft = fishPos - Offset(pulseR, pulseR * 0.52f),
                            size = Size(pulseR * 2f, pulseR * 1.04f),
                            style = Stroke(
                                width = 2.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                            )
                        )
                    }

                    // V-shaped hydrodynamic wake lines behind the fish
                    val wakeDir = if (surge) 1f else -1f
                    drawLine(
                        color = FishingMist.copy(alpha = if (surge) 0.42f else 0.22f),
                        start = fishPos + Offset(wakeDir * r * 0.8f, -r * 0.35f),
                        end = fishPos + Offset(wakeDir * r * 2.6f, -r * 0.95f),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = FishingMist.copy(alpha = if (surge) 0.42f else 0.22f),
                        start = fishPos + Offset(wakeDir * r * 0.8f, r * 0.35f),
                        end = fishPos + Offset(wakeDir * r * 2.6f, r * 0.95f),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    if (state.reelState?.salvage == true) {
                        // Submerged metallic salvage canister with glint & air bubbles
                        drawRoundRect(
                            color = Color(0xFF2B3E47),
                            topLeft = fishPos - Offset(r * 1.2f, r * 0.85f),
                            size = Size(r * 2.4f, r * 1.7f),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = FishingGold,
                            topLeft = fishPos - Offset(r * 1.2f, r * 0.85f),
                            size = Size(r * 2.4f, r * 1.7f),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = biome.causticTint,
                            start = fishPos - Offset(r * 0.7f, 0f),
                            end = fishPos + Offset(r * 0.7f, 0f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    } else {
                        val tailFreq = when {
                            surge -> 11f
                            warning || pull -> 7.5f
                            else -> 4.2f
                        }
                        val tailSwing = sin(timeSec * tailFreq)
                        // Ether teleport afterimage for Void Ray / Singularity fish
                        if (fightStyle == "ether" || surge) {
                            drawSubsurfaceFishSilhouette(
                                center = fishPos + Offset((if (surge) 14f else -10f).dp.toPx(), -6.dp.toPx()),
                                scale = fishScale * 0.92f,
                                minDim = minDim,
                                fightStyle = fightStyle,
                                tailSwing = -tailSwing,
                                bodyColor = biome.causticTint.copy(alpha = 0.22f),
                                finColor = biome.causticTint.copy(alpha = 0.15f),
                                nodeColor = FishingCoral.copy(alpha = 0.30f),
                                facingLeft = surge
                            )
                        }
                        drawSubsurfaceFishSilhouette(
                            center = fishPos,
                            scale = fishScale,
                            minDim = minDim,
                            fightStyle = fightStyle,
                            tailSwing = tailSwing,
                            bodyColor = if (warning || surge) Color(0xFFFFD0C2) else biome.fishCoreTint,
                            finColor = if (warning || surge) FishingCoral else biome.fishFinTint,
                            nodeColor = if (warning || surge) FishingCoral else biome.causticTint,
                            facingLeft = surge
                        )
                    }
                } else {
                    // 9. Resonant Chime Bobber & Hookset Strike Ring
                    if (state.fishingState == FishingState.HOOKSET) {
                        val remainingFrac = ((state.hookState?.timeRemainingMs ?: 2400L) / 2400f).coerceIn(0.15f, 1f)
                        val strikeRingR = (16f + remainingFrac * 28f).dp.toPx()
                        // Contracting gold-coral strike ring
                        drawCircle(
                            color = FishingGold.copy(alpha = 0.88f),
                            radius = strikeRingR,
                            center = floatPos,
                            style = Stroke(width = 2.8.dp.toPx())
                        )
                        drawCircle(
                            color = FishingCoral.copy(alpha = 0.45f),
                            radius = strikeRingR * 0.65f,
                            center = floatPos,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        // Upward swipe chevron trail above strike point
                        for (c in 0..2) {
                            val cy = floatPos.y - (18f + c * 13f).dp.toPx()
                            val chevAlpha = 0.95f - c * 0.22f
                            drawLine(
                                color = FishingGold.copy(alpha = chevAlpha),
                                start = Offset(floatPos.x - 9.dp.toPx(), cy + 7.dp.toPx()),
                                end = Offset(floatPos.x, cy),
                                strokeWidth = 2.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = FishingGold.copy(alpha = chevAlpha),
                                start = Offset(floatPos.x + 9.dp.toPx(), cy + 7.dp.toPx()),
                                end = Offset(floatPos.x, cy),
                                strokeWidth = 2.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Detailed 2-piece Resonant Chime Float
                    val submergedShift = if (state.fishingState == FishingState.HOOKSET) 5.dp.toPx() else 0f
                    val bobberCenter = floatPos + Offset(0f, submergedShift)
                    // Lower submerged ballast stem
                    drawLine(
                        color = biome.causticTint.copy(alpha = 0.55f),
                        start = bobberCenter,
                        end = bobberCenter + Offset(0f, 10.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Bobber body spindle
                    drawOval(
                        color = FishingInk,
                        topLeft = bobberCenter - Offset(5.5.dp.toPx(), 7.dp.toPx()),
                        size = Size(11.dp.toPx(), 14.dp.toPx())
                    )
                    drawOval(
                        color = if (state.fishingState == FishingState.HOOKSET) FishingCoral else FishingGold,
                        topLeft = bobberCenter - Offset(4.5.dp.toPx(), 6.dp.toPx()),
                        size = Size(9.dp.toPx(), 12.dp.toPx())
                    )
                    // Glowing upper antenna bead
                    drawLine(
                        color = FishingMist,
                        start = bobberCenter - Offset(0f, 6.dp.toPx()),
                        end = bobberCenter - Offset(0f, 14.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = biome.causticTint,
                        radius = 2.6.dp.toPx(),
                        center = bobberCenter - Offset(0f, 15.dp.toPx())
                    )
                }

                // 10. Articulated Composite Fishing Rod & Dynamic Tension Line
                val rodFlexX = if (fighting) tension * 0.15f + (if (surge) 0.04f else 0f) else if (state.fishingState == FishingState.HOOKSET) 0.08f else 0.02f
                val rodFlexY = if (fighting) tension * 0.07f else 0f
                val rodBase = Offset(w * 0.92f, h * 1.03f)
                val rodMid = Offset(w * (0.76f - rodFlexX * 0.45f), h * (0.81f + rodFlexY * 0.4f))
                val rodTip = Offset(w * (0.68f - rodFlexX), h * (0.68f + rodFlexY))

                // Fishing line with dynamic slack catenary vs. high-tension transverse vibration
                val linePath = Path().apply {
                    moveTo(rodTip.x, rodTip.y)
                    if (fighting && (tension >= 0.68f || surge || warning)) {
                        val segments = 12
                        for (s in 1..segments) {
                            val t = s / segments.toFloat()
                            val bx = rodTip.x + (floatPos.x - rodTip.x) * t
                            val by = rodTip.y + (floatPos.y - rodTip.y) * t
                            val jitterAmp = if (surge || tension >= 0.75f) 3.5.dp.toPx() else 1.8.dp.toPx()
                            val jitter = sin(t * PI.toFloat() * 4f + timeSec * 30f) * jitterAmp * sin(t * PI.toFloat())
                            lineTo(bx + jitter, by - jitter * 0.5f)
                        }
                    } else {
                        val sag = (1f - tension) * h * 0.11f
                        quadraticTo(
                            (rodTip.x + floatPos.x) * 0.5f - w * 0.06f,
                            (rodTip.y + floatPos.y) * 0.5f + sag,
                            floatPos.x,
                            floatPos.y
                        )
                    }
                }
                // Outer line glow
                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = 0.28f),
                    style = Stroke(width = (3.5f + tension * 2f).dp.toPx(), cap = StrokeCap.Round)
                )
                // Core line
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = (1.5f + tension * 1.1f).dp.toPx(), cap = StrokeCap.Round)
                )

                // Rod shadow & tapered composite shaft
                val rodPath = Path().apply {
                    moveTo(rodBase.x, rodBase.y)
                    quadraticTo(rodMid.x, rodMid.y, rodTip.x, rodTip.y)
                }
                drawPath(
                    path = rodPath,
                    color = Color.Black.copy(alpha = 0.45f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = rodPath,
                    color = FishingGold,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                // Rod guide eyelets & glowing tip sensor
                drawCircle(color = biome.causticTint, radius = 3.2.dp.toPx(), center = rodTip)
                drawCircle(color = FishingMist.copy(alpha = 0.75f), radius = 2.2.dp.toPx(), center = rodMid)
            }

            if (state.fishingState == FishingState.HOOKSET && state.hookState?.practiceHook == true) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = FishingInk.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, FishingGold.copy(alpha = 0.7f)),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                ) {
                    Text(
                        text = "FIRST BITE · NO RUSH",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FishingGold
                    )
                }
            }
        }

        if (showTelemetryFooter && state.fishingState == FishingState.REELING) {
            val mysteryLabel = when {
                state.reelState?.salvage == true -> "Submerged Salvage"
                fightStyle == "dart" -> "Unidentified Catch · Darting"
                fightStyle == "steady" -> "Unidentified Catch · Heavy Pull"
                fightStyle == "drift" -> "Unidentified Catch · Deep Drift"
                fightStyle == "wave" -> "Unidentified Catch · Standing Wave"
                fightStyle == "ether" -> "Unidentified Catch · Phase Shift"
                else -> "Unidentified Catch · Gentle Ripple"
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = mysteryLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = FishingMist
                )
                Text(
                    text = if (tension >= .75f) "Tight — release" else if (tension >= .4f) "Line loading" else "Line relaxed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = lineColor
                )
            }
            LinearProgressIndicator(
                progress = { tension },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                color = lineColor,
                trackColor = Color(0xFF233B44)
            )
        }
    }
}

private fun DrawScope.drawSubsurfaceFishSilhouette(
    center: Offset,
    scale: Float,
    minDim: Float,
    fightStyle: String,
    tailSwing: Float,
    bodyColor: Color,
    finColor: Color,
    nodeColor: Color,
    facingLeft: Boolean
) {
    val r = minDim * 0.055f * scale
    val dir = if (facingLeft) 1f else -1f
    val tailOffset = tailSwing * r * 0.42f

    when (fightStyle) {
        "ether" -> {
            // Wide winged manta / Void Ray silhouette
            val wingPath = Path().apply {
                moveTo(center.x - dir * r * 1.4f, center.y) // Snout
                quadraticTo(center.x - dir * r * 0.2f, center.y - r * 1.65f, center.x + dir * r * 0.9f, center.y - r * 0.35f)
                lineTo(center.x + dir * r * 2.4f, center.y + tailOffset) // Whip tail tip
                lineTo(center.x + dir * r * 0.9f, center.y + r * 0.35f)
                quadraticTo(center.x - dir * r * 0.2f, center.y + r * 1.65f, center.x - dir * r * 1.4f, center.y)
                close()
            }
            drawPath(wingPath, bodyColor)
            drawCircle(nodeColor, radius = r * 0.18f, center = center - Offset(dir * r * 0.4f, 0f))
            drawCircle(FishingInk, radius = r * 0.11f, center = center - Offset(dir * r * 0.95f, r * 0.18f))
        }
        "drift" -> {
            // Sinuous S-curve Stellarium Eel silhouette
            val eelPath = Path().apply {
                moveTo(center.x - dir * r * 1.7f, center.y)
                cubicTo(
                    center.x - dir * r * 0.5f, center.y - r * 0.85f + tailOffset * 0.5f,
                    center.x + dir * r * 0.6f, center.y + r * 0.85f - tailOffset * 0.5f,
                    center.x + dir * r * 2.1f, center.y + tailOffset
                )
            }
            drawPath(eelPath, finColor.copy(alpha = 0.55f), style = Stroke(width = r * 0.95f, cap = StrokeCap.Round))
            drawPath(eelPath, bodyColor, style = Stroke(width = r * 0.58f, cap = StrokeCap.Round))
            drawCircle(nodeColor, radius = r * 0.14f, center = center - Offset(dir * r * 1.3f, 0f))
        }
        else -> {
            // Sculpted fusiform fish (minnow / carp / glowfish / tetra) with dorsal fin & forked tail
            val bodyLen = if (fightStyle == "dart") r * 1.65f else r * 1.50f
            val bodyHalfH = if (fightStyle == "dart") r * 0.52f else r * 0.68f

            // Pectoral / dorsal fins
            val dorsalPath = Path().apply {
                moveTo(center.x - dir * r * 0.2f, center.y - bodyHalfH * 0.7f)
                lineTo(center.x + dir * r * 0.45f, center.y - bodyHalfH * 1.45f)
                lineTo(center.x + dir * r * 0.75f, center.y - bodyHalfH * 0.5f)
                close()
            }
            drawPath(dorsalPath, finColor)

            // Forked caudal tail oscillating with tailSwing
            val tailBaseX = center.x + dir * bodyLen * 0.82f
            val tailTipX = center.x + dir * bodyLen * 1.55f
            val tailPath = Path().apply {
                moveTo(tailBaseX, center.y)
                lineTo(tailTipX, center.y - bodyHalfH * 1.15f + tailOffset)
                lineTo(center.x + dir * bodyLen * 1.22f, center.y + tailOffset * 0.5f)
                lineTo(tailTipX, center.y + bodyHalfH * 1.15f + tailOffset)
                close()
            }
            drawPath(tailPath, finColor)

            // Main sculpted body
            val bodyPath = Path().apply {
                moveTo(center.x - dir * bodyLen, center.y) // Nose
                quadraticTo(center.x - dir * bodyLen * 0.15f, center.y - bodyHalfH * 1.15f, tailBaseX, center.y - bodyHalfH * 0.22f)
                lineTo(tailBaseX, center.y + bodyHalfH * 0.22f)
                quadraticTo(center.x - dir * bodyLen * 0.15f, center.y + bodyHalfH * 1.15f, center.x - dir * bodyLen, center.y)
                close()
            }
            drawPath(bodyPath, bodyColor)

            // Bioluminescent lateral line nodes
            for (n in 0..2) {
                val nx = center.x - dir * (bodyLen * 0.35f - n * r * 0.42f)
                drawCircle(nodeColor.copy(alpha = 0.85f), radius = r * 0.11f, center = Offset(nx, center.y))
            }
            // Eye
            drawCircle(
                color = FishingInk,
                radius = r * 0.13f,
                center = Offset(center.x - dir * bodyLen * 0.62f, center.y - bodyHalfH * 0.18f)
            )
        }
    }
}
