package com.example.starborn.ui.vfx

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

@Composable
fun WeatherOverlay(
    weatherId: String?,
    suppressFlashes: Boolean = false,
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    darkness: Float = 0f
) {
    when (val weatherEffect = when (weatherId?.lowercase()) {
        "dust" -> WeatherEffect.Dust(color = Color(1.0f, 0.93f, 0.75f))
        "rain" -> WeatherEffect.Rain(intensity = "medium", color = Color(0.8f, 0.9f, 1.0f))
        "storm" -> WeatherEffect.Storm(intensity = "medium", color = Color(0.8f, 0.9f, 1.0f), lightningColor = Color.White)
        "snow" -> WeatherEffect.Snow(intensity = "medium", color = Color.White)
        "cave_drip" -> WeatherEffect.CaveDrip(color = Color(0.6f, 0.8f, 1.0f))
        "starfall" -> WeatherEffect.Starfall(color = Color(0.9f, 0.95f, 1.0f))
        "steam" -> WeatherEffect.Steam(color = Color(0.92f, 0.92f, 0.95f))
        "fog" -> WeatherEffect.Fog(color = Color(0.85f, 0.88f, 0.92f), density = 0.95f)
        "gas" -> WeatherEffect.Gas(color = Color(0.42f, 0.72f, 0.28f))
        "resonance" -> WeatherEffect.Resonance(color = Color(0.45f, 0.7f, 1.0f))
        "sparks" -> WeatherEffect.Sparks(color = Color(1.0f, 0.65f, 0.15f))
        else -> null
    }?.applyTint(tintColor, darkness)) {
        is WeatherEffect.Rain -> {
            RainEffect(modifier = modifier, intensity = weatherEffect.intensity, color = weatherEffect.color, drift = 0.0f)
        }
        is WeatherEffect.Snow -> {
            SnowEffect(modifier = modifier, intensity = weatherEffect.intensity, color = weatherEffect.color)
        }
        is WeatherEffect.Dust -> {
            DustEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Storm -> {
            StormEffect(
                modifier = modifier,
                intensity = weatherEffect.intensity,
                color = weatherEffect.color,
                lightningColor = weatherEffect.lightningColor,
                lightningEnabled = !suppressFlashes
            )
        }
        is WeatherEffect.CaveDrip -> {
            CaveDripEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Starfall -> {
            StarfallEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Steam -> {
            SteamEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Fog -> {
            FogEffect(modifier = modifier, color = weatherEffect.color, density = weatherEffect.density)
        }
        is WeatherEffect.Gas -> {
            GasEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Resonance -> {
            ResonanceEffect(modifier = modifier, color = weatherEffect.color)
        }
        is WeatherEffect.Sparks -> {
            SparksEffect(modifier = modifier, color = weatherEffect.color)
        }
        null -> {
            // No weather effect
        }
    }
}

private fun WeatherEffect.applyTint(tintColor: Color?, darkness: Float): WeatherEffect {
    return when (this) {
        is WeatherEffect.Rain -> copy(color = color.tintWith(tintColor, darkness))
        is WeatherEffect.Snow -> copy(color = color.tintWith(tintColor, darkness, 0.25f))
        is WeatherEffect.Dust -> copy(color = color.tintWith(tintColor, darkness, 0.5f))
        is WeatherEffect.Storm -> copy(
            color = color.tintWith(tintColor, darkness),
            lightningColor = lightningColor.tintWith(tintColor, darkness, 0.15f)
        )
        is WeatherEffect.CaveDrip -> copy(color = color.tintWith(tintColor, darkness, 0.3f))
        is WeatherEffect.Starfall -> copy(color = color.tintWith(tintColor, darkness, 0.45f))
        is WeatherEffect.Steam -> copy(color = color.tintWith(tintColor, darkness, 0.4f))
        is WeatherEffect.Fog -> copy(color = color.tintWith(tintColor, darkness, 0.6f))
        is WeatherEffect.Gas -> copy(color = color.tintWith(tintColor, darkness, 0.2f))
        is WeatherEffect.Resonance -> copy(color = color.tintWith(tintColor, darkness, 0.1f))
        is WeatherEffect.Sparks -> copy(color = color.tintWith(tintColor, darkness, 0.1f))
    }
}

private fun Color.tintWith(tint: Color?, darkness: Float, tintFactor: Float = 0.35f): Color {
    val blend = tint?.let { lerp(this, it, tintFactor) } ?: this
    val alphaBoost = 0.85f + darkness.coerceIn(0f, 1f) * 0.15f
    return blend.copy(alpha = (blend.alpha * alphaBoost).coerceIn(0f, 1f))
}

@Composable
fun RainEffect(
    modifier: Modifier = Modifier,
    intensity: String,
    color: Color,
    drift: Float = Random.nextFloat() * 0.02f - 0.01f
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(intensity) {
        while (true) {
            val newParticles = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(newParticles)

            val numToEmit = when (intensity) {
                "low" -> 10
                "medium" -> 20
                "high" -> 40
                else -> 0
            }

            for (i in 0 until numToEmit) {
                val x = random.nextFloat() * 1.7f - 0.5f
                val y = -0.1f
                val size = (random.nextFloat() * 0.003f + 0.001f) to (random.nextFloat() * 0.015f + 0.005f)
                val velocity = Offset(drift + (random.nextFloat() - 0.5f) * 0.01f, random.nextFloat() * 0.01f + 0.01f)
                val life = random.nextFloat() * 0.8f + 0.2f
                particles.add(Particle(Offset(x, y), velocity, size, color, life, life))
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
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val shimmer = (kotlin.math.sin(p.life * 30f) + 1f) / 2f * 0.2f + 0.8f
            rotate(degrees = 15f, pivot = Offset(p.position.x * size.width, p.position.y * size.height)) {
                drawRect(
                    color = p.color.copy(alpha = alpha * shimmer),
                    topLeft = Offset(p.position.x * size.width, p.position.y * size.height),
                    size = androidx.compose.ui.geometry.Size(p.size.first * size.width, p.size.second * size.height)
                )
            }
        }
    }
}

@Composable
fun SnowEffect(
    modifier: Modifier = Modifier,
    intensity: String,
    color: Color
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }
    val accumulationColumns = remember {
        mutableStateListOf<Float>().apply {
            repeat(64) { add(0f) }
        }
    }
    val maxColumnHeight = 0.16f
    val density = LocalDensity.current
    val snowCapPx = with(density) { 72.dp.toPx() }

    LaunchedEffect(intensity) {
        while (true) {
            val newParticles = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(newParticles)

            for (i in accumulationColumns.indices) {
                val baseMelt = 0.00008f + accumulationColumns[i] * 0.00025f
                val decayed = (accumulationColumns[i] - baseMelt).coerceAtLeast(0f)
                if (decayed != accumulationColumns[i]) {
                    accumulationColumns[i] = decayed
                }
            }

            val numToEmit = when (intensity) {
                "low" -> 1
                "medium" -> 1
                "high" -> 2
                else -> 0
            }

            for (i in 0 until numToEmit) {
                val x = random.nextFloat()
                val y = -0.1f
                val flakeRadius = random.nextFloat() * 0.0065f + 0.0035f
                val size = flakeRadius to flakeRadius
                val velocity = Offset(
                    random.nextFloat() * 0.0012f - 0.0006f,
                    random.nextFloat() * 0.0016f + 0.001f
                )
                val life = random.nextFloat() * 4f + 5f
                val turbulence = listOf(
                    random.nextFloat() * 0.0008f - 0.0004f,
                    random.nextFloat() * 0.0006f - 0.0003f
                )
                particles.add(
                    Particle(
                        Offset(x, y),
                        velocity,
                        size,
                        color.copy(alpha = 0.75f),
                        life,
                        life,
                        turbulence = turbulence
                    )
                )
            }

            val particlesToRemove = mutableListOf<Particle>()
            for (p in particles) {
                val turbulenceX = p.turbulence?.get(0) ?: 0f
                val turbulenceY = p.turbulence?.get(1) ?: 0f
                p.position = Offset(p.position.x + p.velocity.x + turbulenceX, p.position.y + p.velocity.y + turbulenceY)
                p.life -= 0.016f

                if (p.position.y >= 1f) {
                    val columnIndex = ((p.position.x.coerceIn(0f, 0.999f)) * accumulationColumns.size).toInt()
                    val deposit = (p.size.first + p.size.second) * 0.2f
                    val updated = (accumulationColumns[columnIndex] + deposit).coerceAtMost(maxColumnHeight)
                    accumulationColumns[columnIndex] = updated
                    particlesToRemove.add(p)
                }
            }
            particles.removeAll(particlesToRemove)

            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.8f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size.first * size.width / 2,
                center = Offset(p.position.x * size.width, p.position.y * size.height)
            )
        }

        if (accumulationColumns.any { it > 0f }) {
            val limitNormalized = minOf(maxColumnHeight, (snowCapPx / size.height).coerceAtLeast(0f))
            val smoothed = accumulationColumns.mapIndexed { index, value ->
                val left = accumulationColumns.getOrNull(index - 1) ?: value
                val right = accumulationColumns.getOrNull(index + 1) ?: value
                val base = (left + value + right) / 3f
                val wobble = kotlin.math.sin(index * 0.4f) * 0.004f
                (base + wobble).coerceAtLeast(0f)
            }
            val path = Path().apply {
                moveTo(0f, size.height)
                smoothed.forEachIndexed { index, rawHeight ->
                    val clamped = rawHeight.coerceIn(0f, limitNormalized)
                    val x = index / (accumulationColumns.size - 1f) * size.width
                    val y = size.height - (clamped * size.height)
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.82f)
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.12f),
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun DustEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            if (particles.size < 24) {
                val deficit = 24 - particles.size
                repeat(deficit.coerceAtMost(3)) {
                    particles.add(spawnDustParticle(random, color))
                }
            }

            val survivors = mutableListOf<Particle>()
            particles.forEach { p ->
                val elapsed = (p.maxLife - p.life).coerceAtLeast(0f)
                val turbulence = p.turbulence
                val verticalDrift = turbulence?.let {
                    val amplitude = it.getOrNull(0) ?: 0f
                    val frequency = it.getOrNull(1) ?: 0f
                    val phase = it.getOrNull(2) ?: 0f
                    amplitude * kotlin.math.sin(elapsed * frequency + phase)
                } ?: 0f
                val crosswind = turbulence?.let {
                    val amplitude = it.getOrNull(0) ?: 0f
                    val frequency = it.getOrNull(1) ?: 0f
                    val phase = it.getOrNull(2) ?: 0f
                    (amplitude * 0.35f) * cos(elapsed * frequency * 0.28f + phase)
                } ?: 0f
                p.position = Offset(
                    p.position.x + p.velocity.x + crosswind,
                    p.position.y + p.velocity.y + verticalDrift
                )
                p.life -= 0.016f
                if (p.life > 0f && p.position.x in -0.25f..1.25f && p.position.y in -0.25f..1.25f) {
                    survivors += p
                }
            }
            particles.clear()
            particles.addAll(survivors)
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = (kotlin.math.sin(progress * PI.toFloat()) * p.color.alpha).coerceIn(0f, 1f)
            val elapsed = p.maxLife - p.life
            val freq = p.turbulence?.getOrNull(1) ?: 0.1f
            val shimmer = (kotlin.math.sin(elapsed * freq * 20f) * 0.35f + 0.65f).coerceIn(0f, 1f)
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            val widthPx = (p.size.first * size.width).coerceAtLeast(2f)
            val heightPx = (p.size.second * size.height).coerceAtLeast(1f)
            drawOval(
                color = p.color.copy(alpha = alpha * shimmer),
                topLeft = Offset(center.x - widthPx / 2f, center.y - heightPx / 2f),
                size = Size(widthPx, heightPx)
            )
        }
    }
}

@Composable
fun StormEffect(
    modifier: Modifier = Modifier,
    intensity: String = "high",
    color: Color = Color(0.6f, 0.8f, 1.0f),
    lightningColor: Color = Color.White.copy(alpha = 0.8f),
    lightningEnabled: Boolean = true
) {
    val lightningAlpha = remember { Animatable(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        RainEffect(
            modifier = Modifier.matchParentSize(),
            intensity = intensity,
            color = color,
            drift = 0f
        )
        if (lightningEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(lightningColor.copy(alpha = lightningAlpha.value))
            )
        }
    }

    LaunchedEffect(lightningEnabled) {
        lightningAlpha.snapTo(0f)
        if (!lightningEnabled) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(1_400L, 4_200L))
            val strikes = Random.nextInt(1, 3)
            repeat(strikes) { index ->
                val peak = Random.nextFloat().coerceIn(0.65f, 0.95f)
                lightningAlpha.animateTo(peak, animationSpec = tween(durationMillis = 70, easing = LinearEasing))
                lightningAlpha.animateTo(peak * 0.4f, animationSpec = tween(durationMillis = 90, easing = LinearEasing))
                if (index < strikes - 1) {
                    delay(Random.nextLong(60L, 160L))
                }
            }
            lightningAlpha.animateTo(0f, animationSpec = tween(durationMillis = 520, easing = LinearEasing))
        }
    }
}

@Composable
fun CaveDripEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }
    val splashes = remember { mutableStateListOf<Splash>() }

    LaunchedEffect(Unit) {
        while (true) {
            val newParticles = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(newParticles)

            if (particles.size < 5) { // Limit number of drips
                val numToEmit = 1

                for (i in 0 until numToEmit) {
                    val x = random.nextFloat()
                    val baseWidth = random.nextFloat() * 0.0035f + 0.0015f
                    val baseHeight = random.nextFloat() * 0.006f + 0.003f
                    val hangDuration = random.nextFloat() * 0.8f + 0.4f
                    val size = baseWidth to baseHeight
                    val life = random.nextFloat() * 2f + 1.2f
                    particles.add(
                        Particle(
                            position = Offset(x, -baseHeight),
                            velocity = Offset.Zero,
                            size = size,
                            color = color,
                            life = life,
                            maxLife = life,
                            state = "pooling",
                            hangTime = hangDuration,
                            angle = hangDuration,
                            initialSize = size
                        )
                    )
                }
            }

            for (p in particles) {
                when (p.state) {
                    "pooling" -> {
                        val remaining = (p.hangTime ?: 0f) - 0.016f
                        p.hangTime = remaining
                        val total = (p.angle ?: (p.hangTime ?: 0f)).coerceAtLeast(0.001f)
                        val progress = 1f - (remaining / total).coerceIn(0f, 1f)
                        val stretch = 0.09f
                        val baseHeight = p.initialSize?.second ?: 0.003f
                        val newHeight = baseHeight + stretch * progress
                        p.size = (p.initialSize?.first ?: p.size.first) to newHeight
                        p.position = Offset(p.position.x, -newHeight)
                        if (remaining <= 0f) {
                            p.state = "falling"
                            p.velocity = Offset(0f, 0.004f)
                        }
                    }
                    else -> {
                        val gravity = 0.0009f
                        val terminal = 0.035f
                        val vy = (p.velocity.y + gravity).coerceAtMost(terminal)
                        p.velocity = Offset(0f, vy)
                        p.position = Offset(p.position.x, p.position.y + vy)
                        if (p.position.y >= 1f) {
                            splashes.add(
                                Splash(
                                    center = Offset(p.position.x, 1f),
                                    maxLife = 0.35f,
                                    life = 0.35f,
                                    radius = p.size.first * 1.5f,
                                    color = color.copy(alpha = 0.55f)
                                )
                            )
                            p.life = 0f
                        }
                    }
                }
                p.life -= 0.016f
            }

            delay(16)

            if (splashes.isNotEmpty()) {
                val updated = mutableListOf<Splash>()
                for (splash in splashes) {
                    val newLife = splash.life - 0.016f
                    if (newLife > 0f) {
                        updated.add(splash.copy(life = newLife))
                    }
                }
                splashes.clear()
                splashes.addAll(updated)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 0.8f)
            val widthPx = (p.size.first * size.width).coerceAtLeast(3f)
            val heightPx = (p.size.second * size.height).coerceAtLeast(widthPx * 1.1f)
            val centerX = p.position.x * size.width
            val topY = p.position.y * size.height
            val bottomY = topY + heightPx
            val radius = widthPx / 2f
            val capRect = Rect(
                left = centerX - radius,
                top = topY,
                right = centerX + radius,
                bottom = topY + radius * 2f
            )
            val dropPath = Path().apply {
                moveTo(centerX - radius, topY + radius)
                arcTo(capRect, 180f, 180f, false)
                lineTo(centerX + radius, topY + radius)
                quadraticBezierTo(centerX, bottomY, centerX - radius, topY + radius)
                close()
            }
            drawPath(
                path = dropPath,
                color = p.color.copy(alpha = alpha)
            )
        }
        splashes.forEach { splash ->
            val progress = (splash.life / splash.maxLife).coerceIn(0f, 1f)
            val radiusPx = (splash.radius * size.width) * (1f + (1f - progress))
            val opacity = splash.color.alpha * progress
            val center = Offset(splash.center.x * size.width, splash.center.y * size.height)
            drawCircle(
                color = splash.color.copy(alpha = opacity),
                radius = radiusPx,
                center = center,
                style = Stroke(width = 2f * progress.coerceAtLeast(0.2f))
            )
        }
    }
}

