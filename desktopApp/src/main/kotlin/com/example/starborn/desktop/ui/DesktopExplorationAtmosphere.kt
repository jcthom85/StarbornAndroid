package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.theme.defaultWeatherForEnvironment
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.random.Random

private data class ExplorationParticle(var position: Offset, var velocity: Offset, var size: Pair<Float, Float>, var color: Color, var life: Float, val maxLife: Float, var turbulence: List<Float>? = null)

/** Weather spans the scene and reflections; HUD is composed above it. */
@Composable
internal fun DesktopExplorationAtmosphere(services: DesktopAppServices, room: Room?, isDark: Boolean) {
    val settings = LocalExplorationSettings.current
    val weather = room?.weather ?: defaultWeatherForEnvironment(room?.env) ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val rainWidthScale = (minOf(maxHeight * (9f / 16f), maxWidth) / maxWidth).coerceIn(.2f, 1f)
    key(room?.id, weather) {
        val modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isDark) .15f else 1f }
        when (weather.lowercase(java.util.Locale.ROOT)) {
            "steam" -> ExplorationSteamEffect(services, modifier, Color(.92f, .92f, .95f))
            "fog" -> ExplorationFogEffect(modifier, Color(.85f, .88f, .92f), .95f)
            "gas" -> ExplorationGasEffect(modifier, Color(.42f, .72f, .28f))
            "resonance" -> ExplorationResonanceEffect(modifier, Color(.45f, .7f, 1f))
            else -> DesktopWeatherOverlay(weather, modifier, suppressFlashes = settings.disableFlashes, rainWidthScale = rainWidthScale)
        }
    }
    }
}

private data class SteamJetVent(
    val nozzleX: Float,
    val nozzleY: Float,
    val angleDeg: Float,
    val widthDp: Float,
    val heightDp: Float,
    val playbackFps: Float,
    val frameOffset: Int,
    val baseAlpha: Float
)

@Composable
private fun ExplorationSteamEffect(services: DesktopAppServices,
    modifier: Modifier = Modifier,
    color: Color
) {
    val frames = rememberExplorationSteamJetFrames(services)
    var timeSeconds by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNanos = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { frameNanos ->
                timeSeconds = (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    val vents = remember {
        listOf(
            // Lower left sweating pipe (shoots up & slightly inward into the shaft)
            SteamJetVent(nozzleX = 0.385f, nozzleY = 0.690f, angleDeg = 14f, widthDp = 104f, heightDp = 158f, playbackFps = 20.0f, frameOffset = 0, baseAlpha = 0.52f),
            // Lower right pipe seam (shoots up & slightly inward into the shaft)
            SteamJetVent(nozzleX = 0.615f, nozzleY = 0.690f, angleDeg = -14f, widthDp = 104f, heightDp = 158f, playbackFps = 19.0f, frameOffset = 14, baseAlpha = 0.52f),
            // Mid-left shaft rail vent
            SteamJetVent(nozzleX = 0.360f, nozzleY = 0.490f, angleDeg = 12f, widthDp = 84f, heightDp = 126f, playbackFps = 21.0f, frameOffset = 26, baseAlpha = 0.42f),
            // Mid-right shaft rail vent
            SteamJetVent(nozzleX = 0.640f, nozzleY = 0.490f, angleDeg = -12f, widthDp = 84f, heightDp = 126f, playbackFps = 20.0f, frameOffset = 38, baseAlpha = 0.42f),
            // Central floor grate main plume (tall vertical eruption)
            SteamJetVent(nozzleX = 0.500f, nozzleY = 0.730f, angleDeg = 0f, widthDp = 125f, heightDp = 185f, playbackFps = 18.0f, frameOffset = 8, baseAlpha = 0.48f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (frames.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val tSec = timeSeconds
        val totalFrames = frames.size

        // Ambient floor steam condensation glow
        val fogPulse = 0.12f + 0.04f * kotlin.math.sin(tSec * 0.75f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = fogPulse * 0.9f),
                    color.copy(alpha = fogPulse * 0.5f),
                    Color.Transparent
                ),
                center = Offset(w * 0.50f, h * 0.70f),
                radius = w * 0.38f
            ),
            center = Offset(w * 0.50f, h * 0.70f),
            radius = w * 0.38f
        )

        vents.forEach { vent ->
            val rawPos = tSec * vent.playbackFps + vent.frameOffset
            val floorPos = kotlin.math.floor(rawPos).toInt()
            val frac = (rawPos - floorPos).toFloat()
            val frameIndexA = ((floorPos % totalFrames) + totalFrames) % totalFrames
            val frameIndexB = (frameIndexA + 1) % totalFrames
            val spriteA = frames[frameIndexA]
            val spriteB = frames[frameIndexB]

            val widthPx = vent.widthDp.dp.toPx()
            val heightPx = vent.heightDp.dp.toPx()
            val nozzleCenter = Offset(vent.nozzleX * w, vent.nozzleY * h)

            val pulse = 1.0f + 0.08f * kotlin.math.sin(tSec * 1.0f + vent.frameOffset)
            val finalWidthPx = widthPx * pulse
            val finalHeightPx = heightPx * pulse

            val dstLeft = nozzleCenter.x - (finalWidthPx / 2f)
            val dstTop = nozzleCenter.y - finalHeightPx
            val baseAlpha = (vent.baseAlpha * (0.92f + 0.12f * kotlin.math.sin(tSec * 0.9f + vent.frameOffset))).coerceIn(0f, 0.75f)

            rotate(
                degrees = vent.angleDeg + kotlin.math.sin(tSec * 0.6f + vent.frameOffset) * 2.5f,
                pivot = nozzleCenter
            ) {
                // High-framerate 60fps/120fps sub-frame blending between adjacent frames
                if (1f - frac > 0.01f) {
                    drawImage(
                        image = spriteA,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(spriteA.width, spriteA.height),
                        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = IntSize(finalWidthPx.toInt().coerceAtLeast(1), finalHeightPx.toInt().coerceAtLeast(1)),
                        alpha = baseAlpha * (1f - frac)
                    )
                }
                if (frac > 0.01f) {
                    drawImage(
                        image = spriteB,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(spriteB.width, spriteB.height),
                        dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                        dstSize = IntSize(finalWidthPx.toInt().coerceAtLeast(1), finalHeightPx.toInt().coerceAtLeast(1)),
                        alpha = baseAlpha * frac
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberExplorationSteamJetFrames(services: DesktopAppServices): List<ImageBitmap> =
    (0 until 48).mapNotNull { index ->
        (rememberDesktopAssetPainter("images/vfx/steam_jet/steam_frame_%02d.png".format(java.util.Locale.ROOT, index), services.assetProvider) as? DesktopBitmapPainter)?.bitmap
    }

@Composable
private fun ExplorationFogEffect(
    modifier: Modifier = Modifier,
    color: Color,
    density: Float
) {
    val particles = remember { mutableStateListOf<ExplorationParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(density) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            val maxCount = (12 * density).toInt().coerceAtLeast(8)
            if (particles.size < maxCount) {
                val x = random.nextFloat() * 1.4f - 0.2f
                val y = random.nextFloat() * 0.38f + 0.56f
                val size = (random.nextFloat() * 0.42f + 0.44f) to (random.nextFloat() * 0.070f + 0.070f)
                val vx = random.nextFloat() * 0.0008f + 0.00025f
                val vy = (random.nextFloat() - 0.5f) * 0.0002f
                val life = random.nextFloat() * 9f + 10f
                particles.add(
                    ExplorationParticle(
                        position = Offset(x, y),
                        velocity = Offset(vx, vy),
                        size = size,
                        color = color.copy(alpha = random.nextFloat() * 0.070f + 0.075f),
                        life = life,
                        maxLife = life
                    )
                )
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                if (p.position.x > 1.2f) {
                    p.position = Offset(-0.2f, p.position.y)
                }
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = (kotlin.math.sin(progress * PI.toFloat()) * p.color.alpha).coerceIn(0f, 1f)
            val widthPx = p.size.first * size.width
            val heightPx = p.size.second * size.height
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = alpha),
                        p.color.copy(alpha = alpha * 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = widthPx * 0.5f
                ),
                topLeft = Offset(center.x - widthPx / 2f, center.y - heightPx / 2f),
                size = Size(widthPx, heightPx)
            )
        }
    }
}

@Composable
private fun ExplorationGasEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particles = remember { mutableStateListOf<ExplorationParticle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            if (particles.size < 28) {
                val x = random.nextFloat() * 0.9f + 0.05f
                val y = random.nextFloat() * 0.58f + 0.20f
                val size = (random.nextFloat() * 0.20f + 0.18f) to (random.nextFloat() * 0.060f + 0.060f)
                val vx = (random.nextFloat() - 0.5f) * 0.0012f
                val vy = (random.nextFloat() - 0.5f) * 0.0009f
                val life = random.nextFloat() * 5.5f + 5.5f
                val phase = random.nextFloat() * (2f * PI).toFloat()
                particles.add(
                    ExplorationParticle(
                        position = Offset(x, y),
                        velocity = Offset(vx, vy),
                        size = size,
                        color = color.copy(alpha = random.nextFloat() * 0.105f + 0.120f),
                        life = life,
                        maxLife = life,
                        turbulence = listOf(phase)
                    )
                )
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = (kotlin.math.sin(progress * PI.toFloat()) * p.color.alpha).coerceIn(0f, 1f)
            val widthPx = p.size.first * size.width
            val heightPx = p.size.second * size.height
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            val phase = p.turbulence?.getOrNull(0) ?: p.maxLife
            val offset = kotlin.math.sin(progress * PI.toFloat() * 2f + phase) * widthPx * 0.18f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = alpha),
                        p.color.copy(alpha = alpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = widthPx * 0.52f
                ),
                topLeft = Offset(center.x - widthPx / 2f + offset, center.y - heightPx / 2f),
                size = Size(widthPx, heightPx)
            )
        }
    }
}

@Composable
private fun ExplorationResonanceEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val transition = rememberInfiniteTransition()
    val timeState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = timeState.value
        val steps = 100
        val strokeWidth = 2.dp.toPx()
        val baseAlpha = color.alpha * 0.35f

        for (waveIndex in 0..2) {
            val wavePath = Path()
            val yCenter = size.height * (0.4f + waveIndex * 0.1f)
            val amp = size.height * (0.03f + waveIndex * 0.015f)
            val freq = 4f + waveIndex * 1.5f
            val speedMult = 1f + waveIndex * 0.5f

            for (i in 0..steps) {
                val fraction = i / steps.toFloat()
                val x = fraction * size.width
                val y = yCenter + amp * kotlin.math.sin(fraction * freq * PI.toFloat() + t * speedMult)
                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }

            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = baseAlpha * (1.0f - waveIndex * 0.2f)),
                    color.copy(alpha = baseAlpha * (1.0f - waveIndex * 0.2f)),
                    Color.Transparent
                ),
                startX = 0f,
                endX = size.width
            )

            drawPath(
                path = wavePath,
                brush = brush,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