@Composable
fun StarfallEffect(
    modifier: Modifier = Modifier,
    color: Color = Color(1.0f, 1.0f, 0.8f)
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val newParticles = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(newParticles)

            val numToEmit = 2

            for (i in 0 until numToEmit) {
                val x = random.nextFloat()
                val y = -0.1f
                val size = (random.nextFloat() * 0.005f + 0.002f) to (random.nextFloat() * 0.05f + 0.02f)
                val velocity = Offset(0f, random.nextFloat() * 0.02f + 0.02f)
                val life = random.nextFloat() * 0.5f + 0.2f
                particles.add(Particle(Offset(x, y), velocity, size, color, life, life))
            }

            for (p in particles) {
                p.position = Offset(p.position.x, p.position.y + p.velocity.y)
                p.life -= 0.016f
            }

            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawRoundRect(
                color = p.color.copy(alpha = 0.85f),
                topLeft = Offset(p.position.x * size.width, p.position.y * size.height),
                size = Size(p.size.first * size.width, p.size.second * size.height),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

private data class Splash(
    val center: Offset,
    val maxLife: Float,
    val life: Float,
    val radius: Float,
    val color: Color
)

private fun spawnDustParticle(random: Random, color: Color): Particle {
    val fromLeft = random.nextBoolean()
    val startX = if (fromLeft) -0.12f else 1.12f
    val startY = random.nextFloat()
    val speed = random.nextFloat() * 0.0011f + 0.0004f
    val vx = if (fromLeft) speed else -speed
    val vy = (random.nextFloat() - 0.5f) * 0.0004f
    val width = random.nextFloat() * 0.020f + 0.010f
    val height = width * (random.nextFloat() * 0.18f + 0.12f)
    val life = random.nextFloat() * 5.5f + 7.5f
    val amplitude = random.nextFloat() * 0.004f + 0.002f
    val frequency = random.nextFloat() * 0.22f + 0.08f
    val phase = random.nextFloat() * (2f * PI).toFloat()
    return Particle(
        position = Offset(startX, startY),
        velocity = Offset(vx, vy),
        size = width to height,
        color = color.copy(alpha = random.nextFloat() * 0.08f + 0.06f),
        life = life,
        maxLife = life,
        turbulence = listOf(amplitude, frequency, phase)
    )
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
fun SteamEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val frames = rememberSteamJetFrames()
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
private fun rememberSteamJetFrames(): List<ImageBitmap> {
    val context = LocalContext.current
    return remember(context) {
        (0 until 48).mapNotNull { idx ->
            val path = String.format("images/vfx/steam_jet/steam_frame_%02d.png", idx)
            runCatching {
                context.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}

@Composable
fun FogEffect(
    modifier: Modifier = Modifier,
    color: Color,
    density: Float
) {
    val particles = remember { mutableStateListOf<Particle>() }
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
                    Particle(
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
fun GasEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particles = remember { mutableStateListOf<Particle>() }
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
                    Particle(
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
fun ResonanceEffect(
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

@Composable
fun SparksEffect(
    modifier: Modifier = Modifier,
    color: Color
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val survivors = particles.filter { it.life > 0 }
            particles.clear()
            particles.addAll(survivors)

            if (particles.size < 30) {
                val x = random.nextFloat()
                val y = 1.05f
                val size = (random.nextFloat() * 3.0f + 1.5f) to 0f
                val vx = (random.nextFloat() - 0.5f) * 0.007f
                val vy = -(random.nextFloat() * 0.014f + 0.009f)
                val life = random.nextFloat() * 1.2f + 0.6f
                particles.add(
                    Particle(
                        position = Offset(x, y),
                        velocity = Offset(vx, vy),
                        size = size,
                        color = color.copy(alpha = random.nextFloat() * 0.4f + 0.6f),
                        life = life,
                        maxLife = life
                    )
                )
            }

            for (p in particles) {
                p.position = Offset(p.position.x + p.velocity.x, p.position.y + p.velocity.y + 0.0003f)
                p.life -= 0.016f
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.life / p.maxLife).coerceIn(0f, 1f)
            val flicker = if (random.nextFloat() > 0.3f) 1f else 0.5f
            val alpha = progress * p.color.alpha * flicker
            val center = Offset(p.position.x * size.width, p.position.y * size.height)
            val radiusPx = (p.size.first.dp.toPx() / 2f).coerceAtLeast(1f)
            drawCircle(
                color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = radiusPx,
                center = center
            )
        }
    }
}
